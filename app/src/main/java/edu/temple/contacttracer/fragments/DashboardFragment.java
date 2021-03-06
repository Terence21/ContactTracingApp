package edu.temple.contacttracer.fragments;

import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.button.MaterialButton;
import edu.temple.contacttracer.R;

import java.util.Objects;


public class DashboardFragment extends Fragment {

    MaterialButton startButton;
    MaterialButton stopButton;
    MaterialButton dateButton;
    ActivateServiceInterface listener;

    public DashboardFragment() {
        // Required empty public constructor
    }

    public static DashboardFragment newInstance() {
        DashboardFragment fragment = new DashboardFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        startButton = view.findViewById(R.id._startButton);
        stopButton = view.findViewById(R.id._stopButton);
        dateButton = view.findViewById(R.id._dateButton);

        View.OnClickListener ocl = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int id = view.getId();
                switch (id){
                    case R.id._startButton:
                        LocationManager locationManager = Objects.requireNonNull(getActivity()).getSystemService(LocationManager.class);
                        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            listener.startLocatorService();
                        } else{
                            Toast.makeText(getContext(), "ENABLE LOCATION", Toast.LENGTH_SHORT);
                            Log.i("LOCATION ENABLE", "onClick: ENABLE LOCATION PROVIDER");
                        }
                        break;
                    case R.id._stopButton:
                        // when stopped it loses foreground notification privileges.... should fix?
                        listener.stopLocatorService();
                        break;
                    case R.id._dateButton:
                        listener.showCalendar();
                        break;
                }
            }
        };

        startButton.setOnClickListener(ocl);
        stopButton.setOnClickListener(ocl);
        dateButton.setOnClickListener(ocl);

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof ActivateServiceInterface){
            listener = (ActivateServiceInterface) context;
        } else{
            throw new RuntimeException("Calling activity must implement instance of ActivateServiceInterface");
        }

    }

    public interface ActivateServiceInterface{
        public void startLocatorService();
        public void stopLocatorService();
        public void showCalendar();
    }

}