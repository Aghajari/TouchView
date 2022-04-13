package com.aghajari.touchview;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.MessageFormat;

public class PathActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path);

        AXTouchView touchView = findViewById(R.id.touch);

        touchView.setHelperArrowsEnabled(PathData.helperArrowsEnabled);
        touchView.setPath(PathData.selected);
        touchView.animate((int) (touchView.getPathLength() / 2f), 500, null);

        TextView tv = findViewById(R.id.tv);
        touchView.setOnTouchViewListener(new AXTouchView.OnTouchViewListener() {
            @Override
            public void onProgressChanged(AXTouchView touchView, float progress) {
                if (progress >= 1)
                    Toast.makeText(PathActivity.this, "DONE!", Toast.LENGTH_SHORT).show();
                tv.setText(MessageFormat.format("Progress: {0}%", (int) (progress * 100)));
            }

            @Override
            public void onStartTrackingTouch(AXTouchView touchView) {
            }

            @Override
            public void onStopTrackingTouch(AXTouchView touchView) {
            }
        });
    }

}