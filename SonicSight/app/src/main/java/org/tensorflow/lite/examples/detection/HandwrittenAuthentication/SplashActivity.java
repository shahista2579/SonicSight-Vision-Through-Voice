package org.tensorflow.lite.examples.detection.HandwrittenAuthentication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.tensorflow.lite.examples.detection.R;

public class SplashActivity extends Activity {
    private static final int STORAGE_PERMISSION_CODE = 1001;
    private ModelManager modelManager;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Optionally set a splash layout here
         setContentView(R.layout.activity_splash);

        modelManager = new ModelManager();
        if (hasStoragePermission()) {
            showProgressDialog();
            downloadModel();
        } else {
            requestStoragePermission();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                new String[] {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO
                },
                STORAGE_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                showProgressDialog();
                downloadModel();
            } else {
                Toast.makeText(this, "Storage permission is required to download the model.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void showProgressDialog() {
        progressDialog = new AlertDialog.Builder(this)
                .setTitle("Preparing App")
                .setMessage("Downloading handwriting recognition model...")
                .setCancelable(false)
                .create();
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void downloadModel() {
        modelManager.download()
            .addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String s) {
                    hideProgressDialog();
                    goToMain();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    hideProgressDialog();
                    goToMain();
                    Toast.makeText(SplashActivity.this, "Model download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Optionally, retry or close app
                }
            });
    }

    private void goToMain() {
        Intent intent = new Intent(this, SetPinActivity.class);
        startActivity(intent);
        finish();
    }
}
