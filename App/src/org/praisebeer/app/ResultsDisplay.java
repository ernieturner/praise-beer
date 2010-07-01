package org.praisebeer.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class ResultsDisplay extends Activity
{
    private BeerDetails scanResults;
    public static int REQUEST_CODE = 0x08468EE4;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_results);
        Bundle extras = getIntent().getExtras();
        scanResults = (BeerDetails) extras.getSerializable("scanResults");
        setResultValues();
    }

    /**
     * Sets text on screen to value of product found and type of UPC code scanned
     */
    private void setResultValues()
    {
        String beerNameToDisplay = this.scanResults.getProductName();
        if(beerNameToDisplay == null || beerNameToDisplay.equals("") || beerNameToDisplay.equals("null"))
            beerNameToDisplay = this.scanResults.getStoredBeerName();
        ((TextView) findViewById(R.id.beerName)).setText(beerNameToDisplay);
        ((TextView) findViewById(R.id.beerStyle)).setText(this.scanResults.getBeerStyle());
        String beerABVText = this.scanResults.getBeerABV();
        if(beerABVText != null && !beerABVText.equals("null") && !beerABVText.equals("Unknown ABV"))
            beerABVText += "%";
        ((TextView) findViewById(R.id.beerABV)).setText(beerABVText);

        TextView communityRating = (TextView) findViewById(R.id.communityRating);
        TextView brothersRating = (TextView) findViewById(R.id.brothersRating);

        communityRating.setText(this.scanResults.getCommunityRating());
        brothersRating.setText(this.scanResults.getBrothersRating());
        communityRating.setTextColor(this.scanResults.calculateColorByRating(scanResults.getCommunityRating()));
        brothersRating.setTextColor(this.scanResults.calculateColorByRating(scanResults.getBrothersRating()));
        
        String text = String.format("%1$s <br/><small><small>w/ %2$s ratings</small></small>", this.scanResults.getCommunityRatingDescription(), this.scanResults.getNumberOfRatings());
        ((TextView) findViewById(R.id.communityRatingName)).setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
        ((TextView) findViewById(R.id.brothersRatingName)).setText(this.scanResults.getBrothersRatingDescription());
    }
    
    /**
     * Event handler for when menu button is clicked on results page
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.results_menu, menu);
        return true;
    }
    
    /**
     * Event handler for when a menu item is selected
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.viewBeerPage:
            goToBeerProfilePage();
            return true;
        case R.id.findOtherSimilarStyles:
            goToBeerStylePage();
            return true;
        case R.id.incorrectBeerScanned:
            incorrectBeerResult();
            return true;
        }
        return false;
    }
    
    /**
     * Handles intent to pull up beer profile page
     */
    private void goToBeerProfilePage()
    {
        String beerProfileUrl = this.scanResults.getProductLink();
        if(!beerProfileUrl.equals("") && beerProfileUrl != null)
        {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(beerProfileUrl));
            startActivity(i);
        }
    }
    
    /**
     * Handles intent to pull up beer style page
     */
    private void goToBeerStylePage()
    {
        String beerStyleID = this.scanResults.getBeerStyleID();
        if(!beerStyleID.equals(""))
        {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("http://www.beeradvocate.com/beer/style/" + beerStyleID));
            startActivity(i);
        }
    }

    /**
     * Handles intent to go to modify beer result page
     */
    private void incorrectBeerResult()
    {
        Intent incorrectData = new Intent();
        incorrectData.putExtra("upcCode", this.scanResults.getUpcCode());
        incorrectData.putExtra("currentBeerName", this.scanResults.getStoredBeerName());
        setResult(Activity.RESULT_OK, incorrectData);
        finish();
    }
}