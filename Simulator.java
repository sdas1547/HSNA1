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
		double qDelayParameter = 1;
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

		//to create a connencted graph with each node connected to a random number of other nodes
		Boolean[][] connectionArray = new Boolean[numPeers][numPeers];
		for(int i = 0; i<numPeers; i++){
			for(int j = 0; j<numPeers; j++){
				connectionArray[i][j]=false;
			}
		}
		Random connRand = new Random(System.nanoTime());
		int n1Num = connRand.nextInt(numPeers);
		int numNode = 0;
		Boolean[] tempConnection = new Boolean[numPeers];
		for(int i = 0; i<numPeers; i++){
			tempConnection[i] = false;
		}
		
		tempConnection[n1Num] = true;
		int newNum = connRand.nextInt(numPeers);
		while(tempConnection[newNum]){
			newNum = connRand.nextInt(numPeers);
		}
		tempConnection[newNum] = true;
		connectionArray[n1Num][newNum] = true;
		connectionArray[newNum][n1Num] = true;
		numNode++;

		while (numNode <= numPeers){
			newNum = connRand.nextInt(numPeers);
			while(tempConnection[newNum]){
				newNum = connRand.nextInt(numPeers);
			}
			int oldNum = connRand.nextInt(numPeers);
			while(!tempConnection[oldNum]){
				oldNum = connRand.nextInt(numPeers);
			}

			connectionArray[oldNum][newNum] = true;
			connectionArray[newNum][oldNum] = true;
			numNode++;
		}

		int maxRemainingEdges = ((numPeers-1)*(numPeers-2))/2;
		int remainingEdges = connRand.nextInt()%maxRemainingEdges;
		while(remainingEdges>0){
			int i = connRand.nextInt(numPeers);
			int j = connRand.nextInt(numPeers);
			if(!connectionArray[i][j]){
				connectionArray[i][j] = true;
				connectionArray[j][i] = true;
				remainingEdges--;
			}
		}

		//Creating a 2D array to store Propagation delay between each pair of nodes
		Double[][] propagationDelay  = new Double[numPeers][numPeers];
		Random randProp = new Random(System.nanoTime());
		for(int i=0; i<numPeers; i++){			
			for(int j=0; j<numPeers; j++){
				if(i<=j){					
					boolean makeConnection = connectionArray[i][j];
					if(makeConnection){
						nodeList.get(i).addNode(nodeList.get(j));
						double propDelay = minPropDelay + randProp.nextDouble()*(maxPropDelay - minPropDelay);
						propagationDelay[i][j] = propDelay;
					}
					
				}
				else{
					//To mantain the symmetry of the propagation delay
					if(propagationDelay[j][i]!= null){
						nodeList.get(i).addNode(nodeList.get(j));
						propagationDelay[i][j] = propagationDelay[j][i];	
					}					
				}
				
			}
			// System.out.println("Number of connected nodes of "+i+ "is" + );
		}

		//Creating a array to store the bottle neck link between each pair of nodes
		Double[][] bottleNeck = new Double[numPeers][numPeers];
		for(int i=0; i<numPeers; i++){
			for(int j=0; j<numPeers; j++){
				if(connectionArray[i][j]){
					if(nodeList.get(i).getType() && nodeList.get(j).getType())
						bottleNeck[i][j] = 100.0;
					else
						bottleNeck[i][j] = 5.0;
				}								
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
			double tempTxnMean = 100 + randMean.nextDouble()*50;
			txnMean[i] = 1/tempTxnMean;
		}


		//Probable Tree of the blockchain broadcasted so far

		//Priortiy Queue of events to be executed
		PriorityQueue<Event> pendingEvents = new PriorityQueue<Event>();
		//Priority Queue of events executed so far
		PriorityQueue<Event> finishedEvents = new PriorityQueue<Event>();

		long simTime = 1000*10;
		Timestamp currTime = new Timestamp(System.currentTimeMillis());
		Timestamp startTime = currTime;
		long currTimeOffset = currTime.getTime();
		Timestamp maxTime = new Timestamp(currTimeOffset + (long)(Math.random()*simTime));

		for(int i=0; i<numPeers; i++){
			// long nextTxnLong = (long)(10000*txnMean[i]);
			Random randNext = new Random();
			double nextTimeOffset = randNext.nextDouble();
			while (nextTimeOffset == 0.0){
				nextTimeOffset = randNext.nextFloat();
			}
			// System.out.println(nextTimeOffset);
			double nextTimeGap = -1*Math.log(nextTimeOffset)/txnMean[i];
			// System.out.println(nextTimeGap);
			Timestamp nextTxnTime = new Timestamp(currTimeOffset + (long)nextTimeGap*100);
			// System.out.println(nextTxnTime);
			nodeList.get(i).nextTxnTime = nextTxnTime;
			// System.out.println(nextTxnTime);
			Random receiveRand = new Random(System.nanoTime());
			int rcvNum = receiveRand.nextInt(numPeers);
			while(rcvNum == i){
				rcvNum = receiveRand.nextInt(numPeers);	//choosing a random peer to pay amount to
			}
			//System.out.println(rcvNum);
			String receiverID = nodeList.get(rcvNum).getUID();
			float receivedAmount = 0 ;				//todo choose random amount to pay
			Transaction newTransaction = nodeList.get(i).generateTxn(receiverID, receivedAmount, nextTxnTime);
			Event newEvent = new Event(4, newTransaction, nextTxnTime);
			newEvent.eventAtNode = nodeList.get(i);

			pendingEvents.add(newEvent);
		}

		//Timestamp of the next event to be executed
		if(pendingEvents.peek() == null){
			System.out.println("Panic: No nodes in the Bitcoin System!\nExiting...");
			System.exit(-1);
		}
		Timestamp nextEventTime = pendingEvents.peek().getEventTimestamp();
		Iterator<Event> eventItr = pendingEvents.iterator();
		// System.out.println("first Event time : "+nextEventTime );
		while(nextEventTime.before(maxTime)){			
			if(eventItr.hasNext()){
				Event nextEvent = pendingEvents.poll();
				finishedEvents.add(nextEvent);

				if(nextEvent.getEventType()==1){
					//Code to execute receive Block event
					Node ntmp = nextEvent.eventAtNode;
					Block btmp = nextEvent.getEventBlock();
					if(ntmp == null || btmp==null){
						System.out.println("Panic: Node or Block for the `receive block event` not defined!");
						continue;
					}
					ntmp.recvBlock(btmp);
//					finishedEvents.add(nextEvent);
				}
				else if(nextEvent.getEventType()==2){
					//Code to execute generate Block
					/**
					*	1. decide previous block from the longest chain - longestSoFar
					*	2. Build a block on it - Get all pending transactions and form a block
					*/
					Node ntmp = nextEvent.eventAtNode;
					Block btmp = nextEvent.getEventBlock();
					if(ntmp == null || btmp==null){
						System.out.println("Panic: Node or Block for the `generate block event` not defined!");
						continue;
					}
					
					
				}
				else if(nextEvent.getEventType()==3){

					//Code to execute receive Transaction					
					int receiverNum = nextEvent.getReceiverNum();
					int senderNum = nextEvent.getSenderNum();
					Node tempSenderNode = nodeList.get(receiverNum);
					Transaction newTxn = nextEvent.getEventTransaction();
					String newTxnID = newTxn.getTxnID();
					if(!(tempSenderNode.checkForwarded(newTxnID))){//Only execute if it has not already forwarded the same transaction earlier
												
						int txnReceiverNum = Integer.parseInt((newTxn.getReceiverID()).split("_")[1]);
						System.out.print("Transaction Id "+ newTxnID+" Money receiver :"+txnReceiverNum+" "+"Message Receiver :"+receiverNum);
						if(txnReceiverNum == receiverNum){ //checking the transaction is meant for that node or not
							boolean addReceiveSuccess = nodeList.get(receiverNum).addTxn(newTxn);							
						}

						System.out.println();
						nodeList.get(receiverNum).addForwarded(newTxnID);
						for(int i=0; i<numPeers; i++){
							Node nextNode = tempSenderNode.getNode(i);							
							if(nextNode == null){
								break;
							}							
							else{	
								int nextNodeNum = Integer.parseInt(nextNode.getUID().split("_")[1]);
								if (nextNodeNum != senderNum){

									Random queingRandom = new Random(System.nanoTime());
									double qDelayP1 = queingRandom.nextDouble();
									while (qDelayP1 == 0.0){
										qDelayP1 = queingRandom.nextFloat();
									}
									long qDelay = (long)((-1*Math.log(qDelayP1)*bottleNeck[senderNum][receiverNum])/qDelayParameter);
									// System.out.println(qDelay);
									long pDelay = Math.round(propagationDelay[receiverNum][nextNodeNum]);
									Timestamp receiveTime = new Timestamp(nextEventTime.getTime()+ qDelay + pDelay);
									Event newEvent = new Event(3, newTxn, receiveTime, nextNodeNum, receiverNum);
									pendingEvents.add(newEvent);
								}
							}
						}				
						
						//Timestamp of the next event to be executed
						nextEventTime = nextEvent.getEventTimestamp();
					}
					
				}
				else if(nextEvent.getEventType()==4){

					//Code to handle generate Transaction event
					Transaction newTxn = nextEvent.getEventTransaction();
					String senderID = newTxn.getSenderID();
					int senderNum = Integer.parseInt(senderID.split("_")[1]);

					//Adding a temporary node to enhance efficiency
					Node tempSenderNode = nodeList.get(senderNum);

					//random to generate an amount for the transaction
					Random updateRand = new Random(System.nanoTime());
					float newAmount = updateRand.nextFloat()*tempSenderNode.getCurrOwned();
					newTxn.updateAmount(newAmount);
					System.out.print("b: "+tempSenderNode.getCurrOwned()+" ");

					//Adding the transaction at the sender end.
					boolean addTxnSuccess = nodeList.get(senderNum).addTxn(newTxn);
					nodeList.get(senderNum).addForwarded(newTxn.getTxnID());
					if(addTxnSuccess){			//proceeding only when the transaction is successfully added
						if (newAmount!=0){
							System.out.println(senderID + " sends " + newTxn.getAmount()+ " to " + newTxn.getReceiverID()+" a: "+ nodeList.get(senderNum).getCurrOwned());
							for(int i=0; i<numPeers; i++){
								Node nextNode = tempSenderNode.getNode(i);
								if(nextNode == null){
									break;
								}
								else{
									int nextNodeNum = Integer.parseInt(nextNode.getUID().split("_")[1]);

									Random queingRandom = new Random(System.nanoTime());
									float qDelayP1 = queingRandom.nextFloat();
									while (qDelayP1 == 0.0){
										qDelayP1 = queingRandom.nextFloat();
									}
									long qDelay = (long)((-1*Math.log(qDelayP1)*bottleNeck[senderNum][nextNodeNum])/qDelayParameter);
									long pDelay = Math.round(propagationDelay[senderNum][nextNodeNum]);
									Timestamp receiveTime = new Timestamp(nextEventTime.getTime()+ qDelay + pDelay);
									Event newEvent = new Event(3, newTxn, receiveTime, nextNodeNum, senderNum);
									pendingEvents.add(newEvent);

								}								
							}
						}						

						//to Generate the next transaction for the sending node
						Random randNext = new Random();
						double nextTimeOffset = randNext.nextDouble();
						while (nextTimeOffset == 0.0){
							nextTimeOffset = randNext.nextFloat();
						}
						double nextTimeGap = -1*Math.log(nextTimeOffset)/txnMean[senderNum];
						Timestamp nextTxnTime = new Timestamp(currTimeOffset + (long)nextTimeGap*1000);

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
						// System.out.println(nextEventTime);
					}
					else{
						System.out.println("Add Transaction Failed!!");
					}	
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

		double sum = 0;
		for(int i=0; i<numPeers; i++){
			float value = nodeList.get(i).getCurrOwned();
			sum = sum + value;
			System.out.println(value);
		}
		System.out.println("Total :"+sum);
	}
}
