package temple.edu.contacttracingapp.database;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface ContactUUIDDao {

    @Query("SELECT * FROM ContactUUIDModel")
    List<ContactUUIDModel> getAll();

    @Query("SELECT * FROM ContactUUIDModel WHERE `index` = :place")
    ContactUUIDModel getContactUUIDModel(int place);

    @Insert
    void insert(ContactUUIDModel contactUUIDModel);

    @Query("UPDATE contactuuidmodel SET uuid = :u, year = :y, month = :m, day = :d WHERE `index` = :i")
    void replace(int i, String u, int y, int m, int d);

    @Query("SELECT COUNT(`index`) FROM CONTACTUUIDMODEL")
    int getSize();

    @Query("UPDATE contactuuidmodel SET `index` = `index` + 1 WHERE `index` > -1")
    void incrementIndex();

    @Query("DELETE FROM contactuuidmodel WHERE `index` = :index")
    void delete(int index);

}
