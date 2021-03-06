/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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
/* ------------------------------------------------------------------------- */

package data;

import static java.lang.System.err;
import static java.lang.System.out;
import static util.Util._;
import hds.Address;
import hds.DDAddress;
import hds.DirectoryServer;
import hds.SR;
import hds.Server;
import hds.TypedAddress;

import java.awt.Component;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import ciphersuits.Cipher;
import ciphersuits.CipherSuit;
import ciphersuits.KeyManagement;
import ciphersuits.PK;
import ciphersuits.SK;
import util.P2PDDSQLException;
import streaming.UpdateMessages;
import streaming.UpdatePeersTable;
import table.organization;
import table.peer_org;
import util.Base64Coder;
import util.DBInterface;
import util.DDP2P_DoubleLinkedList;
import util.DDP2P_DoubleLinkedList_Node;
import util.DDP2P_DoubleLinkedList_Node_Payload;
import util.Util;
import widgets.peers.CreatePeer;
import widgets.peers.PeerInput;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.Application;
import config.DD;
import config.Identity;

public class D_PeerAddress extends ASNObj implements DDP2P_DoubleLinkedList_Node_Payload<D_PeerAddress> {
	public static class D_PeerAddress_Basic_Data {
		public String globalID;
		public String name;
		public String slogan;
		public String emails;
		public String phones;
		public String hash_alg;
		public byte[] signature;
		public Calendar creation_date;
		public byte[] picture;
		public boolean broadcastable;
		public String version;
		public String globalIDhash;
		public String plugin_info;
		public boolean revoked;
		public String revokation_instructions;
		public String revokation_GIDH;

		public D_PeerAddress_Basic_Data(String hash_alg, byte[] signature,
				byte[] picture, String version) {
			this.hash_alg = hash_alg;
			this.signature = signature;
			this.picture = picture;
			this.version = version;
		}
	}
	public static class D_PeerAddress_Local_Agent_State {
		//local_agent_state
		public Calendar arrival_date;
		public String plugins_msg;
		public Calendar last_sync_date;
		// Last generalized date when last_sync_date was reset for all orgs
		// (once may need extension for each org separately (advertised by remote)
		public Calendar last_reset;
		public String first_provider_peer;
		public String arrival_date_str;

		public D_PeerAddress_Local_Agent_State() {
		}
	}
	public static class D_PeerAddress_Preferences {
		// on addition of local fields do not forget to update them in setLocals()
		public Calendar preferences_date; // TODO:
		public boolean used; // whether I try to contact and pull/push from this peer
		public boolean filtered;
		public boolean blocked;
		//public boolean no_update;  // replaced by blocked
		//local_preferences
		public boolean hidden;
		public boolean name_verified;
		public boolean email_verified;
		public String category;

		public D_PeerAddress_Preferences(boolean used) {
			this.used = used;
		}
	}
	public static class D_PeerAddress_Node {
		
		/**
		 * Currently loaded peers, ordered by the access time
		 */
		private static DDP2P_DoubleLinkedList<D_PeerAddress> loaded_peers = new DDP2P_DoubleLinkedList<D_PeerAddress>();
		private static Hashtable<Long, D_PeerAddress> loaded_peer_By_LocalID = new Hashtable<Long, D_PeerAddress>();
		private static Hashtable<String, D_PeerAddress> loaded_peer_By_GID = new Hashtable<String, D_PeerAddress>();
		private static Hashtable<String, D_PeerAddress> loaded_peer_By_GIDhash = new Hashtable<String, D_PeerAddress>();
		private static long current_space = 0;
		/**
		 * message is enough (no need to store the Encoder itself)
		 */
		public byte[] message;
		public DDP2P_DoubleLinkedList_Node<D_PeerAddress> my_node_in_loaded;

		public D_PeerAddress_Node(byte[] message,
				DDP2P_DoubleLinkedList_Node<D_PeerAddress> my_node_in_loaded) {
			this.message = message;
			this.my_node_in_loaded = my_node_in_loaded;
		}
		/**
		 * 
		 * @param crt
		 */
		private static void register_fully_loaded(D_PeerAddress crt) {
			assert((crt.component_node.message==null) && (crt.loaded_globals));
			if(crt.component_node.message != null) return;
			if(!crt.loaded_globals) return;
			byte[] message = crt.encode();
			synchronized(loaded_peers) {
				crt.component_node.message = message; // crt.encoder.getBytes();
				if(crt.component_node.message != null) current_space += crt.component_node.message.length;
			}
		}
		/**
		 * Here we manage the registered peers.
		 * @param crt
		 */
		private static void register_loaded(D_PeerAddress crt){
			//crt.encoder = crt.getEncoder();
			if(crt.loaded_globals) crt.component_node.message = crt.encode(); //crt.encoder.getBytes();
			synchronized(loaded_peers) {
				loaded_peers.offerFirst(crt);
				loaded_peer_By_LocalID.put(new Long(crt._peer_ID), crt);
				loaded_peer_By_GID.put(crt.component_basic_data.globalID, crt);
				loaded_peer_By_GIDhash.put(crt.component_basic_data.globalIDhash, crt);
				if(crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				while((loaded_peers.size() > MAX_LOADED_PEERS)
						|| (current_space > MAX_PEERS_RAM)) {
					if(loaded_peers.size() <= MIN_PEERS_RAM) break; // at least _crt_peer and _myself
					D_PeerAddress candidate = loaded_peers.getTail();
					if((candidate == D_PeerAddress._crt_peer)||(candidate == D_PeerAddress.get_myself())){
						setRecent(candidate);
						continue;
					}
					
					D_PeerAddress removed = loaded_peers.removeTail();//remove(loaded_peers.size()-1);
					loaded_peer_By_LocalID.remove(new Long(removed._peer_ID));
					loaded_peer_By_GID.remove(removed.component_basic_data.globalID);
					loaded_peer_By_GIDhash.remove(removed.component_basic_data.globalIDhash);
					if(removed.component_node.message != null) current_space -= removed.component_node.message.length;				
				}
			}
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_PeerAddress crt) {
			//if(loaded_peers.remove(crt)) loaded_peers.offerFirst(crt);
			loaded_peers.moveToFront(crt);
		}

	}
	boolean loaded_basics = false; //have we loaded local parameters (name, slogan)
	boolean loaded_locals = false; //have we loaded localIDs (for item received from remote)
	boolean loaded_globals = false; //have we loaded GIDs (for item loaded from database)
	boolean loaded_addresses = false; //have we loaded addresses
	boolean loaded_served_orgs = false; //have we loaded local IDs for served orgs
	boolean loaded_instances = false; //have we loaded instances of this peer
	boolean loaded_my_data = false; //have we loaded my display strings for this peer
	boolean loaded_local_preferences = false; //have we loaded other data about this peer (blocked, etc)
	boolean loaded_local_agent_state = false; //have we loaded other data about this peer (schedules, etc)

	/**
	 * Flags about things that have to be saved since they were changed.
	 * Currently not always flagged when needed, and a re-implementation should implement them better...
	 */
	public boolean dirty_main = false;
	public boolean dirty_addresses = false;
	public boolean dirty_served_orgs = false;
	public boolean dirty_instances = false;
	public boolean dirty_my_data = false;

	
	public static final String SIGN_ALG_SEPARATOR = ":";
	public static String[] signature_alg = hds.SR.HASH_ALG_V1; // of PrintStr OPT
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	//private static final boolean encode_addresses = false;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	private static final String DEFAULT_VERSION = DDAddress.V2;
	private static final boolean old_addresses_code = false; // for old implementation of addresses
	public static int MAX_LOADED_PEERS = 10000;
	public static long MAX_PEERS_RAM = 10000000;
	private static final int MIN_PEERS_RAM = 2;

	public boolean dirty_any() {
		if (dirty_main) return true;
		if (dirty_addresses) return true;
		if (dirty_served_orgs) return true;
		if (dirty_instances) return true;
		if (dirty_my_data) return true;
		return false;
	}
	public boolean dirty_all() {
		if (
				(dirty_main)
				&& (dirty_addresses)
				&& (dirty_served_orgs)
				&& (dirty_instances)
				&& (dirty_my_data))
			return true;
		return false;
	}
	public void set_dirty_all(boolean dirty) {
		dirty_main = dirty;
		dirty_addresses = dirty;
		dirty_served_orgs = dirty;
		dirty_instances = dirty;
		dirty_my_data = dirty;
	}
	
	public D_PeerAddress_Basic_Data component_basic_data = new D_PeerAddress_Basic_Data(
			D_PeerAddress.getStringFromHashAlg(hds.SR.HASH_ALG_V1), new byte[0], null, DDAddress.V2);
	//locals
	public long _peer_ID = -1;
	public String peer_ID;
	// here go also the local_IDs or served orgs
	
	//globals
	public String instance;  // not part of database entry, not signed, but encoded/decoded

	//addresses
	public TypedAddress[] address=null; // OPT // should be null when signing 
	public TypedAddress[] address_orig=null; // as on the disk
	
	//served_orgs
	public D_PeerOrgs[] served_orgs = null; //OPT
	/**
	 * Not yet implemented
	 */
	public D_PeerOrgs[] served_orgs_orig = null;

	// instances
	public Hashtable<String,D_PeerInstance> instances = null;  
	public Hashtable<String,D_PeerInstance>  instances_orig = null;
	
	public D_PeerAddress_Preferences component_preferences = new D_PeerAddress_Preferences(
			false);
	public D_PeerAddress_Local_Agent_State component_local_agent_state = new D_PeerAddress_Local_Agent_State();
	@Deprecated
	public String experience;
	@Deprecated
	public String exp_avg;

