package com.example.lol;

public class Client {
    private int id;
    private String nom;
    private String prenom;
    private String adresse;
    private String mail;
    private String tel;
    private int agenceId;

    public Client(int id, String nom, String prenom, String adresse, String mail, String tel, int agenceId) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.adresse = adresse;
        this.mail = mail;
        this.tel = tel;
        this.agenceId = agenceId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }
    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }
    public int getAgenceId() { return agenceId; }
    public void setAgenceId(int agenceId) { this.agenceId = agenceId; }
}