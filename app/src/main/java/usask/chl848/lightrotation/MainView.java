package usask.chl848.lightrotation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
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
    }

    private float m_ballRadius;
    private float m_ballBornX;
    private float m_ballBornY;

    private float m_localCoordinateCenterX;
    private float m_localCoordinateCenterY;
    private float m_localCoordinateRadius;

    public class RemotePhoneInfo {
        String m_deviceName;
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
            invalidate();
        }
    };

    //private MainLogger m_logger = null;
    private boolean m_logEnabled;
    private int m_logCount;

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

        m_userName = ((MainActivity)(context)).getUserName();
        m_color = ((MainActivity)(context)).getUserColor();

        m_localCoordinateCenterX = displayMetrics.widthPixels * 0.5f;
        m_localCoordinateCenterY = displayMetrics.heightPixels * 0.5f;
        m_localCoordinateRadius = displayMetrics.widthPixels * 0.5f;

        m_remotePhoneRadius = displayMetrics.widthPixels * 0.05f;

        m_bayesianPrior = new double[360];
        double uniquePrior = 1.0/360.0;
        for(int i=0; i<360; ++i) {
            m_bayesianPrior[i] = uniquePrior;
        }

        setShowRemoteNames(false);
/*
        m_logger = new MainLogger(getContext(), m_username+"_"+getResources().getString(R.string.app_name)+"_angle");
        m_logger.writeHeaders("userName" + ","  + "angle" + "," + "timestamp");
        m_logEnabled = false;
        m_logCount = 0;
        */
    }

    public void enableLog()
    {
        m_logEnabled = true;
    }

    public void disableLog()
    {
        if (isLogEnabled())
        {
            //m_logger.flush();
        }
        m_logEnabled = false;
    }

    public boolean isLogEnabled()
    {
        return m_logEnabled;
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
        float angle = m_lightData.setLightDir(minLoc, maxLoc, imgWidth, imgHeight);
        if (isLogEnabled())
        {
            //m_logger.write(m_username + "," + angle + "," + System.currentTimeMillis(), true);
            //++m_logCount;
        }
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
     * @param localAngleInBenchmark local angle in benchmark's coordinate
     * @param remoteAngleInBenchmark remote angle in benchmark's coordinate
     * @param remoteAngleInBenchmark is remote device benchmark
     * @param isLocalBenchmark is local device benchmark
     * @return remote device's angle in local coordinate (can be used to shows on local screen)
     */
    private float calculateRemoteAngle_Benchmark(float localAngleInBenchmark, float remoteAngleInBenchmark, boolean isLocalBenchmark, boolean isRemoteBenchmark) {
        if (isLocalBenchmark) {
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

            double[] jointProbs = new double[360];
            double marginal = 0.0;

            MainActivity ma = (MainActivity)getContext();
            MainActivity.Mode mode = ma.getCurrentMode();

            switch (mode) {
                case COMPASS:
                    for (int i = 0; i < 360; ++i) {
                        jointProbs[i] = Utility.getCompassErrorProbablility((int) Math.abs(compassReadInBenchmark - i)) * m_bayesianPrior[i];
                        marginal += jointProbs[i];
                    }
                    break;
                case LIGHT:
                    for (int i = 0; i < 360; ++i) {
                        jointProbs[i] = Utility.getLightErrorProbablility((int) Math.abs(lightReadInBenchmark - i)) * m_bayesianPrior[i];
                        marginal += jointProbs[i];
                    }
                    break;
                case COMPASSLIGHT:
                    for (int i = 0; i < 360; ++i) {
                        jointProbs[i] = Utility.getCompassErrorProbablility((int) Math.abs(compassReadInBenchmark - i)) * Utility.getLightErrorProbablility((int) Math.abs(lightReadInBenchmark - i)) * m_bayesianPrior[i];
                        marginal += jointProbs[i];
                    }
                    break;
                case AUTO:
                    if (m_lightData.isAccurate()) {
                        for (int i = 0; i < 360; ++i) {
                            jointProbs[i] = Utility.getCompassErrorProbablility((int) Math.abs(compassReadInBenchmark - i)) * Utility.getLightErrorProbablility((int) Math.abs(lightReadInBenchmark - i)) * m_bayesianPrior[i];
                            marginal += jointProbs[i];
                        }
                    } else {
                        for (int i = 0; i < 360; ++i) {
                            jointProbs[i] = Utility.getCompassErrorProbablility((int) Math.abs(compassReadInBenchmark - i)) * m_bayesianPrior[i];
                            marginal += jointProbs[i];
                        }
                    }
                    break;
            }

            double[] probs = new double[360];
            int maxAngle = 0;
            double maxPro = 0.0;
            for (int i = 0; i < 360; ++i) {
                probs[i] = jointProbs[i]/marginal;
                if (probs[i] > maxPro) {
                    maxPro = probs[i];
                    maxAngle = i;
                }
            }

            // update prior with last record
            System.arraycopy(probs, 0, m_bayesianPrior, 0, jointProbs.length);
            return maxAngle;
        }
    }

    public void setRotationData(float[] values) {
        m_compassData.setRotationData(values);
    }

    public void setIsAccurate (boolean isAccurate) {
        m_compassData.setIsAccurate(isAccurate);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        showMessage(canvas);

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
        //showLogCount(canvas);
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

    private void showLogCount(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.BLUE);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        m_paint.setStrokeWidth(m_textStrokeWidth);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawText("Count : " + m_logCount, displayMetrics.widthPixels * 0.75f, displayMetrics.heightPixels * 0.1f, m_paint);
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
        float top = displayMetrics.heightPixels * 0.5f - m_localCoordinateRadius;
        float right = displayMetrics.widthPixels;
        float bottom = displayMetrics.heightPixels * 0.5f + m_localCoordinateRadius;
        RectF disRect = new RectF(left, top, right, bottom);

        m_paint.setStrokeWidth(m_boundaryStrokeWidth);
        canvas.drawArc(disRect, 160.0f, 220.0f, false, m_paint);

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
                double angle_remote = calculateRemoteAngle_Benchmark(getAngleInBenchmark(), info.m_angleInBenchmark, m_isBenchmark, info.m_isBenchmark);
                float pointX = m_localCoordinateCenterX + m_localCoordinateRadius * (float)Math.cos(Math.toRadians(angle_remote));
                float pointY = m_localCoordinateCenterY - m_localCoordinateRadius * (float)Math.sin(Math.toRadians(angle_remote));
                m_paint.setColor(info.m_color);
                m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawCircle(pointX, pointY, m_remotePhoneRadius, m_paint);

                m_paint.setTextSize(m_textSize);
                m_paint.setStrokeWidth(m_textStrokeWidth);
                float textX1 = pointX - m_remotePhoneRadius;
                float textY1 = pointY - m_remotePhoneRadius * 1.5f;
                canvas.drawText(String.format("%.3f", info.m_angleInBenchmark), textX1, textY1, m_paint);

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
        }
    }

    private void showBoundary(Canvas canvas) {
        m_paint.setColor(Color.RED);
        m_paint.setStrokeWidth(m_boundaryStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawLine(0, displayMetrics.heightPixels * 0.75f, displayMetrics.widthPixels, displayMetrics.heightPixels * 0.75f, m_paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float X = event.getX();
        float Y = event.getY();
        float touchRadius = event.getTouchMajor();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
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
                        double angle_remote = calculateRemoteAngle_Benchmark(getAngleInBenchmark(), remotePhone.m_angleInBenchmark, m_isBenchmark, remotePhone.m_isBenchmark);
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
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (getShowRemoteNames()) {
                    boolean show = false;

                    for (RemotePhoneInfo remotePhone : m_remotePhones) {
                        double angle_remote = calculateRemoteAngle_Benchmark(getAngleInBenchmark(), remotePhone.m_angleInBenchmark,  m_isBenchmark, remotePhone.m_isBenchmark);
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

                        if (!isOverlap & !isBoundary(X, Y)) {
                            ball.m_ballX = X;
                            ball.m_ballY = Y;
                            this.invalidate();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(mLongPressed);
                if (getShowRemoteNames()) {
                    setShowRemoteNames(false);
                    invalidate();
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

                        if (!isOverlap) {
                            String name = isSending(ball.m_ballX, ball.m_ballY);
                            if (!name.isEmpty()) {
                                ((MainActivity) getContext()).showToast("send ball to : " + name);
                                sendBall(ball, name);
                                removeBall(ball.m_id);
                                this.invalidate();
                            }
                        }
                    }
                }

                for (Ball ball : m_balls) {
                    ball.m_isTouched = false;
                }
                break;
        }
        return true;
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
                double angle_remote = calculateRemoteAngle_Benchmark(getAngleInBenchmark(), remotePhoneInfo.m_angleInBenchmark,  m_isBenchmark, remotePhoneInfo.m_isBenchmark);
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
            jsonObject.put("compassangle", m_compassData.getRotationVector().m_z);
            jsonObject.put("lightangle", m_lightData.getAngle());
            jsonObject.put("angleinbenchmark", getAngleInBenchmark());
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
            msg.put("compassangle", m_compassData.getRotationVector().m_z);
            msg.put("lightangle", m_lightData.getAngle());
            msg.put("angleinbenchmark", getAngleInBenchmark());
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

}
