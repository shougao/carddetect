package com.example.eric.animaldetect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AnimalActivity extends Activity {

    private static final String TAG = "AnimalActivityLog";

    @BindView(R.id.btn_grayImage)
    Button mGray;

    @BindView(R.id.btn_binaryImage)
    Button mBinary;

    @BindView(R.id.btn_erodeImage)
    Button mErode;

    @BindView(R.id.btn_filterandcut)
    Button mCut;

    @BindView(R.id.btn_origin)
    Button mOrigin;

    @BindView(R.id.imageView)
    ImageView mImageView;

    private Mat mOriginMat;
    private Mat mGrayMat;
    private Mat mBinaryMat;
    private Mat merodeMat;

    private LoaderCallbackInterface mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                // OpenCV引擎初始化加载成功
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully.");
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
        setContentView(R.layout.activity_animal);

        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showOriginImage();

        if (OpenCVLoader.initDebug()) {
            Log.w(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.w(TAG, "static loading library fail,Using Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }
    }

    private void showOriginImage() {
        showImage(getOriginBitmap());
    }

    private Bitmap getOriginBitmap() {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4; //缩小4倍
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.origin_image, options);
        return bitmap;
    }

    private void showImage(Bitmap bitmap) {
//        Matrix matrix = new Matrix();
//        matrix.postScale(0.25f, 0.25f);
//        Bitmap smallBitmap = bitmap.createBitmap(bitmap, 0, 0, bitmap.getHeight(), bitmap.getWidth(), matrix, true);
        mImageView.setImageBitmap(bitmap);
    }


    @OnClick({R.id.btn_grayImage, R.id.btn_binaryImage, R.id.btn_erodeImage, R.id.btn_filterandcut, R.id.btn_origin})
    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.btn_grayImage:
                displayGrayImage();
                break;
            case R.id.btn_binaryImage:
                displayThreshold();
                break;
            case R.id.btn_erodeImage:
                displayErodeImage();
                break;
            case R.id.btn_filterandcut:
                displayCutImage();
                break;
            case R.id.btn_origin:
                showOriginImage();
                break;
        }
    }

    private void displayGrayImage() {
        showImage(getGrayBmp());
    }

    /**
     * 显示灰度图像
     * @return
     */
    private Bitmap getGrayBmp(){
        Bitmap bitmap = getOriginBitmap();
        Mat srcMat = bitmapToMat(bitmap);
        Mat grayMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGRA2GRAY, 1);
        Bitmap dstBitmap = Bitmap.createBitmap(grayMat.cols(), grayMat.rows(), Bitmap.Config.ARGB_8888 );
        Utils.matToBitmap(grayMat, dstBitmap);
        return dstBitmap;
    }

    //Bitmap转换成Mat
    public Mat bitmapToMat(Bitmap bitmap) {
        Mat rgbMat = new Mat();
        Utils.bitmapToMat(bitmap, rgbMat);
        return rgbMat;
    }

    //Mat转成Bitmap
    public Bitmap MatToBitmap(Mat src) {
        Bitmap bitmap = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, bitmap);
        return bitmap;
    }

    private static final int max1 = 100; //阈值
    private static final int max2 = 110; //灰度值

    /**
     * 显示二值化图像
     */
    private void displayThreshold() {
        Bitmap bitmap = getOriginBitmap();
        Mat srcMat = bitmapToMat(bitmap);
        Mat grayMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGRA2GRAY, 1);

        Mat binaryMat = new Mat(grayMat.height(),grayMat.width(),CvType.CV_8UC1);
        Imgproc.threshold(grayMat, binaryMat, max1, max2, Imgproc.THRESH_BINARY);

        Bitmap dstBitmap = Bitmap.createBitmap(binaryMat.cols(), binaryMat.rows(), Bitmap.Config.ARGB_8888 );
        Utils.matToBitmap(binaryMat, dstBitmap);
        showImage(dstBitmap);
    }

    /**
     * 图像腐蚀显示
     */
    private void displayErodeImage() {
        Bitmap bitmap = getOriginBitmap();
        Mat srcMat = bitmapToMat(bitmap);
        Mat grayMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGRA2GRAY, 1);

        Mat binaryMat = new Mat(grayMat.height(),grayMat.width(),CvType.CV_8UC1);
        Imgproc.threshold(grayMat, binaryMat, max1, max2, Imgproc.THRESH_BINARY);

        Mat destMat = new Mat(); // 腐蚀后的图像
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.erode(binaryMat, destMat, element);

        Bitmap dstBitmap = Bitmap.createBitmap(destMat.cols(), destMat.rows(), Bitmap.Config.ARGB_8888 );
        Utils.matToBitmap(destMat, dstBitmap);
        showImage(dstBitmap);
    }


    /**
     * 显示切割的图像
     */
    private void displayCutImage() {
        Bitmap bitmap = getOriginBitmap();
        Mat srcMat = bitmapToMat(bitmap);
        Mat grayMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGRA2GRAY, 1);

        Mat binaryMat = new Mat(grayMat.height(),grayMat.width(),CvType.CV_8UC1);
        Imgproc.threshold(grayMat, binaryMat, max1, max2, Imgproc.THRESH_BINARY);

        Mat destMat = new Mat(); // 腐蚀后的图像
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.erode(binaryMat, destMat, element);

        //切割图像
        int a = 0, b = 0, state = 0;
        System.out.println(TAG + " destMat.width() = " + destMat.width());
        System.out.println(TAG + " destMat.height() = " + destMat.height());
        System.out.println("AnimalActivityLog, 过滤下界" + Integer.toString(a));
        System.out.println("AnimalActivityLog, 过滤上界" + Integer.toString(b));

        int widthvalue = 250;
        b = 2000;
        a = 800;
        // 参数,坐标X,坐标Y,截图宽度,截图长度
        Rect rect = new Rect(widthvalue, a, destMat.width() - widthvalue * 2, b - a);
        Mat resMat = new Mat(destMat, rect);


        Bitmap dstBitmap = Bitmap.createBitmap(resMat.cols(), resMat.rows(), Bitmap.Config.ARGB_8888 );
        Utils.matToBitmap(resMat, dstBitmap);
        showImage(dstBitmap);
    }
}
