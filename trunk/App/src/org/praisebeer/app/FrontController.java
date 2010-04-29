package org.praisebeer.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class FrontController extends Activity 
{
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        // Obtain handles to UI objects
        Button scanBeer = (Button) findViewById(R.id.scanBeer);
        Button quitApp = (Button) findViewById(R.id.quitApp);
        
        // Register handler for beer scanning
        scanBeer.setOnClickListener(new Button.OnClickListener() {
        	public void onClick(View v)
        	{
        		Intent i = new Intent(FrontController.this, PraiseBeer.class);
    	    	startActivity(i);
            }
        });

    	// Register handler for closing app
        quitApp.setOnClickListener(new Button.OnClickListener() {
        	public void onClick(View v) {
        		finish();
            }
        });
    }
}
