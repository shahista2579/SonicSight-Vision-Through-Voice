package org.tensorflow.lite.examples.detection.HandwrittenAuthentication;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.common.collect.ImmutableSortedSet;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;

import org.tensorflow.lite.examples.detection.R;

import java.util.Locale;
import java.util.Set;

/** Main activity which creates a StrokeManager and connects it to the DrawingView. */
public class DigitalInkMainActivity extends
        Dialog
        implements
        StrokeManager.
                DownloadedModelsChangedListener {
    @VisibleForTesting
    final StrokeManager strokeManager = new StrokeManager();
    public ArrayAdapter<ModelLanguageContainer> languageAdapter;
    static TextToSpeech textToSpeech;
    static Toast toast;
    static DrawingView drawingView;
    static StatusTextView1 textView1;
    static String languageCode;
    static int userintt;
    private static int firstTime = 0;

    public static int getuserint() {
        return userintt;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getWindow() != null) {
            getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT, // Max width
                ViewGroup.LayoutParams.MATCH_PARENT // Set desired height in pixels, or use ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    public DigitalInkMainActivity(@NonNull Context context, int Userint) {
        super(context);
        userintt = Userint;
        Toast.makeText(getContext(), String.valueOf(userintt), Toast.LENGTH_SHORT).show();
    }

    public static void toastString(String text) {
        toast = Toast.makeText(drawingView.getContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_ink_main2);

        Spinner languageSpinner = findViewById(R.id.languages_spinner);
        findViewById(R.id.download_button).setOnClickListener(v -> strokeManager.download());

        if (!ModelLanguageContainer.downloaded) {
            strokeManager.download();
        } else {
            Toast.makeText(drawingView.getContext(), "Model already downloaded", Toast.LENGTH_SHORT).show();
        }


        StatusTextView statusTextView = findViewById(R.id.status_text_view);
        StatusTextView1 statusTextView1 = findViewById(R.id.status_text_view2);
        textToSpeech = new TextToSpeech(getContext(), i -> {
            if (i != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.getDefault());
                textToSpeech.setSpeechRate((float) 0.7);
                textToSpeech.speak("", TextToSpeech.QUEUE_FLUSH, null);
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {

                    }

                    @Override
                    public void onDone(String utteranceId) {
                        if (firstTime == 0) {
                            strokeManager.download();
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {

                    }
                });
            }
        });
        drawingView = findViewById(R.id.drawing_view);


        findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                strokeManager.reset();
                DrawingView drawingView = findViewById(R.id.drawing_view);
                drawingView.clear();
            }
        });

        findViewById(R.id.recognize_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                strokeManager.recognize();
            }
        });
        DrawingView drawingView = findViewById(R.id.drawing_view);

        drawingView.setStrokeManager(strokeManager);
        statusTextView.setStrokeManager(strokeManager);
        strokeManager.setStatusChangedListener(statusTextView);
        statusTextView1.setStrokeManager(strokeManager);
        strokeManager.setStatusChangedListener1(statusTextView1);
        strokeManager.setContentChangedListener(drawingView);
        strokeManager.setDownloadedModelsChangedListener(this);
        strokeManager.setClearCurrentInkAfterRecognition(true);
        strokeManager.setTriggerRecognitionAfterInput(false);

        languageAdapter = populateLanguageAdapter();
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);
        strokeManager.refreshDownloadedModelsStatus();
        languageCode = languageAdapter.getItem(0).getLanguageTag();
        strokeManager.setActiveModel("en-US");
        Log.d("LanguageTag", languageCode);

        strokeManager.reset();
    }


    public static void cleardraw() {
        drawingView.clear();
    }

    public static class ModelLanguageContainer implements Comparable<ModelLanguageContainer> {
        private final String label;
        @Nullable
        private final String languageTag;
        public static boolean downloaded;

        private ModelLanguageContainer(String label, @Nullable String languageTag) {
            this.label = label;
            this.languageTag = languageTag;
        }

        /**
         * Populates and returns a real model identifier, with label, language tag and downloaded
         * status.
         */
        public static ModelLanguageContainer createModelContainer(String label, String languageTag) {
            // Offset the actual language labels for better readability
            return new ModelLanguageContainer(label, languageTag);
        }

        /** Populates and returns a label only, without a language tag. */
        public static ModelLanguageContainer createLabelOnly(String label) {
            return new ModelLanguageContainer(label, null);
        }

        @Nullable
        public String getLanguageTag() {
            return languageTag;
        }

        public void setDownloaded(boolean downloaded) {
            ModelLanguageContainer.downloaded = downloaded;
        }

        @NonNull
        @Override
        public String toString() {
            if (languageTag == null) {
                return label;
            } else if (downloaded) {
                return "   [D] " + label;
            } else {
                return "   " + label;
            }
        }

        @Override
        public int compareTo(ModelLanguageContainer o) {
            return label.compareTo(o.label);
        }
    }

    @Override
    public void onDownloadedModelsChanged(Set<String> downloadedLanguageTags) {
        for (int i = 0; i < languageAdapter.getCount(); i++) {
            ModelLanguageContainer container = languageAdapter.getItem(i);
            container.setDownloaded(downloadedLanguageTags.contains(container.languageTag));
            if (userintt == 0) {
                firstTime = 1;
                textToSpeech.speak("write your pin. and pin must be 4 digit.", TextToSpeech.QUEUE_FLUSH, null, "hel");
            } else if (userintt == 2) {
                textToSpeech.speak("write your password on screen and password must be 6 digit. and Do not forgot the password", TextToSpeech.QUEUE_FLUSH, null);
            }

        }
        languageAdapter.notifyDataSetChanged();
    }


    private ArrayAdapter<ModelLanguageContainer> populateLanguageAdapter() {
        ArrayAdapter<ModelLanguageContainer> languageAdapter =
                new ArrayAdapter<ModelLanguageContainer>(getContext(), android.R.layout.simple_spinner_item);

        // Manually add non-text models first
        ImmutableSortedSet.Builder<ModelLanguageContainer> textModels =
                ImmutableSortedSet.naturalOrder();
        for (DigitalInkRecognitionModelIdentifier modelIdentifier :
                DigitalInkRecognitionModelIdentifier.forRegionSubtag("US")) {
            if (modelIdentifier.getLanguageSubtag().equals("en")) {
                StringBuilder label = new StringBuilder();
                label.append(new Locale(modelIdentifier.getLanguageSubtag()).getDisplayName());

                if (modelIdentifier.getRegionSubtag() != null) {
                    label.append(" (").append(modelIdentifier.getRegionSubtag()).append(")");
                }

                if (modelIdentifier.getScriptSubtag() != null) {
                    label.append(", ").append(modelIdentifier.getScriptSubtag()).append(" Script");
                }
                textModels.add(
                        ModelLanguageContainer.createModelContainer(
                                label.toString(), modelIdentifier.getLanguageTag()));
            }
            languageAdapter.addAll(textModels.build());
        }
        return languageAdapter;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        userintt = 4;//assign some random value when user exit from activity because this value is static
    }

}
