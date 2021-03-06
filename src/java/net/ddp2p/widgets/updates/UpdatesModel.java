package net.ddp2p.widgets.updates;
import static net.ddp2p.common.util.Util.__;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.JButton;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_MirrorInfo;
import net.ddp2p.common.table.mirror;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
public class UpdatesModel extends AbstractTableModel implements TableModel, DBListener , ActionListener{
	public static final int TABLE_COL_NAME = 0; 
	public static final int TABLE_COL_URL = 1; 
	public static final int TABLE_COL_LAST_VERSION = 2; 
	public static final int TABLE_COL_USED = 3; 
	public static final int TABLE_COL_QOT_ROT = 4; 
	public static final int TABLE_COL_DATE = 5; 
	public static final int TABLE_COL_ACTIVITY = 6; 
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	JComboBox comboBox;
	private DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	String columnNames[]={__("Name"),__("URL"),__("Last Version"),__("Use"),__("Tester QoT & RoT"), __("Last Contact"),__("Activity")};
	ArrayList<D_MirrorInfo> data = new ArrayList<D_MirrorInfo>(); 
	public UpdatesModel(DBInterface _db) { 
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.mirror.TNAME)), null);
	}
	@Override
	public int getRowCount() {
		return data.size();
	}
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	@Override
	public String getColumnName(int col) {
		if(DEBUG) System.out.println("UpdatesModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return columnNames[col].toString();
	}
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == TABLE_COL_ACTIVITY) return Boolean.class;
		if(col == TABLE_COL_USED) return Boolean.class;
		if(col == TABLE_COL_QOT_ROT) return PanelRenderer.class;
		return String.class;
	}
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		switch(columnIndex){
		case TABLE_COL_NAME:
		case TABLE_COL_USED:
		case TABLE_COL_URL:
		case TABLE_COL_QOT_ROT:
			return true;
		}
		return false;
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if((rowIndex<0) || (rowIndex>=data.size())) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		D_MirrorInfo crt = data.get(rowIndex);
		if(crt==null) return null;
		switch(columnIndex){
		case TABLE_COL_NAME:
			String result = null;
			result = data.get(rowIndex).my_mirror_name;
			if(result == null) result = data.get(rowIndex).original_mirror_name;
			return result;
		case TABLE_COL_URL:
			return data.get(rowIndex).url;
		case TABLE_COL_LAST_VERSION:
			return data.get(rowIndex).last_version;
		case TABLE_COL_USED:
			return data.get(rowIndex).used; 
		case TABLE_COL_QOT_ROT:
			if(DEBUG)System.out.println("UpdatesModel:getValueAt:TABLE_COL_QOT_ROT");
			if(data.get(rowIndex).testerInfo==null) return null;
			comboBox = new JComboBox();
			for(int i=0; i<data.get(rowIndex).testerInfo.length; i++)
				comboBox.addItem(data.get(rowIndex).testerInfo[i].name);
			JPanel p = new JPanel(new BorderLayout());
			p.add(comboBox);
			TableJButton b = new TableJButton("...",rowIndex );
			b.addActionListener(this);
			if(DEBUG)System.out.println("UpdatesModel:getValueAt:TABLE_COL_QOT_ROT: row"+b.rowNo); 
			b.setPreferredSize(new Dimension(20,30));
			p.add(b, BorderLayout.EAST);
			return p;
		case TABLE_COL_DATE:
			if(crt.last_contact_date == null) return null;
			return Util.getString(crt.last_contact_date.getTime());
		}
		return null;
	}
	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if(DEBUG) System.out.println("setVlaueAt"+row +", "+col);
		if((row<0) || (row>=data.size())) return;
		if((col<0) || (col>this.getColumnCount())) return;
		D_MirrorInfo crt = data.get(row);
		switch(col) {
		case TABLE_COL_NAME:
			crt.my_mirror_name = Util.getString(aValue);
			if(crt.my_mirror_name!=null) crt.my_mirror_name = crt.my_mirror_name.trim();
			if("".equals(crt.my_mirror_name)) crt.my_mirror_name = null;
			try {
				data.get(row).store(D_MirrorInfo.action_update);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_URL:
			data.get(row).url = Util.getString(aValue);
			try {
				data.get(row).store(D_MirrorInfo.action_update);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_USED:
			data.get(row).used = ((Boolean) aValue).booleanValue();
			try {
				data.get(row).store(D_MirrorInfo.action_update);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		}
		fireTableCellUpdated(row, col);
	}
	@Override
	public void addTableModelListener(TableModelListener l) {
	}
	@Override
	public void removeTableModelListener(TableModelListener l) {
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		if(DEBUG) System.out.println("UpdatesModel: update: start:"+table);
		String sql = "SELECT "+mirror.fields_updates+" FROM "+mirror.TNAME+";";
		String[]params = new String[]{};
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		data = new ArrayList<D_MirrorInfo>();
		for(ArrayList<Object> _u :u){
			D_MirrorInfo ui = new D_MirrorInfo(_u);
			if(DEBUG) System.out.println("UpdatesModel: update: add: "+ui);
			data.add(ui); 
		}
		this.fireTableDataChanged();
	}
    public void refresh() {
		data.removeAll(data);
		update(null, null);
	}
	public void setTable(UpdatesTable updatesTable) {
		tables.add(updatesTable);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		TableJButton bb =(TableJButton)e.getSource();
		QualitesTable q = new QualitesTable(data.get(bb.rowNo));
		JPanel p = new JPanel(new BorderLayout());
		p.add(q.getScrollPane());
		final JFrame frame = new JFrame();
		frame.setContentPane(p);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.pack();
		frame.setSize(600,300);
		frame.setVisible(true);
        JButton okBt = new JButton("   OK   ");
        okBt.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            frame.hide();
           }
        });
		p.add(okBt,BorderLayout.SOUTH);
	}
	public static void main(String args[]) {
		JFrame frame = new JFrame();
		try {
			Application.setDB(new DBInterface(Application.DEFAULT_DELIBERATION_FILE));
			JPanel test = new JPanel();
			test.setLayout(new BorderLayout());
			UpdatesTable t = new UpdatesTable(Application.getDB());
			test.add(t);
			test.add(t.getTableHeader(),BorderLayout.NORTH);
			frame.setContentPane(test);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setSize(800,300);
			frame.setVisible(true);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public long get_UpdatesID(int row) {
		if(row<0) return -1;
		try{
			return data.get(row).mirror_ID;
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}
	public String get_UpdatesURL(int row) {
		if(row<0) return null;
		try{
			return data.get(row).url;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
		public String get_UpdatesLastVer(int row) {
		if(row<0) return null;
		try{
			return data.get(row).last_version;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
}
