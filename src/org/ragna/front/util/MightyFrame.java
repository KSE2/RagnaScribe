/*
*  File: MightyFrame.java
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

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.Window;
import java.util.Set;

import javax.swing.JFrame;

import kse.utilclass.misc.Log;
import kse.utilclass.sets.ArraySet;

public class MightyFrame extends JFrame implements ChildWindowKeeper {

   /** List of child windows (which are of the same type/class) */
   private ArraySet<Window> childWindows;

   public MightyFrame() throws HeadlessException {
   }

   public MightyFrame(GraphicsConfiguration gc) {
      super(gc);
   }

   public MightyFrame(String title) throws HeadlessException {
      super(title);
   }

   public MightyFrame(String title, GraphicsConfiguration gc) {
      super(title, gc);
   }

   @Override
   public synchronized void addChildWindow ( Window win ) {
      if ( win != null ) {
         // late instantiation of child window array list 
         if ( childWindows == null ) {
            childWindows  = new ArraySet<Window>();
         }
         
         synchronized (childWindows) {
            if ( childWindows.add( win ) ) {
               Log.log( 7, "(MightyFrame.addChildWindow) added child window : " + win.getName() );
            }
         }
      }
   }
   
   @Override
   public void removeChildWindow ( Window win ) {
      if (childWindows != null) {
         synchronized (childWindows) {
            if ( childWindows.remove( win ) )
               Log.log( 7, "(MightyFrame.addChildWindow) removed child window : " + win.getName() );
         }
      }
   }

   
   @Override
   public void removeAllChildWindows() {
      synchronized (childWindows) {
         childWindows.clear();
      }
   }

   @SuppressWarnings("unchecked")
   @Override
   public Set<Window> getChildWindowSet() {
      synchronized (childWindows) {
         return (Set<Window>)childWindows.clone();
      }
   }

   @Override
   public void show () {
      Log.log( 10, "(MightyFrame.show) enter SHOW for: " + getTitle()); 

      // attempt make all child windows showing
      if ( childWindows != null )
         ShowWindowsRunnable.showWindowsLater( childWindows );
               
      super.show();
      Log.log( 10, "(MightyFrame.show) leaving SHOW for: " + getTitle()); 
   }
   
}
