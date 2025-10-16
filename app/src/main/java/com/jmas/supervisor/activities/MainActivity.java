package com.jmas.supervisor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jmas.supervisor.adapters.TabletAdapter;
import com.jmas.supervisor.models.Tablet;
import com.jmas.supervisor.network.MqttClient;
import com.jmas.supervisor.R; // ✅ este es el correcto


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private TabletAdapter adapter;
    private List<Tablet> tabletList = new ArrayList<>();
    private MqttClient mqttClient;

    private FloatingActionButton fabMap, fabAlerts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TabletAdapter(tabletList);
        recyclerView.setAdapter(adapter);

        fabMap = findViewById(R.id.fabMap);
        fabAlerts = findViewById(R.id.fabAlerts);

        fabMap.setOnClickListener(v -> openMap());
        fabAlerts.setOnClickListener(v -> openAlerts());

        mqttClient = new MqttClient(this);
        mqttClient.connect(new MqttClient.OnMessageReceivedListener() {
            @Override
            public void onLocationUpdate(Tablet tablet) {
                runOnUiThread(() -> updateTabletList(tablet));
            }

            @Override
            public void onAlertReceived(com.jmas.supervisor.models.Alert alert) {
                // Aquí podrías actualizar RecyclerView si quieres mostrar alertas
                Log.d(TAG, "Alerta: " + alert.getMessage());
            }

            @Override
            public void onConnectionChanged(boolean connected) {
                Log.d(TAG, "MQTT conectado: " + connected);
            }
        });
    }

    private void updateTabletList(Tablet tablet) {
        boolean exists = false;
        for (int i = 0; i < tabletList.size(); i++) {
            if (tabletList.get(i).getTabletId().equals(tablet.getTabletId())) {
                tabletList.set(i, tablet);
                exists = true;
                break;
            }
        }
        if (!exists) tabletList.add(tablet);
        adapter.notifyDataSetChanged();
    }

    private void openMap() {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    private void openAlerts() {
        Intent intent = new Intent(this, AlertsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttClient != null) mqttClient.disconnect();
    }
}
