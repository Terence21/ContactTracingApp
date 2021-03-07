package edu.temple.contacttracer.models;

import java.util.ArrayList;

public class TracingModel {
    private long date;
    private ArrayList<String> uuids;

    public TracingModel(long date, ArrayList<String> uuids) {
        this.date = date;
        this.uuids = uuids;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public ArrayList<String> getUuids() {
        return uuids;
    }

    public void setUuids(ArrayList<String> uuids) {
        this.uuids = uuids;
    }
}
