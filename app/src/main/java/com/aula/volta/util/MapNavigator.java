package com.aula.volta.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.aula.volta.R;

import java.util.Locale;

public final class MapNavigator {

    private MapNavigator() {
    }

    public static void openLocation(Context context, double lat, double lng, String label) {
        String uriStr = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                lat, lng, lat, lng, Uri.encode(label));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));

        try {
            context.startActivity(mapIntent);
        } catch (Exception e) {

            try {
                String webUrl = String.format(Locale.US,
                        "https://www.google.com/maps/search/?api=1&query=%f,%f", lat, lng);
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                context.startActivity(webIntent);
            } catch (Exception ex) {
                Toast.makeText(context, R.string.error_load, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
