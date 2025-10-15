package com.jmas.supervisor.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.jmas.supervisor.R;
import com.jmas.supervisor.adapters.TabletsAdapter;
import com.jmas.supervisor.models.Alert;
import java.util.ArrayList;
import java.util.List;

public class AlertsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TabletsAdapter adapter;
    private List<Alert> alertList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        // Aquí usamos el id correcto de tu XML
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicializamos el adapter con la lista vacía
        adapter = new TabletsAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Si quieres mostrar la vista vacía cuando no hay alertas:
        if (alertList.isEmpty()) {
            findViewById(R.id.emptyView).setVisibility(android.view.View.VISIBLE);
        }
    }
}
