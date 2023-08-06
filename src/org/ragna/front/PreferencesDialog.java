/*
*  File: PreferencesDialog.java
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

import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.jpws.pwslib.global.Log;
import org.ragna.core.Global;
import org.ragna.front.util.ButtonBarDialog;
import org.ragna.front.util.DefaultButtonBarListener;
import org.ragna.front.util.DialogButtonBar;
import org.ragna.front.util.ResourceLoader;
import org.ragna.util.PersistentOptions;

import kse.utilclass.gui.BoxedFlowLayout;
import kse.utilclass.gui.VerticalFlowLayout;
import kse.utilclass.misc.UnixColor;
import kse.utilclass.misc.Util;

/** This dialog displays and makes editable user options in the parameters
 * which determine the behaviour of the program. These options are stored,
 * together with other parameters, in the Preferences Context. This context is
 * regularly stored within the Java Preferences system; it can, however, be made
 * storable in a text file named as command-line parameter at program start. 
 * 
 */
public class PreferencesDialog extends ButtonBarDialog {
	private static final int MINIMUM_WORKER_THREADS = 2;
	private static final int MAXIMUM_WORKER_THREADS = 5;
	
	PersistentOptions 		options;
	ActionListener 			actions = new ActListener();

	// display elements
	private JTextField 		startSearchFld;
	private JTextField 		startTranslateFld;
	JComboBox<String>		autoOpenCmb ;
	JComboBox<String>		padEncodingCmb;
	JCheckBox				mirrorChk;
	JCheckBox				historyChk;
	JSpinner				workerSpin;
	JLabel					histDirLab;
	JButton					histDirBut;
	JButton					histResetBut;

	// data elements
	String[] autoOpenValues = new String[] {
		displayText("combo.auto-open.0"), displayText("combo.auto-open.1"), displayText("combo.auto-open.2")
	};
	String[] encodingValues;
	
	public PreferencesDialog() throws HeadlessException {
		super(null, DialogButtonBar.OK_CANCEL_BUTTON, true);
		init();
	}

	private void init () {
		options = Global.getOptions();
		assert options != null;
		
		// prepare encoding values
		String[] enc = Global.getTreepadCompatibleCharsets();
		encodingValues = Util.concatArrays(new String[] {displayText("combo.unknown")}, enc);

		setTitle(displayText("dlg.preferences"));
		setBarGap(6);
		setResizable(true);
		addButtonBarListener(new BarListener());
		setDialogPanel(createContentPanel());
		
		readValues();
	}

	JPanel createContentPanel () {
		JPanel pane = new JPanel(new VerticalFlowLayout(5));
		pane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		JPanel panel, panel2;
		JLabel label;
		
		// auto-open option (combo)
		panel = new JPanel(new BoxedFlowLayout(2, 8, 5));
		panel.add(new JLabel(displayText("prefbox.auto-open")));
		autoOpenCmb = new JComboBox<String>(autoOpenValues);
		panel.add(autoOpenCmb);
		
		// Treepad encoding
 		panel.add(new JLabel(displayText("prefbox.treepad-encoding")));
		padEncodingCmb = new JComboBox<String>(encodingValues);
		panel.add(padEncodingCmb);
		pane.add(panel);
		
		// worker threads (int spinner)
		panel = new JPanel();
		panel.add(new JLabel(displayText("prefbox.workers")));
		workerSpin = new JSpinner(new SpinnerNumberModel(MINIMUM_WORKER_THREADS, 
					 MINIMUM_WORKER_THREADS, MAXIMUM_WORKER_THREADS, 1));
		panel.add(workerSpin);
		pane.add(panel);
		
		// file history check
		panel = new JPanel(new VerticalFlowLayout());
		historyChk = new JCheckBox(displayText("prefbox.history-files"));
		historyChk.setIconTextGap(12);
		historyChk.setOpaque(true);
		panel.add(historyChk);
		
		// mirror usage check
		mirrorChk = new JCheckBox(displayText("prefbox.file-mirroring"));
		mirrorChk.setIconTextGap(12);
		mirrorChk.setOpaque(true);
		panel.add(mirrorChk);

		pane.add(panel);
		
		// file history directory
		panel = new JPanel(new VerticalFlowLayout(4));
		label = new JLabel(displayText("prefbox.history-dir")); 
		panel.add(label);
		panel2 = new JPanel();
		histDirBut = createSwitchButton(displayText("button.select"));
		panel2.add(histDirBut);
		histResetBut = createSwitchButton(displayText("button.reset"));
		panel2.add(histResetBut);
		panel.add(panel2);
		histDirLab = new JLabel();
		histDirLab.setForeground(UnixColor.ForestGreen);
		panel.add(histDirLab);
		pane.add(Box.createVerticalStrut(4));
		pane.add(panel);
		
		// browser start expression for INTERNET SEARCH
		panel = new JPanel(new BoxedFlowLayout(2, 8, 5));
		label = new JLabel(displayText("prefbox.search-service")); 
		panel.add(label);
		startSearchFld = new JTextField(30);
		panel.add(startSearchFld);
		
		// browser start expression for INTERNET SEARCH
		label = new JLabel(displayText("prefbox.translate-service")); 
		panel.add(label);
		startTranslateFld = new JTextField(30);
		panel.add(startTranslateFld);
		pane.add(Box.createVerticalStrut(5));
		pane.add(panel);
		
		return pane;
	}
	
