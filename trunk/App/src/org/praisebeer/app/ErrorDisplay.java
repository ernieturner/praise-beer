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
        String errorMessage = extras.getString("errorMessage");
        String convertedErrorMessage = "";
        switch(errorCode)
        {
            case ApiErrorCodes.NO_BARCODE_RECIEVED:
                convertedErrorMessage = getString(R.string.error_noBarCodeRecieved);
                break;
            case ApiErrorCodes.OUTGOING_REQUEST_FAILURE:
                convertedErrorMessage = getString(R.string.error_requestFailure);
                break;
            case ApiErrorCodes.INVALID_JSON_RESPONSE:
                convertedErrorMessage = getString(R.string.error_invalidJsonResponse);
                break;
            case ApiErrorCodes.INVALID_UPC_CODE:
                convertedErrorMessage = getString(R.string.error_invalidUpcCode);
                break;
            case ApiErrorCodes.NO_UPC_CODE_SENT:
                convertedErrorMessage = getString(R.string.error_noUpcCodeSent);
                break;
            case ApiErrorCodes.RATING_LOOKUP_TIMEOUT:
                convertedErrorMessage = getString(R.string.error_requestTimeout);
                break;
            case ApiErrorCodes.NAME_LOOKUP_NO_DESCRIPTION_PROVIDED:
                convertedErrorMessage = getString(R.string.error_noDescription);
                break;
        }
        if(errorMessage != "" && errorMessage != null)
            convertedErrorMessage = convertedErrorMessage + " - " + errorMessage;
        ((TextView) findViewById(R.id.errorText)).setText(convertedErrorMessage);
    }
}