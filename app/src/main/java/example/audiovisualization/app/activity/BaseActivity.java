package example.audiovisualization.app.activity;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import example.audiovisualization.R;

public class BaseActivity extends AppCompatActivity {

    public Toolbar toolbar;

    public void setupToolbar(final String title, final boolean enableUpNavigation) {
        final Toolbar toolbar = getToolbar();
        if (toolbar == null) {
            return;
        }
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(enableUpNavigation);
        }
    }

    public Toolbar getToolbar() {
        if (toolbar == null) {
            toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
        }
        return toolbar;
    }

    public void setToolbarTitle(String name) {
        final Toolbar toolbar = getToolbar();
        if (toolbar == null) {
            return;
        }
        toolbar.setTitle(name);
    }
}
