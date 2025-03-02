/*
*  File: TextSearchPanel.java
* 
*  Project Ragna Scribe
*  @author Wolfgang Keller
*  Created 
* 
*  Copyright (c) 2024 by Wolfgang Keller, Munich, Germany
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

import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.ragna.core.Global;
import org.ragna.front.util.ResourceLoader;
import org.ragna.util.PersistentOptions;

import kse.utilclass.gui.VerticalFlowLayout;
import kse.utilclass.misc.Log;
import kse.utilclass.misc.Util;

public class TextSearchPanel extends JPanel implements AncestorListener {
	private static final int MAX_LIST_VALUES = 12;
	
	private ResourceLoader res = Global.res;
	private JComboBox<String> textFld;
	private JCheckBox sensitiveChk;
	private JCheckBox wholeWordChk;
	private JRadioButton scopeDoc, scopeArt, scopeAll;
	private boolean articleEnabled = true;
	
	public TextSearchPanel () {
		init();
	}

	private void init() {
		setLayout(new VerticalFlowLayout(6));
		
		JLabel label = new JLabel(res.getDisplay("find.text"));
		add(label);
		
		textFld = new JComboBox<>();
		textFld.setEditable(true);
		add(textFld);

		JPanel blockPanel = new JPanel(new VerticalFlowLayout(0));
		
		/// "case sensitive" and "whole word" checkboxes
		sensitiveChk = new JCheckBox(res.getDisplay("find.checkCS"));
		sensitiveChk.setIconTextGap(6);
		wholeWordChk = new JCheckBox(res.getDisplay("find.checkWD"));
		wholeWordChk.setIconTextGap(6);

		JPanel panel = new JPanel();
		panel.add(sensitiveChk);
		panel.add(wholeWordChk);
//		panel.setBackground(UnixColor.Thistle);
		blockPanel.add(panel);
		
		// search scope panel
		panel = new JPanel();
		Border border1 = new EmptyBorder(0, 3, 0, 5);
		scopeAll = new JRadioButton(res.getDisplay("label.session"));
		scopeAll.setBorder(border1);
		scopeDoc = new JRadioButton(res.getDisplay("label.document"));
		scopeDoc.setBorder(border1);
		scopeArt = new JRadioButton(res.getDisplay("label.article"));
		scopeArt.setBorder(border1);
		ButtonGroup group = new ButtonGroup();
		group.add(scopeArt);
		group.add(scopeDoc);
		group.add(scopeAll);
		panel.add(scopeAll);
		panel.add(scopeDoc);
		panel.add(scopeArt);
		blockPanel.add(panel);
		add(blockPanel);
		
//		panel.setBackground(UnixColor.BurlyWood);
		panel.addAncestorListener(this);
	}
	
	/** Sets the text to be preset as the search value.
	 * 
	 * @param text String, may be null
	 */
	public void setInputText (String text) {
		textFld.setSelectedItem(text);
	}
	
	/** Returns the search text which the user has input. The result is 
	 * trimmed.
	 * 
	 * @return String
	 */
	public String getInputText () {
		String hs = (String) textFld.getEditor().getItem();
		hs = hs == null ? "" : hs.trim();
		Log.debug(8, "(TextSearchPanel.getInputText) -- user input recognised: [" + hs + "]");
		textFld.removeItem(hs);
		textFld.insertItemAt(hs, 0);
		textFld.setSelectedItem(hs);
		return hs;
	}

	/** Returns the user selected scope of the search.
	 * 0 = session (all documents), 1 = document, 2 = article.
	 * 
	 * @return int scope value (0, 1, 2)
	 */
	public int getScope () {
		return scopeAll.isSelected() ? 0 : scopeDoc.isSelected() ? 1 : 2;
	}

	/** Sets the search scope reported by this panel. The value can be 
	 * corrected silently.
	 * 
	 * @param scope int 0 = session, 1 = document, 2 = article
	 */
	public void setScope (int scope) {
		Util.requirePositive(scope);
		if (scope == 0) {
			scopeAll.setSelected(true);
		} else if (scope == 1 || !articleEnabled) {
			scopeDoc.setSelected(true);
		} else {
			scopeArt.setSelected(true);
		}
	}
	
	public boolean isArticleEnabled () {return articleEnabled;}
	
	public void setArticleEnabled (boolean v) {
		articleEnabled = v;
		scopeArt.setEnabled(v);
		if (!articleEnabled && scopeArt.isSelected()) {
			scopeDoc.setSelected(true);
		}
	}
	
	/** Returns the user chosen maximum value for search results.
	 * 
	 * @return int 
	 */
	public int getMaxSearchResults () {
		return 300;
	}
	
	public boolean isCaseSensitive () {
		return sensitiveChk.isSelected();
	}
	
	public boolean isWholeWordOnly () {
		return wholeWordChk.isSelected();
	}

	public void setCaseSensitive (boolean v) {
		sensitiveChk.setSelected(v);
	}

	public void setWholeWordOnly (boolean v) {
		wholeWordChk.setSelected(v);
	}

	@Override
	public void ancestorAdded(AncestorEvent event) {
		PersistentOptions options = Global.getOptions();
		setCaseSensitive(options.isOptionSet("searchCaseSensitive"));
		setWholeWordOnly(options.isOptionSet("searchWholeWords"));
		
		// retrieve history list of search values into combo-box
		List<String> list = options.getStringList("textSearchValues");
		textFld.removeAllItems();
		for (String s : list) {
			textFld.addItem(s);
		}
		
		// retrieve search scope setting
		int val = options.getIntOption("textSearchScope");
		switch (val) {
		case 0:  scopeAll.setSelected(true); break;
		case 2:  scopeArt.setSelected(true); break;
		default: scopeDoc.setSelected(true); break;
		}
	}

	@Override
	public void ancestorMoved (AncestorEvent event) {
	}

	@Override
	public void ancestorRemoved (AncestorEvent event) {
		PersistentOptions options = Global.getOptions();
		options.setOption("searchCaseSensitive", isCaseSensitive());
		options.setOption("searchWholeWords", isWholeWordOnly());
		
		// complete the value input (end edit)
		getInputText();
		
		// store the list of search values in combo-box
		List<String> list = new ArrayList<>();
		for (int i = 0; i < textFld.getItemCount() & i < MAX_LIST_VALUES; i++) {
			list.add(textFld.getItemAt(i));
		}
		options.setStringList("textSearchValues", list);
		
		// store search scope setting
		int val = scopeArt.isSelected() ? 2 : scopeAll.isSelected() ? 0 : 1;
		options.setIntOption("textSearchScope", val);
	}
	
	
}

