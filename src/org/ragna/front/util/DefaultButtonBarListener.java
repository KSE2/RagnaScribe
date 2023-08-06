/*
*  File: DefaultButtonBarListener.java
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

import org.ragna.front.GUIService;

public class DefaultButtonBarListener implements ButtonBarListener
{
   private ButtonBarDialog dialog;
   private DialogButtonBar buttonBar;
   private String          helpCode;


   public DefaultButtonBarListener () {
   }

/**
 * 
 * @param buttonBar DialogButtonBar
 */
public DefaultButtonBarListener ( DialogButtonBar buttonBar ) {
   this.buttonBar = buttonBar;
}

/**
 * Creates a bar-listener with reference to a dialog's button bar.
 * 
 * @param dialog <code>ButtonBarDialog</code>
 */
public DefaultButtonBarListener ( ButtonBarDialog dialog ) {
   if ( dialog != null )
      this.buttonBar = dialog.getButtonBar();
}

/**
 * Creates a bar-listener with reference to a dialog's button bar
 * and by defining a help dialog associated. The help-ID refers to 
 * a text element in DISPLAY bundle. 
 * 
 * @param dialog <code>ButtonBarDialog</code> dialog containing the button bar
 * @param helpCode <code>String</code> the id-string of the help dialog 
 *                 available through the button bar; may be <b>null</b>   
 */
public DefaultButtonBarListener ( ButtonBarDialog dialog, String helpCode )
{
   if ( dialog != null )
   {
      this.dialog = dialog;
      this.buttonBar = dialog.getButtonBar();
      this.helpCode = helpCode;
   }
}

@Override
public boolean okButtonPerformed ()
{
   if ( buttonBar != null )
      buttonBar.disposeDialog();
   
   return true;
}

@Override
public void noButtonPerformed ()
{
   cancelButtonPerformed();
}

@Override
public void cancelButtonPerformed ()
{
   if ( buttonBar != null )
      buttonBar.disposeDialog();
}

@Override
public void helpButtonPerformed ()
{
   if ( dialog != null & helpCode != null )
   {
      GUIService.toggleHelpDialog( dialog, helpCode );
   }
}

@Override
public boolean extraButtonPerformed ( Object button )
{
   return true;
}

}
