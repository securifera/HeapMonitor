/*
 * Trace.java
 *
 */

package heapmonitor;

 //=======================================================================
/**
 * Class structure for Trace
 */
public class Trace {

    private final int address;
    private final String name;       

    //=============================================================
    /**
     * 
     * @param passedAddress
     * @param passedName
     */
    public Trace( int passedAddress, String passedName ){
        address = passedAddress;
        name = passedName;            
    }

} 