package yow2013.immutable;

import java.util.concurrent.locks.LockSupport;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.SingleProducerSequencer;
import com.lmax.disruptor.YieldingWaitStrategy;

public class CustomPerformanceTest
{
    private final CustomRingBuffer<SimpleEvent> ringBuffer;
    
    public CustomPerformanceTest()
    {
        ringBuffer = new CustomRingBuffer<>(new SingleProducerSequencer(1 << 20, new YieldingWaitStrategy()));
    }
    
    public void run()
    {
        try
        {
            doRun();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }        
    }

    private void doRun() throws InterruptedException
    {
        BatchEventProcessor<?> batchEventProcessor = ringBuffer.createHandler(new SimpleEventHandler());
        
        Thread t = new Thread(batchEventProcessor);
        t.start();
        
        long iterations = 1_000_000_000L;
//        long iterations = 40L;
        for (long l = 0; l < iterations; l++)
        {
            SimpleEvent e = new SimpleEvent(l, l, l, l);
            ringBuffer.put(e);
        }
        
        while (batchEventProcessor.getSequence().get() != iterations - 1)
        {
            LockSupport.parkNanos(1);
        }
        
        batchEventProcessor.halt();
        t.join();
    }
            
    public static void main(String[] args)
    {
        new CustomPerformanceTest().run();
    }

}
