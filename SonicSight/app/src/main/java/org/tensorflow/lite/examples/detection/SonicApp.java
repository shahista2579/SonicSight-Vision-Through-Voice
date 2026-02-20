package org.tensorflow.lite.examples.detection;

import android.app.Application;

import org.tensorflow.lite.examples.detection.Translate.TranslateViewModel;

import java.util.Arrays;

public class SonicApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        TranslateViewModel translateViewModel = new TranslateViewModel(this);
        translateViewModel.downloadLanguage(
            Arrays.asList(
                new TranslateViewModel.Language("hi"), // Hindi
                new TranslateViewModel.Language("kn")  // Kannada
            )
        );
    }
}