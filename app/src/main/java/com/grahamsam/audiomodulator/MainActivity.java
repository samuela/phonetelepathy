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
import java.nio.charset.Charset;

import static android.media.AudioTrack.*;

public class MainActivity extends AppCompatActivity {
    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLES_PER_BLOCK = 500;
//    private static final int PREAMBLE_SAMPLE_GAP = 0;
//    private static final int START_GAP = 500;
//    private static final int NUM_PREAMBLES = 1;
    private static final int FREQUENCY_0 = 3000;
    private static final int FREQUENCY_1 = 7000;
    private static final int MAX_RECV_SAMPLES = SAMPLE_RATE * 5;
    private static final int PREAMBLE_SEARCH_LENGTH = SAMPLE_RATE * 3;

    private static float[] FREQUENCY_0_ENVELOPE;
    private static float[] FREQUENCY_1_ENVELOPE;

    private static Charset CHARSET = Charset.forName("ASCII");

    private static final boolean[] BARKER13 = {true, true, true, true, true, false, false, true, true, false, true, false, true};

    private AudioRecord recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        short[] freq0Env = new short[SAMPLES_PER_BLOCK];
        short[] freq1Env = new short[SAMPLES_PER_BLOCK];
        writeQAM(freq0Env, 0, freq0Env.length, 1, 0, FREQUENCY_0);
        writeQAM(freq1Env, 0, freq1Env.length, 1, 0, FREQUENCY_1);

        FREQUENCY_0_ENVELOPE = shortsToFloats(freq0Env);
        FREQUENCY_1_ENVELOPE = shortsToFloats(freq1Env);
    }

    private void writeQAM(short[] buffer, int start, int length, float i, float q, float f0) {
//        double norm = Math.sqrt(i * i + q * q);
        double poop = ((double) length) / 2;
        for (int j = 0; j < length; j++) {
            double x = 2 * Math.PI * f0 * j / SAMPLE_RATE;
            double v = i * Math.cos(x) - q * Math.sin(x);
            double window = (1 - Math.abs(j - poop) / poop);
//            buffer[start + j] = (short) (v * window / norm * Short.MAX_VALUE);
            buffer[start + j] = (short) (v * window * Short.MAX_VALUE);
        }
    }

    private void barker13Preamble(short[] buffer, int start) {
        for (int i = 0; i < BARKER13.length; i++) {
            if (BARKER13[i]) {
                writeQAM(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, 1, 0, FREQUENCY_1);
            } else {
                writeQAM(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, 1, 0, FREQUENCY_0);
            }
        }
    }

//    private void writeSignalByteFM(short[] buffer, int start, byte byt) {
//        for (int i = 0; i < 8; i++) {
//            int bit = (byt >> (7 - i)) & 1;
//            if (bit == 1) {
//                writeQAM(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, 1, 0, FREQUENCY_1);
//            } else {
//                writeQAM(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, 1, 0, FREQUENCY_0);
//            }
//        }
//    }

    private void writeSignalByteAM(short[] buffer, int start, byte byt) {
        for (int i = 0; i < 8; i++) {
            int bit = (byt >> (7 - i)) & 1;
            if (bit == 1) {
                writeQAM(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, 1, 0, FREQUENCY_1);
            } else {
                writeQAM(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, (float) 0.1, 0, FREQUENCY_1);
            }
        }
    }

    private short[] constructSignal(byte[] msg) {
        assert msg.length <= (1 << 16);

        int preambleNumSamples = BARKER13.length * SAMPLES_PER_BLOCK;
        int lengthNumSamples = 2 * 8 * SAMPLES_PER_BLOCK;
        int msgNumSamples = 8 * msg.length * SAMPLES_PER_BLOCK;
        short[] buffer = new short[preambleNumSamples + lengthNumSamples + msgNumSamples];

        // Write the preamble
        int current_ix = 0;
        barker13Preamble(buffer, 0);
        current_ix += preambleNumSamples;

        // Write the number of bytes to follow
        writeSignalByteAM(buffer, current_ix, (byte) (msg.length & (0xFF << 8)));
        current_ix += 8 * SAMPLES_PER_BLOCK;
        writeSignalByteAM(buffer, current_ix, (byte) (msg.length & 0xFF));
        current_ix += 8 * SAMPLES_PER_BLOCK;

        // Write the payload
        for (int i = 0; i < msg.length; i++) {
            writeSignalByteAM(buffer, current_ix, msg[i]);
            current_ix += 8 * SAMPLES_PER_BLOCK;
        }
        return buffer;
    }

    public void onClickSend(final View view) {
        EditText editText = findViewById(R.id.editText2);
        String message = editText.getText().toString();
        byte[] messageBytes = message.getBytes(CHARSET);

        short[] audioSignal = constructSignal(messageBytes);
//        short[] audioSignal = constructSignal(new byte[] {});
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
    }

    public void onClickReceive(View view) {
        if (this.recorder == null) {
            System.out.println("Beginning recording...");
            this.recorder = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    2 * MAX_RECV_SAMPLES);

//            System.out.println(this.recorder.getState());
            this.recorder.startRecording();
        }
    }

    private float[] shortsToFloats(short[] arr) {
        float[] ret = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = ((float) arr[i]) / Short.MAX_VALUE;
        }
        return ret;
    }

