package org.praisebeer.app;

import java.util.Vector;

import android.graphics.Color;

public class BeerDetails implements java.io.Serializable
{
    private static final long serialVersionUID = -5208851430378051565L;

    /* UPC Scanned */
    private String upcCode;

    /* Rating Details */
    private String productName;
    private String storedBeerName;
    private String communityRating;
    private String brothersRating = "N/A";
    private String numberOfRatings;
    private String communityRatingDescription;
    private String brothersRatingDescription = "No Reviews";
    private String beerABV = "Unknown ABV";
    private String beerStyle = "Unknown Beer Type";
    private String beerStyleID = "";
    private String beerBreweryID = "";
    private String beerBreweryName = "";
    private Vector<String> productLinks;

    /** Result Errors and Success */
    private int resultErrorCode;
    private String resultErrorMessage = "";
    private boolean scanSuccess;

    BeerDetails(String upcCode) {
        // Transform UPC code from UPC-E or UPC-A to EAN
        int upcLength = upcCode.length();
        if(upcLength == 12)
            upcCode = "0" + upcCode;
        else if(upcLength == 8 || upcLength == 6)
            upcCode = convertUPCEToEAN(upcCode);

        this.upcCode = upcCode;
    }

    /*
     * Utility method to convert rating into color code that will be used tod display on the ratings page.
     */
    public int calculateColorByRating(String rating)
    {
        String ratingCore = rating.substring(0, 1);

        if(ratingCore.equals("A"))
            return Color.rgb(0x19, 0xe1, 0x1d);
        if(ratingCore.equals("B"))
            return Color.rgb(0xb2, 0xda, 0x14);
        if(ratingCore.equals("C"))
            return Color.rgb(0xe7, 0xef, 0x0c);
        if(ratingCore.equals("D"))
            return Color.rgb(0xf7, 0xb1, 0x2b);
        if(ratingCore.equals("F"))
            return Color.rgb(0xf5, 0x0e, 0x0e);
        return Color.rgb(0x0, 0x0, 0x0);
    }

    /**
     * Get overall community rating
     */
    public String getCommunityRating()
    {
        return this.communityRating;
    }

    /**
     * Set overall community rating
     */
    public void setCommunityRating(String rating)
    {
        this.communityRating = rating;
    }

    /**
     * Get brothers rating
     */
    public String getBrothersRating()
    {
        return this.brothersRating;
    }

    /**
     * Set brothers rating
     */
    public void setBrothersRating(String rating)
    {
        if(rating != null && !rating.equals("null"))
        this.brothersRating = rating;
    }

    /**
     * Returns ID of brewery
     */
    public String getBreweryID()
    {
        return this.beerBreweryID;
    }

    /**
     * Set ID of brewery
     */
    public void setBreweryID(String id)
    {
        this.beerBreweryID = id;
    }
    
    /**
     * Returns name of brewery
     */
    public String getBreweryName()
    {
        return this.beerBreweryName;
    }

    /**
     * Set name of brewery
     */
    public void setBreweryName(String name)
    {
        this.beerBreweryName = name;
    }
    
    /**
     * Returns number of ratings for beer
     */
    public String getNumberOfRatings()
    {
        return this.numberOfRatings;
    }

    /**
     * Set number of overall community ratings
     */
    public void setNumberOfRatings(String ratingCount)
    {
        this.numberOfRatings = ratingCount;
    }

    /**
     * Get community description of rating
     */
    public String getCommunityRatingDescription()
    {
        return this.communityRatingDescription;
    }
    
    /**
     * Set community description of rating
     */
    public void setCommunityRatingDescription(String description)
    {
        String firstLetter = description.substring(0, 1);
        this.communityRatingDescription = firstLetter.toUpperCase() + description.substring(1);
    }
    
    /**
     * Get beer style
     */
    public String getBeerStyle()
    {
        return this.beerStyle;
    }
    
    /**
     * Set beer style
     */
    public void setBeerStyle(String style)
    {
        if(style != null && !style.equals("null"))
            this.beerStyle = style;
    }
    
    /**
     * Get beer style ID
     */
    public String getBeerStyleID()
    {
        return this.beerStyleID;
    }
    
    /**
     * Set beer style ID to build up style URL later
     */
    public void setBeerStyleID(String styleID)
    {
        this.beerStyleID = styleID;
    }

    /**
     * Get beer alcohol by volume
     */
    public String getBeerABV()
    {
        return this.beerABV;
    }

    /**
     * Set beer alcohol by volume
     */
    public void setBeerABV(String ABV)
    {
        if(ABV != null && !ABV.equals("null"))
            this.beerABV = ABV;
    }

    /**
     * Get brothers description of rating
     */
    public String getBrothersRatingDescription()
    {
        return this.brothersRatingDescription;
    }
    
