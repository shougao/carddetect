package com.example.eric.animaldetect;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.app.Activity;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.eric.animaldetect.qrcode.QRManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.opencv.core.Core.mean;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;

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

    @BindView(R.id.btn_contour_detect)
    Button mBUttonContour;

    @BindView(R.id.btn_getCT)
    Button mGetCT;


    @BindView(R.id.firstImageView)
    ImageView mFirstImageView;

    @BindView(R.id.secondImageView)
    ImageView mSecondImageView;

    private Mat mOriginMat;
    private Mat mGrayMat;
    private Mat mBinaryMat;
    private Mat merodeMat;

    private int mX; //假设二维码识别到的向下尺寸距离为 X像素;

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
        mFirstImageView.setImageBitmap(bitmap);
    }

    private void showSmallImage(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(0.25f, 0.25f);
        Bitmap smallBitmap = bitmap.createBitmap(bitmap, 0, 0, bitmap.getHeight(), bitmap.getWidth(), matrix, true);
        mFirstImageView.setImageBitmap(smallBitmap);
    }

    private void showSmallImage(Mat src) {
        showBitmapImage(MatToBitmap(src));
    }


    private void showSmallImageSecond(Mat src) {
        mSecondImageView.setImageBitmap(MatToBitmap(src));
    }

    @OnClick({R.id.btn_grayImage, R.id.btn_binaryImage, R.id.btn_erodeImage,
            R.id.btn_filterandcut, R.id.btn_origin,
            R.id.btn_multiqrcode, R.id.btn_cut_qrcode, R.id.btn_getCT,
            R.id.btn_contour_detect})
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
            case R.id.btn_getCT:
                getCT();
                break;
            case R.id.btn_contour_detect:
                Mat target = getBigContour();
                showSmallImage(target);
                break;
        }
    }

    //========================提取大轮廓

    private Mat getBigContour(){
        int thresh = 0;
        int maxval = 255;

        Mat result = new Mat();

        Mat src = bitmapToMat(getAllMultiQRsrcImage());
        Mat origin = src.clone();
        //1 灰度处理
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY, 1); //最后一个参数标示图像通道数

        //2 阈值处理，使用二值化和OTSU自适应算法
        Imgproc.threshold(src, src, thresh, maxval, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        //3 轮廓提取
        Imgproc.Canny(src, src, 20, 60);

        //4 获取最大轮廓


        return result;
    }



    // multi qrcode reganize.
    private Bitmap getAllMultiQRsrcImage() {
        return BitmapFactory.decodeResource(getResources(), R.drawable.all_photo);
    }

    //========================

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

    private Mat cutAndReduceImage(Mat src, Rect rect) {
        return cutImage(src, rect.x + (int) (rect.width * 0.1), rect.y + (int) (rect.height * 0.1), (int) (rect.width * 0.8), (int) (rect.height * 0.8));
    }

    /**
     * 竖向5等分图片
     * 把一行五个的二维码，变为五个二维码图片。
     *
     * @param srcMat
     * @return
     */
    private ArrayList<Mat> getImageList(Mat srcMat) {
        ArrayList<Mat> images = new ArrayList<Mat>();

        for (int i = 0; i < 5; i++) {
            Mat tmp = cutImage(srcMat, srcMat.cols() / 5 * i, 0, srcMat.cols() / 5, srcMat.rows());
            images.add(tmp.clone());
        }
        return images;
    }

    // multi qrcode reganize.
    private Bitmap getMultiQRsrcImage() {
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
        Mat srcMat = bitmapToMat(getMultiQRsrcImage());
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
        Imgproc.findContours(dstMatOTSU, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);


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

    /**
     * 解析每一个二维码
     *
     * @return
     */
    private Mat cutQrcode() {
        //1 把五个卡上的二维码切到一行上。
        Mat srcMat = bitmapToMat(getMultiQRsrcImage());
        srcMat = cutImage(srcMat, 0, 0, srcMat.cols(), srcMat.cols() / 5);//因为一行五个卡
        Mat dstMatGray = new Mat(srcMat.rows(), srcMat.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(srcMat, dstMatGray, Imgproc.COLOR_BGR2GRAY, 1);


        //2 把一行的五个二维码做阈值处理
        Mat dstMatOTSU = new Mat(srcMat.rows(), srcMat.cols(), CvType.CV_8UC1);
        Imgproc.threshold(dstMatGray, dstMatOTSU, OTSUmax1, OTSUmax2, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        showSmallImage(dstMatOTSU);


        //3 把一行图像转换成五个二维码bitmap图像。
        ArrayList<Bitmap> bpList = new ArrayList<>();
        for (Mat image : getImageList(dstMatOTSU)) {
            Bitmap bp = MatToBitmap(image);
            bpList.add(bp);
            showSmallImage(image);
        }

        // 4 解析并获取每一个二维码值
        QRManager manager = new QRManager();
        for (Bitmap bp : bpList) {
            manager.parseQRcode(bp, new QRManager.IQRListener() {
                @Override
                public void onSuccess(String result) {
                    // 5 回调显示每一个二维码值给JS
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


    private void debugShow(Mat mat) {
        showSmallImage(mat);
        if (true) {
            return;
        }
    }

    private void getCT() {
        // 1.获取没有周围无用边框的原图
        Mat srcMat = bitmapToMat(getMultiQRsrcImage());
        Mat dstMatGray = new Mat(srcMat.rows(), srcMat.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(srcMat, dstMatGray, Imgproc.COLOR_BGR2GRAY, 1);

//        // 使用高斯滤波, 去除过多噪声和纹理, 保证找到的线比较直
//        Mat gausMat = new Mat(dstMatGray.rows(), dstMatGray.cols(), CvType.CV_8UC1);
//        Imgproc.GaussianBlur(dstMatGray, gausMat, new Size(3, 3), 2, 2);


        // 3 竖着平均切割遍历每一张带二维码的图像
        ArrayList<Mat> srcMatList = getImageList(dstMatGray);
        ArrayList<Bitmap> bpList = new ArrayList<>();
        for (Mat image : getImageList(dstMatGray)) {
            Bitmap bp = MatToBitmap(image);
            bpList.add(bp);
        }

        // 2 获取图像的自动阈值处理图像。
//        Mat image = srcMatList.get(3);//假设遍历得到的是第四个。
        ArrayList<Double> grayValueList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject();
        for (Mat image : srcMatList) {
            double grayValue = calcGray(image);
            grayValueList.add(grayValue);
        }

        for (int i = 0; i < grayValueList.size(); i++) {
            try {
                jsonObject.put(String.valueOf(i), grayValueList.get(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private double calcGray(Mat image) {

        Mat targetImage = image.clone();
//        Mat image = new Mat(srcMat.rows(), srcMat.cols(), CvType.CV_8UC1);
        Imgproc.threshold(image, image, OTSUmax1, OTSUmax2, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
//        showSmallImage(image);


        // 4 根据二维码读到的标记尺寸，遍历切割到CT所在的大致位置, 假设二维码识别到的向下尺寸距离都为 mX 像素
        System.out.println(TAG + "image info:" + ", heigh=" + image.height() + ", width=" + image.width()); // heigh=3960, width=827
        Mat ctImage = cutImage(image, image.width() / 5, image.height() / 2, image.width() / 2 - image.width() / 6, image.height() / 6); // 定位CT具体位置
        Mat ctImageTarget = cutImage(targetImage, targetImage.width() / 5, targetImage.height() / 2, targetImage.width() / 2 - targetImage.width() / 6, targetImage.height() / 6); // 定位CT具体位置


        // 5 获取CT的精确定位
        // 使用canny边缘检测
        Mat edgeMat = ctImage.clone();
        Imgproc.Canny(ctImage, edgeMat, 20, 60, 3, false);
        // 膨胀，连接边缘
//        Imgproc.dilate(edgeMat, edgeMat, new Mat(), new Point(-1,-1), 3, 1, new Scalar(1));


        // 6 轮廓提取
        List<MatOfPoint> mCTContours = new ArrayList<>();
        Mat hierarchy2 = new Mat();
        Imgproc.findContours(edgeMat, mCTContours, hierarchy2, RETR_EXTERNAL/*外部轮廓*/, CHAIN_APPROX_SIMPLE);
        Collections.sort(mCTContours, mCTComparator);
        System.out.println("zqc contours size = " + mCTContours.size());


        // 7 找到位置坐标。
        // 方法一
        int indexCT = 0;
        Mat mMatC = null;
        Mat mMatT = null;
        Mat mMatBackground = null;


        // 用于在手机上显示切割后的结果。
        for (MatOfPoint contour : mCTContours) {
            Rect rect = Imgproc.boundingRect(contour);
            System.out.println(indexCT + "   zqc find rect, rect.x = " + rect.x + ", rect.y=" + rect.y + ", width=" + rect.width + ", heigh=" + rect.height);

            // 宽高各减小20%，进行灰度值的计算范围
            switch (indexCT) {
                case 0:
                    mMatC = cutAndReduceImage(ctImageTarget, rect);
                    showSmallImage(mMatC);
//                    getInnerGrayValue(mMatC);
                    break;
                case 1:
//                    mMatT = cutImage(ctImageTarget, rect.x, rect.y, rect.width, rect.height);
                    mMatT = cutAndReduceImage(ctImageTarget, rect);
                    showSmallImageSecond(mMatT);
                    break;
            }
            indexCT++;
        }


        // 8 提取CT线的灰度值, C
        double mGrayCValue = 0.0;
        if (mMatC != null) {
            mGrayCValue = getInnerGrayValue(mMatC);
        }

        // 8 提取CT线的灰度值, T
        double mGrayTValue = 0.0;
        if (mMatT != null) {
            mGrayTValue = getInnerGrayValue(mMatT);
        }

        // 如果C线没有，则按照T线向下的位置距离强制提取
//        if(mCTContours.size() == 1){
//            mCTContours.add(1, );
//        }

        // 9 获取CT线上中下三处的底色值， 最小值为底色值。
        // 仅获取中间值， 其他位置， 以及删除污染底色todo
        Rect rectC = Imgproc.boundingRect(mCTContours.get(0));
        Rect rectT;
        if (mCTContours.size() == 1) {
            rectT = new Rect(137, 537, 119, 45);//经验值 rect.x = 137, rect.y=537, width=119, heigh=45
        } else {
            rectT = Imgproc.boundingRect(mCTContours.get(1));
        }
        System.out.println("background size: x=" + rectC.x + ", y=" + (rectC.y + rectC.height) + ", width=" + rectC.width + ", heigh=" + (rectT.y - rectC.y - rectC.height));
        mMatBackground = cutImage(ctImageTarget, rectC.x, rectC.y + rectC.height, rectC.width, rectT.y - rectC.y - rectC.height);
        double mGrayBackgroundValue = 0.0;
        if (mMatBackground != null) {
            mGrayBackgroundValue = getInnerGrayValue(mMatBackground);
        }
        showSmallImage(mMatBackground);


        // 10 使用前景值减去背景值
        double finalGray = getFinalValue(mGrayTValue, mGrayBackgroundValue);
        System.out.println("zqc finalGray=" + finalGray);
        return finalGray;
    }


    private double getFinalValue(double tValue, double background) {
        return (255 - tValue) - (255 - background);
    }

    /**
     * 矩形的Y值从小到大排序
     */
    private Comparator<MatOfPoint> mCTComparator = new Comparator<MatOfPoint>() {
        @Override
        public int compare(MatOfPoint contour1, MatOfPoint contour2) {
            Rect rect1 = Imgproc.boundingRect(contour1);
            Rect rect2 = Imgproc.boundingRect(contour2);
            if (rect1.y > rect2.y) {
                return 1;
            } else {
                return -1;
            }
        }
    };

    private double getInnerGrayValue(Mat srcMat) {
        double result = 0.0;

        MatOfDouble mu = new MatOfDouble();
        MatOfDouble sigma = new MatOfDouble();
        Core.meanStdDev(srcMat, mu, sigma);
        Scalar resultScalar = Core.mean(srcMat);
        result = resultScalar.val[0];//scala里存放的是每个通道的颜色值， 单通道的就取下标为0的就ok
//        double d = mu.get(0, 0)[0];
        System.out.println("zqc the gray for mean value is: " + result + ", [0]=" + resultScalar.val[0] + ", [1]=" + resultScalar.val[1] + "[2]" + resultScalar.val[2] + "[3]" + resultScalar.val[3]);
        return result;
    }

    /**
     * CT的面积经验值，限制其他小的矩形。
     *
     * @param approxf1
     * @return
     */
    private boolean checkArea(MatOfPoint2f approxf1) {
        return Math.abs(Imgproc.contourArea(approxf1)) > 4000;
    }


    // 根据三个点计算中间那个点的夹角   pt1 pt0 pt2
    private static double getAngle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    // 找到最大的正方形轮廓
    private static int findLargestSquare(List<MatOfPoint> squares) {
        if (squares.size() == 0)
            return -1;
        int max_width = 0;
        int max_height = 0;
        int max_square_idx = 0;
        int currentIndex = 0;
        for (MatOfPoint square : squares) {
            Rect rectangle = Imgproc.boundingRect(square);
            if (rectangle.width >= max_width && rectangle.height >= max_height) {
                max_width = rectangle.width;
                max_height = rectangle.height;
                max_square_idx = currentIndex;
            }
            currentIndex++;
        }
        return max_square_idx;
    }

}