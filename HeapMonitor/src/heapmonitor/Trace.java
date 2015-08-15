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

    private final long address;
    private final String name;       

    //=============================================================
    /**
     * 
     * @param passedAddress
     * @param passedName
     */
    public Trace( long passedAddress, String passedName ){
        address = passedAddress;
        name = passedName;            
    }

} 