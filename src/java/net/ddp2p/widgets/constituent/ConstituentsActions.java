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
import java.awt.event.*;
import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.config.Language;
import net.ddp2p.common.data.DDTranslation;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_FieldValue;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.data.IconObject;
import net.ddp2p.common.hds.ClientSync;
import net.ddp2p.common.hds.Server;
import net.ddp2p.common.population.ConstituentData;
import net.ddp2p.common.population.ConstituentsAddressNode;
import net.ddp2p.common.population.ConstituentsBranch;
import net.ddp2p.common.population.ConstituentsIDNode;
import net.ddp2p.common.population.Constituents_LocationData;
import net.ddp2p.common.population.Constituents_NeighborhoodData;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.java.email.EmailManager;
import net.ddp2p.widgets.app.MainFrame;
import net.ddp2p.widgets.app.Util_GUI;
import net.ddp2p.widgets.components.DebateDecideAction;
import static net.ddp2p.common.util.Util.__;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
@SuppressWarnings("serial")
class AddEmptyNeighborhoodAction extends DebateDecideAction {
    ConstituentsTree tree;ImageIcon icon;
    ConstituentsModel model;
	public AddEmptyNeighborhoodAction(ConstituentsTree tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
		super(text, icon, desc, whatis, mnemonic);
		this.tree = tree; this.icon = icon;;
    	model = (ConstituentsModel)tree.getModel();
	}
	public void actionPerformed(ActionEvent arg0) {
		ConstituentsAddressNode parent=model.getRoot();
    	TreePath tp=tree.getLeadSelectionPath();
    	if(model.getConstituentIDMyself()<=0) {
    		if(0 != Application_GUI.ask(__("No constituent identified!\nStill want to create it?"),
    				__("Constituent absent"),
    				JOptionPane.OK_CANCEL_OPTION))
    			return;
    	}
    	if(tp!=null) {
    		Object source = tp.getLastPathComponent();
    		if(!(source instanceof ConstituentsAddressNode)) return;
    		parent = (ConstituentsAddressNode)source;
    		if((parent.getNeighborhoodData() == null) || (parent.getNeighborhoodData().neighborhoodID<0)) return;
    		ConstituentsAddressNode neig = (ConstituentsAddressNode)parent;
    		if((neig!=null)&&(neig.getNeighborhoodData()!=null)){
    			if(neig.getNeighborhoodData().global_nID==null){
    				Application_GUI.warning(__("Cannot expand unsigned/temporary neighborhood.")+"\n "+
    						__("First edit it with a final value!")+"\n "+__("You can edit it by double clicking on it!"),
    						__("Temporary Neighborhood"));
    				return;
    			}
    		}else{ 
				Application_GUI.warning(__("Cannot expand unsigned/temporary neighborhood.incomplete path value!")+"\n "+
						__("Contact maintainers!"),
						__("Temporary Neighborhood"));
				return;
    		}
    	}
    	parent.addEmptyNeighborhood();
    	if (parent.getNeighborhoods() == 1) tree.expandPath(tp);
	}
}
@SuppressWarnings("serial")
class ConstituentsDelAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
	ConstituentsTree tree;ImageIcon icon;
    ConstituentsModel model;
    public ConstituentsDelAction(ConstituentsTree tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    	model = (ConstituentsModel)tree.getModel();
    }
    boolean remove(TreePath tp){
     	TreePath tpp = tp.getParentPath();
    	Object source = tp.getLastPathComponent();
    	Object o_parent = tpp.getLastPathComponent();
    	if(!(o_parent instanceof ConstituentsAddressNode)) return false;
    	ConstituentsAddressNode parent = (ConstituentsAddressNode)o_parent;
    	int old_index = parent.getIndexOfChild(source);
    	if(old_index == -1) {
    		JOptionPane.showMessageDialog((Component)tree,(new Object[] {__("Item not found!"),tp}),__("Delete item"),JOptionPane.ERROR_MESSAGE);
   			return false;
   		}
   		parent.del(source);
   		model.fireTreeNodesRemoved(new TreeModelEvent(tree,tpp,new int[]{old_index},new Object[]{source}));
   		if(parent.getNchildren()==0){
   			parent.setNchildren(1);
   			tree.collapsePath(tpp);
   		}
   		tp = tpp;
   		for(;;){
    		if(tp.getPath().length<2) break;
    		tpp = tp.getParentPath();
    		Object crt=tp.getLastPathComponent();
    		int new_index = ((ConstituentsAddressNode)tpp.getLastPathComponent()).getIndexOfChild(crt);
    		model.fireTreeNodesChanged(new TreeModelEvent(tree,tpp, new int[]{new_index},
    				new Object[]{crt}));
    		tp = tpp;
    	}
   		return true;
    }
    public void actionPerformed(ActionEvent e) {
	   	if(DEBUG) System.err.println("ConstituentsAction:ConstituentsDelAction: delete const");
	   	TreePath tp=tree.getLeadSelectionPath();
    	tree.addSelectionPath(tp);
    	TreePath sp[] = tree.getSelectionPaths();
    	if((tp==null) || (tp.getPathCount() <= 1)) {
    		JOptionPane.showMessageDialog((Component)tree,(new Object[] {__("Cannot remove this item!")}),__("Delete item"),JOptionPane.ERROR_MESSAGE);
    		return;
    	}
    	Object options[]=new Object[]{__("Yes"),__("Cancel")};
    	int result = JOptionPane.showOptionDialog((Component)tree,(new Object[]{__("Are you sure you want to remove selected items!")}),__("Delete items"),JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
    	if(result == 0){
    		for (int i=0; i<sp.length; i++) {
    			if(DEBUG) System.err.println("ConstituentsAction:ConstituentsDelAction:Removing "+sp[i]);
    	   		remove(sp[i]);
    		}
    	}
    	String GID = D_Constituent.getGIDFromLID(model.getConstituentIDMyself());
    	if (DEBUG) System.err.println("ConstituentsAction:ConstituentsDelAction:GID ="+GID);
    	if (GID == null) {
    		if (DEBUG) System.err.println("ConstituentsAction:ConstituentsDelAction:GID = null (I removed myself)");
    		model.setCurrentConstituent(-1, tree);
    	}
    }
}
class ConstituentsWitnessAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	ConstituentsTree tree;ImageIcon icon;
    public ConstituentsWitnessAction(ConstituentsTree tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	ConstituentsModel model = (ConstituentsModel)tree.getModel();
    	TreePath tp = tree.getLeadSelectionPath();
    	Object target = tp.getLastPathComponent();
    	if(! (target instanceof ConstituentsIDNode)) return;
    	ConstituentsIDNode can = (ConstituentsIDNode) target;
    	ConstituentsWitness dialog = new ConstituentsWitness(tree, tp, 0);
    	if (!dialog.accepted) return;
    	String witness_category = Util_GUI.getJFieldText(dialog.witness_category);
    	String witness_category_trustworthiness = Util_GUI.getJFieldText(dialog.witness_category_trustworthiness);
    	String gcd = can.getConstituent().getC_GID();
    	int new_index=can.getParent().getIndexOfChild(can);
    	try {
    		ArrayList<ArrayList<Object>> sel;
    		String sql="select "+net.ddp2p.common.table.witness.witness_ID+" from "+net.ddp2p.common.table.witness.TNAME+" where " +
    		net.ddp2p.common.table.witness.source_ID+"=? and "+net.ddp2p.common.table.witness.target_ID+"=?;";
    		sel = model.db.select(sql, 
    				new String[]{model.getConstituentIDMyself()+"",
    				can.getConstituent().getC_LID()+""});
    		if(sel.size()>0){
    			model.db.delete(net.ddp2p.common.table.witness.TNAME,
    					new String[]{net.ddp2p.common.table.witness.source_ID,net.ddp2p.common.table.witness.target_ID}, 
    					new String[]{model.getConstituentIDMyself()+"",
    					can.getConstituent().getC_LID()+""});
    		}
    		Calendar creation_date = Util.CalendargetInstance();
    		long organizationID = model.getOrganizationID();
    		String organizationGID = model.getOrgGID();
     		SK sk = DD.getConstituentSK(model.getConstituentIDMyself());
    		D_Witness wbw = new D_Witness();
    		wbw.global_organization_ID(organizationGID);
    		wbw.witnessed_constituentID = can.getConstituent().getC_LID();
    		wbw.witnessed_global_constituentID = can.getConstituent().getC_GID();
    		wbw.witnessing_global_constituentID = model.getConstituentGIDMyself();
    		wbw.witnessing_constituentID = model.getConstituentIDMyself();
    		wbw.witness_eligibility_category = witness_category;
    		wbw.sense_y_n = D_Witness.sense_eligibility.get(witness_category).intValue();
    		wbw.witness_trustworthiness_category = witness_category_trustworthiness;
    		if(!Util.emptyString(witness_category_trustworthiness))
    			wbw.sense_y_trustworthiness = D_Witness.sense_trustworthiness.get(witness_category_trustworthiness).intValue();
    		else wbw.sense_y_trustworthiness = D_Witness.UNKNOWN;
    		wbw.creation_date = creation_date;
    		wbw.arrival_date = creation_date;
    		wbw.global_witness_ID = wbw.make_ID();
        	if(DEBUG) System.out.println("CostituentsAction: addConst: signing="+wbw);
    		wbw.sign(sk);
    		long withID = wbw.storeVerified();
       		if(DEBUG|| DD.TEST_SIGNATURES) {
       			D_Witness w_test = new D_Witness(withID);
       			if(!w_test.verifySignature()){
       				if(_DEBUG) System.out.println("CostituentsAction: addConst: failed signing="+wbw+"\nvs\n"+w_test);    			
       			}
       		}
    		can.updateWitness();
        	model.fireTreeNodesChanged(new TreeModelEvent(tree,tp.getParentPath(), 
        			new int[]{new_index},
        			new Object[]{can}));
     	}catch(Exception ev) {
    		ev.printStackTrace();
    		return;
    	}
    }
 }
