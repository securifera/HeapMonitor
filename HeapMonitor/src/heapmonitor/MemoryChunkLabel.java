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
    private final Long startAddr;
    private final Long endAddr;
    private final TreeMap<Long,MemoryChunk> theTreeMap;
    private static final int BYTE_PIXEL_SIZE = 5;
    
    private volatile int overlap = 0;
    private volatile int underlap = 0;
    
    //===================================================================
    /**
     * Constructor 
     * @param passedModel
     * @param treeMap
     * @param startingAddr
     * @param endingAddr
     */
    public MemoryChunkLabel( DefaultTableModel passedModel, TreeMap<Long,MemoryChunk> treeMap, Long startingAddr, Long endingAddr ) {
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
     * 
     * @param passedUnderlap
     */     
    public void setUnderlap( int passedUnderlap ){
        underlap = passedUnderlap;
    }
    
    //====================================================================
    /**
     * Override the JLabel
     * @param g 
     */
    @Override
    protected void paintComponent(Graphics g) {
        
        //Reset everything
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, BYTE_PIXEL_SIZE * (int)(endAddr - startAddr), 20);
        
        //Get the allocated keys and paint according
        Long tempStartAddr = startAddr;
        
        //If a previous chunk has already overflowed into this one, then paint it
        if( overlap != 0 ){
                        
            //Update the starting pointer
            int fillSize = overlap * BYTE_PIXEL_SIZE;
            tempStartAddr += overlap;
            
            //Fill in the overflow
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, fillSize, 20);
            
            if( overlap == endAddr - startAddr)
                return;
            
        }
        
        //If the following chunk has underflowed into this one
        if( underlap != 0 ){
                        
            //Update the starting pointer
            int fillSize = underlap * BYTE_PIXEL_SIZE;
            Long anAddr = endAddr - underlap;
            int paintAddr = (int) ((anAddr - startAddr) * BYTE_PIXEL_SIZE);
            
            //Fill in the overflow
            g.setColor(Color.YELLOW);
            g.fillRect(paintAddr, 0, fillSize, 20);
            
            if( underlap == endAddr - startAddr)
                return;
            
        }
            
        synchronized(theTreeMap){
            
            Long nextAddr;
            //See if a tuple starts at the current address, if not get the next
            //higher address and see if it falls within the range for this chunk
            MemoryChunk aMemChunk = theTreeMap.get(tempStartAddr);
            if( aMemChunk != null && aMemChunk.isAllocated()){
                
                //Get the start x
                Long startX = (tempStartAddr - startAddr) * BYTE_PIXEL_SIZE;
                
                //Get size
                int size = aMemChunk.getAllocatedSize();
                
                //Fill in header
                if( aMemChunk.isAllocated() ){
                    Long headerStartAddr = (tempStartAddr - startAddr) - 8;
                    if( headerStartAddr >= 0 ){
                        Long fillValue = headerStartAddr * BYTE_PIXEL_SIZE;
                        g.setColor(Color.YELLOW);
                        g.fillRect(fillValue.intValue(), 0, BYTE_PIXEL_SIZE * 8, 20);                  
                    } else {

                        //Fill the amount in this chunk first
                        Long diff = 8 + headerStartAddr;
                        g.setColor(Color.YELLOW);
                        g.fillRect(0, 0, BYTE_PIXEL_SIZE * diff.intValue(), 20);  

                        //Update the previous chunk
                        updatePreviousChunks(aMemChunk, (int)Math.abs(headerStartAddr));
                    }     

                    //Color in the first chunk
                    if( tempStartAddr + size >  endAddr){

                        //Pass on the remainder
                        Long remainder = size - (endAddr - tempStartAddr);   
                        updateFollowingChunks( aMemChunk, remainder.intValue() );
                        //Fill in the rect                    
                        g.setColor(Color.BLUE);
                        g.fillRect(startX.intValue(), 0, BYTE_PIXEL_SIZE * (int)(endAddr - tempStartAddr), 20);                    
                        return;
                    } 

                    //Fill in the rect
                    int fillSize = BYTE_PIXEL_SIZE * size;                
                    g.setColor(Color.BLUE);
                    g.fillRect(startX.intValue(), 0, fillSize, 20);
                }
                
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
                
                aMemChunk = theTreeMap.get(nextAddr);
                if( aMemChunk == null ){
                    break;
                }
                
                //Get size
                int size = aMemChunk.getAllocatedSize();
                
                //Fill in header
                if( aMemChunk.isAllocated() ){
                    Long headerStartAddr = (nextAddr - startAddr) - 8;
                    if( headerStartAddr >= 0 ){
                        Long fillValue = headerStartAddr * BYTE_PIXEL_SIZE;
                        g.setColor(Color.YELLOW);
                        g.fillRect(fillValue.intValue(), 0, BYTE_PIXEL_SIZE * 8, 20);                  
                    } else {

                        //Fill the amount in this chunk first
                        Long diff = 8 + headerStartAddr;
                        g.setColor(Color.YELLOW);
                        g.fillRect(0, 0, BYTE_PIXEL_SIZE * diff.intValue(), 20);  

                        //Update the previous chunk
                        updatePreviousChunks(aMemChunk, (int)Math.abs(headerStartAddr));
                    }  

                    //Color in the first chunk
                    Long startX = (nextAddr - startAddr) * BYTE_PIXEL_SIZE;
                    if( nextAddr + size >  endAddr){

                        Long remainder = size - (endAddr - nextAddr); 
                        updateFollowingChunks( aMemChunk, remainder.intValue() );                    
                         //Fill in the rect                    
                        g.setColor(Color.BLUE);
                        g.fillRect(startX.intValue(), 0, BYTE_PIXEL_SIZE * (int)(endAddr - nextAddr), 20);                    
                        return;
                    }                 

                    //Fill in the rect                
                    g.setColor(Color.BLUE);
                    g.fillRect(startX.intValue(), 0, BYTE_PIXEL_SIZE * size, 20);               
                }
                
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
    private void updatePreviousChunks( MemoryChunk passedChunk, int remainder) {
        
        //Calculate the affected row
        Long entrySize = endAddr - startAddr;
        Long baseaddr = (startAddr >> 16) << 16;
        Long diff = startAddr - baseaddr;
        int row = (int)Math.floor(diff/entrySize);
        
        //Loop through and update the affected chunks
        int numChunksAffected = (int) Math.ceil( (double)remainder / (double)entrySize);
        for( int i=1; i < numChunksAffected + 1; i++ ){
            
            //Get the next label and add it to be tracked
            if( row - i >= 0 ){
                MemoryChunkLabel aLabel = (MemoryChunkLabel) parentModel.getValueAt(row - i, 1);
                passedChunk.addAffectedChunk(aLabel);
                
                //Set the underlap 
                if( remainder <= entrySize ){
                    aLabel.setUnderlap(remainder);
                } else{
                    aLabel.setUnderlap(entrySize.intValue());
                    remainder -= entrySize;
                }   
            } else {
                break;
            }
        }
    }

    //====================================================================
    /**
     * 
     * @param remainder 
     */
    private void updateFollowingChunks( MemoryChunk passedChunk, int remainder) {
        
        //Calculate the affected row
        Long entrySize = endAddr - startAddr;
        Long baseaddr = (startAddr >> 16) << 16;
        Long diff = startAddr - baseaddr;
        int row = (int)Math.floor(diff/entrySize);
        
        //Loop through and update the affected chunks
        int numChunksAffected = (int) Math.ceil( (double)remainder / (double)entrySize);
        for( int i=1; i < numChunksAffected + 1; i++ ){
            
            //Get the next label and add it to be tracked
            if( row + i < 512 ){
                MemoryChunkLabel aLabel = (MemoryChunkLabel) parentModel.getValueAt(row + i, 1);
                passedChunk.addAffectedChunk(aLabel);

                //Set the overlap 
                if( remainder <= entrySize ){
                    aLabel.setOverlap(remainder);
                } else{
                    aLabel.setOverlap(entrySize.intValue());
                    remainder -= entrySize;
                }               
            } else {
                break;
            }
        }
    }

}
