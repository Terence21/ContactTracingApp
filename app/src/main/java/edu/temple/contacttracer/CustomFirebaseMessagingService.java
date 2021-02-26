package edu.temple.contacttracer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.temple.contacttracer.database.ContactUUIDModel;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class CustomFirebaseMessagingService extends FirebaseMessagingService {

    String payload;
    boolean isFiltered;

    LocationManager locationManager;
    LocationListener locationListener;
    Location currentLocation;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("create", "onCreate:  created listener");

        locationManager = getSystemService(LocationManager.class);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocation = location;
            }
        };

        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        currentLocation = locationManager.getLastKnownLocation(provider);

        // location manager is throwing error because it is receiving null

    }

    public CustomFirebaseMessagingService(){
          }

    /**
     *
     * @param PayloadModel
     * @return true if the distance is greater than 6 feet and does the device does not contain that uuid
     */
    public boolean isGrater6Feet_differentUUID(final PayloadModel PayloadModel){

        Location location = new Location("");

       double latitude = Double.parseDouble(PayloadModel.latitude);
       double longitude = Double.parseDouble(PayloadModel.longitude);
       Log.i("locationCheck", "latitude: " + currentLocation.getLatitude() + "\nlongitude: " + currentLocation.getLongitude());
       location.setLatitude(latitude);
       location.setLongitude(longitude);

       final boolean[] containsUUID = {true};
       Thread thread = new Thread(){
           @Override
           public void run() {
               super.run();
               List<ContactUUIDModel> contactUUIDModelList = MainActivity.getContactModelList(getApplicationContext());
               assert contactUUIDModelList != null;
               for (ContactUUIDModel model: contactUUIDModelList){
                   if (model.uuid.equals(PayloadModel.uuid)){
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


        float[] results = new float[1];
        Log.i("Filter", "containsUUID: " + Arrays.toString(containsUUID) + " distance: " + results[0]);
        Location.distanceBetween(latitude,longitude,currentLocation.getLatitude(), currentLocation.getLongitude(), results);

        return results[0] >= 1.8288 && containsUUID[0] == false;
    }



    @SuppressLint("MissingPermission")
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        payload = remoteMessage.getData().get("payload");
        Log.i("PAYLOAD", "onMessageReceived: " + payload);

        PayloadModel payloadModel = generatePayloadModel(payload);
        isFiltered = isGrater6Feet_differentUUID(payloadModel);

        if (isFiltered){
            Log.i("FILTERED", "onMessageReceived: FILTERED");
        } else{
            Log.i("FILTERED", "onMessageReceived: NOT FILTERED");
        }

    }

    @Override
    public void onNewToken(@NonNull @NotNull String s) {
        super.onNewToken(s);

    }



    public PayloadModel generatePayloadModel(String payload){

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



    private class PayloadModel{
        private String uuid;
        private String latitude;
        private String longitude;
        private String sedentary_begin;
        private String getSedentary_end;

        public PayloadModel(String uuid, String latitude, String longitude, String sedentary_begin, String getSedentary_end) {
            this.uuid = uuid;
            this.latitude = latitude;
            this.longitude = longitude;
            this.sedentary_begin = sedentary_begin;
            this.getSedentary_end = getSedentary_end;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getLatitude() {
            return latitude;
        }

        public void setLatitude(String latitude) {
            this.latitude = latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        public void setLongitude(String longitude) {
            this.longitude = longitude;
        }

        public String getSedentary_begin() {
            return sedentary_begin;
        }

        public void setSedentary_begin(String sedentary_begin) {
            this.sedentary_begin = sedentary_begin;
        }

        public String getGetSedentary_end() {
            return getSedentary_end;
        }

        public void setGetSedentary_end(String getSedentary_end) {
            this.getSedentary_end = getSedentary_end;
        }

        @NonNull
        @NotNull
        @Override
        public String toString() {
            return
                    "uuid: " + uuid + "\n" +
                            "lat: " + latitude + "\n" +
                            "long: " + longitude + "\n" +
                            "begin: " + sedentary_begin + "\n" +
                            "end: " + getSedentary_end;
        }
    }



    // method for filtering

}
