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
package streaming;

import hds.ASNDatabase;
import hds.ASNSyncPayload;
import hds.ASNSyncRequest;
import hds.Address;
import hds.ClientSync;
import hds.ResetOrgInfo;
import hds.SyncAnswer;
import hds.Table;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import plugin_data.D_PluginInfo;

import static java.lang.System.out;
import static java.lang.System.err;
import util.P2PDDSQLException;
import static util.Util._;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;
import config.Application;
import config.Application_GUI;
import config.DD;
import config.Identity;
import data.D_Constituent;
import data.D_Justification;
import data.D_Motion;
import data.D_Neighborhood;
import data.D_News;
import data.D_Organization;
import data.D_Peer;
import data.D_PeerInstance;
import data.D_PluginData;
import data.D_Translations;
import data.D_Vote;
import data.D_Witness;
import util.CommEvent;
import util.Util;

public class UpdateMessages {
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int LIMIT_PEERS_LOW = 50;
	private static final int LIMIT_PEERS_MAX = 200;
	private static final int LIMIT_NEWS_LOW = 10;
	private static final int LIMIT_NEWS_MAX = 200;
	private static final int LIMIT_ORG_LOW = 10;
	private static final int LIMIT_ORG_MAX = 300;
	public static byte[] getFieldData(Object obj){
		if (obj==null) return null;
		if (obj instanceof String) return obj.toString().getBytes();
		return obj.toString().getBytes();
	}
	public static SyncAnswer buildAnswer(ASNSyncRequest asr, String peerID) throws P2PDDSQLException {
		boolean DEBUG = true;
		if (DEBUG) System.out.println("\n\nUpdateMessages:buildAnswer<top>: pID="+peerID+" asr="+asr);
		SyncAnswer sa=null;
		String[] _maxDate = new String[1];
		HashSet<String> orgs = new HashSet<String>();
		String peer_GID = null;
		
		if ((peerID == null) && (asr.address != null)) {
			peer_GID = asr.address.component_basic_data.globalID;
			peerID = D_Peer.getLocalPeerIDforGID(peer_GID);
		} else {
			//if(asr.address!=null) peer_GID = asr.address.globalID;
		}
		if (asr.address != null) {
			asr.address.peer_ID = peerID;
			asr.address._peer_ID = Util.get_long(peerID);
		}
		// solved in record peer address
		//if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("UpdateMessages: buildAnswer: recording plugins for pID="+peerID+" gID="+Util.trimmed(peer_GID));
		//D_PluginInfo.recordPluginInfo(asr.plugin_info, peer_GID, peerID);

		
		buildAnswer(asr, _maxDate, true, orgs);
		if (_maxDate[0] == null) {
			_maxDate[0] = Util.getGeneralizedTime();
			if (DEBUG) System.out.println("UpdateMessages:buildAnswer<top>: null maxDate->now="+_maxDate[0]);
		}
		if (DEBUG) System.out.println("UpdateMessages:buildAnswer<top>: interm maxDate="+_maxDate[0]);
		sa = buildAnswer(asr, _maxDate, false, orgs);
		if (DEBUG) System.out.println("UpdateMessages:buildAnswer<top>: done maxDate="+_maxDate[0]+" -> "+sa);
		sa.changed_orgs = ClientSync.getChangedOrgs(peerID);
		
		// pack requested data
		sa.requested = WB_Messages.getRequestedData(asr,sa);
		
		// handle plugin data
		if (peerID != null) {
			if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("UpdateMessages: buildAnswer: get plugin Data for peerID="+peerID);
			sa.plugin_data_set = D_PluginData.getPluginMessages(peerID);
		} else {
			if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("UpdateMessages: buildAnswer: skip plugin_data null peerID");			
		}
		sa.plugins = D_PluginInfo.getRegisteredPluginInfo();
		
		
		if (sa.upToDate != null) {
			Calendar now = Util.CalendargetInstance();
			if (now.before(sa.upToDate)) {
				if (_DEBUG) out.println("UpdateMessages:buildAnswer<top>: replace data date: "+Encoder.getGeneralizedTime(sa.upToDate)+
						" with: "+Encoder.getGeneralizedTime(now));
				sa.upToDate = now;
			}
		}
		if (DEBUG) System.out.println("UpdateMessages:buildAnswer<top>: exit sa="+sa);
		return sa;
	}
	public static SyncAnswer buildAnswer(ASNSyncRequest asr, String[] _maxDate, boolean justDate, HashSet<String> orgs) throws P2PDDSQLException {
		boolean DEBUG = true;
		if(DEBUG) System.out.println("UpdateMessages:buildAnswers: orgs start="+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
		String maxDate =_maxDate[0];
		if(!justDate && (maxDate==null) && (orgs.size()==0)) {
			if(DEBUG) out.println("UpdateMessages:buildAnswer: START-EXIT Nothing new to send!");
			return new SyncAnswer(asr.lastSnapshot, Identity.current_peer_ID.globalID);
		}
		if(DEBUG) out.println("UpdateMessages:buildAnswer: START: Date at entrance: "+_maxDate[0]+" justDate="+justDate);
		String last_sync_date="00000000000000.000Z";
		if(asr.lastSnapshot!=null) last_sync_date = Encoder.getGeneralizedTime(asr.lastSnapshot);
		try{
			if(DEBUG) out.println("UpdateMessages:buildAnswer: Server Handler has obtained a Sync Request for: "+asr);
			if(DEBUG) out.println("UpdateMessages:buildAnswer: Server building answer for request with last sync date: "+last_sync_date+" from: "+asr.address);
		}catch(Exception e){e.printStackTrace();}
		ArrayList<Table> tableslist = new ArrayList<Table>();
		int tables_nb=0;
		if(asr.tableNames!=null) tables_nb = asr.tableNames.length;
				
		if(DEBUG) out.println("UpdateMessages:buildAnswer: requestes tables #"+tables_nb);
		for(int k=0; k<tables_nb; k++) {
			if(DEBUG) out.println("UpdateMessages:buildAnswer: handling table ["+k+"] "+asr.tableNames[k]);
			if(table.peer.G_TNAME.equals(asr.tableNames[k])){
				if(DEBUG) out.println("UpdateMessages:buildAnswer: peers table from date: "+_maxDate[0]);
				Table recentPeers=UpdatePeersTable.buildPeersTable(last_sync_date, _maxDate, justDate, orgs, LIMIT_PEERS_LOW, LIMIT_PEERS_MAX);
				if(DEBUG) System.out.println("UpdateMessages:buildAnswers: orgs after peers="+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
				if(DEBUG) out.println("UpdateMessages:buildAnswer: got peers #"+recentPeers);
				if(justDate){ if(DEBUG) out.println("UpdateMessages:buildAnswer: Date after peers: "+_maxDate[0]); continue;}
				if(recentPeers.rows.length > 0) tableslist.add(recentPeers);
				if(DEBUG) out.println("UpdateMessages:buildAnswer: Date after peers:: "+_maxDate[0]);
			}else{
				if(DEBUG) out.println("UpdateMessages:buildAnswer: non peers table from date: "+_maxDate[0]);
				if(table.news.G_TNAME.equals(asr.tableNames[k])){
					if(DEBUG) out.println("UpdateMessages:buildAnswer: news table from date: "+_maxDate[0]);
					if(DEBUG) System.out.println("UpdateMessages:buildAnswers: orgs after news="+" :"+Util.nullDiscrimArray(orgs.toArray(new String[0])," : "));
					Table recentNews = UpdateNewsTable.buildNewsTable(last_sync_date, _maxDate, justDate, orgs, LIMIT_NEWS_LOW, LIMIT_NEWS_MAX);
					if(justDate) {if(DEBUG) out.println("UpdateMessages:buildAnswer: Date after news: "+_maxDate[0]); continue;}
					if(recentNews.rows.length > 0) tableslist.add(recentNews);							
					if(DEBUG) out.println("UpdateMessages:buildAnswer: Date after news:: "+_maxDate[0]);
				}else
					if(DEBUG) out.println("UpdateMessages:buildAnswer: Table not served: "+asr.tableNames[k]);
			}
		}
		if (!justDate) {
			orgs.remove(OrgHandling.ORG_PEERS);
			orgs.remove(OrgHandling.ORG_NEWS);
		}
		// get orgData based on last Data in asr, not last_sync_date
		D_Organization[] orgData = OrgHandling.getOrgData(asr, last_sync_date, _maxDate, justDate, orgs, LIMIT_ORG_LOW, LIMIT_ORG_MAX);
		if(DEBUG) out.println("UpdateMessages:buildAnswer: Date after orgData: "+_maxDate[0]);
		
		if(justDate) return null;
		if(DEBUG) out.println("UpdateMessages:buildAnswer: Date at end: "+_maxDate[0]);
		SyncAnswer sa = new SyncAnswer();
		
		if ((orgData != null))
			for (D_Organization dorg : orgData) {
				if ((dorg != null) && (dorg.availableHashes != null)) {
					if (sa.advertised == null) // &&(sa.advertised.rd!=null))
						sa.advertised = new SpecificRequest();
					if ((sa.advertised != null) && (sa.advertised.rd != null))						
						sa.advertised.rd.add(dorg.availableHashes);
				}
			}
		sa.orgData = orgData;
		sa.responderGID = Identity.current_peer_ID.globalID;
		if (tableslist.size() > 0) {
			sa.tables = new ASNDatabase();
			sa.tables.snapshot = Util.getCalendar(_maxDate[0]); // Util.CalendargetInstance();
			sa.tables.tables=tableslist.toArray(new Table[]{});
			if(DEBUG) out.println("UpdateMessages:buildAnswer: Prepared tables: "+sa.tables.tables.length);
		}
		if (_maxDate[0] != null) sa.upToDate = Util.getCalendar(_maxDate[0]);
		else sa.upToDate = asr.lastSnapshot;
		if(DEBUG) out.println("UpdateMessages:buildAnswer: EXIT with Answer: "+sa);
		return sa;
	}
	/**
	 * DD.ACCEPT_STREAMING_ANSWER_FROM_NEW_PEERS : to exit automatically on new peer
	 * 
	 * @param asa : arriving data
	 * @param s_address : socket of sender
	 * @param src : caller object (for debugging)
	 * @param _global_peer_ID
	 * @param _peer_ID
	 * @param address_ID : peer_address_ID to update as the one from which I got messages
	 * @param __rq
	 * @throws ASN1DecoderFail
	 * @throws P2PDDSQLException
	 */
	public static boolean integrateUpdate(ASNSyncPayload asa, InetSocketAddress s_address, Object src,
			String _global_peer_ID, D_PeerInstance instance, String _peer_ID, String address_ID, RequestData __rq, D_Peer peer) throws ASN1DecoderFail, P2PDDSQLException {

		//boolean DEBUG = true;
		if (DEBUG || DD.DEBUG_PLUGIN) err.println("UpdateMessages:integrateUpdate: start gID="+Util.trimmed(_global_peer_ID));

		// Check if new peer and exit if new not allowed
		long peer_ID = peer.get_ID_long(); // Util.lval(_peer_ID, -1);
		if (peer_ID <= 0 ) {
			if (DEBUG || DD.DEBUG_TODO)err.println("UpdateMessages:integrateUpdate: peer unknown but may announce self: "+peer);
			if (!DD.ACCEPT_STREAMING_ANSWER_FROM_NEW_PEERS) return false;
		}
		
		DD.ed.fireClientUpdate(new CommEvent(src, null, s_address, "Integrating", asa.toSummaryString()));
		
		// Integrate peers
		// First store just the peer contacting us, in the case he is new.
		// Here we can block him if we want blocking as default for new ones
		//
		
		if ((peer_ID <= 0) && (asa.responderGID != null)) {
			if (asa.tables != null) {
				if (DEBUG) err.println("UpdateMessages:integrateUpdate: tackle peers tables");
				//Calendar tables_date=asa.tables.snapshot;
				for (int k = 0; k < asa.tables.tables.length; k ++) {
					if (DEBUG) err.println("Client: Handling table: "+
							asa.tables.tables[k].name+", rows="+asa.tables.tables[k].rows.length);
					if (table.peer.G_TNAME.equals(asa.tables.tables[k].name)) {
						D_Peer p;
						p = UpdatePeersTable.integratePeersTable(asa, peer, asa.tables.tables[k], asa.responderGID);
						if (p != null) {
							peer_ID = p._peer_ID;
							_peer_ID = p.peer_ID;
						}
						continue;
					}
					if (table.news.G_TNAME.equals(asa.tables.tables[k].name)) continue;
					if (_DEBUG) err.println("Client: I do not handle table: "+asa.tables.tables[k].name);
				}
			}
		
			// Trying again to recover peer object, in case it was just saved from the message at this moment
			if (peer_ID <= 0) {
				D_Peer p = D_Peer.getPeerByGID(asa.responderGID, false, false);
				_peer_ID = null;
				if (p != null) {
					_peer_ID = p.get_ID();
					peer_ID = p.get_ID_long();
					if (peer_ID <= 0) peer_ID = p.storeRequest_getID();
				} else
					peer_ID = -1; // Util.lval(_peer_ID, -1);
				if (peer_ID <= 0) {
					if (DEBUG || DD.DEBUG_TODO) err.println("UpdateMessages:integrateUpdate: peer unknown GID not annouced in message:"+asa.responderGID);
					if (!DD.ACCEPT_STREAMING_ANSWER_FROM_ANONYMOUS_PEERS) return false;
				}
			}
		}
		// else
		{
			if (asa.tables != null) {
				if (DEBUG) err.println("UpdateMessages:integrateUpdate: tackle peers tables");
			  // Calendar tables_date=asa.tables.snapshot;
			  for (int k = 0; k < asa.tables.tables.length; k ++) {
				if (DEBUG) err.println("Client: Handling table: "+
						asa.tables.tables[k].name+", rows="+asa.tables.tables[k].rows.length);
				if (table.peer.G_TNAME.equals(asa.tables.tables[k].name)) {
						UpdatePeersTable.integratePeersTable(asa, peer, asa.tables.tables[k], null); continue;
				}
				if (table.news.G_TNAME.equals(asa.tables.tables[k].name)) continue;
				if (_DEBUG) err.println("Client: I do not handle table: "+asa.tables.tables[k].name);
			  }
			}
		}
		
		// Integrate news
		if (asa.tables != null) {
			if (DEBUG) err.println("UpdateMessages:integrateUpdate: tackle news tables");
		  //Calendar tables_date=asa.tables.snapshot;
		  for (int k = 0; k < asa.tables.tables.length; k ++) {
			if (DEBUG) err.println("Client: Handling table: "+
					asa.tables.tables[k].name+", rows="+asa.tables.tables[k].rows.length);
			if (table.peer.G_TNAME.equals(asa.tables.tables[k].name)) continue;
			if (table.news.G_TNAME.equals(asa.tables.tables[k].name)) {
				UpdateNewsTable.integrateNewsTable(asa.tables.tables[k]); continue;
			}
			if (_DEBUG) err.println("Client: I do not handle table: "+asa.tables.tables[k].name);
		  }
		}
		
		// Distribute data to plugins
		if (asa.plugin_data_set!=null){
			if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nUpdateMessages: integrateUpdate: will distribute to plugins");
			asa.plugin_data_set.distributeToPlugins(_global_peer_ID);
		} else {
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nUpdateMessages: integrateUpdate: nothing for plugins");
			
		}
		
		boolean future_requests = false;
		
		// Reset lastsyncdate for changed orgs
		if (asa.changed_orgs != null) { // currently implemented only in group (one date per peer, not per peer per org)
			if (DEBUG || DD.DEBUG_CHANGED_ORGS) err.println("UpdateMessages:integrateUpdate: changed_orgs="+Util.nullDiscrimArray(asa.changed_orgs.toArray(new ResetOrgInfo[0]),"--"));
			handleChangedOrgs(asa, _peer_ID);
		} else {
			if (DEBUG || DD.DEBUG_CHANGED_ORGS) err.println("UpdateMessages:integrateUpdate: changed_orgs=null");
		}
		
		D_PluginInfo.recordPluginInfo(asa.plugins, _global_peer_ID, _peer_ID);
		
		// Store requested. Put newly obtained in obtained_sr, and newly detected missing in sq_sr 
		Calendar snapshot_date = asa.upToDate;
		HashSet<String> orgs = new HashSet<String>();
		Hashtable<String, RequestData> obtained_sr = new Hashtable<String, RequestData>();
		Hashtable<String, RequestData> missing_sr = new Hashtable<String, RequestData>();
		
		String crt_date = Util.getGeneralizedTime();
		WB_Messages.store(asa, peer, asa.requested, missing_sr, obtained_sr, orgs, " from:"+peer_ID+" ");
		integrate_GIDs_accounting(missing_sr, obtained_sr, _global_peer_ID, peer_ID, peer);

		Hashtable<String, String> missing_peers = new Hashtable<String, String>();

		// Integrating organization data
		if (asa.orgData != null) {
			if (DEBUG) err.println("UpdateMessages:integrateUpdate: tackle org");
			String[] _orgID = new String[1];
			boolean changes = false;
			Calendar arrival__time = Util.CalendargetInstance();
			String arrival_time;
			for (int i = 0; i < asa.orgData.length; i ++) {
				if (DEBUG) out.println("Will integrate ["+i+"]: "+asa.orgData[i]);
				if (asa.orgData[i] == null) continue;
				asa.orgData[i].getLocalIDfromGIDandBlock(); // set blocking if new
				//long organization_ID = Util.lval(D_Organization.getLocalOrgID_fromGIDIfNotBlocked(asa.orgData[i].global_organization_ID), -1);
				//if(organization_ID>0) asa.orgData[i].organization_ID = Util.getStringID(organization_ID);
				OrgPeerDataHashes opdh = new OrgPeerDataHashes(asa.orgData[i]._organization_ID);
				/**
				 * From the following set of hashes we remove what we received now fully
				 *  
				 */
				RequestData old_rq = opdh.getRequestData();//getOldDataRequest(asa.orgData[i]);
				/**
				 * Here we add what is newly advertised and needed
				 */
				RequestData _new_rq = new RequestData();
				/**
				 * Here we add what is newly fully received (solved rq)
				 */
				RequestData _sol_rq = new RequestData();
				//_rq.purge(obtained);
				arrival_time = Encoder.getGeneralizedTime(Util.incCalendar(arrival__time, 1));
				boolean changed = OrgHandling.updateOrg(asa, asa.orgData[i], _orgID, arrival_time, _sol_rq, _new_rq, peer); // integrated other data, stores all if not blocked
				for (String p : _new_rq.peers.keySet()) {
					if (!missing_peers.contains(p)) missing_peers.put(p, _new_rq.peers.get(p));
				}
				//if(asa.orgData[i].signature==null) continue;
				changes |= changed;
				
				//Store last date, for filtering org
				String fdate = asa.orgData[i]._last_sync_date;
				//if(fdate!=null) //should never happen
				if ((_orgID[0] != null) && (_peer_ID != null)) {
					 // overwritten when a peer is saved again!
					peer.updateLastSyncDate(fdate, _orgID[0]);
//				  Application.db.updateNoSync( table.peer_org.TNAME,
//						  new String[]{table.peer_org.last_sync_date},
//						  new String[]{table.peer_org.peer_ID, table.peer_org.organization_ID},
//						new String[]{fdate, _peer_ID, _orgID[0]});
				}
				if ((peer_ID <= 0) || (asa.orgData[i]._organization_ID <= 0)) {
					if (_DEBUG) out.println("UpdateMessages: integrateUpdate: NOT SAVING maybe blocked ["+i+"]: "+asa.orgData[i]+" from "+peer_ID);
					continue;
				}
				// and save future specific requests
				if (! asa.orgData[i].blocked) {
					opdh.updateAfterChanges(old_rq, _sol_rq, _new_rq, peer_ID, crt_date);
					if (!opdh.empty()) future_requests = true;
					opdh.save(asa.orgData[i]._organization_ID, peer_ID, peer);
				} else {
					if(_DEBUG) out.println("UpdateMessages: integrateUpdate: blocked ["+i+"]: "+asa.orgData[i]);					
				}
				//updateDataToRequest(asa.orgData[i], _rq);

			}

			if(changes) Application.db.sync(new ArrayList<String>(Arrays.asList(table.field_extra.TNAME, table.organization.TNAME, table.peer_org.TNAME)));
		}
		integrate_peer_GIDs_accounting(missing_peers, _global_peer_ID, peer_ID, peer, Util.getGeneralizedTime());
		
		// Store information about needed data
		SpecificRequest sp = new SpecificRequest();
		evaluate_interest(asa.advertised, sp); // check existing/non-blocked data and insert wished one into sp, store sp in orgs
		if (store_detected_interests(sp, peer_ID, crt_date, peer)) future_requests = true;
		//purge(orgs, obtained);
		
	
		String gdate = Encoder.getGeneralizedTime(snapshot_date);//
		/*
		Application.db.updateNoSync(table.peer.TNAME, new String[]{table.peer.last_sync_date}, new String[]{table.peer.global_peer_ID},
				new String[]{gdate, _global_peer_ID});
		*/
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setLastSyncDate(instance, gdate);
		peer.releaseReference();
		
		// Update date for the address that has contacted me
		String crtDate = Util.getGeneralizedTime();
		if (address_ID != null) {
			peer.updateAddress_LastConnection(crtDate, address_ID);
//			Application.db.updateNoSync(table.peer_address.TNAME, new String[]{table.peer_address.my_last_connection}, new String[]{table.peer_address.peer_address_ID},
//					new String[]{crtDate, address_ID});
		} else {
			// search for address using UpdateMessages.equalAddress(addr,isa)
			peer.updateAddress_LastConnection(crtDate, s_address);
//			ArrayList<String> address_IDs = peer.getAddressIDs();
//			ArrayList<ArrayList<Object>> p_data=
//				Application.db.select(
//						"SELECT "+table.peer_address.address+", "+table.peer_address.peer_address_ID+" from "+table.peer_address.TNAME+
//						" WHERE "+table.peer_address.peer_ID+" = ? AND "+table.peer_address.type+" = 'Socket'",
//						new String[]{_peer_ID});
//			for (ArrayList<Object> p: p_data) {
//				String address = p.get(0).toString();
//				String _address_ID = p.get(1).toString();
//				if (equalAddress(address,s_address)) {
//					peer.updateAddress_LastConnection(crtDate, _address_ID);
//					//Application.db.updateNoSync(table.peer_address.TNAME, new String[]{table.peer_address.my_last_connection}, new String[]{table.peer_address.peer_address_ID},
//					//		new String[]{crtDate, _address_ID});	
//					break;
//				}
//			}
		}
		Application.db.sync(new ArrayList<String>(Arrays.asList(table.peer.TNAME, table.peer_address.TNAME)));
		if (DEBUG) out.println("UpdateMessages:integrateUpdate: done");
		return future_requests;
	}
	
	private static void handleChangedOrgs(ASNSyncPayload asa, String _peer_ID) throws P2PDDSQLException {
		Calendar reset = null;
		/**
		 * Here we take the earlier among the reset dates for the orgs of this peer
		 */
		for (ResetOrgInfo roi: asa.changed_orgs) {
			if (reset == null){
				reset = roi.reset_date;
			}
			if (reset.before(roi.reset_date)){
				reset = roi.reset_date;
			}
		}
		if (reset != null) {
			//String instance = null;
			String old = D_Peer.getLastResetDate(_peer_ID, asa.get_peer_instance());
			if (DEBUG || DD.DEBUG_CHANGED_ORGS) err.println("UpdateMessages:integrateUpdate: changed_orgs: old reset="+old);
			Calendar _old = null;
			if (old != null) 
				_old = Util.getCalendar(old);
			if ((_old == null) || _old.before(reset)) {
				D_Peer.reset(_peer_ID, asa.get_peer_instance(), reset);
				if (DEBUG || DD.DEBUG_CHANGED_ORGS) err.println("UpdateMessages:integrateUpdate: changed_orgs: called reset="+old+" new="+Encoder.getGeneralizedTime(reset));

//					String date = Util.getGeneralizedTime();//Encoder.getGeneralizedTime(reset);
//					Application.db.updateNoSync(table.peer_address.TNAME,
//							new String[]{table.peer.last_reset, table.peer.last_sync_date},
//							new String[]{table.peer.peer_ID}, new String[]{date, null, _peer_ID}, DEBUG);
			}
		} else {
			if (DEBUG || DD.DEBUG_CHANGED_ORGS) err.println("UpdateMessages:integrateUpdate: changed_orgs: called reset=null");
		}
	}
	/**
	 * return true if not empty
	 * @param sp
	 * @param _peer_ID
	 * @param generalizedTime
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static boolean store_detected_interests(SpecificRequest sp,
			long _peer_ID, String generalizedTime, D_Peer peer) throws P2PDDSQLException {
		boolean result = false;
		//boolean DEBUG = true;
		if(DEBUG) out.println("UpdateMessages: store_detected_interests: start");
		for (RequestData rq: sp.rd) {
			String org = rq.global_organization_ID_hash;
			long orgID = Util.lval(D_Organization.getLocalOrgID_fromHashIfNotBlocked(org), -1);
			if(orgID<=0){
				if(_DEBUG) out.println("UpdateMessages:store_detected_interests: blocked="+org);
				continue;
			}
			if(DEBUG) out.println("UpdateMessages:store_detected_interests: not blocked="+rq);
			
			OrgPeerDataHashes old = new OrgPeerDataHashes(orgID);
			old.add(rq, _peer_ID, generalizedTime);
			if(!rq.empty()) result = true;
			old.save(orgID, _peer_ID, peer);
			/*
			RequestData old = new RequestData(orgID);
			old.add(rq, _peer_ID, generalizedTime);
			old.save(orgID);
			*/
		}
		return result;
	}
	/**
	 *  // check existing/non-blocked data and insert wished one into sp, store sp in orgs
	 * @param advertised
	 * @param sp
	 * @throws P2PDDSQLException 
	 */
	private static void evaluate_interest(SpecificRequest advertised, SpecificRequest sp) throws P2PDDSQLException {
		if((advertised == null)||(advertised.rd==null)) return;
		for (RequestData rq : advertised.rd) {
			RequestData sp_rq = new RequestData();
			sp_rq.global_organization_ID_hash = rq.global_organization_ID_hash;
			evaluate_interest(rq, sp_rq);
			if(!sp_rq.empty()) sp.rd.add(sp_rq);
		}
	}
	/**
	 * // check existing/non-blocked data and insert wished one into sp, store sp in orgs
	 * @param rq
	 * @param sp_rq
	 * @throws P2PDDSQLException 
	 */
	private static void evaluate_interest(RequestData advertised, RequestData sp_rq) throws P2PDDSQLException {
		String orgHash = advertised.global_organization_ID_hash;
		String orgID = D_Organization.getLocalOrgID_fromHashIfNotBlocked(orgHash);
		if(orgID==null) {
			if(_DEBUG) System.out.println("UpdateMessages: evaluate_interest: failure no local unblocked orgID for "+orgHash);
			return;
		}
		sp_rq.cons = D_Constituent.checkAvailability(advertised.cons, orgID, DEBUG);
		sp_rq.witn = D_Witness.checkAvailability(advertised.witn, orgID, DEBUG);
		sp_rq.neig = D_Neighborhood.checkAvailability(advertised.neig, orgID, DEBUG);
		sp_rq.moti = D_Motion.checkAvailability(advertised.moti, orgID, DEBUG);
		sp_rq.just = D_Justification.checkAvailability(advertised.just, orgID, DEBUG);
		sp_rq.sign = D_Vote.checkAvailability(advertised.sign, orgID, DEBUG); // set this debug to true to check votes delivery
		sp_rq.news = D_News.checkAvailability(advertised.news, orgID, DEBUG);
		sp_rq.tran = D_Translations.checkAvailability(advertised.tran, orgID, DEBUG);
	}
	public static Hashtable<String,Hashtable<Long,String>> getMissingPeers() {
		String missing_peers = null;
		try {
			missing_peers = DD.getExactAppText(DD.MISSING_PEERS);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if (missing_peers != null) {
			byte data[] = Util.byteSignatureFromString(missing_peers);
			Decoder d = new Decoder(data);
			Hashtable<String, Hashtable<Long, String>> _peers = OrgPeerDataHashes.decodeData(d.getFirstObject(true));
			return _peers;
		}
		return new Hashtable<String,Hashtable<Long,String>>();
	}
	static void saveMissingPeers(Hashtable<String,Hashtable<Long,String>> peers) {
		Encoder e = OrgPeerDataHashes.getDataEncoder(peers);
		String data = Util.stringSignatureFromByte(e.getBytes());
		try {
			DD.setAppTextNoSync(DD.MISSING_PEERS, data);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
	}
	private static void integrate_peer_GIDs_accounting(Hashtable<String, RequestData> missing_sr,
			Hashtable<String, RequestData> obtained_sr, String _global_peer_ID, long _peer_ID, D_Peer peer,
			String date) throws P2PDDSQLException {
		Hashtable<String,Hashtable<Long,String>> peers = getMissingPeers(); // new ArrayList<String>();
		for (RequestData v : missing_sr.values()) {
			for (String p: v.peers.keySet()) {
				if (! peers.contains(p)){
					Hashtable<Long,String> cause = new Hashtable<Long,String>();
					cause.put(_peer_ID, v.peers.get(p));
					peers.put(p, cause);
				} else {
					Hashtable<Long,String> h = peers.get(p);
					if (!h.contains(_peer_ID))
						h.put(_peer_ID, v.peers.get(p));
				}
			}
		}
		
		for (RequestData v : obtained_sr.values()) {
			for (String p: v.peers.keySet())
				if (peers.contains(p)) peers.remove(p);
		}
		saveMissingPeers(peers);		
	}
	private static void integrate_peer_GIDs_accounting(Hashtable<String, String> missing_peers, String _global_peer_ID, long _peer_ID, D_Peer peer,
			String date) throws P2PDDSQLException {
		Hashtable<String,Hashtable<Long,String>> peers = getMissingPeers(); // new ArrayList<String>();
		for (String p : missing_peers.keySet()) {
			if (! peers.contains(p)) {
				Hashtable<Long,String> cause = new Hashtable<Long,String>();
				cause.put(_peer_ID, missing_peers.get(p));
				peers.put(p, cause);
			} else {
				Hashtable<Long,String> h = peers.get(p);
				if (!h.contains(_peer_ID))
					h.put(_peer_ID, missing_peers.get(p));
			}
		}
		
		saveMissingPeers(peers);		
	}
	
	/**
	 * For these orgs in keys, purge obtained from their store requests
	 * @param missing_sr   : new unknown detected
	 * @param obtained_sr :  obtained entities
	 * @param _global_peer_ID : not used now, but eventually may be saved to know from where we have learned and where we should ask a dependency
	 * @param _peer_ID
	 * @throws P2PDDSQLException
	 */
	private static void integrate_GIDs_accounting(Hashtable<String, RequestData> missing_sr,
			Hashtable<String, RequestData> obtained_sr, String _global_peer_ID, long _peer_ID, D_Peer peer) throws P2PDDSQLException {
		Set<String> toreq = missing_sr.keySet();
		Set<String> got = obtained_sr.keySet();
		Set<String> orgs =	new HashSet<String>();//Collections.emptySet();
		orgs.addAll(toreq);
		orgs.addAll(got);
		
		String date = Util.getGeneralizedTime();
		integrate_peer_GIDs_accounting(missing_sr, obtained_sr, _global_peer_ID, _peer_ID, peer, date);
		
		for (String o : orgs) {
			long oID = D_Organization.getLocalOrgID(o);
			//String orgID_hash = D_Organization.getOrgIDhash(oID);
			
			OrgPeerDataHashes _opdh = new OrgPeerDataHashes(oID); //orgID_hash
			_opdh.add(missing_sr.get(o), _peer_ID, date);
			_opdh.purge(obtained_sr.get(o));
			_opdh.save(oID, _peer_ID, peer);
			/*
			RequestData _rq = new RequestData(oID); //, orgID_hash);
			_rq.add(sq_sr.get(o));
			_rq.purge(obtained_sr.get(o));
			_rq.save(oID);
			*/
		}
	}
	/**
	 * For these orgs, purge obtained from their store requests
	 * @param orgs
	 * @param obtained
	 * @throws P2PDDSQLException
	 */
	/*
	 * Deprecated since RequestData no longer used for this data structure 
	@Deprecated
	private static void purge(HashSet<String> orgs, RequestData obtained) throws P2PDDSQLException {
		if(obtained==null) return;
		if(obtained.empty()) return;
		for(String o: orgs) {
			long orgID = D_Organization.getLocalOrgID(o);
			RequestData rq = new RequestData(orgID);
			rq.purge(obtained);
			rq.save(orgID);
		}
	}
	@Deprecated
	private static RequestData getOldDataRequest(D_Organization orgData) throws P2PDDSQLException {
		long orgID = D_Organization.getLocalOrgID(orgData.global_organization_ID);
		return new RequestData(orgID);
	}
	*/
	@Deprecated
	private static void updateDataToRequest(D_Organization orgData, RequestData rq) throws P2PDDSQLException {
		long orgID = D_Organization.getLocalOrgID(orgData.global_organization_ID);
		rq.save(orgID);
	}
	/*
	@Deprecated
	private static boolean equalAddress(String address, InetSocketAddress s_a) {
		Address ad = new Address(address);
		if((address==null)||(ad.domain==null)||(ad.udp_port<=0)){
			out.println("Empty address "+address+" ("+ad.domain+"):("+ad.udp_port+") from: "+s_a);
			return false;
		}
		InetSocketAddress isa = new InetSocketAddress(ad.domain, ad.udp_port);
		if(
				(isa.getPort()==s_a.getPort()) &&
				( Util.getNonBlockingHostName(isa).equals(Util.getNonBlockingHostName(s_a)) ||
				  (isa.isUnresolved()&&s_a.isUnresolved()&&
				    equalByteArrays(isa.getAddress().getAddress(),s_a.getAddress().getAddress())))) return true;
		return false;
	}
	private static boolean equalByteArrays(byte[] a1, byte[] a2) {
		if((a1==null) || (a2==null)) return a1==a2;
		if(a1.length != a2.length) return false;
		for(int k=0; k<a1.length; k++) if(a1[k]!=a2[k]) return false;
		return true;
	}
	*/
	static public long getonly_constituent_ID(String global_constituent_ID) throws P2PDDSQLException{
		long result=-1;
		if(DEBUG) System.out.println("\n************\nUpdateMessages:getonly_constituentID':  start gcID= = "+Util.trimmed(global_constituent_ID));		
		String sql="SELECT "+table.constituent.constituent_ID+" FROM "+table.constituent.TNAME+" WHERE "+table.constituent.global_constituent_ID+" = ?";
		ArrayList<ArrayList<Object>> dt=Application.db.select(sql, new String[]{global_constituent_ID}, DEBUG);
		if((dt.size()>=1) && (dt.get(0).size()>=1))
			result = Long.parseLong(dt.get(0).get(0).toString());
		if(DEBUG) System.out.println("UpdateMessages:getonly_constituentID':  exit result = "+result);		
		if(DEBUG) System.out.println("****************");		
		return result;
	}
	static public long get_news_ID(String global_news_ID, long constituentID, long organizationID, String date, String news, String type, String signature) throws P2PDDSQLException {
		long result=0;
		ArrayList<ArrayList<Object>> dt=Application.db.select("SELECT "+table.news.news_ID+" FROM "+table.news.TNAME+" WHERE "+table.news.global_news_ID+" = ?",
				new String[]{global_news_ID});
		if((dt.size()>=1) && (dt.get(0).size()>=1)) {
			result = Long.parseLong(dt.get(0).get(0).toString());
			return result;
		}
		result=Application.db.insert(table.news.TNAME,new String[]{table.news.constituent_ID,table.news.organization_ID,table.news.creation_date,table.news.news,table.news.type,table.news.signature,table.news.global_news_ID},
				new String[]{constituentID+"", organizationID+"", date, news, type, signature, global_news_ID});
		return result;
	}
	
	static public long getonly_organizationID(String global_organizationID, String orgID_hash) throws P2PDDSQLException {
		long result=-1;
		if(DEBUG) System.out.println("\n************\nUpdateMessages:getonly_organizationID':  start orgID_hash= = "+orgID_hash);		
		if ((global_organizationID!=null)||(orgID_hash!=null)) {
			String sql="SELECT "+table.organization.organization_ID+
				" FROM "+table.organization.TNAME+
				" WHERE "+table.organization.global_organization_ID+" = ? OR "+table.organization.global_organization_ID_hash+" = ?";
			ArrayList<ArrayList<Object>> dt=Application.db.select(sql, new String[]{global_organizationID, orgID_hash}, DEBUG);
			if((dt.size()>=1) && (dt.get(0).size()>=1)) {
				result = Long.parseLong(dt.get(0).get(0).toString());
				//return result;
			}
		}
		if(DEBUG) System.out.println("UpdateMessages:getonly_organizationID':  exit result = "+result);		
		if(DEBUG) System.out.println("****************");		
		return result;
	}
	/**
	 * Get ID and/or insert temporary org entry
	 * @param global_organizationID
	 * @param org_name
	 * @param adding_date
	 * @param orgHash
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long get_organizationID(String global_organizationID, String org_name, String adding_date, String orgHash) throws P2PDDSQLException {
		long result=0;
		if(DEBUG) System.out.println("\n************\nUpdateMessages:getonly_organizationID':  start orgID_hash= = "+Util.trimmed(global_organizationID));		
		if(global_organizationID==null) return -1;
		String sql = "SELECT "+table.organization.organization_ID+", "+table.organization.name
			+" FROM "+table.organization.TNAME
			+" WHERE "+table.organization.global_organization_ID+" = ?";
		String sql_hash = "SELECT "+table.organization.organization_ID+", "+table.organization.name
			+" FROM "+table.organization.TNAME
			+" WHERE "+table.organization.global_organization_ID_hash+" = ?";
		ArrayList<ArrayList<Object>> dt;
		if(orgHash!=null) dt=Application.db.select(sql_hash, new String[]{orgHash}, DEBUG);
		else dt=Application.db.select(sql, new String[]{global_organizationID}, DEBUG);
		if((dt.size()>=1) && (dt.get(0).size()>=1)) {
			result = Long.parseLong(dt.get(0).get(0).toString());
			String oname = (String)dt.get(0).get(1);
			if(!Util.equalStrings_null_or_not(oname,org_name)) Application_GUI.warning(String.format(_("Old name for org: %1$s new name is: %2$s"),oname,org_name), _("Inconsistency"));
			//return result;
		}else
			result=Application.db.insert(table.organization.TNAME,
					new String[]{table.organization.name,table.organization.global_organization_ID,table.organization.global_organization_ID_hash},
				new String[]{org_name, global_organizationID, orgHash}, DEBUG);
		if(DEBUG) System.out.println("UpdateMessages:get_organizationID':  exit result = "+result);		
		if(DEBUG) System.out.println("****************");		
		return result;
	}
	
	public static OrgFilter[] getOrgFilter(String peer_ID){
		OrgFilter[] orgFilter=null;
		ArrayList<ArrayList<Object>> peers_orgs = null;
		try{
			peers_orgs = Application.db.select("SELECT o."+table.organization.global_organization_ID+", o."+table.organization.global_organization_ID_hash+", p."+table.peer_org.last_sync_date +
					" FROM " + table.peer_org.TNAME + " AS p " +
					" LEFT JOIN "+table.organization.TNAME+" AS o ON (p."+table.peer_org.organization_ID+"=o."+table.organization.organization_ID+")" +
					" WHERE p."+table.peer_org.served+"=1 AND p."+table.peer_org.peer_ID+" = ? " +
					" AND o."+ table.organization.requested  +"= '1'"+
							" ORDER BY p."+table.peer_org.last_sync_date+" ASC;",
					new String[]{peer_ID});
		} catch (P2PDDSQLException e1) {
				Application_GUI.warning(_("Database: ")+e1, _("Database"));
				return null;
		}
		orgFilter = new OrgFilter[peers_orgs.size()];
		for(int of=0; of<peers_orgs.size(); of++) {
				OrgFilter f=orgFilter[of]=new OrgFilter();
				f.orgGID = Util.getString(peers_orgs.get(of).get(0));
				f.orgGID_hash = Util.getString(peers_orgs.get(of).get(1));
				f.setGT(Util.getString(peers_orgs.get(of).get(2)));
		}
		return orgFilter;
	}
}
