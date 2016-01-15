package usask.chl848.lightrotation;

//import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * handle gestures performed on the screen
 */
public class GestureDetectorData implements GestureDetector.OnGestureListener {

    private MainView m_view;
    GestureDetectorData(MainView mv) {
        m_view = mv;

    }


    @Override
    public boolean onDown(MotionEvent e) {
        //Log.d(MainActivity.TAG, "GestureDetectorData::onDown()");
        return m_view.isFlickEnabled() && m_view.isFlicking() || m_view.onTouchDown(e);
    }

    @Override
    public void onShowPress(MotionEvent e) {
        //Log.d(MainActivity.TAG, "GestureDetectorData::onShowPress()");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        //Log.d(MainActivity.TAG, "GestureDetectorData::onSingleTapUp()");
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //Log.d(MainActivity.TAG, "GestureDetectorData::onScroll(), start = (" + e1.getX() + "," + e1.getY() + "), end = (" + e2.getX() + "," + e2.getY() + ")");
        return m_view.isFlickEnabled() && m_view.isFlicking() || m_view.onTouchMove(e2);
    }

    @Override
    public void onLongPress(MotionEvent e) {
        //Log.d(MainActivity.TAG, "GestureDetectorData::onLongPress()");
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        //Log.d(MainActivity.TAG, "GestureDetectorData::onFling(), start = (" + e1.getX() + "," + e1.getY() + "), end = (" + e2.getX() + "," + e2.getY() + "),vx = " + velocityX + ", vy = " + velocityY);
        return m_view.isFlickEnabled() && m_view.isFlicking() || m_view.onFlick(e1, e2, velocityX, velocityY);
    }

    public boolean onUp(MotionEvent e) {
        //Log.d(MainActivity.TAG, "GestureDetectorData::onUp()");
        return m_view.isFlickEnabled() && m_view.isFlicking() || m_view.onTouchUp(e);
    }
}
