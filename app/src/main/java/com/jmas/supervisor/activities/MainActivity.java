package com.jmas.supervisor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.jmas.supervisor.R;
import com.jmas.supervisor.adapters.TabletsAdapter;
import com.jmas.supervisor.models.Alert;
import com.jmas.supervisor.models.Tablet;
import com.jmas.supervisor.network.MqttClient;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MqttClient.OnMessageReceivedListener {

    private RecyclerView recyclerView;
    private TabletsAdapter adapter;
    private List<Tablet> tabletsList = new ArrayList<>();
    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TabletsAdapter(tabletsList);
        recyclerView.setAdapter(adapter);

        // Click en tablet para abrir mapa
        adapter.setOnTabletClickListener(tablet -> {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("tabletId", tablet.getTabletId());
            intent.putExtra("latitude", tablet.getLatitude());
            intent.putExtra("longitude", tablet.getLongitude());
            startActivity(intent);
        });

        // Inicializar cliente MQTT
        mqttClient = new MqttClient(this);
        mqttClient.connect(this);
    }

    @Override
    public void onLocationUpdate(Tablet tablet) {
        runOnUiThread(() -> {
            int index = findTabletIndex(tablet.getTabletId());
            tablet.setLastUpdate(System.currentTimeMillis());
            if (index >= 0) {
                tabletsList.set(index, tablet);
            } else {
                tabletsList.add(tablet);
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onAlertReceived(Alert alert) {
        runOnUiThread(() -> {
            // Vibración corta
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) vibrator.vibrate(400);

            // Mostrar alerta
            Snackbar snackbar = Snackbar.make(recyclerView,
                    "🚨 " + alert.getMessage() + " (Tablet: " + alert.getTabletId() + ")",
                    Snackbar.LENGTH_LONG);
            snackbar.setBackgroundTint(getResources().getColor(android.R.color.holo_red_dark));
            snackbar.show();

            // Actualizar estado de la tablet en la lista
            for (Tablet tablet : tabletsList) {
                if (tablet.getTabletId().equals(alert.getTabletId())) {
                    tablet.setGpsEnabled(false);
                    tablet.setStatus("⚠ " + alert.getMessage());
                    break;
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onConnectionChanged(boolean connected) {
        runOnUiThread(() -> {
            String message = connected ? "✅ Conectado al servidor MQTT" : "❌ Desconectado del servidor MQTT";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private int findTabletIndex(String tabletId) {
        for (int i = 0; i < tabletsList.size(); i++) {
            if (tabletsList.get(i).getTabletId().equals(tabletId)) return i;
        }
        return -1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttClient != null) mqttClient.disconnect();
    }
}
