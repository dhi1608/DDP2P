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
package net.ddp2p.common.config;
import static net.ddp2p.common.util.Util.__;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.PK;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.hds.Client2;
import net.ddp2p.common.hds.ClientSync;
import net.ddp2p.common.hds.Connections;
import net.ddp2p.common.hds.DirectoryAnswerMultipleIdentities;
import net.ddp2p.common.hds.DirectoryServer;
import net.ddp2p.common.hds.EventDispatcher;
import net.ddp2p.common.hds.IClient;
import net.ddp2p.common.hds.Server;
import net.ddp2p.common.hds.UDPServer;
import net.ddp2p.common.network.NATServer;
import net.ddp2p.common.plugin_data.PluginRegistration;
import net.ddp2p.common.simulator.Fill_database;
import net.ddp2p.common.simulator.SimulationParameters;
import net.ddp2p.common.streaming.OrgHandling;
import net.ddp2p.common.table.HashConstituent;
import net.ddp2p.common.util.BMP;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DB_Implementation;
import net.ddp2p.common.util.DDP2P_ServiceThread;
import net.ddp2p.common.util.DD_Address;
import net.ddp2p.common.util.DD_DirectoryServer;
import net.ddp2p.common.util.DD_IdentityVerification_Answer;
import net.ddp2p.common.util.DD_IdentityVerification_Request;
import net.ddp2p.common.util.DD_Mirrors;
import net.ddp2p.common.util.DD_SK;
import net.ddp2p.common.util.DD_Testers;
import net.ddp2p.common.util.DirectoryAddress;
import net.ddp2p.common.util.EmbedInMedia;
import net.ddp2p.common.util.JPEG;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.PNG;
import net.ddp2p.common.util.StegoStructure;
import net.ddp2p.common.util.Util;
import net.ddp2p.common.wireless.BroadcastClient;
import net.ddp2p.common.wireless.BroadcastServer;
import net.ddp2p.common.wireless.Broadcasting_Probabilities;
import net.ddp2p.common.wireless.Refresh;
public class DD {
	public static final String BRANCH = "B";//FIT_HDSSL_SILAGHI";
	public static final String VERSION = "0.10.26";
	public static final boolean ONLY_IP4 = false;
	private static final String PK_Developer = "MIIEGgwDUlNBYAEwAgMBAAECggQASKs9x2VEQH1SRxRwO43yt6HXCTnOmPJVUjN8bQQUTVBdFXhQsTpnTP1yLe/qFlA0jnIzheHT4WEcsU874N800iPMWHCjpCowQwwTj9SQLTmfbfhL8z0a7Dw6ZJQ+DnYoPVhx3JHL57CK3YeVYclZCoHetZ5PEIpcAwxaPmnL3GQaOgJiVHb6CLMi+hNHLxsjQZwTYTeoUOXQKgyTcRDE6xCvw8+q0U6/Uan3KCx/KmtdRQMEtGAXSPANv12kle84Dv8AdJxT1CJGsXm0+N6+wbbvkL77kMr+79sCR/8drZmOnrbjveQpab2pSh0vO//XqslrDRbzhniGSpqFW+YNTOixWAsCp35hNPbAx5xqPXg6DEIrysGslDGo4gC3Ew5mN/JkOQA+pd6uIzC4EgbfWqJKMvrtOQN67hJR7Ysxn7cLDXGvmhK1s7oSJcnOmhWljSZ6joviVwAWKgzdm1gMBhn5+VdgwoEE7g5Inw0dH9UmgufloNiBQMM9m2igdQPaLRuVttrAEcs55F/Z5NFtJquTeQFBLAGux3MVxrYCgivRaoAzAkUMhGOA+00KU3oh3Bds0U8GYCMuYYrwSAWTZf0Z9lvUwJv8HtLJvI6p1p53oGzIW9bo20d0PMz7XrzNDOLEME9PaXKLo6vMCAxXIj19nm/bE1HBY7e7HErKMX3M7LC2xZ8PH7wsnl5M3y0ZZ6c9quwhvz/dWcUAQ5963LtDZ6bOenAGVGBjdWLhHK8/2p9Vgu1ZNA1WWHWnafExsT5GxuwZQ/PMk8YtmxqEkgGy2+xVT19oUK+yO1ok+xRUjvSRZ0IbWUEcOfQ5FvLNmMdV/NSebB6vjQwM5DGCE1YDhix+Qghr558KokVz7BPVrGVe1pUxfPo2XPwHReF8es+vr16lvwXrVEmQNG8KrX1tN5Z5I29+ZVcR6ti4t90RXY6H6lmLtU3P/PSmfOrBQraNHVvDm9y1hnSP9+EhJzuWFaS8v4+7OnodIWuZsYd2WYQp4YcDJ+7grV3s1vvacujzxCOwx5/gosLxOau45bvKqhsFrZ+le6IRNAG7T6ZwC9wesqCGBJlIwS50DlAb/KhPyDIvf+7EH1iwckG4fBtixaK9co8FHnuddn/cEIc6fkWDEzr2Cu3HyxeMeDrcGRvjTRr78Wp/ptvRoOYElOLkxrkmanetjOCMqRl1DJvl53SQKePraRx2DpRemK/TMQ3+5TQkFjjEsI2P455Th0z6vF+JzpetZ3j1NUqx+iEZ2ArMhdDk7dE/4qcn2xwLz5nNMvHSnO2N0T9tCLi96CqZm/HTqGa6jTxFhJOP11sFCCQ9jkKhxvxubs0sww75dnqXQeffpxyolcht3KHwfwwHU0hBLTUxMg==";
	private static PK _PK_Developer = null;
	public static PK get_PK_Developer() {
		if (_PK_Developer != null) return _PK_Developer;
		return _PK_Developer = Cipher.getPK(PK_Developer);
	}
	public static String _APP_NAME = __("Direct Democracy P2P");
	public static String APP_NAME = _APP_NAME+" "+VERSION;
	public static final String DEFAULT_EMAIL_PROVIDER = "my.fit.edu";
	public static boolean DEBUG = false;
	static final boolean _DEBUG = true;
	public static final boolean DEBUG_TODO = set_DEBUG_TODO(false);
	private static boolean set_DEBUG_TODO(boolean dbg_todo) {
		if (dbg_todo) Decoder.DEBUG_TODO = dbg_todo;
		return dbg_todo;
	}
    public static final String WIRELESS_THANKS = "wireless_thanks.wav"; // in scripts
    public static String scripts_prefix = null; 
	public static boolean DELETE_COMBOBOX_WITHOUT_CTRL = true;
	public static final byte TAG_AP0 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)0);
	public static final byte TAG_AP1 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)1);
	public static final byte TAG_AP2 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)2);
	public static final byte TAG_AP3 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)3);
	public static final byte TAG_AP4 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)4);
	public static final byte TAG_AP5 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)5);
	public static final byte TAG_AP6 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)6);
	public static final byte TAG_AP7 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)7);
	public static final byte TAG_AP8 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)8);
	public static final byte TAG_AP9 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)9);
	public static final byte TAG_AP10 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)10);
	public static final byte TAG_AP11 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)11);
	public static final byte TAG_AP12 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)12);
	public static final byte TAG_AP13 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)13);
	public static final byte TAG_AP14 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)14);
	public static final byte TAG_AP15 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)15);
	public static final byte TAG_AP16 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)16);
	public static final byte TAG_AP17 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)17);
	public static final byte TAG_AC0 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)0);
	public static final byte TAG_AC1 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)1);
	public static final byte TAG_AC2 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)2);
	public static final byte TAG_AC3 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)3);
	public static final byte TAG_AC4 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)4);
	public static final byte TAG_AC5 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)5);
	public static final byte TAG_AC6 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)6);
	public static final byte TAG_AC7 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)7);
	public static final byte TAG_AC8 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)8);
	public static final byte TAG_AC9 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)9);
	public static final byte TAG_AC10 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)10);
	public static final byte TAG_AC11 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)11);
	public static final byte TAG_AC12 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)12);
	public static final byte TAG_AC13 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)13);
	public static final byte TAG_AC14 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)14);
	public static final byte TAG_AC15 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)15);
	public static final byte TAG_AC16 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)16);
	public static final byte TAG_AC17 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)17);
	public static final byte TAG_AC18 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)18);
	public static final byte TAG_AC19 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)19);
	public static final byte TAG_AC20 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)20);
	public static final byte TAG_AC21 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)21);
	public static final byte TAG_AC22 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)22);
	public static final byte TAG_AC23 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)23);
	public static final byte TAG_AC24 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)24);
	public static final byte TAG_AC25 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)25);
	public static final byte TAG_AC26 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)26);
	public static final byte TAG_AC27 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)27);
	public static final byte TAG_AC28 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)28);
	public static final byte TAG_AC29 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)29);
	public static final byte TAG_PP0  = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)0);
	public static final byte TYPE_DatabaseName = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)1);
	public static final byte TYPE_FieldName = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)2);
	public static final byte TYPE_FieldType = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)3);
	public static final byte TAG_PP4 = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)4);
	public static final byte TYPE_SignSyncReq = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)5);
	public static final byte MSGTYPE_EmptyPing = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)20);;
	public static final byte TYPE_ORG_DATA = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)29);
	public static String DEFAULT_PEER = "Peer";
	public static String DEFAULT_ORGANIZATION = "Organization";
	public static String DEFAULT_CONSTITUENT = "Constituent";
	public static String DEFAULT_NEIGHBORHOOD = "Neighborhood";
	public static String DEFAULT_WITNESS = "Witness";
	public static String DEFAULT_JUSTIFICATION = "Justification";
	public static String DEFAULT_MOTION = "Motion";
	public static String DEFAULT_VOTE = "Vote";
	public static short getPositive(short tag) {
		if (tag >= 0) return tag;
		return (short)(0x7fff & tag);
	}
	/**
	 * SIGN of images (must all be positive, to enable them as ASN1 tags!)
	 */
	public static final short STEGO_SIGN_PEER = 0x0D0D;
	public static final short STEGO_SIGN_DIRECTORY_SERVER = 0x1881;
	public static final short STEGO_SIGN_CONSTITUENT_VERIF_ANSWER = 0x3EE3;
	public static final short STEGO_SIGN_MIRRORS = 0x4774;
	public static final short STEGO_SIGN_TESTERS = 0x588C;
	public static final short STEGO_SK = 0x3EEF; 
	public static final short STEGO_SIGN_CONSTITUENT_VERIF_REQUEST = 0x7AAD;
	public static final short STEGO_SLOGAN = 0x5EAD; 
	/**
	 * class(2bits)||pc(1b)||number
	 * @param classASN1
	 * @param PCASN1
	 * @param tag_number
	 * @return
	 */
	public static byte asn1Type(int classASN1, int PCASN1, byte tag_number) {
		if((tag_number&0x1F) >= 31){
			Util.printCallPath("Need more bytes");
			tag_number = 25;
		}
		return  (byte)((classASN1<<6)+(PCASN1<<5)+tag_number);
	}
	/**
	 * Function used to register a StegoStructure for enabling its encoding at 
	 * drag and drop
	 * @param ss
	 */
	public static void registerStegoStructure(StegoStructure ss) {
		if (available_stego_structure == null)
			available_stego_structure = 
			new ArrayList<StegoStructure>(Arrays.asList(_getInitialStegoStructureInstances()));
		available_stego_structure.add(ss);
	}
	/**
	 * Currently final. If allowing plugins to register structures, then should not be final.
	 * Could have been initialized with registerStegoStructure().
	 */
	public static ArrayList<StegoStructure> available_stego_structure =
			new ArrayList<StegoStructure>(Arrays.asList(_getInitialStegoStructureInstances()));
	/**
	 * This builds a list with installed StegoStructures.
	 * Hard-coded are:
	 *   DD_Address,
	 *   DD_IdentityVerification_Request, 
	 *   DD_IdentityVerification_Answer,
	 *   DD_DirectoryServer, 
	 *   DD_Testers, 
	 *   DD_Mirrors, 
	 *   DD_SK
	 * @return
	 */
	private static StegoStructure[] _getInitialStegoStructureInstances() {
		DD_Address data1 = new DD_Address();
		DD_IdentityVerification_Request data2 = new DD_IdentityVerification_Request();
		DD_IdentityVerification_Answer data3 = new DD_IdentityVerification_Answer();
		DD_DirectoryServer data4 = new DD_DirectoryServer();
		DD_Testers data5 = new DD_Testers();
		DD_Mirrors data6 = new DD_Mirrors();
		DD_SK data7 = new DD_SK();
		return new StegoStructure[]{data1, data2, data3, data4, data5, data6, data7};
	}
	/**
	 * Tries to infer the type
	 * @param ASN1TAG
	 * @return
	 */
	public static StegoStructure getStegoStructure(BigInteger ASN1TAG) {
		for (StegoStructure ss : getAvailableStegoStructureInstances()) {
			if (ASN1TAG.equals (new BigInteger(""+ss.getSignShort()))) {
				try {
					return (StegoStructure) ss.getClass().newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	/**
	 * Tries one by one
	 * @param ASN1TAG
	 * @return
	 */
	public static StegoStructure _getStegoStructure(BigInteger ASN1TAG) {
		if (ASN1TAG == null) {
			if (_DEBUG) System.out.println("DD: getStegoStructure: null parameter");
			return null;
		}
		if (ASN1TAG.equals(DD_Address.getASN1Tag())) return new DD_Address();
		if (ASN1TAG.equals(DD_IdentityVerification_Request.getASN1Tag())) return new DD_IdentityVerification_Request();
		if (ASN1TAG.equals(DD_IdentityVerification_Answer.getASN1Tag())) return new DD_IdentityVerification_Answer();
		if (ASN1TAG.equals(DD_DirectoryServer.getASN1Tag())) return new DD_DirectoryServer();
		if (ASN1TAG.equals(DD_Testers.getASN1Tag())) return new DD_Testers();
		if (ASN1TAG.equals(DD_Mirrors.getASN1Tag())) return new DD_Mirrors();
		if (ASN1TAG.equals(DD_SK.getASN1Tag())) return new DD_SK();
		return null;
	}
	public static StegoStructure getStegoStructure(Decoder dec) {
		return getStegoStructure(dec.getTagValueBN());
	}
	/**
	 * Function used to query available StegoStructures.
	 * @return
	 */
	public static StegoStructure[] getAvailableStegoStructureInstances(){
		return available_stego_structure.toArray(new StegoStructure[0]);
	}
	public static short[] getAvailableStegoStructureISignatures() {
		StegoStructure[] a = getAvailableStegoStructureInstances();
		if (a == null) return new short[0];
		short []r = new short[a.length];
		for (int k = 0 ; k < a.length; k++)
			r[k] = a[k].getSignShort();
		return r;
	}
	public static boolean STREAM_SEND_ALL_ORG_CREATOR = true;
	public static boolean STREAM_SEND_ALL_FUTURE_ORG = false;
	public static boolean WARN_BROADCAST_LIMITS_REACHED = true;
	public static boolean WARN_OF_WRONG_SYNC_REQ_SK = false;
	public static boolean EXPORT_DDADDRESS_WITH_LOCALHOST = false; 
	public static boolean VERIFY_SIGNATURE_MYPEER_IN_REQUEST = false; 
	public static boolean ADHOC_MESSAGES_USE_DICTIONARIES = true;
	public static boolean ADHOC_DD_IP_WINDOWS_DETECTED_WITH_NETSH = true; 
	public static boolean ADHOC_DD_IP_WINDOWS_DETECTED_ON_EACH_SEND = true;
	public static String ADHOC_DD_IP_WINDOWS_NETSH_IP_IDENTIFIER = "IP";
	public static String ADHOC_DD_IP_WINDOWS_IPCONFIG_IPv4_IDENTIFIER = "IPv4";
	public static String ADHOC_DD_IP_WINDOWS_NETSH_INTERFACE_IDENTIFIER = "Name"; // One may prefer to just extract first label in output
	public static String ADHOC_DD_IP_WINDOWS_NETSH_SSID_IDENTIFIER = "SSID";
	public static Refresh START_REFRESH = null;
	public static final String newsFields[] = new String[]{"global_news_ID","global_author_ID","date","news","type","signature"};
	public static final String newsFieldsTypes[] = new String[]{"TEXT","TEXT","TEXT","TEXT","TEXT","TEXT"};
	public static final String DD_DATA_CLIENT_UPDATES_INACTIVE_ON_START = "data_client_updates_on_start";
	public static final String DD_DATA_CLIENT_INACTIVE_ON_START = "data_client_on_start";
	public static final String DD_DATA_SERVER_ON_START = "data_server_on_start";
	public static final String DD_DATA_USERVER_INACTIVE_ON_START = "data_userver_on_start";
	public static final String DD_DATA_NSERVER_INACTIVE_ON_START = "data_nserver_on_start";
	public static final String DD_DIRECTORY_SERVER_ON_START = "directory_server_on_start";
	public static final String COMMAND_NEW_ORG = "COMMAND_NEW_ORG";
	public static final int MSGTYPE_SyncAnswer = 10;
	public static final int MSGTYPE_SyncRequest = 11;
	public static final String APP_NET_INTERFACES = "INTERFACES";
	public static final String APP_NON_ClientUDP = "!ClientUDP";
	public static final String APP_NON_ClientNAT = "!ClientNAT";
	public static final String APP_ClientTCP = "ClientTCP";
	public static final String APP_LISTING_DIRECTORIES = "listing_directories";
	public static final String APP_LISTING_DIRECTORIES_SEP = ",";
	public static final String APP_LISTING_DIRECTORIES_ELEM_SEP = ":";
	public static final String APP_stop_automatic_creation_of_default_identity = "stop_automatic_creation_of_default_identity";
	public static final String APP_hidden_from_my_peers = "hidden_from_my_peers";
	public static final String APP_my_global_peer_ID = "my_global_peer_ID";
	public static final String APP_my_peer_instance = "my_peer_instance";
	public static final String APP_my_global_peer_ID_hash = "my_global_peer_ID_hash";
	public static final String APP_ID_HASH = Cipher.SHA1; 
	public static final String APP_INSECURE_HASH = Cipher.MD5; 
	public static final String APP_ORGID_HASH = Cipher.SHA256;  
	public static final String APP_ID_HASH_SEP = ":"; // default hash alg for new ID
	public static final String DD_WIRELESS_SERVER_ON_START = "WIRELESS_SERVER_ON_START";
	public static final String DD_CLIENT_SERVER_ON_START = "CLIENT_SERVER_ON_START";
	public static final String DD_SIMULATOR_ON_START = "SIMULATOR_ON_START";
	public static final String APP_LINUX_INSTALLATION_PATH = "SCRIPT_WIRELESS_LINUX_PATH";
	public static final String APP_WINDOWS_INSTALLATION_PATH = "SCRIPT_WIRELESS_WINDOWS_PATH";
	public static final String APP_LINUX_INSTALLATION_ROOT_PATH = "APP_LINUX_INSTALLATION_ROOT_PATH";
	public static final String APP_WINDOWS_INSTALLATION_ROOT_PATH = "SCRIPT_WIRELESS_WINDOWS_ROOT_PATH";
	public static final String BROADCASTING_PROBABILITIES = "BROADCASTING_PROBABILITIES";
	public static final String GENERATION_PROBABILITIES = "GENERATION_PROBABILITIES";
	public static final String PROB_CONSTITUENTS = "C";
	public static final String PROB_ORGANIZATIONS = "O";
	public static final String PROB_MOTIONS = "M";
	public static final String PROB_JUSTIFICATIONS = "J";
	public static final String PROB_WITNESSES = "W";
	public static final String PROB_NEIGHBORS = "N";
	public static final String PROB_VOTES = "V";
	public static final String PROB_PEERS = "P";
	public static final String PROB_SEP = ",";
	public static final String PROB_KEY_SEP = ":";
	public static final int WINDOWS = 1;
	public static final int LINUX = 2;
	public static final int MAC = 3;
	public static boolean DEBUG_PLUGIN = false;
	public static int OS = WINDOWS;
	public static String DEFAULT_DD_SSID = "DirectDemocracy";
	public static String DEFAULT_WIRELESS_ADHOC_DD_NET_MASK = "255.0.0.0";
	public static String DEFAULT_WIRELESS_ADHOC_DD_NET_IP_BASE = "10.0.0.";
	public static String DEFAULT_WIRELESS_ADHOC_DD_NET_BROADCAST_IP = "10.255.255.255";
	public static String DD_SSID = DEFAULT_DD_SSID;
	public static String WIRELESS_ADHOC_DD_NET_MASK = DEFAULT_WIRELESS_ADHOC_DD_NET_MASK;
	public static String WIRELESS_ADHOC_DD_NET_IP_BASE = DEFAULT_WIRELESS_ADHOC_DD_NET_IP_BASE;
	public static String WIRELESS_ADHOC_DD_NET_BROADCAST_IP = DEFAULT_WIRELESS_ADHOC_DD_NET_BROADCAST_IP;
	public static String WIRELESS_IP_BYTE; 
	public static String WIRELESS_ADHOC_DD_NET_IP;
	public static final String APP_LAST_IP = "LAST_IP"; // last wireless adhoc broadcast IP
	public static final String APP_UPDATES_SERVERS = "UPDATES_SERVERS";
	public static final String APP_UPDATES_SERVERS_URL_SEP = ";";
	public static final String LATEST_DD_VERSION_DOWNLOADED = "LATEST_DD_VERSION_DOWNLOADED";
	public static final String TRUSTED_UPDATES_GID = "TRUSTED_UPDATES_GID";
	public static final String TRUSTED_UPDATES_GID_SEP = ",";
	public static final String BROADCASTING_QUEUE_PROBABILITIES = "BROADCASTING_QUEUE_PROBABILITIES";
	public static final String APP_Q_MD = "Q_MD";
	public static final String APP_Q_C = "Q_C";
	public static final String APP_Q_RA = "Q_RA";
	public static final String APP_Q_RE = "Q_RE";
	public static final String APP_Q_BH = "Q_BH";
	public static final String APP_Q_BR = "Q_BR";
	public static final int RSA_BITS_TRUSTED_FOR_UPDATES = 1<<12;
	public static final String APP_DB_TO_IMPORT = "APP_DB_TO_IMPORT";
	public static final String APP_LINUX_SCRIPTS_PATH = "APP_LINUX_SCRIPTS_PATH";
	public static final String APP_LINUX_PLUGINS_PATH = "APP_LINUX_PLUGINS_PATH";
	public static final String APP_LINUX_LOGS_PATH = "APP_LINUX_LOGS_PATH";
	public static final String APP_LINUX_DATABASE_PATH = "APP_LINUX_DATABASE_PATH";
	public static final String APP_LINUX_DD_JAR_PATH = "APP_LINUX_DD_JAR_PATH";
	public static final String APP_WINDOWS_SCRIPTS_PATH = "APP_WINDOWS_SCRIPTS_PATH";
	public static final String APP_WINDOWS_PLUGINS_PATH = "APP_WINDOWS_PLUGINS_PATH";
	public static final String APP_WINDOWS_LOGS_PATH = "APP_WINDOWS_LOGS_PATH";
	public static final String APP_WINDOWS_DATABASE_PATH = "APP_WINDOWS_DATABASE_PATH";
	public static final String APP_WINDOWS_DD_JAR_PATH = "APP_WINDOWS_DD_JAR_PATH";
	public static int MTU = 1472-200;
	/** Limits Used for security at recipient */
	public static int UDP_MAX_MSG_LENGTH = 10000000;
	/** Limits Used for security at recipient */
	public static int UDP_MAX_FRAGMENT_LENGTH = 100000;
	/** Limits Used for security at recipient */
	public static int UDP_MAX_FRAGMENTS = 10000; 
	/** Ask only senders for hashes, if true. Else ask everybody */
	public static final boolean REQUEST_ONLY_SENDERS_FOR_HASHES = true;
	public static ArrayList<InetSocketAddress> directories_failed = new ArrayList<InetSocketAddress>();
	/**
	 * Use ClientUDP?
	 */
	public static boolean ClientNAT = true;
	public static boolean ClientUDP = true;
	public static boolean ClientTCP = false; 
	public static EventDispatcher ed=new EventDispatcher();
	public static final String SERVE_DIRECTLY = "SERVE_DIRECTLY";
	public static final boolean DD_DATA_CLIENT_ON_START_DEFAULT = true;
	public static final boolean DD_DATA_CLIENT_UPDATES_ON_START_DEFAULT = true;
	public static final boolean DD_DATA_USERVER_ON_START_DEFAULT = true;
	public static final boolean DD_DATA_NSERVER_ON_START_DEFAULT = true;
	public static final boolean ORG_UPDATES_ON_ANY_ORG_DATABASE_CHANGE = false;
	public static final String CONSTITUENT_PICTURE_FORMAT = "jpg";
	public static final String WIRELESS_SELECTED_INTERFACES = "WIRELESS_SELECTED_INTERFACES";
	public static final String WIRELESS_SELECTED_INTERFACES_SEP = ":";
	public static final long GETHOSTNAME_TIMEOUT_MILLISECONDS = (long)(1000*0.05);
	public static final String LAST_SOFTWARE_VERSION = "LAST_SOFTWARE_VERSION";
	public static final String DD_DB_VERSION = "DD_DB_VERSION";
	public static final String EMPTYDATE = "";
	public static final String UPDATES_TESTERS_THRESHOLD_WEIGHT = "UPDATES_TESTERS_THRESHOLD_WEIGHT";
	public static final String UPDATES_TESTERS_THRESHOLD_COUNT_VALUE = "UPDATES_TESTERS_THRESHOLD_COUNT_VALUE";
	public static final String UPDATES_TESTERS_THRESHOLD_WEIGHT_VALUE = "UPDATES_TESTERS_THRESHOLD_WEIGHT_VALUE";
	public static final String UPDATES_TESTERS_THRESHOLDS_RELATIVE = "UPDATES_TESTERS_THRESHOLDS_RELATIVE";
	public static final int UPDATES_TESTERS_THRESHOLD_COUNT_DEFAULT = 1;
	public static final float UPDATES_TESTERS_THRESHOLD_WEIGHT_DEFAULT = 0.0f;
	public static final int MAX_DISPLAYED_CONSTITUENT_SLOGAN = 100;
	public static final String WLAN_INTERESTS = "WLAN_INTERESTS";
	public static final boolean SUBMITTER_REQUIRED_FOR_EXTERNAL = false;
	public static final String P2PDDSQLException = null;
	public static boolean VERIFY_FRAGMENT_RECLAIM_SIGNATURE = false;
	public static boolean VERIFY_FRAGMENT_NACK_SIGNATURE = false;
	public static boolean VERIFY_FRAGMENT_ACK_SIGNATURE = false;
	public static boolean VERIFY_FRAGMENT_SIGNATURE = false;
	public static boolean PRODUCE_FRAGMENT_RECLAIM_SIGNATURE = false;
	public static boolean PRODUCE_FRAGMENT_NACK_SIGNATURE = false;
	public static boolean PRODUCE_FRAGMENT_ACK_SIGNATURE = false;
	public static boolean PRODUCE_FRAGMENT_SIGNATURE = false;
	public static final int FRAGMENTS_WINDOW = 10;
	public static final int FRAGMENTS_WINDOW_LOW_WATER = FRAGMENTS_WINDOW/2;
	public static final boolean AVOID_REPEATING_AT_PING = false;
	public static final boolean ORG_CREATOR_REQUIRED = false;
	public static final boolean CONSTITUENTS_ADD_ASK_TRUSTWORTHINESS = false;
	public static final String MY_DEBATE_TOPIC = "MY_DEBATE_TOPIC";
	public static final long LARGEST_BMP_FILE_LOADABLE = 10000000;
	public static final long PAUSE_BEFORE_CONNECTIONS_START = 5*1000;
	public static final long PAUSE_BEFORE_CLIENT_START = 4*1000; //after connections
	public static final long PAUSE_BEFORE_UDP_SERVER_START = 4*1000;
	public static final boolean DROP_DUPLICATE_REQUESTS = false;
	public static final int UDP_SENDING_CONFLICTS = 10; 
	public static final boolean ACCEPT_UNSIGNED_CONSTITUENTS = false;
	public static final boolean ACCEPT_UNSIGNED_NEIGHBORHOOD = false;
	public static final boolean ACCEPT_UNSIGNED_PEERS_FROM_TABLES = false;
	public static final boolean ACCEPT_UNSIGNED_PEERS_FOR_STORAGE = false;
	public static final boolean DEBUG_CHANGED_ORGS = false;
	public static final boolean DEBUG_PRIVATE_ORGS = false;
	/**
	 * For debugging other peers (due to errors sent to us) set the next to true!
	 */
	public static final boolean WARN_ABOUT_OTHER = false;
	public static int MAX_MOTION_ANSWERTO_CHOICES = 100;
	/**
	 * 0 = undecided
	 * 1 = true
	 * -1 = false
	 */
	public static int AUTOMATE_PRIVATE_ORG_SHARING = 0; 
	public static boolean DEBUG_LIVE_THREADS = false;
	public static boolean DEBUG_COMMUNICATION = false;
	public static boolean DEBUG_COMMUNICATION_LOWLEVEL = false;
	public static boolean WARN_ON_IDENTITY_CHANGED_DETECTION = false;
	public static boolean CONSTITUENTS_ORPHANS_SHOWN_BESIDES_NEIGHBORHOODS = true;
	public static boolean CONSTITUENTS_ORPHANS_FILTER_BY_ORG = true;
	public static boolean CONSTITUENTS_ORPHANS_SHOWN_IN_ROOT = false;
	public static boolean NEIGHBORHOOD_SIGNED_WHEN_CREATED_EMPTY = false; 
	public static boolean ACCEPT_STREAMING_SYNC_REQUEST_PAYLOAD_DATA_FROM_UNKNOWN_PEERS = false;
	public static boolean ACCEPT_TEMPORARY_AND_NEW_CONSTITUENT_FIELDS = true;
	public static long UDP_SERVER_WAIT_MILLISECONDS = 1000;
	public static long ADHOC_SENDER_SLEEP_MILLISECONDS = 5;
	public static boolean VERIFY_AFTER_SIGNING_NEIGHBORHOOD = true;
	public static boolean EDIT_VIEW_UNEDITABLE_NEIGHBORHOODS = true;
	public static boolean BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME = false;
	public static boolean BLOCK_NEW_ARRIVING_PEERS_ANSWERING_ME = false;
	public static boolean BLOCK_NEW_ARRIVING_PEERS_FORWARDED_TO_ME = false;
	public static boolean BLOCK_NEW_ARRIVING_ORGS = false;
	public static boolean BLOCK_NEW_ARRIVING_ORGS_WITH_BAD_SIGNATURE = true;
	public static  boolean TEST_SIGNATURES = false;
	public static  boolean WARN_OF_UNUSED_PEERS = true;
	public static  boolean ACCEPT_DATA_FROM_UNSIGNED_PEERS = false;
	public static  boolean EDIT_RELEASED_ORGS = false;
	public static  boolean EDIT_RELEASED_JUST = false;
	public static  boolean ACCEPT_UNSIGNED_DATA = false;
	public static  boolean WARN_OF_INVALID_PLUGIN_MSG = true;
	public static  boolean DEFAULT_BROADCASTABLE_PEER_MYSELF = false;
	public static boolean WARN_OF_FAILING_SIGNATURE_ONRECEPTION = true;
	public static boolean WARN_OF_FAILING_SIGNATURE_ONSEND = true;;
	public static boolean DEFAULT_RECEIVED_PEERS_ARE_USED = false;
	public static boolean DEFAULT_AUTO_CONSTITUENTS_REFRESH = false;
	public static long UPDATES_WAIT_MILLISECONDS = 1000*60*10;
	public static long UPDATES_WAIT_ON_STARTUP_MILLISECONDS = 1000*60*5;
	public static boolean UPDATES_AUTOMATIC_VALIDATION_AND_INSTALL = true;
	public static boolean DELETE_UPGRADE_FILES_WITH_BAD_HASH = false;
	public static boolean ADHOC_WINDOWS_DD_CONTINUOUS_REFRESH = true;
	public static long ADHOC_EMPTY_TIMEOUT_MILLISECONDS = 1000*1; // 1 seconds
	public static long ADHOC_REFRESH_TIMEOUT_MILLISECONDS = 1000*1;
	public static int ADHOC_SERVER_CONSUMMER_BUFFER_SIZE = 20000;
	public static String TESTED_VERSION;
	public static boolean ACCEPT_STREAMING_ANSWER_FROM_ANONYMOUS_PEERS = false;
	public static boolean ACCEPT_STREAMING_ANSWER_FROM_NEW_PEERS = true;
	public static int ACCEPT_STREAMING_UPTO_MAX_PEERS = 1000;
	public static int FRAME_OFFSET = 100;
	public static int FRAME_WIDTH = 600;
	public static int FRAME_HSTART = 100;
	public static int FRAME_HEIGHT = 600;
	public static Calendar startTime;
	public static boolean VERIFY_SENT_SIGNATURES = true;
	public static boolean ACCEPT_STREAMING_REQUEST_UNSIGNED = false;
	public static boolean USE_NEW_ARRIVING_PEERS_CONTACTING_ME = true;
	public static boolean ASK_USAGE_NEW_ARRIVING_PEERS_CONTACTING_ME = true;
	public static long ADHOC_SENDER_SLEEP_SECONDS_DURATION_LONG_SLEEP = 0;
	public static int ADHOC_SENDER_SLEEP_MINUTE_START_LONG_SLEEP = 1;
	public static byte[] Random_peer_Number;
	public static boolean SCRIPTS_ERRORS_WARNING = true;
	public static boolean WARNED_NO_DIRS = false;
	public static boolean REJECT_NEW_ARRIVING_PEERS_CONTACTING_ME = false;
	/**
	 * Needs testing,
	 * Needs tuning the amount of data advertisement sent for indirect ads (increase from current value)
	 * Need to set served orgs when broadcasting, or to disable broadcasting for orgs not in served_orgs
	 * 
	 * @param direct
	 */
	public static void serveDataDirectly(boolean direct){
		OrgHandling.SERVE_DIRECTLY_DATA = direct;
	}
	public final static boolean  preloadedControl = true;
	public static String[] get_preferred_charsets() throws P2PDDSQLException {
    	ArrayList<ArrayList<Object>> id;
    	id=Application.getDB().select("SELECT "+net.ddp2p.common.table.identity.preferred_charsets +
    			" FROM "+net.ddp2p.common.table.identity.TNAME+" AS i" +
    			" WHERE i."+net.ddp2p.common.table.identity.default_id+"==1 LIMIT 1;",
    			new String[]{});
    	if(id.size()==0){
    		if(DEBUG)System.err.println("No default identity found!");
    		return null;
    	}
    	String preferred_charsets = Util.getString(id.get(0).get(0));
    	if(preferred_charsets == null) return new String[]{};
    	return preferred_charsets.split(Pattern.quote(":"));
	}
	public static String get_authorship_charset() throws P2PDDSQLException {
    	ArrayList<ArrayList<Object>> id;
    	id=Application.getDB().select("SELECT "+net.ddp2p.common.table.identity.authorship_charset +
    			" FROM "+net.ddp2p.common.table.identity.TNAME+" AS i" +
    			" WHERE i."+net.ddp2p.common.table.identity.default_id+"==1 LIMIT 1;",
    			new String[]{});
    	if(id.size()==0){
    		if(DEBUG)System.err.println("No default identity found!");
    		return null;
    	}
    	return Util.getString(id.get(0).get(0));
	}
	public static Language get_authorship_lang() throws P2PDDSQLException {
    	ArrayList<ArrayList<Object>> id;
    	id=Application.getDB().select("SELECT "+net.ddp2p.common.table.identity.authorship_lang +
    			" FROM "+net.ddp2p.common.table.identity.TNAME+" AS i" +
    			" WHERE i."+net.ddp2p.common.table.identity.default_id+"==1 LIMIT 1;",
    			new String[]{});
    	if(id.size()==0){
    		if(DEBUG)System.err.println("No default identity found!");
    		return new Language("en","US");//null;
    	}
    	String alang= Util.getString(id.get(0).get(0));
    	String[] lang = alang.split(Pattern.quote("_"));
    	if(lang.length>=2)return new Language(lang[0],lang[1]);
    	return new Language(lang[0],lang[0]);
	}
	public static boolean test_proper_directory(String ld) {
    	String dirs[] = ld.split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_SEP));
    	for(int k=0; k<dirs.length; k++) {
    		if(dirs[k] == null){
    			Application_GUI.warning(__("Test Error for "+dirs[k]), __("Error installing directories (null)"));
    			return false;
    		}
    		Address adr;
    		try {
    			adr = new Address(dirs[k]);
    		} catch (Exception e) {
    			Application_GUI.warning(__("Error for")+" "+dirs[k]+"\nParsing Error: "+e.getMessage(), __("Error installing directories (impropper)"));
    			return false;
    		}
    		try{
    			new InetSocketAddress(InetAddress.getByName(adr.getIP()),adr.getTCPPort());
    		} catch(Exception e) {
    			Application_GUI.warning(__("Error for")+" "+dirs[k]+"\nConnection Error: "+e.getMessage(), __("Error installing directories"));
    			return false;
    		}
    	}
		return true;
	}
	public static void load_listing_directories_noexception() {
		try {
			load_listing_directories();
		} catch (NumberFormatException | UnknownHostException
				| net.ddp2p.common.util.P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Loads the listing directories from the "directory_address" or, if none, from the "application" table
	 * @throws P2PDDSQLException
	 * @throws NumberFormatException
	 * @throws UnknownHostException
	 */
	public static void load_listing_directories() throws P2PDDSQLException, NumberFormatException, UnknownHostException{
		Identity.setListing_directories_loaded(true);
		DirectoryAddress dirs[] = DirectoryAddress.getActiveDirectoryAddresses();
		if ((dirs == null) || (dirs.length == 0)) {
			DirectoryAddress _dirs[] = DirectoryAddress.getDirectoryAddresses();
			if ((_dirs == null) || (_dirs.length == 0)) {
	     		String listing_directories;
				try {
					listing_directories = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
					if (DEBUG) System.out.println("DD: load_listing_directories: Got :"+listing_directories);
					DirectoryAddress.reset(listing_directories);
					dirs = DirectoryAddress.getActiveDirectoryAddresses();
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}
     	}
     	if ((dirs == null) || (dirs.length == 0)) {
    		if (! DD.WARNED_NO_DIRS) {
    			Application_GUI.warning(__("Currently there are no listing_directories for connections found at Connections initialization: " +
    					"\nDo not forget to add some later \n" +
    					"(e.g., from the DirectDemocracyP2P.net list)!\n" +
    					"If you have a stable IP, than you probably do not need it."), __("Configuration"));
    			DD.WARNED_NO_DIRS = true;
    		}
    		return;
    	}
    	Identity.getListing_directories_string().clear();
    	Identity.getListing_directories_inet().clear(); 
    	Identity.getListing_directories_addr().clear();
    	for (int k=0; k<dirs.length; k++) {
    		try{
        		Address adr = new Address(dirs[k]);
        		InetSocketAddress isa = new InetSocketAddress(InetAddress.getByName(adr.getIP()),adr.getTCPPort());
        		adr.inetSockAddr = isa;
        		Identity.getListing_directories_addr().add(adr);
        		Identity.getListing_directories_string().add(dirs[k].toString());
    			Identity.getListing_directories_inet().add(isa);
    		} catch (Exception e) {
    			Application_GUI.warning(__("Error for")+" "+dirs[k]+"\nLoad Error: "+e.getMessage(), __("Error installing directories"));
    			e.printStackTrace();
    		}
    	}
	}
	static public boolean setAppTextNoSync(String field, String value) throws P2PDDSQLException{
		synchronized(Application.getDB()){
			ArrayList<ArrayList<Object>> rows = Application.getDB().select("SELECT "+net.ddp2p.common.table.application.value+
					" FROM "+net.ddp2p.common.table.application.TNAME+
					" WHERE "+net.ddp2p.common.table.application.field+"=?;",
					new String[]{field});
			if(rows.size()>0){
				String oldvalue = Util.getString(rows.get(0).get(0));
				if(((oldvalue==null) && (value==null)) || 
					((oldvalue!=null) && (value!=null) && oldvalue.equals(value))) return true;
				Application.getDB().updateNoSync(
					net.ddp2p.common.table.application.TNAME,
					new String[]{net.ddp2p.common.table.application.value},
					new String[]{net.ddp2p.common.table.application.field},
					new String[]{value, field});
			}else{
					try{
						Application.getDB().insertNoSync(net.ddp2p.common.table.application.TNAME, new String[]{net.ddp2p.common.table.application.field, net.ddp2p.common.table.application.value}, new String[]{field, value});
					}catch(Exception e){
						e.printStackTrace();
						Application_GUI.warning(__("Error inserting:")+"\n"+__("value=")+Util.trimmed(value)+"\n"+__("field=")+field+"\n"+__("Error:")+e.getLocalizedMessage(), __("Database update error"));
					}
					if(DEBUG){
						Application_GUI.warning(__("Added absent property: ")+field, __("Properties"));
						System.err.println("Why absent");
						Util.printCallPath("");
					}
			}
		}
		return true;
	}
	/**
	 * Uses Application.db, which should be set to the right DB
	 * @param field
	 * @param value
	 * @return
	 * @throws P2PDDSQLException
	 */
	static public boolean setAppText(String field, String value) throws P2PDDSQLException{
		return setAppText(field,value,false);
	}
	static public boolean setAppTextNoException(String field, String value) {
		try {
			return setAppText(field,value);
		} catch (net.ddp2p.common.util.P2PDDSQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	public static boolean setAppText(String field, String value,
			boolean debug) throws P2PDDSQLException {
		return setAppText(Application.getDB(), field, value, debug);
	}
	public static boolean setAppText(DBInterface db, String field, String value,
			boolean debug) throws P2PDDSQLException {
		boolean DEBUG = DD.DEBUG || debug;
		if(DEBUG) System.err.println("DD:setAppText: field="+field+" new="+value);
		String _value = getExactAppText(db.getImplementation(), field);
		if(DEBUG) System.err.println("DD:setAppText: field="+field+" old="+_value);
    	db.update(net.ddp2p.common.table.application.TNAME, new String[]{net.ddp2p.common.table.application.value}, new String[]{net.ddp2p.common.table.application.field},
    			new String[]{value, field}, DEBUG);
    	if (value!=null){
    		String old_val = getExactAppText(db.getImplementation(), field);
    		if(DEBUG) System.err.println("DD:setAppText: field="+field+" old="+old_val);
    		if (!value.equals(old_val)) {
    			db.insert(
    					net.ddp2p.common.table.application.TNAME,
    					new String[]{net.ddp2p.common.table.application.field, net.ddp2p.common.table.application.value},
    					new String[]{field, value},
    					DEBUG);
    			if(DEBUG)Application_GUI.warning(__("Added absent property: ")+field, __("Properties"));
    		}
    	}
		return true;
	}
	static public boolean setAppBoolean(String field, boolean val){
		String value = Util.bool2StringInt(val);
		try {
			return setAppText(field, value);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	/**
	 * For empty string "" it returns null;
	 * @param field
	 * @return
	 * @throws P2PDDSQLException
	 */
	static public String getAppText(String field) throws P2PDDSQLException {
		String result = getExactAppText(field);
   		if("".equals(result)) result = null;
   		return result;
	}
	static public String getAppTextNoException(String field) {
		try {
			return getAppText(field);
		} catch (net.ddp2p.common.util.P2PDDSQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * Exact value needed for exact comparison with new valued to preclude reinsertion
	 * @param field
	 * @return
	 * @throws P2PDDSQLException
	 */
	static public String getExactAppText(String field) throws P2PDDSQLException{
		int installation =  Application.getCurrentInstallationFromThread();
		return getExactAppText(Application.getDB(installation).getImplementation(), field);
	}
	/**
	 * 
	 * @param db
	 * @param field
	 * @return
	 * @throws P2PDDSQLException
	 */
	static public String getExactAppText(DB_Implementation db, String field) throws P2PDDSQLException{
    	ArrayList<ArrayList<Object>> id;
    	id=db.select("SELECT "+net.ddp2p.common.table.application.value +
    			" FROM "+net.ddp2p.common.table.application.TNAME+" AS a " +
    			" WHERE "+net.ddp2p.common.table.application.field+"=? LIMIT 1;",
    			new String[]{field}, DEBUG);
    	if(id.size()==0){
    		if(DEBUG) System.err.println(__("No application record found for field: ")+field);
    		return null;
    	}
    	String result = Util.getString(id.get(0).get(0));
   		return result;
	}
	public static boolean getAppBoolean(String field, boolean _def) {
    	String aval = null;
		try {
			aval = DD.getExactAppText(field);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if (aval==null) return false;
		if(aval.equals("1")) return true; 
		return false;
	}
	static public boolean getAppBoolean(String field) throws P2PDDSQLException{
		return getAppBoolean(field, false);
	}
	static public boolean startDirectoryServer(boolean on, int port) throws NumberFormatException, P2PDDSQLException {
		DirectoryServer ds= Application.getG_DirectoryServer();
		if (on == false) {
			if (ds != null) {
				ds.turnOff();
				Application.setG_DirectoryServer(null);
				if(DEBUG)System.out.println("DD:startDirectoryServer:Turning off");
				return true;
			} else {
				return false;
			}
		}
		if (ds != null) {
			if(DEBUG)System.out.println("DD:startDirectoryServer:Turned off already");
			return false;
		}
		if (port <= 0) {
			String ds_port = getAppText("DirectoryServer_PORT");
			if(DEBUG)System.out.println("DD:startDirectoryServer:Saved port="+ds_port);
			if(ds_port!=null)port = Integer.parseInt(ds_port);
			else port = DirectoryServer.PORT;
		}
		try {
			Application.setG_DirectoryServer(new DirectoryServer(port));
			Application.getG_DirectoryServer().start();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	static public boolean startServer(boolean on, Identity peer_id) throws NumberFormatException, P2PDDSQLException {
		Server as = Application.getG_TCPServer();
		if(DEBUG)System.err.println("Will set server as="+as+" id="+peer_id);
		if (on == false) {
			if (as != null) {
				as.turnOff(); Application.setG_TCPServer(null);
				if(DEBUG)System.err.println("Turned off");
				return true;
			} else {
				return false;
			}
		}
		if (as != null){
			if(DEBUG)System.err.println("Was not null");
			return false;
		}
		try {
			Application.setG_TCPServer(new Server(peer_id));
			Application.getG_TCPServer().start();
		} catch (Exception e) {
			if(DEBUG)System.err.println("Error:"+e);
			return false;
		}
		return true;
	}
	/**
	 * 
	 * @param on To turn on or off.
	 * @param peer_id This is the peer_identity as set in the database.
	 * @return
	 * @throws NumberFormatException
	 * @throws P2PDDSQLException
	 */
	static public boolean startUServer(boolean on, Identity peer_id) throws NumberFormatException, P2PDDSQLException {
		boolean DEBUG = true;
		UDPServer aus = Application.getG_UDPServer();
		if(DEBUG) System.err.println("Will set server aus="+aus+" id="+peer_id);
		if (on == false) {
			if (aus != null) {
				aus.turnOff(); Application.setG_UDPServer(null);
				if(DEBUG) System.err.println("Turned off");
				return true;
			} else {
				return false;
			}
		}
		if (aus != null) {
			if (DEBUG) System.err.println("Was not null");
			return false;
		}
		try {
			if(DEBUG) System.err.println("DD:startUServ: <init>");
			Application.setG_UDPServer(new UDPServer(peer_id));
			if(DEBUG) System.err.println("DD:startUServ: <init> done, start");
			Application.getG_UDPServer().start();
		} catch (Exception e) {
			if(DEBUG) System.err.println("Error:"+e);
			return false;
		}
		return true;
	}
	static public boolean startNATServer(boolean on) throws NumberFormatException, P2PDDSQLException {
		NATServer aus = Application.getG_NATServer();
		if(DEBUG) System.err.println("Will set server nat_s="+aus);
		if (on == false) {
			if (aus != null) {
				aus.turnOff(); Application.setG_NATServer(null);
				if(DEBUG) System.err.println("Turned off");
				return true;
			} else {
				return false;
			}
		}
		if (aus != null) {
			if (DEBUG) System.err.println("Was not null");
			return false;
		}
		try {
			if(DEBUG) System.err.println("DD:startNATServ: <init>");
			Application.setG_NATServer(new NATServer());
			if(DEBUG) System.err.println("DD:startNATServ: <init> done, start");
			Application.getG_NATServer().start();
		} catch (Exception e) {
			if(DEBUG) System.err.println("Error:"+e);
			return false;
		}
		return true;
	}
	/**
	 * 
	 * @param on 
	 *   If on is false then the client processed is stopped.
	 * @return
	 * false on error/no need to start, or when nothing to stop
	 * @throws NumberFormatException
	 * @throws P2PDDSQLException
	 */
	static public boolean startClient(boolean on) throws NumberFormatException, P2PDDSQLException {
		boolean DEBUG = DD.DEBUG || Client2.DEBUG || ClientSync.DEBUG;
		if (DEBUG) System.out.println("DD: startClient: " + on);
		IClient old_client = Application.getG_PollingStreamingClient();
		if (on == false) {
			if (old_client != null) {
				old_client.turnOff();
				Application.setG_PollingStreamingClient(null);
				return true;
			} else {
				return false;
			}
		}
		if (old_client != null) return false;
		try {
			Application.setG_PollingStreamingClient(ClientSync.startClient());
		} catch (Exception e) {
			return false;
		}
		if (DEBUG) System.out.println("Client2: startClient: done");
		return true;
	}
	/**
	 * Method to make the client wake up from sleep and retry connections (e.g. after new addresses are received from directories)
	 */
	static public boolean touchClient() {
		boolean result = true;
		IClient old_client = Application.getG_PollingStreamingClient();
		if (old_client == null) {
			try {
				DD.startClient(true);
			} catch (Exception e) {
				e.printStackTrace();
				result = false;
				return result;
			}
			old_client = Application.getG_PollingStreamingClient();
		}
		old_client.wakeUp();
		return result;
	}
	public static SK getConstituentSK(long constituentID) throws P2PDDSQLException {
		String constGID = D_Constituent.getGIDFromLID(constituentID);
		return Util.getStoredSK(constGID);
	}
	public static void setBroadcastServerStatus(boolean run) {
		if(run) {
			if(Application.getG_BroadcastServer() != null) return;
			try {
				Application.setG_BroadcastServer(new BroadcastServer());
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return;
			}
			Application.getG_BroadcastServer().start();
		}else{
			if(Application.getG_BroadcastServer() == null) return;
			Application.getG_BroadcastServer().stopServer();
			Application.setG_BroadcastServer(null);
		}
		Application_GUI.setBroadcastServerStatus_GUI(run);
	}
	public static void setBroadcastClientStatus(boolean run) {
		if(run) {
			if(Application.getG_BroadcastClient() != null) return;
			try {
				Application.setG_BroadcastClient(new BroadcastClient());
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return;
			}
			Application.getG_BroadcastClient().start();
		}else{
			if(Application.getG_BroadcastClient() == null) return;
			Application.getG_BroadcastClient().stopClient();
			Application.setG_BroadcastClient(null);
		}		
		Application_GUI.setBroadcastClientStatus_GUI(run);
	}
	public static void setSimulatorStatus(boolean run) {
		if(run) {
			if(Application.g_Simulator != null) return;
			Application.g_Simulator = new Fill_database();
			Application.g_Simulator.start();
		}else{
			if(Application.g_Simulator == null) return;
			Application.g_Simulator.stopSimulator();
			Application.g_Simulator=null;
		}
		Application_GUI.setSimulatorStatus_GUI(run);
	}
	/**
	 * Called from simulator generating data
	 * @param keys
	 * @param name
	 * @throws P2PDDSQLException
	 */
	public static void storeSK(Cipher keys, String name) throws P2PDDSQLException{
		storeSK(keys, name+Util.getGeneralizedTime(), null, null, null);
	}
	/**
	 * 
	 * @param keys
	 * @param name
	 * @param date
	 * @throws P2PDDSQLException
	 */
	public static void storeSK(Cipher keys, String name, String date) throws P2PDDSQLException{
		storeSK(keys, name, null, null, null, date);
	}
	/**
	 * 
	 * @param keys
	 * @param pGIDname
	 * @param public_key_ID
	 * @param secret_key
	 * @param pGIDhash
	 * @throws P2PDDSQLException
	 */
	public static void storeSK(Cipher keys, String pGIDname, 
			String public_key_ID, String secret_key, String pGIDhash) throws P2PDDSQLException{
		storeSK(keys, pGIDname, public_key_ID, secret_key, pGIDhash, Util.getGeneralizedTime());
	}
	/**
	 * s
	 * @param keys
	 * @param pGIDname
	 * @param public_key_ID
	 * @param secret_key
	 * @param pGIDhash
	 * @param date
	 * @throws P2PDDSQLException
	 */
	public static void storeSK(Cipher keys, String pGIDname, String public_key_ID, String secret_key, String pGIDhash, String date) throws P2PDDSQLException{
		if (public_key_ID ==  null) {
			byte[] pIDb = Util.getKeyedIDPKBytes(keys);
			public_key_ID = Util.getKeyedIDPK(pIDb);
			if(DEBUG) System.out.println("DD:storeSK public key: "+public_key_ID);
		}
		if (secret_key == null) {
			secret_key = Util.getKeyedIDSK(keys);
			if(DEBUG) System.out.println("DD:storeSK secret key: "+secret_key);
		}
		if (pGIDhash == null) {
			pGIDhash = Util.getGIDhash(public_key_ID);
			if(DEBUG) System.out.println("DD:storeSK public key hash: "+pGIDhash);
		}
		if(pGIDname == null) pGIDname = "KEY:"+date;
		Application.getDB().insert(net.ddp2p.common.table.key.TNAME,
				new String[]{net.ddp2p.common.table.key.public_key,net.ddp2p.common.table.key.secret_key,net.ddp2p.common.table.key.ID_hash,net.ddp2p.common.table.key.creation_date,
				net.ddp2p.common.table.key.name,net.ddp2p.common.table.key.type},
				new String[]{public_key_ID, secret_key, pGIDhash,date,
				pGIDname,
				Util.getKeyedIDType(keys)}, DEBUG);
	}
	public static final int[] VERSION_INTS = Util.getVersion(VERSION);
	public static final boolean SIGN_DIRECTORY_ANNOUNCEMENTS = false;
	public static final boolean KEEP_UNCERTIFIED_SOCKET_ADDRESSES = false;
	public static String WARN_OF_INVALID_SCRIPTS_BASE_DIR = null;
	public static final int SIZE_DA_PREFERRED = 1000;
	public static final int SIZE_DA_MAX = 10000;
	public static final Object status_monitor = new Object();
	public static final int MAX_DPEER_UNCERTIFIED_ADDRESSES = 5;
	public static final boolean DIRECTORY_ANNOUNCEMENT_UDP = true;
	public static final boolean DIRECTORY_ANNOUNCEMENT_TCP = true;
	public static final String WINDOWS_NO_IP = "No IP"; // to signal no IP4 in ipconfig
	public static final String ALREADY_CONTACTED = __("Already contacted ***");
	/**
	 * Peers
	 */
	public static final int PEERS_STATE_CONNECTION_FAIL =0;
	public static final int PEERS_STATE_CONNECTION_TCP = 1;
	public static final int PEERS_STATE_CONNECTION_UDP = 2;
	public static final int PEERS_STATE_CONNECTION_UDP_NAT = 3;
	public static final boolean STREAMING_TABLE_PEERS = false;
	public static final boolean STREAMING_TABLE_PEERS_ADDRESS_CHANGE = false;
	public static final boolean ANONYMOUS_ORG_ACCEPTED = true; 
	public static final boolean ANONYMOUS_ORG_AUTHORITARIAN_CREATION = true; 
	public static final boolean ANONYMOUS_ORG_GRASSROOT_CREATION = true; 
	public static boolean ANONYMOUS_ORG_ENFORCED_AT_HANDLING = false;
	public static final boolean ANONYMOUS_CONST_ACCEPTED = true; 
	public static final boolean ANONYMOUS_CONST_CREATION = true; 
	public static final boolean ANONYMOUS_MOTI_ACCEPTED = true; 
	public static final boolean ANONYMOUS_MOTI_CREATION = true; 
	public static final boolean ANONYMOUS_JUST_ACCEPTED = true; 
	public static final boolean ANONYMOUS_JUST_CREATION = true; 
	public static final boolean ANONYMOUS_NEWS_ACCEPTED = true; 
	public static final boolean ANONYMOUS_NEWS_CREATION = true; 
	public static final boolean VERIFY_GIDH_ALWAYS = false;
	public static final String NO_CONTACT = "No contact";
	public static final int CLIENTS_NB_MEMORY = 100; 
	public static final int CLIENTS_RANDOM_MEMORY = 10; 
	public static final String APP_CLAIMED_DATA_HASHES = "CLAIMED_DATA_HASHES";
	public static final long DOMAINS_UPDATE_WAIT = 1000 * 200;
	/**
	 *  // when motions signatures were deleted by error, this fixes those who were signed with a key I know.
	 */
	public static final boolean FIX_UNSIGNED_MOTIONS = false;
	/**
	 * Accept unsigned motions (but normally accompanied with some signatures
	 */
	public static final boolean ACCEPT_ANONYMOUS_MOTIONS = true;
	/**
	 * We can use the null name to detect container D_Organization data that only encode the GIDH of the orh in a message.
	 * That is possible only when ACCEPT_ORGANIZATIONS_WITH_NULL_NAME is FALSE
	 */
	public static final boolean ACCEPT_ORGANIZATIONS_WITH_NULL_NAME = false;
	/**
	 * To store in the application table the date of the last made recommendation of testers
	 */
	public static final String APP_LAST_RECOMMENDATION_OF_TESTERS = "APP_LAST_RECOMMENDATION_OF_TESTERS";
	/**
	 * Set this one to true to stop the recommendation of testers
	 */
	public static final String APP_STOP_RECOMMENDATION_OF_TESTERS = "STOP_RECOMMENDATION_OF_TESTERS";
	public static final String APP_USER_SOPHISTICATED_IN_SELECTING_TESTERS = "APP_USER_SOPHISTICATED_IN_SELECTING_TESTERS";
	public static final String AUTOMATIC_TESTERS_RATING_BY_SYSTEM = "AUTOMATIC_TESTERS_RATING_BY_SYSTEM";
	public static final boolean DEBUG_TMP_GIDH_MANAGEMENT = false;
	public static final int MAX_CONTAINER_SIZE = 5000000; 
	public static boolean DEBUG_COMMUNICATION_ADDRESSES = false;
	public static boolean DEBUG_COMMUNICATION_STUN = false;
	public static int MAX_ORG_ICON_LENGTH = 20000;
	public static int MAX_CONSTITUENT_ICON_LENGTH = 20000; 
	public static int MAX_PEER_ICON_LENGTH = 20000;
	public static boolean RELEASE = true;
	/** dir_IP: (GID: ()addresses) */
	public static Hashtable<String,Hashtable<String,DirectoryAnswerMultipleIdentities>> dir_data = new Hashtable<String,Hashtable<String,DirectoryAnswerMultipleIdentities>>();
	/**
	 * Is the data for me as constituent fully input?
	 * @param organization_ID
	 * @param constituent_ID
	 * @return
	 */
	public boolean isMyConstituentReady(long constituent_ID){
		D_Constituent c = D_Constituent.getConstByLID(constituent_ID, true, false);
		if (c != null && c.getSurname() != null) return true;
		return false;
	}
	/**
		 * Returns the current version as an array of ints: DD.VERSION_INTS
		 * @return
		 */
	public static int[] getMyVersion() {
		return VERSION_INTS;
	}
	/**
	 * String explain[] = new String[1];
	 * if (! config.DD.embedPeerInBMP (file, explain, new util.DD_Address(peer)))
	 * 	//DisplayError(explain[0]);
	 * 
	 * 
	 * Hopefully somebody will create additional StegoStructures to holding (helping to import-export)
	 * objects of type D_Motion, D_Organization, D_Vote (containing eventually the corresponding D_Constituent, D_Vote objects)
	 * 
	 * @param file : handle to the file where the result will be stored. 
	 * @param explain : array of strings with size "1", to store an explanation in case of error.
	 * @param myAddress : An object that will be stored in the bitmap. Can be any subclass of StegoStructure,
	 *  such as:
	 *     util.DD_Address,  (for holding a peer)
	 *     util.DD_Slogan,  (for holding nice slogans to be imported)
	 *     util.DD_Testers,  (for holding a information about testers that voluteer to evaluate new releases)
	 *     util.DD_DirectoryServer, (for holding the address of a directory server)
	 *     util.DD_EmailableAttachment (to verify somebody's identity, .. I forgot in which step)
	 *     util.DD_IdentityVerification_Answer (to carry the answer to a identity verification challenge)
	 *     util.DD_IdentityVerification_Request (to carry the challenge for an identity verification)
	 *     
	 *     
	 * @return returns true on success
	 */
	public static boolean embedPeerInBMP(File file,
			String explain[],
			StegoStructure myAddress
			) {
		BMP[] _data = new BMP[1];
		byte[][] _buffer_original_data = new byte[1][]; 
		byte[] adr_bytes = myAddress.getBytes();
		if (file.exists()) {
			boolean fail = EmbedInMedia.cannotEmbedInBMPFile(file, adr_bytes, explain, _buffer_original_data, _data);
			if (fail) {
				if (_DEBUG) System.out.println("DD: embedPeerInBMP: failed embedding in existing image: "+explain[0]);
				return false;
			}
		}
		if ( EmbedInMedia.DEBUG ) System.out.println("EmbedInMedia:actionExport:bmp");
		try {
			if ( ! file.exists()) {
				EmbedInMedia.saveSteganoBMP(file, adr_bytes, myAddress.getSignShort()); 
			} else {
				FileOutputStream fo;
				fo = new FileOutputStream(file);
				int offset = _data[0].startdata;
				int word_bytes = 1;
				int bits = 4;
				fo.write(EmbedInMedia.getSteganoBytes(adr_bytes, _buffer_original_data[0], offset, word_bytes, bits, myAddress.getSignShort()));
				fo.close();
			}
		} catch (IOException e) {
			if (explain != null && explain.length > 0) explain[0] = e.getLocalizedMessage();
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public static int findStegoIndex(StegoStructure[] adr, StegoStructure ss) {
		for (int i = 0; i < adr.length; i ++) {
			if (ss.getClass().equals(adr[i].getClass())) return i;
		}
		return -1;
	}
	/**
	 * 
	 * @param is
	 * @param mime
	 * @param adr
	 * @param selected (should have length 1, and inited to -1)
	 * @return (returns error message)
	 * @throws IOException
	 */
	public static String loadFromMedia(InputStream is, String mime, StegoStructure[] adr, int[] selected) throws IOException {
		byte[] buffer = new byte[DD.MAX_CONTAINER_SIZE];
		if (selected != null && selected.length > 0) selected[0] = -1;
		int len = Util.readAll(is, buffer);
		if (DEBUG) System.out.println("Util: readAll: got n="+len+"/"+buffer.length);
		if (len == buffer.length)
			if (_DEBUG) System.out.println("Util: readAll: !got n="+len+"/"+buffer.length);
		return loadFromMedia(buffer, len, mime, adr, selected);
	}
	public static String loadFromMedia(File file, String mime, StegoStructure[] adr, int[] selected) throws IOException {
		FileInputStream fis=new FileInputStream(file);
		byte[] b = new byte[(int) file.length()];
		fis.read(b);
		fis.close();
		return loadFromMedia(b, b.length, mime, adr, selected);
	}
	/**
	 * 
	 * @param is
	 * @param adr
	 * @param selected
	 * @return
	 * @throws IOException
	 */
	public static String loadFromMedia(byte[] buffer, int len, String mime, StegoStructure[] adr, int[] selected) throws IOException {
		if (selected != null && selected.length > 0) selected[0] = -1;
		if ("image/bmp".equalsIgnoreCase(mime) || "image/x-ms-bmp".equalsIgnoreCase(mime)) {
			return loadBMP(buffer, adr, selected);
		}
		if ("image/gif".equalsIgnoreCase(mime)) {
			return loadGIF(buffer, adr, selected);
		}
		if ("image/jpeg".equalsIgnoreCase(mime)) {
			JPEG file = new JPEG();
			file.load(buffer, len);
			if (selected != null && selected.length > 0) selected[0] = -1;
			return "Not implemented";
		}
		if ("image/png".equalsIgnoreCase(mime)) {
			PNG file = new PNG();
			file.load(buffer, len);
			if (selected != null && selected.length > 0) selected[0] = -1;
			return "Not implemented";
		}
		if (mime.contains("text/")) {
			StegoStructure ss[] = new StegoStructure[1];
			String err = extractStegoObject(new String(buffer, 0, len), ss);
			if (ss[0] == null) return __("Extracting from text:")+" "+err;
			selected[0] = findStegoIndex(adr, ss[0]);
		}
		return null;
	}
	public static String loadGIF(File file, StegoStructure[] adr, int[] selected) throws IOException {
		FileInputStream fis=new FileInputStream(file);
		byte[] b = new byte[(int) file.length()];  
		fis.read(b);
    	fis.close();
    	return DD.loadGIF(b, adr, selected);
	}
	public static String loadGIF(byte[] b, StegoStructure[] adr, int[] selected) throws IOException {
		boolean found = false;
    	int i = 0;
		int _selected = -1;
		if (_DEBUG) System.err.println("ControlPane: actionImport: Got: gif len="+b.length);
		do {
			while (i < b.length) {
				if (b[i] == (byte) 0x3B) {
					found = true;
					i ++;
					break;
				}
				i++;
			}
			if (_DEBUG) System.err.println("ControlPane: actionImport: Got: gif position="+i);
			if (! found || i >= b.length) {
						return __("Cannot Extract address in GIF file.")+" "+__("No valid data in the picture!");
			}
			byte[] addBy = new byte[b.length-i]; 
			System.arraycopy(b,i,addBy,0,b.length-i);
			for (int k = 0; k < adr.length; k ++) {
				if (_DEBUG) System.err.println("ControlPane: actionImport: Got: gif try adr#="+k+"/"+adr.length);
				try {
					BigInteger expected = new BigInteger(""+adr[k].getSignShort());
					BigInteger _found = new Decoder(addBy).getTagValueBN();
					if (! expected.equals(_found)) {
						if (_DEBUG) System.err.println("ControlPane: actionImport: Got: gif not ASN1 tag of ="+adr[k].getClass()+" "+expected+" vs "+_found);
						continue;
					}
					adr[k].setBytes(addBy);
					_selected = k;
					if (_DEBUG) System.err.println("ControlPane: actionImport: Got: gif success adr#="+k+"/"+adr.length+" val="+adr[k]);
					break;
				} catch (Exception e1) { 
					if (_DEBUG) System.err.println("ControlPane: actionImport: Got: gif failed adr#="+k+"/"+adr.length);
					if (EmbedInMedia.DEBUG){
						e1.printStackTrace();
					}
				}
			}
		} while (i < b.length && _selected < 0);
		if (_DEBUG) System.err.println("ControlPane: actionImport: Got: gif done at i#="+i+" adr="+_selected+" val="+adr[_selected]);
		if (_selected == -1) {
			return __("Failed to parse GIF file");
		}
		if ((selected != null) && (selected.length > 0))
			selected[0] = _selected;
		return null;
	}
	/**
		StegoStructure adr[] = DD.getAvailableStegoStructureInstances();
		int[] selected = new int[1];
		String error;
		if ((error = DD.loadBMP(adr, selected)) == null)	
			adr[selected[0]].save();
		else
		On success, call the following:
		adr[selected[0].save();
	 * 
	 * @param file
	 * @param adr
	 * @param selected
	 * @return null on success, otherwise returns explanation of error.
	 * @throws IOException
	 */
	public static String loadBMP(File file, StegoStructure[] adr, int[] selected) throws IOException {
		FileInputStream fis=new FileInputStream(file);
		byte[] b = new byte[(int) file.length()];
		fis.read(b);
		fis.close();
		return loadBMP(b, adr, selected);
	}
	public static String loadBMP(byte[] b, StegoStructure[] adr, int[] selected) throws IOException {
		String explain = null;
		boolean fail = false;
		BMP data = new BMP(b, 0);
		if ((data.compression != BMP.BI_RGB) || (data.bpp < 24)) {
			explain = " - "+__("Not supported compression: "+data.compression+" "+data.bpp);
			fail = true;
		} else {
			int offset = data.startdata;
			int word_bytes = 1;
			int bits = 4;
			try {
				EmbedInMedia.setSteganoBytes(adr, selected, b, offset, word_bytes, bits);
			} catch (ASN1DecoderFail e1) {
				e1.printStackTrace();
				explain = " - "+ __("No valid data in picture!");
				fail = true;
			}
		}
		if (! fail) {
			return null;
		}
		return explain;
	}
	/**
	 * Create a constituent without name (never called?)
	 * 
	 * first, on ConstituentTree or Orgs, select/create an Identity
	 * In the create Identity, have to select/create a key, slogan, then call this function
	 * 
	 * Later go to ConstituentTree and in popup select "Register" to finalize adding one's address and name.
	 * The name is defining the end of the registration in grass-root
	 * In authoritarian, will wait certificate
	 * 
	 * @param organization_ID
	 * @param key_ID
	 */
	public static void load_broadcast_probabilities(String val) {
		if(val==null) return;
		String[] probs= val.split(Pattern.quote(DD.PROB_SEP));
		float constit = Broadcasting_Probabilities.broadcast_constituent;
		float orgs = Broadcasting_Probabilities.broadcast_organization;
		float motions = Broadcasting_Probabilities.broadcast_motion;
		float justifications = Broadcasting_Probabilities.broadcast_justification;
		float witness = Broadcasting_Probabilities.broadcast_witness;
		float neighbors = Broadcasting_Probabilities.broadcast_neighborhood;
		float votes = Broadcasting_Probabilities.broadcast_vote;
		float peers = Broadcasting_Probabilities.broadcast_peer;
		for(String e: probs) {
			if(e==null) continue;
			String prob[] = e.split(Pattern.quote(DD.PROB_KEY_SEP));
			if(prob.length<2) continue;
			if(DD.PROB_CONSTITUENTS.equals(prob[0])) constit = new Float(prob[1]).floatValue();
			if(DD.PROB_ORGANIZATIONS.equals(prob[0])) orgs = new Float(prob[1]).floatValue();
			if(DD.PROB_MOTIONS.equals(prob[0])) motions = new Float(prob[1]).floatValue();
			if(DD.PROB_JUSTIFICATIONS.equals(prob[0])) justifications = new Float(prob[1]).floatValue();
			if(DD.PROB_WITNESSES.equals(prob[0])) witness = new Float(prob[1]).floatValue();
			if(DD.PROB_NEIGHBORS.equals(prob[0])) neighbors = new Float(prob[1]).floatValue();
			if(DD.PROB_VOTES.equals(prob[0])) votes = new Float(prob[1]).floatValue();
			if(DD.PROB_PEERS.equals(prob[0])) peers = new Float(prob[1]).floatValue();
		}
		float sum = constit + orgs + motions + justifications + witness + neighbors + votes + peers;
		constit = constit/sum;
		orgs = orgs/sum;
		motions = motions/sum;
		justifications = justifications/sum;
		witness = witness/sum;
		neighbors = neighbors/sum;
		votes = votes/sum;
		peers = peers/sum;
		Broadcasting_Probabilities.broadcast_constituent = constit;
		Broadcasting_Probabilities.broadcast_organization = orgs;
		Broadcasting_Probabilities.broadcast_motion = motions;
		Broadcasting_Probabilities.broadcast_justification = justifications;
		Broadcasting_Probabilities.broadcast_witness = witness;
		Broadcasting_Probabilities.broadcast_neighborhood = neighbors;
		Broadcasting_Probabilities.broadcast_vote = votes;
		Broadcasting_Probabilities.broadcast_peer = peers;
	}
	public static void load_generation_probabilities(String val) {
		if(val==null) return;
		String[] probs= val.split(Pattern.quote(DD.PROB_SEP));
		float constit = Broadcasting_Probabilities.broadcast_constituent;
		float orgs = Broadcasting_Probabilities.broadcast_organization;
		float motions = Broadcasting_Probabilities.broadcast_motion;
		float justifications = Broadcasting_Probabilities.broadcast_justification;
		float witness = Broadcasting_Probabilities.broadcast_witness;
		float neighbors = Broadcasting_Probabilities.broadcast_neighborhood;
		float votes = Broadcasting_Probabilities.broadcast_vote;
		float peers = Broadcasting_Probabilities.broadcast_peer;
		for(String e: probs) {
			if(e==null) continue;
			String prob[] = e.split(Pattern.quote(DD.PROB_KEY_SEP));
			if(prob.length<2) continue;
			if(DD.PROB_CONSTITUENTS.equals(prob[0])) constit = new Float(prob[1]).floatValue();
			if(DD.PROB_ORGANIZATIONS.equals(prob[0])) orgs = new Float(prob[1]).floatValue();
			if(DD.PROB_MOTIONS.equals(prob[0])) motions = new Float(prob[1]).floatValue();
			if(DD.PROB_JUSTIFICATIONS.equals(prob[0])) justifications = new Float(prob[1]).floatValue();
			if(DD.PROB_WITNESSES.equals(prob[0])) witness = new Float(prob[1]).floatValue();
			if(DD.PROB_NEIGHBORS.equals(prob[0])) neighbors = new Float(prob[1]).floatValue();
			if(DD.PROB_VOTES.equals(prob[0])) votes = new Float(prob[1]).floatValue();
			if(DD.PROB_PEERS.equals(prob[0])) peers = new Float(prob[1]).floatValue();
		}
		float sum = constit + orgs + motions + justifications + witness + neighbors + votes + peers;
		constit = constit/sum;
		orgs = orgs/sum;
		motions = motions/sum;
		justifications = justifications/sum;
		witness = witness/sum;
		neighbors = neighbors/sum;
		votes = votes/sum;
		peers = peers/sum;
		SimulationParameters.adding_new_constituent = constit;
		SimulationParameters.adding_new_organization = orgs;
		SimulationParameters.adding_new_motion = motions;
		SimulationParameters.adding_new_justification_in_vote = justifications;
		SimulationParameters.adding_new_witness = witness;
		SimulationParameters.adding_new_neighbor = neighbors;
		SimulationParameters.adding_new_vote = votes;
		SimulationParameters.adding_new_peer = peers;		
	}
	/**
	 * Return error message in case of error, null on success
	 * As side effect it sets Application.db
	 * @param attempt
	 * @return
	 */
	public static String testProperDB(String attempt) {
		System.out.println("testProperDB: enter "+attempt);
		File dbfile = new File(attempt);
		DD.TESTED_VERSION = null;
		if (!dbfile.exists() || !dbfile.isFile() || !dbfile.canRead()) {
			System.out.println("testProperDB: failed "+attempt);
			return __("File not readable.");
		}
		try {
			Application.setDB(new DBInterface(attempt));
			System.out.println("testProperDB: load "+attempt);
			ArrayList<ArrayList<Object>> v = Application.getDB().select(
					"SELECT "+net.ddp2p.common.table.application.value+" FROM "+net.ddp2p.common.table.application.TNAME+
					" WHERE "+net.ddp2p.common.table.application.field+"=? LIMIT 1;",
					new String[]{DD.DD_DB_VERSION}, DEBUG);
			if(v.size()>0)DD.TESTED_VERSION=Util.getString(v.get(0).get(0));
		}catch(Exception e){
			try {
				Application.getDB().close();
			} catch (net.ddp2p.common.util.P2PDDSQLException e1) {
				e1.printStackTrace();
			}
			Application.setDB(null);
			e.printStackTrace();
			return e.getLocalizedMessage();
		}
		return null;
	}
	/**
	 * Return error message in case of errr, null on success
	 * @param attempt
	 * @return
	 */
	public static String try_open_database(String attempt) {
		String error = testProperDB(attempt);
		if(error==null) {
			Application.DELIBERATION_FILE = attempt;
			return null;
		}else{
			return error;
		}
	}
	public static DBInterface load_Directory_DB(String dB_PATH) {
		DBInterface dbdir = null;
		String sql = "SELECT "+net.ddp2p.common.table.subscriber.subscriber_ID+
				" FROM "+net.ddp2p.common.table.subscriber.TNAME+
				" LIMIT 1;";
		try {
			String[]params = new String[]{};
			String dbase = Application.DIRECTORY_FILE;
			if(dB_PATH!=null) dbase = dB_PATH+Application.OS_PATH_SEPARATOR+dbase;
			dbdir = DirectoryServer.getDirDB(dbase);
			dbdir.select(sql, params, DEBUG);
		} catch (net.ddp2p.common.util.P2PDDSQLException e) {
			System.out.print(sql);
			e.printStackTrace();
			return null;
		}
		return dbdir;
	}
	public static boolean asking_topic = false;
	public static boolean isThisAnApprovedPeer(String senderID) {
		return true;
	}
	public static boolean GUI = true;
	/**
	 * @deprecated Use {@link MainFrame#main(String[])} instead
	 */
	static public void set_DEBUG(){
	}
	final static public Object stop_monitor = new Object();
	public static final boolean BLOCK_NEW_ARRIVING_TMP_ORGS = false;
	public static final boolean BLOCK_NEW_ARRIVING_TMP_CONSTITUENT = false;
	public static final boolean BLOCK_NEW_ARRIVING_TMP_MOTIONS = false;
    public static final String SAFE_TEXT_MY_HEADER_SEP = " | "; // at the end of the title. Unsafe since spaces often are trimmed
    public static final String SAFE_TEXT_MY_BODY_SEP = "|DDP2P|OBJECT|START||"; // at the beginning of the body
    public static final String SAFE_TEXT_MY_BODY_TRAILER_SEP = "|DDP2P|OBJECT|TRAILER|"; // at the end of the body (optional)
    public static final String SAFE_TEXT_ANDROID_SUBJECT_SEP = " - "; // inserted by 2014 versions of whatsup between title and body
	public static void stop_servers() {
        try {
            DD.startNATServer(false);
            DD.startUServer(false, null);
            DD.startServer(false, null);
            DDP2P_ServiceThread domainsDetectionThread = Server.domainsDetectionThread;
            if (domainsDetectionThread != null) {
            	domainsDetectionThread.turnOff();
            	domainsDetectionThread = null;
            }
            DD.startClient(false);
            Connections c = Client2.g_Connections;
            if (c != null) c.turnOff(); 
            DD.startDirectoryServer(false, 0);
            PluginRegistration.removePlugins();
            System.out.println("Servers Closed...");
        }catch (Exception er){er.printStackTrace();}
	}
	public static void clean_before_exit() {
        while (net.ddp2p.common.data.SaverThreadsConstants.getNumberRunningSaverWaitingItems() > 0) {
        	System.out.println("StartUpThread: exitingIcon: still items to save: "+net.ddp2p.common.data.SaverThreadsConstants.getNumberRunningSaverWaitingItems());
        	synchronized(stop_monitor){try {
        		stop_monitor.wait(2000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}}
        }
        D_Peer.stopSaver();
        D_Organization.stopSaver();
        D_Constituent.stopSaver();
        D_Neighborhood.stopSaver();
        D_Motion.stopSaver();
        D_Justification.stopSaver();
	}
	/**
	 * Stop servers and wait for the saving threads to stop, sleeping 2 seconds at a time.
	 */
	public static void clean_exit() {
        System.out.println("Exiting attempt...");
        stop_servers();
       	System.out.println("StartUpThread: exitingIcon: items to save: "+net.ddp2p.common.data.SaverThreadsConstants.getNumberRunningSaverWaitingItems());
       	clean_before_exit();
        System.out.println("Exiting...");
        System.exit(0); 
	}
	/**
	 * Extracts an object from this text (or null).
	 * Assume the object was encoded with ASN1B64, and included between separators
	 * @param text
	 * @return
	 */
	public static String extractStegoObject(String text, StegoStructure[] result) {
        String body = extractMessage(text);       
        if (body == null) {
        	if (_DEBUG) System.out.println("DD: importText: Extraction of body failed");
            return __("Extraction of body failed");
        }
        net.ddp2p.common.util.StegoStructure imported_object = interpreteASN1B64Object(body);
        if (imported_object == null) return __("Error decoding text object");
        if(result != null && result.length > 0) result[0] = imported_object;
        return null;
	}
	/**
	 * Extracts the message, decodes ASN1B64, and saves it
	 * @param strAddress
	 */
	public static void importText(String strAddress) {
		StegoStructure[] imported_objects = new StegoStructure[1];
		String error = extractStegoObject(strAddress, imported_objects);
        net.ddp2p.common.util.StegoStructure imported_object = imported_objects[0];
        if (imported_object == null) {
            if (_DEBUG) System.out.println("DD: importText: Decoding failed: "+error);
            return;
        }
        String interpretation = imported_object.getNiceDescription();
        if (0 == Application_GUI.ask(__("Do you wish to load?")+"\n"+interpretation, __("Do you wish to load?"), 0)) {
            try {
                imported_object.save();
                Application_GUI.info("Saving successful!", "Saving successful!");
            } catch (P2PDDSQLException e) {
                e.printStackTrace();
                Application_GUI.info("DD: importText: Failed to save: "+e.getLocalizedMessage(), "Failed to save!");
            }
        } 
	}
	/**
	 * Extracts a StegoStructure object from a String in base 64 for ASN1 encoding
	 * @param addressASN1B64
	 * @return
	 */
    public static StegoStructure interpreteASN1B64Object(String addressASN1B64) {
    	byte[] msg = null;
    	StegoStructure ss = null;
    	try {
    		if (DEBUG) System.out.println("DD: interprete: "+ addressASN1B64);
    		msg = Util.byteSignatureFromString(addressASN1B64);
    		Decoder dec = new Decoder(msg);
    		ss = DD.getStegoStructure(dec);
    		if (ss == null) {
    			if (_DEBUG) System.out.println("DD: interprete: Use default stego ... DD_Address");
    			ss = new DD_Address();
    		}
    		ss.setBytes(msg);
    		return ss;
    	} catch (Exception e) {
    		e.printStackTrace();
    		DD_SK dsk = new DD_SK();
    		if (_DEBUG) System.out.println("DD: interprete: Try default... DD_SK");
    		try {
    			dsk.setBytes(msg);
    			if (DEBUG) System.out.println("DD: interprete: got="+dsk);
    			return dsk;
    		} catch (Exception e2) {
    			e2.printStackTrace();
    			if (_DEBUG) System.out.println("DD: interprete: failed even for DD_SK = "+e2.getLocalizedMessage());
    		}
    	}
    	return null;
    }
	public static final String SAFE_TEXT_SEPARATOR = "\n";
	public static final int SAFE_TEXT_SIZE = 16;
	/**
	 * Used to generate the body of a text export for whatsup, skype
	 * @param bytes
	 * @return
	 */
	public static String getExportTextObjectBody(byte[] bytes) {
		return 
				__("Copy and paste the following text in the DDP2P object import menu! In fact you may copy the surrounding text as well (e.g., using Select All).") + "\n"
				+ DD.SAFE_TEXT_SEPARATOR 
				+ DD.SAFE_TEXT_MY_BODY_SEP
				+ DD.SAFE_TEXT_SEPARATOR 
				+ Util.B64Split(Util.stringSignatureFromByte(bytes), DD.SAFE_TEXT_SIZE, DD.SAFE_TEXT_SEPARATOR)
				+ DD.SAFE_TEXT_SEPARATOR 
				+ DD.SAFE_TEXT_MY_BODY_TRAILER_SEP 
				+ DD.SAFE_TEXT_SEPARATOR 
				+ DD.SAFE_TEXT_SEPARATOR 
				+ "\n"+__("Copy and paste the above text in the DDP2P object import menu! In fact you may copy the surrounding text as well (e.g., using Select All).");	
	}
	/**
	 * Builds the string that encapsulates a peer object.
	 * @param peer
	 * @return
	 */
	public static String getExportTextObjectBody(D_Peer peer) {
		DD_Address adr = new DD_Address(peer);
		String msgBody = DD.getExportTextObjectBody(adr.getBytes());
		return msgBody;
	}
	public static String getExportTextObjectBody(DD_SK d_sk) {
		String msgBody = DD.getExportTextObjectBody(d_sk.encode());
		return msgBody;
	}
	/**
	 * Text to be sent in the body of a message for exporting a  peer (gmail/whatsup)
	 * @param peer
	 * @return
	 */
	public static String getExportTextObjectTitle (D_Peer peer) {
		String slogan = peer.getSlogan_MyOrDefault();
		if (slogan == null) slogan = "";
		else slogan = "\""+slogan+"\"";
		return __("DDP2P: Safe Address of")+" \"" + peer.getName() + "\",  " + slogan + DD.SAFE_TEXT_MY_HEADER_SEP;
	}
	public static String getExportTextObjectTitle (D_Organization org) {
		return "DDP2P: Organization Address of \""+ org.getName();
	}
	public static String getExportTextObjectTitle (DD_Address peer) {
		String slogan = peer.getSlogan_MyOrDefault();
		if (slogan == null) slogan = "";
		else slogan = "\""+slogan+"\"";
		return __("DDP2P: Safe Address of")+" \"" + peer.getName() + "\",  " + slogan + DD.SAFE_TEXT_MY_HEADER_SEP;
	}
	public static String getExportTextObjectTitle (D_Motion crt_motion) {
		String testSubject = __("DDP2P: Motion Detail of")+" \""+ crt_motion.getTitleStrOrMy()
				+ "\" in \""+ crt_motion.getOrganization().getName()+"\" " + DD.SAFE_TEXT_MY_HEADER_SEP;
		return testSubject;
	}
	public static String getExportTextObjectTitle (D_Organization org, D_Constituent constituent) {
		String testSubject = __("DDP2P: Organization")+" \""+ org.getName()+"\" "+__("Profile of")+" \""
				+ constituent.getNameOrMy() + " " + DD.SAFE_TEXT_MY_HEADER_SEP;
		return testSubject;
	}
	public static String getExportTextObjectTitle(DD_DirectoryServer ds) {
		String testSubject = __("DDP2P: Directories")+" \""+ ds.getNiceDescription() + "\" " + DD.SAFE_TEXT_MY_HEADER_SEP;
		return testSubject;
	}
    /**
     * Break a message in a string to extract the base64 encoding.
     * we assume it is:<p>
     * (([Title][DD.SAFE_TEXT_MY_HEADER_SEP][DD.SAFE_TEXT_ANDROID_SUBJECT_SEP]...)|[DD.SAFE_TEXT_MY_BODY_SEP])<br>
     * Body<br>
     * DD.SAFE_TEXT_MY_BODY_TRAILER_SEP<br>
     * Instructions<p>
     * 
     * @param strAddress
     * @return
     */
	public static String extractMessage(String strAddress) {
		String addressASN1B64;
        try {
            if (strAddress == null) {
            	if (_DEBUG) System.out.println("DD: extractMessage: Address = null");
                return null;
            }
            if (DEBUG) System.out.println("DD: extractMessage: Address="+strAddress);
            String[] __chunks = strAddress.split(Pattern.quote(DD.SAFE_TEXT_MY_BODY_SEP));
            if (__chunks.length == 0 || __chunks[__chunks.length - 1] == null) {
            	if (_DEBUG) System.out.println("DD: extractMessage: My top Body chunk = null");
                return null;
            }
            if (__chunks.length > 1) { 
            	strAddress = __chunks[__chunks.length - 1];
                String [] ___chunks = strAddress.split(Pattern.quote(DD.SAFE_TEXT_MY_BODY_TRAILER_SEP));
                if (___chunks.length == 0 || ___chunks[0] == null) {
                	if (_DEBUG) System.out.println("DD: extractMessage: My first untrailled Body chunk = null");
                    return null;
                }
                strAddress = ___chunks[0];
            	addressASN1B64 = strAddress; 
            	addressASN1B64 = addressASN1B64.trim();
                if (DEBUG) System.out.println("DD: extractMessage ASN1 Body=["+__chunks.length+"]=" + addressASN1B64);
                if (DEBUG) System.out.println("DD: extractMessage ASN1 Body=[0]=" + __chunks[0]);
                return Util.B64Join(addressASN1B64);
            }
            if (DEBUG) System.out.println("DD: extractMessage: Address after 1 attempt="+strAddress);
            strAddress = __chunks[__chunks.length - 1];
            String[] chunks = strAddress.split(Pattern.quote(DD.SAFE_TEXT_MY_HEADER_SEP));
            if (chunks.length == 0 || chunks[chunks.length - 1] == null) {
            	if (_DEBUG) System.out.println("DD: extractMessage: My Body chunk = null");
                return null;
            }
            String body = chunks[chunks.length - 1];
            if (DEBUG) System.out.println("DD:extractMessage: Body="+body);
            String[] _chunks = body.split(Pattern.quote(DD.SAFE_TEXT_ANDROID_SUBJECT_SEP));
            if (_chunks.length == 0 || _chunks[_chunks.length - 1] == null) {
                if (_DEBUG) System.out.println("DD: extractMessage: Android Body chunk = null");
                return null;
            }
            addressASN1B64 = _chunks[_chunks.length - 1];
        	addressASN1B64 = addressASN1B64.trim();
            if (DEBUG) System.out.println("DD: extractMessage ASN1 Body final=" + addressASN1B64);
            return Util.B64Join(addressASN1B64);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
	public static D_Constituent getCrtConstituent(long oLID) {
		if (oLID <= 0)
			return null;
		long constituent_LID = -1;
		D_Constituent constituent = null;
		try {
			Identity crt_identity = Identity.getCurrentConstituentIdentity();
			if (crt_identity == null) {
			} else
				constituent_LID = net.ddp2p.common.config.Identity
						.getDefaultConstituentIDForOrg(oLID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		if (constituent_LID > 0) {
			constituent = D_Constituent.getConstByLID(constituent_LID, true,
					false);
		}
		return constituent;
	}
	/**
	 * Create a secret key (to be used as tester or mirror) and stores in in the given file (which is created),
	 * A similar file with ending .pk is created to store only the public key.
	 * 
	 * The keys are stored as ASN1 data encoded base 64.
	 * @param fileTrustedSK
	 * @return
	 */
	public static SK createTrustedSKFile(File fileTrustedSK) {
		Cipher trusted = Cipher.getCipher(Cipher.RSA, Cipher.SHA512, __("Trusted For Updates"));
		trusted.genKey(DD.RSA_BITS_TRUSTED_FOR_UPDATES);
		SK sk = trusted.getSK();
		PK pk = trusted.getPK();
		String _sk = Util.stringSignatureFromByte(sk.getEncoder().getBytes());
		String _pk = Util.stringSignatureFromByte(pk.getEncoder().getBytes());
		try {
			Util.storeStringInFile(fileTrustedSK, _sk
					+",\r\n" + 
					_pk);
			Util.storeStringInFile(fileTrustedSK.getAbsoluteFile()+".pk", Util.stringSignatureFromByte(pk.getEncoder().getBytes()));
			try {
				DD.setAppText(DD.TRUSTED_UPDATES_GID, DD.getAppText(DD.TRUSTED_UPDATES_GID)+DD.TRUSTED_UPDATES_GID_SEP+_pk);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sk;
	}
}
