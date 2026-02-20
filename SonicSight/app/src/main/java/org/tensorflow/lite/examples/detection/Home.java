package org.tensorflow.lite.examples.detection;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.examples.detection.Calling.CallActivity;
import org.tensorflow.lite.examples.detection.CurrencyDetection.CurrencyDetection;
import org.tensorflow.lite.examples.detection.Message.MessageReader;
import org.tensorflow.lite.examples.detection.Music.Music;
import org.tensorflow.lite.examples.detection.Navigation.Navigation;
import org.tensorflow.lite.examples.detection.Translate.TranslateActivity;
import org.tensorflow.lite.examples.detection.currencygemniai.MainActivity;
import org.tensorflow.lite.examples.detection.voicemail.UserDetailsActivity;

import java.util.ArrayList;
import java.util.Locale;

public class Home extends AppCompatActivity {
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private static int firstTime = 0;
    private ImageView mVoiceInputTv;
    float x1, x2, y1, y2;
    private static TextToSpeech textToSpeech;
    static String Readmessage;
    public static String name;

    // At the top of the Home class
    private static final String CMD_READ = "read";
    private static final String CMD_MESSAGE = "message";
    private static final String CMD_CALCULATOR = "calculator";
    private static final String CMD_TIME_AND_DATE = "time and date";
    private static final String CMD_WEATHER = "weather";
    private static final String CMD_OBJECT = "object";

    private static final String CMD_CURRENCY_DETECTION = "currency";
    private static final String CMD_CALL = "call";
    private static final String CMD_MUSIC = "music";
    private static final String CMD_BATTERY = "battery";
    private static final String CMD_NAVIGATE = "navigat";
    private static final String CMD_QR = "qr";
    private static final String CMD_REMINDER = "reminder";
    private static final String CMD_BANK = "bank";
    private static final String CMD_PHONE = "phone";
    private static final String CMD_TRANSLATE = "translat";
    private static final String CMD_TRANSLATOR = "translator";
    private static final String CMD_NOTE = "note";
    private static final String CMD_MAIL = "mail";
    private static final String CMD_CURRENCY = "currency";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        textToSpeech.setLanguage(Locale.US);
                        textToSpeech.setSpeechRate(0.8f);
                        if (firstTime == 0)
                            textToSpeech.speak("Welcome to Sonic Sight. Swipe right to listen the features of the app and swipe left and say what you want", TextToSpeech.QUEUE_FLUSH, null);
                        //when user return from another activities to main activities.
                        if (firstTime != 0)
                            textToSpeech.speak("you are in main menu. just swipe left and say what you want", TextToSpeech.QUEUE_FLUSH, null);

                    }
                }
            });
        }


        mVoiceInputTv = findViewById(R.id.voiceInput);

    }


    public boolean onTouchEvent(MotionEvent touchEvent) {
        firstTime = 1;
        switch (touchEvent.getAction()) {

            case MotionEvent.ACTION_DOWN:
                x1 = touchEvent.getX();
                y1 = touchEvent.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2 = touchEvent.getX();
                y2 = touchEvent.getY();
                if (x1 < x2) {
                    firstTime = 1;
                    Intent intent = new Intent(Home.this, Features.class);
                    startActivity(intent);
                    break;
                }
                if (x1 > x2) {
                    firstTime = 1;
                    startVoiceInput();

                    break;
                }


                break;
        }

        return false;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            recreate();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            finish();
        }
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
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String voiceText = result.get(0).toLowerCase();
                    mVoiceInputTv.setContentDescription(voiceText);

                    if (voiceText.contains("exit")) {
                        mVoiceInputTv.setContentDescription(null);
                        finishAffinity();
                        System.exit(0);
                        return;
                    }

                    Class<?> activityClass = getAClass(voiceText);

                    if (activityClass != null) {
                        startActivity(new Intent(getApplicationContext(), activityClass));
                        mVoiceInputTv.setContentDescription(null);
                    } else if (voiceText.contains("message")) {
                        Intent intent = new Intent(Home.this, MessageReader.class);
                        if (voiceText.contains("unread") || voiceText.contains("android message")) {
                            Readmessage = "unread message";
                            intent.putExtra("unread message", Readmessage);
                        } else if (voiceText.contains("yesterday") || voiceText.contains("erday")) {
                            Readmessage = "yesterday message";
                            intent.putExtra("yesterday message", Readmessage);
                        } else {
                            Readmessage = "read message";
                            intent.putExtra("read message", Readmessage);
                            textToSpeech.speak("Getting messages , Please wait", TextToSpeech.QUEUE_FLUSH, null);
                        }
                        startActivity(intent);
                    } else {
                        textToSpeech.speak("Do not understand Swipe left Say again", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
            }

        }
    }

    @Nullable
    private static Class<?> getAClass(String voiceText) {
        Class<?> activityClass = null;

        if (voiceText.contains(CMD_READ) && !voiceText.contains(CMD_MESSAGE)) activityClass = OCRReader.class;
        else if (voiceText.contains(CMD_CALCULATOR)) activityClass = Calculator.class;
        else if (voiceText.contains(CMD_TIME_AND_DATE)) activityClass = DateAndTime.class;
        else if (voiceText.contains(CMD_WEATHER)) activityClass = Weather.class;
        else if (voiceText.contains(CMD_CURRENCY_DETECTION)) activityClass = MainActivity.class;
         //else if (voiceText.contains(CMD_OBJECT)) activityClass = MainActivity.class;
        else if (voiceText.contains(CMD_CALL)) activityClass = CallActivity.class;
        else if (voiceText.contains(CMD_MUSIC)) activityClass = Music.class;
        else if (voiceText.contains(CMD_BATTERY)) activityClass = Battery.class;
        else if (voiceText.contains(CMD_NAVIGATE)) activityClass = Navigation.class;
        else if (voiceText.contains(CMD_TRANSLATE) || voiceText.contains(CMD_TRANSLATOR)) activityClass = TranslateActivity.class;
//        else if (voiceText.contains(CMD_NOTE)) activityClass = Notes.class;
        else if (voiceText.contains(CMD_MAIL)) activityClass = UserDetailsActivity.class;
        else if (voiceText.contains(CMD_CURRENCY)) activityClass = CurrencyDetection.class;
        return activityClass;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (textToSpeech != null) {
            textToSpeech.stop();
        }

    }

}
