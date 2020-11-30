package Server.Connection;

import Exceptions.End;
import Server.Protocols.FileGetter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class Task implements Runnable, Task_I {

    private final SocketChannel client;
    private final ByteBuffer buffer;

    public Task(SocketChannel client_) {
        buffer = ByteBuffer.allocate(4096);
        client = client_;
        try {
            client.configureBlocking(true);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            receive(client);
        } catch (End e){
            shutdown_task();
        }

        try {
            long size = buffer.getLong();
            byte filename_length = buffer.get();
            byte[] filename = new byte[filename_length];
            buffer.get(filename, 0, filename_length);

            new FileGetter(client, buffer, new String(filename), size).confirmCreation();

        }catch(End e){
            System.out.println("Bye\nInterrupt by end, cause is in Socks-class");
            e.printStackTrace();
        }
    }


    @Override
    public void receive(SocketChannel from) throws End {
        buffer.clear();
        try{
            if(from.read(buffer)<0){
                throw new End();
            }
        } catch (IOException e) {
            throw new End();
        }
        buffer.rewind();
    }

    @Override
    public void shutdown_task(){
        System.out.println("Bye\ninterrupt by shutdown, cause in Task-class");
        try {
            client.socket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
