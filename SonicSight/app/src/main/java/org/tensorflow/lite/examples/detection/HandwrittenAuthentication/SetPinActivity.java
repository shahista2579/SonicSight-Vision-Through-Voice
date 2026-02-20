package org.tensorflow.lite.examples.detection.HandwrittenAuthentication;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.examples.detection.Home;

import java.util.Locale;

public class SetPinActivity extends AppCompatActivity implements StrokeManager.PinRecognizedListener {
    private TextToSpeech textToSpeech;
    private DigitalInkMainActivity digitalInkDialog;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_PIN = "user_pin";
    private static final String KEY_LOGGED_IN = "user_logged_in";
    private boolean isFirstTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check SharedPreferences for login state
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isFirstTime = !prefs.getBoolean(KEY_LOGGED_IN, false);
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.getDefault());
                    textToSpeech.setSpeechRate(0.7f);
                    if (isFirstTime) {
                        textToSpeech.speak("Please set your 4-digit PIN.", TextToSpeech.QUEUE_FLUSH, null);
                    } else {
                        textToSpeech.speak("Please enter your 4-digit PIN to unlock.", TextToSpeech.QUEUE_FLUSH, null);
                    }
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {

                        }

                        @Override
                        public void onDone(String utteranceId) {
                            if (utteranceId.equals("incorrect_pin")) {
                                digitalInkDialog.strokeManager.clearPinList();
                            }
                        }

                        @Override
                        public void onError(String utteranceId) {

                        }
                    });
                }
            }
        });
        digitalInkDialog = new DigitalInkMainActivity(this, 0); // 0 for PIN
        digitalInkDialog.strokeManager.setPinRecognizedListener(this);
        digitalInkDialog.show();
    }

    @Override
    public void onPinRecognized(String pin) {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        if (isFirstTime) {
            if (pin != null && pin.matches("\\d{4}")) {
                editor.putString(KEY_PIN, pin);
                editor.putBoolean(KEY_LOGGED_IN, true);
                editor.apply();
                textToSpeech.speak("PIN set successfully. Redirecting to Home.", TextToSpeech.QUEUE_FLUSH, null);
                Intent intent = new Intent(this, Home.class);
                startActivity(intent);
                finish();
            } else {
                textToSpeech.speak("Invalid PIN. Please enter a 4-digit PIN.", TextToSpeech.QUEUE_FLUSH, null,"incorrect_pin");
            }
        } else {
            String savedPin = prefs.getString(KEY_PIN, "");
            if (pin != null && pin.equals(savedPin)) {
                textToSpeech.speak("PIN correct. Welcome!", TextToSpeech.QUEUE_FLUSH, null);
                Intent intent = new Intent(this, Home.class);
                startActivity(intent);
                finish();
            } else {
                textToSpeech.speak("Incorrect PIN. Please try again.", TextToSpeech.QUEUE_FLUSH, null,"incorrect_pin");
            }
        }
    }
}
