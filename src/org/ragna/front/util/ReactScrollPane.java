/*
*  File: ReactScrollPane.java
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

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JScrollPane;
import javax.swing.JViewport;

import kse.utilclass.misc.Log;

/** This {@code JScrollPane} reacts to the VISIBLE status of its view component
 * and makes itself visible or invisible following the view.
 * 
 * ** NOT USED **
 */
public class ReactScrollPane extends JScrollPane {
	private Component view;
	private ComponentListener clistener;

	public ReactScrollPane() {
	}

	public ReactScrollPane (Component view) {
		super(view);
		init();
	}

	public ReactScrollPane (int vsbPolicy, int hsbPolicy) {
		super(vsbPolicy, hsbPolicy);
		init();
	}

	public ReactScrollPane (Component view, int vsbPolicy, int hsbPolicy) {
		super(view, vsbPolicy, hsbPolicy);
		init();
	}

	@Override
	public void setViewport (JViewport viewport) {
		super.setViewport(viewport);
		Component view = getViewport() == null ? null : getViewport().getView();
		setViewportView(view);
	}

	@Override
	public void setViewportView (Component view) {
		super.setViewportView(view);
		
		if (this.view != null) {
			view.removeComponentListener(clistener);
		}
		this.view = getViewport().getView();
		createViewListener();
	}

	private void init () {
		view = getViewport() == null ? null : getViewport().getView();
		createViewListener();
	}
	
	private void createViewListener () {
		if (view == null) return;

		clistener = new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent e) {
				setVisible(true);
				revalidate();
				Log.log(0,  "-- set visible SCROLLPANE");
			}
			@Override
			public void componentResized(ComponentEvent e) {
			}
			@Override
			public void componentMoved(ComponentEvent e) {
			}
			@Override
			public void componentHidden(ComponentEvent e) {
				setVisible(false);
				revalidate();
				Log.log(0,  "-- set invisible SCROLLPANE");
			}
		};
		view.addComponentListener(clistener);
	}
}