/**
 * This class creates and handles the dialog for adding a neighbor and witnessing him
 * @author silaghi
 *
 */
@SuppressWarnings("serial")
class ConstituentsSetMyselfAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	ConstituentsTree tree;ImageIcon icon;
    public ConstituentsSetMyselfAction(ConstituentsTree tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	ConstituentsModel model = (ConstituentsModel)tree.getModel();
    	TreePath tp = tree.getLeadSelectionPath();
    	Object target=model.getRoot();
    	if (tp != null) {
    		target = tp.getLastPathComponent();
    		if(! (target instanceof ConstituentsIDNode)) return;
    	}else{
    		return;
    	}
    	ConstituentsIDNode can = (ConstituentsIDNode) target;
     	model.setCurrentConstituent(can.getConstituent().getC_LID(), tree);
    	D_Constituent lc = D_Constituent.getConstByLID(can.get_constituentID(), true, false);
    	if (lc != null) MainFrame.status.setMeConstituent(lc);
    }
}
/**
 * This class creates and handles the dialog for adding a neighbor and witnessing him
 * @author silaghi
 *
 */
@SuppressWarnings("serial")
class ConstituentsCustomAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static final int MYSELF = 1;
	public static final int REFRESH = 0;
	public static final int WITNESSED = 2;
	public static final int SLOGAN = 3;
	public static final int REFRESH_AUTO = 4;
	public static final int BROADCAST = 5;
	public static final int BLOCK = 6;
	public static final int ADVERTISE = 7;
	public static final int TOUCH = 8;
	public static final int REFRESH_NEED = 9;
	public static final int MOVE = 10;
	public static final int ZAPP = 11;
	public static final int IDENTIFY = 12;
	ConstituentsTree tree;ImageIcon icon; int action;
    public ConstituentsCustomAction(ConstituentsTree tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic,
			     int _action) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon; action = _action;
    }
    public void actionPerformed(ActionEvent e) {
    	ConstituentsModel model = (ConstituentsModel)tree.getModel();
    	TreePath tpath = tree.getLeadSelectionPath();
    	Object root=model.getRoot();
    	Object target = null;
    	if(tpath!=null) target = tpath.getLastPathComponent();
    	System.out.println("ConstAction:"+action);
    	try {
    		if(action == IDENTIFY) {
       			if (tpath != null) {
    	    		if(target instanceof ConstituentsIDNode) {
    	    	    	ConstituentsIDNode can = (ConstituentsIDNode) target;
    	    	    	EmailManager.verify(can.get_constituentID());
    	    		}
     	    	}
    		}
    		if(action == REFRESH_NEED) {
    			model.enableRefresh();
    		}
       		if(action == ADVERTISE) {
    	    	if (tpath != null) {
    	    		if(target instanceof ConstituentsIDNode) {
    	    	    	ConstituentsIDNode can = (ConstituentsIDNode) target;
    	    	    	ConstituentsIDNode.advertise(can, tree.getModel().getOrgGID());
    	    		}
    	       		if(target instanceof ConstituentsAddressNode) {
    	       			ConstituentsAddressNode can = (ConstituentsAddressNode) target;
    	    	    	can.advertise(tree.getModel().getOrganization().getGIDH());
    	    		}
     	    	}
    			return;
    		}
       		if(action == MOVE) {
       			if (tpath != null) {
       				if(target instanceof ConstituentsIDNode) {
       					Application_GUI.warning(__("Select a neighborhood"), __("Select a neighborhood"));
       				}
       				if(target instanceof ConstituentsAddressNode) {
       					ConstituentsAddressNode can = (ConstituentsAddressNode) target;
       					String nGID = can.getNeighborhoodData().global_nID;
       					D_Constituent dc = model.getConstituentMyself();
       					long cID = model.getConstituentIDMyself();
       					if (dc != null) {
       						dc = D_Constituent.getConstByConst_Keep(dc);
       						if (dc != null) {
	       						dc.setNeighborhood_LID(Util.getStringID(can.getNeighborhoodData().neighborhoodID));
	       						dc.setNeighborhoodGID(nGID);
	       						if (dc.getSK() == null) {
	           						SK sk = model.getConstituentSKMyself();
	           						dc.setSK(sk);
	       						}
	       						dc.setCreationDate();
	       						dc.sign();
	       						dc.storeRequest();
	       						dc.releaseReference();
       						}
       						ConstituentsIDNode cin = model.expandConstituentID(tree, ""+cID, true);
       						if (cin != null) {
       							if(DEBUG) System.err.println("ConstituentsAction:SLOGAN:fire changed="+cin);
       							TreePath tp = new TreePath(cin.getPath());
       							((ConstituentsModel)tree.getModel()).fireTreeNodesChanged(new TreeModelEvent(tree, tp.getParentPath(), 
       									new int[]{cin.getParent().getIndexOfChild(cin)},
       									new Object[]{cin}));
       						}
       						DD.touchClient();
       					}else Application_GUI.warning(__("You do not have a constituent self!"), __("No constituent self"));
       				}
       			}       		
       		}
       		if (action == BLOCK) {
       			if (tpath != null) {
    	    		if (target instanceof ConstituentsIDNode) {
    	    	    	ConstituentsIDNode can = (ConstituentsIDNode) target;
    	    	    	can.toggle_block();
    	    		}
    	       		if (target instanceof ConstituentsAddressNode) {
    	       			ConstituentsAddressNode can = (ConstituentsAddressNode) target;
    	    	    	can.block();
    	    		}
     	    	}
        	}
       		if(action == ZAPP) {
       			if (tpath != null) {
    	    		if(target instanceof ConstituentsIDNode) {
    	    	    	ConstituentsIDNode can = (ConstituentsIDNode) target;
    	    	    	D_Constituent constit = can.zapp();
    	    			MainFrame.status.droppingConst(constit);
    	    		}
    	       		if(target instanceof ConstituentsAddressNode) {
    	       			ConstituentsAddressNode can = (ConstituentsAddressNode) target;
    	    	    	can.zapp();
    	    		}
     	    	}
        	}
            if (action == BROADCAST) {
    	    	if (tpath != null) {
    	    		if (target instanceof ConstituentsIDNode) {
    	    	    	ConstituentsIDNode can = (ConstituentsIDNode) target;
    	    	    	can.toggle_broadcast();
    	    		}
    	       		if (target instanceof ConstituentsAddressNode) {
    	       			ConstituentsAddressNode can = (ConstituentsAddressNode) target;
    	    	    	can.broadcast();
    	    		}
     	    	}            	
            }
            if(action == REFRESH) {
    			if(DEBUG) System.out.println("ConstituentActions:ConstituentRefreshActions: REFRESH");
    			Object oldRoot = model.getRoot();
    			model.init(model.getOrganizationID(), model.getConstituentIDMyself(), model.getConstituentGIDMyself(), model.getOrganization());
    			model.fireTreeStructureChanged(new TreeModelEvent(tree,new Object[]{model.getRoot()}));
    			model.refresh(new JTree[]{tree}, oldRoot);
    			model.runCensus();
    		}
            if(action == TOUCH) {
    			if(DEBUG) System.out.println("ConstituentActions:ConstituentRefreshActions: TOUCH");
    			DD.touchClient();
    		}
    		if(action == REFRESH_AUTO) {
    			if(_DEBUG) System.out.println("ConstituentActions:ConstituentRefreshActions: REFRESH_AUTO old="+model.automatic_refresh);
    			DD.DEFAULT_AUTO_CONSTITUENTS_REFRESH = model.automatic_refresh = !model.automatic_refresh;
    			if(_DEBUG) System.out.println("ConstituentActions:ConstituentRefreshActions: REFRESH_AUTO: "+ model.automatic_refresh);
    		}
    		if(action == MYSELF) {
    			if(DEBUG) System.out.println("ConstituentActions:ConstituentRefreshActions: MYSELF");
    			long cID = model.getConstituentIDMyself();
    			if(cID>0)
    				model.expandConstituentID(tree, ""+cID, true);
    			else Application_GUI.warning(__("You do not have a constituent self!"), __("No constituent self"));
    		}
    		if(action == SLOGAN) {
    			if(DEBUG) System.out.println("ConstituentActions:ConstituentRefreshActions: SLOGAN");
    			long cID = model.getConstituentIDMyself();
				D_Constituent dc = D_Constituent.getConstByLID(cID, true, true);
    			if (dc != null) {
    				String peer_Slogan = dc.getSlogan();
    				String val=JOptionPane.showInputDialog(tree, __("Change My Constituent Slogan.\nPreviously: ")+Util.trimmed(peer_Slogan,DD.MAX_DISPLAYED_CONSTITUENT_SLOGAN), __("My Constituent Slogan"), JOptionPane.QUESTION_MESSAGE);
    				if((val!=null)&&(!"".equals(val))){
    					dc.setSlogan(val);
    					if (dc.getSK() == null) {
    						SK sk = model.getConstituentSKMyself();
    						dc.setSK(sk);
    					}
    					dc.setCreationDate();
    					dc.sign();
    					ConstituentsIDNode cin = model.expandConstituentID(tree, ""+cID, true);
    					if(cin!=null) {
    						cin.getConstituent().setSlogan(val);
    						if(DEBUG) System.err.println("ConstituentsAction:SLOGAN:fire changed="+cin);
    						TreePath tp = new TreePath(cin.getPath());
    						((ConstituentsModel)tree.getModel()).fireTreeNodesChanged(new TreeModelEvent(tree, tp.getParentPath(), 
    		        			new int[]{cin.getParent().getIndexOfChild(cin)},
    		        			new Object[]{cin}));
    					}
    					DD.touchClient();
    				}
    				dc.storeRequest();
    				dc.releaseReference();
     			}else Application_GUI.warning(__("You do not have a constituent self!"), __("No constituent self"));
    		}
    		if(action == WITNESSED) {
    			if(DEBUG) System.out.println("ConstituentActions:ConstituentRefreshActions: WITNESSED");
    			long cID = model.getConstituentIDMyself();
    			if(cID>0) {
    				String sql =
    					"SELECT "+net.ddp2p.common.table.witness.target_ID+","+net.ddp2p.common.table.witness.neighborhood_ID+
    					" FROM "+net.ddp2p.common.table.witness.TNAME+
    					" WHERE "+net.ddp2p.common.table.witness.source_ID+"=?;";
    				ArrayList<ArrayList<Object>> w = Application.getDB().select(sql, new String[]{""+cID}, DEBUG);
    				for(ArrayList<Object> W : w) {
       					String tID = Util.getString(W.get(0));
    					if ((tID != null)&&(Integer.parseInt(tID) > 0)) model.expandConstituentID(tree, tID, false);
       					String nID = Util.getString(W.get(1));
    					if ((nID != null)&&(Integer.parseInt(nID) > 0)) model.expandNeighborhoodID(tree, nID);
    				}
    				if(w.size()>0) model.runCensus();
    			}
    			else Application_GUI.warning(__("You do not have a constituent self!"), __("No constituent self"));
    		}
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    }
}
/**
 * This class creates and handles the dialog for adding a neighbor and witnessing him
 * @author silaghi
 *
 */
