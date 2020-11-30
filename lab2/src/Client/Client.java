package Client;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client implements Runnable {

    final byte version = 0x04;
    final byte command = 0x01;
    int port = 80;
    Integer ip = 2130706433;//127.0.0.1
    String server_ip;
    String file_name;

    Client(String _file_name, String _ip, int _port){
        server_ip = _ip;
        file_name = _file_name;
        port = _port;
    }

    ByteBuffer buffer;
    String ID = "vizir";
    String Password = "vadim";

    private void send_buffer(SocketChannel socket) throws IOException {
        buffer.flip();
        System.out.println("SEND: " + socket.write(buffer));
        buffer.clear();
    }

    public void receive_buffer(SocketChannel socket) throws IOException {
        buffer.clear();
        int i  = socket.read(buffer);
        System.out.println("GET BYTES: " + i);
        buffer.rewind();
    }

    public void makeFile(String name, long size, SocketChannel server, FileInputStream reader) throws IOException {
        buffer = ByteBuffer.allocate(4096);
        byte name_size = (byte) name.length();
        buffer.putLong(size).put(name_size).put(name.getBytes());
        send_buffer(server);
        receive_buffer(server);
        System.out.println("GET FROM SERVER:" + buffer.get());

        buffer.clear();
        byte[] buf = new byte[4096];
        int readed_bytes = 0 ;
        while ((readed_bytes = reader.read(buf, 0, buf.length))>0){
            buffer.put(buf,0, readed_bytes);
            send_buffer(server);
        }
    }

    public void make_SOCKS5(SocketChannel server) throws IOException {
        buffer = ByteBuffer.allocate(100);
        buffer.put((byte)0x05).put((byte)0x02).put((byte)0x00).put((byte)0x02);
        send_buffer(server);

        receive_buffer(server);
        buffer.get();
        byte code = buffer.get();
        System.out.println("CODE: " + code);
        if(code == 0x02){
            buffer.clear();
            buffer.put((byte)0x01).put((byte)ID.getBytes().length).put(ID.getBytes()).put((byte)Password.getBytes().length).put(Password.getBytes());
            send_buffer(server);
            receive_buffer(server);
            buffer.get();
            if(buffer.get()!=0x00) return;
        }

        buffer.clear();

        buffer.put((byte)0x05).put((byte)0x01).put((byte)0x00).put((byte)0x01).put(InetAddress.getByName("localhost").getAddress()).putShort((short)81);

        send_buffer(server);

        receive_buffer(server);
        buffer.get();
        System.out.println("Is available: " + buffer.get());

        buffer = ByteBuffer.allocate(4096);

        System.out.println("Send to destination server: " + 3);
        buffer.putInt(3);
        send_buffer(server);

        receive_buffer(server);
        System.out.println("get from destination server: " + buffer.getInt());

        while(true){

        }


    }

    public void make_SOCKS4(SocketChannel server) throws IOException {
        buffer = ByteBuffer.allocate(1000);
        buffer.put((byte)0x04).put((byte)0x01).putShort((short)81).putInt(2130706433).put(ID.getBytes());
        send_buffer(server);

        receive_buffer(server);
        buffer.get();
        System.out.println("Is availabel: " + buffer.get());

        buffer = ByteBuffer.allocate(500);
        buffer.rewind();
        System.out.println("Send to destination server: " + 3);
        buffer.putInt(3);
        send_buffer(server);
        buffer.rewind();
        receive_buffer(server);
        System.out.println("get from destination server: " + buffer.getInt());
    }

    public void run() {
        try {
            byte[] ip_v4 = ByteBuffer.allocate(4).putInt(ip).array();
            ByteBuffer tg = ByteBuffer.allocate(4).put(new Integer(149).byteValue()).put(new Integer(154).byteValue()).put(new Integer(167).byteValue()).put(new Integer(51).byteValue());
            SocketChannel soc = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(server_ip), port));

            File file = new File(file_name);
            long length = file.length();
            FileInputStream reader = new FileInputStream(file);
            makeFile(file_name, length, soc, reader);

            receive_buffer(soc);
            if(buffer.get() == (byte)0x00){
                System.out.println("cool");
            }else
                System.out.println("fail");


            soc.shutdownOutput();
            soc.shutdownInput();
            soc.finishConnect();
            //soc.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
