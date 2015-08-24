//
//package heapmonitor;
///**
// * Class structure for AllocationTuple
// */
//public class AllocationTuple extends MemoryTuple {
//
//    private final int size;
//
//    //==================================================================
//    /**
//     * 
//     * @param passedAddress
//     * @param passedArr
//     * @param passedSize 
//     */
//    public AllocationTuple( long passedAddress, byte[] passedArr, int passedSize ){
//        super(passedAddress, passedArr);
//        size = passedSize;
//    }   
//
//    //==================================================================
//    /**
//     * 
//     * @return 
//     */
//    public int getSize() {
//        return size;
//    }
//    
//    //==================================================================
//    /**
//     * 
//     * @return 
//     */
//    @Override
//    public String toString(){
//        return String.format("0x%08X", memoryAddress ) + ":" + String.format("0x%08X", size );
//    }
//    
//} 