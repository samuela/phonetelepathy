package com.grahamsam.audiomodulator;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AutomaticGainControl;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import static android.media.AudioTrack.*;

public class MainActivity extends AppCompatActivity {
    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLES_PER_BLOCK = 500;
//    private static final int PREAMBLE_SAMPLE_GAP = 0;
//    private static final int START_GAP = 500;
//    private static final int NUM_PREAMBLES = 1;
    private static final int FREQUENCY_0 = 2000;
    private static final int FREQUENCY_1 = 5000;
    private static final int MAX_RECV_SAMPLES = SAMPLE_RATE * 30;

    private static final boolean[] BARKER13 = {true, true, true, true, true, false, false, true, true, false, true, false, true};

    private AudioRecord recorder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private void writeQAM(short[] buffer, int start, int length, float i, float q, float f0) {
        double norm = Math.sqrt(i * i + q * q);
        double poop = ((double) length) / 2;
        for (int j = 0; j < length; j++) {
            double x = 2 * Math.PI * f0 * j / SAMPLE_RATE;
            double v = i * Math.cos(x) - q * Math.sin(x);
            double window = (1 - Math.abs(j - poop) / poop);
            buffer[start + j] = (short) (v * window / norm * Short.MAX_VALUE);
        }
    }

    private void barker13Preamble(short[] buffer, int start) {
//        for (int i = 0; i < BARKER13.length; i++) {
//            if (BARKER13[i]) {
//                writeQAM(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, 1, 1, CARRIER_FREQUENCY);
//            } else {
//                writeQAM(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, -1, -1, CARRIER_FREQUENCY);
//            }
//        }

        for (int i = 0; i < BARKER13.length; i++) {
            if (BARKER13[i]) {
                writeQAM(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, 1, 0, FREQUENCY_1);
            } else {
                writeQAM(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, 1, 0, FREQUENCY_0);
            }
        }
    }

//    private void QAM4(short[] buffer, int start, byte[] msg, int blockLength) {
//        int ix = start;
//        for (int i = 0; i < msg.length; i++) {
//            byte b = msg[i];
//
//            int b1 = (b & 0b00000011);
//            int b2 = (b & 0b00001100) >>> 2;
//            int b3 = (b & 0b00110000) >>> 4;
//            int b4 = (b & 0b11000000) >>> 6;
//
//            byte imask = 0b00000010;
//            int i1 = ((b1 & imask) >>> 1) * 2 - 1;
//            int i2 = ((b2 & imask) >>> 1) * 2 - 1;
//            int i3 = ((b3 & imask) >>> 1) * 2 - 1;
//            int i4 = ((b4 & imask) >>> 1) * 2 - 1;
//
//            byte qmask = 0b00000001;
//            int q1 = (b1 & qmask) * 2 - 1;
//            int q2 = (b2 & qmask) * 2 - 1;
//            int q3 = (b3 & qmask) * 2 - 1;
//            int q4 = (b4 & qmask) * 2 - 1;
//
//            writeQAM(buffer, ix, blockLength, i1, q1, CARRIER_FREQUENCY);
//            ix += blockLength;
//            writeQAM(buffer, ix, blockLength, i2, q2, CARRIER_FREQUENCY);
//            ix += blockLength;
//            writeQAM(buffer, ix, blockLength, i3, q3, CARRIER_FREQUENCY);
//            ix += blockLength;
//            writeQAM(buffer, ix, blockLength, i4, q4, CARRIER_FREQUENCY);
//            ix += blockLength;
//        }
//    }

    private short[] constructSignal(byte[] msg) {
//        int numMsgSamples = msg.length * 8 / 2 * SAMPLES_PER_BLOCK;
//        short[] buffer = new short[(BARKER13.length * SAMPLES_PER_BLOCK + PREAMBLE_SAMPLE_GAP) * NUM_PREAMBLES + numMsgSamples];
//        for (int i = 0; i < NUM_PREAMBLES; i++) {
//            // Leave the next PREAMBLE_SAMPLE_GAP samples to just be zero.
//            barker13Preamble(buffer, (BARKER13.length * SAMPLES_PER_BLOCK + PREAMBLE_SAMPLE_GAP) * i);
//        }

        short[] buffer = new short[BARKER13.length * SAMPLES_PER_BLOCK];
//        writeQAM(buffer, 0, 0, 1, 0, 1500);
        barker13Preamble(buffer, 0);

//        QAM4(buffer, BARKER13.length * SAMPLES_PER_BLOCK + PREAMBLE_SAMPLE_GAP, msg, SAMPLES_PER_BLOCK);
        return buffer;
    }

