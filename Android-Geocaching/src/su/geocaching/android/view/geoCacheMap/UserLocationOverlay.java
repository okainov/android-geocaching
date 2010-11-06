package su.geocaching.android.view.geoCacheMap;

import android.content.Context;
import android.graphics.*;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import su.geocaching.android.view.R;

/**
 * @author Android-Geocaching.su student project team
 * @since October 2010 Overlay contains of arrow rotated by user azimuth and
 *        circle of accuracy
 */
public class UserLocationOverlay extends com.google.android.maps.Overlay {
    private Context context;
    private GeoPoint point;
    private float angle;
    private float radius; // accuracy radius in meters

    public UserLocationOverlay(Context context, GeoPoint gp, float angle,
                               float radius) {
        this.context = context;
        this.point = gp;
        this.angle = angle;
        this.radius = radius;
    }

    @Override
    public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
                        long when) {
        super.draw(canvas, mapView, shadow);

        // Translate the GeoPoint to screen pixels
        Point screenPts = new Point();
        mapView.getProjection().toPixels(point, screenPts);

        // Get default marker
        Bitmap bmpDefault = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.userpoint);

        // Rotate default marker
        Matrix matrix = new Matrix();
        matrix.setRotate(angle);
        Bitmap bmpRotated = Bitmap.createBitmap(bmpDefault, 0, 0,
                bmpDefault.getWidth(), bmpDefault.getHeight(), matrix, true);
        canvas.drawBitmap(bmpRotated, screenPts.x - bmpRotated.getWidth() / 2,
                screenPts.y - bmpRotated.getHeight() / 2, null);

        // Draw accuracy circle
        float radiusInPixels = mapView.getProjection().metersToEquatorPixels(
                radius);
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(screenPts.x, screenPts.y, radiusInPixels, paint);
        return true;
    }

    public void setPoint(GeoPoint point) {
        this.point = point;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}