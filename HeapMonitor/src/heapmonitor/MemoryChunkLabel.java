/*
 * MemoryChunkLabel.java
 *
 */

package heapmonitor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.swing.JLabel;

/**
 *
 * @author b0yd
 */
public class MemoryChunkLabel extends JLabel {

    
    private final MemoryJPanel parentPanel;
    private final Long startAddr;
    private final Long endAddr;
    private final TreeMap<Long,MemoryChunk> theTreeMap;
    public static final int BYTE_PIXEL_SIZE = 5;
        
    //===================================================================
    /**
     * Constructor 
     * @param passedPanel
     * @param treeMap
     * @param startingAddr
     * @param endingAddr
     */
    public MemoryChunkLabel( MemoryJPanel passedPanel, TreeMap<Long,MemoryChunk> treeMap, Long startingAddr, Long endingAddr ) {
        super();
        parentPanel = passedPanel;
        theTreeMap = treeMap;
        startAddr = startingAddr;
        endAddr = endingAddr;
        
        addMouseListener(new MouseAdapter()  
        {  
            @Override
            public void mouseClicked(MouseEvent e)  
            {  
                int x_pos = e.getX();
                int cur_byte = x_pos / BYTE_PIXEL_SIZE;
                Long result = startAddr + cur_byte;
                
                //Get the allocation for this address
                Entry< Long, MemoryChunk> curEntry = parentPanel.getAllocation( MemoryJPanel.LOWER_ADDRESS, result);
                if( curEntry != null ){
                    MemoryChunk aChunk = curEntry.getValue();
                    parentPanel.getMainFrame().setSelectedChunk(aChunk);
                }
            }  
        }); 
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
        
        synchronized(theTreeMap){
            
            //Get previous allocation and see if it overflowed into our chunk
            int overlap = 0;
            Color prevColor = Color.BLUE;
            Entry< Long, MemoryChunk> lowerEntry = parentPanel.getAllocation( MemoryJPanel.LOWER_ADDRESS, startAddr);
            if( lowerEntry != null ){
                
                MemoryChunk prevChunk = lowerEntry.getValue();
                prevColor = prevChunk.getColor();
                int prevSize = prevChunk.getAllocatedSize();
                
                //Calculate overflow
                long prevStarAddr = lowerEntry.getKey();
                if( prevStarAddr + prevSize > endAddr )
                    overlap = (int) (endAddr - startAddr);
                else if( prevStarAddr + prevSize > startAddr )
                    overlap = (int) ( (prevStarAddr + prevSize) - startAddr );
                
            }
            
            //If a previous chunk has already overflowed into this one, then paint it
            if( overlap != 0 ){

                //Update the starting pointer
                int fillSize = overlap * BYTE_PIXEL_SIZE;
                tempStartAddr += overlap;

                //Fill in the overflow
                g.setColor(prevColor);
                g.fillRect(0, 0, fillSize, 20);

                if( overlap == endAddr - startAddr)
                    return;

            }
            
             //Get previous allocation and see if it overflowed into our chunk
            int underlap = 0;
            Entry< Long, MemoryChunk> higherEntry = parentPanel.getAllocation( MemoryJPanel.HIGHER_ADDRESS, endAddr - 1);
            if( higherEntry != null ){
                //Calculate underflow
                long headerStartAddr = higherEntry.getKey() - MainFrame.ALLOCATION_HEADER_SIZE;
                if( headerStartAddr < endAddr )
                    underlap = (int) (endAddr - headerStartAddr);
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
                    Long headerStartAddr = (tempStartAddr - startAddr) - MainFrame.ALLOCATION_HEADER_SIZE;
                    if( headerStartAddr >= 0 ){
                        Long fillValue = headerStartAddr * BYTE_PIXEL_SIZE;
                        g.setColor(Color.YELLOW);
                        g.fillRect(fillValue.intValue(), 0, BYTE_PIXEL_SIZE * MainFrame.ALLOCATION_HEADER_SIZE, 20);                  
                    }     

                    //Color in the first chunk
                    if( tempStartAddr + size >  endAddr){

                        //Fill in the rect                    
                        g.setColor(aMemChunk.getColor());
                        g.fillRect(startX.intValue(), 0, BYTE_PIXEL_SIZE * (int)(endAddr - tempStartAddr), 20);                    
                        return;
                    } 

                    //Fill in the rect
                    int fillSize = BYTE_PIXEL_SIZE * size;                
                    g.setColor(aMemChunk.getColor());
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
                        g.fillRect(fillValue.intValue(), 0, BYTE_PIXEL_SIZE * MainFrame.ALLOCATION_HEADER_SIZE, 20);                  
                    }  

                    //Color in the first chunk
                    Long startX = (nextAddr - startAddr) * BYTE_PIXEL_SIZE;
                    if( nextAddr + size >  endAddr){

                        //Fill in the rect                    
                        g.setColor(aMemChunk.getColor());
                        g.fillRect(startX.intValue(), 0, BYTE_PIXEL_SIZE * (int)(endAddr - nextAddr), 20);                    
                        return;
                    }                 

                    //Fill in the rect                
                    g.setColor(aMemChunk.getColor());
                    g.fillRect(startX.intValue(), 0, BYTE_PIXEL_SIZE * size, 20);               
                }
                
                nextAddr = nextAddr + size;
                nextAddr = theTreeMap.higherKey(nextAddr);
            }
        }
    }
    
//    //=========================================================================
//    /**
//     * 
//     * @param direction
//     * @param address
//     * @return 
//     */
//    public Entry< Long, MemoryChunk> getAllocation( byte direction, long address ){
//        Entry< Long, MemoryChunk> retEntry = null;
//        if( direction == LOWER_ADDRESS)
//            retEntry = theTreeMap.lowerEntry(address);
//        else if( direction == HIGHER_ADDRESS)
//            retEntry = theTreeMap.higherEntry(address);
//                
//        //Make sure it is allocated
//        if( retEntry != null ){
//            MemoryChunk prevChunk = retEntry.getValue();
//            if( !prevChunk.isAllocated())
//                retEntry = getAllocation(direction, retEntry.getKey());                
//        }
//        return retEntry;
//    }
}
