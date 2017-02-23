import java.sql.Timestamp;
import java.util.Random;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Iterator;

public class Simulator{
	public static void main(String[] args){

		int numPeers = Integer.parseInt(args[0]);

		double minPropDelay = 10;
		double maxPropDelay = 500;
		ArrayList<Node> nodeList = new ArrayList<Node>();

		//List to store all the accepted blockchain so far
		ArrayList<Block> blockChain = new ArrayList<Block>();
		long numBlock = 0;

		//List to store all the unincluded transaction of the network
		LinkedList<Transaction> unIncludedTxns = new LinkedList<Transaction>();
		long numUtxns = 0; 

		//Genesys Block
		Timestamp genesisTime = new Timestamp(System.currentTimeMillis());
		Block genesisBlock = new Block("genesis", genesisTime, "satoshi", null);
		numBlock++;
		blockChain.add(genesisBlock);

		//Generating numPeers number of nodes with randomly choosing fast and slow property
		//type true for fast nodes and false for lazy nodes
		Random randType = new Random(System.nanoTime());
		for(int i=0; i<numPeers; i++){
			String nodeID = "Node_"+i;
			boolean type = (randType.nextInt()%2==0);
			Timestamp creationTime = new Timestamp(System.currentTimeMillis());
			Node newNode = new Node(nodeID, type, creationTime, genesisBlock);
			nodeList.add(i,newNode);
		}

		//Creating a 2D array to store Propagation delay between each pair of nodes
		Double[][] propagationDelay  = new Double[numPeers][numPeers];
		Random randProp = new Random(System.nanoTime());
		for(int i=0; i<numPeers; i++){
			for(int j=0; j<numPeers; j++){
				if(i<=j){
					double propDelay = minPropDelay + randProp.nextDouble()*(maxPropDelay - minPropDelay);
					propagationDelay[i][j] = propDelay;
				}
				else{
					propagationDelay[i][j] = propagationDelay[j][i];
				}
				
			}
		}

		//Creating a array to store the bottle neck link between each pair of nodes
		Double[][] bottleNeck = new Double[numPeers][numPeers];
		for(int i=0; i<numPeers; i++){
			for(int j=0; j<numPeers; j++){
				if(nodeList.get(i).getType() && nodeList.get(j).getType())
					bottleNeck[i][j] = 100.0;
				else
					bottleNeck[i][j] = 5.0;
			}
		}

		//Assigning mean to generate T_k later for each node from an exponential distribution
		Double[] cpuPower = new Double[numPeers];
		Random randCpu = new Random(System.nanoTime());
		for(int i=0; i<numPeers; i++){
			double cpuMean = 1 + randCpu.nextDouble()*100;
			cpuPower[i] = cpuMean;
		}


		//Assigning mean to generate transaction later for each node from an exponential distribution
		Double[] txnMean = new Double[numPeers];
		Random randMean = new Random(System.nanoTime());
		for(int i=0; i<numPeers; i++){
			double tempTxnMean = 10 + randMean.nextDouble()*1;
			txnMean[i] = tempTxnMean;
		}


		//Probable Tree of the blockchain broadcasted so far

		//Priortiy Queue of events to be executed
		PriorityQueue<Event> pendingEvents = new PriorityQueue<Event>();
		//Priority Queue of events executed so far
		PriorityQueue<Event> finishedEvents = new PriorityQueue<Event>();

		long simTime = 1000*200;
		Timestamp currTime = new Timestamp(System.currentTimeMillis());
		Timestamp startTime = currTime;
		long currTimeOffset = currTime.getTime();
		Timestamp maxTime = new Timestamp(currTimeOffset + (long)(Math.random()*simTime));

		for(int i=0; i<numPeers; i++){
			long nextTxnLong = (long)(10000*txnMean[i]);
			Timestamp nextTxnTime = new Timestamp(currTimeOffset + (long)(Math.random()*nextTxnLong));
			nodeList.get(i).nextTxnTime = nextTxnTime;
			// System.out.println(nextTxnTime);
			//
			Random receiveRand = new Random(System.nanoTime());
			int rcvNum = receiveRand.nextInt(numPeers);
			while(rcvNum == i){
				rcvNum = receiveRand.nextInt(numPeers);
			}
			//System.out.println(rcvNum);
			String receiverID = nodeList.get(rcvNum).getUID();
			float receivedAmount = 0 ;
			Transaction newTransaction = nodeList.get(i).generateTxn(receiverID, receivedAmount, nextTxnTime);
			Event newEvent = new Event(4, newTransaction, nextTxnTime);
			pendingEvents.add(newEvent);
		}

		//Timestamp of the next event to be executed
		Timestamp nextEventTime = pendingEvents.peek().getEventTimestamp();
		Iterator<Event> eventItr = pendingEvents.iterator();

		while(nextEventTime.before(maxTime)){			
			if(eventItr.hasNext()){
				Event nextEvent = pendingEvents.poll();
				finishedEvents.add(nextEvent);

				if(nextEvent.getEventType()==1){
					//Code to execute receive Block event
				}
				else if(nextEvent.getEventType()==2){
					//Code to execute generate Block
				}
				else if(nextEvent.getEventType()==3){

					//Code to execute receive Transaction
					Transaction newTxn = nextEvent.getEventTransaction();
					int receiverNum = nextEvent.getReceiverNum();
					boolean addReceiveSuccess = nodeList.get(receiverNum).addTxn(newTxn);
					
					nextEventTime = nextEvent.getEventTimestamp();
					finishedEvents.add(nextEvent);
				}
				else if(nextEvent.getEventType()==4){

					//Code to handle generate Transaction event
					Transaction newTxn = nextEvent.getEventTransaction();
					String senderID = newTxn.getSenderID();
					int senderNum = Integer.parseInt(senderID.split("_")[1]);

					Random updateRand = new Random(System.nanoTime());
					float newAmount = updateRand.nextFloat()*nodeList.get(senderNum).getCurrOwned();
					newTxn.updateAmount(newAmount);
					System.out.print("b: "+nodeList.get(senderNum).getCurrOwned()+" ");
					boolean addTxnSuccess = nodeList.get(senderNum).addTxn(newTxn);
					if(addTxnSuccess){
						if (newAmount!=0){
							System.out.println(senderID + " sents " + newTxn.getAmount()+ " to " + newTxn.getReceiverID()+" a: "+ nodeList.get(senderNum).getCurrOwned());
							for(int i=0; i<numPeers; i++){
								Node nextNode = nodeList.get(i);
								if(i==senderNum){
									continue;
								}
								else{
									Random queingRandom = new Random(System.nanoTime());
									long qDelay = (long)queingRandom.nextInt(50);
									long pDelay = Math.round(propagationDelay[senderNum][i]);
									Timestamp receiveTime = new Timestamp(nextEventTime.getTime()+ qDelay + pDelay);
									Event newEvent = new Event(3, newTxn, receiveTime, i);
									pendingEvents.add(newEvent);
								}
							}
						}						

						//to Generate the next transaction for the sending node
						long nextTxnLong = (long)(10000*txnMean[senderNum]);
						Timestamp nextTxnTime = new Timestamp(nextEventTime.getTime() + (long)(Math.random()*nextTxnLong));
						nodeList.get(senderNum).nextTxnTime = nextTxnTime;
						// System.out.println(nextTxnTime);
						Random receiveRand = new Random(System.nanoTime());
						int rcvNum = receiveRand.nextInt(numPeers);
						while(rcvNum == senderNum){
							rcvNum = receiveRand.nextInt(numPeers);
						}

						String receiverID = nodeList.get(rcvNum).getUID();
						float receivedAmount = 0;

						Transaction newTransaction = nodeList.get(senderNum).generateTxn(receiverID, receivedAmount, nextTxnTime);
						Event newEvent = new Event(4, newTransaction, nextTxnTime);
						pendingEvents.add(newEvent);

						//Updating the time to execute next evnet
						nextEventTime = pendingEvents.peek().getEventTimestamp();
					}
					else{
						System.out.println("Add Transaction Failed!!");
					}	
					//Code to generate Transaction
				}
				else{
					System.out.println("Error: Wrong Eventtype Detected.");
					break;
				}
			}
			else{

			}
		}
		
		// while(evnetItr.hasNext()){
		// 	System.out.println(pendingEvents.poll().getEventTimestamp());
		// }

		for(int i=0; i<numPeers; i++){
			System.out.println(nodeList.get(i).getCurrOwned());
		}
	}	
}