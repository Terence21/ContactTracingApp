package temple.edu.contacttracingapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

// https://developer.android.com/guide/components/services
public class LocatorService extends Service {

    LocationManager locationManager;
    LocationListener locationListener;

    String latitude;
    String longitude;

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


}
