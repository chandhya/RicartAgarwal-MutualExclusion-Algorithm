/*Thread listens to messages- Requests,replies and completes from all other nodes and does appropriate operations
 * 
 * */
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class ListeningThread extends Thread {
	
	Socket socket;
	ObjectOutputStream oos;
	ObjectInputStream iis;
	
	int nodeNo;
	public ListeningThread(Socket socket, ObjectInputStream iis)
	 {
		this.socket = socket;
		this.nodeNo = Node.getNodeNumber(socket);
		this.iis = iis;
		//System.out.println("I'm listening for messages from Node "+nodeNo);
	}
	@Override
	public void run(){
		while(true)
		{
			try {
				Message m;
				synchronized (iis) {
				 m = (Message)iis.readObject();
				}
				 if(m.mt.equals(MessageType.INITIATE)){
					 System.out.println("Initiate message from node "+m.sender);
					 Node.initiateMsgCount++;
					 if(Node.initiateMsgCount==Node.nodeNos.length-1)
					 Node.canIStart=true;
				 }
				 else if(m.mt.equals(MessageType.REQUEST))
				{
					Node.reqQueue.add(m);
					//Node.receivedMsgCount++;
					//Thread.sleep(2000);
					Node.totalReceivedCount++;
					/*while(true){
						if(!Node.isInCS){
							break;
						}
					}*/
					System.out.println("Request received from node "+m.sender);
					if(!Node.isInCS)
					Node.sendReplies(m);
					Node.resetTime(m.timeStamp);
					
				}
				else if(m.mt.equals(MessageType.REPLY)){
					//Node.repCount++;
					Node.incrementCount();
					Node.tokens.put(m.sender, 1);
					Node.receivedMsgCount++;
					Node.totalReceivedCount++;
					long[] cache = Node.timeLog.get(Node.reqID);
					cache[4] = Node.receivedMsgCount;
					Node.timeLog.put(Node.reqID, cache);
					//System.out.println("Reply Count now -"+Node.repCount);
					System.out.println("Reply received from :"+m.sender);
					boolean isReady =true;
					if(Node.getRepCount() == Node.nodeNos.length-1){
						//Node.repCount =0;
						Node.setCountToZero();
						Node.isInCS=true;
						Node.receivedMsgCount =0;
						Node.sentMsgCount =0;
						System.out.println("reply Count now -"+Node.getRepCount());
						Node.enterCS();
					}
					/*if(Node.repCount >= Node.nodeNos.length-1)
					{
						for(Integer i:Node.tokens.keySet()){
							if(Node.tokens.get(i) == 0){
								long[] temp =Node.timeLog.get(Node.reqID);
								SendingThread st = new SendingThread(i,MessageType.REQUEST,temp[0]);
								st.start();
								isReady =false;
								}
							}
						if(isReady){
							Node.repCount =0;
							Node.isInCS=true;
							Node.receivedMsgCount =0;
							Node.sentMsgCount =0;
						//System.out.println("reply Count now -"+Node.repCount);
							Node.enterCS();
							}
						}*/
					Node.resetTime(m.timeStamp);
				}
				else if(m.mt.equals(MessageType.COMPLETE)){
					Node.completeCount++;
					//System.out.println("Complete count"+Node.completeCount);
					//System.out.println("Final Node - 1" +(Node.finalNodeNo-1));
					if(Node.completeCount==Node.nodeNos.length-1 ) {
						//System.out.println("And it gets in");
						//synchronized(Node.notifyObject1){
							System.out.println("Received all completes -notifying!");
							Node.out.println("Received all completes -notifying!");
							Node.canSendNotify =true;
							//Node.notifyObject1.notify();
						//}
					}
					
				}
				else if(m.mt.equals(MessageType.NOTIFY)){
					Node.notified=true;
					Thread.sleep(2000);
					break;
				}
				if(Node.notified){
					Thread.sleep(2000);
					break;
				}
				
			} 
			 catch (ClassNotFoundException e) {
				e.printStackTrace();
				break;
			} catch (EOFException e) {
				//System.out.println("Excception contains :"+e.getLocalizedMessage());
				//e.printStackTrace();
				break;
			} 
			catch (IOException e) {
				e.printStackTrace();
				break;
			}catch (Exception e) {
				
				e.printStackTrace();
				break;
			}
			
		}
	}
}
