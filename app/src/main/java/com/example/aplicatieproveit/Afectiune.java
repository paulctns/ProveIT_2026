package com.example.aplicatieproveit;

import java.util.Map;

public class Afectiune {
    public String id; // Îl vom seta noi manual după ce citim din baza de date
    public String nume;
    public String departament;

    // Folosim long pentru că Firebase salvează int64
    public long prioritate;
    public double probabilitate_generala;

    // Dicționarul cu simptome și ponderi (ex: "durere_piept" -> 0.9)
    public Map<String, Double> simptome;

    // Firebase are nevoie obligatoriu de un constructor gol
    public Afectiune() {}
}