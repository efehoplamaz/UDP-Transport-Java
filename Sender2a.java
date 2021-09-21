import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class Sender2a {

    //Run argument input variables
    static InetAddress IPAddress;
    static String port_number;
    private static String retry_timeout;
    private static int N;

    // Initialize indexes, base and next sequence number.
    // I will be working with the indexes rather than working with a buffer.
    private static int base = 0;
    private static int nextseqnum = 0;

    public static Timer timer = new Timer();
    static DatagramSocket clientSocket;

    static {
        try {
            clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    // GBN_pckt will save all the packets as it is divided in the FileInputStream
    private static List<byte[]> GBN_pckt = Collections.synchronizedList(new ArrayList<byte[]>());


    // TimerThread is the thread which will be triggered if a timeout happens.
    // A Timer object will be scheduled for the retry timeout milliseconds and
    // resend the packets between base till the next sequence number to the server/reciever...
    private static class TimerThread extends TimerTask{

        public void run()
        {
            for(int i = base; i<= nextseqnum; i++)
            {
                byte[] next_pckt_sending= GBN_pckt.get(i);
                DatagramPacket sendPacket = new DatagramPacket(next_pckt_sending, next_pckt_sending.length, IPAddress, Integer.parseInt(port_number));
                try {
                    clientSocket.send(sendPacket);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }


    // ACK Receive Thread will always work until base index will reach at the end of
    // the file. If it recieves an ACK, it will cancel the current scheduled timer and
    // restart a new TimerThread with the retry timeout milliseconds.

    //If the received acknumber is greater or equal to the base then the base will be
    // received acknumber + 1 because receiver will send an acknumber bigger than base
    // if it correctly receives the packet. Process of listening the receiver will continue
    // until we got the last packet's acknumber.

    private static class ACKReceiveThread implements Runnable{

        public void run(){
            byte[] receiveData = new byte[2];

            timer.schedule(new TimerThread(), Integer.parseInt(retry_timeout));

            while(true) {
                    DatagramPacket getack = new DatagramPacket(receiveData, receiveData.length);
                    try {
                        clientSocket.receive(getack);
                        timer.cancel();
                        timer = new Timer();
                        timer.schedule(new TimerThread(), Integer.parseInt(retry_timeout));

                        int acknumber = (getack.getData()[0]<<8) + (getack.getData()[1] & 0xff);

                        if(acknumber >= base){
                            base = acknumber+1;
                        }

                        if (base == GBN_pckt.size()){
                            //totalTime = endTime - startTime;
                            //System.out.println("Totaltime is: " + totalTime);
                            break;
                        }


                    } catch (IOException e) {
                        System.out.println(e.fillInStackTrace());
                        break;
                    }

                //}
            }
        }
    }

    public static void main(String args[]) throws Exception{

        long startTime = System.nanoTime();

        ////////////////////////////////////////////////////////
        // run configuration arguments
        String host_name = args[0];
        port_number = args[1];
        String file_name = args[2];
        retry_timeout = args[3];
        N = Integer.parseInt(args[4]);

        IPAddress = InetAddress.getByName(host_name);


        ////////////////////////////////////////////////////////
        // ACK receive thread start
        ACKReceiveThread ackrec = new ACKReceiveThread();
        Thread t = new Thread(ackrec);
        t.start();


        ////////////////////////////////////////////////////////
        // Main thread: Data (Re)Send

        int counter = 0;
        int duplicate_counter = 0;
        long size = 0;

        try {
            FileInputStream fis = new FileInputStream(file_name);
            size = fis.getChannel().size();
            int i = 0;
                do {
                        byte[] sendData = new byte[1024];
                        i = fis.read(sendData);
                        byte[] concat = new byte[i + 3];

                        byte[] sequence_num = new byte[]{(byte) (counter >> 8), (byte) counter};

                        byte eof;
                        if (i != 1024) {
                            eof = (byte) 1;
                        } else {
                            eof = (byte) 0;
                        }

                        concat[0] = sequence_num[0];
                        concat[1] = sequence_num[1];
                        concat[2] = eof;
                        for (int j = 0; j < i; j++) {
                            concat[j + 3] = sendData[j];
                        }
                        GBN_pckt.add(concat);
                        counter++;
                        if(eof == (byte) 1){
                            break;
                        }
            } while (i != -1);

        }catch(Exception e){
            e.printStackTrace();
        }

        while(!GBN_pckt.isEmpty()){
            if(nextseqnum < base + N && nextseqnum <= GBN_pckt.size()-1) {

                byte[] next_pckt_sending= GBN_pckt.get(nextseqnum);
                DatagramPacket sendPacket = new DatagramPacket(next_pckt_sending, next_pckt_sending.length, IPAddress, Integer.parseInt(port_number));
                clientSocket.send(sendPacket);
                nextseqnum++;

                if(next_pckt_sending[2] == 1)
                {System.out.println("Reached at the end of the file, exiting the sending loop..."); break;}
            }
        }

        //while(base != nextseqnum){}
        //long endTime = System.nanoTime();
        //long seconds = (endTime-startTime)/1000000000;
        //long kilobytes = size/1000;
        //System.out.println(seconds);
        GBN_pckt.clear();
        clientSocket.close();
        System.exit(0);
    }
}