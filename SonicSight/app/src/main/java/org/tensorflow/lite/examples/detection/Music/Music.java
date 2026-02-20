package org.tensorflow.lite.examples.detection.Music;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.examples.detection.Home;
import org.tensorflow.lite.examples.detection.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class Music extends AppCompatActivity {

    private static ArrayList<Song> songList;
    MediaPlayer mediaPlayer;
    private static TextToSpeech textToSpeech;
    TextView mtitle, artist, cDuration, tDuration;
    String duration;
    SeekBar seekBar;
    float x1, x2, y1, y2;

    static int pos;
    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        mtitle = findViewById(R.id.textViewTitle);
        artist = findViewById(R.id.textViewArtist);
        cDuration = findViewById(R.id.textViewTduration);
        tDuration = findViewById(R.id.textView7);
        // In onCreate(), after setContentView(...)
        seekBar = findViewById(R.id.seekBar2);// Make sure R.id.seekBar matches your layout
        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
                textToSpeech.setSpeechRate(0.8f);
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {

                    }

                    @Override
                    public void onDone(String utteranceId) {
                        initUi();
                    }

                    @Override
                    public void onError(String utteranceId) {

                    }
                });
                textToSpeech.speak("Welcome to music , swipe right to return to main screen or swipe left for next song", TextToSpeech.QUEUE_FLUSH, null, "complete");
            }
        });


    }

    public void initUi() {
        Random random = new Random();
        songList = new ArrayList<>();

        mediaPlayer = new MediaPlayer();

        if (checkIfAlreadyhavePermission()) {
            getSongList();
            if (!songList.isEmpty()) {
                pos = random.nextInt(songList.size());
                playSong();
                Toast.makeText(getApplicationContext(), "Permission is granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "No songs found.", Toast.LENGTH_SHORT).show();
            }
        } else {
            requestMusicPermission();
        }
    }

    private boolean checkIfAlreadyhavePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO);
            return result == PackageManager.PERMISSION_GRANTED;
        } else {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestMusicPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                    PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent touchEvent) {
        switch (touchEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = touchEvent.getX();
                y1 = touchEvent.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2 = touchEvent.getX();
                y2 = touchEvent.getY();
                if (x1 < x2) {
                    textToSpeech.stop();
                    if (mediaPlayer != null) {
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                        }
                        mediaPlayer.reset();
                    }
                    if (task != null) {
                        task.cancel(true);
                    }
                    Intent i = new Intent(Music.this, Home.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }
                if (x1 > x2) {
                    Random random = new Random();
                    if (songList != null && !songList.isEmpty()) {
                        pos = random.nextInt(songList.size());
                        playSong();
                    } else {
                        Toast.makeText(this, "No songs available.", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
        return false;
    }


    static Task task;

    private void playSong() {
        try {

            mediaPlayer.reset();
            //get song
            Song playSong = songList.get(pos);
            mtitle.setText(playSong.getTitle());
            artist.setText(playSong.getArtist());

            //get id
            long currSong = playSong.getID();
            //set uri
            Uri trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    currSong);

            try {
                mediaPlayer.setDataSource(getApplicationContext(), trackUri);
            } catch (Exception e) {
                Log.e("MUSIC SERVICE", "Error setting data source", e);
            }

            task = new Task();
            mediaPlayer.prepare();
            task.execute();
            mediaPlayer.start();

            seekBar.setMax(mediaPlayer.getDuration());
            duration = milliSecondsToTimer(mediaPlayer.getDuration());
            tDuration.setText(duration);
            cDuration.setText(duration);
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    public void getSongList() {
        //retrieve song info

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        if (musicCursor != null && musicCursor.moveToPosition(3)) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                Log.d("jwenkjewkn", String.valueOf(new Song(thisId, thisTitle, thisArtist)));
                songList.add(new Song(thisId, thisTitle, thisArtist));

            }
            while (musicCursor.moveToNext());
        }
    }

    /**
     * Function to convert milliseconds time to
     * Timer Format
     * Hours:Minutes:Seconds
     */
    public String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    public void seekUpdation() {
        seekBar.setProgress(mediaPlayer.getCurrentPosition());
    }


    @Override
    public void onBackPressed() {
        mediaPlayer.stop();
        finish();
        super.onBackPressed();
    }

    @SuppressLint("StaticFieldLeak")
    class Task extends AsyncTask<Integer, Integer, Void> {
        long i = 0;

        @Override
        protected Void doInBackground(Integer... params) {

            while (mediaPlayer.isPlaying()) {
                i = mediaPlayer.getCurrentPosition();
                seekUpdation();
                publishProgress(0);
                i++;
            }
            if (!mediaPlayer.isPlaying()) {
                textToSpeech.speak("swipe right for next song", TextToSpeech.QUEUE_FLUSH, null);
            }
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            tDuration.setText("" + milliSecondsToTimer(i));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getSongList();
                if (!songList.isEmpty()) {
                    Random random = new Random();
                    pos = random.nextInt(songList.size());
                    playSong();
                    Toast.makeText(getApplicationContext(), "Permission granted ... Reading messages", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "No songs found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Permission denied to read your music files", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        if (task != null) {
            task.cancel(true);
        }
        super.onDestroy();
    }

}
