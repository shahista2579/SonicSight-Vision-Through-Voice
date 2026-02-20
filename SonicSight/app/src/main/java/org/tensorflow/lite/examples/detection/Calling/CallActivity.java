package org.tensorflow.lite.examples.detection.Calling;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.examples.detection.Home;
import org.tensorflow.lite.examples.detection.R;

import java.util.ArrayList;
import java.util.Locale;

public class CallActivity extends AppCompatActivity {
     TextToSpeech textToSpeech;
    float x1, x2;
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    String phoneno, number;
    String n;
    int ind;
    int abxc = -1;
    private TextView txtScreen2, view;
    static String phonenum;
    static ArrayList<String> a = new ArrayList<>();
    static int counter = 1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        txtScreen2 = findViewById(R.id.txtScreen);
        view = findViewById(R.id.view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        textToSpeech.setLanguage(Locale.US);
                        textToSpeech.setSpeechRate(0.8f);
                        textToSpeech.speak("Welcome to voice calling, tap on the screen, and say the number. press long , to return in main menu", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
            });
        }

        if (checkIfAlreadyhavePermission()) {
            Toast.makeText(getApplicationContext(), "Permission is granted", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(CallActivity.this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    1);
        }

        final GestureDetector gesturedt = new GestureDetector(CallActivity.this, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                super.onSingleTapUp(e);
                if (!textToSpeech.isSpeaking()) {
                    startVoiceInput();
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                Intent i = new Intent(getApplicationContext(), Home.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!textToSpeech.isSpeaking()) {
                    startVoiceInput();
                }
            }
        });


        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, PackageManager.PERMISSION_GRANTED);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 0);
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                phoneno = res.get(0).toLowerCase(Locale.ROOT).replaceAll("\\s", "");
                if (phoneno.contains("0") || phoneno.contains("1") || phoneno.contains("2") || phoneno.contains("3") || phoneno.contains("4") || phoneno.contains("5")
                        || phoneno.contains("6") || phoneno.contains("9") || phoneno.contains("7") || phoneno.contains("8")) {
                    @SuppressLint("Recycle")
                    Cursor cursor = this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

                    if (cursor.moveToFirst()) {
                        do {
                            String Name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            String Num = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            a.add(Name.toLowerCase() + "@" + Num);
                        } while (cursor.moveToNext());
                    }
                    ind = a.size();


                    for (int i = 0; i < ind - 1; i++) {
                        if (a.get(i).contains(phoneno)) {
                            abxc = i;
                            break;
                        }
                    }
                    if (abxc == -1) {
                        phonenum = phoneno;
                        txtScreen2.setText(phonenum);
                        String separated = String.join(",", phonenum.split(""));
                        textToSpeech.speak("Phone Number is !!!! " + separated, TextToSpeech.QUEUE_FLUSH, null);
                        textToSpeech.speak("swipe left and, say yes to confirm or, Say No to talk back ", TextToSpeech.QUEUE_ADD, null);

                        Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT).show();
                    } else {
                        n = a.get(abxc);
                        String[] d = n.split("@");
                        number = "Name is " + d[0] + " \n" + "Phone number is " + d[1];
                        phonenum = d[1];
                        if (d[0].isEmpty()) {
                            phonenum = phoneno;
                        }
                        txtScreen2.setText(number);
                        String name = d[1];
                        String separated = String.join(",", name.split(""));
                        textToSpeech.speak("Phone Number is !!!! " + separated + ", and name is," + d[0], TextToSpeech.QUEUE_FLUSH, null);
                        textToSpeech.speak("swipe left and, say yes to confirm or, Say No to talk back ", TextToSpeech.QUEUE_ADD, null);

                    }

                } else if (phoneno.contains("yes")) {
                    textToSpeech.speak("calling", TextToSpeech.QUEUE_FLUSH, null);
                    final Handler h = new Handler();
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent i = new Intent(Intent.ACTION_CALL);
                            i.setData(Uri.parse("tel:" + phonenum));
                            startActivity(i);
                        }
                    }, 2000);

                } else if (phoneno.contains("no")) {
                    textToSpeech.speak("swipe left and say the number", TextToSpeech.QUEUE_FLUSH, null);
                }
                else if (phoneno.contains("back")) {
                     startActivity(new Intent(this, Home.class));
                }
            }
        }

    }

    private boolean checkIfAlreadyhavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);
        return result == PackageManager.PERMISSION_GRANTED;
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
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(getApplicationContext(), "permission granted  ", Toast.LENGTH_SHORT).show();
            } else {

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(CallActivity.this, "Permission denied  ", Toast.LENGTH_SHORT).show();
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}