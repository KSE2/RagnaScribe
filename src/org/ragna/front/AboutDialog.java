/*
*  File: AboutDialog.java
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
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.io.IOException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkListener;

import org.ragna.core.ActionHandler;
import org.ragna.core.Global;
import org.ragna.exception.DuplicateException;
import org.ragna.front.util.ButtonBarDialog;
import org.ragna.front.util.DialogButtonBar;
import org.ragna.front.util.ResourceLoader;

import kse.utilclass.misc.Log;

/**
 *  The "About" dialog of Ragna.
 * 
 * @author Wolfgang Keller
 */
public class AboutDialog extends ButtonBarDialog {
	
   private JEditorPane    epane = new JEditorPane();
   private JPanel         content;
   private HyperlinkListener hyperListener;
   
	/**
     * Constructor.
	 * @throws java.awt.HeadlessException
	 * @throws DuplicateException if this dialog is already showing
	 */
	public AboutDialog() throws HeadlessException, DuplicateException {
		super( Global.getActiveFrame(), DialogButtonBar.OK_BUTTON, false );
		setAutonomous( true );
		if (!markSingleton("ProgramAboutDialog")) {
			throw new DuplicateException();
		}
		setTitle( ActionHandler.displayText("dlg.title.about") );
        content = new JPanel( new BorderLayout() );

		buildTopPanel();
		buildCentrePanel();

        setDialogPanel(content);
		setVisible(true);
	}

   private void destruct () {
      epane.removeHyperlinkListener( hyperListener );
      hyperListener = null;
   }

   @Override
   public void dispose () {
      super.dispose();
      destruct();
   }
   
   private void buildCentrePanel() {
      JScrollPane scroll;
      URL page;
      String hstr;

      hyperListener = createHyperLinkListener();

      epane.setPreferredSize( new Dimension(600,412) );
      epane.setBorder( new EmptyBorder( 0, 40, 0, 40 ) );
      epane.setEditable( false );
      epane.setOpaque( false );
      epane.setContentType( "text/html;charset=UTF-8" );
      epane.addHyperlinkListener( hyperListener );

      try {
         hstr = ResourceLoader.get().getCommand( "html.file.about" );
         page = ResourceLoader.get().getResourceURL( "#system/".concat(hstr));
         
         Log.log(5, "(AboutDialog) loading into JEditorPane: " + page );
         epane.setPage( page );  
      } catch ( IOException e ) {
         epane.setContentType( "text/plain" );
         epane.setText( e.toString() );
      }

      scroll = new JScrollPane( epane );
      scroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
      content.add(scroll, BorderLayout.CENTER);
   }

   
	private void buildTopPanel() {
/*      
      ImageIcon   icon;
      JButton     logo;

   		icon	= ResourceLoader.getImageIcon("logo");
		logo	= new JButton( icon );

		logo.setDisabledIcon( icon );
		logo.setEnabled( false );
		logo.setBorder( new EmptyBorder( 6, 0, 0, 0 ) );

		content.add( logo, BorderLayout.NORTH );
*/      
	}

	public HyperlinkListener createHyperLinkListener () {
       return Global.createHyperLinkListener();
    }
}
