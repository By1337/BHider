package dev.by1337.hider.world.memory;

public class SafeMemoryIntByteBuffer extends MemoryIntByteBufferImpl {
    private static final long INT_SIZE = Integer.BYTES;

    public SafeMemoryIntByteBuffer(long size) {
        super(size);
    }

    @Override
    public long memoryAddress() {
        accessCheck();
        return super.memoryAddress();
    }

    @Override
    public void setInt(int index, int value) {
        accessCheck();
        if ((long) index < 0 || ((long) index + 1) * INT_SIZE > size()) {
            throw new IndexOutOfBoundsException("index " + index + " out of bounds " + size() / INT_SIZE);
        }
        super.setInt(index, value);
    }

    @Override
    public int getInt(int index) {
        accessCheck();
        if ((long) index < 0 || ((long) index + 1) * INT_SIZE > size()) {
            throw new IndexOutOfBoundsException("index " + index + " out of bounds " + size() / INT_SIZE);
        }
        return super.getInt(index);
    }

    @Override
    public void clear() {
        accessCheck();
        super.clear();
    }

    @Override
    public synchronized void free() {
        accessCheck();
        super.free();
    }

    private void accessCheck() {
        if (released()) {
            throw new IllegalStateException("Memory byte buffer is closed!");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (!released()) {
            free();
            throw new IllegalStateException("Memory byte buffer is not closed!");
        }
    }
}
