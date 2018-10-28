
package heapmonitor;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 *
 * @author b0yd
 */
public class MemoryChunk {
    
    protected final long memoryAddress;
    protected final Stack<AllocationTrace> allocationList = new Stack<>();
    protected final Stack<Trace> freeList = new Stack<>();
    private Color memoryColor = Color.BLUE;
    
    private boolean allocated = false;
    
    //=================================================================
    /**
     * 
     * @param passedAddress 
     */
    public MemoryChunk( long passedAddress ) {
        memoryAddress = passedAddress;
    }
    
    //=================================================================
    /**
     * 
     * @param passedAllocation
     */
    public void addAllocation( AllocationTrace passedAllocation ){        
        allocated = true;  
        
        allocationList.push(passedAllocation);
        if( allocationList.size() > 20 )
            allocationList.removeElementAt(0);
        
    }
    
    //=================================================================
    /**
     * 
     * @return 
     */
    public int getAllocatedSize(){
        
        int retSize = 0;   
        if( allocated && !allocationList.isEmpty() ){
            AllocationTrace aTrace = allocationList.peek();
            retSize = aTrace.getSize();
        }
        return retSize;
    }
    
    //=================================================================
    /**
     * 
     * @param freeTrace
     */
    public void addFree( Trace freeTrace ){        
        allocated = false;        
        freeList.push(freeTrace);
        if( freeList.size() > 20 )
            freeList.removeElementAt(0);
    }
    
    //==================================================================
    /**
     * 
     * @return 
     */
    @Override
    public String toString(){
        String retStr = String.format("0x%08X", memoryAddress );
        if( allocated && !allocationList.isEmpty() ){
            AllocationTrace aTrace = allocationList.peek();
            retStr += ":" + String.format("0x%08X", aTrace.getSize() );
        }
        return retStr;
    }

    //==================================================================
    /**
     * 
     * @return 
     */
    public boolean isAllocated() {
        return allocated;
    }

    //==================================================================
    /**
     * 
     * @return 
     */
    public long getAddress() {
        return memoryAddress;
    }

    //==================================================================
    /**
     * 
     * @return 
     */
    public byte[] getLastTrace() {
        byte[] traceArr = null;
        if( allocated && !allocationList.isEmpty() ){
            AllocationTrace aTrace = allocationList.peek();
            traceArr = aTrace.traceByteArr;
        } else if( !freeList.isEmpty() ){
            Trace aTrace = freeList.peek();
            traceArr = aTrace.traceByteArr;
        }
        return traceArr;
    }
    
    //==================================================================
    /**
     * 
     * @return 
     */
    public String getTraceHistory() {
        
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy-hh:mm:ss:SSSZ ");
        
        String retStr = "";
        List<Trace> allTraces = new ArrayList<>();
        
        //Add allocations
        allTraces.addAll(allocationList);
        
        //Add frees
        allTraces.addAll(freeList);
        
        //Sort      
        Collections.sort( allTraces );
        
        for( Trace aTrace : allTraces ){
            
            retStr += " " + format.format( aTrace.dateReceived );
            
            //Add size
            if( aTrace instanceof AllocationTrace ){
                AllocationTrace allocTrace = (AllocationTrace)aTrace;
                retStr += " Size: " + allocTrace.getSize();
            }
            
            retStr += "\n";
            retStr += Trace.processStackTrace( aTrace.traceByteArr );
            retStr += "\n";
            
        }
        
        return retStr;
        
    }
    
    //========================================================================
    /**
     * 
     * @param passedColor 
     */
    public void setColor( Color passedColor ){
        memoryColor = passedColor;
    }

    //=========================================================================
    /**
     * 
     * @return 
     */
    public Color getColor() {
        return memoryColor;
    }
}
