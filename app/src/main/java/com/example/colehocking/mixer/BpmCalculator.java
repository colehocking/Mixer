package com.example.colehocking.mixer;


//Class BpmCalculator from:
//https://github.com/javonharper/Tempo/blob/master/src/com/javonharper/tempo/BpmCalculator.java
//Modified methods in MainActivity to use class to determine BPM with Onset Events


import java.util.ArrayList;


public class BpmCalculator {
    private static final Long MILLISECONDS_IN_A_MINUTE = 60000L;
    public ArrayList<Long> times;
    private boolean isRecording;

    public BpmCalculator() {
        times = new ArrayList<>();
        isRecording = false;
    }

    public void recordTime() {
        Long time = System.currentTimeMillis();
        times.add(time);
        isRecording = true;
    }

    public int getBpm() {
        ArrayList<Long> deltas = getDeltas();
        return calculateBpm(deltas);
    }

    public void clearTimes() {
        times.clear();
        isRecording = false;
    }

    private ArrayList<Long> getDeltas() {
        ArrayList<Long> deltas = new ArrayList<Long>();

        for (int i = 0; i < times.size() - 1; i++) {
            Long delta = times.get(i + 1) - times.get(i);
            deltas.add(delta);
        }

        return deltas;
    }

    private int calculateBpm(ArrayList<Long> deltas) {
        Long sum = 0L;
        for (Long delta : deltas) {
            sum = sum + delta;
        }
        Long average = sum / deltas.size();

        return (int) (MILLISECONDS_IN_A_MINUTE / average);
    }

    public boolean isRecording() {
        return isRecording;
    }
}

