/*
 * MessageHandler.java
 *
 */

package heapmonitor;

import java.nio.ByteBuffer;

/**
 *
 * @author rwincey
 */
public class MessageHandler implements Runnable {

    private final MainFrame theParentFrame;
    private final ByteBuffer theByteBuffer;
    
    public static final short ALLOCATE = 0x12;
    public static final short FREE = 0x13;
    
    //=================================================================
    /**
     * 
     * @param passedParent
     * @param aBB 
     */
    public MessageHandler(MainFrame passedParent, ByteBuffer aBB) {
       theParentFrame = passedParent;
       theByteBuffer = aBB;
    }
    
    //=================================================================
    /**
     * Main loop
     */
    @Override
    public void run() {
        
        //Get message type
        short messageType = theByteBuffer.getShort();
        int address = theByteBuffer.getInt();
        String aStr;
        
        switch( messageType){
            case ALLOCATE:
                
                int size = theByteBuffer.getInt();
                //Loop through 
                
                AllocationTuple aTuple = new AllocationTuple(size);
                for( int i=0; i<5; i++){
                    
                    //Get frame and stack address
                    int frameNum = theByteBuffer.getShort();
                    int stackAddress = (int)theByteBuffer.getLong();
                    
                    if( stackAddress != 0 ){
                        //Get function name
                        byte[] functionNameBytes = new byte[100];
                        theByteBuffer.get(functionNameBytes);
                        String functionName = new String(functionNameBytes).trim();

                        //Create a trace and add to list
                        Trace aTrace = theParentFrame.getFunctionName(frameNum);
                        if( aTrace == null ){            
                            aTrace = new Trace((int)stackAddress, functionName);
                            theParentFrame.setTrace(stackAddress, aTrace);
                        }

                        //Add the trace
                        aTuple.addTrace(aTrace);
                    }
                    //System.out.println("Frame: " + frameNum + " " + functionName + " " + Long.toHexString(stackAddress));
                     
                }
                
                //Add the allocation
                theParentFrame.addAllocation(address, aTuple);
                aStr = "Allocated " + size + " bytes at Address: " + Integer.toHexString(address);
                theParentFrame.addLogMessage(aStr);
                
                break;
            case FREE:
                //Free the allocation, remove entry
                theParentFrame.removeAllocation(address);
                aStr = "Freeing Address: " + Integer.toHexString(address);
                theParentFrame.addLogMessage(aStr);
                
                break;
            default:
                System.err.println("Unknown message type detected.");
                break;
        }
        
    }
    
}
