package org.praisebeer.app;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

public class SearchResultsDisplay extends Activity
{
    private String[] beerTitle;
    private String[] beerUrl;
    
    @SuppressWarnings("unchecked")
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_results);
        
        Bundle extras = getIntent().getExtras();
        if(extras == null){
            return;
        }
        
        ArrayList<String[]> results = (ArrayList<String[]>)extras.getSerializable("searchResults");
        
        beerTitle = new String[results.size()];
        beerUrl = new String[results.size()];
        for(int i=0; i<results.size(); i++)
        {
            String[] result = results.get(i);
            this.beerUrl[i] = result[0];
            this.beerTitle[i] = result[1];
        }
        ListView resultsDisplay = (ListView) findViewById(R.id.list);
        // By using setAdpater method in listview we an add string array in list.
        resultsDisplay.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1 , this.beerTitle));
        
        resultsDisplay.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String URL = SearchResultsDisplay.this.beerUrl[position];
                //Send beer ID entered to API to look up results
                Intent i = new Intent(SearchResultsDisplay.this, ApiHandler.class);
                i.putExtra("beerPageID", URL);
                i.putExtra("requestType", ApiHandler.ID_LOOKUP);
                startActivityForResult(i, ApiHandler.REQUEST_CODE);
                URL += "";
            }
        });
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        BeerDetails scanResults = (BeerDetails) data.getSerializableExtra("scanResults");
        if(scanResults != null)
        {
            if(scanResults.getScanSuccess())
            {
                Intent i = new Intent(this, ResultsDisplay.class);
                i.putExtra("scanResults", scanResults);
                startActivity(i);
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
}