@SuppressWarnings("serial")
class ConstituentsAddAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	ConstituentsTree tree;ImageIcon icon;
    public ConstituentsAddAction(ConstituentsTree tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    }
    static int field_index(long[]fieldID, long field) {
    	for(int k=0;k<fieldID.length;k++){
    		if(fieldID[k]==field) return k;
    	}
    	return -1;
    }
    public void actionPerformed(ActionEvent e) {
    	ConstituentsModel model = (ConstituentsModel)tree.getModel();
    	TreePath tp = tree.getLeadSelectionPath();
    	Object target=model.getRoot();
    	if (tp != null) {
    		target = tp.getLastPathComponent();
    		if(! (target instanceof ConstituentsAddressNode)) return;
    		ConstituentsAddressNode neig = (ConstituentsAddressNode)target;
    		if((neig!=null)&&(neig.getNeighborhoodData()!=null)){
    			if(neig.getNeighborhoodData().global_nID==null){
    				Application_GUI.warning(__("Cannot expand unsigned/temporary neighborhood.")+"\n "+
    						__("First edit it with a final value!")+"\n "+__("You can edit it by double clicking on it!"),
    						__("Temporary Neighborhood"));
    				return;
    			}
    		}else{ 
				Application_GUI.warning(__("Cannot expand unsigned/temporary neighborhood.incomplete path value!")+"\n "+
						__("Contact maintainers!"),
						__("Temporary Neighborhood"));
				return;
    		}
    	}else{
    		tp = new TreePath(new Object[]{model.getRoot()});
    	}
    	ConstituentsAdd dialogCA = new ConstituentsAdd(tree, tp);
    	if (!dialogCA.accepted) return;
    	ConstituentAddData dialog = dialogCA.getConstituentAddData();
    	long id = storeNewConstituentData(tree, dialog, false, true);
    	if (id>=0){
    		if(dialog.sign)
    			D_Constituent.readSignSave(id, model.getConstituentIDMyself());
    		else
    			D_Constituent.readSignSave(id, 0);
    	}
    	model.expandConstituentID(tree, ""+id, true);
    }
    /**
     * This function will store the constituent data found in the dialog
     * for some other constituent
     * @param tree
     * @param dialog
     * @param inserting_field_values
     * @param inserting_neighborhoods
     * @return 
     */
    static long storeNewConstituentData(ConstituentsTree tree, ConstituentAddData dialog,
    		boolean inserting_field_values, boolean inserting_neighborhoods) {
    	if(DEBUG) System.out.println("ConstituentsActions:ConstituentsAddAction:storeNewConstituentData");
      	String gcd;   		
      	D_Constituent con = null;
    	boolean inserted_neigh = false;
    	ConstituentsModel model = (ConstituentsModel)tree.getModel();
    	TreePath tp=dialog.tp;
    	ConstituentsBranch child=null;
    	Object target = tp.getLastPathComponent();
    	Object[] otp = tp.getPath();
    	ConstituentsAddressNode can = (ConstituentsAddressNode) target;
    	long constituentID=-1;
    	IconObject imageicon=null;
    	boolean noChild = true; 
    	String witness_category=dialog.witness_category;
    	String witness_category_trustworthiness=dialog.witness_category_trustworthiness;
    	byte[] byteArray=null;
    	long field_default_next=0;
    	long field_above=-1; 
    	long _parent_nID=-1;
    	String parent_nGID = null;
    	String subdivisions=model.getSubDivisions();
    	if(dialog.pictureImage!=null) byteArray = Util_GUI.getImage(dialog.pictureImage);
    	if("".equals(dialog.gnEditor)&&"".equals(dialog.snEditor)) return constituentID;
       	net.ddp2p.widgets.identities.IdentityBranch ib = ((net.ddp2p.widgets.identities.IdentityBranch)Identity.current_id_branch);
    	if(ib == null) return constituentID;
    	Calendar creation_date = Util.CalendargetInstance();
    	String now = Encoder.getGeneralizedTime(creation_date);
      	try{
      		SK sk = DD.getConstituentSK(model.getConstituentIDMyself());    		
    		long organizationID = model.getOrganizationID();
    		String organizationGID = model.getOrgGID();
    		net.ddp2p.common.data.D_Constituent wbc = D_Constituent.getEmpty();
    		wbc.setWeight(dialog.weight+"");
    		wbc.setEmail(dialog.emailEditor);
    		wbc.setExternal(true);
    		wbc.setForename(dialog.gnEditor);
    		wbc.setSurname(dialog.snEditor);
    		wbc.setOrganization(organizationGID, organizationID);
    		wbc.setPicture(byteArray);
    		wbc.setSlogan(net.ddp2p.common.table.constituent.INIT_EXTERNAL_SLOGAN);
    		wbc.setCreationDate(creation_date);
    		wbc.setHash_alg(net.ddp2p.common.table.constituent.CURRENT_HASH_CONSTITUENT_ALG);
    		wbc.languages = dialog.getLanguages();
    		wbc.setNeighborhood(null);
    		if(dialog.sign)wbc.setSubmitter_ID(Util.getStringID(model.getConstituentIDMyself()));
       		if(dialog.sign)wbc.setSubmitterGID(model.getConstituentGIDMyself());
       		wbc.setArrivalDate();
       		wbc.sign();
       		String GID =  wbc.getGID();
       		D_Constituent wbc_reg = D_Constituent.getConstByGID_or_GIDH(GID, GID, true, true, true, null, organizationID);
       		wbc_reg.loadRemote(null, null, wbc_reg, null, false);
       		constituentID = wbc_reg.storeRequest_getID();
       		wbc_reg.releaseReference();
    		for(int k=1;k<otp.length; k++) {
    			ConstituentsAddressNode cand = (ConstituentsAddressNode)otp[k];
    			String value_language = cand.getValueLanguage();
    			if(inserting_field_values) {
    				model.db.insert(net.ddp2p.common.table.field_value.TNAME, 
    						new String[]{net.ddp2p.common.table.field_value.constituent_ID,net.ddp2p.common.table.field_value.field_extra_ID,
    						net.ddp2p.common.table.field_value.value,
    						net.ddp2p.common.table.field_value.value_lang,
    						net.ddp2p.common.table.field_value.fieldID_above,net.ddp2p.common.table.field_value.field_default_next,net.ddp2p.common.table.field_value.neighborhood_ID}, 
    						new String[]{constituentID+"",cand.getLocation().fieldID+"",
    						cand.getLocation().value,
    						value_language,
    						(field_above<0)?null:(field_above+""),
    								cand.getLocation().getFieldID_default_next()+"",net.ddp2p.common.table.field_extra.NEIGHBORHOOD_ID_NA});
    			}
    			if(inserting_neighborhoods){
    				if(cand.getNeighborhoodData().neighborhoodID<0){
        				String n_key =  ""+model.getConstituentIDMyself()+":"+cand.getLocation().getLabel()+"="+cand.getLocation().value;
        				String[] _subdivisions = D_Neighborhood._getChildSubDivisions(subdivisions, cand.getLocation().getLabel());
    					subdivisions = D_Neighborhood._getChildSubDivision(subdivisions, cand.getLocation().getLabel());
        				net.ddp2p.common.data.D_Neighborhood wbn = D_Neighborhood.getEmpty(); 
        				wbn.setBoundary(null);
        				wbn.setCreationDate(creation_date);
        				wbn.setDescription(null);
        				wbn.setName(cand.getLocation().value);
        				wbn.setName_division(cand.getLocation().getLabel());
        				wbn.setName_lang(DDTranslation.authorship_lang.lang);
        				wbn.setNames_subdivisions(_subdivisions);
        				wbn.parent = null;
        				wbn.setParentLIDstr(Util.getStringID(_parent_nID)); 
        				wbn.setParent_GID(D_Neighborhood.getGIDFromLID(wbn.getParentLIDstr()));
        				wbn.setPicture(null);
        				wbn.setSignature(null);
        				wbn.submitter = null;
        				long subm = model.getConstituentIDMyself();
        				wbn.setSubmitterLIDstr(Util.getStringID(subm)); 
        				wbn.setSubmitter_GID(D_Constituent.getGIDFromLID(model.getConstituentIDMyself()));
        				wbn.setOrgIDs(model.getOrgGID(), model.getOrganizationID());
        				wbn.setGID(wbn.make_ID()); 
        				wbn.sign(sk);
        				D_Neighborhood dn = D_Neighborhood.getNeighByGID(wbn.getGID(), true, true, true, null, wbn.getOrgLID());
        				dn.loadRemote(wbn, null, null, null);
        				dn.storeRequest();
        				dn.releaseReference();
        				wbn = dn;
        				long __neighborhoodID = dn.getLID_force();
        				cand.getNeighborhoodData().neighborhoodID = __neighborhoodID;
        				String _neighborhoodID = Util.getStringID(__neighborhoodID);
    					cand.getNeighborhoodData().global_nID = wbn.getGID();
    					cand.getNeighborhoodData().name = cand.getLocation().value;
    					cand.getNeighborhoodData().name_lang=new Language(DDTranslation.org_language.lang,DDTranslation.org_language.flavor);
    					cand.getNeighborhoodData().name_division=cand.getLocation().getLabel();
    					cand.getNeighborhoodData().name_division_lang=new Language(DDTranslation.org_language.lang,DDTranslation.org_language.flavor);
    					cand.getNeighborhoodData().names_subdivisions=subdivisions;
    					cand.getNeighborhoodData().name_subdivisions_lang=new Language(DDTranslation.org_language.lang,DDTranslation.org_language.flavor);
    					cand.getNeighborhoodData().submitterID=model.getConstituentIDMyself();
    					cand.getNeighborhoodData().organizationID=model.getOrganizationID();
    				} else subdivisions = cand.getNeighborhoodData().names_subdivisions;
    				_parent_nID = cand.getNeighborhoodData().neighborhoodID;
    				parent_nGID = cand.getNeighborhoodData().global_nID;
    			}
    			field_above = cand.getLocation().fieldID;
    			cand.getLocation().inhabitants++;
    		}
        	subdivisions= dialog._subdivisions;
    		if(DEBUG)System.err.println("Size objects: "+dialog.valueEditor.length+" vs:"+dialog.valueEditor.length);
    		for(int k=dialog.valueEditor.length-1;k>=0;k--) {
    			String value=dialog.valueEditor[k];
    			String value_language = dialog.getFieldValueLanguage(k);
    			if(DEBUG)System.err.println(k+": "+value+" neigh="+dialog.partNeigh[k]);
    			if ((value==null) || (value.equals(""))){
    				if(DEBUG)System.err.println("Skipping: "+k);
    				continue;
    			}
    			field_default_next = 0;
    			for(int j = k-1; j>=0; j--) {
    				if ((dialog.valueEditor[j]==null)||(dialog.valueEditor[j].equals(""))) continue;
    				field_default_next=dialog.fieldID[j];
    				break;
    			}
    			long field_valuesID=-1;
    			Constituents_NeighborhoodData n_data=null;
    			boolean is_neighborhood=net.ddp2p.common.table.field_extra.isANeighborhood(dialog.partNeigh[k]);
    			if(inserting_field_values||(!is_neighborhood)) { 
    				field_valuesID = model.db.insert(net.ddp2p.common.table.field_value.TNAME,
    						new String[]{
    						net.ddp2p.common.table.field_value.constituent_ID,
    						net.ddp2p.common.table.field_value.field_extra_ID,
    						net.ddp2p.common.table.field_value.value,
    						net.ddp2p.common.table.field_value.value_lang,
    						net.ddp2p.common.table.field_value.fieldID_above,
    						net.ddp2p.common.table.field_value.field_default_next,
    						net.ddp2p.common.table.field_value.neighborhood_ID},
    						new String[]{constituentID+"",
    						dialog.fieldID[k]+"",
    						value, 
    						value_language, 
    						((field_above<0)||(!is_neighborhood))?null:(field_above+""), 
    						is_neighborhood?(field_default_next+""):null,
    						net.ddp2p.common.table.field_extra.NEIGHBORHOOD_ID_NA},
    						DEBUG);
    			}
    			if(inserting_neighborhoods&&is_neighborhood) {
    				n_data = new Constituents_NeighborhoodData();
    				String n_key =  ""+model.getConstituentIDMyself()+":"+dialog.label[k]+"="+value;
       				String[] _subdivisions = D_Neighborhood._getChildSubDivisions(subdivisions, dialog.label[k]);
    				subdivisions = D_Neighborhood._getChildSubDivision(subdivisions, dialog.label[k]);
    				net.ddp2p.common.data.D_Neighborhood wbn = D_Neighborhood.getEmpty();
    				wbn.setBoundary(null);
    				wbn.setCreationDate(creation_date);
    				wbn.setDescription(null);
    				wbn.setName(value);
    				wbn.setName_division(dialog.label[k]);
    				wbn.setName_lang(DDTranslation.authorship_lang.lang);
    				wbn.setNames_subdivisions(_subdivisions);
    				wbn.parent = null;
    				wbn.setParentLIDstr(Util.getStringID(_parent_nID)); 
    				wbn.setParent_GID(D_Neighborhood.getGIDFromLID(wbn.getParentLIDstr()));
    				wbn.setPicture(null);
    				wbn.setSignature(null);
    				wbn.submitter = null;
    				long subm = model.getConstituentIDMyself();
    				wbn.setSubmitterLIDstr(Util.getStringID(subm)); 
    				wbn.setSubmitter_GID(D_Constituent.getGIDFromLID(model.getConstituentIDMyself()));
    				wbn.setOrgIDs(model.getOrgGID(), model.getOrganizationID());
    				wbn.setGID(wbn.make_ID());
    				wbn.sign(sk);    				
    				D_Neighborhood dn = D_Neighborhood.getNeighByGID(wbn.getGID(), true, true, true, null, wbn.getOrgLID());
    				dn.loadRemote(wbn, null, null, null);
    				dn.storeRequest();
    				dn.releaseReference();
    				wbn = dn;
    				long __neighborhoodID = dn.getLID_force();
    				n_data.neighborhoodID = __neighborhoodID;
					n_data.global_nID = wbn.getGID();
    				if(DEBUG) System.out.println("ConstituentAddAction:storeNewConstituentData: obtained nID="+n_data.neighborhoodID+" for:"+ wbn);
    				n_data.name = value;
    				n_data.name_lang=new Language(DDTranslation.authorship_lang.lang,DDTranslation.authorship_lang.flavor);
    				n_data.name_division=dialog.label[k];
    				n_data.name_division_lang=new Language(dialog.label_lang[k],DDTranslation.org_language.flavor);
    				n_data.names_subdivisions=subdivisions;
    				n_data.name_subdivisions_lang=new Language(DDTranslation.org_language.lang,DDTranslation.org_language.flavor);
					n_data.submitterID=model.getConstituentIDMyself();
					n_data.organizationID=model.getOrganizationID();
    				_parent_nID = n_data.neighborhoodID;
    				parent_nGID = n_data.global_nID;
     			} 
     			if (is_neighborhood&&noChild){
   	   	   			if((child=can.getChildByID(n_data.neighborhoodID))!=null)
   					{
   						tree.collapsePath(tp.pathByAddingChild(child));
   					}
    				if(noChild&&(child==null)) { 
     					int level=field_index(can.getFieldIDs(), dialog.fieldID[k]);
    					Constituents_LocationData data = new Constituents_LocationData();
    					data.value=value;
    					data.inhabitants = 1;
    					data.organizationID = model.getOrganizationID();
    					data.setLabel(dialog.label[k]);
    					data.fieldID = dialog.fieldID[k];
    					data.field_valuesID = field_valuesID;
    					data.setFieldID_default_next(field_index(can.getFieldIDs(), field_default_next));
    					data.fieldID_above = field_above;
    					data.tip = dialog.tip[k];
    					data.list_of_values = dialog.lov[k];
    					can.addChild(child=new ConstituentsAddressNode(model,can,data,null,null,can.getNextAncestors(),can.getFieldIDs(),
    							level, -1, n_data), 0);
    					n_data = null;
    					inserted_neigh = true;
    				}
    				noChild=false;
     				field_above = dialog.fieldID[k];
    			}
    		}
    		con = D_Constituent.getConstByLID(constituentID, true, true);
    		con.setNeighborhoodIDs(parent_nGID, _parent_nID);
    		if(dialog.sign)
    			gcd=net.ddp2p.common.data.D_Constituent.readSignSave(constituentID, model.getConstituentIDMyself());
    		else
    			gcd=net.ddp2p.common.data.D_Constituent.readSignSave(constituentID, 0);
    		con.storeRequest();
    		con.releaseReference();
    		D_Witness wbw = new D_Witness();
    		wbw.global_organization_ID(organizationGID);
    		wbw.witnessed_constituentID = constituentID;
    		wbw.witnessed_global_constituentID = gcd;
    		wbw.witnessing_global_constituentID = model.getConstituentGIDMyself();
    		wbw.witnessing_constituentID = model.getConstituentIDMyself();
    		wbw.witness_eligibility_category = witness_category;
    		wbw.sense_y_n = ConstituentsAdd.sense_eligibility.get(witness_category).intValue();
    		wbw.witness_trustworthiness_category = witness_category_trustworthiness;
    		if(!Util.emptyString(witness_category_trustworthiness))
    			wbw.sense_y_trustworthiness = ConstituentsAdd.sense_trustworthiness.get(witness_category_trustworthiness).intValue();
			else wbw.sense_y_trustworthiness = D_Witness.UNKNOWN;
    		wbw.creation_date = creation_date;
    		wbw.arrival_date = creation_date;
    		wbw.global_witness_ID = wbw.make_ID();
        	if(DEBUG) System.out.println("CostituentsAction: addConst: signing="+wbw);
        	wbw.sign(sk);
        	long witnID = wbw.storeVerified();
       		if(DEBUG|| DD.TEST_SIGNATURES) {
        		D_Witness test_witn = new D_Witness(witnID);
        		if(!test_witn.verifySignature()){
        			if(_DEBUG) System.out.println("CostituentsAction: addConst: failed signing="+wbw+"\nvs\n"+test_witn);    			
        		}
        	}
    	}catch(Exception ev){
    		ev.printStackTrace();
    		return constituentID;
    	}
    	if(DEBUG) System.out.println("NoChildren="+noChild+" cID="+constituentID);
    	if(noChild && (constituentID!=-1)) {
    		ConstituentData data = new ConstituentData(con);
    		data.setC_GID(gcd);
    		data.setC_LID(constituentID);
    		data.setGivenName(dialog.gnEditor);
    		data.setSurname(dialog.snEditor);
    		data.witness_against = 0;
    		data.witness_for = 1;
    		data.witnessed_by_me = 2;
    		data.setIcon(imageicon);
    		data.inserted_by_me = true;
    		data.external = true;
    		data.blocked = false;
    		data.broadcast = true;
    		data.setSlogan(net.ddp2p.common.table.constituent.INIT_EXTERNAL_SLOGAN);
        	data.email = dialog.emailEditor;
        	if(dialog.sign)data.submitter_ID = ""+model.getConstituentIDMyself();
        	if(DEBUG) System.out.println("NoChildren="+child);
    		can.addChild(child=new ConstituentsIDNode(model,can,data,null,null,can.getNextAncestors()), 0);
    		inserted_neigh = true;
        	if(DEBUG) System.out.println("Added="+child);
    		noChild = false;
    	}
    	int new_index=0;
    	if(inserted_neigh) model.fireTreeNodesInserted(new TreeModelEvent(tree,tp, new int[]{new_index},new Object[]{child}));
    	if(can.getNchildren()==1) {
    		if(child != null)
    			tree.expandPath(tp.pathByAddingChild(child));
    		else{
    			model.fireTreeStructureChanged(new TreeModelEvent(tree,tp));
    			tree.expandPath(tp);
    		}
    	}
    	for(;;){
    		if(tp.getPath().length<2) break;
    		TreePath tpp = tp.getParentPath();
    		Object crt=tp.getLastPathComponent();
    		new_index = ((ConstituentsAddressNode)tpp.getLastPathComponent()).getIndexOfChild(crt);
    		model.fireTreeNodesChanged(new TreeModelEvent(tree,tpp, new int[]{new_index},
    				new Object[]{crt}));
    		tp = tpp;
    	}
    	return constituentID;
    }
}
/**
 * This class creates and handles the dialog for adding a neighbor and witnessing him
 * @author silaghi
 *
 */
