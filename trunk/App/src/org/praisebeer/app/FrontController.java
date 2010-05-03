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
                IntentIntegrator.initiateScan(FrontController.this);
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
     * Handles results from both the barcode scanning activity and the result lookup activity
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // Result from bar code scan
        if(requestCode == IntentIntegrator.REQUEST_CODE)
        {
            if(resultCode != RESULT_CANCELED)
            {
                IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                if(scanResult != null)
                {
                    Intent i = new Intent(this, UpcResults.class);
                    i.putExtra("upcCode", scanResult.getContents());
                    i.putExtra("upcFormat", scanResult.getFormatName());
                    startActivityForResult(i, UpcResults.REQUEST_CODE);
                }
            }
        }
        // Result from UPC lookup
        else if(requestCode == UpcResults.REQUEST_CODE)
        {
            if(resultCode != RESULT_CANCELED)
            {
                ScanDetails scanResults = (ScanDetails) data.getSerializableExtra("scanResults");
                if(scanResults != null)
                {
                    if(scanResults.getScanSuccess())
                    {
                        Intent i = new Intent(this, ResultsDisplay.class);
                        i.putExtra("scanResults", scanResults);
                        startActivity(i);
                    }
                    else
                    {
                        switch(scanResults.getResultErrorCode())
                        {
                            case ApiErrorCodes.NO_BARCODE_RECIEVED:
                                //Generic text error
                                break;
                            case ApiErrorCodes.OUTGOING_REQUEST_FAILURE:
                                //Generic text error
                                break;
                            case ApiErrorCodes.INVALID_JSON_RESPONSE:
                                //Generic text error
                                break;
                            case ApiErrorCodes.INVALID_UPC_CODE:
                                //Generic text error
                                break;
                            case ApiErrorCodes.NO_UPC_CODE_SENT:
                                //Generic text error
                                break;
                            case ApiErrorCodes.NO_UPC_FOUND:
                                Intent i = new Intent(this, ManualUserEntry.class);
                                i.putExtra("scanResults", scanResults);
                                startActivity(i);
                                break;
                            case ApiErrorCodes.NO_BEER_FOUND:
                                break;
                            case ApiErrorCodes.RATING_LOOKUP_TIMEOUT:
                                break;
                        }
                        
                    }
                }
                else
                {
                    // TODO: Handle lack of response from UPC lookup, load
                    // generic error page?
                }
            }
        }
    }
}
