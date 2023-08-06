/*
*  File: PadFileChooser.java
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

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;

import kse.utilclass.gui.AutomoveAdapter;

public class PadFileChooser extends JFileChooser {

   public PadFileChooser() {
   }

   public PadFileChooser(String currentDirectoryPath) {
      super(currentDirectoryPath);
   }

   public PadFileChooser(File currentDirectory) {
      super(currentDirectory);
   }

   @Override
   protected JDialog createDialog (Component parent) throws HeadlessException {
      JDialog dialog = super.createDialog(parent);
      new AutomoveAdapter(parent, dialog);
      return dialog;
   }

}
