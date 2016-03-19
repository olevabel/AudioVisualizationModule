package myown.soundwavetest.waveform;

import android.animation.ValueAnimator;
import android.media.AudioFormat;
import android.media.AudioRecord;
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;


import myown.soundwavetest.R;
import myown.soundwavetest.waveform.view.MicVolumeMeter;

/**
 * Created by olevabel on 28/02/16.
 */
public class MicVolumeFragment extends Fragment {

    private static final int RECORDER_SAMPLE_RATE = 44000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private MediaRecorder recorder;
    private boolean isRecording;
    private Spinner fileFormatSpinner;
    private ProgressBar meter;
    private int value = 0;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_voice_meter, container, false);
        meter = (ProgressBar) rootView.findViewById(R.id.sound_meter);
        initRecorder();
        Button btnRecord = (Button) rootView.findViewById(R.id.btn_record);
        Button btnStop = (Button) rootView.findViewById(R.id.btn_stop);
        fileFormatSpinner = (Spinner) rootView.findViewById(R.id.file_format_spinner);
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recorder != null) {
                    recorder.start();
                    isRecording = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while(isRecording) {
                                value = recorder.getMaxAmplitude();
                                handler.postDelayed(update, 5000);
                            }
                        }
                    }).start();

                }
            }

        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recorder != null) {
                    recorder.stop();
                    isRecording = false;

                }
            }
        });
        return rootView;
    }


    private void initRecorder() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/lahe.3gpp");
        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Runnable update = new Runnable() {
        @Override
        public void run() {
            meter.setProgress(value);
        }
    };

}
