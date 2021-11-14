package jmh.bench.allocators;

import jmh.perf.ElementsFactory;
import zmq.Ctx;
import zmq.Msg;
import zmq.SocketBase;
import zmq.ZMQ;
import zmq.msg.MsgAllocator;

public class AllocatorFactory implements ElementsFactory {

    private final MsgAllocator na;
    
    AllocatorFactory(MsgAllocator na) {
        this.na = na;
    }

    @Override
    public Ctx getContext() {
        return ZMQ.init(1);
    }

    @Override
    public String getUrl() {
        return "tcp://127.0.0.1:13800";
    }

    @Override
    public SocketBase getClientSocket(Ctx ctx) {
        SocketBase s = ZMQ.socket(ctx, ZMQ.ZMQ_PUSH);
        s.setSocketOpt(zmq.ZMQ.ZMQ_MSG_ALLOCATOR, na);
        s.setSocketOpt(zmq.ZMQ.ZMQ_SNDHWM, 10);
        return s;
    }

    @Override
    public SocketBase getServerSocket(Ctx ctx) {
        SocketBase serverSocket = ZMQ.socket(ctx, ZMQ.ZMQ_PULL);
        serverSocket.setSocketOpt(zmq.ZMQ.ZMQ_MSG_ALLOCATOR, na);
        return serverSocket;
    }

    @Override
    public Msg getClientMsg(int msgSize) {
        return na.allocate(msgSize);
    }

    @Override
    public Msg getServerMsg(Msg cmsg) {
        return null;
    }

    @Override
    public boolean withServer() {
        return true;
    }

    @Override
    public boolean waitAnswser() {
        return false;
    }

}