    /**
     * Set brothers description of rating
     */
    public void setBrothersRatingDescription(String description)
    {
        if(!description.equals("no reviews; yet!"))
        {
            String firstLetter = description.substring(0, 1);
            this.brothersRatingDescription = firstLetter.toUpperCase() + description.substring(1);
        }
    }

    /**
     * Returns product name (i.e. scanned value on BA)
     */
    public String getProductName()
    {
        return productName;
    }
    
    /**
     * Sets product name (i.e. scanned value on BA)
     */
    public void setProductName(String name)
    {
        if(name != null && !name.equals("null"))
            productName = name;
    }
    
    /**
     * Returns stored beer name (i.e. what's in the DB)
     */
    public String getStoredBeerName()
    {
        return storedBeerName;
    }
    
    /**
     * Sets stored beer name (i.e. what's in the DB)
     */
    public void setStoredBeerName(String name)
    {
        if(name != null && !name.equals("null"))
            storedBeerName = name;
    }

    /**
     * Returns the UPC code set
     */
    public String getUpcCode()
    {
        return upcCode;
    }

    /**
     * Returns all product links found
     */
    public Vector<String> getProductLinks()
    {
        return productLinks;
    }
    
    /**
     * Sets links found based on product name
     */
    public void setProductLinks(Vector<String> links)
    {
        productLinks = links;
    }

    /**
     * Returns the first, i.e. most relavent, product link found
     */
    public String getProductLink()
    {
        return productLinks.firstElement();
    }

    /**
     * Returns error code set in results
     */
    public int getResultErrorCode()
    {
        return this.resultErrorCode;
    }
    
    /**
     * Sets error code returned from API
     */
    public void setResultErrorCode(int error)
    {
        this.resultErrorCode = error;
    }
    
    /**
     * Returns error message set during request
     */
    public String getResultErrorMessage()
    {
        return this.resultErrorMessage;
    }
    
    /**
     * Sets error message to display
     */
    public void setResultErrorMessage(String errorMessage)
    {
        this.resultErrorMessage = errorMessage;
    }

    /**
     * Returns whether or not the scan results were succcessful, i.e. a beer UPC/rating was found
     */
    public boolean getScanSuccess()
    {
        return this.scanSuccess;
    }
    
    /**
     * Sets whether scan was successful or not
     */
    public void setScanSuccess(boolean result)
    {
        this.scanSuccess = result;
    }

    /**
     * Converts either an 8 digit or 6 digic UPC-E code into a UPC-13 code
     * 
     * @param UPCe
     * @return
     */
    private String convertUPCEToEAN(String UPCe)
    {
        if(UPCe.length() == 8)
        {
            UPCe = UPCe.substring(1, 7);
        }
        else if(UPCe.length() != 6)
        {
            return "";
        }

        String mfg = "";
        String prod = "";

        String upcDigit = UPCe.substring(5, 6);
        if(upcDigit.equals("0"))
        {
            mfg = "0" + UPCe.substring(0, 2) + "000";
            prod = "00" + UPCe.substring(2, 5);
        }
        else if(upcDigit.equals("1"))
        {
            mfg = "0" + UPCe.substring(0, 2) + "100";
            prod = "00" + UPCe.substring(2, 5);
        }
        else if(upcDigit.equals("2"))
        {
            mfg = "0" + UPCe.substring(0, 2) + "200";
            prod = "00" + UPCe.substring(2, 5);
        }
        else if(upcDigit.equals("3"))
        {
            mfg = "0" + UPCe.substring(0, 3) + "00";
            prod = "000" + UPCe.substring(3, 5);
        }
        else if(upcDigit.equals("4"))
        {
            mfg = "0" + UPCe.substring(0, 4) + "0";
            prod = "0000" + UPCe.substring(4, 5);
        }
        else
        {
            mfg = "0" + UPCe.substring(0, 5);
            prod = "0000" + UPCe.substring(5, 6);
        }
        return "0" + mfg + prod + calculateCheckDigit(mfg + prod);
    }

    /**
     * Run check digit algorithm to convert UPC-E to UPC-A
     */
    private String calculateCheckDigit(String upc)
    {
        int check = 0;
        char[] chars = upc.toCharArray();

        // Reverse array
        int left = 0;
        int right = chars.length - 1;
        while(left < right)
        {
            char temp = chars[left];
            chars[left] = chars[right];
            chars[right] = temp;

            left++;
            right--;
        }

        for(int i = 0; i < chars.length; i++)
        {
            if((i % 2) != 0)
            {
                check += Character.digit(chars[i], 10);
            }
            else
            {
                check += (3 * Character.digit(chars[i], 10));
            }
        }

        check = check % 10;
        if(check != 0)
        {
            check = 10 - check;
        }

        // return check.
        return Integer.toString(check);
    }
}