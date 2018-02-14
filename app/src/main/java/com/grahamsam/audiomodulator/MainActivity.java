package com.grahamsam.audiomodulator;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingDeque;

import static android.media.AudioTrack.MODE_STATIC;
import static android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import static android.media.AudioTrack.getMaxVolume;

public class MainActivity extends AppCompatActivity {
    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLES_PER_BLOCK = 99;
//    private static final int FREQUENCY_0 = 3000;
    private static final int FREQUENCY_1 = 2000;
    private static final int RECORDER_BUFFER_SIZE_BYTES = 2 * SAMPLE_RATE;
    private static final int PREAMBLE_SEARCH_LENGTH = SAMPLE_RATE * 3;

    private static final float LOW_AMPLITUDE = (float) 0.1;
    private static final float HIGH_AMPLITUDE = 1;

//    private static float[] FREQUENCY_0_ENVELOPE;
//    private static float[] FREQUENCY_1_ENVELOPE;

    private static Charset CHARSET = Charset.forName("ASCII");

    private static final boolean[] BARKER13 = {true, true, true, true, true, false, false, true, true, false, true, false, true};

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

//    private AudioRecord recorder;
    private AsyncTask<Void, Void, Void> recordingAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        short[] freq0Env = new short[SAMPLES_PER_BLOCK];
//        short[] freq1Env = new short[SAMPLES_PER_BLOCK];
//        writeAMSignal(freq0Env, 0, freq0Env.length, 1, 0, FREQUENCY_0);
//        writeAMSignal(freq1Env, 0, freq1Env.length, 1, 0, FREQUENCY_1);

//        FREQUENCY_0_ENVELOPE = shortsToFloats(freq0Env);
//        FREQUENCY_1_ENVELOPE = shortsToFloats(freq1Env);
    }

    private void writeAMSignal(short[] buffer, int start, int length, float i, float f0) {
//        double norm = Math.sqrt(i * i + q * q);
        double poop = ((double) length) / 2;
        for (int j = 0; j < length; j++) {
            double x = 2 * Math.PI * f0 * (j - poop) / SAMPLE_RATE;
            double v = i * Math.cos(x);
            double window = (1 - Math.abs(j - poop) / poop);
//            buffer[start + j] = (short) (v * window / norm * Short.MAX_VALUE);
            buffer[start + j] = (short) (v * window * Short.MAX_VALUE);
        }
    }

//    private void barker13PreambleFM(short[] buffer, int start) {
//        for (int i = 0; i < BARKER13.length; i++) {
//            if (BARKER13[i]) {
//                writeAMSignal(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, 1, 0, FREQUENCY_1);
//            } else {
//                writeAMSignal(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, 1, 0, FREQUENCY_0);
//            }
//        }
//    }

    private void barker13PreambleAM(short[] buffer, int start) {
        for (int i = 0; i < BARKER13.length; i++) {
            if (BARKER13[i]) {
                writeAMSignal(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, HIGH_AMPLITUDE, FREQUENCY_1);
            } else {
                writeAMSignal(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, LOW_AMPLITUDE, FREQUENCY_1);
            }
        }
    }

