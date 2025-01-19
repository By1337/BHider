package dev.by1337.hider.util;

public class FlagState {
    private final boolean[] votes;
    private boolean currentState;
    private boolean previousState;

    public FlagState(boolean[] votes) {
        this.votes = votes;
    }

    public void setVote(int index, boolean state) {
        votes[index] = state;
    }

    public void result(){
        currentState = calculateState();
    }
    public boolean is(){
        return currentState;
    }
    public boolean old(){
        return previousState;
    }
    public void sync(){
        previousState = currentState;
    }
    public boolean changed(){
        return currentState != previousState;
    }
    private boolean calculateState() {
        for (boolean vote : votes) {
            if (vote) {
                return true;
            }
        }
        return false;
    }
}