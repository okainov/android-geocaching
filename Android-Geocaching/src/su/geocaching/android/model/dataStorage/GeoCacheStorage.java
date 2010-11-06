package su.geocaching.android.model.dataStorage;

import su.geocaching.android.model.dataType.GeoCache;

import java.util.LinkedList;


/**
 * Author: Yuri Denison
 * Date: 04.11.2010 21:16:59
 */
public class GeoCacheStorage {
    private static GeoCacheStorage instance;
    private LinkedList<GeoCache> savedGeoCaches;

    private GeoCacheStorage() {
        savedGeoCaches = new LinkedList<GeoCache>();
    }

    public static GeoCacheStorage getInstance() {
        if (instance == null) {
            synchronized (GeoCacheStorage.class) {
                if (instance == null) {
                    instance = new GeoCacheStorage();
                }
            }
        }
        return instance;
    }

    /**
     * Get GeoCache from favorites by ID
     */
    public GeoCache getGeoCacheByID(int id) {
        for (GeoCache geoCache : savedGeoCaches) {
            if (geoCache.getId() == id) {
                return geoCache;
            }
        }
        return null;
    }

    public LinkedList<GeoCache> getGeoCacheList() {
        return savedGeoCaches;
    }

    /**
     * Adding new GeoCache to favorites
     */
    public void add(GeoCache geoCache) {
        savedGeoCaches.add(geoCache);
    }
}