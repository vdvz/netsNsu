import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

public class Client {

    public enum TYPE{
        WELCOME
    }

    static int countTypes = 100;
    static byte[] intToByte = new byte[countTypes];

    Set<Integer> aliveClients = new HashSet<>();

    ActionListener task = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
        }
    };

    public static void main(String[] args) {

        for(int i = 0; i<countTypes; i++){
            intToByte[i] = (byte) 0;
        }

        String ip = args[0];
        int port = 6789;
        try {
            InetAddress groupAddress = InetAddress.getByName(ip);
            MulticastSocket s = new MulticastSocket();
            SocketAddress socketAddr = new InetSocketAddress(groupAddress, port); 
            s.joinGroup(socketAddr, NetworkInterface.getByInetAddress(groupAddress));
            DatagramPacket welcome = welcomeMessage();
            welcome.setSocketAddress(socketAddr);
            s.send(welcome);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static DatagramPacket welcomeMessage(){
        byte msg = intToByte[TYPE.WELCOME.ordinal()];
        return null;
    }

    public void updateMessage(){

    }

    public void parseMsg(){

    }


}
