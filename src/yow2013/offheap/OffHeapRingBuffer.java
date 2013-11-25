package yow2013.offheap;

import java.nio.ByteBuffer;

import com.lmax.disruptor.DataProvider;
import com.lmax.disruptor.Sequencer;

public class OffHeapRingBuffer implements DataProvider<ByteBuffer> {
    private final Sequencer sequencer;
    private final int entrySize;
    private final ByteBuffer buffer;
    private final int mask;
    
    private final ThreadLocal<ByteBuffer> perThreadBuffer = 
            new ThreadLocal<ByteBuffer>() {
                @Override
                protected ByteBuffer initialValue() {
                    return buffer.duplicate();
                }
            };

    public OffHeapRingBuffer(Sequencer sequencer, int entrySize) {
        this.sequencer = sequencer;
        this.entrySize = entrySize;
        this.mask = sequencer.getBufferSize() - 1;
        buffer = ByteBuffer.allocateDirect(sequencer.getBufferSize() * entrySize);
    }

    @Override
    public ByteBuffer get(long sequence) {
        int index = index(sequence);
        int position = index * entrySize;
        int limit = position + entrySize;
        
        ByteBuffer byteBuffer = perThreadBuffer.get();
        byteBuffer.position(position).limit(limit);
        
        return byteBuffer;
    }
    
    public void put(byte[] data) {
        long next = sequencer.next();
        try {
            get(next).put(data);
        } finally {
            sequencer.publish(next);
        }
    }

    private int index(long next) {
        return (int)(next & mask);
    }
}
