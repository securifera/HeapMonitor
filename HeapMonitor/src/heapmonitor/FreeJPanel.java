package heapmonitor;

import static heapmonitor.MainFrame.COLORIZE_ALLOC;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author b0yd
 */
public class FreeJPanel extends javax.swing.JPanel {

    private final MainFrame parentFrame;  
    private boolean autoScroll = false;
    
    /**
     * Creates new form AllocationJPanel
     * @param parent
     */
    public FreeJPanel(  MainFrame parent ) {
        initComponents();
        initializeComponents();
        parentFrame = parent;     
    }
    
    //=======================================================================
    /**
     * 
     */
    private void initializeComponents() {
    
        DefaultListModel listModel = new DefaultListModel();
        freeJList.setModel(listModel);
        freeJList.setFont( new Font(Font.MONOSPACED, Font.PLAIN, 12 ));
        freeJList.addListSelectionListener( new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if( !e.getValueIsAdjusting()){
                    loadMemoryAddr();
                }
            }
        });
        
        freeJList.addMouseListener( new MouseAdapter(){
            @Override
            public void mouseReleased(MouseEvent e){
                if(e.isPopupTrigger()){
                    doTreePopupMenuLogic(e);
                } 
            }
        });
        
        freeJList.setCellRenderer( new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus ) {  
                Component c = super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );  
                if( value instanceof MemoryChunk ){
                    MemoryChunk mem = (MemoryChunk)value;
                    Color val_c = mem.getColor();
                    if( val_c != Color.BLUE ) {  
                        c.setBackground( val_c );  //yellow every even row
                    } 
                }
                return c;  
            }  
        });
    }
   
    
    //=======================================================================
    /**
    *  Determines what menu options to show on the popup menu based on the
    *  {@link XmlObject} object contained in the currently selected node.
    *
    *  @param  e   the {@code MouseEvent} that triggered the popup
    */
    public void doTreePopupMenuLogic( MouseEvent e ) {       
        
       JPopupMenu popup = new JPopupMenu();
       JMenuItem menuItem;
       
       menuItem = new JMenuItem( "Colorize");
       menuItem.setActionCommand( MainFrame.COLORIZE_FREE );
       menuItem.addActionListener(parentFrame);
       menuItem.setEnabled( true );
       popup.add(menuItem);

       if( popup.getComponentCount() > 0 )
          popup.show(e.getComponent(), e.getX(), e.getY());
       
    }
    
    //=======================================================================
    /**
     * 
     * @return 
     */
    public MemoryChunk getSelected() {
        return (MemoryChunk)freeJList.getSelectedValue();
    }
    
    private void loadMemoryAddr(){
        MemoryChunk aChunk = (MemoryChunk)freeJList.getSelectedValue();
        if( aChunk != null ){
            parentFrame.getTracePanel().setStackTraceTextArea( aChunk.getTraceHistory() );   
            parentFrame.getMemoryPanel().loadMemoryPage( aChunk.getAddress(), false );
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        allocationScrollPane = new javax.swing.JScrollPane();

        allocationScrollPane.setBorder(null);
        allocationScrollPane.setPreferredSize(new java.awt.Dimension(45, 165));

        allocationScrollPane.setViewportView(freeJList);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 185, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(allocationScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 353, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(allocationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane allocationScrollPane;
    private final javax.swing.JList freeJList = new javax.swing.JList();
    // End of variables declaration//GEN-END:variables

    //=======================================================================
    /**
     * 
     * @param aChunk 
     */
    public void addFree(MemoryChunk aChunk) {
        DefaultListModel listModel = (DefaultListModel) freeJList.getModel();
        listModel.removeElement( aChunk );
        listModel.addElement( aChunk );   
        
         //If autoscroll
        if( autoScroll ){
            int lastIndex = listModel.getSize() - 1;
            if (lastIndex >= 0) {
                freeJList.ensureIndexIsVisible(lastIndex);
            }
        }
    }

    //=======================================================================
    /**
     * 
     * @param aChunk 
     */
    public void removeMemoryChunk(MemoryChunk aChunk) {
        DefaultListModel listModel = (DefaultListModel) freeJList.getModel();
        listModel.removeElement( aChunk );
    }

    //=======================================================================
    /**
     * 
     */
    public void clearPanel() {
        DefaultListModel listModel = (DefaultListModel)freeJList.getModel();
        listModel.clear();
    }
    
    //=========================================================================
    /**
     * 
     * @param selected 
     */
    public void setAutoscrollFlag(boolean selected) {
        autoScroll = selected;
    }

    //=========================================================================
    /**
     * 
     * @param object 
     */
    public void setSelected(Object object) {
        if( object == null)
            freeJList.clearSelection();
        else
            freeJList.setSelectedValue(object, true);
    }
}
