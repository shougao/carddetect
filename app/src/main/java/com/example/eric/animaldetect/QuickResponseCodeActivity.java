package com.example.eric.animaldetect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.eric.animaldetect.qrcode.QRCodeDecoder;
import com.example.eric.animaldetect.qrcode.QRManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class QuickResponseCodeActivity extends Activity {

    private static final String TAG = "zqc";//"QuickResponseCodeActivity";

    @BindView(R.id.quick_response_result)
    public TextView mCode;
    private QRManager qrManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_response_code);
        ButterKnife.bind(this);
        qrManager = new QRManager();
    }

    @Override
    protected void onResume() {
        super.onResume();


        qrManager.parseQRcode(getQRsrc(), null);
    }

    @OnClick({R.id.quick_response_result})
    public void onViewClick(View view) {
        int vId = view.getId();
        switch (vId) {
            case R.id.quick_response_result:

                break;
        }
    }

    private Bitmap getQRsrc() {
        return BitmapFactory.decodeResource(getResources(), R.drawable.generate);
    }


}
