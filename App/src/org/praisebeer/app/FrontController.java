package org.praisebeer.app;

import org.praisebeer.app.xing.IntentIntegrator;
import org.praisebeer.app.xing.IntentResult;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class FrontController extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        // Obtain handles to UI objects
        Button scanBeer = (Button) findViewById(R.id.scanBeer);
        Button quitApp = (Button) findViewById(R.id.quitApp);

        // Register handler for beer scanning
        scanBeer.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
                IntentIntegrator.initiateScan(FrontController.this,
                                              IntentIntegrator.DEFAULT_TITLE,
                                              IntentIntegrator.DEFAULT_MESSAGE,
                                              IntentIntegrator.DEFAULT_YES,
                                              IntentIntegrator.DEFAULT_NO,
                                              IntentIntegrator.PRODUCT_CODE_TYPES);
            }
        });

        // Register handler for closing app
        quitApp.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
                finish();
            }
        });
    }

    /**
     * Handles results from both the barcode scanning activity, the result lookup activity, and the
     * manual user entry activity
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // Result from bar code scan
        if(requestCode == IntentIntegrator.REQUEST_CODE && resultCode != RESULT_CANCELED)
        {
            this.handleScanResposne(requestCode, resultCode, data);
        }
        //Result from manual beer entry or user fixing an entry
        else if((requestCode == ManualUserEntry.REQUEST_CODE || requestCode == FixEntry.REQUEST_CODE) && resultCode != RESULT_CANCELED)
        {
            this.handleManualUserEntryResponse(data);
        }
        // Result from UPC lookup
        else if(requestCode == ApiHandler.REQUEST_CODE && resultCode != RESULT_CANCELED)
        {
            this.handleResultsResponse(data);
        }
        //Result from results display i.e. incorrect beer reported
        else if(requestCode == ResultsDisplay.REQUEST_CODE && resultCode != RESULT_CANCELED)
        {
            this.handleIncorrectResultShown(data, true);
        }
    }
    
    /**
     * Handles data returned from scan activity and sends data to API query activity
     * @param requestCode int Request code sent back from activity
     * @param resultCode int Result code sent back from activity
     * @param data Intent Full data recieved from activity
     */
    private void handleScanResposne(int requestCode, int resultCode, Intent data)
    {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(scanResult != null)
        {
            String upcCodeScanned = scanResult.getContents();
            int upcCodeLength = upcCodeScanned.length();

            //Check the length of the UPC code scanned. If it isn't one of the supported lengths,
            //that means there might have been a scanning error, so display the error message
            if(upcCodeLength != 6 && upcCodeLength != 8 && upcCodeLength != 12 && upcCodeLength != 13)
            {
                Intent i = new Intent(this, ErrorDisplay.class);
                i.putExtra("errorCode", ApiErrorCodes.INVALID_UPC_CODE);
                startActivity(i);
                return;
            }
            Intent i = new Intent(this, ApiHandler.class);
            i.putExtra("upcCode", scanResult.getContents());
            startActivityForResult(i, ApiHandler.REQUEST_CODE);
        }
    }
    
    /**
     * Pulls out the UPC and description sent back by the ManualUserEntry class
     * and sends it to the UpcResults class
     * @param data Intent Data recieved from activity
     */
    private void handleManualUserEntryResponse(Intent data)
    {
        //Start lookup, but pass in description as well as UPC code. This
        //activity knows what URL to use depending on what data is passed to it
        Intent i = new Intent(this, ApiHandler.class);
        i.putExtra("upcCode", data.getStringExtra("upcCode"));
        i.putExtra("descriptionEntered", data.getStringExtra("descriptionEntered"));
        boolean fixingEntry = data.getBooleanExtra("entryModification", false);
        if(fixingEntry == true)
            i.putExtra("entryModification", true);
        startActivityForResult(i, ApiHandler.REQUEST_CODE);
    }
    
    /**
     * Pulls out the ScanDetails instance from the intent, checks for errors
     * and sends data to the next activity to display the results.
     * @param data Intent Data recieved from activity
     */
    private void handleResultsResponse(Intent data)
    {
        BeerDetails scanResults = (BeerDetails) data.getSerializableExtra("scanResults");
        if(scanResults != null)
        {
            if(scanResults.getScanSuccess())
            {
                Intent i = new Intent(this, ResultsDisplay.class);
                i.putExtra("scanResults", scanResults);
                startActivityForResult(i, ResultsDisplay.REQUEST_CODE);
            }
            else
            {
                //No UPC found, ask user for manual entry
                if(scanResults.getResultErrorCode() == ApiErrorCodes.NO_UPC_FOUND)
                {
                    Intent i = new Intent(this, ManualUserEntry.class);
                    i.putExtra("upcValue", scanResults.getUpcCode());
                    startActivityForResult(i, ManualUserEntry.REQUEST_CODE);
                }
                //No beer found, ask user for description modification
                else if(scanResults.getResultErrorCode() == ApiErrorCodes.NO_BEER_FOUND)
                {
                    if(scanResults.getProductName() != null)
                    {
                        Intent fixDescription = new Intent();
                        fixDescription.putExtra("currentBeerName", scanResults.getProductName());
                        fixDescription.putExtra("upcCode", scanResults.getUpcCode());
                        this.handleIncorrectResultShown(fixDescription, false);
                    }
                }
                //Some other error, handle with generic message display
                else
                {
                    Intent i = new Intent(this, ErrorDisplay.class);
                    i.putExtra("errorCode", scanResults.getResultErrorCode());
                    i.putExtra("errorMessage", scanResults.getResultErrorMessage());
                    startActivity(i);
                }
            }
        }
        else
        {
            // TODO: Handle lack of response from UPC lookup, load
            // generic error page?
        }
    }
    
    /**
     * Handles use case when user clicks button saying we've found the wrong beer. At
     * this point we just ask the user to manually enter the beer name as we don't really
     * have any better options at this time.
     * @param data Intent Data of results we've shown so we can make a modification
     */
    private void handleIncorrectResultShown(Intent data, boolean existingEntry)
    {
        Intent i = new Intent(this, FixEntry.class);
        i.putExtra("upcCode", data.getStringExtra("upcCode"));
        i.putExtra("currentBeerName", data.getStringExtra("currentBeerName"));
        if(existingEntry == false)
            i.putExtra("nonExistingEntry", true);
        startActivityForResult(i, FixEntry.REQUEST_CODE);
    }
}
