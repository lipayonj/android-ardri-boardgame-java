package com.thesis.ArdRi;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Created by jerwinlipayon on 3/18/15.
 */
public class About extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.how_to_play);
        WebView wv = (WebView) findViewById(R.id.webView);
        wv.loadUrl("file:///android_asset/html/About.html");
    }
}