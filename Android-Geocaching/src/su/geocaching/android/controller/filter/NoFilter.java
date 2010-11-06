package su.geocaching.android.controller.filter;

import su.geocaching.android.model.dataType.GeoCache;

import java.util.LinkedList;

/**
 * Author: Yuri Denison
 * Date: 05.11.2010 1:50:54
 */
public class NoFilter implements Filter {
    private static NoFilter instance;

    private NoFilter() {
    }

    public static NoFilter getInstance() {
        if (instance == null) {
            synchronized (NoFilter.class) {
                if (instance == null) {
                    instance = new NoFilter();
                }
            }
        }
        return instance;
    }

    @Override
    public LinkedList<GeoCache> filter(LinkedList<GeoCache> list) {
        return list;
    }
}