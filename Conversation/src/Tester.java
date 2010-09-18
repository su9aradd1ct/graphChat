
/**
 * Write a description of class Tester here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Tester
{
	public static void main (String[] args)
	{
		main();
	}
    public static int main()
    {
        JID person1 = new JID("terminalRahulKurana");
        JID person2 = new JID("ubuntuArham");
        String gay = "fuckbowles";
        int[] array = new int[10];
        
        String gayer = "fuckbowlesmore";
        int[] array1 = new int[14];
        
        long[] gayarray = new long[1];
        
        gayarray[0] = 10;
        
        long[] gayarray2 = new long[3];
        
        gayarray2[0] = 12;
        gayarray2[1] = 15;
        gayarray2[2] = 10;
        
        for(int i = 1; i<15; i++)
        {
            array1[i-1] = i;
        }
        
        for(int i = 1; i<11; i++)
        {
            array[i-1] = i;
        }
        
        ConversationNode root = new ConversationNode(person1, 10, gay.toCharArray(), array, null);
        ConversationTree tree = new ConversationTree(person1, person2, root);
        
        ConversationNode bitches = new ConversationNode(person2, 12, gayer.toCharArray(), array1, gayarray, tree.getRoot());
        ConversationNode freshofftheboat = new ConversationNode(person1, 15, "yo answer me beyotch".toCharArray(), arrayMaker(20), gayarray, tree.getRoot());
        
        ConversationNode tits = new ConversationNode(person2, 17, "i like dicks".toCharArray(), arrayMaker(12), gayarray2, tree.getRoot());
        
        
        System.out.println(tits.getParents().length);
        return 6;
    }
    
    public static int[] arrayMaker(int i)
        {
            int array[] = new int[i];
            for(int j = 1; j < i + 1; j ++)
            {
                array[j - 1] = j;
            }
            return array;
        }
}
