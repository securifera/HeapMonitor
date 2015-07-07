/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package heapmonitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rwincey
 */
public class SocketHandler implements Runnable {

    private final Socket theClientSocket;
    private final MainFrame theParentFrame;
    
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
     * Main loop
     */
    @Override
    public void run() {
        
        try {
            
            theClientSocket.setSoTimeout(1000);
            InputStream dataFromServer = theClientSocket.getInputStream();
            
            int bytesRead = 0;
            byte[] byteArr = new byte[1000];
            while(true){
                
                try{
                    //Read bytes from socket
                    bytesRead = dataFromServer.read(byteArr);
                    if( bytesRead > 0 ){
                         ByteBuffer aBB = ByteBuffer.wrap(byteArr).order(ByteOrder.LITTLE_ENDIAN);
                         MessageHandler aHandler = new MessageHandler(theParentFrame, aBB);
                         MainFrame.Executor.execute(aHandler);
                    }
                    
                } catch(SocketTimeoutException ex){
                }
            }
            
        } catch (IOException ex) {
            Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
            theParentFrame.setSocketHandler(null);
        }
    }
    
}
