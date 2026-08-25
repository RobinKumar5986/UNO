package com.kgjr.uno;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.kgjr.uno.ide.EditorActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.editorButton).setOnClickListener(v ->
                startActivity(new Intent(this, EditorActivity.class)));
    }
}