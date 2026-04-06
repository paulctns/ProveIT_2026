# 🏥 VitalIS - Intelligent Medical Triage System

**VitalIS** este un ecosistem digital inovator conceput pentru a decongestiona Unitățile de Primiri Urgențe (UPU) și a eficientiza triajul medical folosind Inteligența Artificială, baze de date în timp real și modele matematice de probabilitate. 

Proiect dezvoltat în cadrul hackathonului **ProveIT 2026** de echipa **NeuralData**.

---

## ⚠️ Problema
În sistemul medical actual, pacienții se confruntă cu timpi de așteptare uriași la urgențe, triajul manual este predispus la erori umane în situații de stres maxim, iar spitalele află prea târziu detaliile critice ale unui pacient aflat în drum spre ei cu ambulanța.

## 💡 Soluția: Ecosistemul VitalIS
VitalIS rezolvă această problemă printr-o abordare hibridă (AI + Matematică + Cloud Data) și este structurat în 3 module interconectate în timp real:

1. **📱 Modulul Pacient:** Permite solicitarea rapidă a ambulanței (prin GPS) și preluarea simptomelor vocal sau prin text.
2. **📸 Modulul Vision AI (Scanare):** Dedicat paramedicilor, folosește OCR pentru a scana instantaneu un buletin, identifică pacientul în baza de date și îi recuperează istoricul medical (sau creează un profil "Guest Mode" temporar).
3. **💻 Modulul Medic (Dashboard):** Un panou de control live pentru spital, unde medicii pot vedea pacienții aflați pe drum, prioritatea lor (Roșu, Galben, Verde) și sala/medicul alocat automat.

---

## ⚙️ Arhitectură, Baze de Date și Pipeline Tehnic

Sistemul nostru NU lasă un simplu chatbot să pună diagnostice, ci se bazează pe un ecosistem de date bine structurat și un mecanism de **Safe Over-Triage**:

* **Integrare Baze de Date (Real-time):** Sistemul este conectat la o bază de date non-relațională (NoSQL) unde sunt stocate și interogate instantaneu profilurile pacienților, istoricul lor medical, resurselor spitalicești (săli, secții, medici de gardă) și cozile de așteptare.
* **Extracție date (NLP):** Folosim **Google Gemini AI** pentru a extrage și standardiza simptomele din limbajul natural al pacientului.
* **Triage Engine:** Simptomele sunt introduse într-un motor de probabilități matematice bazat pe **Teorema lui Naive Bayes**, care calculează scorul de gravitate extrăgând și corelând antecedentele medicale din baza de date.
* **Sincronizare:** Orice actualizare de status se reflectă instantaneu în spital prin baza de date cloud (Firestore).

**Mecanism de Siguranță:** Proiectul include un sistem de *Offline Fallback* în cazul în care serverele AI pică (limitări API 429), garantând că fluxul salvării nu este niciodată blocat.

---

## 🛠️ Tech Stack
* **Platformă:** Android (Java)
* **Baze de Date & Backend:** Firebase Firestore (Real-time Database), Firebase Authentication (Gestiune conturi)
* **Inteligență Artificială:** Google Gemini API (NLP & Vision OCR), Meta Llama 3.2 (Fallback)
* **Algoritmi implementați:** Naive Bayes Classifier

---

## 👥 Echipa NeuralData
* **Buga Simone** * **Chiuariu Silviu** * **Catinas Paul** *Timpul tău. Viața lor.*
