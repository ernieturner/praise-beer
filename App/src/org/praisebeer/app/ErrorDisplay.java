package org.praisebeer.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ErrorDisplay extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.error);
        Bundle extras = getIntent().getExtras();
        int errorCode = extras.getInt("errorCode");
        String errorMessage = "";
        switch(errorCode)
        {
            case ApiErrorCodes.NO_BARCODE_RECIEVED:
                errorMessage = getString(R.string.error_noBarCodeRecieved);
                break;
            case ApiErrorCodes.OUTGOING_REQUEST_FAILURE:
                errorMessage = getString(R.string.error_requestFailure);
                break;
            case ApiErrorCodes.INVALID_JSON_RESPONSE:
                errorMessage = getString(R.string.error_invalidJsonResponse);
                break;
            case ApiErrorCodes.INVALID_UPC_CODE:
                errorMessage = getString(R.string.error_invalidUpcCode);
                break;
            case ApiErrorCodes.NO_UPC_CODE_SENT:
                errorMessage = getString(R.string.error_noUpcCodeSent);
                break;
            case ApiErrorCodes.RATING_LOOKUP_TIMEOUT:
                errorMessage = getString(R.string.error_requestTimeout);
                break;
        }
        ((TextView) findViewById(R.id.errorText)).setText(errorMessage);
    }
}