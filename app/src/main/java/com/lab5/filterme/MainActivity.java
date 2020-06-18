package com.lab5.filterme;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static org.opencv.android.CameraBridgeViewBase.CAMERA_ID_FRONT;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";
    private static String successMessage = "Photo Taken ;)";
    private static String errorMessage = "Error saving photo :(";
    JavaCameraView mJavaCameraView;
    Mat mRgba, mask, mGray;
    int absoluteFaceSize = 0;
    CascadeClassifier faceRec;
    ImageButton cameraButton, galleryButton;
    Boolean success = false;
    double minFaceSize = 20.0;
    double maxFaceSize = 200.0;
    private BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            final int MY_CAMERA_REQUEST_CODE = 100;
            final int WRITE_EXTERNAL_STORAGE_CODE = 100;

            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
            }

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},WRITE_EXTERNAL_STORAGE_CODE);
            }

            switch (status) {
                /*case BaseLoaderCallback.SUCCESS:
                    mJavaCameraView.enableView();
                    loadCascade();
                    break;*/
                case LoaderCallbackInterface.SUCCESS:
                    mJavaCameraView.enableView();
                    mask = new Mat();
                    loadCascade();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    //take image frame from camera modify it and display it on the screen.
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //Rotating the input frame
        Mat mGray = inputFrame.gray();
        mRgba = inputFrame.gray();

        Core.flip(mRgba, mRgba, 1);
        Core.flip(mGray, mGray, 1);

        //Detecting face in the frame
        MatOfRect faces = new MatOfRect();
        if(faceRec != null)
        {
            faceRec.detectMultiScale(mGray, faces, 1.1, 2, 2, new Size(200,200), new Size());
        }

        Rect[] facesArray = faces.toArray();
        Scalar color = new Scalar( 216, 41, 41 );
        for (int i = 0; i < facesArray.length; i++) {
            minFaceSize = facesArray[i].width * 0.7;
            maxFaceSize = facesArray[i].height * 1.5;
            Point centre = new Point(facesArray[i].x +facesArray[i].width * 0.5, facesArray[i].y + facesArray[i].height * 0.55);
            Imgproc.rectangle(mRgba, new Point(centre.x - facesArray[i].width / 2, centre.y - facesArray[i].width / 2),
                    new Point(centre.x + facesArray[i].width / 2, centre.y + facesArray[i].width / 2), color);
        }
        return mRgba;
        // take captures image frame from camera and turn it into grayscale
        //return inputFrame.gray();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind variable with layout
        mJavaCameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
        // Make camera visible
        mJavaCameraView.setVisibility(SurfaceView.VISIBLE);
        // Set Frontal Camera
        mJavaCameraView.setCameraIndex(CAMERA_ID_FRONT);
        // Set a listener for the camera
        mJavaCameraView.setCvCameraViewListener(this);



        // Set Up Camera Button
        cameraButton = (ImageButton)findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Take a picture :)
                Mat mIntermediateMat = new Mat();
                Imgproc.cvtColor(mRgba, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);

                // Get current Date and Time
                String currentDateTimeString = new SimpleDateFormat("YYYYMMDDHHmmss").format(new Date());

                // Create Path + Filename
                String filename = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/Camera/";
                filename += currentDateTimeString + ".jpg";

                // Save Image
                success = Imgcodecs.imwrite(filename, mIntermediateMat);

                if (success == true) {
                    Toast.makeText(getApplicationContext(), successMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "SUCCESS writing image to external storage");
                }
                else {
                    Log.d(TAG, "Fail writing image to external storage");
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Set Up Gallery Button
        galleryButton = (ImageButton)findViewById(R.id.galleryButton);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to gallery :)
                Intent openGallery = new Intent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media"));
                startActivity(openGallery);
            }
        });
    }

    private void loadMask() {
        InputStream stream = null;
        Uri uri = Uri.parse("android.resource://com.lab5.filterme/drawable/round");
        try {
            stream = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bmp = BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);
        Utils.bitmapToMat(bmp, mask);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    protected void onPause() {
        super.onPause();

        if (mJavaCameraView != null) {
            mJavaCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mJavaCameraView != null) {
            mJavaCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV loaded successfully!");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            Log.i(TAG, "OpenCV NOT loaded successfully!");
            // Try to load OpenCV again in case of failure
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallBack);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    /*@Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        findFaces();
        return mRgba;
    }*/

    public void loadCascade() {
        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            faceRec = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if (faceRec.empty()) {
                Log.d(TAG, "Error loading FaceCascade");
                return;
            }
            else {
                Log.d(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    // Function used to detect faces
    public void findFaces() {
        // Load Mask
        loadMask();

        // Find Faces
        MatOfRect faces = new MatOfRect();
        Mat grey = new Mat();
        Imgproc.cvtColor(mRgba, grey, Imgproc.COLOR_RGB2BGR);
        faceRec.detectMultiScale(grey, faces, 1.2, 2, 0|2, new Size(0, 0), new Size(400, 400));

        List<Rect> faces_ =  faces.toList();

        // draw circles on the detected faces
        if (!faces_.isEmpty()) {
            for (int i = 0; i < faces_.size(); i++) {
                minFaceSize = faces_.get(i).width * 0.7;
                maxFaceSize = faces_.get(i).height * 1.5;
                Point centre = new Point(faces_.get(i).x + faces_.get(i).width * 0.5, faces_.get(i).y + faces_.get(i).height * 0.55);

//              // START JUNK
                Imgproc.rectangle(mRgba, new Point(centre.x - faces_.get(i).width / 2, centre.y - faces_.get(i).width / 2),
                       new Point(centre.x + faces_.get(i).width / 2, centre.y + faces_.get(i).width / 2), new Scalar(0,0,0));
//                // Create Path + Filename
                String filename = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/Camera/file.jpg";
//
//                // Save Image
                success = Imgcodecs.imwrite(filename, mRgba);
//                // END JUNK
            }
        }
    }
}