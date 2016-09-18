package be.pxl.troger.ar.views;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import be.pxl.troger.ar.R;
import be.pxl.troger.ar.tools.BarcodeDatabase;
import be.pxl.troger.ar.MyActivity;


/**
 * A basic Camera preview class
 * @author Michael Troger
 */
public class CameraPreviewView extends SurfaceView implements SurfaceHolder.Callback {
    /**
     * class name for debugging with logcat
     */
    private static final String TAG = CameraPreviewView.class.getName();
    /**
     * the holder of the SurfaceView
     */
    private SurfaceHolder mHolder;
    /**
     * the instance to the active camera
     */
    private Camera mCamera;
    /**
     * reader for the ZXing library
     */
    private MultiFormatReader mMultiFormatReader;
    /**
     * reference to the MyActivity.class
     */
    private MyActivity mContext;
    /**
     * for displaying Toast info messages
     */
    private Toast toast;
    /**
     * time of last frame - for calculating FPS
     */
    private long lastTime;
    /**
     * holds the database to get the command connected to a barcode
     */
    private HashMap<String, String> dataBase;
    /**
     * responsible for displaying images on top of the camera picture
     */
    private OverlayView overlayView;
    /**
     * if the FPS shall be calculated and printed to logcat
     */
    private static final boolean LOG_FPS = true;
    /**
     * if the FPS shall be calculated and printed to logcat
     */
    private static final boolean LOG_MEM_USAGE = true;
    /**
     * whether or not a picture shall be taken after barcode recognition
     */
    private static final boolean TAKE_PICTURE_ON_BARCODE_DETECTION = true;
    /**
     * the preview width of the camera
     */
    private int previewWidth;
    /**
     * the preview height of the camera
     */
    private int previewHeight;
    /**
     * the directory where the taken photos are stored
     */
    private static final String PIC_DIRECTORY = "AR_Barcode_App";
    /**
     * if its safe to take a picture, otherwise it would be tried to take several pictures within
     * a second which would lead to problems
     */
    private boolean safeToTakePicture = false;
    /**
     * record type image
     */
    public static final int MEDIA_TYPE_IMAGE = 1;
    /**
     * record type video
     */
    public static final int MEDIA_TYPE_VIDEO = 2;

    /**
     * time after barcode detection in which a camera picture is taken
     */
    private static final int PICTURE_TAKING_TIMEOUT = 3000;



    /**
     * called when a CameraPreviewView instance is created
     * @param context the context - MyActivity
     * @param camera  the camera to use
     */
    public CameraPreviewView(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        mContext = (MyActivity)context;

        // get the OverlayView responsible for displaying images on top of the camera
        overlayView = (OverlayView) mContext.findViewById(R.id.overlay_view);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        //mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mMultiFormatReader = new MultiFormatReader();

        BarcodeDatabase barDB = new BarcodeDatabase();
        dataBase = barDB.getDataBase();

        Log.d(TAG, "started :)");
    }


    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }


    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    /**
     * manually called when the app is resumed
     */
    private void onResume() {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewCallback(mPreviewCallback);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
        safeToTakePicture = true;
    }


    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
       onResume();
    }


    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            if (LOG_MEM_USAGE) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) mContext.getSystemService(mContext.ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(mi);
                long availableMegs = mi.availMem / 1048576L; // 1024 * 1024
                //Percentage can be calculated for API 16+
                //long percentAvail = mi.availMem / mi.totalMem;
                Log.d(TAG, "available mem: " + availableMegs);
            }

            if (LOG_FPS) { // optionally calc. and log FPS
                long now = SystemClock.elapsedRealtime();
                long frametime = now - lastTime;
                lastTime = now;
                double fps = 1000.0 / frametime;
                Log.i(TAG, "fps:" + fps);
            }

            if (!overlayView.isTimerRunning()) {
                PlanarYUVLuminanceSource source = null;
                source = new PlanarYUVLuminanceSource(
                        data, previewWidth, previewHeight, 0, 0, previewWidth, previewHeight, false
                );


                //Log.d(TAG, "source height:" + source.getHeight());
                //Log.d(TAG, "source width:" + source.getWidth() );
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                Result result = null;
                try {
                    result = mMultiFormatReader.decode(bitmap);
                    String text = result.getText();

                    doSomethingWithBarcodeContent(text);

                }  catch (NotFoundException e) {
                    //e.printStackTrace(); //ignore for now
                }
            }


        }
    };

    /**
     * sets the given camera as the used one
     * @param camera the camera to use
     */
    public void setCamera(Camera camera) {
        mCamera = camera;

        previewWidth = camera.getParameters().getPreviewSize().width;
        previewHeight = camera.getParameters().getPreviewSize().height;

    }

    /**
     * called when the camera is paused
     */
    public void onPause() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        }
        if(toast != null)
            toast.cancel();
    }



    /**
     * displays a TOAST message by making sure
     * Toast is not called multiple times
     * @param message the message to display
     */
    private void displayToast(String message) {
        if(toast != null)
            toast.cancel();
        toast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * strips the line breaks from the given string
     * @param content the string to change
     * @return the string w/o linebreaks
     */
    private static String stripLinebreaks(String content) {
        StringBuilder sb = new StringBuilder(content);
        int i = 0;
        while (i < sb.length()) {
            if (sb.charAt(i) == '\r'
                    || sb.charAt(i) == '\n') {
                sb.deleteCharAt(i);
            } else {
                i++;
            }
        }

        return sb.toString();
    }

    /**
     * makes an logcat/console output with the string detected
     * displays also a TOAST message and finally sends the command to the overlay
     * @param content the content of the detected barcode
     */
    private void doSomethingWithBarcodeContent(String content) {
        Log.d(TAG, "barcode content: " + stripLinebreaks(content)); // for debugging in console
        //displayToast(content);                                      // for debugging when wearing the glasses


        String command = dataBase.get(content);
        Log.d(TAG, "connected command: " + command);
        overlayView.changeCanvas(command);


        if (TAKE_PICTURE_ON_BARCODE_DETECTION &&
                safeToTakePicture) {
            safeToTakePicture = false;
            displayToast("taking photo in " + PICTURE_TAKING_TIMEOUT / 1000 + " seconds");
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    // Actions to do after 10 seconds
                    mCamera.takePicture(null, null, mPicture);
                }
            }, PICTURE_TAKING_TIMEOUT);

        }



    }


    /** Create a file Uri for saving an image or video
     * @param type MEDIA_TYPE_IMAGE for images MEDIA_TYPE_VIDEo for videos
     * @return  returns the file Uri
     */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video
     * @param type  MEDIA_TYPE_IMAGE for images MEDIA_TYPE_VIDEo for videos
     * @return returns the created file
     */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), PIC_DIRECTORY);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(PIC_DIRECTORY, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: "); //+            e.getMessage());
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            onPause();
            onResume();

            displayToast("Photo taken!");
        }
    };
}