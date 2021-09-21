import java.util.Arrays;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.lang.Thread;


public class Sender1a {
	
	public static void main(String[] args) throws Exception
	{
		
		String host_name = args[0];
		String port_number = args[1];
		String file_name = args[2];
		
		DatagramSocket clientSocket = new DatagramSocket(); 
		InetAddress IPAddress = InetAddress.getByName(host_name);
		
		int counter = 0;
		
		try {
			
			FileInputStream fis = new FileInputStream(file_name);
            int i = 0;
            do {
            	
                byte[] sendData = new byte[1024];
                i = fis.read(sendData);
            	
                byte[] concat = new byte[i+3];
            	
            	byte[] sequence_num = new byte[] {(byte)(counter>>8),(byte) counter};

                byte eof;
                if (i == -1){
                	eof = (byte) 1;
                	break;
                }
                else {
                	 eof = (byte) 0;
                }
                
                concat[0] = sequence_num[0];
                concat[1] = sequence_num[1];
                concat[2] = eof;
                for(int j = 0; j<i; j++){
                	concat[j+3] = sendData[j];
                }
                
        		DatagramPacket sendPacket = new DatagramPacket(concat, concat.length, IPAddress, Integer.parseInt(port_number)); 
        		clientSocket.send(sendPacket);
        		TimeUnit.MILLISECONDS.sleep(130);
        		counter += 1;
            } while (i != -1);
        }
		 catch (Exception e) {
				e.printStackTrace();
		 }
		clientSocket.close();
		
	}
	
	
}
