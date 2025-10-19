package com.wordblock.model;
public class User {
    private int id; private String username; private String passwordHash;
    private int totalPoints; private String status;
    public User() {}
    public User(String u, String pHash) { this.username=u; this.passwordHash=pHash; }
    public int getId(){return id;} public void setId(int v){id=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public int getTotalPoints(){return totalPoints;} public void setTotalPoints(int v){totalPoints=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
