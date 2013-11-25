package yow2013.basic;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;

public abstract class BaseMessage {
    public boolean isValid;
}

class SafeyTranslatorOneArg<T extends BaseMessage, A> 
implements EventTranslatorOneArg<T, A> {
    private EventTranslatorOneArg<T, A> delegate;

    public SafeyTranslatorOneArg(EventTranslatorOneArg<T, A> delegate) {
        this.delegate = delegate;
    }

    public void translateTo(T t, long sequence, A arg0) {
        t.isValid = false;
        delegate.translateTo(t, sequence, arg0);
        t.isValid = true;
    }
}

class SafeyHandler<T extends BaseMessage> implements EventHandler<T> {
    private EventHandler<T> delegate;

    public SafeyHandler(EventHandler<T> handler) {
        this.delegate = handler;
    }

    public void onEvent(T t, long sequence, boolean endOfBatch) throws Exception {
        if (!t.isValid) {
            return;
        }
        delegate.onEvent(t, sequence, endOfBatch);
    }
}