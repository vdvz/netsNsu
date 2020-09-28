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
    private String ip;
    private static final int port = 6789;
    private InetAddress groupAddress;

    private  void sendWelcomeMessage(){
        byte[] buf = new byte[1000];
        buf[0] = 1;
        DatagramPacket welcome = new DatagramPacket(buf, buf.length, groupAddress, port);
        welcome.setData(buf);
        try {
            s.send(welcome);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setIp(String _ip){
        ip = _ip;
    }

    public void run(){
        System.out.println("My: " + ip);
        try {
            groupAddress = InetAddress.getByName(ip);
            s = new MulticastSocket(port);
            s.joinGroup(groupAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Timer timer1 = new Timer(3000, checkClients);
        Timer timer2 = new Timer(500, sendWelcome);
        timer1.start();
        timer2.start();

        while(true){
            byte[] buf = new byte[1000];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            try {
                s.receive(dp);
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
        }
    }

    private  void checkIfClientAlive(){
        for (Map.Entry<String, Integer> entry: aliveClients.entrySet()) {
            int validLoose = 0;
            if(entry.getValue() == validLoose) {
                String key = entry.getKey();
                System.out.println("Client disconnect: " + key);
                aliveClients.remove(key);
            }
        }

        for (Map.Entry<String, Integer> entry: aliveClients.entrySet()) {
            aliveClients.replace(entry.getKey(),0);
        }
    }
}
