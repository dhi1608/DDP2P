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
package net.ddp2p.java.db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DB_Implementation;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public
class DB_Implementation_JDBC_SQLite implements DB_Implementation {
    private static final boolean DEBUG = false;
    File file;
    /**
     * true to keep it open between different queries.
     * false to close it after each query.
     */
	private boolean conn_open = true;
	Connection conn;
	private String filename;
	static boolean loaded = loadClass();
	static boolean loadClass(){
		try {
			Object c = Class.forName("org.sqlite.JDBC");
			if (c != null) return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}
    public synchronized ArrayList<ArrayList<Object>> select(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	try {
			tmp_open(true);
		} catch (Exception e) {
			throw new P2PDDSQLException(e);
		}
    	ArrayList<ArrayList<Object>> result = _select(sql, params, DEBUG);
    	try {
			tmp_dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return result;
    }
    /**
     * For already existing connections in db
     * @param sql
     * @param params
     * @param DEBUG
     * @return
     * @throws com.almworks.sqlite4java.SQLiteException
     */
    public synchronized void _query(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	PreparedStatement st;
    	try {
    		st = conn.prepareStatement(sql);
    		if(DEBUG) System.out.println("sqlite_jdbc:query:sql: "+sql+" length="+params.length);
    		try {
    			for(int k=0; k<params.length; k++){
    				st.setString(k+1, params[k]);
    				if(DEBUG) System.out.println("sqlite_jdbc:query:bind: "+params[k]);
    			}
    		} finally {st.close();}
    	} catch (SQLException e) {
    		e.printStackTrace();
    		throw new P2PDDSQLException(e);
    	}
    	if(DEBUG) System.out.println("DBInterface_jdbc:query:results#=ok");
    	return;
    }
    public synchronized ArrayList<ArrayList<Object>> _select(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	ArrayList<ArrayList<Object>> result = new ArrayList<ArrayList<Object>>();
    	PreparedStatement st;
    	try {
    		st = conn.prepareStatement(sql);
    		if(DEBUG) System.out.println("sqlite:select:sql: "+sql+" length="+params.length);
    		try {
    			for(int k=0; k<params.length; k++){
    				st.setString(k+1, params[k]);
    				if(DEBUG) System.out.println("sqlite:select:bind: "+params[k]);
    			}
    			ResultSet rs = st.executeQuery();
    			ResultSetMetaData md = rs.getMetaData();
    			int cols = md.getColumnCount(); 
    			while (rs.next()) {
    				ArrayList<Object> cresult = new ArrayList<Object>();
    				for(int j=1; j<=cols; j++){
    					cresult.add(rs.getString(j));
    				}
    				result.add(cresult);
    				if (result.size()>DBInterface.MAX_SELECT) {
    					Application_GUI.warning("Found more results than: "+DBInterface.MAX_SELECT, "JDBC select");
    					break;
    				}
    			}
    		} finally {st.close();}
    	} catch (SQLException e) {
    		e.printStackTrace();
    		throw new P2PDDSQLException(e);
    	}
    	if(DEBUG) System.out.println("DBInterface:select:results#="+result.size());
    	return result;
    }
    public synchronized long insert(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	long result;
    	try {
			tmp_open(true);
			result = _insert(sql, params, DEBUG);
			tmp_dispose();
		} catch (Exception e) {
			System.out.println("DB_Impl:insert:"+sql);
   			for (int k=0; k<params.length; k++) {
				System.out.println("sqlite:insert:bind: "+Util.nullDiscrim(params[k]));
			}
			e.printStackTrace();
			throw new P2PDDSQLException(e);
		}
    	return result;
    }
    public synchronized long _insert(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	return _nosyncinsert(sql, params, DEBUG, true);
    }
    public synchronized long _nosyncinsert(String sql, String[] params, boolean DEBUG, boolean return_result) throws P2PDDSQLException{
    	long result = -1;
    	PreparedStatement st;
    	try {
    		if(return_result)
    			st = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    		else
    			st = conn.prepareStatement(sql, Statement.NO_GENERATED_KEYS);
    		if(DEBUG) System.out.println("sqlite:insert:sql: "+sql);
    		try {
    			for(int k=0; k<params.length; k++){
    				st.setString(k+1, params[k]);
    				if(DEBUG) System.out.println("sqlite:insert:bind: "+Util.nullDiscrim(params[k]));
    			}
    			st.execute(); 
    			if(return_result) {
	    			ResultSet keys = st.getGeneratedKeys();
	    			result = keys.getLong("last_insert_rowid()");//db.getLastInsertId();
    			}
    			if(DEBUG) System.out.println("sqlite:insert:result: "+result);
    		}
    		catch(SQLException e){
    			e.printStackTrace();
    			System.err.println("sqlite:insert:sql: "+sql);
    			for(int k=0; k<params.length; k++){
    				System.err.println("sqlite:insert:bind: "+Util.nullDiscrim(params[k]));
    			}
    			throw e;
    		}finally {st.close();}
    	} catch (SQLException e1) {
    		throw new P2PDDSQLException(e1);
    	}
    	return result;
    }
	public synchronized void update(String sql, String[] params, boolean dbg) throws P2PDDSQLException{
    	try {
			tmp_open(true);
			_update(sql, params, dbg);    	
			tmp_dispose();
		} catch (Exception e) {
			e.printStackTrace();
			throw new P2PDDSQLException(e);
		}
	}	
    public synchronized void _update(String sql, String[] params, boolean dbg) throws P2PDDSQLException{
    	PreparedStatement st;
    	try {
    		st = conn.prepareStatement(sql);
    		if(dbg) System.out.println("sqlite:update:sql: "+sql);
    		try {
    			for(int k=0; k<params.length; k++){
    				st.setString(k+1, params[k]);
    				if(dbg) System.out.println("sqlite:update:bind: "+params[k]);
    			}
    			st.execute();
    		} finally {st.close();}
    	} catch (SQLException e) {
			throw new P2PDDSQLException(e);
		}
    }
    public synchronized void delete(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	try {
    		tmp_open(true);
    		PreparedStatement st = conn.prepareStatement(sql);
    		st.setQueryTimeout(30);
    		if(DEBUG) System.out.println("sqlite:delete:sql: "+sql);
    		try {
    			for(int k=0; k<params.length; k++){
    				st.setString(k+1, params[k]);
    				if(DEBUG) System.out.println("sqlite:delete:bind: "+params[k]);
    			}
    			st.execute();
    		} finally {st.close();}
    		tmp_dispose();
    	} catch (SQLException e) {
			throw new P2PDDSQLException(e);
    	}
    }
	@Override
	public void close() throws P2PDDSQLException {
		if (conn_open) {
			try {
				if (conn != null) conn.close();
				conn = null;
			} catch (SQLException e) {
				throw new P2PDDSQLException(e);
			}
		}
	}
	private void tmp_dispose() throws P2PDDSQLException {
		if (! conn_open) {
			try {
				conn.close();
			} catch (SQLException e) {
				throw new P2PDDSQLException(e);
			}
		}
	}
	private void tmp_open(boolean b) throws P2PDDSQLException {
		if (! conn_open) {
			try {
				conn = DriverManager.getConnection("jdbc:sqlite:"+filename);
			} catch (SQLException e) {
				throw new P2PDDSQLException(e);
			}
		}
	}
	public void open_and_keep(boolean b) throws P2PDDSQLException {
		tmp_open(b);
	}
	public void dispose_and_keep() throws P2PDDSQLException {
		tmp_dispose();
	}
	@Override
	public void open(String _filename) throws P2PDDSQLException {
		try {
			filename = _filename;
			file = new File(filename);
			Logger logger = Logger.getLogger("com.almworks.sqlite4java");
			logger.setLevel(Level.SEVERE);
			conn = DriverManager.getConnection("jdbc:sqlite:"+filename);
			if (! conn_open)
				conn.close();
		} catch (SQLException e) {
			throw new P2PDDSQLException(e);
		}
	}
	public void _insert(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException {
    	String sql = DBInterface.makeInsertOrIgnoreSQL(table, fields, params);
    	_nosyncinsert(sql, params, dbg, false);
	}
	@Override
	public boolean hasParamInsert() {
		return false;
	}
	@Override
	public long tryInsert(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException {
		throw new RuntimeException("");
	}
	@Override
	public boolean hasParamDelete() {
		return false;
	}
	@Override
	public void tryDelete(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException {
		throw new RuntimeException("");
	}
	@Override
	public boolean hasParamUpdate() {
		return false;
	}
	@Override
	public void tryUpdate(String table, String[] fields, String[] selector,
			String[] params, boolean dbg) throws P2PDDSQLException {
		throw new RuntimeException("");
	}
	@Override
	public String getName() {
		return filename;
	}
}
