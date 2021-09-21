import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Receiver1a {

	public static void main(String[] args) throws Exception
	{
		
		String port_number = args[0];
		String file_name = args[1];
		
		DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(port_number)); 
		byte[] receiveData = new byte[1027];
		int counter = 0;
		while(true) {
			System.out.println(counter);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
			serverSocket.receive(receivePacket);
			byte[] saved_pack = new byte[receivePacket.getLength()-3];
			
			for (int i = 0; i<receivePacket.getLength()-3; i++){
				saved_pack[i] = receivePacket.getData()[i+3];
			}
			
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
				System.out.println("File written successfully.");
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
			
			counter++;
		}
		}
		
		
	
	
}
