package edu.temple.contacttracer.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Model class for UUID, each method is a respective column with the uuid being the primary key
 */
@Entity
public class ContactUUIDModel{

    public ContactUUIDModel(int index, String uuid, int year, int month, int day){
        this.index = index;
        this.uuid = uuid;
        this.year = year;
        this.month = month;
        this.day = day;
    }

    @ColumnInfo(name = "index")
    public int index;

    @NonNull
    @PrimaryKey
    public String uuid;

    @ColumnInfo(name="year")
    public int year;

    @ColumnInfo(name="month")
    public int month;

    @ColumnInfo(name="day")
    public int day;

    @NonNull
    @Override
    public String toString() {
        return index + ": " + uuid + " " + month + "/" + day + "/" + year;
    }
}