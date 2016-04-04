package myown.soundwavetest.waveform;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
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
import android.widget.Spinner;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import myown.soundwavetest.R;

/**
 * Created by olevabel on 28/02/16.
 */
public class MicVolumeFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private static final int RECORDER_SAMPLE_RATE = 44000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord record;
    private boolean isRecording;
    private Spinner sourceSpinner;
    private ProgressBar meter;
    private short[] buffer = new short[1024];
    private int value = 0;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_voice_meter, container, false);
        meter = (ProgressBar) rootView.findViewById(R.id.sound_meter);
        Button btnRecord = (Button) rootView.findViewById(R.id.btn_record);
        Button btnStop = (Button) rootView.findViewById(R.id.btn_stop);
        sourceSpinner = (Spinner) rootView.findViewById(R.id.source_spinner);
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.sources_array,android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceSpinner.setAdapter(arrayAdapter);
        sourceSpinner.setOnItemSelectedListener(this);
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (initRecorder(MediaRecorder.AudioSource.MIC)) {
                    record.startRecording();
                    isRecording = true;
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
                                handler.postDelayed(update, 2000);
                            }
                        }
                    }).start();

                }
            }

        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (record != null) {
                    record.stop();
                    isRecording = false;
                    value = 0;
                    handler.post(update);

                }
            }
        });
        return rootView;
    }


    private boolean initRecorder(int source) {
        record = new AudioRecord(source, RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING));
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
        if(position == 1) {
            initRecorder(MediaRecorder.AudioSource.MIC);
        } else {
            initRecorder(MediaRecorder.AudioSource.CAMCORDER);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
         initRecorder(MediaRecorder.AudioSource.CAMCORDER);
    }
}
