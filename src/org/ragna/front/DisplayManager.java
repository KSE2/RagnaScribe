/*
*  File: DisplayManager.java
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.TextAction;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.undo.UndoManager;

import org.ragna.core.ActionHandler;
import org.ragna.core.Global;
import org.ragna.core.PadArticle;
import org.ragna.core.PadDocument;
import org.ragna.core.PrintParameters;
import org.ragna.core.PrintParameters.ArticlePrintModus;
import org.ragna.core.PrintParameters.PrintScope;
import org.ragna.util.ActionManager;
import org.ragna.util.PersistentOptions;
import org.ragna.util.ActionManager.UnknownActionException;

import kse.utilclass.gui.MenuActivist;
import kse.utilclass.gui.VerticalFlowLayout;
import kse.utilclass.misc.Log;
import kse.utilclass.misc.UnixColor;
import kse.utilclass.misc.Util;
import kse.utilclass.sets.SetStack;

public class DisplayManager {
	
   private static final Color IDLE_SCREEN_COLOR = Color.pink;
   private static final Color EMPTY_EDITOR_COLOR = UnixColor.Thistle;
   private static final Color READONLY_EDITOR_COLOR = UnixColor.WhiteSmoke;
   private static final Color READONLY_TEXT_COLOR = Color.DARK_GRAY;
   private static final Color READONLY_ORDERVIEW_COLOR = new Color(0xEFEFEF);
   private static final Color ORDER_VIEW_COLOR = UnixColor.AliceBlue;
   private static final int DEFAULT_DIVIDER_LOCATION = 300;

   private static DisplayManager instance = new DisplayManager();
   
   /** Indicates the appearance of the document display (Article, Order or both) */  
   public enum DisplayModus { ComleteDisplay, ArticleDisplay, OrderDisplay;
	   
      public static DisplayModus fromString (String value) {
         DisplayModus mod = null;
         try { mod = valueOf(value); 
         } catch (Exception e) {
         }
         return mod != null ? mod : ComleteDisplay;
      }
   }
   
   /** Viewable objects of interest which can be addressed by a focus change. */
   public enum FocusableView { Order, Article }
   
   SetStack<DocumentDisplay> displayList = new SetStack<>();

   private JMenuBar              mainMenuBar;
   private JPanel                primaryDisplayPanel;
   private MainScreenPanel       mainScreenPanel;
   private ToolsPanel            toolsPanel;
   private StatusBar             statusLine;
   /** The default document display modus (on new documents). */
   private DisplayModus          defaultDisplayModus;
   
   private SingleDocumentDisplay singleDisplay;
   private MultiDocumentDisplay  multiDisplay;
   private DocumentDisplayField  currentDisplay;
   
   public static DisplayManager get () {
      return instance;
   }
   
   /** Returns a standard color for the depth information of a pad-article.
    * If depth does not match, Color.Black is returned.
    * 
    * @param depth int order depth
    * @return Color
    */
   private static Color getDepthColor (int depth) {
       Color color = Color.black;
       switch (depth) {
       case 1 : color = UnixColor.DarkMagenta; break;
       case 2 : color = UnixColor.SeaGreen; break;
       case 3 : color = UnixColor.Crimson; break;
       case 4 : color = UnixColor.RoyalBlue; break;
       case 5 : color = UnixColor.Chocolate; break;
       case 6 : color = UnixColor.MediumOrchid; break;
       case 7 : color = UnixColor.OliveDrab; break;
       case 8 : color = UnixColor.Coral; break;
       case 9 : color = UnixColor.DarkGoldenRod; break;
       case 10 : color = UnixColor.SlateBlue; break;
       }
       return color;
   }
   
   private DisplayManager () {
   }
   
   public void init () {
      // self-startup
//      PropertyChangeListener pchListener = new SystemListener();
//      Global.getSystemProperties().addPropertyChangeListener(pchListener);
      PersistentOptions options = Global.getOptions();
      PropertyChangeListener optListener = new RegistryListener();
      String hstr = options.getOption("defaultDocumentDisplayModus");
      defaultDisplayModus = DisplayModus.fromString(hstr);

      options.addPropertyChangeListener(optListener);
      Global.getDocumentRegistry().addPropertyChangeListener(optListener);

      // create GUI mainframe
      Ragna ragna = new Ragna();
      Global.mainframe = ragna;

      // main command menu
      mainMenuBar = MenuHandler.getMainMenuBar();
      Global.mainframe.setJMenuBar( mainMenuBar );
      
      // primary setup
      mainScreenPanel = new MainScreenPanel();
      toolsPanel = new ToolsPanel();
      statusLine = new StatusBar();
      primaryDisplayPanel = new JPanel( new BorderLayout() );
      primaryDisplayPanel.add( statusLine, BorderLayout.SOUTH );
      primaryDisplayPanel.add( mainScreenPanel, BorderLayout.CENTER );
      primaryDisplayPanel.add( toolsPanel, BorderLayout.NORTH );
      ragna.getContentPane().add( primaryDisplayPanel, BorderLayout.CENTER );
      
      // content
      setupIdleScreen();  // this should normally be the primary display status
//      setupSingleDocument();  // this is the display status if only one document is loaded
//      setupMultiDocument();

      // refresh menus
      refreshCommandDisplay();
      
      ragna.pack();
      ragna.gainMemoryBounds();
      ragna.setVisible(true);

   }
   
   public StatusBar getStatusBar () {
      return statusLine;
   }
   
   public MainScreenPanel getMainScreenPanel () {
      return mainScreenPanel;
   }
   
   /** Returns the the default display modus which comes into action for
    * new documents.
    * 
    * @return <code>DisplayModus</code> or null
    */
   public DisplayModus getDefaultDisplayModus () {
      return defaultDisplayModus;
   }

   /** Returns the current display modus of the currently selected document
    * or null if no document is available.
    * 
    * @return <code>DisplayModus</code> or null
    */
   public DisplayModus getSelectedDisplayModus () {
      DocumentDisplay display = getSelectedDisplay();
      return display == null ? null : display.getViewModus();
   }
   
   /** Sets the IDLE SCREEN on the main screen panel.
    */
   private void setupIdleScreen () {
      JPanel idleScreen = new JPanel();
      idleScreen.setOpaque(true);
      idleScreen.setBackground(IDLE_SCREEN_COLOR);
      
      getMainScreenPanel().setContent(idleScreen);
      currentDisplay = null;
   }

   /** The number of visible document displays in the DisplayManager.
    *  
    * @return int number of document displays
    */
   public int displayCount () {
      return displayList.size();
   }
   
   /** Creates a new display for a given document. This method waits until 
    * the document is added to the display. This method's functionality runs
    * on the EDT (hence is synchronised).
    * 
    * @param doc <code>PadDocument</code>
    */
   public void addDocumentDisplay (final PadDocument doc) {
	  Objects.requireNonNull(doc);

      // define Runnable to perform display organisation for the document
      Runnable run = new Runnable () {
         @Override
         public void run () {
            // create new display and add to manager's display set
            DocumentDisplay display = new DocumentDisplay(doc);
            displayList.add(display);
            String title = display.getDocument().getShortTitle();
            setupDisplay();
            currentDisplay.addDisplay(display, true);
            Log.debug(10, "(DisplayManager.addDocumentDisplay) document display added: "
                    + title + ", counter=" + displayCount());
         }
      };
      
      // make the Runnable to run on the EDT and wait
      try {
		ActionManager.runOnEDT(run, true);
	} catch (InvocationTargetException | InterruptedException e) {
		e.printStackTrace();
	}
  }
   
   /** Collects preferences for the given document from the current display.
    * This updates the preferences in the document.
    * 
    * @param doc {@code PadDocument}, may be null
    */
   public void endEditDocument (PadDocument doc) {
	   if (doc != null) {
		   for (DocumentDisplay display : displayList) {
			   if (doc.equals(display.getDocument())) {
				   display.finishEdit();
				   break;
			   }
		   }
	   }
   }
   
   /** Whether the given document is an element of the displayed documents
    * of this manager.
    * 
    * @param doc {@code PadDocument}, may be null
    * @return boolean true = document showing, false = document not showing
    */
   public boolean isDocumentShowing (PadDocument doc) {
	   if (doc != null) {
		   for (DocumentDisplay display : displayList) {
			   if (doc.equals(display.getDocument())) {
				   return true;
			   }
		   }
	   }
	   return false;
   }
   
   /** Removes all known displays for the given document from the 
    * display manager. This method waits until the document is removed from
    * display. This method's base functionality runs on the EDT 
    * (hence is synchronised).
    * 
    * @param doc <code>PadDocument</code>
    */
   public void removeDocumentDisplay (PadDocument doc) {
      
      // remove all displays for a single document
      for (Iterator<DocumentDisplay> it = displayList.iterator(); it.hasNext();) {
         final DocumentDisplay display = it.next();
         
         if (display.getDocument().equals(doc)) {
            // define Runnable to perform display organisation for the document
            Runnable run = new Runnable () {
               @Override
               public void run () {
                   // remove from base display set
                   it.remove();
                   String title = display.getDocument().getShortTitle();
                   Log.debug(10, "(DisplayManager) display removed from list: "
                         + title + ", counter=" + displayCount());
                   
                  // remove from current display
                  if (currentDisplay != null) {
                     currentDisplay.removeDisplay(display);
                  }
                  display.setDocument(null);
                  setupDisplay();

                  Log.debug(10, "(DisplayManager.removeDocumentDisplay) document display removed: "
                          + title + ", counter=" + displayCount());
               }
            };
            
            // make the Runnable to run on the EDT
            try {
				ActionManager.runOnEDT(run, true);
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
			}
         }
      }
   }

   private void setupDisplay () {
      switch ( displayCount() ) {
      case 0: 
         setupIdleScreen();
         break;
      case 1: 
         setupSingleDisplay();
         break;
      default:
         setupMultiDocument();
      }
   }
   
