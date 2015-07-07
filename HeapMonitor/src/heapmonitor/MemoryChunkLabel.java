/*
 * MemoryChunkLabel.java
 *
 */

package heapmonitor;

import java.awt.Color;
import java.awt.Graphics;
import java.util.TreeMap;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author rwincey
 */
public class MemoryChunkLabel extends JLabel {

    private final DefaultTableModel parentModel;
    private final int startAddr;
    private volatile int overlap = 0;
    private final int endAddr;
    private final TreeMap<Integer,AllocationTuple> theTreeMap;
    private static final int BYTE_PIXEL_SIZE = 5;
    
    //===================================================================
    /**
     * Constructor 
     * @param passedModel
     * @param treeMap
     * @param startingAddr
     * @param endingAddr
     */
    public MemoryChunkLabel( DefaultTableModel passedModel, TreeMap<Integer,AllocationTuple> treeMap, int startingAddr, int endingAddr ) {
        super();
        parentModel = passedModel;
        theTreeMap = treeMap;
        startAddr = startingAddr;
        endAddr = endingAddr;
    }
    
    //====================================================================
    /**
     * 
     * @param passedOverlap
     */     
    public void setOverlap( int passedOverlap ){
        overlap = passedOverlap;
    }
    
    //====================================================================
    /**
     * Override the JLabel
     * @param g 
     */
    @Override
    protected void paintComponent(Graphics g) {
        
        //Get the allocated keys and paint according
        int tempStartAddr = startAddr;
        if( overlap != 0 ){
            
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, BYTE_PIXEL_SIZE * (endAddr - startAddr), 20);     
            
            //Update the starting pointer
            int fillSize = overlap * BYTE_PIXEL_SIZE;
            tempStartAddr += overlap;
            
            //Fill in the overflow
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, fillSize, 20);
            
            if( overlap == endAddr - startAddr)
                return;
            
        }
            
        g.setColor(Color.BLUE);
        synchronized(theTreeMap){
            
            Integer nextAddr;
            AllocationTuple aTuple = theTreeMap.get(tempStartAddr);
            if( aTuple != null ){
                
                //Color in the first chunk
                int size = aTuple.getSize();
                int startX = (tempStartAddr - startAddr) * BYTE_PIXEL_SIZE;
                if( tempStartAddr + size >  endAddr){
                    
                    //Pass on the remainder
                    int remainder = size - (endAddr - tempStartAddr);   
                    updateFollowingChunks( aTuple, remainder );
                    //Fill in the rect
                    g.fillRect(startX, 0, BYTE_PIXEL_SIZE * (endAddr - tempStartAddr), 20);                    
                    return;
                } 
                
                //Fill in the rect
                int fillSize = BYTE_PIXEL_SIZE * size;
                g.fillRect(startX, 0, fillSize, 20);
                
                nextAddr = tempStartAddr + size;
                nextAddr = theTreeMap.higherKey(nextAddr);
                
            } else {
                //Get the next key
                nextAddr = theTreeMap.higherKey(tempStartAddr);
                if( nextAddr == null )
                    return;                
            }
            
            //Loop through next chunks
            while( nextAddr != null && nextAddr < endAddr ){
                
                aTuple = theTreeMap.get(nextAddr);
                if( aTuple == null ){
                    break;
                }
                                                
                //Color in the first chunk
                int size = aTuple.getSize();
                int startX = (nextAddr - startAddr) * BYTE_PIXEL_SIZE;
                if( nextAddr + size >  endAddr){
                    
                    int remainder = size - (endAddr - nextAddr); 
                    updateFollowingChunks( aTuple, remainder );                    
                     //Fill in the rect
                    g.fillRect(startX, 0, BYTE_PIXEL_SIZE * (endAddr - nextAddr), 20);                    
                    return;
                }                 
                
                //Fill in the rect
                g.fillRect(startX, 0, BYTE_PIXEL_SIZE * size, 20);               
                
                nextAddr = nextAddr + size;
                nextAddr = theTreeMap.higherKey(nextAddr);
            }
        }
    }

    //====================================================================
    /**
     * 
     * @param remainder 
     */
    private void updateFollowingChunks( AllocationTuple passedTuple, int remainder) {
        
        //Calculate the affected row
        int entrySize = endAddr - startAddr;
        int baseaddr = (startAddr >> 16) << 16;
        int diff = startAddr - baseaddr;
        int row = (int)Math.floor(diff/entrySize);
        
        //Loop through and update the affected chunks
        int numChunksAffected = (int) Math.ceil( (double)remainder / (double)entrySize);
        for( int i=1; i < numChunksAffected + 1; i++ ){
            
            //Get the next label and add it to be tracked
            if( row + 1 < 512 ){
                MemoryChunkLabel aLabel = (MemoryChunkLabel) parentModel.getValueAt(row + i, 1);
                passedTuple.addAffectedChunk(aLabel);

                //Set the overlap 
                if( remainder <= entrySize ){
                    aLabel.setOverlap(remainder);
                } else{
                    aLabel.setOverlap(entrySize);
                    remainder -= entrySize;
                } 
            } else {
                break;
            }
        }
    }

}
