package com.jmas.supervisor.network;

import android.content.Context;
import android.util.Log;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.jmas.supervisor.models.Tablet;
import com.jmas.supervisor.models.Alert;
import java.util.HashMap;
import java.util.Map;

public class MqttClient {
    private static final String TAG = "MqttClient";
    private static final String BROKER_URI = "tcp://broker.emqx.io:1883";
    private static final String LOCATION_TOPIC = "tablet/location";
    private static final String ALERT_TOPIC = "tablet/alert";

    private MqttAsyncClient mqttClient;
    private Context context;
    private OnMessageReceivedListener messageListener;
    private Map<String, Tablet> tablets = new HashMap<>();
    private boolean isConnected = false;

    public interface OnMessageReceivedListener {
        void onLocationUpdate(Tablet tablet);
        void onAlertReceived(Alert alert);
        void onConnectionChanged(boolean connected);
    }

    public MqttClient(Context context) {
        this.context = context;
    }

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
                    if (messageListener != null) {
                        messageListener.onConnectionChanged(false);
                    }
                    reconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());

                    Log.d(TAG, "Mensaje recibido en " + topic + ": " + payload);

                    if (topic.equals(LOCATION_TOPIC)) {
                        Tablet tablet = parseLocationPayload(payload);
                        if (tablet != null) {
                            tablets.put(tablet.getTabletId(), tablet);
                            if (messageListener != null) {
                                messageListener.onLocationUpdate(tablet);
                            }
                        }
                    } else if (topic.equals(ALERT_TOPIC)) {
                        Alert alert = parseAlertPayload(payload);
                        if (alert != null && messageListener != null) {
                            messageListener.onAlertReceived(alert);
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // No se usa en este cliente
                }
            });

            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Conectado al broker MQTT");
                    isConnected = true;
                    if (messageListener != null) {
                        messageListener.onConnectionChanged(true);
                    }
                    subscribeToTopics();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Error al conectar al broker MQTT: " + exception.getMessage());
                    isConnected = false;
                    if (messageListener != null) {
                        messageListener.onConnectionChanged(false);
                    }
                    reconnect();
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Error al crear cliente MQTT: " + e.getMessage());
            isConnected = false;
            if (messageListener != null) {
                messageListener.onConnectionChanged(false);
            }
            reconnect();
        }
    }

    private void reconnect() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!isConnected) {
                connect(messageListener);
            }
        }, 5000);
    }

    private void subscribeToTopics() {
        try {
            mqttClient.subscribe(LOCATION_TOPIC, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Suscrito a " + LOCATION_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Error al suscribirse a " + LOCATION_TOPIC + ": " + exception.getMessage());
                }
            });

            mqttClient.subscribe(ALERT_TOPIC, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Suscrito a " + ALERT_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Error al suscribirse a " + ALERT_TOPIC + ": " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Error al suscribirse a topics: " + e.getMessage());
        }
    }

    private Tablet parseLocationPayload(String payload) {
        try {
            // Ejemplo de payload: {"tabletId":"Ramona","lat":31.729713,"lon":-106.531311}
            // Implementa el parsing según el formato real de tus mensajes
            // Aquí un ejemplo simple (deberías usar Gson o similar en producción)
            String[] parts = payload.split(",");
            String tabletId = parts[0].split(":")[1].replace("\"", "").trim();
            double lat = Double.parseDouble(parts[1].split(":")[1].replace("}", "").trim());
            double lon = Double.parseDouble(parts[2].split(":")[1].replace("}", "").trim());

            Tablet tablet = tablets.get(tabletId);
            if (tablet == null) {
                tablet = new Tablet(tabletId);
            }
            tablet.setLatitude(lat);
            tablet.setLongitude(lon);
            tablet.setLastUpdate(System.currentTimeMillis());
            tablet.setGpsEnabled(true);

            return tablet;
        } catch (Exception e) {
            Log.e(TAG, "Error al parsear payload de ubicación: " + e.getMessage());
            return null;
        }
    }

    private Alert parseAlertPayload(String payload) {
        try {
            // Ejemplo de payload: {"tabletId":"Ramona","message":"GPS desactivado","timestamp":123456789,"type":"gps_disabled"}
            // Implementa el parsing según el formato real de tus mensajes
            // Aquí un ejemplo simple (deberías usar Gson o similar en producción)
            String[] parts = payload.split(",");
            String tabletId = parts[0].split(":")[1].replace("\"", "").trim();
            String message = parts[1].split(":")[1].replace("\"", "").trim();
            long timestamp = Long.parseLong(parts[2].split(":")[1].trim());
            String type = parts[3].split(":")[1].replace("\"", "").replace("}", "").trim();

            Tablet tablet = tablets.get(tabletId);
            if (tablet != null) {
                tablet.setGpsEnabled(false);
            }

            return new Alert(tabletId, message, timestamp, type);
        } catch (Exception e) {
            Log.e(TAG, "Error al parsear payload de alerta: " + e.getMessage());
            return null;
        }
    }

    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
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
}
