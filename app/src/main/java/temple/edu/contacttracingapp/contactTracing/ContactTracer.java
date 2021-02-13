package temple.edu.contacttracingapp.contactTracing;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;

public class ContactTracer {
    private LinkedList<ContactUUID> uuidContactList;
    private static final int LIMIT = 14;
    private int size;

    public ContactTracer(){
        this.size = 0;
        this.uuidContactList = new LinkedList<>();
    }

    public void addUUID(){
        this.uuidContactList.add(new ContactUUID());
        if (this.uuidContactList.size() == LIMIT + 1){
            this.uuidContactList.remove(14);
        }else{
            size++;
        }
    }

    public ContactUUID getUUIDMinusDays(int difference){
        if (difference > 0 && difference < size){
            return null;
        }
        return uuidContactList.get(size-difference);
    }

    public ContactUUID getUUID(int index){
        if (index >= 0 && index < size){
            return uuidContactList.get(index);
        }
        return null;
    }

    private class ContactUUID{
        private UUID uuid;
        private Calendar calendar;

        public ContactUUID(){
            this.uuid = UUID.randomUUID();
            this.calendar = Calendar.getInstance();
        }

        public UUID getUuid() {
            return uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }

        public Calendar getCalendar() {
            return calendar;
        }

        public void setCalendar(Calendar calendar) {
            this.calendar = calendar;
        }
    }
}
