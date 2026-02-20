package org.tensorflow.lite.examples.detection.Translate;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.tensorflow.lite.examples.detection.Home;
import org.tensorflow.lite.examples.detection.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment view for handling translations
 */
public class TranslateFragment extends Fragment implements TextToSpeech.OnUtteranceCompletedListener, RecognitionListener {

    private TextToSpeech textToSpeech, kntts;
    SpeechRecognizer hin, en, kn;
    TextView textView;
    static ArrayList<String> tt = new ArrayList<>();
    private TextToSpeech ktts;

    TextView textView2;

    static String results;
    String hindi, english, kannada;
    private TranslateViewModel viewModel = null;

    LinearProgressIndicator progressBar;


    public static TranslateFragment newInstance() {
        return new TranslateFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        textToSpeech = new TextToSpeech(requireContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(new Locale("hi"));
                textToSpeech.setSpeechRate(0.8f);
            }
        });
        ktts = new TextToSpeech(requireContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                ktts.setLanguage(Locale.getDefault());
                ktts.setSpeechRate(0.9f);
                ktts.setOnUtteranceCompletedListener(TranslateFragment.this);
                ktts.speak("", TextToSpeech.QUEUE_FLUSH, null);
            }
        });
        kntts = new TextToSpeech(requireContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                kntts.setLanguage(new Locale("kn"));
                kntts.setSpeechRate(1f);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(TranslateViewModel.class);
        return inflater.inflate(R.layout.translate_fragment, container, false);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (checkIfAlreadyPermission()) {
            Toast.makeText(getContext(), "Permission is granted", Toast.LENGTH_SHORT).show();

        } else {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        hin = SpeechRecognizer.createSpeechRecognizer(view.getContext());
        hin.setRecognitionListener(this);
        en = SpeechRecognizer.createSpeechRecognizer(view.getContext());
        en.setRecognitionListener(this);
        kn = SpeechRecognizer.createSpeechRecognizer(view.getContext());
        kn.setRecognitionListener(this);
        textView = requireView().findViewById(R.id.result);
        textView2 = requireView().findViewById(R.id.tt);
        progressBar = view.findViewById(R.id.progressBar);
        textView.setOnClickListener(view1 -> {
            if (!textToSpeech.isSpeaking()) {
                if (textView2.getText().toString().contains("English to Hindi")) {
                    startEnglishVoiceInput();
                    return;
                }
                if (textView2.getText().toString().contains("Hindi to English")) {
                    startUrduVoiceInput();
                    return;
                }
                if (textView2.getText().toString().contains("English to kannada")) {
                    startEnglishVoiceInput();
                    return;
                }
                if (textView2.getText().toString().contains("Kannada to English")) {
                    starKannadaVoiceInput();
                    return;
                }

                startEnglishVoiceInput();
            }

        });


        textView.setOnLongClickListener(v -> {
            startActivity(new Intent(getContext(), Home.class));
            return true;
        });


        viewModel.isDownloading.observe(getViewLifecycleOwner(), isDownloading -> {
            if (isDownloading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });

        final TextView downloadedModelsTextView = view.findViewById(R.id.downloadedModels);

        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!downloadedModelsTextView.getText().toString().contains("hi") && !downloadedModelsTextView.getText().toString().contains("kn")) {
                    ktts.speak("translating model is downloading. please do not close the app", TextToSpeech.QUEUE_FLUSH, null);
                    final Spinner targetLangSelector = requireView().findViewById(R.id.targetLangSelector);
                    final ArrayAdapter<TranslateViewModel.Language> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, viewModel.getAvailableLanguages());
                    targetLangSelector.setAdapter(adapter);

                    // Add both Hindi and Kannada to the adapter if not present
                    TranslateViewModel.Language hindiLang = new TranslateViewModel.Language("hi");
                    TranslateViewModel.Language kannadaLang = new TranslateViewModel.Language("kn");
                    if (adapter.getPosition(hindiLang) == -1) {
                        adapter.add(hindiLang);
                    }
                    if (adapter.getPosition(kannadaLang) == -1) {
                        adapter.add(kannadaLang);
                    }
                    adapter.notifyDataSetChanged();

                    // Set default selection to Hindi
                    targetLangSelector.setSelection(adapter.getPosition(hindiLang));
                    ArrayList<TranslateViewModel.Language> languagesToDownload = new ArrayList<>();
                    languagesToDownload.add(hindiLang);
                    languagesToDownload.add(kannadaLang);
                    // Download both Hindi and Kannada models
                    viewModel.downloadLanguage(languagesToDownload);
                } else {
                    ktts.speak("Welcome to Translator, say English to hindi for English to hindi and, say hindi to English for hindi to English, and say English to kannada or kannada to English, Press long on the screen to return in main menu", TextToSpeech.QUEUE_FLUSH, null, "en");
                    textToSpeech.setOnUtteranceCompletedListener(TranslateFragment.this);
                }
            }
        }, 1500);


        final ToggleButton targetSyncButton = view.findViewById(R.id.buttonSyncTarget);
        final Spinner targetLangSelector = view.findViewById(R.id.targetLangSelector);
        final TranslateViewModel viewModel = new ViewModelProvider(this).get(TranslateViewModel.class);

        // Get available language list and set up source and target language spinners
        // with default selections.
        final ArrayAdapter<TranslateViewModel.Language> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, viewModel.getAvailableLanguages());
        targetLangSelector.setAdapter(adapter);
        targetLangSelector.setSelection(adapter.getPosition(new TranslateViewModel.Language("hi")));

        targetSyncButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TranslateViewModel.Language language = adapter.getItem(targetLangSelector.getSelectedItemPosition());
