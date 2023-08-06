/*
*  File: FontChooser.java
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.ragna.front.GUIService;

public class FontChooser extends ButtonBarDialog {
  /** Available font styles. */
  private static final String[] STYLES = new String[] {"Plain", "Bold", "Italic", "Bold Italic"};

  /** Available font sizes. */
  private static final String[] SIZES =
      new String[] { "3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
                     "13", "14", "15", "16", "17", "18", "19", "20", "22",
                     "24", "27", "30", "34", "39", "45", "51", "60" };

  // Lists to display
  private NwList _styleList;
  private NwList _fontList;
  private NwList _sizeList;

  // Swing elements
  private JLabel _sampleText = new JLabel();

  /**
   * Constructs a new modal FontChooser for the given frame,
   * using the specified font.
   */
  private FontChooser (Window parent, Font font) {
    super(parent, DialogButtonBar.OK_CANCEL_BUTTON, true);
    initAll(font);
  }

/**
   * Method used to show the font chooser, and select a new font.
   *
   * @param parent Window the parent window 
   * @param title  The title for this window.
   * @param font   The previously chosen font.
   * 
   * @return the newly chosen font or <b>null</b> if dialog was cancelled
   */
  public static Font showDialog (Window parent, String title, Font font) {
	 Font chosenFont[] = new Font[1];
	 Runnable run = new Runnable() {
		@Override
		public void run() {
		    FontChooser fd = new FontChooser(parent, font);
		    fd.setTitle(title);
		    fd.setVisible(true);

		    if ( fd.isOkPressed() ) {
		      chosenFont[0] = fd.getFont();
		    }
		}
	 };

	try {
		GUIService.executeOnEDT_Wait(run);
	} catch (InvocationTargetException | InterruptedException e) {
	}
    return chosenFont[0];
  }

  private void initAll ( Font font ) {
     JPanel mainPanel, listPanel, panel;
     String fName;
    
     setResizable(true);
    ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    
    mainPanel = new JPanel(new BorderLayout());
    
    // sample text label
    _sampleText = new JLabel();
    _sampleText.setForeground(Color.black);
    _sampleText.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
    mainPanel.add(_sampleText, BorderLayout.SOUTH);

    // add lists
    _fontList = new NwList( GraphicsEnvironment.getLocalGraphicsEnvironment()
          .getAvailableFontFamilyNames() );
    _styleList = new NwList(STYLES);
    _sizeList = new NwList(SIZES);

    listPanel = new JPanel( new BorderLayout( 3, 3 ) );
    listPanel.setPreferredSize( new Dimension( 400, 250 ) );
    mainPanel.add( listPanel, BorderLayout.CENTER );

    panel = new JPanel( new BorderLayout( 3, 3 ) );
    listPanel.add( _fontList, BorderLayout.CENTER );
    panel.add( _styleList, BorderLayout.CENTER );
    panel.add( _sizeList, BorderLayout.EAST );
    listPanel.add( panel, BorderLayout.EAST );

    if (font == null) {
       font = _sampleText.getFont();
    }
    
    fName = font.getFamily();
    if ( fName.equals( "dialog" ) ) {
       fName = "Dialog";
    }
    _fontList.setSelectedItem( fName );
    _sizeList.setSelectedItem( String.valueOf( font.getSize() ));
    _styleList.setSelectedItem( STYLES[font.getStyle()] );

    setDialogPanel( mainPanel );
  }  // initAll

  private void showSample () {
    int g = 0;
    try {
      g = Integer.parseInt(_sizeList.getSelectedValue());
    } catch (NumberFormatException nfe) {
    }
    String st = _styleList.getSelectedValue();
    int s = Font.PLAIN;
    if (st.equalsIgnoreCase("Bold")) s = Font.BOLD;
    if (st.equalsIgnoreCase("Italic")) s = Font.ITALIC;
    if (st.equalsIgnoreCase("Bold Italic")) s = Font.BOLD | Font.ITALIC;
    _sampleText.setFont(new Font(_fontList.getSelectedValue(), s, g));
    _sampleText.setText("The Quick Brown Fox jumped over the lazy White Snake");
    _sampleText.setVerticalAlignment(SwingConstants.TOP);
  }

  /**
   * Returns the currently selected Font.
   */
  @Override
  public Font getFont () {
     return _sampleText == null ? null : _sampleText.getFont();
  }

  /**
   * Private inner class for a list which displays something else in addition to a label
   * indicating the currently selected item.
   */
  public class NwList extends JPanel {
	  
    Border border = BorderFactory.createEmptyBorder( 1, 5, 1, 5 );  
    ListCellRenderer<Object> cellRenderer = new DefaultListCellRenderer() {

    @Override
	public Component getListCellRendererComponent 
			( JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus ) {
         JLabel c = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
         c.setBorder(border);
         return c;
      }
    };
    JList<?> jlist;
    JScrollPane scroll;
    JLabel jt;
    String si = " ";

    public NwList(String[] values) {
      setLayout( new BorderLayout( 0, 3 ) );

      jlist = new JList<Object>(values);
      jlist.setCellRenderer( cellRenderer );
      scroll = new JScrollPane( jlist );
      
      jt = new JLabel();
      jt.setBackground(Color.white);
      jt.setForeground(Color.black);
      jt.setOpaque(true);
      jt.setBorder(new JTextField().getBorder());
      jt.setFont(getFont());
      
      jlist.addListSelectionListener(new ListSelectionListener() {
        @Override
		public void valueChanged(ListSelectionEvent e) {
           String text = (String)jlist.getSelectedValue();
          jt.setText( text );
          si = text;
          showSample();
        }
      });
      
      add(scroll, BorderLayout.CENTER );
      add(jt, BorderLayout.NORTH );
    }

    public String getSelectedValue() {
       return si;
    }

    public void setSelectedItem(String s) {
       jlist.setSelectedValue(s, true);
    }

  }
}
