package com.jmas.supervisor.adapters;

import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.jmas.supervisor.R;
import com.jmas.supervisor.models.Tablet;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TabletAdapter extends RecyclerView.Adapter<TabletAdapter.TabletViewHolder> {

    private List<Tablet> tablets;
    private OnTabletClickListener clickListener;
    private Handler handler = new Handler();

    public interface OnTabletClickListener {
        void onTabletClick(Tablet tablet);
    }

    public TabletAdapter(List<Tablet> tablets) {
        this.tablets = tablets;
    }

    public void setOnTabletClickListener(OnTabletClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public TabletViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tablet, parent, false);
        return new TabletViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TabletViewHolder holder, int position) {
        Tablet tablet = tablets.get(position);

        holder.tabletId.setText(tablet.getTabletId());
        holder.status.setText(tablet.getStatus());

        // --- Última actualización ---
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String lastUpdate = sdf.format(new Date(tablet.getLastUpdate()));
        holder.lastUpdate.setText("Última actualización: " + lastUpdate);

        // --- Mostrar dirección o coordenadas ---
        if (tablet.getAddress() != null && !tablet.getAddress().isEmpty()) {
            holder.location.setText(tablet.getAddress());
        } else {
            holder.location.setText(String.format(Locale.getDefault(),
                    "Lat: %.6f, Lon: %.6f", tablet.getLatitude(), tablet.getLongitude()));
            // Lanzar hilo para obtener dirección si no la tiene
            fetchAddressAsync(holder, tablet);
        }

        // --- Colores y efectos ---
        int colorIndicator;
        int colorCard = ContextCompat.getColor(holder.itemView.getContext(), R.color.card_normal);

        if (tablet.getStatus().contains("⚠")) {
            colorIndicator = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_alert);
            colorCard = ContextCompat.getColor(holder.itemView.getContext(), R.color.card_alert);

            startBlinking(holder.statusIndicator, colorIndicator,
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.status_inactive));

            startBlinkingCard(holder.cardView, colorCard,
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.card_normal));

        } else if (!tablet.isGpsEnabled()) {
            colorIndicator = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_warning);
            stopBlinking(holder.statusIndicator);
            stopBlinkingCard(holder.cardView);

        } else {
            colorIndicator = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_active);
            stopBlinking(holder.statusIndicator);
            stopBlinkingCard(holder.cardView);
        }

        holder.statusIndicator.setBackgroundColor(colorIndicator);

        // --- Click para ver en mapa ---
        holder.cardView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTabletClick(tablet);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tablets.size();
    }

    class TabletViewHolder extends RecyclerView.ViewHolder {
        TextView tabletId, status, lastUpdate, location;
        View statusIndicator;
        CardView cardView;

        TabletViewHolder(@NonNull View itemView) {
            super(itemView);
            tabletId = itemView.findViewById(R.id.tabletId);
            status = itemView.findViewById(R.id.status);
            lastUpdate = itemView.findViewById(R.id.lastUpdate);
            location = itemView.findViewById(R.id.location);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }

    // --- Parpadeo indicador ---
    private void startBlinking(View view, int color1, int color2) {
        final Runnable blinkRunnable = new Runnable() {
            boolean on = true;
            @Override
            public void run() {
                view.setBackgroundColor(on ? color1 : color2);
                on = !on;
                view.postDelayed(this, 500);
            }
        };
        view.setTag(blinkRunnable);
        view.post(blinkRunnable);
    }

    private void stopBlinking(View view) {
        Object tag = view.getTag();
        if (tag instanceof Runnable) {
            view.removeCallbacks((Runnable) tag);
            view.setTag(null);
        }
    }

    // --- Parpadeo Card ---
    private void startBlinkingCard(CardView view, int color1, int color2) {
        final Runnable blinkRunnable = new Runnable() {
            boolean on = true;
            @Override
            public void run() {
                view.setCardBackgroundColor(on ? color1 : color2);
                on = !on;
                view.postDelayed(this, 500);
            }
        };
        view.setTag(blinkRunnable);
        view.post(blinkRunnable);
    }

    private void stopBlinkingCard(CardView view) {
        Object tag = view.getTag();
        if (tag instanceof Runnable) {
            view.removeCallbacks((Runnable) tag);
            view.setTag(null);
            view.setCardBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.card_normal));
        }
    }

    // --- Obtener dirección desde coordenadas ---
    private void fetchAddressAsync(TabletViewHolder holder, Tablet tablet) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(holder.itemView.getContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(tablet.getLatitude(), tablet.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String address = addresses.get(0).getAddressLine(0);
                    tablet.setAddress(address);

                    handler.post(() -> holder.location.setText(address));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
