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
 package net.ddp2p.widgets.components;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Language;
import net.ddp2p.common.data.DDTranslation;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Translations;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.DBSelector;
import net.ddp2p.common.util.Util;
import static net.ddp2p.common.util.Util.__;
/**
 *  The label contains the translations of the 
 *  preferred languages specified in DDTranslation.preferred_languages.
 *  Those are originally extracted from the default identity in the identities table.
 *  
 *  Newly edited values will be saved with DDTranslation,authorship_lang.flavor
 * @author Marius Silaghi
 *
 */
@SuppressWarnings("serial")
public class TranslatedLabel extends JComboBox implements ActionListener, MouseListener , DBListener {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	String dd_text;
	Translation t_text;
	Language lang;
	JTextFieldIconed editor;
	static Language default_language=new Language("en","EU");
	static String block_tip;
	public TranslatedLabel(String _text) {
		block_tip = __("Double-click to edit, or use handle on the right to change!");
		init_widget(_text, default_language);
	}
	public TranslatedLabel(String _text, Language lang) {
		init_widget(_text, lang);
		setBorder(BorderFactory.createEmptyBorder()); 
	}
	@SuppressWarnings("unchecked")
	void init_widget(String _text, Language lang){
		editor = new JTextFieldIconed();
		editor.addMouseListener(this);
		setEditor(editor);
		setEditable(true);
		setFocusable(false);
		setPopupVisible(false);
		setRenderer(new LabelRenderer());
		setText(_text, lang);
		addActionListener(this);
		addMouseListener(this); 
    	if(!this.isFocusable())
    		setToolTipText(block_tip);
    	if(Application.getDB() != null) {
    		Application.getDB().addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.translation.TNAME)),
    				DBSelector.getHashTable(net.ddp2p.common.table.translation.TNAME, net.ddp2p.common.table.translation.value, _text));
    	}
	}
	public void showEditorIcon(boolean show) {
		editor.showIcon = show;
	}
	public void mouseClicked(MouseEvent e){
		int clicks=e.getClickCount();
		if(clicks>=2){
				setFocusable(true);
				this.requestFocusInWindow();
				editor.selectAll();
		}
	}
	public void mouseEntered(MouseEvent e){
	}
	public void mouseExited(MouseEvent e){
	}
	public void mousePressed(MouseEvent e){
	}
	public void mouseReleased(MouseEvent e){
	}
	public void actionPerformed(ActionEvent evt) {
        super.actionPerformed(evt);
       onEdited("comboBoxEdited".equals(evt.getActionCommand()),
        		"comboBoxEdited".equals(evt.getActionCommand()));
	}
	void onEdited(boolean edited, boolean changed){
        int idx = getSelectedIndex();
		if(DEBUG) System.err.println("TranslatedLabel:onEdited:Index="+idx+" edited="+edited);
        if(idx != -1)  {
        	Translation crt = (Translation)getSelectedItem();
        	setToolTipText(crt.getTip());
        	if(!this.isFocusable())
        		setToolTipText(block_tip);
        }else{
        	setToolTipText(Translation.getTip(DDTranslation.authorship_lang.toString()));
        	if (edited) {
        		setTranslation();
        	} 	
        }
		if(DEBUG) System.err.println("TranslatedLabel:onEdited:Done");
	}
	String getOriginalText(){
		return dd_text;
	}
	void setTranslation(){
		String translation = getSelectedItem().toString();
		if(DEBUG) System.err.println("TranslatedLabel:setTranslation:Transl="+translation);
		if ((DDTranslation.constituentID<=0) || (DDTranslation.organizationID<=0))
			Application_GUI.warning(__("Cannot save translation since organizationID and constituentID are not set:")+
					" ("+DDTranslation.organizationID+","+DDTranslation.constituentID+")",
					__("Translations not synchronizable"));
		try{
			ArrayList<ArrayList<Object>> sel =
				DDTranslation.db.select("select ROWID from "+net.ddp2p.common.table.translation.TNAME+" where "+net.ddp2p.common.table.translation.submitter_ID+"==? " +
					" and "+net.ddp2p.common.table.translation.value+" = ? and "+net.ddp2p.common.table.translation.translation_lang+" = ? and "+net.ddp2p.common.table.translation.translation_flavor+" = ?;",
					new String[]{DDTranslation.constituentID+"",
					getOriginalText(),
					DDTranslation.authorship_lang.lang,
					DDTranslation.authorship_lang.flavor}, DEBUG);
			for(int j = 0; j < sel.size(); j++) {
				DDTranslation.db.deleteNoSync(net.ddp2p.common.table.translation.TNAME, new String[]{"ROWID"},
						new String[]{Util.sval(sel.get(j).get(0), "")}, DEBUG);
			}
			D_Translations tr = new D_Translations();
			tr.value = getOriginalText();
			tr.value_lang = lang.lang;
			tr.translation = translation;
			tr.translation_lang = DDTranslation.authorship_lang.lang;
			tr.translation_charset = DDTranslation.authorship_charset;
			tr.translation_flavor = DDTranslation.authorship_lang.flavor;
			tr.organization_ID = Util.getStringID(DDTranslation.organizationID);
			tr.submitter_ID = Util.getStringID(DDTranslation.constituentID);
			tr.global_organization_ID = D_Organization.getGIDbyLID(DDTranslation.organizationID);
			tr.global_constituent_ID = D_Constituent.getGIDFromLID(DDTranslation.constituentID);
			tr.global_translation_ID = tr.make_ID();
			tr.creation_date = Util.CalendargetInstance();
			String gtID = tr.global_translation_ID; 
			long rowID = DDTranslation.db.insert(net.ddp2p.common.table.translation.TNAME,
					new String[]{net.ddp2p.common.table.translation.global_translation_ID, net.ddp2p.common.table.translation.value,net.ddp2p.common.table.translation.value_lang,
					net.ddp2p.common.table.translation.value_ctx,net.ddp2p.common.table.translation.translation,net.ddp2p.common.table.translation.translation_lang,
					net.ddp2p.common.table.translation.translation_charset, net.ddp2p.common.table.translation.translation_flavor, 
					net.ddp2p.common.table.translation.organization_ID, net.ddp2p.common.table.translation.submitter_ID, net.ddp2p.common.table.translation.signature},
					new String[]{gtID, getOriginalText(), lang.lang,
					"field label", translation, DDTranslation.authorship_lang.lang,
					DDTranslation.authorship_charset, DDTranslation.authorship_lang.flavor,
					DDTranslation.organizationID+"",DDTranslation.constituentID+"","NULL"},DEBUG);
		}catch(Exception ev){
    		ev.printStackTrace();
    		return;
		}		
	}
	void setSelectedItem(Translation tr){
		super.setSelectedItem(tr);
		setToolTipText(tr.getTip());
    	if(!this.isFocusable())
    		setToolTipText("Double-click to edit, or use right arrow to selectE");
	}
	@SuppressWarnings("unchecked")
	void setText(String _text, Language lang) {
		dd_text = _text;
		this.lang = lang;
		t_text = new Translation(dd_text,lang.lang,lang.flavor);
		ArrayList<ArrayList<Object>> sel = DDTranslation.translates(dd_text, lang);
		removeAllItems();
		if((sel == null) || (sel.size() == 0)) {
			if(DEBUG)System.err.println("TranslatedLabel:setText:Empty Selection for: "+dd_text);
			addItem(t_text);
			setSelectedItem(t_text);
		}else{
			Translation selected = null;
			for(int k=0; k<sel.size(); k++) {
				String item=Util.sval(sel.get(k).get(0), null);
				String language=Util.sval(sel.get(k).get(1), null);
				String flavor=Util.sval(sel.get(k).get(2), null);
				if(DEBUG)System.err.println("TranslatedLabel:setText:Selection for: "+item);
				Translation entry = new Translation(item, language, flavor);
				if(selected == null) selected = entry;
				if(DEBUG)System.err.println("TranslatedLabel:setText:Selection now: "+selected);
				addItem(entry);
			}
			addItem(t_text);
			setSelectedItem(selected);
			if(DEBUG)System.err.println("TranslatedLabel:setText:Selection set: "+selected);
		}
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String,DBInfo> info) {
		setText(dd_text, lang);
	}
}
