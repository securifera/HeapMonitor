package heapmonitor;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author b0yd
 */
public class SocketHandler implements Runnable {

    private final Socket theClientSocket;
    private final MainFrame theParentFrame;
    
    public static final byte ALLOCATE = 0x12;
    public static final byte FREE = 0x13;
    public static final byte SOCKET_CLOSED = -1;
    
    //=================================================================
    /**
     * 
     * @param parentFrame
     * @param clientSocket 
     */
    public SocketHandler(MainFrame parentFrame, Socket clientSocket) {
        theParentFrame = parentFrame;
        theClientSocket = clientSocket;
    }
    
    //=================================================================
    /**
     * Main receive loop
     * 
     * Protocol format
     * 
     * Allocation
     * [ 1 byte - message type ][ 4 byte - allocation size ][ 8 bytes - address ]
     * [ 4 bytes - trace size ][ (trace_size bytes) trace string array ]
     * 
     * Free
     * [ 1 byte - message type ][ 8 bytes - address ]
     * [ 4 bytes - trace size ][ (trace_size bytes) trace string array ]
     * 
     * 
     * 
     */
    @Override
    public void run() {
        
        try {
            
            boolean socketClosed = false;
            //Create the handler and start it
            MemoryTupleHandler aHandler = new MemoryTupleHandler(theParentFrame);
            aHandler.start();
            
            theClientSocket.setSoTimeout(1000);
            DataInputStream dataStream = new DataInputStream( theClientSocket.getInputStream() );
            byte[] sizeArr = new byte[4];
            byte[] addrArr = new byte[8];
            byte[] traceLen = new byte[4];
            
            while( !socketClosed ){
                
                Trace aTrace = null;
                try{
                    
                    //Get message type
                    byte messageType = (byte)dataStream.read();
                    long address = 0;
                    switch( messageType){
                        case ALLOCATE:

                            //Get allocation size
                            dataStream.readFully(sizeArr);
                            int size = ByteBuffer.wrap(sizeArr).order(ByteOrder.LITTLE_ENDIAN).getInt();

                            //Get address
                            dataStream.readFully(addrArr);
                            address = ByteBuffer.wrap(addrArr).order(ByteOrder.LITTLE_ENDIAN).getLong();
                          
                            //Get trace len
                            dataStream.readFully(traceLen);
                            int trace_len = ByteBuffer.wrap(traceLen).order(ByteOrder.LITTLE_ENDIAN).getInt();
                                                      
                            //Get trace data
                            byte[] traceByteArr = new byte[trace_len];
                            dataStream.readFully(traceByteArr);
                            
                            //Create tuple
                            aTrace = new AllocationTrace( traceByteArr, size);
                            break;
                            
                        case FREE:
                            //Get address
                            dataStream.readFully(addrArr);
                            address = ByteBuffer.wrap(addrArr).order(ByteOrder.LITTLE_ENDIAN).getLong();
                          
                            //Get trace len
                            dataStream.readFully(traceLen);
                            trace_len = ByteBuffer.wrap(traceLen).order(ByteOrder.LITTLE_ENDIAN).getInt();
                            
                            //Get trace data
                            traceByteArr = new byte[trace_len];
                            dataStream.readFully(traceByteArr);

                            //Create tuple
                            aTrace = new Trace( traceByteArr );

                            break;
                        case SOCKET_CLOSED:
                            socketClosed = true;
                            break;
                        default:
                            System.err.println("Unknown message type detected.");
                            break;
                    }
                    
                    //Add to queue to be processed
                    if( address != 0 && aTrace != null )                        
                        aHandler.processIncoming(address, aTrace);
                    
                    
                } catch(SocketTimeoutException ex){
                }
            }
        } catch (SocketException ex) {  
            if( !ex.getMessage().contains("Connection reset"))
                Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        //Set socket handler to null
        theParentFrame.setSocketHandler(null);
        
    }

    //==========================================================================
    /**
     * 
     */
    public void disconnect() {
        try {
            theClientSocket.close();
        } catch (IOException ex) {
        }
    }
    
}
