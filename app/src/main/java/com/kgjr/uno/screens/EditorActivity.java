package com.kgjr.uno.screens;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.kgjr.uno.EdgeToEdge;
import com.kgjr.uno.R;

/**
 * Shell for code mode. All the editor / compile / flash logic now lives in
 * {@link com.kgjr.uno.screens.fragments.CodeModeFragment}; this activity only
 * hosts the {@code code_mode_graph} navigation graph.
 */
public class EditorActivity extends AppCompatActivity {

    @Nullable
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, true);
        setContentView(R.layout.activity_editor);

        NavHostFragment navHost = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.navHostFragment);
        if (navHost != null) {
            navController = navHost.getNavController();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null && navController.navigateUp()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }
}