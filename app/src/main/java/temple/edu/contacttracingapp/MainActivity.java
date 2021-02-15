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
import temple.edu.contacttracingapp.contactTracing.ContactTracer;
import temple.edu.contacttracingapp.database.AppDatabase;
import temple.edu.contacttracingapp.database.ContactUUIDDao;
import temple.edu.contacttracingapp.database.ContactUUIDModel;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


// need to check that the primary key doesn't exist already before trying to add it to the table
public class MainActivity extends AppCompatActivity implements DashboardFragment.ActivateServiceInterface {

    FragmentManager fm;
    DashboardFragment dashboardFragment;
    List<ContactUUIDModel> contactUUIDModelList;

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

        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();

        final ContactUUIDDao contactUUIDDao = db.contactUUIDDao();
        final ContactUUIDModel contactUUIDModel = new ContactUUIDModel(1, "fec", 2021, 2, 21);

        Thread thread = new Thread(){
            @Override
            public void run() {
                contactUUIDDao.insert(contactUUIDModel);
                contactUUIDModelList  = contactUUIDDao.getAll();
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i("add", "contactUUID: " + contactUUIDModelList.get(0).day);

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