//   public void removeDocumentDisplay (SingleDocumentDisplay display) {
//      if ( currentDisplay != null ) {
//         int index = currentDisplay.
//      }
//      
//      if ( displayCount() == 1 ) {
//         setupSingleDisplay(getSelectedDisplay().getDocument());
//      } else if ( displayCount() == 0 ) {
//         setupIdleScreen();
//      }
//   }
   
   /** Sets the MULTI-DOCUMENT view on the main screen panel.
    */
   private void setupMultiDocument () {
      if (multiDisplay == null) {
         multiDisplay = new MultiDocumentDisplay();
      }
      if ( currentDisplay == multiDisplay ) return;
         
      DocumentDisplay selectedDisplay = getSelectedDisplay();
      if ( currentDisplay != null ) {
         currentDisplay.clear();
      }

      // takes over all displays from manager's list
      multiDisplay.clear();
      for (DocumentDisplay display : displayList) {
         multiDisplay.addDisplay(display, true);
      }

      // restore selected display and update screen panel
      multiDisplay.setSelectedDisplay(selectedDisplay);
      getMainScreenPanel().setContent(multiDisplay);
      currentDisplay = multiDisplay;
   }


   /** Sets the SINGLE-DOCUMENT view on the main screen panel.
    */
   private void setupSingleDisplay () {
      if (singleDisplay == null) {
         singleDisplay = new SingleDocumentDisplay();
      }
      if ( currentDisplay == singleDisplay ) return;
      
      // take over a selected display from previous display field
      // or take the last inserted from manager's display list
      DocumentDisplay display = getSelectedDisplay();
      if ( display == null ) {
         display = displayList.peek();
      }
      if ( display != null ) {
         singleDisplay.addDisplay(display, true);
      }
      
      // as for now we clear the previous display field
      if ( currentDisplay != null ) {
         currentDisplay.clear();
      }

      // update the main-screen panel and current-display
      getMainScreenPanel().setContent(singleDisplay);
      currentDisplay = singleDisplay;
   }
   
   public void setSelectedDisplayModus ( DisplayModus modus ) {
      DocumentDisplay selectedDisplay = getSelectedDisplay();
      if ( selectedDisplay != null ) {
         selectedDisplay.setupView(modus);
      }
   }
   
   /** Updates general display elements to accord with the given document's 
    * states. (E.g. after the MODIFIED status of a document has changed.)
    * 
    * @param doc <code>PadDocument</code>
    */
   private void updateDocumentDisplay (PadDocument document) {
      if (singleDisplay != null) {
         singleDisplay.updateDisplay(document);
      }
      if (multiDisplay != null) {
         multiDisplay.updateDisplay(document);
      }
   }
   
   /** Returns the selected DOCUMENT DISPLAY or null if no display
    * is available. As long as there is at least one document loaded, there 
    * is always a value returned for the selected display. 
    * 
    * @return <code>SingleDocumentDisplay</code> or null
    */
   public DocumentDisplay getSelectedDisplay () {
      return currentDisplay == null ? null : currentDisplay.getSelectedDisplay();
   }

   /** Sets the currently selected document display.
    * 
    * @param display {@code DocumentDisplay}, may be null
    */
   public void setSelectedDisplay (DocumentDisplay display) {
	   if (display != null & currentDisplay != null) {
		   currentDisplay.setSelectedDisplay(display);
	   }
   }
   
   /** Returns the article editor of the currently selected document or
    * null if nothing is selected.
    * 
    * @return {@code ArticleEditor} or null
    */
   public ArticleEditor getSelectedEditor () {
	  DocumentDisplay display = getSelectedDisplay();
   	  return display == null ? null : display.getArticleEditor();
   }
   
   /** Returns the currently selected document in the GUI or null if no 
    * document is available.
    * 
    * @return  <code>PadDocument</code> or null
    */ 
   public PadDocument getSelectedDocument () {
      DocumentDisplay display = getSelectedDisplay();
      PadDocument document = display == null ? null : display.getDocument();
      return document;
   }

   /** Returns an array with all pad-documents currently open in the display.
    * 
    * @return {@code PadDocument[]}
    */
   public PadDocument[] getOpenDocuments () {
	   PadDocument[] arr = new PadDocument[displayList.size()];
	   int i = 0;
	   for (DocumentDisplay display : displayList) {
		   arr[i++] = display.getDocument();
	   }
	   return arr;
   }
   
   /** Returns the currently selected document order-view in the GUI or null 
    * if no document is available. (The order-view manages the set of articles
    * in the document GUI.)
    * 
    * @return  <code>DocumentOrderView</code> or null
    */ 
   public DocumentOrderView getSelectedOrderView () {
      DocumentDisplay display = getSelectedDisplay();
      DocumentOrderView view = display == null ? null : display.getOrderView();
      return view;
   }

   /** Returns the {@code DocumentDisplay} associated with the given document
    * or null if no display is associated.
    *  
    * @param document {@code PadDocument}
    * @return {@code DocumentDisplay}
    */
   public DocumentDisplay getDisplay (PadDocument document) {
	   for (DocumentDisplay display : displayList) {
		   if (document.equals(display.getDocument())) {
			   return display;
		   }
	   }
	   return null;
   }
   
   public void refreshCommandDisplay () {
      MenuHandler.get().refreshAll();
   }

   /** Attempts to restore focus to the last focused document view (Order or 
    * Article).
    */
   public void restoreFocus () {
      DocumentDisplay display = getSelectedDisplay();
      if (display != null) {
    	  display.restoreFocus();
      }
   }

   /** Switches the document column display modus between 'All', 'Article' and 'Tree'
    * in a round-robin fashion.
    */
   public void switch_document_display () {
      DocumentDisplay selectedDisplay = getSelectedDisplay();
      if ( selectedDisplay == null ) return;
      DisplayModus newValue;
      
      switch (selectedDisplay.getViewModus()) {
      case ComleteDisplay:
         newValue = DisplayModus.ArticleDisplay;
         break;
      case ArticleDisplay:
         newValue = DisplayModus.OrderDisplay;
         break;
      default:
         newValue = DisplayModus.ComleteDisplay;
         break;
      }
      selectedDisplay.setupView(newValue);
   }


   // ------------------ INNER CLASSES  --------------------------
   
   /** DocumentOrderView is an abstraction for a display based, user interactive
    * tool which expresses and modifies the set of articles within a 
    * {@code PadDocument} and includes a navigable  order applied over its 
    * elements.
    * <p>This interface defines a set of commands to programmatically interact 
    * with the view. The pad-document can be set-up and changed, redefining the content
    * of the display; furthermore, the view's property 'SELECTED ARTICLE'
    * can be handled. 
    * <p>This view issues property change events to registered listeners. 
    * The following properties are reported: 
    * <p><table width=600>
    * <tr><th align=left>KEY</th><th align=left>VALUE OLD</th><th align=left>VALUE NEW</th><th align=left>EVENT</th></tr>
    * <tr><td>selectionChanged</td><td>{@code PadArticle}</td><td>{@code PadArticle}</td><td>element selection change</td></tr>
    * <tr><td>documentChanged</td><td>{@code PadDocument}</td><td>{@code PadDocument}</td><td>document setup</td></tr>
    * <tr><td>structureChanged</td><td>{@code PadArticle} subtracted</td><td>{@code PadArticle} added</td><td>additions, subtractions, movements</td></tr>
    * </table>
    */
   public interface DocumentOrderView extends MenuActivist {
      
      /** Returns the Swing component which holds the order view.
       *  
       * @return JComponent
       */
      JComponent getView();
      
      /** Fires a property change event for the listeners to this view.
       * No controls are performed on the property name or the values.
       * 
       * @param propertyName String 
       * @param oldValue Object
       * @param newValue Object
       */
      void firePropertyChange (String propertyName, Object oldValue, Object newValue);

      /** The bound document of this view or null if no document is set up.
       * 
       * @return {@code PadDocument} or null
       */
      PadDocument getPadDocument();

//      /** Adds an element to this order view by stating a parent - element
//       * relation. If the order is maintained by a TREE (graph of elements)
//       * then parent represents the parent node of the given element. The 
//       * position of element in the field of the parent node is determined by
//       * the following rule: If a sorting is defined for the field then the 
//       * position is determined by the sort-order; otherwise if the current
//       * element selection is also in the field, the position is after the
//       * current element, otherwise it is at the end of the field.
//       * 
//       * @param element PadArticle article to-be-inserted
//       */
//      void addElement(PadArticle element);
      
//      void removeElement(PadArticle element);

      /** Sets the {@code PadDocument} for the content of this view.
       * If this view already contains a document, it is replaced and returned,
       * otherwise null is returned. The argument may be null in which case
       * the display results to be empty. Does nothing if the argument is 
       * identical to the installed document.
       * 
       * @param document {@code PadDocument}, may be null for no document
       * @return {@code PadDocument} or null
       */
      public PadDocument setPadDocument (PadDocument document);
      
      /** Moves the currently selected element one place upwards in the order.
       * The movement can be limited to preservation of the membership of the
       * element in its parent group. Does nothing if no element is selected.
       * 
       * @return boolean true = success, false = no change
       */
      boolean moveUp();

      /** Moves the currently selected element one place downwards in the order.
       * The movement can be limited to preservation of the membership of the
       * element in its parent group. Does nothing if no element is selected.
       * 
       * @return boolean true = success, false = no change
       */
      boolean moveDown();

      /** Changes the order level of the currently selected element one unit 
       * lower if possible (which means one place deeper in the graph). This 
       * works only for orders which support this kind of movement.
       * Does nothing if no element is selected.
       * 
       * @return boolean true = moved, false = no change
       */
      boolean indent();
      
      /** Changes the order level of the currently selected element one unit 
       * higher if possible (which means one place shallower in the graph). 
       * This works only for orders which support this kind of movement.
       * Does nothing if no element is selected.
       * 
       * @return boolean true = moved, false = no change
       */
      boolean outdent();

      // TODO
      PadArticle createChild (PadArticle art); 
      
      // TODO
      PadArticle createSibling (PadArticle art);
      
      /** Returns the currently selected element article or null if nothing 
       * is selected. (The article shall be derived from a display component
       * which associates articles to display elements.) 
       * 
       * @return {@code PadArticle} or null
       */
      PadArticle getSelectedElement ();
      
      /** Returns the first element article in this order view or null if the 
       * view is empty.
       * 
       * @return {@code PadArticle} or null
       */
      PadArticle getFirstElement ();
      
      /** Sets the current element selection in this view. Selection in 
       * the view is always single-selection.
       * 
       * @param article {@code PadArticle} article to select or null to 
       *                de-select
       * @return boolean true = selection changed, false = selection unchanged
       */
      boolean setSelectedElement (PadArticle article);
      
//      /** Removes all elements from this order view. This does not remove
//       * the elements from their database but is a pure display action!
//       */
//      void clear();
      
      /** Returns the number of elements in this order view. 
       */
      int size ();

      /** Requests the GUI focus to this order view.
       */
      void requestFocus();

      /** Adds a focus listener to this order view. This abstracts from whatever
       * hierarchy may exist in this display and delegates the listener to the 
       * relevant component.
       * 
       * @param flistener <code>FocusListener</code>
       */
      void addFocusListener (FocusListener flistener);
      
      /** Returns the mouse-listener active in this order-view.
       *  
       * @return {@code MouseListener}
       */
      MouseListener getMouseListener ();

      /** Adds a property change listener for this view. (See class description).
       * 
       * @param listener {@code PropertyChangeListener}, may be null
       */
      public void addPropertyChangeListener (PropertyChangeListener listener);
      
      /** Removes a property change listener from this view.
       * 
       * @param listener {@code PropertyChangeListener}, may be null
       */
      public void removePropertyChangeListener (PropertyChangeListener listener);

      /** Whether the contained document is set to read-only. Returns false if
       * not document is assigned.
       * 
       * @return boolean
       */
	  boolean isReadOnly ();
	  
	  /** This view is empty if not document is set up or if the document
	   * contains no articles.
	   * 
	   * @return boolean
	   */
	  boolean isEmpty ();
   }
   
   private interface DocumentDisplayField {
      
      void addDisplay (DocumentDisplay display, boolean toFront);
      void removeDisplay (DocumentDisplay display);
      void removeDisplay (int index);
      DocumentDisplay getSelectedDisplay ();
      void setSelectedDisplay(DocumentDisplay display);
      void updateDisplay(PadDocument doc);
      DocumentDisplay getDisplay(int index);
      DocumentDisplay[] getDisplays();
      int indexOf (PadDocument doc);
      void endEdit ();
      void clear ();
   }
   
   private class MultiDocumentDisplay extends JPanel implements DocumentDisplayField {
      JTabbedPane tabs;
//      DisplayChangeListener chListener = new DisplayChangeListener();
      
      public MultiDocumentDisplay () {
         super( new BorderLayout() );
         init();
      }
   
      private void init () {
         tabs = new JTabbedPane();

         // add a local change-listener to the tab-pane
         tabs.addChangeListener(new DisplayChangeListener());
         MDDMouseListener mouseListener = new MDDMouseListener(); 
         tabs.addMouseListener(mouseListener);
         tabs.addMouseWheelListener(mouseListener);
         
         this.add(tabs, BorderLayout.CENTER);
      }
      
      @Override
      public synchronized void removeDisplay (DocumentDisplay display) {
         if (display == null) return;
         int index = tabs.indexOfComponent(display);
         removeDisplay(index);
      }
      
      @Override
      public synchronized int indexOf (PadDocument doc) {
         if (doc == null)
            throw new IllegalArgumentException("doc is null");

         int size = tabs.getTabCount();
         for (int i = 0; i < size; i++) {
            Component c = tabs.getComponentAt(i);
            DocumentDisplay display = (DocumentDisplay)c;
            if ( display.getDocument().equals(doc) ) {
               return i;
            }
         }
         return -1;
      }
      
      private synchronized int indexOf (DocumentDisplay display) {
         if (display == null)
            throw new IllegalArgumentException("display is null");

         return tabs.indexOfComponent(display);
      }

      @Override
      public synchronized void addDisplay (DocumentDisplay display,
                                           boolean toFront) {
         if (display == null)
            throw new IllegalArgumentException("display is null");
         if (indexOf(display) > -1) {
            if (toFront) {
               setSelectedDisplay(display);
            }
            return;
         }

         // arrange a tab title for the given document
         PadDocument doc = display.getDocument();
         String title = doc.getShortTitle();
         if (title == null) {
            title = doc.getTitle();
         }
         
         // add a tab with the display to the end of the tab-list
         int index = tabs.getTabCount();
         String tooltip = doc.getToolTipText();
         tabs.insertTab(null, null, display, tooltip, index);
         createTabLabel(index, title);
         Log.log(8, "(MultiDocumentDisplay.addDisplay) DISPLAY added: " + title
               + ", counter=" + displayCount());

         // bring display to front (selected) if opted
         if (toFront) {
            tabs.setSelectedIndex(index);
         }
      }
      
      void createTabLabel (int index, String text) {
         if (getDisplay(index).getDocument().isModified()) {
            text = "*".concat(text);
         }
         JLabel label = new JLabel(text);
         tabs.setTabComponentAt(index, label);
      }
      
      /** Returns the currently selected document display or null
       * if the multi-document display is empty.
       * 
       * @return SingleDocumentDisplay or null
       */
      @Override
      public DocumentDisplay getSelectedDisplay () {
         return (DocumentDisplay)tabs.getSelectedComponent();
      }

      @Override
      public synchronized void removeDisplay (int index) {
         if ( index > -1 & index < tabs.getTabCount() ) {
            DocumentDisplay display = (DocumentDisplay)tabs.getComponentAt(index);
            display.finishEdit();
            tabs.removeTabAt(index);
            Log.log(8, "(MultiDocumentDisplay.removeDisplay) DISPLAY removed: " 
                  + display.getDocument().getTitle()
                  + ", counter=" + displayCount());
         }
      }

      @Override
      public synchronized DocumentDisplay[] getDisplays () {
         return (DocumentDisplay[])tabs.getComponents();
      }

      @Override
      public void clear () {
         tabs.removeAll();
         Log.log(8, "(MultiDocumentDisplay.clear) MULTI display cleared");
      }

      @Override
      public void endEdit () {
         for (DocumentDisplay display : getDisplays()) {
            display.finishEdit();
         }
      }
      
      private class DisplayChangeListener implements ChangeListener {
//         private int lastSelectedIndex = -1;
         private DocumentDisplay lastSelectedDisplay;
         
         @Override
         public void stateChanged (ChangeEvent e) {
            if ( e.getSource() == tabs ) {

               // determine selected index + display
               int selectedIndex = tabs.getSelectedIndex();
               DocumentDisplay display = (DocumentDisplay)tabs.getSelectedComponent();
               if (display != null) {
                  // react to change of display selection (multi-display)
                  if ( display != lastSelectedDisplay ) {
                     display.restoreFocus();
                     Global.getDocumentRegistry().setSelectedDocument(display.getDocument().getUUID());
   
                     String title = display.getDocument().getTitle();
                     Log.debug(10, "(DisplayChangeListener) new display selection, index " 
                           + selectedIndex + ": " + title);
                  }
               }
               
               // note new selected values
//               lastSelectedIndex = selectedIndex;
               lastSelectedDisplay = display;
            }
         }
      }

      private class MDDMouseListener extends MouseAdapter {

         @Override
         public void mouseClicked (MouseEvent e) {
            int selectedIndex = tabs.getSelectedIndex();
            int hitIndex = tabs.getUI().tabForCoordinate(tabs, e.getX(), e.getY());
               if (hitIndex == selectedIndex) {
               Log.debug(10,"(FocusMouseListener) mouse click on : " + selectedIndex);
//               getSelectedDisplay().setFocusedView(FocusableView.Article);
            }
         }
         
         @Override
         public void mouseWheelMoved (MouseWheelEvent e) {
            int size = tabs.getTabCount();
            if ( e == null || e.getSource() != tabs || size == 1 ) return;
            int delta = e.getWheelRotation();
            int index = tabs.getSelectedIndex();
            int newIndex = index + delta;
            newIndex = Math.min(size-1, Math.max(0, newIndex));
            if (index != newIndex) {
               tabs.setSelectedIndex(newIndex);
            }
         }
      }
      
      @Override
      public void setSelectedDisplay (DocumentDisplay display) {
         tabs.setSelectedComponent(display);
      }

      @Override
      public DocumentDisplay getDisplay (int index) {
         return (DocumentDisplay)tabs.getComponentAt(index);
      }

      @Override
      public void updateDisplay (PadDocument doc) {
         // update TAB title for all displays with the given document
         for (int index = 0; index < tabs.getTabCount(); index++) {
            DocumentDisplay display = (DocumentDisplay)tabs.getComponentAt(index);
            if (display.getDocument().equals(doc)) {
               String tabTitle = doc.getShortTitle();
               createTabLabel(index, tabTitle);
               Log.debug(10, "(MultiDocumentDisplay.updateDisplay) new TAB TITLE = "
                     .concat(tabTitle));
            }
         }
      }

   }


