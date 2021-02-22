package edu.temple.contacttracer;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.room.Room;
import edu.temple.contacttracer.database.AppDatabase;
import edu.temple.contacttracer.database.ContactUUIDDao;
import edu.temple.contacttracer.database.ContactUUIDModel;

import java.util.*;


// need to check that the primary key doesn't exist already before trying to add it to the table

/**
 * 1. get size
 * 2. if last value is equal to the same date... then skip
 * 3. if not, create an instance and add it to the table
 */

public class MainActivity extends AppCompatActivity implements DashboardFragment.ActivateServiceInterface {

    FragmentManager fm;
    DashboardFragment dashboardFragment;

    ArrayList<ContactUUIDModel> contactUUIDModelArrayList;
    
    private static final int DB_SIZE_LIMIT = 14;

    /**
     * Check if application has GPS permissions, if not request them
     * add dashboardFragment to activity
     * updateUUID and log the current database
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!hasGPSPermission()) {
            requestGPSPermission();
        }
        fm = getSupportFragmentManager();

        dashboardFragment = DashboardFragment.newInstance();

        fm.beginTransaction()
                .replace(R.id._mainFragmentFrame, dashboardFragment, "df")
                .addToBackStack(null)
                .commit();

        updateUUID();
        logDatabase();

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

    /**
     * Reset Database back to size 0 for testing
     */
    public void resetDatabase(){
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();
        final ContactUUIDDao contactUUIDDao = db.contactUUIDDao();

        Thread thread = new Thread(){


            @Override
            public void run() {
                contactUUIDModelArrayList = (ArrayList<ContactUUIDModel>) contactUUIDDao.getAll();
                if (contactUUIDModelArrayList != null){
                    if (contactUUIDModelArrayList.size() > 0){
                        for (ContactUUIDModel contactUUIDModel: contactUUIDModelArrayList){
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
    public void logDatabase(){

        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();
        final ContactUUIDDao contactUUIDDao = db.contactUUIDDao();

        Thread thread = new Thread(){
            @Override
            public void run() {
                String database = "";
                ArrayList<ContactUUIDModel> contactUUIDModelArrayList = (ArrayList<ContactUUIDModel>) contactUUIDDao.getAll();

                for (ContactUUIDModel contactUUIDModel : contactUUIDModelArrayList){
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
     *
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
     *
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
}