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
        private String uuid;
        private int year;
        private int month;
        private int day;

        public ContactUUID(){
            this.uuid = UUID.randomUUID().toString();
            Calendar calendar = Calendar.getInstance();
            this.year = calendar.get(Calendar.YEAR);
            this.month = calendar.get(Calendar.MONTH);
            this.day = calendar.get(Calendar.DAY_OF_MONTH);

        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
        }
    }
}
