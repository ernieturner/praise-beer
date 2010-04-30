package org.praisebeer.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
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

public class UpcResults extends Activity implements Runnable
{
	public static final int REQUEST_CODE = 0x009c5ca4;
	private static int SEARCH_DIALOG_ID = 1;
	private ScanDetails upcDetails;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if(extras == null)
        {
        	setResult(RESULT_CANCELED);
        	finish();
        }
        
        upcDetails = new ScanDetails(extras.getString("upcCode"), extras.getString("upcFormat"));
        
        if(upcDetails.getUpcCode() != null && upcDetails.getUpcCode() != "")
    	{	
    		showDialog(SEARCH_DIALOG_ID);
    		Thread thread = new Thread(this);
    		thread.start();
    		return;
    	}
    	else
    	{
    		//TODO: Error handling, invalid barcode scanned
    	}
    }

    /**
     * Method extended from Runnable class when thread is started. Takes product UPC
     * code and makes an XML-RPC request to UPC database. Once result is found, extracts
     * out description and sends update message back to UI
     */
	public void run()
    {
        try
        {
        	URL upcLookup = new URL("http://praisebeer.appspot.com/upclookup?upc=" + upcDetails.getUpcCode());
        	BufferedReader in = new BufferedReader(new InputStreamReader(upcLookup.openStream()));

        	String jsonResults = "";
        	String inputLine;

        	while ((inputLine = in.readLine()) != null)
        		jsonResults += inputLine;
        	in.close();
        	try
        	{
        		JSONObject apiResult = new JSONObject(jsonResults);
        		boolean success = apiResult.getBoolean("success");
        		if(success)
        		{
        			upcDetails.setProductName(apiResult.getString("description"));
        			JSONObject ratings = apiResult.getJSONObject("rating");
        			if(ratings != null)
        			{
        				try{
        					JSONArray communityRating = ratings.getJSONArray("overall");
    	        			upcDetails.setCommunityRating(communityRating.getString(0));
    	        			upcDetails.setCommunityRatingDescription(communityRating.getString(1));
    	        			upcDetails.setNumberOfRatings(communityRating.getString(2));
        				}
        				catch(JSONException e){}
        				
        				try{
        					JSONArray brothersRating = ratings.getJSONArray("bros");
    	        			upcDetails.setBrothersRating(brothersRating.getString(0));
    	        			upcDetails.setBrothersRatingDescription(brothersRating.getString(1));
        				}
        				catch(JSONException e){}
        				
        			}
        			//Convert links from JSON array to normal array
        			JSONArray apiLinks = apiResult.getJSONArray("links");
        			Vector <String> links = new Vector<String>();
        			for(int i=0; i<apiLinks.length(); i++)
        				links.add(apiLinks.getString(i));
        			upcDetails.setProductLinks(links);
        		}
        		else
        		{
        			//TODO: Error handling
        			//      1-check if UPC lookup fail
        			//        a-Ask user for manual entry
        			//      2-check if search lookup fail
        			//        b-Give user product name found, ask for modification
        			upcDetails.setProductName(apiResult.getString("errorResponse"));
        		}
        	}
        	catch(JSONException e)
        	{
        		//TODO: Error handling
        	}
        }
        catch (Exception e)
        {
        	//TODO: Error handling
        }
        handler.sendEmptyMessage(0);
    }
    
    /**
     * Handler for thread control. Handles response from thread (UPC lookup result), sets
     * result and finishes activity
     */
    private Handler handler = new Handler() 
    {
    	@Override
	    public void handleMessage(Message msg)
	    {
    		dismissDialog(UpcResults.SEARCH_DIALOG_ID);
        	Intent intent = new Intent();
        	intent.putExtra("scanResults", upcDetails);
        	setResult(Activity.RESULT_OK, intent);
        	finish();
	    }
	};
	
	/**
	 * Default dialog function to create progress dialog
	 * during search results
	 */
	protected Dialog onCreateDialog(int id) 
	{
		if(id == SEARCH_DIALOG_ID)
		{
			ProgressDialog loadingDialog = new ProgressDialog(this);
			loadingDialog.setTitle("Searching...");
			loadingDialog.setMessage("Searching for results");
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(false);
			return loadingDialog;
		}
		return super.onCreateDialog(id);
	}
}
