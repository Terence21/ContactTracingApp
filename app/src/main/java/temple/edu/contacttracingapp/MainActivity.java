package temple.edu.contacttracingapp;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.room.Room;
import temple.edu.contacttracingapp.database.AppDatabase;
import temple.edu.contacttracingapp.database.ContactUUIDDao;
import temple.edu.contacttracingapp.database.ContactUUIDModel;

import java.io.File;
import java.io.FileWriter;
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
    List<ContactUUIDModel> contactUUIDModelList;
    
    private static final int DB_SIZE_LIMIT = 14;

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
    
    public void updateUUID() {
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();
        final ContactUUIDDao contactUUIDDao = db.contactUUIDDao();

        Thread thread = new Thread() {


            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_WEEK);

                ContactUUIDModel previousUUID = contactUUIDDao.getContactUUIDModel(0);

                if (previousUUID != null) {

                    // only update if the day has changed
                    if (previousUUID.month == month && previousUUID.year == year && previousUUID.day == day) {
                        Log.i("Database", "DatabaseOperation: " + "insertion incomplete");
                    } else {
                        int size = contactUUIDDao.getSize();
                        if (size == DB_SIZE_LIMIT) {
                            contactUUIDDao.delete(DB_SIZE_LIMIT - 1);
                        }
                        contactUUIDDao.incrementIndex(); // delete from the end of the table... where index 13 is the last

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

    public void resetDatabase(){
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();
        final ContactUUIDDao contactUUIDDao = db.contactUUIDDao();

        Thread thread = new Thread(){


            @Override
            public void run() {
                contactUUIDDao.delete(0);
                contactUUIDDao.delete(1);
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

    private boolean isRunning(Class<?> serviceClass) {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasGPSPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestGPSPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1111);
    }


    @Override
    public void startLocatorService() {
        Intent serviceIntent = new Intent(this, LocatorService.class);
        startService(serviceIntent);
        Log.i("LocatorService", "Starting LocatorService");
    }

    @Override
    public void stopLocatorService() {
        Intent serviceIntent = new Intent(this, LocatorService.class);
        stopService(serviceIntent);
        Log.i("LocatorService", "Stopping LocatorService ");
    }
}