package edu.temple.contacttracer.database;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for accessing ContactUUIIDModel entities from Database
 */
@Dao
public interface ContactUUIDDao {

    // return rows as contactUUIDModel list
    @Query("SELECT * FROM ContactUUIDModel")
    List<ContactUUIDModel> getAll();

    // 1 day = 86400000 milliseconds
    // if this equals 0 then should generate a new UUID, because one doesn't locally for the specified difference in time (i.e. 1 day)
    @Query("SELECT COUNT(`uuid`) FROM CONTACTUUIDMODEL WHERE isLocal = 1 AND Abs(:time - sedentary_end) <= 86400000")
    int shouldGeneratedID(long time);

    // return the row for the existing uuid for the given day
    @Query("SELECT * FROM CONTACTUUIDMODEL WHERE isLocal = 1 AND Abs(:time - sedentary_end) <= 86400000")
    ContactUUIDModel getSameDayUUID(long time);

    // return the contact model for the given uuid
    // not used but need to change to LIST
    @Query("SELECT * FROM ContactUUIDModel WHERE `uuid` = :uuid")
    ContactUUIDModel getContactUUIDModel(String uuid);

    // insert ContactUUIDModel into the database
    @Insert
    void insert(ContactUUIDModel contactUUIDModel);

    // get the size of the database
    @Query("SELECT COUNT(`uuid`) FROM CONTACTUUIDMODEL")
    int getSize();


    // 14 days = 1209600000 milliseconds
    // for all entries where the difference between the sedentary time and the current time is more than 14 days, delete it
    @Query("DELETE FROM contactuuidmodel WHERE Abs(:time - sedentary_end) >= 1209600000")
    void deleteOverdue(long time);

}
