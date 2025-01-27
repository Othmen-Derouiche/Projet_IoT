package com.example.myapplication;
import android.content.Context;
import android.os.Vibrator;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.JsonReader;
import android.util.Log;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.HashMap;
import java.util.Locale;

import java.util.Timer;
import java.util.TimerTask;
import android.os.AsyncTask;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;





public class MainService extends Service {

    private Timer timer;
    private static final String URL_STRING = "http://iotlab.telecomnancy.eu:8080/iotlab/rest/data/1/light1/last";
    private HashMap<String, Boolean> lightStatus ; // Stocke l'état de la lumière (allumée/éteinte) par mote
    private HashMap<String, Double> lastLightValues; // Stocke les dernières valeurs de luminosité par mote
    private int currentPeriod = 1; // Compteur de périodes
    private SharedPreferences sharedPreferences;
    private Vibrator vibrator;
    private static class BackgroundTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("AsyncTask", "Tache en Background éxecutée pour le test");
            return null;
        }
    }

    public MainService() {
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MainService", "Service created");

        // Initialiser les HashMap qui stockent les états des motes
        lastLightValues = new HashMap<>();
        lightStatus = new HashMap<>();
        // lancer la tache background
        new BackgroundTask().execute();
        // Récupérer les préférences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Récupérer l'instance du vibreur
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Initialize and start the timer
        timer = new Timer();
        scheduleTask();
    }

/***********************************************************************************************
/*                                                                                            **
/*     Planifier une tâche pour qu'elle s'exécute périodiquement, avec un délai de 60         **
/*     secondes entre la fin d'une exécution et le début de la suivante.                      **
/*                                                                                            **
/**********************************************************************************************/
    private void scheduleTask() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d("MainService", "Periodic task executed");
                //fetchData();
                simulateData();
                // Reschedule the task after completion
                scheduleTask();
            }
        }, 30000); // Delay for 30 seconds before running the task
    }
/***********************************************************************************************
/*                                                                                            **
/*                  simuler d'une facon cyclique les données des capteurs                     **
/*                                                                                            **
/**********************************************************************************************/
    private void simulateData() {
        // Données de base pour les 3 motes
        String[] moteIds = {"9.138", "32.131", "53.105"};
        double[][] periodValues = {
                {100, 200, 100},    // Période 1
                {180, 300, 200},    // Période 2  --> 1 & 3 allumées
                {120, 200, 200},    // Période 3  --> 1 & 3 allumées
                {100, 380, 180},    // Période 4  --> 1 & 2 & 3 allumées
                {10, 70, 30}        // Période 5
        };

        // Vérifier si on dépasse la 5ème période
        if (currentPeriod > 5) {
            currentPeriod = 1; // Réinitialiser le compteur
        }

        // Récupérer les valeurs pour la période actuelle
        double[] lightValues = periodValues[currentPeriod - 1];

        // Construire la réponse JSON simulée
        StringBuilder simulatedResponse = new StringBuilder();
        simulatedResponse.append("{\"data\":[");

        for (int i = 0; i < moteIds.length; i++) {
            String moteId = moteIds[i];
            double lightValue = lightValues[i];

            // Ajouter les données du mote à la réponse simulée
            simulatedResponse.append("{\"timestamp\":").append(System.currentTimeMillis()).append(",");
            simulatedResponse.append("\"label\":\"light1\",");
            simulatedResponse.append("\"value\":").append(lightValue).append(",");
            simulatedResponse.append("\"mote\":\"").append(moteId).append("\"}");

            if (i < moteIds.length - 1) {
                simulatedResponse.append(",");
            }
        }

        simulatedResponse.append("]}");

        // Appeler parseAndDisplayJSON avec la réponse simulée
        parseAndDisplayJSON(simulatedResponse.toString());

        // Passer à la période suivante
        currentPeriod++;
    }
