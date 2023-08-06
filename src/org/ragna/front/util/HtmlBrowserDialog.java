/*
*  File: HtmlBrowserDialog.java
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
import java.awt.HeadlessException;
import java.awt.Window;
import java.io.IOException;
import java.net.URL;

import org.ragna.core.Global;
import org.ragna.util.PersistentOptions;


/**
 *  Dialog to display and browse a HTML text document.
 */
public class HtmlBrowserDialog extends ButtonBarDialog {
	
   private HtmlDialogPane htmlPane;
   private String boundsToken; 
   private boolean boundsRelative;

/**
 * Creates a non-modal dialog without owner and title.
 * 
 * @throws java.awt.HeadlessException
 */
public HtmlBrowserDialog () throws HeadlessException {
	this(null, false);
}

/**
 * Creates a dialog with owner and modality as given.
 * 
 * @param owner Window
 * @param modal boolean
 * @throws java.awt.HeadlessException
 */
public HtmlBrowserDialog ( Window owner, boolean modal ) throws HeadlessException {
   super( owner, DialogButtonBar.CLOSE_BUTTON, modal );
   init();
}

/**
 * Creates a dialog with owner, title and modality as given.
 * 
 * @param owner
 * @param title
 * @param modal
 * @throws java.awt.HeadlessException
 */
public HtmlBrowserDialog ( Window owner, String title, boolean modal ) throws HeadlessException {
   this( owner, modal );
   setTitle( title );
}

private void init () {
   htmlPane = new HtmlDialogPane();
   // TODO HyperlinkListener
//   htmlPane.addHyperlinkListener( Global.createHyperLinkListener() );
   htmlPane.setPreferredSize( new Dimension(600,400) );
   setResizable( true );
   setDialogPanel( htmlPane );
}

/** Returns the URL of the current editor page or null if the current page is
 * empty or not loaded from an URL.
 * 
 * @return {@code URL} or null
 */
public URL getUrl () {return htmlPane.getUrl();}

/** Gets the type of content that this dialog is currently set to deal with.
 *  
 * @return String
 */
public String getContentType () {
	return htmlPane.getContentType();
}

/*
public void setVisible ( boolean v )
{
   if ( v )
      Util.centreWithin( this.getOwner(), this );
   super.setVisible( v );
}
*/
/** Sets the text of this dialog to the specified content, which is expected 
 * to be in the format of the content type of this editor.
 *  
 * @param text String
 */
public void setText ( String text ) {
   htmlPane.setText( text );
}

/** Sets the current URL being displayed. The content type of the pane is set
 * according to the type of the referenced document. This may cause, but not
 * necessarily, asynchronous loading.
 * 
 * @param page {@code URL}
 * @throws IOException
 */
public void setPage ( URL page ) throws IOException {
   htmlPane.setPage( page );
}

/**
 * If <code>token</code> is not <b>null</b> this activates
 * persistent storage and recovery of this dialog's bounds
 * in global options.
 * 
 * @param token String name of bounds record
 * @param relative if <b>true</b> bounds are seen in relation to parent window
  *        otherwise absolute 
 */
public void setBoundsToken( String token, boolean relative ) {
   boundsRelative = relative;
   boundsToken = token;
}

@Override
public void show () {
   PersistentOptions options = Global.getOptions();
   if ( boundsToken != null && options.isOptionSet( "rememberScreen" ) ) {
      gainBounds( options, boundsToken, boundsRelative );
   }
   super.show();
}

@Override
public void dispose () {
   PersistentOptions options = Global.getOptions();
   if ( boundsToken != null && options.isOptionSet( "rememberScreen" ) ) {
      storeBounds( options, boundsToken, boundsRelative );
   }
   super.dispose();
}

}
