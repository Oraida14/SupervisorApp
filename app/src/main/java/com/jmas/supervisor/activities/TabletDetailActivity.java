package com.jmas.supervisor.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.jmas.supervisor.R;
import com.jmas.supervisor.models.Tablet;

public class TabletDetailActivity extends AppCompatActivity {
    public static final String EXTRA_TABLET = "extra_tablet";
    private TextView tabletId, status, lastUpdate, location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tablet_detail);

        // Actualiza los IDs para que coincidan con el XML
        tabletId = findViewById(R.id.tabletId);
        status = findViewById(R.id.status);
        lastUpdate = findViewById(R.id.lastUpdate);
        location = findViewById(R.id.location);

        Tablet tablet = (Tablet) getIntent().getSerializableExtra(EXTRA_TABLET);
        if (tablet != null) {
            tabletId.setText(tablet.getTabletId());
            status.setText(tablet.getStatus());
            lastUpdate.setText(String.valueOf(tablet.getLastUpdate()));
            location.setText(tablet.getLatitude() + ", " + tablet.getLongitude());
        }
    }
}
