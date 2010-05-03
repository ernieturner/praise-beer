package org.praisebeer.app;

import android.app.Activity;
import android.os.Bundle;

public class ManualUserEntry extends Activity
{
    //private ScanDetails scanResults;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manual_entry);
        //Bundle extras = getIntent().getExtras();
        //ScanDetails scanResults = (ScanDetails) extras.getSerializable("scanResults");
    }
}