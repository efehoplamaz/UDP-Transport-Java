import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Receiver2a {

    public static void main(String[] args) throws Exception
    {
        String port_number = args[0];
        String file_name = args[1];

        ArrayList<byte[]> allPackets = new ArrayList<byte[]>();

        DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(port_number));
        byte[] receiveData = new byte[1027];
        byte[] sendData  = new byte[] {0,1};

        int last_recieved_ack = 0;

        while(true) {

            //recieving data from the client
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            byte[] saved_pack = new byte[receivePacket.getLength()-3];

            for (int i = 0; i<receivePacket.getLength()-3; i++){
                saved_pack[i] = receivePacket.getData()[i+3];
            }

            // configuring the IPAdress and port of the client
            InetAddress IPAddress = receivePacket.getAddress();
            //int port = receivePacket.getPort();
            //System.out.println("Pack recieved!");
            //System.out.println(Arrays.toString(receivePacket.getData()));
            //send the sequence number as ACK packet

            int recently_recieved_ack = (receivePacket.getData()[0]<<8) + (receivePacket.getData()[1] & 0xff);

            if(recently_recieved_ack == last_recieved_ack +1 || recently_recieved_ack == 0)
            {

                // sending the recently recieved ack to sender

                byte[] ack = new byte[] {receivePacket.getData()[0], receivePacket.getData()[1]};
                //System.out.println("With ACK " + ((ack[0]<<8) + (ack[1] & 0xff)));
                DatagramPacket sendPacket = new DatagramPacket(ack, ack.length, IPAddress, receivePacket.getPort());
                serverSocket.send(sendPacket);
                allPackets.add(saved_pack);

                // update last recieved ack to the recently recieved ack

                last_recieved_ack = recently_recieved_ack;

                // if end of the file, then break

                if(receivePacket.getData()[2] == 1){
                    break;
                }


            }
            else
            {

                // send the previous one and don't save anything

                byte[] ack = new byte[]{(byte) (last_recieved_ack >> 8), (byte) last_recieved_ack};
                System.out.println("With ACK " + ((ack[0]<<8) + (ack[1] & 0xff)));
                DatagramPacket sendPacket = new DatagramPacket(ack, ack.length, IPAddress, receivePacket.getPort());
                serverSocket.send(sendPacket);


            }

        }

        // save the recieved packet

        File file = null;
        FileOutputStream fileOutputStream = null;

        try {
            file = new File(file_name);
            fileOutputStream = new FileOutputStream(file, true);
            //create file if not exists
            if (!file.exists()) {
                file.createNewFile();
            }
            for(byte[] pckts: allPackets){
                fileOutputStream.write(pckts);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            //System.out.println("File written successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

        allPackets.clear();
        //System.out.println(file.length());
        System.out.println("Saved the image! Now closing...");
        serverSocket.close();
    }

}
