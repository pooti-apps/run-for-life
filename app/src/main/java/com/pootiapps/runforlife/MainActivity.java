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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.guidance.VoiceCatalog;
import com.here.android.mpa.guidance.VoicePackage;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.Maneuver;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.pootiapps.runforlife.main.DirectionEnum;
import com.pootiapps.runforlife.main.RunningRoute;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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

    private double distanceToRun = 0;
    private double distanceToRunThresh = 0;
    private DirectionEnum directionEnum;

    private SimpleLocation location;
    private boolean paused;

    private NavigationManager navigationManager = null;
    private List<Maneuver> maneuverList = null;

    private static final double METER_IN_MILE = 1609.344;
    private static final double METER_IN_KM = 1000;
    private static final double THRESHOLD = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.navigationBtn);
        floatingActionButton.setImageResource(R.drawable.maneuver_icon_48);

        Spinner spinner = (Spinner) findViewById(R.id.distanceSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.distance_array,R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        location = new SimpleLocation(this);

        if(!location.hasLocationEnabled()){
            SimpleLocation.openSettings(this);
        }

        Toast.makeText(this, location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_LONG).show();

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

                    map.getPositionIndicator().setVisible(true);
                    PositioningManager.getInstance().addListener(
                            new WeakReference<PositioningManager.OnPositionChangedListener>(positionListener));
                    paused = false;
                    PositioningManager.getInstance().start(PositioningManager.LocationMethod.GPS_NETWORK);

                } else {
                    System.out.println("ERROR : " +error.toString());
                    System.out.println("ERROR: Cannot initialize Map Fragment");
                }
            }
        });
        textViewResult = (TextView) findViewById(R.id.title);
        textViewResult.setText(R.string.textview_routecoordinates_2waypoints);
        directionEnum = DirectionEnum.SE;
        // Register positioning listener

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

    private RouteManager.Listener routeManagerListener =
            new RouteManager.Listener()
            {
                public void onCalculateRouteFinished(RouteManager.Error errorCode,
                                                     List<RouteResult> result) {
                    if (errorCode == RouteManager.Error.NONE &&
                            result.get(0).getRoute().getLength() < distanceToRunThresh &&
                            result.get(0).getRoute() != null) {

                        // create a map route object and place it on the map

                        mapRoute = new MapRoute(result.get(0).getRoute());
                        mapRoute.setManeuverNumberVisible(true);
                        map.addMapObject(mapRoute);
                        // Get the bounding box containing the route and zoom in
                        GeoBoundingBox gbb = result.get(0).getRoute().getBoundingBox();
                        map.zoomTo(gbb, Map.Animation.LINEAR,
                                Map.MOVE_PRESERVE_ORIENTATION);
                        maneuverList = result.get(0).getRoute().getManeuvers();
                        textViewResult.setText(
                                String.format("Route calculated with %d maneuvers. Length - %d",
                                        result.get(0).getRoute().getManeuvers().size(),result.get(0).getRoute().getLength()));
                    } else {
                        int dist = result.get(0).getRoute().getLength();
                        distanceToRun-=100;
                        System.out.println("Total dist - "+dist+", reducing distance to " + distanceToRun);
                        System.out.println("Direction - "+directionEnum+", changing direction to " + directionEnum.next());

                        getRoute(distanceToRun);

//                        textViewResult.setText(
//                                String.format("Route calculation failed: %s",
//                                        errorCode.toString()));
                    }
                }
                public void onProgress(int percentage) {
                    StringBuilder progress = new StringBuilder();
                    progress.append("Calculating");
                    for(int i = 0; i < percentage%4 ; i++){
                        progress.append(".");
                    }
                    textViewResult.setText(
                            String.format(progress.toString()));
                }
            };

    // Define positioning listener
    private PositioningManager.OnPositionChangedListener positionListener = new
            PositioningManager.OnPositionChangedListener() {
                public void onPositionUpdated(PositioningManager.LocationMethod method,
                                              GeoPosition position, boolean isMapMatched) {
                    // set the center only when the app is in the foreground
                    // to reduce CPU consumption
                    if (!paused) {
                        map.setCenter(position.getCoordinate(),
                                Map.Animation.NONE);
                    }
                }
                public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                                 PositioningManager.LocationStatus status) {
                }
            };


    public void getNext(View view){
        Toast.makeText(this,"Shuffling",Toast.LENGTH_SHORT).show();
        getRoute(distanceToRun);
    }
    // Functionality for taps of the "Get Directions" button
    public void getDirections(View view) {
        EditText editText = (EditText) findViewById(R.id.distanceText);
        editText.clearFocus();
        Spinner spinner = (Spinner) findViewById(R.id.distanceSpinner);
        distanceToRun = Double.parseDouble(editText.getText().toString());
        String spinnerText = spinner.getSelectedItem().toString();
        int distanceMeasure = 0;
        if(spinnerText.equalsIgnoreCase("miles")){
            distanceMeasure = 1;
        } else if (spinnerText.equalsIgnoreCase("kms")){
            distanceMeasure = 2;
        } else {
            distanceMeasure = 3;
        }
        switch(distanceMeasure){
            case 1 :
                System.out.println("Converting "+ distanceToRun +" miles into meters.");
                distanceToRun = distanceToRun * METER_IN_MILE;
                System.out.println(" = "+distanceToRun +" meters");
                break;
            case 2 :
                System.out.println("Converting "+ distanceToRun +" kms into meters.");
                distanceToRun = distanceToRun * METER_IN_KM;
                System.out.println(" = "+distanceToRun +" meters");
                break;
            case 3 :
                break;
            default: break;
        }
        distanceToRunThresh = distanceToRun + THRESHOLD;
        getRoute(distanceToRun);
    }

    public void getRoute(double dist){
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
        System.out.println("Direction before - " + directionEnum);
        java.util.Map<DirectionEnum,List<GeoCoordinate>> routeMap = runningRoute.getNextRunningRoute(
                new GeoCoordinate(location.getLatitude(), location.getLongitude()), distanceToRun,directionEnum);
        Set<DirectionEnum> directionEnumSet = routeMap.keySet();
        directionEnum = directionEnumSet.iterator().next();
        System.out.println("Direction after - " + directionEnum);
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
    }
    @Override
    protected void onResume() {
        super.onResume();

        // make the device update its location
        location.beginUpdates();


    }

    @Override
    protected void onPause() {
        // stop location updates (saves battery)
        location.endUpdates();
        PositioningManager.getInstance().stop();
        paused = true;
        super.onPause();
    }

    @Override
    public void onDestroy(){
        PositioningManager.getInstance().removeListener(positionListener);
        navigationManager.stop();
        map = null;
        super.onDestroy();
    }

    public void startNavigation(View view){
        navigationManager = NavigationManager.getInstance();

        if(mapRoute!=null){
            // start listening to navigation events
            navigationManager.addNewInstructionEventListener(
                    new WeakReference<NavigationManager.NewInstructionEventListener>(instructListener));
            // start listening to position events
            navigationManager.addPositionListener(
                    new WeakReference<NavigationManager.PositionListener>(navPositionListener));

            NavigationManager.Error error = navigationManager.startNavigation(mapRoute.getRoute());
        }
//        System.out.println(maneuverList.size());
//        if (maneuverList.size()>0){
//            System.out.println("dist to 0" + maneuverList.get(0).getDistanceToNextManeuver());
//            System.out.println("turn to 0" + maneuverList.get(0).getTurn().value());
//            System.out.println("dist to 1" + maneuverList.get(1).getDistanceToNextManeuver());
//            System.out.println("turn to 1" + maneuverList.get(1).getTurn().value());
//            System.out.println("dist to 2" + maneuverList.get(2).getDistanceToNextManeuver());
//            System.out.println("turn to 2" + maneuverList.get(2).getTurn().value());
//            System.out.println("dist to 3" + maneuverList.get(3).getDistanceToNextManeuver());
//            System.out.println("turn to 3" + maneuverList.get(3).getTurn().value());
//            System.out.println("dist to 4" + maneuverList.get(4).getDistanceToNextManeuver());
//            System.out.println("turn to 4" + maneuverList.get(4).getTurn().value());
//            System.out.println("dist to 5" + maneuverList.get(5).getDistanceToNextManeuver());
//            System.out.println("turn to 5" + maneuverList.get(5).getTurn().value());
//            System.out.println("dist to 6" + maneuverList.get(6).getDistanceToNextManeuver());
//            System.out.println("turn to 6" + maneuverList.get(6).getTurn().value());
//            System.out.println("dist to 7" + maneuverList.get(7).getDistanceToNextManeuver());
//            System.out.println("turn to 7" + maneuverList.get(7).getTurn().value());
//        }
    }

    private NavigationManager.NewInstructionEventListener instructListener
            = new NavigationManager.NewInstructionEventListener() {
        @Override
        public void onNewInstructionEvent() {
            // Interpret and present the Maneuver object as it contains
            // turn by turn navigation instructions for the user.
            navigationManager.getNextManeuver();
        }
    };
    private NavigationManager.PositionListener navPositionListener
            = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(GeoPosition loc) {
            // the position we get in this callback can be used
            // to reposition the map and change orientation.
            loc.getCoordinate();
            loc.getHeading();
            loc.getSpeed();
            // also remaining time and distance can be
            // fetched from navigation manager
            navigationManager.getTimeToArrival(true,
                    Route.TrafficPenaltyMode.DISABLED);
            navigationManager.getDestinationDistance();
        }
    };
}
