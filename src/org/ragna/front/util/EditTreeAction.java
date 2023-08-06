/*
*  File: EditTreeAction.java
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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 *	A simple popup editor for a {@code JList} that allows you to modify
 *  the displayed text value of the selected row. The type argument to the
 *  class represents the type argument of the tree model, i.e. the JList.
 *
 *  <p>You have to extend this class and implement the 'applyValueToModel' 
 *  method. The action event of the triggering call must have the JList instance
 *  as source argument.
 *
 *  <p>In the subclass you can invoke the 'setModelClass' method to restrict
 *  the tree model to your needs. By default the restriction is set to 
 *  'ListModel', the most generic model class.
 *  
 *  <p>Source: https://tips4java.wordpress.com/2008/10/19/tree-editor/
 *
 *  @author Rob Camick, 2008
 *  @author Wolfgang Keller, 2021
 */
public abstract class EditTreeAction extends AbstractAction
{
	private JTree tree;
	private JPopupMenu editPopup;
	private JTextField editTextField;
	private Class<?> modelClass;

	public EditTreeAction() {
		setModelClass(TreeModel.class);
	}

	protected void setModelClass (Class<? extends TreeModel> modelClass) {
		this.modelClass = modelClass;
	}

	/** Apply the result of the editing action which is transferred through
	 * parameter 'value'.
	 * 
	 * @param value String edited text value
	 * @param path TreePath selected tree-path 
	 * @param row int index value of edited row
	 */
	protected abstract void applyValueToModel (String value, TreePath path, int row);

	/*
	 *	Display the popup editor when requested
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed (ActionEvent e) {
		if (!(e.getSource() instanceof JTree)) return;
		
		tree = (JTree)e.getSource();
		TreeModel model = tree.getModel();
		if (! modelClass.isAssignableFrom(model.getClass())) return;

		//  lazy creation of the popup editor
		if (editPopup == null) {
    		createEditPopup();
		}

		//  position the popup editor over top of the selected row
		int row = tree.getMinSelectionRow();
		Rectangle r = tree.getRowBounds(row);
		editPopup.setPreferredSize(new Dimension(r.width, r.height));
		editPopup.show(tree, r.x + 18, r.y);

		//  prepare the text field for editing
		TreePath path = tree.getPathForRow(row);
		editTextField.setText( path.getLastPathComponent().toString() );
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
		editTextField.setBorder(border);

		//  add an Action to the text field to save the new value to the model
		editTextField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String value = editTextField.getText();
				int row = tree.getMinSelectionRow();
				TreePath path = tree.getSelectionPath();
				applyValueToModel(value, path, row);
				editPopup.setVisible(false);
			}
		});

		//  create popup and add the editor to it
	    editPopup = new JPopupMenu();
		editPopup.setBorder( new EmptyBorder(0, 0, 0, 0) );
    	editPopup.add(editTextField);
	}
}