//    private byte readSignalByteFM(float[] buffer, int start) {
//        int res = 0;
//        for (int j = 0; j < 8; j++) {
//            float correlation0 = 0;
//            float correlation1 = 0;
//            for (int i = 0; i < SAMPLES_PER_BLOCK; i++) {
//                correlation0 += FREQUENCY_0_ENVELOPE[i] * buffer[start + j * SAMPLES_PER_BLOCK + i];
//                correlation1 += FREQUENCY_1_ENVELOPE[i] * buffer[start + j * SAMPLES_PER_BLOCK + i];
//            }
//            res = res << 1;
////            if (correlation0 < correlation1) {
////                res |= 1;
////            }
//            if (Math.abs(correlation0) < Math.abs(correlation1)) {
//                res |= 1;
//            }
//        }
//        return (byte) res;
//    }

    private byte readSignalByteAM(float[] buffer, int start) {
        int res = 0;
        for (int j = 0; j < 8; j++) {
            float amplitude = 0;
            for (int i = 0; i < SAMPLES_PER_BLOCK; i++) {
                amplitude += Math.abs(buffer[start + j * SAMPLES_PER_BLOCK + i]);
            }
            System.out.print("amplitude: ");
            System.out.println(amplitude);
            res = res << 1;
            if (amplitude > 50) {
                res |= 1;
            }
        }
        return (byte) res;
    }

    public void onClickFinish(View view) {
        if (this.recorder != null) {
            System.out.println("Finished receiving...");
            short[] signal = new short[MAX_RECV_SAMPLES];
            this.recorder.read(signal, 0, signal.length);
            this.recorder.stop();
            this.recorder.release();
            this.recorder = null;

            float[] floatSignal = shortsToFloats(signal);

//        try {
//            File file = File.createTempFile("received", "stuff");
//            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file));
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

            short[] preamble = new short[BARKER13.length * SAMPLES_PER_BLOCK];
            barker13Preamble(preamble, 0);

            float[] floatPreamble = shortsToFloats(preamble);

//        try {
//            File file = File.createTempFile("truepreamble", "");
//            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file));
//            for (int i = 0; i < preamble.length; i++) {
//                osw.write(Float.toString(preamble[i]));
//                osw.write("\n");
//            }
//            osw.flush();
//            file.setReadable(true, false);
//            System.out.println(file.getAbsolutePath());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

            System.out.println("Searching for preamble");
            int best_start_ix = 0;
            float best_correlation = Float.MIN_VALUE;
            for (int i = 0; i < PREAMBLE_SEARCH_LENGTH; i++) {
                float correlation = 0;
                for (int j = 0; j < preamble.length; j++) {
                    correlation += floatPreamble[j] * floatSignal[i + j];
                }
                if (correlation > best_correlation) {
                    best_start_ix = i;
                    best_correlation = correlation;
                }
            }
            System.out.println("... and done.");

            int current_ix = best_start_ix + floatPreamble.length;
            byte firstLengthByte = readSignalByteAM(floatSignal, current_ix);
            current_ix += 8 * SAMPLES_PER_BLOCK;
            byte secondLengthByte = readSignalByteAM(floatSignal, current_ix);
            current_ix += 8 * SAMPLES_PER_BLOCK;

            int messageLength = ((firstLengthByte & 0xFF) << 8) | (secondLengthByte & 0xFF);
//            messageLength = Math.min(messageLength, 45); // HACK
            messageLength = 16;
            System.out.print("Message length: ");
            System.out.println(messageLength);
            System.out.println("Demodulating...");
            byte[] recvBytes = new byte[messageLength];
            for (int i = 0; i < messageLength; i++) {
                recvBytes[i] = readSignalByteAM(floatSignal, current_ix);
                current_ix += 8 * SAMPLES_PER_BLOCK;
            }
            System.out.println("... and done");

            String recvMessage = new String(recvBytes, CHARSET);
            EditText editText = findViewById(R.id.editText2);
            editText.setText(recvMessage);
        }
    }
}
