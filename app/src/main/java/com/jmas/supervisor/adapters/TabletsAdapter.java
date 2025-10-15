package com.jmas.supervisor.adapters;



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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TabletsAdapter extends RecyclerView.Adapter<TabletsAdapter.TabletViewHolder> {

    private List<Tablet> tablets;
    private OnTabletClickListener clickListener;
    private Handler handler = new Handler();

    public interface OnTabletClickListener {
        void onTabletClick(Tablet tablet);
    }

    public TabletsAdapter(List<Tablet> tablets) {
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

        // Formatear última actualización
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String lastUpdate = sdf.format(new Date(tablet.getLastUpdate()));
        holder.lastUpdate.setText("Última actualización: " + lastUpdate);

        // Formatear ubicación
        holder.location.setText(String.format(Locale.getDefault(),
                "Lat: %.6f, Lon: %.6f", tablet.getLatitude(), tablet.getLongitude()));

        // Colores y parpadeo
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

            cardView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onTabletClick(tablets.get(getAdapterPosition()));
                }
            });
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
}
