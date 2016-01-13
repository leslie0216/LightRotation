package usask.chl848.lightrotation;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * sensor data from device
 */
public class SensorData {
    private MainActivity m_activity;
    private SensorManager sm;
    private boolean m_isSensorRegistered = false;

    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];
    private int m_magneticFieldAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    private static final int MAX_ACCURATE_COUNT = 20;
    private static final int MAX_INACCURATE_COUNT = 20;
    private volatile int m_accurateCount;
    private volatile int m_inaccurateCount;
    private volatile boolean m_isCalibration = true;

    final SensorEventListener myListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelerometerValues = event.values;
            }

            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticFieldValues = event.values;
                m_magneticFieldAccuracy = event.accuracy;
            }

            calculateOrientation();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    SensorData(MainActivity activity) {
        m_activity = activity;
    }

    private void resetAccurateCount() {
        m_accurateCount = 0;
    }

    private void increaseAccurateCount() {
        m_accurateCount++;
    }

    private void resetInaccurateCount() {
        m_inaccurateCount = 0;
    }

    private void increaseInaccurateCount() {
        m_inaccurateCount++;
    }

    public void registerSensors() {
        if (!m_isSensorRegistered) {
            m_isSensorRegistered = true;
            sm = (SensorManager) m_activity.getSystemService(Context.SENSOR_SERVICE);

            Sensor aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            sm.registerListener(myListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sm.registerListener(myListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void unRegisterSensors() {
        sm.unregisterListener(myListener);
        m_isSensorRegistered = false;
    }

    private void calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];

        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);

        SensorManager.getOrientation(R, values);

        values[0] = (float)Math.toDegrees(values[0]);
        values[1] = (float)Math.toDegrees(values[1]);
        values[2] = (float)Math.toDegrees(values[2]);

        calculateAccuracy();

        updateView(values);
    }

    private void calculateAccuracy() {
        double data = Math.sqrt(Math.pow(magneticFieldValues[0], 2) + Math.pow(magneticFieldValues[1], 2) + Math.pow(magneticFieldValues[2], 2));

        if (m_isCalibration) {
            if (m_magneticFieldAccuracy != SensorManager.SENSOR_STATUS_UNRELIABLE && (data >= 25 && data <= 65)) {
                increaseAccurateCount();
            } else {
                resetAccurateCount();
            }

            if (m_accurateCount >= MAX_ACCURATE_COUNT) {
                m_isCalibration = false;
                resetInaccurateCount();
            }

        } else {
            if (m_magneticFieldAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || (data < 25 || data > 65)) {
                increaseInaccurateCount();
            } else {
                resetInaccurateCount();
            }

            if (m_inaccurateCount >= MAX_INACCURATE_COUNT) {
                m_isCalibration = true;
                resetAccurateCount();
            }
        }
    }

    public  void updateView(float[] values) {
        if(m_activity.m_drawView != null) {
            m_activity.m_drawView.setRotationData(values, !m_isCalibration);
        }
    }
}
