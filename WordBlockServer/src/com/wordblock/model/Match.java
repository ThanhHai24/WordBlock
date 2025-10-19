package com.wordblock.model;
public class Match {
    private int id, player1Id, player2Id, score1, score2;
    public Match() {}
    public Match(int p1,int p2){player1Id=p1;player2Id=p2;}
    public int getId(){return id;} public void setId(int v){id=v;}
    public int getPlayer1Id(){return player1Id;} public void setPlayer1Id(int v){player1Id=v;}
    public int getPlayer2Id(){return player2Id;} public void setPlayer2Id(int v){player2Id=v;}
    public int getScore1(){return score1;} public void setScore1(int v){score1=v;}
    public int getScore2(){return score2;} public void setScore2(int v){score2=v;}
}