//    private void writeSignalByteFM(short[] buffer, int start, byte byt) {
//        for (int i = 0; i < 8; i++) {
//            int bit = (byt >> (7 - i)) & 1;
//            if (bit == 1) {
//                writeAMSignal(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, 1, 0, FREQUENCY_1);
//            } else {
//                writeAMSignal(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, 1, 0, FREQUENCY_0);
//            }
//        }
//    }

    private void writeSignalByteAM(short[] buffer, int start, byte byt) {
        for (int i = 0; i < 8; i++) {
            int bit = (byt >> (7 - i)) & 1;
            if (bit == 1) {
                writeAMSignal(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, HIGH_AMPLITUDE, FREQUENCY_1);
            } else {
                writeAMSignal(buffer, start + i * SAMPLES_PER_BLOCK, SAMPLES_PER_BLOCK, LOW_AMPLITUDE, FREQUENCY_1);
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
        barker13PreambleAM(buffer, 0);
        current_ix += preambleNumSamples;

        // Write the number of bytes to follow
        writeSignalByteAM(buffer, current_ix, (byte) ((msg.length >>> 8) & 0xFF));
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

        System.out.print("True message length (in bytes): ");
        System.out.println(messageBytes.length);

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
        EditText editText = findViewById(R.id.editText2);
        editText.setText("");

        // Only start recording if we are not already.
        if (recordingAsyncTask == null) {
            // Clear the previously accumulated recording data.
            this.outputStream.reset();

            recordingAsyncTask = new AsyncTask<Void, Void, Void>() {
                private AudioRecord audioRecord;

                @Override
                protected void onPreExecute() {
//                mState = State.RECORDING;
                    audioRecord = new AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            RECORDER_BUFFER_SIZE_BYTES);
                }

                @Override
                protected Void doInBackground(Void... params) {
                    audioRecord.startRecording();
                    ByteBuffer bb = ByteBuffer.allocate(RECORDER_BUFFER_SIZE_BYTES / 4);
                    short[] shortBuffer = new short[bb.capacity() / 2];
                    while (!isCancelled()) {
                        // We reuse bb so don't fuck it up.
                        bb.clear();

                        // Returns the number of shorts read, or negative value on error.
                        int read = audioRecord.read(shortBuffer, 0, shortBuffer.length);

//                        System.out.println(Arrays.toString(shortBuffer));

                        // avoid an infinite loop in the following code
                        assert read >= 0;

                        for (int i = 0; i < read; i++) {
                            bb.putShort(shortBuffer[i]);
                        }
                        byte[] actualBytes = new byte[read * 2];
                        bb.rewind();
                        bb.get(actualBytes, 0, actualBytes.length);
                        outputStream.write(actualBytes, 0, actualBytes.length);

                        System.out.println("processed buffer read.");
                    }

                    audioRecord.release();
                    audioRecord = null;

                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    recordingAsyncTask = null;
                    if (audioRecord != null) {
                        audioRecord.release();
                    }
                    audioRecord = null;
                }

                @Override
                protected void onCancelled() {
                    recordingAsyncTask = null;
                    if (audioRecord != null) {
                        audioRecord.release();
                    }
                    audioRecord = null;
                }
            };

            recordingAsyncTask.execute();
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

    private byte readSignalByteAM(float[] buffer, int start, float threshold) {
        int res = 0;
        for (int j = 0; j < 8; j++) {
            float amplitude = 0;
            for (int i = 0; i < SAMPLES_PER_BLOCK; i++) {
                amplitude += Math.abs(buffer[start + j * SAMPLES_PER_BLOCK + i]);
            }
            System.out.print("amplitude: ");
            System.out.println(amplitude);
            res = res << 1;
            if (amplitude > threshold) {
                res |= 1;
            }
        }
        return (byte) res;
    }

    public void onClickFinish(View view) {
        if (this.recordingAsyncTask != null) {
            this.recordingAsyncTask.cancel(true);
            this.recordingAsyncTask = null;

            byte[] outputStreamByteArray = outputStream.toByteArray();
            short[] signal = new short[outputStreamByteArray.length / 2];
            ByteBuffer.wrap(outputStreamByteArray).asShortBuffer().get(signal);
            float[] floatSignal = shortsToFloats(signal);

            // TODO perhaps calculate this ahead of time.
            short[] preamble = new short[BARKER13.length * SAMPLES_PER_BLOCK];
            barker13PreambleAM(preamble, 0);

            float[] floatPreamble = shortsToFloats(preamble);

            System.out.println("Searching for preamble");
            int best_start_ix = 0;
            float best_correlation = Float.MIN_VALUE;
            for (int i = 0; i < Math.min(PREAMBLE_SEARCH_LENGTH, floatSignal.length - floatPreamble.length); i++) {
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

            int current_ix = best_start_ix;
            float sum0Amplitude = 0;
            float sum1Amplitude = 0;
            for (int i = 0; i < BARKER13.length; i++) {
                float amplitude = 0;
                for (int j = current_ix; j < current_ix + SAMPLES_PER_BLOCK; j++) {
                    amplitude += Math.abs(floatSignal[j]);
                }
                current_ix += SAMPLES_PER_BLOCK;

                if (BARKER13[i]) {
                    sum1Amplitude += amplitude;
                } else {
                    sum0Amplitude += amplitude;
                }
            }
            float avg0Amplitude = sum0Amplitude / 4;
            float avg1Amplitude = sum1Amplitude / 9;
            float amplitudeThreshold = (avg0Amplitude + avg1Amplitude) / 2;
            System.out.print("Amplitude thresholding at ");
            System.out.println(amplitudeThreshold);

            System.out.print("current_ix before");
            System.out.println(current_ix);

            current_ix = best_start_ix + floatPreamble.length;
            System.out.print("current_ix after");
            System.out.println(current_ix);

            System.out.print("Starting read at ");
            System.out.println(current_ix);
            byte firstLengthByte = readSignalByteAM(floatSignal, current_ix, amplitudeThreshold);
            current_ix += 8 * SAMPLES_PER_BLOCK;
            byte secondLengthByte = readSignalByteAM(floatSignal, current_ix, amplitudeThreshold);
            current_ix += 8 * SAMPLES_PER_BLOCK;

            int messageLength = ((firstLengthByte & 0xFF) << 8) | (secondLengthByte & 0xFF);
            // TODO don't go over the length of the signal thing.
            System.out.print("Message length: ");
            System.out.println(messageLength);
            System.out.println("Demodulating...");
            byte[] recvBytes = new byte[messageLength];
            int maxBytes = (floatSignal.length - current_ix) / (8 * SAMPLES_PER_BLOCK);
            if (messageLength > maxBytes) {
                System.out.println("MESSAGE LENGTH IS DEF WRONG");
                System.out.print("Length reported: ");
                System.out.println(messageLength);
                System.out.print("Maximum possible: ");
                System.out.println(maxBytes);

                EditText editText = findViewById(R.id.editText2);
                editText.setText("MESSAGE LENGTH IS DEF WRONG");

                return;
            }
//            int messageLengthFixed = Math.min(messageLength, maxBytes);
            for (int i = 0; i < messageLength; i++) {
                recvBytes[i] = readSignalByteAM(floatSignal, current_ix, amplitudeThreshold);

                int peak_ix = current_ix + 8 * SAMPLES_PER_BLOCK;
                float maxPeakValue = 0;
                for (int j = current_ix + 7 * SAMPLES_PER_BLOCK + SAMPLES_PER_BLOCK / 4; j < current_ix + 8 * SAMPLES_PER_BLOCK - SAMPLES_PER_BLOCK / 4; j++) {
                    if (floatSignal[j] > maxPeakValue) {
                        peak_ix = j;
                        maxPeakValue = floatSignal[j];
                    }
                }
//                current_ix += 8 * SAMPLES_PER_BLOCK;
                current_ix = peak_ix + (SAMPLES_PER_BLOCK / 2);
            }
            System.out.println("... and done");

            String recvMessage = new String(recvBytes, CHARSET);
            EditText editText = findViewById(R.id.editText2);
            editText.setText(recvMessage);
        }

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
    }
}
