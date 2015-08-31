package heapmonitor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 *
 * @author b0yd
 */
public class MemoryMapLabel extends JLabel {
    
    public static final int MEMORY_MAP_LABEL_WIDTH = 755;
    public static final int MEMORY_MAP_LABEL_HEIGHT = 24;
    
    private final MemoryJPanel parentPanel;
    private BufferedImage memoryMapImage;
    private final Object lockObj = new Object();
    
    private volatile long curr_start_address = 0;
    private volatile long curr_end_address = 0;
    
    //======================================================================
    /**
     * 
     */
    public MemoryMapLabel( MemoryJPanel passedParent ) {   
        initialize();
        memoryMapImage = new BufferedImage( MEMORY_MAP_LABEL_WIDTH, MEMORY_MAP_LABEL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        parentPanel = passedParent;
    }   
    
    //=======================================================================
    /**
     * 
     * @param g 
     */
    @Override
    public void paintComponent( Graphics g ){
        //super.paintComponent(g);
        synchronized(lockObj ){
            g.drawImage(memoryMapImage, 0, 0, null);
        }
    }
    
    //=======================================================================
    /**
     * 
     * @param passedMemMap
     */
    public void updateMemoryMap( TreeMap<Long, MemoryChunk> passedMemMap ){
        synchronized(lockObj ){
            
            curr_start_address = passedMemMap.firstKey();
            curr_end_address = passedMemMap.lastKey();
            
            long range = curr_end_address - curr_start_address;      
            if( range != 0 ){
                
                double pixel_per_byte = (double)MEMORY_MAP_LABEL_WIDTH/(double)range;

                //Get the graphics
                Graphics g = memoryMapImage.getGraphics();
                //Paint background first
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, MEMORY_MAP_LABEL_WIDTH, MEMORY_MAP_LABEL_HEIGHT);
                
                //Set color for the rest
                g.setColor(Color.BLUE);

                for ( Entry<Long, MemoryChunk> anEntry : passedMemMap.entrySet() ) {
                    long chunk_address = anEntry.getKey();
                    MemoryChunk aChunk = anEntry.getValue();
                    if( aChunk.isAllocated() ){
                        int start_location = (int)(Math.floor( ((double)chunk_address - (double)curr_start_address) * pixel_per_byte));
                        int size = (int) ((double)aChunk.getAllocatedSize() * pixel_per_byte);
                        if( size == 0)
                            size = 1;

                        g.fillRect(start_location, 0, size, MEMORY_MAP_LABEL_HEIGHT);
                    }
                }
            }
        }
        
        //Repaint if the adddress space is open
        SwingUtilities.invokeLater( new Runnable(){
            @Override
            public void run() {
                repaint();
            }
        });
    }

    //=======================================================================
    /**
     * 
     */
    private void initialize() {
        
        //Set border
        setBorder(  BorderFactory.createLineBorder(Color.black) );
        
        //Add mouse listener for click
        addMouseListener( new MouseAdapter(){
            
            @Override
            public void mouseClicked(MouseEvent evt) {
                
                if (evt.getClickCount() == 1) {
                
                    int x = evt.getX();
                    int y = evt.getY();

                    long range = curr_end_address - curr_start_address;      
                    if( range != 0 ){
                        double pixel_per_byte = (double)MEMORY_MAP_LABEL_WIDTH/(double)range;
                        long clicked_address = (long) ( curr_start_address + ( (double)x / pixel_per_byte));
                        
                        long baseaddr = (clicked_address >> 16) << 16;
                        parentPanel.loadMemoryPage(baseaddr, true);
                    }
                                        
                } 
            }
        
        });
        
        //Change mouse cursor
        addMouseMotionListener( new MouseMotionListener(){

            @Override
            public void mouseDragged(MouseEvent e) {            
            }

            //Change the cursor
            @Override
            public void mouseMoved(MouseEvent e) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        
        });
    }

    //========================================================================
    /**
     * 
     */
    public void clear() {
        memoryMapImage = new BufferedImage( MEMORY_MAP_LABEL_WIDTH, MEMORY_MAP_LABEL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }
    
}
