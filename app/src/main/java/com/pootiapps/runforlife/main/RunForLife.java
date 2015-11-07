package com.pootiapps.runforlife.main;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;

/**
 * Created by ksingh on 11/7/2015.
 */


public class RunForLife {
    private Map map = null;
    private MapFragment mapFragment = null;

    public java.util.Map<DirectionEnum,MapRoute> getRunningRoute(GeoCoordinate geoCoordinate){
        return getRunningRoute(geoCoordinate,DirectionEnum.NE);
    }

    public java.util.Map<DirectionEnum,MapRoute> getNextRunningRoute(GeoCoordinate geoCoordinate, DirectionEnum directionEnum){
        return getRunningRoute(geoCoordinate,directionEnum.next());
    }

    public java.util.Map<DirectionEnum,MapRoute> getRunningRoute(GeoCoordinate geoCoordinate, DirectionEnum directionEnum){
        return null;
    }
}