    public void onClickSend(final View view) {
        EditText editText = findViewById(R.id.editText2);
        String message = editText.getText().toString();
        byte[] messageBytes = message.getBytes();

//        System.out.println(message);
//        short[] audioSignal = QAM4(messageBytes, SAMPLES_PER_BLOCK);

//        short[] audioSignal = constructSignal(messageBytes);
        short[] audioSignal = constructSignal(new byte[] {});

//        short[] audioSignal = new short[SAMPLE_RATE];
//        writeQAM(audioSignal, 0, 11025, 1, 1, 500);
//        writeQAM(audioSignal, 11025, 11025, -1, 1, 500);
//        writeQAM(audioSignal, 22050, 11025, -1, -1, 500);
//        writeQAM(audioSignal, 33075, 11025, 1, -1, 500);

//        short[] audioSignal = new short[44100];
//        for (int i = 0; i < 100; i++) {
//            writeQAM(audioSignal, 441 * i, 441, 1, 0, 120 * i + 500);
//        }
//        for (int i = 0; i < 8; i++) {
//            writeQAM(audioSignal, 5000 * i, 5000, 1, 0, 1000 * i + 1000);
//        }
//        writeQAM(audioSignal, 0, 11025, 1, 0, 500);
//        writeQAM(audioSignal, 11025, 11025, 1, 0, 5000);
//        writeQAM(audioSignal, 22050, 11025, 1, 0, 10000);
//        writeQAM(audioSignal, 33075, 11025, 1, 0, 15000);

//        try {
//            File file = File.createTempFile("sent", "stuff");
//            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file));
//            float[] floatSignal = shortsToFloats(audioSignal);
//            for (int i = 0; i < floatSignal.length; i++) {
//                osw.write(Float.toString(floatSignal[i]));
//                osw.write("\n");
//            }
//            osw.flush();
//            file.setReadable(true, false);
//            System.out.println(file.getAbsolutePath());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        short[] audioSignal = new short[SAMPLE_RATE];
//        writeQAM(audioSignal, 0, SAMPLE_RATE, 1, 1, 500);

        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        final AudioTrack track = new AudioTrack(
//                AudioManager.STREAM_SYSTEM,
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                channelConfig,
                audioFormat,
                audioSignal.length * 2,
                MODE_STATIC);

        track.setStereoVolume(getMaxVolume(), getMaxVolume());
//        track.play();
        track.write(audioSignal, 0, audioSignal.length);
        track.play();

        track.setNotificationMarkerPosition(audioSignal.length);
        track.setPlaybackPositionUpdateListener(
                new OnPlaybackPositionUpdateListener() {
                    @Override
                    public void onMarkerReached(AudioTrack audioTrack) {
                        System.out.println("finished playing!");
                        track.stop();
                        track.flush();
                        track.release();
                    }

                    @Override
                    public void onPeriodicNotification(AudioTrack audioTrack) {

                    }
                }
        );



//        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
//        alertDialog.setTitle("Alert");
//        alertDialog.setMessage("playing tone");
//        alertDialog.show();

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }).run();
//
//
//        switch (track.getPlayState()) {
//            case PLAYSTATE_PAUSED:
//                track.stop();
//                track.reloadStaticData();
//                track.play();
//                break;
//            case PLAYSTATE_PLAYING:
//                track.stop();
//                track.reloadStaticData();
//                track.play();
//                break;
//            case PLAYSTATE_STOPPED:
//                track.reloadStaticData();
//                track.play();
//                break;
//        }
    }

    public void onClickReceive(View view) {
        int numSamples = SAMPLE_RATE * 5;
        this.recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                2 * numSamples);

