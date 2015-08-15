/*
 * MessageHandler.java
 *
 */

package heapmonitor;

import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author rwincey
 */
public class MemoryTupleHandler extends ManagedRunnable {

    private final MainFrame theParentFrame;    
    private final Queue<MemoryTuple> incomingTupleQueue = new LinkedList<>();
    
    //=================================================================
    /**
     * 
     * @param passedParent 
     */
    public MemoryTupleHandler( MainFrame passedParent ) {
        super( MainFrame.Executor );
        theParentFrame = passedParent;
    }
    
    //=================================================================
    /**
     * Main loop
     */
    @Override
    public void go() {
        
        MemoryTuple aTuple;
        while( !shutdownRequested ){
            
            //Wait till something is added to the queue
            waitToBeNotified();
            
            //Waits until a msg comes in
            while( !isIncomingEmpty() ){


                // Handle the next message
                synchronized(incomingTupleQueue) {
                    aTuple = (MemoryTuple)incomingTupleQueue.poll();
                }

                //Handles a message if there is one
                if(aTuple != null){
                    handleIncoming(aTuple);
                }

            }
        }
    }
    
     //===============================================================
     /**
     *  Handles incoming messages
     *
     * @param theMessage
     * @return 
    */
    private void handleIncoming( final MemoryTuple aTuple ) {  
        
        //String aStr = null;
        if( aTuple != null ){
        
            //create the stack traces etc
            aTuple.process( theParentFrame );
            long address = aTuple.getMemoryAddress();
            if( aTuple instanceof AllocationTuple ){  

                AllocationTuple anAllocTuple = (AllocationTuple)aTuple;
                //Add the allocation
                //int size = anAllocTuple.getSize();

                theParentFrame.addAllocation(address, anAllocTuple);
               // aStr = "Allocated " + size + " bytes at Address: " + Long.toHexString(address);

            } else if( aTuple instanceof FreeTuple ){

                //Free the allocation, remove entry
                theParentFrame.removeAllocation(address);
                //aStr = "Freeing Address: " + Long.toHexString(address);
                
            }
//
//            //Add to log
//            if( aStr != null ){
//                theParentFrame.addLogMessage(aStr);
//            }
        }
    }  
      
               
    //===============================================================
    /**
    *  Checks if the incoming queue is empty
    *
     * @return 
    */
    public boolean isIncomingEmpty(){

        boolean retVal;

        synchronized(incomingTupleQueue) {
            retVal = incomingTupleQueue.isEmpty();
        }
        return retVal;
    }
    
    //===============================================================
    /**
    *  Queues a MemoryTuple
    *
    * @param passedMessage
    */
    public void processIncoming( MemoryTuple passedMessage) {

        //Copy over the bytes
        if(passedMessage != null){

            synchronized(incomingTupleQueue) {
                incomingTupleQueue.add(passedMessage);
            }
            beNotified();

        }
    }
    

}
