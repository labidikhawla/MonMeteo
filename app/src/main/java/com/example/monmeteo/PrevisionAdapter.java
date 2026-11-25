package com.example.monmeteo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class PrevisionAdapter extends BaseAdapter {

    private final Context context;
    private final List<Prevision> previsions;

    public PrevisionAdapter(Context context, List<Prevision> previsions) {
        this.context = context;
        this.previsions = previsions;
    }

    @Override
    public int getCount() {
        return previsions.size();
    }

    @Override
    public Prevision getItem(int position) {
        return previsions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_prevision, parent, false);
            h = new ViewHolder();
            h.symbole = convertView.findViewById(R.id.imgSymbole);
            h.jour = convertView.findViewById(R.id.txtJour);
            h.condition = convertView.findViewById(R.id.txtConditionLigne);
            h.temperature = convertView.findViewById(R.id.txtTemperatureLigne);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        Prevision p = getItem(position);
        h.symbole.setText(p.getSymbole());
        h.jour.setText(p.getJour());
        h.condition.setText(p.getCondition());
        h.temperature.setText(p.getMin() + "° / " + p.getMax() + "°");
        return convertView;
    }

    // Le recyclage des lignes évite de refaire findViewById() à chaque défilement.
    static class ViewHolder {
        TextView symbole;
        TextView jour;
        TextView condition;
        TextView temperature;
    }
}
