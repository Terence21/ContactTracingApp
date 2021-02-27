package edu.temple.contacttracer;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;
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
import okhttp3.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

// https://developer.android.com/guide/components/services
public class LocatorService extends Service {

    private static final String ENDPOINT = "https://kamorris.com/lab/ct_tracking.php";
    LocationManager locationManager;
    LocationListener locationListener;

    String latitude;
    String longitude;
    String begin;
    String end;

    String payload;

    Location prevLocation;
    boolean isCountdown;



    /**
     *
     * TODO:
     *      1. change time to milliseconds
     *      2. filter own uuid and check for 6 foot distance
     *      3. store locally
     */

    AppDatabase db;
    ContactUUIDDao contactUUIDDao;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        isCountdown = false;
        locationManager = getSystemService(LocationManager.class);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("FMS"));
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();
        final ContactUUIDDao contactUUIDDao = db.contactUUIDDao();
        final CountDownTimer countDownTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long l) {
                isCountdown = true;
            }

            /**
             * create a Debug warning if the timer reaches 60 seconds...
             */
            @Override
            public void onFinish() {

                Log.d("stationaryTimer", "YOU HAVE STAYED IN A NEW LOCATION FOR MORE THAN 60 SECONDS");
                isCountdown = false;
                end = String.valueOf(Calendar.getInstance().getTimeInMillis());

                String uuid = UUID.randomUUID().toString();
                final ContactUUIDModel[] contactUUIDModel = {new ContactUUIDModel(uuid, Calendar.getInstance().getTimeInMillis(), true)};
                Thread thread = new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        if (contactUUIDDao.shouldGeneratedID(Calendar.getInstance().getTimeInMillis()) == 0){
                            Log.i("db size", "run: smaller than 1");
                            contactUUIDDao.insert(contactUUIDModel[0]);
                        }

                        ContactUUIDModel model = contactUUIDDao.getSameDayUUID(Calendar.getInstance().getTimeInMillis());
                        Log.i("difference", "run: " + model.sedentary_end + " - " + Calendar.getInstance().getTimeInMillis());
                        contactUUIDModel[0] = new ContactUUIDModel(model.uuid, Float.parseFloat(latitude), Float.parseFloat(longitude), Long.parseLong(begin), Long.parseLong(end), true);
                        contactUUIDDao.insert(contactUUIDModel[0]);
                        sendSedentaryEvent(contactUUIDModel[0].uuid, latitude, longitude, begin, end);
                    }
                };
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

                if (prevLocation == null){
                    prevLocation = location;
                }

                if (location.distanceTo(prevLocation) >= 10.0) {
                    showNotification();
                    // restart the countDownTimer
                    if (isCountdown){
                        countDownTimer.cancel();
                        isCountdown = false;
                    }
                    latitude = String.valueOf(location.getLatitude());
                    longitude = String.valueOf(location.getLongitude());
                    Log.i("latitude", "onLocationChanged: " + latitude);
                    Log.i("longitude", "onLocationChanged: " + longitude);

                    countDownTimer.start();
                    begin = String.valueOf(Calendar.getInstance().getTimeInMillis());
                    prevLocation = location;
                }
            }
        };



        FirebaseMessaging.getInstance().subscribeToTopic("TRACKING").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                String msg = task.isSuccessful()? "success": "failed";
                Log.d("subscribeMessage", msg);
            }
        });




    }

    // https://stackoverflow.com/questions/51587863/bad-notification-for-start-foreground-invalid-channel-for-service-notification
    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 1, locationListener);
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 1, locationListener);

        return super.onStartCommand(intent, flags, startId);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
    }

    public void showNotification(){

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        String channelID = "default";
        String channelName = "Foreground Service Channel";
        NotificationChannel notificationChannel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_NONE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(notificationChannel);


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Location change")
                .setContentText("you have moved 10 meters from your last location")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        startForeground(2, notification);
    }


    public String uuid;
    private String lat;
    private String longt;
    private String sedentary_begin;
    private String sedentary_end;

    public String sendSedentaryEvent(String uid, final String latitude, final String longitude, String sed_begin, final String sed_end){
        uuid = uid;
        this.lat = latitude;
        this.longt = longitude;
        this.sedentary_begin = sed_begin;
        this.sedentary_end = sed_end;
        final String[] output = {""};
        final String body = "{ 'uuid': '" + uuid + "'" +
                ", 'latitude': '" + latitude + "'" +
                ", 'longitude': '" + longitude + "'" +
                ", 'sedentary_begin': '" + sedentary_begin + "'" +
                ", 'sedentary_end': " + sedentary_end + "' }";
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();

                try {

                    RequestBody formBody = new FormBody.Builder()
                            .add("uuid", uuid)
                            .add("latitude", lat)
                            .add("longitude", longt)
                            .add("sedentary_begin", sedentary_begin)
                            .add("sedentary_end", sedentary_end)
                            .build();

                    Request request = new Request.Builder()
                            .url(ENDPOINT)
                            .post(formBody)
                            .build();

                    OkHttpClient httpClient = new OkHttpClient();
                    try(Response response = httpClient.newCall(request).execute()){

                        output[0] = response.body().string();
                        Log.i("RESPONSE", "response: " + output[0]);
                    } catch (Exception e){
                        e.printStackTrace();
                        Log.i("CONNECTION", "SendSedentaryEvent: CONNECTION NOT ESTABLISHED");
                        output[0] = null;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    output[0] = null;
                }
            }
        };

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return output[0];

    }


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            payload = intent.getStringExtra("payload");
            Log.i("receive payload", "onReceive: " + payload);
            PayloadModel payloadModel = generatePayloadModel(payload);
            if (isGrater6Feet_differentUUID(payloadModel)){
                storePayload(payloadModel);
                Log.i("PAYLOAD", "onReceive " + "received new filtered payload");
                logDatabase();
            } else{
                Log.i("PAYLOAD", "onReceive " + "cannot add payload, either > 6 feet or same uuid");
            }

        }
    };

    public void logDatabase() {

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


    }


    public void storePayload(final PayloadModel payloadModel){
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                //try {
                db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();
                contactUUIDDao = db.contactUUIDDao();
                ContactUUIDModel contactUUIDModel = new ContactUUIDModel(payloadModel.getUuid(), Float.parseFloat(payloadModel.getLatitude()), Float.parseFloat(payloadModel.getLongitude()), Long.parseLong(payloadModel.getSedentary_begin()), Long.parseLong(payloadModel.getGetSedentary_end()), false);
                contactUUIDDao.insert(contactUUIDModel);
                // } catch (Exception ignored){}
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    public boolean isGrater6Feet_differentUUID(final PayloadModel payloadModel) {

        if (prevLocation == null){
            prevLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (prevLocation != null) {
            Location location = new Location("");
            double latitude = Double.parseDouble(payloadModel.getLatitude());
            double longitude = Double.parseDouble(payloadModel.getLongitude());
            Log.i("locationCheck", "latitude: " + prevLocation.getLatitude() + "\nlongitude: " + prevLocation.getLongitude());
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
            Location.distanceBetween(latitude, longitude, prevLocation.getLatitude(), prevLocation.getLongitude(), results);

            return results[0] <= 1.8288 && containsUUID[0] == false;
        }
        return false;
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
