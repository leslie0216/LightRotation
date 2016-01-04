package usask.chl848.lightrotation;

import android.util.DisplayMetrics;

import org.opencv.core.Point;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Light gradient data
 */
public class LightData {
    private static final int m_filterSize = 15;

    private float m_angle;
    private Point m_minLoc;
    private Point m_maxLoc;
    private int m_imgWidth;
    private int m_imgHeight;
    private boolean m_isAccurate;
    private Queue<Float> m_angleQueue = new LinkedList<>();
    private double m_sigma;

    public LightData() {
        m_minLoc = new Point(0.0,0.0);
        m_maxLoc = new Point(0.0,0.0);
        m_imgWidth = 0;
        m_imgHeight = 0;
        m_angle = 0.0f;

        m_isAccurate = true;
        m_sigma = 0.0;
    }

    public float setLightDir(Point minLoc, Point maxLoc, int imgWidth, int imgHeight){
        m_minLoc = minLoc;
        m_maxLoc = maxLoc;
        m_imgWidth = imgWidth;
        m_imgHeight = imgHeight;
        m_angle = (float)Math.toDegrees(Math.atan2((m_minLoc.x - m_maxLoc.x), (m_minLoc.y - m_maxLoc.y)));

        if (m_angle < 0) {
            m_angle += 360;
        }

        updateAngleQueue(m_angle);

        return m_angle;
    }

    public float getAngle() {
        return m_angle;
    }

    public Point getMaxPtInScreen(DisplayMetrics displayMetrics)
    {
        return new Point((m_maxLoc.x/m_imgWidth) * displayMetrics.widthPixels, (m_maxLoc.y/m_imgHeight) * displayMetrics.heightPixels);
    }

    public Point getMinPtInScreen(DisplayMetrics displayMetrics)
    {
        return new Point((m_minLoc.x/m_imgWidth) * displayMetrics.widthPixels, (m_minLoc.y/m_imgHeight) * displayMetrics.heightPixels);
    }

    private void updateAngleQueue(float angle) {
        int size = m_angleQueue.size();
        if (size >= m_filterSize) {
            m_angleQueue.poll();
        }

        m_angleQueue.offer(angle);

        float sum = 0.0f;
        for(float a : m_angleQueue) {
            sum += a;
        }

        float mean = sum / m_angleQueue.size();

        float sqrsum = 0.0f;

        for (float b : m_angleQueue) {
            sqrsum += Math.pow(b-mean,2);
        }

        m_sigma = Math.sqrt(sqrsum / m_angleQueue.size());

        if (m_sigma > 5.0) {
            m_isAccurate = false;
        } else {
            m_isAccurate = true;
        }
    }

    public double getSigma() {
        return m_sigma;
    }

    public boolean isAccurate() {
        return m_isAccurate;
    }
}
