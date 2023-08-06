/*
*  File: MenuHandler.java
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.ragna.core.ActionHandler;
import org.ragna.core.Global;
import org.ragna.core.PadDocument;
import org.ragna.front.DisplayManager.DisplayModus;
import org.ragna.front.DisplayManager.DocumentDisplay;
import org.ragna.io.IO_Manager;
import org.ragna.util.PersistentOptions;
import org.ragna.util.ActionManager.UnknownActionException;

import kse.utilclass.gui.ActiveJMenu;
import kse.utilclass2.gui.JMenuBarReader;
import kse.utilclass2.gui.SkilledJMenuBar;

public class MenuHandler {
   // *** STATIC HANDLING *** 
   
   // Singleton-Handling
   private static MenuHandler instance;
   
   public static MenuHandler get () {
      if ( instance == null ) {
         instance = new MenuHandler();
      }
      return instance;
   }
   
   public static JMenuBar getMainMenuBar () {
      return get().mainMenuBar;
   }

   public static Color MENUITEM_MARKED_COLOR = Color.BLUE;
   
   // *** MEMBER VARIABLES  ***
   
   /** The application's main toolbar on the main-frame. */
   private SkilledJMenuBar mainMenuBar;
   
   
   private MenuHandler () {
      init();
   }
   
   private void init () {
      mainMenuBar = createMainMenuBar();
      
   }

   private SkilledJMenuBar createMainMenuBar () {
      
      try {
    	 Locale locale = Global.getLocale();
    	 
    	 // available locale menues: German, English
    	 String menuResource;
    	 if (locale.getLanguage().equals(new Locale("de").getLanguage())) {
    		 menuResource = "#system/mainmenu_de.txt";
    	 } else {
    		 menuResource = "#system/mainmenu_en.txt";
    	 }
         InputStream input = Global.res.getResourceStream(menuResource);
         JMenuBarReader reader = new OurMenuBarReader(input, null, false, false);
         reader.setDefaultEnabled(false);
         SkilledJMenuBar bar = reader.readMenu();
         return bar;
         
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      } 
   }
   
   /** Creates and returns the EDIT menu of the currently selected view or
    * null if there is no suitable view selected.
    * 
    * @return {@code JMenu} or null
    */
   private JMenu createEditMenu () {
	   JMenu menu = null;
	   DocumentDisplay display = DisplayManager.get().getSelectedDisplay();
	   if (display != null) {
		   menu = display.getEditMenuAcivist().getJMenu(); 
	   }
	   return menu;
   }
   
   /** Creates and returns the history files menu of the currently selected 
    * view.
    * 
    * @return {@code JMenu} or null
    */
   private JMenu createHistoryFilesMenu () {
	   JMenu menu = new JMenu();
	   PadDocument doc = DisplayManager.get().getSelectedDocument();
	   if (doc != null) {
		   File[] files = IO_Manager.get().getFileHistory(doc);
		   if (files != null) {
			   // add list of unique history files
			   for (File f : files) {
				   Action a = ActionHandler.get().getLoadDocumentAction(f, doc);
				   JMenuItem item = new JMenuItem(a);
				   menu.add(item);
			   }
			   
			   // add "delete history" command
			   if (files.length > 0) {
				   try {
					   Action a = ActionHandler.get().getAction(ActionHandler.ActionNames.MANAGE_DELETE_HISTORY);
					   JMenuItem item = new JMenuItem(a);
					   menu.addSeparator();
					   menu.add(item);
				   } catch (UnknownActionException e) {
					   e.printStackTrace();
				   }
			   }
		   }
	   }
	   return menu;
   }
   
   public void refreshAll () {
      refreshViewMenu();
   }

   public void refreshViewMenu () {
      // supply relevant system states
      DisplayModus displayModus = DisplayManager.get().getSelectedDisplayModus();
      PersistentOptions options = Global.getOptions();
      boolean mainToolbarActive = options.isOptionSet("hasMainToolbar");
      boolean treeToolbarActive = options.isOptionSet("hasTreeToolbar");
      boolean articleToolbarActive = options.isOptionSet("hasArticleToolbar");
      
      // set toolbar checkbox choices
      JMenuItem item1 = mainMenuBar.getMenuItem(ActionHandler.ActionNames.VIEW_MAIN_TOOLBAR);
      JMenuItem item2 = mainMenuBar.getMenuItem(ActionHandler.ActionNames.VIEW_TREE_TOOLBAR);
      JMenuItem item3 = mainMenuBar.getMenuItem(ActionHandler.ActionNames.VIEW_ARTICLE_TOOLBAR);

      if ( item1 != null ) {
         item1.setSelected(mainToolbarActive);
      }
      if ( item2 != null ) {
         item2.setSelected(treeToolbarActive);
      }
      if ( item3 != null ) {
         item3.setSelected(articleToolbarActive);
      }
      
      // 3 radio buttons about display modus of the selected document
      // get relevant menu items
      item1 = mainMenuBar.getMenuItem(ActionHandler.ActionNames.VIEW_DISPLAY_ALL);
      item2 = mainMenuBar.getMenuItem(ActionHandler.ActionNames.VIEW_DISPLAY_ARTICLE);
      item3 = mainMenuBar.getMenuItem(ActionHandler.ActionNames.VIEW_DISPLAY_TREE);

      // set radio buttons
      if ( item1 != null ) {
         item1.setSelected( displayModus == DisplayModus.ComleteDisplay );
      }
      if ( item2 != null ) {
         item2.setSelected( displayModus == DisplayModus.ArticleDisplay );
      }
      if ( item3 != null ) {
         item3.setSelected( displayModus == DisplayModus.OrderDisplay );
      }
   }
   
   private class OurMenuBarReader extends JMenuBarReader {

      public OurMenuBarReader(InputStream input, Charset cs, boolean icons,
            boolean tooltips) {
         super(input, cs, icons, tooltips);
      }

      @Override
      public JMenu getSpecialMenu (String name) {
         JMenu menu = null;
         
         if ("file.open.recent".equals(name)) {
            menu = Global.getRecentFilesStack().getMenu(null);

         } else if ("menu.edit".equals(name)) {
        	 menu = new ActiveJMenu("noname") {
				@Override
				protected JMenu getCurrentMenu() {
					return createEditMenu();
				}
        	 };

         } else if ("admin.file.restore".equals(name)) {
        	 menu = new ActiveJMenu("noname") {
				@Override
				protected JMenu getCurrentMenu() {
					JMenu menu = createHistoryFilesMenu();
					return menu; 
				}
        	 };
        	 
         }
         return menu;
      }

	  @Override
	  public Action getItemAction(String name, boolean async) {
		  try {
             return ActionHandler.get().getAction(name, async);
		  } catch (UnknownActionException e) {
			  return null;
		  }
	}
   } // OurMenuBarReader

   /** This is called from other managers when the document order-view in 
    * display indicates a change in the structure of the current document.
    */
   public void orderViewUpdate() {
	  // update Undo/Redo elements from current order-view
	  PadDocument document = DisplayManager.get().getSelectedDocument();
	  if (document != null) {
		  String undo = document.getUndoManager().getUndoPresentationName();
		  String redo = document.getUndoManager().getRedoPresentationName();
		  
		  JMenuItem item = mainMenuBar.getMenuItem("");
		  if (item != null) {
			  item.setToolTipText(undo);
		  }
		  item = mainMenuBar.getMenuItem("");
		  if (item != null) {
			  item.setToolTipText(redo);
		  }
	  }
   }
}
