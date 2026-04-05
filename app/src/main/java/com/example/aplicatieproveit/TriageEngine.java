package com.example.aplicatieproveit;

import java.util.List;
import java.util.Map;

public class TriageEngine {

    // Funcția matematică bazată pe Naive Bayes
    public static double calculeazaScor(Afectiune boala, List<String> simptomePacient) {

        // 1. Pornim cu probabilitatea de bază a bolii P(Boală)
        double scor = boala.probabilitate_generala;

        // Protecție în caz că boala nu are simptome trecute
        if (boala.simptome == null) return 0.0;

        // 2. Trecem prin TOATE simptomele pe care le cunoaște această boală
        for (Map.Entry<String, Double> entry : boala.simptome.entrySet()) {
            String simptomDinBazaDeDate = entry.getKey();
            double pondere = entry.getValue();

            // 3. Verificăm dacă pacientul ARE acest simptom
            if (simptomePacient.contains(simptomDinBazaDeDate)) {
                // Dacă îl are, înmulțim scorul cu ponderea simptomului P(Simptom|Boală)
                scor = scor * pondere;
            } else {
                // Dacă NU îl are, penalizăm scorul înmulțind cu inversul ponderii (1 - P)
                scor = scor * (1.0 - pondere);
            }
        }

        // Returnăm scorul final (matematic)
        return scor;
    }
}