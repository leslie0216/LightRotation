package usask.chl848.lightrotation;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Compass data
 */
public class CompassData {
    public class RotationVector {
        float m_x;
        float m_y;
        float m_z;
    }
    private final Queue<RotationVector> m_rotationVectorQueue = new LinkedList<>();
    private static final int m_filterSize = 15;
    private boolean m_isAccurate = true;

    public void setRotationData(float[] values) {
        RotationVector rotationVector = new RotationVector();
        rotationVector.m_x = values[1];
        rotationVector.m_y = values[2];
        rotationVector.m_z = values[0];

        if (rotationVector.m_z < 0) {
            rotationVector.m_z += 360;
        }

        /*
        if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            rotationVector.m_z += 90;
        }*/

        synchronized (m_rotationVectorQueue) {
            int size = m_rotationVectorQueue.size();
            if (size >= m_filterSize) {
                m_rotationVectorQueue.poll();
            }

            m_rotationVectorQueue.offer(rotationVector);
        }
        //this.invalidate();
    }

    public void setIsAccurate (boolean isAccurate) {
        m_isAccurate = isAccurate;
    }

    public boolean isAccurate()
    {
        return m_isAccurate;
    }

    public RotationVector getRotationVector() {
        RotationVector rotationVector = new RotationVector();
        float x, y, z;
        x = y = z = 0.0f;
        synchronized (m_rotationVectorQueue) {
            int size = m_rotationVectorQueue.size();
            for (RotationVector rv : m_rotationVectorQueue) {
                x += rv.m_x;
                y += rv.m_y;
                z += rv.m_z;
            }
            rotationVector.m_x = x / size;
            rotationVector.m_y = y / size;
            rotationVector.m_z = z / size;
        }
        return rotationVector;
    }
}
