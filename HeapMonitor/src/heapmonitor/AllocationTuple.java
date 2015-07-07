/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package heapmonitor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

//=========================================================================
/**
 * Class structure for AllocationTuple
 */
public class AllocationTuple {

    private final int size;
    private final Queue<Trace> stackTrace = new LinkedList<>();    
    private final List<MemoryChunkLabel> affectedChunks = new ArrayList<>();

    //==================================================================
    /**
     * 
     * @param passedSize 
     */
    public AllocationTuple( int passedSize ){
        size = passedSize;
    }   

    //==================================================================
    /**
     * 
     * @return 
     */
    public int getSize() {
        return size;
    }
    
    //==================================================================
    /**
     * 
     * @param aTrace 
     */
    public void addTrace( Trace aTrace ){
        stackTrace.add(aTrace);
    }

     //==================================================================
    /**
     * 
     * @param aLabel 
     */
    public void addAffectedChunk(MemoryChunkLabel aLabel) {
        affectedChunks.add(aLabel);
    }

    //==================================================================
    /**
     * 
     * @param aLabel 
     */
    public void resetAffectedChunks() {
        //Reset the overlap and then clear
        for( MemoryChunkLabel aLabel : affectedChunks ){
            aLabel.setOverlap(0);
        }
        affectedChunks.clear();
    }
} 