// -------------------------------------------------------------------
   
   public class SingleDocumentDisplay extends JPanel implements DocumentDisplayField {
      // -------------------------------------------------------------------
         
      DocumentDisplay display;
      
      /** Creates an empty SingleDocumentDisplay. 
       */
      public SingleDocumentDisplay () {
         super( new BorderLayout() );
      }
      
      /** Sets the given document display as the current display of this
       * display field. Clears if parameter is null.
       * 
       * @param display <code>DocumentDisplay</code>, may be null
       */
      public void setDisplay (DocumentDisplay display) {
         if ( display != null && display != this.display) {
            this.display = display;
            
            // make it visible
            this.removeAll();
            this.add(display);
            
            Log.log(8, "(SingleDocumentDisplay.setDisplay) display added for: " 
                  + display.getDocument().getTitle() + ", counter=" + displayCount());

         } else if (display == null & this.display != null) {
            this.removeAll();
            this.display = null;
         }
      }

      @Override
      public void addDisplay (DocumentDisplay display, boolean toFront) {
         if (display == null)
            throw new IllegalArgumentException("display is null");
         if (display == this.display) return;
         
         if ( this.display != null ) {
            this.display.finishEdit();
         }
         setDisplay(display);
      }

      @Override
      public void removeDisplay (DocumentDisplay display) {
         if ( display == this.display ) {
            display.finishEdit();
            clear();
            Log.log(8, "(SingleDocumentDisplay.removeDocument) document removed: " 
                  + display.getDocument().getTitle() + ", counter=" + displayCount());
         }
      }

      @Override
      public void removeDisplay (int index) {
         if ( index == 0 ) {
            removeDisplay(display);
         } throw new IndexOutOfBoundsException();
      }

      @Override
      public DocumentDisplay getSelectedDisplay () {
         return display;
      }

      @Override
      public int indexOf (PadDocument doc) {
         return display == null ? -1 : display.getDocument().equals(doc) ? 0 : -1;
      }

      @Override
      public DocumentDisplay[] getDisplays () {
         return display == null ? new DocumentDisplay[] {} :
                new DocumentDisplay[] {display};
      }

      @Override
      public void endEdit () {
         if ( display != null ) {
            display.finishEdit();
         }
      }

      @Override
      public void clear () {
         setDisplay(null);
         Log.log(8, "(SingleDocumentDisplay.clear) SINGLE display cleared");
      }

      @Override
      public void setSelectedDisplay (DocumentDisplay display) {
      }

      @Override
      public DocumentDisplay getDisplay (int index) {
         return index == 0 ? display : null;
      }

      @Override
      public void updateDisplay (PadDocument doc) {
      }
   }

   
   private class MainScreenPanel extends JPanel {
      private Component holder;
      
      public MainScreenPanel () {
         super( new BorderLayout() );
         init();
      }
      
      private void init () {
         setOpaque(true);
         setBackground(Color.yellow);
      }
      
      public void setContent ( Component c ) {
         
         // place the new content component 
         if ( holder != null ) {
            remove( holder );
         }
         if ( c != null ) {
            add( c, BorderLayout.CENTER );
            holder = c;
         }
         revalidate();
         repaint();
      }
      
      @SuppressWarnings("unused")
	  public Component getContent () {
         return holder;
      }
   }
   
   /** The ToolsPanel organises the toolbars on top of the main screen.
    * Currently there is only one such toolbar that is optionally visible 
    * or not visible.
    */
   private class ToolsPanel extends JPanel {
      
      public ToolsPanel () {
         super( new BorderLayout() );
         
         // add toolbar to panel
         JToolBar toolbar = ToolbarHandler.get().getMainToolBar();
         add( toolbar, BorderLayout.NORTH );
      }
   }
   
   private class PopupAction extends AbstractAction {
	   private Action action;
	 
	   /** Creates a normalised popup-action from the given Action instance.
	    * 
	    * @param a {@code Action}
	    */
	   public PopupAction (Action a) {
		   Objects.requireNonNull(a);
		   action = a;
		   
		   putValue(ACTION_COMMAND_KEY, a.getValue(ACTION_COMMAND_KEY));
	       putValue(NAME, a.getValue(NAME));
	       putValue(SELECTED_KEY, a.getValue(SELECTED_KEY));
	       setEnabled(a.isEnabled());
	   }
	   
	  @Override
	  public void actionPerformed (ActionEvent e) {
	      action.actionPerformed(e);
	  }
   }
   
   /**
    * This is a listener to global application options AND to the
    * Document Registry. It reacts to document entries, removals
    * and selection changes (singular) with display related actions.
    */
   private class RegistryListener implements PropertyChangeListener {

      @Override
      public void propertyChange (PropertyChangeEvent evt) {
         String key = evt.getPropertyName();

         if ( key == "defaultDocumentDisplayModus" ) {
            String value = (String)evt.getNewValue();
            defaultDisplayModus = DisplayModus.fromString(value);

         } else if ( key == "documentAdded" ) {
            PadDocument document = (PadDocument)evt.getNewValue();
            if (!isDocumentShowing(document)) {
            	addDocumentDisplay(document);
            	Global.getDocumentRegistry().setSelectedDocument(document.getUUID());
            }
            
         } else if ( key == "documentRemoved" ) {
            PadDocument document = (PadDocument)evt.getNewValue();
            removeDocumentDisplay(document);
         
         } else if ( key == "documentReplaced" ) {
             PadDocument newDoc = (PadDocument)evt.getNewValue();
             DocumentDisplay display = getDisplay(newDoc);
             if (display != null) {
            	 display.setDocument(newDoc);
             }
          
         } else if ( key == "documentSelected" ) {
            PadDocument document = (PadDocument)evt.getNewValue();
            if (document != null) {
	        	if (multiDisplay != null && currentDisplay == multiDisplay &&
	                Util.notEqual(multiDisplay.getSelectedDisplay().getDocument(), document)) {
	               int index = multiDisplay.indexOf(document);
	               DocumentDisplay display = multiDisplay.getDisplay(index); 
	               Log.debug(10, "(DisplayManager.RegistryListener) display selection from REGISTRY, index " 
	                     + index + ": " + display.getDocument().getShortTitle());
	               multiDisplay.setSelectedDisplay(display);
	            }
            }
         }
         
      }
   }
   
   
   
   // -------------------------------------------------------------------
      
   /** A display panel for a single Pad-Document. Organises a JPanel
    * to show editable components to manage the contents of a document,
    * which comprises the article tree, article editor and useful managing
    * buttons.
    */
      public class DocumentDisplay extends JPanel {
         
         PadDocument document;
         PadArticle  currentArticle;
         Document emptyEditorDocument = new PlainDocument();
         DocumentListener docListener = new DocumentListener(this);
         FocusMouseListener focusMouseListener = new FocusMouseListener();
         DocumentOrderView orderView;
         JSplitPane splitPane;
         JPanel leftComponent;
         JPanel rightComponent;
         OrderViewPanel treePanel;
         ArticlePanel articlePanel;
         DisplayModus viewModus;
         Font defaultTextFont;
         Color defaultBackgroundColor, defaultForegroundColor;
         int dividerPosition;
         /** most recently focused display view (Article or Order) */ 
         FocusableView focus = FocusableView.Order;
         
         /** Creates an empty <code>DocumentDisplay</code>. The content
          * can be added with "setDocument()". 
          */
         public DocumentDisplay () {
            super( new BorderLayout() );
            init();
         }
         
         /** Creates a <code>DocumentDisplay</code> with the given document
          * as content.
          *  
          * @param doc <code>PadDocument</code>
          * @throws IllegalArgumentException if doc is null
          */
         public DocumentDisplay (PadDocument doc) {
            this();
            setDocument(doc);
         }
         
         private void init () {
//            orderView = new ListOrderView();
            orderView = new JTreeOrderView();
            treePanel = new OrderViewPanel();
            leftComponent = treePanel;
            articlePanel = new ArticlePanel();
            rightComponent = articlePanel;
            
            FocusListener fListener = new FocusListener() {
               @Override
               public void focusLost (FocusEvent e) {
                  if (document == null) return;
                  boolean isEditor = e.getComponent() == getEditorView();
                  if (isEditor) {
                     captureEditorProperties();
                  }
                  Log.debug(10, "(DocumentDisplay.FocusListener) " +
                          document.getShortTitle()  + ", focus lost == " + focus);
               }
               
               @Override
               public void focusGained (FocusEvent e) {
                  if (document == null) return;
                  focus = e.getComponent() == getEditorView() ? FocusableView.Article : FocusableView.Order; 
                  if (focus == FocusableView.Order) {
                	  restoreTextSelection();
                  }
                  Log.debug(10, "(DocumentDisplay.FocusListener) " +
                        document.getShortTitle()  + ", focus gained == " + focus);
               }
            };

            /** A listener to changes in the order-view.
             */
            PropertyChangeListener pListener = new PropertyChangeListener() {
               @Override
               public void propertyChange (PropertyChangeEvent evt) {
                  if (evt.getSource() != getOrderView()) return;
                  
                  if (evt.getPropertyName() == "selectionChanged") {
                     // TODO article end-edit
                     captureEditorProperties();

                     // install new article into editor
                     PadArticle article = (PadArticle)evt.getNewValue();
                     Log.log(8, "(DocumentDisplay.PropertyChangeListener) received selection change event, new selection == "
                          + article );
                     setupEditor(article);
                    
                     // inform document about selection change
                     if (article != null) {
                        document.setSelectedArticle(article);
                     }

                  } else if (evt.getPropertyName() == "documentChanged") {
                	  // TODO ?
                  }
              }
               
            };
            
            orderView.addFocusListener(fListener);
            orderView.addPropertyChangeListener(pListener);
            getEditorView().addFocusListener(fListener);
            
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
            setDefaultDividerPosition();
            setupView(getDefaultDisplayModus());
         }
         
         public DocumentOrderView getOrderView () {
            return orderView;
         }
         
         private JTextComponent getEditorView () {
            return articlePanel.editor.getView();
         }
         
         /** The text editor for the article of this display.
          * 
          * @return {@code ArticleEditor}
          */
         public ArticleEditor getArticleEditor () {return articlePanel.editor;}
         
         /** Returns the UndoManager of the currently focused work-panel of this
          * display (order-view or article-view).
          *  
          * @return {@code UndoManager}
          */
         public UndoManager getUndoManger () {
            UndoManager man = getLastFocusedView() == FocusableView.Article ?
        			 getArticleEditor().getUndoManager() : getDocument().getUndoManager();
        	return man;
         }
         
         /** Prints the currently selected branch or article, according to the
          * given print parameters.
          *  
          * @param param {@code PrintParameters}
          * @return boolean true == print was performed, 
          *                  false == print error or user interrupted
          */
         public synchronized boolean printArticles (PrintParameters param) {
        	Objects.requireNonNull(param);
        	PadDocument document = param.document;
            PadArticle article = param.printScope == PrintScope.DOCUMENT ? 
            			document.getArticle(0) : param.article;
            int index = document.indexOf(article);
            if (index < 0) 
            	throw new IllegalArgumentException("article not found");
            
            if (param.printModus != ArticlePrintModus.CONTINUOUS) {
                GUIService.infoMessage("Print Job", "The selected print modus is currently not available");
            	return false;
            }
            
            // setup an editor for printing
            JTextArea editor = new JTextArea("");
//            JTextComponent artEditor = getEditorView();

            Font printFont = param.font;
            editor.setFont(printFont);
            editor.setBackground(Color.white);
            editor.setLineWrap(param.wrapLines);
            editor.setWrapStyleWord(param.wrapWords);

            // create a print-page header
            MessageFormat header = null;
            if (param.printHeaders) {
            	StringBuffer sbuf = new StringBuffer(256);
	            sbuf.append(param.headerText);
	//             sbuf.append(sbuf.length() > 40 ? "\r\n" : ", ");
	//             sbuf.append(", ");
	            sbuf.append(", {0}");
	            header = new MessageFormat(sbuf.toString());
            }

//            boolean isOneJob = param.printScope == PrintScope.ARTICLE ||
//            				   param.printModus != ArticlePrintModus.SEPARATE;
            boolean appendTitles = param.printTitles;
            
            // modus: print articles continuous
            if (param.printModus == ArticlePrintModus.CONTINUOUS) {
            	StringBuffer textBuf = new StringBuffer(2048);
            	String text;
            	
            	// collect texts from articles
            	switch (param.printScope) {
            	case ARTICLE:
        			text = article.getContent().trim();
        			if (!(param.ignoreEmpty && text.isEmpty())) { 
	    				if (appendTitles) {
	    					textBuf.append("\n---------- " + article.getTitle() + "\n\n");
	    				}
	            		textBuf.append(text);
	            		textBuf.append("\n");
        			}
            		break;
            	case BRANCH:
            	case DOCUMENT:
            		PadArticle[] arts = document.getArticlesAt(index);
            		for (PadArticle a : arts) {
            			text = a.getContent().trim();
            			if (param.ignoreEmpty && text.isEmpty()) continue;
            			
	    				if (appendTitles) {
	    					textBuf.append("\n---------- " + a.getTitle());
	    				}
        				if (!text.isEmpty()) {
        					textBuf.append("\n\n");
                    		textBuf.append(text);
                    		textBuf.append("\n");
        				}
            		}
            		break;
            	}
            	
            	editor.setText(textBuf.toString());
            
	            // run the print job
	            try {
	            	text = editor.getText();
	                return editor.print(header, null);
	            } catch (PrinterException e) {
	                  e.printStackTrace();
	                  GUIService.failureMessage("Unable to print selected object!", e);
	                  return false;
	            }
            }

            // modus: print articles on new page
//			PadArticle[] arts = param.printScope == PrintScope.ARTICLE ? 
//					            new PadArticle[] {article} : document.getArticlesAt(index);
//            int ct = 0;
//			for (PadArticle a : arts) {
//				// text and header
//            	StringBuffer textBuf = new StringBuffer(2048);
//				textBuf.append(a.getContent().trim());
//			    if (param.printHeaders) {
//			    	String hs = a.getTitle() + ", {0}";
//			        header = new MessageFormat(hs);
//			    } else {
//			    	header = null;
//    				if (appendTitles) {
//    					textBuf.insert(0, "---------- " + a.getTitle() + "\n\n");
//    				}
//			    }
//				editor.setText(textBuf.toString());
//
//			    // run the print job
//			    try {
//			    	if (ct++ == 0) {
//			    		editor.print(header, null);
//			    	} else {
//			    		editor.print(header, null, false, null, null, true);
//			    	}
//			    } catch (PrinterException e) {
//			          e.printStackTrace();
//			          GUIService.failureMessage("Unable to print selected object!", e);
//			          return false;
//			    }
//			}
			return true;
         }
         
         /** Returns an enum value indicating which of ORDER or ARTICLE view
          * has been last focused.
          * 
          * @return {@code FocusableView}
          */
         public FocusableView getLastFocusedView () {return focus;}
         
         /** Attempts to restore focus to the last focused view (Order or Article).
          */
         public void restoreFocus () {
            if ( focus == FocusableView.Article ) {
            	Log.log(10, "-- attempting to restore ARTICLE focus");
               getEditorView().requestFocusInWindow();
            } else {
//               treePanel.restoreFocus();
            	Log.log(10, "-- attempting to restore ORDER focus");
               getOrderView().requestFocus();
            }
         }

         /** Sets the focused view (memorises and changes focus if necessary).
          * 
          * @param view {@code FocusableView}
          */
         public void setFocusedView (FocusableView view) {
            focus = view == null ? FocusableView.Order : view;
            restoreFocus();
         }
         
         /** Returns the renderer of an EDIT menu dependent on the currently
          * focused view.
          * 
          * @return {@code MenuActivist}
          */
         public MenuActivist getEditMenuAcivist () {
        	 switch (getLastFocusedView()) {
			 case Article:
				 return getArticleEditor();
			 case Order:
			 default:
				 return getOrderView();
        	 }
         }
         
         /** Captures text selection, cursor position and other properties
          * from the editor and stores them into the current pad-article. 
          */
         private void captureEditorProperties () {
            JTextComponent editor = getEditorView();
            PadArticle article = getSelectedArticle();
            if (article != null) {
               int mark = editor.getCaret().getMark();
               int dot = editor.getCaret().getDot();
               Rectangle rect = editor.getVisibleRect();
               article.setCursorPosition(dot);
               article.setSelectionMark(mark);
               article.setDocumentVisibleRect(rect);
               Log.log(10, "(DocumentDisplay.captureEditorProperties) capture cursor/selection for "
                     .concat(article.toString()));
            }
         }
         
         /** Restore display in editor with last noted text selection and 
          * cursor position from current article. This also scrolls to the
          * memorised visible rectangle.
          */
         private void restoreTextSelection () {
            JTextComponent editor = getEditorView();
            PadArticle article = getSelectedArticle();
            if (article != null) {
               try {
            	  // restore text selection
                  int mark = article.getSelectionMark();
                  int dot = article.getCursorPosition();
                  if ( mark == dot ) {
                      editor.select(dot, dot);
                  } else {
                	  Dimension sel = article.getSelection();
                	  editor.select(sel.width, sel.height);
                  }

                  // restore visible rectangle
                  Rectangle rect = article.getVisibleRect();
                  editor.scrollRectToVisible(rect);
                  Log.log(10, "(DocumentDisplay.restoreTextSelection) restore cursor/selection for "
                        .concat(article.toString()));
                  
                  editor.repaint();
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
         }
         
         /** Returns the currently selected article in this display or
          * null if there is no article selected.
          * 
          * @return <code>PadArticle</code> or null
          */
         public PadArticle getSelectedArticle () {
            return currentArticle;
         }

         /** Returns the currently selected text section or null if no text is 
          * selected.
          * 
          * @return String or null
          */
         public String getSelectedText () {
            return articlePanel.editor.getSelectedText();
         }

         /** Sets this display's divider position to the system default value.
          */
         private void setDefaultDividerPosition () {
            int value = Global.getOptions().getIntOption("defaultSliderPosition");
            dividerPosition = value > 0 ? value : DEFAULT_DIVIDER_LOCATION;
            splitPane.setDividerLocation(dividerPosition);
         }
         
         /** Sets up this display's view in relation to the two
          * primary panels "order-view" and "article-editor". This includes
          * the setting of the split-pane divider position.
          * 
          * @param modus DisplayModus modus
          * @return boolean true == view has been arranged and repainted
          */
         public boolean setupView ( DisplayModus modus ) {
            if ( modus == viewModus ) return false;
            dividerPosition = splitPane.getDividerLocation();
            removeAll();
   
            // realise the display modus (order-view / article)
            switch (modus) {
            case ComleteDisplay :
               splitPane.setLeftComponent(leftComponent);
               splitPane.setRightComponent(rightComponent);
               splitPane.setDividerLocation(dividerPosition);
               add(splitPane);
               focus = FocusableView.Article;
               break;
            case ArticleDisplay :
               add(rightComponent);
               focus = FocusableView.Article;
               break;
            case OrderDisplay :
               add(leftComponent);
               focus = FocusableView.Order;
               break;
            }
            viewModus = modus;
            revalidate();
            repaint();
            restoreFocus();

            // refresh menus if this is the selected display
            if ( isSelectedDisplay() ) {
               MenuHandler.get().refreshViewMenu();
            }
            return true;
         }
   
         public void finishEdit () {
            if ( document != null ) {
               document.setPreferredDividerPosition(splitPane.getDividerLocation());
               document.setPreferredDisplayModus(getViewModus());
               document.setOrderviewFont(orderView.getView().getFont());

               // finish article editor values
               if (currentArticle != null) {
                  captureEditorProperties();
//                  document.setSelectedArticle(currentArticle);
               }
               
               // store preferences
               PersistentOptions options = document.getOptions();
               if (orderView instanceof JTreeOrderView) {
            	   byte[] info = ((JTreeOrderView)orderView).getTreeExpansionInfo();
                   options.setOption("tree-expansion-info", Util.bytesToHex(info));
               }
               
               Log.log(6, "(DocumentDisplay.finishEdit) document finished: " 
                        + document.getTitle());  
            }
         }

         /** Returns an enum value indicating the graphical setup of the document
          * display (Order, Article or both).
          *  
          * @return DisplayModus
          */
         public DisplayModus getViewModus () {
            return viewModus;
         }
         
         /** Returns the tree expansion data for serialisation from the current 
          * order view if it is a tree-order-view or null otherwise.
          *  
          * @return byte[] or null
          */
         public byte[] getTreeExpansionInfo () {
      	     DocumentOrderView view = getOrderView(); 
      	     if (view instanceof JTreeOrderView) {
      		     return ((JTreeOrderView)view).getTreeExpansionInfo();
      	     }
      	     return null;
         }
         
         /** Sets the active content for this document display.
          * If a document is currently held by this display, its edition is
          * finished and it is removed. <code>null</code> can be used to 
          * remove the current document from display.
          * 
          * @param doc <code>PadDocument</code>, may be null
          */
         public void setDocument (final PadDocument doc) {
        	 if (doc == document) return;
        	 Log.log(5, "(DocumentDisplay.setDocument) setting display to " + doc);

            // finish a previous document edition
            finishEdit();
            if (document != null) {
            	document.removePropertyChangeListener(docListener);
            }
            
            if (doc == null) {
               document = null;
               removeAll();
               setupEditor(null);
               setupOrderView(null);
               
            } else {
               // take over parameter values from the document
               document = doc;
               focus = FocusableView.Article;
               defaultBackgroundColor = doc.getPreferredBackgroundColor();
               defaultForegroundColor = doc.getPreferredForegroundColor();
               defaultTextFont = doc.getDefaultTextFont();    

               // set the split-pane divider position after document value
               if ( doc.getPreferredDividerPosition() > 0 ) {
                  splitPane.setDividerLocation( doc.getPreferredDividerPosition() );
               } else {
                  setDefaultDividerPosition();
               }
   
               // set up the display-modus (order-view/article) 
               // includes restore the last component focus
               setupView( doc.getPreferredDisplayModus() );
               Log.log(8, "(DocumentDisplay.setDocument) document defined: " 
                     + doc.getTitle());
               
               // set up editor content and order-view content
               PadArticle selectedArticle = doc.getSelectedArticle();
               setupEditor(selectedArticle);
               setupOrderView(selectedArticle);
               
               // finally hook into document events
               document.addPropertyChangeListener(docListener);
               updateDocumentDisplay(document);
            }
         }
   
         /** Sets up content and appearance of the article editor in dependence
          * of settings in document and specified article.
          * 
          * @param article {@code PadArticle} to be shown or null if nothing is 
          *                to be shown
          */
         private void setupEditor (PadArticle article) {
        	if (article != null && (article.getDocument() == null || article.getDocument() != document))
        		throw new IllegalArgumentException("unrelated article in " + document);
        	
        	// set current article setting and exchange document listener
            JTextComponent editor = getEditorView();
            if (currentArticle != null) {
            	editor.removePropertyChangeListener(currentArticle.getEditorListener());
            }
            currentArticle = article;
            if (article != null) {
            	editor.addPropertyChangeListener(article.getEditorListener());
            }
            Log.debug(6, "(DocumentDisplay.setupEditor) setting up EDITOR with ARTICLE " + article);

            if (article == null) {
               // set default appearance values (for "no article")
               editor.setBackground(EMPTY_EDITOR_COLOR);
               editor.setFont(defaultTextFont);
               editor.setEnabled(false);
     // TODO create Document after document type ?               
               editor.setDocument(emptyEditorDocument);
               editor.select(0, 0);

            } else {
                // conditional settings
                editor.setEditable(!document.isReadOnly());
                if (editor instanceof JTextArea) {
             	   ((JTextArea)editor).setLineWrap(article.getLineWrap());
                }

               // set appearance for "article present"
               Color color = !editor.isEditable() ? READONLY_EDITOR_COLOR :
            	     article.getBackgroundColor() != null ?
                     article.getBackgroundColor() : defaultBackgroundColor;
               editor.setBackground(color);
               color = !editor.isEditable() ? READONLY_TEXT_COLOR :
          	     	article.getForegroundColor() != null ?
          	     	article.getForegroundColor() : defaultForegroundColor;
               editor.setForeground(color);
               Font font = article.getDefaultFont() != null ? article.getDefaultFont()
                     : defaultTextFont;
               editor.setFont(font);
               editor.setEnabled(true);
               
               // give text to the editor
               Document editorDocument = article.getEditorDocument();
               articlePanel.editor.setDocument(editorDocument);
               
               // set text selection, cursor position and visible rect
               restoreTextSelection();
            }
         }
         
         /** Sets the current text selection in the editor component without
          * directly modifying corresponding article values.
          * 
          * @param sel Dimension (width=start, height=end)
          */
         public void setEditorTextSelection (Dimension sel) {
             JTextComponent editor = getEditorView();
             editor.select(sel.width, sel.height);
         }
         
         private void setupOrderView (PadArticle selectedArticle) {
        	 orderView.setPadDocument(document);
        	 
            // set visibility and content of document MARKER panel
            long backupTime = document == null ? 0 : document.getBackupTime();
            if (backupTime > 0) {
          	   treePanel.markerLabel.setText("BACKUP of " + Util.localeTimeString(backupTime));
            }
      	    treePanel.markerPanel.setVisible(backupTime > 0);

            // select article, choose first article if parameter is null
            if (selectedArticle == null & !orderView.isEmpty()) {
               selectedArticle = orderView.getFirstElement();
            }
            orderView.setSelectedElement(selectedArticle);
            
       	 	// set the background color of the order-view
            Color bgdCol = document != null && document.isReadOnly() ? READONLY_ORDERVIEW_COLOR : ORDER_VIEW_COLOR;
            treePanel.setBackground(bgdCol);
            
            // set font if defined in document
            Font font = document == null ? null : document.getOrderviewFont();
            if (font != null) {
            	orderView.getView().setFont(font);
            }
         }
         
         public PadDocument getDocument () {
            return document;
         }

         public boolean isSelectedDisplay () {
            return getSelectedDisplay() == this;
         }
         
         // ----------------------------------------------
         
         private class FocusMouseListener extends MouseAdapter {

            @Override
            /** This mouse-listener reacts to MOUSE-PRESSED events on any
             * JComponent and depending on a property "FOCUS_TARGET" of that
             * component installs the focus either on the OrderView or the
             * ArticleView. If no property is found, the focus is installed
             * on the latest reported location (editor or order-view). 
             */
            public void mousePressed (MouseEvent e) {
               try {
                  String property = (String)((JComponent)e.getComponent())
                                    .getClientProperty("FOCUS_TARGET");
                  if ("ORDERVIEW" == property) {
                     focus = FocusableView.Order;
                  } else if ("ARTICLEVIEW" == property) {
                     focus = FocusableView.Article;
                  }
               } catch (Exception ex) {
                  ex.printStackTrace();
               }
               restoreFocus();

               // protocol
               String objName = e.getComponent().getName();
               if (objName == null || objName.isEmpty()) {
            	   objName = e.getComponent().getClass().getSimpleName();
               }
               Log.debug(10,"(DocumentDisplay.MDDMouseListener) mouse pressed on : " + objName);
            }
         }

         private class OrderViewPanel extends JPanel {
            JPanel viewPanel;
            JPanel markerPanel;
            JLabel markerLabel;
            JScrollPane scrollPane;
            
            public OrderViewPanel () {
               super( new BorderLayout() );
               
               // add toolbar to panel NORTH
               JPanel northPanel = new JPanel(new BorderLayout());
               add(northPanel, BorderLayout.NORTH);
               JToolBar toolbar = ToolbarHandler.createTreeToolBar();
               northPanel.add(toolbar, BorderLayout.NORTH);

               // prepare panel and label for BACKUP file marker
               markerPanel = new JPanel(new BorderLayout());
               markerPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 2, 5));
               markerPanel.setBackground(UnixColor.Aquamarine);
               markerLabel = new JLabel("BACKUP");
               markerPanel.add(markerLabel);
               markerPanel.setVisible(false);
               northPanel.add(markerPanel, BorderLayout.SOUTH);
               
               // let all toolbar buttons force focus to Order view 
               assert focusMouseListener != null;
               for (Component c : toolbar.getComponents()) {
            	   if (c instanceof JButton) {
            		   JButton b = (JButton) c;
                       b.putClientProperty("FOCUS_TARGET", "ORDERVIEW");
                       b.addMouseListener(focusMouseListener);
//                       Log.debug(8, "-- order toolbar: " + b.getActionCommand());
            	   }
               }
         
               // create content panel + scroll pane
               viewPanel = new JPanel(new VerticalFlowLayout() );
               viewPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 0));
//               viewPanel.setOpaque(false);
               viewPanel.setBackground(ORDER_VIEW_COLOR);
               scrollPane = new JScrollPane(viewPanel);
               scrollPane.putClientProperty("FOCUS_TARGET", "ORDERVIEW");
               add(scrollPane);
               
               // add order-view (ordering structure)
               orderView.getView().setBackground(ORDER_VIEW_COLOR);
               viewPanel.add(orderView.getView());
               scrollPane.addMouseListener(orderView.getMouseListener());
               
               // add mouse listener to capture activation clicks
               scrollPane.addMouseListener(focusMouseListener);
               JScrollBar hBar = scrollPane.getHorizontalScrollBar(); 
               hBar.putClientProperty("FOCUS_TARGET", "ORDERVIEW");
               hBar.addMouseListener(focusMouseListener);
               JScrollBar vBar = scrollPane.getVerticalScrollBar();
               vBar.putClientProperty("FOCUS_TARGET", "ORDERVIEW");
               vBar.addMouseListener(focusMouseListener);
            }

			@Override
			public void setBackground(Color bgd) {
				super.setBackground(bgd);
				if (viewPanel != null) {
					viewPanel.setBackground(bgd);
				}
				if (orderView.getView() != null) {
					orderView.getView().setBackground(bgd);
				}
			}
            
            
