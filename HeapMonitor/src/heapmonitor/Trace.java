/*
 * Trace.java
 *
 */

package heapmonitor;

 //=======================================================================
import java.util.Date;


/**
 * Class structure for Trace
 */
public class Trace implements Comparable<Trace>{

    protected final byte[] traceByteArr;
    protected final Date dateReceived;

    //=============================================================
    /**
     * 
     * @param passedByteArr
     */
    public Trace( byte[] passedByteArr ){
        traceByteArr = passedByteArr;
        dateReceived = new Date();
    }
    
    //=============================================================
    /**
     * 
     * @return 
     */
    public byte[] getTraceByteArray(){
        return traceByteArr;  
    }

    //=============================================================
    /**
     * 
     * @param otherTrace
     * @return 
     */
    @Override
    public int compareTo(Trace otherTrace) {
        return otherTrace.dateReceived.compareTo(dateReceived);
    }
    
    //==================================================================
    /**
     * 
     * @param traceByteArr
     * @return 
     */
    public static String processStackTrace( byte[] traceByteArr ) {
        
        String retStr = "";
        if( traceByteArr != null ){
            
            String traceIniStr = new String(traceByteArr);
            String[] traceArr = traceIniStr.split("\n");
            //Loop through                 
            for( String traceStr : traceArr ){

                //Get frame name
                String[] traceElementArr = traceStr.split("\t");
                if( traceElementArr.length == 3 ){

                    //Get function name
                    String functionName = traceElementArr[0].trim();
                    
                    //Get source path
                    String sourcePath = traceElementArr[1].trim();

                    //Get trace address
                    String stackAddressStr = traceElementArr[2].trim();
                    retStr += String.format( "%-20.20s %-30.30s %-10s \n", stackAddressStr, functionName, sourcePath );

                } else {
                    System.err.println("Improperly formatted trace.");
                }          

            }
        }
        
        return retStr;
    }

} 