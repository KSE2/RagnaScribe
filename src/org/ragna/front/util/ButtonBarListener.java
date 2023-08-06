/*
*  File: ButtonBarListener.java
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


/**
 * Defines a listener to a {@link org.jpws.front.util.DialogButtonBar}.
 */
public interface ButtonBarListener {

   /** Performs actions associated to the "OK" button
    * of the button bar. The return value is normally <b>false</b>
    * if some pre-condition of operation has failed, including
    * a possible abortion by the user. 
    * 
    * @return boolean <b>true</b> indicating that operations were performed
    *         as regarded "normal", <b>false</b> indicating that operation has
    *         been broken by some cause
    */
   boolean okButtonPerformed ();
   
   void cancelButtonPerformed ();
   void noButtonPerformed ();
   void helpButtonPerformed ();
   
   /**
    * Performs a user supplied (additional) button. 
    * The returned boolean controls whether the button will be 
    * enabled or disabled after termination of this method.
    * 
    * @param button
    * @return boolean button enable status (<b>true</b> = enabled)
    * @since 0-4-0
    */
   boolean extraButtonPerformed ( Object button );

}
