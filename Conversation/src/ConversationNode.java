import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author HP
 * A node of the ConversationTree. Can have multiple parents and children.
 * Stores sender number, start time of message, message, offset times, parents, and children.
 * Up to 3 children and parents
 */

public class ConversationNode 
{
    private JID sender;
    private long startTime;
    private char[] message;
    private int[] offsetTime;
    private ConversationNode parents[];
    private ConversationNode children[];
    private int numberOfParents;
    private int numberOfChildren;
    private ConversationNode rootNode;
    
    public ConversationNode(JID send, long start, char[] msg, int[] offset, long[] parent)
    {
        sender = send;
        startTime = start;
        message = msg;
        if (parent != null)
        {
            for(int i=0; i<parent.length; i++)
            {
                parents[i] = isFound(getRoot(),parent[i],null);
            }
        }
        
        offsetTime = offset;

        if(parents != null)
            {
                numberOfParents = parents.length + 1;
                for(ConversationNode p: parents)
                { 
                    p.addChild(this);                
                }
            }   
        setRoot();
    }
    
    private void setRoot()
    {
        rootNode = this;
    }
    //Constructs a node using all parameters
    public ConversationNode(JID send, long start, char[] msg, int[] offset, long[] parent, ConversationNode root)
    {
        rootNode = root;
        sender = send;
        startTime = start;
        message = msg;
        parents = new ConversationNode[3];
        
        for(int i=0; i<parent.length; i++)
        {
            ConversationNode dude = isFound(getRoot(),parent[i],null);
            //System.out.println(dude.getStartTime() + " parent of " + this.getStartTime()+ " " + parent.length);
            parents[i] = dude;
            if(dude != null)
            {
                numberOfParents++;
            }
        }
        
        offsetTime = offset;
        if(parents != null)
            {
                for(ConversationNode p: parents)
                { 
                    if(p != null)
                    {
                        p.addChild(this);
                        
                    }
                }
            }   
  
    }

    public ConversationNode getRoot()
    {
        return rootNode;
    }
    
    public JID getSender()
    {
        return sender;
    }
    
    public long getStartTime()
    {
        return startTime;
    }
    
    public char[] getMessage()
    {
        return message;
    }
    
    public char getCharacter(int i)
    {
        return message[i];
    }
    
    public int[] getOffsets()
    {
        return offsetTime;
    }
    
    public int getOffset(int i)
    {
        return offsetTime[i];
    }
    
    public ConversationNode[] getParents()
    {
        return parents;
    }
    
    public ConversationNode[] getChildren()
    {
        return children;
    }
    
    //up to 3 parents allowed
    public void addParent(ConversationNode p)
    {
    /** parents[numberOfParents] = p;
        numberOfParents++;
        **/
        return;
    }
    
    //up to 3 children allowed
    public void addChild(ConversationNode c)
    {
        
        if (children == null)
        {
            children = new ConversationNode[3];
            numberOfChildren = 1;
            children[0] = c;
        }
        else
        {
            children[numberOfChildren] = c;
            numberOfChildren++;
        }
    }
    
    /**
    public void preorder()
    {
        Set visitedNodes = new HashSet<ConversationNode>();
        
        preorderTraversal(visitedNodes);    
    }
    
    **/
    
    public void addChildPointer(ConversationNode child)
    {
        children[numberOfChildren] = child;
        numberOfChildren++;
    }
    
    private void preorderTraversal(Set theSet, long timestamp, ConversationNode result)
    {
        if(!(theSet.contains(this)))
        {
            isFound(this, timestamp, result);
            theSet.add(this);
            
            for(ConversationNode c: children)
            {
                c.preorderTraversal(theSet, timestamp, result);
            }
        }
        
    }
    
    private ConversationNode isFound(ConversationNode node, long tStamp, ConversationNode theResult)
    {
        if (theResult != null)
            return theResult;
        else if 
            (node == null)
        {
            return null;
        }
        else 
        {
            if(tStamp == node.getStartTime())
            { 
                return node;
            }
            
            else
            {
                boolean found = false;
                if(node.getChildren() == null)
                    return null;
                else
                {
                    for(ConversationNode c: node.getChildren())
                    {
                        ConversationNode check = isFound(c, tStamp, null);
                        if (check!= null)
                        {
                            found = true;
                            return check;
                        }       
                    }
                        return null;
                }
            }
        }
    }
}