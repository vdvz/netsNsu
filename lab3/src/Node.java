import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Node {
    final String name;
    final int accuracy;

    Selector selector;

    InetSocketAddress substitutionAddress = null;

    Set<SocketAddress> neighbours = new HashSet<>();
    DatagramSocket socket;
    DatagramChannel channel;

    InetSocketAddress daddy = null;

    Map<UUID, List<SocketAddress>> sendingMessages = new HashMap<>();
    Map<UUID, DatagramPacket> receivedMessages = new HashMap<>();

    private final BlockingQueue<String> lines = new LinkedBlockingQueue<>();

    Set<SocketAddress> blackList = new HashSet<>();

    int resendDelay = 100;
    ActionListener resendMsg = e -> sendingMessages.forEach((key, value) -> value.forEach(socketAddress -> {
        byte[] sub = receivedMessages.get(key).getData();
        send(socketAddress, ByteBuffer.wrap(sub).position(sub.length));
    }));

    int checkAliveDelay = 100;
    ActionListener checkIfAlive = e -> sendMulticast(null, ByteBuffer.allocate(1).put((byte)0x03));

    int killDiedDelay = 300;
    ActionListener killDiedNodes = e -> {
        blackList.forEach(socketAddress -> {
            neighbours.remove(socketAddress);
            System.out.println("DIED: " + socketAddress);
            if(socketAddress.equals(daddy)){
                rebuildTree();
            }
            sendingMessages.entrySet().removeIf(entry -> entry.getValue().contains(socketAddress));
        });
        blackList.clear();
        blackList.addAll(neighbours);
    };



    Node(String _name, int _accuracy){
        name = _name;
        accuracy = _accuracy;
        try {
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(InetAddress.getByName("localhost"), 4445));
            socket = channel.socket();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            channel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        startReader(selector);
        try {
            channel.register(selector, SelectionKey.OP_READ);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }

        Timer resendTimer = new Timer(resendDelay,resendMsg);
        resendTimer.start();

        Timer killDiedTimer = new Timer(killDiedDelay, killDiedNodes);
        killDiedTimer.start();

        Timer checkAliveTimer = new Timer(checkAliveDelay, checkIfAlive);
        checkAliveTimer.start();

    }

    void startReader(Selector selector){
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        Thread backgroundReaderThread = new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    lines.add(line);
                    selector.wakeup();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        backgroundReaderThread.start();
    }

    Node(String _name, int _accuracy, String ip_con, int port_con){
        name = _name;
        accuracy = _accuracy;
        InetSocketAddress address = null;
        try {
            channel = DatagramChannel.open();
            address = new InetSocketAddress(InetAddress.getByName(ip_con), port_con);
            socket = channel.socket();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            channel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        startReader(selector);
        try {
            channel.register(selector, SelectionKey.OP_READ);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
        sendWelcome(address);

        Timer resendTimer = new Timer(resendDelay,resendMsg);
        resendTimer.start();

        Timer killDiedTimer = new Timer(killDiedDelay, killDiedNodes);
        killDiedTimer.start();

        Timer checkAliveTimer = new Timer(checkAliveDelay, checkIfAlive);
        checkAliveTimer.start();

    }

    public static void main(String[] args) {
        /*int mode = Integer.parseInt(args[0]);
        String name = args[1];
        int accuracy = Integer.parseInt(args[2]);
        switch (mode){
            case 0 -> {
                Node node = new Node(name, accuracy);
                node.run();
            }
            case 1 -> {
                String ip = args[3];
                int port = Integer.parseInt(args[4]);
                Node node = new Node(name, accuracy, ip, port);
                node.run();
            }
        }*/
        //Node node1 = new Node("V", 1,"localhost",4445);
        Node node1 = new Node("V", 60);
        node1.run();

    }

    void run(){
        while(true){
            try {
                if (!lines.isEmpty()) {
                    sendMsg(lines.poll());
                }
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Set<SelectionKey> channels = selector.selectedKeys();
            for(SelectionKey key : channels) {
                if (key.isValid()) {
                    if (key.isReadable()) {
                        receive();
                    }
                }
            }
        }
    }

    void receive(){
        ByteBuffer sub = ByteBuffer.allocate(4096);
        byte type = 0x05;
        DatagramPacket datagramPacket = null;
        try {
            SocketAddress address = channel.receive(sub);
            if(address==null) return;
            int lim = sub.flip().limit();
            byte[] buf = new byte[lim];
            sub.get(buf,0,lim);
            type = buf[0];
            datagramPacket = new DatagramPacket(buf, lim, address);
        } catch (IOException e) {
            e.printStackTrace();
        }

        switch (type){
            case 0x00 -> {
                if(!IsItFaultPacket()) receiveMessage(datagramPacket);
            }
            case 0x01 -> {
                receiveHandshake(datagramPacket);
            }
            case 0x02 -> {
                receiveWelcome(datagramPacket);
            }
            case 0x03 -> {
                receiveAlive(datagramPacket);
            }
            case 0x04 -> {
                receiveConfirmation(datagramPacket);
            }
            default -> System.out.println("Receive error type");
        }
    }

    void receiveAlive(DatagramPacket packet){
        blackList.remove(packet.getSocketAddress());
    }

    void send(SocketAddress socketAddress, ByteBuffer buffer){
        try {
            buffer.flip();
            channel.send(buffer, socketAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Random random = new Random();
    boolean IsItFaultPacket(){
        return random.nextInt(99) < accuracy;
    }

    void sendMulticast(SocketAddress notSend, ByteBuffer buffer){
        neighbours.stream().filter(socketAddress -> !socketAddress.equals(notSend)).forEach(socketAddress -> send(socketAddress, buffer));
    }


    void sendMsg(String msg){
        System.out.println("Daddy:" + daddy);
        System.out.println("Substitution:" + substitutionAddress);
        UUID uuid = UUID.randomUUID();
        ByteBuffer buf = ByteBuffer.allocate(4096);
        DatagramPacket packet = new DatagramPacket(buf.array(), buf.array().length);
        buf.put((byte)0x00)
                .putInt(uuid.toString().getBytes().length)
                .put(uuid.toString().getBytes())
                .putInt(name.getBytes().length)
                .put(name.getBytes())
                .putInt(msg.getBytes().length)
                .put(msg.getBytes());
        receivedMessages.put(uuid, packet);
        List<SocketAddress> list = new ArrayList<>(neighbours);
        sendingMessages.put(uuid, list);
        sendMulticast(null, buf);
    }

    void receiveMessage(DatagramPacket packet){
        SocketAddress receiveFrom = packet.getSocketAddress();
        ByteBuffer buf = ByteBuffer.wrap(packet.getData());
        buf.get();
        int lengthUUID = buf.getInt();
        byte[] uuid_buf = new byte[lengthUUID];
        buf.get(uuid_buf, 0, lengthUUID);
        int lengthName = buf.getInt();
        byte[] name_buf = new byte[lengthName];
        buf.get(name_buf, 0, lengthName);
        int lengthMsg = buf.getInt();
        byte[] msg_buf = new byte[lengthMsg];
        buf.get(msg_buf, 0, lengthMsg);
        UUID uuid = UUID.fromString(new String(uuid_buf,0,lengthUUID));
        String msg = new String(msg_buf,0,lengthMsg);
        String from_name = new String(name_buf,0,lengthName);
        if(receivedMessages.containsKey(uuid)){
            return;
        }

        receivedMessages.put(uuid, packet);

        System.out.println("UUID: " + uuid);
        System.out.println("Name: " + from_name);
        System.out.println("Message: " + msg);

        List<SocketAddress> list = new ArrayList<>(neighbours);
        list.remove(receiveFrom);
        sendingMessages.put(uuid, list);

        sendConfirmation(receiveFrom, uuid);
        sendMulticast(receiveFrom, buf);

    }

    void sendConfirmation(SocketAddress socketAddress, UUID uuidMsg){
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        buffer.put((byte)0x04)
                .putInt(uuidMsg.toString().getBytes().length)
                .put(uuidMsg.toString().getBytes());
        send(socketAddress, buffer);
    }

    void receiveConfirmation(DatagramPacket packet){
        ByteBuffer buf = ByteBuffer.wrap(packet.getData());
        buf.get();
        int lengthUUID = buf.getInt();
        byte[] uuid_buf = new byte[4096];
        buf.get(uuid_buf, 0, lengthUUID);
        System.out.println("Receive confirmation of:" + new String(uuid_buf,0,lengthUUID));
        UUID uuid = UUID.fromString(new String(uuid_buf,0,lengthUUID));

        sendingMessages.get(uuid).remove(packet.getSocketAddress());
    }

    void rebuildTree(){
        System.out.println("start rebuilding");
        if(substitutionAddress!=null & !substitutionAddress.equals(daddy)){
            sendWelcome(substitutionAddress);
            return;
        }
        Optional<SocketAddress> new_addr = neighbours.stream().filter(e->!e.equals(daddy)).findFirst();
        if(new_addr.isPresent()){
            substitutionAddress = (InetSocketAddress) new_addr.get();
            daddy = (InetSocketAddress) new_addr.get();
            sendUpdateSubstitutionForChild();
        }else{
            daddy = null;
            substitutionAddress=null;
            System.out.println("Im alone");
        }
    }

    void sendUpdateSubstitutionForChild(){
        ByteBuffer buf = ByteBuffer.allocate(4096);
        buf.put((byte)0x01)
                .putInt(substitutionAddress.getPort())
                .putInt(substitutionAddress.getHostName().length())
                .put(substitutionAddress.getHostName().getBytes());
        sendMulticast(null, buf);
    }

    void sendWelcome(InetSocketAddress address){
        System.out.println("Send welcome" + address);
        daddy = address;
        neighbours.add(address);
        send(address, ByteBuffer.allocate(1).put((byte)0x02));
    }

    void receiveWelcome(DatagramPacket packet){
        System.out.println("Receive welcome");
        SocketAddress address = packet.getSocketAddress();
        System.out.println(address);
        neighbours.add(address);
        sendHandshakeMsg(address);
    }

    void sendHandshakeMsg(SocketAddress address){
        System.out.println("Send handshake");
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        if(daddy == null){ //if only 1 node in da tree
            substitutionAddress = (InetSocketAddress) socket.getLocalSocketAddress(); //mb not, dont know how to know self inetsoketaddress
            buffer.put((byte)0x01)
                    .putInt(substitutionAddress.getPort())
                    .putInt(substitutionAddress.getHostName().length())
                    .put(substitutionAddress.getHostName().getBytes());
            substitutionAddress = (InetSocketAddress) address;
            daddy = (InetSocketAddress) address;
        }else{
            buffer.put((byte)0x01)
                    .putInt(daddy.getPort())
                    .putInt(daddy.getHostName().length())
                    .put(daddy.getHostName().getBytes());
        }
        send(address, buffer);
    }

    void receiveHandshake(DatagramPacket packet){
        System.out.println("Receive handshake");
        ByteBuffer buf = ByteBuffer.wrap(packet.getData());
        buf.get();//code
        int port = buf.getInt();
        int host_len = buf.getInt();
        byte[] host_buf = new byte[host_len];
        buf.get(host_buf, 0, host_len);
        String host = new String(host_buf);
        try {
            System.out.println("Update substitution");
            InetSocketAddress sub_addr = new InetSocketAddress(InetAddress.getByName(host), port);
            if(socket.getLocalSocketAddress().equals(sub_addr)){
                substitutionAddress = (InetSocketAddress) packet.getSocketAddress();
                daddy = (InetSocketAddress) packet.getSocketAddress();
            } else substitutionAddress = sub_addr;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

}
