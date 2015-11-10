package com.pootiapps.runforlife;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.guidance.VoiceCatalog;
import com.here.android.mpa.guidance.VoicePackage;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.Maneuver;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.pootiapps.runforlife.main.DirectionEnum;
import com.pootiapps.runforlife.main.RunningRoute;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;



import im.delight.android.location.SimpleLocation;

public class MainActivity extends AppCompatActivity {

    private Map map = null;
    private MapFragment mapFragment = null;
    private TextView textViewResult = null;
    private MapRoute mapRoute = null;

    private double distanceToRun = 0;
    private double distanceToRunThresh = 0;
    private DirectionEnum directionEnum;

    private SimpleLocation location;
    private boolean paused;

    private NavigationManager navigationManager = null;

    private static final double METER_IN_MILE = 1609.344;
    private static final double METER_IN_KM = 1000;
    private static final double THRESHOLD = 200;
    final Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.startNavigationBtn);
        floatingActionButton.setImageResource(R.drawable.maneuver_icon_48);
        FloatingActionButton floatingActionButton1 = (FloatingActionButton) findViewById(R.id.stopNavigationBtn);
        floatingActionButton1.setImageResource(R.drawable.hand_stop_palm_512);
        floatingActionButton1.hide();

        Spinner spinner = (Spinner) findViewById(R.id.distanceSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.distance_array,R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        location = new SimpleLocation(this);

        if(!location.hasLocationEnabled()){
            SimpleLocation.openSettings(this);
        }

        //Toast.makeText(this, location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_LONG).show();

        new UrlOperation().execute("");

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

    private class UrlOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            StringBuilder builder = new StringBuilder();
            StringBuilder sb = new StringBuilder();
            sb.append("");
            String result = "";
            location = new SimpleLocation(getApplicationContext());
            try {
                URL url = new URL("http://api.breezometer.com/baqi/?lat="+location.getLatitude()+"&lon="+location.getLongitude()+"&key=40555a34e15d4c0682a9ce14c43df31e");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                    for (String line = null; (line = br.readLine()) != null;) {
                        builder.append(line).append("\n");
                    }
                    JSONTokener tokener = new JSONTokener(builder.toString());
                    //JSONArray finalResult = new JSONArray(tokener); //This is your JSON result
                    JSONObject jsonObject = new JSONObject(tokener);
                    if(jsonObject.optInt("breezometer_aqi")!=0
                            && !jsonObject.optString("dominant_pollutant_canonical_name").isEmpty()
                            && !jsonObject.optJSONObject("random_recommendations").optString("sport").isEmpty()) {
                        //System.out.println(jsonObject.optInt("breezometer_aqi"));
                        sb.append("Air Quality Index - " + jsonObject.optInt("breezometer_aqi"));
                        //System.out.println(jsonObject.optString("dominant_pollutant_canonical_name"));
                        sb.append("\nDominant polutant - " + jsonObject.optString("dominant_pollutant_canonical_name"));
                        //System.out.println(jsonObject.optJSONObject("random_recommendations").optString("sport"));
                        sb.append("\n" + jsonObject.optJSONObject("random_recommendations").optString("sport"));
                    }

                }catch (IOException e) {
                    System.out.println("IOException");
                }
                catch(JSONException je){
                    System.out.println("JSONException");
                    je.printStackTrace();
                }
                finally{
                    System.out.println("finally");
                    urlConnection.disconnect();
                }

            } catch (MalformedURLException me){
                System.out.println("MalformedURLException");
            }
            catch (IOException ie) {
                System.out.println("IOException 2");
            }
            return sb.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println(result);
            final Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.custom);
            Date dt = new Date();
            String title;

            Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTime(dt);
            int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
            if (hourOfDay > 17) {
                title = "Good Evening";
            } else if (hourOfDay > 12) {
                title = "Good Afternoon";
            } else {
                title = "Good Morning";
            }
            dialog.setTitle(title);

            if(result.equalsIgnoreCase("")){
                result = "We're sorry.\nWe couldn't find air quality data for your location.";
            }
            TextView textView = (TextView) dialog.findViewById(R.id.dialogText);
            textView.setText(result);
            Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
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
                        textViewResult.setText(
                                String.format("Route calculated with %d maneuvers. Length - %d",
                                        result.get(0).getRoute().getManeuvers().size(),result.get(0).getRoute().getLength()));
                    } else {
                        int dist = result.get(0).getRoute().getLength();
                        if(distanceToRun < 0) {
                        }
                        else {
                            if (distanceToRun < 10) {
                                distanceToRun -= 1;
                            } else if (distanceToRun < 100) {
                                distanceToRun -= 10;
                            } else {
                                distanceToRun -= 100;
                            }
                            System.out.println("Total dist - " + dist + ", reducing distance to " + distanceToRun);
                            System.out.println("Direction - " + directionEnum + ", changing direction to " + directionEnum.next());
                            getRoute(distanceToRun);
                        }
                    }
                }
                public void onProgress(int percentage) {
                    StringBuilder progress = new StringBuilder();
                    progress.append("Calculating");
                    for(int i = 0; i < percentage%4 ; i++){
                        progress.append(".");
                    }
                    textViewResult.setText(progress.toString());
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
        if(distanceToRun<=0){
            Toast.makeText(this,"Please select distance and hit calculate route.",Toast.LENGTH_LONG).show();
        }
        else {
            getRoute(distanceToRun);
        }
    }
    // Functionality for taps of the "Get Directions" button
    public void getDirections(View view) {
        try {
            EditText editText = (EditText) findViewById(R.id.distanceText);
            editText.clearFocus();
            Spinner spinner = (Spinner) findViewById(R.id.distanceSpinner);
            distanceToRun = Double.parseDouble(editText.getText().toString());
            String spinnerText = spinner.getSelectedItem().toString();
            int distanceMeasure;
            if (spinnerText.equalsIgnoreCase("miles")) {
                distanceMeasure = 1;
            } else if (spinnerText.equalsIgnoreCase("kms")) {
                distanceMeasure = 2;
            } else {
                distanceMeasure = 3;
            }
            switch (distanceMeasure) {
                case 1:
                    System.out.println("Converting " + distanceToRun + " miles into meters.");
                    distanceToRun = distanceToRun * METER_IN_MILE;
                    System.out.println(" = " + distanceToRun + " meters");
                    break;
                case 2:
                    System.out.println("Converting " + distanceToRun + " kms into meters.");
                    distanceToRun = distanceToRun * METER_IN_KM;
                    System.out.println(" = " + distanceToRun + " meters");
                    break;
                case 3:
                    break;
                default:
                    break;
            }
            distanceToRunThresh = distanceToRun + THRESHOLD;
            getRoute(distanceToRun);
        } catch (NullPointerException ne) {}
        catch (NumberFormatException nume) {}
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

        ///*ksingh
        boolean isConnected = PebbleKit.isWatchConnected(this);
        Toast.makeText(this, "Pebble " + (isConnected ? "is" : "is not") + " connected!", Toast.LENGTH_LONG).show();


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
        map = null;
        super.onDestroy();
    }

    public void stopNavigation(View view){
        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.startNavigationBtn);
        floatingActionButton.show();
        FloatingActionButton floatingActionButton1 = (FloatingActionButton) findViewById(R.id.stopNavigationBtn);
        floatingActionButton1.hide();
        navigationManager.stop();
    }

    public void startNavigation(View view){
        if(mapRoute!=null) {
            FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.startNavigationBtn);
            floatingActionButton.hide();
            FloatingActionButton floatingActionButton1 = (FloatingActionButton) findViewById(R.id.stopNavigationBtn);
            floatingActionButton1.show();
            navigationManager = NavigationManager.getInstance();
            // start listening to navigation events
            navigationManager.addNewInstructionEventListener(
                    new WeakReference<NavigationManager.NewInstructionEventListener>(instructListener));
            // start listening to position events
            navigationManager.addPositionListener(
                    new WeakReference<NavigationManager.PositionListener>(navPositionListener));
            navigationManager.setNaturalGuidanceMode(EnumSet.of(NavigationManager.NaturalGuidanceMode.JUNCTION));
            VoiceCatalog voiceCatalog = VoiceCatalog.getInstance();
            voiceCatalog.downloadCatalog(new VoiceCatalog.OnDownloadDoneListener() {
                @Override
                public void onDownloadDone(VoiceCatalog.Error error) {
                    if (error == VoiceCatalog.Error.NONE) {
                        System.out.println("Catalog successfuly downloaded");
                    }
                }
            });
            List<VoicePackage> voicePackages = VoiceCatalog.getInstance().getCatalogList();
            long id = -1;
            System.out.println(id);
            for (VoicePackage voicePackage : voicePackages) {
                if (voicePackage.getMarcCode().compareToIgnoreCase("eng") == 0) {
                    if (voicePackage.isTts()) {
                        id = voicePackage.getId();
                        break;
                    }
                }
            }
            System.out.println(id);
            if (!voiceCatalog.isLocalVoiceSkin(id)) {
                voiceCatalog.downloadVoice(id, new VoiceCatalog.OnDownloadDoneListener() {
                    @Override
                    public void onDownloadDone(VoiceCatalog.Error error) {
                        if (error == VoiceCatalog.Error.NONE) {
                            System.out.println("No error while downloading");
                        }
                    }
                });
            }
            System.out.println(id);
            NavigationManager.Error error = navigationManager.simulate(mapRoute.getRoute(), 8);
            if (id >= 0) {
                navigationManager.setVoiceSkin(voiceCatalog.getLocalVoiceSkin(id));
            }
            System.out.println(id);
        } else {
            Toast.makeText(this,"Please select distance and hit calculate route.",Toast.LENGTH_LONG).show();
        }
    }


    private NavigationManager.NewInstructionEventListener instructListener
            = new NavigationManager.NewInstructionEventListener() {
        @Override
        public void onNewInstructionEvent() {
            // Interpret and present the Maneuver object as it contains
            // turn by turn navigation instructions for the user.
            Maneuver maneuver = navigationManager.getNextManeuver();
            if(maneuver != null){
                if(maneuver.getAction() == Maneuver.Action.END){
                    System.out.println("Route Finished");
                    FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.startNavigationBtn);
                    floatingActionButton.show();
                    FloatingActionButton floatingActionButton1 = (FloatingActionButton) findViewById(R.id.stopNavigationBtn);
                    floatingActionButton1.hide();
                }
                if(maneuver.getAction() == Maneuver.Action.UTURN) {
                    System.out.println(Maneuver.Action.UTURN);
                } else {
                    System.out.println(maneuver.getTurn());
                    System.out.println(maneuver.getDistanceFromPreviousManeuver());

                    String title = "";
                    Maneuver.Turn turn = maneuver.getTurn();

                    if(turn.toString().contains("LEFT")||turn.toString().contains("left")){
                        title = "Turn Left";
                    }
                    if(turn.toString().contains("RIGHT")||turn.toString().contains("right")){
                        title = "Turn Right";
                    }
                    if(turn.toString().contains("UNDEFINED")||turn.toString().contains("undefined")){
                        title = "U-Turn";
                    }
                    if(turn.toString().contains("NO")||turn.toString().contains("no")){
                        title = "Go Straight";
                    }

                    String body;
                    body = "In " + maneuver.getDistanceFromPreviousManeuver() + " meters.";
                    // Push a notification

                    final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

                    final java.util.Map data = new HashMap();
                    data.put("title", title);
                    data.put("body", body);
                    final JSONObject jsonData = new JSONObject(data);
                    final String notificationData = new JSONArray().put(jsonData).toString();

                    i.putExtra("messageType", "PEBBLE_ALERT");
                    i.putExtra("sender", "PebbleKit Android");
                    i.putExtra("notificationData", notificationData);
                    sendBroadcast(i);

                }
            } else {
                System.out.println("null maneuver");
            }
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
