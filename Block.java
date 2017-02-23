import java.sql.Timestamp;
import java.util.ArrayList;

public class Block{

	private String uBlokckID;
	private Timestamp creationTime;
	private String creatorID;
	private String pBlockID;

	//This list contains all the transactions included in the block
	private ArrayList<Transaction> txnList = new ArrayList<Transaction>();
	private int numTxns = 0;

	Block(String uBlokckID, Timestamp creationTime, String creatorID, String pBlockID){
		this.uBlokckID = uBlokckID;
		this.creationTime = creationTime;
		this.creatorID = creatorID;
		this.pBlockID = pBlockID;
	}

	//function to add txns to a block
	public void addTxn(Transaction newTxn){
		txnList.add(numTxns++,newTxn);
	}

	//to return block ID
	public String getBlockID(){
		return uBlokckID;
	}

	//to return creation time of the block
	public Timestamp getCreationTime(){
		return creationTime;
	}

	//to get number of transaction in the Block
	public long getNumTxns(){
		return numTxns;
	}

}