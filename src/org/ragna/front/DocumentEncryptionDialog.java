/*
*  File: DocumentEncryptionDialog.java
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.ragna.core.ActionHandler;
import org.ragna.core.PadDocument;
import org.ragna.front.util.ButtonBarDialog;
import org.ragna.front.util.DefaultButtonBarListener;
import org.ragna.front.util.DialogButtonBar;
import org.ragna.front.util.MessageDialog;
import org.ragna.front.util.ResourceLoader;

import kse.com.fhash.main.Functions;
import kse.utilclass.gui.BoxedFlowLayout;
import kse.utilclass.gui.VerticalFlowLayout;
import kse.utilclass.misc.Util;

/** Dialog class to let the user decide over document encryption and input
 * an encryption passphrase (secret key). The class refers to the currently 
 * selected pad-document and updates the document with modified dialog values.
 */
public class DocumentEncryptionDialog extends ButtonBarDialog {
	
	public enum TerminationType {CANCELED, FAILED, CONFIRMED}

	private JCheckBox chkEncr;
	private JPasswordField fldPass1, fldPass2;

	private PadDocument document;
	private byte[] initialPass; 
	private boolean initialCheck;
	
	   /** Equivalent to {@code "ResourceLoader.get().getDisplay(token)"}.
	    * 
	    * @param token String text token
	    * @return String text or "FIXME" if unknown
	    */
	   public static String displayText (String token) {
		   return ResourceLoader.get().getDisplay(token);
	   }
	   
	/** Creates a new document encryption dialog, referring to the PadDocument
	 * currently selected in display. If the document is encrypted then access
	 * is controlled by asking user to verify. 
	 * 
	 * @throws IllegalStateException if there is no document selected
	 */
	public DocumentEncryptionDialog () {
		super();
		setModal(true);
		init();
	}
	
	private void init () {
		setTitle(displayText("dlg.title.encryption"));
		addButtonBarListener(new OurButtonBarListener());
		
		document = DisplayManager.get().getSelectedDocument();
		if (document == null)
			throw new IllegalStateException("no document selected");
		initialCheck = document.isEncrypted();
		initialPass = document.getPassphrase();
		
		JPanel pane = new JPanel(new VerticalFlowLayout(8));
		pane.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
		
		chkEncr = new JCheckBox(displayText("chk.encrypt.doc"), initialCheck);
		chkEncr.setToolTipText(displayText("tooltip.encrypt.doc"));
		chkEncr.setIconTextGap(15);
		chkEncr.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fldPass1.setEnabled(chkEncr.isSelected());
				fldPass2.setEnabled(chkEncr.isSelected());
			}
		});
		pane.add(chkEncr);
		
		JPanel panel = new JPanel(new BoxedFlowLayout(2, 8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		panel.add(new JLabel(displayText("label.encrypt.key")));
		fldPass1 = new JPasswordField(20);
		fldPass1.setEnabled(initialCheck);
		panel.add(fldPass1);
		panel.add(new JLabel(displayText("label.encrypt.retype")));
		fldPass2 = new JPasswordField(20);
		fldPass2.setEnabled(initialCheck);
		panel.add(fldPass2);
		pane.add(panel);

		setDialogPanel(pane);
	}
	
	
	public static TerminationType verifyPassphrase (Component owner, byte[] initial) {
		JPanel pane = new JPanel(new VerticalFlowLayout(8));
		pane.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
		pane.add(new JLabel(displayText("label.enter.key")));
		JPasswordField passFld = new JPasswordField(20);
		pane.add(passFld);

		// show a message dialog (question)
		boolean opted = MessageDialog.showConfirmMessage(owner, "dlg.access.control", pane, DialogButtonBar.OK_CANCEL_BUTTON);

		// try verify the input
		if (opted) {
			char[] input = passFld.getPassword();
			boolean checked = Util.equalArrays(initial, Util.fingerPrint(input));
			Arrays.fill(input, (char)0);
			return checked ? TerminationType.CONFIRMED : TerminationType.FAILED; 
		}
		return TerminationType.CANCELED;
	}

	/** Returns a machine-near transformation of the user key input or null 
	 * if the input is empty or faulty. If not null this returns a result of a
	 * fixed length (SHA-256 digest).
	 * 
	 * @return byte[] or null
	 */
	public byte[] getPassphrase () {
		char[] ca = fldPass1.getPassword();
		char[] ca2 = fldPass2.getPassword();
		if (!Arrays.equals(ca, ca2)) return null;
		return ca.length == 0 ? null : Functions.makeEncryptKey(ca);
	}
	
	private class OurButtonBarListener extends DefaultButtonBarListener {

		OurButtonBarListener() {
			super(DocumentEncryptionDialog.this);
		}
		
		@Override
		public boolean okButtonPerformed () {
			byte[] passwd = getPassphrase();
			if (!chkEncr.isSelected()) {
				document.setEncrypted(null);
				
			} else {
				if (passwd == null) {
					GUIService.infoMessage("dlg.input-error", displayText("msg.error.passphrase"));
					return false;
				}

				// modify and store document if password is new or changed
				if (!Arrays.equals(passwd, initialPass)) {
					// verify modified passphrase
					document.setEncrypted(passwd);
					ActionHandler.get().saveDocumentToFileSystem(document, true);
				}
			}
			return super.okButtonPerformed();
		}
		
	}
}
