package usask.chl848.lightrotation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

/**
 * View
 */
public class MainView extends View {
    Paint m_paint;

    private static final int m_textSize = 50;
    private static final int m_textStrokeWidth = 2;
    private static final int m_boundaryStrokeWidth = 10;

    Bitmap m_pic;

    private CompassData m_compassData;
    private LightData m_lightData;
    private boolean m_isBenchmark;
    private float m_compassBenchmark;
    private float m_lightBenchmark;

    private double[] m_bayesianPrior;

    private String m_id;
    private String m_deviceName;
    private String m_userName;
    private int m_color;
    private String m_message;

    private ArrayList<Ball> m_balls;
    private int m_touchedBallId;

    private class Ball {
        public int m_ballColor;
        public float m_ballX;
        public float m_ballY;
        public boolean m_isTouched;
        public String m_id;
        public String m_name;
    }

    private float m_ballRadius;
    private float m_ballBornX;
    private float m_ballBornY;

    private float m_localCoordinateCenterX;
    private float m_localCoordinateCenterY;
    private float m_localCoordinateRadius;

    public class RemotePhoneInfo {
        //String m_deviceName;
        String m_userName;
        int m_color;
        float m_compassAngle;
        float m_lightAngle;
        float m_angleInBenchmark;
        boolean m_isBenchmark;
    }

    private ArrayList<RemotePhoneInfo> m_remotePhones;
    private float m_remotePhoneRadius;

    private boolean m_showRemoteNames;
    final Handler handler = new Handler();
    Runnable mLongPressed = new Runnable() {
        @Override
        public void run() {
            setShowRemoteNames(true);
            m_numberOfLongPress++;
            invalidate();
        }
    };

    private boolean m_isLocked = false;
    private float m_lockedCompassAngle;
    private float m_lockedLightAngle;
    private float m_lockedBaysianAngle;

    public class FlickInfo {
        float startX;
        float startY;
        float velocityX;
        float velocityY;
        long startTime;
    }

    private GestureDetector m_gestureDectector;
    private GestureDetectorData m_gestureDectectorData;
    private boolean m_isFlickEnabled = true;
    private boolean m_isFlicking = false;
    private FlickInfo m_flickInfo = new FlickInfo();
    private static final int TARGET_MAGNETISM_DEGREE = 40;

    /**
     * experiment begin
     */
    private ArrayList<String> m_ballNames;
    private long m_trailStartTime;
    private int m_numberOfDrops;
    private int m_numberOfErrors;
    private int m_numberOfTouch;
    private int m_numberOfTouchBall;
    private int m_numberOfLongPress;
    private int m_numberOfRelease;
    private String m_receiverName;
    private int m_maxBlocks;
    private int m_maxTrails;
    private int m_currentBlock;
    private int m_currentTrail;
    private static final int m_experimentPhoneNumber = 1;
    private MainLogger m_logger;
    private MainLogger m_angleLogger;
    private MainLogger m_bayesianLogger;
    private boolean m_isStarted;
    private boolean m_isExperimentInitialised;
    /**
     * experiment end
     */

    public MainView(Context context) {
        super(context);

        m_paint = new Paint();
        m_compassData = new CompassData();
        m_lightData = new LightData();

        m_touchedBallId = -1;
        m_balls = new ArrayList<>();

        m_remotePhones = new ArrayList<>();

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        initBallBornPoints(displayMetrics);

        m_pic = BitmapFactory.decodeResource(this.getResources(), R.drawable.uparrow);

        m_message = "No Message";

        m_id = ((MainActivity)(context)).getUserId();
        m_userName = ((MainActivity)(context)).getUserName();
        //m_color = ((MainActivity)(context)).getUserColor();
        Random rnd = new Random();
        m_color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        m_localCoordinateCenterX = displayMetrics.widthPixels * 0.5f;
        m_localCoordinateCenterY = displayMetrics.heightPixels * 0.45f;
        m_localCoordinateRadius = displayMetrics.widthPixels * 0.5f;

        m_remotePhoneRadius = displayMetrics.widthPixels * 0.1f;

        m_bayesianPrior = new double[360];
        double uniquePrior = 1.0/360.0;
        for(int i=0; i<360; ++i) {
            m_bayesianPrior[i] = uniquePrior;
        }

        setShowRemoteNames(false);

        m_gestureDectectorData = new GestureDetectorData(this);
        m_gestureDectector = new GestureDetector(context, m_gestureDectectorData);
        m_gestureDectector.setIsLongpressEnabled(false);

        /**
         * experiment begin
         */
        resetCounters();
        m_isStarted = false;
        m_isExperimentInitialised = false;
        /**
         * experiment end
         */
    }

    public void setLock(boolean isLocked) {
        if (isLocked) {
            m_lockedCompassAngle = m_compassData.getRotationVector().m_z;
            m_lockedLightAngle = m_lightData.getAngle();
            m_lockedBaysianAngle = getAngleInBenchmark();
        }
        m_isLocked = isLocked;
    }

    private void initBallBornPoints(DisplayMetrics displayMetrics) {
        m_ballRadius = displayMetrics.widthPixels * 0.08f;
        m_ballBornX = displayMetrics.widthPixels * 0.5f;
        m_ballBornY = displayMetrics.heightPixels * 0.75f - m_ballRadius * 2.0f;
    }

    private void setShowRemoteNames(boolean show) {
        m_showRemoteNames = show;
    }

    private boolean getShowRemoteNames() {
        return m_showRemoteNames;
    }

    public void setDeviceName(String name) {
        m_deviceName = name;
    }

    public String getDeviceName() {
        return m_deviceName;
    }

    public String getUserName() {
        return m_userName;
    }

    public void setMessage(String message) {
        m_message = message;
    }

    public void setLightDir(Point minLoc, Point maxLoc, int imgWidth, int imgHeight){
        m_lightData.setLightDir(minLoc, maxLoc, imgWidth, imgHeight);
    }

    public void enableFlick() {
        m_isFlickEnabled = true;
    }

