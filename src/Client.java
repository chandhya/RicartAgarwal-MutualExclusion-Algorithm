import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class Client extends Thread{
	Socket clientSocket;
	ObjectOutputStream oos ;
	 ;
	@Override
	public void run(){
		
		for(int i=0;i<Node.nodeNo-1;i++) {
			if(Node.connectedNodes.size()==Node.nodeNos.length-1)
				break;
		while (true) {
			try {
				//clientSocket = new Socket("dc0" + Node.jFromFile[i] + ".utdallas.edu",Node.SERVER_PORT);
				
				clientSocket = new Socket("dc" +Node.nodeNos[i]+ ".utdallas.edu",Node.SERVER_PORT);
				if (clientSocket == null) 
				{
				Thread.sleep(1000);
				} 
				else 
				{
				System.out.println("Client made connection with Node: "+ Node.getNodeNumber(clientSocket));
				
				oos = new ObjectOutputStream(clientSocket.getOutputStream());
				Node.outMap.put(Node.getNodeNumber(clientSocket),oos);
				ObjectInputStream iis = new ObjectInputStream(clientSocket.getInputStream());
				ListeningThread lt = new ListeningThread(clientSocket,iis);
				Node.threadListeners.add(lt);
				Node.connectedNodes.add(clientSocket);
				
				break;
				}
				} 
				catch (Exception e) {
				//	e.printStackTrace();	
				}
			}
	
		}
		
		while(true){
			if(Node.notified)
				{
				System.out.println("Client socket to be closed");
				break;
				
				}
			
		}
		
	}

}
