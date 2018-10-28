package heapmonitor;

import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author b0yd
 */
public final class MemoryJPanel extends javax.swing.JPanel {

    private final TreeMap<Long, MemoryChunk> memoryTreeMap = new TreeMap();
    private static final int MAX_ROW_LENGTH = 0x80;
    private JTable memTable;
    private final MainFrame parentFrame;
    private MemoryMapLabel memoryMapLabel;    
    
    public static final byte LOWER_ADDRESS = 2;
    public static final byte HIGHER_ADDRESS = 3;
    public static final byte LOWER_EQUAL_ADDRESS = 4;
    
    /**
     * Creates new form MemoryJPanel
     * @param parent
     */
    public MemoryJPanel( MainFrame parent ){
        initComponents();
        initializeComponents();
        parentFrame = parent;
    }
    
    //=====================================================================
    /**
     * 
     */
    public void initializeComponents(){
        
         //Create JTable        
        memTable = new JTable();
        memTable.setShowVerticalLines(true);
        memTable.setTableHeader(null);
        memTable.setFont( new Font(Font.MONOSPACED, Font.PLAIN, 12));
        memTable.setEnabled(false);
       
        //Create DefaultTableModel
        DefaultTableModel aModel = new DefaultTableModel(0,2);
        memTable.setModel(aModel);
   
        //Get ColumnModel, set width and renderer
        TableColumnModel columnModel = memTable.getColumnModel();
        TableColumn aColumn = columnModel.getColumn(1);
        aColumn.setPreferredWidth(638);
        aColumn.setCellRenderer( new DefaultTableCellRenderer(){
        
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column) {  
                if( value instanceof MemoryChunkLabel){
                    return (Component)value;
                } 
                return this;
            }
        
        });
        
