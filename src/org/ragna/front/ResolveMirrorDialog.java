/*
*  File: ResolveMirrorDialog.java
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
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.ragna.core.ActionHandler;
import org.ragna.core.PadDocument;
import org.ragna.front.util.ButtonBarDialog;
import org.ragna.front.util.ButtonBarListener;
import org.ragna.io.IO_Manager;

import kse.utilclass.gui.VerticalFlowLayout;
import kse.utilclass.misc.Util;


public class ResolveMirrorDialog extends ButtonBarDialog implements ButtonBarListener {

   // dialog result values
   public static final int REPLACE_ACTION = 1;
   public static final int DELETE_ACTION = 2;
   public static final int SHOW_ACTION = 3;
   public static final int NO_ACTION = 0;

   private File origFile;
   private JRadioButton deleteButton, actionButton, openButton;
   private int action;

   /**
    * Constructs a mirror resolving dialog to obtain user's choice of action.
    * 
    * @param document {@code PadDocument} the reference document
    * @param mirror File the mirror file
    * @throws IllegalArgumentException if the original file was not found (undefined)
    * @throws IOException if an IO error occurred 
    */
   public ResolveMirrorDialog (PadDocument document, File mirror)  throws IllegalArgumentException, IOException {
	  super();
      setModal(true);
      init(document, mirror);
   }

   private void init (PadDocument document, File mirror) throws IOException {
	  Objects.requireNonNull(document, "document is null");
	  Objects.requireNonNull(mirror, "mirror is null");
      ButtonGroup grp;
      JPanel pane, panel;
      JLabel msgLabel;
      String text;
      
      // dialog init
      setTitle(ActionHandler.displayText("dlg.title.mirror.manager"));
      addButtonBarListener( this );
      
      // get the document's current storage file
      IO_Manager ioMan = IO_Manager.get();
      String path = document.getExternalPath();
      if (path == null) {
          throw new IllegalArgumentException("no storage file defined for: " + document.getShortTitle());
      }
      origFile = new File(path);

      // identify recommended action
      boolean isReplacer = mirror.lastModified() > origFile.lastModified();
      action = NO_ACTION;
      
      // framework
      setTitle( ActionHandler.displayText("dlg.mirror.resolve") );
      pane = new JPanel( new VerticalFlowLayout( 20 ) );
      pane.setBorder( BorderFactory.createEmptyBorder( 20, 30, 0, 30 ) );
      
      // message text
      text = ActionHandler.displayText("msg.mirror.resolve");
         
      text = Util.substituteText( text, "$dbname", document.getShortTitle() );
      text = Util.substituteText( text, "$dbpath", path );
      text = Util.substituteText( text, "$time-m", Util.localeTimeString(mirror.lastModified()) );
      text = Util.substituteText( text, "$time-o", Util.localeTimeString(origFile.lastModified() ));
      text = Util.substituteText( text, "$t-color", isReplacer ? "blue" : "red" );
      msgLabel = new JLabel( text );
      msgLabel.setFont( msgLabel.getFont().deriveFont(Font.PLAIN) );
      pane.add( msgLabel );
      
      // create radio buttons
      panel = new JPanel( new VerticalFlowLayout(10) );
      pane.add( panel );
      grp = new ButtonGroup();
	  actionButton = new JRadioButton( ActionHandler.displayText("radio.resolvemirror.replace") );
      deleteButton = new JRadioButton( ActionHandler.displayText("radio.resolvemirror.delete") );
      openButton = new JRadioButton( ActionHandler.displayText("radio.resolvemirror.open") );
      if (isReplacer) {
    	  actionButton.setSelected(true);
          grp.add( actionButton );
          panel.add( actionButton );
      } else {
    	  deleteButton.setSelected(true);
      }
      grp.add( deleteButton );
      grp.add( openButton );
      panel.add( deleteButton );
      panel.add( openButton );

      setDialogPanel( pane );
   }
   
   /** Returns the user's choice of this dialog. Possible values are the 
    * "_ACTION" constants of this class.
    * 
    * @return int dialog termination option or NO_ACTION if cancelled
    */
   public int getUserOption () {
	   return action;
   }

   /** Returns the original document external file definition.
    * 
    * @return File document storage file
    */
   public File getOriginalFile () {
	   return origFile;
   }
   
   // ******** IMPLEMENTS ButtonBarListener ***************

   @Override
   public boolean extraButtonPerformed ( Object button ) {return false;}
   
   @Override
   public void noButtonPerformed () {}

   @Override
   public void helpButtonPerformed () {}

   @Override
   public void cancelButtonPerformed () {
      dispose();
   }

   @Override
   public boolean okButtonPerformed () {
      action = actionButton.isSelected() ? REPLACE_ACTION : deleteButton.isSelected() ? DELETE_ACTION : SHOW_ACTION;
      dispose();
      return true;
   }

}  // class ResolveMirrorDialog
