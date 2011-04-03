package org.praisebeer.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ApiHandler extends Activity implements Runnable
{
    public static final int REQUEST_CODE = 0x009c5ca4;
    
    public static final int UPC_LOOKUP = 1;
    public static final int KEYWORD_SEARCH = 2;
    public static final int ID_LOOKUP = 3;
    public int currentRequestType = 0;
    
    private static int SEARCH_DIALOG_ID = 1;
    private String requestUrl = "http://praisebeer.appspot.com";
    private String baBaseUrl = "http://beeradvocate.com/beer/profile/";
    
    private BeerDetails beerDetails;
    private ArrayList<String[]> searchResults;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if(extras == null)
        {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
        
        this.currentRequestType = extras.getInt("requestType");
        
        if(this.currentRequestType == ApiHandler.UPC_LOOKUP)
        {
            String upcCode = extras.getString("upcCode");
            String description = extras.getString("descriptionEntered");
            if(upcCode != null && !upcCode.equals(""))
            {
                this.beerDetails = new BeerDetails(extras.getString("upcCode"));
                if(description != null && !description.equals(""))
                {
                    this.beerDetails.setStoredBeerName(description);
                    this.requestUrl += "/namelookup?upc=" + this.beerDetails.getUpcCode() + "&description=" + URLEncoder.encode(this.beerDetails.getStoredBeerName());
                    if(extras.getBoolean("entryModification") == true)
                        this.requestUrl += "&mod=1";
                }
                else
                {
                    this.requestUrl += "/upclookup?upc=" + this.beerDetails.getUpcCode();
                }
            }
            else
            {
                this.beerDetails.setResultErrorCode(ApiErrorCodes.NO_BARCODE_RECIEVED);
                this.beerDetails.setScanSuccess(false);
                sendResultAndFinishActivity();
                return;
            }
        }
        else if(this.currentRequestType == ApiHandler.KEYWORD_SEARCH)
        {
            String keyword = extras.getString("keyword");
            this.requestUrl += "/namesearch?keyword=" + URLEncoder.encode(keyword);
        }
        else if(this.currentRequestType == ApiHandler.ID_LOOKUP)
        {
            String beerPageID = extras.getString("beerPageID");
            this.beerDetails = new BeerDetails("");
            //Because we wont get back links from the request and we know what the resulting URL is,
            //just hardcode in the URL
            Vector<String> link = new Vector<String>();
            link.add(this.baBaseUrl + beerPageID);
            this.beerDetails.setProductLinks(link);
            if(beerPageID != null && !beerPageID.equals(""))
            {
                this.requestUrl += "/idlookup?id=" + URLEncoder.encode(beerPageID);
            }
        }

        showDialog(SEARCH_DIALOG_ID);
        Thread thread = new Thread(this);
        thread.start();
        return;
    }

    /**
     * Method extended from Runnable class when thread is started. Takes product UPC code and makes an XML-RPC request to UPC database. Once result is found, extracts out description and sends update message back to UI
     */
    public void run()
    {
        String jsonResults = "";
        String inputLine;
        try
        {
            URL upcLookup = new URL(this.requestUrl);
            BufferedReader in = new BufferedReader(new InputStreamReader(upcLookup.openStream()));
            
            while((inputLine = in.readLine()) != null)
                jsonResults += inputLine;
            in.close();
        }
        catch(Exception e)
        {
            this.beerDetails.setResultErrorCode(ApiErrorCodes.OUTGOING_REQUEST_FAILURE);
            this.beerDetails.setResultErrorMessage(e.getMessage());
            this.beerDetails.setScanSuccess(false);
            handler.sendEmptyMessage(0);
            return;
        }
        
        if(this.currentRequestType == ApiHandler.UPC_LOOKUP || this.currentRequestType == ApiHandler.ID_LOOKUP){
            this.handleBeerResult(jsonResults);
        }
        else if(this.currentRequestType == ApiHandler.KEYWORD_SEARCH){
            this.handleSearchResults(jsonResults);
        }
        
        handler.sendEmptyMessage(0);
    }

    /**
     * Handler for parsing results of UPC lookup or name lookup
     * @param results JSON string results from API request
     */
    private void handleBeerResult(String results)
    {
        try
        {
            JSONObject apiResult = new JSONObject(results);
            boolean success = apiResult.getBoolean("success");
            if(success)
            {
                this.beerDetails.setScanSuccess(true);
                if(this.currentRequestType == ApiHandler.UPC_LOOKUP)
                    this.beerDetails.setStoredBeerName(apiResult.getString("description"));
                JSONObject beerInfo = apiResult.getJSONObject("beer_info");
                if(beerInfo != null)
                {
                    JSONObject ratings = beerInfo.getJSONObject("ratings");
                    try
                    {
                        JSONArray communityRating = ratings.getJSONArray("overall");
                        this.beerDetails.setCommunityRating(communityRating.getString(0));
                        this.beerDetails.setCommunityRatingDescription(communityRating.getString(1));
                        this.beerDetails.setNumberOfRatings(communityRating.getString(2));
                    }
                    catch(JSONException e){/**We're going to assume we always send back valid JSON*/}

                    try
                    {
                        JSONArray brothersRating = ratings.getJSONArray("bros");
                        this.beerDetails.setBrothersRating(brothersRating.getString(0));
                        this.beerDetails.setBrothersRatingDescription(brothersRating.getString(1));
                    }
                    catch(JSONException e){/**We're going to assume we always send back valid JSON*/}
                    
                    try
                    {
                        JSONObject stats = beerInfo.getJSONObject("stats");
                        try{
                            this.beerDetails.setProductName(stats.getString("name"));
                        }
                        catch(JSONException e){}
                        try{
                            this.beerDetails.setBeerABV(stats.getString("abv"));
                        }
                        catch(JSONException e){}
                        try{
                            this.beerDetails.setBeerStyle(stats.getString("style_name"));
                        }
                        catch(JSONException e){}
                        try{
                            this.beerDetails.setBeerStyleID(stats.getString("style_id"));
                        }
                        catch(JSONException e){}
                        try{
                            JSONArray breweryInfo = stats.getJSONArray("brewery");
                            this.beerDetails.setBreweryID(breweryInfo.getString(0));
                            this.beerDetails.setBreweryName(breweryInfo.getString(1));
                        }
                        catch(JSONException e){}
                    }
                    catch(JSONException e){}
                }
                if(this.currentRequestType == ApiHandler.UPC_LOOKUP)
                {
                    // Convert links from JSON array to normal array
                    JSONArray apiLinks = apiResult.getJSONArray("links");
                    Vector<String> links = new Vector<String>();
                    for(int i = 0; i < apiLinks.length(); i++)
                        links.add(apiLinks.getString(i));
                    this.beerDetails.setProductLinks(links);
                }
            }
            else
            {
                int errorCode = apiResult.getInt("error_code");
                //If we didn't find a beer, we should at least have gotten a description
                if(errorCode == ApiErrorCodes.NO_BEER_FOUND && this.currentRequestType == ApiHandler.UPC_LOOKUP)
                {
                    try
                    {
                        JSONObject errorDetails = apiResult.getJSONObject("errorResponse");
                        this.beerDetails.setProductName(errorDetails.getString("description"));
                    }
                    catch(JSONException e){}
                }
                this.beerDetails.setResultErrorCode(errorCode);
                this.beerDetails.setScanSuccess(false);
            }
        }
        catch(JSONException e)
        {
            this.beerDetails.setResultErrorCode(ApiErrorCodes.INVALID_JSON_RESPONSE);
            this.beerDetails.setResultErrorMessage(e.getMessage());
            this.beerDetails.setScanSuccess(false);
        }
    }
    
    /**
     * Handles results from search request
     * @param results JSON string of search results
     */
    private void handleSearchResults(String results)
    {
        try{
            JSONObject apiResult = new JSONObject(results);
            if(apiResult.getBoolean("success"))
            {
                JSONArray searchResults = apiResult.getJSONArray("results");
                ArrayList<String[]> list = new ArrayList<String[]>();     
                for (int i=0;i<searchResults.length();i++){
                    String[] result = new String[] {searchResults.getJSONObject(i).getString("url"), searchResults.getJSONObject(i).getString("title")};
                    list.add(result);
                }
                this.searchResults = list;
            }
            else
            {
                //int errorCode = apiResult.getInt("error_code");
            }
        }
        catch(JSONException e){}
    }
    
    /**
     * Handler for thread control. Handles response from thread (UPC/name lookup result), sets result and finishes activity
     */
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg)
        {
            sendResultAndFinishActivity();
        }
    };

    /**
     * Default dialog function to create progress dialog during search results
     */
    protected Dialog onCreateDialog(int id)
    {
        if(id == SEARCH_DIALOG_ID)
        {
            ProgressDialog loadingDialog = new ProgressDialog(this);
            loadingDialog.setTitle(getString(R.string.pleaseWaitEllipsis));
            loadingDialog.setMessage(getString(R.string.searchingForResults));
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
            return loadingDialog;
        }
        return super.onCreateDialog(id);
    }
    
    /**
     * Dismissing dialog and sends results back
     */
    private void sendResultAndFinishActivity()
    {
        dismissDialog(ApiHandler.SEARCH_DIALOG_ID);
        Intent intent = new Intent();
        
        if(this.currentRequestType == ApiHandler.UPC_LOOKUP || this.currentRequestType == ApiHandler.ID_LOOKUP){
            intent.putExtra("scanResults", this.beerDetails);
        }
        else if(this.currentRequestType == ApiHandler.KEYWORD_SEARCH){
            intent.putExtra("searchResults", this.searchResults);
        }
        intent.putExtra("requestType", this.currentRequestType);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
