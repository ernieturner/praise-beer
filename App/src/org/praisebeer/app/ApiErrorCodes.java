package org.praisebeer.app;

public class ApiErrorCodes
{
    public static final int NO_BARCODE_RECIEVED = -3;
    public static final int OUTGOING_REQUEST_FAILURE = -2;
    public static final int INVALID_JSON_RESPONSE = -1;
    public static final int INVALID_UPC_CODE = 0;
    public static final int NO_UPC_CODE_SENT = 1;
    public static final int NO_UPC_FOUND = 2;
    public static final int NO_BEER_FOUND = 3;
    public static final int RATING_LOOKUP_TIMEOUT = 4;
    public static final int NAME_LOOKUP_NO_DESCRIPTION_PROVIDED = 5;
}
