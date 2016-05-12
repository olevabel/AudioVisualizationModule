package example.audiovisualization.app.fragment;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
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

import example.audiovisualization.R;
import example.audiovisualization.app.AudioSaveListener;
import example.audiovisualization.app.activity.AudioVisulizationActivity;

/**
 * Created by olevabel on 28/02/16.
 */
public class AudioRecordFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_SAMPLE_RATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final String TEMP_FILE = "record_temp.raw";
    private static final String WAV_EXT = ".wav";
    private static final String MP4_EXT = ".mp4";
    private static final String MIME_TYPE_MP4 = "audio/mp4a-latm";
    private static final String DIR_NAME = "AudioRecordModule";
    private static final String FILENAME = "tore";
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
    private String fullFileName;
    private Thread recordingThread;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_voice_meter, container, false);
        meter = (ProgressBar) rootView.findViewById(R.id.sound_meter);
        Button btnCheckVolume = (Button) rootView.findViewById(R.id.btn_check_volume);
        Button btnRecord = (Button) rootView.findViewById(R.id.btn_record);
        Button btnStop = (Button) rootView.findViewById(R.id.btn_stop);
        final RadioButton btnMp4 = (RadioButton) rootView.findViewById(R.id.format_mp4);
        final RadioButton btnWav = (RadioButton) rootView.findViewById(R.id.format_wav);
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
                            if (btnMp4.isChecked()) {
                                try {
                                    writeAudioDataToFile(true);
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            } else {
                                try {
                                    writeAudioDataToFile(false);
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }

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
                            if (btnWav.isChecked()) {
                                rawToWave(new File(setupFileName(true, false)), new File(setupFileName(false, false)));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        isVolumeChecking = false;
                    }
                }
                Intent intent = new Intent(getActivity(), AudioVisulizationActivity.class);
                intent.putExtra(AudioVisulizationActivity.EXTRA_FILE_NAME, fullFileName);
                startActivity(intent);
            }
        });
        return rootView;
    }

    private void writeAudioDataToFile(boolean isMp4) throws IOException {
        FileOutputStream out = null;
        MediaMuxer muxer = null;
        MediaFormat audioFormat;
        ByteBuffer inputBuffer = null;
        MediaCodec.BufferInfo bufferInfo = null;
        int audioIndex = 0;
        byte data[] = new byte[bufferSize];
        if (isMp4) {
            String filename = setupFileName(false, true);
            muxer = new MediaMuxer(filename, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            audioFormat = MediaFormat.createAudioFormat(MIME_TYPE_MP4, RECORDER_SAMPLE_RATE, RECORDER_CHANNELS);
            audioIndex = muxer.addTrack(audioFormat);
            bufferInfo = new MediaCodec.BufferInfo();
        }
        String filename = setupFileName(true, false);
        try {
            out = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int read;
        if (out != null) {
            while (isRecording) {
                read = record.read(data, 0, bufferSize);
                inputBuffer = ByteBuffer.wrap(data);
                if (isMp4) {
                    muxer.start();
                    muxer.writeSampleData(audioIndex, inputBuffer, bufferInfo);
                } else {
                    if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                        try {
                            byte[] copyData = new byte[data.length];
                            for(int i = 0; i < data.length; i++) {
                                byte b = (byte) Math.min(data[i] * volumeMultiplier, 255);
                                copyData[i] = b;
                            }
                            out.write(copyData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (isMp4) {
                try {
                    muxer.stop();
                    muxer.release();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
    private String setupFileName(boolean isTemp, boolean isMp4) {
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
        if (isMp4) {
            fullFileName = directory.getAbsolutePath() + "/" + FILENAME + MP4_EXT;
            return fullFileName;
        }
        fullFileName = directory.getAbsolutePath() + "/" + FILENAME + WAV_EXT;
        return fullFileName;
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
            outFile.write(intToByteArray((int) RECORDER_SAMPLE_RATE), 0, 4);        // 24 - samples per second (numbers per second)
            outFile.write(intToByteArray((int) RECORDER_SAMPLE_RATE * RECORDER_CHANNELS / Byte.SIZE), 0, 4);        // 28 - bytes per second
            outFile.write(shortToByteArray((short) 2), 0, 2);    // 32 - # of bytes in one sample, for all channels
            outFile.write(shortToByteArray((short) RECORDER_BPP), 0, 2);    // 34 - how many bits in a sample(number)?  usually 16 or 24
            outFile.writeBytes("data");                    // 36 - data
            outFile.write(intToByteArray((int) rawData.length), 0, 4);        // 40 - how big is this data chunk
            outFile.write(rawData);                        // 44 - the actual data itself - just a long string of numbers
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private static byte[] intToByteArray(int i) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array();
    }

    // convert a short to a byte array
    public static byte[] shortToByteArray(short data) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(data).array();
    }
}