package com.example.eric.animaldetect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.eric.animaldetect.qrcode.QRManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

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

    @BindView(R.id.btn_multiqrcode)
    Button mMultiqrcode;

    @BindView(R.id.btn_cut_qrcode)
    Button mCutQRCode;

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
        showBitmapImage(getOriginBitmap());
    }

    private Bitmap getOriginBitmap() {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4; //缩小4倍
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.origin_image, options);
        return bitmap;
    }

    private void showBitmapImage(Bitmap bitmap) {
//        Matrix matrix = new Matrix();
//        matrix.postScale(0.25f, 0.25f);
//        Bitmap smallBitmap = bitmap.createBitmap(bitmap, 0, 0, bitmap.getHeight(), bitmap.getWidth(), matrix, true);
        mImageView.setImageBitmap(bitmap);
    }

    private void showSmallImage(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(0.25f, 0.25f);
        Bitmap smallBitmap = bitmap.createBitmap(bitmap, 0, 0, bitmap.getHeight(), bitmap.getWidth(), matrix, true);
        mImageView.setImageBitmap(smallBitmap);
    }

    private void showSmallImage(Mat src) {
        showBitmapImage(MatToBitmap(src));
    }

    @OnClick({R.id.btn_grayImage, R.id.btn_binaryImage, R.id.btn_erodeImage, R.id.btn_filterandcut, R.id.btn_origin, R.id.btn_multiqrcode, R.id.btn_cut_qrcode})
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
                displayCutImage(getOriginBitmap());
                break;
            case R.id.btn_origin:
                showOriginImage();
                break;
            case R.id.btn_multiqrcode:
                parseMultiQrcode();
                break;
            case R.id.btn_cut_qrcode:
                cutQrcode();
                break;
        }
    }

    private void displayGrayImage() {
        showBitmapImage(getBmpToGrayBmp(getOriginBitmap()));
    }

    /**
     * 显示灰度图像
     *
     * @return
     */
    private Bitmap getBmpToGrayBmp(Bitmap bitmap) {
        Mat srcMat = bitmapToMat(bitmap);
        Mat grayMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGRA2GRAY, 1);
        Bitmap dstBitmap = Bitmap.createBitmap(grayMat.cols(), grayMat.rows(), Bitmap.Config.ARGB_8888);
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

        Mat binaryMat = new Mat(grayMat.height(), grayMat.width(), CvType.CV_8UC1);
        Imgproc.threshold(grayMat, binaryMat, max1, max2, Imgproc.THRESH_BINARY);

        Bitmap dstBitmap = Bitmap.createBitmap(binaryMat.cols(), binaryMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(binaryMat, dstBitmap);
        showBitmapImage(dstBitmap);
    }

    /**
     * 图像腐蚀显示
     */
    private void displayErodeImage() {
        Bitmap bitmap = getOriginBitmap();
        Mat srcMat = bitmapToMat(bitmap);
        Mat grayMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGRA2GRAY, 1);

        Mat binaryMat = new Mat(grayMat.height(), grayMat.width(), CvType.CV_8UC1);
        Imgproc.threshold(grayMat, binaryMat, max1, max2, Imgproc.THRESH_BINARY);

        Mat destMat = new Mat(); // 腐蚀后的图像
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.erode(binaryMat, destMat, element);

        Bitmap dstBitmap = Bitmap.createBitmap(destMat.cols(), destMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(destMat, dstBitmap);
        showBitmapImage(dstBitmap);
    }


    /**
     * 显示切割的图像
     */
    private void displayCutImage(Bitmap bitmap) {
        Mat srcMat = bitmapToMat(bitmap);
        Mat grayMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGRA2GRAY, 1);

        Mat binaryMat = new Mat(grayMat.height(), grayMat.width(), CvType.CV_8UC1);
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


        Bitmap dstBitmap = Bitmap.createBitmap(resMat.cols(), resMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resMat, dstBitmap);
        showBitmapImage(dstBitmap);
    }

    private Mat cutImage(Mat srcMat, int startX, int startY, int width, int heigh) {
        //切割图像
        // 参数,坐标X,坐标Y,截图宽度,截图长度, 假设裁掉上下大小一样
        Rect rect = new Rect(startX, startY, width, heigh);
        Mat resMat = new Mat(srcMat, rect);
        return resMat;
    }

    /**
     * 把一行五个的二维码，变为五个二维码图片。
     * @param srcMat
     * @return
     */
    private ArrayList<Mat> getQRImages(Mat srcMat){
        ArrayList<Mat> images = new ArrayList<Mat>();

        for(int i=0; i<5; i++){
            Mat tmp = cutImage(srcMat, srcMat.cols() / 5 * i, 0, srcMat.cols() / 5, srcMat.rows());
            images.add(tmp.clone());
        }
        return images;
    }

    // multi qrcode reganize.
    private Bitmap getMultiQRsrc() {
        return BitmapFactory.decodeResource(getResources(), R.drawable.originpicture_multi_qr_small);
    }

    private int OTSUmax1 = 0;
    private int OTSUmax2 = 255;

    /**
     * 1. 首先把输入图像转换为灰度图像
     * 2. 通过OTSU转换为二值图像
     * 3. 对二值图像使用轮廓发现得到轮廓
     * 4. 根据二维码三个区域的特征，对轮廓进行面积与比例过滤得到最终结果显示
     */
    private void parseMultiQrcode() {
        Mat srcMat = bitmapToMat(getMultiQRsrc());
        Mat dstMatGray = new Mat(srcMat.rows(), srcMat.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(srcMat, dstMatGray, Imgproc.COLOR_BGR2GRAY, 1);

        Mat original = srcMat.clone();


//        showSmallImage(dstMatGray);//1. 首先把输入图像转换为灰度图像
        Mat dstMatOTSU = new Mat(srcMat.rows(), srcMat.cols(), CvType.CV_8UC1);
        Imgproc.threshold(dstMatGray, dstMatOTSU, OTSUmax1, OTSUmax2, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        showSmallImage(dstMatOTSU);
        if (true) {
            return;
        }

        // find the center of the image
        double[] centers = {(double) dstMatOTSU.width() / 2, (double) dstMatOTSU.height() / 2};
        Point image_center = new Point(centers);

//        // 使用高斯滤波, 去除过多噪声和纹理，
//        Mat gausMat = new Mat(srcMat.rows(), srcMat.cols(), CvType.CV_8UC1);
//        Imgproc.GaussianBlur(dstMatGray, gausMat, new Size(3,3), 2, 2);
//        showSmallImage(gausMat);
        if (true) {
            return;
        }

        // finding the contours
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(dstMatOTSU, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


        // finding best bounding rectangle for a contour whose distance is closer to the image center that other ones
        double d_min = Double.MAX_VALUE;
        Rect rect_min = new Rect();
        for (MatOfPoint contour : contours) {
            Rect rec = Imgproc.boundingRect(contour);
            // find the best candidates
            if (rec.height > srcMat.height() / 2 & rec.width > srcMat.width() / 2)
                continue;
            Point pt1 = new Point((double) rec.x, (double) rec.y);
            Point center = new Point(rec.x + (double) (rec.width) / 2, rec.y + (double) (rec.height) / 2);
            double d = Math.sqrt(Math.pow((double) (pt1.x - image_center.x), 2) + Math.pow((double) (pt1.y - image_center.y), 2));
            if (d < d_min) {
                d_min = d;
                rect_min = rec;
            }
        }
        // slicing the image for result region
        int pad = 6;
        rect_min.x = rect_min.x - pad;
        rect_min.y = rect_min.y - pad;

        rect_min.width = rect_min.width + 2 * pad;
        rect_min.height = rect_min.height + 2 * pad;

        Mat result = original.submat(rect_min);
        showSmallImage(result);
    }

    private Mat cutQrcode() {
        //把五个卡上的二维码切到一行上。
        Mat srcMat = bitmapToMat(getMultiQRsrc());
        srcMat = cutImage(srcMat, 0, 0, srcMat.cols(), srcMat.cols()/5);//因为一行五个卡
        Mat dstMatGray = new Mat(srcMat.rows(), srcMat.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(srcMat, dstMatGray, Imgproc.COLOR_BGR2GRAY, 1);


        //把一行的五个二维码做阈值处理
        Mat dstMatOTSU = new Mat(srcMat.rows(), srcMat.cols(), CvType.CV_8UC1);
        Imgproc.threshold(dstMatGray, dstMatOTSU, OTSUmax1, OTSUmax2, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        showSmallImage(dstMatOTSU);


        //把一行图像转换成五个二维码bitmap图像。
        ArrayList<Bitmap> bpList = new ArrayList<>();
        for(Mat image : getQRImages(dstMatOTSU)){
            Bitmap bp = MatToBitmap(image);
            bpList.add(bp);
            showSmallImage(image);
        }

        QRManager manager = new QRManager();
        for (Bitmap bp : bpList) {
            manager.parseQRcode(bp, new QRManager.IQRListener() {
                @Override
                public void onSuccess(String result) {
                    System.out.println(TAG + "to js, qrcode=" + result);
                }

                @Override
                public void onFailed() {
                    System.out.println(TAG + "to js, qrcode failed.");
                }
            });
            showBitmapImage(bp);
        }

        return dstMatGray;
    }

}