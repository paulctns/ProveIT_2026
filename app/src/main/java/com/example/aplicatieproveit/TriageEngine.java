package com.example.aplicatieproveit;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TriageEngine {

    public interface AICallback {
        void onSuccess(String diagnostic_ai, String departament_ai, long prioritate, double probabilitate);
        void onError(String mesajEroare);
    }

    // ==========================================
    // STRUCTURILE DE DATE PENTRU NAIVE BAYES
    // ==========================================
    private static boolean modelAntrenat = false;
    private static int totalInregistrari = 0;

    // De câte ori apare o boală: [Boala -> Număr]
    private static Map<String, Integer> countBoli = new HashMap<>();

    // De câte ori apare un simptom specific pentru o anumită boală: [Boala -> [Simptom -> [Valoare -> Număr]]]
    private static Map<String, Map<String, Map<Integer, Integer>>> countSimptome = new HashMap<>();

    // Maparea departamentelor
    private static Map<String, String> departamente = new HashMap<>();
    private static Map<String, Long> prioritati = new HashMap<>();

    // ==========================================
    // 1. DESCĂRCAREA DATELOR ȘI ANTRENAMENTUL
    // ==========================================
    public static void descarcaSiAntreneaza(String ipLaptop) {
        if (modelAntrenat) return; // Deja a învățat

        String url = "http://" + ipLaptop + ":5000/get_dataset";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("NAIVE_BAYES", "Eroare descărcare date: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String jsonString = response.body().string();
                        JSONObject root = new JSONObject(jsonString);

                        JSONArray dataset = root.getJSONArray("dataset");
                        JSONObject mapare = root.getJSONObject("mapare");

                        // Antrenăm modelul (Numărăm frecvențele)
                        antreneazaNaiveBayesLocal(dataset, mapare);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private static void antreneazaNaiveBayesLocal(JSONArray dataset, JSONObject mapare) throws JSONException {
        countBoli.clear();
        countSimptome.clear();
        departamente.clear();
        prioritati.clear();
        totalInregistrari = dataset.length();

        // 1. Încărcăm maparea spitalului
        JSONArray boliMapare = mapare.names();
        if (boliMapare != null) {
            for (int i = 0; i < boliMapare.length(); i++) {
                String boala = boliMapare.getString(i);
                JSONObject detalii = mapare.getJSONObject(boala);
                departamente.put(boala, detalii.getString("departament"));
                prioritati.put(boala, detalii.getLong("prioritate"));
            }
        }

        // 2. Parcurgem Excel-ul (dataset-ul) și numărăm pentru teorema lui Bayes
        String[] features = {"Fever", "Cough", "Fatigue", "Difficulty Breathing", "Age", "Gender", "Blood Pressure", "Cholesterol Level"};

        for (int i = 0; i < dataset.length(); i++) {
            JSONObject rand = dataset.getJSONObject(i);
            String boala = rand.getString("Disease");

            // Numărăm probabilitatea bolii: P(Boala)
            countBoli.put(boala, countBoli.getOrDefault(boala, 0) + 1);

            // Structura pentru a număra simptomele
            if (!countSimptome.containsKey(boala)) {
                countSimptome.put(boala, new HashMap<>());
                for (String f : features) countSimptome.get(boala).put(f, new HashMap<>());
            }

            // Numărăm probabilitatea simptomului: P(Simptom | Boala)
            for (String f : features) {
                int valoare = rand.getInt(f);
                Map<Integer, Integer> featureVals = countSimptome.get(boala).get(f);
                featureVals.put(valoare, featureVals.getOrDefault(valoare, 0) + 1);
            }
        }

        modelAntrenat = true;
        Log.d("NAIVE_BAYES", "Model antrenat cu succes pe " + totalInregistrari + " cazuri!");
    }

    // ==========================================
    // 2. PREDICȚIA LOCALĂ (NAIVE BAYES ALGORITHM)
    // ==========================================
    public static void evalueazaCuAI(
            boolean fever, boolean cough, boolean fatigue, boolean breathing,
            int age, int gender, int bloodPressure, int cholesterol,
            AICallback callback) {

        if (!modelAntrenat) {
            callback.onError("Modelul se descarcă încă. Te rog mai încearcă în câteva secunde.");
            return;
        }

        // Transformăm vârsta în categorii exact cum a făcut Python
        int ageCat = age < 30 ? 0 : (age <= 50 ? 1 : 2);

        Map<String, Integer> pacientCurent = new HashMap<>();
        pacientCurent.put("Fever", fever ? 1 : 0);
        pacientCurent.put("Cough", cough ? 1 : 0);
        pacientCurent.put("Fatigue", fatigue ? 1 : 0);
        pacientCurent.put("Difficulty Breathing", breathing ? 1 : 0);
        pacientCurent.put("Age", ageCat);
        pacientCurent.put("Gender", gender);
        pacientCurent.put("Blood Pressure", bloodPressure);
        pacientCurent.put("Cholesterol Level", cholesterol);

        String boalaPrezisa = "Necunoscut";
        double maxProbabilitate = Double.NEGATIVE_INFINITY;

        // FORMULA NAIVE BAYES (Logaritmică pentru a preveni numerele prea mici)
        for (String boala : countBoli.keySet()) {

            // 1. P(C) - Probabilitatea Apriorică a bolii
            double pBoala = (double) countBoli.get(boala) / totalInregistrari;
            double scorLogaritmic = Math.log(pBoala);

            // 2. P(x | C) - Înmulțim probabilitățile condiționate
            for (Map.Entry<String, Integer> simptom : pacientCurent.entrySet()) {
                String numeSimptom = simptom.getKey();
                int valoarePacient = simptom.getValue();

                int aparitii = 0;
                if (countSimptome.containsKey(boala) && countSimptome.get(boala).containsKey(numeSimptom)) {
                    aparitii = countSimptome.get(boala).get(numeSimptom).getOrDefault(valoarePacient, 0);
                }

                // Corecția Laplace (pentru a evita înmulțirea cu 0)
                double probSimptom = (double) (aparitii + 1) / (countBoli.get(boala) + 3);

                scorLogaritmic += Math.log(probSimptom);
            }

            // Găsim boala cu scorul maxim
            if (scorLogaritmic > maxProbabilitate) {
                maxProbabilitate = scorLogaritmic;
                boalaPrezisa = boala;
            }
        }

        // 3. Extragem departamentul și urgența
        String departamentFinal = departamente.getOrDefault(boalaPrezisa, "Triaj General");
        long prioritateFinala = prioritati.getOrDefault(boalaPrezisa, 3L);

        // Modelul a terminat predicția locală! (Fără a mai face apel de rețea)
        callback.onSuccess(boalaPrezisa, departamentFinal, prioritateFinala, 85.0);
    }
}