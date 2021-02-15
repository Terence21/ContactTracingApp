package temple.edu.contacttracingapp.database;

import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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
