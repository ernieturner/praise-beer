package org.praisebeer.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.util.Linkify;
import android.widget.TextView;

public class UpcResults extends Activity implements Runnable
{
	private boolean sentUpcRequest = false;
	private static int SEARCH_DIALOG_ID = 1;
	
	private ScanDetails upcDetails;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upc_results);
        
        if(savedInstanceState == null)
        {
        	startRequestThread();
        }
    }

/***********************************
* Search thread functions
**********************************/
    
    /**
     * Takes input (UPC content, UPC type) from intent object and starts a thread
     * to search UPC database for result.
     */
    private void startRequestThread()
    {
    	Bundle extras = getIntent().getExtras(); 
    	if(extras != null)
        {
    		upcDetails = new ScanDetails(extras.getString("upcCode"), extras.getString("upcFormat"));
        	if(upcDetails.getUpcCode() != null && upcDetails.getUpcCode() != "")
        	{	
        		showDialog(SEARCH_DIALOG_ID);
        		Thread thread = new Thread(this);
        		sentUpcRequest = true;
        		thread.start();
        		return;
        	}
        }
    	//Handle failure state
        TextView mainText = (TextView) findViewById(R.id.beerUpcContent);
        mainText.setText(this.getString(R.string.noDataEntered));
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
        			//Convert links from JSON array to normal array
        			JSONArray apiLinks = apiResult.getJSONArray("links");
        			String[] links = new String[16];
        			for(int i=0; i<apiLinks.length(); i++)
        				links[i] = apiLinks.getString(i);
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
            //product = "Request to server could not be made: " + e.getMessage();
        }
        handler.sendEmptyMessage(0);
    }
    
    /**
     * Handler for thread control. Handles response from thread (UPC lookup result)
     * and updates UI
     */
    private Handler handler = new Handler() 
    {
	        @Override
	        public void handleMessage(Message msg) 
	        {
        		dismissDialog(UpcResults.SEARCH_DIALOG_ID);
        		setResultText();
	        }
	};
	
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
	
	/**
	 * Sets text on screen to value of product found and type of UPC code scanned
	 */
	private void setResultText()
	{
		//TODO: Eventually we need to forward to another activity based on what happened
		TextView resultContent = (TextView) findViewById(R.id.beerUpcContent);
		TextView resultLinks = (TextView) findViewById(R.id.beerUpcLink);
        TextView resultType = (TextView) findViewById(R.id.beerUpcType);
        
        resultContent.setText(upcDetails.getProduct());
        resultLinks.setText(upcDetails.getProductLink());
        Linkify.addLinks(resultLinks, Linkify.WEB_URLS);
        resultType.setText(upcDetails.getUpcCode() + " - " + upcDetails.getUpcFormat());
	}

/***********************************
 * State saving functions
 **********************************/
	
	/**
	 * Saves product results, UPC type, and UPC code so a duplicate request
	 * does not have to be made to database.
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) 
	{
		//Save found product in case Activity gets killed (orientation change, etc)
		savedInstanceState.putString("scannedProduct", upcDetails.getProduct());
		savedInstanceState.putString("upcType", upcDetails.getUpcFormat());
		savedInstanceState.putString("upcCode", upcDetails.getUpcCode());
		savedInstanceState.putStringArray("productLinks", upcDetails.getProductLinks());
		savedInstanceState.putBoolean("sentUpcRequest", sentUpcRequest);
		super.onSaveInstanceState(savedInstanceState);
	}
	
	/**
	 * Attempts to pull saved data out of instance state. If data is found, the
	 * UI is updated with the results. If no data is found, a request to the UPC
	 * database is made.
	 */
	@Override
    public void onRestoreInstanceState(Bundle savedInstanceState) 
    {
		//Attempt to read out saved data
        super.onRestoreInstanceState(savedInstanceState);
        upcDetails = new ScanDetails(savedInstanceState.getString("upcCode"), 
        							savedInstanceState.getString("upcType"),
        							savedInstanceState.getString("scannedProduct"),
        							savedInstanceState.getStringArray("productLinks")
        							);

        sentUpcRequest = savedInstanceState.getBoolean("sentUpcRequest");
        
        //If data exists, display it. Otherwise request new data
        if(sentUpcRequest == true)
        {
        	setResultText();
        }
        else
        {
        	startRequestThread();
        }
    }
}
