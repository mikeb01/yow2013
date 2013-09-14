package yow2013.immutable;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.DataProvider;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequencer;

public class CustomRingBuffer<T> implements DataProvider<EventAccessor<T>>, EventAccessor<T>
{
    private final Sequencer sequencer;
    private final Object[] buffer;
    private final int mask;
    
    public CustomRingBuffer(Sequencer sequencer)
    {
        this.sequencer = sequencer;
        buffer = new Object[sequencer.getBufferSize()];
        mask = sequencer.getBufferSize() - 1;
    }
    
    private int index(long sequence)
    {
        return (int) (sequence & mask);
    }
    
    public void put(SimpleEvent e)
    {
        long next = sequencer.next();
        buffer[index(next)] = e;
        sequencer.publish(next);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T take(long sequence)
    {
        T t = (T) buffer[index(sequence)];
        buffer[index(sequence)] = null;
        return t;
    }   

    @Override
    public EventAccessor<T> get(long sequence)
    {
        return this;
    }
    
    public BatchEventProcessor<EventAccessor<T>> createHandler(final EventHandler<T> handler)
    {
        BatchEventProcessor<EventAccessor<T>> processor = new BatchEventProcessor<>(this, sequencer.newBarrier(), 
                new EventHandler<EventAccessor<T>>()
                {
                    @Override
                    public void onEvent(EventAccessor<T> accessor, long sequence, boolean endOfBatch) throws Exception
                    {
                        handler.onEvent(accessor.take(sequence), sequence, endOfBatch);
                    }
                });
        
        sequencer.addGatingSequences(processor.getSequence());
        
        return processor;
    }
}
