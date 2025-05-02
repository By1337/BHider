package dev.by1337.hider.world.memory;

public interface MemoryIntByteBuffer {
    boolean released();
    long memoryAddress();
    void setInt(int index, int value);
    int getInt(int index);
    void clear();
    void free();
    long size();
}
