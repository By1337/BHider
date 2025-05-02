package dev.by1337.hider.world.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
public class MemoryIntByteBufferTest {

    @Test
    void testBasicReadWriteAndClear() {
        MemoryIntByteBuffer buffer = new SafeMemoryIntByteBuffer(256 * Integer.BYTES);

        assertEquals(0, buffer.getInt(0));

        buffer.setInt(0, 128);
        buffer.setInt(1, 256);
        buffer.setInt(63, 4096);

        assertEquals(128, buffer.getInt(0));
        assertEquals(256, buffer.getInt(1));
        assertEquals(4096, buffer.getInt(63));

        buffer.clear();

        assertEquals(0, buffer.getInt(0));
        assertEquals(0, buffer.getInt(1));
        assertEquals(0, buffer.getInt(63));

        buffer.free();
    }

    @Test
    void testIndexOutOfBounds() {
        MemoryIntByteBuffer buffer = new SafeMemoryIntByteBuffer(16 * Integer.BYTES);

        assertThrows(IndexOutOfBoundsException.class, () -> buffer.setInt(16, 42));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.getInt(16));

        buffer.free();
    }

    @Test
    void testAccessAfterFree() {
        MemoryIntByteBuffer buffer = new SafeMemoryIntByteBuffer(16 * Integer.BYTES);

        buffer.setInt(0, 42);
        buffer.free();

        assertThrows(IllegalStateException.class, () -> buffer.getInt(0));
        assertThrows(IllegalStateException.class, () -> buffer.setInt(0, 1));
        assertThrows(IllegalStateException.class, buffer::clear);
        assertThrows(IllegalStateException.class, buffer::free);
    }

    @Test
    void testReleasedFlag() {
        MemoryIntByteBuffer buffer = new SafeMemoryIntByteBuffer(16 * Integer.BYTES);
        assertFalse(buffer.released());
        buffer.free();
        assertTrue(buffer.released());
    }
  
}