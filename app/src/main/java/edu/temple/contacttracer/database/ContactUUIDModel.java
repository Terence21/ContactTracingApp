package edu.temple.contacttracer.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Model class for UUID, each method is a respective column with the uuid being the primary key
 */
@Entity
public class ContactUUIDModel{

    public ContactUUIDModel(@NonNull String uuid, float latitude, float longitude, float sedentary_begin, float sedentary_end, boolean isLocal) {
        this.uuid = uuid;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sedentary_begin = sedentary_begin;
        this.sedentary_end = sedentary_end;
        this.isLocal = isLocal;

    }

    @Ignore
    public ContactUUIDModel(@NonNull String uuid, float sedentary_end, boolean isLocal){
        this.uuid = uuid;
        this.isLocal = isLocal;
        this.sedentary_end = sedentary_end;
    }


    @PrimaryKey(autoGenerate = true)
    public int id = 0;

    @NonNull
    public String uuid;

    @ColumnInfo(name = "latitude")
    public float latitude;

    @ColumnInfo(name = "longitude")
    public float longitude;


    @ColumnInfo(name = "sedentary_begin")
    public float sedentary_begin;

    @ColumnInfo(name = "sedentary_end")
    public float sedentary_end;

    @ColumnInfo(name = "isLocal")
    public boolean isLocal;

    @NonNull
    @Override
    public String toString() {
        return uuid + ": \tlat: " + latitude + "\tlong: " + longitude + "\tsed_begin: " + sedentary_begin + "\tsed_end: " + sedentary_end;
    }
}
