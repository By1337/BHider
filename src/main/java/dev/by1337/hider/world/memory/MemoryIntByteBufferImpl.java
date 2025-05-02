package dev.by1337.hider.world.memory;

import dev.by1337.hider.util.UnsafeUtil;
import sun.misc.Unsafe;

public class MemoryIntByteBufferImpl implements MemoryIntByteBuffer {
    private static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();
    private static final long INT_SIZE = Integer.BYTES;
    private volatile long address;
    private final long size;


    public MemoryIntByteBufferImpl(long size) {
        this.address = UNSAFE.allocateMemory(size);
        this.size = size;
        clear();
    }

    @Override
    public boolean released() {
        return address == 0;
    }

    @Override
    public long memoryAddress() {
        return address;
    }

    public void setInt(int index, int value) {
        UNSAFE.putInt(address + (INT_SIZE * index), value);
    }

    public int getInt(int index) {
        return UNSAFE.getInt(address + (INT_SIZE * index));
    }

    public void clear() {
        UNSAFE.setMemory(address, size, (byte) 0);
    }

    public synchronized void free() {
        if (address != 0) {
            UNSAFE.freeMemory(address);
            address = 0;
        }
    }

    @Override
    public long size() {
        return size;
    }
}
