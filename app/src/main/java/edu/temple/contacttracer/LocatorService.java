package edu.temple.contacttracer;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import edu.temple.contacttracer.database.ContactUUIDModel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

// https://developer.android.com/guide/components/services
public class LocatorService extends Service {

    private static final String ENDPOINT = "https://kamorris.com/lab/ct_tracking.php";
    LocationManager locationManager;
    LocationListener locationListener;

    String latitude;
    String longitude;
    String begin;
    String end;

    Location prevLocation;
    boolean isCountdown;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {

        isCountdown = false;
        locationManager = getSystemService(LocationManager.class);

        final CountDownTimer countDownTimer = new CountDownTimer(60000, 1000) {
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
                end = Calendar.getInstance().getTime().toString();

                List <ContactUUIDModel> contactUUIDModelList = MainActivity.getContactModelList(getApplicationContext());
                String mostRecentUUID = contactUUIDModelList.get(0).uuid;
                sendSedentaryEvent(mostRecentUUID, latitude, longitude, begin, end);
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
                    begin = Calendar.getInstance().getTime().toString();
                    prevLocation = location;
                }




            }
        };

        super.onCreate();

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

    public String sendSedentaryEvent(String uuid, String latitude, String longitude, String sedentary_begin, String sedentary_end){
        try {
            URL url = new URL(ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);

            String body = "{ 'uuid': '" + uuid + "'" +
                    ", 'latitude': '" + latitude + "'" +
                    ", 'longitude': '" + longitude + "'" +
                    ", 'sedentary_begin': '" + sedentary_begin + "'" +
                    ", 'sedentary_end': " + sedentary_end + "' }";


            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK){
                Log.i("CONNECTION", "SendSedentaryEvent: CONNECTION ESTABLISHED");

                try (OutputStream os = connection.getOutputStream()){
                    byte[] input = body.getBytes("utf-8");
                    os.write(input, 0, input.length);
                } catch (Exception e){
                    Log.i("OUTPUTSTREAM", "SendSedentaryEvent: COULD NOT WRITE TO URL");
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = reader.readLine()) != null){
                    sb.append(line.trim());
                }
                reader.close();
                connection.disconnect();
                Log.i("RESPONSE", "response: " + sb.toString());
                return sb.toString();
            } else{
                Log.i("CONNECTION", "SendSedentaryEvent: CONNECTION NOT ESTABLISHED");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
