/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package yow2013.immutable;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;

import java.io.PrintStream;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.SingleProducerSequencer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.util.DaemonThreadFactory;

/**
 * <pre>
 * Disruptor:
 * ==========
 *               +----------+
 *               |          |
 *               |   get    V
 *  waitFor   +=====+    +=====+  claim
 *    +------>| SB2 |    | RB2 |<------+
 *    |       +=====+    +=====+       |
 *    |                                |
 * +-----+    +=====+    +=====+    +-----+
 * | EP1 |--->| RB1 |    | SB1 |<---| EP2 |
 * +-----+    +=====+    +=====+    +-----+
 *       claim   ^   get    |  waitFor
 *               |          |
 *               +----------+
 *
 * EP1 - Pinger
 * EP2 - Ponger
 * RB1 - PingBuffer
 * SB1 - PingBarrier
 * RB2 - PongBuffer
 * SB2 - PongBarrier
 *
 * </pre>
 *
 * Note: <b>This test is only useful on a system using an invariant TSC in user space from the System.nanoTime() call.</b>
 */
public final class CustomPingPongLatencyTest
{
    private static final int BUFFER_SIZE = 1 << 16;
    private static final long ITERATIONS = 10_000_000L;
    private static final long PAUSE_NANOS = 500L;
    private final ExecutorService executor = Executors.newCachedThreadPool(DaemonThreadFactory.INSTANCE);

    private final Histogram histogram = new Histogram(TimeUnit.HOURS.toMicros(1), 4);

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private final CustomRingBuffer<SimpleEvent> pingBuffer = new CustomRingBuffer<>(new SingleProducerSequencer(BUFFER_SIZE, new YieldingWaitStrategy()));
    private final CustomRingBuffer<SimpleEvent> pongBuffer = new CustomRingBuffer<>(new SingleProducerSequencer(BUFFER_SIZE, new YieldingWaitStrategy()));

    private final Pinger pinger = new Pinger(pingBuffer, ITERATIONS, PAUSE_NANOS);
    private final Ponger ponger = new Ponger(pongBuffer);
    
    private final BatchEventProcessor<EventAccessor<SimpleEvent>> pingProcessor = pongBuffer.createHandler(pinger);
    private final BatchEventProcessor<EventAccessor<SimpleEvent>> pongProcessor = pingBuffer.createHandler(ponger);

    ///////////////////////////////////////////////////////////////////////////////////////////////

    public void runTest() throws Exception
    {
        final int runs = 3;

        for (int i = 0; i < runs; i++)
        {
            System.gc();
            histogram.reset();

            runDisruptorPass();

            if (histogram.getHistogramData().getTotalCount() < ITERATIONS)
            {
                throw new IllegalStateException();
            }
            
            System.out.format("%s run %d Disruptor %s\n", getClass().getSimpleName(), Long.valueOf(i), histogram);
            dumpHistogram(histogram, System.out);
        }
    }

    private static void dumpHistogram(Histogram histogram, final PrintStream out)
    {
        histogram.getHistogramData().outputPercentileDistribution(out, 1, 1000.0);
    }

    private void runDisruptorPass() throws InterruptedException, BrokenBarrierException
    {
        CountDownLatch latch = new CountDownLatch(1);
        CyclicBarrier barrier = new CyclicBarrier(3);
        pinger.reset(barrier, latch, histogram);
        ponger.reset(barrier);

        executor.submit(pongProcessor);
        executor.submit(pingProcessor);

        barrier.await();
        latch.await();

        pingProcessor.halt();
        pongProcessor.halt();
    }

    public static void main(String[] args) throws Exception
    {
        CustomPingPongLatencyTest test = new CustomPingPongLatencyTest();
        test.runTest();
    }

    private static class Pinger implements EventHandler<SimpleEvent>, LifecycleAware
    {
        private final CustomRingBuffer<SimpleEvent> buffer;
        private final long maxEvents;
        private final long pauseTimeNs;

        private long counter = 0;
        private CyclicBarrier barrier;
        private CountDownLatch latch;
        private Histogram histogram;
        private long t0;

        public Pinger(CustomRingBuffer<SimpleEvent> buffer, long maxEvents, long pauseTimeNs)
        {
            this.buffer = buffer;
            this.maxEvents = maxEvents;
            this.pauseTimeNs = pauseTimeNs;
        }

        @Override
        public void onEvent(SimpleEvent event, long sequence, boolean endOfBatch) throws Exception
        {
            long t1 = System.nanoTime();

            histogram.recordValueWithExpectedInterval(t1 - t0, pauseTimeNs);

            if (event.getCounter() < maxEvents)
            {
                while (pauseTimeNs > (System.nanoTime() - t1))
                {
                    Thread.yield();
                }

                send();
            }
            else
            {
                latch.countDown();
            }
        }

        private void send()
        {
            t0 = System.nanoTime();
            
            buffer.put(new SimpleEvent(t0, counter, counter, counter));

            counter++;
        }

        @Override
        public void onStart()
        {
            try
            {
                barrier.await();

                Thread.sleep(1000);
                send();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onShutdown()
        {
        }

        public void reset(CyclicBarrier barrier, CountDownLatch latch, Histogram histogram)
        {
            this.histogram = histogram;
            this.barrier = barrier;
            this.latch = latch;

            counter = 0;
        }
    }

    private static class Ponger implements EventHandler<SimpleEvent>, LifecycleAware
    {
        private final CustomRingBuffer<SimpleEvent> buffer;

        private CyclicBarrier barrier;

        public Ponger(CustomRingBuffer<SimpleEvent> buffer)
        {
            this.buffer = buffer;
        }

        @Override
        public void onEvent(SimpleEvent event, long sequence, boolean endOfBatch) throws Exception
        {
            buffer.put(event);
        }

        @Override
        public void onStart()
        {
            try
            {
                barrier.await();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onShutdown()
        {
        }

        public void reset(CyclicBarrier barrier)
        {
            this.barrier = barrier;
        }
    }
}
