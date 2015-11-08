package com.pootiapps.runforlife;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.pootiapps.runforlife.main.DirectionEnum;
import com.pootiapps.runforlife.main.RunningRoute;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import im.delight.android.location.SimpleLocation;

public class MainActivity extends AppCompatActivity {

    // map embedded in the map fragment
    private Map map = null;
    // map fragment embedded in this activity
    private MapFragment mapFragment = null;
    // Initial map scheme, initialized in onCreate() and accessed in goHome()
    private static String initial_scheme = "";
    // TextView for displaying the current map scheme
    private TextView textViewResult = null;
    // MapRoute for this activity
    private MapRoute mapRoute = null;


    private SimpleLocation location;

    private boolean paused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        location = new SimpleLocation(this);

        if(!location.hasLocationEnabled()){
            SimpleLocation.openSettings(this);
        }

        Toast.makeText(this,location.getLatitude()+", "+location.getLongitude(),Toast.LENGTH_LONG).show();

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment)getFragmentManager().findFragmentById(
                R.id.mapfragment);
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    OnEngineInitListener.Error error)
            {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();

                    GeoCoordinate geoCoordinate = new GeoCoordinate(location.getLatitude(), location.getLongitude(), 0.0);
                    // Set the map center to the Vancouver region (no animation)
                    map.setCenter(geoCoordinate, Map.Animation.NONE);
                    // Set the zoom level to the average between min and max
                    map.setZoomLevel(
                            (map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);

//                    Image image = new Image();
//                    try{
//                        image.setImageResource(R.drawable.maneuver_icon_43);
//                        MapMarker mapMarker = new MapMarker(geoCoordinate, image);
//                        mapMarker.setVisible(true);
//                    }catch (IOException e){
//                        Toast.makeText(getApplicationContext(),"could not load image",Toast.LENGTH_SHORT).show();
//                    }

                } else {
                    System.out.println("ERROR : " +error.toString());
                    System.out.println("ERROR: Cannot initialize Map Fragment");
                }
            }
        });
        textViewResult = (TextView) findViewById(R.id.title);
        textViewResult.setText(R.string.textview_routecoordinates_2waypoints);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Functionality for taps of the "Go Home" button
    public void goHome(View view) {
        if (map != null) {
        // Change map view to "home" coordinate and zoom level, plus
        // eliminate any rotation or tilt
            map.setCenter(new GeoCoordinate(location.getLatitude(), location.getLongitude(), 0.0),
                    Map.Animation.NONE);
            map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
            map.setOrientation(0);
            map.setTilt(0);
        // Reset the map scheme to the initial scheme
        }
        map.setMapScheme(initial_scheme);
    }

    private RouteManager.Listener routeManagerListener =
            new RouteManager.Listener()
            {
                public void onCalculateRouteFinished(RouteManager.Error errorCode,
                                                     List<RouteResult> result) {
                    if (errorCode == RouteManager.Error.NONE &&
                            result.get(0).getRoute() != null) {
                        // how many routes calculated?
                        System.out.println(result.size());

                        // create a map route object and place it on the map
                        mapRoute = new MapRoute(result.get(0).getRoute());
                        mapRoute.setManeuverNumberVisible(true);
                        map.addMapObject(mapRoute);
                        // Get the bounding box containing the route and zoom in
                        GeoBoundingBox gbb = result.get(0).getRoute().getBoundingBox();
                        map.zoomTo(gbb, Map.Animation.LINEAR,
                                Map.MOVE_PRESERVE_ORIENTATION);
                        textViewResult.setText(
                                String.format("Route calculated with %d maneuvers.",
                                        result.get(0).getRoute().getManeuvers().size()));
                    } else {
                        textViewResult.setText(
                                String.format("Route calculation failed: %s",
                                        errorCode.toString()));
                    }
                }
                public void onProgress(int percentage) {
                    textViewResult.setText(
                            String.format("... %d percent done ...", percentage));
                }
            };

    // Functionality for taps of the "Get Directions" button
    public void getDirections(View view) {
        // 1. clear previous results
        textViewResult.setText("");
        if (map != null && mapRoute != null) {
            map.removeMapObject(mapRoute);
            mapRoute = null;
        }
        // 2. Initialize RouteManager
        RouteManager routeManager = new RouteManager();
        // 3. Select routing options via RoutingMode
        RoutePlan routePlan = new RoutePlan();
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.PEDESTRIAN);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);
        // 4. Select Waypoints for your routes
        // START: Nokia, Burnaby
        RunningRoute runningRoute = new RunningRoute();
        DirectionEnum directionEnum = DirectionEnum.NW;
        java.util.Map<DirectionEnum,List<GeoCoordinate>> routeMap = runningRoute.getRunningRoute(
                new GeoCoordinate(location.getLatitude(), location.getLongitude()), 5000,directionEnum);
        List<GeoCoordinate> geoCoordinateList = routeMap.get(directionEnum);
        routePlan.addWaypoint(geoCoordinateList.get(0));
        routePlan.addWaypoint(geoCoordinateList.get(1));
        routePlan.addWaypoint(geoCoordinateList.get(2));
        routePlan.addWaypoint(geoCoordinateList.get(3));
        routePlan.addWaypoint(geoCoordinateList.get(0));
        // 5. Retrieve Routing information via RouteManagerListener
        RouteManager.Error error =
                routeManager.calculateRoute(routePlan, routeManagerListener);
        if (error != RouteManager.Error.NONE) {
            Toast.makeText(getApplicationContext(),
                    "Route calculation failed with: " + error.toString(),
                    Toast.LENGTH_SHORT)
                    .show();
        }
    };
    @Override
    protected void onResume() {
        super.onResume();

        // make the device update its location
        location.beginUpdates();

        // ...
    }

    @Override
    protected void onPause() {
        // stop location updates (saves battery)
        location.endUpdates();

        // ...

        super.onPause();
    }
}
