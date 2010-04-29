package org.praisebeer.app;

import org.praisebeer.app.xing.IntentIntegrator;
import org.praisebeer.app.xing.IntentResult;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.TextView;

public class PraiseBeer extends Activity 
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        IntentIntegrator.initiateScan(PraiseBeer.this); 
    }
    
    /**
     * Handles results from both the barcode scanning activity and the
     * result lookup activity
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	//Result from bar code scan
    	if(requestCode == IntentIntegrator.REQUEST_CODE)
    	{
    		if(resultCode != RESULT_CANCELED)
    		{
	    		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
	    		if (scanResult != null) 
	    		{
	    			Intent i = new Intent(this, UpcResults.class);
	    	    	i.putExtra("upcCode", scanResult.getContents());
	    	    	i.putExtra("upcFormat", scanResult.getFormatName());
	    	    	startActivityForResult(i, UpcResults.REQUEST_CODE);
	    		}
    		}
    		else
    		{
    			//TODO: Handle barcode scan cancel
    		}
    	}
    	//Result from UPC lookup
    	else if(requestCode == UpcResults.REQUEST_CODE)
    	{
    		if(resultCode != RESULT_CANCELED)
    		{
    			ScanDetails scanResults = (ScanDetails) data.getSerializableExtra("scanResults");
    			setResultText(scanResults);
    		}
    		else
    		{
    			//TODO: Handle request cancel
    		}
    	}
    }
    
    /**
	 * Sets text on screen to value of product found and type of UPC code scanned
	 */
	private void setResultText(ScanDetails scanResults)
	{
		TextView resultContent = (TextView) findViewById(R.id.beerUpcContent);
		TextView resultLinks = (TextView) findViewById(R.id.beerUpcLink);
        TextView resultType = (TextView) findViewById(R.id.beerUpcType);
        
        resultContent.setText(scanResults.getProduct());
        resultLinks.setText(scanResults.getProductLink());
        Linkify.addLinks(resultLinks, Linkify.WEB_URLS);
        resultType.setText(scanResults.getUpcCode() + " - " + scanResults.getUpcFormat());
	}
}