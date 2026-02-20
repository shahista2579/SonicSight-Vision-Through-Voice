package org.tensorflow.lite.examples.detection.ObjectDetection;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.tensorflow.lite.examples.detection.Home;
import org.tensorflow.lite.examples.detection.ObjectDetection.deepmodel.DetectionResult;
import org.tensorflow.lite.examples.detection.ObjectDetection.deepmodel.MobileNetObjDetector;
import org.tensorflow.lite.examples.detection.ObjectDetection.deepmodel.OverlayView;
import org.tensorflow.lite.examples.detection.ObjectDetection.utils.ImageUtils;
import org.tensorflow.lite.examples.detection.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends CameraActivity implements OnImageAvailableListener {
    private static final int MODEL_IMAGE_INPUT_SIZE = 300;
    private static final String LOGGING_TAG = MainActivity.class.getName();
    TextToSpeech textToSpeech;
    private Integer sensorOrientation;
    private int previewWidth = 0;
    private int previewHeight = 0;
    FrameLayout frameLayout;
    private MobileNetObjDetector objectDetector;
    private Bitmap imageBitmapForModel = null;
    private Bitmap rgbBitmapForCameraImage = null;
    private boolean computing = false;
    private Matrix imageTransformMatrix;

    private OverlayView overlayView;

    @Override
    public void onPreviewSizeChosen(final Size previewSize, final int rotation) {
        try {
            objectDetector = MobileNetObjDetector.create(getAssets());
            Log.i(LOGGING_TAG, "Model Initiated successfully.");
            Toast.makeText(getApplicationContext(), "MobileNetObjDetector created", Toast.LENGTH_SHORT).show();
        } catch(IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "MobileNetObjDetector could not be created", Toast.LENGTH_SHORT).show();
            finish();
        }
        overlayView = (OverlayView) findViewById(R.id.overlay);
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status!=TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.US) ;
                    textToSpeech.speak("welcome to object detection",TextToSpeech.QUEUE_FLUSH,null);
                }
            }
        });
        frameLayout = findViewById(R.id.container);
        frameLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                finish();
                Intent i = new Intent(getApplicationContext(), Home.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return false;
            }
        });

        final int screenOrientation = getWindowManager().getDefaultDisplay().getRotation();
        //Sensor orientation: 90, Screen orientation: 0
        sensorOrientation = rotation + screenOrientation;
        Log.i(LOGGING_TAG, String.format("Camera rotation: %d, Screen orientation: %d, Sensor orientation: %d",
                rotation, screenOrientation, sensorOrientation));

        previewWidth = previewSize.getWidth();
        previewHeight = previewSize.getHeight();
        Log.i(LOGGING_TAG, "preview width: " + previewWidth);
        Log.i(LOGGING_TAG, "preview height: " + previewHeight);
        // create empty bitmap
        imageBitmapForModel = Bitmap.createBitmap(MODEL_IMAGE_INPUT_SIZE, MODEL_IMAGE_INPUT_SIZE, Config.ARGB_8888);
        rgbBitmapForCameraImage = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

        imageTransformMatrix = ImageUtils.getTransformationMatrix(previewWidth, previewHeight,
                MODEL_IMAGE_INPUT_SIZE, MODEL_IMAGE_INPUT_SIZE, sensorOrientation,true);
        imageTransformMatrix.invert(new Matrix());
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image imageFromCamera = null;

        try {
            imageFromCamera = reader.acquireLatestImage();
            if (imageFromCamera == null) {
                return;
            }
            if (computing) {
                imageFromCamera.close();
                return;
            }
            computing = true;
            preprocessImageForModel(imageFromCamera);
            imageFromCamera.close();
        } catch (final Exception ex) {
            if (imageFromCamera != null) {
                imageFromCamera.close();
            }
            Log.e(LOGGING_TAG, ex.getMessage());
        }

        runInBackground(() -> {
            final List<DetectionResult> results = objectDetector.detectObjects(imageBitmapForModel);
            overlayView.setResults(results);
            if(!results.isEmpty()) {
                String title = results.get(0).getTitle();
                textToSpeech.speak(title,TextToSpeech.QUEUE_FLUSH, null);
                Toast.makeText(MainActivity.this, results.get(0).toString(), Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(getApplicationContext(), "move forward object is not present.", Toast.LENGTH_SHORT).show();
            }
            //Log.i(LOGGING_TAG, results.get(0).toString());

            /*if(results.size() > 0) {
                String title = results.get(0).getTitle();
                for(int ix = 1 ; ix < results.size() - 1; ix++) {
                    title += ", ";
                    title += results.get(ix).getTitle();
                }
                Toast.makeText(MainActivity.this, title, Toast.LENGTH_LONG).show();
            } */

            requestRender();
            computing = false;
        });
    }

    private void preprocessImageForModel(final Image imageFromCamera) {

        rgbBitmapForCameraImage.setPixels(ImageUtils.convertYUVToARGB(imageFromCamera, previewWidth, previewHeight),
                0, previewWidth, 0, 0, previewWidth, previewHeight);

        new Canvas(imageBitmapForModel).drawBitmap(rgbBitmapForCameraImage, imageTransformMatrix, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (objectDetector != null) {
            objectDetector.close();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (textToSpeech != null) {
            textToSpeech.stop();
        }

    }

}