	// handling myself
	private static final Object monitor_init_myself = new Object();
	private static final Object monitor_object_factory = new Object();
	private static D_PeerAddress _crt_peer = null;
	private static D_PeerAddress _myself = null;
	public static void init_myself(String gid, String instance) throws P2PDDSQLException {
		synchronized(monitor_init_myself) {
			_myself = new D_PeerAddress(gid);
			_myself.setCurrentInstance(instance);
			
			if ((Identity.current_peer_ID != null) && (Identity.current_peer_ID.globalID != null)) {
				String global_peer_ID = Identity.current_peer_ID.globalID;
				String peer_instance = Identity.current_peer_ID.instance;
				if (global_peer_ID.equals(_myself.getGID())) {
					_myself.setCurrentInstance(peer_instance);
					if (DEBUG) System.out.println("D_PeerAddress:init_myself: set instance="+peer_instance);
				} else {
					if (DEBUG) System.out.println("D_PeerAddress:init_myself: diff GID="+global_peer_ID);
				}
			} else {
				if (DEBUG) Util.printCallPath("D_PeerAddress:init_myself: starting?");
			}
			
			if (DEBUG) System.out.println("D_PeerAddress:init_myself: got="+_myself);
			
			if ((_myself != null) && (_myself.component_basic_data!=null))
				if (DD.status != null) DD.status.setMePeer(_myself);

		}
	}
	/**
	 * Reload myself (if previously loaded)
	 * If previously unloaded, leave it alone!
	 * @throws P2PDDSQLException
	 */
	public static void re_init_myself() throws P2PDDSQLException {
		synchronized(monitor_init_myself) {
			if(_myself==null) return; // Why?
			init_myself(_myself.getGID(), _myself.getInstance());
		}
	}
	public static D_PeerAddress get_myself_from_Identity()  throws P2PDDSQLException{
		return get_myself(Identity.current_peer_ID.globalID, Identity.current_peer_ID.instance);
	}
	public static D_PeerAddress get_myself() {
		return _myself;
	}
	/**
	 * Returns myself if not null, else tries to load it
	 * @param gid
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_PeerAddress get_myself(String gid, String instance) throws P2PDDSQLException{
		synchronized(monitor_init_myself) {
			if (gid == null) return _myself;
			if ((_myself != null) && (gid.equals(_myself.getGID())))
				return _myself;
			//init_myself(gid);
			_myself = new D_PeerAddress(gid);
			_myself.setCurrentInstance(instance);
			
			String global_peer_ID = Identity.current_peer_ID.globalID;
			String peer_instance = Identity.current_peer_ID.instance;
			if (global_peer_ID.equals(_myself.getGID())) {
				_myself.setCurrentInstance(peer_instance);
				if (DEBUG) System.out.println("D_PeerAddress:get_myself: set instance="+peer_instance);
			} else {
				if (DEBUG) System.out.println("D_PeerAddress:get_myself: diff GID="+global_peer_ID);
			}
			
			if (DEBUG) System.out.println("D_PeerAddress:get_myself: got="+_myself);
			
			if ((_myself != null) && (_myself.component_basic_data!=null))
				if (DD.status != null) DD.status.setMePeer(_myself);
			return _myself;
		}		
	}
	D_PeerAddress_Node component_node = new D_PeerAddress_Node(null, null);
	public int status_references = 0;
	@Override
	public DDP2P_DoubleLinkedList_Node<D_PeerAddress> set_DDP2P_DoubleLinkedList_Node(
			DDP2P_DoubleLinkedList_Node<D_PeerAddress> node) {
		DDP2P_DoubleLinkedList_Node<D_PeerAddress> old = this.component_node.my_node_in_loaded;
		this.component_node.my_node_in_loaded = node;
		return old;
	}
	@Override
	public DDP2P_DoubleLinkedList_Node<D_PeerAddress> get_DDP2P_DoubleLinkedList_Node() {
		return component_node.my_node_in_loaded;
	}
	
	/**
	 * exception raised on error
	 * @param ID
	 * @param load_Globals 
	 * @return
	 */
	static public D_PeerAddress getPeer_Attempt(Long ID, boolean load_Globals){
		Long id = new Long(ID);
		D_PeerAddress crt = D_PeerAddress_Node.loaded_peer_By_LocalID.get(id);
		if(crt == null) return null;
		
		if(load_Globals && !crt.loaded_globals){
			crt.loadGlobals();
			D_PeerAddress_Node.register_fully_loaded(crt);
		}
		D_PeerAddress_Node.setRecent(crt);
		return crt;
	}
	/**
	 * 
	 * @param ID
	 * @param load_Globals
	 * @return
	 */
	static public D_PeerAddress getPeer(long ID, boolean load_Globals){
		if(ID <= 0) return null;
		Long id = new Long(ID);
		D_PeerAddress crt = D_PeerAddress.getPeer_Attempt(id, load_Globals);
		if(crt != null) return crt;
		
		synchronized(monitor_object_factory) {
			crt = D_PeerAddress.getPeer_Attempt(id, load_Globals);
			if(crt != null) return crt;
			
			try {
				crt = new D_PeerAddress(ID);
			} catch (Exception e) {
				if(DEBUG) e.printStackTrace();
				return null;
			}
			D_PeerAddress_Node.register_loaded(crt);
			return crt;
		}
	}
	static public D_PeerAddress getPeerByLID(String ID, boolean load_Globals){
		try{
			return getPeer(Util.lval(ID), load_Globals);
		}catch (Exception e) {
			return null;
		}
	}
	/**
	 * exception raised on error
	 * @param GID
	 * @param load_Globals 
	 * @return
	 */
	static public D_PeerAddress getPeerByGID_Attempt(String GID, boolean load_Globals){
		if(GID == null) return null;
		D_PeerAddress crt = D_PeerAddress_Node.loaded_peer_By_GID.get(GID);
		if(crt == null) return null;
		
		if(load_Globals && !crt.loaded_globals){
			crt.loadGlobals();
			D_PeerAddress_Node.register_fully_loaded(crt);
		}
		D_PeerAddress_Node.setRecent(crt);
		return crt;
	}
	/**
	 * 
	 * @param GID
	 * @param load_Globals
	 * @return
	 */
	static public D_PeerAddress getPeerByGID(String GID, boolean load_Globals){
		if(GID == null) return null;
		D_PeerAddress crt = D_PeerAddress.getPeerByGID_Attempt(GID, load_Globals);
		if(crt != null) return crt;

		synchronized(monitor_object_factory) {
			crt = D_PeerAddress.getPeerByGID_Attempt(GID, load_Globals);
			if(crt != null) return crt;

			try {
				crt = new D_PeerAddress(GID);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			D_PeerAddress_Node.register_loaded(crt);
			return crt;
		}
	}
	/**
	 * exception raised on error
	 * @param GIDhash
	 * @param load_Globals 
	 * @return
	 */
	static public D_PeerAddress getPeerByGIDhash_Attempt(String GIDhash, boolean load_Globals){
		if(GIDhash == null) return null;
		D_PeerAddress crt = D_PeerAddress_Node.loaded_peer_By_GIDhash.get(GIDhash);
		if(crt != null){
			if(load_Globals && !crt.loaded_globals){
				crt.loadGlobals();
				D_PeerAddress_Node.register_fully_loaded(crt);
			}
			D_PeerAddress_Node.setRecent(crt);
			return crt;
		}
		return null;
	}
	/**
	 * 
	 * @param GIDhash
	 * @param load_Globals
	 * @return
	 */
	static public D_PeerAddress getPeerByGIDhash(String GIDhash, boolean load_Globals){
		if(GIDhash == null) return null;
		D_PeerAddress crt = getPeerByGIDhash_Attempt(GIDhash, load_Globals);
		if(crt != null) return crt;
		
		synchronized(monitor_object_factory) {
			crt = getPeerByGIDhash_Attempt(GIDhash, load_Globals);
			if(crt != null) return crt;

			try {
				crt = new D_PeerAddress(GIDhash, false, 0);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			D_PeerAddress_Node.register_loaded(crt);
			return crt;
		}
	}
	/**
	 * 
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals 
	 * @return
	 */
	static public D_PeerAddress getPeerByGID_or_GIDhash_Attempt(String GID, String GIDhash, boolean load_Globals){
		if((GID==null) && (GIDhash==null)) return null;
		if(GID!=null){
			String hash = D_PeerAddress.getGIDHashFromGID(GID);
			if(GIDhash!=null){
				if(!hash.equals(GIDhash)) throw new RuntimeException("No GID and GIDhash match");
			}else GIDhash = hash;
		}
		D_PeerAddress crt = D_PeerAddress_Node.loaded_peer_By_GIDhash.get(GIDhash);
		if((crt==null)&&(GID!=null)) crt = D_PeerAddress_Node.loaded_peer_By_GID.get(GID);
		if(crt != null){
			if(load_Globals && !crt.loaded_globals){
				crt.loadGlobals();
				D_PeerAddress_Node.register_fully_loaded(crt);
			}
			D_PeerAddress_Node.setRecent(crt);
			return crt;
		}
		return null;
	}
	/**
	 * exception raised on error
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals
	 * @return
	 */
	static public D_PeerAddress getPeerByGID_or_GIDhash(String GID, String GIDhash, boolean load_Globals){
		if((GID==null) && (GIDhash==null)) return null;
		D_PeerAddress crt = getPeerByGID_or_GIDhash_Attempt(GID, GIDhash, load_Globals);
		if(crt != null) return crt;

		synchronized(monitor_object_factory) {
			crt = getPeerByGID_or_GIDhash_Attempt(GID, GIDhash, load_Globals);
			if(crt != null) return crt;
			
			try {
				if(GID!=null) crt = new D_PeerAddress(GID);
				else crt = new D_PeerAddress(GIDhash, false, 0);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			D_PeerAddress_Node.register_loaded(crt);
			return crt;
		}
	}
	/**
	 * Used when we are now sure whether what we have is a GID or a GIDhash
	 * exception raised on error
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals (should GIDs in served orgs load full D_Orgs)
	 * @return
	 */
	static public D_PeerAddress getPeerBy_or_hash(String GID_or_hash, boolean load_Globals){
		if(GID_or_hash == null) return null;
		String GID = null;
		String GIDhash = D_PeerAddress.getGIDHashGuess(GID_or_hash);
		if(!GIDhash.equals(GID_or_hash)) GID = GID_or_hash;

		D_PeerAddress crt = getPeerByGID_or_GIDhash_Attempt(GID, GIDhash, load_Globals);
		if(crt != null) return crt;

		synchronized(monitor_object_factory) {
			crt = getPeerByGID_or_GIDhash_Attempt(GID, GIDhash, load_Globals);
			if(crt != null) return crt;
			
			try {
				if(GID!=null) crt = new D_PeerAddress(GID);
				else crt = new D_PeerAddress(GIDhash, false, 0);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			D_PeerAddress_Node.register_loaded(crt);
			return crt;
		}
	}
	/**
	 * TODO
	 * Have to load the served_orgs GIDs
	 */
	private void loadGlobals() {
		if(loaded_globals) return;
		if(!this.loaded_served_orgs) loadServedOrgsLocals();
		// TODO load globals
	}
	/**
	 * TODO
	 */
	private void loadServedOrgsLocals() {
		// TODO Auto-generated method stub
		
	}
	/**
	 * Used to create myself
	 * @param id
	 * @param _creation_date : creationDate used if new
	 * @param creation_date2
	 * @return
	 * @throws P2PDDSQLException 
	 */
	public static D_PeerAddress get_or_create_myself(Identity id,
			Calendar _creation_date, String creation_date2) throws P2PDDSQLException {
		D_PeerAddress me = get_myself(id.globalID, id.instance); 
		if(me.peer_ID == null) { // I did not exist (with this globID)
			if(Server.DEBUG) out.println("Server:update_insert_peer_myself: required new peer");
			me.component_basic_data.emails = Identity.emails;
			me.component_basic_data.globalID = id.globalID; //Identity.current_peer_ID.globalID;
			me.component_basic_data.name = id.name; //Identity.current_peer_ID.name;
			me.component_basic_data.slogan = id.slogan; //Identity.current_peer_ID.slogan;
			me.component_basic_data.creation_date = _creation_date;//Util.CalendargetInstance();
			me.signature_alg = SR.HASH_ALG_V1;
			me.component_basic_data.broadcastable = DD.DEFAULT_BROADCASTABLE_PEER_MYSELF;//Identity.getAmIBroadcastable();
			
			String picture = Identity.getMyCurrentPictureStr();
			me.component_basic_data.picture = Util.byteSignatureFromString(picture);
			
//			byte[] signature = me.sign(DD.getMyPeerSK());//Util.sign_peer(pa);
//			if(Server.DEBUG) {
//				if(!me.verifySignature()) {
//					me.signature = signature;
//					if(Server.DEBUG) out.println("Server:update_insert_peer_myself: signature verification failed at creation: "+me);
//				}else{
//					if(Server.DEBUG) out.println("Server:update_insert_peer_myself: signature verification passed at creation: "+me);			
//				}
//			}
			
			String IDhash = getGIDHashFromGID(id.globalID);
			me.component_basic_data.globalIDhash = IDhash;
			if(Server.DEBUG) out.println("Server:update_insert_peer_myself: will insert new peer");
			me.sign(DD.getMyPeerSK());
			String pID=me.storeVerified(creation_date2);
			Identity.current_identity_creation_date = me.component_basic_data.creation_date;
			Identity.peer_ID = pID;
			if(Server.DEBUG) out.println("Server:update_insert_peer_myself: inserted"+pID);
		}
		return me;
	}

	public String toSummaryString() {
		String result = "\nPeerAddress: v=" +component_basic_data.version+
				" [gID="+Util.trimmed(component_basic_data.globalID)+" name="+((component_basic_data.name==null)?"null":"\""+component_basic_data.name+"\"")
		+" slogan="+(component_basic_data.slogan==null?"null":"\""+component_basic_data.slogan+"\"") + " date="+Encoder.getGeneralizedTime(component_basic_data.creation_date);
		//result += " address="+Util.nullDiscrimArray(address, " --- ");
		//result += "\n\t broadcastable="+(broadcastable?"1":"0");
		//result += " sign_alg["+((signature_alg==null)?"NULL":signature_alg.length)+"]="+Util.concat(signature_alg, ":") +" sign="+Util.byteToHexDump(signature, " ");
		//result += " pict="+Util.byteToHexDump(picture, " ");
		result += " served_org["+((served_orgs!=null)?served_orgs.length:"")+"]="+Util.concat(served_orgs, "----", "\"NULL\"");
		return result+"]";
	}	
	public String toString() {
		String result = 
				"\nD_PeerAddress:ID=["+this._peer_ID+"] v=" +component_basic_data.version+
				" [gID="+Util.trimmed(component_basic_data.globalID)+
				" instance="+instance+
				" name="+((component_basic_data.name==null)?"null":"\""+component_basic_data.name+"\"")+
				" slogan="+(component_basic_data.slogan==null?"null":"\""+component_basic_data.slogan+"\"") + 
				" emails="+((component_basic_data.emails==null)?"null":"\""+component_basic_data.emails+"\"")+
				" phones="+((component_basic_data.phones==null)?"null":"\""+component_basic_data.phones+"\"")+
				" crea_date="+Encoder.getGeneralizedTime(component_basic_data.creation_date);
		
		result += "\n\t address="+Util.nullDiscrimArray(address, " --- ");
		result += "\n\t broadcastable="+(component_basic_data.broadcastable?"1":"0");
		result += "\n\t [] sign_alg["+((signature_alg==null)?"NULL":signature_alg.length)+"]="+Util.concat(signature_alg, ":") +" sign="+Util.byteToHexDump(component_basic_data.signature, " ") 
		+"\n\t[] pict="+Util.byteToHexDump(component_basic_data.picture, " ")+
		"\n\t served_org["+((served_orgs!=null)?served_orgs.length:"")+"]="+Util.concat(served_orgs, "----", "\"NULL\"");
		return result+"]";
	}
	public static String[] getHashAlgFromString(String hash_alg) {
		if(hash_alg!=null) return hash_alg.split(Pattern.quote(DD.APP_ID_HASH_SEP));
		return null;
	}
	private static String getStringFromHashAlg(String[] signature_alg) {
		if (signature_alg==null) return null;
		return Util.concat(signature_alg, DD.APP_ID_HASH_SEP);
	}
	public Encoder getSignatureEncoder(){
		if(DEBUG) System.out.println("D_PeerAddress: getSignatureEncoder_V2: start");
		if(DDAddress.V0.equals(component_basic_data.version)) return getSignatureEncoder_V0();
		if(DDAddress.V1.equals(component_basic_data.version)) return getSignatureEncoder_V1();
		if(DDAddress.V2.equals(component_basic_data.version)) return getSignatureEncoder_V2();
		return getSignatureEncoder_V0();
	}
	private Encoder getSignatureEncoder_V1() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(component_basic_data.version,false));
		enc.addToSequence(new Encoder(component_basic_data.globalID).setASN1Type(Encoder.TAG_PrintableString));
		if(component_basic_data.name!=null)enc.addToSequence(new Encoder(component_basic_data.name,Encoder.TAG_UTF8String));
		if(component_basic_data.slogan!=null)enc.addToSequence(new Encoder(component_basic_data.slogan,DD.TAG_AC0));
		if(component_basic_data.emails!=null)enc.addToSequence(new Encoder(component_basic_data.emails,DD.TAG_AC1));
		if(component_basic_data.phones!=null)enc.addToSequence(new Encoder(component_basic_data.phones,DD.TAG_AC2));
		//if(instance!=null)enc.addToSequence(new Encoder(instance,DD.TAG_AC3));
		if(component_basic_data.creation_date!=null)enc.addToSequence(new Encoder(component_basic_data.creation_date));
		//if(address!=null)enc.addToSequence(Encoder.getEncoder(address));
		enc.addToSequence(new Encoder(component_basic_data.broadcastable));
		//if(signature_alg!=null)enc.addToSequence(Encoder.getStringEncoder(signature_alg, Encoder.TAG_PrintableString));
		if((served_orgs!=null)&&(served_orgs.length>0)) {
			D_PeerOrgs[] old = served_orgs;
			served_orgs = makeSignaturePeerOrgs_VI(served_orgs);
			enc.addToSequence(Encoder.getEncoder(this.served_orgs).setASN1Type(DD.TAG_AC12));
			served_orgs = old;
		}
		//enc.addToSequence(new Encoder(signature));
		//enc.setASN1Type(TAG);
		return enc;
	}
	private Encoder getSignatureEncoder_V2() {
		if(DEBUG) System.out.println("D_PeerAddress: getSignatureEncoder_V2: start: this="+this);
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(component_basic_data.version,false));
		enc.addToSequence(new Encoder(component_basic_data.globalID).setASN1Type(Encoder.TAG_PrintableString));
		if(component_basic_data.name!=null)enc.addToSequence(new Encoder(component_basic_data.name,Encoder.TAG_UTF8String));
		if(component_basic_data.slogan!=null)enc.addToSequence(new Encoder(component_basic_data.slogan,DD.TAG_AC0));
		if(component_basic_data.emails!=null)enc.addToSequence(new Encoder(component_basic_data.emails,DD.TAG_AC1));
		if(component_basic_data.phones!=null)enc.addToSequence(new Encoder(component_basic_data.phones,DD.TAG_AC2));
		//if(instance!=null)enc.addToSequence(new Encoder(instance,DD.TAG_AC3));
		if(component_basic_data.creation_date!=null)enc.addToSequence(new Encoder(component_basic_data.creation_date));
		if(address!=null)enc.addToSequence(Encoder.getEncoder(address));
		enc.addToSequence(new Encoder(component_basic_data.broadcastable));
		//if(signature_alg!=null)enc.addToSequence(Encoder.getStringEncoder(signature_alg, Encoder.TAG_PrintableString));
		if((served_orgs!=null)&&(served_orgs.length>0)) {
			D_PeerOrgs[] old = served_orgs;
			served_orgs = makeSignaturePeerOrgs_VI(served_orgs);
			enc.addToSequence(Encoder.getEncoder(this.served_orgs).setASN1Type(DD.TAG_AC12));
			served_orgs = old;
		}
		if(DEBUG) System.out.println("D_PeerAddress: getSignatureEncoder_V2: got:"+Util.byteToHexDump(enc.getBytes()));
		//enc.addToSequence(new Encoder(signature));
		//enc.setASN1Type(TAG);
		return enc;
	}

	/**
	 * Assumes served orgs are already ordered by GID
	 * @param served_orgs
	 * @return
	 */
	public static D_PeerOrgs[] makeSignaturePeerOrgs_VI(D_PeerOrgs[] served_orgs) {
		D_PeerOrgs[] result = new D_PeerOrgs[served_orgs.length];
		for(int k=0;k<result.length;k++) {
			result[k] = new D_PeerOrgs();
			if(served_orgs[k].global_organization_IDhash != null) 
				result[k].global_organization_IDhash = served_orgs[k].global_organization_IDhash;
			else 
				result[k].global_organization_IDhash =
				D_Organization.getOrgGIDHashGuess(served_orgs[k].global_organization_ID);
		}
		result = sortByOrgGIDHash(result);
		return result;
	}

	public static D_PeerOrgs[] sortByOrgGIDHash(D_PeerOrgs[] result) {
		Arrays.sort(result, new Comparator<D_PeerOrgs>() {
			@Override
			public int compare(D_PeerOrgs o1, D_PeerOrgs o2) {
				String s1 = o1.global_organization_IDhash;
				if(s1==null) return -1;
                String s2 = o2.global_organization_IDhash;
				if(s2==null) return -1;
                return s1.compareTo(s2);
			}
       });
		return result;
	}

	public Encoder getSignatureEncoder_V0(){
		TypedAddress[] _address = address;
		byte[] _signature = component_basic_data.signature;
		address = null;
		component_basic_data.signature = new byte[0];
		Encoder enc = getEncoder();
		address = _address;
		component_basic_data.signature = _signature;
		return enc;
	}
	public static D_PeerAddress getPeerAddress(String peer_ID, boolean _addresses, boolean _served) {
		D_PeerAddress result=null;
		try {
			result = new D_PeerAddress(Util.lval(peer_ID,-1), false, _addresses, _served);
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
		return result;
	}
	/**
	 * For unknown reason this method loads the peer twice 
	 * @param peer_ID
	 * @param _addresses
	 * @param _served
	 * @return
	 */
	@Deprecated
	public static D_PeerAddress _getPeerAddress(String peer_ID, boolean _addresses, boolean _served) {
		D_PeerAddress result=null;
		try {
			result = new D_PeerAddress(Util.lval(peer_ID,-1), false, _addresses, _served);
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
		String sql = "SELECT "+table.peer.broadcastable+
			","+table.peer.creation_date+
			","+table.peer.global_peer_ID+
			","+table.peer.hash_alg+
			","+table.peer.name+
			","+table.peer.picture+
			","+table.peer.signature+
			","+table.peer.slogan+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.peer_ID+"=?;";
		ArrayList<ArrayList<Object>> p;
		try {
			p = Application.db.select(sql, new String[]{peer_ID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		if(p.size()==0) return null;
		if(DEBUG)System.out.println("D_PeerAddress:init:old peerID="+peer_ID);
		result.component_basic_data.broadcastable="1".equals(p.get(0).get(0));
		result.component_basic_data.creation_date=Util.getCalendar(Util.getString(p.get(0).get(1)));
		result.component_basic_data.globalID=Util.getString(p.get(0).get(2));
		String hash_alg = Util.getString(p.get(0).get(3));
		if(hash_alg!=null) result.signature_alg=hash_alg.split(Pattern.quote(DD.APP_ID_HASH_SEP));
		result.component_basic_data.name=Util.getString(p.get(0).get(4));
		result.component_basic_data.picture=Util.byteSignatureFromString(Util.getString(p.get(0).get(5)));
		result.component_basic_data.signature=Util.byteSignatureFromString(Util.getString(p.get(0).get(6)));
		result.component_basic_data.slogan=Util.getString(p.get(0).get(7));
		String addresses=null;
		if(_addresses) {
			try {
				addresses = UpdatePeersTable.getPeerAddresses(peer_ID, Encoder.getGeneralizedTime(0), Util.getGeneralizedTime());
				result.address=TypedAddress.getAddress(addresses);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		if(_served) result.served_orgs=data.D_PeerAddress._getPeerOrgs(result.component_basic_data.globalID);;
		return result;
	}	
	/**
	 * No init
	 */
	public D_PeerAddress(){}
	@SuppressWarnings("unchecked")
	@Deprecated
	public D_PeerAddress(D_PeerAddress pa) {
		component_basic_data.globalID = pa.component_basic_data.globalID; //Printable
		component_basic_data.name = pa.component_basic_data.name; //UTF8
		component_basic_data.slogan = pa.component_basic_data.slogan; //UTF8
		address = pa.address; //UTF8
		address_orig = pa.address_orig;
		component_basic_data.creation_date = pa.component_basic_data.creation_date;
		component_basic_data.signature = pa.component_basic_data.signature; //OCT STR
		//global_peer_ID_hash =Util.getGIDhash(pa.globalID);
		component_basic_data.broadcastable = pa.component_basic_data.broadcastable;
		signature_alg = pa.signature_alg;
		component_basic_data.version = pa.component_basic_data.version;
		component_basic_data.picture = pa.component_basic_data.picture;
		served_orgs = pa.served_orgs; //OPT
		//type = pa.type;
		component_basic_data.emails = pa.component_basic_data.emails;
		component_basic_data.phones = pa.component_basic_data.phones;
		instances = (Hashtable<String, D_PeerInstance>) pa.instances.clone();
	}

	/**
	 * 
	 * @param peerID
	 * @param failOnNew avoids getting a peer with inexistant ID
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(String peerID, boolean failOnNew) throws P2PDDSQLException{
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.peer_ID+"=?;";
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerID});
		if(p.size()==0){
			if(failOnNew) throw new RuntimeException("Absent peer "+peerID);
			peer_ID = peerID;
		}else{
			init(p.get(0));
		}
	}
	/**
	 * 
	 * @param peerGID
	 * @param peerGIDhash
	 * @param failOnNew
	 * @param _addresses
	 * @param _served_orgs
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(String peerGID, String peerGIDhash, boolean failOnNew, boolean _addresses, boolean _served_orgs) throws P2PDDSQLException{
		String sql =
				"SELECT "+table.peer.fields_peers+
				" FROM "+table.peer.TNAME+
				" WHERE ("+table.peer.global_peer_ID+"=? OR "+table.peer.global_peer_ID_hash+"=?);";
			//String _peerID = Util.getStringID();
			ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerGID, peerGIDhash});
			if(p.size()==0){
				//peer_ID = _peerID;
				if(failOnNew) throw new RuntimeException("Absent peer="+peerGID+" hash="+peerGIDhash);
			}else{
				init(p.get(0), _addresses, _served_orgs);
			}
	}
	/**
	 * 
	 * @param peerID
	 * @param failOnNew
	 * @param _addresses
	 * @param _served_orgs
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(long peerID, boolean failOnNew, boolean _addresses, boolean _served_orgs) throws P2PDDSQLException{
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.peer_ID+"=?;";
		String _peerID = Util.getStringID(peerID);
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{_peerID});
		if(p.size()==0){
			peer_ID = _peerID;
			if(failOnNew) throw new RuntimeException("Absent peer "+peerID);
		}else{
			init(p.get(0), _addresses, _served_orgs);
		}
	}
	/**
	 * Exits with Exception when no such peer exists
	 * @param peerID
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(long peerID) throws P2PDDSQLException{
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.peer_ID+"=?;";
		String _peerID = Util.getStringID(peerID);
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{_peerID});
		if(p.size()==0){
			peer_ID = _peerID;
			if(true) throw new RuntimeException("Absent peer "+peerID);
		}else{
			init(p.get(0), true, true);
		}
	}
	/**
	 * 
	 * @param peerID
	 * @param failOnNew
	 * @param _addresses
	 * @param _served_orgs
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(String peerGID, boolean failOnNew, boolean _addresses, boolean _served_orgs) throws P2PDDSQLException{
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID+"=?;";
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerGID});
		if(p.size()==0){
			this.component_basic_data.globalID = peerGID;
			if(failOnNew) throw new RuntimeException("Absent peer "+peerGID);
		}else{
			init(p.get(0), _addresses, _served_orgs);
		}
	}
	/**
	 * 
	 * Simple presence of parameter isGID signals a GID
	 * @param peerGID
	 * @param isGID dummy parameter to differentiate from case with local peerID
	 * @param failOnNew
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(String peerGID, int isGID, boolean failOnNew) throws P2PDDSQLException{
		init(peerGID, isGID, failOnNew);
	}
	/**
	 * This does not fail on new
	 * @param peerGID
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(String peerGID) throws P2PDDSQLException {
		init(peerGID, 1, false);
	}
	 
	/**
	 * 
	 * @param peerGID
	 * @param isGID dummy parameter to differentiate from case with local peerID
	 * @param failOnNew
	 * @param _addresses
	 * @param _served_orgs
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(String peerGID, int isGID, boolean failOnNew, boolean _addresses, boolean _served) throws P2PDDSQLException{
			String sql =
				"SELECT "+table.peer.fields_peers+
				" FROM "+table.peer.TNAME+
				" WHERE "+table.peer.global_peer_ID+"=?;";
			ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerGID});
			if(p.size()==0){
				this.component_basic_data.globalID = peerGID;
				if(failOnNew) throw new RuntimeException("Absent peer "+peerGID);
			}else{
				init(p.get(0), _addresses, _served);
			}
		}
	public D_PeerAddress(D_PeerAddress dd, boolean encode_addresses) {
		component_basic_data.version = dd.component_basic_data.version;
		component_basic_data.globalID = dd.component_basic_data.globalID;
		component_basic_data.name = dd.component_basic_data.name;
		component_basic_data.emails = dd.component_basic_data.emails;
		component_basic_data.phones = dd.component_basic_data.phones;
		component_basic_data.slogan = dd.component_basic_data.slogan;
		component_basic_data.creation_date = dd.component_basic_data.creation_date;
		component_basic_data.picture = dd.component_basic_data.picture;
		served_orgs = dd.served_orgs;
		if(encode_addresses){
			address = dd.address;
		}
		component_basic_data.broadcastable = dd.component_basic_data.broadcastable;
		signature_alg = dd.signature_alg;
		component_basic_data.signature = dd.component_basic_data.signature;
	}
	public D_PeerAddress(DDAddress dd, boolean encode_addresses) {
		component_basic_data.version = dd.version;
		if(!encode_addresses && DDAddress.V2.equals(component_basic_data.version))
			Util.printCallPath("Here we thought that no address is needed");
		component_basic_data.globalID = dd.globalID;
		component_basic_data.name = dd.name;
		component_basic_data.slogan = dd.slogan;
		component_basic_data.emails = dd.emails;
		component_basic_data.phones = dd.phones;
		component_basic_data.creation_date = Util.getCalendar(dd.creation_date);
		component_basic_data.picture = dd.picture;
		if(encode_addresses){
			String[]addresses_l = Address.split(dd.address);
			// may have to control addresses, to defend from DOS based on false addresses
			address = new TypedAddress[addresses_l.length];
			for(int k=0; k<addresses_l.length; k++) {
				TypedAddress a = new TypedAddress();
				String taddr[] = addresses_l[k].split(Pattern.quote(Address.ADDR_PART_SEP),TypedAddress.COMPONENTS);
				if(taddr.length < 2) continue;
				a.type = taddr[0];
				String adr[] = taddr[1].split(Pattern.quote(TypedAddress.PRI_SEP), 2);
				a.address = adr[0];
				if(adr.length > 1) {
					a.certified = true;
					try{
						a.priority = Integer.parseInt(adr[1]);
					}catch(Exception e){e.printStackTrace();}
				}
				address[k] = a;
			}
		}
		component_basic_data.broadcastable = dd.broadcastable;
		signature_alg = dd.hash_alg;
		component_basic_data.hash_alg = D_PeerAddress.getStringFromHashAlg(signature_alg);
		component_basic_data.signature = dd.signature;
		served_orgs = dd.served_orgs;
	}
	/**
	 * Simple presence of parameter isGID signals a GID
	 * @param peerGID
	 * @param isGID
	 * @param failOnNew
	 * @throws P2PDDSQLException
	 */
	public void init(String peerGID, int isGID, boolean failOnNew) throws P2PDDSQLException{
		if(DEBUG) System.out.println("D_PeerAddress: init: GID");
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID+"=?;";
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerGID}, DEBUG);
		if(p.size()==0){
			if(failOnNew) throw new RuntimeException("Absent peer "+peerGID);
			this.component_basic_data.globalID = peerGID;
			this.component_basic_data.globalIDhash = getGIDHashFromGID(peerGID);
		}else{
			init(p.get(0));
		}
	}
	/**
	 * 
	 * @param GIDhash
	 * @param failOnNew
	 * @param dummy
	 * @throws P2PDDSQLException 
	 */
	private D_PeerAddress(String GIDhash, boolean failOnNew, int dummy) throws P2PDDSQLException{
		inithash(GIDhash, dummy, failOnNew);
	}
	public void inithash(String peerGIDhash, int isGID, boolean failOnNew) throws P2PDDSQLException{
		if(DEBUG) System.out.println("D_PeerAddress: init: GID");
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID+"=?;";
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerGIDhash}, DEBUG);
		if(p.size()==0){
			if(failOnNew) throw new RuntimeException("Absent peer "+peerGIDhash);
			this.component_basic_data.globalID = null;
			this.component_basic_data.globalIDhash = peerGIDhash; //getGIDHashFromGID(peerGID);
		}else{
			init(p.get(0));
		}
	}
	
	private void init(ArrayList<Object> p) {
		init(p, true, true);
	}
	private void init(ArrayList<Object> p, boolean encode_addresses, boolean encode_served_orgs) {
		if(!encode_addresses)Util.printCallPath("Here we thought addresses are not needed!");
		_init(p,true,encode_served_orgs);
	}
	@SuppressWarnings("unchecked")
	private void _init(ArrayList<Object> p, boolean encode_addresses, boolean encode_served_orgs) {
		this.loaded_basics = true;
		component_basic_data.version = Util.getString(p.get(table.peer.PEER_COL_VERSION));
		component_basic_data.globalID = Util.getString(p.get(table.peer.PEER_COL_GID)); //dd.globalID;
		component_basic_data.globalIDhash = Util.getString(p.get(table.peer.PEER_COL_GID_HASH)); //dd.globalID;
		component_basic_data.name =  Util.getString(p.get(table.peer.PEER_COL_NAME)); //dd.name;
		component_basic_data.emails =  Util.getString(p.get(table.peer.PEER_COL_EMAILS));
		component_basic_data.phones =  Util.getString(p.get(table.peer.PEER_COL_PHONES));
		component_basic_data.slogan = Util.getString(p.get(table.peer.PEER_COL_SLOGAN)); //dd.slogan;
		component_basic_data.creation_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_CREATION)));
		component_basic_data.picture = Util.byteSignatureFromString(Util.getString(p.get(table.peer.PEER_COL_PICTURE)));
		component_basic_data.broadcastable = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_BROADCAST)), false);
		component_basic_data.hash_alg = Util.getString(p.get(table.peer.PEER_COL_HASH_ALG));
		component_basic_data.revoked = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_REVOKED)), false);
		component_basic_data.revokation_instructions = Util.getString(p.get(table.peer.PEER_COL_REVOK_INSTR));
		component_basic_data.revokation_GIDH = Util.getString(p.get(table.peer.PEER_COL_REVOK_GIDH));
		signature_alg = D_PeerAddress.getHashAlgFromString(component_basic_data.hash_alg);
		component_basic_data.signature = Util.byteSignatureFromString(Util.getString(p.get(table.peer.PEER_COL_SIGN)));
		component_basic_data.plugin_info =  Util.getString(p.get(table.peer.PEER_COL_PLUG_INFO));
		
		this.loaded_locals = true;
		peer_ID = Util.getString(p.get(table.peer.PEER_COL_ID));
		_peer_ID = Util.lval(peer_ID, -1);
		
		if(encode_served_orgs)
			try {
				served_orgs = getPeerOrgs(_peer_ID);
				served_orgs_orig = served_orgs.clone();
				this.loaded_served_orgs = true;
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		if(encode_addresses){ //address = TypedAddress.getPeerAddresses(peer_ID);
			// addresses always needed
			address = TypedAddress.loadPeerAddresses(peer_ID);
			this.address_orig = TypedAddress.deep_clone(address);
			this.loaded_addresses = true;
		}

		try {
			instances = data.D_PeerInstance.loadInstancesToHash(peer_ID);
			instances_orig = D_PeerInstance.deep_clone(instances);
			this.loaded_instances = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		component_local_agent_state.arrival_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_ARRIVAL)));
		component_local_agent_state.plugins_msg = Util.getString(p.get(table.peer.PEER_COL_PLUG_MSG));
		component_local_agent_state.last_sync_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_LAST_SYNC)));
		component_local_agent_state.last_reset = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_LAST_RESET)), null);
		component_local_agent_state.first_provider_peer = Util.getString(p.get(table.peer.PEER_COL_FIRST_PROVIDER_PEER));
		this.loaded_local_agent_state = true;

		component_preferences.preferences_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_PREFERENCES_DATE)));
		//no_update = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_NOUPDATE)), false);
		component_preferences.used = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_USED)), false);
		component_preferences.blocked = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_BLOCKED)), false);
		component_preferences.filtered = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_FILTERED)), false);
		component_preferences.hidden = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_HIDDEN)), false);
		component_preferences.name_verified = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_VER_NAME)), false);
		component_preferences.email_verified = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_VER_EMAIL)), false);
		component_preferences.category = Util.getString(p.get(table.peer.PEER_COL_CATEG));
		this.loaded_local_preferences = true;
		
		experience = Util.getString(p.get(table.peer.PEER_COL_EXPERIENCE));
		exp_avg = Util.getString(p.get(table.peer.PEER_COL_EXP_AVG));
	}

	/**
	 * Used when exporting, building a request etc.
	 * @param component_basic_data.globalID
	 * @return
	 */
	public static D_PeerOrgs[] _getPeerOrgs(long peer_ID) {
		D_PeerOrgs[] result = null;
		if(peer_org.DEBUG) System.out.println("\n************\npeer_org: getPeerOrgs: enter "+peer_ID);
		try {
			result = getPeerOrgs(peer_ID);
//			String local_peer_ID = ""+peer_ID;
//			String orgs = getPeerOrgs(local_peer_ID, null);
//			result = peer_org.peerOrgsFromString(orgs);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if(peer_org.DEBUG) System.out.println("\n************\npeer_org: getPeerOrgs: result "+result);
		return result;
	}
	public static D_PeerOrgs[] getPeerOrgs(long _peer_ID) throws P2PDDSQLException {
		String peer_ID = Util.getStringID(_peer_ID);
		if(DEBUG) System.out.println("peer_org: getPeerOrgs: "+peer_ID);
		D_PeerOrgs[] result = null;
		String queryOrgs = "SELECT o."+
					table.organization.global_organization_ID+
					", o."+table.organization.name+", po."+table.peer_org.last_sync_date+
					", o."+table.organization.global_organization_ID_hash+
					", po."+table.peer_org.served+
					", po."+table.peer_org.organization_ID+
				" FROM "+table.peer_org.TNAME+" AS po " +
				" LEFT JOIN "+table.organization.TNAME+" AS o ON (po."+table.peer_org.organization_ID+"==o."+table.organization.organization_ID+") " +
				" WHERE ( po."+table.peer_org.peer_ID+" == ? ) "+
				" AND po."+table.peer_org.served+"== '1'"+
				" ORDER BY o."+table.organization.global_organization_ID;
		ArrayList<ArrayList<Object>>p_data = Application.db.select(queryOrgs, new String[]{peer_ID}, DEBUG);
		result = new D_PeerOrgs[p_data.size()];
		for(int i=0; i < p_data.size(); i++) {
			ArrayList<Object> o = p_data.get(i);
			result[i] = new D_PeerOrgs();
			result[i].global_organization_ID = Util.getString(o.get(0));
			result[i].org_name = Util.getString(o.get(1));
			result[i].last_sync_date = Util.getString(o.get(2));
			//result[i].global_organization_IDhash = Util.getString(o.get(3));
			result[i].served = Util.stringInt2bool(o.get(4), false);
			result[i].organization_ID = Util.lval(o.get(5), -1);
			
			//String name64 = util.Base64Coder.encodeString(name);
			//String global_org_ID = Util.getString(o.get(0))+peer_org.ORG_NAME_SEP+name64;
			//if(DEBUG) System.out.println("peer_org: getPeerOrgs: next ="+global_org_ID);
			//if(orgs!=null){orgs.add(global_org_ID);}
			//if(null==result) result = global_org_ID;
			//else result = result+peer_org.ORG_SEP+global_org_ID;
			
		}
		if(DEBUG) System.out.println("peer_org: getPeerOrgs: result ="+result);
		return result;
	}

	/**
	 * Used when exporting, building a request etc.
	 * @param globalID
	 * @return
	 */
	public static D_PeerOrgs[] _getPeerOrgs(String globalID) {
		D_PeerOrgs[] result = null;
		if(peer_org.DEBUG) System.out.println("\n************\npeer_org: getPeerOrgs: enter "+Util.trimmed(globalID));
		try {
			long peer_ID = _getLocalPeerIDforGID(globalID);
			return _getPeerOrgs(peer_ID);
			/*
			String local_peer_ID = peer_ID+"";
			String orgs = UpdatePeersTable.getPeerOrgs(local_peer_ID, null);
			result = UpdatePeersTable.peerOrgsFromString(orgs);
			*/
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if(peer_org.DEBUG) System.out.println("\n************\npeer_org: getPeerOrgs: result "+result);
		return result;
	}

	/**
	 * Prepare peer_orgs for sync answer
	 * @param peer_ID
	 * @param orgs : An output (if non null), gathering organization_IDs in an ArrayList
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String getPeerOrgs(String peer_ID, ArrayList<String> orgs) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if(peer_org.DEBUG) System.out.println("peer_org: getPeerOrgs: "+peer_ID+" orgs="+Util.concat(orgs," ","NULL"));
		String result = null;
		String queryOrgs = "SELECT o."+
					table.organization.global_organization_ID+
					", o."+table.organization.name+", po."+table.peer_org.last_sync_date+
				" FROM "+table.peer_org.TNAME+" AS po " +
				" LEFT JOIN "+table.organization.TNAME+" AS o ON (po."+table.peer_org.organization_ID+"==o."+table.organization.organization_ID+") " +
				" WHERE ( po."+table.peer_org.peer_ID+" == ? ) "+
				" AND po."+table.peer_org.served+"== '1'"+
				" ORDER BY o."+table.organization.global_organization_ID;
		ArrayList<ArrayList<Object>>p_data = Application.db.select(queryOrgs, new String[]{peer_ID}, peer_org.DEBUG);
		for(ArrayList<Object> o: p_data) {
			String name = ""+Util.getString(o.get(1)); //make "null" for null
			String name64 = util.Base64Coder.encodeString(name);
			String global_org_ID = Util.getString(o.get(0))+peer_org.ORG_NAME_SEP+name64;
			if(peer_org.DEBUG) System.out.println("peer_org: getPeerOrgs: next ="+global_org_ID);
			if(orgs!=null){
					orgs.add(global_org_ID);
			}
			if(null==result) result = global_org_ID;
			else result = result+peer_org.ORG_SEP+global_org_ID;
		}
		if(peer_org.DEBUG) System.out.println("peer_org: getPeerOrgs: result ="+result);
		return result;
	}

	/**
	 * 			" "+
			global_peer_ID+","+
			global_peer_ID_hash+","+
			peer_ID+","+
			name+","+
			slogan+","+
			hash_alg+","+
			signature+","+
			creation_date+","+
			broadcastable+","+
			no_update+","+
			plugin_info+","+
			plugins_msg+","+
			filtered+","+
			last_sync_date+","+
			arrival_date+","+
			used+","+
			picture+","+
			exp_avg+","+
			experience
	 * @param global_peer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static ArrayList<Object> getD_PeerAddress_Info(String global_peer_ID) throws P2PDDSQLException {
		ArrayList<Object> result=null;
		if(DEBUG) System.err.println("UpdateMessages:get_peer_only: for: "+Util.trimmed(global_peer_ID));
		String sql = "SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID+" = ?;";
		ArrayList<ArrayList<Object>> dt=Application.db.select(sql, new String[]{global_peer_ID});
		if((dt.size()>=1) && (dt.get(0).size()>=1)) {
			result = dt.get(0); // Long.parseLong(dt.get(0).get(0).toString());
			//if(DEBUG) err.println("Client: found peer_ID "+result+" for: "+Util.trimmed(global_peer_ID));	
		}
		if(DEBUG) System.err.println("UpdateMessages:get_peer_only: exit: "+result);
		return result;
	}
	/**
	 * version, globalID, name, slogan, creation_date, address*, broadcastable, signature_alg, served_orgs, signature*
	 */
	public Encoder getEncoder(){
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(component_basic_data.version,false));
		enc.addToSequence(new Encoder(component_basic_data.globalID).setASN1Type(Encoder.TAG_PrintableString));
		if(component_basic_data.name!=null)enc.addToSequence(new Encoder(component_basic_data.name,Encoder.TAG_UTF8String));
		if(component_basic_data.slogan!=null)enc.addToSequence(new Encoder(component_basic_data.slogan,DD.TAG_AC0));
		if(component_basic_data.emails!=null)enc.addToSequence(new Encoder(component_basic_data.emails,DD.TAG_AC1));
		if(component_basic_data.phones!=null)enc.addToSequence(new Encoder(component_basic_data.phones,DD.TAG_AC2));
		if(instance!=null)enc.addToSequence(new Encoder(instance,DD.TAG_AC3));
		if(component_basic_data.creation_date!=null)enc.addToSequence(new Encoder(component_basic_data.creation_date));
		if(address!=null)enc.addToSequence(Encoder.getEncoder(address));
		enc.addToSequence(new Encoder(component_basic_data.broadcastable));
		if(signature_alg!=null)enc.addToSequence(Encoder.getStringEncoder(signature_alg, Encoder.TAG_PrintableString));
		if((served_orgs!=null)&&(served_orgs.length>0))enc.addToSequence(Encoder.getEncoder(this.served_orgs).setASN1Type(DD.TAG_AC12));
		enc.addToSequence(new Encoder(component_basic_data.signature));
		enc.setASN1Type(TAG);
		return enc;
	}
	public D_PeerAddress decode(Decoder dec) throws ASN1DecoderFail{
		Decoder content=dec.getContent();
		component_basic_data.version=content.getFirstObject(true).getString();
		component_basic_data.globalID=content.getFirstObject(true).getString();
		if(content.getTypeByte() == Encoder.TAG_UTF8String)component_basic_data.name = content.getFirstObject(true).getString();else component_basic_data.name=null;
		if(content.getTypeByte() == DD.TAG_AC0)component_basic_data.slogan = content.getFirstObject(true).getString();else component_basic_data.slogan = null;
		if(content.getTypeByte() == DD.TAG_AC1)component_basic_data.emails = content.getFirstObject(true).getString();else component_basic_data.emails = null;
		if(content.getTypeByte() == DD.TAG_AC2)component_basic_data.phones = content.getFirstObject(true).getString();else component_basic_data.phones = null;
		if(content.getTypeByte() == DD.TAG_AC3) {
			instance = content.getFirstObject(true).getString();
			if (!instances.containsKey(instance)) {
				D_PeerInstance inst = new D_PeerInstance(instance);
				instances.put(instance, inst);
				this.dirty_instances = true;
			}
		} else {
			instance = null;
		}
//		try {
//			instances = data.D_PeerInstance.loadInstancesToHash(peer_ID);
//			this.loaded_instances = true;
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
		
		if(content.getTypeByte() == Encoder.TAG_GeneralizedTime)
			component_basic_data.creation_date = content.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		else component_basic_data.creation_date = null;
		if(content.getTypeByte() == Encoder.TYPE_SEQUENCE)
			address = content.getFirstObject(true)
			.getSequenceOf(Encoder.TYPE_SEQUENCE,
				new TypedAddress[]{}, new TypedAddress());
		else address=null;
		//if(content.getTypeByte() == Encoder.TAG_BOOLEAN)
		component_basic_data.broadcastable = content.getFirstObject(true).getBoolean();
		if(content.getTypeByte() == Encoder.TYPE_SEQUENCE)
			signature_alg=content.getFirstObject(true)
			.getSequenceOf(Encoder.TAG_PrintableString);
		if(content.getTypeByte() == DD.TAG_AC12)
			served_orgs = content.getFirstObject(true)
			.getSequenceOf(Encoder.TYPE_SEQUENCE,
					new D_PeerOrgs[]{}, new D_PeerOrgs());
		if(content.getTypeByte() == Encoder.TAG_OCTET_STRING) 
			component_basic_data.signature = content.getFirstObject(true).getBytes();
		return this;
	}
	public byte[] sign(ciphersuits.SK sk){
		//boolean DEBUG = true;
		component_basic_data.version = DEFAULT_VERSION;
		TypedAddress[] addr = address;
		byte[] pict = component_basic_data.picture;
		component_basic_data.signature = new byte[0];
		address = TypedAddress.getOrderedCertifiedTypedAddresses(addr);
		String old_instance = instance; instance = null;
		component_basic_data.picture=null;
		signature_alg = SR.HASH_ALG_V1;
		if(DEBUG)System.err.println("PeerAddress: sign: peer_addr="+this);
		byte msg[] = this.getSignatureEncoder().getBytes();
		if(DEBUG)System.out.println("PeerAddress: sign:\n\t msg["+msg.length+"]="+Util.byteToHex(msg)+"\n\t hash="+Util.getGID_as_Hash(msg));
		component_basic_data.signature = Util.sign(msg,  sk);
		component_basic_data.picture = pict;
		address = addr;
		instance = old_instance;
		if(DEBUG)System.err.println("PeerAddress: sign: sign=["+component_basic_data.signature.length+"]"+Util.byteToHex(component_basic_data.signature, "")+" hash="+Util.getGID_as_Hash(component_basic_data.signature));
		return component_basic_data.signature;
	}
	public boolean verifySignature(ciphersuits.PK pk) {
		//boolean DEBUG = true;
		if(DEBUG)System.err.println("PeerAddress: verifySignature(pk): start this="+this);
		boolean result=false;
		byte[] sgn = component_basic_data.signature;
		if(DEBUG)System.err.println("PeerAddress: verifySignature:" +
				"\n\t sign=["+component_basic_data.signature.length+"]"+Util.byteToHex(component_basic_data.signature, "")+
				"\n\t hash(sign)="+Util.getGID_as_Hash(component_basic_data.signature));
		TypedAddress[] addr = address;
		String old_instance = instance; instance = null;
		byte[] pict = component_basic_data.picture;
		component_basic_data.signature=new byte[0];
		address = TypedAddress.getOrderedCertifiedTypedAddresses(addr);
		component_basic_data.picture = null;
		//result = Util.verifySign(this, pk, sgn);
		byte msg[] =  this.getSignatureEncoder().getBytes();
		if(DEBUG)System.out.println("PeerAddress: verifySignature(pk): Will check: this" +
				"\n\t msg["+msg.length+"]="+Util.byteToHex(msg)+
				"\n\t hash(msg)="+Util.getGID_as_Hash(msg));
		try{
			result = Util.verifySign(msg, pk, sgn);
		}catch(Exception e){
			e.printStackTrace();
		}
		if(result==false){ if(DEBUG||DD.DEBUG_TODO)System.out.println("PeerAddress:verifySignature(pk): Failed verifying: "+this+
				"\n sgn="+Util.byteToHexDump(sgn,":")+Util.getGID_as_Hash(sgn)+
				"\n msg="+Util.byteToHexDump(msg,":")+Util.getGID_as_Hash(msg)+
				"\n pk="+pk
				);
		}else{ if(DEBUG)System.out.println("PeerAddress:verifySignature(pk):Success verifying: "+this+
				"\n sgn="+Util.byteToHexDump(sgn,":")+"\n\t hash(sgn)="+Util.getGID_as_Hash(sgn)+
				"\n msg="+Util.byteToHexDump(msg,":")+"\n\t hash(msg)="+Util.getGID_as_Hash(msg)+
				"\n pk="+pk
				);
		}
		component_basic_data.picture = pict;
		address = addr;
		component_basic_data.signature=sgn;
		instance = old_instance;
		return result;
	}
	/**
	 * Verifying with SK=this.globalID
	 * @return
	 */
	public boolean verifySignature() {
		//boolean DEBUG = true;
		if(DEBUG)System.err.println("PeerAddress: verifySignature: start: peer_addr="+this+"\n\n");
		PK c = ciphersuits.Cipher.getPK(component_basic_data.globalID);
		if(DEBUG)System.err.println("PeerAddress: verifySignature: pk="+c+"\n\n");
		boolean result = verifySignature(c);
		if(DEBUG)System.err.println("PeerAddress: verifySignature: got="+result+"\n\n");
		return result;
	}
	static public long _getLocalPeerIDforGID(String peerGID) throws P2PDDSQLException{
		long result=-1;
		String adr = getLocalPeerIDforGID(peerGID);
		if(adr==null){
			if(DEBUG)System.out.println("DD_PeerAddress: _getLocalPeerIDforGID: null addr result = "+result);
			return result;
		}
		result = Util.lval(adr, -1);
		if(DEBUG)System.out.println("DD_PeerAddress: _getLocalPeerIDforGID: result = "+result);
		return result;
	}
	public static String getLocalPeerIDforGID(String peerGID) throws P2PDDSQLException {
		if(DEBUG)System.out.println("DD_PeerAddress: getLocalPeerIDforGID");
		String sql = "SELECT "+table.peer.peer_ID+" FROM "+table.peer.TNAME+
		" WHERE "+table.peer.global_peer_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{peerGID}, DEBUG);
		if(o.size()==0) return null;
		String result = Util.getString(o.get(0).get(0));
		if(DEBUG)System.out.println("DD_PeerAddress: getLocalPeerIDforGID: result = "+result);
		return result;
	}
	public static String getPeerGIDforID(String peerID) throws P2PDDSQLException {
		if(DEBUG)System.out.println("DD_PeerAddress: getPeerGIDforID");
		String sql = "SELECT "+table.peer.global_peer_ID+" FROM "+table.peer.TNAME+
		" WHERE "+table.peer.peer_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{peerID}, DEBUG);
		if(o.size()==0) return null;
		String result = Util.getString(o.get(0).get(0));
		if(DEBUG)System.out.println("DD_PeerAddress: getPeerGIDforID: result = "+result);
		return result;
	}
	public static String getLocalPeerIDforGIDhash(String peerGIDhash) throws P2PDDSQLException {
		String sql = 
			"SELECT "+table.peer.peer_ID+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID_hash+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{peerGIDhash}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	/**
	 * Adds a "P:" in front f the hash of the GID
	 * @param GID
	 * @return
	 */
	public static String getGIDHashFromGID(String GID){
		return "P:"+Util.getGIDhash(GID);
	}
	// These are not yet used...
	/**
	 * Checks if there is a P: in front, else generates a GIDhash with getGIDHashFromGID
	 */
	public static String getGIDHashGuess(String s) {
		if(s.startsWith("P:")) return s; // it is an external
		String hash = D_PeerAddress.getGIDHashFromGID(s);
		if(hash.length() != s.length()) return hash;
		return s;
	}
	public static long insertTemporaryGID(D_PeerAddress provider, String peerGID) throws P2PDDSQLException{
		return insertTemporaryGID(provider, peerGID, getGIDHashFromGID(peerGID));
	}
	public static long insertTemporaryGID(D_PeerAddress provider, String peerGID, String peerGIDhash) throws P2PDDSQLException{
		return insertTemporaryGID(provider, peerGID, peerGIDhash, Util.getGeneralizedTime());
	}
	public static long insertTemporaryGID(D_PeerAddress provider, String peerGID, String peerGIDhash, String crt_date) throws P2PDDSQLException{
		long result=-1;
		if(DEBUG) System.out.println("\n\n******\nPeerAddress:insertTemporaryPeerGID: start");
		if(peerGIDhash==null) peerGIDhash = getGIDHashFromGID(peerGID);
		result = Application.db.insert(table.peer.TNAME,
				new String[]{table.peer.global_peer_ID, table.peer.global_peer_ID_hash,
				table.peer.arrival_date, table.peer.first_provider_peer},
				new String[]{peerGID, peerGIDhash, crt_date, provider.peer_ID},
				DEBUG);
		if(DEBUG) System.out.println("\n\n******\nPeerAddress:insertTemporaryPeerGID: got peerID="+result);
		return result;
	}
	/**
	 * 
	 * @param creator_global_ID
	 * @param pa, if present, it is verified and stored
	 * @param crt_date
	 * @return
	 */
	public static String storePeerAndGetOrInsertTemporaryLocalForPeerGID(D_PeerAddress provider, String creator_global_ID, D_PeerAddress pa, String crt_date) {
		if(pa!=null){
			String c_ID = null;
			try {
				c_ID = storeReceived(pa, Util.getCalendar(crt_date), crt_date);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			if(creator_global_ID.equals(pa.component_basic_data.globalID)) return c_ID;
		}
		try {
			//long peer_ID = UpdateMessages.getonly_organizationID(creator_global_ID, null);
			long peer_ID = D_PeerAddress._getLocalPeerIDforGID(creator_global_ID);
			if(peer_ID>=0) return Util.getStringID(peer_ID);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		long id;
		try {
			id = D_PeerAddress.insertTemporaryGID(provider, creator_global_ID, null, crt_date);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
			return null;
		}
		return Util.getStringID(id);
	}

	/**
	 * 
	 * @param pa
	 * @param crt_date
	 * @param _crt_date 
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String storeReceived(D_PeerAddress pa, Calendar crt_date, String _crt_date) throws P2PDDSQLException {
		//try{
			return Util.getStringID(_storeReceived(pa,crt_date, _crt_date));
		//}catch(Exception e){e.printStackTrace(); throw e;}
	}
	/**
	 * 
	 * @param pa
	 * @param crt_date
	 * @param _crt_date 
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long _storeReceived(D_PeerAddress pa, Calendar crt_date, String _crt_date) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if(DEBUG) System.err.println("D_PeerAddress: store: start");
		if((pa == null) || (pa.component_basic_data.globalID == null)){
			if(_DEBUG) System.err.println("D_PeerAddress: store: null "+pa);
			return -1;
		}
		D_PeerAddress local = new D_PeerAddress(pa.component_basic_data.globalID, 0, false);
		if(local.peer_ID==null)
			if(DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME) return -1;
		
		if(!pa.verifySignature()){
			System.err.println("D_PeerAddress:store: Error verifying peer: "+pa);
			//return D_PeerAddress.getLocalPeerIDforGID(pa.globalID);
			return local._peer_ID;
		}
		local.setRemote(pa);
		return local._storeVerified(crt_date, _crt_date);
	}
	public void setRemote(D_PeerAddress pa) {
		this.set(pa.component_basic_data.globalID, pa.component_basic_data.name, pa.component_basic_data.emails, pa.component_basic_data.phones, pa.component_local_agent_state.arrival_date, pa.component_basic_data.slogan,
				this.component_preferences.used, pa.component_basic_data.broadcastable, pa.component_basic_data.hash_alg, pa.component_basic_data.signature,
				pa.component_basic_data.creation_date, pa.component_basic_data.picture, pa.component_basic_data.version, pa.component_preferences.preferences_date, pa.address);
		this.address = pa.address;
		this.served_orgs = pa.served_orgs;
	}
	
	public void setLocals(D_PeerAddress la){
		this.component_preferences.used = la.component_preferences.used;
		this._peer_ID = la._peer_ID;
		this.exp_avg = la.exp_avg;
		this.experience = la.experience;
		this.component_preferences.filtered = la.component_preferences.filtered;
		this.component_local_agent_state.last_sync_date = la.component_local_agent_state.last_sync_date;
		//this.no_update = la.no_update;
		this.peer_ID = la.peer_ID;
		this.component_basic_data.plugin_info = la.component_basic_data.plugin_info;
		this.component_local_agent_state.plugins_msg = la.component_local_agent_state.plugins_msg;
		this.component_preferences.used = la.component_preferences.used;
		this.component_preferences.blocked = la.component_preferences.blocked;
		this.component_local_agent_state.last_reset = la.component_local_agent_state.last_reset;
		//this.signature_alg = la.signature_alg; // do not use local hash_alg
		if(this.component_basic_data.picture==null) this.component_basic_data.picture = la.component_basic_data.picture;
	}
	/**
	 * Store after loading new values
	 * @param global_peer_ID
	 * @param name
	 * @param adding_date
	 * @param slogan
	 * @param used
	 * @param broadcastable
	 * @param hash_alg
	 * @param signature
	 * @param creation_date
	 * @param picture
	 * @param version
	 * @param served_orgs2
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long storeVerified(String global_peer_ID, String name, String emails, String phones, String adding_date, String slogan,
			boolean used, boolean broadcastable, String hash_alg, byte[] signature, String creation_date, byte[] picture, String version, D_PeerOrgs[] served_orgs2,
			TypedAddress[] _a, boolean existing[]) throws P2PDDSQLException {
		// boolean DEBUG = true;
		if(DEBUG) System.err.println("D_PeerAddress: storeVerified: ");
		//long result_peer_ID=-1;
		D_PeerAddress dpa = new D_PeerAddress(global_peer_ID, 0, false);

		String old_creation_date = Encoder.getGeneralizedTime(dpa.component_basic_data.creation_date);
		if((creation_date == null) ||
				((old_creation_date!=null)&&(creation_date.compareTo(old_creation_date) <= 0)) ||
				(
						Util.equalStrings_null_or_not(dpa.component_basic_data.name,name)
						&& Util.equalStrings_null_or_not(dpa.component_basic_data.slogan,slogan)
						&& (dpa.component_basic_data.broadcastable == broadcastable)
						&& Util.equalStrings_null_or_not(dpa.component_basic_data.hash_alg,hash_alg)
				//&& Util.eq(dpa.signature,sign)
				//&& Util.eq(dpa.creation_date.toString(),creation_date)
				//&& Util.eq(dpa.signature,picture_str)
				)){
			if(DEBUG) System.err.println("D_PeerAddress: storeVerified: do not change!");
			existing[0] = true;
			return dpa._peer_ID;
		}
		existing[0] = false;
		dpa.set( global_peer_ID,  name, emails, phones, adding_date,  slogan,
				 used,  broadcastable,  hash_alg,  signature,  creation_date,
				 picture,  version, null, _a);
		dpa.served_orgs = served_orgs2;
		if(DEBUG) System.err.println("D_PeerAddress: storeVerified: will save!");
		return dpa._storeVerified(adding_date);
	}
	// public String storeVerified() throws P2PDDSQLException{return storeVerified(Util.getGeneralizedTime());}
	public long _storeVerified() throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified()");
		long r = _storeVerified(Util.CalendargetInstance());
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(): done:"+r);
		return r;
	}
	public String storeVerifiedForce() throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: storeVerified()");
		return Util.getStringID(_storeVerifiedForce(Util.CalendargetInstance()));
	}
	/**
	 * Set the arrival date to now
	 * @return
	 * @throws P2PDDSQLException
	 */
	public String storeVerified() throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: storeVerified()");
		return Util.getStringID(_storeVerified(Util.CalendargetInstance()));
	}
	public String storeVerified(String crt_date) throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: storeVerified(date)");
		return Util.getStringID(_storeVerified(crt_date));
	}
	public long _storeVerified(String crt_date) throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(date)");
		Calendar _crt_date = Util.getCalendar(crt_date);
		return _storeVerified(_crt_date, crt_date);
	}
	/**
	 * No save forcing
	 * @param _crt_date
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long _storeVerified(Calendar _crt_date) throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(calendar)");
		String crt_date = Encoder.getGeneralizedTime(_crt_date);
		long r = _storeVerified(_crt_date, crt_date);
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(calendar): got="+r);
		return r;
	}
	/**
	 * Set force to true
	 * @param _crt_date
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long _storeVerifiedForce(Calendar _crt_date) throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(calendar)");
		String crt_date = Encoder.getGeneralizedTime(_crt_date);
		return _storeVerified(_crt_date, crt_date, true);
	}	
	/**
	 * Saving not forced
	 * @param _crt_date
	 * @param crt_date
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long _storeVerified(Calendar _crt_date, String crt_date) throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(calendar, str): start");
		long r = _storeVerified(_crt_date, crt_date, false);
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(calendar, str): done");
		return r;
	}
	/**
	 * @param _crt_date
	 * @param crt_date
	 * @param force : save even if it is not newer ... ?
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long _storeVerified(Calendar _crt_date, String crt_date, boolean force) throws P2PDDSQLException{
		if (force) this.set_dirty_all(true);
		setArrivalTime(_crt_date, crt_date);
		return storeAct();
	}
	public void setArrivalTime(Calendar _crt_date, String crt_date) {
		if (_crt_date != null) {
			this.component_local_agent_state.arrival_date = _crt_date;
			if (crt_date == null) {
				this.component_local_agent_state.arrival_date_str = Encoder.getGeneralizedTime(_crt_date);
				return;
			}
		}
		if (crt_date != null) {
			this.component_local_agent_state.arrival_date_str = crt_date;
			if (_crt_date == null) {
				this.component_local_agent_state.arrival_date = Util.getCalendar(crt_date);
				return;
			}
		}
	}
	public String getArrivalTimeStr() {
		return this.component_local_agent_state.arrival_date_str;
	}
	public Calendar getArrivalTime() {
		return this.component_local_agent_state.arrival_date;
	}
	/**
	 * This can be delayed saving
	 * @return
	 */
	public void storeAsynchronouslyNoException() {
		try {
			storeAct();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return;
	}
	/**
	 * 
	 * The actual saving function where everything happens :)
	 * @return
	 * @throws P2PDDSQLException 
	 */
	public long storeAct() throws P2PDDSQLException {
		//boolean DEBUG=true;
		if (DEBUG) System.err.println("D_PeerAddress: _storeVerified(_crt_date="+getArrivalTimeStr()+") force="+this.dirty_all());
		if (DEBUG) Util.printCallPath("");
		if (DEBUG) System.err.println("D_PeerAddress: _storeVerified: this="+this);
		//this.component_local_agent_state.arrival_date = _crt_date;
		/*
		long peer_ID=D_PeerAddress.storeVerified(this.globalID, this.name, crt_date, this.slogan, false, 
				this.broadcastable, Util.concat(this.signature_alg,D_PeerAddress.SIGN_ALG_SEPARATOR), this.signature,
				Encoder.getGeneralizedTime(this.creation_date), this.picture, this.version);
		*/
		
		D_PeerAddress old = new D_PeerAddress(this.component_basic_data.globalID);
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified: old="+old);
		/**
		 * Quit if old is newer or equal
		 */
		if (!dirty_any()) {
			if ( // if old exists and signed
					(old._peer_ID > 0) 
					&& (old.component_basic_data.creation_date != null) 
					&& (old.component_basic_data.signature != null)) {
				
				if (this.component_basic_data.creation_date == null) return -1;
				int cmp_creation_date = old.component_basic_data.creation_date.compareTo(this.component_basic_data.creation_date);
				if(cmp_creation_date > 0) {
					if(DEBUG) System.err.println("D_PeerAddress: _storeVerified: newer old=: "+old+"\n vs new: "+this);
					return old._peer_ID;
				}
				this._peer_ID = old._peer_ID;
				this.peer_ID = old.peer_ID;
				if(!old_addresses_code) {
					/* // new implementation (but skipped since will not store new addresses)
					if(((cmp_creation_date==0) || identical_except_addresses(old)) && (this.address!=null)) {
						TypedAddress[] old_addr = TypedAddress.getPeerAddresses(this.peer_ID);
						TypedAddress.init_intersection(old_addr, this.address);
						TypedAddress.delete_difference(old_addr, this.address);
						TypedAddress.store_and_update(this.address, crt_date);
						if(DEBUG) System.err.println("D_PeerAddress: __storeVerified: identical: "+ old._peer_ID);
						return old._peer_ID;					
					}
					*/
					if (cmp_creation_date == 0) { // same date
						// TODO: can add extra addresses from the old/new one
						if(DEBUG) System.err.println("D_PeerAddress: _storeVerified: exit with old="+old);
						return old._peer_ID;
					}
				}else{
					   // old implementation
					if(this.address==null) Util.printCallPath("Null address:"+this);
					if(((cmp_creation_date==0) || identical_except_addresses(old)) && (this.address!=null)) {
						for(int k=0; k<this.address.length; k++) { // should be done only for addresses not in old
							if(TypedAddress.knownAddress(old.address,this.address[k].address, this.address[k].type)) continue;
							//long address_ID = 
							D_PeerAddress.get_peer_addresses_ID(this.address[k].address,
									this.address[k].type, old._peer_ID, this.getArrivalTimeStr(),
									this.address[k].certified, this.address[k].priority);
						}
						if(DEBUG) System.err.println("D_PeerAddress: __storeVerified: identical: "+ old._peer_ID);
						return old._peer_ID;
					}
				}
			}
		} else {
			if (
					(old._peer_ID > 0)
					&& (old.component_basic_data.creation_date != null)
				) {
				
				this._peer_ID = old._peer_ID;
				this.peer_ID = old.peer_ID;
			}
		}
		long new_peer_ID = doSave_except_Served_and_Addresses();
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(...): done base");

		if (!old_addresses_code) {
			if (DEBUG) System.err.println("D_PeerAddress: _storeVerified(...): !old_address_code: save addr");
			if (old.peer_ID != null) {
				if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(...): !old_address_code:oldID!=null: save addr");
				// TODO: when storage is unified, should use this
				// TypedAddress[] old_addr = this.address_orig; 
				if (DD.DEBUG_TODO) System.out.println("TODO: D_PeerAddress:storeVerified: should optimize here when storage unified!");
				TypedAddress[] old_addr = TypedAddress.loadPeerAddresses(this.peer_ID);//.getPeerAddresses(this.peer_ID);
				if (DEBUG) System.out.println("old="+Util.concat(old_addr, "#", "NULL"));
				TypedAddress.init_intersection(old_addr, this.address);
				//if (DEBUG) System.out.println("after init_intersection old=="+Util.concat(old_addr, "#", "NULL"));
				if (DEBUG) System.out.println("after init_intersection adr="+Util.concat(address, "#", "NULL"));
				TypedAddress.delete_difference(old_addr, this.address);
				//if (DEBUG) System.out.println("after delete_difference old="+Util.concat(old_addr, "#", "NULL"));
				if (DEBUG) System.out.println("after delete_difference addr="+Util.concat(address, "#", "NULL"));
				if (!DD.KEEP_UNCERTIFIED_SOCKET_ADDRESSES)
					this.address = TypedAddress.delete_uncertified(old_addr, this.address);
				if (DEBUG) System.out.println("after delete_difference addr="+Util.concat(address, "#", "NULL"));
			}
			if (DEBUG) System.err.println("D_PeerAddress: _storeVerified(...): !old_address_code: store");
			TypedAddress.store_and_update(this.address, Util.getStringID(new_peer_ID));
			this.address_orig = TypedAddress.deep_clone(address);
		} else {
			if (DEBUG) System.err.println("D_PeerAddress: _storeVerified(...): old_address_code: save addr");
			if (this.address != null)
				if (DEBUG) System.err.println("D_PeerAddress: _storeVerified(...): old_address_code:addr: save addr");
				for (int k = 0; k < this.address.length; k ++) {
					if (
							(old.peer_ID != null)
							&& TypedAddress.knownAddress(old.address,this.address[k].address, this.address[k].type)
							)
						continue;
					//long address_ID = 
					D_PeerAddress.get_peer_addresses_ID(this.address[k].address,
							this.address[k].type, new_peer_ID, this.getArrivalTimeStr(),
							this.address[k].certified, this.address[k].priority);
				}
		}
		//long peers_orgs_ID = get_peers_orgs_ID(peer_ID, global_organizationID);
		//long organizationID = get_organizationID(global_organizationID, org_name);
		if (DEBUG) System.err.println("D_PeerAddress: _storeVerified(...): done addresses");
		if ((old.peer_ID == null) || !this.identical_served(old.served_orgs))
			D_PeerAddress.integratePeerOrgs(this.served_orgs, new_peer_ID, this.getArrivalTimeStr());
		if (DEBUG) System.err.println("D_PeerAddress: _storeVerified(...): got ID = "+new_peer_ID);
		return new_peer_ID;
		//return UpdateMessages.get_and_verify_peer_ID(pa, crt_date);
	}
	private boolean identical_except_addresses(D_PeerAddress old) {
		if(Util.equalBytes(this.component_basic_data.signature, old.component_basic_data.signature)) return true;
		if(!Util.equalStrings_null_or_not(this.component_basic_data.slogan, old.component_basic_data.slogan)) return false;
		if(!Util.equalStrings_null_or_not(this.component_basic_data.name, old.component_basic_data.name)) return false;
		if(this.component_basic_data.broadcastable != old.component_basic_data.broadcastable) return false;
		if(this.component_preferences.blocked != old.component_preferences.blocked) return false;
		if(this.component_preferences.used != old.component_preferences.used) return false;
		if(this.component_preferences.filtered != old.component_preferences.filtered) return false;
		//if(this.no_update != old.no_update) return false;
		if(!Util.equalCalendars_null_or_not(this.component_local_agent_state.last_reset, old.component_local_agent_state.last_reset)) return false;
		if(!Util.equalCalendars_null_or_not(this.component_local_agent_state.last_sync_date, old.component_local_agent_state.last_sync_date)) return false;

		return identical_served(old.served_orgs);
	}

	/**
	 * should copy last_sync_date from old
	 * @param old
	 * @return
	 */
	private boolean identical_served(D_PeerOrgs[] old) {
		boolean result = true;
		if((this.served_orgs==null)^(old==null)) return false;
		if(this.served_orgs == null) return true;
		if(this.served_orgs.length != old.length) result = false;
		D_PeerOrgs s;
		for(int i=0; i<this.served_orgs.length; i++){
			s = this.served_orgs[i];
			int j=0;
			if((i<old.length)&&
					Util.equalStrings_null_or_not(s.global_organization_ID,old[i].global_organization_ID)&&
					Util.equalStrings_null_or_not(s.global_organization_IDhash,old[i].global_organization_IDhash)
			) j=i;
			else
				for(j=0; j<old.length; j++) {
					if(Util.equalStrings_null_or_not(s.global_organization_ID,old[j].global_organization_ID)) break;
				}
			if(j>=old.length) {result = false; continue;}
			this.served_orgs[i].last_sync_date = old[j].last_sync_date;
			//if(!Util.equalStrings_null_or_not(s.global_organization_ID,old[i].global_organization_ID)) return false;
			//if(!Util.equalStrings_null_or_not(s.global_organization_IDhash,old[i].global_organization_IDhash)) return false;
			
		}
		return result;
	}

	/*
	 * 		arrival_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_ARRIVAL)));
		last_sync_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_LAST_SYNC)));
		no_update = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_NOUPDATE)), false);
		used = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_USED)), false);
		filtered = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_FILTERED)), false);
		plugin_info =  Util.getString(p.get(table.peer.PEER_COL_PLUG_INFO));
		plugins_msg = Util.getString(p.get(table.peer.PEER_COL_PLUG_MSG));
		experience = Util.getString(p.get(table.peer.PEER_COL_EXPERIENCE));
		exp_avg = Util.getString(p.get(table.peer.PEER_COL_EXP_AVG));
	 */
	private long doSave_except_Served_and_Addresses() throws P2PDDSQLException{
		//boolean DEBUG = true;
		//if(DEBUG) Util.printCallPath("Saving");
		if(DEBUG) System.out.println("\n\n***********\nD_PeerAddress: doSave: "+this);
		if(peer_ID!=null) if(_peer_ID<=0) _peer_ID = Util.lval(peer_ID, -1);
		if(_peer_ID>0) if(peer_ID == null) peer_ID = Util.getStringID(_peer_ID);
		if(peer_ID==null) {
			if(this.component_basic_data.globalID!=null) peer_ID = D_PeerAddress.getLocalPeerIDforGID(component_basic_data.globalID);
			else if(this.component_basic_data.globalIDhash!=null) peer_ID = D_PeerAddress.getLocalPeerIDforGIDhash(component_basic_data.globalIDhash);
			_peer_ID = Util.lval(peer_ID, -1);
		}
		if(component_basic_data.globalID!=null) component_basic_data.globalIDhash = D_PeerAddress.getGIDHashFromGID(component_basic_data.globalID);
		
		if(component_basic_data.hash_alg==null) component_basic_data.hash_alg = D_PeerAddress.getStringFromHashAlg(signature_alg);
		
		String params[];
		if(peer_ID==null) params = new String[table.peer.PEER_COL_FIELDS_NO_ID];
		else params = new String[table.peer.PEER_COL_FIELDS];
		params[table.peer.PEER_COL_GID]=component_basic_data.globalID;
		params[table.peer.PEER_COL_GID_HASH]=component_basic_data.globalIDhash;
		params[table.peer.PEER_COL_NAME]=component_basic_data.name;
		params[table.peer.PEER_COL_SLOGAN]=component_basic_data.slogan;
		params[table.peer.PEER_COL_EMAILS]=component_basic_data.emails;
		params[table.peer.PEER_COL_PHONES]=component_basic_data.phones;
		params[table.peer.PEER_COL_HASH_ALG]=component_basic_data.hash_alg;
		params[table.peer.PEER_COL_REVOKED]=Util.bool2StringInt(component_basic_data.revoked);
		params[table.peer.PEER_COL_REVOK_INSTR]=component_basic_data.revokation_instructions;
		params[table.peer.PEER_COL_REVOK_GIDH]=component_basic_data.revokation_GIDH;
		params[table.peer.PEER_COL_SIGN]=Util.stringSignatureFromByte(component_basic_data.signature);
		params[table.peer.PEER_COL_CREATION]=Encoder.getGeneralizedTime(component_basic_data.creation_date);
		params[table.peer.PEER_COL_BROADCAST]=Util.bool2StringInt(component_basic_data.broadcastable);
		//params[table.peer.PEER_COL_NOUPDATE]=Util.bool2StringInt(no_update);
		params[table.peer.PEER_COL_PLUG_INFO]=component_basic_data.plugin_info;
		params[table.peer.PEER_COL_PLUG_MSG]=component_local_agent_state.plugins_msg;
		params[table.peer.PEER_COL_FILTERED]=Util.bool2StringInt(component_preferences.filtered);
		params[table.peer.PEER_COL_LAST_SYNC]=Encoder.getGeneralizedTime(component_local_agent_state.last_sync_date);
		params[table.peer.PEER_COL_ARRIVAL]=Encoder.getGeneralizedTime(component_local_agent_state.arrival_date);
		params[table.peer.PEER_COL_FIRST_PROVIDER_PEER]=component_local_agent_state.first_provider_peer;
		params[table.peer.PEER_COL_PREFERENCES_DATE]=Encoder.getGeneralizedTime(component_preferences.preferences_date);
		params[table.peer.PEER_COL_LAST_RESET]=(component_local_agent_state.last_reset!=null)?Encoder.getGeneralizedTime(component_local_agent_state.last_reset):null;
		params[table.peer.PEER_COL_USED]=Util.bool2StringInt(component_preferences.used);
		params[table.peer.PEER_COL_BLOCKED]=Util.bool2StringInt(component_preferences.blocked);
		params[table.peer.PEER_COL_HIDDEN]=Util.bool2StringInt(component_preferences.hidden);
		params[table.peer.PEER_COL_VER_EMAIL]=Util.bool2StringInt(component_preferences.email_verified);
		params[table.peer.PEER_COL_VER_NAME]=Util.bool2StringInt(component_preferences.name_verified);
		params[table.peer.PEER_COL_CATEG]=component_preferences.category;
		params[table.peer.PEER_COL_PICTURE]=Util.stringSignatureFromByte(component_basic_data.picture);
		params[table.peer.PEER_COL_EXP_AVG]=exp_avg;
		params[table.peer.PEER_COL_EXPERIENCE]=experience;
		params[table.peer.PEER_COL_VERSION]=component_basic_data.version;
		if(peer_ID==null) {
			_peer_ID = Application.db.insert(table.peer.TNAME,
					Util.trimmed(table.peer.list_fields_peers_no_ID),
					params,
					DEBUG);
			peer_ID = Util.getStringID(_peer_ID);
			if(DEBUG) System.out.println("D_PeerAddress: doSave: inserted "+_peer_ID);
		} else {
			params[table.peer.PEER_COL_ID]=peer_ID;
			if("-1".equals(peer_ID))Util.printCallPath("peer_ID -1: "+this);
			Application.db.update(table.peer.TNAME,
					Util.trimmed(table.peer.list_fields_peers_no_ID),
					new String[]{table.peer.peer_ID},
					params,
					DEBUG);
			if(DEBUG) System.out.println("D_PeerAddress: doSave: updated "+peer_ID);
		}
		data.D_PeerInstance.store(peer_ID, _peer_ID, instances); // checks peer_ID and instances
		if (DEBUG) System.out.println("D_PeerAddress: doSave: return "+_peer_ID);
		return _peer_ID;
	}
	/**
	 * Called directly when saving DDAddress
	 * @param global_peer_ID
	 * @param name
	 * @param adding_date
	 * @param slogan
	 * @param used
	 * @param broadcastable
	 * @param hash_alg
	 * @param signature
	 * @param creation_date
	 * @param picture
	 * @param version
	 * @param preferences_date2 
	 */
	public void set(String global_peer_ID, String name, String emails, String phones, Calendar adding_date, String slogan,
			boolean used, boolean broadcastable, String hash_alg, byte[] signature, Calendar creation_date, byte[] picture,
			String version, Calendar preferences_date2, TypedAddress[] _a) {
		if(_a !=null )address = _a;
		this.component_basic_data.globalID = global_peer_ID;
		this.component_basic_data.name = name;
		this.component_basic_data.emails = emails;
		this.component_basic_data.phones = phones;
		this.component_local_agent_state.arrival_date = adding_date;
		this.component_preferences.preferences_date = preferences_date2;
		this.component_basic_data.slogan = slogan;
		this.component_preferences.used = used;
		this.component_basic_data.broadcastable = broadcastable;
		this.component_basic_data.hash_alg = hash_alg;
		this.signature_alg = D_PeerAddress.getHashAlgFromString(hash_alg);
		this.component_basic_data.signature = signature;
		this.component_basic_data.creation_date = creation_date;
		if(this.component_basic_data.picture == null)this.component_basic_data.picture = picture; // this condition should be removed if picture is part of signature
		this.component_basic_data.version = version;
	}
	/**
	 * Called when saving a received peer in sync request
	 * @param global_peer_ID
	 * @param name
	 * @param adding_date
	 * @param slogan
	 * @param used
	 * @param broadcastable
	 * @param hash_alg
	 * @param signature
	 * @param creation_date
	 * @param picture
	 * @param version
	 */
	public void set(String global_peer_ID, String name, String emails,
			String phones, String adding_date, String slogan,
				boolean used, boolean broadcastable, String hash_alg, byte[] signature,
				String creation_date, byte[] picture, String version,
				Calendar preferences_date, TypedAddress[] _a) {
		set(global_peer_ID, name, emails, phones, Util.getCalendar(adding_date),
				slogan, used, broadcastable, hash_alg,
				signature, Util.getCalendar(creation_date), picture, version, preferences_date, _a);
	}
	/*		
	public static long storeVerified(String global_peer_ID, String name, String adding_date, String slogan,
			boolean used, boolean broadcastable, String hash_alg, byte[] signature, String creation_date, byte[] picture, String version) throws P2PDDSQLException {
		// boolean DEBUG = true;
		long result=-1;
		String broadcast = broadcastable?"1":"0";
		if(DEBUG) System.err.println("UpdateMessages:get_peer_ID: for: "+Util.trimmed(global_peer_ID)+" new slogan="+slogan+" broadcast="+broadcast+" hash_alg="+hash_alg+" creation_date="+creation_date);
		String sql = "SELECT "+table.peer.fields_peers+
				" FROM "+table.peer.TNAME+" WHERE "+table.peer.global_peer_ID+" = ?;";
		String sign = Util.stringSignatureFromByte(signature);
		ArrayList<ArrayList<Object>> dto=Application.db.select(sql, new String[]{global_peer_ID}, DEBUG);
		if((dto.size()>=1) && (dto.get(0).size()>=1)) {
			ArrayList<Object> _dt = dto.get(0);
			result = Long.parseLong(_dt.get(table.peer.PEER_COL_ID).toString());
			if(DEBUG) System.err.println("UpdateMessages:get_peer_ID: found peer_ID "+result+" for: "+Util.trimmed(global_peer_ID));
			String picture_str = null;
			if(picture!=null) picture_str = Util.byteToHex(picture);
			if(DEBUG) System.err.println("UpdateMessages:get_peer_ID: got "
					+_dt.get(table.peer.PEER_COL_ID)+","+_dt.get(table.peer.PEER_COL_NAME)+","
					+_dt.get(table.peer.PEER_COL_SLOGAN)+","+_dt.get(table.peer.PEER_COL_BROADCAST)+","
					+_dt.get(table.peer.PEER_COL_HASH_ALG)+","+_dt.get(table.peer.PEER_COL_SIGN)+","+_dt.get(table.peer.PEER_COL_PICTURE));
			String old_creation_date = Util.getString(_dt.get(table.peer.PEER_COL_CREATION));
			if((creation_date == null) ||
					((old_creation_date!=null)&&(creation_date.compareTo(old_creation_date) <= 0)) ||
					(
							Util.eq(Util.getString(_dt.get(table.peer.PEER_COL_NAME)),name)
							&& Util.eq(Util.getString(_dt.get(table.peer.PEER_COL_SLOGAN)),slogan)
							&& Util.eq(Util.getString(_dt.get(table.peer.PEER_COL_BROADCAST)),broadcast)
							&& Util.eq(Util.getString(_dt.get(table.peer.PEER_COL_HASH_ALG)),hash_alg)
					//&& Util.eq(_dt.get(table.peer.PEER_COL_SIGN).toString(),sign)
					//&& Util.eq(_dt.get(table.peer.PEER_COL_CREATION).toString(),creation_date)
					//&& Util.eq(_dt.get(table.peer.PEER_COL_SIGNATURE).toString(),picture_str)
					))
				return result;
			if(picture_str == null) picture_str = Util.getString(_dt.get(table.peer.PEER_COL_PICTURE));
			if (DEBUG)
					System.out.println("UpdateMessages:get_peer_IDChanged \""+_dt.get(table.peer.PEER_COL_NAME)+"\" to \""+name+"\" or \""+(String)_dt.get(table.peer.PEER_COL_SLOGAN)+"\" to \""+slogan+"\"");
			Application.db.update(table.peer.TNAME,
					new String[]{table.peer.name,table.peer.slogan,table.peer.arrival_date,
								table.peer.broadcastable, table.peer.hash_alg, table.peer.signature,
								table.peer.picture, table.peer.creation_date, table.peer.version},
					new String[]{table.peer.peer_ID},
					new String[]{name, slogan, adding_date,
								broadcast, hash_alg, sign, picture_str,creation_date, Util.getStringID(result), version},
					DEBUG);
			if(DEBUG) System.err.println("UpdateMessages:get_peer_ID: Updated slogan");
			return result;
		}
		String IDhash = Util.getGIDhash(global_peer_ID);
		result=Application.db.insert(table.peer.TNAME,
				new String[]{table.peer.global_peer_ID,table.peer.global_peer_ID_hash,table.peer.name,table.peer.arrival_date,table.peer.slogan, table.peer.used, table.peer.broadcastable, table.peer.hash_alg, table.peer.signature, table.peer.version},
				new String[]{global_peer_ID, IDhash, name, adding_date, slogan, used?"1":"0", broadcastable?"1":"0", hash_alg, sign, version},
				DEBUG);
		if(DEBUG) System.err.println("UpdateMessages:get_peer_ID: new peer_ID "+result+" for: "+Util.trimmed(global_peer_ID));
		return result;
	}
*/
	/**
	 * update signature
	 * @param peer_ID should exist
	 * @param signer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_PeerAddress readSignSave(long peer_ID, long signer_ID, boolean trim) throws P2PDDSQLException {
		D_PeerAddress w=new D_PeerAddress(Util.getStringID(peer_ID), true);
		if(trim)w.address = TypedAddress.trimTypedAddresses(w.address, 2);
		ciphersuits.SK sk = util.Util.getStoredSK(w.component_basic_data.globalID);
		w.component_basic_data.creation_date = Util.CalendargetInstance();
		w.sign(sk);
		w._storeVerified();
		return w;
	}
	public D_PeerAddress instance() throws CloneNotSupportedException{
		return new D_PeerAddress();
	}


	/**
	 * Integrate the served_orgs from peer_ID at date adding_date
	 * after deleting all old served ones (sets all old orgs to served = 0)
	 *  and updates the peer date
	 * called from _storeVerified and wireless
	 * 
	 * should be fixed to preserve last_sync_date field !!!!!!
	 * 
	 * @param served_orgs
	 * @param peer_ID
	 * @param adding_date
	 * @throws P2PDDSQLException
	 */
	public static void integratePeerOrgs(D_PeerOrgs[] served_orgs, long peer_ID, String adding_date) throws P2PDDSQLException{
		//boolean DEBUG=true;
		if(DEBUG) System.err.println("D_PeerAddress:integratePeerOrgs: START: ");
		//String adding_date;
		if(served_orgs==null){
			if(DEBUG) out.println("D_PeerAddress:integratePeerOrgs: EXIT null");
			return;
		}
		Application.db.delete(table.peer_org.TNAME,
				new String[]{table.peer_org.peer_ID},
				new String[]{Util.getStringID(peer_ID)}, DEBUG);
//		Application.db.update(table.peer_org.TNAME,
//				new String[]{table.peer_org.served},
//				new String[]{table.peer_org.peer_ID},
//				new String[]{"0", Util.getStringID(peer_ID)}, DEBUG);
		for(int o=0; o<served_orgs.length; o++) {
			String global_organizationID = served_orgs[o].global_organization_ID;
			String org_name = served_orgs[o].org_name;
			if(global_organizationID!=null) {
				//adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
				long organizationID = UpdateMessages.get_organizationID(global_organizationID, org_name, adding_date,null);
				//adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
				//long peers_orgs_ID = get_peers_orgs_ID(peer_ID, organizationID, adding_date);
				Application.db.insert(table.peer_org.TNAME,
						new String[]{table.peer_org.peer_ID, table.peer_org.organization_ID, table.peer_org.served, table.peer_org.last_sync_date},
						new String[]{Util.getStringID(peer_ID), Util.getStringID(organizationID), "1", served_orgs[o].last_sync_date}, DEBUG);
			}
		}
//		Application.db.delete(table.peer_org.TNAME,
//				new String[]{table.peer_org.served,
//				table.peer_org.peer_ID},
//				new String[]{"0", Util.getStringID(peer_ID)}, DEBUG);
		Application.db.update(table.peer.TNAME,
				new String[]{table.peer.arrival_date},
				new String[]{table.peer.peer_ID},
				new String[]{adding_date, Util.getStringID(peer_ID)}, DEBUG);
		if(DEBUG) out.println("D_PeerAddress:integratePeerOrgs: DONE #:"+served_orgs.length);
	}
	/**
	 * Private,  setting this org as served, or inserting it as served
	 *  and update the arrival-time of peer
	 *  never use
	 * @param peer_ID
	 * @param organizationID
	 * @param adding_date
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long get_peers_orgs_ID(long peer_ID, long organizationID, String adding_date) throws P2PDDSQLException {
		long result=0;
		if(DEBUG) System.err.println("\n************\nD_PeerAddress:get_peers_orgs_ID':  start peer_ID = "+peer_ID+" oID="+organizationID);		
		if((peer_ID <= 0) || (organizationID<=0))  return -1;
		String sql = "SELECT "+table.peer_org.peer_org_ID + "," + table.peer_org.served +
						" FROM "+table.peer_org.TNAME+" AS po " +
						" WHERE po."+table.peer_org.peer_ID+" = ? and po."+table.peer_org.organization_ID+" = ?";
		ArrayList<ArrayList<Object>> dt;
		dt=Application.db.select(sql, new String[]{Util.getStringID(peer_ID), Util.getStringID(organizationID)});
		if((dt.size()>=1) && (dt.get(0).size()>=1)) {
			String s_result = Util.getString(dt.get(0).get(0));
			result = Long.parseLong(s_result);
			if(!"1".equals(Util.getString(dt.get(0).get(1)))) {
					Application.db.update(table.peer_org.TNAME,
							new String[]{table.peer_org.served},
							new String[]{table.peer_org.peer_org_ID},
							new String[]{"1", s_result});
				
					Application.db.update(table.peer.TNAME,
							new String[]{table.peer.arrival_date},
							new String[]{table.peer.peer_ID},
							new String[]{adding_date, Util.getStringID(peer_ID)});
			}
		} else {
			result=Application.db.insert(table.peer_org.TNAME,
				new String[]{table.peer_org.peer_ID, table.peer_org.organization_ID, table.peer_org.served},
				new String[]{Util.getStringID(peer_ID), Util.getStringID(organizationID), "1"});
			Application.db.update(table.peer.TNAME,
				new String[]{table.peer.arrival_date},
				new String[]{table.peer.peer_ID},
				new String[]{adding_date, Util.getStringID(peer_ID)});
		}
		if(DEBUG) System.out.println("D_PeerAddress:get_peers_orgs_ID':  exit result = "+result);		
		if(DEBUG) System.out.println("****************");		
		return result;
	}

	/**
	 * Tries to get the ID, and inserts if the tuple is empty
	 * @param address
	 * @param type
	 * @param peer_ID
	 * @param adding_date
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static long _monitored_get_peer_addresses_ID(String address, String type,
			long peer_ID, String adding_date, boolean certificate, int priority) throws P2PDDSQLException{
		if(DEBUG) err.println("UpdateMessages:get_peer_addresses_ID for: "+Util.trimmed(address)+" id="+peer_ID);
		long result=0;
		if((type==null)||(address==null)){
			if(DEBUG) err.println("UpdateMessages:get_peer_addresses_ID null for: "+Util.trimmed(address)+" id="+peer_ID);
			return -1;
		}
		if("null".equals(type)||"null".equals(address)){
			if(DEBUG) err.println("UpdateMessages:get_peer_addresses_ID null for: "+Util.trimmed(address)+" id="+peer_ID);
			return -1;
		}
		//Util.printCallPath("peer");
		String sql = 
			"SELECT "+table.peer_address.peer_address_ID+","+table.peer_address.certified+","+table.peer_address.priority+
			" FROM "+table.peer_address.TNAME+
			" WHERE "+table.peer_address.peer_ID+" = ? AND "+table.peer_address.address+" = ? AND "+table.peer_address.type+" = ?;";
		ArrayList<ArrayList<Object>> dt=
			Application.db.select(sql, new String[]{Util.getStringID(peer_ID), address, type}, DEBUG);
		if(dt.size()>=1) { // && (dt.get(0).size()>=1)) {
			boolean old_c = Util.stringInt2bool(dt.get(0).get(1), false);
			int old_p = Util.get_int(dt.get(0).get(2));
			if((old_c!=certificate)||(old_p!=priority)){
				Application.db.update(table.peer_address.TNAME,
						new String[]{table.peer_address.certified, table.peer_address.priority},
						new String[]{table.peer_address.peer_address_ID},
						new String[]{Util.bool2StringInt(certificate), ""+priority, Util.getString(dt.get(0).get(0))}, DEBUG);
			}
			return Long.parseLong(Util.getString(dt.get(0).get(0)));
		}
		try{
		result=Application.db.insert(table.peer_address.TNAME,
				new String[]{table.peer_address.address,table.peer_address.type,
				table.peer_address.peer_ID,table.peer_address.arrival_date,
				table.peer_address.certified, table.peer_address.priority},
				new String[]{address, type, Util.getStringID(peer_ID), adding_date,
				Util.bool2StringInt(certificate), ""+priority}, 
				DEBUG);
		}catch(Exception e){
			e.printStackTrace();
			dt=  Application.db.select(sql, new String[]{Util.getStringID(peer_ID), address, type}, DEBUG);
			if(dt.size()>=1) {
				long r = Long.parseLong(Util.getString(dt.get(0).get(0)));
				if(DEBUG) System.out.println("D_PeerAddress:ger_peer_addresses_ID: failed:"+r);
				return r;
			}else{
				if(DEBUG) System.out.println("D_PeerAddress:ger_peer_addresses_ID: failed: ?");
				return -1;
			}
		}
		Application.db.update(table.peer.TNAME,
				new String[]{table.peer.arrival_date},
				new String[]{table.peer.peer_ID},
				new String[]{adding_date, Util.getStringID(peer_ID)});
		return result;		
	}

	static Object monitor_get_peer_addresses_ID = new Object();
	/**
	 * Tries to get the ID, and inserts if the tuple is empty
	 * @param address
	 * @param type
	 * @param peer_ID
	 * @param adding_date
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long get_peer_addresses_ID(String address, String type,
			long peer_ID, String adding_date, boolean certificate, int priority) throws P2PDDSQLException{

		synchronized(monitor_get_peer_addresses_ID){
			return _monitored_get_peer_addresses_ID(address, type, peer_ID, adding_date, certificate, priority);
		}
	}
	
	public static void createMyPeerID() throws P2PDDSQLException{
		String name = Identity.current_peer_ID.name;
		String slogan = Identity.current_peer_ID.slogan;
		createMyPeerID(new PeerInput(name, slogan, Identity.emails, null));
	}
	/**
	 * Create and set a new peerID, and its keys in "key" table and "application" table
	 * @throws P2PDDSQLException
	 */
	public static void createMyPeerID(PeerInput peerInput) throws P2PDDSQLException{
		if(DEBUG) out.println("\n*********\nDD:createMyPeerID: start");
		String addresses = Identity.current_server_addresses();

		if(peerInput.name==null) peerInput.name = System.getProperty("user.name", _("MySelf"));
		if(addresses==null) addresses = _("LocalMachine");
		
		
		ciphersuits.Cipher keys;
		//keys = Util.getKeyedGlobalID("peer", peerInput.name+"+"+addresses);
		//keys.genKey(1024);
		keys = Cipher.getCipher(
				peerInput.cipherSuite.cipher,
				peerInput.cipherSuite.hash_alg,
				peerInput.name+"+"+addresses);
		keys.genKey(peerInput.cipherSuite.ciphersize);
		if(DEBUG) out.println("DD:createMyPeerID: keys generated");
		String secret_key = Util.getKeyedIDSK(keys);
		if(DEBUG) out.println("DD:createMyPeerID: secret_key="+secret_key);
		byte[] pIDb = Util.getKeyedIDPKBytes(keys);
		String pGID=Util.getKeyedIDPK(pIDb);
		if(DEBUG) out.println("DD:createMyPeerID: public_key="+pGID);
		String pGIDhash = Util.getGIDhash(pGID);
		String pGIDname = "PEER:"+peerInput.name+":"+Util.getGeneralizedTime();
		
		DD.storeSK(keys, pGIDname, pGID, secret_key, pGIDhash);
		
		D_PeerAddress me = new D_PeerAddress();
		me.setPeerInputNoCiphersuit(peerInput);
		me.setGID(pGID, pGIDhash);
		doSetMyself(me, keys.getSK());
		//doSetMyself(pGID, secret_key, peerInput.name, peerInput.slogan, pGIDhash); //sets myself
		
		
		if(DEBUG) out.println("DD:createMyPeerID: exit");
		if(DEBUG) out.println("**************");
	}
/*
	private static void doSetMyself(String gID, String secret_key, String name, String slogan, String gIDhash) throws P2PDDSQLException{
		if(secret_key == null) {
			Util.printCallPath("GID="+gID+"\n; peerID="+secret_key+" name="+name+" hash="+gIDhash);
			Application.warning(_("We do not know the secret key of this peer!!"), _("Peer Secret Key not available!"));
			return;
		}
		DD.setMyPeerGID(gID, gIDhash);
		DD.setAppText(DD.APP_my_peer_name, name);
		DD.setAppText(DD.APP_my_peer_slogan, slogan);
		Identity.current_peer_ID.name = name;
		Identity.current_peer_ID.slogan = slogan;
		if(DD.WARN_ON_IDENTITY_CHANGED_DETECTION) {
			if(name!=null) Application.warning(_("Now you are:")+" \""+name+"\"", _("Peer Changed!"));
			else{ Application.warning(_("Now have an anonymous identity. You have to choose a name!"), _("Peer Changed!"));			
			}
		}
		//Application.warning(_("Now you are: \""+name+"\"\n with key:"+Util.trimmed(secret_key)), _("Peer Changed!"));
		Server.set_my_peer_ID_TCP(Identity.current_peer_ID); // tell directories and save crt address, setting myself
	}
*/
	private static void doSetMyself(D_PeerAddress me, SK secret_key) throws P2PDDSQLException{
		if(secret_key == null) {
			Util.printCallPath("GID="+me.getGID()+"\n; peerID="+secret_key+" name="+me.getName()+" hash="+me.getGIDH());
			Application.warning(_("We do not know the secret key of this peer!!"), _("Peer Secret Key not available!"));
			return;
		}
		DD.setMyPeerGID(me.getGID(), me.getGIDH());
		DD.setAppText(DD.APP_my_peer_name, me.getName());
		DD.setAppText(DD.APP_my_peer_slogan, me.getSlogan());
		DD.setAppText(DD.APP_my_peer_instance, me.getInstance());
		Identity.current_peer_ID.name = me.getName();
		Identity.current_peer_ID.slogan = me.getSlogan();
		Identity.current_peer_ID.instance = me.getInstance();
		if (DD.WARN_ON_IDENTITY_CHANGED_DETECTION) {
			if (me.getName() != null) Application.warning(_("Now you are:")+" \""+me.getName()+"\"", _("Peer Changed!"));
			else{ Application.warning(_("Now have an anonymous identity. You have to choose a name!"), _("Peer Changed!"));
			}
		}
		//Application.warning(_("Now you are: \""+name+"\"\n with key:"+Util.trimmed(secret_key)), _("Peer Changed!"));
		Server.set_my_peer_ID_TCP(me); // tell directories and save crt address, setting myself
	}
	public static void setMyself(String peerID) throws P2PDDSQLException {
		D_PeerAddress me = D_PeerAddress.getPeerByLID(peerID, false);
		if (me == null) {
			Util.printCallPath("D_PeerAddress:setMyself: fail peerID="+peerID);
			Application.warning(_("We do not know this peer!"), _("Peer Secret Key not available!"));
		}
		setMyself(me);
	}
	/**
	 * Searches secret _key
	 * Decides peer_instance to use (first locally created)
	 * @param me
	 * @throws P2PDDSQLException
	 */
	public static void setMyself(D_PeerAddress me) throws P2PDDSQLException {
		if (me == null) {
			Util.printCallPath("D_PeerAddress:setMyself: fail peer="+me);
		}
		SK sk = Util.getStoredSK(me.getGID(), me.getGIDH()); 
		if (sk == null)  {
			Util.printCallPath("D_PeerAddress:setMyself: fail peerID="+me);
			Application.warning(_("We do not know the secret key of this peer!"), _("Peer Secret Key not available!"));
		}
		
		//D_PeerAddress.setCurrentInstanceInDB(null);
		me.loadInstances();
		if ((me.getInstance() == null) || (!me.instances.containsKey(me.getInstance()))) {
			for (  D_PeerInstance i : me.instances.values()) {
				if (i.createdLocally()) {
					//D_PeerAddress.setCurrentInstanceInDB(i.peer_instance);
					me.setCurrentInstance(i.peer_instance);
					break;
				}
			}
		}
		doSetMyself(me, sk);
	}
	/**
	 * Only loads if the flag loaded_instances is not set (then sets it)
	 */
	public void loadInstances() {
		if(this.loaded_instances) return;

		try {
			this.instances = D_PeerInstance.loadInstancesToHash(this.peer_ID);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		this.loaded_instances = true;
	}
	/**
	 * Just sets the corresponding member
	 * @param peer_instance
	 */
	public void setCurrentInstance(String peer_instance) {
		this.instance = peer_instance;
	}
	/**
	 * 
	 * @param peerID
	 * @throws P2PDDSQLException 
	 */
	/*
	public static void _setMyself(String peerID) throws P2PDDSQLException {
		String sql = "SELECT p."+table.peer.global_peer_ID+", k."+table.key.secret_key+
		", p."+table.peer.name+", p."+table.peer.slogan+", p."+table.peer.global_peer_ID_hash+
		" FROM "+table.peer.TNAME+" AS p "+
		" LEFT JOIN "+table.key.TNAME+" AS k ON (k."+table.key.public_key+"=p."+table.peer.global_peer_ID+") "+
		" WHERE "+table.peer.peer_ID+" = ?;";
		ArrayList<ArrayList<Object>> a;
		a = Application.db.select(sql, new String[]{peerID}, DEBUG);
		String secret_key = null;
		String gID;
		
		if((a.size() > 0) || (a.get(0).get(1)!=null)) {
			gID = Util.getString(a.get(0).get(0));
			secret_key = Util.getString(a.get(0).get(1));
			String name = Util.getString(a.get(0).get(2));
			String slogan = Util.getString(a.get(0).get(3));
			String gIDhash = Util.getString(a.get(0).get(4));
			doSetMyself(gID, secret_key, name, slogan, gIDhash);
		}else{
			Util.printCallPath(sql+"; peerID="+peerID);
			Application.warning(_("We do not know the secret key of this peer!"), _("Peer Secret Key not available!"));
		}
	}
	*/
	public static byte getASN1Type() {
		return TAG;
	}
	
	/**
	 * 
	 * @param gID
	 * @param DBG
	 * @return
	 *  0 for absent,
	 *  1 for present&signed,
	 *  -1 for temporary
	 * @throws P2PDDSQLException
	 */
	public static int isGIDavailable(String gID, boolean DBG) throws P2PDDSQLException {
		String sql = 
			"SELECT "+table.peer.peer_ID+","+table.peer.signature+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID_hash+"=? ;";
			//" AND "+table.constituent.organization_ID+"=? "+
			//" AND ( "+table.constituent.sign + " IS NOT NULL " +
			//" OR "+table.constituent.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{gID}, DEBUG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_News:available: "+gID+" in "+" = "+result);
		if(a.size()==0) return 0;    
		String signature = Util.getString(a.get(0).get(1));
		if((signature!=null) && (signature.length()!=0)) return 1;
		return -1;
	}
	public static String queryName(Component win) {
		String peer_Name = Identity.current_peer_ID.name;
		String val=
			JOptionPane.showInputDialog(win,
					(peer_Name != null)?
							(_("Change Peer Name.")+"\n"+_("Previously:")+" "+peer_Name):
							(_("Dear:")+" "+System.getProperty("user.name")+"\n"+_("Select a Peer Name recognized by your peers, such as: \"John Smith\"")),
					_("Peer Name"),
					JOptionPane.QUESTION_MESSAGE);
		return val;
	}
	public static void changeMyPeerName(Component win) throws P2PDDSQLException {
		if(DEBUG)System.out.println("peer_ID: "+Identity.current_peer_ID.globalID);
		String val = queryName(win);
		if((val!=null)&&(!"".equals(val))){
			Identity.current_peer_ID.name = val;
			DD.setAppText(DD.APP_my_peer_name, val);

			if(Identity.current_peer_ID.globalID == null) {
				JOptionPane.showMessageDialog(win,
					_("You are not yet a peer.\n Start your server first!"),
					_("Peer Init"), JOptionPane.WARNING_MESSAGE);
				return;
			}

			D_PeerAddress me = D_PeerAddress.get_myself_from_Identity();
			me.component_basic_data.name = val;
			me.component_basic_data.creation_date = Util.CalendargetInstance();
			me.sign(DD.getMyPeerSK());
			me.storeVerified();
			DD.touchClient();
			if(DEBUG) System.out.println("D_PeerAddress:changeMyPeerName:Saving:"+me);
//
//			Server.update_my_peer_ID_peers_name_slogan();
//			D_PeerAddress.re_init_myself();
//			DD.touchClient();
		}else{
			if(DEBUG)System.out.println("peer_ID: Empty Val");
		}
	}

	public static void changeMyPeerSlogan(Component win) throws P2PDDSQLException {
		if(Identity.current_peer_ID.globalID==null){
			JOptionPane.showMessageDialog(win,
					_("You are not yet a peer.\n Start your server first!"),
					_("Peer Init"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(DEBUG)System.out.println("peer_ID: "+Identity.current_peer_ID.globalID);
		String peer_Slogan = Identity.current_peer_ID.slogan;
		String val=JOptionPane.showInputDialog(win, _("Change Peer Slogan.\nPreviously: ")+peer_Slogan, _("Peer Slogan"), JOptionPane.QUESTION_MESSAGE);
		if((val!=null)&&(!"".equals(val))){
			D_PeerAddress me = D_PeerAddress.get_myself_from_Identity();
			DD.setAppText(DD.APP_my_peer_slogan, val);
			me.component_basic_data.slogan = val;
			me.component_basic_data.creation_date = Util.CalendargetInstance();
			me.sign(DD.getMyPeerSK());
			me.storeVerified();
			DD.touchClient();
			if(DEBUG) System.out.println("D_PeerAddress:changeMyPeerSlodan:Saving:"+me);

//			Identity.current_peer_ID.slogan = val;
//			DD.setAppText(DD.APP_my_peer_slogan, val);
//			Server.update_my_peer_ID_peers_name_slogan();
//			D_PeerAddress.re_init_myself();
//			DD.touchClient();
		}
	}

	public static String queryEmails (Component win) throws P2PDDSQLException{
		String peer_Emails = D_PeerAddress.get_myself_from_Identity().component_basic_data.emails;
		String val=JOptionPane.showInputDialog(win, _("Change Peer Email.\nPreviously: ")+
				peer_Emails,
				_("Peer Emails"), JOptionPane.QUESTION_MESSAGE);
		return val;
	}
	public static String queryNewEmails (Component win) throws P2PDDSQLException{
		String val=JOptionPane.showInputDialog(win, _("Specify the email address where you can be verified by peers."),
				_("Peer Emails"), JOptionPane.QUESTION_MESSAGE);
		return val;
	}
	
	public static void changeMyPeerEmails(Component win) throws P2PDDSQLException {
		if(Identity.current_peer_ID.globalID==null){
			JOptionPane.showMessageDialog(win,
					_("You are not yet a peer.\n Start your server first!"),
					_("Peer Init"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(DEBUG)System.out.println("peer_ID: "+Identity.current_peer_ID.globalID);
		//String peer_Slogan = Identity.current_peer_ID.slogan;
		String val = queryEmails(win);
		if((val!=null)&&(!"".equals(val))){
			D_PeerAddress me = D_PeerAddress.get_myself_from_Identity();
			me.component_basic_data.emails = val;
			me.component_basic_data.creation_date = Util.CalendargetInstance();
			me.sign(DD.getMyPeerSK());
			me.storeVerified();
			DD.touchClient();
			if(DEBUG) System.out.println("D_PeerAddress:changeMyPeerEmails:Saving:"+me);
		}
	}
	/**
	 * Updating my name and slogan based on my global peer ID
	 * new values are taken from globals
	 */
	public static void update_my_peer_ID_peers_name_slogan_broadcastable(boolean broadcastable){
		//boolean DEBUG = true;
		if(DEBUG) out.println("BEGIN Server.update_my_peer_ID_peers_name_slogan_broadcastable");
		if(Identity.current_peer_ID.globalID==null){
			if(DEBUG)out.println("END Server.update_my_peer_ID_peers_name_slogan_broadcastable: Null ID");
			return;
		}
		try {
			String global_peer_ID = Identity.current_peer_ID.globalID;
			D_PeerAddress peer_data = D_PeerAddress.get_myself_from_Identity();
			
			// now will update potentially modified fields from globals
			peer_data.component_basic_data.globalID = global_peer_ID;
			peer_data.component_basic_data.name = Identity.current_peer_ID.name;
			peer_data.component_basic_data.slogan = Identity.current_peer_ID.slogan;
			Identity.current_identity_creation_date = peer_data.component_basic_data.creation_date = Util.CalendargetInstance();
			peer_data.signature_alg = SR.HASH_ALG_V1;
			peer_data.component_basic_data.broadcastable = broadcastable;
			peer_data.served_orgs = data.D_PeerAddress._getPeerOrgs(peer_data.component_basic_data.globalID);
			peer_data.component_basic_data.picture = Util.byteSignatureFromString(Identity.getMyCurrentPictureStr());
			if(DEBUG) out.println("Server.update_my_peer_ID_peers_name_slogan_broadcastable: sign: "+peer_data);
			byte[] signature = peer_data.sign(DD.getMyPeerSK());//Util.sign_peer(pa);
			peer_data.component_basic_data.signature = signature;
			if(DEBUG){
				//out.println("Server.update_my_peer_ID_peers_name_slogan_broadcastable "+peer_data);
				if(!peer_data.verifySignature()){
					if(DEBUG) out.println("Server:update_my_peer_ID_peers_name_slogan_broadcastable: signature verification failed at creation: "+peer_data);
				}else{
					if(DEBUG) out.println("Server:update_my_peer_ID_peers_name_slogan_broadcastable: signature verification passed at creation: "+peer_data);			
				}
			}
			peer_data.storeVerified();
			
			//D_PeerAddress.re_init_myself();
		} catch (P2PDDSQLException e) {
			Application.warning(_("Failed updating peer!"), _("Updating Peer"));
		}
		if(DEBUG) out.println("END Server.update_my_peer_ID_peers_name_slogan_broadcastable ");
	}
	/**
	 * Called when a server starts
	 * @param id
	 * @throws P2PDDSQLException
	 */
	public static long _set_my_peer_ID (Identity id) throws P2PDDSQLException {		
		Calendar _creation_date=Util.CalendargetInstance();
		String creation_date=Encoder.getGeneralizedTime(_creation_date);
		//long pID = -1;
		
		D_PeerAddress me = D_PeerAddress.get_or_create_myself(id, _creation_date, creation_date);
		return _set_my_peer_ID(me);
	}
	/**
	 * Called when a server starts, to store local addresses and directories
	 * announce to status
	 * 
	 * @param me
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long _set_my_peer_ID (D_PeerAddress me) throws P2PDDSQLException {		
		if (DEBUG) System.out.println("D_PeerAddress:_set_my_peer_ID: Myself="+me);
		String addresses = Identity.current_server_addresses();
		Calendar _creation_date=Util.CalendargetInstance();
		String creation_date=Encoder.getGeneralizedTime(_creation_date);
		
		if(addresses != null) {
			String address[] = addresses.split(Pattern.quote(DirectoryServer.ADDR_SEP));
			for(int k=0; k<address.length; k++){
//				if(k==0){
//					pID = update_insert_peer_myself(id, address[k], Address.SOCKET, _creation_date, creation_date, false, 0);
//					if(pID<=0) throw new RuntimeException("Failure to insert myself");
//				}
//				else insert_peer_address_if_new(Util.getStringID(pID), address[k], Address.SOCKET, creation_date, false, 0);
				me.addAddress(address[k], Address.SOCKET, _creation_date, creation_date, false, 0, true);
			}
		}
		if (DEBUG) System.out.println("D_PeerAddress:_set_my_peer_ID: Myself crea 1="+me.getCreationDate());
		
		int i=0;
		for(String dir : Identity.listing_directories_string ) {
			if(Server.DEBUG) out.println("server: announce to: "+dir);
			Address d  = new Address(dir);
			String address_dir = d.getStringAddress(); //dir;//.getHostName()+":"+dir.getPort();
			if(DEBUG) out.println("D_PeerAddr:_set_my_pID: dir="+dir+" ->"+address_dir+" via d="+d);
//			if(pID==-1){
//				pID = update_insert_peer_myself(id, address_dir, Address.DIR, _creation_date, creation_date, true, i);
//				if(pID<=0) throw new RuntimeException("Failure to insert my directories");
//			}
//			else insert_peer_address_if_new(Util.getStringID(pID), address_dir, Address.DIR, creation_date, true, i);
			me.addAddress(address_dir, Address.DIR, _creation_date, creation_date, true, i, true);
			i++;
		}
		//me.component_basic_data.creation_date = Util.CalendargetInstance();
		me.signMe(); //.sign(DD.getMyPeerSK());
		me._storeVerified(_creation_date, creation_date, false);
		
		if (DD.status != null) DD.status.setMePeer(me);
		//re_init_myself();
		if(Server.DEBUG) out.println("server: setID Done!");
		return me._peer_ID;
	}
	public String getCreationDate() {
		return Encoder.getGeneralizedTime(this.component_basic_data.creation_date);
	}
	public void addAddress(String _address, String type, Calendar _creation_date,
			String arrival_date, boolean certified, int priority, boolean keep_old_priority_if_exists) throws P2PDDSQLException {
		TypedAddress ta = new TypedAddress();
		if(DEBUG) System.out.println("D_PeerAddress: addAddress: adr="+_address+" ty="+type+" cer="+certified+" pri="+priority+" keep="+keep_old_priority_if_exists);
		if(DEBUG) System.out.println("D_PeerAddress: existing adr="+Util.concat(address, ":", "null"));
		ta.address = _address;
		ta.type = type;
		ta.certified=certified;
		ta.priority = priority;
		ta.arrival_date = arrival_date;
		if(DEBUG) System.out.println("D_PeerAddress: ta="+ta);
		if(this.address==null){
			if(DEBUG) System.out.println("D_PeerAddress: address=null, just add this");
			address = new TypedAddress[]{ta};
		}else{
			if(ta.certified && existsCertifiedAddressPriority(address, priority)) {
				ta.priority = priority = getMaxCertifiedAddressPriority()+1;
				if(DEBUG) System.out.println("D_PeerAddress: new priority="+priority);
			}else{
				if(DEBUG) System.out.println("D_PeerAddress: no conflict on priority="+priority);
			}
			TypedAddress c = TypedAddress.getLastContact(address, ta);
			if(c == null){
				if(DEBUG) System.out.println("D_PeerAddress: adding current="+ta);
				address = TypedAddress.insert(address, ta);
				if(ta.certified){
					if(DEBUG) System.out.println("D_PeerAddress: date changed adding current="+ta);
					this.component_basic_data.creation_date = _creation_date;
					signMe();
					_storeVerified(_creation_date, arrival_date, true);
				}else{
					ta.store_or_update(peer_ID, false);
				}
			}else{
				if(keep_old_priority_if_exists && (c.certified == ta.certified)) {
					ta.priority = c.priority;
					ta.peer_address_ID = c.peer_address_ID;
					ta.store_or_update(peer_ID, false);
				}else{
					c.certified = certified;
					c.priority = priority;
					ta.peer_address_ID = c.peer_address_ID;
					ta.store_or_update(peer_ID, false);
					//c.arrival_date = arrival_date;
				}
				if(DEBUG) System.out.println("D_PeerAddress: modify old="+ta);
			}
		}
	}
	/**
	 * Returns -1 if there is no certified address
	 * @return
	 */
	public int getMaxCertifiedAddressPriority() {
		int crt_max = -1;
		if(address == null) return crt_max;
		for(int k=0; k < address.length; k++)
			if(address[k].certified)
				crt_max = Math.max(crt_max, address[k].priority); 
		return crt_max;
	}
	private static boolean existsCertifiedAddressPriority(TypedAddress[] _address,
			int priority) {
		if(_address == null) return false;
		for(int k=0; k < _address.length; k++)
			if((_address[k].priority == priority)&&_address[k].certified) return true; 
		return false;
	}
	/**
	 * Either insert or update myself as peer in the peers table and in peer_addressess
	 * Called from _set_my_peer_ID when starting a TCP or UDP server
	 * @param id
	 * @param address
	 * @param type
	 * @param certified 
	 * @param priority 
	 * @throws P2PDDSQLException
	 */
	@Deprecated
	public static long update_insert_peer_myself(Identity id, String address, String type,
			Calendar _creation_date, String creation_date, boolean certified, int priority) throws P2PDDSQLException{
		if(Server.DEBUG) out.println("Server:update_insert_peer_myself: id="+id+" address="+address+" type="+type);
		long pID;
		D_PeerAddress peer_data = D_PeerAddress.get_myself(id.globalID, id.instance);
		//if ((myself==null) || (!id.globalID.equals(myself.globalID)))
		//	peer_data = new D_PeerAddress(id.globalID, 0, false, false, true);
		pID = peer_data._peer_ID;
	
		
		if(peer_data.peer_ID == null) { // I did not exist (with this globID)
			if(Server.DEBUG) out.println("Server:update_insert_peer_myself: required new peer");
			
			peer_data.component_basic_data.globalID = id.globalID; //Identity.current_peer_ID.globalID;
			peer_data.component_basic_data.name = id.name; //Identity.current_peer_ID.name;
			peer_data.component_basic_data.slogan = id.slogan; //Identity.current_peer_ID.slogan;
			peer_data.component_basic_data.creation_date = _creation_date;//Util.CalendargetInstance();
			peer_data.signature_alg = SR.HASH_ALG_V1;
			peer_data.component_basic_data.broadcastable = DD.DEFAULT_BROADCASTABLE_PEER_MYSELF;//Identity.getAmIBroadcastable();
			String picture = Identity.getMyCurrentPictureStr();
			peer_data.component_basic_data.picture = Util.byteSignatureFromString(picture);
			byte[] signature = peer_data.sign(DD.getMyPeerSK());//Util.sign_peer(pa);
			if(Server.DEBUG) {
				if(!peer_data.verifySignature()) {
					peer_data.component_basic_data.signature = signature;
					if(Server.DEBUG) out.println("Server:update_insert_peer_myself: signature verification failed at creation: "+peer_data);
				}else{
					if(Server.DEBUG) out.println("Server:update_insert_peer_myself: signature verification passed at creation: "+peer_data);			
				}
			}
			
			String IDhash = getGIDHashFromGID(id.globalID);
			peer_data.component_basic_data.globalIDhash = IDhash;
			if(Server.DEBUG) out.println("Server:update_insert_peer_myself: will insert new peer");
			pID=peer_data._storeVerified();
			Identity.current_identity_creation_date = peer_data.component_basic_data.creation_date;
			Identity.peer_ID = Util.getStringID(pID);
			if(Server.DEBUG) out.println("Server:update_insert_peer_myself: inserted"+pID);
			pID= Application.db.insert(table.peer_address.TNAME,
					new String[]{table.peer_address.address, table.peer_address.type,
					table.peer_address.peer_ID, table.peer_address.arrival_date,
					table.peer_address.certified, table.peer_address.priority},
					new String[]{address, type, Util.getStringID(pID), creation_date,
					Util.bool2StringInt(certified), ""+priority},
					Server.DEBUG);
		}else{
			String my_peer_ID = peer_data.peer_ID;
			if(Server.DEBUG) out.println("old val:"+my_peer_ID);
			insert_peer_address_if_new(my_peer_ID, address, type, creation_date, certified, priority);
		}
		if(Server.DEBUG) out.println("Server:update_insert_peer_myself: done");
		return pID;
	}

	/**
	 * insert my address in peer_addressess if new
	 * @param _peer_ID
	 * @param address
	 * @param type
	 * @param creation_date
	 * @param certified 
	 * @param priority 
	 * @return
	 * @throws P2PDDSQLException
	 */
	@Deprecated
	public static long insert_peer_address_if_new(String _peer_ID, String address, String type,
			String creation_date, boolean certified, int priority) throws P2PDDSQLException{
		long result=-1;
		ArrayList<ArrayList<Object>> op = Application.db.select("SELECT "+table.peer_address.peer_address_ID+" FROM "+table.peer_address.TNAME+
				" WHERE "+table.peer_address.peer_ID+" = ? AND "+table.peer_address.address+"=?;",
				new String[]{_peer_ID+"", address});
		if(Server.DEBUG) out.println("Server:update_insert_peer_address_myself:db addr results="+op.size());
		if (op.size()==0) {
			result = Application.db.insert(table.peer_address.TNAME,
					new String[]{table.peer_address.address, table.peer_address.type,
					table.peer_address.peer_ID, table.peer_address.arrival_date,
					table.peer_address.certified, table.peer_address.priority},
					new String[]{address, type, _peer_ID, creation_date, Util.bool2StringInt(certified), ""+ priority} , Server.DEBUG);
		}
		else result = Util.lval(op.get(0).get(0), -1);
		return result;
	}

	/**
	 * Should use "myself"
	 * @return
	 * @throws P2PDDSQLException 
	 */
	public static DDAddress getMyDDAddress() throws P2PDDSQLException {
		DDAddress myAddress;
		D_PeerAddress me = D_PeerAddress.get_myself_from_Identity();
		if(DEBUG) System.out.println("D_PeerAddress:getMyDDAddress: D_PeerAddress: "+me);
		if(!me.verifySignature()) {
			if(_DEBUG) System.out.println("D_PeerAddress:getMyDDAddress: fail signature: "+me);
			me.sign(DD.getMyPeerSK());
			me.storeVerifiedForce();
		}
		if(!me.verifySignature()) {
			Application.warning(_("Inconsistent identity. Please restart app."), _("Inconsistant Identity"));
			return null;
		}
		myAddress = new DDAddress(me);
		if(DEBUG) System.out.println("D_PeerAddress:getMyDDAddress: DD_Address: "+myAddress);
		return myAddress;
	}
		/*
		myAddress.globalID = Identity.current_peer_ID.globalID;
		myAddress.name = Identity.current_peer_ID.name;
		myAddress.slogan = Identity.current_peer_ID.slogan;
		myAddress.creation_date = Util.getGeneralizedTime();
		myAddress.address = "";
		myAddress.broadcastable = Identity.getAmIBroadcastable();
		myAddress.served_orgs = table.peer_org.getPeerOrgs(myAddress.globalID);
		if(Identity.listing_directories_string.size()>0) {
			String isa = Server.DIR+":"+Identity.listing_directories_string.get(0);
			for(int k=1; k<Identity.listing_directories_string.size(); k++) {
				isa += DirectoryServer.ADDR_SEP+Server.DIR+":"+Identity.listing_directories_string.get(k);
			}
			myAddress.address = isa;
			if(DEBUG) System.out.println("actionExport: Directories: "+isa);
		}
		String crt_adr=Identity.current_server_addresses();
		if(DEBUG) System.out.println("actionExport: Sockets: "+crt_adr);
		if(crt_adr!=null) {
			String[] server_addr = crt_adr.split(Pattern.quote(DirectoryServer.ADDR_SEP));
			if("".equals(myAddress.address)) myAddress.address = Server.SOCKET+":"+server_addr[0];
			else myAddress.address += DirectoryServer.ADDR_SEP+Server.SOCKET+":"+server_addr[0];
			
			for(int k=1; k<server_addr.length; k++)
				myAddress.address += DirectoryServer.ADDR_SEP+Server.SOCKET+":"+server_addr[k];
		}
		if(DEBUG) System.out.println("actionExport: Adresses: "+myAddress.address);
		
		myAddress.hash_alg = SR.HASH_ALG_V1;
		SK my_sk = DD.getMyPeerSK();
		if(my_sk == null) Application.warning(_("Address is not siged. Secret Key missing!"), _("No Secret Key!"));
		myAddress.sign(my_sk);
		*/

	public static String getAddressID(InetSocketAddress saddr, String peer_ID) throws P2PDDSQLException {
		//String sname = saddr.toString();
		String ip = Util.get_IP_from_SocketAddress(saddr);
		ArrayList<ArrayList<Object>> a = Application.db.select(
				"SELECT "+table.peer_address.peer_address_ID+
				" FROM "+table.peer_address.TNAME+
				" WHERE "+table.peer_address.peer_ID+"=? AND "+table.peer_address.address+" LIKE '"+peer_ID+"%';", new String[]{});
		if(a.size()==0) return null;
		return Util.getString(a.get(0).get(0));
	}
	/**
	 * get the last reset date
	 * @param _peerID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String getLastResetDate(String _peerID) throws P2PDDSQLException {
		ArrayList<ArrayList<Object>> a = Application.db.select("SELECT "+table.peer.last_reset+" FROM "+table.peer.TNAME+" WHERE "+table.peer.peer_ID+"=?;", new String[]{_peerID});
		if(a.size() == 0) return null;
		return Util.getString(a.get(0).get(0));
	}
	/**
	 * Store "now" as the last date when reset was made,
	 * and set last_sync_date to null
	 * @param peerID
	 * @param reset 
	 */
	public static void reset(String peerID, String instance, Calendar reset) {
    	try {
    		String now = null;
    		if(reset!=null) now = Encoder.getGeneralizedTime(reset);
    		if(_DEBUG) System.out.println("\n***********\n\nD_PeerAddress:reset:"+peerID+" rst="+now+ " ins="+instance);
    		if(now==null) now = Util.getGeneralizedTime();
    		if(instance == null) {
    			Application.db.update(table.peer.TNAME, new String[]{table.peer.last_reset, table.peer.last_sync_date}, new String[]{table.peer.peer_ID},
					new String[]{now, null, peerID}, DEBUG);
    		}else{
    			Application.db.update(table.peer_instance.TNAME, new String[]{table.peer_instance.last_reset, table.peer_instance.last_sync_date}, new String[]{table.peer_instance.peer_ID,table.peer_instance.peer_instance},
					new String[]{now, null, peerID, instance}, DEBUG);
    		}
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
	}
	/**
	 * Does this serve this orgID?
	 * only implement if it can be done efficiently
	 * @param orgID
	 * @return
	 */
	public boolean servesOrgEntryExists(long orgID) {
		if(orgID<=0) return false;
		if(this.served_orgs==null) return false;
		for(int k=0; k < this.served_orgs.length; k++) {
			if(this.served_orgs[k].organization_ID == orgID) return true;
		}
		return false;
	}
	public static String getDisplayName(long peer_ID) {
		if(peer_ID <= 0) return null; 
		String sql = 
				"SELECT p."+table.peer.name+",m."+table.peer_my_data.name+
					" FROM "+table.peer.TNAME+" AS p "+
					" LEFT JOIN "+table.peer_my_data.TNAME+" AS m ON (p."+table.peer.peer_ID+"=m."+table.peer_my_data.peer_ID+") "+
					" WHERE p."+table.peer.peer_ID+"=? ;";
		ArrayList<ArrayList<Object>> n;
		try {
			n = Application.db.select(sql, new String[]{Util.getStringID(peer_ID)}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		if(n.size()==0) return null;
		String my_name = Util.getString(n.get(0).get(1));
		if(my_name!=null) return my_name;
		String name = Util.getString(n.get(0).get(0));
		return name;
	}

	
	public static void _main(String[] args) {
		try {
			DEBUG = true;
			hds.TypedAddress.DEBUG=true;
			//hds.TypeAddress.DEBUG=true;
			long id = 1;
			if(args.length>0) id = Integer.parseInt(args[0]);
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			if(id<0){
				id=-id;
				D_PeerAddress w = readSignSave(id,id, true); 
				System.out.println("\n************Signed\n**********\nread="+w);
				if(!w.verifySignature()) System.out.println("\n************Signature Failure\n**********\nread=");
				else System.out.println("\n************Signature Pass\n**********\nread=");
				if(true) return;}
			
			D_PeerAddress c=new D_PeerAddress(""+id, true);
			if(!c.verifySignature()) System.out.println("\n************Signature Failure\n**********\nread="+c);
			else System.out.println("\n************Signature Pass\n**********\nread="+c);
			Decoder dec = new Decoder(c.getEncoder().getBytes());
			D_PeerAddress d = new D_PeerAddress().decode(dec);
			//Calendar arrival_date = d.arrival_date=Util.CalendargetInstance();
			//if(d.global_organization_ID==null) d.global_organization_ID = OrgHandling.getGlobalOrgID(d.organization_ID);
			if(!d.verifySignature()) System.out.println("\n************Signature Failure\n**********\nrec="+d);
			else System.out.println("\n************Signature Pass\n**********\nrec="+d);
			//d.storeVerified(Encoder.getGeneralizedTime(arrival_date));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		try{
			if(args.length == 0) {
				System.out.println("prog database id fix verbose");
				return;
			}
			
			String database = Application.DELIBERATION_FILE;
			if(args.length>0) database = args[0];
			
			long id = 0;
			if(args.length>1) id = Long.parseLong(args[1]);
			
			boolean fix = false;
			if(args.length>2) fix = Util.stringInt2bool(args[2], false);
			
			//boolean verbose = false;
			if(args.length>3) DEBUG = Util.stringInt2bool(args[3], false);
			
			
			Application.db = new DBInterface(database);
			
			ArrayList<ArrayList<Object>> l;
			if(id<=0){
				l = Application.db.select(
						"SELECT "+table.peer.peer_ID+
						" FROM "+table.peer.TNAME, new String[]{}, DEBUG);
				for(ArrayList<Object> a: l){
					String m_ID = Util.getString(a.get(0));
					long ID = Util.lval(m_ID, -1);
					D_PeerAddress m = new D_PeerAddress(ID);
					if(m.component_basic_data.signature==null){
						System.out.println("Fail:temporary "+m_ID+":"+m.peer_ID+":"+m.component_basic_data.name);
						continue;
					}
					if(m.component_basic_data.globalIDhash==null){
						System.out.println("Fail:edited "+m_ID+":"+m.peer_ID+":"+m.component_basic_data.name);
						continue;
					}
					if(!m.verifySignature()){
						System.out.println("Fail: "+m.peer_ID+":"+m.component_basic_data.name);
						if(fix){
							readSignSave(ID,ID,true);
						}
					}
				}
				return;
			}else{
				long ID = id;
				D_PeerAddress m = new D_PeerAddress(ID+"");
				if(fix)
					if(!m.verifySignature()) {
						System.out.println("Fixing: "+m.peer_ID+":"+m.component_basic_data.name);
						readSignSave(ID, ID, true);
					}
				else if(!m.verifySignature())
					System.out.println("Fail: "+m.peer_ID+":"+m.component_basic_data.name);
				return;
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void signMe() {
		sign(DD.getMyPeerSK());
	}
	public boolean removeAddress(int row, boolean me) {
		if((address == null) || (address.length==0) ||
				(!me && address[row].certified)) return false;
		int dst = 0;
		TypedAddress[] _address = new TypedAddress[address.length - 1];
		for(int k=0; k<address.length; k++){
			if(row==k) continue;
			_address[dst] = address[k];
			dst++;
		}
		address = _address;
		return true;
	}
	public String getAddressesDesc() {
		if(this.address==null) return "null";
		String result = "#"+address.length+"#";
		result += Util.concat(this.address, ", ");
		return result;
	}
	public static boolean checkValid(long peerID) {
		try {
			D_PeerAddress m = new D_PeerAddress(peerID);
			return m.verifySignature();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	public String getGID() {
		if(this.component_basic_data == null) return null;
		return this.component_basic_data.globalID;
	}

	public String getGIDH() {
		if(this.component_basic_data == null) return null;
		return this.component_basic_data.globalIDhash;
	}
	/**
	 * if GIDH is null, computes it from GID
	 * @param gID
	 * @param gIDH
	 */
	public void setGID(String gID, String gIDH) {
		this.component_basic_data.globalID = gID;
		if ((gID != null) && (gIDH == null))
			gIDH = D_PeerAddress.getGIDHashFromGID(gIDH);
		this.component_basic_data.globalIDhash = gIDH;
	}
	public PeerInput getPeerInput() {
		PeerInput result = new PeerInput();
		result.email = this.getEmail();
		result.slogan = this.getSlogan();
		result.name = this.getName();
		result.cipherSuite = this.getCipherSuite();
		return result;
	}
	public void setPeerInputNoCiphersuit(PeerInput data) {
		setEmail(data.email);
		setSlogan(data.slogan);
		setName(data.name);
	}

	public CipherSuit getCipherSuite() {
		PK pk = Cipher.getPK(getGID());
		return Cipher.getCipherSuite(pk);
	}
	public String getName() {
		return this.component_basic_data.name;
	}
	public void setName(String _name) {
		this.component_basic_data.name = _name;
	}
	public String getSlogan() {
		return this.component_basic_data.slogan;
	}
	public void setSlogan(String _slogan) {
		this.component_basic_data.slogan = _slogan;
	}
	public String getEmail() {
		return this.component_basic_data.emails;
	}
	public void setEmail(String _email) {
		this.component_basic_data.emails = _email;
	}
	public String get_ID() {
		if ((this.peer_ID == null) && (this._peer_ID > 0))
			this.peer_ID = Util.getStringID(_peer_ID);
		return this.peer_ID;
	}
	public void set_ID(String __peer_ID) {
		this.peer_ID = __peer_ID;
		this._peer_ID = Util.lval(__peer_ID, -1);
	}
	public void set_ID(long __peer_ID) {
		this.peer_ID = Util.getStringID(__peer_ID);
		this._peer_ID = __peer_ID;
	}
	/**
	 * Does not create keys if absent
	 * @param new_sk
	 * @param old_data
	 * @param _pk
	 * @return
	 */
	public static D_PeerAddress getPeer(SK new_sk, PeerInput old_data,
			String _pk) {
		if ((new_sk == null) && (_pk==null)) return null;
		if (_pk == null) {
			PK new_pk = new_sk.getPK();
			_pk = Util.getKeyedIDPK(new_pk);
		}
		D_PeerAddress peer;
		try {
			peer = new D_PeerAddress(_pk);
			peer.setPeerInputNoCiphersuit(old_data);
			peer.sign(new_sk);
			peer.storeVerified();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		return peer;
	}
	public static void setCurrentInstanceInDB(String peer_instance) {
		try {
			DD.setAppText(DD.APP_my_peer_instance, peer_instance);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public static String getCurrentInstanceInDB() {
		try {
			return DD.getAppText(DD.APP_my_peer_instance);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static boolean samePeers(D_PeerAddress p1, D_PeerAddress p2) {
		if (p1 == p2) return true;
		if ((p1 == null) || (p2 == null)) return false;
		if (p1.getGIDH() == p2.getGIDH()) return true;
		if ((p1.getGIDH() == null) || (p2.getGIDH() == null)) return false;
		if (p1.getGIDH().equals(p2.getGIDH())) return true;
		return true;
	}
	public void deleteInstance(String peer_instance) {
		this.loadInstances();
		if (this.instances_orig == null) {
			this.instances_orig = D_PeerInstance.deep_clone(instances);
		}
		this.instances.remove(peer_instance);
		this.dirty_instances = true;
		this.storeAsynchronouslyNoException();
	}
	/**
	 * 
	 * @param instance2 (if null, try current time)
	 * @param createdLocally
	 * @return: returns false if it already existed
	 */
	public boolean addInstanceElem(String instance2, boolean createdLocally){
		if (instance2 == null) instance2 = Util.getGeneralizedTime();
		
		if (this.instances.containsKey(instance2)) return false;

		D_PeerInstance nou = new D_PeerInstance(instance2);
		nou.createdLocally = createdLocally;
		nou.last_sync_date = Util.CalendargetInstance();
		nou._last_sync_date = Encoder.getGeneralizedTime(nou.last_sync_date);

		if (this.instances_orig == null) {
			this.instances_orig = D_PeerInstance.deep_clone(instances);
		}
		this.instances.put(instance2, nou);
		this.dirty_instances = true;
		return true;
	}
	/**
	 * Loads instances if needed,
	 * @param instance2 : the name should have been provided
	 * @param createdLocally
	 * @return : returns false if it already existed
	 */
	public boolean addInstance(String instance2, boolean createdLocally) {
		this.loadInstances();
		if (!addInstanceElem(instance2, createdLocally)) return false;
		this.storeAsynchronouslyNoException();
		return true;
	}
	public void makeNewInstance() {
		//Encoder enc = new Encoder().initSequence();
		//enc.addToSequence(new Encoder(Util.CalendargetInstance()));
		Calendar cal;
		this.loadInstances();
		do {
			cal = Util.CalendargetInstance();
			this.instance = Encoder.getGeneralizedTime(cal);//Util.getHash(enc.getBytes(), Cipher.MD5);
		} while (instances.containsKey(this.instance));
		
		if (!addInstanceElem(this.instance, true)) return;
		
		//D_PeerInstance value =  new D_PeerInstance(this.instance, true);
		//value.createdLocally = true;
		//this.instances.put(this.instance, value);
		//this.dirty_instances = true;
	}
	public String getInstance() {
		return instance;
	}
}
