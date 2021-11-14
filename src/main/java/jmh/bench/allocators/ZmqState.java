package jmh.bench.allocators;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import io.netty.buffer.PooledByteBufAllocator;
import jmh.allocators.NettyAllocatorCleaner;
import jmh.allocators.NettyAllocatorReference;
import jmh.perf.BenchmarkContext;
import zmq.msg.MsgAllocator;
import zmq.msg.MsgAllocatorDirect;
import zmq.msg.MsgAllocatorHeap;

@State(Scope.Benchmark)
public class ZmqState {

    @Param({"0", "1", "100", "10000", "100000", "10000000"})
    public int msgSize;
    @Param({"nettyHeapReference", "nettyDirectReference", "nettyHeapCleaner", "nettyDirectCleaner", "heap", "direct"})
    public String allocator;

    BenchmarkContext zctx;

    private MsgAllocator getAllocator() {
        switch (allocator) {
        case "nettyHeapReference":
            return new NettyAllocatorReference(new PooledByteBufAllocator(false), 4);
        case "nettyDirectReference":
            return new NettyAllocatorReference(new PooledByteBufAllocator(true), 4);
        case "nettyHeapCleaner":
            return new NettyAllocatorCleaner(new PooledByteBufAllocator(false));
        case "nettyDirectCleaner":
            return new NettyAllocatorCleaner(new PooledByteBufAllocator(true));
        case "heap":
            return new MsgAllocatorHeap();
        case "direct":
            return new MsgAllocatorDirect();
        default:
            throw new IllegalArgumentException("Unknown allocator " + allocator);
        }
    }
    
    @Setup
    public void setup() {
        zctx = new BenchmarkContext(new AllocatorFactory(getAllocator()));
    }

    @TearDown
    public void tearDown() {
        zctx.stop();
    }

}
