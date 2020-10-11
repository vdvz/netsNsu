package Server.Protocols;

import Exceptions.End;
import Server.Selectors.MySelector;
import Server.Server;
import javafx.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FileGetter implements FileGetter_I {

    private final long SIZE;
    private final String File_Name;
    private final SocketChannel client;
    private final ByteBuffer buffer;
    private MySelector client_selector = null;
    private volatile boolean isClose = false;

    private final FileOutputStream writer;
    private final int timeout = 100000;

    private long getting_bytes;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public FileGetter(SocketChannel _client, ByteBuffer _buffer, String file_name, long _size) throws End {
        client = _client;
        buffer = _buffer;
        SIZE = _size;
        File_Name = file_name;
        try {
            writer = new FileOutputStream("uploads/"+file_name);
        } catch (IOException e) {
            throw new End();
        }
        getting_bytes = 0;
    }

    @Override
    public void confirmCreation() throws End {
        buffer.clear();
        buffer.put((byte) 0x01);//IT IS OK CODE

        try {
            send(client);
        } catch (IOException e) {
            throw new End();
        }

        buffer.clear();

        try{
            client.configureBlocking(false);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            shutdown();
        }

        client_selector = Server.getInstance().getSelectorsPool().register(client, SelectionKey.OP_READ, this);

        lock.lock();
        try{
            while(!isClose){
                try {
                    if(!condition.await(timeout, TimeUnit.MILLISECONDS)) isClose = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Start exit: " + Thread.currentThread().getName());
        }finally {
            lock.unlock();
        }
        //Check if size same and send answer

        buffer.clear();

        if(getting_bytes == SIZE){
            buffer.put((byte)0x00);
        }else{
            buffer.put((byte)0x01);
        }

        try{
            send(client);
        } catch (IOException ignore){}

        close_sockets();
        close_file();
    }


    @Override
    public void receive_file(SocketChannel from)
    {
        int sub = -1;
        try {
            while(from.read(buffer)>0){
                if(!buffer.hasRemaining()){
                    buffer.flip();
                    byte[] array = buffer.array();
                    sub += array.length;
                    writer.write(array, 1, array.length);
                    buffer.clear();
                }
            }
            if(buffer.position()!=0){
                buffer.flip();
                byte[] array = buffer.array();
                sub += array.length;
                writer.write(array);
                buffer.clear();
            }
        } catch (IOException e) {
            try {
                shutdown();
            } catch (End end) {
                end.printStackTrace();
            }
        }
        if(sub != -1){
            getting_bytes += sub+1;
            setValidFLag();
        }
    }

    @Override
    public void send(SocketChannel to)  throws IOException{
        buffer.flip();
        to.write(buffer);
    }

    @Override
    public void shutdown() throws End {
        close_sockets();
        close_file();
        throw new End();
    }

    public void close_file() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close_sockets(){
        System.out.println("Close socket for thread: " + Thread.currentThread().getName());
        try {
            if(client_selector!=null)Server.getInstance().getSelectorsPool().unregister(client_selector);
            if(!client.socket().isClosed()) client.socket().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setCloseFLag() {
        lock.lock();
        try {
            if(!isClose) {
                isClose = true;
                condition.signal();
                System.out.println("EXCEPTION IS SET: " + Thread.currentThread().getName());
            }
        }finally{
            lock.unlock();
        }
    }


    public void setValidFLag(){
        lock.lock();
        try {
            condition.signal();
        }finally {
            lock.unlock();
        }
    }

}
