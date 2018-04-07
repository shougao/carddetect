package com.example.eric.animaldetect;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HelloOpenCVActivity extends Activity {

    private static final String TAG = "HelloOpenCVActivity";
    @BindView(R.id.HelloOpenCvView)
    CameraBridgeViewBase mOpenCvCameraView;

    private LoaderCallbackInterface mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                // OpenCV引擎初始化加载成功
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully.");
                    // 连接到Camera
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello);

        // 全屏显示
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 绑定View
        ButterKnife.bind(this);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        // 注册Camera连接状态事件监听器
        mOpenCvCameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {


            @Override
            public void onCameraViewStarted(int width, int height) {
            }

            @Override
            public void onCameraViewStopped() {
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                return inputFrame.rgba();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // OpenCVLoader.initDebug()静态加载OpenCV库
        // OpenCVLoader.initAsync()为动态加载OpenCV库，即需要安装OpenCV Manager
        if (OpenCVLoader.initDebug()) {
            Log.w(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.w(TAG, "static loading library fail,Using Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 断开与Camera的连接
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 断开与Camera的连接
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }
}

