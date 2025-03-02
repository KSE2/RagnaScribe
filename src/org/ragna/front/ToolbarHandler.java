/*
*  File: ToolbarHandler.java
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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoManager;

import org.ragna.core.ActionHandler;
import org.ragna.core.Global;
import org.ragna.core.PadDocument;
import org.ragna.front.DisplayManager.DocumentDisplay;
import org.ragna.util.ActionManager.UnknownActionException;

public class ToolbarHandler {

   // *** STATIC HANDLING *** 
   
   private static final String TOOLBAR_VALUES_MAIN = 
      ActionHandler.ActionNames.FILE_NEW + "," +
      ActionHandler.ActionNames.FILE_OPEN + "-asy" + "," +
      ActionHandler.ActionNames.FILE_SAVE + "-asy" + "," +
      ActionHandler.ActionNames.FILE_SAVE_ALL + ",," +
      ActionHandler.ActionNames.FILE_CLOSE + "-asy" + "," +
      ActionHandler.ActionNames.FILE_CLOSE_ALL + ",," +
      ActionHandler.ActionNames.EDIT_UNDO + "," +
      ActionHandler.ActionNames.EDIT_REDO + ",," +
      ActionHandler.ActionNames.ACTION_SEARCH_WEB + "," +
      ActionHandler.ActionNames.ACTION_TRANSLATE_WEB + "," +
      ActionHandler.ActionNames.ACTION_SEARCH + "," +
      ActionHandler.ActionNames.ACTION_PRINT + ",," +
      ActionHandler.ActionNames.VIEW_DISPLAY_SWITCH + "," +
//      ActionHandler.ActionNames.MANAGE_ENCRYPTION + "," +
//      ActionHandler.ActionNames.MANAGE_PREFERENCES + ",," +
      ActionHandler.ActionNames.SYSTEM_EXIT;
      
   ;
//   "tree.move.up,tree.move.down,tree.move.indent,tree.move.outdent," +
//   "tree.create.child,tree.create.below,tree.create.above," + 
//   "tree.edit.copy,tree.edit.cut,tree.edit.paste,tree.doc.toolbox";

   public static final String TOOLBAR_VALUES_TREE = 
         ActionHandler.ActionNames.TREE_MOVE_UP + "," +
         ActionHandler.ActionNames.ORDER_MOVE_DOWN + "," +
         ActionHandler.ActionNames.ORDER_MOVE_INDENT + "," +
         ActionHandler.ActionNames.ORDER_MOVE_OUTDENT + "," +
         ActionHandler.ActionNames.ORDER_CREATE_CHILD + "," +
         ActionHandler.ActionNames.ORDER_CREATE_SIBLING + "," + 
//         ActionHandler.ActionNames.ORDER_CREATE_DUPL + "," +
         
         ActionHandler.ActionNames.ORDER_EDIT_COPY + "," +
         ActionHandler.ActionNames.ORDER_EDIT_CUT + "," +
         ActionHandler.ActionNames.ORDER_EDIT_PASTE + "," +
         ActionHandler.ActionNames.ORDER_EDIT_DELETE  + "," +
         ActionHandler.ActionNames.ORDER_DOC_TOOLBOX // + "," +
         ;

   private static final String TOOLBAR_VALUES_ARTICLE = 
         ActionHandler.ActionNames.EDIT_INSERT_FILE + "," +
         ActionHandler.ActionNames.EDIT_EXPORT_FILE + "," +
         ActionHandler.ActionNames.EDIT_CUT + "," +
         ActionHandler.ActionNames.EDIT_COPY + "," +
         ActionHandler.ActionNames.EDIT_PASTE + "," +
         ActionHandler.ActionNames.EDIT_DELETE // + "," +
   ; 

   // Singleton-Handling
   private static ToolbarHandler instance;
   
   public static ToolbarHandler get () {
      if ( instance == null ) {
         instance = new ToolbarHandler();
      }
      return instance;
   }
   
   public JToolBar getMainToolBar() {
      return mainToolbar;
   }

   public static JToolBar createTreeToolBar() {
      AwarenessToolBar toolbar = new AwarenessToolBar("hasTreeToolbar");
      toolbar.loadToolbarFromActions2(TOOLBAR_VALUES_TREE);
      return toolbar;
   }

   public static JToolBar createArticleToolBar () {
      AwarenessToolBar toolbar = new AwarenessToolBar("hasArticleToolbar");
      toolbar.loadToolbarFromActions2(TOOLBAR_VALUES_ARTICLE);
      return toolbar;
   }

//   public static JToolBar getWorkToolBar() {
//      return get().workToolbar;
//   }

   // *** MEMBER VARIABLES  ***
   
   private RegistryListener regListener = new RegistryListener();
   
   /** The application's main toolbar on the main-frame. */
   private AwarenessToolBar mainToolbar;

