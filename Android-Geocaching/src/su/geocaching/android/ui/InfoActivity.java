package su.geocaching.android.ui;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

import su.geocaching.android.controller.Controller;
import su.geocaching.android.controller.UiHelper;
import su.geocaching.android.controller.apimanager.DownloadPageTask;
import su.geocaching.android.controller.apimanager.DownloadPhotoTask;
import su.geocaching.android.controller.managers.DbManager;
import su.geocaching.android.controller.managers.LogManager;
import su.geocaching.android.model.GeoCache;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebBackForwardList;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author Alekseenko Vladimir
 */
public class InfoActivity extends Activity {

    private static final String TAG = InfoActivity.class.getCanonicalName();
    private static final String GEOCACHE_INFO_ACTIVITY_FOLDER = "/GeoCacheInfoActivity";
    private static final String HTML_ENCODING = "UTF-8";
    private static final String PAGE_TYPE = "page type", SCROOLX = "scrollX", SCROOLY = "scrollY", ZOOM = "ZOOM", CURRENTPCTURE = "currentPicture", FileDir = "Android-Geocaching";

    public enum PageType {
        INFO, NOTEBOOK
    }

    private WebView webView;
    private WebViewClient webViewClient;
    private CheckBox cbFavoriteCache;
    private Controller controller;
    private DbManager dbManager;
    private GeoCache geoCache;
    private AsyncTask<Void, Void, String> infoTask, notebookTask;
    private AsyncTask<Void, Integer, List<Bitmap>> photoTask;

    private PageType pageType = PageType.INFO;
    private float webViewZoom;
    private int webViewScrollY, webViewScrollX;
    private boolean isCacheStoredInDataBase;
    private boolean isPhotoStored;

    private boolean goToMap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogManager.d(TAG, "onCreate");
        setContentView(R.layout.info_activity);
        controller = Controller.getInstance();
        dbManager = controller.getDbManager();
        geoCache = getIntent().getParcelableExtra(GeoCache.class.getCanonicalName());
        if (savedInstanceState != null) {
            pageType = PageType.values()[savedInstanceState.getInt(PAGE_TYPE, PageType.INFO.ordinal())];
        }
        initViews(savedInstanceState);