	/** Creates and returns a formatted button for in-panel switches.
	 * 
	 * @param text String button title
	 * @return {@code JButton}
	 */
	JButton createSwitchButton (String text) {
		JButton b = new JButton(text);
		Font font = b.getFont().deriveFont(10.0F);
		b.setFont(font);
		b.setMargin(new Insets(2, 5, 2, 5));
		b.addActionListener(actions);
		return b;
	}
	
	/** Reads system options into dialog elements.
	 */
	private void readValues () {
		int h;
		String hs;
		
		// auto-open combo
		h = options.isOptionSet("isAutoOpenRecentFile") ? 1 :
			options.isOptionSet("isAutoOpenSession") ? 2 : 0;
		autoOpenCmb.setSelectedIndex(h);
		
		// Treepad encoding
		String value = options.getOption("defaultTreepadEncoding");
		if (Util.arrayContains(encodingValues, value)) {
			padEncodingCmb.setSelectedItem(value);
		} else {
			padEncodingCmb.setSelectedIndex(0);
		}
		
		// number worker threads
		h = options.getIntOption("workerThreads");
		workerSpin.setValue(h);
		
		// mirroring and history
		mirrorChk.setSelected(options.isOptionSet("useMirroring"));
		historyChk.setSelected(options.isOptionSet("useFileHistory"));
		
		// history directory
		hs = Global.getHistoryDirectory().getAbsolutePath();
		histDirLab.setText(hs);
		histDirLab.setToolTipText(hs);
		
		// start browser expressions
		startSearchFld.setText(options.getOption("startSearchExp"));
		startTranslateFld.setText(options.getOption("startTranslateExp"));
	}
	
	/** Writes dialog element values into system options iff they are valid.
	 */
	private void writeValues () {
		// auto-open combo
		switch (autoOpenCmb.getSelectedIndex()) {
		case 0: 
			options.setOption("isAutoOpenRecentFile", false);
			options.setOption("isAutoOpenSession", false);
			break;
		case 1:
			options.setOption("isAutoOpenRecentFile", true);
			options.setOption("isAutoOpenSession", false);
			break;
		case 2:
			options.setOption("isAutoOpenRecentFile", false);
			options.setOption("isAutoOpenSession", true);
			break;
		}
		
		// Treepad encoding
		String value = padEncodingCmb.getSelectedIndex() == 0 ? null : 
			           (String) padEncodingCmb.getSelectedItem();
		options.setOption("defaultTreepadEncoding", value);
		Log.debug(8, "(PreferencesDialog.writeValues) default Treepad encoding = " + value);

		// number worker threads
		options.setIntOption("workerThreads", (int) workerSpin.getValue());
		
		// mirroring and history files
		options.setOption("useMirroring", mirrorChk.isSelected());
		options.setOption("useFileHistory", historyChk.isSelected());
		
		// history directory
		try {
			Global.setHistoryDirectory(new File(histDirLab.getText()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// start browser expressions
		options.setOption("startSearchExp", startSearchFld.getText().trim());
		options.setOption("startTranslateExp", startTranslateFld.getText().trim());
	}
	
	private boolean verifyInputValues () {
		// http values have no blanks
		String hs1 = startSearchFld.getText().trim();
		String hs2 = startTranslateFld.getText().trim();
		if (hs1.indexOf(' ') > -1 || hs2.indexOf(' ') > -1) {
			GUIService.infoMessage("dlg.input-warning", "prefmsg.illegal-http");
			return false;
		}
		
		// history directory exists
		hs1 = histDirLab.getText();
		if (hs1.isEmpty() || !new File(hs1).isDirectory()) {
			GUIService.infoMessage("dlg.input-warning", "prefmsg.illegal-histdir");
			return false;
		}
		return true;
	}
	
    /** Equivalent to {@code "ResourceLoader.get().getDisplay(token)"}.
     * 
     * @param token String text token
     * @return String text or "FIXME" if token is unknown
     */
    public static String displayText (String token) {
	   return ResourceLoader.get().getDisplay(token);
    }
	   
    
    private class BarListener extends DefaultButtonBarListener {

    	public BarListener () {
    		super(PreferencesDialog.this);
    	}
    	
		@Override
		public boolean okButtonPerformed() {
			// control input value set
			if (!verifyInputValues()) {
				return false;
			}
			
			// input verified
			writeValues();
			return super.okButtonPerformed();
		}

//		@Override
//		public void cancelButtonPerformed() {
//			super.cancelButtonPerformed();
//		}
    }
    
    private class ActListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e == null) return;
			Object source = e.getSource();
			
			try {
			// History Dir select button
			if (source == histDirBut) {
				File dir = new File(histDirLab.getText());
				JFileChooser chooser = new JFileChooser(dir);
				chooser.setDialogTitle("Select History Directory");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				if (chooser.showOpenDialog(Global.getActiveFrame()) == JFileChooser.APPROVE_OPTION) {
					dir = chooser.getSelectedFile();
					if (dir != null && dir.isDirectory()) {
						histDirLab.setText(dir.getCanonicalPath());
						histDirLab.setToolTipText(histDirLab.getText());
					}
				}
			}
			
			// History Dir reset value button
			if (source == histResetBut) {
				String text = Global.getDefaultHistoryDirName().getAbsolutePath();
				histDirLab.setText(text);
				histDirLab.setToolTipText(text);
			}
			
			} catch (Throwable ex) {
		        GUIService.failureMessage(null, ex);
		        ex.printStackTrace();
			}
		}
    	
    }
}
