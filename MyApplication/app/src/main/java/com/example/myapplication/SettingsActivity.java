package com.example.myapplication;

import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import java.util.Calendar;

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Charger les préférences depuis le fichier XML
        addPreferencesFromResource(R.xml.preferences);

        // Mettre à jour les summaries pour chaque EditTextPreference
        updateSummaries();
    }
    private void updateSummaries() {
        // Récupérer les préférences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Mettre à jour le summary pour chaque EditTextPreference
        updateSummary("notification_start_time", sharedPreferences);
        updateSummary("notification_end_time", sharedPreferences);
        updateSummary("email_weekend_start_time", sharedPreferences);
        updateSummary("email_weekend_end_time", sharedPreferences);
        updateSummary("email_week_start_time", sharedPreferences);
        updateSummary("email_week_end_time", sharedPreferences);
        updateSummary("email_address", sharedPreferences);
    }

    private void updateSummary(String key, SharedPreferences sharedPreferences) {
        // Récupérer la préférence
        EditTextPreference preference = (EditTextPreference) findPreference(key);

        if (preference != null) {
            // Récupérer la valeur actuelle
            String value = sharedPreferences.getString(key, "");

            // Mettre à jour le summary avec la valeur
            preference.setSummary(value);

            // Écouter les changements de valeur
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Mettre à jour le summary avec la nouvelle valeur
                    preference.setSummary(newValue.toString());
                    //showTimePickerDialog(preference);
                    return true;
                }
            });
        }
    }
    private void showTimePickerDialog(Preference preference) {
        // Récupérer l'heure actuelle
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Créer un TimePickerDialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        // Formater l'heure sélectionnée
                        String time = String.format("%02d:%02d", hourOfDay, minute);

                        // Mettre à jour la préférence
                        ((EditTextPreference) preference).setText(time);
                        preference.setSummary(time);
                    }
                },
                hour,
                minute,
                DateFormat.is24HourFormat(this)
        );

        // Afficher le TimePickerDialog
        timePickerDialog.show();
    }
}