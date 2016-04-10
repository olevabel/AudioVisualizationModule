package myown.soundwavetest.waveform;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import myown.soundwavetest.R;

/**
 * Created by olevabel on 28/02/16.
 */
public class MicVolumeFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_SAMPLE_RATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final String TEMP_FILE = "record_temp.raw";
    private static final String EXT = ".wav";
    private static final String DIR_NAME = "AudioRecordModule";
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord record;
    private boolean isRecording;
    private boolean isInitialized;
    private int source;
    private ProgressBar meter;
    private int bufferSize;
    private int volumeSeek;
    private double volumeMultiplier;
    private short[] buffer = new short[2048];
    private int value = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isVolumeChecking;
    private Thread recordingThread;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_voice_meter, container, false);
        meter = (ProgressBar) rootView.findViewById(R.id.sound_meter);
        Button btnCheckVolume = (Button) rootView.findViewById(R.id.btn_check_volume);
        Button btnRecord = (Button) rootView.findViewById(R.id.btn_record);
        Button btnStop = (Button) rootView.findViewById(R.id.btn_stop);
        SeekBar volumeChangeSeekBar = (SeekBar) rootView.findViewById(R.id.seekbar_volume_change);
        volumeChangeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeSeek = progress;
                volumeMultiplier = volumeMultiplyingFormula(volumeSeek);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        final Spinner sourceSpinner = (Spinner) rootView.findViewById(R.id.source_spinner);
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.sources_array, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceSpinner.setAdapter(arrayAdapter);
        sourceSpinner.setOnItemSelectedListener(this);
        meter.setScaleY(3f);
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        btnCheckVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (record == null) {
                    initRecorder(source);
                }
                if (isInitialized) {
                    record.startRecording();
                    isRecording = true;
                    isVolumeChecking = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            while (isRecording) {
                                double sum = 0;
                                int readSize = record.read(buffer, 0, buffer.length);
                                for (int i = 0; i < readSize; i++) {
                                    sum += buffer[i] * buffer[i];
                                }
                                if (readSize > 0) {
                                    final double amplitude = sum / readSize;
                                    value = (int) amplitude;
                                }
                                handler.postDelayed(update, 1000);
                            }
                        }
                    }).start();
                }
            }

        });
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (record == null) {
                    initRecorder(source);
                }
                if (isInitialized) {
                    record.startRecording();
                    isRecording = true;
                    recordingThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            writeAudioDataToFile();
                        }
                    });
                    recordingThread.start();
                }
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (record != null) {
                    isRecording = false;
                    isInitialized = false;
                    if (record.getState() == 1) {
                        record.stop();
                        record.release();
                    }
                    record = null;
                    recordingThread = null;
                    if (!isVolumeChecking) {
                        try {
                            rawToWave(new File(setupFileName(true)), new File(setupFileName(false)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        isVolumeChecking = false;
                    }
                }
            }
        });
        return rootView;
    }

    private void writeAudioDataToFile() {
        FileOutputStream out = null;
        byte data[] = new byte[bufferSize];
        String filename = setupFileName(true);
        try {
            out = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int read;
        if (out != null) {
            while (isRecording) {
                read = record.read(data, 0, bufferSize);
                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                    try {
                        out.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private double volumeMultiplyingFormula(int volumeMultiplier) {
        return 1 + volumeMultiplier / 100.0;
    }


    private boolean initRecorder(int source) {
        record = new AudioRecord(source, RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);
        return record.getState() == AudioRecord.STATE_INITIALIZED;
    }

    private Runnable update = new Runnable() {
        @Override
        public void run() {
            meter.setProgress(value);
        }
    };

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == 1) {
            source = MediaRecorder.AudioSource.MIC;
            isInitialized = initRecorder(source);
        } else {
            source = MediaRecorder.AudioSource.CAMCORDER;
            isInitialized = initRecorder(source);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        initRecorder(MediaRecorder.AudioSource.CAMCORDER);
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String setupFileName(boolean isTemp) {
        String path = Environment.getExternalStorageDirectory().getPath();
        File directory = new File(path, DIR_NAME);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        if (isTemp) {
            File tempFile = new File(path, TEMP_FILE);
            if (tempFile.exists()) {
                tempFile.delete();
            }
            return directory.getAbsolutePath() + "/" + TEMP_FILE;
        }
        return directory.getAbsolutePath() + "/" + "hullult äge" + EXT;
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }

    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, RECORDER_SAMPLE_RATE); // sample rate
            writeInt(output, RECORDER_SAMPLE_RATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }
            output.write(bytes.array());
        } finally {
            if (output != null) {
                output.close();
            }
        }
        try {
            DataOutputStream outFile = new DataOutputStream(new FileOutputStream(waveFile));

            // write the wav file per the wav file format
            outFile.writeBytes("RIFF");                    // 00 - RIFF
            outFile.write(intToByteArray((int) 36 + rawData.length), 0, 4);        // 04 - how big is the rest of this file?
            outFile.writeBytes("WAVE");                    // 08 - WAVE
            outFile.writeBytes("fmt ");                    // 12 - fmt
            outFile.write(intToByteArray((int) 16), 0, 4);    // 16 - size of this chunk
            outFile.write(shortToByteArray((short) 1), 0, 2);        // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
            outFile.write(shortToByteArray((short) 1), 0, 2);    // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
            outFile.write(intToByteArray((int) 44100), 0, 4);        // 24 - samples per second (numbers per second)
            outFile.write(intToByteArray((int) 44100 * 2), 0, 4);        // 28 - bytes per second
            outFile.write(shortToByteArray((short) 2), 0, 2);    // 32 - # of bytes in one sample, for all channels
            outFile.write(shortToByteArray((short) 16), 0, 2);    // 34 - how many bits in a sample(number)?  usually 16 or 24
            outFile.writeBytes("data");                    // 36 - data
            outFile.write(intToByteArray((int) rawData.length), 0, 4);        // 40 - how big is this data chunk
            outFile.write(rawData);                        // 44 - the actual data itself - just a long string of numbers
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }
    private static byte[] intToByteArray(int i)
    {
        byte[] b = new byte[4];
        b[0] = (byte) (i & 0x00FF);
        b[1] = (byte) ((i >> 8) & 0x000000FF);
        b[2] = (byte) ((i >> 16) & 0x000000FF);
        b[3] = (byte) ((i >> 24) & 0x000000FF);
        return b;
    }

    // convert a short to a byte array
    public static byte[] shortToByteArray(short data)
    {
        return new byte[]{(byte)(data & 0xff),(byte)((data >>> 8) & 0xff)};
    }
}