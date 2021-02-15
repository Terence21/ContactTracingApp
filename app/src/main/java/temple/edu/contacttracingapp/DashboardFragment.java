package temple.edu.contacttracingapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.button.MaterialButton;


public class DashboardFragment extends Fragment {

    MaterialButton startButton;
    MaterialButton stopButton;
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

        View.OnClickListener ocl = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int id = view.getId();
                switch (id){
                    case R.id._startButton:
                        listener.startLocatorService();
                        break;
                    case R.id._stopButton:
                        listener.stopLocatorService();
                        break;
                }
            }
        };

        startButton.setOnClickListener(ocl);
        stopButton.setOnClickListener(ocl);

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
    }

}