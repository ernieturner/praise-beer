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
import android.widget.TextView;

public class FixEntry extends Activity
{
    //private ScanDetails scanResults;'
    public static final int REQUEST_CODE = 0x4160A34C;
    private String upcCode = "";
    private String currentDescription = "";
    private boolean modification = true;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fix_entry);
        Bundle extras = getIntent().getExtras();
        this.upcCode = extras.getString("upcCode");
        this.currentDescription = extras.getString("currentBeerName");
        
        //If we're not modifying an existing entry, we need to change up the wording we display to make
        //more sense as to what we're doing
        if(extras.getBoolean("nonExistingEntry") == true)
        {
            this.modification = false;
            ((TextView)findViewById(R.id.FE_noResultsFoundMessage)).setText(getString(R.string.noBeerFoundWithDescription));
        }
        
        ((TextView) findViewById(R.id.FE_beerNameEntry)).setText(this.currentDescription);
        
        Button submitBeerUPC = (Button) findViewById(R.id.FE_submitButton);
        Button cancelSubmission = (Button) findViewById(R.id.FE_cancelButton);
        
        submitBeerUPC.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
                Editable beerDescription = ((EditText) findViewById(R.id.FE_beerNameEntry)).getText();
                if(beerDescription.toString().trim().equals(""))
                {
                    AlertDialog.Builder alert = new AlertDialog.Builder(FixEntry.this);
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
                    i.putExtra("upcCode", FixEntry.this.upcCode);
                    i.putExtra("entryModification", FixEntry.this.modification);
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