    public boolean isFlickEnabled() {
        return m_isFlickEnabled;
    }

    public boolean isFlicking() {
        return m_isFlicking;
    }

    /***
     * Calculate angle shows in benchmark's coordinate
     * @param benchmark benchmark device's angle
     * @param angle local device's angle
     * @return angleInBenchmark
     */
    private float calculateAngleToBenchmark(float benchmark, float angle) {
        float angleInBenchmark = (benchmark - angle)/2.0f;
        if (angleInBenchmark < 0){
            angleInBenchmark = 180.0f+angleInBenchmark;
        }

        return angleInBenchmark;
    }

    /***
     * Calculate local device's angle through angle in benchmark, reverse function of calculateAngleToBenchmark
     * @param benchmark benchmark device's angle
     * @param angleInBenchmark angle in benchmark's coordinate
     * @return local device's angle
     */
    private float calculateAngleFromBenchmark(float benchmark, float angleInBenchmark) {
        if (angleInBenchmark > benchmark) {
            angleInBenchmark = angleInBenchmark - 180f;
        }

        return benchmark - angleInBenchmark * 2.0f;
    }

    /***
     * Calculate remote angle in local coordinate
     * @param remoteAngleInBenchmark  remote angle in benchmark's coordinate
     * @param isRemoteBenchmark is remote device benchmark
     * @return remote device's angle in local coordinate
     */
    private float calculateRemoteAngle_Benchmark(float remoteAngleInBenchmark, boolean isRemoteBenchmark) {
        float angle;
        if (m_isLocked) {
            angle = calculateRemoteAngle_Benchmark_core(m_lockedBaysianAngle, remoteAngleInBenchmark, isRemoteBenchmark);
            float currentCompassAngle = m_compassData.getRotationVector().m_z;
            float delta = m_lockedCompassAngle - currentCompassAngle;
            if (delta <= 0.0) {
                angle += Math.abs(delta);
            } else {
                angle -= Math.abs(delta);
            }

        } else {
            angle = calculateRemoteAngle_Benchmark_core(getAngleInBenchmark(), remoteAngleInBenchmark, isRemoteBenchmark);
        }

        return angle;
    }

    /***
     * Calculate remote angle in local coordinate when not locked
     * @param localAngleInBenchmark local angle in benchmark's coordinate
     * @param remoteAngleInBenchmark remote angle in benchmark's coordinate
     * @return remote device's angle in local coordinate (can be used to shows on local screen)
     */
    private float calculateRemoteAngle_Benchmark_core(float localAngleInBenchmark, float remoteAngleInBenchmark, boolean isRemoteBenchmark) {
        if (m_isBenchmark) {
            return remoteAngleInBenchmark;
        }

        if (isRemoteBenchmark) {
            return 180.0f - localAngleInBenchmark;
        } else {
            float remoteAngle = remoteAngleInBenchmark - localAngleInBenchmark;

            if (remoteAngle < 0) {
                remoteAngle = 180.0f + remoteAngle;
            }
            return remoteAngle;
        }
    }

    private static final int  SIZE = 180;
    /***
     * Use Bayesian to calculate angle combine Compass with Light
     * @return angleInBenchmark with the max probability
     */
    private float getAngleInBenchmark() {
        if (m_isBenchmark) {
            return 0.0f;
        } else {
            float compassRead = m_compassData.getRotationVector().m_z;
            float compassReadInBenchmark = calculateAngleToBenchmark(m_compassBenchmark, compassRead);
            float lightRead = m_lightData.getAngle();
            float lightReadInBenchmark = calculateAngleToBenchmark(m_lightBenchmark, lightRead);

            double[] jointProbs = new double[SIZE];
            double marginal = 0.0;

            MainActivity ma = (MainActivity)getContext();
            MainActivity.Mode mode = ma.getCurrentMode();

            switch (mode) {
                case COMPASS:
                    for (int i = 0; i < SIZE; ++i) {
                        jointProbs[i] = Utility.getCompassErrorProbablility((int) Math.abs(compassReadInBenchmark - i)) * m_bayesianPrior[i];
                        marginal += jointProbs[i];
                    }
                    break;
                case LIGHT:
                    for (int i = 0; i < SIZE; ++i) {
                        jointProbs[i] = Utility.getLightErrorProbablility((int) Math.abs(lightReadInBenchmark - i)) * m_bayesianPrior[i];
                        marginal += jointProbs[i];
                    }
                    break;
                case COMPASSLIGHT:
                    for (int i = 0; i < SIZE; ++i) {
                        jointProbs[i] = Utility.getCompassErrorProbablility((int) Math.abs(compassReadInBenchmark - i)) * Utility.getLightErrorProbablility((int) Math.abs(lightReadInBenchmark - i)) * m_bayesianPrior[i];
                        marginal += jointProbs[i];
                    }
                    break;
                case AUTO:
                    if (m_lightData.isAccurate()) {
                        for (int i = 0; i < SIZE; ++i) {
                            jointProbs[i] = Utility.getCompassErrorProbablility((int) Math.abs(compassReadInBenchmark - i)) * Utility.getLightErrorProbablility((int) Math.abs(lightReadInBenchmark - i)) * m_bayesianPrior[i];
                            marginal += jointProbs[i];
                        }
                    } else {
                        for (int i = 0; i < SIZE; ++i) {
                            jointProbs[i] = Utility.getCompassErrorProbablility((int) Math.abs(compassReadInBenchmark - i)) * m_bayesianPrior[i];
                            marginal += jointProbs[i];
                        }
                    }
                    break;
            }

            double[] probs = new double[SIZE];
            int maxAngle = 0;
            double maxPro = 0.0;
            for (int i = 0; i < SIZE; ++i) {
                probs[i] = jointProbs[i]/marginal;
                if (probs[i] > maxPro) {
                    maxPro = probs[i];
                    maxAngle = i;
                }
            }

            // log
            if (m_isStarted && m_bayesianLogger != null) {
                //<participantID> <participantName> <lockMode> <passMode> <block#> <trial#> <mode> <compass> <light> <bayesian> <isCompassAccurate> <isLightAccurate> <timestamp>
                m_bayesianLogger.write(m_id + "," + m_userName + "," + ((MainActivity) getContext()).getLockMode().toString() + "," + ((MainActivity) getContext()).getPassMode().toString() + "," + m_currentBlock + "," + m_currentTrail + "," + mode.toString() + "," + compassReadInBenchmark + "," + lightReadInBenchmark + "," + maxAngle + "," + m_compassData.isAccurate() + "," + m_lightData.isAccurate() + "," + System.currentTimeMillis(), false);
            }

            // update prior with last record
            //System.arraycopy(probs, 0, m_bayesianPrior, 0, jointProbs.length);
            return maxAngle;
        }
    }

