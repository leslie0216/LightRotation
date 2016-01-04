package usask.chl848.lightrotation;

import android.bluetooth.BluetoothSocket;
import android.net.LocalSocket;
import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * Utility functions
 */
public class Utility {
    public static void cleanCloseFix(BluetoothSocket btSocket) throws IOException
    {
        synchronized(btSocket)
        {
            Field socketField = null;
            LocalSocket mSocket = null;
            try
            {
                socketField = btSocket.getClass().getDeclaredField("mSocket");
                socketField.setAccessible(true);

                mSocket = (LocalSocket)socketField.get(btSocket);
            }
            catch(Exception e)
            {
                e.printStackTrace();
                //PlutoLogger.Instance().write("Utility::cleanCloseFix() - Exception getting mSocket in cleanCloseFix(): " + e.toString());
            }

            if(mSocket != null)
            {
                mSocket.shutdownInput();
                mSocket.shutdownOutput();
                mSocket.close();

                mSocket = null;

                try { socketField.set(btSocket, mSocket); }
                catch(Exception e)
                {
                    e.printStackTrace();
                    //PlutoLogger.Instance().write("Utility::cleanCloseFix() - Exception setting mSocket = null in cleanCloseFix(): " + e.toString());
                }
            }


            Field pfdField = null;
            ParcelFileDescriptor mPfd = null;
            try
            {
                pfdField = btSocket.getClass().getDeclaredField("mPfd");
                pfdField.setAccessible(true);

                mPfd = (ParcelFileDescriptor)pfdField.get(btSocket);
            }
            catch(Exception e)
            {
                e.printStackTrace();
                //PlutoLogger.Instance().write("Utility::cleanCloseFix() - Exception getting mPfd in cleanCloseFix(): " + e.toString());
            }

            if(mPfd != null)
            {
                mPfd.close();

                mPfd = null;

                try { pfdField.set(btSocket, mPfd); }
                catch(Exception e)
                {
                    e.printStackTrace();
                    //PlutoLogger.Instance().write("Utility::cleanCloseFix() - Exception setting mPfd = null in cleanCloseFix(): " + e.toString());
                }
            }

        } //synchronized
    }

