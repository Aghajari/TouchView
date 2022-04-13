package com.aghajari.touchview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends AppCompatActivity {

    RecyclerView rv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rv = findViewById(R.id.rv);
        rv.setAdapter(new Adapter());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        AppCompatCheckBox checkBox = (AppCompatCheckBox) menu.getItem(0).getActionView();
        checkBox.setChecked(PathData.helperArrowsEnabled);
        checkBox.setText(R.string.helper_arrows);
        checkBox.setPadding(0, 0, 40, 0);
        checkBox.setOnCheckedChangeListener((v, c) -> {
            PathData.helperArrowsEnabled = c;
            rv.getAdapter().notifyDataSetChanged();
        });
        return true;
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rv_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MainActivity.Adapter.VH holder, int position) {
            holder.touchView.setHelperArrowsEnabled(PathData.helperArrowsEnabled);
            holder.touchView.setPath(PathData.paths[position]);
            holder.itemView.setOnClickListener(v -> {
                PathData.selected = PathData.paths[position];
                startActivity(new Intent(MainActivity.this, PathActivity.class));
            });
        }

        @Override
        public int getItemCount() {
            return PathData.paths.length;
        }

        private class VH extends RecyclerView.ViewHolder {
            AXTouchView touchView;

            public VH(@NonNull View itemView) {
                super(itemView);
                touchView = itemView.findViewById(R.id.touch);
                touchView.setEnabled(false);
            }
        }
    }
}