//                        if (isChecked) {
//                            viewModel.downloadLanguage(la);
//                        } else {
//                            viewModel.deleteLanguage(language);
//                        }
            }
        });

        // Update sync toggle button states based on downloaded models list.
        viewModel.availableModels.observe(

                getViewLifecycleOwner(), new Observer<List<String>>() {
                    @Override
                    public void onChanged(@Nullable List<String> translateRemoteModels) {
                        String output = requireContext().getString(R.string.downloaded_models_label, translateRemoteModels);
                        downloadedModelsTextView.setText(output);
                        if (downloadedModelsTextView.getText().toString().contains("kn")) {
                            ktts.speak("tap on screen and say English to hindi for English to hindi and, say hindi to English for hindi to English, Press long on the screen to return in main menu", TextToSpeech.QUEUE_FLUSH, null);
                            ;
                        }
                        targetSyncButton.setChecked(!viewModel.requiresModelDownload(adapter.getItem(targetLangSelector.getSelectedItemPosition()), translateRemoteModels));
                    }
                });
    }


    private void startUrduVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
        hin.startListening(intent);
    }

    private void starKannadaVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "kn_IN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
        kn.startListening(intent);
    }

    private void startEnglishVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
        en.startListening(intent);
    }


    @Override
    public void onUtteranceCompleted(String s) {
        if (s.equals("hi")) {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startUrduVoiceInput();
                }
            });
        }
        if (s.equals("en")) {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final TextView downloadedModelsTextView = requireView().findViewById(R.id.downloadedModels);
                    if (downloadedModelsTextView.getText().toString().contains("hi") || downloadedModelsTextView.getText().toString().contains("kn")) {
                        ktts.speak("tap on the screen and say", TextToSpeech.QUEUE_FLUSH, null);
                    }

                }
            });
        }
        if (s.equals("kn")) {
            requireActivity().runOnUiThread(this::starKannadaVoiceInput);
        }

    }

    private boolean checkIfAlreadyPermission() {
        int result = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED;
    }
