/*
*  File: ShowWindowsRunnable.java
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

import java.awt.Dialog;
import java.awt.Window;
import java.util.Collection;

import javax.swing.SwingUtilities;

import org.ragna.front.GUIService;

import kse.utilclass.misc.Log;
import kse.utilclass.sets.ArraySet;

public class ShowWindowsRunnable implements Runnable   {
   Window child;
   
   public ShowWindowsRunnable ( Window child ) {
      this.child = child;
   }

   @SuppressWarnings("deprecation")
   public void run () {
      if ( !child.isShowing() ) {
         Log.log( 8, "(ShowWindowsRunnable.run) *** Window Children SHOW OPERATION : " + child.getName() );
         child.show();
      }
   }
   
   /**
    * Evaluates a set of windows and makes them show through the EDT
    * synchronically where possible and by  
    * <code>SwingUtilities.invokeLater()</code> for modal Dialog elements.
    * Windows that are already showing are ignored.
    * 
    * @param windows set of element type <code>Window</code>
    */
   @SuppressWarnings("unchecked")
   public static void showWindowsLater ( Collection<Window> windows ) {
      ArraySet<Window> wins = new ArraySet<Window>(windows);
      ArraySet<Window> winsCopy = (ArraySet<Window>)wins.clone();

      // sort out showing windows
      for ( Window win : winsCopy ) {
         if ( win.isShowing() ) {
            wins.remove(win);
         }
      }
      
      // in the standard case all windows are showing and we can break the routine
      if ( wins.isEmpty() ) {
         return;
      }
      
      // first level: show all windows which are not dialogs
      winsCopy = (ArraySet<Window>)wins.clone();
      for ( Window win : winsCopy ) {
         if ( !(win instanceof Dialog) ) {
            GUIService.executeOnEDT( new ShowWindowsRunnable( win ) );
            wins.remove(win);
         }
      }

      // second level: show all non-modal dialogs
      winsCopy = (ArraySet<Window>)wins.clone();
      for ( Window win : winsCopy ) {
         if ( !((Dialog)win).isModal() ) {
            GUIService.executeOnEDT( new ShowWindowsRunnable( win ) );
            wins.remove(win);
         }
      }

      // third level: show all modal dialogs
      for ( Window win : wins ) {
         // we use "invokeLater" here to give the calling task a chance
         // to perform before the first modal dialog appears
         SwingUtilities.invokeLater( new ShowWindowsRunnable( win ) );
      }
   }
}