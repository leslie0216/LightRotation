package usask.chl848.lightrotation;

import android.content.Context;

/**
 * moveball
 */
public class BallFlickThread extends Thread {
    private Context m_context;
    private MainView.FlickInfo m_flickInfo;

    public BallFlickThread(Context context, MainView.FlickInfo flickInfo) {
        m_context = context;
        m_flickInfo = flickInfo;
    }

    @Override
    public void run() {
        MainActivity ma = (MainActivity)m_context;

        if (ma.m_drawView.getTouchedBallId() > -1) {
            while (ma.m_drawView.isFlicking()) {
                float newX, newY;
                long currentTime = System.currentTimeMillis();
                long timeElapse = currentTime - m_flickInfo.startTime;
                float xDist = m_flickInfo.velocityX * timeElapse;
                float yDist = m_flickInfo.velocityY * timeElapse;
                //Log.d(MainActivity.TAG, "doFlick() timeElapse = " + timeElapse + ", xDist = " + xDist + ", yDist = " + yDist);
                newX = m_flickInfo.startX + xDist;
                newY = m_flickInfo.startY + yDist;
                ma.m_drawView.moveBall(newX, newY);
                ma.m_drawView.postInvalidate();
            }

            ma.m_drawView.setLock(false);
        }
    }
}
