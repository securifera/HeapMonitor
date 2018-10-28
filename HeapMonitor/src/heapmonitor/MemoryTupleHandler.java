/*
 * MessageHandler.java
 *
 */

package heapmonitor;

import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author b0yd
 */
public class MemoryTupleHandler extends ManagedRunnable {

    private final MainFrame theParentFrame;    
    private final Queue<Object[]> incomingTraceQueue = new LinkedList<>();
    
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
        
        Object[] anObjArr;
        while( !shutdownRequested ){
            
            //Wait till something is added to the queue
            waitToBeNotified();
            
            //Waits until a msg comes in
            while( !isIncomingEmpty() ){


                // Handle the next message
                synchronized(incomingTraceQueue) {
                    anObjArr = (Object[])incomingTraceQueue.poll();
                }

                //Handles a message if there is one
                if(anObjArr != null){
                    handleIncoming(anObjArr);
                    anObjArr = null;
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
    private void handleIncoming( Object[] anObjArr ) {  
        
        //String aStr = null;
        if( anObjArr != null && anObjArr.length == 2){
        
            Long address = (Long)anObjArr[0];
            Trace theTrace = (Trace)anObjArr[1];
            theParentFrame.addTrace( address, theTrace );

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

        synchronized(incomingTraceQueue) {
            retVal = incomingTraceQueue.isEmpty();
        }
        return retVal;
    }
    
    //===============================================================
    /**
    *  Queues a MemoryTuple
    *
     * @param address
    * @param passedTrace
    */
    public void processIncoming( long address, Trace passedTrace) {

        //Copy over the bytes
        if(passedTrace != null){

            synchronized(incomingTraceQueue) {
                incomingTraceQueue.add(new Object[]{ address, passedTrace });
            }
            beNotified();

        }
    }
    

}
