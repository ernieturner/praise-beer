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

public class SearchForBeer extends Activity
{
    public static final int REQUEST_CODE = 0x05347093;
    
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_form);
        
        Button submitSearch = (Button) findViewById(R.id.SF_submitButton);
        Button cancelSearch = (Button) findViewById(R.id.SF_cancelButton);
        
        submitSearch.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
                Editable keyword = ((EditText) findViewById(R.id.SF_beerNameEntry)).getText();
                if(keyword.toString().trim().equals(""))
                {
                    AlertDialog.Builder alert = new AlertDialog.Builder(SearchForBeer.this);
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
                    //Send keyword entered to API to do a search
                    Intent i = new Intent(SearchForBeer.this, ApiHandler.class);
                    i.putExtra("keyword", keyword.toString().trim());
                    i.putExtra("requestType", ApiHandler.KEYWORD_SEARCH);
                    startActivityForResult(i, ApiHandler.REQUEST_CODE);
                }
            }
        });

        cancelSearch.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
                Intent intent = new Intent();
                setResult(Activity.RESULT_CANCELED, intent);
                finish();
            }
        });
    }
    
    /**
     * Handles results from API. Specifically, gets the list of search results. Then
     * passes these to the results display page.
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == ApiHandler.REQUEST_CODE && resultCode != RESULT_CANCELED)
        {
            Intent i = new Intent(this, SearchResultsDisplay.class);
            i.putExtra("searchResults", data.getSerializableExtra("searchResults"));
            startActivity(i);
        }
    }
}