/***********************************************************************************************
/*                                                                                            **
/*                                                                                            **
/*                                                                                            **
/**********************************************************************************************/
    private void fetchData() {
        try {
            URL url = new URL(URL_STRING);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            Log.d("MainService", "HTTP Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Log the response and parse JSON
                Log.d("MainService", "Response: " + response);
                parseAndDisplayJSON(response.toString());
            } else {
                // Show an error message using Toast
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(MainService.this, "Error: " + responseCode, Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            Log.e("MainService", "Error fetching data", e);
        }
    }
/***********************************************************************************************
/*                                                                                            **
/*                                                                                            **
/*                                                                                            **
/**********************************************************************************************/
    private void parseAndDisplayJSON(String json) {
        try {
            JsonReader jsonReader = new JsonReader(new StringReader(json));

            StringBuilder parsedData = new StringBuilder();
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if (name.equals("data")) {
                    jsonReader.beginArray();
                    while (jsonReader.hasNext()) {
                        jsonReader.beginObject();
                        String moteId = "";
                        String light = "";
                        String timestamp = "";
                        while (jsonReader.hasNext()) {
                            String key = jsonReader.nextName();
                            switch (key) {
                                case "mote":
                                    moteId = jsonReader.nextString();
                                    break;
                                case "value":
                                    light = jsonReader.nextString();
                                    break;
                                case "timestamp":
                                    timestamp = jsonReader.nextString();
                                    break;
                                default:
                                    jsonReader.skipValue();
                                    break;
                            }
                        }
                        jsonReader.endObject();

                        // Formater les données
                        if (!moteId.isEmpty()) {

                            double lightValue = Double.parseDouble(light);

                            // Simuler une variation aléatoire de la luminosité (50 % de chance d'augmenter ou de diminuer)
                            //lightValue = simulateRandomLightVariation(lightValue);

                            parsedData.append("Mote ID: ").append(moteId).append("\n");
                            parsedData.append("Luminosité: ").append(light).append(" lux\n");
                            parsedData.append("Timestamp: ").append(formatTimestamp(timestamp)).append("\n\n");

                            // Vérifier les changements d'état
                            checkLightStatus(moteId, lightValue);
                        }
                    }
                    jsonReader.endArray();
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();

            // Envoyer les données parsées à MainActivity
            Intent intent = new Intent("com.example.tp1.RESULT");
            intent.putExtra("data", parsedData.toString());
            sendBroadcast(intent);

            // Log les données parsées
            Log.d("MainService", "Parsed Data: " + parsedData.toString());
        } catch (Exception e) {
            Log.e("MainService", "Error parsing JSON", e);
        }
    }
/***********************************************************************************************
/*                                                                                            **
/*                    Générer un format de temps lisible par l'utilisateur                    **
/*                                                                                            **
/**********************************************************************************************/
    private String formatTimestamp(String timestamp) {
        try {
            long time = Long.parseLong(timestamp);
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    .format(new Date(time));
        } catch (NumberFormatException e) {
            return "Invalid timestamp";
        }
    }
/***********************************************************************************************
/*                                                                                            **
/*                                                                                            **
/*                                                                                            **
/**********************************************************************************************/
    private void checkLightStatus(String moteId, double lightValue) {

        boolean wasLightOn = lightStatus.getOrDefault(moteId, false);
        boolean isLightOn = false;

        // Récupérer la dernière valeur de luminosité
        double lastValue = lastLightValues.getOrDefault(moteId, lightValue);

        // Détecter une augmentation de 80 %
        if (lightValue >= lastValue * 1.8) {
            isLightOn = true;
        }
        // Détecter une diminution de 80 %
        else if (lightValue <= lastValue * 0.2) {
            isLightOn = false;
        }

        // Vérifier si l'état a changé
        // émettre une notification si une nouvelle lumière vient d'être allumée
        if (isLightOn != wasLightOn && isLightOn ) {
            lightStatus.put(moteId, isLightOn);

            if (vibrator != null && vibrator.hasVibrator()) {
                Log.d("MainService", "Déclenchement de la vibration pour le capteur " + moteId);
                vibrator.vibrate(2000); // 500 millisecondes
                Log.d("MainService", "Vibration terminée pour le capteur " + moteId);
            } else {
                Log.e("MainService", "Le vibreur n'est pas disponible sur cet appareil.");
            }

            // Envoyer une notification si l'heure est dans un intervalle à régler
            if (isWithinNotificationTimeRange()) { //
                Log.d("MainService", "Envoi de la notification pour le capteur " + moteId);
                sendNotification(moteId, isLightOn);
                Log.d("MainService", "Notification envoyée pour le capteur " + moteId);
            }

            // Envoyer un email si la lumière est allumée en dehors des heures de travail
            if (isOutsideWorkingHours()) {
                String recipient = sharedPreferences.getString("email_address", "othmene.derouich123@gmail.com");
                String subject = "Alerte Lumière Allumée";
                String body = "La lumière du capteur " + moteId + " a été allumée.";
                Log.d("MainService", "Envoi d'un e-mail à " + recipient + " pour le capteur " + moteId);
                sendEmail(recipient, subject, body);
                Log.d("MainService", "E-mail envoyé à " + recipient + " pour le capteur " + moteId);
            }
        }
        // Mettre à jour la dernière valeur de luminosité
        lastLightValues.put(moteId, lightValue);
    }
/***********************************************************************************************
/*                                                                                            **
/*                                                                                            **
/*                                                                                            **
/**********************************************************************************************/
    @SuppressLint("IntentReset")
    private void sendEmail(String recipient, String subject, String body) {
        // Créer une Intent avec l'action ACTION_SEND
        Intent emailIntent = new Intent(Intent.ACTION_SEND);

        // Spécifier le type de données (email)
        emailIntent.setData(Uri.parse("mailto:")); // Indique que l'Intent est destinée à envoyer un email
        emailIntent.setType("text/plain"); // Type de données : texte brut

        // Ajouter les paramètres de l'email
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient}); // Destinataire
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject); // Objet
        emailIntent.putExtra(Intent.EXTRA_TEXT, body); // Corps du message

        // Créer un Chooser pour permettre à l'utilisateur de choisir une application de messagerie
        Intent chooserIntent = Intent.createChooser(emailIntent, "Envoyer un email...");

        // Ajouter également le flag FLAG_ACTIVITY_NEW_TASK au Chooser
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Envoyer l'Intent et ouvrir une application de messagerie
        try {
            startActivity(chooserIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            // Gérer le cas où aucune application de messagerie n'est installée
            Toast.makeText(this, "Aucune application de messagerie trouvée.", Toast.LENGTH_SHORT).show();
        }
    }
    private boolean isOutsideWorkingHours() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        // Récupérer les plages horaires configurées pour les e-mails
        int emailWeekendStartTime = Integer.parseInt(sharedPreferences.getString("email_weekend_start_time", "19"));
        int emailWeekendEndTime = Integer.parseInt(sharedPreferences.getString("email_weekend_end_time", "23"));
        int emailWeekStartTime = Integer.parseInt(sharedPreferences.getString("email_week_start_time", "23"));
        int emailWeekEndTime = Integer.parseInt(sharedPreferences.getString("email_week_end_time", "6"));

        // Week-end ou semaine
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            // Week-end : vérifier si l'heure est entre emailWeekendStartTime et emailWeekendEndTime
            return hour >= emailWeekendStartTime && hour <= emailWeekendEndTime;
        } else {
            // Semaine : vérifier si l'heure est entre emailWeekStartTime et emailWeekEndTime
            if (emailWeekStartTime > emailWeekEndTime) {
                // Cas où la plage horaire traverse minuit (23h à 6h)
                return hour >= emailWeekStartTime || hour <= emailWeekEndTime;
            } else {
                // Cas où la plage horaire est dans la même journée
                return hour >= emailWeekStartTime && hour <= emailWeekEndTime;
            }
        }
    }
    private boolean isWithinNotificationTimeRange() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY); // Heure actuelle (format 24h)
        //return hour >= 12 && hour <= 23; // Vérifier si l'heure est entre 18h et 23h
        // Récupérer les plages horaires configurées
        int startTime = Integer.parseInt(sharedPreferences.getString("notification_start_time", "19"));
        int endTime = Integer.parseInt(sharedPreferences.getString("notification_end_time", "23"));

        return hour >= startTime && hour <= endTime;
    }

/***********************************************************************************************
/*                                                                                            **
/*                                                                                            **
/*                                                                                            **
/**********************************************************************************************/
    private void sendNotification(String moteId, boolean isLightOn) {

        // Créer un canal de notification (nécessaire pour Android 8.0 et supérieur)
        NotificationChannel channel = new NotificationChannel(
                "light_status_channel",
                "Light Status Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

        // Créer la notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "light_status_channel")
                .setSmallIcon(R.drawable.ic_lightbulb) // Icône de la notification
                .setContentTitle("Changement d'état de la lumière")
                .setContentText("Mote " + moteId + " : " + (isLightOn ? "Allumé" : "Éteint"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Afficher la notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(moteId.hashCode(), builder.build()); // Utiliser un ID unique pour chaque notification
    }

    //////////////////////////////////////////////////////
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MainService", "Service started");
        return START_STICKY; // Auto-restart service if it gets killed
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("MainService", "Service destroyed");

        // Cancel the timer
        // Arrête le timer lorsque le service est détruit pour libérer les ressources
        // The onDestroy method cancels the Timer to prevent tasks from continuing to execute
        // after the service is destroyed.
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {

        return null; // Not bound to an activity
    }
}