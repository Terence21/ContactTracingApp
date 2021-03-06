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
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import edu.temple.contacttracer.database.AppDatabase;
import edu.temple.contacttracer.database.ContactUUIDDao;
import edu.temple.contacttracer.database.ContactUUIDModel;

import java.util.*;


// need to check that the primary key doesn't exist already before trying to add it to the table


public class MainActivity extends AppCompatActivity implements DashboardFragment.ActivateServiceInterface {

    FragmentManager fm;
    DashboardFragment dashboardFragment;

    private String payload;

    AppDatabase db;
    ContactUUIDDao contactUUIDDao;

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

        if (!hasGPSPermission()) {
            requestGPSPermission();
        }



        fm = getSupportFragmentManager();

        dashboardFragment = DashboardFragment.newInstance();

        fm.beginTransaction()
                .replace(R.id._mainFragmentFrame, dashboardFragment, "df")
                .addToBackStack(null)
                .commit();

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();
        contactUUIDDao = db.contactUUIDDao();
        updateUUID();
        logDatabase();



    }

    /**
     * Using Android Room to access SQLITE
     * When called, add new ContactUUIDModel to the database
     */
    public void updateUUID() {
        removeOverdueUUIDs();

        Thread thread = new Thread() {


            @Override
            public void run() {
                if (contactUUIDDao.shouldGeneratedID(Calendar.getInstance().getTimeInMillis()) == 0){
                    String uuid = UUID.randomUUID().toString();
                    ContactUUIDModel contactUUIDModel = new ContactUUIDModel(uuid, Calendar.getInstance().getTimeInMillis(), true);
                    contactUUIDDao.insert(contactUUIDModel);
                }
            }
        };

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    /**
     * generate an arraylist of all the entities in the database
     * @param context calling application
     * @return arraylist representation of database entries
     */
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
     * Delete all uuid over 14 days from current time... using DAO method
     */
    public void removeOverdueUUIDs() {

        Thread thread = new Thread() {
            @Override
            public void run() {
                contactUUIDDao.deleteOverdue(Calendar.getInstance().getTimeInMillis());
            }

        };

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Log all entries of database
     */
    public void logDatabase() {

        Thread thread = new Thread() {
            @Override
            public void run() {
                StringBuilder database = new StringBuilder();
                ArrayList<ContactUUIDModel> contactUUIDModelArrayList = (ArrayList<ContactUUIDModel>) contactUUIDDao.getAll();

                for (ContactUUIDModel contactUUIDModel : contactUUIDModelArrayList) {
                    database.append(contactUUIDModel.toString()).append("\n");
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


    }

    /**
     * destroy instance of database on application
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // db.close();
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

    private String selectedDate;
    @Override
    public void showCalendar() {
        MaterialDatePicker.Builder<Long> materialDateBuilder = MaterialDatePicker.Builder.datePicker();
        materialDateBuilder.setTitleText("SELECT A DATE");

        final MaterialDatePicker<Long> materialDatePicker = materialDateBuilder.build();
        materialDatePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");

        materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
            @Override
            public void onPositiveButtonClick(Object selection) {
                selectedDate = materialDatePicker.getHeaderText();
                Log.i("DATE", "onPositiveButtonClick: " + selectedDate);
            }
        });
    }
}

