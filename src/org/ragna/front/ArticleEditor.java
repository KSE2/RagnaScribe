/*
*  File: ArticleEditor.java
* 
*  Project Ragna Scribe
*  @author Wolfgang Keller
*  Created 
* 
*  Copyright (c) 2023 by Wolfgang Keller, Munich, Germany
* 
This program is not public domain software but copyright protected to the 
author(s) stated above. However, you can use, redistribute and/or modify it 
under the terms of the The GNU General Public License (GPL) as published by
the Free Software Foundation, version 2.0 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the License along with this program; if not,
write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, 
Boston, MA 02111-1307, USA, or go to http://www.gnu.org/copyleft/gpl.html.
*/

package org.ragna.front;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.swing.Action;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;

import kse.utilclass.gui.MenuActivist;

/** Abstraction layer for Pad-Article editors.
 * 
 */
public interface ArticleEditor extends MenuActivist {

	static final String ACTION_COPY_NAME = "menu.edit.copy";
	static final String ACTION_CUT_NAME = "menu.edit.cut";
	static final String ACTION_PASTE_NAME = "menu.edit.paste";
	static final String ACTION_DELETE_NAME = "menu.edit.delete";
	static final String ACTION_UNDO_NAME = "menu.edit.undo";
	static final String ACTION_REDO_NAME = "menu.edit.redo";
	static final String ACTION_PRINT = "menu.edit.print";
	static final String ACTION_HELP = "menu.edit.help";
	

	/** Returns a list with edit actions available in this editor for the 
	 * purpose of creating a menu. The elements of the list are stable during 
	 * a program session, i.e. they can be persistently modified.
	 * <p>The list contains at least the actions of the standard action names 
	 * defined in this interface with Undo and Redo at the leading places.
	 * 
	 * @return {@code List<Action>}
	 */
	@Override
	List<Action> getMenuActions ();
	
	
	/** The text area component of this editor.
	 * 
	 * @return {@code JTextComponent}
	 */
	JTextComponent getView ();

	void setDocument (Document document);

	/** Returns the document of the editor component.
	 * 
	 * @return {@code Document}
	 */
	Document getDocument ();

	/** The currently user-selected text segment.
	 *  
	 * @return String
	 */
	String getSelectedText ();

	/** The current input caret position relative to the beginning of the 
	 * document.
	 * 
	 * @return int
	 */
	int getCaretPosition ();
	
	/** Undoes the latest edit action, which may be cumulative, in this editor
	 * or does nothing if there is no such action noted or this editor is not
	 * editable.
	 */
	void undo ();
	
	/** Redoes the latest undone edit action, which may be cumulative, in this 
	 * editor or does nothing if there is no such action noted or this editor 
	 * is not editable.
	 */
	void redo ();

	/** Sets the input caret position of this editor if feasible.
	 * 
	 * @param cpo int new care position
	 */
	void setCaretPosition (int cpo);
	
	
	void insertText (String text, int position);
	
    /** Returns the {@code UndoManager} of the text editor. In our context the
     * undo-manager is bound to the text document.
     * 
     * @return {@code UndoManager}
     */
    UndoManager getUndoManager ();
	   
    /** Sets the READ-ONLY mode of this editor.
     * 
     * @param readOnly
     */
    void setReadOnly (boolean readOnly);
    
    /** Whether this editor is set to READ-ONLY mode.
     * 
     * @return boolean true = READ-ONLY is set, false = READ-ONLY is not set
     */
    boolean isReadOnly ();
    
    /** Sets the Font to display this editor's text content.
     *  
     * @param font {@code Font}
     */
    void setFont (Font font);

    /** Sets the background color of this editor's text area.
     * 
     * @param color Color
     */
    void setBackground (Color color);
    
    /** Sets the foreground color of this editor's text area.
     * 
     * @param color Color
     */
    void setForeground (Color color);
    
    Font getFont ();


    /** Sets the current text selection in this editor. The values are 
     * corrected to suit the document length.
     * 
     * @param start int
     * @param end int
     */
	void setTextSelection (int start, int end);
    
    
}
