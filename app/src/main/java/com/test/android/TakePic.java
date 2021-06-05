package com.test.android;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class TakePic {

    private final String TAG = TakePic.class.getSimpleName();
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private PreviewView previewView;
    private final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private AppCompatActivity mContext;
    private TextureView imageView;
    private int lens = CameraSelector.LENS_FACING_BACK;

    private volatile static TakePic instance = null;

    // 私有化构造方法
    private TakePic() {

    }

    public static TakePic getInstance() {
        if (instance == null) {
            synchronized (TakePic.class) {
                if (instance == null) {
                    instance = new TakePic();
                }
            }

        }
        return instance;
    }

    public void init(AppCompatActivity context, PreviewView previewView, TextureView imageView) {
        this.mContext = context;
        this.previewView = previewView;
        this.imageView = imageView;
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> instance = ProcessCameraProvider.getInstance(mContext);
        instance.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = instance.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().build();
                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                CameraSelector cameraSelector=null;
                if(lens==CameraSelector.LENS_FACING_FRONT){
                     cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                }else{
                     cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                }
                imageAnalysis.setAnalyzer(Runnable::run, new FaceTrackingAnalyzer(previewView, imageView,lens));
                cameraProvider.unbindAll();
                // 将用例绑定到相机
                cameraProvider.bindToLifecycle(mContext, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(mContext));

    }

    public void takePhoto(String path) {
        File photoFile = new File(path, new SimpleDateFormat(FILENAME_FORMAT,
                Locale.US).format(System.currentTimeMillis()) + ".jpg");
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(mContext),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.i(TAG,"保存成功");
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        exception.printStackTrace();
                    }
                });
    }


}


