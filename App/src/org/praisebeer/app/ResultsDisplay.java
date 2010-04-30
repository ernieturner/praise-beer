package org.praisebeer.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ResultsDisplay extends Activity 
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_results);
        Bundle extras = getIntent().getExtras();
        ScanDetails scanDetails = (ScanDetails) extras.getSerializable("scanResults");
        setResultValues(scanDetails);
    }
    
    /**
	 * Sets text on screen to value of product found and type of UPC code scanned
	 */
	private void setResultValues(ScanDetails scanResults)
	{
        ((TextView) findViewById(R.id.beerName)).setText(scanResults.getProduct());
        ((TextView) findViewById(R.id.beerStyle)).setText(scanResults.getBeerStyle());
        ((TextView) findViewById(R.id.beerABV)).setText(scanResults.getBeerABV());

        TextView communityRating = (TextView) findViewById(R.id.communityRating);
        TextView brothersRating = (TextView) findViewById(R.id.brothersRating);
        
        communityRating.setText(scanResults.getCommunityRating());
        brothersRating.setText(scanResults.getBrothersRating());
        communityRating.setTextColor(scanResults.calculateColorByRating(scanResults.getCommunityRating()));
        brothersRating.setTextColor(scanResults.calculateColorByRating(scanResults.getBrothersRating()));
        ((TextView) findViewById(R.id.communityRatingName)).setText(scanResults.getCommunityRatingDescription());
        ((TextView) findViewById(R.id.brothersRatingName)).setText(scanResults.getBrothersRatingDescription());
        ((TextView) findViewById(R.id.communityRatingCount)).setText("w/ " + scanResults.getNumberOfRatings() + " reviews");
        
        //TextView beerOverviewPage = (TextView) findViewById(R.id.viewBeerPage);
        //Linkify.addLinks(beerOverviewPage.getText, pattern, scheme, null, mentionFilter);
        //TextView beerStylePage = (TextView) findViewById(R.id.findOtherSimilarStyles);
	}
}