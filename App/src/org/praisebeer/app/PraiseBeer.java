package org.praisebeer.app;

import org.praisebeer.app.xing.IntentIntegrator;
import org.praisebeer.app.xing.IntentResult;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class PraiseBeer extends Activity {
	
	private Button scanBeer;
	private Button quitApp;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Obtain handles to UI objects
        scanBeer = (Button) findViewById(R.id.scanBeer);
        quitApp = (Button) findViewById(R.id.quitApp);
        
        // Register handler for beer scanning
        scanBeer.setOnClickListener(new Button.OnClickListener() {
        	public void onClick(View v) {
        		IntentIntegrator.initiateScan(PraiseBeer.this); 
            }
        });
    	// Register handler for closing app
        quitApp.setOnClickListener(new Button.OnClickListener() {
        	public void onClick(View v) {
        		finish();
            }
        });
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    { 
    	if(requestCode == IntentIntegrator.REQUEST_CODE && resultCode != RESULT_CANCELED)
    	{
    		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
    		if (scanResult != null) { 
    			Intent i = new Intent(this, UpcResults.class);
    	    	i.putExtra("content", scanResult.getContents());
    	    	i.putExtra("format", scanResult.getFormatName());
    	    	startActivity(i);
    		} 
    	}
    }
}