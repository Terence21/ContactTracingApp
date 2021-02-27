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

    @Query("SELECT * FROM ContactUUIDModel")
    List<ContactUUIDModel> getAll();

    // 1 day = 86400000 milliseconds
    @Query("SELECT COUNT(`uuid`) FROM CONTACTUUIDMODEL WHERE isLocal = 1 AND Abs(:time - sedentary_end) <= 86400000")
    int shouldGeneratedID(long time);

    // fix this function
    @Query("SELECT * FROM CONTACTUUIDMODEL WHERE isLocal = 1 AND Abs(:time - sedentary_end) <= 86400000")
    ContactUUIDModel getSameDayUUID(long time);

    @Query("SELECT * FROM ContactUUIDModel WHERE `uuid` = :uuid")
    ContactUUIDModel getContactUUIDModel(String uuid);

    @Insert
    void insert(ContactUUIDModel contactUUIDModel);


    @Query("SELECT COUNT(`uuid`) FROM CONTACTUUIDMODEL")
    int getSize();


    // 14 days = 1209600000 milliseconds
    @Query("DELETE FROM contactuuidmodel WHERE Abs(:time - sedentary_end) >= 1209600000")
    void deleteOverdue(long time);

}
