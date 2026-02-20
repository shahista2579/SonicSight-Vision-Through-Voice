package org.tensorflow.lite.examples.detection.ObjectDetection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.examples.detection.ObjectDetection.deepmodel.OverlayView;
import org.tensorflow.lite.examples.detection.R;


public abstract class CameraActivity extends AppCompatActivity
        implements OnImageAvailableListener {
    private static final String LOGGING_TAG = "objdetector";
    private static final int PERMISSIONS_REQUEST = 1;

    private Handler handler;
    private HandlerThread handlerThread;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(null);
      //  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera1);

        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        setContentView(R.layout.activity_camera1);
        setFragment();
    }

    @Override
    public synchronized void onPause() {
//        if (!isFinishing()) {
//            finish();
//        }

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException ex) {
            Log.e(LOGGING_TAG, "Exception: " + ex.getMessage());
        }

        super.onPause();
    }

    protected synchronized void runInBackground(final Runnable runnable) {
        if (handler != null) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    handler.post(runnable);
                }
            }, 3000);

        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setFragment();
            }
        }
    }

    private boolean hasPermission() {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(CameraActivity.this,
                    "Camera AND storage permission are required for this app", Toast.LENGTH_LONG).show();
        }
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
    }

    protected void setFragment() {
        CameraConnectionFragment cameraConnectionFragment = new CameraConnectionFragment();
        cameraConnectionFragment.addConnectionListener(CameraActivity.this::onPreviewSizeChosen);
        cameraConnectionFragment.addImageAvailableListener(this);

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, cameraConnectionFragment)
                .commit();
    }

    public void requestRender() {
        final OverlayView overlay = findViewById(R.id.overlay);
        if (overlay != null) {
            overlay.postInvalidate();
        }
    }

    public void addCallback(final OverlayView.DrawCallback callback) {
        final OverlayView overlay = findViewById(R.id.overlay);
        if (overlay != null) {
            overlay.addCallback(callback);
        }
    }

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);
}