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
package net.ddp2p.common.hds;
import static java.lang.System.out;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_PeerInstance;
import net.ddp2p.common.data.D_RecommendationOfTestersBundle;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.recommendationTesters.MessageReceiver;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.streaming.UpdateMessages;
import net.ddp2p.common.util.CommEvent;
import net.ddp2p.common.util.DDP2P_ServiceThread;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class UDPServerThread extends net.ddp2p.common.util.DDP2P_ServiceThread {
	private static final HashSet<SocketAddress> handled = new HashSet<SocketAddress>();
	public static boolean DEBUG = false;
	public static boolean _DEBUG = true;
	DatagramPacket pak;
	UDPServer us;
	String peer_address;
	private static int verification_warning = 0;
	private static final int MAX_VERIFICATION_WARNING = 3;
	/**
	 * Handle a UDP packet
	 * @param _pak
	 * @param _us
	 */
	public UDPServerThread(DatagramPacket _pak, UDPServer _us){
		super("UDP Server Thread", false);
		pak = _pak; us = _us;
		InetSocketAddress sa = (InetSocketAddress) pak.getSocketAddress();
		try {
			peer_address = sa.getAddress().getHostAddress()+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+sa.getPort();
		} catch(Exception e){};
	}
	static int k = 0;
	public void _run() {
		this.setName("UDP Server Thread: "+(k++));
		try{
			synchronized(us.lock){
				us.incThreads();
			}
			try{
				__run();
			}catch(Exception e){
				e.printStackTrace();
			}
			synchronized(us.lock){
				us.decThreads();
				us.lock.notify();
			}
			if(us.getThreads() < 0) {
				System.err.println("UDPServerThread:run:Threads number under 0: "+ us.getThreads()+"/"+UDPServer.MAX_THREADS);
			}
		}catch(Exception e){e.printStackTrace();}
	}
	public void __run() {
		if(DEBUG)System.out.println("UDPServerThread:run: Running UDPHandler thread:"+us.getThreads()+" from"+pak.getSocketAddress());
		byte[] buffer = pak.getData();
		byte[] msg=null;
		msg = pak.getData();
		Decoder dec = new Decoder(pak.getData(), pak.getOffset(), pak.getLength());
		if (dec.getTypeByte() == DD.TAG_AC12) { 
			if (DEBUG) System.out.println("UDPServer:run: Fragment received, will decode");
			UDPFragment frag = new UDPFragment();
			msg = null; 
			try {
				frag.decode(dec);
				if (DEBUG) System.out.println("UDPServer:run: receives fragment "+frag.getSequence()+"/"+frag.getFragments()+" from:"+pak.getSocketAddress());
				if (frag.getFragments() > DD.UDP_MAX_FRAGMENTS) {
					if (_DEBUG) System.out.println("UDPServer:run: Too Many Fragments for local security check: max = "+DD.UDP_MAX_FRAGMENTS+" incoming: "+frag);
					return;
				}
				if ((frag.getData()!=null) && (frag.getData().length > DD.UDP_MAX_FRAGMENT_LENGTH)) {
					if (_DEBUG) System.out.println("UDPServer:run: Too Large Fragments: "+frag.getData().length);
					return;
				}
				msg = us.getFragment(frag, pak.getSocketAddress());
				if (DEBUG) System.out.println("UDPServer:run: Fragment received: "+frag);
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (msg == null) {
				if (DEBUG) System.out.println("UDPServer:run: Fragment not ready");
				return;
			}
			dec = new Decoder(msg);
			if (DEBUG) System.out.println("UDPServer:run: Continuing to decode message!");
		} else {
			if (dec.getTypeByte()==DD.TAG_AC11) { 
				UDPFragmentAck frag = new UDPFragmentAck();
				try {
					frag.decode(dec);
					if(DEBUG)System.out.println("UDPServer:run: Packet received is fragment "+frag.msgID+" ack from: "+pak.getSocketAddress());
					us.getFragmentAck(frag, pak.getSocketAddress());
				} catch (ASN1DecoderFail e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			} else {
				if(dec.getTypeByte()==DD.TAG_AC15){
					UDPFragmentNAck frag = new UDPFragmentNAck();
					try {
						frag.decode(dec);
						if (DEBUG) System.out.println("UDPServer:run: Packet received is fragment "+frag.msgID+" nack from: "+pak.getSocketAddress());
						us.getFragmentNAck(frag);
					} catch (ASN1DecoderFail e) {
						e.printStackTrace();
					}
					return;
				} else {
				  if (dec.getTypeByte() == DD.TAG_AC16) {
					if (DEBUG) System.out.println("UDPServer:run: Packet received is fragments reclaim");
					UDPReclaim frag = new UDPReclaim();
					try {
						frag.decode(dec);
					} catch (ASN1DecoderFail e) {
						e.printStackTrace();
					}
					try {
						us.getFragmentReclaim(frag, pak.getSocketAddress());
					} catch (IOException e) {
						e.printStackTrace();
					}
					return;					
				  }
				}
			}
		}
		if (DEBUG) System.out.println("UDPServer:run: Message received will be interpreted");
		if	(dec.getTypeByte()	==	DD.TAG_AC13) {
			ASNUDPPing aup = new ASNUDPPing();
			try { 
				if (DEBUG) System.out.println("UDPServer:run: UDPServer receives ping from:"+pak.getSocketAddress());
				if (DEBUG) System.out.println("UDPServer:run: UDPServer attempts decoding Ping");
				aup.decode(dec);
				if (DEBUG || DD.DEBUG_COMMUNICATION_STUN) System.out.println("UDPServer:run: receives: "+aup+" from:"+pak.getSocketAddress());
				if (aup.senderIsPeer) {
					Connections.acknowledgeReply(aup, pak.getSocketAddress());
					if (! handleSTUNfromPeer(aup)) return;
				} else {
					if (DEBUG) System.out.println("UDPServer:run: receives forwarded ping from directory/initiator! "+aup);
					if (! handleSTUNForward(aup)) return;
				}			
				if (DEBUG) System.out.println("UDPServer:run: UDPServer pinged on request: "+aup);
			} catch (ASN1DecoderFail e) {
				if (DEBUG) System.out.println("Ping decoding failed! "+e);
				e.printStackTrace();
			} catch (P2PDDSQLException e) {
				if (DEBUG) System.out.println("Database access failed! "+e);
				e.printStackTrace();
			}
			return;
		}
		if (dec.getTypeByte() == DirectoryAnnouncement_Answer.getASN1Type()) {
		  if (DEBUG) System.out.println("UDPServer:run: Announcement answer decoding!");
		  try {
			Decoder answer_dec=dec;
			DirectoryAnnouncement_Answer daa=new DirectoryAnnouncement_Answer(answer_dec);
			if (DEBUG) out.println("UDPServer: Directory Answer: "+daa.result);
			if (Application.directory_status != null) {
				Application.directory_status.setUDPOn(peer_address, new Boolean(true));
			} else {
				System.out.println("UDPServer:gotAnnouncementAnswer no display");
			}
		  } catch(Exception e){
			  if (DEBUG) System.out.println("UDPServer: UDP Announcement answer decoding failed! "+e);
			  if (Application.directory_status != null)
					Application.directory_status.setUDPOn(peer_address, new Boolean(false));
		  }
		  return;
		}
		if (dec.getTypeByte() == net.ddp2p.common.hds.DirectoryAnswerMultipleIdentities.getASN1Type()) { 
			try {
				DirectoryAnswerMultipleIdentities dami = new DirectoryAnswerMultipleIdentities(dec);
				net.ddp2p.common.hds.Connections.registerIncomingDirectoryAnswer(dami, pak);
				if (DEBUG) out.println("UDPServer: DirectoryAnswerMultipleIdentities: Directory Answer: "+dami);
				if (Application.directory_status != null) {
					Application.directory_status.setUDPOn(peer_address, new Boolean(true));
				} else {
					System.out.println("UDPServer: DirectoryAnswerMultipleIdentities no display");
				}
			} catch (ASN1DecoderFail e) {
				  if(DEBUG)System.out.println("UDPServer: DirectoryAnswerMultipleIdentities: UDP Announcement answer decoding failed! "+e);
				  if(Application.directory_status!=null)
						Application.directory_status.setUDPOn(peer_address, new Boolean(false));
				e.printStackTrace();
			}
			return;
		}
		if (dec.getTypeByte()==DD.TAG_AC7) {
			if (DEBUG) System.out.println("\n*******************\nUDPServer:run: Trying to decode request!");
			handleRequest(dec, msg);
			return;
		}
		if (dec.getTypeByte() == DD.TAG_AC8) {
			if (DEBUG || DD.DEBUG_COMMUNICATION) System.out.println("\n*************\nUDPServer:run: Answer received fully from "+pak.getSocketAddress());
			handleAnswer(dec);
			return;
		}
		if (DEBUG) System.out.println("Unknown message["+pak.getLength()+"]: "+Util.byteToHexDump(buffer));
		if (msg != null) if (DEBUG) System.out.println(" msg["+msg.length+"]="+Util.byteToHexDump(msg));
		return;
	}
	private void handleAnswer(Decoder dec) {
		SyncAnswer sa = new SyncAnswer();
		try {
			sa.decode(dec);
			if (DEBUG) System.out.println("***\n   UDPServerThread: handleAnswer: Answer from "+pak.getSocketAddress()+" inst=["+sa.peer_instance+"] /upto:"+Encoder.getGeneralizedTime(sa.upToDate)+"\n"+sa);
			if (DEBUG) System.out.println("UDPServer:run: Answer received & decoded from: "+pak.getSocketAddress());
			if (DEBUG || DD.DEBUG_COMMUNICATION) System.out.println("UDPServerThread: handleAnswer: Answer received is: "+sa.toSummaryString());
			if (DEBUG || DD.DEBUG_COMMUNICATION || DD.DEBUG_CHANGED_ORGS)
				System.out.println("\n\n\nUDPServerThread: handleAnswer: Answer rcv! ch_org="+Util.nullDiscrimArraySummary(sa.changed_orgs,"--"));
			String global_peer_ID = sa.responderGID;
			D_Peer peer = null;
			String peer_ID = null;
			boolean blocked[] = new boolean[]{true};
			if (global_peer_ID != null) {
				String gidHash = null;
				String gid = global_peer_ID;
				if (D_Peer.isGIDHash(global_peer_ID)) {
					gidHash = global_peer_ID;
					gid = null;
				}
				peer = D_Peer.getPeerByGID_or_GIDhash(gid, gidHash, true, false, false, null);
				if (DEBUG) System.out.println("   UDPServerThread: handleAnswer: Answer from "+pak.getSocketAddress()+" "+peer.getName()+":"+peer.getInstance()+" /"+Encoder.getGeneralizedTime(sa.upToDate));
				if (DEBUG || DD.DEBUG_TMP_GIDH_MANAGEMENT) {
					if (sa.peer_instance == null) {
						System.out.println("UDPServerThread: handleAnswer: Answer from "+peer.getName()+" inst=["+sa.peer_instance+"] /upto:"+Encoder.getGeneralizedTime(sa.upToDate));
					} else {
						System.out.println("UDPServerThread: handleAnswer: Answer from "+peer.getName()+" inst=["+sa.peer_instance.peer_instance+"] /upto:"+Encoder.getGeneralizedTime(sa.upToDate));
					}
					if (_DEBUG) System.out.println("UDPServerThread: handleAnswer: adv=" + sa.advertised);
					if (_DEBUG) System.out.println("UDPServerThread: handleAnswer: orgs=" + sa.advertised_orgs);
					if (_DEBUG) System.out.println("UDPServerThread: handleAnswer: o_hash=" + sa.advertised_orgs_hash);
				}
				if (peer != null) {
					peer_ID = peer.getLIDstr(); 
					blocked[0] = peer.getBlocked();
				}
			} else {
				if (_DEBUG) System.out.println("   UDPServerThread: handleAnswer: unknown peer GID");
			}
			if ((peer_ID != null) && blocked[0]) {
				if (_DEBUG) System.out.println("   UDPServerThread: handleAnswer: blocked LID="+peer_ID);
				return;
			}
			if (peer_ID == null) {
				if (DEBUG || DD.DEBUG_TODO) System.out.println("UDPServerThread:_run: Answer received from unknown peer: "+global_peer_ID);
			} else
				if (Application.peers != null) Application.peers.setConnectionState(peer_ID, DD.PEERS_STATE_CONNECTION_UDP);
			if (DEBUG) System.out.println("UDPServer:run: Answer received will be integrated");
			InetSocketAddress saddr = (InetSocketAddress) pak.getSocketAddress();
			String address_ID = null;
			if (peer_ID != null) address_ID = D_Peer.getAddressID (saddr, peer_ID);
			RequestData rq = new RequestData(); 
			if (peer != null)
				if (DEBUG) System.out.println("UDPServerThread: handleAnswer: answer from safe: "+peer.getName()+" from IP: "+saddr.getAddress()+":"+saddr.getPort()+" upto: "+Encoder.getGeneralizedTime(sa.upToDate)+" e: "+sa.elements());
			if (( UpdateMessages.integrateUpdate(sa, saddr, this, global_peer_ID, sa.peer_instance, peer_ID, address_ID, rq, peer, true)) ||
					((rq != null) && !rq.empty()) ) {
				if (DEBUG) System.out.println("UDPServerThread: handleAnswer: Should reply with request for: "+rq);
				DD.touchClient();
			}
			/**
			 * Integrate recommendations of testers if available.
			 */
			if (sa.recommendation_testers != null) {
				try {
					MessageReceiver.msgListener(peer, sa.recommendation_testers);
				} catch (Exception e) {e.printStackTrace();}
			}
			if (DEBUG) System.out.println("UDPServerThread: handleAnswer: Answer received and integrated "+pak.getSocketAddress());
			if (DEBUG) if ((rq != null) && !rq.empty()) System.out.println("UDPServerThread: handleAnswer: Should reply with request for: "+rq);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} 
	}
	/**
	 * Called when a SyncRequest message was detected.
	 * @param dec
	 * @param msg
	 */
	private void handleRequest(Decoder dec, byte[] msg) {
		  ASNSyncRequest asr;
		  SocketAddress psa = null;
		  try { 
			psa = pak.getSocketAddress();
			if (DD.DROP_DUPLICATE_REQUESTS) {
				synchronized (handled) {
					if (handled.contains(psa)) {
						if (DEBUG || DD.DEBUG_COMMUNICATION) System.out.println("UDPServer: Abandoned duplicate request from: "+psa);
						return;
					}
					handled.add(psa);
				}
			}
			asr = new ASNSyncRequest();
			asr.decode(dec);
			if (! registerASR(asr, peer_address, msg)) {
				if (DEBUG) System.out.println("UDPServerThread: handleRequest: Drop duplicate request from: "+psa+" "+Util.nullDiscrim(asr.getPeerName())+":"+asr.getInstanceDisplay_ASR_and_Peer()+" /"+Encoder.getGeneralizedTime(asr.lastSnapshot));
				if (true || ! DD.RELEASE) return;
			}
			if (_DEBUG) System.out.println("UDPServerThread: handleRequest: Request from: "+psa+" "+Util.nullDiscrim(asr.getPeerName())+":"+asr.getInstanceDisplay_ASR_and_Peer()+" /"+Encoder.getGeneralizedTime(asr.lastSnapshot) + " len=" + msg.length);
			if (DEBUG || DD.DEBUG_TMP_GIDH_MANAGEMENT) System.out.println("UDPServerThread: handleRequest: specific requests=" + asr.request);
			if (asr.pushChanges != null) {
				if (DEBUG || DD.DEBUG_TMP_GIDH_MANAGEMENT) System.out.println("UDPServerThread: handleRequest: advertised=" + asr.pushChanges.advertised);
				if (DEBUG || DD.DEBUG_TMP_GIDH_MANAGEMENT) System.out.println("UDPServerThread: handleRequest: advertised_orgs=" + asr.pushChanges.advertised_orgs);
				if (DEBUG || DD.DEBUG_TMP_GIDH_MANAGEMENT) System.out.println("UDPServerThread: handleRequest: advertised_orgs_hash=" + asr.pushChanges.advertised_orgs_hash);
			}
			if (DEBUG) System.out.println("UDPServerThread: handleRequest: Received request from: "+psa);
			if (DEBUG || DD.DEBUG_COMMUNICATION) System.out.println("UDPServerTh: Decoded request: "+asr.toSummaryString()+" from: "+psa);
			if (DEBUG || DD.DEBUG_COMMUNICATION || DD.DEBUG_CHANGED_ORGS)
				if (asr.pushChanges != null) System.out.println("\n\n\nUDPServerTh: handleReq: Request rcv! ch_org="+Util.nullDiscrimArraySummary(asr.pushChanges.changed_orgs,"--"));
			if ( ! DD.ACCEPT_STREAMING_REQUEST_UNSIGNED && ! asr.verifySignature()) {
				DD.ed.fireServerUpdate(new CommEvent(this, null, psa, "UDPServerTh", "Unsigned Sync Request received: "+asr));
				System.err.println("UDPServerTh: handleReq: Unsigned Request received: "+asr.toSummaryString());
				System.err.println("UDPServerTh: handleReq: Unsigned Request received: "+asr.toString());
				throw new Exception("Unsigned request");
			}
			if ( ! Server.extractDataSyncRequest(asr, psa, this)) {
				if (_DEBUG) System.out.println("UDPServerTh: handleReq: Request Discarded ************************* \""+((asr.address == null)?"Unknown":asr.address.getName())+"\"");
				return;
			}
			if (DEBUG) System.out.println("UDPServerTh: handleReq: Request Data extracted *************************");
			String peerGID = null, instance = null;
			if (asr.address != null) {
				peerGID = asr.address.component_basic_data.globalID;
				instance = asr.address.getInstance();
			}
			else {
				if (DEBUG) System.out.println("UDPServerTh:handleRequest: request from UNKNOWN abandoned");
				if (DEBUG) System.out.println("UDPServerTh:handleRequest: Answer not sent!");
				throw new Exception("Unknown peer");
			}
			if (UDPServer.transferringPeerAnswerMessage(peerGID, instance)) {
				if(DEBUG)System.out.println("UDPServerTh:handleRequest: UDPServer Answer being sent for: "+Util.trimmed(peerGID));
				return;
			}
			String peer_ID = null;
			D_Peer peer_sender = D_Peer.getPeerByGID_or_GIDhash(peerGID, null, true, false, false, null);
			boolean blocked[] = new boolean[] {true}; 
			if (peer_sender != null) {
				peer_ID = peer_sender.getLIDstr_keep_force(); 
				blocked = new boolean[] {peer_sender.getBlocked()};
			}
			if (asr.address != null) asr.address.setLID(peer_ID);
			if ((Application.peers != null) && (peer_ID != null)) Application.peers.setConnectionState(peer_ID, DD.PEERS_STATE_CONNECTION_UDP);
			if (blocked[0] && (peer_sender != null)) {
				if (DEBUG) System.out.println("UDPServer:run: Blocked! "+peer_ID+" "+((asr.address!=null)?asr.address.component_basic_data.name:"noname"));
				return;
			}
			if (peer_sender != null) {
				if (DEBUG) System.out.println("UDPServerThread:handleRequest: request from safe: "+peer_sender.getName()+" from IP: "+psa+" from: "+Encoder.getGeneralizedTime(asr.lastSnapshot));
				ArrayList<Address> adr = ClientSync.peer_contacted_addresses.get(peer_sender.getGID());
				if (adr == null) {
					adr = new ArrayList<Address>();
					ClientSync.peer_contacted_addresses.put(peer_sender.getGID(), adr);
					Address e = new Address(psa, asr.dpi);
					adr.add(e);
				}
			}
			SyncAnswer sa = UpdateMessages.buildAnswer(asr, peer_ID);
			if (sa == null) {
				if (_DEBUG) System.out.println("UDPServerThread: handleRequest: Null Reply to "+psa+" "+asr.getPeerName()+":"+asr.getInstanceDisplay_ASR_and_Peer()+" /"+Encoder.getGeneralizedTime(null));
				return;
			}
			/**
			 * If any recommendation of testers is scheduled for this peer (load it, if not in cache) and send it now!
			 */
			try {
				String recomendation_testers = peer_sender.getTesterRecommendationBundleASN1Base64();
				if (recomendation_testers != null) sa.recommendation_testers = Util.byteSignatureFromString(recomendation_testers);
			} catch (Exception e){e.printStackTrace();}
			D_Peer p = net.ddp2p.common.data.HandlingMyself_Peer.get_myself_with_wait();
			sa.peer_instance = p.getPeerInstance(p.getInstance());
			if (peer_sender != null) if (DEBUG) System.out.println("UDPServerThread:handleRequest: return "+sa.elements()+" upto: "+Encoder.getGeneralizedTime(sa.upToDate));
			if (DEBUG) System.out.println("UDPServer:run: Prepared answer!");
			if (DEBUG || DD.DEBUG_TMP_GIDH_MANAGEMENT) System.out.println("UDPServerTh:handleRequest: Prepared answer advertised "+sa.advertised);
			if (DEBUG || DD.DEBUG_TMP_GIDH_MANAGEMENT) System.out.println("UDPServerTh:handleRequest: Prepared answer advertised "+Util.concat(sa.advertised_orgs, ";\n ", null));
			if (DEBUG || DD.DEBUG_TMP_GIDH_MANAGEMENT) System.out.println("UDPServerTh:handleRequest: Prepared answer advertised "+Util.concat(sa.advertised_orgs_hash, ";\n ", null));
			if (DEBUG) System.out.println("\n\nPrepared answer: "+sa.toString());
			byte[] sa_msg = sa.encode();
			if (DEBUG) System.out.println("\n\nPrepared answer2: "+new ASNSyncPayload().decode(new Decoder(sa_msg)).toString());
			if (DEBUG) System.out.println("   UDPServerThread: handleRequest: Reply to "+psa+" "+asr.getPeerName()+":"+asr.getInstanceDisplay_ASR_and_Peer()+" /"+Encoder.getGeneralizedTime(sa.upToDate)+"\n"+sa);
			us.sendLargeMessage(psa, sa_msg, DD.MTU, peerGID, DD.MSGTYPE_SyncAnswer);
			if (DEBUG || DD.DEBUG_COMMUNICATION || DD.DEBUG_CHANGED_ORGS)
				System.out.println("\n\n\nUDPServerTh:handleRequest: Answer sent! ch_org="+Util.nullDiscrimArraySummary(sa.changed_orgs,"--"));
		  } catch (ASN1DecoderFail e) {
			e.printStackTrace();
		  } catch (P2PDDSQLException e) {
			e.printStackTrace();
		  } catch (IOException e) {
			e.printStackTrace();
		  } catch (Exception e) {
			e.printStackTrace();
		  }
		  if (DD.DROP_DUPLICATE_REQUESTS) {
			  synchronized(handled) {
				  if (psa != null) handled.remove(psa);
			  }
		  }
	}
	class ASR_Record {
		String syncDate;
		byte[] randomID; 
		byte[] hash;
		int msg_len;
		Calendar timestamp;
		/**
		 * 
		 * msg is the encoded syncreq
		 * @param asr
		 * @param msg
		 */
		public ASR_Record(ASNSyncRequest asr, byte[] msg) {
			syncDate = Encoder.getGeneralizedTime(asr.lastSnapshot);
			randomID = asr.randomID;
			timestamp = Util.CalendargetInstance();
			hash = Util.simple_hash(msg, Cipher.SHA1);
			msg_len = msg.length;
		}
		/**
		 * msg is the encoded syncreq
		 * @param asr
		 * @param msg
		 * @return
		 */
		public boolean same(ASNSyncRequest asr, byte[] msg) {
			if (msg_len != msg.length)
				return false;
			if (! Util.equalStrings_null_or_not(syncDate, Encoder.getGeneralizedTime(asr.lastSnapshot)))
				return false;
			if (! Util.equalBytes_null_or_not(randomID, asr.randomID))
				return false;
			byte[] _hash = Util.simple_hash(msg, Cipher.SHA1);
			if (! Util.equalBytes_null_or_not(hash, _hash))
				return false;
			Calendar _timestamp = Util.CalendargetInstance();
			_timestamp.add(Calendar.MINUTE, -1);
			if (_timestamp.after(timestamp)) {
				if (DEBUG) System.out.println("UDPServerThread: same: timeout");
				return false;
			}
			if (DEBUG)
				System.out.println("UDPServerThread: same: "
						+ " len=" + msg.length
						+ " date=" + syncDate
						+ " rnd=" + Util.concat(asr.randomID, ":", "NULL")
						+ " hash=" + Util.concat(_hash, ":", "NULL")
						);
			return true;
		}
		public String toString() {
			return ""+syncDate+":"+Util.byteToHex(randomID)+" t="+Encoder.getGeneralizedTime(timestamp);
		}
	}
	static Hashtable <String,ASR_Record> registeredASR = new Hashtable <String,ASR_Record>();
	public boolean registerASR(ASNSyncRequest asr, String peer_address, byte[] msg) {
		boolean __DEBUG = DEBUG;
		String key;
		ASR_Record nou = null;
		if (asr.randomID != null && asr.randomID.length > DD.CLIENTS_RANDOM_MEMORY) return false;
		synchronized (registeredASR) {
			if (registeredASR.size() > DD.CLIENTS_NB_MEMORY) {
				registeredASR.clear();
			}
			if (peer_address != null) {
				key = peer_address;
				ASR_Record old = registeredASR.get(key);
				if (old != null) {
					if (old.same(asr, msg)) {
						if (__DEBUG) System.out.println("UDPServerThread: registerASR: same: old="+old);
						return false;
					}
				}
				registeredASR.put(key, nou = new ASR_Record(asr, msg));
				if (__DEBUG) System.out.println("UDPServerThread: registerASR: new:"+key+" -> "+nou);
			} else {
				if (__DEBUG) System.out.println("UDPServerThread: registerASR: null socket");
			}
			if (asr.address != null) {
				key = asr.address.getGIDH_force();
				ASR_Record old = registeredASR.get(key);
				if (old != null) {
					if (old.same(asr, msg)) {
						if (__DEBUG) System.out.println("UDPServerThread: registerASR: same: old+"+old);
						return false;
					}
				}
				registeredASR.put(key, nou=new ASR_Record(asr, msg));
				if (__DEBUG) System.out.println("UDPServerThread: registerASR: new:"+key+" -> "+nou);
			} else {
				if (__DEBUG) System.out.println("UDPServerThread: registerASR: null address");
			}
		}
		if (nou == null) {
			if (__DEBUG) System.out.println("UDPServerThread: registerASR: not same");
		}
		return true;
	}
	private boolean handleSTUNForward(ASNUDPPing aup) throws P2PDDSQLException {
		boolean DEBUG = UDPServerThread.DEBUG || DD.DEBUG_COMMUNICATION_STUN;
		if (DEBUG || DD.DEBUG_COMMUNICATION_STUN)
			System.out.println("UDPServer: handleSTUNForward: Ping received "+aup);
		if (!aup.senderIsInitiator) { 
			if (Application.directory_status != null)
				Application.directory_status.setNATOn(peer_address, new Boolean(true));
		}
		if (Identity.getMyPeerGID().equals(aup.initiator_globalID)) {
			if (DEBUG) System.out.println("UDPServer:run: Ping received from myself! Abandon.");
			return true;
		}
		if (!Util.equalStrings_null_or_not(Identity.getMyPeerGID(), aup.peer_globalID)) {
			if (DEBUG) System.out.println("UDPServer:run: Ping received for smbdy else!"+aup+" \n\tme="+Identity.getMyPeerGID());
			aup.peer_globalID = Identity.getMyPeerGID();
			aup.peer_instance = Identity.getMyPeerInstance();
		}
		if (!Util.equalStrings_null_or_not(Identity.getMyPeerInstance(), aup.peer_instance)) {
			if (DEBUG) System.out.println("UDPServer:run: Ping received for other instance!"+aup+" \n\tme="+Identity.getMyPeerInstance());
			aup.peer_instance = Identity.getMyPeerInstance();
		}
		aup.senderIsPeer = true;
		InetSocketAddress next_dest;
		String peer_ID = D_Peer.getPeerLIDbyGID(aup.initiator_globalID);
		if (aup.senderIsInitiator == true) {
			aup.senderIsInitiator = false;
			next_dest = (InetSocketAddress) pak.getSocketAddress();
			if(Application.peers!=null) Application.peers.setConnectionState(peer_ID, DD.PEERS_STATE_CONNECTION_UDP_NAT);
		}else{
			next_dest = new InetSocketAddress(aup.initiator_domain,aup.initiator_port);
			if(Application.peers!=null) Application.peers.setConnectionState(peer_ID, DD.PEERS_STATE_CONNECTION_UDP);
		}
		byte[] buf = aup.encode();
		DatagramPacket reqDP = new DatagramPacket(buf, buf.length);
		reqDP.setSocketAddress(next_dest);
		try {
			us.getUDPSocket().send(reqDP);
			if(DEBUG)System.out.println("UDPServer:run: ping returned to initiator: "+next_dest+" ping="+aup);
		} catch (IOException e) {
			e.printStackTrace();
			return true;
		}
		return false;
	}
	/**
	 * When a DDP2P STUN message is received from a remote peer, send a SyncRequest.
	 * Do nothing if the peer is unknown 
	 * 
	 * @param aup
	 * @return
	 * @throws ASN1DecoderFail
	 */
	private boolean handleSTUNfromPeer(ASNUDPPing aup) throws ASN1DecoderFail {
		boolean DEBUG = UDPServerThread.DEBUG || DD.DEBUG_COMMUNICATION_STUN;
		if (DEBUG || DD.DEBUG_COMMUNICATION_STUN)
			System.out.println("UDPServer: handleSTUNfromPeer: Ping received "+aup);
		synchronized (us.lock_reply) {
			String g_peerID = aup.peer_globalID;
			if (g_peerID == null) {
				if (_DEBUG) System.out.println("UDPServer:run: receives null GID ping: "+aup+" from:"+pak.getSocketAddress());
				return false;
			}
			String instance = aup.peer_instance;
			if (DD.AVOID_REPEATING_AT_PING && !Application.getG_UDPServer().hasSyncRequests(g_peerID, instance)) {
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Received ping confirmation already handled from peer"));
				if (DEBUG || DD.DEBUG_COMMUNICATION_STUN) System.out.println("UDPServer:run: Ping already handled for: "+Util.trimmed(g_peerID));
				return false;					
			}
			D_Peer peer = D_Peer.getPeerByGID_or_GIDhash(g_peerID, null, true, false, false, null);
			if (peer == null) {
				if (DEBUG || DD.DEBUG_COMMUNICATION_STUN) System.out.println("UDPServer:run: Abandon Ping from unknown peer: "+Util.trimmed(g_peerID));
				return false; 
			}
			DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Received ping confirmation from peer"));
			if (DEBUG) System.out.println("UDPServer:run: GID ping: "+aup+" from: "+pak.getSocketAddress());
			Application.getG_UDPServer().delSyncRequests(g_peerID, instance);
			D_PeerInstance dpi = peer.getPeerInstance(instance);
			String _lastSnapshotString = null;
			Calendar lastSnapshotString = null;
			String inst = null;
			if (dpi != null) {
				_lastSnapshotString = dpi.get_last_sync_date_str(); 
				lastSnapshotString = dpi.get_last_sync_date(); 
				inst = dpi.get_peer_instance();
			}
			if (DEBUG) System.out.println("UDPServer:run: Request being sent at snapshot: "+_lastSnapshotString+" to: "+peer.getName()+":"+inst+" GID: "+Util.trimmed(g_peerID));
			/** Checks whether any message is being exchanged (sending request or receiving payload, and cancel sending request if true) */
			if (UDPServer.transferringPeerMessage(g_peerID, instance)) {
				if (_DEBUG) System.out.println("UDPServer:run: Request being sent for: "+Util.trimmed(g_peerID));
				return false;
			}
			if (DEBUG) System.out.println("UDPServer:run: will build request! "+Util.trimmed(g_peerID)+" ... time="+_lastSnapshotString);
			if (DEBUG) System.out.println("UDPServer:run: ping reply from: "+peer.getName()+" <"+peer.getEmail()+"> "+" from:"+pak.getSocketAddress());
			String peer_ID = peer.getLIDstr_keep_force();
			boolean filtered = peer.component_preferences.filtered;
			ASNSyncRequest asreq;
			asreq = ClientSync.peer_scheduled_requests.get(g_peerID);
			if (asreq != null) {
				if (DEBUG) System.out.println("\n\n\nUDPServer:run: scheduled Request: "+asreq);
				ClientSync.peer_scheduled_requests.remove(g_peerID);
				asreq.lastSnapshot = lastSnapshotString;
				if (asreq.pushChanges == null)
					asreq.pushChanges = ClientSync.getSyncReqPayload(peer_ID);
				try {
					if (asreq.request == null)
						asreq.request = net.ddp2p.common.streaming.SpecificRequest.getPeerRequest(peer_ID);
					else {
						if (_DEBUG) System.out.println("UDPServerThread: handleSTUNfromPeer: scheduled Request: request already present: "+asreq.request);
					}
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
				if (DEBUG) System.out.println("Client: buildRequests: request=: "+asreq);
				if (Identity.getAmIBroadcastableToMyPeers()) {
					try {
						asreq.address = HandlingMyself_Peer.get_myself_with_wait();
						asreq.dpi = asreq.address.getPeerInstance(asreq.address.getInstance());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (DEBUG || DD.DEBUG_COMMUNICATION || DD.DEBUG_CHANGED_ORGS) {
					if (asreq.pushChanges != null) System.out.println("\n\n\nUDPServer:run: sched Request snd! ch_org="+Util.nullDiscrimArraySummary(asreq.pushChanges.changed_orgs,"--"));
					else System.out.println("\n\n\nUDPServer:run: sched Request snd! ch_org=null");
				}
			} else {
				if (DEBUG || DD.DEBUG_CHANGED_ORGS) System.out.println("\n\n\nUDPServer:run: build Request");
				asreq = ClientSync.buildRequest(_lastSnapshotString, null, peer_ID);
				if (DEBUG || DD.DEBUG_COMMUNICATION || DD.DEBUG_CHANGED_ORGS)
					if (asreq.pushChanges != null) System.out.println("\n\n\nUDPServer:run: Request snd! ch_org="+Util.nullDiscrimArraySummary(asreq.pushChanges.changed_orgs,"--"));
					else System.out.println("\n\n\nUDPServer:run: Request snd! ch_org=null");
			}
			if (filtered) asreq.orgFilter = UpdateMessages.getOrgFilter(peer_ID);
			SK sk = asreq.sign();
			net.ddp2p.ciphersuits.PK _pk = null;
			if (DD.VERIFY_SENT_SIGNATURES) {
				try {
					_pk = net.ddp2p.ciphersuits.Cipher.getPK(asreq.address.component_basic_data.globalID);
					if ( ! Cipher.isPair(sk,_pk)) {
						Util.printCallPath("Unmatched keys undecoded");
						System.out.println("\n\n\nUDPServerTh: handleSTUNdromPeer: pk="+_pk);
						System.out.println("\n\n\nUDPServerTh: handleSTUNdromPeer: sk="+sk);
						System.out.println("\n\n\nUDPServerTh: handleSTUNdromPeer: asreq="+asreq);
					}
				}catch(Exception e){e.printStackTrace();}
				boolean r = asreq.verifySignature();
				if (!r) Util.printCallPath("before decoding: failed verifying: "+asreq);
				else
					if(DEBUG)System.out.println("UDPServerTh: handleSTUNdromPeer: before decoded: signature success");
			}
			byte[] buf = asreq.encode();
			if (DD.VERIFY_SENT_SIGNATURES) {
				Decoder d = new Decoder(buf);
				ASNSyncRequest asr = new ASNSyncRequest();
				asr.decode(d);
				if(DEBUG)System.out.println("UDPServer: handleSTUNfromPeer: verif sent Decoded request: "+asr.toSummaryString());
				net.ddp2p.ciphersuits.PK pk=null;
				try {
					pk = net.ddp2p.ciphersuits.Cipher.getPK(asr.address.component_basic_data.globalID);
					if (! Cipher.isPair(sk,pk)) {
						Util.printCallPath("Unmatched keys");
						System.out.println("UDPServerTh: handleSTUNdromPeer: cipher pair failed : pk ="+pk);
						System.out.println("UDPServerTh: handleSTUNdromPeer: cipher pair failed : _pk ="+_pk);
						System.out.println("UDPServerTh: handleSTUNdromPeer: cipher pair failed : pk_eq ="+pk.equals(_pk));
					}
				} catch(Exception e) { e.printStackTrace(); }
				if (!asr.verifySignature()) {
					System.err.println("UDPServerTh: handleSTUNdromPeer: Unsigned Request sent: "+asr.toSummaryString());
					System.err.println("UDPServerTh: handleSTUNdromPeer: Unsigned Request rsent: "+asr.toString());
					System.err.println("UDPServerTh: handleSTUNdromPeer: Unsigned Request old: "+asreq.toString());
					System.err.println("UDPServerTh: handleSTUNdromPeer: SK was: "+sk);
					System.err.println("UDPServerTh: handleSTUNdromPeer: PK was: "+pk);
					System.err.println("UDPServerTh: handleSTUNdromPeer: old verif: "+asreq.verifySignature(pk, true));
					System.err.println("UDPServerTh: handleSTUNdromPeer: Messages for: "+aup+" peer="+peer);
					if (verification_warning ++ < MAX_VERIFICATION_WARNING)
						new DDP2P_ServiceThread("UDP Server Warning", true) {
						public void _run(){
							Application_GUI.warning(Util.__("Abandoned sending message with inconsistent request")+
									"\n"+
									Util.__("You can disable verification of sent messages!")+
									"\n"+verification_warning+"/"+MAX_VERIFICATION_WARNING,
									Util.__("Message no longer sent!"));
						}
					}.start();
					return false;
				}					
			}
			SocketAddress rsa = new InetSocketAddress(aup.peer_domain,aup.peer_port);
			try {
				us.sendLargeMessage(rsa, buf, DD.MTU, g_peerID, DD.MSGTYPE_SyncRequest);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			if (DEBUG) {
				String slogan=null;
				if (asreq.address != null) slogan = asreq.address.component_basic_data.slogan;
				if (DEBUG)System.out.println("UDPServer:run: sends request: "+slogan+" to: "+rsa);
				if (DEBUG) System.out.println("UDPServerThread:run: sends request: "+asreq+" to: "+rsa);
			}
		}
		return true;
	}
}