//        int sessionId = this.recorder.getAudioSessionId();
//        AutomaticGainControl agc = AutomaticGainControl.create(sessionId);
//        agc.setEnabled(false);

        this.recorder.startRecording();
    }

    private float[] shortsToFloats(short[] arr) {
        float[] ret = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = ((float) arr[i]) / Short.MAX_VALUE;
        }
        return ret;
    }

    public void onClickFinish(View view) {
        short[] signal = new short[SAMPLE_RATE * 5];
        this.recorder.read(signal, 0, signal.length);
        this.recorder.stop();
        this.recorder.release();

        float[] floatSignal = shortsToFloats(signal);

        try {
            File file = File.createTempFile("received", "stuff");
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file));
            for (int i = 0; i < floatSignal.length; i++) {
                osw.write(Float.toString(floatSignal[i]));
                osw.write("\n");
            }
            osw.flush();
            file.setReadable(true, false);
            System.out.println(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        short[] preamble = new short[BARKER13.length * SAMPLES_PER_BLOCK];
        barker13Preamble(preamble, 0);

        float[] floatPreamble = shortsToFloats(preamble);

        try {
            File file = File.createTempFile("truepreamble", "");
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file));
            for (int i = 0; i < preamble.length; i++) {
                osw.write(Float.toString(preamble[i]));
                osw.write("\n");
            }
            osw.flush();
            file.setReadable(true, false);
            System.out.println(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        short[] preamble = new short[(BARKER13.length * SAMPLES_PER_BLOCK + PREAMBLE_SAMPLE_GAP) * NUM_PREAMBLES];
//        for (int i = 0; i < NUM_PREAMBLES; i++) {
//            // Leave the next PREAMBLE_SAMPLE_GAP samples to just be zero.
//            barker13Preamble(preamble, (BARKER13.length * SAMPLES_PER_BLOCK + PREAMBLE_SAMPLE_GAP) * i);
//        }
//
//        float trueCorrelation = 0;
//        for (int i = 0; i < preamble.length; i++) {
//            float v = ((float) preamble[i]) / Short.MAX_VALUE;
//            trueCorrelation += v * v;
//        }

        System.out.println("Searching for preamble");

        float[] corrs = new float[signal.length - preamble.length];
        for (int i = 0; i < signal.length - preamble.length; i++) {
//            float minSignal = Short.MAX_VALUE;
//            float maxSignal = Short.MIN_VALUE;
//            for (int j = 0; j < preamble.length; j++) {
//                short v = signal[i + j];
//                if (v < minSignal) {
//                    minSignal = v;
//                }
//                if (v > maxSignal) {
//                    maxSignal = v;
//                }
//            }
//            float correlation = 0;
//            for (int j = 0; j < preamble.length; j++) {
//                correlation += floatSignal[j] * ((((float) signal[i + j]) - minSignal) / (maxSignal - minSignal) * 2 - 1);
//            }

            float correlation = 0;
            for (int j = 0; j < preamble.length; j++) {
                correlation += floatPreamble[j] * floatSignal[i + j];
            }
            corrs[i] = correlation;
//            if (correlation / trueCorrelation > 0.1) {
//                System.out.print(i + ": ");
//                System.out.println(correlation / trueCorrelation);
//            }
        }
        System.out.println("... and done.");

        System.out.println("Saving correlations file...");
        try {
            File file = File.createTempFile("correlation", "");
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file));
            for (int i = 0; i < corrs.length; i++) {
                osw.write(Float.toString(corrs[i]));
                osw.write("\n");
            }
            osw.flush();
            file.setReadable(true, false);
            System.out.println(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("... and done.");

//        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
//        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
//
//        AudioTrack track = new AudioTrack(
////                AudioManager.STREAM_SYSTEM,
//                AudioManager.STREAM_MUSIC,
//                SAMPLE_RATE,
//                channelConfig,
//                audioFormat,
//                signal.length * 2,
//                MODE_STATIC);
//
//        track.setStereoVolume(getMaxVolume(), getMaxVolume());
//        track.write(signal, 0, signal.length);
//        track.play();
    }
}
