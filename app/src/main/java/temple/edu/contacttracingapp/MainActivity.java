package temple.edu.contacttracingapp;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity {

    FragmentManager fm;
    DashboardFragment dashboardFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!hasGPSPermission()){
            requestGPSPermission();
        }
        fm = getSupportFragmentManager();

        dashboardFragment = DashboardFragment.newInstance();

        fm.beginTransaction()
                .replace(R.id._mainFragmentFrame, dashboardFragment, "df")
                .addToBackStack(null)
                .commit();

        Intent serviceIntent = new Intent(this, LocatorService.class);

       if (!isRunning(LocatorService.class)) {
            startService(serviceIntent);
           Log.i("running", "onCreate: " + "is running");
        }


    }

    private boolean isRunning(Class<?> serviceClass){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasGPSPermission(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestGPSPermission(){
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1111);
    }


}