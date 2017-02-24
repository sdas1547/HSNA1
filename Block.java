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
	
	//to return pblock ID
	public String getPBlockID(){
		return pBlockID;
	}
	
	//to set pblock ID
	public void setPBlockID(String pBlockId){
		pBlockID = pBlockId;
	}

	//to return creation time of the block
	public Timestamp getCreationTime(){
		return creationTime;
	}

	//to get number of transaction in the Block
	public long getNumTxns(){
		return numTxns;
	}
	
	public void printBlock(String ident){
		System.out.println(ident+"Block UID:" + this.uBlokckID);
		System.out.println(ident+"Creation Time:" + this.creationTime);
		System.out.println(ident+"Creator ID:" + this.creatorID);
		System.out.println(ident+"Previous Block UID:" + this.pBlockID);
	}

	
	public boolean matchID(String id){
		if(this.uBlokckID.equals(id)){
			return true;
		}
		return false;
	}
}