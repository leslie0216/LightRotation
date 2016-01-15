package usask.chl848.lightrotation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.ArrayList;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{

    protected static final int REQUEST_ENABLE_BLUETOOTH = 21;

    public enum Mode {
        COMPASS, //pure compass
        LIGHT,  // pure light
        COMPASSLIGHT, // compass+light
        AUTO //auto switch
    }

    private Mode m_mode;

    public enum LockMode {
        NONE, // no lock
        STATIC, // enbable lock button
        DYNAMIC // lock when touch ball
    }
    private LockMode m_lockMode;

    public enum PassMode {
        SINGLE,
        MULTIPLE
    }
    private PassMode m_passMode;

    private String m_userName;
    private String m_userId;
    private int m_userColor;
    private boolean m_isLogEnabled;

    private Button m_debugBtn;
    private boolean m_isDebugMode;

    private Button m_lockBtn;
    private boolean m_isLocked = false;

    /**
     * sensors begin
     */
    private SensorData m_sensorData;
    /**
     * sensors end
     */

    public static final String  TAG = "LightRotation";
    private CameraBridgeViewBase mOpenCvCameraView;
    public MainView m_drawView;

    public BluetoothClientData m_bluetoothData;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!m_isLocked) {
                if (m_bluetoothData.isMessageListEmpty()) {
                    m_drawView.cookLocationMsg();
                }
                m_bluetoothData.sendMessage();
            }
            m_drawView.invalidate();
            timerHandler.postDelayed(this, 200);
        }
    };

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /**
     * experiment begin
     */
    private Button m_startBtn;
    private Button m_continueBtn;
    /**
     * experiment end
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //setContentView(R.layout.activity_main);

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle.containsKey("user")) {
            m_userName = bundle.getString("user");
        }
        if (bundle.containsKey("id")) {
            m_userId = bundle.getString("id");
        }
        if (bundle.containsKey("color")) {
            m_userColor = bundle.getInt("color");
        }
        if (bundle.containsKey("mode")) {
            String mode = bundle.getString("mode");
            m_mode = Mode.valueOf(mode.toUpperCase());
        } else {
            m_mode = Mode.COMPASS;
        }
        if (bundle.containsKey("lockMode")) {
            String lockmode = bundle.getString("lockMode");
            m_lockMode = LockMode.valueOf(lockmode.toUpperCase());
        } else {
            m_lockMode = LockMode.NONE;
        }
        if (bundle.containsKey("passMode")) {
            String passmode = bundle.getString("passMode");
            m_passMode = PassMode.valueOf(passmode.toUpperCase());
        } else {
            m_passMode = PassMode.MULTIPLE;
        }
        m_isLogEnabled = bundle.containsKey("isLogEnabled") && bundle.getBoolean("isLogEnabled");

       // mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        if (m_mode != Mode.COMPASS) {
            mOpenCvCameraView = (CameraBridgeViewBase) new JavaCameraView(this, -1);
            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            setContentView(mOpenCvCameraView);
            mOpenCvCameraView.setCvCameraViewListener(this);
        } else {
            setContentView(R.layout.activity_main);
        }

        // Debug Button
        m_debugBtn = new Button(this);
        m_debugBtn.setText(getResources().getString(R.string.debug));

        m_debugBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_drawView != null) {
                    if (m_isDebugMode) {
                        m_isDebugMode = false;
                        m_drawView.setBackgroundColor(Color.WHITE);
                        m_debugBtn.setText(getResources().getString(R.string.debug));
                    } else {
                        m_isDebugMode = true;
                        m_drawView.setBackgroundColor(Color.TRANSPARENT);
                        m_debugBtn.setText(getResources().getString(R.string.normal));
                    }
                }
            }
        });

        RelativeLayout relativeLayout_debug = new RelativeLayout(this);

        RelativeLayout.LayoutParams layoutParams_debug = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams_debug.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        relativeLayout_debug.addView(m_debugBtn, layoutParams_debug);

        // Lock Button
        m_lockBtn = new Button(this);
        m_lockBtn.setText(getResources().getString(R.string.lock));

        m_lockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_drawView != null) {
                    if (m_isLocked) {
                        m_isLocked = false;
                        m_lockBtn.setText(getResources().getString(R.string.lock));
                        m_drawView.setLock(m_isLocked);
                    } else {
                        m_isLocked = true;
                        m_lockBtn.setText(getResources().getString(R.string.unlock));
                        m_drawView.setLock(m_isLocked);
                    }
                }
            }
        });

        RelativeLayout relativeLayout_lock = new RelativeLayout(this);

        RelativeLayout.LayoutParams layoutParams_lock = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams_lock.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        relativeLayout_lock.addView(m_lockBtn, layoutParams_lock);

        /**
         * experiment begin
         */
        // start button
        m_startBtn = new Button(this);
        m_startBtn.setText("Start");
        m_startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_drawView != null && m_drawView.getBallCount() == 0) {
                    if (!m_drawView.isFinished()) {
                        m_drawView.startBlock();
                    } else {
                        m_drawView.closeLogger();
                        finish();
                        System.exit(0);
                    }
                }
            }
        });

        RelativeLayout relativeLayout_start = new RelativeLayout(this);

        RelativeLayout.LayoutParams layoutParams_start = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams_start.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relativeLayout_start.addView(m_startBtn, layoutParams_start);

        setStartButtonEnabled(false);

        // continue button
        m_continueBtn = new Button(this);
        m_continueBtn.setText("Continue");
        m_continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_drawView != null && m_drawView.getBallCount() == 0)
                    if (!m_drawView.isFinished()) {
                        m_drawView.nextBlock();
                    } else {
                        showDoneButton();
                    }
            }
        });

        RelativeLayout relativeLayout_con = new RelativeLayout(this);

        RelativeLayout.LayoutParams layoutParams_con = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams_con.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams_con.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        relativeLayout_con.addView(m_continueBtn, layoutParams_con);

        setContinueButtonEnabled(false);
        /**
         * experiment end
         */

        // Draw View
        m_drawView = new MainView(this);
        m_drawView.setBackgroundColor(Color.WHITE);

        m_isLocked = false;
        m_drawView.setLock(false);
        m_drawView.enableFlick();

        // add views
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        // add draw view
        this.addContentView(m_drawView, new LinearLayout.LayoutParams(displayMetrics.widthPixels, (displayMetrics.heightPixels)));
        // add debugButton
        this.addContentView(relativeLayout_debug, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        // add lockButton
        if (m_lockMode == LockMode.STATIC) {
            this.addContentView(relativeLayout_lock, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        /**
         * experiment begin
         */
        // add startButton
        this.addContentView(relativeLayout_start, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        // add continueButton
        this.addContentView(relativeLayout_con, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        /**
         * experiment end
         */

        m_bluetoothData = new BluetoothClientData(this);
        m_bluetoothData.init();

        m_sensorData = new SensorData(this);
        m_sensorData.registerSensors();

        timerHandler.postDelayed(timerRunnable, 0);
    }

    /**
     * experiment begin
     */
    public void setStartButtonEnabled(boolean enabled) {
        m_startBtn.setEnabled(enabled);
    }

    public void setContinueButtonEnabled(boolean enabled) {
        m_continueBtn.setEnabled(enabled);
    }

    public void showDoneButton() {
        setContinueButtonEnabled(false);
        m_startBtn.setText("Done");
        m_startBtn.setEnabled(true);
    }

    public boolean isLogEnabled() {
        return m_isLogEnabled;
    }

    public LockMode getLockMode() {
        return m_lockMode;
    }

    public PassMode getPassMode() {
        return m_passMode;
    }

    /**
     * experiment end
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (m_mode != Mode.COMPASS) {
            if (!OpenCVLoader.initDebug()) {

                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }

        m_bluetoothData.setupThread();
        if (m_bluetoothData.getIsRecevierRegistered()) {
            m_bluetoothData.registerBluetoothReceiver();
        }

        m_sensorData.registerSensors();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        m_bluetoothData.unRegisterBluetoothReceiver();

        m_sensorData.unRegisterSensors();
    }

    @Override
    protected void onRestart() {
        m_bluetoothData.setupThread();
        m_sensorData.registerSensors();
        super.onRestart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        m_bluetoothData.stopThreads();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void exit() {
        new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_HOLO_DARK).setTitle(getResources().getString(R.string.exitTitle)).setMessage(getResources().getString(R.string.exitMsg)).setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                System.exit(0);
            }
        }).setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //
            }
        }).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            //PlutoLogger.Instance().write("MainActivity::onActivityResult(), enable bt : " + resultCode);
            if (resultCode == RESULT_OK) {
                m_bluetoothData.setupThread();
            }
            else {
                showToast(getResources().getString(R.string.bluetoothEnableFailed));
            }
        }
    }

    public boolean getIsDebug() {
        return m_isDebugMode;
    }

    public boolean isLocked() {
        return m_isLocked;
    }

    public String getUserName() {
        return m_userName;
    }

    public String getUserId() {
        return m_userId;
    }

    public int getUserColor() {return m_userColor;}

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        calculateAngle2(inputFrame.gray());
        return inputFrame.rgba();
        //if (m_isDirty) {
            //m_isDirty = false;
            //long timeStart =  System.currentTimeMillis();
            //calculateAngle2(inputFrame.gray());
            //Log.d(MainActivity.TAG, "=========================== time =" + (System.currentTimeMillis() - timeStart));
/*
            Mat m = new Mat(5, 5, CvType.CV_8U);
            double k = 1;
            for (int i=0; i<5; i++) {
                for (int j=0; j<5; j++) {
                    m.put(i,j,k);
                    Log.d(MainActivity.TAG, "=========================== i =" + i + ", j=" + j + ", value=" + m.get(i, j)[0]);
                    k += 1;
                }
            }
            //Log.d(MainActivity.TAG, "=========================== mean ="+Core.mean(m).val[0]);

            Log.d(MainActivity.TAG, "**************************************");

            Mat mask = new Mat(3,3, CvType.CV_8U);
            for (int i=0; i<3; i++) {
                for (int j=0; j<3; j++){
                    mask.put(i,j,1);
                }
            }
            Mat res = new Mat();
            Size ks = new Size(3,3);
            Imgproc.blur(m, res,ks);
            //Imgproc.filter2D(m, res, m.depth(), mask);

            for (int i=0; i<5; i++) {
                for (int j=0; j<5; j++) {
                    Log.d(MainActivity.TAG, "=========================== i =" + i + ", j=" + j + ", value=" + res.get(i, j)[0]);
                }
            }


            Mat weight = new Mat();
            Scalar scalar = new Scalar(0.5f);
            Core.multiply(m, scalar, weight);

            for (int i=0; i<2; i++) {
                for (int j=0; j<2; j++) {
                    Log.d(MainActivity.TAG, "=========================== i =" + i + ", j="+j+", value="+weight.get(i,j)[0]);
                }
            }

            Mat m2 = new Mat(2, 2, CvType.CV_32FC1);
            k=1.0f;
            for (int i=0; i<2; i++) {
                for (int j=0; j<2; j++) {
                    m2.put(i,j,k);
                    k+=1.0f;
                }
            }

            Mat weight2 = new Mat();
            Core.multiply(m, m2, weight2);

            for (int i=0; i<2; i++) {
                for (int j=0; j<2; j++) {
                    Log.d(MainActivity.TAG, "=========================== i =" + i + ", j="+j+", value="+weight2.get(i,j)[0]);
                }
            }*/
        //}
        //return rt;
    }

    // MinMax
    private Mat calculateAngleMinMax(Mat gray) {
        Size ks = new Size(3,3);
        Mat blured = new Mat();
        Imgproc.blur(gray, blured,ks);

        Core.MinMaxLocResult rt = Core.minMaxLoc(blured);

        Point minLoc = rt.minLoc;

        Point maxLoc = rt.maxLoc;

        m_drawView.setLightDir(minLoc, maxLoc, gray.width(), gray.height());

        return gray;
    }

    private void calculateAngle2(Mat img) {
        Mat dst = new Mat();
        Imgproc.resize(img, dst, new Size(), 0.25, 0.25, Imgproc.INTER_LINEAR);

        calculateAngleMinMax(dst);
    }

    private Mat calculateAngle3(Mat img, int index) {
        ArrayList<Mat> channel = new ArrayList<>();
        Mat hsv = new Mat();
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_RGB2HSV);

        Core.split(hsv, channel);
        m_drawView.setMessage("index = " + index);
        return calculateAngleMinMax(channel.get(index));
    }

    // Sobel
    private double calculateAngle(Mat gray) {
        double rt = 0.0;

        Mat sobelX = new Mat();
        Mat sobelY = new Mat();
        Imgproc.Sobel(gray, sobelX, CvType.CV_32F, 1,0); // get x
        Imgproc.Sobel(gray, sobelY, CvType.CV_32F, 0,1); // get y
        //Imgproc.Scharr(gray, sobelX, CvType.CV_32F, 1, 0);// get x
        //Imgproc.Scharr(gray, sobelY, CvType.CV_32F, 0, 1);// get y
;
        Mat magnitude = new Mat();
        Mat angle = new Mat();
        Log.d(MainActivity.TAG, "***************************");

        Core.cartToPolar(sobelX, sobelY, magnitude, angle, true);

        double maxMagnitude = Core.minMaxLoc(magnitude).maxVal;
        Log.d(MainActivity.TAG, "maxMagnitude="+maxMagnitude);

        Scalar scalar = new Scalar(1/maxMagnitude);

        Mat weight = new Mat();
        Core.multiply(magnitude, scalar, weight);

        Mat weightedAngle = new Mat();
        Core.multiply(angle, weight, weightedAngle);

        double mean = Core.mean(weightedAngle).val[0];
        //m_drawView.setAngle(mean);
        Log.d(MainActivity.TAG, "angle=" + mean);

        //Mat sobelX_ABS = new Mat();
        //Mat sobelY_ABS = new Mat();

        //Core.convertScaleAbs(sobelX, sobelX_ABS);
        //Core.convertScaleAbs(sobelY, sobelY_ABS);

        //add(abs_grad_x, abs_grad_y, OutputArray, noArray(), CV_32F);

        //Mat result = new Mat();
        //Core.addWeighted(sobelX_ABS, 0.5, sobelY_ABS, 0.5, 0, result);

        return rt;
    }

    public void showToast(final String message)
    {
        final Activity ac = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ac, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public Mode getCurrentMode() {
        return m_mode;
    }

}