        aColumn = columnModel.getColumn(0);
        aColumn.setCellRenderer( new DefaultTableCellRenderer(){
        
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column) {                
                
                //TODO come back and chage this to longs
                JLabel thisLabel = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Long aLong = (Long) value;
                thisLabel.setText( String.format("0x%08X", aLong.intValue()));
                return this;
            }
        
        });
           
        //Set the view
        memoryTableScrollPane.setViewportView(memTable);
        
        //Add memoryMapLabel to memory jpanel
        memoryMapLabel = new MemoryMapLabel( this );
        javax.swing.GroupLayout memoryPanelLayout = new javax.swing.GroupLayout(memoryPanel);
        memoryPanel.setLayout(memoryPanelLayout);
        memoryPanelLayout.setHorizontalGroup(
            memoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(memoryPanelLayout.createSequentialGroup()
                    .addGap(14)
                    .addComponent(memoryMapLabel, javax.swing.GroupLayout.PREFERRED_SIZE, MemoryMapLabel.MEMORY_MAP_LABEL_WIDTH, MemoryMapLabel.MEMORY_MAP_LABEL_WIDTH)
                    .addGap(14))
        );
        memoryPanelLayout.setVerticalGroup(
            memoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(memoryPanelLayout.createSequentialGroup()
                    .addGap(10)
                    .addComponent(memoryMapLabel, javax.swing.GroupLayout.PREFERRED_SIZE, MemoryMapLabel.MEMORY_MAP_LABEL_HEIGHT, MemoryMapLabel.MEMORY_MAP_LABEL_HEIGHT)
                    .addGap(2))
        );
        
        //Add mouse listener to the table
        memTable.addMouseListener(new MouseAdapter()  
        {  
            @Override
            public void mouseClicked(MouseEvent e)  
            {  
                int row = memTable.rowAtPoint(e.getPoint());
                DefaultTableModel theModel = (DefaultTableModel)memTable.getModel();
                Long startAddr = (Long) theModel.getValueAt(row, 0);
                
                int x_pos = e.getX();
                int cur_byte = x_pos / MemoryChunkLabel.BYTE_PIXEL_SIZE;
                Long result = startAddr + cur_byte;
                
                //Get the allocation for this address
                Map.Entry< Long, MemoryChunk> curEntry = getAllocation( MemoryJPanel.LOWER_EQUAL_ADDRESS, result);
                if( curEntry != null ){
                    MemoryChunk aChunk = curEntry.getValue();
                    getMainFrame().setSelectedChunk(aChunk);
                }
            }  
        }); 
         
    }
    
    //=======================================================================
    /**
     * 
     * @return 
     */
    public MainFrame getMainFrame(){
        return parentFrame;
    }
    
    //=======================================================================
    /**
     * 
     * @param memAddress 
     * @param firstAlloc 
     */ 
    public void loadMemoryPage( final long memAddress, final boolean firstAlloc ){
        
        final MemoryJPanel thisPanel = this;
        SwingUtilities.invokeLater( new Runnable(){

            @Override
            public void run() {
                
                long passedAddress = memAddress;
                
                //Get the model and add row
                DefaultTableModel theModel = (DefaultTableModel)memTable.getModel();
                long baseaddr = (passedAddress >> 16) << 16;
                
                //Goto first allocation 
                if( firstAlloc){
                    long firstAddress = memoryTreeMap.higherKey(passedAddress);
                    if( firstAddress < baseaddr + 0x10000)
                        passedAddress = firstAddress;
                }
                
                
                //Get the row count
                if( theModel.getRowCount() > 0 ){
                    
                    //Check current base against one for passed value
                    Long addr = (Long)theModel.getValueAt(0, 0);
                    if( addr != baseaddr ){
                        theModel.setRowCount(0);

                        long startAddr = baseaddr;
                        for( int i=0; i< 0x200; i++){
                            theModel.addRow(new Object[]{ startAddr, new MemoryChunkLabel( thisPanel, memoryTreeMap, startAddr, startAddr + MAX_ROW_LENGTH)  });
                            startAddr += 0x80;
                        }
                    }
                    
                } else {
                    
                    //Add addresses for first time
                    long startAddr = baseaddr;
                    for( int i=0; i< 0x200; i++){
                        theModel.addRow(new Object[]{ startAddr, new MemoryChunkLabel(thisPanel, memoryTreeMap, startAddr, startAddr + MAX_ROW_LENGTH)  });
                        startAddr += 0x80;
                    }
                    
                }
          
                //Get the difference 
                if( passedAddress != baseaddr ){
                    long diff = passedAddress - baseaddr;
                    int row = (int) Math.floor(diff/MAX_ROW_LENGTH);
                    
                    //Scroll to address
                    memTable.setRowSelectionInterval(row, row);
                    Rectangle aRect = memTable.getCellRect(row, 0, true);
                    memTable.scrollRectToVisible(aRect);
                }
                
            }
            
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        memoryPanel = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        memoryTableScrollPane = new javax.swing.JScrollPane();

        javax.swing.GroupLayout memoryPanelLayout = new javax.swing.GroupLayout(memoryPanel);
        memoryPanel.setLayout(memoryPanelLayout);
        memoryPanelLayout.setHorizontalGroup(
            memoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 780, Short.MAX_VALUE)
        );
        memoryPanelLayout.setVerticalGroup(
            memoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 36, Short.MAX_VALUE)
        );

        jLabel10.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText("0x28");

        jLabel8.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("0x18");

        jLabel13.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel13.setText("0x80");

        jLabel9.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("0x20");

        jLabel14.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel14.setText("0x78");

        jLabel6.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel6.setText("0x8");

        jLabel15.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel15.setText("0x70");

        jLabel7.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("0x10");

        jLabel16.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel16.setText("0x68");

        jLabel17.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel17.setText("0x50");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel4.setText("Address");

        jLabel18.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel18.setText("0x48");

        jLabel19.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel19.setText("0x40");

        jLabel12.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("0x38");

        jLabel11.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("0x30");

        jLabel20.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel20.setText("0x58");

        jLabel21.setFont(new java.awt.Font("FreeMono", 1, 10)); // NOI18N
        jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel21.setText("0x60");

        memoryTableScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(memoryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(40, 40, 40)
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(15, 15, 15)
                    .addComponent(memoryTableScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 755, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(memoryPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 527, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(62, 62, 62)
                    .addComponent(memoryTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 512, Short.MAX_VALUE)
                    .addContainerGap()))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel memoryPanel;
    private javax.swing.JScrollPane memoryTableScrollPane;
    // End of variables declaration//GEN-END:variables

    //=====================================================================
    /**
     * 
     * @param passedAddress
     * @return 
     */
    public MemoryChunk getMemoryChunk(long passedAddress) {
        
        MemoryChunk aChunk;
        synchronized( memoryTreeMap){
            aChunk = memoryTreeMap.get(passedAddress);
        }
        return aChunk;
    }

    //=====================================================================
    /**
     * 
     * @param passedAddress 
     * @param aChunk 
     */
    public void setMemoryChunk(long passedAddress, MemoryChunk aChunk) {
    
        TreeMap<Long, MemoryChunk> memoryTreeMapCpy = new TreeMap();
        synchronized( memoryTreeMap){
            memoryTreeMap.put( passedAddress, aChunk );
            memoryTreeMapCpy.putAll(memoryTreeMap);
        }
        
        memoryMapLabel.updateMemoryMap(memoryTreeMapCpy);
    }

    //=====================================================================
    /**
     * 
     */
    public void clearPanel() {
                
        memoryTreeMap.clear();  
        
        memoryMapLabel.clear();
                        
        //Clear tree model
        DefaultTableModel theModel = (DefaultTableModel)memTable.getModel();
        theModel.setRowCount(0); 
        
        refreshMemoryMap();
    }
    
    //=====================================================================
    /**
     * 
     */
    public void refreshMemoryMap() {
        //Repaint if the adddress space is open
        SwingUtilities.invokeLater( new Runnable(){
            @Override
            public void run() {
                memoryMapLabel.repaint();
            }
        });
    }

    //=====================================================================
    /**
     * 
     * @param passedAddress 
     */
    public void refreshMemoryPage(final long passedAddress) {
        
        //Get the model and add row
        DefaultTableModel theModel = (DefaultTableModel)memTable.getModel();

        //Get the row count
        if( theModel.getRowCount() > 0 ){

            long baseaddr = (passedAddress >> 16) << 16;
            //Check current base against one for passed value
            Long addr = (Long) theModel.getValueAt(0, 0);
            if( addr == baseaddr ){
        
                //Repaint if the adddress space is open
                SwingUtilities.invokeLater( new Runnable(){
                    @Override
                    public void run() {
                        memTable.repaint();
                    }
                });
            }
        }
    }
    
    //=========================================================================
    /**
     * 
     * @param direction
     * @param address
     * @return 
     */
    public Map.Entry< Long, MemoryChunk> getAllocation( byte direction, long address ){
        Map.Entry< Long, MemoryChunk> retEntry = null;
        if( direction == LOWER_ADDRESS)
            retEntry = memoryTreeMap.lowerEntry(address);
        else if( direction == HIGHER_ADDRESS)
            retEntry = memoryTreeMap.higherEntry(address);
        else if( direction == LOWER_EQUAL_ADDRESS)
            retEntry = memoryTreeMap.floorEntry(address);
                
        //Make sure it is allocated
        if( retEntry != null ){
            MemoryChunk prevChunk = retEntry.getValue();
            if( !prevChunk.isAllocated())
                retEntry = getAllocation(direction, retEntry.getKey());                
        }
        return retEntry;
    }
}
