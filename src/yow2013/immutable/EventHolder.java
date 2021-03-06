package yow2013.immutable;

import com.lmax.disruptor.EventFactory;

public class EventHolder {
    
    public static final EventFactory<EventHolder> FACTORY = 
            new EventFactory<EventHolder>() {
        public EventHolder newInstance() {
            return new EventHolder();
        }
    };

    public SimpleEvent event;
}
