package com.pootiapps.runforlife.main;

import com.here.android.mpa.common.GeoCoordinate;
import com.pootiapps.runforlife.utils.Geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ksingh on 11/7/2015.
 */
public class BoundingBox {

    private static final double absoluteDirection = 360;

    public List<GeoCoordinate> getBoundingBox(GeoCoordinate geoCoordinate, double distance, DirectionEnum directionEnum){
        Geometry geometry = new Geometry();
        List<GeoCoordinate> geoCoordinateList = new ArrayList<GeoCoordinate>();
        int direction = directionEnum.getDirection();
        double dist = distance/4;
        double bCourse = (direction + 45) % absoluteDirection;
        double cCourse = direction;
        double dCourse = (direction - 45) % absoluteDirection;
        double bDistance = dist;
        double cDistance = dist * Math.sqrt(2);
        double dDistance = dist;
        geoCoordinateList.add(geoCoordinate);
        geoCoordinateList.add(geometry.extrapolate(geoCoordinate,bCourse,bDistance));
        geoCoordinateList.add(geometry.extrapolate(geoCoordinate,cCourse,cDistance));
        geoCoordinateList.add(geometry.extrapolate(geoCoordinate,dCourse,dDistance));
        return geoCoordinateList;
    }
}
