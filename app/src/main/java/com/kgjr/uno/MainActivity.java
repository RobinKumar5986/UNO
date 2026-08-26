package com.kgjr.uno;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kgjr.uno.adapters.ActionCardAdapter;
import com.kgjr.uno.screens.EditorActivity;
import com.kgjr.uno.models.ActionItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActionCardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<ActionItem> items = new ArrayList<>();
        items.add(new ActionItem("Code Mode", R.drawable.ic_code, R.color.accent_yellow));
        items.add(new ActionItem("Save Project", R.drawable.ic_save, R.color.accent_blue));
        items.add(new ActionItem("Donate", R.drawable.ic_favorite, R.color.accent_red));
        items.add(new ActionItem("Help", R.drawable.ic_help, R.color.accent_green));

        adapter = new ActionCardAdapter(items, (position, item) -> {
            switch (item.getTitle()) {
                case "Code Mode":
                    startActivity(new Intent(this, EditorActivity.class));
                    break;
                case "Save Project":
                    Toast.makeText(this, "Save Project coming soon", Toast.LENGTH_SHORT).show();
                    break;
                case "Donate":
                    Toast.makeText(this, "Donate coming soon", Toast.LENGTH_SHORT).show();
                    break;
                case "Help":
                    Toast.makeText(this, "Help coming soon", Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        RecyclerView recyclerView = findViewById(R.id.actionCardsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);
    }
}