// add toast to check whether it is working or not

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        Toast.makeText(getActivity(), "Listening...", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float v) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int i) {

    }

    @Override
    public void onResults(Bundle bundle) {
        tt = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (tt.get(0).equalsIgnoreCase("English to hindi")) {
            textView2.setText(tt.get(0));
            ktts.speak("tap on the screen and tell me the word that you want to translate", TextToSpeech.QUEUE_FLUSH, null);
        }
        if (tt.get(0).equalsIgnoreCase("English to kannada")) {
            textView2.setText("English to kannada");
            ktts.speak("tap on the screen and tell me the word that you want to translate", TextToSpeech.QUEUE_FLUSH, null);
        }
        if (tt.get(0).equalsIgnoreCase("Kannada to English")) {
            textView2.setText("Kannada to English");
            kntts.speak("tap on the screen and tell me the word that you want to translate", TextToSpeech.QUEUE_FLUSH, null);
        }
        if (tt.get(0).contains("hindi to English")) {
            textView2.setText(tt.get(0));
            textToSpeech.speak("", TextToSpeech.QUEUE_FLUSH, null);
        }
        if (tt.get(0).contains("main menu")) {
            startActivity(new Intent(getContext(), Home.class));
        }
        if (textView2.getText().toString().equalsIgnoreCase("English to hindi")) {
            ArrayList<String> result = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            TranslatorOptions options = new TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.HINDI).build();

            Translator englishhindiTranslator = Translation.getClient(options);
            if (result != null) {
                english = result.get(0).replace("English to Hindi", "");
            }
            DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();

            englishhindiTranslator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object v) {
                    englishhindiTranslator.translate(english).addOnSuccessListener(new OnSuccessListener() {
                                @Override
                                public void onSuccess(Object translatedText) {
                                    textView.setText((String) translatedText);
                                    if (textView.getText().toString().equals("")) {
                                        Toast.makeText(getContext(), translatedText.toString(), Toast.LENGTH_SHORT).show();
                                    } else {
                                        results = translatedText.toString();
                                        Toast.makeText(getContext(), translatedText.toString(), Toast.LENGTH_LONG).show();
                                        textToSpeech.speak(translatedText.toString(), TextToSpeech.QUEUE_FLUSH, null);
                                        ktts.speak("tap on the screen and say the word", TextToSpeech.QUEUE_ADD, null);
                                    }
                                    Log.i("TAG", "Translation is " + (String) translatedText);
                                }
                            }).addOnCanceledListener(new OnCanceledListener() {
                                @Override
                                public void onCanceled() {
                                    Toast.makeText(getContext(), "Downloading cancelled...", Toast.LENGTH_SHORT).show();
                                }
                            })

                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }).addOnFailureListener(e -> {
                // Model couldn’t be downloaded or other internal error.
                Toast.makeText(getContext(), "Model could n’t be downloaded ", Toast.LENGTH_SHORT).show();

            });


        }
        if (textView2.getText().toString().contains("English to kannada")) {
            ArrayList<String> result = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            TranslatorOptions options = new TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.KANNADA).build();

            final Translator hindienglishtranslator = Translation.getClient(options);

            hindi = result.get(0).replace("English to kannada", "");


            DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();


            hindienglishtranslator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object v) {
                    hindienglishtranslator.translate(hindi).addOnSuccessListener(new OnSuccessListener() {
                        @Override
                        public void onSuccess(Object translatedText) {
                            textView.setText(translatedText.toString());
                            if (textView.getText().toString().equals("")) {
                                Toast.makeText(getContext(), translatedText.toString(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), translatedText.toString(), Toast.LENGTH_LONG).show();
                                ktts.speak((String) translatedText, TextToSpeech.QUEUE_FLUSH, null);
                                ktts.speak("tap on the screen and say the word", TextToSpeech.QUEUE_ADD, null);
                            }
                            Log.i("TAG", "Translation is " + (String) translatedText);
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Error", "Translation faliled " + e);
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Model couldn’t be downloaded or other internal error.
                    Log.e("Error", "Model could n’t be downloaded " + e);

                }
            });

        }
        if (textView2.getText().toString().contains("hindi to English")) {
            ArrayList<String> result = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            TranslatorOptions options = new TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.HINDI).setTargetLanguage(TranslateLanguage.ENGLISH).build();

            final Translator hindienglishtranslator = Translation.getClient(options);

            hindi = result.get(0).replace("hindi to English", "");


            DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();


            hindienglishtranslator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object v) {
                    hindienglishtranslator.translate(hindi).addOnSuccessListener(new OnSuccessListener() {
                        @Override
                        public void onSuccess(Object translatedText) {
                            textView.setText(translatedText.toString());
                            if (textView.getText().toString().equals("")) {
                                Toast.makeText(getContext(), translatedText.toString(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), translatedText.toString(), Toast.LENGTH_LONG).show();
                                ktts.speak((String) translatedText, TextToSpeech.QUEUE_FLUSH, null);
                                ktts.speak("tap on the screen and say the word", TextToSpeech.QUEUE_ADD, null);
                            }
                            Log.i("TAG", "Translation is " + (String) translatedText);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("Error", "Translation faliled " + e);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Model couldn’t be downloaded or other internal error.
                    Log.e("Error", "Model could n’t be downloaded " + e);

                }
            });

        }
        if (textView2.getText().toString().contains("Kannada to English")) {
            ArrayList<String> result = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.KANNADA)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build();

            final Translator kannadaEnglishTranslator = Translation.getClient(options);

            if (result != null) {
                kannada = result.get(0).replace("Kannada to English", "");
            }

            DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();

            kannadaEnglishTranslator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object v) {
                    kannadaEnglishTranslator.translate(kannada).addOnSuccessListener(new OnSuccessListener() {
                        @Override
                        public void onSuccess(Object translatedText) {
                            textView.setText(translatedText.toString());
                            if (textView.getText().toString().equals("")) {
                                Toast.makeText(getContext(), translatedText.toString(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), translatedText.toString(), Toast.LENGTH_LONG).show();
                                ktts.speak((String) translatedText, TextToSpeech.QUEUE_FLUSH, null);
                                ktts.speak("tap on the screen and say the word", TextToSpeech.QUEUE_ADD, null);
                            }
                            Log.i("TAG", "Translation is " + (String) translatedText);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("Error", "Translation failed " + e);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Model couldn’t be downloaded or other internal error.
                    Log.e("Error", "Model couldn’t be downloaded " + e);
                }
            });
        }

    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(getContext(), "permission granted", Toast.LENGTH_SHORT).show();
            } else {

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(getContext(), "Permission denied .please allow to record the audio", Toast.LENGTH_SHORT).show();
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
