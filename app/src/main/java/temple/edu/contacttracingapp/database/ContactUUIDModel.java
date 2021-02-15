package temple.edu.contacttracingapp.database;

import android.provider.BaseColumns;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ContactUUIDModel{

    public ContactUUIDModel(int index, String uuid, int year, int month, int day){
        this.index = index;
        this.year = year;
        this.month = month;
        this.day = day;
    }
    @PrimaryKey
    public int index;

    @ColumnInfo(name= "uuid")
    public String uuid;

    @ColumnInfo(name="year")
    public int year;

    @ColumnInfo(name="month")
    public int month;

    @ColumnInfo(name="day")
    public int day;

}
