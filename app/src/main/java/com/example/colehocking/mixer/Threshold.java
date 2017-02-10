package com.example.colehocking.mixer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


public class Threshold extends AppCompatActivity {
    public static final String MyPREFERENCES = "MyPrefs";
    public static final String tag = "Threshold";
    public static final String pkStr = "peakKey";
    public static final String silStr = "silenceKey";
    public static final String mIOIStr = "mIOIKey";
    SharedPreferences sharedPrefs;

    private int CHANGE_THRESHOLD = 16;
    private double pkThreshold = 0.5; //peak threshold dB level 0.0 - 1.0
    private double slThreshold = -70; //silence threshold dB level
    private double minOnsIntvl = 0.004; //minimum Inter-Onset interval

    // peakThreshold: A threshold used for peak picking.
    // Values between 0.1 and 0.8. Default is 0.3, if too many onsets
    // are detected adjust to 0.4 or 0.5
    TextView peakThreshold;
    SeekBar peak;

    //silenceThreshold The threshold that defines when a buffer is silent.
    // Default is -70dBSPL. -90 is also used.
    TextView sThreshold;
    SeekBar silenceT;

    //minimumInterOnsetInterval The minimum inter-onset-interval in seconds.
    // When two onsets are detected within this interval the last one does not count.
    // Default is 0.004 seconds.
    TextView minIOI;
    SeekBar mIOI;

    Button defaultB; // restore sliders to their default values
    Button doneB; //return with new values

    private int pkInt;
    private int slInt;
    private int mInt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.threshold);

        Toolbar tBar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(tBar);
        tBar.setSubtitle("Adjust Listening Threshold");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        peakThreshold = (TextView) findViewById(R.id.textView);
        sThreshold = (TextView) findViewById(R.id.textView2);
        minIOI = (TextView) findViewById(R.id.textView3);

        peak = (SeekBar) findViewById(R.id.seekBar);
        peak.setMax(80);
        silenceT = (SeekBar) findViewById(R.id.seekBar2);
        silenceT.setMax(140);
        mIOI = (SeekBar)findViewById(R.id.seekBar3);
        mIOI.setMax(150);

        sharedPrefs = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        pkInt = sharedPrefs.getInt(pkStr, 30);
        slInt = sharedPrefs.getInt(silStr, 70);
        mInt = sharedPrefs.getInt(mIOIStr, 40);

        Log.i(tag, "Preferences values: " + pkInt + ", " + slInt + ", " + mInt);

        peak.setProgress(pkInt);
        silenceT.setProgress(slInt);
        mIOI.setProgress(mInt);

        peak.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pkThreshold = (double) progress/100.0;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        silenceT.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                slThreshold = (double) -progress;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mIOI.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minOnsIntvl = (double) progress/10000.0;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        defaultB = (Button) findViewById(R.id.defaults);
        doneB = (Button) findViewById(R.id.doneBtn);

        defaultB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pkInt = 30;
                slInt = 70;
                mInt = 40;
                peak.setProgress(pkInt);
                silenceT.setProgress(slInt);
                mIOI.setProgress(mInt);
                pkThreshold = 0.3; //peak threshold dB level 0.0 - 1.0
                slThreshold = -70; //silence threshold dB level
                minOnsIntvl = 0.04; //minimum onset interval
            }
        });

        doneB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pkInt = peak.getProgress();
                slInt = silenceT.getProgress();
                mInt = mIOI.getProgress();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putInt(pkStr, pkInt);
                editor.putInt(silStr, slInt);
                editor.putInt(mIOIStr, mInt);
                editor.apply();
                Toast.makeText(Threshold.this,"Saved Settings!", Toast.LENGTH_LONG).show();
                Intent mIntent = new Intent();
                mIntent.putExtra("tX", pkThreshold);
                mIntent.putExtra("sX", slThreshold);
                mIntent.putExtra("mX", minOnsIntvl);
                setResult(CHANGE_THRESHOLD, mIntent);
                finish();
            }
        });
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.home) {

            finish();
        }
        return super.onOptionsItemSelected(item);

    }

}