        isCacheStoredInDataBase = dbManager.isCacheStored(geoCache.getId());
        if (isCacheStoredInDataBase) {
            cbFavoriteCache.setChecked(true);
        }
        controller.getGoogleAnalyticsManager().trackPageView(GEOCACHE_INFO_ACTIVITY_FOLDER);
        loadWebView(pageType);
    }

    private void initViews(Bundle savedInstanceState) {
        TextView tvName = (TextView) findViewById(R.id.info_text_name);
        TextView tvTypeGeoCache = (TextView) findViewById(R.id.info_GeoCache_type);
        TextView tvStatusGeoCache = (TextView) findViewById(R.id.info_GeoCache_status);
        tvName.setText(geoCache.getName());
        tvTypeGeoCache.setText(controller.getResourceManager().getGeoCacheType(geoCache));
        tvStatusGeoCache.setText(controller.getResourceManager().getGeoCacheStatus(geoCache));
        ImageView image = (ImageView) findViewById(R.id.imageCache);
        image.setImageDrawable(controller.getResourceManager(this).getMarker(geoCache.getType(), geoCache.getStatus()));
        cbFavoriteCache = (CheckBox) findViewById(R.id.info_geocache_add_del);
       
        webView = (WebView) findViewById(R.id.info_web_brouse);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(CURRENTPCTURE)) {
                final File f = new File(savedInstanceState.getString(CURRENTPCTURE));
                webView.restorePicture(savedInstanceState, f);
                f.delete();
            }
            webView.restoreState(savedInstanceState);
        } else {
            webViewClient = new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    String urlNotebook = String.format("http://pda.geocaching.su/note.php?cid=%d&mode=0", geoCache.getId());
                    String urlInfoGeocache = String.format("http://pda.geocaching.su/cache.php?cid=%d", geoCache.getId());
                    if (url == null || url.equals(urlInfoGeocache)) {
                        togglePageType();
                        return true;
                    }

                    if (url.regionMatches(0, urlNotebook, 0, urlNotebook.length() - 1)) {
                        if (url.length() == urlNotebook.length()) {
                            togglePageType();
                            return true;
                        } else {
                            return false;
                        }
                    }
                    Uri uri = Uri.parse(url);
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    return true;
                };

                @Override
                public void onLoadResource(WebView view, String url) {
                    shouldOverrideUrlLoading(view, url);
                }
            };

            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(webViewClient);
            webView.getSettings().setBuiltInZoomControls(true);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            loadWebView(pageType);
        }
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(PAGE_TYPE, pageType.ordinal());
        outState.putInt(SCROOLX, webView.getScrollX());
        outState.putInt(SCROOLY, webView.getScrollY());

        final WebBackForwardList list = webView.saveState(outState);
        File mThubnailDie = getDir(FileDir, 0);
        if (list != null) {
            final File f = new File(mThubnailDie, webView.hashCode() + "_picSave");
            if (webView.savePicture(outState, f)) {
                outState.putString(CURRENTPCTURE, f.getPath());
            }
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

//    @Override
//    protected Dialog onCreateDialog(int id) {
//        return new DownloadNotebookDialog(this, infoTask, geoCache);
//    }

    private void saveCacheInDB() {
        try {
            isCacheStoredInDataBase = true;
            if (infoTask == null) {
                infoTask = new DownloadPageTask(this, geoCache.getId(), webViewScrollX, webViewScrollY, null, null, PageType.INFO).execute();
            }
            if (notebookTask == null) {
                if (!goToMap && controller.getConnectionManager().isInternetConnected()) {
                    if (!controller.getPreferencesManager().getDownloadNoteBookAlways()) {
                        {
                            showDialog(0);
                        }
                    } else {
                        notebookTask = new DownloadPageTask(this, geoCache.getId(), webViewScrollX, webViewScrollY, null, null, PageType.NOTEBOOK).execute();
                        dbManager.addGeoCache(geoCache, infoTask.get(), notebookTask.get());
                    }
                } else {
                    dbManager.addGeoCache(geoCache, infoTask.get(), "");
                }
            } else {
                dbManager.addGeoCache(geoCache, infoTask.get(), notebookTask.get());
            }
        } catch (InterruptedException e) {
            LogManager.e(TAG, "InterruptedException", e);
        } catch (ExecutionException e) {
            LogManager.e(TAG, "ExecutionException", e);
        }
    }

    private void delCacheFromDB() {
        isCacheStoredInDataBase = false;
        dbManager.deleteCacheById(geoCache.getId());
    }

    public void onAddDelGeoCacheInDatabaseClick(View v) {
        if (cbFavoriteCache.isChecked()) {
            saveCacheInDB();
        } else {
            delCacheFromDB();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.geocache_info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.show_web_notebook_cache:
                togglePageType();
                return true;
            case R.id.show_web_add_delete_cache:
                cbFavoriteCache.performClick();
                return true;
            case R.id.show_web_search_cache:
                goToMap();
                return true;
            case R.id.show_geocache_notes:
                startActivity(new Intent(this, CacheNotesActivity.class));
                return true;
            case R.id.show_cache_photos:
                if (isPhotoStored) {
                    Intent intent = new Intent(this, GalleryActivity.class);
                    intent.putExtra(GeoCache.class.getCanonicalName(), geoCache.getId());
                    startActivity(intent);
                } else {
                    photoTask = new DownloadPhotoTask(this, geoCache.getId()).execute();
                }
                return true;
           default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean isPhotoStored(int cacheId) {
        File dir = new File(Environment.getExternalStorageDirectory(), String.format(this.getString(R.string.cache_directory), cacheId));
        String[] imageNames = dir.list();
        if (imageNames != null) {
            return imageNames.length != 0;
        }
        return imageNames != null;
    }

  

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        isPhotoStored = isPhotoStored(geoCache.getId());
        if (isPhotoStored){
            menu.getItem(2).setTitle(R.string.menu_show_cache_photos);
            menu.getItem(2).setIcon(R.drawable.ic_menu_gallery);

        } else {
            menu.getItem(2).setTitle(R.string.menu_download_cache_photos);
            menu.getItem(2).setIcon(R.drawable.ic_menu_upload);
        }

        if (pageType == PageType.INFO) {
            menu.getItem(0).setTitle(R.string.menu_show_web_notebook_cache);
            menu.getItem(0).setIcon(R.drawable.ic_menu_notebook);
            if (!isCacheStoredInDataBase && !controller.getConnectionManager().isInternetConnected()) {
                menu.getItem(0).setEnabled(false);
            } else {
                menu.getItem(0).setEnabled(true);
            }
        } else {
            menu.getItem(0).setTitle(R.string.menu_show_info_cache);
            menu.getItem(0).setIcon(R.drawable.ic_menu_info_details);
        }

        if (isCacheStoredInDataBase) {
            menu.getItem(3).setTitle(R.string.menu_show_web_delete_cache);
            menu.getItem(3).setIcon(R.drawable.ic_menu_off);
        } else {
            menu.getItem(3).setTitle(R.string.menu_show_web_add_cache);
            menu.getItem(3).setIcon(android.R.drawable.ic_menu_save);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void togglePageType() {

        switch (pageType) {
            case INFO:
                pageType = PageType.NOTEBOOK;
                break;
            case NOTEBOOK:
                pageType = PageType.INFO;
                break;
        }
        loadWebView(pageType);
    }

    private void loadWebView(PageType type) {
        LogManager.d(TAG, "loadWebView PageType " + type);
        if (isCacheStoredInDataBase || controller.getConnectionManager().isInternetConnected()) {
            try {
                switch (type) {
                    case INFO:
                        if (infoTask == null) {
                            infoTask = new DownloadPageTask(this, geoCache.getId(), webViewScrollX, webViewScrollY, webView, null, PageType.INFO).execute();
                        } else {
                            String infoText = infoTask.get();
                            infoTask = new DownloadPageTask(this, geoCache.getId(), webViewScrollX, webViewScrollY, webView, infoText, PageType.INFO).execute();
                        }
                        break;
                    case NOTEBOOK:
                        if (notebookTask == null) {
                            notebookTask = new DownloadPageTask(this, geoCache.getId(), webViewScrollX, webViewScrollY, webView, null, PageType.NOTEBOOK).execute();
                        } else {
                            String notebookText = notebookTask.get();
                            notebookTask = new DownloadPageTask(this, geoCache.getId(), webViewScrollX, webViewScrollY, webView, notebookText, PageType.NOTEBOOK).execute();
                        }
                        break;
                }
            } catch (InterruptedException e) {
                LogManager.e(TAG, "InterruptedException", e);
            } catch (ExecutionException e) {
                LogManager.e(TAG, "ExecutionException", e);
            }
        } else {
            webView.loadData("<?xml version='1.0' encoding='utf-8'?><center>" + getString(R.string.info_geocach_not_internet_and_not_in_DB) + "</center>", "text/html", HTML_ENCODING);
        }
    }

    public void onMapClick(View v) {
        goToMap();
    }

    private void goToMap() {
        if (!isCacheStoredInDataBase) {
            goToMap = true;
            cbFavoriteCache.setChecked(true);
            saveCacheInDB();
        }
        goToMap = false;
        UiHelper.startSearchMapActivity(this, geoCache);
    }

    public void onHomeClick(View v) {
        UiHelper.goHome(this);
    }
}