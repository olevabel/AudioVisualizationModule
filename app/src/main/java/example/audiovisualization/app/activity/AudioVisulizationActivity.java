package example.audiovisualization.app.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import example.audiovisualization.R;
import example.audiovisualization.app.fragment.Segment;
import example.audiovisualization.app.fragment.WaveformFragment;

public class AudioVisulizationActivity extends BaseActivity {
    private  static String filename;



    public static final String EXTRA_FILE_NAME = "example.audiovisualization.app.EXTRA_FILE_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        filename = getIntent().getStringExtra(EXTRA_FILE_NAME);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_visualization);
        setupToolbar(getString(R.string.audio_visualization_activity), true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.audio_visualization_container, new CustomWaveformFragment())
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
            return filename;
        }

        /**
         * Optional - provide list of segments (start and stop values in seconds) and their corresponding colors
         *
         * @return
         */
        @Override
        protected List<Segment> getSegments() {
            List<Segment> segments = new ArrayList<>();
            segments.add(new Segment(1.0, 4.0, Color.rgb(238, 23, 104)));
            return segments;
        }
    }
}
