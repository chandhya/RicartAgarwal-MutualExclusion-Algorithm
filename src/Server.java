/*Server thread that accepts connections and spawns listening thread and retains one instance of ObjectOutput/Input Streams
 * Listening thread is spawn once it has accepted connection.
 * Closes server socket once it has received all complete messages
 * */
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
public class Server extends Thread{
	 Socket socket;
	 
	 ObjectOutputStream oos;
	 
	 @Override
	 public void run(){
		 ServerSocket serverSocket=null;
			try {
				serverSocket = new ServerSocket(Node.SERVER_PORT);
				int count =0;
				for(count=0;count<Node.nodeNos.length-Node.nodeNo;count++) {
		    			
						socket = serverSocket.accept();
						
						oos = new ObjectOutputStream(socket.getOutputStream());
						Node.outMap.put(Node.getNodeNumber(socket),oos);
						ObjectInputStream iis = new ObjectInputStream(socket.getInputStream());
						ListeningThread lt = new ListeningThread(socket,iis);
						Node.threadListeners.add(lt);
						Node.connectedNodes.add(socket);
					
				
				}
				System.out.println("No of connections accepted at Server :"+count);
				while(true){
					if(Node.notified)
					{
						System.out.println("Server socket to be closed!");
						serverSocket.close();
						break;
					}
				}
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	 }
}
