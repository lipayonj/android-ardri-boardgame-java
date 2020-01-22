package com.thesis.ArdRi;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class MyGameActivity extends FragmentActivity{

    /**
     * Called when the activity is first created.
     */

    private boolean multiplayerPanelDisp = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.main);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/a.ttf");
        ((TextView)findViewById(R.id.logo)).setTypeface(font);
    }

    @Override
    public void onBackPressed() {
        if(multiplayerPanelDisp) {
            getSupportFragmentManager().popBackStack();
            multiplayerPanelDisp = false;
        }
        else {
            finish();
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        android.os.Process.killProcess(android.os.Process.myPid());
        super.onDestroy();
    }

    public void clickPassPlay(View view){
        Log.d("Menu"," Pass and Play ");
        Intent ardriBluetoothIntent = new Intent(this, ArdRiPractice.class);
        startActivity(ardriBluetoothIntent);
    }

    public void clickMultiplayer(View view){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        MultiplayerFragment fragment = new MultiplayerFragment();
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
        multiplayerPanelDisp = true;
    }

    public void clickHowToPlay(View view){
        Log.d("Menu"," How To Play ");
        Intent ardriBluetoothIntent = new Intent(this, HowToPlay.class);
        startActivity(ardriBluetoothIntent);
    }

    public void clickAbout(View view){
        Log.d("Menu"," How To Play ");
        Intent ardriBluetoothIntent = new Intent(this, About.class);
        startActivity(ardriBluetoothIntent);
    }

    public void exitGame(View view){
        Log.d("Menu"," How To Play ");
        finish();
    }
}
