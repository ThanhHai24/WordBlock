package com.wordblock.game;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameSession {
    private final String roomId, playerA, playerB, letterPool;
    private final long durationMs;
    private final WordValidator validator;
    private final Map<String, Set<String>> submittedByPlayer = new ConcurrentHashMap<>();
    private final Map<String,Integer> scores = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private long endAt;
    private ScheduledExecutorService ses;

    public interface TickListener { void onTick(String roomId, int secLeft, Map<String,Integer> scores); void onEnd(String roomId, Map<String,Integer> finalScores); }
    private TickListener tickListener;

    public GameSession(String roomId, String a, String b, WordValidator validator, long durationMs, String letterPool){
        this.roomId=roomId; this.playerA=a; this.playerB=b; this.validator=validator; this.durationMs=durationMs; this.letterPool=letterPool;
        scores.put(a,0); scores.put(b,0);
    }
    public void setTickListener(TickListener l){ this.tickListener=l; }
    public String getRoomId(){return roomId;} public String getLetterPool(){return letterPool;}
    public boolean isRunning(){ return running.get(); }

    public void start(){
        running.set(true);
        endAt = System.currentTimeMillis()+durationMs;
        ses = Executors.newSingleThreadScheduledExecutor();
        ses.scheduleAtFixedRate(() -> {
            int left = (int)Math.max(0, (endAt-System.currentTimeMillis())/1000);
            if(tickListener!=null) tickListener.onTick(roomId,left, new HashMap<>(scores));
            if(left<=0){ stop(); }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public synchronized boolean submitWord(String user, String word){
        if(!running.get()) return false;
        String w = word.toLowerCase(Locale.ROOT).trim();
        int current = scores.getOrDefault(user, 0);
        
        submittedByPlayer.putIfAbsent(user, ConcurrentHashMap.newKeySet());
        Set<String> submitted = submittedByPlayer.get(user);
        // đã dùng -> ko trừ điểm
        if (submitted.contains(w)) {
            return false;
        }

        // Nếu không có trong dictionary -> trừ điểm
        if (!validator.isValid(w, letterPool)) {   // truyền thêm pool của game
            scores.put(user, current - 1);
            return false;
        }



        // Từ hợp lệ -> lưu và cộng điểm
        submitted.add(w);
        scores.put(user, current + 5);
        return true;
    }

    public synchronized Map<String,Integer> getScores(){ return new HashMap<>(scores); }

    public synchronized void stop(){
        if(!running.getAndSet(false)) return;
        if(ses!=null) ses.shutdownNow();
        if(tickListener!=null) tickListener.onEnd(roomId, new HashMap<>(scores));
    }
    
    public synchronized void setPlayerScore(String player, int score) {
        if (scores.containsKey(player)) {
            scores.put(player, score);
        }
    }
    
    public String getPlayerA(){ return playerA; }
    public String getPlayerB(){ return playerB; }
}
