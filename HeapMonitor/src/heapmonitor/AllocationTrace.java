/*
 * Trace.java
 *
 */

package heapmonitor;

 //=======================================================================
/**
 * Class structure for Trace
 */
public class AllocationTrace extends Trace {

    private final int size;

    //=============================================================
    /**
     * 
     * @param passedByteArr
     * @param passedSize
     */
    public AllocationTrace( byte[] passedByteArr, int passedSize ){
        super( passedByteArr );
        size = passedSize;
    }

    //=============================================================
    /**
     * 
     * @return 
     */
    public int getSize() {
        return size;
    }    

} 