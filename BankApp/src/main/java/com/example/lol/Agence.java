package com.example.lol;

public class Agence {
    private int id;
    private String nom;
    private String adresse;
    private String email;
    private int numAgence; // New field
    private String responsable; // New field

    public Agence(int id, String nom, String adresse, String email, int numAgence, String responsable) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.email = email;
        this.numAgence = numAgence;
        this.responsable = responsable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getNumAgence() {
        return numAgence;
    }

    public void setNumAgence(int numAgence) {
        this.numAgence = numAgence;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }
}