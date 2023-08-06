/*
*  File: EditListAction.java
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

package org.ragna.front.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 *	A simple popup editor for a {@code JList} that allows you to modify
 *  the displayed text value of the selected row. The type argument to the
 *  class represents the type argument of the list model, i.e. the JList.
 *
 *  <p>You have to extend this class and implement the 'applyValueToModel' 
 *  method. The action event of the triggering call must have the JList instance
 *  as source argument.
 *
 *  <p>In the subclass you can invoke the 'setModelClass' method to restrict
 *  the list model to your needs. By default the restriction is set to 
 *  'ListModel', the most generic model class.
 *  
 *  <p>Source: https://tips4java.wordpress.com/2008/10/19/list-editor/
 *
 *  @author Rob Camick, 2008
 *  @author Wolfgang Keller, 2021
 */
public abstract class EditListAction<T> extends AbstractAction {
	
	private JList<T> list;
	private JPopupMenu editPopup;
	private JTextField editTextField;
	private Class<?> modelClass;

	public EditListAction() {
		setModelClass(ListModel.class);
	}

	protected void setModelClass (Class<? extends ListModel> modelClass) {
		this.modelClass = modelClass;
	}

	/** Apply the result of the editing action which is transferred through
	 * parameter 'value'.
	 * 
	 * @param value String edited text value
	 * @param model the list model in use
	 * @param row int index value of edited row
	 */
	protected abstract void applyValueToModel (String value, ListModel<T> model, int row);

	/*
	 *	Display the popup editor when requested
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed (ActionEvent e) {
		if (!(e.getSource() instanceof JList)) return;
		
		list = (JList<T>)e.getSource();
		ListModel<T> model = list.getModel();

		if (! modelClass.isAssignableFrom(model.getClass())) return;

		//  Do a lazy creation of the popup editor

		if (editPopup == null) {
    		createEditPopup();
		}

		//  Position the popup editor over top of the selected row

		int row = list.getSelectedIndex();
		Rectangle r = list.getCellBounds(row, row);

		editPopup.setPreferredSize(new Dimension(r.width, r.height));
		editPopup.show(list, r.x, r.y);

		//  Prepare the text field for editing

		editTextField.setText( list.getSelectedValue().toString() );
		editTextField.selectAll();
		editTextField.requestFocusInWindow();
	}

	/*
	 *  Create the popup editor
	 */
	private void createEditPopup() {
		//  create a text field as the editor
		editTextField = new JTextField();
		Border border = UIManager.getBorder("List.focusCellHighlightBorder");
		editTextField.setBorder( border );

		//  Add an Action to the text field to save the new value to the model

		editTextField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String value = editTextField.getText();
				ListModel<T> model = list.getModel();
				int row = list.getSelectedIndex();
				applyValueToModel(value, model, row);
				editPopup.setVisible(false);
			}
		});

		//  create popup and add the editor to it
	    editPopup = new JPopupMenu();
		editPopup.setBorder( new EmptyBorder(0, 0, 0, 0) );
    	editPopup.add(editTextField);
	}
}