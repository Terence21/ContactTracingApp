package temple.edu.contacttracingapp.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ContactUUIDModel.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ContactUUIDDao contactUUIDDao();
}