@SuppressWarnings("serial")
class ConstituentsAddMyselfAction extends DebateDecideAction {
    private static final boolean _DEBUG = true;
    private static final boolean DEBUG = false;
	ConstituentsTree tree;ImageIcon icon;
    public ConstituentsAddMyselfAction(ConstituentsTree tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    }
    static int field_index(long[]fieldID, long field) {
    	for (int k = 0; k < fieldID.length; k ++){
    		if (fieldID[k] == field) return k;
    	}
    	return -1;
    }
    public void actionPerformed(ActionEvent e) {
    	try {
    		_actionPerformed(e);
    	} catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
    class ConstituentAddDataCtx {
		public ConstituentAddData dialog;
		public ConstituentsModel model;
		public ConstituentsTree tree;
		protected long id;
    }
    public void _actionPerformed(ActionEvent e) {
    	ConstituentsModel model = (ConstituentsModel)tree.getModel();
    	TreePath tp = tree.getLeadSelectionPath();
    	Object target=model.getRoot();
    	if (tp != null) {
    		target = tp.getLastPathComponent();
    		if (! (target instanceof ConstituentsAddressNode)) return;
    		ConstituentsAddressNode neig = (ConstituentsAddressNode)target;
    		if ((neig != null) && (neig.getNeighborhoodData() != null)) {
    			if (neig.getNeighborhoodData().global_nID==null){
    				Application_GUI.warning(__("Cannot expand unsigned/temporary neighborhood.")+"\n "+
    						__("First edit it with a final value!")+"\n "+__("You can edit it by double clicking on it!"),
    						__("Temporary Neighborhood"));
    				return;
    			}
    		}else{ 
				Application_GUI.warning(__("Cannot expand unsigned/temporary neighborhood.incomplete path value!")+"\n "+
						__("Contact maintainers!"),
						__("Temporary Neighborhood"));
				return;
    		}
    	}else{
    		tp = new TreePath(new Object[]{model.getRoot()});
    	}
    	ConstituentsAdd dialogCA = new ConstituentsAdd(tree, tp, true);
     	if ( ! dialogCA.accepted ) return;
    	ConstituentAddData dialog = dialogCA.getConstituentAddData();
    	ConstituentAddDataCtx _ctx = new ConstituentAddDataCtx();
    	_ctx.dialog = dialog;
    	_ctx.model = model;
    	_ctx.tree = tree;
    	new net.ddp2p.common.util.DDP2P_ServiceThread("New Constituent Save Thread", true, _ctx) {
    		public void _run () {
    	    	long id;
    	    	ConstituentAddDataCtx __ctx = (ConstituentAddDataCtx) ctx;
    	    	ConstituentAddData dialog = __ctx.dialog;
       	    	ConstituentsModel model = __ctx.model;
      	    	ConstituentsTree tree = __ctx.tree;
      	    	try {
    	    		id = storeMyConstituentData(tree, dialog, false, true);
    	    		if (id >= 0) {
    	    			D_Constituent.readSignSave(id, model.getConstituentIDMyself());
    	    			MainFrame.status.setMeConstituent(D_Constituent.getConstByLID(id, true, false));
    	    		}else
    	    			MainFrame.status.setMeConstituent(null);
    	    		__ctx.id = id;
    	    		SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable("New Constituent Save Runnable", false, false, __ctx) {
    	        		public void _run () {
    	           	    	ConstituentAddDataCtx __ctx = (ConstituentAddDataCtx) ctx;
    	           	    	ConstituentsModel model = __ctx.model;
    	           	    	ConstituentsTree tree = __ctx.tree;
    	          	    	long id = __ctx.id;
    	        			model.expandConstituentID(tree, ""+id, true);
    	        		}
    	    		});
    	    	} catch (P2PDDSQLException e2) {
    	    		e2.printStackTrace();
    	    	}
    		}
    	}.start();
    }
    /**
     * This function will store the constituent data found in the dialog as myself
     * called from ConstituentAddMyselfAction
     * @param tree
     * @param dialog
     * @param inserting_field_values
     * @param inserting_neighborhoods
     * @return 
     * @throws P2PDDSQLException 
     */
    static long storeMyConstituentData (ConstituentsTree tree, ConstituentAddData dialog,
    		boolean inserting_field_values, boolean inserting_neighborhoods) throws P2PDDSQLException {
    	boolean inserted_neigh = false;
    	ConstituentsModel model = (ConstituentsModel)tree.getModel();
    	TreePath tp=dialog.tp;
    	ConstituentsBranch child=null;
    	Object target = tp.getLastPathComponent();
    	Object[] otp = tp.getPath();
    	ConstituentsAddressNode can = (ConstituentsAddressNode) target;
    	long constituentID=-1;
    	IconObject imageicon=null;
    	boolean noChild = true; 
    	String witness_category = dialog.witness_category;
    	byte[] byteArray=null;
    	long field_default_next=0;
    	long field_above=-1; 
    	long parent_nID=-1;
    	String subdivisions = model.getSubDivisions(); 
		D_Constituent cn = null;
		if (DEBUG) System.out.println("ConstituemtActions:AddMyself:storeMyConst: subdivs="+subdivisions);
     	if ("".equals(dialog.gnEditor)&&"".equals(dialog.snEditor)) return constituentID;
    	net.ddp2p.widgets.identities.IdentityBranch ib = ((net.ddp2p.widgets.identities.IdentityBranch)Identity.current_id_branch);
    	if(ib == null) return constituentID;
    	Calendar creation_date = Util.CalendargetInstance();
    	String now = Encoder.getGeneralizedTime(creation_date);
    	Cipher keys;
    	SK sk = ib.getKeys();
    	keys = ib.getCipher();
    	if (keys == null) {
        	keys = Cipher.mGetStoreCipher(
        			dialog.ciphersuit,
        			dialog.hash_alg,
        			dialog.ciphersize, "CST:"+dialog.snEditor+"_"+dialog.gnEditor,
        			"Constituent",
        			dialog.emailEditor, now);
    		sk = keys.getSK();
    	}
		String gcd = Util.getKeyedIDPK(keys);
		String sID = Util.getKeyedIDSK(keys);
		String gcdhash = D_Constituent.getGIDHashFromGID_NonExternalOnly(gcd);
		String type = Util.getKeyedIDType(keys);
		if(dialog.pictureImage!=null) byteArray = Util_GUI.getImage(dialog.pictureImage);
		long organizationID = model.getOrganizationID();
		String organizationGID = model.getOrgGID();
		net.ddp2p.common.data.D_Constituent wbc = D_Constituent.getEmpty();
		wbc.setWeight(dialog.weight+"");
		wbc._set_GID(gcd);
		wbc.setGIDH(gcdhash);
		wbc.setEmail(dialog.emailEditor);
		wbc.setExternal(false);
		wbc.setForename(dialog.gnEditor);
		wbc.setSurname(dialog.snEditor);
		wbc.setOrganization(organizationGID, organizationID);
		wbc.setPicture(byteArray);
		wbc.setSlogan(net.ddp2p.common.table.constituent.INIT_SLOGAN);
		wbc.setCreationDate (creation_date);
		wbc.setHash_alg(net.ddp2p.common.table.constituent.CURRENT_HASH_CONSTITUENT_ALG);
		wbc.languages = dialog.getLanguages();
		wbc.setNeighborhood(null);
		wbc.sign();
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(wbc.getGID(), wbc.getGIDH(), true, true, true, null, organizationID);
		if (c == null) {
			if (_DEBUG) System.out.println("ConstituentsModel:storeMyConstData: Got null const for: "+wbc.getGID());
			return -1;
		}
		c.loadRemote(null, null, wbc, null, false);
		constituentID = c.storeRequest_getID();
		c.releaseReference();
		if (DEBUG) System.out.println("ConstituentsModel:storeMyConstData: c="+c);
		model.setCurrentConstituent(constituentID, tree);
		D_Witness wbw = new D_Witness();
		wbw.global_organization_ID(organizationGID);
		wbw.witnessed_constituentID = constituentID;
		wbw.witnessed_global_constituentID = gcd;
		wbw.witnessing_constituentID = constituentID;
		wbw.witnessing_global_constituentID = gcd;
		wbw.witness_eligibility_category = witness_category;
		wbw.sense_y_n = ConstituentsAdd.sense_eligibility.get(witness_category).intValue();
		wbw.witness_trustworthiness_category = dialog.witness_category_trustworthiness;
		if(!Util.emptyString(dialog.witness_category_trustworthiness))
			wbw.sense_y_trustworthiness = ConstituentsAdd.sense_trustworthiness.get(dialog.witness_category_trustworthiness).intValue();
		else wbw.sense_y_trustworthiness = D_Witness.FAVORABLE;
		wbw.creation_date = creation_date;
		wbw.arrival_date = creation_date;
 		wbw.global_witness_ID = wbw.make_ID();
    	if(DEBUG) System.out.println("CostituentsAction: addmyselfConst: signing="+wbw);
		wbw.sign(sk);
		long witnID = wbw.storeVerified();
		if(DEBUG|| DD.TEST_SIGNATURES) {
			D_Witness test_witn = new D_Witness(witnID);
			if(!test_witn.verifySignature()){
				if(_DEBUG) System.out.println("CostituentsAction: addConst: failed signing="+wbw+"\nvs\n"+test_witn);    			
			}
		}
		if(DEBUG)System.out.println("ConstituentActions:AddMyself:storeMyConst: subdivs2="+subdivisions); //must have
		try{
   			if(DEBUG)System.out.println("ConstituentActions:AddMyself:storeMyConst: ancestors="+otp.length);
   	   		for(int k=1;k<otp.length; k++) {
    			if(DEBUG)System.out.println("ConstituentActions:AddMyself:storeMyConst: k="+k+"/"+otp.length);
    			ConstituentsAddressNode cand = (ConstituentsAddressNode)otp[k];
    			String language = cand.getValueLanguage();
    			if(inserting_field_values) {
    				model.db.insert(net.ddp2p.common.table.field_value.TNAME, 
    						new String[]{net.ddp2p.common.table.field_value.constituent_ID,
    						net.ddp2p.common.table.field_value.field_extra_ID,
    						net.ddp2p.common.table.field_value.value,
    						net.ddp2p.common.table.field_value.value_lang,
    						net.ddp2p.common.table.field_value.fieldID_above,
    						net.ddp2p.common.table.field_value.field_default_next,
    						net.ddp2p.common.table.field_value.neighborhood_ID}, 
    						new String[]{Util.getStringID(constituentID),
    						cand.getLocation().fieldID+"",
    						cand.getLocation().value,
    						language,
    						(field_above<0)?null:(field_above+""),
    						cand.getLocation().getFieldID_default_next()+"",
    						net.ddp2p.common.table.field_extra.NEIGHBORHOOD_ID_NA}, DEBUG);
    			}
    			if(inserting_neighborhoods){
    				if(DEBUG)System.out.println("ConstituemtActions:AddMyself:storeMyConst: subdivs3="+subdivisions);
    				if(cand.getNeighborhoodData().neighborhoodID<0){
    					if(DEBUG)System.out.println("ConstituemtActions:AddMyself:storeMyConst: subdivs_1="+subdivisions);
        				String[] _subdivisions = D_Neighborhood._getChildSubDivisions(subdivisions, cand.getLocation().getLabel());
    					if(DEBUG)System.out.println("ConstituemtActions:AddMyself:storeMyConst: subdivs_2="+subdivisions);
        				subdivisions = D_Neighborhood._getChildSubDivision(subdivisions, cand.getLocation().getLabel());
           			    if(DEBUG)System.out.println("ConstituemtActions:AddMyself:storeMyConst: subdivs="+subdivisions+" vs _subd="+Util.concat(_subdivisions, ":"));
        				net.ddp2p.common.data.D_Neighborhood wbn = net.ddp2p.common.data.D_Neighborhood.getEmpty();
        				wbn.setBoundary(null);
        				wbn.setCreationDate(creation_date);
        				wbn.setDescription(null);
        				wbn.setName(cand.getLocation().value);
        				wbn.setName_division(cand.getLocation().getLabel());
        				wbn.setName_lang(DDTranslation.authorship_lang.lang);
        				wbn.setNames_subdivisions(_subdivisions);
        				wbn.parent = null;
        				wbn.setParentLIDstr(Util.getStringID(parent_nID)); 
        				wbn.setParent_GID(D_Neighborhood.getGIDFromLID(wbn.getParentLIDstr()));
        				wbn.setPicture(null);
        				wbn.setSignature(null);
        				wbn.submitter = null;
        				long subm = model.getConstituentIDMyself();
        				wbn.setSubmitterLIDstr(Util.getStringID(subm));
        				wbn.setSubmitter_GID(D_Constituent.getGIDFromLID(model.getConstituentIDMyself()));
        				wbn.setOrgIDs(model.getOrgGID(), model.getOrganizationID());
        				wbn.setGID(wbn.make_ID());
        				wbn.sign(sk);
        				D_Neighborhood dn = D_Neighborhood.getNeighByGID(wbn.getGID(), true, true, true, null, wbn.getOrgLID());
        				dn.loadRemote(wbn, null, null, null);
        				dn.storeRequest();
        				dn.releaseReference();
        				wbn = dn;
        				long __neighborhoodID = dn.getLID_force();
        				cand.getNeighborhoodData().neighborhoodID = __neighborhoodID;
    					cand.getNeighborhoodData().global_nID = wbn.getGID();
    					cand.getNeighborhoodData().name=cand.getLocation().value;
    					cand.getNeighborhoodData().name_lang=new Language(DDTranslation.org_language.lang,DDTranslation.org_language.flavor);
    					cand.getNeighborhoodData().name_division=cand.getLocation().getLabel();
    					cand.getNeighborhoodData().name_division_lang=new Language(DDTranslation.org_language.lang,DDTranslation.org_language.flavor);
    					cand.getNeighborhoodData().names_subdivisions=subdivisions;
    					cand.getNeighborhoodData().name_subdivisions_lang=new Language(DDTranslation.org_language.lang,DDTranslation.org_language.flavor);
    					cand.getNeighborhoodData().submitterID=model.getConstituentIDMyself();
    					cand.getNeighborhoodData().organizationID=model.getOrganizationID();
    				} else{
    					subdivisions = cand.getNeighborhoodData().names_subdivisions;
        				if(DEBUG)System.out.println("ConstituentActions:AddMyself:storeMyConst: subdivs4="+subdivisions);
    				}
    				parent_nID=cand.getNeighborhoodData().neighborhoodID;
    			}
    			field_above = cand.getLocation().fieldID;
    			cand.getLocation().inhabitants++;
    		}
        	subdivisions= dialog._subdivisions;
			if(DEBUG)System.out.println("ConstituentActions:AddMyself:storeMyConst: descendants subdivs6="+subdivisions); //must here
    		if(DEBUG)System.err.println("ConstituentAddMyself:Size objects: "+dialog.valueEditor.length+" vs:"+dialog.valueEditor.length);
    		for (int k = dialog.valueEditor.length - 1; k >= 0; k --) {
    			if (DEBUG) System.out.println("ConstituentActions:AddMyself:descendants: k==" + k);
    			String value = dialog.valueEditor[k];
    			String language = dialog.getFieldValueLanguage(k);
				if (DEBUG) System.err.println("ConstituentActions:AddMyself:loop_editor "+k+": "+value+" neigh="+dialog.partNeigh[k]);
    			if ((value == null) || (value.equals(""))) {
    				if(DEBUG)System.err.println("Skipping: "+k);
    				continue;
    			}
    			field_default_next = 0;
    			for (int j = k - 1; j >= 0; j --) {
    				if ((dialog.valueEditor[j] == null) || (dialog.valueEditor[j].equals(""))) continue;
    				field_default_next = dialog.fieldID[j];
    				break;
    			}
    			long field_valuesID=-1; 
    			Constituents_NeighborhoodData n_data=null;
    			boolean is_neighborhood = net.ddp2p.common.table.field_extra.isANeighborhood(dialog.partNeigh[k]);
    			if (DEBUG) System.out.println("ConstituentsAction: AddMyself: storeConstData: is_neigh="+is_neighborhood+" for "+dialog.partNeigh[k]);
    			if (inserting_field_values || (! is_neighborhood)) {
    	    		cn = D_Constituent.getConstByLID(constituentID, true, true);
    	    		D_FieldValue fv = new D_FieldValue();
    	    		fv.field_extra_ID = dialog.fieldID[k];
    	    		fv.field_extra_GID = model.getOrganization().getFieldExtraGID(fv.field_extra_ID);
    	    		fv.value = value;
    	    		fv.value_lang = language;
    	    		fv.field_ID_above = ((field_above<0)||(!is_neighborhood))?-1:(field_above);
    	    		fv.field_GID_above = model.getOrganization().getFieldExtraGID(fv.field_ID_above);
    	    		fv.field_ID_default_next = is_neighborhood?(field_default_next):0;
    	    		fv.field_GID_default_next = model.getOrganization().getFieldExtraGID(fv.field_ID_default_next);
    	    		fv.neighborhood_ID = Util.lval(net.ddp2p.common.table.field_extra.NEIGHBORHOOD_ID_NA, 0);
    	    		fv.global_neighborhood_ID = D_Neighborhood.getGIDFromLID(fv.neighborhood_ID);
    	    		cn.setFieldValue(fv);
    	    		cn.sign();
    	    		cn.storeRequest();
    	    		cn.releaseReference();
        			if (DEBUG) System.out.println("ConstituentsAction: AddMyself: storeConstData: inserting field value");
    			}
    			if (inserting_neighborhoods && is_neighborhood) {
    				n_data = new Constituents_NeighborhoodData();
    				String n_key =  ""+model.getConstituentIDMyself()+":"+dialog.label[k]+"="+value;
       				String[] _subdivisions = D_Neighborhood._getChildSubDivisions(subdivisions, dialog.label[k]);
       				subdivisions = D_Neighborhood._getChildSubDivision(subdivisions, dialog.label[k]);
       			    if(DEBUG)System.out.println("ConstituentActions:AddMyself:storeMyConst: subdiv="+subdivisions+" vs _subd="+Util.concat(_subdivisions, ":"));
    				net.ddp2p.common.data.D_Neighborhood wbn = net.ddp2p.common.data.D_Neighborhood.getEmpty();
    				wbn.setBoundary(null);
    				wbn.setCreationDate(creation_date);
    				wbn.setDescription(null);
    				wbn.setName(value);
    				wbn.setName_division(dialog.label[k]);
    				wbn.setName_lang(DDTranslation.authorship_lang.lang);
    				wbn.setNames_subdivisions(_subdivisions);
    				wbn.parent = null;
    				wbn.setParentLIDstr(Util.getStringID(parent_nID)); 
    				wbn.setParent_GID(D_Neighborhood.getGIDFromLID(wbn.getParentLIDstr()));
    				wbn.setPicture(null);
    				wbn.setSignature(null);
    				wbn.submitter = null;
    				long subm = model.getConstituentIDMyself();
    				wbn.setSubmitterLIDstr(Util.getStringID(subm)); 
    				wbn.setSubmitter_GID(D_Constituent.getGIDFromLID(model.getConstituentIDMyself()));
    				wbn.setOrgIDs(model.getOrgGID(), model.getOrganizationID());
    				wbn.setGID(wbn.make_ID());
    				wbn.sign(sk);
    				D_Neighborhood dn = D_Neighborhood.getNeighByGID(wbn.getGID(), true, true, true, null, wbn.getOrgLID());
    				dn.loadRemote(wbn, null, null, null);
    				dn.storeRequest();
    				dn.releaseReference();
    				wbn = dn;
    				long __neighborhoodID = dn.getLID_force();
        			if (DEBUG) System.out.println("ConstituentsAction: AddMyself: storeConstData: got neigh "+dn);
    				n_data.global_nID = wbn.getGID();
    				n_data.neighborhoodID = __neighborhoodID; 
    				n_data.name = value;
    				n_data.name_lang=new Language(DDTranslation.authorship_lang.lang,DDTranslation.authorship_lang.flavor);
    				n_data.name_division=dialog.label[k];
    				n_data.name_division_lang=new Language(dialog.label_lang[k],DDTranslation.org_language.flavor);
    				n_data.names_subdivisions=subdivisions;
    				n_data.name_subdivisions_lang=new Language(DDTranslation.org_language.lang,DDTranslation.org_language.flavor);
   					n_data.submitterID=model.getConstituentIDMyself();
					n_data.organizationID=model.getOrganizationID();
					parent_nID=n_data.neighborhoodID;
     			} 
     			if (is_neighborhood && noChild){
        			if (DEBUG) System.out.println("ConstituentsAction: AddMyself: storeConstData: neigh no child");
   	   				if((child=can.getChildByID(n_data.neighborhoodID))!=null)
   					{
   						tree.collapsePath(tp.pathByAddingChild(child));
   					}
    				if (noChild&&(child==null)) { 
            			if (DEBUG) System.out.println("ConstituentsAction: AddMyself: storeConstData: no visible child in neigh");
     					int level = field_index(can.getFieldIDs(), dialog.fieldID[k]);
    					Constituents_LocationData data = new Constituents_LocationData();
    					data.value=value;
    					data.inhabitants = 1;
    					data.organizationID = model.getOrganizationID();
    					data.setLabel(dialog.label[k]);
    					data.fieldID = dialog.fieldID[k];
    					data.field_valuesID = field_valuesID; 
    					data.setFieldID_default_next(field_index(can.getFieldIDs(), field_default_next));
    					data.fieldID_above = field_above;
    					data.tip = dialog.tip[k];
    					data.list_of_values = dialog.lov[k];
    					can.addChild(child=new ConstituentsAddressNode(model,can,data,null,null,can.getNextAncestors(),can.getFieldIDs(),
    							level, -1, n_data), 0);
    					n_data = null;
    					inserted_neigh = true;
    				}
    				noChild=false;
     				field_above = dialog.fieldID[k];
    			}
    		}
    		cn = D_Constituent.getConstByLID(constituentID, true, true);
    		cn.setNeighborhoodIDs(null, parent_nID);
    		cn.sign();
    		cn.storeRequest();
    		cn.releaseReference();
			if (DEBUG) System.out.println("ConstituentsAction: AddMyself: storeConstData: inserted const "+cn);
    	}catch(Exception ev){
    		ev.printStackTrace();
    		return constituentID;
    	}
    	if(DEBUG) System.out.println("NoChildren="+noChild+" cID="+constituentID);
    	if(noChild && (constituentID!=-1)) {
    		ConstituentData data = new ConstituentData(cn);
    		data.setC_GID(gcd);
    		data.setC_LID(constituentID);
    		data.setGivenName(dialog.gnEditor);
    		data.setSurname(dialog.snEditor);
    		data.witness_against = 0;
    		data.witness_for = 1;
    		data.witnessed_by_me = 2;
    		data.setIcon(imageicon);
    		data.inserted_by_me = true;
    		data.external = false;
    		data.blocked = false;
    		data.broadcast = true;
    		data.setSlogan(net.ddp2p.common.table.constituent.INIT_SLOGAN);
        	data.email = dialog.emailEditor;
        	data.submitter_ID = ""+model.getConstituentIDMyself();
        	if(DEBUG) System.out.println("NoChildren="+child);
    		can.addChild(child=new ConstituentsIDNode(model,can,data,null,null,can.getNextAncestors()), 0);
        	if(DEBUG) System.out.println("Added="+child);
    		noChild = false;
    		inserted_neigh=true;
    	}
    	int new_index=0;
    	if(inserted_neigh)
    		model.fireTreeNodesInserted(new TreeModelEvent(tree,tp, new int[]{new_index},new Object[]{child}));
    	if(can.getNchildren()==1) {
    		if(child != null)
    			tree.expandPath(tp.pathByAddingChild(child));
    		else{
    			model.fireTreeStructureChanged(new TreeModelEvent(tree,tp));
    			tree.expandPath(tp);
    		}
    	}
    	for(;;) {
    		if (tp.getPath().length < 2) break;
    		TreePath tpp = tp.getParentPath();
    		Object crt=tp.getLastPathComponent();
    		new_index = ((ConstituentsAddressNode)tpp.getLastPathComponent()).getIndexOfChild(crt);
			if (DEBUG) System.out.println("ConstituentsAction: AddMyself: storeConstData: fire nodes changed in "+crt+" idx="+new_index);
    		model.fireTreeNodesChanged(new TreeModelEvent(tree,tpp, new int[]{new_index},
    				new Object[]{crt}));
    		tp = tpp;
    	}
    	if(DEBUG) System.out.println("constituentAction:astoreMyself:setBroadcastable");
    	D_Organization crt_org = model.getOrganization();
    	if(DEBUG) System.out.println("constituentAction:astoreMyself:setBroadcastable org = "+ crt_org);
    	if (true || ! crt_org.broadcasted) {
        	if(DEBUG) System.out.println("constituentAction:astoreMyself:setBroadcastable org set broadcasted");
        	crt_org = crt_org.getOrgByOrg_Keep(crt_org);
    		crt_org.broadcasted = true;
    		crt_org.storeLocalFlags();
    		crt_org.releaseReference();
    	}
    	if(DEBUG) System.out.println("constituentAction:astoreMyself:setBroadcastable done");
    	return constituentID;
    }
}
