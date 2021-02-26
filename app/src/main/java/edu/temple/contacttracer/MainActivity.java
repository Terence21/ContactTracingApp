package edu.temple.contacttracer;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.temple.contacttracer.database.AppDatabase;
import edu.temple.contacttracer.database.ContactUUIDDao;
import edu.temple.contacttracer.database.ContactUUIDModel;

import java.util.*;


// need to check that the primary key doesn't exist already before trying to add it to the table


public class MainActivity extends AppCompatActivity implements DashboardFragment.ActivateServiceInterface {

    FragmentManager fm;
    DashboardFragment dashboardFragment;

    ArrayList<ContactUUIDModel> contactUUIDModelArrayList;

    private static final int DB_SIZE_LIMIT = 14;

    private String payload;

    /**
     * Check if application has GPS permissions, if not request them
     * add dashboardFragment to activity
     * updateUUID and log the current database
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = getSystemService(LocationManager.class);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocation = location;
            }
        };

        if (!hasGPSPermission()) {
            requestGPSPermission();
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 1, locationListener);
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 1, locationListener);

        fm = getSupportFragmentManager();

        dashboardFragment = DashboardFragment.newInstance();

        fm.beginTransaction()
                .replace(R.id._mainFragmentFrame, dashboardFragment, "df")
                .addToBackStack(null)
                .commit();

        updateUUID();
        logDatabase();

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("FMS"));

    }

    /**
     * Using Android Room to access SQLITE
     * compare the most recent date to the current date... if the same day do not add UUID
     * check that the DB does not contain 14 entries, if so.. treat as a queue and remove the first in
     */
    public void updateUUID() {
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();
        final ContactUUIDDao contactUUIDDao = db.contactUUIDDao();


        Thread thread = new Thread() {


            @Override
            public void run() {

                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                ContactUUIDModel previousUUID = contactUUIDDao.getContactUUIDModel(0);
                Log.i("calendar", "Today: " + month + "/" + day + "/" + year);
                if (previousUUID != null) {

                    // only update if the day has changed from it's last launch
                    if (previousUUID.month == month && previousUUID.year == year && previousUUID.day == day) {
                        Log.i("Database", "DatabaseOperation: " + "insertion incomplete");
                    } else {
                        int size = contactUUIDDao.getSize();
                        if (size == DB_SIZE_LIMIT) {
                            // delete from the end of the table... where index 13 is the last
                            contactUUIDDao.delete(DB_SIZE_LIMIT - 1);
                        }
                        contactUUIDDao.incrementIndex();
                        String uuid = UUID.randomUUID().toString();
                        final ContactUUIDModel contactUUIDModel = new ContactUUIDModel(0, uuid, year, month, day);

                        contactUUIDDao.insert(contactUUIDModel);
                        Log.i("Database", "DatabaseOperation: " + "insertion complete");
                    }
                } else {
                    String uuid = UUID.randomUUID().toString();
                    final ContactUUIDModel contactUUIDModel = new ContactUUIDModel(0, uuid, year, month, day);
                    contactUUIDDao.insert(contactUUIDModel);
                    Log.i("Database", "DatabaseOperation: " + "first insertion complete");
                }
            }
        };

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        db.close();

    }

