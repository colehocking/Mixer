package com.example.colehocking.mixer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.colehocking.mixer.visualizer.VisualizerViewAlt;
import static com.example.colehocking.mixer.R.raw.blues_beat;

import java.util.Timer;
import java.util.TimerTask;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.util.PitchConverter;

public class MainActivity extends AppCompatActivity {

    private Intent intent;
    private TextView bpmVal;

    private Thread listenerThread;
    private ComplexOnsetDetector cod;
    private AudioProcessor pitchP;
    private PitchDetectionHandler pdh;

    private final String tag = "MainActivity";
    private String bpmValueStr = "0";

    private final int REC_AUDIO = 11;
    private final int WRITE_EXT = 12;
    private final int READ_EXT = 13;
    private final int WAKE_LOCK = 14;
    private final int FINE_LOC = 15;
    private final int CHANGE_THRESHOLD = 16;
    private final int TAP_TEMPO = 17;

    private MediaPlayer mPlay;
    private VisualizerViewAlt vizAlt;

    private AudioDispatcher dispatcher;

    private double pThreshold = 0.5; //peak threshold dB level
    private double sThreshold = -70; //silence threshold dB level
    private double minOnsIntvl = 0.04; //minimum Inter-Onset interval
    private final int bufferSize = 1024; //512 alt
    private final int sampleRate = 22050; //44100 alt
    private final int overlap = 0;

    public static long RESET_DURATION = 2000;
    BpmCalculator bpmCalculator;
    Timer timer;
    boolean musicPlaying = false;
    Button playBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar tBar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(tBar);
        tBar.setSubtitle("Listening to the Music");

        initFonts(); //Set the desired Fonts

        playBtn = (Button) findViewById(R.id.plyButton);
        vizAlt = (VisualizerViewAlt) findViewById(R.id.vizView);
        //mPlay = new MediaPlayer();
        bpmCalculator = new BpmCalculator();

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!musicPlaying){
                    cueMusic();
                    musicPlaying = true;
                }
                else { cleanUp();
                       musicPlaying = false; }
            }
        });

        micListener();
    }
//--------------------------------------------------------------------------------------
    private void initFonts(){
        Typeface font = Typeface.createFromAsset(getAssets(), "SourceSansPro-Light.ttf");
        bpmVal = (TextView)(findViewById(R.id.songInfo));
        bpmVal.setTypeface(font);
        bpmVal.setText(getString(R.string.songInfo));
    }
//--------------------------------------------------------------------------------------
    @Override
    protected void onPause() {
        if (dispatcher != null){
            dispatcher.stop();
        }
        super.onPause();
    }
//---------------------------------------------------------------------------------------
    @Override
    protected void onRestart() {
        super.onRestart();
        micListener();
    }
//---------------------------------------------------------------------------------------
    public void googleMusicSearch() {
        checkPermissions(REC_AUDIO);
        intent = new Intent("com.google.android.googlequicksearchbox.MUSIC_SEARCH");
        startActivity(intent);
    }

//--------------------------------------------------------------------------------------
    public void micListener() {

        // Onset/Pitch detection code modified from Tarsos DSP. Full source code available @:
        // https://github.com/JorenSix/TarsosDSP
        // Included as .jar in '/app/libs' dir

        //Log.i(tag, "changed sample/buffer size is:");
        checkPermissions(REC_AUDIO);
        checkPermissions(WAKE_LOCK);

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap);
        cod = new ComplexOnsetDetector(bufferSize, pThreshold, minOnsIntvl, sThreshold);
        cod.setHandler(new OnsetHandler() {
            @Override
            public void handleOnset(double v, double v1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //sends an onset to the bpm calculator
                        //Log.i(tag, "there's an onset!");
                        //vizAlt.onsetIsBeat(true);
                        bpmCalculator.recordTime();
                        restartResetTimer();
                        updateView();
                    }
                });
            }
        });
                pdh = new PitchDetectionHandler() {
                    @Override
                    public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                        final double pitchInHz = result.getPitch();
                        final AudioEvent event = e;
                        //fftBytes = e.getByteBuffer();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView text = (TextView) findViewById(R.id.getNote);
                                audioBytes(event);
                                int midi = PitchConverter.hertzToMidiKey(pitchInHz);
                                String noteOct = getKeyOct(midi);
                                if (noteOct != null){
                                    text.setText("Note: " + noteOct);
                                    Typeface font = Typeface.createFromAsset(getAssets(), "SourceSansPro-Light.ttf");
                                    text.setTypeface(font);

                                }
                                //Log.i(tag, "handlePitch is running!");
                            }
                        });
                    }
                };
        pitchP = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                sampleRate, bufferSize, pdh);
        dispatcher.addAudioProcessor(pitchP);
        dispatcher.addAudioProcessor(cod);
        listenerThread = new Thread(dispatcher,"Audio Dispatcher");
        listenerThread.start();
    }
//--------------------------------------------------------------------------------------------
    public void audioBytes(AudioEvent e){
        checkPermissions(REC_AUDIO);
        byte[] audio_bytes = e.getByteBuffer();
        vizAlt.updateVisualizer(audio_bytes);
    }
//--------------------------------------------------------------------------------------------
    private void cueMusic(){
        checkPermissions(REC_AUDIO);
        mPlay = MediaPlayer.create(this, blues_beat);
        mPlay.setLooping(true);
        mPlay.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        mPlay.start();
    }
