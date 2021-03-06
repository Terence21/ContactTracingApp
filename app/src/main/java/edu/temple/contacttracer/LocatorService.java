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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import edu.temple.contacttracer.database.AppDatabase;
import edu.temple.contacttracer.database.ContactUUIDDao;
import edu.temple.contacttracer.database.ContactUUIDModel;
import edu.temple.contacttracer.models.PayloadModel;
import edu.temple.contacttracer.models.TracingModel;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
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

    AppDatabase db;
    ContactUUIDDao contactUUIDDao;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    boolean canRun;
    @Override
    public void onCreate() {
        super.onCreate();

        isCountdown = false;
        locationManager = getSystemService(LocationManager.class);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("FMS"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("CONTACT"));
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();
        final ContactUUIDDao contactUUIDDao = db.contactUUIDDao();
        final CountDownTimer countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long l) {
                isCountdown = true;
            }

            /**
             * on sedentary_end create a UUID model and store the instance as local to the database
             * broadcasting the model fields to other registered devices
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

                        // there should always be an existing uuid for day before sedentary event, if not.. make one
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


        // subscribe to firebase messaging TRACKING topic
        FirebaseMessaging.getInstance().subscribeToTopic("TRACKING").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                String msg = task.isSuccessful()? "success": "failed";
                Log.d("TRACKING subscribeMessage", msg);
            }
        });

        // subscribe to firebase messaging TRACING topic
        FirebaseMessaging.getInstance().subscribeToTopic("TRACING").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<Void> task) {
                String msg = task.isSuccessful()? "success": "failed";
                Log.d("TRACING subscribeMessage", msg);
            }
        });


    }

    // request location updates
    // https://stackoverflow.com/questions/51587863/bad-notification-for-start-foreground-invalid-channel-for-service-notification
    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 1, locationListener);
            locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 1, locationListener);
            canRun = true;

        } catch (Exception e){
            Toast toast = Toast.makeText(getApplicationContext(), "ENABLE NETWORK", Toast.LENGTH_SHORT);
            toast.show();

        }

        return super.onStartCommand(intent, flags, startId);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
    }

    /**
     * create notification channel and start foreground service
     */
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

    /**
     *
     * @param uid
     * @param latitude
     * @param longitude
     * @param sed_begin
     * @param sed_end
     * @return response from server
     */
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

    /**
     * broadcast receiver to receive payload from Firebase messaging service
     * receive  payload string, generate Payload model from string, then filter payload for different uuid and less than 6 feet distance
     * if passes filter then store payload model in database
     */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("FMS")) {

                String type = intent.getStringExtra("type");
                switch (type) {
                    case "tracing":
                        payload = intent.getStringExtra("tracing");

                        TracingModel tracingModel = generateTracingModel(payload);
                        Log.i("receive tracking payload", "onReceive: " + payload);

                        boolean isLocalReport = containsLocalTracing(tracingModel);
                        if (!isLocalReport){
                            Log.i("TRACING PAYLOAD", "onReceive: received new filtered tracing payload");
                        }else{
                            Log.i("TRACING PAYLOAD", "onReceive " + "cannot add payload same uuid");
                        }
                        break;
                    case "tracking":
                        payload = intent.getStringExtra("tracking");
                        Log.i("receive tracing payload", "onReceive: " + payload);
                        PayloadModel payloadModel = generatePayloadModel(payload);
                        if (isGrater6Feet_differentUUID(payloadModel)) {
                            storePayload(payloadModel);
                            Log.i("TRACKING PAYLOAD", "onReceive " + "received new filtered tracking payload");
                            logDatabase();
                        } else {
                            Log.i("TRACKING PAYLOAD", "onReceive " + "cannot add payload, either > 6 feet or same uuid");
                        }
                        break;
                }

            } else if (intent.getAction().equals("CONTACT")) {
                Log.i("contact", "onReceive: sending");
                long date = 0;
                date = intent.getLongExtra("date", date);
                ArrayList<String> uuids = intent.getStringArrayListExtra("uuids");
                sendContactEvent(date, uuids);
            }


        }
    };

    // log database
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

    // store payload model in database
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

    /**
     *
     * @param payloadModel
     * @return true if uuid is not the same as device, and distance is less than 6 feet
     * return false if payload not proper or if aforementioned condition is false
     */
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

    // convert payload string to payload model
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

    private TracingModel generateTracingModel(String payload){
        try{
            JsonObject payloadObject = new JsonParser().parse(payload).getAsJsonObject();
            long date = payloadObject.get("date").getAsLong();
            JsonElement uuidElement = payloadObject.get("uuids");
            Type arraylistType = new TypeToken<ArrayList<String>>() {}.getType();
            ArrayList<String> uuids = new Gson().fromJson(uuidElement, arraylistType);
            return new TracingModel(date, uuids);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public String sendContactEvent(long d, final ArrayList<String> uuids){
        String uuidJsonString = buildUUIDString(uuids);
        final String date = String.valueOf(d);
        final String[] output = {""};

        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();

                RequestBody formBody = new FormBody.Builder()
                        .add("date", date)
                        .add("uuids", Arrays.toString(uuids.toArray()))
                        .build();

                Request request = new Request.Builder()
                        .url("https://kamorris.com/lab/ct_tracing.php")
                        .post(formBody)
                        .build();

                OkHttpClient httpClient = new OkHttpClient();
                try (Response response = httpClient.newCall(request).execute()){
                    output[0] = response.body().string();
                    Log.i("RESPONSE", "response: " + output[0]);
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

    public String buildUUIDString(ArrayList<String> uuids){
        StringBuilder body = new StringBuilder("[ ");
        for (String uuid: uuids){
            if (!uuid.equals(uuids.get(uuids.size()-1))) {
                body.append("'").append(uuid).append("', ");
            } else{
                body.append("'").append(uuid).append("' ");
            }
        }
        body.append("]");
        return body.toString();
    }

    public boolean containsLocalTracing(final TracingModel tracingModel){
        final boolean[] doesContain = {false};
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "uuid-database").build();
                contactUUIDDao = db.contactUUIDDao();
                ArrayList<ContactUUIDModel> contactUUIDModels = (ArrayList<ContactUUIDModel>) contactUUIDDao.getAllLocal();
                for (ContactUUIDModel model : contactUUIDModels){
                    if (tracingModel.getUuids().contains(model.uuid)){
                        doesContain[0] = true;
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
        return doesContain[0];
    }


}
