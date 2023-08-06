/*
*  File: ButtonBar.java
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;


/**
 * Creates a container suitable for holding buttons.  The buttons are added 
 * using the {@link #add(Component)}
 * method and are arranged as a horizontal strip with each button the same size 
 * as the widest button.  If
 * the button bar is put into a container wider than its natural size, the 
 * buttons are not resized, but
 * horizontal spacing is added to centre the buttons within the area.
 * 
 * @author Kevin Preece
 */
public class ButtonBar extends JPanel
{
	public static final int		LEFT		= 0;
	public static final int		TOP			= 0;
	public static final int		RIGHT		= 1;
	public static final int		BOTTOM		= 1;
	public static final int		CENTRE		= 2;
	public static final int		CENTER		= 2;
	public static final int		HORIZONTAL	= 3;
	public static final int		VERTICAL	= 4;

	private JPanel		buttonPanel;

	/**
	 * Creates a button bar that is initially empty with the default orientation of
	 * <code>HORIZONTAL</code> and alignment of <code>CENTRE</code>.
	 */
	public ButtonBar()
	{
		this( HORIZONTAL, CENTRE );
	}

    /**
     * Creates a bar that JButton buttons are added to.  All buttons will be resized
     * so that they are equal. There is a default margin for the border area.
     * 
     * @param orientation  the orientation of the button bar <code>HORIZONTAL</code>
     *        or <code>VERTICAL</code>.
     * @param alignment    the button bar alignment - <code>LEFT</code>, 
     *        <code>CENTRE</code> or <code>RIGHT</code>. 
     */
    public ButtonBar( int orientation, int alignment )
    {
       this( orientation, alignment, new Insets( 12, 0, 6, 0 ) );
    }

	/**
	 * Creates a bar that JButton buttons may get added to.  All buttons will be resized
	 * so that they are equal.
	 * 
	 * @param orientation  the orientation of the button bar <code>HORIZONTAL</code> or <code>VERTICAL</code>.
	 * @param alignment    the button bar alignment - <code>LEFT</code>, <code>CENTRE</code> or <code>RIGHT</code>.
    * @param margin <code>Insets</code> to define the empty border area of the bar 
	 */
	public ButtonBar( int orientation, int alignment, Insets margin )
	{
		super();

        JPanel				panel;
		GridBagConstraints	c;
		GridLayout			layout;

		if ( orientation == HORIZONTAL )
		{
			// rows, cols, hgap, vgap
			layout = new GridLayout( 1, 0, 8, 0 );
		}
		else if ( orientation == VERTICAL )
		{
			// rows, cols, hgap, vgap
			layout = new GridLayout( 0, 1, 0, 4 );
		}
		else
		{
			throw new IllegalArgumentException();
		}

		if ( (alignment != LEFT) && (alignment != CENTRE) && (alignment != RIGHT) )
		{
			throw new IllegalArgumentException();
		}

		panel			= new JPanel( new GridBagLayout() );
        panel.setOpaque( false );
		buttonPanel		= new JPanel( layout );
        buttonPanel.setOpaque( false );
		c				= new GridBagConstraints();

		if ( (alignment == CENTRE) || (alignment == RIGHT) )
		{
			c.fill			= GridBagConstraints.HORIZONTAL;
			panel.add( spacerPanel( 10, 10 ), c );
		}
		
		c.fill			= GridBagConstraints.NONE;
		panel.add( buttonPanel, c );

		if ( (alignment == CENTRE) || (alignment == LEFT) )
		{
			c.fill			= GridBagConstraints.HORIZONTAL;
			panel.add( spacerPanel( 10, 10 ), c );
		}

		super.setBorder( new EmptyBorder( margin ) );
		super.add( panel );
	}

   private JPanel spacerPanel ( int width, int height )
   {
      JPanel panel;
      
      panel = new JPanel();
      panel.setOpaque( false );
      panel.setPreferredSize( new Dimension( width, height ) );
      return panel;
   }
   
	/**
	 * Adds a component, like e.g. a button, to this button bar.
	 * 
	 * @param component the component to add
	 */
	@Override
	public Component add( Component component ) {
		return buttonPanel.add( component );
	}

   /**
     * Adds a component, like e.g. a button, to this button bar at a specified index.
     * 
     * @param component the component to add
     * @param index the position index of the new component 
     * @since 0-4-0
     */
    @Override
	public Component add( Component component, int index )
    {
    	return buttonPanel.add( component, index );
    }
    
    // since 0-5-0
    @Override
	public void remove ( Component comp )
   {
      buttonPanel.remove( comp );
   }

    // since 0-5-0
   @Override
public void remove ( int index )
   {
      buttonPanel.remove( index );
   }

   // since 0-5-0
   @Override
public void removeAll ()
   {
      buttonPanel.removeAll();
   }

   // since 0-5-0
   @Override
public void setBackground ( Color c )
    {
       Component[] carr;
       int i;
       
       super.setBackground( c );
       
       // set all button-panel components
       if ( buttonPanel != null && (carr = buttonPanel.getComponents()) != null )
          for ( i = 0; i < carr.length; i++ )
             carr[ i ].setBackground( c );
          
    }
   
   public boolean isEmpty ()
   {
      return getNrOfComponents() == 0;
   }
   
   public int getNrOfComponents ()
   {
      return buttonPanel.getComponentCount();
   }
}
