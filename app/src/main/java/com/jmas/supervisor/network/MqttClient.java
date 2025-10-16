package com.jmas.supervisor.network;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jmas.supervisor.models.Alert;
import com.jmas.supervisor.models.Tablet;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cliente MQTT para recibir ubicaciones y alertas en tiempo real desde las tabletas.
 * También convierte coordenadas en direcciones y mantiene el estado de conexión.
 */
public class MqttClient {

    private static final String TAG = "MqttClient";
    private static final String BROKER_URI = "tcp://broker.emqx.io:1883";
    private static final String LOCATION_TOPIC = "tablet/location";
    private static final String ALERT_TOPIC = "tablet/alert";

    private static final long TIMEOUT_MS = 120000; // 2 min sin datos
    private static final long STATIONARY_MS = 900000; // 15 min en un solo sitio

    private MqttAsyncClient mqttClient;
    private Context context;
    private OnMessageReceivedListener messageListener;
    private Map<String, Tablet> tablets = new HashMap<>();
    private Map<String, LocationSnapshot> lastLocations = new HashMap<>();
    private boolean isConnected = false;
    private final Gson gson = new Gson();

    public interface OnMessageReceivedListener {
        void onLocationUpdate(Tablet tablet);
        void onAlertReceived(Alert alert);
        void onConnectionChanged(boolean connected);
    }

    public MqttClient(Context context) {
        this.context = context;
    }

    // 🔌 Conexión al broker MQTT
    public void connect(OnMessageReceivedListener listener) {
        this.messageListener = listener;

        try {
            String clientId = "Supervisor_" + System.currentTimeMillis();
            mqttClient = new MqttAsyncClient(BROKER_URI, clientId, null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(false);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Conexión MQTT perdida: " + cause.getMessage());
                    isConnected = false;
                    if (messageListener != null) messageListener.onConnectionChanged(false);
                    reconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    try {
                        String payload = new String(message.getPayload());
                        Log.d(TAG, "Mensaje recibido en " + topic + ": " + payload);

                        if (topic.equals(LOCATION_TOPIC)) {
                            Tablet tablet = parseLocationPayload(payload);
                            if (tablet != null) {
                                tablets.put(tablet.getTabletId(), tablet);
                                if (messageListener != null) messageListener.onLocationUpdate(tablet);
                            }
                        } else if (topic.equals(ALERT_TOPIC)) {
                            Alert alert = parseAlertPayload(payload);
                            if (alert != null && messageListener != null) messageListener.onAlertReceived(alert);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando mensaje MQTT: " + e.getMessage());
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "✅ Conectado al broker MQTT");
                    isConnected = true;
                    if (messageListener != null) messageListener.onConnectionChanged(true);
                    subscribeToTopics();
                    startStatusChecker();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "❌ Error al conectar al broker: " + exception.getMessage());
                    isConnected = false;
                    if (messageListener != null) messageListener.onConnectionChanged(false);
                    reconnect();
                }
            });

        } catch (MqttException e) {
            Log.e(TAG, "Error creando cliente MQTT: " + e.getMessage());
            reconnect();
        }
    }

    // 🔁 Reconexión automática
    private void reconnect() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isConnected) connect(messageListener);
        }, 5000);
    }

    // 📡 Suscripción a topics
    private void subscribeToTopics() {
        try {
            mqttClient.subscribe(LOCATION_TOPIC, 1);
            mqttClient.subscribe(ALERT_TOPIC, 1);
            Log.d(TAG, "📡 Suscrito a topics de ubicación y alertas");
        } catch (MqttException e) {
            Log.e(TAG, "Error al suscribirse a topics: " + e.getMessage());
        }
    }

    // 🗺️ Parseo de ubicación
    private Tablet parseLocationPayload(String payload) {
        try {
            JsonObject json = gson.fromJson(payload, JsonObject.class);
            String tabletId = json.get("tabletId").getAsString();
            double lat = json.get("lat").getAsDouble();
            double lon = json.get("lon").getAsDouble();

            Tablet tablet = tablets.getOrDefault(tabletId, new Tablet(tabletId));
            tablet.setLatitude(lat);
            tablet.setLongitude(lon);
            tablet.setLastUpdate(System.currentTimeMillis());
            tablet.setGpsEnabled(true);
            tablet.setOnline(true);

            // Convertir coordenadas a dirección
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    tablet.setAddress(addresses.get(0).getAddressLine(0));
                } else {
                    tablet.setAddress("Ubicación desconocida");
                }
            } catch (Exception e) {
                tablet.setAddress("Error obteniendo dirección");
                Log.e(TAG, "Error geocodificando: " + e.getMessage());
            }

            return tablet;
        } catch (Exception e) {
            Log.e(TAG, "Error al parsear ubicación: " + e.getMessage());
            return null;
        }
    }

    // 🚨 Parseo de alerta
    private Alert parseAlertPayload(String payload) {
        try {
            JsonObject json = gson.fromJson(payload, JsonObject.class);
            String tabletId = json.get("tabletId").getAsString();
            String message = json.get("message").getAsString();
            long timestamp = json.get("timestamp").getAsLong();
            String type = json.get("type").getAsString();

            Tablet tablet = tablets.get(tabletId);
            if (tablet != null) tablet.setGpsEnabled(!type.equals("gps_disabled"));

            return new Alert(tabletId, message, timestamp, type);
        } catch (Exception e) {
            Log.e(TAG, "Error al parsear alerta: " + e.getMessage());
            return null;
        }
    }

    // 🔍 Revisar estado de tabletas
    private class LocationSnapshot {
        double lat, lon;
        long timestamp;
    }

    private void startStatusChecker() {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable checkRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (Tablet tablet : tablets.values()) {

                    // 🚫 Inactividad GPS
                    if (now - tablet.getLastUpdate() > TIMEOUT_MS) {
                        Alert alert = new Alert(tablet.getTabletId(),
                                "Sin señal GPS por más de 2 minutos",
                                now,
                                "gps_timeout");
                        if (messageListener != null) messageListener.onAlertReceived(alert);
                    }

                    // 📍 Permanencia en un solo sitio
                    LocationSnapshot snap = lastLocations.get(tablet.getTabletId());
                    if (snap == null) {
                        snap = new LocationSnapshot();
                        snap.lat = tablet.getLatitude();
                        snap.lon = tablet.getLongitude();
                        snap.timestamp = tablet.getLastUpdate();
                        lastLocations.put(tablet.getTabletId(), snap);
                    } else {
                        double distance = distanceMeters(snap.lat, snap.lon, tablet.getLatitude(), tablet.getLongitude());
                        if (distance < 5) {
                            if (now - snap.timestamp > STATIONARY_MS) {
                                Alert alert = new Alert(tablet.getTabletId(),
                                        "Tableta en la misma ubicación >15 min",
                                        now,
                                        "stationary");
                                if (messageListener != null) messageListener.onAlertReceived(alert);
                            }
                        } else {
                            snap.lat = tablet.getLatitude();
                            snap.lon = tablet.getLongitude();
                            snap.timestamp = now;
                        }
                    }
                }

                handler.postDelayed(this, 30000); // revisar cada 30s
            }
        };
        handler.postDelayed(checkRunnable, 30000);
    }

    // 🔌 Desconexión segura
    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                isConnected = false;
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error al desconectar: " + e.getMessage());
        }
    }

    public Map<String, Tablet> getTablets() {
        return tablets;
    }

    public boolean isConnected() {
        return isConnected;
    }

    // Distancia entre coordenadas en metros
    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }
}
