package example.audiovisualization.app.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

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
import example.audiovisualization.app.FileConvertListener;
import example.audiovisualization.app.activity.AudioVisulizationActivity;

/**
 * Created by olevabel on 28/02/16.
 *
 *  General idea for making volume checker http://developer.samsung.com/technical-doc/view.do?v=T000000086
 */
public class AudioRecordFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private static final int PERMISSION_RECORD_AUDIO = 101;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 102;
    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 103;

    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_SAMPLE_RATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final String TEMP_FILE = "record_temp.raw";
    private static final String WAV_EXT = ".wav";
    private static final String MP4_EXT = ".m4a";
    private static final String MIME_TYPE_MP4 = "audio/mp4a-latm";
    private static final String DIR_NAME = "AudioRecordModule";
    private static final String FILENAME = "tore";
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord record;
    private Button btnCheckVolume;
    private Button btnRecord;
    private Button btnStop;
    private RadioButton radioButtonMp4;
    private RadioButton radioButtonWav;
    private SeekBar volumeChangeSeekBar;
    private boolean isRecording;
    private boolean isInitialized;
    private int source;
    private ProgressBar meter;
    private int bufferSize;
    private int volumeSeek;
    private double volumeMultiplier = 1;
    private short[] buffer = new short[2048];
    private int value = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isVolumeChecking;
    private String fullFileName;
    private Thread recordingThread;
    private FFmpeg ffmpeg;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_voice_meter, container, false);
        initFFmpeg();
        checkPermissions();
        meter = (ProgressBar) rootView.findViewById(R.id.sound_meter);
        btnCheckVolume = (Button) rootView.findViewById(R.id.btn_check_volume);
        btnRecord = (Button) rootView.findViewById(R.id.btn_record);
        btnStop = (Button) rootView.findViewById(R.id.btn_stop);
        radioButtonMp4 = (RadioButton) rootView.findViewById(R.id.format_mp4);
        radioButtonWav = (RadioButton) rootView.findViewById(R.id.format_wav);
        RadioButton radioButtonAutomaticVolume = (RadioButton) rootView.findViewById(R.id.audio_volume_type_automatic);
        RadioButton radioButtonManualVolume = (RadioButton) rootView.findViewById(R.id.audio_volume_type_manual);
        volumeChangeSeekBar = (SeekBar) rootView.findViewById(R.id.seekbar_volume_change);
        volumeChangeSeekBar.setEnabled(false);
        radioButtonManualVolume.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                volumeChangeSeekBar.setEnabled(isChecked);
            }
        });
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
                initRecorder(source);
                if (isInitialized) {
                    btnCheckVolume.setEnabled(false);
                    record.startRecording();
                    isRecording = true;
                    isVolumeChecking = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            while (isRecording && record != null) {
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
                if (record != null && record.getState() == AudioRecord.STATE_INITIALIZED) {
                    record.release();
                    initRecorder(source);
                } else {
                    initRecorder(source);
                }
                if (isInitialized) {
                    btnRecord.setEnabled(false);
                    btnStop.setEnabled(true);
                    btnCheckVolume.setEnabled(true);
                    record.startRecording();
                    isRecording = true;
                    isVolumeChecking = false;
                    recordingThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                writeAudioDataToFile();
                            } catch (IOException e1) {
                                e1.printStackTrace();
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
                meter.setProgress(0);
                btnStop.setEnabled(false);
                btnRecord.setEnabled(true);
                if (record != null) {
                    isRecording = false;
                    if (record.getState() == AudioRecord.STATE_INITIALIZED) {
                        record.stop();
                        record.release();
                    }
                    record = null;
                    recordingThread = null;
                    if (!isVolumeChecking) {
                        try {
                            if (radioButtonWav.isChecked()) {
                                rawToWave(new File(setupFileName(true, false)), new File(setupFileName(false, false)), new FileConvertListener() {
                                    @Override
                                    public void onFileConverted() {
                                        startAudioVisualizationActivity();
                                    }
                                });
                            } else if (radioButtonMp4.isChecked()) {
                                rawToMp4(new File(setupFileName(true, true)), new File(setupFileName(false, true)), new FileConvertListener() {
                                    @Override
                                    public void onFileConverted() {
                                        startAudioVisualizationActivity();
                                    }
                                });
                            }
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

    private void checkPermissions() {
        if(ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[] {android.Manifest.permission.RECORD_AUDIO},PERMISSION_RECORD_AUDIO);
        }
        if(ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[] {android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_RECORD_AUDIO:
                resolvePermissionRequest(grantResults);
                break;
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                resolvePermissionRequest(grantResults);
                break;
            case PERMISSION_READ_EXTERNAL_STORAGE:
                resolvePermissionRequest(grantResults);
                break;
        }
    }

    private void resolvePermissionRequest(@NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            // No nothing we have the permission
            Log.d("Permission granted", "Record Audio permission granted");
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getActivity().getString(R.string.dialog_permission_not_granted));
            builder.setCancelable(false);
            builder.setPositiveButton(getActivity().getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().finish();
                }
            });
            builder.show();
        }
    }

    private void startAudioVisualizationActivity() {
        Intent intent = new Intent(getActivity(), AudioVisulizationActivity.class);
        intent.putExtra(AudioVisulizationActivity.EXTRA_FILE_NAME, fullFileName);
        startActivity(intent);
    }

    private void initFFmpeg() {
        ffmpeg = FFmpeg.getInstance(getActivity());
        try {
            ffmpeg.loadBinary(new FFmpegLoadBinaryResponseHandler() {
                @Override
                public void onFailure() {

                }

                @Override
                public void onSuccess() {

                }

                @Override
                public void onStart() {

                }

                @Override
                public void onFinish() {

                }
            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
        }
    }

    private void writeAudioDataToFile() throws IOException {
        FileOutputStream out = null;
        byte data[] = new byte[bufferSize];
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
                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                    try {
                        byte[] copyData = new byte[data.length];
                        for (int i = 0; i < data.length; i++) {
                            byte b = (byte) Math.min(data[i] * volumeMultiplier, 255);
                            copyData[i] = b;
                        }
                        out.write(copyData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            out.close();
        }
    }

    private double volumeMultiplyingFormula(int volumeMultiplier) {
        return 1 + volumeMultiplier / 100.0;
    }


    private boolean initRecorder(int source) {
        record = new AudioRecord(source, RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);
        int state = record.getState();
        if (state == AudioRecord.STATE_INITIALIZED) {
            isInitialized = true;
            return true;
        }
        return false;
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
            File mp4File = new File(directory.getAbsolutePath() + "/" + FILENAME + MP4_EXT);
            if(mp4File.exists()) {
                mp4File.delete();
            }
            fullFileName = directory.getAbsolutePath() + "/" + FILENAME + MP4_EXT;
            return fullFileName;
        }
        File wavFile = new File(directory.getAbsolutePath() + "/" + FILENAME + WAV_EXT);
        if(wavFile.exists()) {
            wavFile.delete();
        }
        fullFileName = directory.getAbsolutePath() + "/" + FILENAME + WAV_EXT;
        return fullFileName;
    }
    /*
    * Code example from http://stackoverflow.com/questions/9179536/writing-pcm-recorded-data-into-a-wav-file-java-android
    * */
    private void rawToWave(final File rawFile, final File waveFile, FileConvertListener listener) throws IOException {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getActivity().getString(R.string.progress_dialog_loading));
        progressDialog.show();
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
            outFile.close();
            rawFile.delete();
            progressDialog.dismiss();
            listener.onFileConverted();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            progressDialog.dismiss();
        }


    }

  /*
  * Instructions from http://writingminds.github.io/ffmpeg-android-java/
  * */
    private void rawToMp4(final File rawFile, File mp4File, final FileConvertListener listener) {
        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getActivity().getString(R.string.progress_dialog_loading));
        progressDialog.show();
        String[] cmd = new String[]{"-f", "s16le", "-ar", "44100", "-ac", "1", "-i", rawFile.getAbsolutePath(),  mp4File.getAbsolutePath()};
        try {
            ffmpeg.execute(cmd, new FFmpegExecuteResponseHandler() {
                @Override
                public void onSuccess(String message) {
                    Log.d("success", message);
                    rawFile.delete();
                    progressDialog.dismiss();
                    listener.onFileConverted();
                }

                @Override
                public void onProgress(String message) {
                    Log.d("in progress", message);
                }

                @Override
                public void onFailure(String message) {
                    Log.d("failure", message);
                    progressDialog.dismiss();
                }

                @Override
                public void onStart() {
                    Log.d("start", "start converting");
                }

                @Override
                public void onFinish() {
                    Log.d("finish", "finished converting");
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    // convert an int to a byte array with little endian order
    private static byte[] intToByteArray(int i) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array();
    }

    // convert a short to a byte array with little endian order
    public static byte[] shortToByteArray(short data) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(data).array();
    }
}