import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Client {

    private static int countTypes = 100;
    private static byte[] intToByte = new byte[countTypes];

    private static Map<String, Integer> aliveClients = new HashMap<>();

    private static ActionListener sendWelcome = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            sendWelcomeMessage();
        }
    };

    private static void sendWelcomeMessage(){
        byte[] buf = new byte[1000];
        DatagramPacket welcome = new DatagramPacket(buf, buf.length);
        buf[0] = 1;
        welcome.setSocketAddress(socketAddr);
        try {
            s.send(welcome);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static MulticastSocket s;
    private static MulticastSocket r;
    private static SocketAddress socketAddr;

    public static void main(String[] args) {

        for(int i = 0; i<countTypes; i++){
            intToByte[i] = (byte) 0;
        }

        String ip = args[0];
        System.out.println("My: " + ip);
        int port1 = 6789;
        int port2 = 6790;
        try {
            InetAddress groupAddress = InetAddress.getByName(ip);
            s = new MulticastSocket();
            r = new MulticastSocket();
            socketAddr = new InetSocketAddress(groupAddress, port1);
            SocketAddress socketAddr2 = new InetSocketAddress(groupAddress, port2);
            s.joinGroup(socketAddr, NetworkInterface.getByInetAddress(groupAddress));
            r.joinGroup(socketAddr2, NetworkInterface.getByInetAddress(groupAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Timer timer1 = new Timer(1000, checkClients);
        Timer timer2 = new Timer(1000, sendWelcome);

        while(true){
            byte[] buf = new byte[1000];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            try {
                r.receive(dp);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String host = dp.getAddress().getHostName();
            if(!aliveClients.containsKey(host)){
                System.out.println("Client connected: " + host);
                aliveClients.put(host, 1);
            }else {
                Integer i = aliveClients.get(host) + 1;
                aliveClients.replace(host, i);
            }
        }
    }

    private static ActionListener checkClients = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            checkIfClientAlive();
        }
    };

    private static void checkIfClientAlive(){
        for (Map.Entry<String, Integer> entry: aliveClients.entrySet()) {
            int validLoose = 10;
            if(entry.getValue()< validLoose){
                System.out.println("Client disconnect: " + entry.getValue());
            }
        }
    }

}
