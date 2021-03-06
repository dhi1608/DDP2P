package util;

import static util.Util._;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.Application_GUI;
import config.DD;
public class DD_DirectoryServer extends ASNObj implements StegoStructure {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	int version = 0;
	ArrayList<DirectoryAddress> dirs = new ArrayList<DirectoryAddress>();
	
	@Override
	public void save() {
		if(DEBUG) System.out.println("DD_DirServ:save");
		new DirectoriesSaverThread(this).start();
    	if(true){
    		Application_GUI.warning(_("Adding directories:")+" \n"+this.toString(), _("Import"));
    		//return;
    	}
	}

	@Override
	public void setBytes(byte[] asn1) throws ASN1DecoderFail {
		if(DEBUG)System.out.println("DD_DirectoryServer:decode "+asn1.length);
		decode(new Decoder(asn1));
		if(DEBUG)System.out.println("DD_DirectoryServer:decoded");
	}

	@Override
	public byte[] getBytes() {
		return encode();
	}

	@Override
	public String getNiceDescription() {
		return this.toString();
	}

	@Override
	public String getString() {
		return "DD_DirectoryServer:\n"+Util.concat(dirs, DD.APP_LISTING_DIRECTORIES_SEP,"");
	}

	public void add(DirectoryAddress l) {
		dirs.add(l);
		if(DEBUG) System.out.println("DD_DirServ:added:"+l);
	}
	
	public void add(String domain, int port){
		
		if (DEBUG) System.out.println("DD_DirServ:add: dom="+domain+" port="+port);
		DirectoryAddress d = new DirectoryAddress();
		d.domain = domain;
		d.tcp_port = port;
		dirs.add(d);
		if (DEBUG) System.out.println("DD_DirServ:added:"+d);
	}
	
	/**
	 * return a directory string
	 * compatible with what is stored in application table
	 */
	@Override
	public String toString() {
		return Util.concat(dirs, DD.APP_LISTING_DIRECTORIES_SEP,null);
	}
	public boolean parseAddress(DirectoryAddress[] directoryAddresses) {
    	for (DirectoryAddress l : directoryAddresses) {
    		//this.add(l.domain, l.tcp__port);
    		this.add(l);
    	}
		if(DEBUG) System.out.println("DD_DirServ:parsed:"+this.getString());
		return true;
	}

	/**
	 *  add the directories from a string (as one from "application" table)
	 *  For stego in text
	 */
	@Override
	public boolean parseAddress(String content) {
		if (content == null) return false;
    	String old_dirs[] = content.split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_SEP));
    	for (String l : old_dirs) {
    		String[] d = l.split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_ELEM_SEP));
    		if (d.length >= 2) {
    			try {
    				this.add(d[0], Integer.parseInt(d[1]));
    			} catch (Exception e) {
    				if (DEBUG) e.printStackTrace();
    				return false;
    			}
    		}
    	}
		if (DEBUG) System.out.println("DD_DirServ:parsed:"+this.getString());
		return true;
	}

	@Override
	public short getSignShort() {
		if(DEBUG) System.out.println("DD_DirServ:get_sign=:"+DD.STEGO_SIGN_DIRECTORY_SERVER);
		return DD.STEGO_SIGN_DIRECTORY_SERVER;
	}

	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(Encoder.getEncoder(dirs));
		return enc;
	}

	@Override
	public DD_DirectoryServer decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getInteger().intValue();
		dirs = d.getFirstObject(true).getSequenceOfAL(DirectoryAddress.getASN1Type(), new DirectoryAddress());
		return this;
	}

	
}