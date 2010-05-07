package org.praisebeer.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ManualUserEntry extends Activity
{
    //private ScanDetails scanResults;'
    public static final int REQUEST_CODE = 0x04390357;
    private String upcCode = "";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manual_entry);
        Bundle extras = getIntent().getExtras();
        this.upcCode = extras.getString("upcValue");
        
        Button submitBeerUPC = (Button) findViewById(R.id.ME_submitButton);
        Button cancelSubmission = (Button) findViewById(R.id.ME_cancelButton);
        
        submitBeerUPC.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
                Editable beerDescription = ((EditText) findViewById(R.id.ME_beerNameEntry)).getText();
                if(beerDescription.toString().trim().equals(""))
                {
                    AlertDialog.Builder alert = new AlertDialog.Builder(ManualUserEntry.this);
                    alert.setMessage(getString(R.string.pleaseFillOutDescription))
                           .setCancelable(false)
                           .setNegativeButton(getString(R.string.okLabel), new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                               }
                           });
                    alert.show();
                }
                else
                {
                    Intent i = new Intent();
                    i.putExtra("descriptionEntered", beerDescription.toString().trim());
                    i.putExtra("upcCode", ManualUserEntry.this.upcCode);
                    setResult(Activity.RESULT_OK, i);
                    finish();
                }
            }
        });

        cancelSubmission.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
                Intent intent = new Intent();
                setResult(Activity.RESULT_CANCELED, intent);
                finish();
            }
        });
    }
}