/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 
		Author: Khalid Alhamed
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

/**

 * @(#)tableJButton.java
 *
 *
 * @author 
 * @version 1.00 2012/12/23
 */
package widgets.updates; 
 
import javax.swing.JButton;

public class TableJButton extends JButton {
    public int rowNo=-1;
    public TableJButton() {
    	super();
    }
    
     public TableJButton(String label) {
     	super(label);
    }
    
    public TableJButton(String label, int row) {
     	super(label);
     	rowNo=row;
    }
}