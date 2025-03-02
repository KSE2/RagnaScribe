/*
*  File: DocumentToolboxDialog.java
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

import java.awt.BorderLayout;
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

import org.ragna.core.ActionHandler;
import org.ragna.core.Global;
import org.ragna.core.PadDocument;
import org.ragna.core.PadDocument.DocumentType;
import org.ragna.front.util.ButtonBarDialog;
import org.ragna.front.util.DefaultButtonBarListener;
import org.ragna.front.util.DialogButtonBar;
import org.ragna.front.util.ResourceLoader;
import org.ragna.io.IO_Manager;

import kse.utilclass.gui.VerticalFlowLayout;
import kse.utilclass.misc.Log;
import kse.utilclass.misc.UUID;
import kse.utilclass.misc.Util;

public class DocumentToolboxDialog extends ButtonBarDialog {

	private ActionHandler ahandler = ActionHandler.get();
	private ActionListener actions = new OurActions();
	private PadDocument document;
	private String[] encodingValues;
	private UUID oldUUID;
	private String oldEncoding;
	private PadDocument oldDoc;

	private JLabel titleLabel;
	private JLabel uuidLabel;
	private JComboBox<String> encodingCmb;
	private JCheckBox chkReadOnly;
	
	private UUID currentUUID;
	private String currentEncoding;
	private boolean isUncopedEncoding;
	private boolean haveFile;

	public DocumentToolboxDialog (PadDocument doc) throws HeadlessException {
		super(Global.getActiveFrame(), DialogButtonBar.OK_CANCEL_BUTTON, true);
		document = doc;
		init();
	}
	
	/** Updates variable display values.
	 */
	private void setupDisplay () {
		uuidLabel.setText("UUID = " + currentUUID.toString());
		encodingCmb.setSelectedItem(currentEncoding);
	}

	private void init () {
		Objects.requireNonNull(document, "document is null");
		setTitle(ActionHandler.displayText("dlg.title.toolbox"));
		moveRelatedTo(Global.getActiveFrame());
		
		// note down current modifiable values
		oldDoc = document;
		oldUUID = document.getUUID();
		currentUUID = oldUUID;
		oldEncoding = document.getEncoding();
		currentEncoding = oldEncoding;
		
		// prepare possible encoding values
		String[] enc = Global.getTreepadCompatibleCharsets();
		encodingValues = Util.concatArrays(new String[] {Global.res.getDisplay("combo.unknown")}, enc);

		// dialog content panel
		JPanel pane = new JPanel(new VerticalFlowLayout(7));
		pane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		// document title label
		String text = ActionHandler.displayText("label.title") + " = " + document.getTitle();
		titleLabel = new JLabel(text);
		pane.add(titleLabel);

		// filepath label
		String filepath = IO_Manager.get().getExternalFileReference(oldUUID);
		File file = filepath == null ? null : new File(filepath);
		haveFile = file == null ? false : file.isFile();
		String pathText = filepath != null ? filepath : "<font color=\"orange\">" 
		                  + displayText("label.no-filepath") + "</font>";
		text = "<html>" + displayText("label.file")  + " = " + pathText;
		JLabel label = new JLabel(text);
		pane.add(label);
		
		// iff external file exists: labels for file-time and file-length 
		if (haveFile) {
			long time = file.lastModified();
			long fsize = file.length();
			pane.add(new JLabel(ActionHandler.displayText("label.file-time") + " = " + Util.localeTimeString(time)));
			pane.add(new JLabel(ActionHandler.displayText("label.file-length") + " = " + Util.dottedNumber(fsize)));
		}

		// UUID label and NEW button
		JPanel panel = new JPanel(new BorderLayout(10, 0));
		uuidLabel = new JLabel(); 
		panel.add(uuidLabel);
		JButton button = new JButton(Global.res.getCommand("action.toolbox.new-uuid"));
		button.setActionCommand("action.new.uuid");
		button.addActionListener(actions);
		button.setToolTipText(Global.res.getCommand("toolbox.new-uuid.tooltip"));
		panel.add(button, BorderLayout.EAST);
		pane.add(panel);

		// indicator for file encryption
		try {
			boolean isEnc = !haveFile ? document.isEncrypted() : IO_Manager.get().isFileEncrypted(file);
			pane.add(new JLabel(ActionHandler.displayText("label.encrypted") + " = " + Global.yesNoText(isEnc)));
		} catch (IOException e1) {
			throw new IllegalStateException(e1);
		}
		
		// checkbox setting for READ-ONLY property
		chkReadOnly = new JCheckBox(ActionHandler.displayText("label.document.read-only"), document.isReadOnly());
		chkReadOnly.setIconTextGap(15);
		pane.add(chkReadOnly);
		
		// combo choice for TREEPAD encoding (iff TREEPAD document)
		if (document.getDocType() == DocumentType.TreePad) {
			// Treepad encoding
			panel = new JPanel();
	 		panel.add(new JLabel(Global.res.getDisplay("prefbox.encoding")));
			encodingCmb = new JComboBox<String>(encodingValues);
			encodingCmb.setSelectedItem(null);
			encodingCmb.addActionListener(actions);
			panel.add(encodingCmb);
			
			// file RELOAD button iff external file exists
			if (haveFile) {
				// document reload button
				JButton b = new JButton(Global.res.getCommand("action.file.reload"));
				b.setActionCommand("action.file.reload");
				b.setToolTipText(Global.res.getCommand("file.reload.tooltip"));
				b.addActionListener(actions);
				panel.add(b);
			}
			pane.add(panel);
		}
		
		setupDisplay();
		setDialogPanel(pane);
		addButtonBarListener(new OurButtonBarListener());
	}
	
    /** Equivalent to {@code "ResourceLoader.get().getDisplay(token)"}.
     * 
     * @param token String text token
     * @return String text or "FIXME" if unknown
     */
    public static String displayText (String token) {
 	   return ResourceLoader.get().getDisplay(token);
    }
   
