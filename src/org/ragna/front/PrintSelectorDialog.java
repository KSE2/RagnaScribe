/*
*  File: PrintSelectorDialog.java
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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.ragna.core.Global;
import org.ragna.core.PadArticle;
import org.ragna.core.PadDocument;
import org.ragna.core.PrintParameters;
import org.ragna.core.PrintParameters.ArticlePrintModus;
import org.ragna.core.PrintParameters.PrintScope;
import org.ragna.front.util.ButtonBarDialog;
import org.ragna.front.util.DefaultButtonBarListener;
import org.ragna.util.PersistentOptions;

import kse.utilclass.gui.FontChooser;
import kse.utilclass.gui.VerticalFlowLayout;


public class PrintSelectorDialog extends ButtonBarDialog {
	private static final int MIN_FONT_SIZE = 4;
	private static final int MAX_FONT_SIZE = 34;
	
	private OurChangeListener changeListener = new OurChangeListener();
	private PadDocument document;
	private PadArticle article;
	private boolean isBranch, isArticle, isRoot;
	private PrintParameters printParam; 
	
	private JRadioButton chkDoc, chkBran, chkArt; 
	private ButtonGroup chkGroup = new ButtonGroup();
	private JCheckBox chkHeader, chkTitles, chkEmpty, chkWrapLines;
	private JComboBox<String> combo;
	private JTextField headerFld;
	private JSpinner spinfontSize;
	private JButton fontBut;
	

	/** Creates a new modal {@code PrintSelectorDialog}.
	 * 
	 */
	public PrintSelectorDialog () {
		super();
		init();
	}
	
	private void init () {
		// take up situation
		PersistentOptions options = Global.getOptions();
		document = DisplayManager.get().getSelectedDocument();
		if (document == null) { 
			throw new IllegalStateException("no document selected");
		}
		article = document.getSelectedArticle();
		isArticle = article != null;
		isRoot = article.isRoot();
		isBranch = isArticle && !isRoot && document.hasChildren(article);
		printParam = new PrintParameters(document, article);
		if (options.contains("printFont")) {
		   printParam.font = options.getFontOption("printFont");
		}
		
		// adjust dialog
		setModal(true);
		setTitle(displayText("dlg.title.print.selector"));
		moveRelatedTo(Global.getActiveFrame());
		getButtonBar().getOkButton().setText(displayText("label.print"));
		addButtonBarListener(new OurButtonBarListener());
		
		// setup base panel
		JPanel pane = new JPanel(new VerticalFlowLayout(5));
		pane.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
		pane.add(new JLabel(displayText("label.printscope")));

		// printing scope panel (radio selection)
		JPanel panel = new JPanel(new VerticalFlowLayout(2));
		panel.setBorder(BorderFactory.createEmptyBorder(0, 30, 8, 10));
		chkDoc = new JRadioButton(displayText("radio.entire.doc"), true);
		chkDoc.setIconTextGap(10);
		chkDoc.addChangeListener(changeListener);
		chkGroup.add(chkDoc);
		panel.add(chkDoc);
		chkBran = new JRadioButton(displayText("radio.selected.branch"));
		chkBran.setIconTextGap(10);
		if (isBranch) {
			chkGroup.add(chkBran);
			panel.add(chkBran);
		}
		chkArt = new JRadioButton(displayText("radio.selected.art"));
		chkArt.setIconTextGap(10);
		if (isArticle) {
			chkGroup.add(chkArt);
			panel.add(chkArt);
		}
		JRadioButton selBut = !isBranch && !isRoot && isArticle ? chkArt : isBranch ? chkBran : chkDoc;
		selBut.setSelected(true);
		pane.add(panel);
		
		// combo selector for print result type
		combo = new JComboBox<>();
		combo.addItem(displayText("choice.printmod.continuous"));
		combo.addItem(displayText("choice.printmod.newpage")); // articles start on new page
//		combo.addItem(displayText("choice.printmod.separate")); // each article as separate print
//		int select = options.getIntOption("documentPrintModus");
//		combo.setSelectedIndex(Math.max(0, Math.min(1, select)));
//		pane.add(combo);
		combo.setSelectedIndex(0);
		
		// check-box selectors
		chkEmpty = new JCheckBox(displayText("chk.print.ignore.empty"), options.isOptionSet("printIgnoreEmpty"));
		chkEmpty.setIconTextGap(10);
		pane.add(chkEmpty);
		chkWrapLines = new JCheckBox(displayText("chk.print.wrapping"), options.isOptionSet("printLineWrap"));
		chkWrapLines.setIconTextGap(10);
		pane.add(chkWrapLines);
		chkTitles = new JCheckBox(displayText("chk.print.outline"), options.isOptionSet("printOutlineTitles"));
		chkTitles.setIconTextGap(10);
		pane.add(chkTitles);
		chkHeader = new JCheckBox(displayText("chk.print.header"), options.isOptionSet("printHeaders"));
		chkHeader.setIconTextGap(10);
		chkHeader.addChangeListener(changeListener);
		pane.add(chkHeader);
		
		// header text field
		headerFld = new JTextField(30);
		String text = selBut == chkDoc ? document.getTitle() : article.getTitle();
		headerFld.setText(text);
		headerFld.setEnabled(chkHeader.isSelected());
		pane.add(headerFld);
		
		// font size spinner
		int fontSize = printParam.font.getSize();
		fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize));
		spinfontSize = new JSpinner(new SpinnerNumberModel(fontSize, MIN_FONT_SIZE, MAX_FONT_SIZE, 1));
		JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
		panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
		panel1.add(new JLabel(displayText("label.font.size")));
		panel1.add(Box.createHorizontalStrut(10));
		panel1.add(spinfontSize);

		panel = new JPanel(new BorderLayout(10, 0));
		panel.add(panel1, BorderLayout.WEST);
		fontBut = new JButton(printParam.font.getFamily());
		fontBut.setFont(fontBut.getFont().deriveFont(11F));
		fontBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Font font = printParam.font.deriveFont((float)getEditedFontSize());
				font = FontChooser.showDialog(Global.mainframe, null, font);
				if (font != null) {
					printParam.font = font;
					fontBut.setText(font.getFamily());
					spinfontSize.getModel().setValue(font.getSize());
					options.setFontOption("printFont", font);
				}
			}
		});
		panel.add(fontBut, BorderLayout.CENTER);
		pane.add(panel);
		
		setDialogPanel(pane);
	}
	
	/** Gets the text resource from "display" resource bundle. Returns "FIXME"
	 * if the token is not found.
	 * 
	 * @param token String resource key
	 * @return String
	 */
	private String displayText (String token) {
		return Global.res.getDisplay(token);
	}
	
	private int getEditedFontSize () {
		try {
			spinfontSize.commitEdit();
		} catch (ParseException e) {
			GUIService.failureMessage(displayText("msg.failure.fontsize"), null);
		}
		return (int)spinfontSize.getValue();
	}
	
	/** The print parameters if the "Print" button was pressed, otherwise
	 * null.
	 * 
	 * @return {@code PrintParameters} or null
	 */
	public PrintParameters getPrintParameters () {return printParam;}
	
	private class OurButtonBarListener extends DefaultButtonBarListener {

		OurButtonBarListener () {
			super(PrintSelectorDialog.this);
		}
		
		@Override
		public boolean okButtonPerformed() {
			int fontSize = getEditedFontSize();

			int comboSel = combo.getSelectedIndex();
			printParam.printModus = ArticlePrintModus.values()[comboSel == -1 ? 0 : comboSel];
			printParam.printHeaders = chkHeader.isSelected();
			printParam.printTitles = chkTitles.isSelected();
			printParam.ignoreEmpty = chkEmpty.isSelected();
			printParam.wrapLines = chkWrapLines.isSelected();
			printParam.wrapWords = true;
			printParam.printScope = chkArt.isSelected() ? PrintScope.ARTICLE :
					chkBran.isSelected() ? PrintScope.BRANCH : PrintScope.DOCUMENT;
			printParam.headerText = headerFld.getText();
			printParam.font = printParam.font.deriveFont((float)fontSize);
			
			PersistentOptions options = Global.getOptions();
			options.setOption("printHeaders", printParam.printHeaders);
			options.setOption("printIgnoreEmpty", printParam.ignoreEmpty);
			options.setOption("printOutlineTitles", printParam.printTitles);
			options.setOption("printLineWrap", printParam.wrapLines);
			options.setIntOption("documentPrintModus", printParam.printModus.ordinal());
			options.setFontOption("printFont", printParam.font);
			
			return super.okButtonPerformed();
		}
	}
	
	private class OurChangeListener implements ChangeListener {

		@Override
		public void stateChanged (ChangeEvent e) {
			// header selection
			Object source = e.getSource();
			if (source == chkHeader) {
				headerFld.setEnabled(chkHeader.isSelected());
			} else if (source == chkDoc && headerFld != null) {
				String text = chkDoc.isSelected() ? document.getTitle() : article.getTitle();
				headerFld.setText(text);
			}
		}
		
	}
}
