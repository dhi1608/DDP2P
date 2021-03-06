package net.ddp2p.widgets.dir_management;
import static net.ddp2p.common.util.Util.__;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Date;
import java.util.Calendar;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.JComboBox;
import javax.swing.DefaultCellEditor;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.data.D_DirectoryServerSubscriberInfo;
import net.ddp2p.common.table.registered;
import net.ddp2p.common.table.subscriber;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
class CBoxItem {
	public String GID;
	public String name;
	public String GID_hash;
	public String toString(){
		return name;
	}
} 
@SuppressWarnings("serial")
public class DirModel extends AbstractTableModel implements TableModel, DBListener{
	public static final int TABLE_COL_NAME = 0;
	public static final int TABLE_COL_INSTANCE = 1;
	public static final int TABLE_COL_TOPIC = 2;
	public static final int TABLE_COL_AD = 3;
	public static final int TABLE_COL_PLAINTEXT = 4;
	public static final int TABLE_COL_PAYMENT = 5;
	public static final int TABLE_COL_MODE = 6;
	public static final int TABLE_COL_EXPIRATION = 7;
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	JComboBox<CBoxItem> namesCBox = new JComboBox<CBoxItem>();
	JComboBox<String> modeCBox;
	public DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	DirTable dirTable;
	String columnNames[]={__("Name"),__("Instance"),__("Topic"),__("AD"),__("Plaintext"),__("Payment"),__("Services"),__("Expiration")};
	ArrayList<D_DirectoryServerSubscriberInfo> data = new ArrayList<D_DirectoryServerSubscriberInfo>();
	private DirPanel panel;
	public DirModel(DBInterface _db, DirPanel _panel) { 
		if(_db==null){
			System.err.println("DirModel:<init>: no directory database");
			panel = _panel;
			return;
		}
		db = _db;
		panel = _panel;
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.subscriber.TNAME,net.ddp2p.common.table.peer.TNAME)), null);
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
		if(DEBUG) System.out.println("DirModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return Util.getString(columnNames[col]);
	}
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == TABLE_COL_NAME ||col == TABLE_COL_INSTANCE || col==TABLE_COL_MODE || col == TABLE_COL_EXPIRATION) return getValueAt(0, col).getClass();
		return Boolean.class;
	}
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if((rowIndex<0) || (rowIndex>=data.size())) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		D_DirectoryServerSubscriberInfo crt = data.get(rowIndex);
		if(crt==null) return null;
		switch(columnIndex){
		case TABLE_COL_TOPIC:
			return data.get(rowIndex).topic;
		case TABLE_COL_AD:
			return data.get(rowIndex).ad;
		case TABLE_COL_PLAINTEXT:
			return data.get(rowIndex).plaintext;
		case TABLE_COL_PAYMENT:
			return data.get(rowIndex).payment;
		case TABLE_COL_EXPIRATION:
			 if(data.get(rowIndex).expiration != null)
				return data.get(rowIndex).expiration.getTime().toGMTString();
			 return null;
		case TABLE_COL_NAME:
				return	data.get(rowIndex).name;	 
		case TABLE_COL_INSTANCE:
				return	data.get(rowIndex).instance;
		case TABLE_COL_MODE:
			return  data.get(rowIndex).mode; 
		}
		return null;
	}
	public JComboBox<CBoxItem> getComboBox(String gid){
		if(DEBUG) System.out.println("DirModel: getComboBox: start:");
		String sql = "SELECT DISTINCT "+registered.global_peer_ID+" ,"+ registered.global_peer_ID_hash+" FROM "+registered.TNAME+";";
		String[]params = new String[]{};
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		namesCBox.removeAllItems() ;
		for(ArrayList<Object> _u :u){
			CBoxItem item = new CBoxItem();
			item.GID = Util.getString(_u.get(0));
			item.name= Util.getString(_u.get(1)); 
			namesCBox.addItem(item);
		}
		CBoxItem item = new CBoxItem();
		item.name="Default";
		namesCBox.addItem(item);
		namesCBox.setToolTipText("test");
		return namesCBox;
	}
	JComboBox<String> instanceCBox = new JComboBox<String>();
	public JComboBox<String> getInstanceComboBox(){
		if(DEBUG) System.out.println("DirModel: getInstanceComboBox: start:");
		String sql = "SELECT "+registered.instance+
			         " FROM  "+registered.TNAME+
			         " WHERE  "+registered.global_peer_ID+" = ?" +" ;";
		if(((CBoxItem)namesCBox.getSelectedItem()).GID==null) return instanceCBox;	         
		String[]params = new String[]{""+((CBoxItem)namesCBox.getSelectedItem()).GID};// where clause?
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		instanceCBox.removeAllItems();
		for(ArrayList<Object> _u :u){
			instanceCBox.addItem(Util.getString(_u.get(0)));
		}
		instanceCBox.addItem("all instances");
		return instanceCBox;
	}
	public JComboBox<String> getModeComboBox(){
		if(DEBUG) System.out.println("DirModel: getModeComboBox: start:");
		modeCBox = new JComboBox<String>();
		modeCBox.addItem("Forward");
		modeCBox.addItem("Address");
		modeCBox.addItem("Start up com.");
		modeCBox.addItem("All Services");
		modeCBox.setSelectedIndex(0);
		return modeCBox;
	}
	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if(_DEBUG) System.out.println("DirModel:setValueAt: r="+row +", c="+col+" val="+aValue);
		if((row<0) || (row>=data.size())) return;
		if((col<0) || (col>this.getColumnCount())) return;
		D_DirectoryServerSubscriberInfo crt = data.get(row);
		if(_DEBUG) System.out.println("DirModel:setValueAt: old crt="+crt);
		switch(col) {
		case TABLE_COL_TOPIC:
			crt.topic = ((Boolean) aValue).booleanValue();
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_AD:
			crt.ad = ((Boolean) aValue).booleanValue();
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_PLAINTEXT:
			crt.plaintext = ((Boolean) aValue).booleanValue();
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_EXPIRATION:
			crt.expiration = Calendar.getInstance();
			try{
				crt.expiration.setTime(new Date(Util.getString(aValue)));
			}
			catch(IllegalArgumentException e){	   
			   crt.expiration =null;
			   if(Util.getString(aValue)!=null && !Util.getString(aValue).trim().equals(""))
			   		Application_GUI.warning(Util.getString(aValue)+ " is not a correct date format" , "Date format");	
			}
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_NAME:
			if (aValue instanceof CBoxItem){
				crt.GID = ((CBoxItem)aValue).GID;
				crt.name = Util.getString( aValue);
				try {
					crt.storeNoSync("update");
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}
			break;
		case TABLE_COL_INSTANCE:
			crt.instance = Util.getString( aValue);
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_MODE:
			crt.mode = Util.getString( aValue);
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_PAYMENT:
			if(_DEBUG) System.out.println("DirModel:setValueAt: PAYMENT");
			crt.payment = ((Boolean) aValue).booleanValue();
			try {
				crt.storeNoSync("update");
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
		if(DEBUG ) System.out.println("DirModel: update: start:");
		getComboBox(null);
		dirTable.repaint();
		String sql =
				"SELECT "+
						subscriber.fields_subscribers+
				" FROM "+subscriber.TNAME+";";
		String[]params = new String[]{};
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		data = new ArrayList<D_DirectoryServerSubscriberInfo>();
		for(ArrayList<Object> _u :u){
			D_DirectoryServerSubscriberInfo ui = new D_DirectoryServerSubscriberInfo(_u, db);
			if(DEBUG) System.out.println("DirModel: update: add: "+ui);
			data.add(ui); 
		}
		this.fireTableDataChanged();
		DEBUG=false;
	}
	public void refresh() {
		data.removeAll(data);
		update(null, null);
	}
	public void setTable(DirTable dirTable) {
		this.dirTable = dirTable;
	}
public long getSubscriberID(int row) {
		if(row<0) return -1;
		try{
			return data.get(row).subscriber_ID;
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}
}
