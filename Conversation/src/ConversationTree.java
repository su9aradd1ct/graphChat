public class ConversationTree 
{
	private JID person1;
	private JID person2;
	private ConversationNode root;
	
	public ConversationTree(JID personOne, JID personTwo, ConversationNode theRoot)
	{
		person1 = personOne;
		person2 = personTwo;
		root = theRoot;
	}
	
	public ConversationNode getRoot()
	{
		return root;		
	}
	
	public String getPersonOne()
	{
		return person1.getString();			
	}
	
	public String getPersonTwo()
	{
		return person2.getString();
	}
	
	public JID getIDOne()
	{
		return person1;
	}
	
	public JID getIDTwo()
	{
		return person2;
	}
	

}
