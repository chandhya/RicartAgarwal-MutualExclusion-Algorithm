
/*Thread that sends Requests, replies and complete messages and keeps track of counts. 
 * It does the required operations and dies out.
 * */
import java.io.IOException;
import java.io.ObjectOutputStream;


public class SendingThread extends Thread{
	int receiver;
	MessageType mt;
	long timeStamp;
	
	
	ObjectOutputStream oos;
	SendingThread(int receiver,MessageType mt, long timeStamp){
		this.receiver = receiver;
		this.mt = mt;
		this.timeStamp = timeStamp;
		
	}
	@Override
	public void run(){
		String timeStr = Node.convertSecondsToHMmSs(timeStamp);
		if(mt.equals(MessageType.INITIATE)){
			Message m1 = new Message(receiver,Node.nodeNo,MessageType.INITIATE,timeStamp,-1);
			oos = Node.outMap.get(receiver);
			try {
				oos.writeObject(m1);
				oos.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//System.out.println("Node "+Node.nodeNo+" sending initiate to "+receiver+" at time "+timeStamp);
	
		}
		if(mt.equals(MessageType.REQUEST)){
			Message m1 = new Message(receiver,Node.nodeNo,MessageType.REQUEST,timeStamp,Node.reqID);
			if((!Node.tokens.containsKey(m1.receiver) || (Node.tokens.containsKey(m1.receiver)&&Node.tokens.get(m1.receiver)==0))){
			//System.out.println("Node "+Node.nodeNo+" sending request to "+receiver+" at time "+timeStamp+" with req ID "+Node.reqID);
			
		//	Node.reqQueue.add(m1);
				
			oos = Node.outMap.get(receiver);
			synchronized(oos){
				try {
					oos.writeObject(m1);
					oos.flush();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Node "+Node.nodeNo+" sending request to "+receiver+" at time "+timeStamp+" with req ID "+Node.reqID);
				Node.sentMsgCount++;
				long[] cache = Node.timeLog.get(Node.reqID);
				cache[3] = Node.sentMsgCount;
				Node.timeLog.put(Node.reqID, cache);
				Node.totalSentCount++;
			
			}
			else{
				
				Node.incrementCount();
				if(Node.getRepCount()== Node.nodeNos.length-1 && Node.reqID>36 && Node.reqQueue.isEmpty()){
					Node.setCountToZero();
					Node.isInCS=true;
					Node.receivedMsgCount =0;
					Node.sentMsgCount =0;
					System.out.println("reply Count now -"+Node.getRepCount());
					Node.enterCS();
				}
			}
		}
		else if(mt.equals(MessageType.REPLY)) {
			
			Message m1 = new Message(receiver,Node.nodeNo,MessageType.REPLY,timeStamp,-1);
			Node.tokens.put(m1.receiver, 0);
			//System.out.println("Node "+Node.nodeNo+" sending reply to "+receiver+" at time "+timeStamp);
			oos = Node.outMap.get(receiver);
			synchronized(oos){
			try {
				oos.writeObject(m1);
				oos.flush();
				//Node.sentMsgCount++;
			} catch (IOException e) {
				
				e.printStackTrace();
				
			}}
			System.out.println("Node "+Node.nodeNo+" sending reply to "+receiver+" at time "+timeStamp);
			Node.totalSentCount++;
		}
		else if(mt.equals(MessageType.COMPLETE)){
			
			Message m1 = new Message(receiver,Node.nodeNo,MessageType.COMPLETE,timeStamp,-1);
			System.out.println("Node "+Node.nodeNo+" sending complete message to "+receiver+" at time "+timeStamp);
			oos = Node.outMap.get(receiver);
			
			try {
				oos.writeObject(m1);
				oos.flush();
			} catch (IOException e) {
				
				e.printStackTrace();
				
			}
		}
		else if(mt.equals(MessageType.NOTIFY)){
			
			Message m1 = new Message(receiver,Node.nodeNo,MessageType.NOTIFY,timeStamp,-1);
			System.out.println("Node "+Node.nodeNo+" sending final notification to "+receiver+" at time "+timeStamp);
			oos = Node.outMap.get(receiver);
			
			try {
				oos.writeObject(m1);
				oos.flush();
			} catch (IOException e) {
				
				e.printStackTrace();
				
			}
		}
		
	}
}
