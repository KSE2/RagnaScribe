/*
*  File: ColorChooserDialog.java
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

import javax.swing.BorderFactory;

import org.ragna.core.Global;
import org.ragna.front.util.ButtonBarDialog;
import org.ragna.front.util.DialogButtonBar;

import kse.utilclass.gui.ColorChooserPanel;
import kse.utilclass.misc.Log;

public class ColorChooserDialog extends ButtonBarDialog {

	protected Color[] colors;
	protected ColorChooserPanel cpanel;
	protected Color initialColor;
	protected Color selectedColor;
	
	/** Creates a modal chooser dialog w/o title and owner.
	 * 
	 * @param colors Color[] set of color options
	 * @param initialColor Color initial color option, may be null
	 */
	public ColorChooserDialog (Color[] colors, Color initialColor) {
		this(null, null, colors, initialColor);
	}
	
	/** Creates a modal chooser dialog with the given title and owner.
	 * 
	 * @param owner Window
	 * @param title String dialog title
	 * @param colors Color[] set of color options
	 */
	public ColorChooserDialog (Window owner, String title, Color[] colors, Color initialColor) {
		super(owner, DialogButtonBar.BUTTONLESS, true);
		Objects.requireNonNull(colors, "colors is null");
		setTitle(title);
		this.colors = colors;
		this.initialColor = initialColor;
		init();
	}

	private void init() {
		cpanel = new ColorChooserPanel(new Dimension(70, 30), colors, 10);
		cpanel.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
		cpanel.setSelectorActive(Global.res.getCommand("button.colorchooser"), 
				  Global.res.getDisplay("dlg.title.jcolorchooser"),	initialColor);
		
		cpanel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectedColor = cpanel.getSelectedColor();
				Log.debug(10, "(ColorChooserDialog) selected Color = " + selectedColor);
				dispose();
			}
		});
		setDialogPanel(cpanel);
	}
	
	/** Returns the user selected color or null if nothing was selected.
	 * 
	 * @return {@code Color} or null
	 */
	public Color getSelectedColor () {return selectedColor;}
	
	/** Shows a modal color chooser dialog and returns the user opted Color
	 * or null if no color was chosen.
	 *   
	 * @param parent Component display relation of dialog, may be null
	 * @param title String title of dialog, may be null for default
	 * @param colors Color[] set of color options
	 * @param initialColor Color any color or null
	 * @return Color or null
	 */
	public static Color showDialog (Component parent, String title, Color[] colors, Color initialColor) {
		if (title == null) {
			title = Global.res.getDisplay("dlg.title.choose.color");
		}
		ColorChooserDialog dlg = new ColorChooserDialog(null, title, colors, initialColor);
		dlg.setLocationRelativeTo(parent == null ? dlg.getOwner() : parent);
		dlg.show();
		return dlg.getSelectedColor();
	}
}
