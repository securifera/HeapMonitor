
/*
 * ManagedRunnable.java
 *
 * Created on June 2, 2013  10:21 PM
 */

package heapmonitor;

import java.util.concurrent.Executor;

/**
 *
 *  
 */
abstract public class ManagedRunnable implements Runnable {
    
    protected volatile boolean shutdownRequested = false;
    private volatile boolean isRunning = false;
    private volatile boolean notified = false;
    protected final Executor theExecutor;

    //===============================================================
    /**
    *  Base constructor
     * @param passedExecutor
    */
    protected ManagedRunnable( Executor passedExecutor ) {
        theExecutor = passedExecutor;
    }   
   
    //===============================================================
    /**
    *  Starts the detector thread
    */
    public synchronized void start(){
        if( !isRunning ){           
            theExecutor.execute( this );
        }
    }
    
    //===============================================================
    /**
     *  Used for setting the run flags
    */
    @Override //Runnable
    final public void run() {
        
        //Set flag
        isRunning = true;
        
        //Run the main function
        go();
        
        //Set flag
        isRunning = false;
    
    }
    
    //===============================================================
    /**
     *  The main thread function
    */
    abstract protected void go();
    
    //===============================================================
    /**
    *  Shut down the detector
    */
    public synchronized void shutdown(){
        shutdownRequested = true;
        notifyAll();
    }
    
    
     // ==========================================================================
    /**
    * Causes the calling {@link Thread} to <tt>wait()</tt> until notified by
    * another.
    * <p>
    * <strong>This method most certainly "blocks".</strong>
     * @param anInt
    */
    public synchronized void waitToBeNotified( Integer... anInt ) {

        while( !notified && !shutdownRequested) { //Until notified...

            try {
                
                //Add a timeout if necessary
                if( anInt.length > 0 ){
                    
                    wait( anInt[0]);
                    break;
                    
                } else {
                    wait(); //Wait here until notified
                }
                
            } catch( InterruptedException ex ) {
            }

        }
        notified = false;
    }
    
    //===============================================================
    /**
     * Notifies the thread
    */
    protected synchronized void beNotified() {
        notified = true;
        notifyAll();
    }
    
     // ==========================================================================
    /**
    *  Checks the shutdown flag.
    *
     * @return 
    */
    public synchronized boolean finished() {
        return shutdownRequested;
    }
    
    // ==========================================================================
    /**
    *  Check if the running flag has been set
    *
     * @return 
    */
    public boolean isRunning() {
        return isRunning;
    }
}