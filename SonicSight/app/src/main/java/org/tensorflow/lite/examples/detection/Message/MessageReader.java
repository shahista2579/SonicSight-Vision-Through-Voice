package org.tensorflow.lite.examples.detection.Message;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.examples.detection.Home;
import org.tensorflow.lite.examples.detection.R;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MessageReader extends AppCompatActivity {
    private static final int TYPE_INCOMING_MESSAGE = 1;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private ListView messageList;
    private MessageListAdapter messageListAdapter;
    private ArrayList<Message> recordsStored;
    private ArrayList<Message> listInboxMessages;
    private ProgressDialog progressDialogInbox;
    private CustomHandler customHandler;
    private TextToSpeech textTospeech;
    static ArrayList<String> messagee = new ArrayList<>();
    static ArrayList<String> unreadmessagee = new ArrayList<>();
    static ArrayList<String> yesterdaymessage = new ArrayList<>();
    float x1, x2, y1, y2;
    static String msg;
    private static int i = 0;
    TextView textView;
    private static final int YESTERDAY_UNREAD_SMS = 24 * 60 * 60 * 1000;
    static String readmessage, unreadmessage, yesterdaymessagee;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_reader);
        textView = findViewById(R.id.list_textview);
        if (getIntent().getStringExtra("read message") == null) {
            readmessage = "read message";
        } else {
            readmessage = getIntent().getStringExtra("read message");
        }
