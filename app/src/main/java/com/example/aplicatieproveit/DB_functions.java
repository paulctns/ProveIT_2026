package com.example.aplicatieproveit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DB_functions {
    /**
     * Emulator (AVD): folosește mereu 10.0.2.2 — este alias către localhost-ul PC-ului tău.
     * Telefon fizic (același Wi‑Fi cu PC): pune aici IPv4 din ipconfig (ex. 192.168.1.100), NU localhost.
     */
    private static final String IP = "10.0.2.2";

    private static final String PORT = "3306";
    private static final String DB_NAME = "healthcare_app";

    private static final String USER = "admin_app";
    private static final String PASS = "agartha69";

    private static final String URL = "jdbc:mysql://" + IP + ":" + PORT + "/" + DB_NAME
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private Connection getConnection() throws SQLException {
        try {
            // Încercăm ambele variante de driver pentru siguranță
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                Class.forName("com.mysql.jdbc.Driver");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // ==========================================
    // 1. ZONA PACIENȚILOR
    // ==========================================

    // Verificare dacă un CNP este deja înregistrat
    public boolean isCnpRegistered(String cnp) {
        String query = "SELECT cnp FROM patients WHERE cnp = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, cnp);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Verificare dacă un Email este deja înregistrat
    public boolean isEmailRegistered(String email) {
        String query = "SELECT email FROM patients WHERE email = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Obținere email pacient după CNP
    public String getPatientEmail(String cnp) {
        String query = "SELECT email FROM patients WHERE cnp = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, cnp);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("email");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Înregistrare doar datele de bază ale pacientului (fără istoric medical)
    public boolean registerPatientBasic(String cnp, String fName, String lName, String series, String number, String email, String passHash, int age) {
        String query = "INSERT INTO patients (cnp, first_name, last_name, id_card_series, id_card_number, email, password_hash, age) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, cnp);
            stmt.setString(2, fName);
            stmt.setString(3, lName);
            stmt.setString(4, series);
            stmt.setString(5, number);
            stmt.setString(6, email);
            stmt.setString(7, passHash);
            stmt.setInt(8, age);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Inregistrare Pacient Nou (cu Istoric Medical inclus)
    public boolean registerPatient(String cnp, String fName, String lName, String series, String number, String email, String passHash, int age,
                                   String bloodType, String allergies, String chronicDiseases, String medications) {
        String queryPatient = "INSERT INTO patients (cnp, first_name, last_name, id_card_series, id_card_number, email, password_hash, age) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String queryHistory = "INSERT INTO medical_history (patient_cnp, blood_group, allergies, affections, treatment_taken) VALUES (?, ?, ?, ?, ?)";
        
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Începem o tranzacție

            // 1. Inserăm în tabela patients
            try (PreparedStatement stmt1 = conn.prepareStatement(queryPatient)) {
                stmt1.setString(1, cnp);
                stmt1.setString(2, fName);
                stmt1.setString(3, lName);
                stmt1.setString(4, series);
                stmt1.setString(5, number);
                stmt1.setString(6, email);
                stmt1.setString(7, passHash);
                stmt1.setInt(8, age);
                stmt1.executeUpdate();
            }

            // 2. Inserăm în tabela medical_history
            try (PreparedStatement stmt2 = conn.prepareStatement(queryHistory)) {
                stmt2.setString(1, cnp);
                stmt2.setString(2, bloodType);
                stmt2.setString(3, allergies);
                stmt2.setString(4, chronicDiseases);
                stmt2.setString(5, medications);
                stmt2.executeUpdate();
            }

            conn.commit(); // Salvăm ambele inserări
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Dacă una eșuează, dăm înapoi tot
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Login Pacient (Returnează CNP-ul dacă e cu succes, sau null dacă eșuează)
    public String loginPatientcuMailandParola(String email, String passHash) {
        String query = "SELECT cnp FROM patients WHERE email = ? AND password_hash = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            stmt.setString(2, passHash);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("cnp"); // Login reușit, returnăm CNP-ul pentru a-l folosi mai departe
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String loginPatientDateBuletin(String cnp, String serie, String numar) {
        String query = "SELECT cnp FROM patients WHERE cnp = ? AND id_card_series = ? AND id_card_number = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, cnp);
            stmt.setString(2, serie);
            stmt.setString(3, numar);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("cnp");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ==========================================
    // 2. ZONA DE URGENȚE ȘI AI
    // ==========================================

    // Trimitere cerere de urgență (Apăsare Buton Roșu/Galben/Verde)
    public boolean sendEmergencyRequest(String patientCnp, String priorityColor, boolean isForSelf, String description, boolean needsAmbulance, int estimatedTime) {
        String query = "INSERT INTO emergency_requests (patient_cnp, priority_color, is_for_self, description, needs_ambulance, estimated_time) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, patientCnp);
            stmt.setString(2, priorityColor); // 'red', 'yellow' sau 'green'
            stmt.setBoolean(3, isForSelf);
            stmt.setString(4, description);
            stmt.setBoolean(5, needsAmbulance);
            stmt.setInt(6, estimatedTime);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Salvare rezultate de la AI
    public boolean saveAITriageResult(int requestId, String keywords, String specialty, int severityScore) {
        String query = "INSERT INTO ai_triage_results (request_id, extracted_keywords, recommended_specialty, severity_score) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, requestId);
            stmt.setString(2, keywords);
            stmt.setString(3, specialty);
            stmt.setInt(4, severityScore);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==========================================
    // 3. ZONA OPERATORILOR (MEDICI)
    // ==========================================

    // Verificare dacă un CNP de Operator este deja înregistrat
    public boolean isOperatorCnpRegistered(String cnp) {
        String query = "SELECT cnp FROM operators WHERE cnp = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, cnp);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Verificare dacă un Email de Operator este deja înregistrat
    public boolean isOperatorEmailRegistered(String email) {
        String query = "SELECT email_institutional FROM operators WHERE email_institutional = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Înregistrare Operator (Medic) nou
    public boolean registerOperator(String cnp, String fName, String lName, String series, String number, 
                                    String email, String passHash, Integer hospitalId, String specialty, int codParafa) {
        String query = "INSERT INTO operators (cnp, first_name, last_name, id_card_series, id_card_number, email_institutional, password_hash, hospital_id, specialty, cod_parafa, is_available) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, cnp);
            stmt.setString(2, fName);
            stmt.setString(3, lName);
            stmt.setString(4, series);
            stmt.setString(5, number);
            stmt.setString(6, email);
            stmt.setString(7, passHash);
            if (hospitalId != null) {
                stmt.setInt(8, hospitalId);
            } else {
                stmt.setNull(8, java.sql.Types.INTEGER);
            }
            stmt.setString(9, specialty);
            stmt.setInt(10, codParafa);
            stmt.setInt(11, 1); // is_available = true by default

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Actualizare status disponibilitate medic
    public boolean updateOperatorAvailability(String cnp, boolean isAvailable) {
        String query = "UPDATE operators SET is_available = ? WHERE cnp = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, isAvailable);
            stmt.setString(2, cnp);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Obținere nume complet operator
    public String getOperatorName(String cnp) {
        String query = "SELECT first_name, last_name FROM operators WHERE cnp = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, cnp);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return "Dr. " + rs.getString("first_name") + " " + rs.getString("last_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Medic";
    }

    // Obținere specialitate operator
    public String getOperatorSpecialty(String cnp) {
        String query = "SELECT specialty FROM operators WHERE cnp = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, cnp);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("specialty");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Generalist";
    }

    // Login Operator
    public String loginOperator(String email, String passHash) {
        String query = "SELECT cnp FROM operators WHERE email_institutional = ? AND password_hash = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            stmt.setString(2, passHash);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("cnp");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String loginOperatorcuDateBuletin(String cnp, String serie, String numar) {
        String query = "SELECT cnp FROM operators WHERE cnp = ? AND id_card_series = ? AND id_card_number = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, cnp);
            stmt.setString(2, serie);
            stmt.setString(3, numar);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("cnp");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Actualizare status cerere de către medic (ex: din 'waiting' în 'in_operation' sau 'completed')
    public boolean updateRequestStatus(int queueId, String newStatus) {
        String query = "UPDATE operation_queue SET status = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newStatus); // 'waiting', 'in_operation', 'completed'
            stmt.setInt(2, queueId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}