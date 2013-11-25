package yow2013.offheap;

import java.nio.ByteBuffer;

import com.lmax.disruptor.EventHandler;

public class BufferEventHandler 
implements EventHandler<ByteBuffer> {

    public void onEvent(ByteBuffer buffer, 
                        long sequence, 
                        boolean endOfBatch) {
        // Do stuff...
    }
}
