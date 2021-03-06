package su.geocaching.android.ui.preferences;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import su.geocaching.android.controller.Controller;
import su.geocaching.android.controller.managers.NavigationManager;
import su.geocaching.android.ui.R;

public class TrafficPreferenceActivity extends SherlockPreferenceActivity {
    private static final String MOBILE_TRAFFIC_ACTIVITY_NAME = "/preferences/MobileTraffic";

    /*
    * (non-Javadoc)
    *
    * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Controller.getInstance().getGoogleAnalyticsManager().trackActivityLaunch(MOBILE_TRAFFIC_ACTIVITY_NAME);
        getSupportActionBar().setHomeButtonEnabled(true);
        addPreferencesFromResource(R.xml.traffic_preference);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavigationManager.startDashboardActivity(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}