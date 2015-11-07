package com.pootiapps.runforlife.utils;

import com.here.android.mpa.common.GeoCoordinate;

/**
 * Created by ksingh on 11/7/2015.
 */
public class Geometry {
    private static int eRadius = 6378137;
    private static double MINUTES_TO_METERS = 1852d;
    private static double DEGREE_TO_MINUTES = 60d;

    private double toRadians(double degrees)
    {
        return (Math.PI / 180) * degrees;
    }

    private double toDegrees(double radians)
    {
        return (180 / Math.PI) * radians;
    }

    public double getDistance(GeoCoordinate g1, GeoCoordinate g2)
    {
        double distance = 0;
        double lat1 = g1.getLatitude();
        double lat2 = g2.getLatitude();
        double lng1 = g1.getLongitude();
        double lng2 = g2.getLongitude();

        // conversion to radians
        lat1 = toRadians(lat1);
        lng1 = toRadians(lng1);

        lat2 = toRadians(lat2);
        lng2 = toRadians(lng2);

        double dlat = lat2 - lat1;
        double dlng = lng2 - lng1;

        double bX = Math.cos(lat2) * Math.cos(dlng);
        double bY = Math.cos(lat2) * Math.sin(dlng);

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlng / 2) * Math.sin(dlng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        distance = eRadius * c;

        return distance;
    }

    public GeoCoordinate getMidPoint(GeoCoordinate g1, GeoCoordinate g2)
    {
        double lat1 = g1.getLatitude();
        double lat2 = g2.getLatitude();
        double lng1 = g1.getLongitude();
        double lng2 = g2.getLongitude();

        // conversion to radians
        lat1 = toRadians(lat1);
        lng1 = toRadians(lng1);

        lat2 = toRadians(lat2);
        lng2 = toRadians(lng2);

        double dlat = lat2 - lat1;
        double dlng = lng2 - lng1;

        double bX = Math.cos(lat2) * Math.cos(dlng);
        double bY = Math.cos(lat2) * Math.sin(dlng);
        double lat = Math.atan2(Math.sin(lat1) + Math.sin(lat2),
                Math.sqrt((Math.cos(lat1) + bX) * (Math.cos(lat1) + bX) +
                        bY * bY));
        double lng = lng1 + Math.atan2(bY, Math.cos(lat1) + bX);

        return new GeoCoordinate(toDegrees(lat),toDegrees(lng));
    }

    public GeoCoordinate extrapolate(GeoCoordinate g1, double course, double distance)
    {
        double crs = toRadians(course);
        double dist = toRadians(distance / MINUTES_TO_METERS / DEGREE_TO_MINUTES);
        double lat1 = toRadians(g1.getLatitude());
        double long1 = toRadians(g1.getLongitude());

        double lat = Math.asin(Math.sin(lat1) * Math.cos(dist) + Math.cos(lat1) * Math.sin(dist) * Math.cos(crs));
        double lng = Math.atan2(Math.sin(crs) * Math.sin(dist) * Math.cos(lat1), Math.cos(dist) - Math.sin(lat1) * Math.sin(lat));
        lng = ((long1 + lng + Math.PI) % (2 * Math.PI)) - Math.PI;

        return new GeoCoordinate(toDegrees(lat),toDegrees(lng));
    }

    public double getCourse(GeoCoordinate g1, GeoCoordinate g2)
    {
        double course = 0;
        double lat1 = toRadians(g1.getLatitude());
        double lat2 = toRadians(g2.getLatitude());
        double long2 = toRadians(g2.getLongitude());

        course = Math.acos((Math.sin(lat1) - Math.sin(lat2) * Math.cos(long2)) / (Math.sin(long2) * Math.cos(lat2)));
        course = toDegrees(course);

        return course;
    }
}
