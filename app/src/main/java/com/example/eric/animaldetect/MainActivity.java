package com.example.eric.animaldetect;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity {

    @BindView(R.id.btn_hello)
    Button mBtnHello;

    @BindView(R.id.btn_faceDetect)
    Button mBtnFaceDetect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 绑定View
        ButterKnife.bind(this);
    }

    @OnClick({R.id.btn_hello,R.id.btn_faceDetect})
    public void onViewClick(View view) {
        int vId = view.getId();
        switch (vId) {
            case R.id.btn_hello:
                Intent intentHello = new Intent(MainActivity.this, HelloOpenCVActivity.class);
                startActivity(intentHello);
                break;
            case R.id.btn_faceDetect:
                Intent intentFd = new Intent(MainActivity.this,FaceDetectActivity.class);
                startActivity(intentFd);
                break;
        }
    }
}
