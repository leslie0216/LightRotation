package usask.chl848.lightrotation;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * sensor data from device
 */
public class SensorData {
    private MainActivity m_activity;
    private SensorManager sm;
    private boolean m_isSensorRegistered = false;
    private static final AtomicBoolean computing = new AtomicBoolean(false);

    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];
    float[] smoothed = new float[3];
    private int m_magneticFieldAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    private static final int MAX_ACCURATE_COUNT = 20;
    private static final int MAX_INACCURATE_COUNT = 20;
    private volatile int m_accurateCount;
    private volatile int m_inaccurateCount;
    private volatile boolean m_isCalibration = true;

    private static LocationManager locationMgr = null;
    private static Location currentLocation = null;
    private static GeomagneticField gmf = null;
    private static final int MIN_TIME = 30 * 1000;
    private static final int MIN_DISTANCE = 10;

    final SensorEventListener myListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!computing.compareAndSet(false, true)) return;

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                smoothed = LowPassFilter.filter(event.values, accelerometerValues);
                accelerometerValues = smoothed;
            }

            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                smoothed = LowPassFilter.filter(event.values, magneticFieldValues);
                magneticFieldValues = smoothed;
                m_magneticFieldAccuracy = event.accuracy;
            }

            calculateOrientation();

            computing.set(false);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    final LocationListener myLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location == null) throw new NullPointerException();
            currentLocation = (location);
            gmf = new GeomagneticField((float) currentLocation.getLatitude(), (float) currentLocation.getLongitude(), (float) currentLocation.getAltitude(),
                    System.currentTimeMillis());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

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

            locationMgr = (LocationManager) m_activity.getSystemService(Context.LOCATION_SERVICE);

            locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, myLocationListener);

            try {
                /* defaulting to our place */
                Location hardFix = new Location("SK");
                hardFix.setLatitude(52.13239);
                hardFix.setLongitude(-106.635931);
                hardFix.setAltitude(1);

                try {
                    Location gps = locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Location network = locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (gps != null) currentLocation = (gps);
                    else if (network != null) currentLocation = (network);
                    else currentLocation = (hardFix);
                } catch (Exception ex2) {
                    currentLocation = (hardFix);
                }
                myLocationListener.onLocationChanged(currentLocation);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void unRegisterSensors() {
        sm.unregisterListener(myListener);
        locationMgr.removeUpdates(myLocationListener);
        m_isSensorRegistered = false;
    }

    private void calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];

        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);

        SensorManager.getOrientation(R, values);

        values[0] = (float)Math.toDegrees(values[0]); // bearing
        values[1] = (float)Math.toDegrees(values[1]);
        values[2] = (float)Math.toDegrees(values[2]);

        if (gmf != null) {
            values[0] += gmf.getDeclination();
        }

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