//        if (getIntent().getStringExtra("unread message") == null) {
//            unreadmessage = "unread message";
//        } else {
//            unreadmessage = getIntent().getStringExtra("unread message");
//        }
//        if (getIntent().getStringExtra("yesterday message") == null) {
//            yesterdaymessagee = "yesterday message";
//        } else {
//            yesterdaymessagee = getIntent().getStringExtra("yesterday message");
//        }
        textTospeech = new TextToSpeech(MessageReader.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textTospeech.setLanguage(Locale.US);
                    textTospeech.setSpeechRate(0.8f);
                    textTospeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {

                        }

                        @Override
                        public void onDone(String utteranceId) {
                            runOnUiThread(() -> new Handler().postDelayed(
                                    () -> {
                                        Messages();
                                        progressDialogInbox.dismiss();
                                    }, 3000));
//                            Messages();
//                            progressDialogInbox.dismiss();

                        }

                        @Override
                        public void onError(String utteranceId) {

                        }
                    });
                    textTospeech.speak("reading messages please wait. press long to return in main menu", TextToSpeech.QUEUE_FLUSH, null, "ddd");
                    textView.setText("reading messages please wait");

                }
            }
        });

        textView.setOnTouchListener((v, touchEvent) -> {
            final int SWIPE_THRESHOLD = 100; // Minimum distance for swipe
            switch (touchEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x1 = touchEvent.getX();
                    y1 = touchEvent.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    x2 = touchEvent.getX();
                    y2 = touchEvent.getY();
                    float deltaY = y1 - y2;
                    if (deltaY > SWIPE_THRESHOLD) {
                        if (readmessage != null && readmessage.equals("read message")) {
                            if (i < messagee.size()) {
                                textTospeech.speak(messagee.get(i), TextToSpeech.QUEUE_FLUSH, null);
                                textTospeech.speak("swipe up to read next messages", TextToSpeech.QUEUE_ADD, null);
                                i++;
                                Toast.makeText(getApplicationContext(), String.valueOf(i), Toast.LENGTH_SHORT).show();
                            } else {
                                textView.setText("All messages have been read");
                                textTospeech.speak("All messages have been read", TextToSpeech.QUEUE_FLUSH, null);
                                textTospeech.speak("returning to main menu", TextToSpeech.QUEUE_ADD, null);
                                i = 0;
                                final Handler h = new Handler(Looper.getMainLooper());
                                h.postDelayed(() -> {
                                    messagee.clear();
                                    startActivity(new Intent(MessageReader.this, Home.class));
                                    finish();
                                }, 4500);
                            }
                        }
                        if (unreadmessage != null && unreadmessage.equals("unread message")) {
                            if (i < unreadmessagee.size()) {
                                textTospeech.speak(unreadmessagee.get(i), TextToSpeech.QUEUE_FLUSH, null);
                                i++;
                                textTospeech.speak("swipe up to read next messages", TextToSpeech.QUEUE_ADD, null);
                            } else {
                                textView.setText("All messages have been read");
                                textTospeech.speak("All messages have been read.", TextToSpeech.QUEUE_FLUSH, null);
                                textTospeech.speak("returning to main menu", TextToSpeech.QUEUE_ADD, null);
                                i = 0;
                                final Handler h = new Handler();
                                h.postDelayed(() -> {
                                    unreadmessagee.clear();
                                    startActivity(new Intent(MessageReader.this, Home.class));
                                    finish();
                                }, 6000);
                            }
                        }
                        if (yesterdaymessagee != null && yesterdaymessagee.equals("yesterday message")) {
                            if (i < yesterdaymessage.size()) {
                                textTospeech.speak(yesterdaymessage.get(i), TextToSpeech.QUEUE_FLUSH, null);
                                textTospeech.speak("swipe up to read next messages", TextToSpeech.QUEUE_ADD, null);
                                i++;
                            } else {
                                textView.setText("All messages have been read");
                                textTospeech.speak("All messages have been read.", TextToSpeech.QUEUE_FLUSH, null);
                                textTospeech.speak("returning to main menu", TextToSpeech.QUEUE_ADD, null);
                                i = 0;
                                final Handler h = new Handler(Looper.getMainLooper());
                                h.postDelayed(() -> {
                                    yesterdaymessage.clear();
                                    startActivity(new Intent(MessageReader.this, Home.class));
                                    finish();
                                }, 5500);
                            }
                        }
                    }
                    break;
            }
            return false;
        });

        textView.setOnLongClickListener(view -> {
            Intent i = new Intent(MessageReader.this, Home.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            return true;
        });
        if (checkIfAlreadyhavePermission()) {
            initViews();
        } else {
            requestSmsPermission();
        }
    }

    private boolean checkIfAlreadyhavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_SMS},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initViews();
                Toast.makeText(getApplicationContext(), "Permission granted ... Reading messages", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied to read your SMS messages", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Home.class));
            }
        }
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        if (checkIfAlreadyhavePermission()) {
//            populateMessageList();
//        }
//    }

    public void initViews() {
        customHandler = new CustomHandler(MessageReader.this);
        progressDialogInbox = new ProgressDialog(this);

        recordsStored = new ArrayList<>();

        messageList = findViewById(R.id.messageList);
        populateMessageList();
    }

    public void Messages() {
        if (readmessage != null && readmessage.equals("read message")) {
            if (!messagee.isEmpty()) {
                textTospeech.speak(messagee.get(0), TextToSpeech.QUEUE_FLUSH, null);
                i++;
                textTospeech.speak("swipe up to read next messages ", TextToSpeech.QUEUE_ADD, null);
                textView.setText("swipe up to read next messages");

            }
            if (messagee.isEmpty()) {
                textView.setText("You don't have any messages for today");
                textTospeech.speak("You don't have any messages for today", TextToSpeech.QUEUE_FLUSH, null);
                textTospeech.speak("returning to main menu", TextToSpeech.QUEUE_ADD, null);
                final Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopThread();
                        unreadmessagee.clear();
                        messagee.clear();
                        messageListAdapter.clear();
                        Intent i = new Intent(MessageReader.this, Home.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        finish();
                    }
                }, 5000);

            }
        }

        if (unreadmessage != null && unreadmessage.equals("unread message")) {
            if (unreadmessagee.size() > 0) {
                textTospeech.speak(unreadmessagee.get(i), TextToSpeech.QUEUE_FLUSH, null);
                i++;
                textTospeech.speak("swipe up to read next message ", TextToSpeech.QUEUE_ADD, null);
                textView.setText("swipe up to read next messages");

            }
            if (unreadmessagee.size() == 0) {
                textView.setText("You don't have any unread messages for today");
                textTospeech.speak("You don't have any unread messages for today", TextToSpeech.QUEUE_FLUSH, null);
                textTospeech.speak("returning to main menu", TextToSpeech.QUEUE_ADD, null);
                final Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        i = 0;
                        unreadmessagee.clear();
                        messagee.clear();
                        Intent i = new Intent(MessageReader.this, Home.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        finish();
                    }
                }, 5000);
            }

        }

        if (yesterdaymessagee != null && yesterdaymessagee.equals("yesterday message")) {
            if (yesterdaymessage.size() > 0) {
                textTospeech.speak(yesterdaymessage.get(0), TextToSpeech.QUEUE_FLUSH, null);
                i++;
                textTospeech.speak("swipe up to read next messages ", TextToSpeech.QUEUE_ADD, null);
                textView.setText("swipe up to read next messages");

            }
            if (yesterdaymessage.size() == 0) {
                textView.setText("You don't have any messages for yesterday");
                textTospeech.speak("You don't have any messages for yesterday", TextToSpeech.QUEUE_FLUSH, null);
                textTospeech.speak("returning to main menu", TextToSpeech.QUEUE_ADD, null);
                final Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        unreadmessagee.clear();
                        messagee.clear();
                        yesterdaymessage.clear();
                        messageListAdapter.clear();
                        Intent i = new Intent(MessageReader.this, Home.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        ;
                        finish();
                    }
                }, 5000);

            }
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent touchEvent) {
        super.onTouchEvent(touchEvent);
        final int SWIPE_THRESHOLD = 100; // Minimum distance for swipe
        switch (touchEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = touchEvent.getX();
                y1 = touchEvent.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2 = touchEvent.getX();
                y2 = touchEvent.getY();
                float deltaY = y1 - y2;
                if (deltaY > SWIPE_THRESHOLD) {
                    if (readmessage != null && readmessage.equals("read message")) {
                        if (i < messagee.size()) {
                            textTospeech.speak(messagee.get(i), TextToSpeech.QUEUE_FLUSH, null);
                            textTospeech.speak("swipe up to read next messages", TextToSpeech.QUEUE_ADD, null);
                            i++;
                            Toast.makeText(getApplicationContext(), String.valueOf(i), Toast.LENGTH_SHORT).show();
                        } else {
                            textView.setText("All messages have been read");
                            textTospeech.speak("All messages have been read", TextToSpeech.QUEUE_FLUSH, null);
                            textTospeech.speak("returning to main menu", TextToSpeech.QUEUE_ADD, null);
                            i = 0;
                            final Handler h = new Handler(Looper.getMainLooper());
                            h.postDelayed(() -> {
                                messagee.clear();
                                startActivity(new Intent(MessageReader.this, Home.class));
                                finish();
                            }, 4500);
                        }
                    }
                    if (unreadmessage != null && unreadmessage.equals("unread message")) {
                        if (i < unreadmessagee.size()) {
                            textTospeech.speak(unreadmessagee.get(i), TextToSpeech.QUEUE_FLUSH, null);
                            i++;
                            textTospeech.speak("swipe up to read next messages", TextToSpeech.QUEUE_ADD, null);
                        } else {
                            textView.setText("All messages have been read");
                            textTospeech.speak("All messages have been read.", TextToSpeech.QUEUE_FLUSH, null);
                            textTospeech.speak("returning to main menu", TextToSpeech.QUEUE_ADD, null);
                            i = 0;
                            final Handler h = new Handler();
                            h.postDelayed(() -> {
                                unreadmessagee.clear();
                                startActivity(new Intent(MessageReader.this, Home.class));
                                finish();
                            }, 6000);
                        }
                    }
                    if (yesterdaymessagee != null && yesterdaymessagee.equals("yesterday message")) {
                        if (i < yesterdaymessage.size()) {
                            textTospeech.speak(yesterdaymessage.get(i), TextToSpeech.QUEUE_FLUSH, null);
                            textTospeech.speak("swipe up to read next messages", TextToSpeech.QUEUE_ADD, null);
                            i++;
                        } else {
                            textView.setText("All messages have been read");
                            textTospeech.speak("All messages have been read.", TextToSpeech.QUEUE_FLUSH, null);
                            textTospeech.speak("returning to main menu", TextToSpeech.QUEUE_ADD, null);
                            i = 0;
                            final Handler h = new Handler(Looper.getMainLooper());
                            h.postDelayed(() -> {
                                yesterdaymessage.clear();
                                startActivity(new Intent(MessageReader.this, Home.class));
                                finish();
                            }, 5500);
                        }
                    }
                }
                break;
        }
        return false;
    }


    public void populateMessageList() {
        fetchInboxMessages();
        messageListAdapter = new MessageListAdapter(this,
                R.layout.message_list_item, recordsStored);
        messageList.setAdapter(messageListAdapter);
        messageListAdapter.notifyDataSetChanged();
    }

    private void showProgressDialog() {
        progressDialogInbox.setMessage("Fetching Inbox Messages...");

        progressDialogInbox.setIndeterminate(true);
        progressDialogInbox.setCancelable(true);
        progressDialogInbox.show();
    }

    private void fetchInboxMessages() {
        if (listInboxMessages == null) {
            showProgressDialog();
            startThread();
        } else {
            // messageType = TYPE_INCOMING_MESSAGE;
            recordsStored = listInboxMessages;
            messageListAdapter.setArrayList(recordsStored);
        }
    }

    public class FetchMessageThread extends Thread {

        public int tag = -1;

        public FetchMessageThread(int tag) {
            this.tag = tag;
        }

        @Override
        public void run() {
            if (readmessage != null && readmessage.equals("read message")) {
                recordsStored = fetchAllInboxSms(TYPE_INCOMING_MESSAGE);
                listInboxMessages = recordsStored;
                customHandler.sendEmptyMessage(0);
            }
            if (unreadmessage != null && unreadmessage.equals("unread message")) {
                recordsStored = fetchunreadInboxSms(TYPE_INCOMING_MESSAGE);
                listInboxMessages = recordsStored;
                customHandler.sendEmptyMessage(0);

            }
            if (yesterdaymessagee != null && yesterdaymessagee.equals("yesterday message")) {
                recordsStored = fetchyesterdyInboxSms(TYPE_INCOMING_MESSAGE);
                listInboxMessages = recordsStored;
                customHandler.sendEmptyMessage(0);

            }

        }

    }

    @SuppressLint("Range")
    public ArrayList<Message> fetchunreadInboxSms(int type) {
        ArrayList<Message> smsInbox = new ArrayList<Message>();
        @SuppressLint("Recycle")
        Uri uriSms = Uri.parse("content://sms");

        @SuppressLint("Recycle")
        Cursor cursor = this.getContentResolver()
                .query(uriSms,
                        new String[]{"_id", "address", "date", "body",
                                "type"}, " read = 0 AND type = " + type, null,
                        "date" + " COLLATE LOCALIZED ASC");

        if (cursor != null) {
            cursor.moveToLast();
            if (cursor.getCount() > 0) {

                do {
                    Message message = new Message();
                    msg = cursor.getString((cursor.getColumnIndex("address")));
                    //message.messageContent = cursor.getString(cursor.getColumnIndex("date"));
                    String date = cursor.getString(cursor.getColumnIndex("date"));
                    String content = cursor.getString(cursor.getColumnIndex("body"));
                    long timestamp = Long.parseLong(date);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(timestamp);
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.setTimeInMillis(System.currentTimeMillis());
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                    Date finaldate = calendar.getTime();
                    Date finaldate1 = calendar1.getTime();
                    String date1 = formatter.format(finaldate);
                    String date2 = formatter.format(finaldate1);
                    //message.messageContent = content;
                    if (date1.equals(date2)) {
                        message.messageNumber = msg;
                        message.messageContent = content;
                        unreadmessagee.add("You have a message from " + msg.substring(3, msg.length()).toLowerCase(Locale.getDefault()) + " and message is " + content.toLowerCase(Locale.getDefault()));
                        smsInbox.add(message);
                    }

                } while (cursor.moveToPrevious());
            }
        }

        return smsInbox;

    }

    @SuppressLint("Range")
    public ArrayList<Message> fetchAllInboxSms(int type) {
        ArrayList<Message> smsInbox = new ArrayList<Message>();
        @SuppressLint("Recycle")
        Uri uriSms = Uri.parse("content://sms");

        @SuppressLint("Recycle") Cursor cursor = this.getContentResolver()
                .query(uriSms,
                        new String[]{"_id", "address", "date", "body",
                                "type", "read"}, "type=" + type, null,
                        "date" + " COLLATE LOCALIZED ASC");

        if (cursor != null) {
            cursor.moveToLast();
            if (cursor.getCount() > 0) {

                do {
                    Message message = new Message();
                    msg = cursor.getString((cursor.getColumnIndex("address")));
                    //message.messageContent = cursor.getString(cursor.getColumnIndex("date"));
                    @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex("date"));
                    @SuppressLint("Range") String content = cursor.getString(cursor.getColumnIndex("body"));
                    long timestamp = Long.parseLong(date);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(timestamp);
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.setTimeInMillis(System.currentTimeMillis());
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                    Date finaldate = calendar.getTime();
                    Date finaldate1 = calendar1.getTime();
                    String date1 = formatter.format(finaldate);
                    String date2 = formatter.format(finaldate1);
                    //message.messageContent = content;
                    if (date1.equals(date2)) {
                        message.messageNumber = msg;
                        message.messageContent = content;

                        messagee.add(" You have a message from " + message.messageNumber.substring(3, message.messageNumber.toString().length()).toLowerCase(Locale.getDefault()) + " and message is " + content.toLowerCase(Locale.getDefault()));
                        smsInbox.add(message);

                    }
                } while (cursor.moveToPrevious());
            }
        }
        return smsInbox;
    }

    @SuppressLint("Range")
    public ArrayList<Message> fetchyesterdyInboxSms(int type) {
        ArrayList<Message> smsInbox = new ArrayList<Message>();
        @SuppressLint("Recycle")
        Uri uriSms = Uri.parse("content://sms");
        @SuppressLint("Recycle") Cursor cursor = this.getContentResolver()
                .query(uriSms,
                        new String[]{"_id", "address", "date", "body",
                                "type", " read "}, "type=" + type, null,
                        "date" + " COLLATE LOCALIZED ASC");

        if (cursor != null) {
            cursor.moveToLast();
            if (cursor.getCount() > 0) {

                do {
                    Message message = new Message();
                    msg = cursor.getString((cursor.getColumnIndex("address")));
                    //message.messageContent = cursor.getString(cursor.getColumnIndex("date"));
                    @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex("date"));
                    @SuppressLint("Range") String content = cursor.getString(cursor.getColumnIndex("body"));
                    long timestamp = Long.parseLong(date);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(timestamp);
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.setTimeInMillis(System.currentTimeMillis() - YESTERDAY_UNREAD_SMS);
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                    Date finaldate = calendar.getTime();
                    Date finaldate1 = calendar1.getTime();
                    String date1 = formatter.format(finaldate);
                    String date2 = formatter.format(finaldate1);
                    //message.messageContent = content;
                    if (date1.equals(date2)) {
                        message.messageNumber = msg;
                        message.messageContent = content;
                        yesterdaymessage.add("You have a message from " + msg.substring(3, msg.length()).toLowerCase(Locale.getDefault()) + " and message is " + content.toLowerCase(Locale.getDefault()));
                        smsInbox.add(message);
                    }

                } while (cursor.moveToPrevious());
            }
        }

        return smsInbox;

    }


    public void onPause() {
        if (textTospeech != null) {
            textTospeech.stop();
        }
        super.onPause();

    }

    private FetchMessageThread fetchMessageThread;

    private int currentCount = 0;

    public synchronized void startThread() {

        if (fetchMessageThread == null) {
            fetchMessageThread = new FetchMessageThread(currentCount);
            fetchMessageThread.start();
        }
    }

    public synchronized void stopThread() {
        if (fetchMessageThread != null) {
            Log.i("Cancel thread", "stop thread");
            FetchMessageThread moribund = fetchMessageThread;
            currentCount = fetchMessageThread.tag == 0 ? 1 : 0;
            fetchMessageThread = null;
            moribund.interrupt();
        }
    }

    static class CustomHandler extends Handler {
        private final WeakReference<MessageReader> activityHolder;

        CustomHandler(MessageReader inboxListActivity) {
            activityHolder = new WeakReference<>(inboxListActivity);
        }

        @Override
        public void handleMessage(android.os.Message msg) {

            MessageReader inboxListActivity = activityHolder.get();
            if (inboxListActivity.fetchMessageThread != null
                    && inboxListActivity.currentCount == inboxListActivity.fetchMessageThread.tag) {
                Log.i("received result", "received result");
                inboxListActivity.fetchMessageThread = null;

                inboxListActivity.messageListAdapter
                        .setArrayList(inboxListActivity.recordsStored);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressDialogInbox.dismiss();
    }

    private final OnCancelListener dialogCancelListener = new OnCancelListener() {

        @Override
        public void onCancel(DialogInterface dialog) {
            stopThread();
        }

    };


}
