package yow2013.immutable;

import java.util.Random;

import com.lmax.disruptor.EventHandler;

public class SimpleEventHandler implements EventHandler<SimpleEvent>
{
    private final Random r = new Random(9);
    private final SimpleEvent[] events = new SimpleEvent[1 << 16];
    
    @Override
    public void onEvent(SimpleEvent arg0, long arg1, boolean arg2) throws Exception
    {
        int index = (int) ((r.nextGaussian() * (events.length/8)) + (events.length / 2));
        try
        {
            events[index] = arg0;
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            // Don't care.
        }
    }
}
