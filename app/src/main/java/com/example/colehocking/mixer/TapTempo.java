package com.example.colehocking.mixer;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class TapTempo extends AppCompatActivity {
    private int TAP_TEMPO = 17; //Intent Request Code
    private final String tag = "TapTempo";
    private String displayValue = "0"; //Display BPM
    Timer timer;
    BpmCalculator bpmCalculator;
    Vibrator vibes;
    TextView bpmTextView;
    Button tapButton;
    Button gotIt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tap_tempo);

        checkVibPermissions();
        vibes = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        bpmCalculator = new BpmCalculator();
        tapButton = (Button) (findViewById(R.id.tapButton));
        gotIt = (Button)(findViewById(R.id.gotIt));
        Typeface font = Typeface.createFromAsset(getAssets(),
                "SourceSansPro-Light.ttf");
        bpmTextView = (TextView) (findViewById(R.id.bpmTextView));
        bpmTextView.setTypeface(font);

        Toolbar tBar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(tBar);
        tBar.setSubtitle("Tap Tempo BPM Counter");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        tapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate();
                bpmCalculator.recordTime();
                restartResetTimer();
                updateView();
            }
        });

        gotIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mIntent = new Intent();
                mIntent.putExtra("BPM", displayValue);
                setResult(TAP_TEMPO, mIntent);
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

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        bpmCalculator.clearTimes();
        super.onDestroy();
    }

    private void updateView() {
        if (bpmCalculator.times.size() >= 2) {
            int bpm = bpmCalculator.getBpm();
            if (bpm < 70){
                bpm = (bpm*2);
            }
            else if (bpm > 140){
                bpm = (bpm/2);
            }
            displayValue = Integer.valueOf(bpm).toString();
        } else {
            bpmTextView.setText(getString(R.string.tap_again));
        }
        bpmTextView.setText(displayValue + " BPM");
    }

    private void restartResetTimer() {
        stopResetTimer();
        startResetTimer();
    }

    private void startResetTimer() {
        int RESET_DURATION = 2000;
        timer = new Timer("reset-bpm-calculator", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                bpmCalculator.clearTimes();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        }, RESET_DURATION);
    }

    private void stopResetTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void vibrate() {
        vibes.vibrate(50);
    }

    private void checkVibPermissions(){
        int VIB = 11;
        if (ContextCompat.checkSelfPermission(TapTempo.this,
                Manifest.permission.VIBRATE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TapTempo.this, new
                    String[]{Manifest.permission.VIBRATE}, VIB);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {
            Log.i(tag, "vib permission granted");
        } else {
            Log.i(tag, "vib permission denied");
        }
    }
}

