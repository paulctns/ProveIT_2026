package com.example.aplicatieproveit;

import java.util.List;
import java.util.Map;

public class TriageEngine {

    public static double calculeazaScor(Afectiune boala, List<String> simptomePacient, List<String> istoricMedical) {
        // Pornim cu probabilitatea de bază a bolii
        double scor = boala.probabilitate_generala;

        if (boala.simptome == null) return 0.0;

        int simptomeGasite = 0;

        for (Map.Entry<String, Double> entry : boala.simptome.entrySet()) {
            String sBaza = entry.getKey();
            double pondere = entry.getValue();

            // Aplicăm boost pentru istoric medical
            if (istoricMedical != null && istoricMedical.contains(sBaza)) {
                pondere = Math.min(0.99, pondere * 1.3);
            }

            if (simptomePacient.contains(sBaza)) {
                // Dacă simptomul se potrivește, scorul crește
                scor *= (pondere * 10);
                simptomeGasite++;
            } else {
                // Penalizare blândă pentru simptome lipsă
                scor *= 0.8;
            }
        }

        // Dacă nu am găsit niciun simptom care să aparțină bolii, scorul e 0
        return (simptomeGasite > 0) ? scor : 0.0;
    }
}