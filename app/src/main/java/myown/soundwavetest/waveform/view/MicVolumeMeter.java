package myown.soundwavetest.waveform.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.media.MediaRecorder;
import android.view.View;

/**
 * Created by Olev on 19/02/2016.
 */
public class MicVolumeMeter extends View {

    int a = 100;
    int r = 100;
    int g = 100;
    int b = 100;
    int x;
    int y;
    int baseWidth;
    int nwidth;
    int height;
    Paint color = new Paint();
    MediaRecorder recorder;
    Rect rect = new Rect(x, y, x + (baseWidth * nwidth) , y + height);
    ShapeDrawable shapeDrawable = new ShapeDrawable();

    public MicVolumeMeter(Context context, MediaRecorder recorder) {
        super(context);
        this.recorder = recorder;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        changeColorAndSize();
        initRecorder();
        shapeDrawable.setBounds(rect);
        shapeDrawable.getPaint().set(color);
        shapeDrawable.draw(canvas);
    }

    private void initRecorder() {
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        recorder.setOutputFile();
    }

    private void changeColorAndSize() {
        double amp = getAmplitude();
        nwidth =  (int)amp;
        color.setARGB(a, r * nwidth, g, b);
    }

    public double getAmplitude() {
        if(recorder != null) {
            return recorder.getMaxAmplitude();
        }
        return 0;
    }
}
