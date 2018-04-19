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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_response_code);
        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        parsePhoto(getQRsrc());
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

    private void parsePhoto(final Bitmap bitmap) {

        Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                //解析二维码
                String result = QRCodeDecoder.syncDecodeQRcode(bitmap);
                if (!TextUtils.isEmpty(result)) {
                    e.onNext(result);
                }
                e.onComplete();
            }
        });

        // 1. 定义任务
        Observable<String> observable = new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
                //解析二维码
                String result = QRCodeDecoder.syncDecodeQRcode(bitmap);
                if (!TextUtils.isEmpty(result)) {
                    observer.onNext(result);
                }
            }
        };

        // 2. 启动任务
//        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
        observable2.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
    }

    // 3. 任务回调
    private Observer<String> observer = new Observer<String>() {
        @Override
        public void onSubscribe(Disposable d) {
            Log.d(TAG, "onSubscribe");
            Toast.makeText(getApplicationContext(), "onSubscribe", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNext(String value) {
            Toast.makeText(getApplicationContext(), "code=" + value, Toast.LENGTH_SHORT).show();

            Log.d(TAG, "onNext， value=" + value);
            if (!TextUtils.isEmpty(value)) {
                mCode.setText(getString(R.string.qrcode) + value);
                Toast.makeText(getApplicationContext(), "识别到二维码/条码", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onError(Throwable e) {
            Toast.makeText(getApplicationContext(), "onError", Toast.LENGTH_SHORT).show();

            Log.d(TAG, "onError");
        }

        @Override
        public void onComplete() {
            Toast.makeText(getApplicationContext(), "onComplete", Toast.LENGTH_SHORT).show();

            Log.d(TAG, "onComplete");
        }
    };


}
