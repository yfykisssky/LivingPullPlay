package com.living.pullplay.activity.aoa.view;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.living.pullplay.R;

import java.util.List;

public class DeviceListAdapter extends ArrayAdapter<UsbDevice> {
    private int resourceId;

    public DeviceListAdapter(Context context, int textViewResourceId, List<UsbDevice> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        UsbDevice device = getItem(position);

        View view;
        ViewHolder viewHolder;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.nameTex = view.findViewById(R.id.nameTex);
            viewHolder.pidTex = view.findViewById(R.id.pidTex);
            viewHolder.vidTex = view.findViewById(R.id.vidTex);
            viewHolder.startBnt = view.findViewById(R.id.startBnt);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        viewHolder.nameTex.setText(device.getDeviceName());

        return view;
    }

    class ViewHolder {
        TextView nameTex;
        TextView pidTex;
        TextView vidTex;
        Button startBnt;
    }

}

