package dev.by1337.hider.util;

public class BoolWatcher {

    private boolean value;
    private boolean last;

    public BoolWatcher(boolean value) {
        this.value = value;
        this.last = value;
    }

    public boolean get() {
        return value;
    }

    public boolean getLast() {
        return last;
    }

    public boolean isDirty() {
        return value != last;
    }

    public void setDirty(boolean flag) {
        if (flag) last = !value;
        else last = value;
    }

    public void set(boolean value) {
        this.value = value;
    }
}
