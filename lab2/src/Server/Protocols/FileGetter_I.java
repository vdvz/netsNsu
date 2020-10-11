package Server.Protocols;

import Exceptions.End;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface FileGetter_I {

    void confirmCreation() throws End;

    void receive_file(SocketChannel from) throws IOException;

    void send(SocketChannel to) throws End, IOException;

    void shutdown() throws End;

    void close_sockets();

    void setCloseFLag();
}
