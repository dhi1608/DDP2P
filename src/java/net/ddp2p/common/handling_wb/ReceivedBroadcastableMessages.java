/*   Copyright (C) 2012 Osamah Dhannoon
		Author: Osamah Dhannoon: odhannoon2011@fit.edu
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
package net.ddp2p.common.handling_wb;
import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Calendar;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Message;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_OrgConcepts;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.simulator.WirelessLog;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.streaming.UpdatePeersTable;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class ReceivedBroadcastableMessages {
	public static long all_msg_received=0;
	public static long all_myInterest_msg_received=0;
	public static long all_msg_received_fromMyself=0;
	public static long all_not_myInterest_msg_received=0;
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private static final int MY_PEER_BAG_MAX_SIZE = 10;
	public static D_Message public_msg;
	private static boolean CHECK_MESSAGE_SIGNATURES = false;
	public static ArrayList<String> my_bag_of_peers = new ArrayList<String>();
	static String my_GPIDhash;
	private static long last =  Util.CalendargetInstance().getTimeInMillis()-10000;
	private static long crt;
	public static int count_dots = 0;
	/**
	 * 
	 * @param obtained
	 * @param _amount  starting point in received message buffer?
	 * @param address
	 * @param length
	 * @param IP
	 * @param cnt_val
	 * @throws P2PDDSQLException
	 * @throws ASN1DecoderFail
	 * @throws IOException
	 */
	public static void integrateMessage(byte[] obtained, int _amount, SocketAddress address,
									int length, String IP, long cnt_val, String Msg_Time) throws P2PDDSQLException, ASN1DecoderFail, IOException {
		D_Peer __peer = null;
		PreparedMessage pm = new PreparedMessage();
		pm.raw=obtained;
		public_msg = null;
		if(DEBUG)System.out.println("ReceivedBroadcastableMessages : integrateMessage : msg : "+pm.raw);
		Decoder dec = new Decoder(obtained);
		D_Message msg = new D_Message().decode(dec);
		if(DEBUG)System.out.println("integrateMessage : After Decoding msg : msg.vote.org_ID :"+msg.vote.getOrganizationGID());
		public_msg = msg;
		if(obtained!=null)if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage: msg received");
		try{
			long added_org=-1,added_motion=-1,added_constituent=-1,
					added_vote=-1,added_peer=-1,added_witness=-1, added_neigh=-1;
			check_global_peerID(msg); 
			{	
				all_msg_received++;
				if(net.ddp2p.common.handling_wb.BroadcastQueueRequested.myInterests!=null && msg.organization!=null)
				{
					pm.org_ID_hash = msg.organization.getGIDH();
					boolean exists = false;
					if(net.ddp2p.common.handling_wb.BroadcastQueueRequested.myInterests.org_ID_hashes!=null)
					for(int i=0; i<net.ddp2p.common.handling_wb.BroadcastQueueRequested.myInterests.org_ID_hashes.size(); i++)
						if(net.ddp2p.common.handling_wb.BroadcastQueueRequested.myInterests.org_ID_hashes.get(i).equals(pm.org_ID_hash))
						{
							exists = true;
							all_myInterest_msg_received++;
							break;
						}
					if(!exists)
					{
						all_not_myInterest_msg_received++;
						if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage: So far : Total msg received:"+all_msg_received+" Total myInterest msg received:"+ all_myInterest_msg_received+" Total msg received not my interest:"+all_not_myInterest_msg_received);
						return;
					}
				}
				if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage: So far : Total msg received:"+all_msg_received+" Total myInterest msg received:"+ all_myInterest_msg_received+" Total msg received not my interest:"+all_not_myInterest_msg_received);
				if((msg.organization!=null)&&(msg.constituent==null)&&(msg.witness==null)&&(msg.vote==null)) { 
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage:RECEIVE ORGANIZATION ONLY"); 
					int result = D_Organization.isOrgAvailableForGIDH(msg.organization.getGIDH(), false);
					pm.org_ID_hash = msg.organization.getGIDH();
					added_org=handle_org(pm, msg.organization);
					WirelessLog.RCV_logging(WirelessLog.org_type,msg.sender.component_basic_data.globalID,pm.raw,length,result,IP,cnt_val,Msg_Time);
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage:.");
					count_dots++;
				}
				if((msg.constituent!=null)&&(msg.neighborhoods==null)&&(msg.vote==null)&&(msg.witness==null)) { 
					if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:RECEIVE CONSTITUENT");
					long added_Org=-1;
					String goid = null;
					if(msg.organization!=null) {
						pm.org_ID_hash = msg.organization.getGIDH();
						added_Org = handle_org(pm, msg.organization); 
					}
					if(goid == null) {
						goid = msg.constituent.getOrganizationGID();
						pm.constituent_ID_hash.add(msg.constituent.getGIDH());
					}
					if(goid==null){
						if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage: So far : Total msg received:"+all_msg_received+" Total myInterest msg received:"+ all_myInterest_msg_received+" Total msg received not my interest:"+all_not_myInterest_msg_received);
						return;
					}
					if(added_Org<=0) added_Org = D_Organization.getLIDbyGID(goid);
					if(added_Org<=0) added_Org = D_Organization.insertTemporaryGID_long(goid, __peer);
					int result = D_Constituent.isGIDHash_available(msg.constituent.getGIDH(), added_Org, false);				
					added_constituent = handle_constituent(pm,msg.constituent,goid,added_Org, __peer);
					WirelessLog.RCV_logging(WirelessLog.const_type,msg.sender.component_basic_data.globalID,pm.raw,length,result,IP,cnt_val,Msg_Time);
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage: .");
					count_dots++;
				}
				if((msg.witness!=null)&&(msg.constituent==null)&&(msg.vote==null)){
					if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage: RECEIVE Witness : "+msg.witness);
					long added_Org = -1;
					long added_witnessed_cons = -1;
					long added_witnessing_cons = -1;
					String goid = null;
					if(msg.organization!=null){
						pm.org_ID_hash=msg.organization.getGIDH();
						added_Org = handle_org(pm, msg.organization); 
					}
					goid = msg.witness.global_organization_ID;
					if(msg.witness.witnessed!=null) added_witnessed_cons = handle_constituent(pm,msg.witness.witnessed,goid,added_Org, __peer);
					if(msg.witness.witnessing!=null) added_witnessing_cons = handle_constituent(pm, msg.witness.witnessing,goid,added_Org, __peer);
					int result = D_Witness.isGIDavailable(msg.witness.global_witness_ID, false);
					added_witness = handle_witness(pm,msg.witness, __peer);
					WirelessLog.RCV_logging(WirelessLog.wit_type,msg.sender.component_basic_data.globalID,pm.raw,length,result,IP,cnt_val,Msg_Time);
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage:.");
					count_dots++;
				}
				if((msg.neighborhoods!=null)&&(msg.witness==null)&&(msg.vote==null)&&(msg.organization==null)) {
					if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:RECEIVE Neighborhoods hierarchy");
					int result = D_Neighborhood.isGIDavailable(msg.neighborhoods[0].getGID(), added_org, false);
					for(int i=0;i<msg.neighborhoods.length;i++) {
						if(DEBUG)System.out.println("Parent_ID : "+msg.neighborhoods[i]);
						added_neigh = handle_Neighborhoods(pm,msg.neighborhoods[i], added_org, __peer);
						if(DEBUG)System.out.println("RCV NID : "+added_neigh);
					}
					WirelessLog.RCV_logging(WirelessLog.neigh_type,msg.sender.component_basic_data.globalID,pm.raw,length,result,IP,cnt_val,Msg_Time);
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage:.");
					count_dots++;
				}
				if(msg.vote!=null) { 
					if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:RECEIVE VOTE in ReceivedBroacastableMessage:integrateMessage"); 
					if(msg.organization!=null) { 
						pm.org_ID_hash=msg.organization.getGIDH();
						long added_Org = handle_org(pm, msg.organization);
						if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:ORG ID : "+added_Org);
						long added_cons =  handle_constituent(pm, msg.vote.getConstituent_force(),msg.organization.getGID(),added_Org, __peer);
						if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:CONS ID : "+added_cons);
					}
					int result = D_Vote.isGIDavailable(msg.vote.getGID(), false);
					added_vote = handle_vote(pm, msg.vote, __peer);
					WirelessLog.RCV_logging(WirelessLog.vote_type,msg.sender.component_basic_data.globalID,pm.raw,length,result,IP,cnt_val,Msg_Time);
					if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:VOTE DATA : "+msg.vote.getGID());
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage:.");
					count_dots++;
				}
				if(msg.Peer!=null){
					if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:RECEIVE PEER");
					int result = D_Peer.isGIDavailable(msg.Peer.component_basic_data.globalIDhash, false);
					added_peer = get_peer(msg.Peer);
					WirelessLog.RCV_logging(WirelessLog.peer_type,msg.sender.component_basic_data.globalID,pm.raw,length,result,IP,cnt_val,Msg_Time);
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage:.");
					count_dots++;
				}
				if(count_dots%40==0) System.out.println();
			}
		}catch(SignatureVerificationException e){e.printStackTrace();}
		if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:So far : Total msg received:"+all_msg_received+" Total myInterest msg received:"+ all_myInterest_msg_received+" Total msg received not my interest:"+all_not_myInterest_msg_received);
	}
	private static String GOID_by_local(long org_id) throws P2PDDSQLException {
		String sql = "select "+net.ddp2p.common.table.organization.global_organization_ID+
				" from "+net.ddp2p.common.table.organization.TNAME+" WHERE "+net.ddp2p.common.table.organization.organization_ID+
				"=?;";
		ArrayList<ArrayList<Object>> org = Application.getDB().select(sql, new String[]{Util.getStringID(org_id)});
		if(org.size()==0) return null;
		String GOid = Util.getString(org.get(0).get(0));
		return GOid;
	}
	/**
	 * check the new incoming vote if its new add it and return the ID. if its already exist in
		the db do not add it just return the ID
	 * @param vote
	 * @return
	 * @throws P2PDDSQLException
	 * @throws SignatureVerificationException
	 */
	private static long handle_vote(PreparedMessage pm, D_Vote vote, D_Peer __peer) throws P2PDDSQLException, SignatureVerificationException {
		long o_ID = -1; 
		long motion_id = -1;
		if (vote.getMotionFromObjOrLID() != null) {
			if (DEBUG) System.out.println("with MOTION");
			if (DEBUG) System.out.println("MOTION Data : "+vote.getMotionFromObjOrLID());
			if (o_ID <= 0) o_ID = D_Organization.getLIDbyGID(vote.getMotionFromObjOrLID().getGID()); 
			RequestData sol_rq = new RequestData();
			RequestData new_rq = new RequestData();
			D_Motion m = D_Motion.getMotiByGID(vote.getMotionFromObjOrLID().getGID(), true, true, true, __peer, o_ID, null);
			m.loadRemote(vote.getMotionFromObjOrLID(), sol_rq, new_rq, __peer); 
			motion_id = m.storeRequest_getID();
			m.releaseReference();
			if (DEBUG) System.out.println("MOTION ID : "+motion_id);
		}
		else 
			if (DEBUG) System.out.println("WITHOUT MOTION");
		long just_id = -1;
		if(vote.getJustificationFromObjOrLID()!=null) {
			if(DEBUG)System.out.println("with JUSTIFICATION");
			if(DEBUG)System.out.println("JUST Data : "+vote.getJustificationFromObjOrLID());
			RequestData sol_rq = new RequestData();
			RequestData new_rq = new RequestData();
			try {
				D_Justification j = D_Justification.getJustByGID(vote.getJustificationFromObjOrLID().getGID(), true, true, true, __peer, o_ID, motion_id, vote.getJustificationFromObjOrLID());
				j.loadRemote(vote.getJustificationFromObjOrLID(), sol_rq, new_rq, __peer);
				just_id = j.storeRequest_getID();
				j.releaseReference();
			} catch(Exception e){e.printStackTrace();}
			if(DEBUG)System.out.println("JUST ID : "+just_id);
		}
		else 
			if(DEBUG)System.out.println("without justificatio");
		long vote_id = -1;
		RequestData sol_rq = new RequestData();
		RequestData new_rq = new RequestData();
		try{
			vote_id =  vote.store(pm, sol_rq, new_rq, __peer);
		}catch(Exception e){e.printStackTrace();}
		if(DEBUG)System.out.println("vote ID : "+vote_id);
		return vote_id;
	}
	private static long handle_witness(PreparedMessage pm, D_Witness witness, D_Peer __peer) throws P2PDDSQLException {
		long add_witness =-1;
		RequestData sol_rq = new RequestData();
		RequestData new_rq = new RequestData();
		try{
			add_witness = witness.store(sol_rq, new_rq, __peer);
			if(DEBUG)System.out.println("handle_witness : after witness.stor");
		}catch(Exception e){e.printStackTrace();}
		return add_witness;
	}
	/**
	 * check the new incoming constituent if its new add it and return the ID. if its already exist in
	   the db do not add it just return the ID
	 * @param constituent
	 * @param goid
	 * @param added_Org
	 * @return
	 * @throws P2PDDSQLException
	 * @throws SignatureVerificationException
	 */
	private static long handle_constituent(PreparedMessage pm,D_Constituent constituent, String goid, long added_Org, D_Peer __peer) throws P2PDDSQLException, SignatureVerificationException {
		String now = Util.getGeneralizedTime();
		if(DEBUG)System.out.println("Org_id : "+added_Org);
		if(DEBUG)System.out.println("Global_Org_id : "+goid);
		if(DEBUG)System.out.println("handle_constituent: Neighborhood data : "+constituent.getNeighborhood()[0]);
		if(DEBUG)System.out.println("Reaceived : handle_constituent() : CON HASH : "+constituent.getGIDH());
		constituent.setArrivalDate(now);
		constituent.setOrganization(goid, added_Org);
		System.out.println("ReceivedBroadcasted : handle_constituent: should handle new received and detect references, in org");
		RequestData sol_rq = null; 
		RequestData new_rq = null; 
		return constituent.storeRemoteThis(pm, sol_rq, new_rq, __peer);
	}
	private static long handle_Neighborhoods(PreparedMessage pm,D_Neighborhood neigh, long orgLID, D_Peer __peer) {
		long neigh_id = -1;
		RequestData sol_rq = new RequestData();
		RequestData new_rq = new RequestData();
		String orgGID = null;
		try{
			neigh_id = neigh.storeRemoteThis(orgGID, orgLID, Util.getGeneralizedTime(), sol_rq, new_rq, __peer);
		}catch(Exception e){e.printStackTrace();}
		return neigh_id;
	}
	/**
	 * check the new incoming org if its new add it and return the ID. if its already exist in
	   the db do not add it just return the ID
	 * @param ORG
	 * @return
	 * @throws P2PDDSQLException
	 * @throws SignatureVerificationException
	 */
	private static long handle_org(PreparedMessage pm, D_Organization ORG) throws P2PDDSQLException, SignatureVerificationException {
		if(ORG == null) return -1;
		ArrayList<ArrayList<Object>> orgs = null;
		String sql = "select "+net.ddp2p.common.table.organization.organization_ID+
				" from "+net.ddp2p.common.table.organization.TNAME+" WHERE "+net.ddp2p.common.table.organization.global_organization_ID+
				"=?;";
		orgs = Application.getDB().select(sql, new String[]{ORG.getGID()});
		if(orgs.isEmpty()) 
		{
			if(DEBUG)System.out.println("Its a new Organization : "+ORG.creator);
			return insert_org(ORG,true);
		}
		else {
			if(DEBUG)System.out.println("we already have this Organization");
			if(DEBUG)System.out.print(" it's ID is : "+orgs.get(0).get(0)+"\n");
			return Integer.parseInt(Util.getString(orgs.get(0).get(0)));
		}		
	}
	/**
	 * insert the new org in the db and return it's id
	 * @param ORG
	 * @param needs_verification
	 * @return
	 * @throws P2PDDSQLException
	 * @throws SignatureVerificationException
	 */
	public static long insert_org(D_Organization ORG, boolean needs_verification) throws P2PDDSQLException, SignatureVerificationException {	
		if(DEBUG) System.out.println("ReceivedBroadcastable:insert_org: org="+ORG);
		if(needs_verification) {
			if(!ORG.verifySignature())  
				throw new SignatureVerificationException("Invalid Signature");
		}
		ORG.broadcasted = true;
		ORG = D_Organization.storeRemote(ORG, null);
		long org_id = ORG.getLID_forced(); 
		if(DEBUG)System.out.println("New Org ID is : "+org_id);
		return org_id;
	}
	/**
	 * check the incoming peer who create the org if it's new or already exist
	 * @param creator
	 * @return
	 * @throws P2PDDSQLException
	 * @throws SignatureVerificationException
	 */
	private static long get_peer(D_Peer creator) throws P2PDDSQLException, SignatureVerificationException {
		ArrayList<ArrayList<Object>> peers = null;
		String sql = "select "+net.ddp2p.common.table.peer.peer_ID+
				" from "+net.ddp2p.common.table.peer.TNAME+" WHERE "+net.ddp2p.common.table.peer.global_peer_ID+
				"=?;";
		peers = Application.getDB().select(sql, new String[]{creator.component_basic_data.globalID});
		if(peers.isEmpty()) 
		{
			if(DEBUG)System.out.println("Its a new Peer");
			return add_peer(creator, true);
		}
		else {
			if(DEBUG)System.out.println("We already have this Peer");
			if(DEBUG)System.out.println("Old peer ID is : "+peers.get(0).get(0));
			return Integer.parseInt(peers.get(0).get(0).toString());
		}
	}
	public static long add_peer(D_Peer creator, boolean needs_verification) throws P2PDDSQLException, SignatureVerificationException {
		String signature = Util.stringSignatureFromByte(creator.getSignature());
		if(DEBUG)System.out.println("SIGNATURE : "+signature);
		creator.component_local_agent_state.arrival_date=Util.CalendargetInstance();
		String crt_date = Encoder.getGeneralizedTime(creator.component_local_agent_state.arrival_date);
		if (needs_verification) {
			if((creator == null) || !creator.verifySignature())
				if(!DD.ACCEPT_UNSIGNED_PEERS_FROM_TABLES) throw new SignatureVerificationException("Invalid Signature");
		}
		D_Peer p = D_Peer.storeReceived(creator, true, false, crt_date);
		long peer_id = p.getLID_keep_force();
		if(DEBUG)System.out.println("New Peer ID is : "+peer_id);
		return peer_id;
	}
	/**
	 * Check if it should be discarded:
	 *  - you are receiving from the same machine
	 *  - signature fails (CHECK_MESSAGE_SIGNATURES)
	 * @param msg
	 * @return
	 * @throws P2PDDSQLException
	 * @throws IOException 
	 */
	private static boolean check_global_peerID(D_Message msg) throws P2PDDSQLException, IOException {
		String my_GPID = HandlingMyself_Peer.getMyPeerGID();
		String hash_GID = Util.getGIDhash(msg.sender.component_basic_data.globalID);
		my_GPIDhash = Util.getGIDhash(my_GPID);
		if(hash_GID.compareTo(my_GPIDhash)==0)  {
			if(DEBUG)System.out.println("RCV from myself");
			return true;
			}
		if(CHECK_MESSAGE_SIGNATURES ){
		}
		my_bag_of_peers.remove(hash_GID);
		my_bag_of_peers.add(hash_GID);
		if(my_bag_of_peers.size()>MY_PEER_BAG_MAX_SIZE) my_bag_of_peers.remove(0);
		if ((msg.recent_senders!=null)&&(msg.recent_senders.contains(my_GPIDhash))){
			net.ddp2p.common.config.Application_GUI.playThanks();
		}
		return false;
	}
}
