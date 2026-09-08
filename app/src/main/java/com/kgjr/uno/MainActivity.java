package com.kgjr.uno;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kgjr.uno.adapters.ActionCardAdapter;
import com.kgjr.uno.data.ProjectSession;
import com.kgjr.uno.screens.EditorActivity;
import com.kgjr.uno.screens.SavedProjectsActivity;
import com.kgjr.uno.models.ActionItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /** Card titles double as the dispatch key, so they are constants rather than loose strings. */
    private static final String CODE_MODE = "Code Mode";
    private static final String SAVED_PROJECTS = "Saved Projects";

    private ActionCardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, true);
        setContentView(R.layout.activity_main);

        List<ActionItem> items = new ArrayList<>();
        items.add(new ActionItem(CODE_MODE, R.drawable.ic_code, R.color.accent_yellow));
        items.add(new ActionItem(SAVED_PROJECTS, R.drawable.ic_save, R.color.accent_blue));
        items.add(new ActionItem("Donate", R.drawable.ic_favorite, R.color.accent_red));
        items.add(new ActionItem("Help", R.drawable.ic_help, R.color.accent_green));

        adapter = new ActionCardAdapter(items, (position, item) -> {
            switch (item.getTitle()) {
                case CODE_MODE:
                    openCodeMode();
                    break;
                case SAVED_PROJECTS:
                    startActivity(new Intent(this, SavedProjectsActivity.class));
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

    /**
     * Code Mode always means a new project, so the first save mints a fresh id. Reopening
     * earlier work is what the Saved Projects card is for.
     */
    private void openCodeMode() {
        ProjectSession.startNew(this);
        startActivity(new Intent(this, EditorActivity.class));
    }
}