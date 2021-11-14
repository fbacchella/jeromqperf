package jmh.perf;

import zmq.Ctx;
import zmq.Msg;
import zmq.SocketBase;

public interface ElementsFactory {

    boolean withServer();
    
    boolean waitAnswser();

    Ctx getContext();

    String getUrl();

    SocketBase getServerSocket(Ctx ctx);
    
    SocketBase getClientSocket(Ctx ctx);

    Msg getClientMsg(int msgSize);

    Msg getServerMsg(Msg cmsg);

}
