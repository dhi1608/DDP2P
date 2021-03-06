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
package net.ddp2p.common.plugin_data;
import java.util.ArrayList;
import java.util.Hashtable;
public
class PluginMenus{
	public static final String GID = "GID";
	public static final String NAME = "NAME";
	public static final String ALIAS = "ALIAS";
	public static final String SLOGAN = "SLOGAN";
	public static final int COLUMN_NAME = 0;
	public static final int COLUMN_SERVING = 1;
	public static final int COLUMN_CONNECTION = 2;
	public static final int COLUMN_SLOGAN = 3;
	public static final int COLUMN_MYSELF = -1;
	public static final int COLUMN_ALL = -2;
	public static final String ROW_MYPEER = "MYSELF";
	public static final String ROW_OTHERS_ABSENT = "OTHERS_ABSENT";
	public static final String ROW_OTHERS_PRESENT = "OTHERS_PRESENT";
	String plugin_ID;
	String plugin_name;
	public ArrayList<Object> plugin_menu_action = new ArrayList<Object>(); 
	public ArrayList<Object> plugin_menu_item = new ArrayList<Object>(); 
	public PluginMenus(String plugin_ID, String plugin_name){
		this.plugin_ID = plugin_ID;
		this.plugin_name = plugin_name;
	}
	void addMenu(int col, 
			Object plugin_menu_item){
		this.plugin_menu_item.add(plugin_menu_item);		
	}
	void addAction(int col, 
			Object plugin_menu_action){
		this.plugin_menu_action.add(plugin_menu_action);
	}
	public PluginMenus clone() {
		PluginMenus result = new PluginMenus(this.plugin_ID, this.plugin_name);
		for(Object pa: plugin_menu_action) result.plugin_menu_action.add(pa); 
		for(Object ma: plugin_menu_item) result.plugin_menu_item.add(ma); 
		return result;
	}
	/**
	 * Merge into mn
	 * @param mn : input and output
	 * @param mn2
	 * @param pluginGID : if not null, then only add menus from this plugin
	 * @return
	 */
	public static Hashtable<String, PluginMenus> merge(
			Hashtable<String, PluginMenus> mn,
			Hashtable<String, PluginMenus> mn2, String pluginGID) {
	   	for(String plug_gid : mn2.keySet()) {
	   		if ((pluginGID != null) && (!pluginGID.equals(plug_gid))) continue;
	   		PluginMenus pm2 = mn2.get(plug_gid);
	   		PluginMenus pm = mn.get(plug_gid);
	   		if(pm == null) {
	   			mn.put(plug_gid, pm2);
	   			continue;
	   		}
	   		PluginMenus new_pm = pm.clone();
     		for(Object pa: pm2.plugin_menu_action){ 
     			if(!new_pm.plugin_menu_action.contains(pa)) new_pm.plugin_menu_action.add(pa);
    		for(Object ma: pm2.plugin_menu_item) 
    			if(!new_pm.plugin_menu_item.contains(ma)) new_pm.plugin_menu_item.add(ma);
    		}
     		mn.put(plug_gid, new_pm);
	   	}
		return mn;
	}
}