//            public void restoreFocus () {
////            	viewPanel.requestFocusInWindow();
//            }
         } // OrderViewPanel

         private class ArticlePanel extends JPanel {
        	ArticleEditor editor;
            
            public ArticlePanel () {
               super( new BorderLayout() );
            
               // add a toolbar to panel
               JToolBar toolbar = ToolbarHandler.createArticleToolBar();
               JPanel p1 = new JPanel(new BorderLayout());
               p1.add(toolbar);
               add(p1, BorderLayout.NORTH);
             
               // let all toolbar buttons force focus to Article view 
               for (Component c : toolbar.getComponents()) {
            	   if (c instanceof JButton) {
            		   JButton b = (JButton) c;
                       b.putClientProperty("FOCUS_TARGET", "ARTICLEVIEW");
                       b.addMouseListener(focusMouseListener);
//                       Log.debug(8, "-- article toolbar: " + b.getActionCommand());
            	   }
               }
         
               // create text editor
               editor = new PlainTextEditor("ArticleEditor");
         
               // create scroll pane
               JScrollPane scrollPane = new JScrollPane(editor.getView());
               scrollPane.putClientProperty("FOCUS_TARGET", "ARTICLEVIEW");
               add(scrollPane);

               // add focus mouse listener
               scrollPane.addMouseListener(focusMouseListener);
               JScrollBar hBar = scrollPane.getHorizontalScrollBar(); 
               hBar.putClientProperty("FOCUS_TARGET", "ARTICLEVIEW");
               hBar.addMouseListener(focusMouseListener);
               JScrollBar vBar = scrollPane.getVerticalScrollBar();
               vBar.putClientProperty("FOCUS_TARGET", "ARTICLEVIEW");
               vBar.addMouseListener(focusMouseListener);
            }
            
//            public ArticleEditor getEditor () {return editor;}
         }

         private abstract class AbstractOrderView implements DocumentOrderView {
             protected PadDocument bindingDoc;
             protected SelectionListener componentSelectionListener = new SelectionListener();
             protected PropertyChangeSupport support = new PropertyChangeSupport(this);
		     protected PadArticle lastSelected;
             
             /** A listener to {@code PadDocument} objects' "selectionChanged" 
              * property change.
              */
             private PropertyChangeListener docChangeListener = new PropertyChangeListener() {
				@Override
				public void propertyChange (PropertyChangeEvent evt) {
					PadArticle article = (PadArticle) evt.getNewValue();
					PadArticle oldArticle =  (PadArticle) evt.getOldValue();
					setSelectedElement(article);
		            firePropertyChange("selectionChanged", oldArticle, article);
				}
			 };
			 
			 @Override
			 public void addFocusListener (FocusListener flistener) {
			    getView().addFocusListener(flistener);
			 }

			 @Override
			 public void addPropertyChangeListener (PropertyChangeListener listener) {
			     support.addPropertyChangeListener(listener);
			 }

			 @Override
			 public boolean isReadOnly () {
				 return bindingDoc == null ? false : bindingDoc.isReadOnly();
			 }
			 
			@Override
			public PadArticle getFirstElement () {
			   return isEmpty() ? null : bindingDoc.getArticle(0);
			}

			@Override
			public boolean indent () {
			   int index = getSelectedIndex();
			   if (index > 1 & !isReadOnly()) {
			      PadArticle item = bindingDoc.getArticle(index);
			      // jump back over descendants of predecessor
			      int depth = item.getOrderDepth();
			      int i = index;
			      while (--i > 0 && bindingDoc.getArticle(i).getOrderDepth() > depth) 
			         ;
			      int index2 = i;
			      PadArticle pred = bindingDoc.getArticle(index2);
			      
			      // if selected article is a sibling to predecessor
			      if (pred.getOrderDepth() == item.getOrderDepth()) {
			         // insert article as child to predecessor 
			         PadArticle[] aPack = bindingDoc.cutoutArticleAt(index);
			         bindingDoc.insertArticleAt(pred, index, aPack);
			         setSelectedIndex(index);
			      }
			      return item.getOrderDepth() > depth;
			   }
			   return false;
			}

			@Override
			public boolean moveDown () {
			   int size = bindingDoc.getArticleCount();
			   int index1 = getSelectedIndex();
			   if (index1 > 0 & index1 < size-1 & !isReadOnly()) {
			      // calculate target position
			      PadArticle article = bindingDoc.getArticle(index1);
			      int depth = article.getOrderDepth();
			      int i = index1;
			      while (++i < size && bindingDoc.getArticle(i).getOrderDepth() > depth) 
			         ;
			
			      // if next article of same depth is available
			      if (i < size && bindingDoc.getArticle(i).getOrderDepth() == depth) {
			         // jump over next position + descendants
			         depth = bindingDoc.getArticle(i).getOrderDepth();
			         while (++i < size && bindingDoc.getArticle(i).getOrderDepth() > depth) 
			            ;
			         int index2 = i;
			         
			         // cause article swapping in document
			         PadArticle[] aPack = bindingDoc.cutoutArticleAt(index1);
			         index2 -= aPack.length;
			         bindingDoc.insertArticleAt(article.getParent(), index2, aPack);
			         setSelectedIndex(index2);
			      }
			      return getSelectedIndex() > index1;
			   }
			   return false;
			}


			//            private int indexOf (PadArticle article) {
			//            	return bindingDoc.indexOf(article);
			//            }
			            
			//            public void addElement (UUID parent, UUID element) {
			//               if (element == null)
			//                  throw new IllegalArgumentException("element is null");
			//               
			//               // we add an article to the view which is already contained
			//               // in the document
			//               PadArticle article = getDocument().getArticle(element);
			//               if (article != null) {
			//                  PadArticle parentA = getDocument().getArticle(parent);
			//                  addElement(parentA, article);
			//               }
			//            }
			
			//            /** Adds an article to this view. Semantical tests are performed
			//             * to verify matching of the article to the existing set of this
			//             * list view. The resulting list index in the model is identical 
			//             * with the article's index in its document.  
			//             * 
			//             * @param article <code>PadArticle</code>
			//             * @throws IllegalArgumentException if article is null, has no
			//             *          document OR does not fit the existing model content
			//             */
			//            @Override
			//            public void addElement (PadArticle article) {
			//               if (article == null)
			//                  throw new IllegalArgumentException("article is null");
			//
			//               PadDocument document = article.getDocument();
			//               PadArticle parent = article.getParent();
			//               if (bindingDoc == null) {
			//                  bindingDoc = document;
			//               }
			//               
			//               // test for semantical correctness of article
			//               if (document == null)
			//                  throw new IllegalArgumentException("article has no document");
			//               if (Util.notEqual(bindingDoc, document))
			//                  throw new IllegalArgumentException("false document of article");
			//               if (parent != null && model.indexOf(parent) == -1 )
			//                  throw new IllegalArgumentException("article parent not found");
			//               
			//               int index = document.indexOf(article); 
			//               model.insertElementAt(article, index);
			//            }
			
			//            @Override
			//            public void removeElement (PadArticle element) {
			//               if (element == null) return;
			//               boolean isSelected = element.equals(getSelectedElement());
			//               
			//               int index = indexOf(element);
			//               if (index > -1) {
			//                  model.remove(index);
			//                  if (isSelected) {
			//                     index = Math.min(index, size()-1);
			//                     setSelectedIndex(index);
			//                  }
			//               }
			//            }
			
            @Override
            public boolean moveUp () {
               int index1 = getSelectedIndex();
               if (index1 > 1 & !isReadOnly()) {
                  // calculate target position
                  PadArticle article = bindingDoc.getArticle(index1);
                  int depth = article.getOrderDepth();
                  int parentIndex = bindingDoc.indexOf(article.getParent());
                  int i = index1;
                  while (--i > parentIndex && bindingDoc.getArticle(i).getOrderDepth() > depth) 
                     ;
                  int index2 = i > parentIndex ? i : parentIndex+1;
                  
                  if (index2 < index1) {
                     // cause article swapping in document
                     PadArticle[] aPack = bindingDoc.cutoutArticleAt(index1);
                     bindingDoc.insertArticleAt(article.getParent(), index2, aPack);
                     setSelectedIndex(index2);
                  }
			      return getSelectedIndex() < index1;
               }
               return false;
            }


			@Override
			public boolean outdent () {
			   int index = getSelectedIndex();
			   if (index > 1 & !isReadOnly()) {
			      PadArticle item = bindingDoc.getArticle(index);
			      int depth1 = item.getOrderDepth();
			      PadArticle parent = item.getParent();
			      // if parent of selected article is not the root node
			      if (parent.getOrderDepth() > 0) {
			         PadArticle[] aPack = bindingDoc.cutoutArticleAt(index);
			
			         // insert article as sibling to parent 
			         int i = bindingDoc.indexOf(parent);
			         int depth = parent.getOrderDepth();
			         while (++i < bindingDoc.getArticleCount() && 
			            bindingDoc.getArticle(i).getOrderDepth() > depth)
			            ;
			         int tIndex = i;
			         bindingDoc.insertArticleAt(parent.getParent(), tIndex, aPack);
			         setSelectedIndex(tIndex);
			      }
			      return item.getOrderDepth() < depth1;
			   }
			   return false;
			}

			@Override
			public PadArticle createChild (PadArticle where) {
				if (isReadOnly()) return null;
	        	PadArticle art = document.newArticle(where, true);
	            setSelectedElement(art);
				return art;
			}

			@Override
			public PadArticle createSibling(PadArticle where) {
				if (isReadOnly()) return null;
	        	PadArticle art = document.newArticle(where, false);
	            setSelectedElement(art);
				return art;
			}

			@Override
			public void removePropertyChangeListener (
			      PropertyChangeListener listener) {
			   support.removePropertyChangeListener(listener);
			}


			@Override
			public void requestFocus () {
			   getView().requestFocusInWindow();
			}

			@Override
			public boolean isEmpty() {
				return bindingDoc == null || bindingDoc.getArticleCount() == 0;
			}

			@Override
			public int size () {
			   return bindingDoc == null ? 0 : bindingDoc.getArticleCount();
			}
        	 
            public void setSelectedIndex (int index) {
            	try {
            		PadArticle art = bindingDoc.getArticle(index);
            		setSelectedElement(art);
            	} catch (IndexOutOfBoundsException e) {
            		e.printStackTrace();
            	}
            }
        	 
            /** Returns the index value of the currently selected element
             * in this view or -1 if nothing is selected.
             *  
             * @return int element index or -1
             */
            public int getSelectedIndex () {
            	PadArticle sel = getSelectedElement();
            	int index = bindingDoc.indexOf(sel);
            	return index;
            }

            @Override
            public PadDocument getPadDocument () {return bindingDoc;}
            
			@Override
			public PadDocument setPadDocument (PadDocument document) {
				if (document == bindingDoc) return null;
				PadDocument oldDoc = bindingDoc;
				
				// remove document listener from existing (bound) document
				if (bindingDoc != null) {
					bindingDoc.removePropertyChangeListener("selectionChanged", docChangeListener);
//					getView().setVisible(false);
				}

				// set new value for bound document
				// if we have a document, add our document listener
				if (document == null) {
					bindingDoc = null;
				} else {
					bindingDoc = document;
					bindingDoc.addPropertyChangeListener("selectionChanged", docChangeListener);
				}
            	
//            	getView().setVisible(true);
                support.firePropertyChange("documentChanged", oldDoc, document);
				return oldDoc;
			}
			
			@Override
			public void firePropertyChange (String propertyName, Object oldValue, Object newValue) {
				Log.log(8, "(AbstractOrderView.firePropertyChange) Property Change = " + propertyName 
						+ ", V1=" + oldValue + ", V2=" + newValue);
				support.firePropertyChange(propertyName, oldValue, newValue);
			}

			@Override
			public JMenu getJMenu() {
				JMenu menu = new JMenu();
				JMenuItem item;
				UndoManager undoManager = bindingDoc.getUndoManager();
				ActionHandler ah = ActionHandler.get();
				Action action;

				if (bindingDoc.isReadOnly()) {
					// case read-only state
					try {
						action = ah.getAction(ActionHandler.ActionNames.ORDER_EDIT_COPY);
						menu.add(new PopupAction(action));
						action = ah.getAction(ActionHandler.ActionNames.ORDER_DOC_TOOLBOX);
						menu.add(new PopupAction(action));
					} catch (UnknownActionException e) {
					} 
					
				} else {
					// case editable state
				    if (undoManager.canUndo()) {
					      item = new JMenuItem( new UndoAction(undoManager) );
					      item.setAccelerator( KeyStroke.getKeyStroke(
					            KeyEvent.VK_Z, ActionEvent.CTRL_MASK) );
					      menu.add( item );
					}
					   
					if (undoManager.canRedo()) {
					      item = new JMenuItem( new RedoAction(undoManager) );
					      item.setAccelerator( KeyStroke.getKeyStroke(
					            KeyEvent.VK_Y, ActionEvent.CTRL_MASK) );
					      menu.add( item );
				    }
	
					int offset = menu.getMenuComponentCount();
					if (offset > 0) {
						menu.addSeparator();
						offset++;
					}
					
					for (Action a : getMenuActions()) {
						menu.add(new PopupAction(a));
					}
					
					if (menu.getMenuComponentCount() >= 10) {
						menu.insertSeparator(4 + offset);
						menu.insertSeparator(7 + offset);
						menu.insertSeparator(12 + offset);
						
						// add DUPLICATE ARTICLE command
						try {
							action = ah.getAction(ActionHandler.ActionNames.ORDER_CREATE_DUPL);
							menu.insert(new PopupAction(action), 7 + offset);
						} catch (UnknownActionException e) {
						}
					}
				}
				return menu;
			}

			@Override
			public List<Action> getMenuActions () {
				return ActionHandler.get().getActionList(ToolbarHandler.TOOLBAR_VALUES_TREE);
			}

			private class UndoAction extends TextAction	{
				UndoManager undoManager;

				public UndoAction (UndoManager manager) {
			      super( manager.getUndoPresentationName() );
			      putValue(ACTION_COMMAND_KEY, "menu.edit.undo");
			      undoManager = manager;
			   }
			
				@Override
				public void actionPerformed ( ActionEvent e ) {
			       if (undoManager.canUndo()) {
			          undoManager.undo();
			       }
			    }
			}
			
			private class RedoAction extends TextAction	{
				UndoManager undoManager;

			   public RedoAction (UndoManager manager) {
			      super(manager.getRedoPresentationName());
			      putValue(ACTION_COMMAND_KEY, "menu.edit.redo");
			      undoManager = manager;
			   }
			
			   @Override
			   public void actionPerformed ( ActionEvent e ) {
			      if (undoManager.canRedo()) {
			         undoManager.redo();
			      }
			   }
			}

			/** This row selection listener (for jList or JTree) fetches the 
			  * document's article assigned to the selected row, marks it
			  * as SELECTED and causes the document to assume the new selection. 
			  */
			 private class SelectionListener implements ListSelectionListener, TreeSelectionListener {
			     
				@Override
				public void valueChanged (TreeSelectionEvent e) {
			        if (e == null) return;
			        selectionChanged();
				}
			
				@Override
				public void valueChanged (ListSelectionEvent e) {
			        if (e == null || e.getValueIsAdjusting()) return;
			        selectionChanged();
				}
				 
				private void selectionChanged () {
			        PadArticle selArticle = getSelectedElement();
			        
			        if (Util.notEqual(selArticle, lastSelected)) {
			           Log.log(8, "(AbstractOrderView) component event: SELECTION CHANGED: " +
			              lastSelected + " --> " +  selArticle );
			           
			           lastSelected = selArticle;
			           PadDocument document = getPadDocument();
			           if (document != null) {
			        	   document.setSelectedArticle(lastSelected);
			           
//				           // TEST function: display depth and index of selected article
//				           if (selArticle != null && isSelectedDisplay()) {
//				              int index = document.indexOf(selArticle);
//				              String text = "index=" + index + 
//				                    "  depth=" + selArticle.getOrderDepth() +
//				                    "  parent=" + selArticle.getParent();
//				              Global.getStatusBar().clearMessage();
//				              Global.getStatusBar().putMessage(text, 4000, UnixColor.Indigo);
//				           }
			           }
			        }
				}
			 }
         } // AbstractOrderView
         
         private class JTreeOrderView extends AbstractOrderView {
             private JTree jTree;
             private TreeModel model;
             private MouseListener mouseListener;
             
             JTreeOrderView () {
                 init();
             }

             private void init () {
            	 jTree = new JTree();
            	 jTree.setOpaque(false);
            	 jTree.setBackground(UnixColor.Chartreuse);
            	 jTree.setEditable(true);
            	 jTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);;
            	 jTree.getSelectionModel().addTreeSelectionListener(componentSelectionListener);
            	 model = jTree.getModel();

                 jTree.setCellRenderer(new DefaultTreeCellRenderer() {
                     // this cell renderer ensures that no empty string is
                     // inserted as list row. Empty shortTitles from a pad-
                     // article are substituted by a string containing a blank.
                     // This solution only affects display and we need not bear
                     // weird restrictions in the object.
                     @Override
                     public Component getTreeCellRendererComponent (JTree tree, Object value, 
                    		 boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                    	if (value == null) return null;
                    	JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, 
                    			 sel, expanded, leaf, row, hasFocus);
                    	
                    	if (value instanceof PadArticle) {
                        	// determine text value
	                        PadArticle a = (PadArticle) value;
	                        String s = a.toString();
	                        if (s == null || s.isEmpty()) {
	                           label.setText(" ");
	                        }
	                        
	                        // set text color and background
	                        Color fColor = getDepthColor(a.getOrderDepth());
	                        label.setForeground(fColor);
	                        label.setOpaque(!sel);
                    	}
                        
                        return label;
                     }
                  });

                 // add a listener to mouse wheel events over the list
                 // we move the item selection over the showing nodes
                 MouseWheelListener mwListener = new MouseWheelListener() {
                    // this mouse-wheel listener enables scrolling through the
                    // list rows by turning the mouse wheel. Nice!
                    @Override
                    public void mouseWheelMoved (MouseWheelEvent e) {
                       if (e == null || e.getSource() != jTree
                           || model.getRoot() == null  ) return;
                       int delta = e.getWheelRotation();
                       int index = getSelectedIndex();
                       int newIndex = index;
                       TreePath path;
                       do {
	                       index = newIndex;
	                       newIndex = index + delta;
	                       newIndex = Math.min(size()-1, Math.max(0, newIndex));
	                       path = bindingDoc.getArticleTreePath(bindingDoc.getArticle(newIndex));
                       } while (newIndex != index && !jTree.isVisible(path));
                       
                       if (index != newIndex) {
                          setSelectedIndex(newIndex);
                       }
                    }
                 };
                 jTree.addMouseWheelListener(mwListener);
                 
                 // add a mouse listener (popup menu)
                 mouseListener = new  MouseAdapter() {
                    @Override
            		public void mousePressed(MouseEvent e) {
            		   maybeShowPopup(e);
            		}
            		
            		@Override
            		public void mouseReleased(MouseEvent e) {
            		   maybeShowPopup(e);
            		}
            		
            		private void maybeShowPopup(MouseEvent e) {
//            			Log.log(8, "(JTreeOrderView.mouseListener) popup check");
            		    if (e.isPopupTrigger()) {
            		       JPopupMenu pm = getPopupMenu();
            		       if (pm != null) {
            		    	   requestFocus();
            		    	   pm.show(e.getComponent(), e.getX(), e.getY());
            		       }
            		    }
            		}
                 };
                 jTree.addMouseListener(mouseListener);
                 
