/*Node class is the Main class initiates the entire implementation
 * Initiates client and server thread to start processing
 * Data collection and control of other operations is done in this class
 * 
 * */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Node {
	//numeric constants
	public static final int SERVER_PORT = 1929;
	public static final int intialNodeNo=1;
	public static final int finalNodeNo=12;
	public static final int totalNodes=10;
	public static final int totalCSCount=40;
	public static int startRange1=5;
	public static int endRange1=10;
	public static int startRange2=45;
	public static int endRange2=60;
	public static long timeinCS=30;
	public static long time;
	static Timer clock;

	//Connectivity related variables
	static InetSocketAddress currentNode;
	public static int nodeNo;
	public static int index=0;
	public static String[] nodeNos= new String[totalNodes];
	public static volatile int reqID;
	
	
	//Variables that record the count of replies, requests and msgs exchanged
	public static int repCount;
	public static int currentCSCount;
	public static volatile int completeCount;
	public static volatile int sentMsgCount;
	public static volatile int receivedMsgCount;
	public static volatile int totalSentCount;
	public static volatile int totalReceivedCount;
	public static volatile int initiateMsgCount;

	//Boolean flags that is used to notify state of the node i.e(In critical section, if it has completed etc) 
	public static volatile boolean isComplete=false;
	public static volatile boolean notified=false;
	public static volatile boolean hasExited=false;
	static volatile boolean canSendNotify=false;
	public static volatile boolean isInCS=false;
	public static volatile boolean canIStart=false;
	public static volatile boolean timeFlag =true;
	boolean canIssueReq = false;
	
	//File write related variables
	public static PrintWriter out;
	public static BufferedWriter bfw;
	static String FILE_NAME;
	static String FILE_NAME_AGRR = "Aggregate_Report.txt";
	static File fileAggr = new File(FILE_NAME_AGRR);
		
	//Collections used to ensure concurrency during thread operations
	public static ConcurrentHashMap<Integer, ObjectOutputStream> outMap = new ConcurrentHashMap<Integer, ObjectOutputStream>();
	public static CopyOnWriteArrayList<Message> reqQueue = new CopyOnWriteArrayList<Message>();
	public static ConcurrentHashMap<Integer,long[]> timeLog = new ConcurrentHashMap<Integer,long[]>();
	public static ConcurrentHashMap<Integer,Integer> tokens = new ConcurrentHashMap<Integer,Integer>();
	public static CopyOnWriteArrayList<Socket> connectedNodes = new CopyOnWriteArrayList<Socket>();
	public static CopyOnWriteArrayList<ListeningThread> threadListeners = new CopyOnWriteArrayList<ListeningThread>();
	
	public static void main(String args[]) {
		try {
			currentNode = new InetSocketAddress(InetAddress.getLocalHost().getHostName(),SERVER_PORT);
			nodeNo = Integer.parseInt((currentNode.getHostName().substring(2,4)));
			System.out.println("Node no is - "+nodeNo);
			FILE_NAME = "node_"+nodeNo+".txt";
			
			File file = new File (FILE_NAME);
			out = new PrintWriter(file);
			
			
			if(!fileAggr.exists()){
				fileAggr.createNewFile();
    		}
 
    		//true = append file
    	/*	FileWriter fileWritter = new FileWriter(fileAggr.getName(),true);
    	    bfw = new BufferedWriter(fileWritter);
    	    bfw.write("Test write");
    	    bfw.close();*/
			for(int i=intialNodeNo;i<=finalNodeNo;i++){
			
				if(i!=10 && i!=11){
				if(i<10)
					nodeNos[index] = "0"+String.valueOf(i);
				else
					nodeNos[index] = ""+String.valueOf(i);
				
				index++;
				}
			}
			
			for(int i=0;i<Node.nodeNos.length;i++)
				System.out.println("Node "+Node.nodeNos[i]);
			
			initialiseNetwork();
			while(true){
				//System.out.println(Node.connectedNodes.size());
				//if(Node.connectedNodes.size()==nodeNos.length-1)
				if(Node.connectedNodes.size()==Node.nodeNos.length-1)
					{
					startWork();
					break;
					}
			}
			logData();
			
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		/*for(int i=1;i<=10;i++)
		System.out.println("Value: "+i+" - modulo - "+i%2);*/ catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//Method that handles printing output
	private static void logData() {
		System.out.println("DATA COLLECTION:");
		out.println("DATA COLLECTION:");
		String format = "%-20s%-10s%-10s%n";
		System.out.printf(format, "CS entry","Latency","Msgs exchanged(Req sent+reply received)");
		System.out.println();
		out.printf(format, "CS entry","Latency","Msgs exchanged(Req sent+reply received)");
		out.println();
		int msgsSent=0 ,msgsReceived=0;
		List<Integer> reqIds = new ArrayList<Integer>();
		for(Integer id :timeLog.keySet())
			reqIds.add(id);
		
		Collections.sort(reqIds);
		//for(Integer r:timeLog.keySet()){
		for(Integer r:reqIds){
			long[] cache = timeLog.get(r);
			msgsSent+=cache[3];
			msgsReceived+=cache[4];
			System.out.printf(format, r, String.valueOf(cache[2]-cache[0]), String.valueOf(cache[3]+cache[4]));
			System.out.println();
			out.printf(format, r, String.valueOf(cache[2]-cache[0]), String.valueOf(cache[3]+cache[4]));
			out.println();
		}
		System.out.println("Total Msgs sent :"+Node.totalSentCount);
		System.out.println("\tTotal Requests sent :"+msgsSent);
		System.out.println("\tTotal Replies sent :"+(Node.totalSentCount - msgsSent));
		System.out.println("Total Msgs received :"+Node.totalReceivedCount);
		System.out.println("\tTotal Requests received :"+(Node.totalReceivedCount-msgsReceived));
		System.out.println("\tTotal Replies received :"+msgsReceived);
		System.out.println("Total Msgs exchanged(Requests sent+replies Received) :"+(msgsReceived+msgsSent));
		out.println("Total Msgs sent :"+Node.totalSentCount);
		out.println("\tTotal Requests sent :"+msgsSent);
		out.println("\tTotal Replies sent :"+(Node.totalSentCount - msgsSent));
		out.println("Total Msgs received :"+Node.totalReceivedCount);
		out.println("\tTotal Requests received :"+(Node.totalReceivedCount-msgsReceived));
		out.println("\tTotal Replies received :"+msgsReceived);
		out.println("Total Msgs exchanged((Requests sent+replies Received) :"+(msgsReceived+msgsSent));
		Node.out.close();
		
		
	}
	//Method that starts the client and server threads and initiates requests 
	private static void startWork() {
		//SendingThread st;
		for(int k=0;k<Node.threadListeners.size();k++){
			ListeningThread lt = threadListeners.get(k);
			lt.start();
		}
		System.out.println("Initializing network..");
		for(int j=0;j<Node.nodeNos.length;j++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long timeStamp = Node.getTime();
			int receiver = Integer.parseInt(Node.nodeNos[j]);
			if(nodeNo != receiver){
				SendingThread st = new SendingThread(receiver,MessageType.INITIATE,timeStamp);
				st.start();
				}
		}
		clock = new Timer();
		clock.schedule(new ClockTick(), 0, 1);
		while(true){
			if(canIStart)
			break;
		}
		try {
		for(int i=0;i<totalCSCount;i++) {
			Node.reqID++;
			//long timeStamp = Node.getTime();
			Node.tick();
			long timeStamp = Node.getTime();
			String timeStr = Node.convertSecondsToHMmSs(timeStamp);
			long[] timeElapsed = new long[5];
			timeElapsed[0] = timeStamp; 
			timeElapsed[1] = 1;
			
			Node.timeLog.put(Node.reqID, timeElapsed);
			
			for(int j=0;j<Node.nodeNos.length;j++) {
			int receiver = Integer.parseInt(Node.nodeNos[j]);
			if(nodeNo != receiver){
			SendingThread st = new SendingThread(receiver,MessageType.REQUEST,timeStamp);
			st.start();
			
			}
			}
			while(true){
				if(hasExited)
					{
					hasExited =false;
					break;
					}
			}
			Random random = new Random();
		   if(Node.reqID>20 && nodeNo%2==0) 
			 {
			   int randInt = getRandomInteger(startRange2,endRange2,random);
			   Thread.sleep(randInt*10);
			   //System.out.println("Shouldnt get in here!");
			 }
			else {
				int randInt = getRandomInteger(startRange1,endRange1,random);
				Thread.sleep(randInt*10);
			}
			//Thread.sleep(1000);
		}
		
		while(true){
		if(currentCSCount==totalCSCount && Node.reqQueue.isEmpty())
		{
		Thread.sleep(2000);
		isComplete =true;
		clock.cancel();
		sendCompleteMsgs();
		break;
		}
		}
			while(true){
			if(notified){
				
				Thread.sleep(2000);
				break;
			}
			
			}
			/*
			while(true) {
				if(Node.reqQueue.size()==4){
					//for(int i=0;i<Node.reqQueue.size();i++){
						Iterator<Message> itrm = Node.reqQueue.iterator();
						while(itrm.hasNext()){
						Message m= 	itrm.next();
					 	System.out.println(m.timeStamp);
					 	
					}
					break;
				}
			}*/
			
			} catch (Exception e) {
			
				e.printStackTrace();
			}
		//}
		
	}
	//Method to send replies after each request(decided whether or not to send reply or defer reply)
	static void sendReplies(Message m) {
		boolean amIinQueue=false;
		Node.tick();
		//if(timeLog.containsKey(reqID) && !isInCS)
		if(!isInCS)
		{
			if(!timeLog.isEmpty()) {
			 for(Integer i:timeLog.keySet()){
				 long[] tsInfo =new long[5];
				 tsInfo= timeLog.get(i);
				// System.out.println("My ts - "+tsInfo[0]);
					//System.out.println("Incoming req ts - "+m.timeStamp);
					//System.out.println("Is my request still on? "+tsInfo[1]);
					if(tsInfo[1]==1 && (tsInfo[0]<m.timeStamp ||  (tsInfo[0]==m.timeStamp && nodeNo<m.sender))) {
						amIinQueue=true;
						break;
					}
					
			 }
			 if(!amIinQueue){
				 if(Node.reqQueue.remove(m)){
				 	 long timeStamp = Node.getTime();
				 //System.out.println("Sending Reply");
				 SendingThread st = new SendingThread(m.sender,MessageType.REPLY,timeStamp);
				st.start();
				 }
				
			 }
			}
			else{
				Node.reqQueue.remove(m);
				long timeStamp = Node.getTime();
				//System.out.println("Sending Reply");
				SendingThread st = new SendingThread(m.sender,MessageType.REPLY,timeStamp);
				st.start();
					
				
			}
			/*if(timeLog.containsKey(reqID)){
			//System.out.println("Req ID exists!");
			long[] tsInfo =new long[3];
			tsInfo= timeLog.get(reqID);
			System.out.println("My ts - "+tsInfo[0]);
			System.out.println("Incoming req ts - "+m.timeStamp);
			System.out.println("Is my request still on? "+tsInfo[1]);
			if(tsInfo[1]==0 ||(tsInfo[1]==1 && tsInfo[0]>m.timeStamp)) {
				st = new SendingThread(m.sender,MessageType.REPLY);
				st.start();
				Node.reqQueue.remove(m);
			}
			}
			else {
				st = new SendingThread(m.sender,MessageType.REPLY);
				st.start();
				Node.reqQueue.remove(m);	
			}*/
		} 
		
	}
	//Method that sends replies post critical section entry
	static synchronized void sendReplies(){
		 
		while(!Node.reqQueue.isEmpty())
			{
		//	System.out.println("Requeue size - "+Node.reqQueue.size());
			Message m;
			//System.out.println("It could be blocked here");
			//m = Node.reqQueue.take();
			m = Node.reqQueue.remove(0);
			Node.tick();
			//long timeStamp = Node.getTime();
			long timeStamp = Node.getTime();
			//System.out.println("Reply post CS");
			SendingThread st = new SendingThread(m.sender,MessageType.REPLY,timeStamp);
			st.start();
				
				//Node.reqQueue.remove(m);
			//}
			/*for(long[] value:timeLog.values()) {
				if(value[0]>m.timeStamp)
				{st = new SendingThread(m.sender,MessageType.REPLY);
				st.start();
				
					Node.reqQueue.take();
				}
				} */
	}
		//System.out.println("currentCSCount -"+currentCSCount);
		//System.out.println("totalCSCount -"+totalCSCount);
		/*if(currentCSCount==totalCSCount)
			{
			isComplete =true;
			sendCompleteMsgs();
			}*/
	}
	//Method that sends complete messages after finishing work
	private static void sendCompleteMsgs() {
		SendingThread st2;
		if(nodeNo != intialNodeNo){
			//long timeStamp = Node.getTime();
			Node.tick();
			long timeStamp = Node.getTime();
			st2 = new SendingThread(intialNodeNo,MessageType.COMPLETE,timeStamp);
			st2.start();
		}
		else {
			
			/*synchronized (notifyObject1)
			{
				
			   while (!canSendNotify){
				   System.out.println("In wait mode!");
			      try
			      {
			    	  notifyObject1.wait();
			      }
			      catch (InterruptedException e) {}
			   }
			}*/
			while(true){
				if(canSendNotify) {
			for(int k=0;k<Node.nodeNos.length;k++){
				int receiver = Integer.parseInt(Node.nodeNos[k]);
				if(receiver != 1){
					System.out.println("Now sending notification");
					//long timeStamp = Node.getTime();
					Node.tick();
					long timeStamp = Node.getTime();
					st2 = new SendingThread(receiver,MessageType.NOTIFY,timeStamp);
					st2.start();
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Node.notified=true;
				}
			}
			break;
			}
			
			}
			
		}
		
	}
	//Method that holds performs operations on critical section entry
	public static synchronized void enterCS() {
		try {
		isInCS=true;
		currentCSCount++;
		System.out.println("Entering CS - Node "+nodeNo);
		out.println("Entering CS - Node "+nodeNo);
		FileWriter fw = new FileWriter(fileAggr,true);
		//bfw = new BufferedWriter(fw);
		fw.write("Entering CS - Node "+nodeNo+"\n");
		//long timeStamp = Node.getTime();
		long timeStamp = Node.getTime();
		String timeStr = Node.convertSecondsToHMmSs(timeStamp);
		System.out.println("Current physical time: "+Node.getSysTime());
		out.println("Current physical time: "+Node.getSysTime());
		fw.write("Current physical time: "+Node.getSysTime()+"\n");
		Thread.sleep(timeinCS);
		if(!timeLog.isEmpty()) {
			 for(Integer i:timeLog.keySet()) {
				 long[] cache =new long[5];
				 cache= timeLog.get(i);
				 	if(cache[1]==1)
					 {
				 	cache[1] =0; 
				 	cache[2] = timeStamp;
				 	timeLog.put(i, cache);
				 	break;
					 }
				 	else 
				 	 continue;
				 }
			 }
		/*if(Node.timeLog.containsKey(Node.reqID)){
			long[] cache = new long[3];
			cache =timeLog.get(reqID);
			cache[1] = 0;
			cache[2] = timeStamp;
			timeLog.put(reqID, cache);
		}*/
		long timeStampEnd = Node.getTime();
		String timeStrEnd = Node.convertSecondsToHMmSs(timeStampEnd);
		System.out.println("Current physical time: "+Node.getSysTime());
		out.println("Current physical time: "+Node.getSysTime());
		fw.write("Current physical time: "+Node.getSysTime()+"\n");
		fw.close();
		//Node.repCount=0;
		
		sendReplies();
		isInCS=false;
		hasExited =true;
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	//Method that returns current system time/logical time
	static long getTime() {
		if(timeFlag)
		return  System.currentTimeMillis();
		else
		return time;
	}
	//Method that returns current system time only
	static long getSysTime() {
		
		return  System.currentTimeMillis();
		
	}
	//Method that spawns the client and server thread
	private static void initialiseNetwork() {
		Thread t1 = new Server();
		Thread t2 = new Client();
		t1.start();
		t2.start();
			
	}
	//Method that gets the node number based on node
	 static int getNodeNumber(Socket connectedNode) {
			if(connectedNode!=null)
			{
			int x = Integer.parseInt((connectedNode.getInetAddress().getHostName().substring(2,4)));
			return x;
			}
			return 0;
		}
	 //Method that converts physical time to hr/min/s format
	 public static String convertSecondsToHMmSs(long seconds) {
		    long s = seconds % 60;
		    long m = (seconds / 60) % 60;
		    long h = (seconds / (60 * 60)) % 24;
		    return String.format("%d:%02d:%02d", h,m,s);
		}
	 //Method to generate random numbers in a certain range - Source :http://www.javapractices.com/topic/TopicAction.do?Id=62
	  private static int getRandomInteger(int aStart, int aEnd, Random aRandom){
		    if (aStart > aEnd) {
		      throw new IllegalArgumentException("Start cannot exceed End.");
		    }
		    //get the range, casting to long to avoid overflow problems
		    long range = (long)aEnd - (long)aStart + 1;
		    // compute a fraction of the range, 0 <= frac < range
		    long fraction = (long)(range * aRandom.nextDouble());
		    int randomNumber =  (int)(fraction + aStart);    
		   return randomNumber;
		  }

	//Synchronized methods to update values that are changed by multiple threads
	public synchronized static void tick() {
		time++;
	}
	static synchronized void incrementCount(){
		repCount++;
	}
	static int getRepCount(){
		return repCount;
	}
	static synchronized void setCountToZero(){
		repCount=0;
	}
	public synchronized static void resetTime(long ts){
	Node.tick();
	Node.time = Node.time<ts?ts:Node.time;
	}
}

