package org.tensorflow.lite.examples.detection.facelogin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.examples.detection.Home;
import org.tensorflow.lite.examples.detection.R;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;

import androidx.camera.core.ImageProxy;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;

public class FaceLoginActivity extends AppCompatActivity {

    private PreviewView previewView;
    private FaceDetector detector;
    private TextToSpeech textToSpeech;
    CameraSelector cameraSelector;

    boolean start = true, flipX = false;
    float distance = 1.0f;

    int cam_face = CameraSelector.LENS_FACING_FRONT; //Default Back Camera
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    int[] intValues;
    int inputSize = 112;  //Input size for model
    boolean isModelQuantized = false;
    float[][] embeedings;
    float IMAGE_MEAN = 128.0f;
    float IMAGE_STD = 128.0f;
    int OUTPUT_SIZE = 192; //Output size of model
    private static int SELECT_PICTURE = 1;
    ProcessCameraProvider cameraProvider;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    String modelFile = "mobile_face_net.tflite"; //model name

    Interpreter tfLite;
    private HashMap<String, SimilarityClassifier.Recognition> registered = new HashMap<>(); //saved Faces

    private static final int REQ_CODE_SPEECH_INPUT = 100;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registered = readFromSP(); //Load saved faces from memory when app starts
        registered.putAll(readFromSP());
        setContentView(R.layout.activity_main10);
        previewView = findViewById(R.id.previewView);
        SharedPreferences sharedPref = getSharedPreferences("Distance", Context.MODE_PRIVATE);
        distance = sharedPref.getFloat("distance", 1.00f);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 1);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder().setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE).setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).build();

        detector = FaceDetection.getClient(options);
