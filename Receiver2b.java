import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class Receiver2b {

    public static void main(String args[]) throws Exception {

        String port = args[0];
        String filename = args[1];
        int window_size = Integer.parseInt(args[2]);
        int base = 0;

        HashMap<Integer, byte[]> SR_pckts = new HashMap<>();
        ArrayList<byte[]> allPackets = new ArrayList<byte[]>();

        DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(port));

        byte[] receiveData = new byte[1027];

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            InetAddress IPAddress = receivePacket.getAddress();

            byte[] receivedArray = receivePacket.getData();
            byte[] saved_pack = new byte[receivePacket.getLength() - 3];

            //if duplicate detected, then do not write duplicate packet into output file.
//            if(Arrays.equals(ackData, lastAck)) {
//                System.out.println("Duplicate Found!");
//                continue;
//            }
            int acknumber = (receivedArray[0] << 8) + (receivedArray[1] & 0xff);
            //System.out.println("Recieved Packet with sequence number" + receiveSeqNumber);
            if (acknumber >= base+window_size) continue; // after the window, so ignore


            byte[] ackData = new byte[]{receivedArray[0], receivedArray[1]}; // sequence number goes into ackdata
            DatagramPacket sendAck = new DatagramPacket(ackData, ackData.length, IPAddress, receivePacket.getPort());

            //send ack packet to client
            serverSocket.send(sendAck);

            // remove headers from packet and store image data into packetData variable
            for (int i = 0; i < receivePacket.getLength() - 3; i++) {
                saved_pack[i] = receivedArray[i + 3];
            }
            // if received packet is within the window
            if (acknumber >= base && acknumber < base + window_size) {
                //if received packet has been received before then do nothing(ack has been sent already)
                if(SR_pckts.containsKey(acknumber)) {
                    continue;
                }
                if(base == acknumber) {

                    allPackets.add(saved_pack);
                    base++;

                    for(int i = base; i<base+window_size-1; i++){
                        if (SR_pckts.containsKey(i)) {
                            allPackets.add(SR_pckts.get(i));
                            base++;
                        }
                        else{
                            break;
                        }
                    }
                }
                SR_pckts.put(acknumber, saved_pack);
                System.out.println("Base updated: "  + base);
            }

            if(receivePacket.getData()[2] == 1){
                System.out.println("BREAKKK");
                break;
            }
        }

        //write to file
        File file = null;
        FileOutputStream fileOutputStream = null;
        try {

            file = new File(filename);
            fileOutputStream = new FileOutputStream(file, true);
            for(byte[] pckts: allPackets){
                fileOutputStream.write(pckts);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

        allPackets.clear();
        System.out.println("Saved the image! Now closing...");
        serverSocket.close();
    }
}
