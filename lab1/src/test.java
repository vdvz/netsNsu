import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class test {

    static ActionListener sendWelcome = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            printMsg();
        }
    };


    public static void main(String[] args) {
        Timer timer = new Timer(1000, sendWelcome);
        timer.start();
        try {
            System.out.println("Thread blocked");
            Thread.currentThread().sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return;
    }

    private static void printMsg(){
        System.out.println("Hello");
    }

}
