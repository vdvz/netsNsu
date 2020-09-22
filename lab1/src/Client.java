import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Client extends Thread{

    private final Map<String, Integer> aliveClients = new HashMap<>();

    private final ActionListener sendWelcome = e -> sendWelcomeMessage();
    private final ActionListener checkClients = e -> checkIfClientAlive();


    private  MulticastSocket s;
    private  MulticastSocket r;
    private  SocketAddress socketAddr;


    private  void sendWelcomeMessage(){
        byte[] buf = new byte[1000];
        buf[0] = 1;
        DatagramPacket welcome = new DatagramPacket(buf, buf.length, groupAddress, port);
        welcome.setData(buf);
        //welcome.setSocketAddress(socketAddr);
        try {
            s.send(welcome);
            System.out.println("Message send");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String ip;
    public void setIp(String _ip){
        ip = _ip;
    }
    InetAddress groupAddress;
    int port = 6789;
    public void run(){
        System.out.println("My: " + ip);
        int port1 = 6789;
        int port2 = 6790;
        try {
            groupAddress = InetAddress.getByName(ip);
            s = new MulticastSocket(6789);
            r = new MulticastSocket(port2);
            //socketAddr = new InetSocketAddress(groupAddress, port1);
            //SocketAddress socketAddr2 = new InetSocketAddress(groupAddress, port2);
            s.joinGroup(groupAddress);
            r.joinGroup(groupAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Timer timer1 = new Timer(10000, checkClients);
        Timer timer2 = new Timer(1000, sendWelcome);
        timer1.start();
        timer2.start();

        //while(true){
        byte[] buf = new byte[1000];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        try {
            System.out.println("try block");
            s.receive(dp);
            System.out.println("receive");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String host = dp.getAddress().getHostName();
        if(!aliveClients.containsKey(host)){
            System.out.println("Client connected: " + host);
            aliveClients.put(host, 1);
        }else {
            aliveClients.replace(host, aliveClients.get(host) + 1);
        }
        //}
    }

    private  void checkIfClientAlive(){
        for (Map.Entry<String, Integer> entry: aliveClients.entrySet()) {
            int validLoose = 0;
            if(entry.getValue() == validLoose) {
                System.out.println("Client disconnect: " + entry.getValue());
                aliveClients.remove(entry);
            }
        }

        for (Map.Entry<String, Integer> entry: aliveClients.entrySet()) {
            aliveClients.replace(entry.getKey(),0);
        }

    }

}
