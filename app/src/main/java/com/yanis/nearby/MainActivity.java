package com.yanis.nearby;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

public class MainActivity extends AppCompatActivity {
    public static final String SERVICE_ID = "com.yanis.nearby";
    private TextView textView;
    private final String name = "name"; // i don't know what it does, but it works

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
                    Log.d("Nearby", "This is onPayloadReceived()");
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                    Log.d("Nearby", "This is onPayloadTransferUpdate()");
                }
            };

    private final  ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.d("Nearby", "this is 1 & endpointId = " + endpointId);
                    Nearby.getConnectionsClient(MainActivity.this).acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {
                    Log.d("Nearby", "this is 2 & endpointId = " + endpointId);
                    switch (connectionResolution.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK :
                            Log.d("Nearby", "ConnectionsStatusCodes = OK");
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            Log.d("Nearby", "ConnectionsStatusCodes = REJECTED");
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            Log.d("Nearby", "ConnectionsStatusCodes = ERROR");
                            break;
                        default:
                            Log.d("Nearby", "How knows what is the problem ?");
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.d("Nearby", "this is 3 & endpointId = " + endpointId);
                }
            };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpoitId, DiscoveredEndpointInfo info) {
                    Nearby.getConnectionsClient(MainActivity.this)
                            .requestConnection(name, endpoitId, connectionLifecycleCallback)
                            .addOnSuccessListener(
                                    (Void unused) -> {
                                        // ??
                                    })
                            .addOnFailureListener(
                                    (Exception e) -> {
                                        // ??
                                    });
                }

                @Override
                public void onEndpointLost(@NonNull String s) {
                    // ..
                }
            };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonAdverting = findViewById(R.id.btnAdverting);
        Button buttonDiscovery = findViewById(R.id.btnDiscovery);
        Button buttonStop = findViewById(R.id.btnStop);
        textView = findViewById(R.id.txt);

        buttonAdverting.setOnClickListener(v -> {
            if(checkPermission()) {
                startAdverting();
            }
        });

        buttonDiscovery.setOnClickListener(v -> {
            if(checkPermission()) {
                startDiscovery();
            }
        });

        buttonStop.setOnClickListener(v -> {
            Nearby.getConnectionsClient(MainActivity.this).stopDiscovery();
            Nearby.getConnectionsClient(this).stopAdvertising();
            Log.d("Nearby", "try to stop");
        });
    }

    private void startAdverting() {
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        Nearby.getConnectionsClient(this)
                .startAdvertising(
                        getLocalClassName(), SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            textView.setText("Adverting");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            textView.setText("Clicked but failed");
                        });
    }

    private void startDiscovery() {
        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        Nearby.getConnectionsClient(MainActivity.this)
                .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            textView.setText("Discovry");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            textView.setText("fail");
                });
    }


    // Ask for permission ; ACCESS_FINE_LOCATION
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted ->{
                if(isGranted) {
                    Log.d("PERMISSION", "PERMISSION OK! Go next.");
                } else {
                    Log.d("PERMISSION", "PERMISSION IS DENIED, NOTHING CAN BE DONE");
                }
            });

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkPermission() {
        if(ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            Log.d("PERMISSION", "THE PERMISSION IS GIVEN.");
            return true;
        } else if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
            Log.d("PERMISSION", "WE DON'T HAVE PERMISSION, AND HAVE ALREADY BEEN DENIED.");
            requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION);
            return false;
        } else {
            Log.d("PERMISSION", "WE DON'T HAVE PERMISSION.");
            requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION);
            return false;
        }
    }
}