package myown.soundwavetest;

import android.graphics.Color;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import myown.soundwavetest.waveform.MicVolumeFragment;
import myown.soundwavetest.waveform.Segment;
import myown.soundwavetest.waveform.WaveformFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new MicVolumeFragment())
                    .commit();
        }
    }
    public static class CustomWaveformFragment extends WaveformFragment {

        /**
         * Provide path to your audio file.
         *
         * @return
         */
        @Override
        protected String getFileName() {
            File extStore = Environment.getExternalStorageDirectory();
            if(extStore.getAbsoluteFile().exists()){
              return extStore.getAbsolutePath() + "/Tore.mp3";
            }
            return "path to your audio file";
        }

        /**
         * Optional - provide list of segments (start and stop values in seconds) and their corresponding colors
         *
         * @return
         */
        @Override
        protected List<Segment> getSegments() {
            return Arrays.asList(
                    new Segment(55.2, 55.8, Color.rgb(238, 23, 104)),
                    new Segment(56.2, 56.6, Color.rgb(238, 23, 104)),
                    new Segment(58.4, 59.9, Color.rgb(184, 92, 184)));
        }
    }
}
