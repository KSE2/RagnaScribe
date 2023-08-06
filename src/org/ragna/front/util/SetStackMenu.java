/*
*  File: SetStackMenu.java
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.ragna.core.ActionHandler;
import org.ragna.core.Global;
import org.ragna.front.GUIService;
import org.ragna.util.ActionManager.UnknownActionException;

import kse.utilclass.sets.SetStack;

public class SetStackMenu extends SetStack<Object> {
   private JMenu menu;
   private JMenuItem deleteItem;
   private Color itemTextColor;
   private Color itemBgdColor;
   private String name;
   private int maxEntries;
   private boolean allowListDeletion;
   
   
   /** Creates a stack-menu with no name, infinite number of items and
    * list deletion unavailable.
    */
   public SetStackMenu () {
      this(null, Integer.MAX_VALUE);
   }
   
   /** Creates a stack-menu with given name, maximum number of items and
    * list deletion unavailable.
    * 
    * @param name String stack name (human readable)
    * @param maxEntries int maximum number of stack entries
    */
   public SetStackMenu (String name, int maxEntries) {
      this(name, maxEntries, false);
   }
   
   /** Creates a stack-menu with given name, maximum number of items and
    * list deletion option.
    * 
    * @param name String stack name (human readable)
    * @param maxEntries int maximum number of stack entries
    * @param allowDeletion boolean whether the list deletion option is 
    *                       available in the menu
    */
   public SetStackMenu (String name, int maxEntries, boolean allowDeletion) {
      this.name = name;
      this.maxEntries = Math.max(0, maxEntries);
      init();
   }
   
   /** Call-back function to collect a user's or application's decision on
    * whether a user triggered deletion command from the menu shall be 
    * executed. By default returns <code>true</code>.
    *  
    * @param name String name of this SetStackMenu
    * @param size int current number of elements in this SetStack
    * @return boolean true = deletion go, false = deletion break
    */
   public boolean confirmListDeletion (String name, int size) {
      return GUIService.userConfirm("msg.ask.recentlist.clear");
   }
   
   private void init () {
//		try {
//			Action deleteAction = ActionHandler.get().getAction(ActionHandler.ActionNames.MANAGE_CLEAR_RECENTLIST);
//			menu.addSeparator();
//       	menu.add(new JMenuItem(a));
//		} catch (UnknownActionException e) {
//			e.printStackTrace();
//		}

	  String title = Global.res.getCommand("action.admin.recentlist.clear");
      Action deleteAction = new AbstractAction(title) {
         @Override
         public void actionPerformed (ActionEvent e) {
            if ( confirmListDeletion(name, size()) ) {
               clear();
            }
         }
      };
      deleteItem = new JMenuItem(deleteAction);
   }
   
   /** Returns a new <code>JMenu</code> that contains the current elements of 
    * this <code>SetStack</code> from top to bottom. The menu's content  is 
    * re-created each time the menu is selected in the command structure.
    * 
    * @param action <code>Action</code> definition of the <code>JMenuItem</code>
    *                aspect of the menu
    * @return <code>JMenu</code>
    */
   public JMenu getMenu (Action action) {
      menu = new JMenu(action);
      menu.addMenuListener(new OurMenuListener());
      return menu;
   }
   
   @Override
   public synchronized boolean add (Object e) {
      boolean ok = super.add(e);
      if (ok && size() > maxEntries) {
         Object lastElement = iterator().next(); // getList().get(0);
         remove(lastElement);
      }
      return ok;
   }

   public void loadContent (Object[] content) {
      clear();
      if (content != null) {
         for (Object obj : content) {
            add(obj);
         }
      }
   }
   
   public void loadStringContent (String content, char separator) {
      clear();
      if (content != null) {
         Object[] arr = content.split(String.valueOf(separator));
         loadContent(arr);
      }
   }
   
   public Object[] getContent () {
      return getList().toArray();
   }
   
   @Override
   public synchronized Object pop () {
      return super.pop();
   }

   @Override
   public synchronized Object peek () {
      return super.peek();
   }

   @Override
   public synchronized int search (Object o) {
      return super.search(o);
   }

   @Override
   public synchronized List<Object> getList () {
      return super.getList();
   }

   
   @Override
   public synchronized Iterator<Object> iterator () {
      return super.iterator();
   }

   @Override
   public synchronized boolean contains (Object o) {
      return super.contains(o);
   }

   @Override
   public synchronized boolean remove (Object o) {
      return super.remove(o);
   }

   @Override
   public synchronized Object remove (int index) {
      return super.remove(index);
   }

   @Override
   public synchronized void clear () {
      super.clear();
   }

   @Override
   public synchronized Object clone () {
      return super.clone();
   }

   /** Returns a String representation  of this set stack by listing each
    * contained object x as if converted by 'String.valueOf(x)'. If the list
    * is empty an empty string is returned.
    * 
    * @param separator char separator mark between two consecutive 
    * representations
    * @return String
    */
   public String getStringContent (char separator) {
      Object[] arr = getContent();
      StringBuffer buffer = new StringBuffer(512);
      for (Object obj : arr) {
         if (buffer.length() > 0) {
            buffer.append(separator);
         }
         buffer.append(obj);
      }
      return buffer.toString();
   }
   
   public Color getItemTextColor () {
      return itemTextColor;
   }

   public void setItemTextColor (Color color) {
      this.itemTextColor = color;
   }

   public Color getItemBgdColor () {
      return itemBgdColor;
   }

   public void setItemBgdColor (Color color) {
      this.itemBgdColor = color;
   }

   public String getName () {
      return name;
   }

   public void setName (String name) {
      this.name = name;
   }

   public int getMaxEntries () {
      return maxEntries;
   }

   public void setMaxEntries (int maxEntries) {
      this.maxEntries = Math.max(0, maxEntries);
   }

   public boolean isAllowListDeletion () {
      return allowListDeletion;
   }

   public void setAllowListDeletion (boolean allowListDeletion) {
      this.allowListDeletion = allowListDeletion;
   }



   private class OurMenuListener implements MenuListener, ActionListener {

      @Override
      public void menuSelected (MenuEvent e) {
         // prepare generics
         menu.removeAll();
         if (itemBgdColor != null) {
            deleteItem.setBackground(itemBgdColor);
         }

         // add menu elements from the stack-list
         List<Object> list = getList(); 
         int size = list.size(); 
         for (int i = size-1; i >= 0; i--) {
        	Object obj = list.get(i);
            String title = obj == null ? "- null -" : obj.toString();
            if (title != null && !title.isEmpty()) {
               JMenuItem item = new JMenuItem(title);
               // set the list index integer of this item as action command
               item.setActionCommand(String.valueOf(i));
               item.addActionListener(this);
               menu.add(item);
               
               if (itemTextColor != null) {
                  item.setForeground(itemTextColor);
               }
               if (itemBgdColor != null) {
                  item.setBackground(itemBgdColor);
               }
            }
         }
         
         // add delete command if feasible
         if (allowListDeletion && menu.getItemCount() > 0) {
            menu.addSeparator();
            menu.add(deleteItem);
         }
      }

      @Override
      public void menuDeselected (MenuEvent e) {
         menu.removeAll();
      }

      @Override
      public void menuCanceled (MenuEvent e) {
         menu.removeAll();
      }

      @Override
      public void actionPerformed (ActionEvent e) {
         // we collect the list index from menu-item event
         // and hand it over to menu Action as ID of a new event 
         String cmd = e.getActionCommand();
         int index = Integer.parseInt(cmd);
         Action action = menu.getAction();
         if (action != null) {
            ActionEvent evt = new ActionEvent(SetStackMenu.this, index, 
                  (String)action.getValue(Action.ACTION_COMMAND_KEY));
            action.actionPerformed(evt);
         }
      }
      
   }
}
