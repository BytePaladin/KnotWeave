package com.example.knotweavesdk;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.knotweave.sdk.KnotWeaveManager;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize the SDK and set its view as the main content of this activity
        View sdkView = new KnotWeaveManager().initialize(this);
        setContentView(sdkView);
    }
}