//        startCamera();

        initTextToSpeech();

        previewView.setOnLongClickListener(v -> {
//                captureFace();
            SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition("0", "user", -1f);
            result.setExtra(embeedings);
            return false;
        });

        //Camera Permission
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }

        //Load model
        try {
            tfLite = new Interpreter(loadModelFile(FaceLoginActivity.this, modelFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void initTextToSpeech() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        textToSpeech.setLanguage(Locale.US);
                        textToSpeech.setSpeechRate(0.8f);
                        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String utteranceId) {

                            }

                            @Override
                            public void onDone(String utteranceId) {
                                if (utteranceId.equals("complete")) {
                                    startActivity(new Intent(FaceLoginActivity.this, Home.class)); //Redirect to Home Activity
                                } else {
                                    cameraBind();
                                }
                            }

                            @Override
                            public void onError(String utteranceId) {

                            }
                        });
                        textToSpeech.speak("welcome to sonic app, we are setting up facial login to your app" +
                                "Please position your face in front of the camera. " +
                                "You will hear a sound when your face is detected. ", TextToSpeech.QUEUE_FLUSH, null, "");
                    }
                }
            });
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

    @SuppressLint("UnsafeOptInUsageError")
    private void processImageProxy(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            detector.process(inputImage).addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null) {
                    String voiceText = result.get(0);
                    if (voiceText.equalsIgnoreCase("yes")) {
                        // startCamera();
                        cameraBind();
                    }
                }
            }
        }
    }

    private void cameraBind() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        cameraSelector = new CameraSelector.Builder().requireLensFacing(cam_face).build();

        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(640, 480)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //Latest frame is shown
                .build();

        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @ExperimentalGetImage
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                try {
                    Thread.sleep(100);  //Camera preview refreshed every 10 millisec(adjust as required)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                InputImage image = null;


                @SuppressLint("UnsafeExperimentalUsageError")
                // Camera Feed-->Analyzer-->ImageProxy-->mediaImage-->InputImage(needed for ML kit face detection)

                Image mediaImage = imageProxy.getImage();

                if (mediaImage != null) {
                    image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
//                  System.out.println("Rotation "+imageProxy.getImageInfo().getRotationDegrees());
                }
                //Process acquired image to detect faces
                if (image != null) {
                    Task<List<Face>> result = detector.process(image).addOnSuccessListener(faces -> {
                        Log.d("FaceLoginActivity", "Faces detected: " + faces.size());
                        if (!faces.isEmpty()) {

                            Face face = faces.get(0); //Get first face from detected faces
                            //                                                    System.out.println(face);

                            //mediaImage to Bitmap
                            Bitmap frame_bmp = toBitmap(mediaImage);
                            if (frame_bmp == null) {
                                imageProxy.close();
                                return;
                            }

                            int rot = imageProxy.getImageInfo().getRotationDegrees();
                            Bitmap frame_bmp1 = rotateBitmap(frame_bmp, rot, false, false);
                            if (frame_bmp1 == null) {
                                imageProxy.close();
                                return;
                            }

                            //Get bounding box of face
                            RectF boundingBox = new RectF(face.getBoundingBox());

                            //Crop out bounding box from whole Bitmap(image)
                            Bitmap cropped_face = getCropBitmapByCPU(frame_bmp1, boundingBox);

                            if (flipX) cropped_face = rotateBitmap(cropped_face, 0, flipX, false);
                            //Scale the acquired Face to 112*112 which is required input for model
                            Bitmap scaled = Bitmap.createScaledBitmap(cropped_face, 112, 112, true);

                            if (start)
                                recognizeImage(scaled); //Send scaled bitmap to create face embeddings.
                            //                                                    System.out.println(boundingBox);

                        }

                    }).addOnFailureListener(e -> Log.d("FaceLoginActivity", "Face detection failed: " + e.getMessage())).addOnCompleteListener(new OnCompleteListener<List<Face>>() {
                        @Override
                        public void onComplete(@NonNull Task<List<Face>> task) {

                            imageProxy.close(); //v.important to acquire next frame for analysis
                        }
                    });
                }


            }
        });


        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

    private void addFace() {
        SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                "0", "", -1f);
        result.setExtra(embeedings);
        registered.put("person", result);
        insertToSP(registered, 0); //Save the face to Shared Preferences
        start = false;
        textToSpeech.speak("Face added successfully.", TextToSpeech.QUEUE_FLUSH, null, "complete");
    }

    public void recognizeImage(@NonNull final Bitmap bitmap) {
        //Create ByteBuffer to store normalized image

        ByteBuffer imgData = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4);

        imgData.order(ByteOrder.nativeOrder());

        intValues = new int[inputSize * inputSize];

        //get pixel values from Bitmap to normalize
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();

        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        //imgData is input to our model
        Object[] inputArray = {imgData};

        Map<Integer, Object> outputMap = new HashMap<>();


        embeedings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable

        outputMap.put(0, embeedings);

        tfLite.runForMultipleInputsOutputs(inputArray, outputMap); //Run model


        float distance_local = Float.MAX_VALUE;
        String id = "0";
        String label = "?";

        //Compare new face with saved Faces.
        if (!registered.isEmpty()) {
//            finish();
//            startActivity(new Intent(FaceLoginActivity.this, Home.class)); //Redirect to Home Activity

            final List<Pair<String, Float>> nearest = findNearest(embeedings[0]);//Find 2 closest matching face

            if (nearest.get(0) != null) {

                final String name = nearest.get(0).first; //get name and distance of closest matching face
                // label = name;
                distance_local = nearest.get(0).second;
                if (distance_local < distance) //If distance between Closest found face is more than 1.000 ,then output UNKNOWN face.
                {
                    Toast.makeText(this, "Nearest: " + name +
                                    "\nDist: " + String.format("%.3f", distance_local) +
                                    "\n2nd Nearest: " + nearest.get(1).first +
                                    "\nDist: " + String.format("%.3f", nearest.get(1).second),
                            Toast.LENGTH_LONG).show();
                    Log.d("Nearest: ", name +
                                    "\nDist: " + String.format("%.3f", distance_local) +
                                    "\n2nd Nearest: " + nearest.get(1).first +
                                    "\nDist: " + String.format("%.3f", nearest.get(1).second)
                    );
                    start = false; //Set start to false so that next time it does not prompt to recognize  face.
                    finish();
                    startActivity(new Intent(FaceLoginActivity.this, Home.class)); //Redirect to Home Activity
                }
//                    System.out.println("nearest: " + name + " - distance: " + distance_local);
            }
        } else {
            addFace(); //If no face is detected and no faces are registered, prompt to add face
        }
    }


    //Compare Faces by distance between face embeddings
    private List<Pair<String, Float>> findNearest(float[] emb) {
        List<Pair<String, Float>> neighbour_list = new ArrayList<Pair<String, Float>>();
        Pair<String, Float> ret = null; //to get closest match
        Pair<String, Float> prev_ret = null; //to get second closest match
        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : registered.entrySet()) {

            final String name = entry.getKey();
            final float[] knownEmb = ((float[][]) entry.getValue().getExtra())[0];

            float distance = 0;
            for (int i = 0; i < emb.length; i++) {
                float diff = emb[i] - knownEmb[i];
                distance += diff * diff;
            }
            distance = (float) Math.sqrt(distance);
            if (ret == null || distance < ret.second) {
                prev_ret = ret;
                ret = new Pair<>(name, distance);
            }
        }
        if (prev_ret == null) prev_ret = ret;
        neighbour_list.add(ret);
        neighbour_list.add(prev_ret);

        return neighbour_list;

    }

    public static Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF) {
        Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(),
                (int) cropRectF.height(), Bitmap.Config.ARGB_8888);
        Canvas cavas = new Canvas(resultBitmap);

        // draw background
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        cavas.drawRect(
                new RectF(0, 0, cropRectF.width(), cropRectF.height()),
                paint);

        Matrix matrix = new Matrix();
        matrix.postTranslate(-cropRectF.left, -cropRectF.top);

        cavas.drawBitmap(source, matrix, paint);

        if (!source.isRecycled()) {
            source.recycle();
        }

        return resultBitmap;
    }

    private static Bitmap rotateBitmap(
            Bitmap bitmap, int rotationDegrees, boolean flipX, boolean flipY) {
        if (bitmap == null) return null; // Prevent NullPointerException
        Matrix matrix = new Matrix();

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees);

        // Mirror the image along the X or Y axis.
        matrix.postScale(flipX ? -1.0f : 1.0f, flipY ? -1.0f : 1.0f);
        Bitmap rotatedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }

    //IMPORTANT. If conversion not done ,the toBitmap conversion does not work on some devices.
    private static byte[] YUV_420_888toNV21(Image image) {

        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = width * height / 4;

        byte[] nv21 = new byte[ySize + uvSize * 2];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int rowStride = image.getPlanes()[0].getRowStride();
        assert (image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;

        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        } else {
            long yBufferPos = -rowStride; // not an actual position
            for (; pos < ySize; pos += width) {
                yBufferPos += rowStride;
                yBuffer.position((int) yBufferPos);
                yBuffer.get(nv21, pos, width);
            }
        }

        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();

        assert (rowStride == image.getPlanes()[1].getRowStride());
        assert (pixelStride == image.getPlanes()[1].getPixelStride());

        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            try {
                vBuffer.put(1, (byte) ~savePixel);
                if (uBuffer.get(0) == (byte) ~savePixel) {
                    vBuffer.put(1, savePixel);
                    vBuffer.position(0);
                    uBuffer.position(0);
                    vBuffer.get(nv21, ySize, 1);
                    uBuffer.get(nv21, ySize + 1, uBuffer.remaining());

                    return nv21; // shortcut
                }
            } catch (ReadOnlyBufferException ex) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                int vuPos = col * pixelStride + row * rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }

        return nv21;
    }

    private Bitmap toBitmap(Image image) {

        byte[] nv21 = YUV_420_888toNV21(image);


        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        //System.out.println("bytes"+ Arrays.toString(imageBytes));

        //System.out.println("FORMAT"+image.getFormat());

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    @ExperimentalGetImage
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) return null;
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    //Save Faces to Shared Preferences.Conversion of Recognition objects to json string
    private void insertToSP(HashMap<String, SimilarityClassifier.Recognition> jsonMap, int mode) {
        if (mode == 1)  //mode: 0:save all, 1:clear all, 2:update all
            jsonMap.clear();
        else if (mode == 0)
            jsonMap.putAll(readFromSP());
        String jsonString = new Gson().toJson(jsonMap);
        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : jsonMap.entrySet()) {
            System.out.println("Entry Input " + entry.getKey() + " " + entry.getValue().getExtra());
        }
        SharedPreferences sharedPreferences = getSharedPreferences("HashMap", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("map", jsonString);
        System.out.println("Input josn" + jsonString);
        editor.apply();
        Toast.makeText(getApplicationContext(), "Recognitions Saved", Toast.LENGTH_SHORT).show();
    }

    //Load Faces from Shared Preferences.Json String to Recognition object
    private HashMap<String, SimilarityClassifier.Recognition> readFromSP() {
        SharedPreferences sharedPreferences = getSharedPreferences("HashMap", MODE_PRIVATE);
        String defValue = new Gson().toJson(new HashMap<String, SimilarityClassifier.Recognition>());
        String json = sharedPreferences.getString("map", defValue);
        // System.out.println("Output json"+json.toString());
        TypeToken<HashMap<String, SimilarityClassifier.Recognition>> token = new TypeToken<>() {
        };
        HashMap<String, SimilarityClassifier.Recognition> retrievedMap = new Gson().fromJson(json, token.getType());
         System.out.println("Output map"+retrievedMap.toString());

        //During type conversion and save/load procedure,format changes(eg float converted to double).
        //So embeddings need to be extracted from it in required format(eg.double to float).
        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : retrievedMap.entrySet()) {
            float[][] output = new float[1][OUTPUT_SIZE];
            Object extra = entry.getValue().getExtra();
            if (extra instanceof ArrayList<?> arrayList) {
                if (!arrayList.isEmpty() && arrayList.get(0) instanceof Double) {
                    for (int counter = 0; counter < arrayList.size(); counter++) {
                        output[0][counter] = ((Double) arrayList.get(counter)).floatValue();
                    }
                }
            }
            entry.getValue().setExtra(output);
            System.out.println("Entry output " + entry.getKey() + " " + entry.getValue().getExtra());
        }
//        System.out.println("OUTPUT"+ Arrays.deepToString(outut));
        Toast.makeText(getApplicationContext(), "Recognitions Loaded", Toast.LENGTH_SHORT).show();
        return retrievedMap;
    }


    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
