package net.ddp2p.widgets.dir_fw_terms;
import static net.ddp2p.common.util.Util.__;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.JComboBox;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.data.D_DirectoryServerPreapprovedTermsInfo;
import net.ddp2p.common.table.directory_forwarding_terms;
import net.ddp2p.common.table.directory_tokens;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
@SuppressWarnings("serial")
public class TermsModel extends AbstractTableModel implements TableModel, DBListener{
	public static final int TABLE_COL_PRIORITY = 0;
	public static final int TABLE_COL_TOPIC = 1;
	public static final int TABLE_COL_AD = 2;
	public static final int TABLE_COL_PLAINTEXT = 3;
	public static final int TABLE_COL_PAYMENT = 4;
	public static final int TABLE_COL_SERVICE = 5;
	public static final int TABLE_COL_PRIORITY_TYPE = 6;
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	JComboBox<String> comboBox;
	JComboBox<String> serviceCBox;
	JComboBox<String> priorityTypeCBox;
	public long peerID =-1;
	public String dirAddr;
	public long _peerID =-1;
	public String _dirAddr;
	public long selectedInstanceID;
	public long _selectedInstanceID;
	private DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	String columnNames[]={__("Priority"),__("Topic"),__("AD"),__("Plaintext"),__("Payment"), __("Service"),  __("Priority Type")};
	ArrayList<D_DirectoryServerPreapprovedTermsInfo> data = new ArrayList<D_DirectoryServerPreapprovedTermsInfo>(); 
	private TermsPanel panel;
	public TermsModel(DBInterface _db, TermsPanel _panel) { 
		db = _db;
		panel = _panel;
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.directory_forwarding_terms.TNAME)), null);
	}
    public void setPeerID(long peerID2){
    	this._peerID = this.peerID = peerID2;
    }
    public void setDirAddr(String dirAddr){
    	this._dirAddr = this.dirAddr = dirAddr;
    }
    public void setSelectedInstanceID(long selectedInstanceID){
    	this._selectedInstanceID=this.selectedInstanceID = selectedInstanceID;
    }
    public int getLastPriority(){
    	if(data == null || data.size()==0)
    		return 0;
    	return (data.get(getRowCount()-1).priority);
    }
     public JComboBox getPriorityTypeComboBox(){
		if(DEBUG) System.out.println("DirModel: getModeComboBox: start:");
		priorityTypeCBox = new JComboBox<String>();
		priorityTypeCBox.addItem("Normal");
		priorityTypeCBox.addItem("Proactive");
		priorityTypeCBox.setSelectedIndex(0);
		return priorityTypeCBox;
	}
    public JComboBox getServiceComboBox(){
		if(DEBUG) System.out.println("DirModel: getModeComboBox: start:");
		serviceCBox = new JComboBox<String>();
		serviceCBox.addItem("All Services");
		serviceCBox.addItem("Forward");
		serviceCBox.addItem("Address");
		serviceCBox.addItem("Start up com.");
		serviceCBox.setSelectedIndex(0);
		return serviceCBox;
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
		if(DEBUG) System.out.println("TermsModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return Util.getString(columnNames[col]);
	}
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == TABLE_COL_SERVICE || col == TABLE_COL_PRIORITY_TYPE) return getValueAt(0, col).getClass();
		if(col == TABLE_COL_PRIORITY) return String.class;
		return Boolean.class;
	}
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		switch(columnIndex){
		case TABLE_COL_PRIORITY:
			return false;
		case TABLE_COL_PAYMENT:
			return false;
		}
		return true;
	}
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if((rowIndex<0) || (rowIndex>=data.size())) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		D_DirectoryServerPreapprovedTermsInfo crt = data.get(rowIndex);
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
		case TABLE_COL_PRIORITY:
			return data.get(rowIndex).priority;
		case TABLE_COL_SERVICE:
			return  serviceCBox.getItemAt(data.get(rowIndex).service);
		case TABLE_COL_PRIORITY_TYPE:
			return  priorityTypeCBox.getItemAt(data.get(rowIndex).priority_type); 
		}
		return null;
	}
	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if(DEBUG) System.out.println("TermsModel:setValueAt: r="+row +", c="+col+" val="+aValue);
		if((row<0) || (row>=data.size())) return;
		if((col<0) || (col>this.getColumnCount())) return;
		D_DirectoryServerPreapprovedTermsInfo crt = data.get(row);
		if(DEBUG) System.out.println("TermsModel:setValueAt: old crt="+crt);
		switch(col) {
		case TABLE_COL_TOPIC:
			crt.topic = ((Boolean) aValue).booleanValue();
			if(crt.topic && (getPanel().peerTopicTxt == null || getPanel().peerTopicTxt.getText().trim().equals("") ))
			{   Application_GUI.warning(__("Topic field has no value"), __("No topic assigned"));
				crt.topic = false;
			}else{
				if(directory_tokens.searchForToken(crt.peer_ID, crt.peer_instance_ID, crt.dir_addr, crt.dir_tcp_port)== null)
				{
					System.out.println("no token(topic) in DB!!!");
					return;
				}
			}	
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
		case TABLE_COL_PAYMENT:
			if(_DEBUG) System.out.println("TermsModel:setValueAt: PAYMENT");
			crt.payment = ((Boolean) aValue).booleanValue();
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_SERVICE:
			crt.service = getIndex(Util.getString(aValue), serviceCBox);
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_PRIORITY_TYPE:
			crt.priority_type = getIndex(Util.getString(aValue), priorityTypeCBox);
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		}
		fireTableCellUpdated(row, col);
	}
   public int getIndex(String item, JComboBox cbox){
   		for( int i=0; i<cbox.getItemCount(); i++){
   			if(cbox.getItemAt(i).equals(item))
   				return i;	
   		}
   		return -1;	
   }
	@Override
	public void addTableModelListener(TableModelListener l) {
	}
	@Override
	public void removeTableModelListener(TableModelListener l) {
	}
	static ArrayList<ArrayList<Object>> getTerms(long _peerID2, String dirAddr, long instanceID){
		String dir_domain= null;
		String dir_port = null;
    	if(dirAddr!=null){
    		dir_domain=dirAddr.split(":")[0];
    		dir_port=dirAddr.split(":")[1];
    	}
		String sql;
		String[]params;
		if(dirAddr!=null) {
			sql = "SELECT "+directory_forwarding_terms.fields_terms+
				" FROM  "+directory_forwarding_terms.TNAME+
			    " WHERE "+directory_forwarding_terms.peer_ID+" =? " +
			    " AND "+directory_forwarding_terms.peer_instance_ID+" =? "+
				" AND "+directory_forwarding_terms.dir_domain+" =? "+
				" AND "+directory_forwarding_terms.dir_tcp_port+" =? "+
				" ORDER BY "+directory_forwarding_terms.priority+";";
			params = new String[]{""+_peerID2, ""+instanceID, dir_domain, dir_port};
		}else{
			sql = "SELECT "+directory_forwarding_terms.fields_terms+
					" FROM  "+directory_forwarding_terms.TNAME+
				    " WHERE "+directory_forwarding_terms.peer_ID+" =? "+
				    " AND   "+directory_forwarding_terms.peer_instance_ID+" =? "+
				    " AND   "+directory_forwarding_terms.dir_domain+" IS NULL "+
				    " AND   "+directory_forwarding_terms.dir_tcp_port+" IS NULL "+
				    " ORDER BY "+directory_forwarding_terms.priority+";";
			params = new String[]{""+_peerID2, ""+instanceID};
		}
		ArrayList<ArrayList<Object>> u;
		try {
			u = Application.getDB().select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		return u;
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		if(DEBUG) System.out.println("TermsModel: update: start: table="+Util.concat(table, " ", "null"));
		ArrayList<ArrayList<Object>> u;
		if (DEBUG) System.out.println("TermsModel:update (_peerID, _dirAddr, _selectedInstanceID) = ("+_peerID+", "+ _dirAddr+", "+ _selectedInstanceID+")");
		u = getTerms(_peerID, _dirAddr, _selectedInstanceID);
		if(u==null){
			System.out.println("TermsModel.update(): u=null");
			 return;
		}
		if((u.size()==0) && (_peerID!=0) && (_dirAddr!=null) && (_selectedInstanceID!=-1)) {
			u = getTerms(_peerID, null, _selectedInstanceID); if(u==null) return;
			if(u.size()==0) {
				u = getTerms(0, _dirAddr, _selectedInstanceID); if(u==null) return;
				if(u.size()==0) {
					u = getTerms(_peerID, _dirAddr, -1); if(u==null) return;
					if(u.size()==0) {
						u = getTerms(0, null, -1); if(u==null) return;
						getPanel().setGeneralGlobal();
						_dirAddr = null; _peerID = 0; _selectedInstanceID=-1;
				}else{
					getPanel().setGeneralDirectory();
					_peerID = 0; _selectedInstanceID=-1;
				}
			}else{
				getPanel().setGeneralPeer();
				_dirAddr = null;
			}
		}
		}
		data = new ArrayList<D_DirectoryServerPreapprovedTermsInfo>();
		for(ArrayList<Object> _u :u){
			D_DirectoryServerPreapprovedTermsInfo ui = new D_DirectoryServerPreapprovedTermsInfo(_u);
			if(DEBUG) System.out.println("TermsModel: update: add: "+ui);
			data.add(ui); 
		}
		this.fireTableDataChanged();
	}
	public int getPriority(int row){
		return 	data.get(row).priority;
	}
	public void shiftByOne(int priority)throws P2PDDSQLException{
		System.out.println("data size: "+ data.size());
		for(int i=priority-1; i< data.size(); i++ ){
			data.get(i).priority -= 1;
			data.get(i).storeNoSync("update");
		}	
	}
	public void swap(int r1 ,int r2) throws P2PDDSQLException{
		D_DirectoryServerPreapprovedTermsInfo temp;
		temp = data.get(r1);
		data.set(r1, data.get(r2));
		data.set(r2, temp);
		int priorityTemp = data.get(r1).priority;
		data.get(r1).priority = data.get(r2).priority;
		data.get(r2).priority = priorityTemp;
		data.get(r1).storeNoSync("update");
		data.get(r2).storeNoSync("update");
		this.fireTableDataChanged();
	}   
    private TermsPanel getPanel() {
		return panel;
	}
	public void refresh() {
		data.removeAll(data);
		update(null, null);
	}
	public void setTable(TermsTable termsTable) {
		tables.add(termsTable);
	}
	public long get_TermID(int row) {
		if(row<0) return -1;
		try{
			return data.get(row).term_ID;
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}
}
