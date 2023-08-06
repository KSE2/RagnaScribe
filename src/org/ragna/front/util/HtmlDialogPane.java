/*
*  File: HtmlDialogPane.java
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
import java.io.IOException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkListener;

/**
 *  A <code>JPanel</code> containing a scrollable HTML browsing facility based
 *  on <code>JEditorPane</code>.
 */
public class HtmlDialogPane extends JPanel {
   
   private JEditorPane editor;
   private JScrollPane scroll;
   private URL url;
   
/**
 * Constructs an empty scrollable HTML display panel. 
 */
public HtmlDialogPane () {
   super();
   init( true );
}

/**
 * Constructs an empty HTML display panel whose scrollability can be determined
 * by parameter.
 * 
 * @param scrollable whether html display will be an element of a <code>JScrollPane</code> 
 */
public HtmlDialogPane ( boolean scrollable ) {
   super();
   init( scrollable );
}

/** Constructs a scrollable HTML display panel with initial display text.
 * @param text initial text to be displayed
 */
public HtmlDialogPane ( String text ) {
   super();
   init( true );
   editor.setText( text );
}

/** Constructs a scrollable HTML display panel with initial display text
 *  fetched from the given URL.
 * 
 * @param url initial page to be displayed
 * @throws IOException if the given url cannot be accessed
 */
public HtmlDialogPane ( URL url ) throws IOException {
   super();
   init( true );
   editor.setPage( url );
}

private void init ( boolean scrollable ) {
   BorderLayout layout;
   Component component;
   
   layout = new BorderLayout();
   setLayout( layout );
   
   
   editor = new JEditorPane();
   editor.setBorder( BorderFactory.createEmptyBorder( 4, 10, 4, 0 ) );
   editor.setEditable( false );
   editor.setBackground(Color.white);
   editor.setContentType( "text/html;charset=ISO-8859-1" );
   component = editor;
   
   if ( scrollable ) {
      scroll = new JScrollPane( editor );
      scroll.setHorizontalScrollBarPolicy( 
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED  );
      scroll.setVerticalScrollBarPolicy( 
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED ); 
      component = scroll;
   }
   
   this.add( component, BorderLayout.CENTER );
}  // init

public JScrollPane getScrollPane () {
   return scroll;
}

/** Returns the URL of the current editor page or null if the current page is
 * empty or not loaded from an URL.
 * 
 * @return {@code URL} or null
 */
public URL getUrl () {return url;}

/** Gets the type of content that this editor is currently set to deal with.
 *  
 * @return String
 */
public String getContentType () {
	return editor.getContentType();
}

public JEditorPane getEditorPane () {
   return editor;
}

/** Sets the text of this dialog to the specified content, which is expected 
 * to be in the format of the content type of this editor.
 *  
 * @param text String
 */
public void setText ( String text ) {
   editor.setText( text );
   editor.setCaretPosition(0);
}

/** Sets the current URL being displayed. The content type of the pane is set
 * according to the type of the referenced document. This may cause, but not
 * necessarily, asynchronous loading.
 * 
 * @param page {@code URL}
 * @throws IOException
 */
public void setPage ( URL page ) throws IOException {
   editor.setPage( page );
   url = page;
}

public void addHyperlinkListener( HyperlinkListener hyperListener )
{
   editor.addHyperlinkListener( hyperListener );
}

public void removeHyperlinkListener( HyperlinkListener hyperListener ) {
   editor.removeHyperlinkListener( hyperListener );
}

//@Override
//public void setBackground(Color bg) {
//	super.setBackground(bg);
//	if (editor != null) {
//	   editor.setBackground(bg);
//	}
//}
//
//@Override
//public void setForeground(Color bg) {
//	super.setForeground(bg);
//	if (editor != null) {
//	   editor.setForeground(bg);
//	}
//}


}
