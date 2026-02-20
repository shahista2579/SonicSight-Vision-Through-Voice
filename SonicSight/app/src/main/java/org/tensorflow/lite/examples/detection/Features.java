package org.tensorflow.lite.examples.detection;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.examples.detection.Calling.CallActivity;
import org.tensorflow.lite.examples.detection.CurrencyDetection.CurrencyDetection;
import org.tensorflow.lite.examples.detection.Location.LocationActivity;
import org.tensorflow.lite.examples.detection.Message.MessageReader;
import org.tensorflow.lite.examples.detection.Music.Music;
import org.tensorflow.lite.examples.detection.Navigation.Navigation;
import org.tensorflow.lite.examples.detection.Translate.TranslateActivity;
import org.tensorflow.lite.examples.detection.currencygemniai.MainActivity;

import java.util.ArrayList;
import java.util.Locale;

public class Features extends AppCompatActivity {
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private TextView mVoiceInputTv;
    float x1, x2, y1, y2;
    private static TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_features);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
                textToSpeech.setSpeechRate(0.8f);
                textToSpeech.speak("say read for read, " +
                        "calculator for calculator, " +
                        "Weather for weather, " +
                        "Location for location, " +
                        "Battery, Time and date." +
                        "Say navigation to navigate to the destination. " +
                        "Say message to read the messages. " +
                        "Say music to listen songs. " +
                        "Say back to return to Home screen." +
                        "Say translate to translate open translator." +
                        "say currency detection to detect the currency." +
                        " say exit for closing the application.  Swipe left and say what you want ", TextToSpeech.QUEUE_FLUSH, null);
            }
        });
        mVoiceInputTv = (TextView) findViewById(R.id.voiceInput);


    }


    public boolean onTouchEvent(MotionEvent touchEvent) {
        switch (touchEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = touchEvent.getX();
                y1 = touchEvent.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2 = touchEvent.getX();
                y2 = touchEvent.getY();
                if (x1 > x2) {
                    textToSpeech.stop();
                    startVoiceInput();
                }
                break;
        }
        return false;
    }


    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            a.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mVoiceInputTv.setText(result.get(0));

                if (mVoiceInputTv.getText().toString().contains("read") && !mVoiceInputTv.getText().toString().contains("message")) {
                    Intent intent = new Intent(getApplicationContext(), OCRReader.class);
                    startActivity(intent);
                    mVoiceInputTv.setText(null);
                } else if (mVoiceInputTv.getText().toString().contains("calculator")) {
                    Intent intent = new Intent(getApplicationContext(), Calculator.class);
                    startActivity(intent);
                    mVoiceInputTv.setText(null);
                } else if (mVoiceInputTv.getText().toString().contains("time and date")) {
                    Intent intent = new Intent(getApplicationContext(), DateAndTime.class);
                    startActivity(intent);
                    mVoiceInputTv.setText(null);
                } else if (mVoiceInputTv.getText().toString().contains("weather")) {
                    Intent intent = new Intent(getApplicationContext(), Weather.class);
                    startActivity(intent);
                    mVoiceInputTv.setText(null);
                } else if (mVoiceInputTv.getText().toString().contains("message")) {
                    Intent intent = new Intent(getApplicationContext(), MessageReader.class);
                    startActivity(intent);
                    mVoiceInputTv.setText(null);

                } else if (mVoiceInputTv.getText().toString().contains("call")) {
                    Intent intent = new Intent(getApplicationContext(), CallActivity.class);
                    startActivity(intent);
                    mVoiceInputTv.setText(null);
                } else if (mVoiceInputTv.getText().toString().contains("music")) {
                    Intent intent = new Intent(getApplicationContext(), Music.class);
                    startActivity(intent);
                    mVoiceInputTv.setText(null);
                } else if (mVoiceInputTv.getText().toString().contains("back")) {
                    mVoiceInputTv.setText(null);
                    startActivity(new Intent(this, Home.class));
                } else if (mVoiceInputTv.getText().toString().contains("battery")) {
                    Intent intent = new Intent(getApplicationContext(), Battery.class);
                    startActivity(intent);
                    mVoiceInputTv.setText(null);

                } else if (mVoiceInputTv.getText().toString().contains("navigat")) {
                    Intent intent = new Intent(getApplicationContext(), Navigation.class);
                    startActivity(intent);
                    mVoiceInputTv.setText(null);

                } else if (mVoiceInputTv.getText().toString().contains("currency")) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    mVoiceInputTv.setText(null);

                }
                else if (mVoiceInputTv.getText().toString().contains("location")) {
                    Intent intent = new Intent(getApplicationContext(), LocationActivity.class);
                    startActivity(intent);
                    mVoiceInputTv.setText(null);

                }
                else if (mVoiceInputTv.getText().toString().contains("exit")) {
                    onPause();
                    finishAffinity();
                } else if (mVoiceInputTv.getText().toString().contains("translat") || mVoiceInputTv.getText().toString().contains("translator")) {
                    Intent intent = new Intent(getApplicationContext(), TranslateActivity.class);
                    startActivity(intent);
                    mVoiceInputTv.setText(null);

//                } else if (mVoiceInputTv.getText().toString().contains("note")) {
//                    Intent intent = new Intent(getApplicationContext(), Notes.class);
//                    startActivity(intent);
//                    mVoiceInputTv.setText(null);
//
//                } else {
                    textToSpeech.speak("Do not understand Swipe left Say again", TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        }
    }

    public void onDestroy() {
        if (mVoiceInputTv.getText().toString().contains("exit")) {
            finish();
        }
        super.onDestroy();
    }

    public void onPause() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        super.onPause();

    }
}

