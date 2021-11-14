package jmh.allocators;

import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import jmh.ThreadBuilder;
import zmq.Msg;

public class NettyAllocatorCleaner extends MetricsAllocator {

    static private final Meter phantomCount = metrics.meter(MetricRegistry.name(NettyAllocatorCleaner.class, "collectedCount"));
    static private final Meter phantomBytes = metrics.meter(MetricRegistry.name(NettyAllocatorCleaner.class, "collectedBytes"));

    private final ByteBufAllocator allocator;
    private final Cleaner cleaner = Cleaner.create(ThreadBuilder.get().setPriority(Thread.MAX_PRIORITY - 2).getFactory("NettyAllocatorCleaner"));

    public NettyAllocatorCleaner() {
        allocator = new PooledByteBufAllocator(false);
    }

    public NettyAllocatorCleaner(ByteBufAllocator allocator) {
        this.allocator = allocator;
    }

    @Override
    public Msg allocate(int size) {
        if (size == 0) {
            return new Msg(0);
        } {
            ByteBuf buffer = allocator.buffer(size);
            buffer.capacity(size);
            buffer.writerIndex(size);
            ByteBuffer jbuffer = buffer.nioBuffer();
            jbuffer.position(0);
            jbuffer.limit(size);
            assert buffer.nioBufferCount() == 1;
            Msg msg = new Msg(jbuffer);
            cleaner.register(msg, () -> destroy(buffer));
            return msg;
        }
    }

    private void destroy(ByteBuf phantom) {
        phantomCount.mark();
        phantomBytes.mark(phantom.capacity());
        phantom.release();
    }

}
