package org.tensorflow.lite.examples.detection.voicemail;

import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.examples.detection.R;

public class OpenActivity extends AppCompatActivity {
    private static final int Splash_time = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent HomeIntent = new Intent(OpenActivity.this,UserDetailsActivity.class);
                startActivity(HomeIntent);
                finish();
            }

            },Splash_time);
    }
}