    /*
    private static final Map<Integer, Float> compassErrorRate;
    private static final Map<Integer, Float> compassDistribution;
    static {
        Map<Integer, Float> aMap = new HashMap<>();
        aMap.put(-18, 0.0000699653f);
        aMap.put(-17, 0.0002658681f);
        aMap.put(-16, 0.0002518751f);
        aMap.put(-16, 0.0002518751f);
        aMap.put(-15, 0.0004057987f);
        aMap.put(-14, 0.001161424f);
        aMap.put(-13, 0.003176424f);
        aMap.put(-12, 0.006856599f);
        aMap.put(-11, 0.009851114f);
        aMap.put(-10, 0.01238386f);
        aMap.put(-9, 0.01663775f);
        aMap.put(-8, 0.01679167f);
        aMap.put(-7, 0.01789712f);
        aMap.put(-6, 0.02139539f);
        aMap.put(-5, 0.02605508f);
        aMap.put(-4, 0.03473077f);
        aMap.put(-3, 0.04710064f);
        aMap.put(-2, 0.06091179f);
        aMap.put(-1, 0.08136964f);
        aMap.put(0, 0.1031708f);
        aMap.put(1, 0.1284003f);
        aMap.put(2, 0.1365863f);
        aMap.put(3, 0.1136796f);
        aMap.put(4, 0.076612f);
        aMap.put(5, 0.04466585f);
        aMap.put(6, 0.02189914f);
        aMap.put(7, 0.01029889f);
        aMap.put(8, 0.004379828f);
        aMap.put(9, 0.001819098f);
        aMap.put(10, 0.0005597224f);
        aMap.put(11, 0.0003218404f);
        aMap.put(12, 0.00008395836f);
        aMap.put(13, 0.0001679167f);
        aMap.put(14, 0.00004197918f);
        compassErrorRate = Collections.unmodifiableMap(aMap);

        Map<Integer, Float> bMap = new HashMap<>();
        for (int i=-180; i<= 180; ++i) {
            bMap.put(i, getCompassErrorRate(i));
        }
        compassDistribution = Collections.unmodifiableMap(bMap);
    }

    public static float getCompassErrorRate(int error) {
        if (compassErrorRate.containsKey(error)) {
            return compassErrorRate.get(error);
        } else {
            return 0.0f;
        }
    }

    public static float getCompassDistribution(int bin) {
        if (compassDistribution.containsKey(bin)) {
            return compassDistribution.get(bin);
        } else {
            return 0.0f;
        }
    }


    private static final Map<Integer, Float> lightErrorRate;
    private static final Map<Integer, Float> lightDistribution;
    static {
        Map<Integer, Float> aMap = new HashMap<>();
        aMap.put(-35, 0.000008853318f);
        aMap.put(-34, 0.0f);
        aMap.put(-33, 0.00002655995f);
        aMap.put(-32, 0.000008853318f);
        aMap.put(-31, 0.0f);
        aMap.put(-30, 0.0f);
        aMap.put(-29, 0.00001770664f);
        aMap.put(-28, 0.0f);
        aMap.put(-27, 0.00001770664f);
        aMap.put(-26, 0.0f);
        aMap.put(-25, 0.00002655995f);
        aMap.put(-24, 0.00001770664f);
        aMap.put(-23, 0.00002655995f);
        aMap.put(-22, 0.00007082655f);
        aMap.put(-21, 0.00008853318f);
        aMap.put(-20, 0.00007967986f);
        aMap.put(-19, 0.0001239465f);
        aMap.put(-18, 0.00005311991f);
        aMap.put(-17, 0.00003541327f);
        aMap.put(-16, 0.00002655995f);
        aMap.put(-15, 0.00004426659f);
        aMap.put(-14, 0.00007082655f);
        aMap.put(-13, 0.0001327998f);
        aMap.put(-12, 0.0008764785f);
        aMap.put(-11, 0.001478504f);
        aMap.put(-10, 0.001832637f);
        aMap.put(-9, 0.01125257f);
        aMap.put(-8, 0.07617395f);
        aMap.put(-7, 0.1034864f);
        aMap.put(-6, 0.06290283f);
        aMap.put(-5, 0.02662193f);
        aMap.put(-4, 0.01638749f);
        aMap.put(-3, 0.01434238f);
        aMap.put(-2, 0.01983143f);
        aMap.put(-1, 0.02634748f);
        aMap.put(0, 0.02496636f);
        aMap.put(1, 0.02344359f);
        aMap.put(2, 0.03662618f);
        aMap.put(3, 0.3285466f);
        aMap.put(4, 0.1204405f);
        aMap.put(5, 0.06342517f);
        aMap.put(6, 0.02050429f);
        aMap.put(7, 0.008915291f);
        aMap.put(8, 0.003364261f);
        aMap.put(9, 0.002399249f);
        aMap.put(10, 0.002195623f);
        aMap.put(11, 0.001443091f);
        aMap.put(12, 0.0006728522f);
        aMap.put(13, 0.0003452794f);
        aMap.put(14, 0.0001859197f);
        aMap.put(15, 0.00005311991f);
        aMap.put(16, 0.00001770664f);
        aMap.put(17, 0.00003541327f);
        aMap.put(18, 0.000008853318f);
        lightErrorRate = Collections.unmodifiableMap(aMap);
        Map<Integer, Float> bMap = new HashMap<>();
        for (int i=-180; i<= 180; ++i) {
            bMap.put(i, getLightErrorRate(i));
        }
        lightDistribution = Collections.unmodifiableMap(bMap);
    }

    public static float getLightErrorRate(int error) {
        if (lightErrorRate.containsKey(error)) {
            return lightErrorRate.get(error);
        } else {
            return 0.0f;
        }
    }

    public static float getLightDistribution(int bin) {
        if (lightDistribution.containsKey(bin)) {
            return lightDistribution.get(bin);
        } else {
            return 0.0f;
        }
    }
*/
    private static final float COMPASS_ERROR_MEAN = 0.0f;
    private static final float COMPASS_ERROR_SD = 4.03f;
    private static final float LIGHT_ERROR_MEAN = 0.0f;
    private static final float LIGHT_ERROR_SD = 4.9f;

    private static final Map<Integer, Double> compassErrorDistribution;
    static {
        NormalDistribution nd = new NormalDistribution(COMPASS_ERROR_MEAN, COMPASS_ERROR_SD);
        Map<Integer, Double> aMap = new HashMap<>();
        for (int i=-180; i<= 180; ++i) {
            aMap.put(i, nd.probability(i-0.5f, i+0.5f));
        }
        compassErrorDistribution = Collections.unmodifiableMap(aMap);
    }

    public static double getCompassErrorProbablility(int error) {
        if (compassErrorDistribution.containsKey(error)) {
            return compassErrorDistribution.get(error);
        } else {
            return 0.0;
        }
    }

    private static final Map<Integer, Double> lightErrorDistribution;
    static {
        NormalDistribution nd = new NormalDistribution(LIGHT_ERROR_MEAN, LIGHT_ERROR_SD);
        Map<Integer, Double> aMap = new HashMap<>();
        for (int i=-180; i<= 180; ++i) {
            aMap.put(i, nd.probability(i-0.5f, i+0.5f));
        }
        lightErrorDistribution = Collections.unmodifiableMap(aMap);
    }

    public static double getLightErrorProbablility(int error) {
        if (lightErrorDistribution.containsKey(error)) {
            return lightErrorDistribution.get(error);
        } else {
            return 0.0;
        }
    }
}
