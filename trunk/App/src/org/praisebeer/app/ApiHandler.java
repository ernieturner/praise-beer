package org.praisebeer.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
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
    private static int SEARCH_DIALOG_ID = 1;
    private BeerDetails beerDetails;
    private String requestUrl = "http://praisebeer.appspot.com";

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
        
        beerDetails = new BeerDetails(extras.getString("upcCode"));
        String description = extras.getString("descriptionEntered");
        
        //Determine if this is a UPC lookup or a name lookup
        if(description == null || description.equals(""))
        {
            this.requestUrl += "/upclookup?upc=" + beerDetails.getUpcCode();
        }
        else
        {
            beerDetails.setStoredBeerName(description);
            this.requestUrl += "/namelookup?upc=" + beerDetails.getUpcCode() + "&description=" + URLEncoder.encode(beerDetails.getStoredBeerName());
            if(extras.getBoolean("entryModification") == true)
                this.requestUrl += "&mod=1";
        }
        
        if(beerDetails.getUpcCode() != null && !beerDetails.getUpcCode().equals(""))
        {
            showDialog(SEARCH_DIALOG_ID);
            Thread thread = new Thread(this);
            thread.start();
            return;
        }
        else
        {
            beerDetails.setResultErrorCode(ApiErrorCodes.NO_BARCODE_RECIEVED);
            beerDetails.setScanSuccess(false);
            sendResultAndFinishActivity();
        }
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
            beerDetails.setResultErrorCode(ApiErrorCodes.OUTGOING_REQUEST_FAILURE);
            beerDetails.setResultErrorMessage(e.getMessage());
            beerDetails.setScanSuccess(false);
            handler.sendEmptyMessage(0);
            return;
        }
        try
        {
            JSONObject apiResult = new JSONObject(jsonResults);
            boolean success = apiResult.getBoolean("success");
            if(success)
            {
                beerDetails.setScanSuccess(true);
                beerDetails.setStoredBeerName(apiResult.getString("description"));
                JSONObject beerInfo = apiResult.getJSONObject("beer_info");
                if(beerInfo != null)
                {
                    JSONObject ratings = beerInfo.getJSONObject("ratings");
                    try
                    {
                        JSONArray communityRating = ratings.getJSONArray("overall");
                        beerDetails.setCommunityRating(communityRating.getString(0));
                        beerDetails.setCommunityRatingDescription(communityRating.getString(1));
                        beerDetails.setNumberOfRatings(communityRating.getString(2));
                    }
                    catch(JSONException e){/**We're going to assume we always send back valid JSON*/}

                    try
                    {
                        JSONArray brothersRating = ratings.getJSONArray("bros");
                        beerDetails.setBrothersRating(brothersRating.getString(0));
                        beerDetails.setBrothersRatingDescription(brothersRating.getString(1));
                    }
                    catch(JSONException e){/**We're going to assume we always send back valid JSON*/}
                    
                    try
                    {
                        JSONObject stats = beerInfo.getJSONObject("stats");
                        try{
                            beerDetails.setProductName(stats.getString("name"));
                        }
                        catch(JSONException e){}
                        try{
                            beerDetails.setBeerABV(stats.getString("abv"));
                        }
                        catch(JSONException e){}
                        try{
                            beerDetails.setBeerStyle(stats.getString("style_name"));
                        }
                        catch(JSONException e){}
                        try{
                            beerDetails.setBeerStyleID(stats.getString("style_id"));
                        }
                        catch(JSONException e){}
                    }
                    catch(JSONException e){}
                }
                // Convert links from JSON array to normal array
                JSONArray apiLinks = apiResult.getJSONArray("links");
                Vector<String> links = new Vector<String>();
                for(int i = 0; i < apiLinks.length(); i++)
                    links.add(apiLinks.getString(i));
                beerDetails.setProductLinks(links);
            }
            else
            {
                int errorCode = apiResult.getInt("error_code");
                //If we didn't find a beer, we should at least have gotten a description
                if(errorCode == ApiErrorCodes.NO_BEER_FOUND)
                {
                    try
                    {
                        JSONObject errorDetails = apiResult.getJSONObject("errorResponse");
                        beerDetails.setProductName(errorDetails.getString("description"));
                    }
                    catch(JSONException e){}
                }
                beerDetails.setResultErrorCode(errorCode);
                beerDetails.setScanSuccess(false);
            }
        }
        catch(JSONException e)
        {
            beerDetails.setResultErrorCode(ApiErrorCodes.INVALID_JSON_RESPONSE);
            beerDetails.setResultErrorMessage(e.getMessage());
            beerDetails.setScanSuccess(false);
        }
        
        handler.sendEmptyMessage(0);
    }

    /**
     * Handler for thread control. Handles response from thread (UPC lookup result), sets result and finishes activity
     */
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){sendResultAndFinishActivity();}
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
        intent.putExtra("scanResults", beerDetails);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
