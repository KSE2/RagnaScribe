/*
*  File: Ragna.java
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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;

import org.ragna.core.Global;
import org.ragna.core.PadDocument;
import org.ragna.front.util.MightyFrame;
import org.ragna.io.IO_Manager;

import kse.utilclass.misc.Util;

public class Ragna extends MightyFrame {
   private static final String BOUNDS_TOKEN = "RagnaFrameBounds";
   private String          titleTrunk;
   private PropertyListener propertyListener = new PropertyListener();

   public Ragna () {
      super();
      
      // frame head attributes
      titleTrunk = Global.APPLICATION_TITLE.concat("  ");
      setTitle( titleTrunk);
      setIconImage( Global.res.getImageIcon( "system.logo" ).getImage() );
      setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
      Global.getDocumentRegistry().addPropertyChangeListener(propertyListener);
   }
   
   /**
    * @param args
    */
   public static void main (String[] args) {
      Global.main(args);
   }

   /** Decomposes and shuts down the main window and its components.
    */
   public void exit () {
      Rectangle bounds = getBounds();
      Global.getOptions().setBounds(BOUNDS_TOKEN, bounds);
   }

   public void gainMemoryBounds () {

      // obtain previous session bounds
      Rectangle bounds = Global.getOptions().getBounds( BOUNDS_TOKEN );
      
      if ( bounds == null ) {
         // create default bounds in centre of screen
         Point framePos = centredWindowLocation( 
               new Rectangle(Global.getScreenSize()), Global.DEFAULT_FRAME_DIM );
         bounds = new Rectangle( framePos, Global.DEFAULT_FRAME_DIM );
      }
      
      // activate mainframe bounds including corrections related to screen size
      bounds = Util.correctedWindowBounds( bounds, true, false );
      setBounds( bounds );
      Global.getOptions().setBounds( BOUNDS_TOKEN, bounds );
   }

   /**
    * Calculates the X and Y coordinates that a child component should have to be centred
    * within its parent.  This method does not try to constrain the calculation so that
    * the point remains on visible screen.
    * 
    * @param rec  the bounding rectangle of the parent.
    * @param dim  the dimensions of the child.
    * 
    * @return the X and Y coordinates the child should have.
    */
   public static Point centredWindowLocation( Rectangle rec, Dimension dim ) {
       Point   pos;

       pos     = new Point();
       pos.x   = rec.x + ((rec.width - dim.width) >> 1);
       pos.y   = rec.y + ((rec.height - dim.height) >> 1);
       
       return pos;
   }

   private void showProgramTitle (PadDocument document) {
      String docTitle = "";
      if (document != null) {
         docTitle = document.getTitle();

         // if doc title is empty OR global option is set 
         // take the filepath instead
         if (docTitle.isEmpty() || 
             Global.getOptions().isOptionSet("showFilePathInsteadOfTitle")) {
            String path = IO_Manager.get().getExternalFileReference(document.getUUID());
            docTitle = path == null ? docTitle : path;  
         }
         
         // prefix asterisk if document is modified
         if (document.isModified()) {
            docTitle = "* ".concat(docTitle);
         }
      }
      
      // render and show new title
      String newTitle = titleTrunk.concat(docTitle);
      setTitle(newTitle);
   }
   
   @Override
   public void processWindowEvent( WindowEvent e ) {
   //      System.out.println( "-- Mainframe Window Event: " + e.getID() );
      switch ( e.getID() ) {
      case WindowEvent.WINDOW_CLOSING :
         Global.exit();
         return;
      }
   }

private class PropertyListener implements PropertyChangeListener {
     PadDocument liDocument;

     @Override
     public void propertyChange (PropertyChangeEvent evt) {
        String key = evt.getPropertyName();
        
        // reaction to DOCUMENT-SELECTED event (Document Registry)
        if (key == "documentSelected") {
           // display title of selected document
           PadDocument document = (PadDocument)evt.getNewValue();
           showProgramTitle(document);
           
           // remove a previous document from listener
           if (liDocument != null) {
              liDocument.removePropertyChangeListener("documentModified", this);
              liDocument.removePropertyChangeListener("titleChanged", this);
           }
           
           // listen to selected document
           if (document != null) {
              liDocument = document;
              liDocument.addPropertyChangeListener("documentModified", this);
              liDocument.addPropertyChangeListener("titleChanged", this);
           }

           // reaction to DOCUMENT events
        }  else if (key == "documentModified") {
           showProgramTitle(liDocument);
        }  else if (key == "titleChanged") {
           showProgramTitle(liDocument);
        }
    }
  }
  
}