//--------------------------------------------------------------------------------------------
    private void cleanUp()
    {
        if (mPlay != null) {
            mPlay.release();
            mPlay = null;
        }
    }
//--------------------------------------------------------------------------------------------
   //Block for BPM/timer calculation

    private void updateView(){
        if (bpmCalculator.times.size() >= 2) {
            int bpm = bpmCalculator.getBpm();
            if (bpm < 70){
                bpm = (bpm*2);
            }
            else if (bpm > 140){
                bpm = (bpm/2);
            }
            bpmValueStr = Integer.valueOf(bpm).toString();
        } else {
            //bpmValueStr = getString(R.string.tap_again);
            bpmVal.setText(getString(R.string.tap_again));
        }

        bpmVal.setText(bpmValueStr + " BPM");
    }
    private void restartResetTimer(){
        stopResetTimer();
        startResetTimer();
    }
    private void startResetTimer(){
        timer = new Timer("reset-bpm-calculator", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                bpmCalculator.clearTimes();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //resetBackground();
                    }
                });
            }
        }, RESET_DURATION);
    }
    public void stopResetTimer(){
        if (timer != null){
            timer.cancel();
        }
    }
//--------------------------------------------------------------------------------------------
    public String getKeyOct (int initialNote){
        String[] noteString = new String[] { "C", "C#", "D",
                "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B" };
        int octave = (initialNote / 12) - 1;
        int noteIndex = (initialNote % 12);
        String note = noteString[noteIndex];
        return note;
    }
//--------------------------------------------------------------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case RESULT_OK:
                Log.i(tag, "Result Okay");
                break;
            case CHANGE_THRESHOLD:
                Log.i(tag, "Change threshold okay");
                Bundle bundle = getIntent().getExtras();
                if (bundle != null){
                    pThreshold = bundle.getDouble("pX");
                    sThreshold = bundle.getDouble("sX");
                    minOnsIntvl = bundle.getDouble("mX");
                    if (dispatcher != null){
                        dispatcher.stop();
                        micListener();
                    }
                }
                else{
                    Log.i(tag, "no bundle");
                }
                break;
            case TAP_TEMPO:
                Log.i(tag, "tap tempo okay");
                String tapBPM = data.getStringExtra("BPM");
                Log.i(tag, tapBPM + " BPM");
                break;
            default:
                Log.i(tag, "Request Code: " + requestCode);
                Log.i(tag, "Result Code: " + resultCode);
                break;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();
        switch (id){
            case R.id.action_settings: {
                Intent changeT = new Intent(MainActivity.this, Threshold.class);
                MainActivity.this.startActivityForResult(changeT, CHANGE_THRESHOLD);
                return true;
            }
            case R.id.tap_tempo: {
                Intent tapT = new Intent(MainActivity.this, TapTempo.class);
                MainActivity.this.startActivityForResult(tapT, TAP_TEMPO);
                return true;
            }
            case R.id.googleSong: {
                googleMusicSearch();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

//--------------------------------------------------------------------------------------------
    public void checkPermissions(int reqCode) {
        //Rec Audio Permissions
        switch (reqCode) {
            case REC_AUDIO: {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new
                            String[]{Manifest.permission.RECORD_AUDIO}, REC_AUDIO);
                }
                break;
            }
            // Write External Permissions
            case WRITE_EXT: {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new
                            String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXT);
                }
                break;
            }
            //Read External Permissions
            case READ_EXT: {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new
                            String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXT);
                }
                break;
            }
            //Wake Lock Permissions
            case WAKE_LOCK: {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WAKE_LOCK) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new
                            String[]{Manifest.permission.WAKE_LOCK}, WAKE_LOCK);
                }
                break;
            }
            case FINE_LOC: {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new
                            String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOC);
                }
                break;
            }
            default: {
                Log.i(tag, "Invalid Req Code: " + reqCode);
            }
            break;

        }
    }
//---------------------------------------------------------------------------------------
    //Handle Results for checkPermissions Method
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            switch (requestCode) {
                case REC_AUDIO: {
                    if (grantResults.length > 0 && grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {
                        Log.i(tag, "rec permission granted");
                    } else {
                        Log.i(tag, "rec permission denied");
                    }
                    break;
                }
                case WRITE_EXT: {
                    if (grantResults.length > 0 && grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {
                        Log.i(tag, "write permission granted");
                    } else {
                        Log.i(tag, "write permission denied");
                    }
                    break;
                }
                case READ_EXT: {
                    if (grantResults.length > 0 && grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {
                        Log.i(tag, "read permission granted");
                    } else {
                        Log.i(tag, "read permission denied");
                    }
                    break;
                }
                case WAKE_LOCK: {
                    if (grantResults.length > 0 && grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {
                        Log.i(tag, "wake permission granted");
                    } else {
                        Log.i(tag, "wake permission denied");
                    }
                    break;
                }
                case FINE_LOC: {
                    if (grantResults.length > 0 && grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {
                        Log.i(tag, "Fine Location permission granted");
                    } else {
                        Log.i(tag, "Fine Location permission denied");
                    }
                    break;
                }
            }
        }
//---------------------------------------------------------------------------------------

}
