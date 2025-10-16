package com.jmas.supervisor.activities;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.jmas.supervisor.R;
import com.jmas.supervisor.models.Alert;
import com.jmas.supervisor.models.Tablet;
import com.jmas.supervisor.network.MqttClient;
import com.mapbox.geojson.Point;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.annotation.AnnotationConfig;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;


import java.util.HashMap;
import java.util.Map;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";
    private static final String MAPBOX_TOKEN = "pk.eyJ1IjoiZnJlZGR5ZmllcnJvIiwiYSI6ImNtMzk2eHFtYzExbGcyam9tZG8yN3d2aXQifQ.Yx7HsOnTVplMFrFJXMRYSw";

    private MapView mapView;
    private MapboxMap mapboxMap;

    private MqttClient mqttClient;
    private Map<String, PointAnnotation> tabletMarkers = new HashMap<>();
    private PointAnnotationManager pointAnnotationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Inicializar Mapbox con el token


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.mapView);
        mapboxMap = mapView.getMapboxMap();

        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                // Obtener API de anotaciones
                AnnotationPlugin annotationApi = (AnnotationPlugin) mapView.getPlugin("mapbox-annotation");

                if (annotationApi != null) {
                    pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationApi, new AnnotationConfig());
                } else {
                    Log.e(TAG, "No se pudo obtener el plugin de anotaciones.");
                }

                // Inicializar MQTT
                initMqtt();
            }
        });
    }

    private void initMqtt() {
        mqttClient = new MqttClient(this);
        mqttClient.connect(new MqttClient.OnMessageReceivedListener() {
            @Override
            public void onLocationUpdate(Tablet tablet) {
                runOnUiThread(() -> updateMarker(tablet));
            }

            @Override
            public void onAlertReceived(Alert alert) {
                runOnUiThread(() -> handleAlert(alert));
            }

            @Override
            public void onConnectionChanged(boolean connected) {
                Log.d(TAG, "MQTT conectado: " + connected);
            }
        });
    }

    private void updateMarker(Tablet tablet) {
        if (pointAnnotationManager == null) {
            Log.e(TAG, "pointAnnotationManager no inicializado");
            return;
        }

        Point point = Point.fromLngLat(tablet.getLongitude(), tablet.getLatitude());

        if (tabletMarkers.containsKey(tablet.getTabletId())) {
            PointAnnotation marker = tabletMarkers.get(tablet.getTabletId());
            marker.setPoint(point);
            pointAnnotationManager.update(marker);
        } else {
            PointAnnotationOptions options = new PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage("marker-icon"); // Cambiar por tu icono real
            PointAnnotation marker = pointAnnotationManager.create(options);
            tabletMarkers.put(tablet.getTabletId(), marker);
        }
    }

    private void handleAlert(Alert alert) {
        if (pointAnnotationManager == null) return;

        PointAnnotation marker = tabletMarkers.get(alert.getTabletId());
        if (marker != null) {
            PointAnnotationOptions newOptions = new PointAnnotationOptions()
                    .withPoint(marker.getPoint())
                    .withIconImage(marker.getIconImage())
                    .withIconColor("#FF0000"); // rojo
            pointAnnotationManager.delete(marker);
            PointAnnotation newMarker = pointAnnotationManager.create(newOptions);
            tabletMarkers.put(alert.getTabletId(), newMarker);
        }

        // Vibrar
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) vibrator.vibrate(500);

        // Sonar notificación
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            Log.e(TAG, "Error al reproducir sonido: " + e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (mqttClient != null) mqttClient.disconnect();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
