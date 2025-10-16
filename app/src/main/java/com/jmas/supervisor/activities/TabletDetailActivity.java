package com.jmas.supervisor.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.jmas.supervisor.R;
import com.jmas.supervisor.models.Tablet;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;


public class TabletDetailActivity extends AppCompatActivity {
    public static final String EXTRA_TABLET = "extra_tablet";
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private TextView tabletId, status, lastUpdate, location;
    private MapView mapView;
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tablet_detail);

        // Inicializa los TextView
        tabletId = findViewById(R.id.tabletId);
        status = findViewById(R.id.status);
        lastUpdate = findViewById(R.id.lastUpdate);
        location = findViewById(R.id.location);

        // Inicializa el MapView
        mapView = findViewById(R.id.mapView);

        // Recibe los datos de la tableta
        Tablet tablet = (Tablet) getIntent().getSerializableExtra(EXTRA_TABLET);
        if (tablet == null) {
            Toast.makeText(this, "Error: No se recibieron datos de la tableta", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Muestra los detalles de la tableta
        tabletId.setText(tablet.getTabletId() != null ? tablet.getTabletId() : "Desconocido");
        status.setText(tablet.getStatus() != null ? tablet.getStatus() : "Desconocido");

        // Convierte lastUpdate (long) a String para mostrar
        long lastUpdateValue = tablet.getLastUpdate();
        lastUpdate.setText(lastUpdateValue != 0 ? String.valueOf(lastUpdateValue) : "Desconocido");

        // Obtiene latitud y longitud como double
        latitude = tablet.getLatitude();
        longitude = tablet.getLongitude();

        // Muestra las coordenadas en formato texto
        location.setText(String.valueOf(latitude) + ", " + String.valueOf(longitude));

        // Solicita permisos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
        } else {
            setupMap();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupMap();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado. El mapa usará coordenadas por defecto.", Toast.LENGTH_SHORT).show();
                setupMap();
            }
        }
    }

    private void setupMap() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            mapView.getMapboxMap().setCamera(
                    new CameraOptions.Builder()
                            .center(Point.fromLngLat(longitude, latitude))
                            .zoom(14.0)
                            .build()
            );
        });
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
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