    public void setRotationData(float[] values, boolean isAccurate) {
        m_compassData.setRotationData(values);
        m_compassData.setIsAccurate(isAccurate);
        /**
         * experiment begin
         */
        if (m_isStarted) {
            if (m_angleLogger != null) {
                //<participantID> <participantName> <lockMode> <passMode> <block#> <trial#> <azimuth(Z)> <pitch(X)> <roll(Y)> <isAccurate> <timestamp>
                m_angleLogger.write(m_id + "," + m_userName + "," + ((MainActivity) getContext()).getLockMode().toString() + "," + ((MainActivity) getContext()).getPassMode().toString() + "," + m_currentBlock + "," + m_currentTrail + "," + values[0] + "," + values[1] + "," + values[2] + "," + (isAccurate?1:0) + "," + System.currentTimeMillis(), false);
            }
        }

        if (m_isLocked) {
            invalidate();
        }
        /**
         * experiment end
         */
    }

    @Override
    protected void onDraw(Canvas canvas) {
        showMessage(canvas);
        showUserName(canvas);

        MainActivity ma = (MainActivity)getContext();
        if (ma != null && ma.getIsDebug()) {
            showDirection(canvas);
        }

        showCompassAccuracy(canvas);
        showLightAccuracy(canvas);

        showMode(canvas);
        showLocalCircleCoordinate(canvas);
        showLocalRotationAngle(canvas);
        showLocalLightAngle(canvas);
        showBalls(canvas);
        showBoundary(canvas);
        showArrow(canvas);
        /**
         * experiment begin
         */
        showProgress(canvas);
        showLock(canvas);
        /**
         * experiment end
         */
    }

    public void showArrow(Canvas canvas) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        float picWidth = m_pic.getScaledWidth(displayMetrics);
        float picHeight = m_pic.getScaledHeight(displayMetrics);