//                 TimerTask treeTest = new TimerTask() {
//					@Override
//					public void run() {
//						byte[] info = Util.getTreeExpansionInfo(jTree);
//						info = Util.reversedArray(info);
//						String text = "TREE expansion = " + Util.bytesToHex(info) + ", rows=" + jTree.getRowCount();
//						Global.getStatusBar().putMessage(text , UnixColor.Crimson);
//						
//					}
//                 };
//                 Global.getTimer().schedule(treeTest,  1000, 2000);
             }  // init
             
			@Override
			public JComponent getView() {return jTree;}

			@Override
			public PadArticle getSelectedElement() {
				PadArticle sel = (PadArticle)jTree.getLastSelectedPathComponent();
				return sel;
			}

			@Override
			public boolean setSelectedElement (PadArticle article) {
		        if (Util.notEqual(article, lastSelected)) {
			        Log.log(8, "(JTreeOrderView.setSelectedElement) setting TREE PATH article selection: " + article );
//			        lastSelected = article;

			        if (article == null) {
			        	jTree.clearSelection();
			        } else {
			           
			        	TreePath path = article.getTreePath();
						TreePath selPath = jTree.getSelectionPath();
						if (Util.notEqual(path, selPath)) {
							jTree.setSelectionPath(path); 
							jTree.scrollPathToVisible(path);
						}
			        }
			        return true;
		        }
		        return false;
			}
             
            @Override
            public PadDocument setPadDocument (PadDocument document) {
            	if (document == bindingDoc) return null;
                Log.debug(6, "(JTreeOrderView.setPadDocument) setting up document with " + document);
            	PadDocument oldDoc = bindingDoc;
            	super.setPadDocument(document);
            	
            	// create the model for JTree
            	if (document == null) {
            		model = null;
            		jTree.setModel(null);
            	} else {
            		// setting data model from new document
            		model = document.getTreeModel();
               	 	model.addTreeModelListener(new ModelListener());
            		jTree.setModel(model);
            		jTree.revalidate();
            		jTree.repaint();

            		// reading tree expansion info from document and restoring state of jTree
            		String hstr = document.getOptions().getOption("tree-expansion-info");
            		byte[] info = Util.hexToBytes(hstr);
           		 	setTreeExpansionFromInfo(info);

           		 	// setting document's selected article
            		PadArticle selected = document.getSelectedArticle();
            		if (selected != null) {
            			setSelectedElement(selected);
            		}
            	}
            	
                return oldDoc;
            }

            public byte[] getTreeExpansionInfo () {
            	byte[] info = Util.getTreeExpansionInfo(jTree);
            	Log.log(8, "(JTreeOrderView) extracting JTree expansion value: " + Util.bytesToHex(info));
       		    return info;
            }
            
            public void setTreeExpansionFromInfo (byte[] info) {
            	Log.log(8, "(JTreeOrderView) setting JTree expansion from value " + Util.bytesToHex(info));
            	Util.setTreeExpansionFromInfo(jTree, info);
            }

			@Override
			public MouseListener getMouseListener() {return mouseListener;}
			
			private class ModelListener implements TreeModelListener {
				@Override
				public void treeNodesChanged (TreeModelEvent e) {
				}

				@Override
				public void treeNodesInserted (TreeModelEvent e) {
					firePropertyChange("structureChanged", null, null);	// TODO values for events
				}

				@Override
				public void treeNodesRemoved (TreeModelEvent e) {
					firePropertyChange("structureChanged", null, null);
				}

				@Override
				public void treeStructureChanged (TreeModelEvent e) {
					firePropertyChange("structureChanged", null, null);
				}
			}
         } // JTreeOrderView

		private class DocumentListener implements PropertyChangeListener {
		      DocumentDisplay display;
		      
		      DocumentListener (DocumentDisplay display) {
		         if (display == null)
		            throw new NullPointerException("display is null");
		         
		         this.display = display;
		      }
		      
		      @Override
		      public void propertyChange (PropertyChangeEvent evt) {
		         
		         DocumentOrderView orderView = display.getOrderView();
		         ArticleEditor editor = display.getArticleEditor();
		         PadDocument document = (PadDocument)evt.getSource();
		         String key = evt.getPropertyName();
		
		//         if ( key == "defaultDocumentDisplayModus" ) {
		//            String value = (String)evt.getNewValue();
		//            defaultDisplayModus = DisplayModus.fromString(value);
		//
		
		//         Log.log(6, "(DisplayManager.DocumentListener) received PROPERTY CHANGE: "
		//               .concat(key));
		         
		         // TODO react to selection change
		         
		         if ( key == "titleChanged" ) {
		            updateDocumentDisplay(document);
		            
		         } else if ( key == "documentModified" ) {
		            updateDocumentDisplay(document);
		            
//		         } else if ( key == "articleTitleChanged" ) {
		//            updateDocumentDisplay(document);
		
		         } else if ( key == "articleAdded" ) {
		            PadArticle article = (PadArticle)evt.getNewValue();
		//            orderView.addElement(article);
		            orderView.setSelectedElement(article);
		
		         } else if ( key == "articleRemoved" ) {
		//            PadArticle article = (PadArticle)evt.getNewValue();
		//            orderView.removeElement(article);
		            orderView.setSelectedElement(null);
		
		         } else if ( key == "readOnlyChanged" ) {
		        	 boolean setting = (Boolean) evt.getNewValue();
		        	 editor.setReadOnly(setting);
		//        	 orderView.setReadOnly(setting);
		
		        	 // set background and foreground color of the text component
		             PadArticle article = display.getSelectedArticle();
		             Color bgdCol = setting ? READONLY_EDITOR_COLOR :
		        	     (article != null && article.getBackgroundColor() != null) ?
		                 article.getBackgroundColor() : display.defaultBackgroundColor;
		             display.getEditorView().setBackground(bgdCol);
		             Color fgrCol = setting ? READONLY_TEXT_COLOR :
		        	     (article != null && article.getForegroundColor() != null) ?
		                 article.getForegroundColor() : display.defaultForegroundColor;
		             display.getEditorView().setForeground(fgrCol);
		
		        	 // set the background color of the order-view
		             bgdCol = setting ? READONLY_ORDERVIEW_COLOR : ORDER_VIEW_COLOR;
		             display.treePanel.setBackground(bgdCol);
		         }
		      }
		   }
         