//   ********  INNER CLASSES  *******    
	
	private class OurButtonBarListener extends DefaultButtonBarListener {

		@Override
		public boolean okButtonPerformed() {
			// return to edit option if new encoding uncoped
			if (isUncopedEncoding) {
				if (GUIService.userConfirm(DocumentToolboxDialog.this, "msg.toolbox.return-option")) {
					return false;
				}
			}

			// set new values in document
			document.setReadOnly(chkReadOnly.isSelected());
			document.setUUID(currentUUID);
			boolean encodingChanged = !currentEncoding.equals(oldEncoding);
			boolean uuidChanged = !currentUUID.equals(oldUUID);

			// automated saving of the document if we have a file and (the document is unmodified and a new encoding set
			// or if UUID has changed)
			if (haveFile && (!document.isModified() && isUncopedEncoding || uuidChanged)) {
				Log.log(4, "(DocumentToolBox) auto-saving document: " + document.getShortTitle());
				document.setModified();
				ahandler.saveDocumentToFileSystem(document, true);
			} else {
				if (encodingChanged) {
					document.setModified();
					document.savePreferences(false);
					String mirName = ActionHandler.get().mirrorAdapterIdentifier(document);
					Global.getMirrorFileManager().startMirrorSavingFor(mirName);
				}
			}
			
			if (uuidChanged) {
				Global.saveSystemOptions();
			}
			DocumentToolboxDialog.this.dispose();
			return true;
		}

		@Override
		public void cancelButtonPerformed() {
			// restore old values
			document.setEncoding(oldEncoding);
			oldDoc.setEncoding(oldEncoding);
			
			// if document was reloaded 
			// then restore old version through again reloading
			if (document != oldDoc) {
				Log.log(8, "(DocumentToolboxDialog) CANCEL: resetting to old document");
				Global.getStatusBar().clearMessage();
				Global.getDocumentRegistry().replaceDocument(document, oldDoc);
			}

			DocumentToolboxDialog.this.dispose();
		}
	}
	
	private class OurActions implements ActionListener {

		@Override
		public void actionPerformed (ActionEvent e) {
			Object source = e.getSource();
			String command = e.getActionCommand();
			
			if (source == encodingCmb) {
				@SuppressWarnings("unchecked")
				String hstr = (String) ((JComboBox<String>)e.getSource()).getSelectedItem();
				document.setEncoding(hstr);
				currentEncoding = document.getEncoding();
				isUncopedEncoding = haveFile && !hstr.equals(oldEncoding);

			} else if (command.equals("action.file.reload")) {
				PadDocument doc = ahandler.reloadDocument(document);
				if (doc != null) {
					document = doc;
					currentEncoding = document.getEncoding();
					setupDisplay();
				}
				isUncopedEncoding = false;
				
			} else if (command.equals("action.new.uuid")) {
				if (haveFile && document.isModified()) {
					GUIService.infoMessage(DocumentToolboxDialog.this, "msg.failure", "msg.toolbox.unmodified-doc");
				} else if (chkReadOnly.isSelected()) {
					GUIService.infoMessage(DocumentToolboxDialog.this, "msg.failure", "msg.toolbox.readonly-doc");
				} else {
					currentUUID = new UUID();
					setupDisplay();
				}
			}
		}
		
	}
}
