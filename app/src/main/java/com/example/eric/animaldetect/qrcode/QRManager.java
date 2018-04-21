package com.example.eric.animaldetect.qrcode;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.example.eric.animaldetect.LogUtil;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class QRManager {

    private static final String TAG = "QRManager";

    public interface IQRListener {
        public void onSuccess(String result);
        public void onFailed();
    }

    public void parseQRcode(final Bitmap bitmap, final IQRListener listener) {

        // 1. 定义任务
        Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                //解析二维码
                String result = QRCodeDecoder.syncDecodeQRcode(bitmap);
                if (!TextUtils.isEmpty(result)) {
                    e.onNext(result);
                    if (listener != null) {
                        listener.onSuccess(result);
                    }
                } else {
                    if (listener != null) {
                        listener.onFailed();
                    }
                }
                e.onComplete();
            }
        });

        // 2. 启动任务
        observable2.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
    }

    // 3. 任务回调
    private Observer<String> observer = new Observer<String>() {
        @Override
        public void onSubscribe(Disposable d) {
            LogUtil.d(TAG, "onSubscribe");
        }

        @Override
        public void onNext(String value) {
            if (!TextUtils.isEmpty(value)) {
                LogUtil.d(TAG, "onNext，qrcode value=" + value);
            }
        }

        @Override
        public void onError(Throwable e) {
            LogUtil.d(TAG, "onError");
        }

        @Override
        public void onComplete() {
            LogUtil.d(TAG, "onComplete");
        }
    };
}