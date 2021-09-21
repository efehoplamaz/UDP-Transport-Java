import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit; 


public class Sender1b {
	
	public static void main(String args[]) throws Exception {
		
		//Input arguments
		String host_name = args[0];
		String port_number = args[1];
		String file_name = args[2];
		String retry_timeout = args[3];

		// Start client socket
		DatagramSocket clientSocket = new DatagramSocket(); 
		InetAddress IPAddress = InetAddress.getByName(host_name);
		
		int counter = 0;
		int duplicate_counter = 0;
		long size = 0;
		
		
		try {
			FileInputStream fis = new FileInputStream(file_name);
			size = fis.getChannel().size();
			int i = 0;
			long startTime = System.nanoTime();
            do {
            		
                byte[] sendData = new byte[1024];
                i = fis.read(sendData);
            	            
                byte[] concat = new byte[i+3];
            	
            	byte[] sequence_num = new byte[] {(byte)(counter>>8),(byte) counter};
            	
                byte[] receiveData = new byte[2];
                
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
        		        		
        		clientSocket.setSoTimeout(Integer.parseInt(retry_timeout));
        		while(true) {
        		    DatagramPacket getack = new DatagramPacket(receiveData, receiveData.length);
        		    try {
        		    	clientSocket.receive(getack);
        		    	
        		    	if(Arrays.equals(getack.getData(), sequence_num)) {
        		    		break;
        		    	}
        		    	else{
        		    		continue;
        		    	}
        		    } catch (SocketTimeoutException e) {
        		       // resend if ACK not recieved
        		    	clientSocket.send(sendPacket);
        		    	duplicate_counter++;
        		        continue;
        		    }

        		}
        		counter += 1;
        	
            } while (i != -1);
            long endTime   = System.nanoTime();
            long totalTime = endTime - startTime;
			System.out.println(duplicate_counter + " " + (size/1000)/(totalTime/1000000000));
            
		}catch(Exception e){
			e.printStackTrace();
		}
		
		clientSocket.close();
		
		
	}


}
