package org.tensorflow.lite.examples.detection.facelogin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.face.Landmark;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import org.tensorflow.lite.examples.detection.Home;
import org.tensorflow.lite.examples.detection.R;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private boolean isLoggedIn = false; // prevent multiple logins

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main11);
        previewView = findViewById(R.id.previewView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview
                androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
                preview.setSurfaceProvider(previewView.createSurfaceProvider());

                // ImageAnalysis for ML Kit
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (Exception e) {
                Log.e("CameraX", "Camera start failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageProxy(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(inputImage)
                .addOnSuccessListener(faces -> {
                    if (!isLoggedIn && !faces.isEmpty()) {
                        Face face = faces.get(0); // use first detected face
                        float[] features = extractFaceFeatures(face);
                        if (features == null) {
                            Log.d("FaceRecognition", "Incomplete landmarks, skipping frame...");
                            imageProxy.close();
                            return;
                        }

                        float[] stored = retrieveStoredFeatures();
                        if (stored != null && matchFeatures(features, stored)) {
                            isLoggedIn = true;
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Face recognized!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(MainActivity.this, Home.class));
                                finish();
                            });
                        } else {
                            saveFeatures(features);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FaceDetection", "Failed: " + e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    @Nullable
    private float[] extractFaceFeatures(Face face) {
        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
        FaceLandmark nose = face.getLandmark(FaceLandmark.NOSE_BASE);
        FaceLandmark mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);

        if (leftEye == null || rightEye == null || nose == null || mouth == null) {
            Log.d("recongitions ---> ", leftEye.toString() + rightEye.toString() + nose.toString() + mouth.toString());
            return null; // One or more landmarks not detected
        }
        Log.d("recongitions ---> ", leftEye + rightEye.toString() + nose + mouth.toString());

        return new float[]{
                leftEye.getPosition().x,
                leftEye.getPosition().y,
                rightEye.getPosition().x,
                rightEye.getPosition().y,
                nose.getPosition().x,
                nose.getPosition().y,
                mouth.getPosition().x,
                mouth.getPosition().y
        };
    }


    private boolean matchFeatures(float[] current, float[] stored) {
        float dist = 0;
        for (int i = 0; i < current.length; i++) {
            dist += (float) Math.pow(current[i] - stored[i], 2);
        }
        Log.d("matchFeatures" , String.valueOf(Math.sqrt(dist)));
        return Math.sqrt(dist) < 6.0; // strict threshold
    }


    private float[] retrieveStoredFeatures() {
        String saved = getSharedPreferences("face_prefs", MODE_PRIVATE)
                .getString("features", null);
        if (saved == null) return null;
        String[] parts = saved.replace("[", "").replace("]", "").split(",");
        float[] arr = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            arr[i] = Float.parseFloat(parts[i].trim());
        }
        return arr;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    // Optional: register face manually using one-time image
    private void saveFeatures(float[] features) {
        getSharedPreferences("face_prefs", MODE_PRIVATE)
                .edit()
                .putString("features", Arrays.toString(features))
                .apply();
    }

    // Request permission result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
