package yow2013.offheap;

import java.nio.ByteBuffer;

public class ThreadLocalBuffer extends ThreadLocal<ByteBuffer> {
    private ByteBuffer buffer;

    public ThreadLocalBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
    
    @Override
    protected ByteBuffer initialValue() {
        return buffer.duplicate();
    }
}
