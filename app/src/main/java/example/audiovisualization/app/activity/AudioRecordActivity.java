package example.audiovisualization.app.activity;

import android.os.Bundle;

import example.audiovisualization.R;
import example.audiovisualization.app.fragment.AudioRecordFragment;

public class AudioRecordActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        setupToolbar(getString(R.string.audio_record_activity), false);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.audio_record_container, new AudioRecordFragment())
                    .commit();
        }
    }
}
