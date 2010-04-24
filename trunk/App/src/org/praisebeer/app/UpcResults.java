package org.praisebeer.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

public class UpcResults extends Activity implements Runnable{

	private String upcCode;
	private String upcFormat;
	private String product;
	private ProgressDialog pd;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upc_results);
        
        if(savedInstanceState == null)
        {
        	startRequestThread();
        }
    }
    
    /**
     * Takes input (UPC content, UPC type) from intent object and starts a thread
     * to search UPC database for result.
     */
    private void startRequestThread()
    {
    	Bundle extras = getIntent().getExtras(); 
    	if(extras != null)
        {
        	upcCode = extras.getString("content");
        	upcFormat = extras.getString("format");
        	if(upcCode != null && upcCode != "")
        	{	
        		pd = ProgressDialog.show(this, "Searching", "Searching for results", true, false);
        		Thread thread = new Thread(this);
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
    	int upcLength = upcCode.length();
    	if(upcLength != 12 && upcLength != 13)
    	{
    		//TODO: Error handling
    	}
    	else
    	{
	        try
	        {
	        	URL upcLookup = new URL("http://praisebeer.appspot.com/upclookup?upc=" + upcCode);
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
	        			product = apiResult.getString("description");
	        		}
	        		else
	        		{
	        			String errorMessage = apiResult.getString("errorResponse");
	        			product = errorMessage;
	        		}
	        	}
	        	catch(JSONException e)
	        	{
	        		//TODO: Error handling
	        	}
	        }
	        catch (Exception e)
	        {
	            product = "Request to server could not be made: " + e.getMessage();
	        }
    	}
        handler.sendEmptyMessage(0);
    }
    
    /**
     * Handler for thread control. Handles response from thread (UPC lookup result)
     * and updates UI
     */
    private Handler handler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	                pd.dismiss();
	                setResultText();
	        }
	};
	
	/**
	 * Sets text on screen to value of product found and type of UPC code scanned
	 */
	private void setResultText()
	{
		TextView resultContent = (TextView) findViewById(R.id.beerUpcContent);
        TextView resultType = (TextView) findViewById(R.id.beerUpcType);
        resultContent.setText(product);
        resultType.setText(this.getString(R.string.upcFormat) + " " + upcFormat);
	}
	
	/**
	 * Saves product results, UPC type, and UPC code so a duplicate request
	 * does not have to be made to database.
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) 
	{
		//Save found product in case Activity gets killed (orientation change, etc)
		savedInstanceState.putString("scannedProduct", product);
		savedInstanceState.putString("upcType", upcFormat);
		savedInstanceState.putString("upcCode", upcCode);
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
        product = savedInstanceState.getString("scannedProduct");
        upcFormat = savedInstanceState.getString("upcType"); 
        upcCode = savedInstanceState.getString("upcCode");
        
        //If data exists, display it. Otherwise request new data
        if(product != null && product != "")
        {
        	setResultText();
        }
        else
        {
        	startRequestThread();
        }
    }
}
