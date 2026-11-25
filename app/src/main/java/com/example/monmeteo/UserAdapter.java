package com.example.monmeteo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class UserAdapter extends BaseAdapter {

    private final Context context;
    private final List<UserProfile> users;
    private final LayoutInflater inflater;

    public UserAdapter(Context context, List<UserProfile> users) {
        this.context = context;
        this.users = users;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return users.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = inflater.inflate(R.layout.row_user, parent, false);
        }
        UserProfile user = users.get(position);
        TextView name = row.findViewById(R.id.tvUserName);
        TextView email = row.findViewById(R.id.tvUserEmail);
        TextView role = row.findViewById(R.id.tvUserRole);
        name.setText(user.getFullName());
        email.setText(user.getEmail());
        role.setText("Rôle : " + user.getRole().name());
        return row;
    }
}
