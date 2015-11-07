package com.pootiapps.runforlife.main;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;

import java.util.HashMap;
import java.util.List;

/**
 * Created by ksingh on 11/7/2015.
 */


public class RunForLife {
    private Map map = null;
    private MapFragment mapFragment = null;
    private BoundingBox boundingBox = new BoundingBox();

    public java.util.Map<DirectionEnum,List<GeoCoordinate>> getRunningRoute(GeoCoordinate geoCoordinate, double distance){
        return getRunningRoute(geoCoordinate,distance,DirectionEnum.NE);
    }

    public java.util.Map<DirectionEnum,List<GeoCoordinate>> getNextRunningRoute(GeoCoordinate geoCoordinate, double distance, DirectionEnum directionEnum){
        return getRunningRoute(geoCoordinate,distance,directionEnum.next());
    }

    public java.util.Map<DirectionEnum,List<GeoCoordinate>> getRunningRoute(GeoCoordinate geoCoordinate, double distance, DirectionEnum directionEnum){
        List<GeoCoordinate> geoCoordinateList = boundingBox.getBoundingBox(geoCoordinate,distance,directionEnum);
        java.util.Map<DirectionEnum,List<GeoCoordinate>> runningMap = new HashMap<DirectionEnum,List<GeoCoordinate>>();
        runningMap.put(directionEnum,geoCoordinateList);
        return runningMap;
    }
}
