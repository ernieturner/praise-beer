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
    private ScanDetails scanResults;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_results);
        Bundle extras = getIntent().getExtras();
        scanResults = (ScanDetails) extras.getSerializable("scanResults");
        setResultValues();

        Button goToBeerProfilePage = (Button) findViewById(R.id.viewBeerPage);
        Button goToBeerStylePage = (Button) findViewById(R.id.findOtherSimilarStyles);

        goToBeerProfilePage.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v)
            {
                String beerProfileUrl = ResultsDisplay.this.scanResults.getProductLink();
                if(beerProfileUrl != "" && beerProfileUrl != null)
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
                if(beerStyleID != "")
                {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("http://www.beeradvocate.com/beer/style/" + beerStyleID));
                    startActivity(i);
                }
            }
        });
    }

    /**
     * Sets text on screen to value of product found and type of UPC code scanned
     */
    private void setResultValues()
    {
        ((TextView) findViewById(R.id.beerName)).setText(this.scanResults.getProduct());
        ((TextView) findViewById(R.id.beerStyle)).setText(this.scanResults.getBeerStyle());
        ((TextView) findViewById(R.id.beerABV)).setText(this.scanResults.getBeerABV() + "%");

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