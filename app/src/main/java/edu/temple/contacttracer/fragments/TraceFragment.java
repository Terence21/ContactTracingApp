package edu.temple.contacttracer.fragments;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import edu.temple.contacttracer.R;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;


public class TraceFragment extends Fragment implements  OnMapReadyCallback{

    private TextView dateTextView;
    private TextView timeTextView;
    private MapView mapView;
    private GoogleMap gMap;

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
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


        mapView.onCreate(savedInstanceState);
        String dateText = "CONTACT DATE:\n" + calendar.get(Calendar.DAY_OF_MONTH) + "/" +
                calendar.get(Calendar.MONTH) + "/" +
                calendar.get(Calendar.YEAR);

        String timeText = "CONTACT TIME:\n" + calendar.get(Calendar.HOUR) + ":" +
                calendar.get(Calendar.MINUTE) + ":" +
                calendar.get(Calendar.SECOND) + ":" +
                calendar.get(Calendar.AM_PM);

        dateTextView.setText(dateText);
        timeTextView.setText(timeText);

        return  view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView = view.findViewById(R.id._traceMapView);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng contactMadeLocation = new LatLng(latitude, longitude);

        googleMap.addMarker(new MarkerOptions().position(contactMadeLocation).title("CONTACT MADE HERE"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(contactMadeLocation, 15));
        gMap = googleMap;

    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    public interface  TraceFragmentListener{

    }
}