/*
*  File: TextLocationListPanel.java
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import org.ragna.core.Global;
import org.ragna.core.TextSearcher.DocumentTextPosition;
import org.ragna.front.util.ResourceLoader;

import kse.utilclass.gui.VerticalFlowLayout;
import kse.utilclass.misc.UnixColor;

public class TextLocationListPanel extends JPanel {
	
	private ResourceLoader res = Global.res;
	private DocumentTextPosition items[] = new DocumentTextPosition[0];
	private JTable table;
	private OurTableModel model;
	private JScrollPane scrollPane;
	private String expression;
	private int scope;
	
	/** Creates a new location list panel w/ the given text locations and
	 * search parameters.
	 * 
	 * @param positions {@code DocumentTextPosition[]} text locations in array
	 * @param searchText String the search pattern
	 * @param scope int 0 = session, 1 = document, 2 = article
	 */
	public TextLocationListPanel (DocumentTextPosition[] positions, String searchText, int scope) {
		expression = searchText;
		this.scope = scope;
		init(positions);
	}
	
	/** Creates a new location list panel w/ the given text locations and
	 * search parameters.
	 * 
	 * @param positions {@code List<DocumentTextPosition>} list of text locations
	 * @param searchText String the search pattern
	 * @param scope int 0 = session, 1 = document, 2 = article
	 */
	public TextLocationListPanel (List<DocumentTextPosition> positions, String searchText, int scope) {
		this(positions.toArray(new DocumentTextPosition[positions.size()]), searchText, scope);
	}
	
	private void init (DocumentTextPosition[] positions) {
		Objects.requireNonNull(positions);
		items = positions;

		// construct the table
		model = new OurTableModel();
		table = new JTable(model);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setTextPositions(positions);
		
		// set the column headers
		TableColumnModel colModel = table.getColumnModel();
		switch (model.columns) {
		case 2:  
			colModel.getColumn(0).setHeaderValue(res.getDisplay("label.article"));
			colModel.getColumn(1).setHeaderValue(res.getDisplay("label.position"));
			colModel.getColumn(1).setMaxWidth(60);
			break;
		case 3:
			colModel.getColumn(0).setHeaderValue(res.getDisplay("label.document"));
			colModel.getColumn(1).setHeaderValue(res.getDisplay("label.article"));
			colModel.getColumn(2).setHeaderValue(res.getDisplay("label.position"));
			colModel.getColumn(2).setMaxWidth(60);
			break;
		}
		
		// construct the parameter information panel
		VerticalFlowLayout layout = new VerticalFlowLayout(0, false); 
		layout.setAlignment(VerticalFlowLayout.CENTER);
		JPanel comPanel = new JPanel(layout); 

		// search text
		JPanel panel = new JPanel();
		JLabel label = new JLabel(res.getDisplay("find.text") + ": ");
		panel.add(label);
		String text = expression.substring(0, Math.min(expression.length(), 60));
		label = new JLabel(text);
		label.setForeground(UnixColor.BlueViolet);
		panel.add(label);	
		comPanel.add(panel);
		
		// scope line definition
		String scopeLine = null;
		switch (scope) {
		case 0: scopeLine = res.getDisplay("scope.session"); break;
		case 1: scopeLine = items.length == 0 ? res.getDisplay("scope.document") : 
			    (res.getDisplay("label.document") + ": " + items[0].getDocument().getShortTitle()); 
		        break;
		case 2: scopeLine = res.getDisplay("scope.article"); break;
		}
		if (scopeLine != null) {
			label = new JLabel(scopeLine);
			comPanel.add(label);
		}
		
		// construct the main panel
		setPreferredSize(new Dimension(400, 250));
		setLayout(new BorderLayout(0, 10));
		add(comPanel, BorderLayout.NORTH);
		scrollPane = new JScrollPane(table);
		add(scrollPane);
	}
	
	/** Adds a list selection listener that is notified each time a change
	 * in table row selection occurs.
	 * 
	 * @param x {@code ListSelectionListener}
	 */
	public void addListSelectionListener (ListSelectionListener x) {
		table.getSelectionModel().addListSelectionListener(x);
	}
	
	@Override
	public void grabFocus() {
		table.grabFocus();
	}

	/** Sets the list of document text positions to be displayed in this panel.
	 * 
	 * @param positions {@code DocumentTextPosition[]}
	 */
	private void setTextPositions (DocumentTextPosition[] positions) {
		Objects.requireNonNull(positions);
		items = positions;
		model.fireTableDataChanged();
	}
	
	/** Returns the currently selected text position or null if there is 
	 * nothing selected.
	 * 
	 * @return {@code DocumentTextPosition} or null
	 */
	public DocumentTextPosition getSelectedItem () {
		DocumentTextPosition position = null;
		int index = table.getSelectedRow();
		if (index > -1) { 
			try {
				position = model.getTextPosition(index);
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
		return position;
	}
	
	private class OurTableModel extends AbstractTableModel {
		private int columns;
		
		OurTableModel () {
			columns = scope == 0 ? 3 : 2;
		}
		
//		/** Counts the number of different documents in the list of text
//		 * positions.
//		 * 
//		 * @return int
//		 */
//		private int countDocuments() {
//			int count = 0;
//			Map<PadDocument, Object> map = new HashMap<>();
//			
//			for (DocumentTextPosition pos : items) {
//				if (!map.containsKey(pos.getDocument())) {
//					map.put(pos.getDocument(), null);
//					count++;
//				}
//			}
//			return count;
//		}

		@Override
		public int getColumnCount() {return columns;}

		@Override
		public int getRowCount() {return items.length;}

		@Override
		public Object getValueAt (int rowIndex, int columnIndex) {
			DocumentTextPosition pos = items[rowIndex];
			if (columns == 2) {
				columnIndex++;
			}
			Object obj;
			switch (columnIndex) {
			case 0:  obj = pos.getDocument().getShortTitle(); break;
			case 1:  obj = pos.getArticle().getShortTitle(); break;
			case 2:  obj = pos.getCursorPos(); break;
			default: obj = "";
			}
			return obj;
		}
		
		/** Returns the {@code DocumentTextPosition} which is associated with
		 * the given model index.
		 * 
		 * @param row int model index
		 * @return {@code DocumentTextPosition}
		 * @throws IndexOutOfBoundsException
		 */
		public DocumentTextPosition getTextPosition (int row) {
			if (row < 0 | row >= items.length)
				throw new IndexOutOfBoundsException("bad model index: " + row);
			return items[row];
		}
	}
}