//         private class ListOrderView extends AbstractOrderView {
//            private JList<PadArticle> jList;
//            private AbstractListModel<PadArticle> model;
//            
//            ListOrderView () {
//                init();
//             }
//             
////            ListOrderView (PadDocument document) {
////               Objects.requireNonNull(document);
////               bindingDoc = document;
////               init();
////            }
//            
//            private void init () {
//               model = new DefaultListModel<>();
//               jList = new JList<>(model);
//               jList.setOpaque(false);
//               jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//
//               // add a listener to the list's row selection events
//               jList.getSelectionModel().addListSelectionListener(componentSelectionListener);
//               
//               // customise cell rendering
//               jList.setCellRenderer(new DefaultListCellRenderer() {
//                  // this cell renderer ensures that no empty string is
//                  // inserted as list row. Empty shortTitles from a pad-
//                  // article are substituted by a string containing a blank.
//                  // This solution only affects display and we need not bear
//                  // weird restrictions in the object.
//                  @Override
//                  public Component getListCellRendererComponent (JList<?> list,
//                        Object value, int index, boolean isSelected,
//                        boolean cellHasFocus) {
//                	 if (value == null) return null;
//                	 JLabel label = (JLabel)super.getListCellRendererComponent(list, value,
//                			index, isSelected, cellHasFocus);
//                     PadArticle a = (PadArticle)value;
//                     String s = a.toString();
//                     if (s == null || s.isEmpty()) {
//                        label.setText(" ");
//                     }
//                     
//                     // set text color
//                     Color fColor = getDepthColor(a.getOrderDepth());
//                     label.setForeground(fColor);
//                     
//                     return label;
//                  }
//               });
////               jList.setDragEnabled(true);
//               
//               // add a listener to mouse wheel events over the list
//               // we move the item selection over the list
//               MouseWheelListener mwListener = new MouseWheelListener() {
//                  // this mouse-wheel listener enables scrolling through the
//                  // list rows by turning the mouse wheel. Nice!
//                  @Override
//                  public void mouseWheelMoved (MouseWheelEvent e) {
//                     if (e == null || e.getSource() != jList 
//                         || model.getSize() == 0  ) return;
//                     int delta = e.getWheelRotation();
//                     int index = jList.getSelectedIndex();
//                     int newIndex = index + delta;
//                     newIndex = Math.min(model.getSize()-1, Math.max(0, newIndex));
//                     if (index != newIndex) {
//                        setSelectedIndex(newIndex);
//                     }
//                  }
//               };
//               jList.addMouseWheelListener(mwListener);
//               
//               // add a mouse listener to allow in-situ editing of article names
//               MouseListener mouseListener = new  MouseAdapter() {
//                  // this mouse listener enables and triggers off at double-click
//                  // editing of a list cell's article title. The modified
//                  // value goes directly into the article object and the row
//                  // is refreshed.
//                  EditListAction<PadArticle> action = new EditListAction<PadArticle>() {
//                	  @Override
//                	  protected void applyValueToModel(String value, ListModel<PadArticle> model, int row) {
//                		  PadArticle art = model.getElementAt(row); 
//                		  art.setTitle(value);
//                	  }
//                  };
//                  
//                  @Override
//                  public void mouseClicked (MouseEvent e) {
//                     if (e == null) return;
//                     if (e.getClickCount() >= 2) {
//                        action.actionPerformed(new ActionEvent(jList, 0, ""));
//                     }
//                  }
//               };
//               jList.addMouseListener(mouseListener);
//            }  // init
//
//            @Override
//            public JComponent getView () {
//               return jList;
//            }
//
//            @Override
//            public PadArticle getSelectedElement () {
//               int index = jList.getSelectedIndex();
//               if (index > -1) {
//                  PadArticle article = model.getElementAt(index);
//                  return article;
//               }
//               return null;
//            }
//            
////            private void setSelectedIndex (int index) {
////               if (model == null) return;
////               if (index > -1 & index < model.getSize()) {
////                  jList.setSelectedIndex(index);
////                  jList.ensureIndexIsVisible(index);
////               }
////            }
//
//            @Override
//            public void setSelectedElement (PadArticle article) {
//               if (article != null & jList != null) {
//                  jList.setSelectedValue(article, true);
//               }
//            }
//
//            @Override
//            public void setPadDocument (PadDocument document) {
//            	PadDocument oldDoc = bindingDoc;
//            	if (document == bindingDoc) return;
//            	super.setPadDocument(document);
//            	
//            	if (document == null) {
//            		model = null;
//            	} else {
//            		model = document.getListModel();
//            		jList.setModel(document.getListModel());
//            	}
//                support.firePropertyChange("documentChanged", oldDoc, document);
//            }
//
//			@Override
//			public MouseListener getMouseListener() {return null;}
//
//         }  // ListOrderView
         
      }  // DocumentDisplay
         

}
