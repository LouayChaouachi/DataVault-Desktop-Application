package com.example.lol;

import java.util.Date;

public class Compte {
    private int id;
    private double solde;
    private String type;
    private Date date_ouverture;
    private int clientId;

    public Compte(int id, double solde, String type, Date date_ouverture, int clientId) {
        this.id = id;
        this.solde = solde;
        this.type = type;
        this.date_ouverture = date_ouverture;
        this.clientId = clientId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getSolde() { return solde; }
    public void setSolde(double solde) { this.solde = solde; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Date getDate_ouverture() { return date_ouverture; }
    public void setDate_ouverture(Date date_ouverture) { this.date_ouverture = date_ouverture; }
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }
}