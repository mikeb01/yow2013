package yow2013.immutable;

import com.lmax.disruptor.EventHandler;

public class SimpleEventHandler implements EventHandler<SimpleEvent>
{
    @Override
    public void onEvent(SimpleEvent arg0, long arg1, boolean arg2) throws Exception
    {
    }
}
