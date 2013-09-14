package yow2013.immutable;

public interface EventAccessor<T>
{
    T take(long sequence);
}
