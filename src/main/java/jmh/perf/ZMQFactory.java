package jmh.perf;

import zmq.Ctx;
import zmq.Msg;
import zmq.SocketBase;

/**
 * @author Fabrice Bacchella
 *
 */
public interface ZMQFactory {

    public interface ServerProcessing {
        public void process(SocketBase s);
    }

    /**
     * If it return true, a ZMQ server will be started
     * @return True if a server is needed
     */
    boolean withServer();
    
    /**
     * 
     * @return True if an answer is expected from the server
     */
    boolean waitAnswser();

    /**
     * Create and configure a context
     * @return The new context
     */
    Ctx getContext();

    /**
     * The URL used for the connection
     * @return
     */
    String getUrl();

    /**
     * Create an socket that will be used by the server
     * @param ctx
     * @return
     */
    SocketBase getServerSocket(Ctx ctx);
    
    /**
     * Create an socket that will be used by the client, in the iteration loops
     * @param ctx
     * @return
     */
    SocketBase getClientSocket(Ctx ctx);

    /**
     * Generate query message
     * @param msgSize
     * @return
     */
    Msg getQueryMsg(int msgSize);

    /**
     * Generate an answer message, from a query
     * @param cmsg
     * @return
     */
    Msg getAnswerMsg(Msg cmsg);

    /**
     * The processing in the server
     * @return
     */
    ServerProcessing getServerProcessing(BenchmarkContext ctx);

}
