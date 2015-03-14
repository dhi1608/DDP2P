package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

class Database_Creation {
	String _create = "CREATE TABLE peer ('peer_ID' INTEGER PRIMARY KEY NOT NULL,"
		    +"'global_peer_ID' TEXT UNIQUE ON CONFLICT FAIL,"
		    +"'global_peer_ID_hash' TEXT UNIQUE ON CONFLICT FAIL,"
		    +"'GID_key' INTEGER DEFAULT NULL UNIQUE ON CONFLICT FAIL,"
		    +"'name' TEXT,"
		    +"'broadcastable' INTEGER DEFAULT (0), "
		    +"'slogan' TEXT,"
		    +"'used' INTEGER DEFAULT (0)," 
		    +"'blocked' INTEGER DEFAULT (0)," 
		    +"'hash_alg' TEXT,"
		    +"'version' TEXT,"
		    +"'signature' BLOB,"
		    +"'picture' BLOB,"
		    +"'plugin_info' TEXT,"
		    +"'exp_avg' REAL, "
		    +"'experience' INTEGER," 
		    +"'filtered' INTEGER DEFAULT (0),"
		    +"'last_sync_date' TEXT,"
		    +"'last_reset' TEXT, "
		    +"'emails' TEXT,"
		    +"'email_verified' TEXT,"
		    +"'name_verified' TEXT,"
		    +"'category' TEXT,"
		    +"'phones' TEXT,"
		    +"'urls' TEXT,"
		    +"'plugins_msg' BLOB," 
		    +"'revoked' INTEGER DEFAULT (0), "
		    +"'revokation_instructions' TEXT,"
		    +"'revokation_replacement_GIDhash' TEXT,"
		    +"'hidden' INTEGER DEFAULT (0),"
		    +"'creation_date' TEXT, "
		    +"'arrival_date' TIMESTAMP, "
		    +"'first_provider_peer' INTEGER DEFAULT NULL,"
		    +"'preferences_date' TEXT,"
		    +"FOREIGN KEY (first_provider_peer) REFERENCES peer(peer_ID),"
		    +"FOREIGN KEY (GID_key) REFERENCES public_keys(pk_ID)"
		+"); "
		+ "CREATE TABLE 'application' ( "
+ "'field' TEXT PRIMARY KEY NOT NULL, "
+ "'value' BLOB "
+ "); "
+ "CREATE TABLE 'key' ( "
+ "'key_ID' INTEGER PRIMARY KEY, "
+ "'public_key' TEXT  UNIQUE ON CONFLICT FAIL, "
+ "'secret_key' TEXT  UNIQUE ON CONFLICT FAIL, "
+ "'ID_hash' TEXT  UNIQUE ON CONFLICT FAIL, "
+ "'name' TEXT, "
+ "'preference_date' TEXT, "
+ "'creation_date' TEXT, "
+ "'type' TEXT, "
+ "'hide' TEXT "
+ "); "
+ "CREATE TABLE public_keys ("
+ " pk_ID INTEGER PRIMARY KEY, "
+ " public_key TEXT UNIQUE ON CONFLICT FAIL, "
+ " pk_hash TEXT UNIQUE ON CONFLICT FAIL "
+ ");"
+ "CREATE TABLE peer_instance ( "
+ "'peer_instance_ID' INTEGER PRIMARY KEY," 
+ "peer_ID INTEGER NOT NULL," 
+ "peer_instance TEXT," 
+ "branch TEXT," 
+ "version TEXT," 
+ "plugin_info TEXT," 
+ "last_sync_date TEXT,"  
+ "last_reset TEXT,  " 
+ "last_contact_date TEXT," 
+ "objects_synchronized INTEGER," 
+ "signature_date TEXT, " 
+ "signature TEXT, " 
+ "created_locally TEXT, " 
+ "UNIQUE (peer_ID, peer_instance) ON CONFLICT FAIL, " 
+ "FOREIGN KEY(peer_ID) REFERENCES peer(peer_ID) " 
+ "); " 
+ "CREATE TABLE peer_address ( " 
    + "peer_address_ID INTEGER PRIMARY KEY, " 
    + "peer_ID INTEGER," 
    + "instance INTEGER," 
    + "type TEXT, " 
    + "domain TEXT," 
    + "tcp_port INTEGER," 
    + "udp_port INTEGER," 
    + "address TEXT, " 
    + "certified TEXT DEFAULT (0)," 
    + "priority INTEGER DEFAULT (0), " 
    + "my_last_connection TEXT," 
    + "arrival_date TIMESTAMP, " 
    + "UNIQUE (peer_ID , address, type) ON CONFLICT FAIL," 
    + "FOREIGN KEY(instance) REFERENCES peer_instance(peer_instance_ID)," 
    + "FOREIGN KEY(peer_ID) REFERENCES peer(peer_ID)" 
+ ");" 
+ "CREATE TABLE peer_my_data ( " 
+ "peer_ID INTEGER UNIQUE NOT NULL," 
+ "name TEXT," 
+ "slogan TEXT," 
+ "broadcastable TEXT," 
+ "picture BLOB," 
+ "my_topic TEXT," 
+ "FOREIGN KEY(peer_ID) REFERENCES peer(peer_ID)" 
+ ");"; // take create script from resources

}