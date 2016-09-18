package be.pxl.troger.ar.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import be.pxl.troger.ar.R;

/**
 * The overlay view is responsible for displaying
 * information on top of the camera
 * @author Michael Troger
 */
public class OverlayView extends View {
    /**
     * class name for debugging with logcat
     */
    private static final String TAG = OverlayView.class.getName();
    /**
     * holds the arrow image
     */
    private Drawable arrowright;
    /**
     * holds the arrow image
     */
    private Drawable arrowleft;
    /**
     * holds the arrow image
     */
    private Drawable arrowup;
    /**
     * holds the arrow image
     */
    private Drawable arrowdown;
    /**
     * holds the ring image
     */
    private Drawable ring;
    /**
     * command by which the canvas should be changed
     * e.g. to switch the image
     */
    private String changingType;
    /**
     * the context
     */
    private Context mContext;
    /**
     * the sound of a bear
     */
    private MediaPlayer bearSound;
    /**
     * the sound of a chicken
     */
    private MediaPlayer chickenSound;
    /**
     * time since overlay changed
     */
    private long lastTimeOverlayChanged;
    /**
     * time after barcode detection in which a camera picture is taken
     */
    private static final int SHOW_OVERLAY_TIME = 3000;
    /**
     * reference to object of this class
     */
    private OverlayView mThis;


    /**
     * whether or not the timer is currently running - for displaying image via augmented reality
     */
    private boolean timerRunning;

    /**
     * creates an instance of the OverlayView
     * @param context the context
     * @param attrs the attributes
     */
    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        // preload sounds and images
        loadSounds();
        loadImages();

        mThis = this;

        Log.d(TAG, "started :)");
    }

    /**
     * sounds are preloaded so that they must no be loaded in the main loop
     */
    private void loadSounds() {
        bearSound = MediaPlayer.create(mContext, R.raw.bear);
        chickenSound = MediaPlayer.create(mContext, R.raw.chicken);
    }

    /**
     * images are preloaded so that they must no be loaded in the main loop
     */
    private void loadImages() {
        arrowright = mContext.getResources().getDrawable(R.drawable.right);
        arrowleft = mContext.getResources().getDrawable(R.drawable.left);
        arrowup = mContext.getResources().getDrawable(R.drawable.up);
        arrowdown = mContext.getResources().getDrawable(R.drawable.down);
        ring = mContext.getResources().getDrawable(R.drawable.ring);
    }

    /**
     * called when the view is drawn
     * @param canvas the canvas on which you can draw
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (changingType != null) {
            switch (changingType) { // looking for the chosen command
                case "right":
                    arrowright.setBounds(canvas.getClipBounds());
                    arrowright.draw(canvas);
                    //bearSound.start();
                    break;
                case "left":
                    arrowleft.setBounds(canvas.getClipBounds());
                    arrowleft.draw(canvas);
                    break;
                case "up":
                    arrowup.setBounds(canvas.getClipBounds());
                    arrowup.draw(canvas);
                    break;
                case "down":
                    arrowdown.setBounds(canvas.getClipBounds());
                    arrowdown.draw(canvas);
                    break;
                case "goal":
                    ring.setBounds(canvas.getClipBounds());
                    ring.draw(canvas);
                    bearSound.start();
                    break;
                case "clear":
                    //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    //chickenSound.start();
                    break;
                default:

            }
        }

    }

    /**
     * change the canvas by given command
     * @param changingType the command as String
     */
    public void changeCanvas(String changingType) {
        // force redraw with the given command
        // but only if same command has not been used before
        if (!timerRunning)
        {
            lastTimeOverlayChanged = SystemClock.elapsedRealtime();
            timerRunning = true;
            this.invalidate();
            this.changingType = changingType;

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (lastTimeOverlayChanged >= SHOW_OVERLAY_TIME) {
                        mThis.invalidate();
                        mThis.changingType = "clear";
                        timerRunning = false;
                    }
                }
            }, SHOW_OVERLAY_TIME);
        }


    }

    public boolean isTimerRunning() {
        return timerRunning;
    }


}
