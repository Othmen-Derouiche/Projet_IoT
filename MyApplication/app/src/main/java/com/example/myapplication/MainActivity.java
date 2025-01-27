package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.SharedPreferences;
import androidx.appcompat.widget.Toolbar;


import android.view.Menu;
import android.view.MenuItem;

import java.util.HashMap;

//SharedPreferences est une API utilisée pour sauvegarder des données persistantes sous forme de paires clé-valeur.

public class MainActivity extends AppCompatActivity {

    //*********************************************************************************************************

    private HashMap<String, Double> lastLightValues = new HashMap<>(); // Stocke les dernières valeurs de luminosité par mote
    private HashMap<String, Boolean> lightStatus = new HashMap<>(); // Stocke l'état de la lumière (allumée/éteinte) par mote
    private ToggleButton toggleButton;
    private TextView tv2;
    private TextView allDataTextView;
    private Button clearButton; // Référence au bouton Clear
    private BroadcastReceiver dataReceiver;
    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_START_AT_BOOT = "startAtBoot";
    private ScrollView scrollView;

    //*********************************************************************************************************

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "MainActivity created");

        lightStatus = new HashMap<>();
        lastLightValues = new HashMap<>();
        // Configurer la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        clearButton = findViewById(R.id.clearButton);

        // Écouteur de clic pour le bouton Clear
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Effacer le contenu du TextView
                allDataTextView.setText("Waiting for sensors measurements");
                Log.d("MainActivity", "ScrollView content cleared");
            }
        });
        // Find views
        tv2 = findViewById(R.id.tv2);
        toggleButton = findViewById(R.id.btn2);
        tv2.setText("Intially Stopped"); // Set default text for TV2


        // Charger l'état sauvegardé de l'option "Start at boot"
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean startAtBoot = preferences.getBoolean(KEY_START_AT_BOOT, false);
        toggleButton.setChecked(startAtBoot); // Synchroniser l'état du ToggleButton

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Toggle is ON: Start service
                    tv2.setText("Running");
                    Log.d("MainActivity", "MainActivity starts the MainService");
                    startService(new Intent(getApplicationContext(), MainService.class));
                } else {
                    // Toggle is OFF: Stop service
                    tv2.setText("Stopped");
                    Log.d("MainActivity", "MainActivity stops the MainService");
                    stopService(new Intent(getApplicationContext(), MainService.class));
                }
                Log.d("MainActivity", "ToggleButton state changed: " + isChecked);
                // Sauvegarder l'état dans SharedPreferences
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(KEY_START_AT_BOOT, isChecked);
                editor.apply();
            }
        });
//*********************************************************************************************************
        allDataTextView = findViewById(R.id.allDataTextView);
        // Auto-scroll to top (since newest entries are at the top)
        scrollView = findViewById(R.id.scroll);
        // Set up the BroadcastReceiver
        dataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.tp1.RESULT".equals(intent.getAction())) {
                    String data = intent.getStringExtra("data");
                    // Effacer le texte par défaut si nécessaire
                    if (allDataTextView.getText().toString().equals("Waiting for sensors measurements")) {
                        allDataTextView.setText(""); // Effacer le texte par défaut
                    }
                    if (data != null) {

                        // Extraire les données des motes
                        String[] entries = data.split("\n\n"); // Séparer les entrées par mote
                        for (String entry : entries) {
                            String[] lines = entry.split("\n"); // Séparer les lignes de l'entrée

                            String moteId = "";
                            double lightValue = 0;
                            String timestamp = "";

                            for (String line : lines) {
                                if (line.startsWith("Mote ID:")) {
                                    moteId = line.replace("Mote ID:", "").trim();
                                } else if (line.startsWith("Luminosité:")) {
                                    lightValue = Double.parseDouble(line.replace("Luminosité:", "").replace(" lux", "").trim());
                                } else if (line.startsWith("Timestamp:")) {
                                    timestamp = line.replace("Timestamp:", "").trim();
                                }
                            }

                            if (!moteId.isEmpty()) {
                                // Récupérer l'état précédent de la mote (etteinte par défaut si non défini)
                                boolean isLightOn = lightStatus.getOrDefault(moteId, true);

                                if (lastLightValues.containsKey(moteId)) {
                                    double lastValue = lastLightValues.get(moteId);

                                    // Vérifier une augmentation de 80 % ou plus
                                    if (lightValue >= lastValue * 1.8) {
                                        isLightOn = true;
                                    }
                                    // Vérifier une diminution de 80 % ou plus
                                    else if (lightValue <= lastValue * 0.2) {
                                        isLightOn = false;
                                    }
                                    // Si la variation est inférieure à 80 %, conserver l'état précédent
                                } else {
                                    isLightOn = false; // Éteint par défaut
                                }

                                // Mettre à jour les structures de données
                                lastLightValues.put(moteId, lightValue);
                                lightStatus.put(moteId, isLightOn);

                                // Afficher l'état de la lumière, le mote ID, la luminosité et le timestamp dans allDataTextView
                                String lightStatusText = isLightOn ? "🟢" : "🔴"; // Icône vert (allumé) ou rouge (éteint)
                                allDataTextView.append(lightStatusText + " Mote ID: " + moteId + "\nLuminosité: " + lightValue + " lux\nTimestamp: " + timestamp + "\n\n");
                            }
                        }
                        //allDataTextView.append(data);
                        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                        allDataTextView.append("----------------------------\n");
                    }
                }
            }
        };

        // Register the receiver
        IntentFilter filter = new IntentFilter("com.example.tp1.RESULT");
        registerReceiver(dataReceiver, filter, Context.RECEIVER_EXPORTED);

        //***********************************************************************************************************
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Gérer les clics sur les éléments de l'action bar
        int id = item.getItemId();

        // Si l'utilisateur clique sur "action_settings", lancer SettingsActivity
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}