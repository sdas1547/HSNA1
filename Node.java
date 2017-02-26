import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.ArrayList;

public class Node{

	private boolean type; //type true for fast nodes and false for lazy nodes
	private String uID;
	private float currOwned;
	private Timestamp creationTime;
	private Block genesisBlock;

	//For the next set of events to executed by the node
	Timestamp nextTxnTime;
	Timestamp nextBlockTime;
	LinkedList<Transaction> pendingTxnRcv = new LinkedList<Transaction>();
	long numPendingTxnRcv = 0;
	LinkedList<Block> pendingBlockRcv = new LinkedList<Block>();
	long numPendingBlockRcv = 0;

	//Varialbes to store information about longest chain received so far
	Block longestSoFar;
	long longestLength = 0;

	//Transaction details
	ArrayList<Transaction> receivedTxn = new ArrayList<Transaction>();
	int numReceivedTxn = 0;
	ArrayList<Transaction> sentTxn = new ArrayList<Transaction>();
	int numSentTxn = 0;

	//Connection Details
	LinkedList<Node> connectedNode = new LinkedList<Node>();
	int numConnection = 0;

	//Tree to store all the blocks heard by the Node so far
	TreeNode<Block> bTree;		//root added at node initialization

	//Default constructor
	Node(String uID, boolean type, Timestamp creationTime, Block genesisBlock){
		this.uID = uID;
		this.type = type;
		this.creationTime = creationTime;
		this.currOwned = 50;
		this.genesisBlock = genesisBlock;
		this.bTree = new TreeNode<Block>(genesisBlock);
		this.longestSoFar = genesisBlock;
		this.longestLength = 0;
	}

	
	public boolean addBlock(Block b){
		TreeNode<Block> tmp;
		tmp = bTree.findNodeByBlockID(b.getPBlockID());	//get node with block id equals previous block id
		if(tmp!=null){
			tmp.addChild(b);
			//maintaining longest block chain last block
			if(tmp.level == this.longestLength){
				this.longestSoFar = b;
				this.longestLength++;
			}
			return true;
		}
		return false;
	}
	
	//called on receiving a block
	boolean recvBlock(Block b){
		TreeNode<Block> tmp = bTree.findNode(b);	
		if(tmp!=null){
			//block already in the tree
			return false;
		}
		boolean added = this.addBlock(b);
		if(!added){
			//add only if not already in pending blocks
			if(!this.pendingBlockRcv.contains(b)){
				this.pendingBlockRcv.add(b);
				this.numPendingBlockRcv++;
			}
		}else{
			//check if any of the pending blocks can be added
			addPendingBlocks();
		}
		return added;
	}
	
	public void addPendingBlocks(){
		TreeNode<Block> tmp;
		boolean added;
		for(int i=0;i<this.pendingBlockRcv.size();i++){
			tmp = bTree.findNode(this.pendingBlockRcv.get(i));	
			if(tmp!=null){
				//block already in tree
				this.pendingBlockRcv.remove(i);
				continue;
			}
			added = this.addBlock(this.pendingBlockRcv.get(i));
			if(added){
				this.pendingBlockRcv.remove(i);
			}
		}
	}
	
	//function to generate a transaction
	Transaction generateTxn(String receiverID, float txnAmount, Timestamp txnTime){
		String txnID = uID + "_" + numSentTxn;
		Transaction newTxn = new Transaction(txnID, uID, receiverID, txnAmount, txnTime);
		return newTxn;
	}
	
	//function to add a new transaction to a node
	boolean addTxn(Transaction newTxn){
		if(newTxn.getSenderID().equals(this.uID)){
			
			if(newTxn.getAmount()<=currOwned){
				//Add to sentTxn ArrayList
				sentTxn.add(numSentTxn, newTxn);
				currOwned = currOwned - newTxn.getAmount();
				numSentTxn++;
				return true;
			}
			else{
				return false;
			}
			
		}
		else if(newTxn.getReceiverID().equals(this.uID)){
			//Add to receivedTxn ArrayList
			receivedTxn.add(numReceivedTxn, newTxn);
			currOwned = currOwned + newTxn.getAmount();
			numReceivedTxn++;
			return true;
		}
		else{
			receivedTxn.add(numReceivedTxn, newTxn);
			numReceivedTxn++;
			return true;
		}
	}

	//Function to forward a txn received
	void forwardTxn(Transaction fwdTxn, Node sentNode){
		
		for(int i=0; i < numConnection; i++){

		}
	}

	//Add Node to connected Nodes
	void addNode(Node newNode){
		connectedNode.add(newNode);
		numConnection++;
	}

	//Deleting Node from connected Nodes
	boolean removeNode(Node newNode){
		boolean success = connectedNode.remove(newNode);
		if(success){
			numConnection--;
			return true;
		}
		else{
			return false;
		}
	}

	//userID return
	public String getUID(){
		return uID;
	}

	//type return
	public boolean getType(){
		return type;
	}

	//creationTime return
	public Timestamp getCreationTime(){
		return creationTime;
	}

	//userID return
	public float getCurrOwned(){
		return currOwned;
	}

	//to update the currently owned value
	public void updateCurrOwned(float newAmount){
		this.currOwned = newAmount;
	}

	//overwritting toString method for Node
	public String toString(){
		return "ID: "+this.uID+" type: "+ (this.type?"fast":"lazy") + " Creation time: "+this.creationTime  + " Balance: "+this.currOwned;
	}
}