//   /** The workshelf toolbar on the main-frame. */
//   private JToolBar workToolbar;
   
//   /** The application's main toolbar on the main-frame. */
//   private AwarenessToolBar treeToolbar;
//
//   /** The application's toolbar for the article editor. */
//   private AwarenessToolBar articleToolbar;

   
   private ToolbarHandler () {
      init();
   }
   
   private void init () {
      ActionHandler.get();
      Global.getDocumentRegistry().addPropertyChangeListener(regListener);
      
      // define PROGRAM (MAIN) toolbar
      mainToolbar = new AwarenessToolBar("hasMainToolbar");
      mainToolbar.loadToolbarFromActions(TOOLBAR_VALUES_MAIN);
      mainToolbar.setFocusable(false);
      
      MouseListener mouseLi = new MouseAdapter() {
 		 @Override
 		 public void mouseEntered(MouseEvent e) {
 			DocumentDisplay display = DisplayManager.get().getSelectedDisplay();
 			if (display != null) {
 				undoLabelUpdate(display.getUndoManger());
 			}
 		 }
 	   };
      
      // special operations
      JButton button = mainToolbar.getActionButton(ActionHandler.ActionNames.EDIT_UNDO);
      button.addMouseListener(mouseLi);
      
      button = mainToolbar.getActionButton(ActionHandler.ActionNames.EDIT_REDO);
      button.addMouseListener(mouseLi);
   }
   
   private void undoLabelUpdate (UndoManager undoMan) {
	  String undo = undoMan.getUndoPresentationName();
	  String redo = undoMan.getRedoPresentationName();
	  
	  JButton button = mainToolbar.getActionButton(ActionHandler.ActionNames.EDIT_UNDO);
	  if (button != null) {
		 button.setToolTipText(undo);
	  }
	  button = mainToolbar.getActionButton(ActionHandler.ActionNames.EDIT_REDO);
	  if (button != null) {
		 button.setToolTipText(redo);
	  }
   }
   
   /** Subclass to JToolBar which self-determines its visibility after a
    * specific boolean option in global options. The option is named with the 
    * constructor. Whenever the value of the option changes, visibility of this
    * toolbar is set accordingly (visible == true).
    * <p>Furthermore this toolbar adapts its "isFloatable" property according to 
    * global boolean option "isToolbarsFloatable".
    *  
    */
   private static class AwarenessToolBar extends JToolBar 
                        implements PropertyChangeListener {
      
      private String visibleProperty;
      
      /** Creates a new {@code AwarenessToolBar} which is visible with the given
       * property in global options.
       * 
       * @param visibleProperty String option property name
       */
      public AwarenessToolBar (String visibleProperty) {
         this.visibleProperty = visibleProperty;
         
         // initialise toolbar with respect to system options
         boolean floatable = Global.getOptions().isOptionSet("isToolbarsFloatable");
         setFloatable(floatable);
         boolean visible = Global.getOptions().isOptionSet(visibleProperty);
         setVisible(visible);
         setMinimumSize(new Dimension(0,0));
         
         // register at global options for property change events
         Global.getOptions().addPropertyChangeListener(visibleProperty, this);
      }
      
      /** Loads the contents (icons etc.) of this toolbar by use of a definition
       * string containing technical action names which are defined in
       * {@code ActionHandler}.
       * <p>The parameter is a comma separated list of action names. The empty
       * name signifies a separator in the toolbar. 
       * 
       * @param actions String list of names
       */
      public void loadToolbarFromActions (String actions) {
         try {
            ActionHandler ah = ActionHandler.get();
            String[] values = actions.split(",");
            for (String actionName : values) {
               if ( actionName.isEmpty() ) {
                  addSeparator();
               } else {
            	  // interpret action name
            	  int index = actionName.indexOf("-asy");
            	  boolean async = index > -1;
            	  if (async) {
            		  actionName = actionName.substring(0, index);
            	  }
            	  // get AH action in appropriate syncronicity modus
                  add(ah.getAction(actionName, async));
               }
            }
         } catch (UnknownActionException e) {
            e.printStackTrace();
         }
      }
      
      /** Returns the action button from this toolbar of the given action-name
       * or null if such an element is not found.
       * 
       * @param name String Action name
       * @return {@code JButton} or null
       */
      public JButton getActionButton (String name) {
    	  for (Component c : getComponents()) {
    		  if (!(c instanceof JButton)) continue;
    		  JButton button = (JButton) c;
    		  if (button.getAction() != null && 
    		      button.getAction().getValue(Action.ACTION_COMMAND_KEY).equals(name)) {
    			  return button;
    		  }
    	  }
		  return null;
      }
      
      @Override
      public void propertyChange (PropertyChangeEvent evt) {
         String key = evt.getPropertyName();
         String newValue = (String)evt.getNewValue();

         if ( key.equals(visibleProperty) ) {
            boolean visible = Boolean.valueOf(newValue);
            setVisible(visible);
         }
      }

	/** Loads the contents (icons etc.) of this toolbar by use of a definition
	   * string containing technical action names which are defined in
	   * {@code ActionHandler}. This version lets all actions be performed
	   * in a "later-on-EDT" modus in order to avoid fatal jamming of the EDT.
	   * <p>The parameter is a comma separated list of action names. The empty
	   * name signifies a separator in the toolbar. 
	   * 
	   * @param actions String list of names
	   */
	  public void loadToolbarFromActions2 (String actions) {
	        ActionHandler ah = ActionHandler.get();
	        String[] values = actions.split(",");
	        for (String actionName : values) {
	           if ( actionName.isEmpty() ) {
	              addSeparator();
	           } else {
	      	     try {
	      	    	 Action ac = ah.getAction(actionName); 
	      	    	 add(new LaterOnEDT_Action(ac));
	    	     } catch (UnknownActionException e) {
	    	    	 System.err.println("** (ToolbarHandler) unknown Action: " + actionName);
	    	     }
	           }
	        }
	  }
   } // AwarenessToolBar
   
   /** Implements an {@code Action} which takes another {@code Action} B as
    * argument. When this action is performed, B is always scheduled to perform
    * on the EDT at a later time. Practically this moves execution of B to a 
    * later time while the calling thread terminates {@code actionPerformed()}
    * immediately. This is also true if the EDT is the calling thread.
    * <p>This action represents the argument action in all methodical aspects.
    */
   private static class LaterOnEDT_Action implements Action {
	   private Action action;
	   
	   LaterOnEDT_Action (Action a) {
		   Objects.requireNonNull(a);
		   action = a;
	   }

		@Override
		public void actionPerformed (ActionEvent e) {
			Runnable run = new Runnable() {
				@Override
				public void run() {
					action.actionPerformed(e);
				}
			};
			SwingUtilities.invokeLater(run);
		}
	
		@Override
		public Object getValue(String key) {
			return action.getValue(key);
		}
	
		@Override
		public void putValue(String key, Object value) {
			action.putValue(key, value);
		}
	
		@Override
		public void setEnabled(boolean b) {
			action.setEnabled(b);
		}
	
		@Override
		public boolean isEnabled() {
			return action.isEnabled();
		}
	
		@Override
		public void addPropertyChangeListener(PropertyChangeListener listener) {
			action.addPropertyChangeListener(listener);
		}
	
		@Override
		public void removePropertyChangeListener(PropertyChangeListener listener) {
			action.removePropertyChangeListener(listener);
		}
   } // LaterOnEDT_Action

   /** Class that listens to global document registry and all its contained
    * pad-documents. Target is to control enabled state of the 'save' and
    * 'save-all' actions.
    */
   private class RegistryListener implements PropertyChangeListener {

	  @Override
	  public void propertyChange (PropertyChangeEvent evt) {
		  if (evt != null) {
			  String name = evt.getPropertyName();
			  PadDocument document = (PadDocument)evt.getNewValue(); 
			  
			  if ("documentAdded".equals(name) || "documentRemoved".equals(name) ||
				  "documentModified".equals(name) || "documentSelected".equals(name) ||
				  "documentReplaced".equals(name)) {
				  runModifiedControl(document);
			  } 
		  }
	  }
   }

   public void runModifiedControl (PadDocument document) {
	   int nrMod = Global.getDocumentRegistry().numberModified();
	   boolean isSelected = Global.getDocumentRegistry().isSelectedDocument(document);
       try {
		  Action a = ActionHandler.get().getAction(ActionHandler.ActionNames.FILE_SAVE);
		  a.setEnabled(isSelected && document.isModified());
		  a = ActionHandler.get().getAction(ActionHandler.ActionNames.FILE_SAVE_ALL);
		  a.setEnabled(nrMod > 0);
       } catch (UnknownActionException e) {
	   }
   }
   
}
