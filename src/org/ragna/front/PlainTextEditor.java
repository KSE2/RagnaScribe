/*
*  File: PlainTextEditor.java
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
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;

import org.ragna.core.ActionHandler;
import org.ragna.core.Global;
import org.ragna.core.PadArticle;
import org.ragna.core.PadDocument;
import org.ragna.core.PrintParameters;
import org.ragna.front.util.ButtonBarDialog;
import org.ragna.front.util.HtmlBrowserDialog;
import org.ragna.util.ActionManager.UnknownActionException;

import kse.utilclass.gui.AmpleTextArea;
import kse.utilclass.misc.UnixColor;

public class PlainTextEditor extends AmpleTextArea implements ArticleEditor {

	private static final Color BRIGHT_CARET_COLOR = UnixColor.LightGrey;
	private static final Color DARK_CARET_COLOR = UnixColor.Indigo;
	
	public PlainTextEditor (String name) {
		super (name);
		init();
	}

	public PlainTextEditor (String name, int rows, int columns) {
		super (name, rows, columns);
		init();
	}

	public PlainTextEditor (String name, String text) {
		super (name, text);
		init();
	}

	public PlainTextEditor (String name, String text, int rows, int columns) {
		super (name, text, rows, columns);
		init();
	}

	public PlainTextEditor (String name, Document doc) {
		super (name, doc);
		init();
	}

	public PlainTextEditor (String name, Document doc, String text, int rows, int columns) {
		super (name, doc, text, rows, columns);
		init();
	}

	@Override
	public void undo() {
		if (isEditable() && getUndoManager().canUndo()) {
			getUndoManager().undo();
		}
	}

	@Override
	public void redo() {
		if (isEditable() && getUndoManager().canRedo()) {
			getUndoManager().redo();
		}
	}

	private void init () {
        setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
        setExecutor(ActionHandler.get().getExecutor());
        
        defineMenuActions();
        defineKeys();
	}
	
	private void defineKeys() {
		Keymap map = getKeymap();
		
		// replace the default key binding from CTR-E to ALT-D 
		KeyStroke key = KeyStroke.getKeyStroke( KeyEvent.VK_E, InputEvent.CTRL_MASK );
		Action action = map.getAction(key);
		map.removeKeyStrokeBinding(key);
		KeyStroke key2 = KeyStroke.getKeyStroke( KeyEvent.VK_D, InputEvent.ALT_MASK );
		map.addActionForKeyStroke(key2, action);
		
		// replace the default key binding from CTR-F to ALT-T 
		key = KeyStroke.getKeyStroke( KeyEvent.VK_F, InputEvent.CTRL_MASK );
		action = map.getAction(key);
		map.removeKeyStrokeBinding(key);
		key2 = KeyStroke.getKeyStroke( KeyEvent.VK_T, InputEvent.ALT_MASK );
		map.addActionForKeyStroke(key2, action);
	}

	private void defineMenuActions() {
        // define a separate PRINT action to branch into DispayManager print method
        Action action = new  AbstractAction(getIntl(ACTION_PRINT)) {
			@Override
			public void actionPerformed(ActionEvent e) {
				PadDocument document = DisplayManager.get().getSelectedDocument();
				PadArticle article = document == null ? null : document.getSelectedArticle();
				if (article != null) {
					PrintParameters param = new PrintParameters(document, article);
					param.font = param.font.deriveFont((float)10);
					DisplayManager.get().getSelectedDisplay().printArticles(param);
				}
			}
        };
        action.putValue(Action.ACTION_COMMAND_KEY, ACTION_PRINT);
        
        // replace the default popup-menu action PRINT
        List<Action> list = getMenuActions();
        for (Action a : list) {
        	if (ACTION_PRINT.equals(a.getValue(Action.ACTION_COMMAND_KEY))) {
        		list.set(list.indexOf(a), action);
        	}
        }
        
        // add Font chooser command
        try {
			action = ActionHandler.get().getAction(ActionHandler.ActionNames.EDIT_CHOOSE_FONT);
			list.add(action);
		} catch (UnknownActionException e1) {
		}
	}

	@Override
	public JTextComponent getView() {
		return this;
	}

	@Override
	public void setBackground (Color bgd) {
		super.setBackground(bgd);
		
		int bright = bgd.getRed() + bgd.getGreen() + bgd.getBlue();
		Color cc = bright > 320 ? DARK_CARET_COLOR : BRIGHT_CARET_COLOR;
		setCaretColor(cc);
	}

	@Override
	public void insertText(String text, int position) {
		super.insert(text, position);
		
	}

	@Override
	public void setReadOnly (boolean readOnly) {
		setEditable(!readOnly);
	}

	@Override
	public boolean isReadOnly () {
		return !isEditable();
	}

	@Override
	public JMenu getJMenu() {
		JMenu menu = super.getJMenu();
		ActionHandler aha = ActionHandler.get();
		JMenuItem item;
		
		// "Color" entry
		String name = Global.res.getCommand("action.edit.color");
		JMenu me = new JMenu(name);
		menu.add(me);
		try {
			Action action = aha.getAction(ActionHandler.ActionNames.EDIT_COLOR_BGD);
			item = me.add(action);
			item.setIcon(null);
			action = aha.getAction(ActionHandler.ActionNames.EDIT_COLOR_FGR);
			item = me.add(action);
			item.setIcon(null);
		} catch (UnknownActionException e) {
			e.printStackTrace();
		}

		// "Text" entry (import/export)
		name = Global.res.getCommand("action.edit.text");
		me = new JMenu(name);
		menu.add(me);
		
		try {
			Action action = aha.getAction(ActionHandler.ActionNames.EDIT_INSERT_FILE);
			item = me.add(action);
			item.setIcon(null);
			action = aha.getAction(ActionHandler.ActionNames.EDIT_EXPORT_FILE);
			item = me.add(action);
			item.setIcon(null);
		} catch (UnknownActionException e) {
			e.printStackTrace();
		}
		
		// HELP command
		menu.addSeparator();
		Action action = new Help_Action();
		item = menu.add(action);
		item.setIcon(null);
		
		return menu;
	}

	private class Help_Action extends AbstractAction {
		
		public Help_Action () {
			super(getIntl(ACTION_HELP));
			putValue(Action.ACTION_COMMAND_KEY, ACTION_HELP);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (ButtonBarDialog.isDialogActive(ACTION_HELP)) return;
			HtmlBrowserDialog dlg = new HtmlBrowserDialog();
			dlg.setBoundsToken("editor-help-win", true);
			dlg.markSingleton(ACTION_HELP);
			
			try {
				String textName = Global.res.getCommand("html.file.help.editor");
				String hs = "#system/" + textName;
				String text = Global.res.getResourceText(hs, "UTF-8");
				dlg.setText(text);
			} catch (IOException e1) {
				GUIService.failureMessage(null, e1);
			}
			dlg.show();
		}
		
	}

	@Override
	public void setTextSelection (int start, int end) {
		grabFocus();
		select(start, end);
	}
	
}
