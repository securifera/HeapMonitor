///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package heapmonitor;
//
//import java.util.ArrayList;
//import java.util.List;
//
////=========================================================================
///**
// * Class structure for AllocationTuple
// */
//public class MemoryTuple {
//
//    protected final long memoryAddress;
//    protected final List<MemoryChunkLabel> affectedChunks = new ArrayList<>();
//    protected final byte[] traceByteArr;
//
//    //=================================================================
//    /**
//     * 
//     * @param passedAddress 
//     * @param passedTraceByteArr 
//     */
//    public MemoryTuple(long passedAddress, byte[] passedTraceByteArr ) {
//        memoryAddress = passedAddress;
//        traceByteArr = passedTraceByteArr;
//    }
//
//    //==================================================================
//    /**
//     * 
//     * @return 
//     */
//    public long getMemoryAddress() {
//        return memoryAddress;
//    }
//  
//     //==================================================================
//    /**
//     * 
//     * @param aLabel 
//     */
//    public void addAffectedChunk(MemoryChunkLabel aLabel) {
//        affectedChunks.add(aLabel);
//    }
//
//    //==================================================================
//    /**
//     * 
//     */
//    public void resetAffectedChunks() {
//        //Reset the overlap and then clear
//        for( MemoryChunkLabel aLabel : affectedChunks ){
//            aLabel.setOverlap(0);
//        }
//        affectedChunks.clear();
//    }
//
////    //==================================================================
////    /**
////     * 
////     * @param parentFrame
////     */
////    public void process( MainFrame parentFrame ) {
////        
////        if( traceByteArr != null ){
////            
////            String traceIniStr = new String(traceByteArr);
////            String[] traceArr = traceIniStr.split("\n");
////            //Loop through                 
////            for( String traceStr : traceArr ){
////
////                //Get frame name
////                String[] traceElementArr = traceStr.split("\t");
////                if( traceElementArr.length == 3 ){
////
////                    //Get function name
////                    String functionName = traceElementArr[0];
////                    
////                    //Get source path
////                    String sourcePath = traceElementArr[1];
////
////                    //Get trace address
////                    String stackAddressStr = traceElementArr[2].replace("(", "").replace(")", "");
////                    try {
////                        long stackAddress = Long.parseLong( stackAddressStr, 16 );
////
////                        //Create a trace and add to list
////                        Trace aTrace = parentFrame.getTrace(stackAddress);
////                        if( aTrace == null ){            
////                            aTrace = new Trace( functionName, sourcePath, stackAddress );
////                            parentFrame.setTrace(stackAddress, aTrace);
////                        }
////
////                        //Add the trace
////                        addTrace(aTrace);
////                    } catch( NumberFormatException ex ){
////                        System.err.println("Error processing trace: \"" + traceStr +"\"" );
////                    }
////
////                } else {
////                    System.err.println("Improperly formatted trace.");
////                }          
////
////                //System.out.println("Frame: " + frameNum + " " + functionName + " " + Long.toHexString(stackAddress));
////
////            }
////        }
////    }
//} 