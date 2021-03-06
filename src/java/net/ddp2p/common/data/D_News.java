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
package net.ddp2p.common.data;
import static java.lang.System.out;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.streaming.ConstituentHandling;
import net.ddp2p.common.streaming.MotionHandling;
import net.ddp2p.common.streaming.OrgHandling;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public
class D_News extends ASNObj{
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	public String hash_alg;
	public String global_news_ID;
	public String global_constituent_ID;
	public String global_organization_ID;
	public String global_motion_ID;
	public D_Document_Title title = new D_Document_Title();
	public D_Document news = new D_Document();
	private Calendar creation_date;
	public byte[] signature;
	private Calendar arrival_date;
	public D_Constituent constituent;
	public D_Organization organization;
	public D_Motion motion;
	public String news_ID;
	public String constituent_ID;
	public String organization_ID;
	public String motion_ID;
	public boolean requested = false;
	public boolean blocked = false;
	public boolean broadcasted = D_Organization.DEFAULT_BROADCASTED_ORG;
	public int status_references = 0;
	public static D_News getEmpty() {return new D_News();}
	public D_News() {}
	public D_News(long _news_ID) throws P2PDDSQLException {
		if(_news_ID<=0) return;
		news_ID = ""+_news_ID;
		String sql = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.news.fields,"n")+
			" FROM "+net.ddp2p.common.table.news.TNAME+" AS n "+
			" WHERE n."+net.ddp2p.common.table.news.news_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> m = Application.getDB().select(sql, new String[]{news_ID}, DEBUG);
		if (m.size() == 0) return;
		init(m.get(0));
	}
	public D_News(String news_GID) throws P2PDDSQLException {
		if (news_GID == null) return;
		this.global_news_ID = news_GID;
		String sql = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.news.fields,"n")+
			" FROM "+net.ddp2p.common.table.news.TNAME+" AS n "+
			" WHERE n."+net.ddp2p.common.table.news.global_news_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> m = Application.getDB().select(sql, new String[]{news_GID}, DEBUG);
		if(m.size() == 0) return;
		init(m.get(0));
	}
	/**
			"SELECT "+Util.setDatabaseAlias(table.news.fields,"n")+
			", c."+table.constituent.global_constituent_ID+
			", o."+table.organization.global_organization_ID+
			", m."+table.motion.global_motion_ID+
			" FROM "+table.news.TNAME+" AS n "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=n."+table.news.constituent_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=n."+table.news.organization_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=n."+table.news.motion_ID+")"+
	 * 
	 * @param o
	 * @throws P2PDDSQLException
	 */
	public D_News(ArrayList<Object> o) throws P2PDDSQLException {
		init(o);
	}
	public D_News instance() throws CloneNotSupportedException{
		return new D_News();
	}
	public void setArrivalDate() {
		this.setArrivalDate(Util.CalendargetInstance());
	}
	public Calendar setArrivalDate(Calendar calendargetInstance) {
		return this.arrival_date = calendargetInstance;
	}
	public Calendar getArrivalDate() {
		return this.arrival_date;
	}	
	public void setCreationDate() {
		this.setCreationDate(Util.CalendargetInstance());
	}
	public void setCreationDate(Calendar calendargetInstance) {
		this.creation_date = calendargetInstance;
	}
	public Calendar getCreationDate() {
		return this.creation_date;
	}
	public static D_News getNewsByLID(long _news_LID) {
		try {
			return new D_News(_news_LID);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static D_News getNewsByGID(String _news_GID) {
		try {
			return new D_News(_news_GID);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * 		"SELECT "+Util.setDatabaseAlias(table.news.fields,"n")+
			", c."+table.constituent.global_constituent_ID+
			", o."+table.organization.global_organization_ID+
			", m."+table.motion.global_motion_ID+
			" FROM "+table.news.TNAME+" AS n "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=n."+table.news.constituent_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=n."+table.news.organization_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=n."+table.news.motion_ID+")"+
	 * @param o
	 * @throws P2PDDSQLException
	 */
	void init(ArrayList<Object> o) throws P2PDDSQLException {
		hash_alg = Util.getString(o.get(net.ddp2p.common.table.news.N_HASH_ALG));
		title.title_document.setFormatString(Util.getString(o.get(net.ddp2p.common.table.news.N_TITLE_FORMAT)));
		title.title_document.setDocumentString(Util.getString(o.get(net.ddp2p.common.table.news.N_TITLE)));
		news.setFormatString(Util.getString(o.get(net.ddp2p.common.table.news.N_TEXT_FORMAT)));
		news.setDocumentString(Util.getString(o.get(net.ddp2p.common.table.news.N_TEXT)));
		setCreationDate(Util.getCalendar(Util.getString(o.get(net.ddp2p.common.table.news.N_CREATION))));
		setArrivalDate(Util.getCalendar(Util.getString(o.get(net.ddp2p.common.table.news.N_ARRIVAL))));
		signature = Util.byteSignatureFromString(Util.getString(o.get(net.ddp2p.common.table.news.N_SIGNATURE)));
		news_ID = Util.getString(o.get(net.ddp2p.common.table.news.N_ID));
		constituent_ID = Util.getString(o.get(net.ddp2p.common.table.news.N_CONSTITUENT_ID));
		organization_ID = Util.getString(o.get(net.ddp2p.common.table.news.N_ORG_ID));
		motion_ID = Util.getString(o.get(net.ddp2p.common.table.news.N_MOT_ID));
		this.blocked = Util.stringInt2bool(o.get(net.ddp2p.common.table.news.N_BLOCKED),false);
		this.requested = Util.stringInt2bool(o.get(net.ddp2p.common.table.news.N_REQUESTED),false);
		this.broadcasted = Util.stringInt2bool(o.get(net.ddp2p.common.table.news.N_BROADCASTED), D_Organization.DEFAULT_BROADCASTED_ORG_ITSELF);
		global_news_ID = Util.getString(o.get(net.ddp2p.common.table.news.N_NEWS_GID));
		this.organization = D_Organization.getOrgByLID_NoKeep(organization_ID, true);
		global_organization_ID = D_Organization.getGIDbyLIDstr(organization_ID);
		global_constituent_ID = D_Constituent.getGIDFromLID(constituent_ID);
		this.motion = D_Motion.getMotiByLID(motion_ID, true, false);
		if (motion != null) global_motion_ID = motion.getGID();
	}
	public String toString() {
		return "WB_News: "+
		"\n hash_alg="+hash_alg+
		"\n global_news_ID="+global_news_ID+
		"\n title="+title+
		"\n news="+news+
		"\n constituent="+constituent+
		"\n global_constituent_ID="+global_constituent_ID+
		"\n global_organization_ID="+global_organization_ID+
		"\n global_motion_ID="+global_motion_ID+
		"\n creation_date="+Encoder.getGeneralizedTime(getCreationDate())+
		"\n arrival_date="+Encoder.getGeneralizedTime(getArrivalDate())+
		"\n signature="+Util.byteToHexDump(signature)+
		"\n news_ID="+news_ID+
		"\n constituent_ID="+constituent_ID+
		"\n motion_ID="+motion_ID+
		"\n organization_ID="+organization_ID;
	}
	@Override
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) {
		return getEncoder(dictionary_GIDs, 0);
	}
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs, int dependants) {
		int new_dependants = dependants;
		if (dependants > 0) new_dependants = dependants - 1;
		Encoder enc = new Encoder().initSequence();
		if (hash_alg != null) enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if (global_news_ID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_news_ID);
			enc.addToSequence(new Encoder(repl_GID, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		}
		if (title!=null)enc.addToSequence(title.getEncoder().setASN1Type(DD.TAG_AC2));
		if (news!=null)enc.addToSequence(news.getEncoder().setASN1Type(DD.TAG_AC3));
		if (global_constituent_ID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_constituent_ID);
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		}
		/**
		 * TODO
		 * May decide to comment encoding of "global_organization_ID" out completely, since the org_GID is typically
		 * available at the destination from enclosing fields, and will be filled out at expansion
		 * by ASNSyncPayload.expand at decoding.
		 * However, it is not that damaging when using compression, and can be stored without much overhead.
		 * So it is left here for now.  Test if you comment out!
		 */
		if (global_organization_ID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_organization_ID);
			enc.addToSequence(new Encoder(repl_GID, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		}
		if (getCreationDate() != null) enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC7));
		if (signature != null) enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC8));
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (constituent!=null) enc.addToSequence(constituent.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC10));
			if (organization != null) enc.addToSequence(organization.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC12));
		}
		if (global_motion_ID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_motion_ID);
			enc.addToSequence(new Encoder(repl_GID, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC14));
		}
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (motion != null) enc.addToSequence(motion.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC13));
		}
		return enc;
	}
	public Encoder getSignableEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(global_news_ID!=null)enc.addToSequence(new Encoder(global_news_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(title!=null)enc.addToSequence(title.getEncoder().setASN1Type(DD.TAG_AC2));
		if(news!=null)enc.addToSequence(news.getEncoder().setASN1Type(DD.TAG_AC3));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC5));
		if(global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC7));
		if(global_motion_ID!=null)enc.addToSequence(new Encoder(global_motion_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC9));
		if(getCreationDate()!=null)enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC8));
		return enc;
	}
	public Encoder getHashEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(title!=null)enc.addToSequence(title.getEncoder().setASN1Type(DD.TAG_AC2));
		if(news!=null)enc.addToSequence(news.getEncoder().setASN1Type(DD.TAG_AC3));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC5));
		if(global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC7));
		if(global_motion_ID!=null)enc.addToSequence(new Encoder(global_motion_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC9));
		if(getCreationDate()!=null)enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC8));
		return enc;
	}
	@Override
	public D_News decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0)hash_alg = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if(dec.getTypeByte()==DD.TAG_AC1)global_news_ID = dec.getFirstObject(true).getString(DD.TAG_AC1);
		if(dec.getTypeByte()==DD.TAG_AC2)title = new D_Document_Title().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC3)news = new D_Document().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC4)global_constituent_ID = dec.getFirstObject(true).getString(DD.TAG_AC4);
		if(dec.getTypeByte()==DD.TAG_AC6)global_organization_ID = dec.getFirstObject(true).getString(DD.TAG_AC6);
		if(dec.getTypeByte()==DD.TAG_AC7)setCreationDate(dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC7));
		if(dec.getTypeByte()==DD.TAG_AC8)signature = dec.getFirstObject(true).getBytes(DD.TAG_AC8);
		if(dec.getTypeByte()==DD.TAG_AC10)constituent = D_Constituent.getEmpty().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC12)organization = D_Organization.getOrgFromDecoder(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC14)global_motion_ID = dec.getFirstObject(true).getString(DD.TAG_AC14);
		if(dec.getTypeByte()==DD.TAG_AC13)motion = D_Motion.getEmpty().decode(dec.getFirstObject(true));	
		return this;
	}
	/**
	 * Both store signature and returns it
	 * @return
	 */
	public byte[] sign() {
		return sign(this.global_constituent_ID);
	}
	/**
	 * Both store signature and returns it
	 * @param signer_GID
	 * @return
	 */
	public byte[] sign(String signer_GID) {
		if(DEBUG) System.out.println("WB_Motion:sign: start signer="+signer_GID);
		net.ddp2p.ciphersuits.SK sk = net.ddp2p.common.util.Util.getStoredSK(signer_GID);
		if(sk==null) {
			if(DEBUG) System.out.println("WB_Motion:sign: no signature");
			Application_GUI.warning(Util.__("No secret key! You should select a constituent"), Util.__("No secret key!"));
			return null;
		}
		if(DEBUG) System.out.println("WB_Motion:sign: sign="+sk);
		return sign(sk);
	}
	/**
	 * Both store signature and returns it
	 * @param sk
	 * @return
	 */
	public byte[] sign(SK sk){
		if(DEBUG) System.out.println("WB_Motion:sign: this="+this+"\nsk="+sk);
		signature = Util.sign(this.getSignableEncoder().getBytes(), sk);
		if(DEBUG) System.out.println("WB_Motion:sign:got this="+Util.byteToHexDump(signature));
		return signature;
	}
	public void setGID(String gid) {
		this.global_news_ID = gid;
	}
	public String make_ID(){
		try {
			fillGlobals();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return D_GIDH.d_News+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
	}
	public boolean verifySignature(){
		if(DEBUG) System.out.println("WB_Motion:verifySignature: start");
		String pk_ID = this.global_constituent_ID;
		if((pk_ID == null) && (this.constituent!=null) && (this.constituent.getGID()!=null))
			pk_ID = this.constituent.getGID();
		if(pk_ID == null) return false;
		String newGID = make_ID();
		if(!newGID.equals(this.global_news_ID)) {
			Util.printCallPath("WB_Motion: WRONG EXTERNAL GID");
			if(DEBUG) System.out.println("WB_Motion:verifySignature: WRONG HASH GID="+this.global_news_ID+" vs="+newGID);
			if(DEBUG) System.out.println("WB_Motion:verifySignature: WRONG HASH GID result="+false);
			return false;
		}
		boolean result = Util.verifySignByID(this.getSignableEncoder().getBytes(), pk_ID, signature);
		if(DEBUG) System.out.println("WB_Witness:verifySignature: result wGID="+result);
		return result;
	}
	/**
	 * before call, one should set organization_ID and global_newsID
	 * @param rq
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long store(net.ddp2p.common.streaming.RequestData sol_rq, RequestData new_rq) throws P2PDDSQLException {
		D_Peer __peer = null;
		boolean default_blocked = false;
		boolean locals = fillLocals(new_rq, true, true, true, true);
		if(!locals) return -1;
		if(!this.verifySignature())
			if(! DD.ACCEPT_UNSIGNED_DATA)
				return -1;
		String _old_date[] = new String[1];
		if ((this.news_ID == null) && (this.global_news_ID != null))
			this.news_ID = getLocalIDandDate(this.global_news_ID, _old_date);
		if(this.news_ID != null ) {
			String old_date = _old_date[0];
			if(old_date != null) {
				String new_date = Encoder.getGeneralizedTime(this.getCreationDate());
				if(new_date.compareTo(old_date)<=0) return new Integer(news_ID).longValue();
			}
		}
		net.ddp2p.common.config.Application_GUI.inform_arrival(this, __peer);
		if((this.organization_ID == null ) && (this.global_organization_ID != null))
			this.organization_ID = D_Organization.getLIDstrByGID_(this.global_organization_ID);
		if((this.organization_ID == null ) && (this.global_organization_ID != null)) {
			organization_ID = ""+net.ddp2p.common.data.D_Organization.insertTemporaryGID(global_organization_ID, __peer);
			new_rq.orgs.add(global_organization_ID);
		}
		if((this.constituent_ID == null ) && (this.global_constituent_ID != null))
			this.constituent_ID = D_Constituent.getLIDstrFromGID(this.global_constituent_ID, Util.Lval(this.organization_ID));
		if((this.constituent_ID == null ) && (this.global_constituent_ID != null)) {
			constituent_ID =
				""+D_Constituent.insertTemporaryGID(global_constituent_ID, null, Util.lval(this.organization_ID), __peer, default_blocked);
			new_rq.cons.put(global_constituent_ID,DD.EMPTYDATE);
		}
		if (sol_rq!=null)sol_rq.news.add(this.global_news_ID);
		return storeVerified();
	}
	static long insertTemporaryGID(String const_GID, String org_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("WB_News:insertTemporaryGID: start");
		return Application.getDB().insert(net.ddp2p.common.table.news.TNAME,
				new String[]{net.ddp2p.common.table.news.global_news_ID, net.ddp2p.common.table.news.organization_ID},
				new String[]{const_GID, org_ID},
				DEBUG);
	}
	private static String getDateFor(String newsID) throws P2PDDSQLException {
		String sql = "SELECT "+net.ddp2p.common.table.news.creation_date+" FROM "+net.ddp2p.common.table.news.TNAME+
		" WHERE "+net.ddp2p.common.table.news.news_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.getDB().select(sql, new String[]{""+newsID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public boolean fillLocals(RequestData new_rq, boolean tempOrg, boolean default_blocked_org, boolean tempConst, boolean tempMotion) throws P2PDDSQLException {
		D_Peer __peer = null;
		boolean default_blocked = false;
		boolean default_blocked_mot = false;
		if((global_organization_ID==null)&&(organization_ID == null)){
			Util.printCallPath("cannot store witness with not orgGID");
			return false;
		}
		if((this.global_constituent_ID==null)&&(constituent_ID == null)){
			Util.printCallPath("cannot store witness with not submitterGID");
			return false;
		}
		if((this.global_motion_ID==null)&&(motion_ID == null)){
			Util.printCallPath("cannot store just with no motionGID");
			return false;
		}
		if((global_organization_ID!=null)&&(organization_ID == null)){
			organization_ID = Util.getStringID(D_Organization.getLIDbyGID(global_organization_ID));
			if(tempOrg && (organization_ID == null)) {
				String orgGID_hash = D_Organization.getOrgGIDHashGuess(global_organization_ID);
				if(new_rq!=null)new_rq.orgs.add(orgGID_hash);
				organization_ID = Util.getStringID(D_Organization.insertTemporaryGID(global_organization_ID, orgGID_hash, default_blocked_org, __peer));
				if(default_blocked_org) return false;
			}
			if(organization_ID == null) return false;
		}
		if((this.global_constituent_ID!=null)&&(constituent_ID == null)){
			this.constituent_ID = D_Constituent.getLIDstrFromGID(global_constituent_ID, Util.Lval(this.organization_ID));
			if(tempConst && (constituent_ID == null ))  {
				String consGID_hash = D_Constituent.getGIDHashFromGID(global_constituent_ID);
				if(new_rq!=null)new_rq.cons.put(consGID_hash,DD.EMPTYDATE);
				constituent_ID = Util.getStringID(D_Constituent.insertTemporaryGID(global_constituent_ID, null, Util.lval(organization_ID), __peer, default_blocked));
			}
			if(constituent_ID == null) return false;
		}
		if((this.global_motion_ID!=null)&&(motion_ID == null)){
			this.motion_ID = D_Motion.getLIDstrFromGID(global_motion_ID, Util.lval(organization_ID));
			if(tempMotion && (motion_ID == null ))  {
				if(new_rq!=null)new_rq.moti.add(global_motion_ID);
				motion_ID = Util.getStringID(D_Motion.insertTemporaryGID(global_motion_ID, Util.lval(organization_ID), __peer, default_blocked_mot));
			}
			if(motion_ID == null) return false;
		}
		return true;
	}
	private void fillGlobals() throws P2PDDSQLException {
		if((this.motion_ID != null ) && (this.global_motion_ID == null))
			this.global_motion_ID = D_Motion.getGIDFromLID(this.motion_ID);
		if((this.organization_ID != null ) && (this.global_organization_ID == null))
			this.global_organization_ID = D_Organization.getGIDbyLIDstr(this.organization_ID);
		if((this.constituent_ID != null ) && (this.global_constituent_ID == null))
			this.global_constituent_ID = D_Constituent.getGIDFromLID(this.constituent_ID);
	}
	public long storeVerified() throws P2PDDSQLException {
		Calendar now = Util.CalendargetInstance();
		return storeVerified(now);
	}
	public long storeVerified(Calendar arrival_date) throws P2PDDSQLException {
		long result = -1;
		if(DEBUG) System.out.println("WB_News:storeVerified: storing="+this);
		if(DEBUG) System.out.println("WB_Motion:storeVerified: start arrival="+Encoder.getGeneralizedTime(arrival_date));
		if(this.constituent_ID == null )
			constituent_ID = D_Constituent.getLIDstrFromGID(this.global_constituent_ID, Util.Lval(this.organization_ID));
		if(constituent_ID == null){
			if(DEBUG) System.out.println("WB_Motion:storeVerified: no signer!");
			return -1;
		}
		if((this.organization_ID == null ) && (this.global_organization_ID != null))
			this.organization_ID = D_Organization.getLIDstrByGID_(this.global_organization_ID);
		if((this.motion_ID == null ) && (this.global_motion_ID != null))
			this.motion_ID = D_Motion.getLIDstrFromGID(this.global_motion_ID, Util.Lval(organization_ID));
		if((this.news_ID == null ) && (this.global_news_ID != null))
			this.news_ID = D_News.getLocalID(this.global_news_ID);
		if(DEBUG) System.out.println("WB_Motion:storeVerified: fixed local="+this);
		String params[] = new String[net.ddp2p.common.table.news.N_FIELDS];
		params[net.ddp2p.common.table.news.N_NEWS_GID] = this.global_news_ID;
		params[net.ddp2p.common.table.news.N_HASH_ALG] = this.hash_alg;
		params[net.ddp2p.common.table.news.N_TITLE_FORMAT] = this.title.title_document.getFormatString();
		params[net.ddp2p.common.table.news.N_TEXT_FORMAT] = this.news.getFormatString();
		params[net.ddp2p.common.table.news.N_TITLE] = this.title.title_document.getDocumentString();
		params[net.ddp2p.common.table.news.N_TEXT] = this.news.getDocumentString();
		params[net.ddp2p.common.table.news.N_CONSTITUENT_ID] = this.constituent_ID;
		params[net.ddp2p.common.table.news.N_ORG_ID] = organization_ID;
		params[net.ddp2p.common.table.news.N_MOT_ID] = motion_ID;
		params[net.ddp2p.common.table.news.N_SIGNATURE] = Util.stringSignatureFromByte(signature);
		params[net.ddp2p.common.table.news.N_CREATION] = Encoder.getGeneralizedTime(this.getCreationDate());
		params[net.ddp2p.common.table.news.N_ARRIVAL] = Encoder.getGeneralizedTime(arrival_date);
		params[net.ddp2p.common.table.news.N_BLOCKED] = Util.bool2StringInt(blocked);
		params[net.ddp2p.common.table.news.N_REQUESTED] = Util.bool2StringInt(requested);
		params[net.ddp2p.common.table.news.N_BROADCASTED] = Util.bool2StringInt(broadcasted);
		if (this.news_ID == null) {
			if (this.getJustificationLID() > 0) {
				D_Justification j = D_Justification.getJustByLID_AttemptCacheOnly(this.getJustificationLID(), false);
				if (j != null) j.resetCache(); 
			}
			if (this.getMotionLIDstr() != null) {
				D_Motion m = D_Motion.getMotiByLID_AttemptCacheOnly(this.getMotionLID(), false);
				if (m != null) m.resetCache(); 
			}
			if (this.getOrganizationLIDstr() != null) {
				D_Organization o = D_Organization.getOrgByLID_AttemptCacheOnly_NoKeep(this.getOrganizationLID(), false);
				if (o != null) o.resetCache(); 
			}
			if (DEBUG) System.out.println("WB_Motion:storeVerified:inserting");
			result = Application.getDB().insert(net.ddp2p.common.table.news.TNAME,
					net.ddp2p.common.table.news.fields_array,
					params,
					DEBUG
					);
			news_ID=""+result;
		} else {
			if(DEBUG) System.out.println("WB_Motion:storeVerified:updating");
			params[net.ddp2p.common.table.news.N_ID] = news_ID;
			Application.getDB().update(net.ddp2p.common.table.news.TNAME,
					net.ddp2p.common.table.news.fields_no_ID_array,
					new String[]{net.ddp2p.common.table.news.news_ID},
					params,
					DEBUG
					);
			result = Util.lval(this.news_ID, -1);
		}
		return result;
	}
	public long getMotionLID() {
		return Util.lval(this.motion_ID);
	}
	public long getOrganizationLID() {
		return Util.lval(this.organization_ID);
	}
	public String getOrganizationLIDstr() {
		return this.organization_ID;
	}
	public String getMotionLIDstr() {
		return this.motion_ID;
	}
	public void setOrganizationLID(String oLID) {
		this.organization_ID = oLID;
		if (oLID != null) {
			this.organization = D_Organization.getOrgByLID(oLID, false, false);
			if (this.organization != null) this.global_organization_ID = this.organization.getGID();
		}
	}
	/**
	 * News currently not oriented towards justifications
	 * @return
	 */
	private long getJustificationLID() {
		return -1;
	}
	/**
	 * update signature
	 * @param witness_ID
	 * @param signer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long readSignSave(long news_ID, long signer_ID) throws P2PDDSQLException {
		D_News w=new D_News(news_ID);
		net.ddp2p.ciphersuits.SK sk = net.ddp2p.common.util.Util.getStoredSK(D_Constituent.getGIDFromLID(signer_ID));
		w.sign(sk);
		return w.storeVerified();
	}
	private static String getLocalIDandDate(String global_newsID,	String[] _old_date) throws P2PDDSQLException {
		String sql = "SELECT "+net.ddp2p.common.table.news.news_ID+","+net.ddp2p.common.table.news.creation_date+" FROM "+net.ddp2p.common.table.news.TNAME+
		" WHERE "+net.ddp2p.common.table.news.global_news_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.getDB().select(sql, new String[]{global_newsID}, DEBUG);
		if(o.size()==0) return null;
		_old_date[0] = Util.getString(o.get(0).get(1));
		return Util.getString(o.get(0).get(0));
	}
	public static String getLocalID(String global_newsID) throws P2PDDSQLException {
		String sql = "SELECT "+net.ddp2p.common.table.news.news_ID+" FROM "+net.ddp2p.common.table.news.TNAME+
		" WHERE "+net.ddp2p.common.table.news.global_news_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.getDB().select(sql, new String[]{global_newsID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public static String getGlobalID(String newsID) throws P2PDDSQLException {
		String sql = "SELECT "+net.ddp2p.common.table.news.global_news_ID+" FROM "+net.ddp2p.common.table.news.TNAME+
		" WHERE "+net.ddp2p.common.table.news.news_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.getDB().select(sql, new String[]{newsID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public void setEditable() {
		signature = null;
		this.global_news_ID = null;
	}
	public boolean isEditable() {
		if(signature == null){
			if(DEBUG) out.println("D_News:editable: no sign");
			return true;
		}
		if(this.global_news_ID == null){
			if(DEBUG) out.println("D_News:editable: no GID");
			return true;
		}
		return false;
	}
	public static ArrayList<String> checkAvailability(ArrayList<String> news,
			String orgID, boolean DBG) throws P2PDDSQLException {
		ArrayList<String> result = new ArrayList<String>();
		for (String nHash : news) {
			if (! available(nHash, orgID, DBG)) {
				String nGIDHash = D_News.getGIDfromGID(nHash);
				if (nGIDHash != null)
					result.add(nGIDHash);
			}
		}
		return result;
	}
	/**
	 * Return null if invalid
	 * @param mHash
	 * @return
	 */
	public static String getGIDfromGID(String mHash) {
		if (mHash.startsWith(D_GIDH.d_News)) return mHash; 
		return null;
	}
	private static boolean available(String hash, String orgID, boolean DBG) throws P2PDDSQLException {
		String sql = 
			"SELECT "+net.ddp2p.common.table.news.news_ID+
			" FROM "+net.ddp2p.common.table.news.TNAME+
			" WHERE "+net.ddp2p.common.table.news.global_news_ID+"=? "+
			" AND "+net.ddp2p.common.table.news.organization_ID+"=? "+
			" AND "+net.ddp2p.common.table.news.signature + " IS NOT NULL;";
		ArrayList<ArrayList<Object>> a = Application.getDB().select(sql, new String[]{hash, orgID}, DEBUG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_News:available: "+hash+" in "+orgID+" = "+result);
		return result;
	}
	public static void main(String[] args) {
		try {
			Application.setDB(new DBInterface(Application.DELIBERATION_FILE));
			if(args.length>0){readSignSave(3,1); if(true) return;}
			D_News c=new D_News(3);
			if(!c.verifySignature()) System.out.println("\n************Signature Failure\n**********\nread="+c);
			else System.out.println("\n************Signature Pass\n**********\nread="+c);
			Decoder dec = new Decoder(c.getEncoder().getBytes());
			D_News d = D_News.getEmpty().decode(dec);
			Calendar arrival_date = d.setArrivalDate(Util.CalendargetInstance());
			if(!d.verifySignature()) System.out.println("\n************Signature Failure\n**********\nrec="+d);
			else System.out.println("\n************Signature Pass\n**********\nrec="+d);
			d.global_news_ID = d.make_ID();
			d.storeVerified(arrival_date);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static byte getASN1Type() {
		return TAG;
	}
	static final String sql_new = "SELECT count(*) FROM "+net.ddp2p.common.table.news.TNAME+" AS n "+
			" WHERE "+net.ddp2p.common.table.news.organization_ID+" = ?;";
	static final String sql_new_crea = "SELECT count(*) FROM "+net.ddp2p.common.table.news.TNAME+" AS n "+
			" WHERE n."+net.ddp2p.common.table.news.organization_ID+" = ? AND n."+net.ddp2p.common.table.news.creation_date+">?;";
	static final String sql_new_arriv = "SELECT count(*) FROM "+net.ddp2p.common.table.news.TNAME+" AS n "+
			" WHERE n."+net.ddp2p.common.table.news.organization_ID+" = ? AND n."+net.ddp2p.common.table.news.arrival_date+">?;";
	/**
	 * Negative days for the latest arrived days, and positive for latest creation days.
	 * Use "null" days for all!
	 * 
	 * Query is for a given organization.
	 * 
	 * @param orgID
	 * @param days
	 * @return
	 */
	public static long getCount(String orgID, int days) {
		long result = 0;
		try {
			ArrayList<ArrayList<Object>> orgs;
			if (days == 0) orgs = Application.getDB().select(sql_new, new String[]{orgID});
			else if (days > 0) orgs = Application.getDB().select(sql_new_crea, new String[]{orgID, Util.getGeneralizedDate(days)});
			else orgs = Application.getDB().select(sql_new_arriv, new String[]{orgID, Util.getGeneralizedDate(-days)});
			if (orgs.size() > 0) result = Util.get_long(orgs.get(0).get(0));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	static final String sql_just = "SELECT count(*) FROM "+net.ddp2p.common.table.news.TNAME+" AS n "+
		" WHERE "+net.ddp2p.common.table.news.justification_ID+" = ?;";
	static final String sql_just2 = "SELECT count(*) FROM "+net.ddp2p.common.table.news.TNAME+" AS n "+
	" WHERE n."+net.ddp2p.common.table.news.justification_ID+" = ? AND n."+net.ddp2p.common.table.news.arrival_date+">?;";
	public static long getCountJust(D_Justification j, int days) {
		long result = 0;
		try {
			ArrayList<ArrayList<Object>> orgs;
			if (days == 0) orgs = Application.getDB().select(sql_just, new String[]{j.getLIDstr()});
			else orgs = Application.getDB().select(sql_just2, new String[]{j.getLIDstr(), Util.getGeneralizedDate(days)});
			if (orgs.size() > 0)
				result = Util.lval(orgs.get(0).get(0),0);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	public String getTitle() {
		if (this.title == null) return null;
		return this.title.getTitleStr();
	}
	public void setTitle(String _tit) {
		this.title = new D_Document_Title();
		this.title.title_document.setDocumentString(_tit);
		this.title.title_document.setFormatString(D_Document.TXT_FORMAT);
	}
	public void setBody(String _body) {
		this.news = new D_Document();
		this.news.setDocumentString(_body);
		this.news.setFormatString(D_Document.TXT_BODY_FORMAT);
	}
	/**
	 * Returns null if the title is not TXT or HTML, else return the text content
	 * @return
	 */
	public String getNewsTitleStr() {
		if (this.title == null || title.title_document == null)
			return null;
		if (
				title.title_document.getFormatString() != null
				&&
				! D_Document.TXT_FORMAT.equals(title.title_document.getFormatString())
				&&
				! D_Document.HTM_BODY_FORMAT.equals(title.title_document.getFormatString())
		) return null;
		return title.title_document.getDocumentUTFString();
	}
	public String getTitleStrOrMy() {
		return getNewsTitleStr();
	}
	public String getNewsBodyStr() {
		if (this.news == null)
			return null;
		if (
				news.getFormatString() != null
				&&
				! D_Document.TXT_FORMAT.equals(news.getFormatString())
				&&
				! D_Document.HTM_BODY_FORMAT.equals(news.getFormatString())
		) return null;
		return news.getDocumentUTFString();
	}
	public D_Constituent getConstituent() {
		return this.constituent;
	}
	public void setConstituent(D_Constituent constituent) {
		this.constituent = constituent;
		if (constituent != null) {
			this.constituent_ID = constituent.getLIDstr();
			this.global_constituent_ID = constituent.getGID();
		}
	}
	public D_Constituent getConstituentForce() {
		if (this.constituent == null && this.constituent_ID != null)
			this.constituent = D_Constituent.getConstByLID(constituent_ID, false, false);
		return this.getConstituent();
	}
	public final static String sql_all_news = 
			"SELECT " + net.ddp2p.common.table.news.news_ID
			+ " FROM "+net.ddp2p.common.table.news.TNAME
			+ " WHERE "+net.ddp2p.common.table.news.organization_ID + "=? ";
	/**
	 * The index of the motion LID in the result of getAllNews()
	 */
	public static final int SELECT_ALL_NEWS_LID = 0;
	/**
	 * 
	 * @param hide (if true, then skip hidden)
	 * @param o_LID (organization LID)
	 * @param crt_motion_LID (if nonull, then set filter)
	 * @param crt_justif_LID (if nonull, then set filter)
	 * @param LIMIT (max entries when > 0)
	 * @return
	 * news_ID is at index SELECT_ALL_NEWS_LID
	 */
	public static java.util.ArrayList<java.util.ArrayList<Object>>
			getAllMotions(
					String o_LID,
					boolean hide,
					String crt_motion_LID,
					String crt_justif_LID,
					int LIMIT,
					boolean order_creation) {
		ArrayList<ArrayList<Object>> moti;
		if (Application.getDB() == null) return new ArrayList<ArrayList<Object>>();
		String sql = sql_all_news;
		if (crt_motion_LID != null) {
			sql += " AND " + net.ddp2p.common.table.news.motion_ID + " = ?";
		}		
		if (crt_justif_LID != null) {
			sql += " AND " + net.ddp2p.common.table.news.justification_ID + " = ?";
		}		
		if (order_creation)
			sql	 +=	" ORDER BY "+net.ddp2p.common.table.news.creation_date+" DESC ";
		else
			sql	 +=	" ORDER BY "+net.ddp2p.common.table.news.arrival_date+" DESC ";
		if (LIMIT > 0)	sql	 +=	" LIMIT " + LIMIT;
		try {
			if (crt_motion_LID != null) {
				moti = Application.getDB().select(sql+";", new String[]{o_LID, crt_motion_LID});
			} else {
				moti = Application.getDB().select(sql+";", new String[]{o_LID});
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return new ArrayList<ArrayList<Object>>();
		}
		return moti;
	}
}
