package jmh.allocators;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import jmh.ThreadBuilder;
import zmq.Msg;

public class NettyAllocatorReference extends MetricsAllocator {

    static private final Meter phantomCount = metrics.meter(MetricRegistry.name(NettyAllocatorReference.class, "collectedCount"));
    static private final Meter phantomBytes = metrics.meter(MetricRegistry.name(NettyAllocatorReference.class, "collectedBytes"));

    private static class PhantomMsg extends PhantomReference<Msg> {
        private final ByteBuf buffer;
        public PhantomMsg(Msg referent, ByteBuf buffer,
                          ReferenceQueue<Msg> q) {
            super(referent, q);
            this.buffer = buffer;
        }
    }

    private static final AtomicInteger count = new AtomicInteger();

    private final ReferenceQueue<Msg> queue = new ReferenceQueue<>();
    private final Set<Reference<Msg>> phs = ConcurrentHashMap.newKeySet();
    private final ByteBufAllocator allocator;
    private final ExecutorService executorService;

    public NettyAllocatorReference(int workers) {
        ThreadBuilder.get().setDaemon(true).setTask(this::cleaner).setPriority(Thread.MAX_PRIORITY - 2).setName("ZMQNettyCleaner" + count.incrementAndGet()).build(true);
        allocator = new PooledByteBufAllocator(false); //PooledByteBufAllocator.DEFAULT;
        if (workers > 1) {
            executorService = Executors.newFixedThreadPool(workers, ThreadBuilder.get().getFactory("NettyAllocatorCleaner" + count.get()));
        } else {
            executorService = null;
        }
    }

    public NettyAllocatorReference(ByteBufAllocator allocator, int workers) {
        ThreadBuilder.get().setDaemon(true).setTask(this::cleaner).setPriority(Thread.MAX_PRIORITY - 2).setName("ZMQNettyCleaner" + count.incrementAndGet()).build(true);
        this.allocator = allocator;
        if (workers > 1) {
            executorService = Executors.newFixedThreadPool(workers, ThreadBuilder.get().getFactory("NettyAllocatorCleaner" + count.get()));
        } else {
            executorService = null;
        }
    }

    @Override
    public Msg allocate(int size) {
        if (size == 0) {
            return new Msg(0);
        } else {
            ByteBuf buffer = allocator.buffer(size);
            buffer.capacity(size);
            buffer.writerIndex(size);
            ByteBuffer jbuffer = buffer.nioBuffer();
            jbuffer.position(0);
            jbuffer.limit(size);
            assert buffer.nioBufferCount() == 1;
            Msg msg = new Msg(jbuffer);
            phs.add(new PhantomMsg(msg, buffer, queue));
            return msg;
        }
    }

    private void cleaner() {
        while (true) {
            try {
                PhantomMsg phantom = (PhantomMsg) queue.remove();
                if (executorService != null) {
                    executorService.execute(() -> destroy(phantom));
                } else {
                    destroy(phantom);
                }
            } catch (InterruptedException e) {
                // never stopping
            }
        }
    }

    private void destroy(PhantomMsg phantom) {
        phantomCount.mark();
        phantomBytes.mark(phantom.buffer.capacity());
        phantom.buffer.release();
        phs.remove(phantom);
    }

}
