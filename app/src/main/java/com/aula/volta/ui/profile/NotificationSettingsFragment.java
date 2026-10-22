package com.aula.volta.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aula.volta.R;

/**
 * 23e · Perfil · notificações (Figma 1721:1460).
 */
public class NotificationSettingsFragment extends Fragment {

    private static final String PREFS_NAME = "prefs_notification_settings";
    private static final String KEY_APPROVED = "notif_approved";
    private static final String KEY_COLLECTION = "notif_collection";
    private static final String KEY_SECTOR = "notif_sector";

    private SwitchCompat switchApproved;
    private SwitchCompat switchCollection;
    private SwitchCompat switchSector;

    public NotificationSettingsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switchApproved = view.findViewById(R.id.switchNotifApproved);
        switchCollection = view.findViewById(R.id.switchNotifCollection);
        switchSector = view.findViewById(R.id.switchNotifSector);

        view.findViewById(R.id.btnBackNotificationSettings).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean prefApproved = prefs.getBoolean(KEY_APPROVED, true);
        boolean prefCollection = prefs.getBoolean(KEY_COLLECTION, true);
        boolean prefSector = prefs.getBoolean(KEY_SECTOR, true);

        switchApproved.setChecked(prefApproved);
        switchCollection.setChecked(prefCollection);
        switchSector.setChecked(prefSector);

        switchApproved.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(KEY_APPROVED, checked).apply());

        switchCollection.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(KEY_COLLECTION, checked).apply());

        switchSector.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(KEY_SECTOR, checked).apply());

        // Permitir clicar em toda a linha para alternar o switch
        view.findViewById(R.id.rowNotifApproved).setOnClickListener(v ->
                switchApproved.setChecked(!switchApproved.isChecked()));

        view.findViewById(R.id.rowNotifCollection).setOnClickListener(v ->
                switchCollection.setChecked(!switchCollection.isChecked()));

        view.findViewById(R.id.rowNotifSector).setOnClickListener(v ->
                switchSector.setChecked(!switchSector.isChecked()));
    }
}
