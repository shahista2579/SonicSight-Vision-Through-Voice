package org.tensorflow.lite.examples.detection.CurrencyDetection;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.examples.detection.Home;
import org.tensorflow.lite.examples.detection.R;
import org.tensorflow.lite.examples.detection.ml.ModelUnquant;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.Objects;


/** @noinspection deprecation*/
public class CurrencyDetection extends AppCompatActivity implements SurfaceHolder.Callback {

    TextView result, confidence;
     Camera camera1;
    SurfaceView surfaceView;
    private static TextToSpeech textToSpeech;
    SurfaceHolder surfaceHolder;
    public static boolean previewing = false;

     int imageSize = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency);
        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
                textToSpeech.setSpeechRate(0.8f);
                textToSpeech.speak("Welcome to currency detection. Press long to return in main menu",TextToSpeech.QUEUE_FLUSH,null);
                textToSpeech.speak("tap on the screen to detect the currency",TextToSpeech.QUEUE_ADD,null);
            }
        });
        result = findViewById(R.id.result);
        confidence = findViewById(R.id.confidence);
         getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = new SurfaceView(this);
        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        surfaceView.setBackgroundResource(R.drawable.chair);


        if(!previewing){

            camera1 = Camera.open();
            if (camera1 != null){
                try {
                    camera1.setDisplayOrientation(90);
                    camera1.setPreviewDisplay(surfaceHolder);
                    camera1.startPreview();
                    Camera.Parameters params = camera1.getParameters();
                    if (params.getSupportedFocusModes().contains(
                            Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }
                    camera1.setParameters(params);
                     previewing = true;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

        }
        }
        surfaceView.setOnClickListener(v -> {
            if(camera1 != null)
            {
                try {
                    camera1.takePicture(myShutterCallback, myPictureCallback_RAW, myPictureCallback_JPG);
                }
                catch (Exception e) {
                    Log.d("kjdne", Objects.requireNonNull(e.getMessage()));
                }

            }
        });
        
        surfaceView.setOnLongClickListener(view -> {
            Toast.makeText(getApplicationContext(), "returning to main menu ", Toast.LENGTH_SHORT).show();
            finish();
            Intent i = new Intent(CurrencyDetection.this, Home.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            return true;
        });


    }
    Camera.ShutterCallback myShutterCallback = () -> {
        // TODO Auto-generated method stub
    };

    Camera.PictureCallback myPictureCallback_RAW = (arg0, arg1) -> {
        // TODO Auto-generated method stub
    };

    Camera.PictureCallback myPictureCallback_JPG = (arg0, arg1) -> {
        // TODO Auto-generated method stub
        Bitmap bitmapPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);

        Bitmap correctBmp = Bitmap.createBitmap(bitmapPicture, 0, 0, bitmapPicture.getWidth(), bitmapPicture.getHeight(), null, true);
        int dimension = Math.min(correctBmp.getWidth(), correctBmp.getHeight());
        correctBmp = ThumbnailUtils.extractThumbnail(correctBmp, dimension, dimension);
        //imageView.setImageBitmap(correctBmp);

        correctBmp = Bitmap.createScaledBitmap(correctBmp, imageSize, imageSize, false);
        classifyImage(correctBmp);


    };


    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub
        if(previewing){
            camera1.stopPreview();
            previewing = false;
        }

        if (camera1 != null){
            try {
                camera1.setPreviewDisplay(surfaceHolder);
                camera1.startPreview();
                Camera.Parameters params = camera1.getParameters();
                if (params.getSupportedFocusModes().contains(
                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }
                camera1.setParameters(params);
                previewing = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }

    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        camera1.stopPreview();
        camera1.release();
        camera1 = null;
        previewing = false;

    }

    /** Explanation for below method classify image
     *The first line creates a new instance of the ModelUnquant object, which is a pre-trained machine learning model for image classification.
     * <p>
     * The next few lines set up the input to the model. First, it creates a TensorBuffer object to hold the input data, with dimensions 1x224x224x3 (one image with height and width of 224 pixels and 3 color channels). It then creates a ByteBuffer object to hold the pixel values of the input image.
     * <p>
     * The pixel values of the input image are extracted using the getPixels() method and stored in an int array. The two for loops iterate over each pixel and extract the red, green, and blue values, which are then converted to float values between 0 and 1 and stored in the ByteBuffer.
     * <p>
     * The input data is loaded into the TensorBuffer using the loadBuffer() method.
     * <p>
     * The model is run on the input data using the process() method of the ModelUnquant object, which returns the model's output.
     * <p>
     * The output is a TensorBuffer containing the predicted confidences for each class. The index of the class with the highest confidence is found using a for loop.
     * <p>
     * The denomination of the currency note is displayed in a TextView object.
     * <p>
     * The confidence values for each class are displayed in another TextView object.
     * <p>
     * The denomination is spoken out loud using the TextToSpeech object.
     * <p>
     * Finally, the camera is opened and set up for previewing, and the surfaceHolder object is passed to the camera for displaying the camera preview.
     */
     public void classifyImage(Bitmap image){
        try {

            ModelUnquant model = ModelUnquant.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            // get 1D array of 224 * 224 pixels in image
            int [] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

            // iterate over pixels and extract R, G, and B values. Add to bytebuffer.
            int pixel = 0;
            for(int i = 0; i < imageSize; i++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            ModelUnquant.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for(int i = 0; i < confidences.length; i++){
                if(confidences[i] > maxConfidence){
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            /**
             * 0 ten rupees
             * 1 hundred rupees
             * 2 two hundred rupees
             * 3 five hundred rupees
             * 4 two thousand rupees
             * 5 fifty rupees
             */

            String[] classes = {"ten rupees", "hundred rupees", "two hundred rupees", "five hundred rupees","two thousand rupees","fifty rupees"};
            result.setText(classes[maxPos]);

            StringBuilder s = new StringBuilder();
            for(int i = 0; i < classes.length; i++){
                s.append(String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100));
            }
            confidence.setText(s.toString());
            textToSpeech.speak(result.getText().toString(),TextToSpeech.QUEUE_FLUSH,null);
            textToSpeech.speak("tap to detect the currency ",TextToSpeech.QUEUE_ADD,null);


            // Releases model resources if no longer used.
            model.close();

                camera1 = Camera.open();
                if (camera1 != null){
                    try {
                        camera1.setDisplayOrientation(90);
                        camera1.setPreviewDisplay(surfaceHolder);
                        camera1.startPreview();
                        Camera.Parameters params = camera1.getParameters();
                        if (params.getSupportedFocusModes().contains(
                                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        }
                        camera1.setParameters(params);
                        previewing = true;
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

        } catch (IOException e) {
            // TODO Handle the exception
        }
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bitmap image = (Bitmap) data.getExtras().get("data");
            int dimension = Math.min(image.getWidth(), image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            //imageView.setImageBitmap(image);

            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            classifyImage(image);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        camera1 = Camera.open();
        if (camera1 != null) {
            try {
                camera1.setDisplayOrientation(90);
                camera1.setPreviewDisplay(surfaceHolder);
                camera1.startPreview();
                Camera.Parameters params = camera1.getParameters();
                if (params.getSupportedFocusModes().contains(
                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }
                camera1.setParameters(params);
                previewing = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (textToSpeech != null) {
            textToSpeech.stop();
        }

    }

}
