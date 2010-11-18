package su.geocaching.android.ui.searchgeocache;

import su.geocaching.android.model.datatype.GeoCache;
import su.geocaching.android.ui.R;
import su.geocaching.android.ui.geocachemap.GeoCacheLocationManager;
import su.geocaching.android.ui.geocachemap.ILocationAware;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

/**
 * @author Grigory Kalabin. grigory.kalabin@gmail.com
 * @Nov 18, 2010
 * @description This manager handle many common situation of search geocache activities
 */
public class SearchGeoCacheManager implements ILocationAware {
    private boolean isLocationFixed;
    private GeoCacheLocationManager locationManager;
    private ISearchActivity activity;
    private GeoCache geoCache;
    private GPSStatusListener gpsStatusListener;

    /**
     * @param context - activity which used this manager
     */
    public SearchGeoCacheManager(ISearchActivity context) {
	this.activity = context;
	locationManager = new GeoCacheLocationManager(this, context.getLocationManager());
	gpsStatusListener = new GPSStatusListener(activity);
    }

    /**
     * Called when activity pausing
     */
    public void onPause() {
	if (isLocationFixed) {
	    locationManager.pause();
	    gpsStatusListener.pause();
	}
    }

    /**
     * Called when activity resuming
     */
    public void onResume() {
	if (!locationManager.isBestProviderEnabled()) {
	    if (!locationManager.isBestProviderGPS()) {
		Toast.makeText(activity.getContext(), activity.getContext().getString(R.string.device_without_gps_alert), Toast.LENGTH_LONG).show();
	    }
	    askTurnOnGPS();
	} else {
	    activity.runLogic();
	}
    }

    /**
     * Ask user turn on GPS, if this disabled
     */
    private void askTurnOnGPS() {
	if (locationManager.isBestProviderEnabled()) {
	    return;
	}
	AlertDialog.Builder builder = new AlertDialog.Builder(activity.getContext());
	builder.setMessage(activity.getContext().getString(R.string.ask_enable_gps_text)).setCancelable(false)
		.setPositiveButton(activity.getContext().getString(R.string.ask_enable_gps_yes), new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
			Intent startGPS = new Intent(Settings.ACTION_SECURITY_SETTINGS);
			activity.getContext().startActivity(startGPS);
			dialog.cancel();
		    }
		}).setNegativeButton(activity.getContext().getString(R.string.ask_enable_gps_no), new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
			dialog.cancel();
			// activity is MapActivity or Activity
			((Activity) activity.getContext()).finish();
		    }
		});
	AlertDialog turnOnGPSAlert = builder.create();
	turnOnGPSAlert.show();
    }

    /**
     * Show cancelable alert which tell user what location fixing
     */
    private void showWaitingLocationFix() {
	Toast.makeText(activity.getContext(), activity.getContext().getString(R.string.waiting_location_fix_message), Toast.LENGTH_LONG).show();
	activity.updateStatus(activity.getContext().getString(R.string.waiting_location_fix_message));
    }

    /* (non-Javadoc)
     * @see su.geocaching.android.ui.geocachemap.ILocationAware#updateLocation(android.location.Location)
     */
    @Override
    public void updateLocation(Location location) {
	if (locationManager.isLocationFixed()) {
	    if (!isLocationFixed) {
		activity.updateStatus("Location fixed");
	    }
	    isLocationFixed = true;
	} else {
	    if (isLocationFixed) {
		locationManager.setLocationFixed();
	    } else {
		return;
	    }
	}
	activity.updateLocation(location);
    }

    /* (non-Javadoc)
     * @see su.geocaching.android.ui.geocachemap.ILocationAware#onStatusChanged(java.lang.String, int, android.os.Bundle)
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
	String statusString = "";
	if (!isLocationFixed()) {
	    statusString = "Fixing location:\n\t";
	}
	statusString += "Provider: " + provider + "\n\t";
	switch (status) {
	case LocationProvider.OUT_OF_SERVICE:
	    statusString += "Status: out of service";
	case LocationProvider.TEMPORARILY_UNAVAILABLE:
	    statusString += "Status: temporarily unavailable";
	case LocationProvider.AVAILABLE:
	    statusString += "Status: available";
	}
	if (provider.equals(LocationManager.GPS_PROVIDER)) {
	    statusString += "\n\t" + "Satellites: " + extras.getString("satellites");
	}
	activity.updateStatus(statusString);
    }

    @Override
    public void onProviderEnabled(String provider) {
	// its really need? can we call this?

    }

    /* (non-Javadoc)
     * @see su.geocaching.android.ui.geocachemap.ILocationAware#onProviderDisabled(java.lang.String)
     */
    @Override
    public void onProviderDisabled(String provider) {
	if (!locationManager.isBestProviderEnabled()) {
	    askTurnOnGPS();
	}
    }

    /**
     * @return true if user location has been fixed
     */
    public boolean isLocationFixed() {
	return isLocationFixed;
    }

    /**
     * Common part of init and run search geocache activities
     */
    public void runLogic() {
	Intent intent = ((Activity) activity).getIntent();
	geoCache = new GeoCache(intent.getIntExtra("GeoCache id", -1));
	isLocationFixed = intent.getBooleanExtra("location fixed", false);

	if (!isLocationFixed()) {
	    showWaitingLocationFix();
	}
	locationManager.resume();
	gpsStatusListener.resume();
    }

    /**
     * @return geocache which we search
     */
    public GeoCache getGeoCache() {
	return geoCache;
    }

    /**
     * @return current user location
     */
    public Location getCurrentLocation() {
	return locationManager.getCurrentLocation();
    }
}