        float left = displayMetrics.widthPixels * 0.5f - picWidth * 0.25f;
        float top = displayMetrics.heightPixels * 0.02f;
        float right = displayMetrics.widthPixels * 0.5f + picWidth * 0.25f;
        float bottom = displayMetrics.heightPixels * 0.01f + picHeight*0.3f;
        RectF disRect = new RectF(left, top, right, bottom );
        canvas.drawBitmap(m_pic, null, disRect, m_paint);
    }

    private void showMode(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.BLUE);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        m_paint.setStrokeWidth(m_textStrokeWidth);

        MainActivity ma = (MainActivity)getContext();
        MainActivity.Mode mode = ma.getCurrentMode();

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawText(mode.toString(), displayMetrics.widthPixels * 0.7f, displayMetrics.heightPixels * 0.05f, m_paint);
    }

    public void showCompassAccuracy(Canvas canvas) {
        if (!m_compassData.isAccurate()) {
            m_paint.setTextSize(m_textSize);
            m_paint.setColor(Color.RED);
            m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

            String output = getResources().getString(R.string.inaccurate);
            canvas.drawText(output, (int) (displayMetrics.widthPixels * 0.05), (int) (displayMetrics.heightPixels * 0.1), m_paint);
        }
    }

    public void showLightAccuracy(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.RED);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        String output = String.format("%.3f", m_lightData.getSigma());
        canvas.drawText(output, (int) (displayMetrics.widthPixels * 0.8), (int) (displayMetrics.heightPixels * 0.1), m_paint);
    }

    private void showLocalLightAngle(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.RED);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        m_paint.setStrokeWidth(m_textStrokeWidth);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        String value = "L :";
        value += String.format("%.3f", m_lightData.getAngle());
        if (!m_isBenchmark) {
            value += "(";
            value += String.format("%.3f", calculateAngleToBenchmark(m_lightBenchmark, m_lightData.getAngle()));
            value += ")";
        }
        canvas.drawText(value, displayMetrics.widthPixels * 0.32f, displayMetrics.heightPixels * 0.95f, m_paint);
    }

    private void showLocalRotationAngle(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.RED);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        m_paint.setStrokeWidth(m_textStrokeWidth);

        CompassData.RotationVector rotationVector = m_compassData.getRotationVector();
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        String value = "C :";
        value += String.format("%.3f", rotationVector.m_z);
        if (!m_isBenchmark) {
            value += "(";
            value += String.format("%.3f", calculateAngleToBenchmark(m_compassBenchmark, rotationVector.m_z));
            value += ")";
        }
        canvas.drawText(value, displayMetrics.widthPixels * 0.32f, displayMetrics.heightPixels * 0.9f, m_paint);
    }

    private void showMessage(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.GREEN);
        m_paint.setStrokeWidth(m_textStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawText(m_message, displayMetrics.widthPixels * 0.32f, displayMetrics.heightPixels * 0.85f, m_paint);
    }

    private void showUserName(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.BLUE);
        m_paint.setStrokeWidth(m_textStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawText("(" + m_id + ")" + m_userName, displayMetrics.widthPixels * 0.32f, displayMetrics.heightPixels * 0.8f, m_paint);
    }

    private void showDirection(Canvas canvas) {
        m_paint.setColor(Color.RED);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        m_paint.setStrokeWidth(m_textStrokeWidth);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        Point maxPt = m_lightData.getMaxPtInScreen(displayMetrics);
        canvas.drawCircle((float) maxPt.x, (float) maxPt.y, 30, m_paint);

        Point minPt = m_lightData.getMinPtInScreen(displayMetrics);
        canvas.drawLine((float) maxPt.x, (float) maxPt.y, (float) minPt.x, (float) minPt.y, m_paint);
    }

    private void showLocalCircleCoordinate(Canvas canvas){
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        m_paint.setColor(Color.BLUE);
        m_paint.setStyle(Paint.Style.STROKE);

        // draw coordinate
        float left = 0.0f;
        float top = displayMetrics.heightPixels * 0.45f - m_localCoordinateRadius;
        float right = displayMetrics.widthPixels;
        float bottom = displayMetrics.heightPixels * 0.45f + m_localCoordinateRadius;
        RectF disRect = new RectF(left, top, right, bottom);

        m_paint.setStrokeWidth(m_boundaryStrokeWidth);
        canvas.drawArc(disRect, 0.0f, 360.0f, false, m_paint);

        MainActivity mainActivity = (MainActivity)getContext();
        if ((mainActivity != null) && mainActivity.m_bluetoothData.isConnected()) {
            showRemotePhones(canvas);
        }
    }

    private void showRemotePhones(Canvas canvas) {
        if (!m_remotePhones.isEmpty()) {
            int size = m_remotePhones.size();
            for (int i=0; i<size; ++i) {
                RemotePhoneInfo info = m_remotePhones.get(i);
                double angle_remote = calculateRemoteAngle_Benchmark(info.m_angleInBenchmark, info.m_isBenchmark);
                float pointX = m_localCoordinateCenterX + m_localCoordinateRadius * (float)Math.cos(Math.toRadians(angle_remote));
                float pointY = m_localCoordinateCenterY - m_localCoordinateRadius * (float)Math.sin(Math.toRadians(angle_remote));
                m_paint.setColor(info.m_color);
                m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawCircle(pointX, pointY, m_remotePhoneRadius, m_paint);

                /* show remote angle
                m_paint.setTextSize(m_textSize);
                m_paint.setStrokeWidth(m_textStrokeWidth);
                float textX1 = pointX - m_remotePhoneRadius;
                float textY1 = pointY - m_remotePhoneRadius * 1.5f;
                canvas.drawText(String.format("%.3f", info.m_angleInBenchmark), textX1, textY1, m_paint);
                */
                if (getShowRemoteNames()) {
                    m_paint.setTextSize(m_textSize);
                    m_paint.setStrokeWidth(m_textStrokeWidth);
                    float textX = pointX - m_remotePhoneRadius;
                    float textY = pointY - m_remotePhoneRadius * 1.5f;
                    if (info.m_userName.length() > 5) {
                        textX = pointX - m_remotePhoneRadius * 2.0f;
                    }
                    canvas.drawText(info.m_userName, textX, textY, m_paint);
                }
            }
        }
    }

    private void showBalls(Canvas canvas) {
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        for (Ball ball : m_balls) {
            m_paint.setColor(ball.m_ballColor);
            canvas.drawCircle(ball.m_ballX, ball.m_ballY, m_ballRadius, m_paint);

            /**
             * experiment begin
             */

            m_paint.setStrokeWidth(m_textStrokeWidth);
            m_paint.setTextSize(m_textSize);
            float textX = ball.m_ballX - m_ballRadius;
            float textY = ball.m_ballY - m_ballRadius;
            if (ball.m_name.length() > 5) {
                textX = ball.m_ballX - m_ballRadius * 2.0f;
            }
            canvas.drawText(ball.m_name, textX, textY, m_paint);
            /**
             * experiment end
             */
        }
    }

    private void showBoundary(Canvas canvas) {
        m_paint.setColor(Color.RED);
        m_paint.setStrokeWidth(m_boundaryStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawLine(0, displayMetrics.heightPixels * 0.75f, displayMetrics.widthPixels, displayMetrics.heightPixels * 0.75f, m_paint);
    }

    public void showProgress(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.BLUE);
        m_paint.setStrokeWidth(m_textStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        String block = "Block: " + m_currentBlock +"/" + m_maxBlocks;
        canvas.drawText(block, (int) (displayMetrics.widthPixels * 0.05), (int) (displayMetrics.heightPixels * 0.15), m_paint);

        String trial = "Trial: " + m_currentTrail +"/" + m_maxTrails;
        canvas.drawText(trial, (int) (displayMetrics.widthPixels * 0.05), (int) (displayMetrics.heightPixels * 0.2), m_paint);
    }

    public void showLock(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.BLUE);
        m_paint.setStrokeWidth(m_textStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        String lock = "Lock: " + m_isLocked;
        canvas.drawText(lock, (int) (displayMetrics.widthPixels * 0.8), (int) (displayMetrics.heightPixels * 0.15), m_paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean rt = m_gestureDectector.onTouchEvent(event);
        if (!rt && event.getAction() == MotionEvent.ACTION_UP) {
            m_gestureDectectorData.onUp(event);
        }
        return rt;
    }

    public boolean onTouchDown(MotionEvent event) {
        float X = event.getX();
        float Y = event.getY();
        float touchRadius = event.getTouchMajor();
        m_numberOfTouch++; // experiment
        m_touchedBallId = -1;
        for (int i = 0; i < m_balls.size(); ++i) {
            Ball ball = m_balls.get(i);
            ball.m_isTouched = false;

            double dist;
            dist = Math.sqrt(Math.pow((X - ball.m_ballX), 2) + Math.pow((Y - ball.m_ballY), 2));
            if (dist <= (touchRadius + m_ballRadius)) {
                ball.m_isTouched = true;
                m_touchedBallId = i;

                boolean isOverlap = false;
                for (int j = 0; j < m_balls.size(); ++j) {
                    if (j != m_touchedBallId) {
                        Ball ball2 = m_balls.get(j);

                        double dist2 = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                        if (dist2 <= m_ballRadius * 2) {
                            isOverlap = true;
                        }
                    }
                }

                if (!isOverlap && !isBoundary(X, Y)) {
                    ball.m_ballX = X;
                    ball.m_ballY = Y;
                    this.invalidate();
                }
            }

            if (m_touchedBallId > -1) {
                break;
            }
        }

        if (m_touchedBallId == -1) {

            boolean show = false;

            for (RemotePhoneInfo remotePhone : m_remotePhones) {
                double angle_remote = calculateRemoteAngle_Benchmark(remotePhone.m_angleInBenchmark, remotePhone.m_isBenchmark);
                float pointX = m_localCoordinateCenterX + m_localCoordinateRadius * (float) Math.cos(Math.toRadians(angle_remote));
                float pointY = m_localCoordinateCenterY - m_localCoordinateRadius * (float) Math.sin(Math.toRadians(angle_remote));

                double dist = Math.sqrt(Math.pow((X - pointX), 2) + Math.pow((Y - pointY), 2));

                if (dist <= (touchRadius + m_remotePhoneRadius)) {
                    show = true;
                    break;
                }
            }

            if (show) {
                handler.postDelayed(mLongPressed, 500);
            }
        } else {
            // experiment
            m_numberOfTouchBall++;
            if (((MainActivity) getContext()).getLockMode() == MainActivity.LockMode.DYNAMIC) {
                setLock(true);
            }
        }
        return true;
    }

    public boolean onTouchMove(MotionEvent event) {
        float X = event.getX();
        float Y = event.getY();
        float touchRadius = event.getTouchMajor();
        if (getShowRemoteNames()) {
            boolean show = false;

            for (RemotePhoneInfo remotePhone : m_remotePhones) {
                double angle_remote = calculateRemoteAngle_Benchmark(remotePhone.m_angleInBenchmark,remotePhone.m_isBenchmark);
                float pointX = m_localCoordinateCenterX + m_localCoordinateRadius * (float) Math.cos(Math.toRadians(angle_remote));
                float pointY = m_localCoordinateCenterY - m_localCoordinateRadius * (float) Math.sin(Math.toRadians(angle_remote));

                double dist = Math.sqrt(Math.pow((X - pointX), 2) + Math.pow((Y - pointY), 2));

                if (dist <= (touchRadius + m_remotePhoneRadius)) {
                    show = true;
                    break;
                }
            }

            if (!show) {
                handler.removeCallbacks(mLongPressed);
                setShowRemoteNames(false);
                invalidate();
            }
        }

        if (m_touchedBallId > -1) {
            Ball ball = m_balls.get(m_touchedBallId);
            if (ball.m_isTouched) {
                boolean isOverlap = false;

                for (int j = 0; j < m_balls.size(); ++j) {
                    if (j != m_touchedBallId) {
                        Ball ball2 = m_balls.get(j);

                        double dist = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                        if (dist <= m_ballRadius * 2) {
                            isOverlap = true;
                        }
                    }
                }

                if (!isOverlap && !isBoundary(X, Y)) {
                    ball.m_ballX = X;
                    ball.m_ballY = Y;
                    this.invalidate();
                }
            }
        }
        return true;
    }

    public boolean onTouchUp(MotionEvent event) {
        float X = event.getX();
        float Y = event.getY();

        handler.removeCallbacks(mLongPressed);
        if (getShowRemoteNames()) {
            setShowRemoteNames(false);
            invalidate();
        }

        m_numberOfRelease++; // experiment

        if (m_touchedBallId > -1) {
            m_numberOfDrops++; // experiment
            Ball ball = m_balls.get(m_touchedBallId);
            if (ball.m_isTouched) {
                boolean isOverlap = false;

                for (int j = 0; j < m_balls.size(); ++j) {
                    if (j != m_touchedBallId) {
                        Ball ball2 = m_balls.get(j);

                        double dist = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                        if (dist <= m_ballRadius * 2) {
                            isOverlap = true;
                        }
                    }
                }

                if (!isOverlap) {
                    String name = isSending(ball.m_ballX, ball.m_ballY);
                    // experiment
                    if (!name.isEmpty() && !ball.m_name.isEmpty()) {
                        if (ball.m_name.equalsIgnoreCase(name)) {
                            ((MainActivity) getContext()).showToast("send ball to : " + name);
                            //sendBall(ball, name);
                            removeBall(ball.m_id);
                            this.invalidate();
                            endTrail();
                        } else {
                            m_numberOfErrors++;
                        }
                    }
                }
            }

            // experiment
            if (((MainActivity) getContext()).getLockMode() == MainActivity.LockMode.DYNAMIC) {
                setLock(false);
            }
        }

        for (Ball ball : m_balls) {
            ball.m_isTouched = false;
        }
        return true;
    }

    public boolean onFlick(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        m_numberOfRelease++; // experiment
        if (!m_isFlicking && m_touchedBallId > -1) {
            m_numberOfDrops++; // experiment
            m_isFlicking = true;
            m_flickInfo.startX = e2.getX();
            m_flickInfo.startY = e2.getY();
            m_flickInfo.velocityX = velocityX/1000;
            m_flickInfo.velocityY = velocityY/1000;
            m_flickInfo.startTime = System.currentTimeMillis();

            double radiusSqr = Math.pow(m_localCoordinateRadius, 2);
            float newX = e2.getX();
            float newY = e2.getY();
            double vy = Math.abs(velocityY / velocityX);
            double distSqr = Math.pow(Math.abs(m_localCoordinateCenterX - newX), 2) + Math.pow(Math.abs(m_localCoordinateCenterY - newY), 2);
            //Log.d(MainActivity.TAG, "getFlickAngle startXY = (" + e2.getX() + "," + e2.getY() + "), vy = " + vy);
            while (distSqr < radiusSqr) {
                if (velocityX >= 0) {
                    newX += 1;
                } else {
                    newX -= 1;
                }
                if (velocityY >= 0) {
                    newY += vy;
                } else {
                    newY -= vy;
                }
                distSqr = Math.pow(Math.abs(m_localCoordinateCenterX - newX), 2) + Math.pow(Math.abs(m_localCoordinateCenterY - newY), 2);
            }
            //Log.d(MainActivity.TAG, "getFlickAngle endXY = (" + newX + "," + newY + ")");
            double flickAngle = getFlickAngle(m_localCoordinateCenterX, m_localCoordinateCenterY, newX, newY);
            //Log.d(MainActivity.TAG, "onFlick flickAngle = " + flickAngle );

            // find nearest remote phone
            boolean isFound = false;
            double maxAngle = 360;
            double target_angle_remote = 0.0;
            for (RemotePhoneInfo phone : m_remotePhones) {
                double angle_remote = calculateRemoteAngle_Benchmark(phone.m_angleInBenchmark, phone.m_isBenchmark);
                double deltaAngle = Math.abs(flickAngle - angle_remote);
                if (deltaAngle > 180) {
                    deltaAngle = 360 - deltaAngle;
                }
                if (deltaAngle < TARGET_MAGNETISM_DEGREE && deltaAngle < maxAngle) {
                    isFound = true;
                    maxAngle = deltaAngle;
                    target_angle_remote = angle_remote;
                }
            }

            if (isFound) {
                float pointX = m_localCoordinateCenterX + m_localCoordinateRadius * (float)Math.cos(Math.toRadians(target_angle_remote));
                float pointY = m_localCoordinateCenterY - m_localCoordinateRadius * (float)Math.sin(Math.toRadians(target_angle_remote));

                // calculate new velocity
                double velocity = Math.sqrt(Math.pow(m_flickInfo.velocityX, 2) + Math.pow(m_flickInfo.velocityY, 2));
                double newFlickAngle = getFlickAngle(e2.getX(), e2.getY(), pointX, pointY);
                m_flickInfo.velocityX = (float)(velocity * Math.cos(Math.toRadians(newFlickAngle)));
                m_flickInfo.velocityY = (float)((-1) * velocity * Math.sin(Math.toRadians(newFlickAngle)));
            }

            BallFlickThread thread = new BallFlickThread(getContext(), m_flickInfo);
            thread.start();
        }
        return true;
    }

    private double getFlickAngle(float startX, float startY, float endX, float endY) {
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        double angle = -1;

        int quadrant = -1;
        if (deltaX > 0 && deltaY < 0) {
            quadrant = 1;
        } else if (deltaX < 0 && deltaY < 0) {
            quadrant = 2;
        } else if (deltaX < 0 && deltaY > 0) {
            quadrant = 3;
        } else if (deltaX > 0 && deltaY > 0) {
            quadrant = 4;
        }

        if (quadrant != -1) {
            angle = Math.toDegrees(Math.atan(Math.abs(deltaY/deltaX)));
            switch (quadrant) {
                case 1:
                    break;
                case 2:
                    angle = 180 - angle;
                    break;
                case 3:
                    angle = 180 + angle;
                    break;
                case 4:
                    angle = 360 - angle;
                    break;
            }
        }

        return angle;
    }

    public void moveBall(float newX, float newY) {
        if (m_touchedBallId > -1) {
            Ball ball = m_balls.get(m_touchedBallId);
            if (ball.m_isTouched) {
                boolean isOverlap = false;

                for (int j = 0; j < m_balls.size(); ++j) {
                    if (j != m_touchedBallId) {
                        Ball ball2 = m_balls.get(j);

                        double dist = Math.sqrt(Math.pow((newX - ball2.m_ballX), 2) + Math.pow((newY - ball2.m_ballY), 2));
                        if (dist <= m_ballRadius * 2) {
                            isOverlap = true;
                        }
                    }
                }

                String name = isSending(ball.m_ballX, ball.m_ballY);
                // experiment
                if (!name.isEmpty() && !ball.m_name.isEmpty()) {
                    if (ball.m_name.equalsIgnoreCase(name)) {
                        ((MainActivity) getContext()).showToast("send ball to : " + name);
                        //sendBall(ball, name);
                        removeBall(ball.m_id);
                        endTrail();
                    } else {
                        m_numberOfErrors++;
                    }

                    m_isFlicking = false;
                }else if (!isOverlap && !isBoundary(newX, newY)) {
                    ball.m_ballX = newX;
                    ball.m_ballY = newY;
                } else {
                    m_isFlicking = false;
                }
            }
        }
    }

    public int getTouchedBallId() {
        return m_touchedBallId;
    }

    private boolean isBoundary(float x, float y) {
        boolean rt = false;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        while (true) {
            // check bottom
            if ((y + m_ballRadius) >= (displayMetrics.heightPixels * 0.9f)) {
                rt = true;
                break;
            }

            // check left
            if (x - m_ballRadius <= 0.0f) {
                rt = true;
                break;
            }

            // check right
            if (x + m_ballRadius >= displayMetrics.widthPixels) {
                rt = true;
                break;
            }

            //check top
            double dist = Math.sqrt(Math.pow((x - m_localCoordinateCenterX), 2) + Math.pow((y - m_localCoordinateCenterY), 2));
            if (dist + m_ballRadius >= m_localCoordinateRadius) {
                rt = true;
            }
            break;
        }

        return rt;
    }

    private String isSending(float x, float y) {
        String receiverName = "";
        float rate = 10000.0f;
        if (!m_remotePhones.isEmpty()) {
            for (RemotePhoneInfo remotePhoneInfo : m_remotePhones) {
                double angle_remote = calculateRemoteAngle_Benchmark(remotePhoneInfo.m_angleInBenchmark, remotePhoneInfo.m_isBenchmark);
                float pointX = m_localCoordinateCenterX + m_localCoordinateRadius * (float)Math.cos(Math.toRadians(angle_remote));
                float pointY = m_localCoordinateCenterY - m_localCoordinateRadius * (float)Math.sin(Math.toRadians(angle_remote));

                double dist = Math.sqrt(Math.pow((x - pointX), 2) + Math.pow((y - pointY), 2));
                if (dist < (m_remotePhoneRadius + m_ballRadius)){
                    if (dist < rate) {
                        receiverName = remotePhoneInfo.m_userName;
                        rate = (float)dist;
                    }
                }
            }
        }

        return receiverName;
    }

    public void addBall() {
        Ball ball = new Ball();
        Random rnd = new Random();
        ball.m_ballColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        ball.m_ballX = m_ballBornX;
        ball.m_ballY = m_ballBornY;
        ball.m_isTouched = false;
        ball.m_id = UUID.randomUUID().toString();
        ball.m_name = getBallName();
        m_receiverName = ball.m_name;
        m_balls.add(ball);
    }

    public  void removeBall(String id) {
        for (Ball ball : m_balls) {
            if (ball.m_id.equalsIgnoreCase(id)) {
                m_balls.remove(ball);
                m_touchedBallId = -1;
                break;
            }
        }
    }

    public void receivedBall(String id, int color) {
        boolean isReceived = false;
        for (Ball ball : m_balls) {
            if (ball.m_id.equalsIgnoreCase(id)) {
                isReceived = true;
                break;
            }
        }

        if (!isReceived) {
            Ball ball = new Ball();
            ball.m_id = id;
            ball.m_ballColor = color;
            ball.m_isTouched = false;

            ball.m_ballX = m_ballBornX;
            ball.m_ballY = m_ballBornY;

            m_balls.add(ball);
        }
    }

    public void sendBall(Ball ball, String receiverName) {
        JSONObject jsonObject = new JSONObject();
        try {
            if (m_isLocked) {
                jsonObject.put("compassangle", m_lockedCompassAngle);
                jsonObject.put("lightangle",m_lockedLightAngle);
                jsonObject.put("angleinbenchmark", m_lockedBaysianAngle);
            } else {
                jsonObject.put("compassangle", m_compassData.getRotationVector().m_z);
                jsonObject.put("lightangle", m_lightData.getAngle());
                jsonObject.put("angleinbenchmark", getAngleInBenchmark());
            }
            jsonObject.put("color", m_color);
            jsonObject.put("name", m_userName);
            jsonObject.put("isSendingBall", true);
            jsonObject.put("ballId", ball.m_id);
            jsonObject.put("ballColor", ball.m_ballColor);
            jsonObject.put("receiverName", receiverName);
        } catch (JSONException e){
            e.printStackTrace();
        }

        MainActivity ca = (MainActivity)getContext();
        if (ca != null) {
            ca.m_bluetoothData.addMessage(jsonObject.toString());
        }
    }

    public void cookLocationMsg(){
        JSONObject msg = new JSONObject();
        try {
            if (m_isLocked) {
                msg.put("compassangle", m_lockedCompassAngle);
                msg.put("lightangle",m_lockedLightAngle);
                msg.put("angleinbenchmark", m_lockedBaysianAngle);
            } else {
                msg.put("compassangle", m_compassData.getRotationVector().m_z);
                msg.put("lightangle", m_lightData.getAngle());
                msg.put("angleinbenchmark", getAngleInBenchmark());
            }
            msg.put("name", m_userName);
            msg.put("color", m_color);
            msg.put("isSendingBall", false);
        } catch (JSONException e){
            e.printStackTrace();
        }

        MainActivity ca = (MainActivity)getContext();
        if (ca != null) {
            ca.m_bluetoothData.addMessage(msg.toString());
        }
    }

    public void updateRemotePhone(String name, int color, float angleInBenchmark, float compassAngle,float lightAngle, boolean isBenchmark){
        if (name.isEmpty()) {
            return;
        }

        if (name.equalsIgnoreCase(m_userName)) {
            m_isBenchmark = isBenchmark;
            return;
        }

        int size = m_remotePhones.size();
        boolean isFound = false;
        for (int i = 0; i<size; ++i) {
            RemotePhoneInfo info = m_remotePhones.get(i);
            if (info.m_userName.equalsIgnoreCase(name)) {
                info.m_color = color;
                info.m_angleInBenchmark = angleInBenchmark;
                info.m_compassAngle = compassAngle;
                info.m_lightAngle = lightAngle;
                info.m_isBenchmark = isBenchmark;
                if (isBenchmark) {
                    m_compassBenchmark = compassAngle;
                    m_lightBenchmark = lightAngle;
                }
                isFound = true;
                break;
            }
        }

        if (!isFound) {
            RemotePhoneInfo info = new RemotePhoneInfo();
            info.m_userName = name;
            info.m_color = color;
            info.m_angleInBenchmark = angleInBenchmark;
            m_remotePhones.add(info);

            /**
             * experiment end
             */
            if (m_remotePhones.size() == m_experimentPhoneNumber && !m_isExperimentInitialised) {
                initExperiment();
            }
            /**
             * experiment end
             */
        }
    }

    public ArrayList<RemotePhoneInfo> getRemotePhones() {
        return m_remotePhones;
    }

    public void removePhones(ArrayList<RemotePhoneInfo> phoneInfos) {
        m_remotePhones.removeAll(phoneInfos);
    }

    public void clearRemotePhoneInfo() {
        m_remotePhones.clear();
    }

    public int getBallCount() {
        return m_balls.size();
    }

    /**
     * experiment begin
     */
    private void initExperiment() {
        m_isExperimentInitialised = true;

        // init ball names
        m_ballNames = new ArrayList<>();

        m_maxBlocks = 5;
        m_maxTrails = 9;

        m_currentBlock = 0;
        m_currentTrail = 0;

        m_isStarted = false;

        resetBlock();

        m_logger = null;
        m_logger = new MainLogger(getContext(), m_id+"_"+m_userName+"_"+getResources().getString(R.string.app_name));
        //<participantID> <participantName> <lockMode> <passMode> <block#> <trial#> <receiver name> <elapsed time for this trial> <number of errors for this trial> <number of release for this trial> <number of drops for this trial> <number of touch for this trial> <number of touch ball for this trial> <number of long press for this trial> <timestamp>
        m_logger.writeHeaders("participantID" + "," + "participantName" + "," + "lockMode" + "," + "passMode" + "," + "block" + "," + "trial" + "," + "receiverName" + "," + "elapsedTime" + "," + "errors" + "," + "release" + "," + "drops" + "," + "touch" + "," + "touchBall" + "," + "longPress" + "," + "timestamp");

        m_angleLogger = null;
        m_angleLogger = new MainLogger(getContext(), m_id+"_"+m_userName+"_"+getResources().getString(R.string.app_name)+"_orientation");
        //<participantID> <participantName> <condition> <block#> <trial#> <azimuth(Z)> <pitch(X)> <roll(Y)> <isAccurate> <timestamp>
        m_angleLogger.writeHeaders("participantID" + "," + "participantName" + "," + "lockMode" + "," + "passMode" + "," + "block" + "," + "trial" + "," + "azimuth(Z)" + "," + "pitch(X)" + "," + "roll(Y)" + "," + "isAccurate" + "," + "timestamp");

        m_bayesianLogger = null;
        m_bayesianLogger = new MainLogger(getContext(), m_id+"_"+m_userName+"_"+getResources().getString(R.string.app_name)+"_bayesian");
        //<participantID> <participantName> <lockMode> <passMode> <block#> <trial#> <mode> <compass> <light> <bayesian> <isCompassAccurate> <isLightAccurate> <timestamp>
        m_bayesianLogger.writeHeaders("participantID" + "," + "participantName" + "," + "lockMode" + "," + "passMode" + "," + "block" + "," + "trial" + "," + "mode" + "," + "compass" + "," + "light" + "," + "bayesian" + "," + "isCompassAccurate" + "," + "isLightAccurate" + "," + "timestamp");

        ((MainActivity) getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((MainActivity) getContext()).setStartButtonEnabled(true);
                ((MainActivity) getContext()).setContinueButtonEnabled(false);
            }
        });
    }

    private String getBallName() {
        if (m_ballNames.isEmpty()) {
            return "";
        }

        Random rnd = new Random();
        int index = rnd.nextInt(m_ballNames.size());
        String name = m_ballNames.get(index);
        m_ballNames.remove(index);
        return name;
    }

    public boolean isFinished() {
        return m_currentBlock == m_maxBlocks;
    }

    public void nextBlock() {
        ((MainActivity)getContext()).setStartButtonEnabled(true);
        ((MainActivity)getContext()).setContinueButtonEnabled(false);
    }

    public void resetBlock() {
        // reset ball names
        m_ballNames.clear();
        for (RemotePhoneInfo remotePhoneInfo : m_remotePhones){
            for(int i=0; i<3; i++){
                m_ballNames.add(remotePhoneInfo.m_userName);
            }
        }

        // reset self phone color
        Random rnd = new Random();
        m_color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        resetCounters();
    }

    public void startBlock() {
        m_currentBlock += 1;
        m_currentTrail = 0;
        m_isStarted = true;

        resetBlock();
        startTrial();
        ((MainActivity)getContext()).setStartButtonEnabled(false);
        ((MainActivity)getContext()).setContinueButtonEnabled(false);
    }

    public void endBlock() {
        m_isStarted = false;

        if (isFinished()) {
            closeLogger();
        }

        ((MainActivity) getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(getContext()).setTitle("Warning").setMessage("You have completed block " + m_currentBlock + ", please wait for other participants.").setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();

                ((MainActivity) getContext()).setContinueButtonEnabled(true);
                ((MainActivity) getContext()).setStartButtonEnabled(false);
            }
        });

        m_currentTrail = 0;
    }

    public void startTrial() {
        m_trailStartTime = System.currentTimeMillis();
        m_currentTrail += 1;
        resetCounters();
        addBall();
    }

    public void endTrail() {
        long trailEndTime = System.currentTimeMillis();
        long timeElapse = trailEndTime - m_trailStartTime;

        if (m_currentBlock == 0) {
            ++m_currentBlock;
        }

        if (m_currentTrail == 0) {
            ++m_currentTrail;
        }

        //<participantID> <participantName> <lockMode> <passMode> <block#> <trial#> <receiver name> <elapsed time for this trial> <number of errors for this trial> <number of release for this trial> <number of drops for this trial> <number of touch for this trial> <number of touch ball for this trial> <number of long press for this trial> <timestamp>
        if (m_logger != null) {
            m_logger.write(m_id + "," + m_userName + "," + ((MainActivity) getContext()).getLockMode().toString() + "," + ((MainActivity) getContext()).getPassMode().toString() + "," + m_currentBlock + "," + m_currentTrail + "," + m_receiverName + "," + timeElapse + "," + m_numberOfErrors + "," + m_numberOfRelease + "," + m_numberOfDrops + "," + m_numberOfTouch + "," + m_numberOfTouchBall + "," + m_numberOfLongPress + "," + trailEndTime, true);
        }

        if (m_angleLogger != null) {
            m_angleLogger.flush();
        }

        if (m_bayesianLogger != null) {
            m_bayesianLogger.flush();
        }

        if (m_currentTrail < m_maxTrails) {
            startTrial();
        } else {
            endBlock();
        }
    }

    public void closeLogger() {
        if (m_logger != null) {
            m_logger.close();
        }

        if (m_angleLogger != null) {
            m_angleLogger.close();
        }

        if (m_bayesianLogger != null) {
            m_bayesianLogger.close();
        }
    }

    private void resetCounters() {
        m_numberOfDrops = 0;
        m_numberOfErrors = 0;
        m_numberOfTouch = 0;
        m_numberOfTouchBall = 0;
        m_numberOfLongPress = 0;
        m_numberOfRelease = 0;
        m_receiverName = "";
    }
    /**
     * experiment end
     */
}
