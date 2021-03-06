package edu.temple.contacttracer.fragments;

import android.os.Bundle;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import edu.temple.contacttracer.R;

import java.util.Calendar;


public class TraceFragment extends Fragment implements OnMapReadyCallback {

    private TextView dateTextView;
    private TextView timeTextView;
    private MapView mapView;
    private GoogleMap googleMap;

    private long date;
    private double latitude;
    private double longitude;

    private Calendar calendar;

    public TraceFragment() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters
    public static TraceFragment newInstance(long date, double latitude, double longitude) {
        TraceFragment fragment = new TraceFragment();
        Bundle args = new Bundle();
        args.putLong("date", date);
        args.putDouble("latitude", latitude);
        args.putDouble("longitude", longitude);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            date = getArguments().getLong("date");
            latitude = getArguments().getDouble("latitude");
            longitude = getArguments().getDouble("longitude");
            calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_trace, container, false);

        dateTextView = view.findViewById(R.id._traceDateTextView);
        timeTextView = view.findViewById(R.id._traceTimeTextView);
        mapView = view.findViewById(R.id._traceMapView);

        String dateText = "CONTACT DATE:\n" + calendar.get(Calendar.DAY_OF_MONTH) + "/" +
                calendar.get(Calendar.MONTH) + "/" +
                calendar.get(Calendar.YEAR);

        String timeText = "CONTACT TIME:\n" + calendar.get(Calendar.HOUR) + ":" +
                calendar.get(Calendar.MINUTE) + ":" +
                calendar.get(Calendar.SECOND) + ":" +
                calendar.get(Calendar.AM_PM);

        dateTextView.setText(dateText);
        timeTextView.setText(timeText);

        mapView.getMapAsync(this);

        return  view;
    }

    public void setDate(long date){
        calendar.setTimeInMillis(date);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng contactMadeLocation = new LatLng(latitude, longitude);
        this.googleMap = googleMap;
        googleMap.addMarker(new MarkerOptions().position(contactMadeLocation).title("CONTACT MADE HERE"));
    }

    public interface  TraceFragmentListener{

    }
}