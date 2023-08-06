/*
*  File: DialogDocumentToolbox.java
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

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jpws.pwslib.global.Log;
import org.ragna.core.ActionHandler;
import org.ragna.core.Global;
import org.ragna.core.PadDocument;
import org.ragna.core.ActionHandler.ActionNames;
import org.ragna.core.PadDocument.DocumentType;
import org.ragna.front.util.ButtonBarDialog;
import org.ragna.front.util.DefaultButtonBarListener;
import org.ragna.front.util.DialogButtonBar;
import org.ragna.io.IO_Manager;
import org.ragna.util.ActionManager.UnknownActionException;

import kse.utilclass.gui.VerticalFlowLayout;
import kse.utilclass.misc.UUID;
import kse.utilclass.misc.Util;

public class DialogDocumentToolbox extends ButtonBarDialog {
	private PadDocument document;
	private JLabel titleLabel;
	private JLabel uuidLabel;
	private JComboBox<String>		encodingCmb;
	private String[] encodingValues;
	private String currentEncoding;
	private boolean isUncopedEncoding;

	public DialogDocumentToolbox (PadDocument doc) throws HeadlessException {
		super(Global.getActiveFrame(), DialogButtonBar.OK_BUTTON, true);
		document = doc;
		init();
	}
	
	private void setupDisplay () {
		titleLabel.setText(ActionHandler.displayText("label.title") + " = " + document.getTitle());
		uuidLabel.setText("UUID = " + document.getUUID().toString());
	}

	private void init () {
		Objects.requireNonNull(document, "document is null");
		setTitle(ActionHandler.displayText("dlg.title.toolbox"));
		moveRelatedTo(Global.getActiveFrame());
		
		// prepare encoding values
		String[] enc = Global.getTreepadCompatibleCharsets();
		encodingValues = Util.concatArrays(new String[] {Global.res.getDisplay("combo.unknown")}, enc);

		JPanel pane = new JPanel(new VerticalFlowLayout(7));
		pane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		titleLabel = new JLabel();
		pane.add(titleLabel);

		currentEncoding = document.getEncoding();
		UUID uuid = document.getUUID();
		String filepath = IO_Manager.get().getExternalFileReference(uuid);
		File file = filepath == null ? null : new File(filepath);
		boolean haveFile = file == null ? false : file.isFile();
		JLabel label = new JLabel(ActionHandler.displayText("label.file") + " = " + filepath);
		pane.add(label);
		
		if (haveFile) {
			long time = file.lastModified();
			pane.add(new JLabel(ActionHandler.displayText("label.file-time") + " = " + Util.localeTimeString(time)));
		}
		uuidLabel = new JLabel(); 
		pane.add(uuidLabel);

		// whether file is encrypted
		try {
			boolean isEnc = !haveFile ? document.isEncrypted() : IO_Manager.get().isFileEncrypted(file);
			pane.add(new JLabel(ActionHandler.displayText("label.encrypted") + " = " + isEnc));
		} catch (IOException e1) {
			throw new IllegalStateException(e1);
		}
		
		JCheckBox chkReadOnly = new JCheckBox(ActionHandler.displayText("label.document.read-only"), document.isReadOnly());
		chkReadOnly.setIconTextGap(15);
		chkReadOnly.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				document.setReadOnly(chkReadOnly.isSelected());
			}
		});
		pane.add(chkReadOnly);
		
		if (document.getDocType() == DocumentType.TreePad) {
			// Treepad encoding
			JPanel panel = new JPanel();
	 		panel.add(new JLabel(Global.res.getDisplay("prefbox.encoding")));
			encodingCmb = new JComboBox<String>(encodingValues);
			encodingCmb.setSelectedItem(document.getEncoding());
			encodingCmb.addActionListener(new ActionListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void actionPerformed (ActionEvent e) {
					String hstr = (String) ((JComboBox<String>)e.getSource()).getSelectedItem();
					document.setEncoding(hstr);
					isUncopedEncoding = !hstr.equals(currentEncoding);
				}
			});
			panel.add(encodingCmb);
			
			// document reload button
			if (document.hasExternalFile()) {
				JButton b = new JButton(Global.res.getCommand("action.file.reload"));
				b.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						PadDocument doc = ActionHandler.get().reloadDocument(document);
						if (doc != null) {
							document = doc;
							currentEncoding = document.getEncoding();
							setupDisplay();
						}
						isUncopedEncoding = false;
					}
				});
				panel.add(b);
			}
			
			pane.add(panel);
		}
		
		setupDisplay();
		setDialogPanel(pane);
		addButtonBarListener(new OurButtonBarListener());
	}
	
	private class OurButtonBarListener extends DefaultButtonBarListener {

		@Override
		public boolean okButtonPerformed() {
			if (!document.isModified() && isUncopedEncoding) {
				Log.log(4, "(DocumentToolBox) auto-saving document (encoding changed): " + document.getShortTitle());
				document.setModified();
				ActionHandler.get().saveDocumentToFileSystem(document, true);
			}
			DialogDocumentToolbox.this.dispose();
			return true;
		}
		
	}
}
