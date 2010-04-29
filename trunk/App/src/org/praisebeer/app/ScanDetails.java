package org.praisebeer.app;

import java.util.Vector;

public class ScanDetails implements java.io.Serializable
{
	private static final long serialVersionUID = -5208851430378051565L;
	/**Properties*/
	private String upcCode;
	private String upcFormat;
	private String productName;
	private Vector<String> productLinks;
	
	/**Error conditions*/
	public static int NO_UPC_RESULTS_FOUND = 1;
	public static int NO_SEARCH_RESULTS_FOUND = 2;
	public static int SCRAPE_TIMEOUT = 3;
	
	ScanDetails(String upcCode, String upcFormat)
	{
		//Transform UPC code from UPC-E or UPC-A to EAN
		int upcLength = upcCode.length();
		if(upcLength == 12)
			upcCode = "0" + upcCode;
		else if(upcLength == 8 || upcLength == 6)
			upcCode = convertUPCEToEAN(upcCode);
		
		this.upcCode = upcCode;
		this.upcFormat = upcFormat;
	}
	
	ScanDetails(String upcCode, String upcFormat, String productName, Vector<String> productLinks)
	{
		//Transform UPC code from UPC-E or UPC-A to EAN
		int upcLength = upcCode.length();
		if(upcLength == 12)
			upcCode = "0" + upcCode;
		else if(upcLength == 8 || upcLength == 6)
			upcCode = convertUPCEToEAN(upcCode);
		
		this.upcCode = upcCode;
		this.upcFormat = upcFormat;
		this.productName = productName;
		this.productLinks = productLinks;
	}
	
	/**
	 * Sets links found based on product name
	 * @param links
	 */
	public void setProductLinks(Vector<String> links)
	{
		productLinks = links;
	}
	
	/**
	 * Sets product name
	 * @param name
	 */
	public void setProductName(String name)
	{
		productName = name;
	}
	
	/**
	 * Returns product name
	 * @return
	 */
	public String getProduct()
	{
		return productName;
	}
	
	/**
	 * Returns the UPC code set
	 * @return
	 */
	public String getUpcCode()
	{
		return upcCode;
	}
	
	/**
	 * Returns the initial UPC code scanned
	 * @return
	 */
	public String getUpcFormat()
	{
		return upcFormat;
	}
	
	/**
	 * Returns all product links found
	 * @return
	 */
	public Vector<String> getProductLinks()
	{
		return productLinks;
	}
	
	/**
	 * Returns the first, i.e. most relavent, product link found
	 * @return
	 */
	public String getProductLink()
	{
		return productLinks.firstElement();
	}
	
	/**
	 * Converts either an 8 digit or 6 digic UPC-E code into a
	 * UPC-13 code
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

	   //Reverse array
	   int left  = 0;
	   int right = chars.length-1;
	   while (left < right) 
	   {
		   char temp = chars[left]; 
		   chars[left]  = chars[right]; 
		   chars[right] = temp;

		   left++;
		   right--;
	   }
	   
	   for(int i=0; i<chars.length; i++)
	   {
		   if ((i % 2)!= 0)
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

	   //return check.
	   return Integer.toString(check);
	}
}