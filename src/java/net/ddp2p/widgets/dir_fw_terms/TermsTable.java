package net.ddp2p.widgets.dir_fw_terms;
import static net.ddp2p.common.util.Util.__;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.KeyEvent;
import java.awt.Point;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.DefaultCellEditor;
import javax.swing.table.TableColumn;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.widgets.app.DDIcons;
public class TermsTable extends JTable implements MouseListener{
	public static boolean DEBUG = false;
	public static boolean _DEBUG = true;
	private DefaultTableCellRenderer centerRenderer;
	private TermsPanel termsPanel;
	public TermsTable(TermsPanel termsPanel) {
		super(new TermsModel(Application.getDB(), termsPanel));
		this.termsPanel = termsPanel;
		init();
	}
	public TermsTable(DBInterface _db, TermsPanel termsPanel) {
		super(new TermsModel(_db, termsPanel));
		init();
	}
	public TermsTable(TermsModel dm) {
		super(dm);
		init();
	}
	public TermsModel getModel(){
		return (TermsModel)super.getModel();
	}
	void init(){
		if(DEBUG) System.out.println("TermsTable:init:start");
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		initColumnSizes();
		getColumnModel().getColumn(TermsModel.TABLE_COL_SERVICE).setCellRenderer(new DefaultTableCellRenderer());
		getColumnModel().getColumn(TermsModel.TABLE_COL_SERVICE).setCellEditor(new DefaultCellEditor(getModel().getServiceComboBox()));
		getColumnModel().getColumn(TermsModel.TABLE_COL_PRIORITY_TYPE).setCellRenderer(new DefaultTableCellRenderer());
		getColumnModel().getColumn(TermsModel.TABLE_COL_PRIORITY_TYPE).setCellEditor(new DefaultCellEditor(getModel().getPriorityTypeComboBox()));		
		getColumnModel().getColumn(TermsModel.TABLE_COL_PRIORITY).setWidth(0);
		getColumnModel().getColumn(TermsModel.TABLE_COL_PRIORITY).setMaxWidth(0);
		getColumnModel().getColumn(TermsModel.TABLE_COL_PRIORITY).setMinWidth(0);
		getModel().update(null,null);
		if(DEBUG) System.out.println("termsTable:init:done");
  	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		jtableMouseReleased(e);
	}
	@Override
	public void mousePressed(MouseEvent e) {
		jtableMouseReleased(e);
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		jtableMouseReleased(e);
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		jtableMouseReleased(e);
	}
	@Override
	public void mouseExited(MouseEvent e) {
	}
	private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
		if(DEBUG) System.out.println("termsTable:jtableMouseReleased: mouserelease");
    	int _row; 
    	int model_row = -1;
    	int col; 
    	if(!evt.isPopupTrigger()){
    		if(DEBUG) System.out.println("termsTable:jtableMouseReleased: not popup");
    		return;
    	}
    	Point point = evt.getPoint();
        _row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(_row, _row);
        if(_row>=0) model_row = this.convertRowIndexToModel(_row);
    	JPopupMenu popup = getPopup(model_row,col);
    	if(popup == null){
    		if(DEBUG) System.out.println("termsTable:jtableMouseReleased: popup null");
    		return;
    	}
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
    JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	ImageIcon upicon = DDIcons.getImpImageIcon(__("move up"));
    	ImageIcon downicon = DDIcons.getImpImageIcon(__("move down"));
    	JPopupMenu popup = new JPopupMenu();
    	TermsCustomAction aAction;
    	aAction = new TermsCustomAction(this, __("Add a Term"), addicon,__("Add a Term"), __("AddTerm"),KeyEvent.VK_A, TermsCustomAction.M_ADD);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	aAction = new TermsCustomAction(this, __("Move UP"), upicon,__("Move UP"), __("MoveUP"),KeyEvent.VK_U, TermsCustomAction.M_UP);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	aAction = new TermsCustomAction(this, __("Move Down"), downicon,__("Move Down"), __("MoveDown"),KeyEvent.VK_O, TermsCustomAction.M_DOWN);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	aAction = new TermsCustomAction(this, __("Delete"), delicon,__("Delete Mirror"), __("Delete"),KeyEvent.VK_D, TermsCustomAction.M_DELETE);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	return popup;
	}
	private void initColumnSizes() {
		this.rowHeight=20;
        TermsModel model = (TermsModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
        for (int i = 0; i < model.getColumnCount(); i++) {
        	int headerWidth = 0;
        	int cellWidth = 0;
        	column = this.getColumnModel().getColumn(i);
            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
            for(int r=0; r<model.getRowCount(); r++) {
            	comp = this.getDefaultRenderer(model.getColumnClass(i)).
                             getTableCellRendererComponent(
                                 this, getValueAt(r, i),
                                 false, false, 0, i);
            	cellWidth = Math.max(comp.getPreferredSize().width, cellWidth);
            }
            if (DEBUG) {
                System.out.println("Initializing width of column "
                                   + i + ". "
                                   + "headerWidth = " + headerWidth
                                   + "; cellWidth = " + cellWidth);
            }
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
    }
}
