package com.metaphasicgroup.luma;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/** Simple searchable grid adapter over the generated IconCatalog. Original code. */
public class IconAdapter extends RecyclerView.Adapter<IconAdapter.Holder> {
    static class Holder extends RecyclerView.ViewHolder {
        ImageView icon; TextView label;
        Holder(View v) { super(v); icon = v.findViewById(R.id.item_icon); label = v.findViewById(R.id.item_label); }
    }
    private final Context ctx;
    private final String[] names; private final int[] drawables;
    private final List<Integer> shown = new ArrayList<>();

    IconAdapter(Context ctx, String[] names, int[] drawables) {
        this.ctx = ctx; this.names = names; this.drawables = drawables;
        for (int i = 0; i < names.length; i++) shown.add(i);
    }
    void filter(String q) {
        shown.clear();
        String needle = q.toLowerCase();
        for (int i = 0; i < names.length; i++) {
            if (needle.isEmpty() || names[i].toLowerCase().contains(needle)) shown.add(i);
        }
        notifyDataSetChanged();
    }
    @Override public Holder onCreateViewHolder(ViewGroup p, int t) {
        return new Holder(LayoutInflater.from(ctx).inflate(R.layout.item_icon, p, false));
    }
    @Override public void onBindViewHolder(Holder h, int pos) {
        int i = shown.get(pos);
        h.icon.setImageResource(drawables[i]);
        h.label.setText(names[i]);
    }
    @Override public int getItemCount() { return shown.size(); }
}
