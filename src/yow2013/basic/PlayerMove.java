package yow2013.basic;

import java.nio.ByteBuffer;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;

class PlayerMove
{
    long id;
    long direction;
    long distance;
}

class PlayerMoveFactory implements EventFactory<PlayerMove>
{
    public PlayerMove newInstance()
    {
        return new PlayerMove();
    }
}

class PlayerHandler 
implements EventHandler<PlayerMove>
{
    public void onEvent(PlayerMove event,
                        long sequence, 
                        boolean onBatchEnd) throws Exception
    {
        Player player = findPlayer(event.id);
        player.move(event.direction, event.distance);
    }
    
    Player findPlayer(long id)
    {
        return null;
    }
}

class Player
{
    void move(long direction, long distance)
    {
    }
}

class NetworkHandler
{
    void handle(ByteBuffer packet)
    {
        RingBuffer<PlayerMove> buffer = getBuffer();
        long next = buffer.next();
        PlayerMove playerMove = buffer.get(next);
        playerMove.id = packet.getLong();
        playerMove.direction = packet.getLong();
        playerMove.distance = packet.getLong();
        buffer.publish(next);
    }

    private RingBuffer<PlayerMove> getBuffer()
    {
        // TODO Auto-generated method stub
        return null;
    }
}

class NetworkHandler2
{
    void handle(final ByteBuffer packet)
    {
        RingBuffer<PlayerMove> buffer = getBuffer();
        buffer.publishEvent(new EventTranslator<PlayerMove>()
        {
            public void translateTo(PlayerMove playerMove, long sequence)
            {
                playerMove.id = packet.getLong();
                playerMove.direction = packet.getLong();
                playerMove.distance = packet.getLong();
            }
        });
    }

    private RingBuffer<PlayerMove> getBuffer()
    {
        // TODO Auto-generated method stub
        return null;
    }
}

class PlayerMoveTranslator implements EventTranslatorOneArg<PlayerMove, ByteBuffer>
{
    public static final PlayerMoveTranslator INSTANCE = new PlayerMoveTranslator();
    
    @Override
    public void translateTo(PlayerMove playerMove, long seq, ByteBuffer packet)
    {
        playerMove.id = packet.getLong();
        playerMove.direction = packet.getLong();
        playerMove.distance = packet.getLong();
    }
}

class NetworkHandler3
{
    void handle(final ByteBuffer packet)
    {
        RingBuffer<PlayerMove> buffer = getBuffer();
        buffer.publishEvent(PlayerMoveTranslator.INSTANCE, packet);
    }

    private RingBuffer<PlayerMove> getBuffer()
    {
        // TODO Auto-generated method stub
        return null;
    }    
}
