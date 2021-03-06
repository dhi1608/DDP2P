/*   Copyright (C) 2011 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
 package net.ddp2p.widgets.constituent;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.config.Language;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_OID;
import net.ddp2p.common.data.D_OrgParam;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.hds.ClientSync;
import net.ddp2p.common.population.ConstituentData;
import net.ddp2p.common.population.ConstituentsAddressNode;
import net.ddp2p.common.population.ConstituentsBranch;
import net.ddp2p.common.population.ConstituentsCensus;
import net.ddp2p.common.population.ConstituentsIDNode;
import net.ddp2p.common.population.ConstituentsInterfaceDone;
import net.ddp2p.common.population.ConstituentsInterfaceInput;
import net.ddp2p.common.population.ConstituentsNode;
import net.ddp2p.common.population.ConstituentsPropertyNode;
import net.ddp2p.common.population.Constituents_AddressAncestors;
import net.ddp2p.common.population.Constituents_LocationData;
import net.ddp2p.common.population.Constituents_NeighborhoodData;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.MainFrame;
import net.ddp2p.widgets.components.TreeModelSupport;
import net.ddp2p.widgets.org.OrgExtra;
import java.util.*;
import java.text.MessageFormat;
import static net.ddp2p.common.util.Util.__;
public class ConstituentsModel extends TreeModelSupport implements TreeModel, DBListener, ConstituentsInterfaceInput, ConstituentsInterfaceDone {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private ConstituentsAddressNode root;
	DBInterface db;
	boolean automatic_refresh = DD.DEFAULT_AUTO_CONSTITUENTS_REFRESH;
	private long[] fieldIDs;
	private long organizationID;
	private D_Organization crt_org;
	private long my_constituentID=-1;
	private String my_global_constituentID=null;
	private String subdivisions;
	private SK my_sk = null;
	boolean hasNeighborhoods;
	public ConstituentsCensus census=null;
	private long census_value;
	ArrayList<JTree> trees = new ArrayList<JTree>();
	private RefreshListener refreshListener;
	public void setTree(JTree tree) {
		if(trees.contains(tree)) return;
		trees.add(tree);
	}
	public D_Constituent getConstituentMyself(){
		return D_Constituent.getConstByLID(getConstituentIDMyself(), false, false);
	}
	public long getConstituentIDMyself(){
		return my_constituentID;
	}
	public String getConstituentGIDMyself(){
		return my_global_constituentID;
	}
	public SK getConstituentSKMyself(){
		return my_sk;
	}
	public long getOrganizationID(){
		return this.organizationID;
	}
	public String getConstituentMyselfName() {
		long lid = getConstituentIDMyself();
		D_Constituent cons = null;
		if (lid > 0) cons = D_Constituent.getConstByLID(lid, true, false);
		if (cons == null) return __("None");
		return cons.getSurname();
	}
	public String getConstituentMyselfNames() {
		D_Constituent cons = D_Constituent.getConstByLID(getConstituentIDMyself(), true, false);
		if (cons == null) return __("None");
		return cons.getNameFull();
	}
	/**
	 * Set a current constituent as myself, for witnessing, etc.
	 * @param _constituent_ID
	 * @param global_constituent_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	boolean setConstituentIDMyself(long _constituent_ID, String global_constituent_ID) throws P2PDDSQLException{
		if ((_constituent_ID <= 0) && (global_constituent_ID == null)) {
			my_constituentID = -1;
			my_global_constituentID = null;
			my_sk = null;
			return true;
		}
		if (global_constituent_ID == null) {
			global_constituent_ID = D_Constituent.getGIDFromLID(_constituent_ID);
		}
		if (global_constituent_ID == null) {
			Util.printCallPathTop("lID="+_constituent_ID+" GID="+global_constituent_ID);
			Application_GUI.warning(__("This Constituent cannot be set to myself (no GID"), __("Cannot be Myself!"));
			return false;
		}
		if (_constituent_ID < 0)
			_constituent_ID = D_Constituent.getLIDFromGID(global_constituent_ID, this.organizationID);
		SK sk = DD.getConstituentSK(_constituent_ID);
		if (sk == null) {
			Application_GUI.warning(__("Constituent cannot be set to myself (no SK)"+_constituent_ID), __("Cannot be Myself!"));
			return false;
		}
		my_sk = sk;
		my_constituentID = _constituent_ID;
		my_global_constituentID = global_constituent_ID;
		if(DEBUG) System.err.println("ConstituentsModel:setConstituentIDMyself: my_ID="+_constituent_ID+" my_GID="+global_constituent_ID);
		return true;
	}
	public ConstituentsModel(DBInterface _db, long organizationID2, 
			long _constituentID, String _global_constituentID, D_Organization org, RefreshListener _refreshListener) {
		if(DEBUG) System.err.println("ConstituentsModel: start org="+organizationID2+
				" myconstID="+_constituentID+" gID="+_global_constituentID);
		db = _db;
		refreshListener = _refreshListener;
		if(db == null) {
			JOptionPane.showMessageDialog(null,__("No database in Model!"));
			return;
		}
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.constituent.TNAME, net.ddp2p.common.table.witness.TNAME, net.ddp2p.common.table.neighborhood.TNAME, net.ddp2p.common.table.field_value.TNAME)), null);
		try {
			init(organizationID2, _constituentID, _global_constituentID, org);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}catch(Exception e){
			JOptionPane.showMessageDialog(null,e.toString());
			e.printStackTrace();
			return;
		}
	}
	public ConstituentsIDNode expandConstituentID(JTree tree, String constituentID, boolean census) {
		ConstituentsIDNode cin = null;
		if(DEBUG) System.err.println("ConstituentsModel:expandConstituentID start cID="+constituentID);
		if(constituentID == null) return null;
		D_Constituent c;
		c = D_Constituent.getConstByLID(constituentID, true, false); 
		if (c == null) {
			if (_DEBUG) System.err.println("ConstituentsModel:expandConstituentID null constituent = "+constituentID);
			return null;
		}
		c.loadNeighborhoods(D_Constituent.EXPAND_ALL);
		if ((c.getNeighborhood()==null) || (c.getNeighborhood().length == 0)) {
			if(DEBUG) System.err.println("ConstituentsModel:expandConstituentID root constituent="+c);
			return null;
		}
		ConstituentsAddressNode n = expandNeighborhoodID(tree, getRoot(), c.getNeighborhood());
		if(census)runCensus();
		if(n!=null){
			cin = n.getChildByConstituentID(new Integer(constituentID).longValue());
		}
		if(DEBUG) System.err.println("ConstituentsModel:expandConstituentID end");
		return cin;
	}
	public ConstituentsAddressNode expandNeighborhoodID(ConstituentsTree tree, String nID) throws P2PDDSQLException {
		D_Neighborhood neighborhood[] = D_Neighborhood.getNeighborhoodHierarchy(null, nID, D_Constituent.EXPAND_ALL, this.getOrganizationID());
		return expandNeighborhoodID(tree, getRoot(), neighborhood);
	}
	public static ConstituentsAddressNode expandNeighborhoodID(JTree tree, ConstituentsAddressNode crt, D_Neighborhood neighborhood[]) {
		if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID begin");
		ConstituentsAddressNode child=null;
		ArrayList<Object> _crt_path= new ArrayList<Object>();
		_crt_path.add(crt);
		for(int k=neighborhood.length-1; k>=0; k--) {
			if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID k="+k);
			String nGID = neighborhood[k].getGID();
			String nID = neighborhood[k].getLIDstr(); 
			if (nID == null) {
				nID = D_Neighborhood.getLIDstrFromGID(nGID, neighborhood[k].getOrgLID());
				if (nID == null) return null;
			}
			long neighborhoodID = Util.lval(nID, 0);
			if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID nID="+neighborhoodID+" n="+neighborhood[k]);
			child = crt.getChildByID(neighborhoodID);
			if(child == null) {
				if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID end of children, STOP");
				return null;
			}
			if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID end of child="+child);
			_crt_path.add(child);
			Object crtpath[] = _crt_path.toArray();
			if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID expand path="+Util.concat(crtpath, "#"));
			if(child.isColapsed()) {
				child.populate();
				tree.expandPath(new TreePath(crtpath));
			}
			crt = child;
		}
		if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID end");
		return child;
	}
	public void runCensus() {
		this.stopCensusRequest();
		this.startCensus();
	}
	public void doRefreshAll() throws P2PDDSQLException{
		if(DEBUG) System.err.println("ConstituentsModel:deRefreshAll: start");
		ConstituentsModel model = this;
		Object oldRoot = model.getRoot();
		model.init(model.getOrganizationID(), model.getConstituentIDMyself(), model.getConstituentGIDMyself(), model.getOrganization());
		if(trees.size()>1)if(_DEBUG) System.err.println("ConstituentsModel: doRefreshAll:Too many JTrees");
		Object model_root = model.getRoot();
		for(JTree tree: trees) {
			if(DEBUG) System.err.println("ConstituentsModel:deRefreshAll: tree="+tree);
			if(model_root!=null)model.fireTreeStructureChanged(new TreeModelEvent(tree,new Object[]{model_root}));
			model.refresh(new JTree[]{tree}, oldRoot);
			if(DEBUG) System.err.println("ConstituentsModel:deRefreshAll: refreshed");
		}
		if(DEBUG) System.err.println("ConstituentsModel:deRefreshAll: will census");
		model.runCensus();
		if(DEBUG) System.err.println("ConstituentsModel:deRefreshAll: done");
	}
	/**
	 * Will try to keep the same nodes expanded
	 * @param organizationID2
	 * @param _constituentID
	 * @param _global_constituentID
	 * @throws P2PDDSQLException
	 */
	public void refresh(JTree trees[], Object _old_root) throws P2PDDSQLException {
		if(this.refreshListener != null) this.refreshListener.disableRefresh();
		if(DEBUG) System.err.println("ConstituentsModel:refresh start");
		if((_old_root==null) || !(_old_root instanceof ConstituentsAddressNode)){
			if(DEBUG) System.err.println("ConstituentsModel:refresh  Abandoned no root: "+getRoot());
			return;
		}
		ConstituentsAddressNode old_root = (ConstituentsAddressNode)_old_root;
		for(JTree tree: trees)
			translate_expansion(tree, old_root, getRoot());
		if(DEBUG) System.err.println("ConstituentsModel:refresh Done");
	}
	private void translate_expansion(JTree tree, ConstituentsNode _old_root,
			ConstituentsNode _new_root) {
		if(DEBUG) System.err.println("ConstituentsModel:translate_expansion start \""+_old_root+"\" vs. \""+_new_root+"\"");
		if(!(_old_root instanceof ConstituentsBranch)){
			if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop old is leaf");
			return;
		}
		if(!(_new_root instanceof ConstituentsBranch)){
			if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop new is leaf");
			return;
		}
		ConstituentsBranch old_root = (ConstituentsBranch)_old_root;
		ConstituentsBranch new_root = (ConstituentsBranch)_new_root;
		if(old_root.isColapsed()){
			if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop old not expanded");
			return;
		}
		if(new_root.isColapsed()){
			if(DEBUG) System.err.println("ConstituentsModel:translate_expansion populating "+new_root);
			new_root.populate();
			tree.expandPath(new TreePath(new_root.getPath()));
			if(DEBUG) System.err.println("ConstituentsModel:translate_expansion populated "+new_root);
		}
		for(int k=0; k < old_root.getChildren().length; k++) {
			ConstituentsNode cb =	old_root.getChildren()[k];
			if(cb instanceof ConstituentsAddressNode) {
				if(!(new_root instanceof ConstituentsAddressNode)){
					if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop new root not address parent");
					continue;
				}
				ConstituentsAddressNode o_can = (ConstituentsAddressNode) cb;
				ConstituentsAddressNode n_can = (ConstituentsAddressNode) new_root;
				long neighborhoodID = o_can.getNeighborhoodData().neighborhoodID;
				ConstituentsNode nc = n_can.getChildByID(neighborhoodID);
				if((nc!=null)&&(cb!=null))translate_expansion(tree, cb,nc);
			}
			if(cb instanceof ConstituentsIDNode) {
				if(!(new_root instanceof ConstituentsAddressNode)){
					if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop new root not constituent parent (address)");
					continue;
				}
				ConstituentsIDNode o_can = (ConstituentsIDNode) cb;
				ConstituentsAddressNode n_can = (ConstituentsAddressNode) new_root;
				long constituentID = o_can.get_constituentID();
				ConstituentsNode nc = n_can.getChildByConstituentID(constituentID);
				if((nc!=null)&&(cb!=null))translate_expansion(tree, cb,nc);
			}
			if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop round for "+k);
		}
		if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop");		
	}
	public void init(long _organizationID, 
			long _constituentID, String _global_constituentID, D_Organization org) throws P2PDDSQLException {
		if(DEBUG) System.err.println("ConstituentsModel:init start org="+_organizationID+
				" myconstID="+_constituentID+" gID="+_global_constituentID+" org="+org);
		setRoot(null);
		setFieldIDs(null);
		setSubDivisions(null);
		organizationID = -1;
		hasNeighborhoods = false;
		crt_org = null;
		this.setConstituentIDMyself(-1, null);
		ArrayList<ArrayList<Object>> fields_neighborhood, subneighborhoods;
		organizationID = _organizationID;
		if (organizationID <= 0) return;
		crt_org = org;
		if (crt_org == null)
			try {
				crt_org = D_Organization.getOrgByLID_NoKeep(organizationID, true);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		if(DEBUG)System.out.println("ConstituentsModel: final org = crt_org "+crt_org);
		setConstituentIDMyself(_constituentID, _global_constituentID);
		ArrayList<Object> subdivision_fields = crt_org.getDefaultRootSubdivisions();
		String subdivs = Util.getString(subdivision_fields.get(0));
		setSubDivisions(subdivs);
		if(DEBUG)System.out.println("ConstituentsModel: init: subdiv "+this.getSubDivisions()+" vs "+subdivs);
		setFieldIDs((long[]) subdivision_fields.get(1));
		hasNeighborhoods = (getFieldIDs().length > 0);
		if(DEBUG)System.out.println("ConstituentsModel: init: hadN= "+hasNeighborhoods);
		setRoot(new ConstituentsAddressNode(this,null,null,"",null,null,getFieldIDs(),0,-2,null));
		if ( getFieldIDs().length > 0 ) {
			subneighborhoods = D_Constituent.getRootConstValues(getFieldIDs()[0], organizationID);
		}
		else subneighborhoods = new ArrayList<ArrayList<Object>>();
		ArrayList<Long> neighborhood_branch_obj = D_Neighborhood.getNeighborhoodRootsLIDs(organizationID);
	    if(DEBUG) System.err.println("ConstituentsModel: init: neigh_obj = #" + neighborhood_branch_obj.size());
		int n = 0;
		if(DEBUG) System.err.println("ConstituentsModel: Sub-neighborhoods (branches) Records= "+subneighborhoods.size());
		for (int i = 0; i < subneighborhoods.size(); i ++) {
		    String count, fieldID;
		    Object obj;
			String value=Util.sval(subneighborhoods.get(i).get(0),null);
		    if(DEBUG) System.err.println("ConstituentsModel: init: subn obj i=" + i+" n="+n+" val="+value);
		    if (value != null) {
		    	for (; n < neighborhood_branch_obj.size(); n ++) {
		    		D_Neighborhood dn = D_Neighborhood.getNeighByLID(neighborhood_branch_obj.get(n), true, false);
		    		String n_name = dn.getName(); 
		    		int cmp = value.compareToIgnoreCase(n_name);
				    if(DEBUG) System.err.println("ConstituentsModel: init: subn_obj i=" + i+" n="+n+" nam="+n_name);
		    		if (cmp > 0) break; 
		    		if (cmp < 0) {
		    			long nID = neighborhood_branch_obj.get(n); 
		    			Constituents_NeighborhoodData nd=new Constituents_NeighborhoodData(nID, -1, organizationID);
		    			nd.neighborhoodID = nID;
		    			getRoot().addChild(new ConstituentsAddressNode(this, getRoot(), nd, new Constituents_AddressAncestors[0]),0);
		    		}
		    	}
		    }
		    obj = subneighborhoods.get(i).get(1);
		    if(obj!=null) fieldID = obj.toString(); else fieldID = "-1";
		    obj = subneighborhoods.get(i).get(2);
		    if(obj!=null) count = obj.toString(); else count = null;
		    if(DEBUG) System.err.println("ConstituentsModel: init: "+i+" Got: v="+value+" c="+count+" fID="+fieldID);
		    Constituents_LocationData data=new Constituents_LocationData();
		    data.value = value;
		    data.fieldID = Long.parseLong(fieldID);
		    data.inhabitants = Integer.parseInt(count);
		    data.tip = (String)subneighborhoods.get(i).get(3);
		    data.partNeigh = Util.ival(subneighborhoods.get(i).get(4),0);
		    data.fieldID_above = Util.lval(subneighborhoods.get(i).get(5),-1);
		    data.setFieldID_default_next(Util.lval(subneighborhoods.get(i).get(6),-1));
		    data.neighborhood = Util.ival(subneighborhoods.get(i).get(4),0);
		    getRoot().addChild(
		    		new ConstituentsAddressNode(this, getRoot(),
		    				data,
		    				"", null,
		    				new Constituents_AddressAncestors[0],
		    				getFieldIDs(),0, -1,null), 
		    			0);
		}
	    for (; n < neighborhood_branch_obj.size(); n ++) {
    		D_Neighborhood dn = D_Neighborhood.getNeighByLID(neighborhood_branch_obj.get(n), true, false);
    		String n_name = dn.getName(); 
	    	long nID = neighborhood_branch_obj.get(n); 
		    if(DEBUG) System.err.println("ConstituentsModel: init: nei_b_obj n="+n+"/nID="+nID+" nam="+n_name);
	    	Constituents_NeighborhoodData nd=new Constituents_NeighborhoodData(nID, -1, organizationID);
	    	nd.neighborhoodID = nID;
	    	getRoot().addChild(new ConstituentsAddressNode(this, getRoot(), nd, new Constituents_AddressAncestors[0]), 0);
	    }
	    if (getFieldIDs().length == 0) getRoot().populateIDs();
	    else {
	    	if(DD.CONSTITUENTS_ORPHANS_SHOWN_IN_ROOT) populateOrphans();
	    }
	    runCensus(); 
	}
	public void populateOrphans() {
		ArrayList<Long> orphans = D_Constituent.getOrphans(this.organizationID);
		if (DEBUG) System.err.print("ConstituentsModel: populateOrphans Records="+orphans.size());
		for (int i = 0; i < orphans.size(); i ++ ) {
			D_Constituent c = D_Constituent.getConstByLID(orphans.get(i), true, false);
			if (c == null) { 
				if(_DEBUG) Util.printCallPath("Wrong size!");
				return;
			}
			String name, forename, slogan, email;
			if (DEBUG) System.err.println("ConstituentsModel: populateOrphans got const="+c.getNameOrMy());
			name = Util.getString(c.getSurname(),__("Unknown Yet"));
			forename = c.getForename(); 
			boolean external = c.isExternal(); 
			long submitterID = c.getSubmitterLID(); 
			ConstituentData data = new ConstituentData(c);
			data.setC_GID(c.getGID()); 
			data.setC_LID(c.getLID()); 
			data.setGivenName(forename);
			data.setSurname(name);
			data.inserted_by_me=((getConstituentIDMyself() == submitterID)&&(getConstituentIDMyself()>=0));
			data.external = external;
			data.blocked = c.blocked;
			data.broadcast = c.broadcasted;
			slogan = c.getSlogan(); 
			email = c.getEmail(); 
			data.setSlogan(slogan);
			data.email = email;
			String submitter_ID = c.getSubmitterLIDstr(); 
			data.submitter_ID = submitter_ID;
			if (DEBUG) System.err.print("ConstituentsModel: populateOrphans child");
			getRoot().populateChild(new ConstituentsIDNode(this, getRoot(), data,"",null, getRoot().getNextAncestors()),0);
		}
		int new_size = orphans.size()+getRoot().getNchildren();
		getRoot().setNChildren(new_size);
		if (DEBUG) System.err.print("ConstituentsModel: populateOrphans fire ->#"+new_size);
	}
	public Object	getChild(Object parent, int index) {	
		if (! (parent instanceof ConstituentsBranch)) return -1;
		ConstituentsBranch cbParent = (ConstituentsBranch)parent;
		return cbParent.getChild(index);
	}
	public int	getChildCount(Object parent) {	
		if (! (parent instanceof ConstituentsBranch)) return -1;
		ConstituentsBranch cbParent = (ConstituentsBranch)parent;
		return cbParent.getChildCount();
	}
	public int	getIndexOfChild(Object parent, Object child) {	
		if (! (parent instanceof ConstituentsBranch)) return -1;
		ConstituentsBranch cbParent = (ConstituentsBranch)parent;
		return cbParent.getIndexOfChild(child);
	}
    public ConstituentsAddressNode	getRoot() {
    	if (root == null) {
    		setFieldIDs(new long[0]);
    		root = new ConstituentsAddressNode(this,null,null,"",null,null,this.getFieldIDs(),0,-2,null);
    	}
    	return root;
    }
    public boolean	isLeaf(Object node) {
    	if (node instanceof ConstituentsPropertyNode) return true;
    	if (node instanceof ConstituentsBranch)
    	    return (((ConstituentsBranch)node).getNchildren()==0);
    	return false;
    }
    public void	valueForPathChanged(TreePath path, Object newValue) {
    	boolean HARD_SAVE = false;
    	if(DEBUG) System.err.println("ConstitentsModel:valueForPathChanged: "+path+" = "+newValue);
    	if(newValue == null) {
    		return;
    	}
    	Calendar creation_date = Util.CalendargetInstance();
    	String s_creation_date = Encoder.getGeneralizedTime(creation_date);
    	Object node=path.getLastPathComponent();
    	if(!(node instanceof ConstituentsAddressNode)) return;
    	ConstituentsAddressNode neigh = (ConstituentsAddressNode)node;
    	Constituents_NeighborhoodData nd = (Constituents_NeighborhoodData)newValue; 
    	if (DEBUG) System.err.println("ConstitentsModel:valueForPathChanged: old edited neigh_ID="+nd.neighborhoodID);
     	try {
    		Constituents_NeighborhoodData ndo = neigh.getNeighborhoodData(); 
        	if (DEBUG) System.err.println("ConstitentsModel:valueForPathChanged: old neigh_ID="+ndo.neighborhoodID);
    		if (ndo.global_nID == null) HARD_SAVE = true;
    		else{
    			Application_GUI.warning(__("Cannot change data of signed neighborhood! Create a new one"),__("Cannot change!"));
    			return;
    		}
    		ndo.name = nd.name;
    		neigh.getLocation().value = nd.name;
    		ndo.name_lang = nd.name_lang;
    		ndo.name_division = nd.name_division;
    		ndo.name_division_lang = nd.name_division_lang;
    		ndo.names_subdivisions = nd.names_subdivisions;
    		ndo.name_subdivisions_lang = nd.name_subdivisions_lang;
     		if (HARD_SAVE) { 
       			String submitter_ID = Util.getStringID(this.getConstituentIDMyself());
       			String submitter_GID = (this.getConstituentGIDMyself());
    			String org_local_ID = Util.getStringID(this.organizationID);
    			String arrival_time = Util.getGeneralizedTime();
    			SK sk = Util.getStoredSK(this.getConstituentGIDMyself());
    			String orgGID = D_Organization.getGIDbyLID(this.organizationID);
     			D_Neighborhood d_neighborhood = 
     					D_Neighborhood.getNeighByLID(ndo.neighborhoodID, true, true);
     			if (DEBUG) System.out.println("Modifying neigh: "+d_neighborhood);
     			if (d_neighborhood.getGID() != null) {
     				d_neighborhood.releaseReference();
    				Application_GUI.warning(__("Signed Neighborhood!"), __("Not editable"));
    				return;
     			}
     			if ( ! Util.equalStrings_null_or_not(d_neighborhood.getSubmitterLIDstr(),submitter_ID) ) {
     				Application_GUI.warning(__("Submitter differs. Changed to current!"), __("Submitter conflict"));
     				d_neighborhood.setSubmitterLIDstr(submitter_ID);
     				d_neighborhood.setSubmitter_GID(submitter_GID);
     			}
     			d_neighborhood.setNames_subdivisions(D_Neighborhood.splitSubDivisions(nd.names_subdivisions));
     			d_neighborhood.setName(nd.name);
     			d_neighborhood.setName_lang(nd.name_lang.toString());
     			d_neighborhood.setName_division(nd.name_division);
      			d_neighborhood.setCreationDateStr(arrival_time);
      			d_neighborhood.setOrgIDs(orgGID, this.organizationID);
      			d_neighborhood.setGID(d_neighborhood.make_ID());
     			d_neighborhood.sign(sk);
     			d_neighborhood.storeRequest();
     			d_neighborhood.releaseReference();
     			ndo.global_nID = d_neighborhood.getGID();
     			ndo.signature = d_neighborhood.getSignature();
     			ndo.neighborhoodID = d_neighborhood.getLID_force();
     			ndo.submitterID = d_neighborhood.getSubmitterLID();
    		}
    		int idx = neigh.getParent().getIndexOfChild(neigh);
    		this.fireTreeNodesChanged(new TreeModelEvent(this,path.getParentPath(),new int[]{idx},new Object[]{neigh}));
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	if(DEBUG) System.err.println("ConstitentsModel:valueForPathChanged: exit");
    }
	public boolean setCurrentConstituent(long _constituentID, ConstituentsTree tree) {
    	if (DEBUG) System.err.println("ConstitentsModel:setCurrentConstituent: set "+_constituentID);
		try {
			if ( ! this.setConstituentIDMyself(_constituentID, null) ) {
		    	if (_DEBUG) System.err.println("ConstitentsModel:setCurrentConstituent: myself failed ");
				return false;
			}
			Identity.setCurrentConstituentForOrg(_constituentID, this.organizationID);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		tree.preparePopup();
    	if (DEBUG) System.err.println("ConstitentsModel:setCurrentConstituent: Done");
    	return true;
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		if (this.automatic_refresh) {
			try {
				this.doRefreshAll();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;
		}
		if(refreshListener != null) this.refreshListener.enableRefresh();
		else  System.err.println("ConstituentsModel:update: No refresh listener!");
		if(DEBUG)System.err.println("ConstituentsModel:update: Need to update Constituents!");
	}
	/**
	 * This removed model from census thread, and it will no longer fire events
	 * It will also request it to eventually stop.
	 */
	public synchronized void stopCensusRequest(){
		if(DEBUG) System.err.println("ConstituentsModel:stopCensusRequest: start");
	    if(census!=null){
	    	census.giveUp();
			if(DEBUG) System.err.println("ConstituentsModel:stopCensusRequest: gaveUP");
	    }else{
			if(DEBUG) System.err.println("ConstituentsModel:stopCensusRequest: no census running");	    	
	    }
		if(DEBUG) System.err.println("ConstituentsModel:stopCensusRequest: stop");
	}
	public synchronized void startCensus(){
		if(DEBUG) System.err.println("ConstituentsModel:startCensus: start");
	    census = new ConstituentsCensus(this, this, getRoot());
	    census_value = 0;
	    census.start();
		if(DEBUG) System.err.println("ConstituentsModel:startCensus: done");
	}
	public void updateCensus(Object source,
			Object[] path2parent, int idx) {
		this.fireTreeNodesChanged(new TreeModelEvent(this, path2parent, 
				new int[]{idx},new Object[]{source}));
	}
	public void updateCensus(Object source, Object[] path) {
		try{
			fireTreeNodesChanged(new TreeModelEvent(source, path));
		}catch(Exception e){
			System.err.println("ConstituentsCensus: announce: "+e.getLocalizedMessage());
			System.err.println("ConstituentsCensus: announce: path="+Util.concat(path, " ; "));
		}
	}
	public void updateCensusStructure(Object source, Object[] path) {
		fireTreeStructureChanged(new TreeModelEvent(source,path));
	}
	public void updateCensusInserted(
			Object source, Object[] path2parent,
			int[] idx, Object[] children) {
		fireTreeNodesInserted(new TreeModelEvent(this, path2parent,
				idx, children));
	}
	/**
	 * Clean up if still relevant
	 * @param constituentsCensus
	 * @param result
	 */
	public synchronized void censusDone(ConstituentsCensus constituentsCensus, long result) {
		if(DEBUG) System.err.println("ConstituentsModel:censusDone: Got="+result);
		if(census!=constituentsCensus) {
			if(DEBUG) System.err.println("ConstituentsModel:censusDone: quit as irrelevant");
			return;
		}
		census = null;
		census_value = result;
		if(DEBUG) System.err.println("ConstituentsModel:censusDone: Done!");
	}
	public synchronized void runCensus(TreePath path) {
		if(DEBUG) System.err.println("ConstituentsModel:runCensus: Got="+path);
		if(census!=null){
			if(DEBUG) System.err.println("ConstituentsModel:runCensus: interrupting");
			return;
		}
		Object expanded = path.getLastPathComponent();
		if(!(expanded instanceof ConstituentsAddressNode)) {
			if(DEBUG) System.err.println("ConstituentsModel:runCensus: not address");
			return;
		}
		ConstituentsAddressNode can = ((ConstituentsAddressNode)expanded);
		if(can==null) return;
		census = new ConstituentsCensus(this, this, (ConstituentsAddressNode)can.getParent());
		census.start();
		if(DEBUG) System.err.println("ConstituentsModel:runCensus: done");
	}
	public D_Organization getOrganization() {
		return crt_org;
	}
	public String getOrgGID() {
		if(crt_org==null)
			try {
				crt_org = D_Organization.getOrgByLID_NoKeep(this.getOrganizationID(), true);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		return crt_org.getGID();
	}
	public void enableRefresh() {
		this.refreshListener.enableRefresh();
	}
	public String getSubDivisions() {
		return subdivisions;
	}
	public void setSubDivisions(String subdivisions) {
		this.subdivisions = subdivisions;
	}
	public long[] getFieldIDs() {
		return fieldIDs;
	}
	public void setFieldIDs(long[] fieldIDs) {
		this.fieldIDs = fieldIDs;
	}
	public void setRoot(ConstituentsAddressNode root) {
		this.root = root;
	}
}
