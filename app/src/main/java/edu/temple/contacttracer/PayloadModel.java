package edu.temple.contacttracer;

import android.location.LocationListener;
import android.location.LocationManager;
import androidx.annotation.NonNull;
import org.jetbrains.annotations.NotNull;

public class PayloadModel {
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

