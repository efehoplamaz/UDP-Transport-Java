import java.io.*;
import java.net.*;
import java.util.*;

public class Sender2b {

    private static int retry_timeout;

    private static int base = 0;
    private static int nextseqnum = 0;
    private static int N;

    private static long startTime = 0;
    private static long endTime = 0;

    static DatagramSocket clientSocket;
    public static Timer timer = new Timer();

    static {
        try {
            clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private static List<byte[]> SR_sender = Collections.synchronizedList(new ArrayList<byte[]>());
    private static ArrayList<Integer> SR_acknumber = new ArrayList<>();
    private static HashMap<Integer, TimerThread> timerBuffer = new HashMap<Integer, TimerThread>();


    private static class TimerThread extends TimerTask{

        private DatagramPacket packet;

        private TimerThread(DatagramPacket packet){

            this.packet = packet;

        }
        public void run()
        {
            try {
                System.out.println("Time out...");
                int packetnumber = (packet.getData()[0]<<8) + (packet.getData()[1] & 0xff);
                TimerThread timeout = new TimerThread(packet);
                timer.schedule(timeout, retry_timeout);
                timerBuffer.put(packetnumber, timeout);
                System.out.println("Resend: " + Arrays.toString(packet.getData()));
                clientSocket.send(packet);
            }
            catch (IOException e){
                e.printStackTrace();
            }

        }
    }


    private static class ACKReceiveThread implements Runnable{

        public void run(){
            byte[] receiveData = new byte[2];

            while(true) {
                DatagramPacket getack = new DatagramPacket(receiveData, receiveData.length);
                try {
                    System.out.println("Data recieved");
                    clientSocket.receive(getack);

                    int acknumber = (getack.getData()[0]<<8) + (getack.getData()[1] & 0xff);

                    if (timerBuffer.containsKey(acknumber)){
                        timerBuffer.get(acknumber).cancel();
                        timerBuffer.remove(acknumber);
                    }

                    if(acknumber == base){
                        SR_acknumber.remove(SR_acknumber.indexOf(base));
                        base++;
                        while(SR_acknumber.contains(base)){

                            SR_acknumber.remove(SR_acknumber.indexOf(base));
                            base++;

                        }
                    }

                    else if(acknumber > base && acknumber < base + N){

                        SR_acknumber.add(acknumber);

                    }

                    else{System.out.println("Ack number less then base !!!!");}

                    if (base == SR_sender.size()){
                        System.out.println("Finished recieving ACKs with base "+ base);
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

        startTime = System.nanoTime();

        ////////////////////////////////////////////////////////
        // run configuration arguments
        String host_name = args[0];
        String port_number = args[1];
        String file_name = args[2];
        retry_timeout = Integer.parseInt(args[3]);
        N = Integer.parseInt(args[4]);

        InetAddress IPAddress = InetAddress.getByName(host_name);


        ////////////////////////////////////////////////////////
        // Timer and ACK receive threads
        ACKReceiveThread ackrec = new ACKReceiveThread();

        Thread t2 = new Thread(ackrec);

        t2.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //System.out.println("ACK Recieving started!");


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
                SR_sender.add(concat);

                DatagramPacket packet = new DatagramPacket(concat, concat.length, IPAddress, Integer.parseInt(port_number));
                int acknumber = (sequence_num[0] << 8) + (sequence_num[1] & 0xff);
                clientSocket.send(packet);

                TimerThread timeout = new TimerThread(packet);
                timer.schedule(timeout, retry_timeout);
                //System.out.println("Just sent :" + acknumber);
                timerBuffer.put(acknumber, timeout);
                nextseqnum++;
                counter++;

                if(eof == (byte) 1){
                    break;
                }
            } while (i != -1);

        }catch(Exception e){
            e.printStackTrace();
        }


        // Throughput

        SR_sender.clear();
        SR_acknumber.clear();
        timerBuffer.clear();
        clientSocket.close();
        System.exit(0);
    }

}
