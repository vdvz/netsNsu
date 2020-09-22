public class Main {


    public static void main(String[] args) {
        Client client = new Client();
        client.setIp(args[0]);
        client.start();
    }
}

