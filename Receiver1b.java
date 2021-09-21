import java.io.*;
import java.net.*;
import java.util.Arrays; 


public class Receiver1b {
	
	public static void main(String[] args) throws Exception
	{
		String port_number = args[0];
		String file_name = args[1];
		
		
		DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(port_number)); 
		byte[] receiveData = new byte[1027]; 
		byte[] sendData  = new byte[] {0,1}; 
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
		
		//send the sequence number as ACK packet
		byte[] ack = new byte[] {receivePacket.getData()[0], receivePacket.getData()[1]}; 
		DatagramPacket sendPacket = new DatagramPacket(ack, ack.length, IPAddress, receivePacket.getPort()); 
		serverSocket.send(sendPacket);
		
		//check if the current ACK matches with the previous one, if so then don't write it to the file.
		
		if(Arrays.equals(sendData, ack)) {
			continue;
		}
		
		else {
			//the last ack will be the new ack
			sendData = ack;
			//remove the sequence count and EoF from the recieved bytes
			
			//Open a file and write onto it
			File file = null;
			FileOutputStream fileOutputStream = null;
			
			try {
				file = new File(file_name);
				fileOutputStream = new FileOutputStream(file, true);
				//create file if not exists
				if (!file.exists()) {
					file.createNewFile();
				}
				fileOutputStream.write(saved_pack);
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
		}
		
	}
	
	
	
	}
	
}