    public static List<ContactUUIDModel> getContactModelList(Context context) {
        AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, "uuid-database").build();
        final ContactUUIDDao contactUUIDDao = db.contactUUIDDao();
        final List<ContactUUIDModel>[] contactUUIDModelList = new List[]{new ArrayList<>()};
        Thread thread = new Thread() {

            @Override
            public void run() {
                super.run();
                contactUUIDModelList[0] = contactUUIDDao.getAll();
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return contactUUIDModelList[0];
    }

    /**
     * Reset Database back to size 0 for testing
     */
    public void resetDatabase() {
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();
        final ContactUUIDDao contactUUIDDao = db.contactUUIDDao();

        Thread thread = new Thread() {


            @Override
            public void run() {
                contactUUIDModelArrayList = (ArrayList<ContactUUIDModel>) contactUUIDDao.getAll();
                if (contactUUIDModelArrayList != null) {
                    if (contactUUIDModelArrayList.size() > 0) {
                        for (ContactUUIDModel contactUUIDModel : contactUUIDModelArrayList) {
                            contactUUIDDao.delete(contactUUIDModel.index);
                        }
                    }
                }
            }

        };

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        db.close();
    }

    /**
     * Log all entries of database
     */
    public void logDatabase() {

        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();
        final ContactUUIDDao contactUUIDDao = db.contactUUIDDao();

        Thread thread = new Thread() {
            @Override
            public void run() {
                String database = "";
                ArrayList<ContactUUIDModel> contactUUIDModelArrayList = (ArrayList<ContactUUIDModel>) contactUUIDDao.getAll();

                for (ContactUUIDModel contactUUIDModel : contactUUIDModelArrayList) {
                    database += contactUUIDModel.toString() + "\n";
                }
                Log.i("logdb", "logDatabase: \n" + database);
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        db.close();

    }

    /**
     * @param serviceClass service to be checked
     * @return true if service is running, false if service is not running
     */
    private boolean isRunning(Class<?> serviceClass) {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return if service has or does not have GPS permission
     */
    private boolean hasGPSPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request FINE location permission
     */
    private void requestGPSPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1111);
    }

    /**
     * Callback method for DashboardFragment start button, stop locator service
     */
    @Override
    public void startLocatorService() {
        Intent serviceIntent = new Intent(this, LocatorService.class);
        startService(serviceIntent);
        Log.i("LocatorService", "Starting LocatorService");
    }

    /**
     * Callback method for DashboardFragment stop button, end locator service
     */
    @Override
    public void stopLocatorService() {
        Intent serviceIntent = new Intent(this, LocatorService.class);
        stopService(serviceIntent);
        Log.i("LocatorService", "Stopping LocatorService ");
    }


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            payload = intent.getStringExtra("payload");
            boolean isFilteredPayload = isGrater6Feet_differentUUID(payload);
            Log.i("filter", "onReceive: " + isFilteredPayload);
        }
    };

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    LocationManager locationManager;
    LocationListener locationListener;
    Location currentLocation;

    public void storePayload(PayloadModel payloadModel){
        
    }

    public boolean isGrater6Feet_differentUUID(String payload) {

        final PayloadModel payloadModel = generatePayloadModel(payload);

        Location location = new Location("");
        double latitude = Double.parseDouble(payloadModel.getLatitude());
        double longitude = Double.parseDouble(payloadModel.getLongitude());
        Log.i("locationCheck", "latitude: " + currentLocation.getLatitude() + "\nlongitude: " + currentLocation.getLongitude());
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        // check if database contains uuid
        final boolean[] containsUUID = {true};
        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();
                List<ContactUUIDModel> contactUUIDModelList = MainActivity.getContactModelList(getApplicationContext());
                assert contactUUIDModelList != null;
                for (ContactUUIDModel model : contactUUIDModelList) {
                    if (model.uuid.equals(payloadModel.getUuid())) {
                        break;
                    }
                    containsUUID[0] = false;
                }
            }
        };

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        // check distance between current location and payload location
        float[] results = new float[1];
        Log.i("Filter", "containsUUID: " + Arrays.toString(containsUUID) + " distance: " + results[0]);
        Location.distanceBetween(latitude, longitude, currentLocation.getLatitude(), currentLocation.getLongitude(), results);

        return results[0] >= 1.8288 && containsUUID[0] == false;
    }

    private PayloadModel generatePayloadModel(String payload){

        try {
            JsonObject payloadObject = new JsonParser().parse(payload).getAsJsonObject();
            String uuid = payloadObject.get("uuid").getAsString();
            String latitude = payloadObject.get("latitude").getAsString();
            String longitude = payloadObject.get("longitude").getAsString();
            String sedentary_begin = payloadObject.get("sedentary_begin").getAsString();
            String sedentary_end = payloadObject.get("sedentary_end").getAsString();
            PayloadModel model = new PayloadModel(uuid, latitude, longitude, sedentary_begin, sedentary_end);
            Log.i("PAYLOADMODEL", "generatePayloadModel: " + model.toString());
            return model;
        } catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}

