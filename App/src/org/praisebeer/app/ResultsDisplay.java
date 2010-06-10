package org.praisebeer.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

        Button goToBeerProfilePage = (Button) findViewById(R.id.viewBeerPage);
        Button goToBeerStylePage = (Button) findViewById(R.id.findOtherSimilarStyles);
        Button incorrectBeerScanned = (Button) findViewById(R.id.incorrectBeerScanned);

        goToBeerProfilePage.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
                String beerProfileUrl = ResultsDisplay.this.scanResults.getProductLink();
                if(!beerProfileUrl.equals("") && beerProfileUrl != null)
                {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(beerProfileUrl));
                    startActivity(i);
                }
            }
        });

        goToBeerStylePage.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
                String beerStyleID = ResultsDisplay.this.scanResults.getBeerStyleID();
                if(!beerStyleID.equals(""))
                {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("http://www.beeradvocate.com/beer/style/" + beerStyleID));
                    startActivity(i);
                }
            }
        });
        
        incorrectBeerScanned.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
                Intent incorrectData = new Intent();
                incorrectData.putExtra("upcCode", ResultsDisplay.this.scanResults.getUpcCode());
                incorrectData.putExtra("currentBeerName", ResultsDisplay.this.scanResults.getStoredBeerName());
                setResult(Activity.RESULT_OK, incorrectData);
                finish();
            }
        });
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
        ((TextView) findViewById(R.id.communityRatingName)).setText(this.scanResults.getCommunityRatingDescription());
        ((TextView) findViewById(R.id.brothersRatingName)).setText(this.scanResults.getBrothersRatingDescription());
        ((TextView) findViewById(R.id.communityRatingCount)).setText("w/ " + this.scanResults.getNumberOfRatings() + " reviews");
    }
}