package org.tensorflow.lite.examples.detection.currencygemniai;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import androidx.camera.view.PreviewView;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.FutureCallback;
import com.google.ai.client.generativeai.GenerativeModel;

import org.tensorflow.lite.examples.detection.R;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GeminiTest";
    private Executor executor = Executors.newSingleThreadExecutor();
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private MutableLiveData<Bitmap> bitmap = new MutableLiveData<>();

    private TextToSpeech textToSpeech;

    private ProgressDialog progressDialog;

    private CameraOverlayView cameraOverlayView;

    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int STORAGE_PERMISSION_REQUEST = 1002;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main9);

        cameraOverlayView = findViewById(R.id.cameraOverlayView);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing...");
        progressDialog.setCancelable(false);

        Button pickImageButton = findViewById(R.id.pickImageButton);
        pickImageButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST);
            } else {
                openImagePicker();
            }
        });
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                textToSpeech.setSpeechRate(0.8f);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported");
                }
            } else {
                Log.e(TAG, "Initialization failed");
            }
        });
        previewView = findViewById(R.id.previewView);

        // Request camera permission at runtime
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            // requestPermissions();
        }

        bitmap.observe(this, bmp -> {
            if (bmp != null) {
                generateResult(bmp);
            } else {
                Toast.makeText(MainActivity.this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            }
        });
        previewView.setOnClickListener(v -> {
            captureImage();
//            if (bitmap != null) generateResult(bitmap);
//            else
//                Toast.makeText(MainActivity.this, "Failed to capture image", Toast.LENGTH_SHORT).show();
        });

    }


    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage));
                    generateResult(bitmap);
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
            Toast.makeText(this, "Image picked: " + selectedImage, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                openImagePicker();
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    private void generateResult(Bitmap bitmap) {
        progressDialog.show();
        GenerativeModel gm = new GenerativeModel("models/gemma-3-12b-it", "AIzaSyBi1sTuXAyJo_KT3OsUHX0Z8--rAOhplMg");
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder()
                .addImage(bitmap)
                .addText("what is value of this currency in rupees image answer in one word?")
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                progressDialog.dismiss();
                String resultText = result.getText();
                Log.d(TAG, "AI Response: " + resultText);
                runOnUiThread(() -> Toast.makeText(
                        MainActivity.this,
                        "AI Response: " + resultText,
                        Toast.LENGTH_LONG
                ).show());
                if (resultText == null || resultText.trim().isEmpty()) {
                    textToSpeech.speak("The image is not valid. Please provide a currency image.", TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    textToSpeech.speak("The value of this currency is " + resultText + " rupees.", TextToSpeech.QUEUE_FLUSH, null, null);
                }
                TextView responseTextView = findViewById(R.id.aiResponseText);
                runOnUiThread(() -> {
                            responseTextView.setText(resultText);
                            cameraOverlayView.setTextToDraw(resultText);
                        }
                );
                MainActivity.this.bitmap.postValue(null);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "AI Generation Failed", t);
            }
        }, executor);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == 1) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission granted
//                startCamera();
//            } else {
//                // Permission denied
//                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Create Preview Use Case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.createSurfaceProvider());

                // Create ImageCapture Use Case
                imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind any use cases before rebinding
                cameraProvider.unbindAll();

                // Bind Preview and ImageCapture use cases
                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Camera initialization failed.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureImage() {
        if (imageCapture == null) {
            return;
        }

//        // Define output file options
//        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(
//                getContentResolver(),
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                new ContentValues()
//        ).build();

        // Take a picture
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                // Convert ImageProxy to Bitmap
                bitmap.postValue(imageProxyToBitmap(image));
                image.close();
            }

            @Override
            public void onError(ImageCaptureException exception) {
                super.onError(exception);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        // Convert the image to bytes
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        // Convert byte array to Bitmap
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Release camera when the activity is paused
        if (cameraProviderFuture != null) {
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            cameraProvider.unbindAll();
        }
    }
}
