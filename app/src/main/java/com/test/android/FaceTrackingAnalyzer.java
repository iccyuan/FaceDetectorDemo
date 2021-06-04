package com.test.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FaceTrackingAnalyzer implements ImageAnalysis.Analyzer {
    private static final String TAG = "MLKitFacesAnalyzer";
    private final SurfaceHolder surfaceHolder;
    private FaceDetector detector;
    private PreviewView previewView;
    private SurfaceView imageView;

    private Canvas canvas;
    private Paint linePaint;
    private float widthScaleFactor = 1.0f;
    private float heightScaleFactor = 1.0f;
    private int lens;
    private long lastTimestamp = 0;

    FaceTrackingAnalyzer(PreviewView previewView, SurfaceView imageView, int lens) {
        this.previewView = previewView;
        this.imageView = imageView;
        this.lens = lens;
        FaceDetectorOptions options = new FaceDetectorOptions
                .Builder()
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();
        detector = FaceDetection.getClient(options);
        imageView.setZOrderOnTop(true);//处于顶层
        imageView.getHolder().setFormat(PixelFormat.TRANSPARENT);//设置surface为透明
        surfaceHolder = imageView.getHolder();
        linePaint = new Paint();
        linePaint.setColor(Color.WHITE);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1f);
    }


    @Override
    public void analyze(@NonNull @NotNull ImageProxy imageProxy) {
        try {
          /*  if (System.currentTimeMillis() - lastTimestamp < 1000) {
                return;
            }*/
            lastTimestamp = System.currentTimeMillis();

            final Bitmap bitmap;
            bitmap = ImageUtils.imageProxyToBitmap(imageProxy, imageProxy.getImageInfo().getRotationDegrees());
            InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
            initDetector(inputImage);
            initDrawingUtils(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            imageProxy.close();
        }
    }

    private void initDetector(InputImage inputImage) {

        detector.process(inputImage).addOnSuccessListener(faces -> {
            if (!faces.isEmpty()) {
                Log.i(TAG, "发现人脸");
                processFaces(inputImage,faces);
            } else {
                Log.i(TAG, "NULL");
                canvas = new Canvas();
                canvas = surfaceHolder.lockCanvas();
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                if(canvas!=null){
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

        }).addOnFailureListener(e -> Log.e(TAG, e.toString()));

    }


    private void initDrawingUtils(Bitmap inBitmap) {

    }

    private void processFaces(InputImage inputImage,List<Face> faces) {
        Bitmap bitmap = Bitmap.createBitmap(previewView.getWidth(), previewView.getHeight(), Bitmap.Config.ARGB_4444);
        canvas = new Canvas(bitmap);
        widthScaleFactor = canvas.getWidth() / (inputImage.getWidth() * 1.0f);
        heightScaleFactor = canvas.getHeight() / (inputImage.getHeight() * 1.0f);
        for (Face face : faces) {

            Rect box = new Rect((int) translateX(face.getBoundingBox().left),
                    (int) translateY(face.getBoundingBox().top),
                    (int) translateX(face.getBoundingBox().right),
                    (int) translateY(face.getBoundingBox().bottom));

            Log.i(TAG, "top: " + (int) translateY(face.getBoundingBox().top)
                    + "left: " + (int) translateX(face.getBoundingBox().left)
                    + "bottom: " + (int) translateY(face.getBoundingBox().bottom)
                    + "right: " + (int) translateX(face.getBoundingBox().right));

            Log.i(TAG, "top: " + face.getBoundingBox().top
                    + " left: " + face.getBoundingBox().left
                    + " bottom: " + face.getBoundingBox().bottom
                    + " right: " + face.getBoundingBox().right);
            canvas = surfaceHolder.lockCanvas();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawRect(box, linePaint);
            if(canvas!=null){
                surfaceHolder.unlockCanvasAndPost(canvas);
            }

        }
        //imageView.setImageBitmap(bitmap);
    }

    private float translateY(float y) {
        return y * heightScaleFactor;
    }

    private float translateX(float x) {
        float scaledX = x * widthScaleFactor;
        if (lens == CameraSelector.LENS_FACING_FRONT) {
            //FRONT
            return canvas.getWidth() - scaledX;
        } else {
            return scaledX;
        }


    }


}
