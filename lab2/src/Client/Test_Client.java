package Client;

public class Test_Client {

    public static void main(String[] args) {

        String file_name = args[0];
        String ip = args[1];
        int port = Integer.parseInt(args[2]);

        new Thread(new Client(file_name, ip, port)).start();
        //System.out.println("Second");
        //new Thread(new Client()).start();

    }

}
