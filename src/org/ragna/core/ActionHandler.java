/*
*  File: ActionHandler.java
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

package org.ragna.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import org.ragna.core.PadDocument.DocumentType;
import org.ragna.front.AboutDialog;
import org.ragna.front.ArticleEditor;
import org.ragna.front.ColorChooserDialog;
import org.ragna.front.DialogDocumentToolbox;
import org.ragna.front.DisplayManager;
import org.ragna.front.DisplayManager.DisplayModus;
import org.ragna.front.DisplayManager.DocumentDisplay;
import org.ragna.front.DisplayManager.DocumentOrderView;
import org.ragna.front.DisplayManager.FocusableView;
import org.ragna.front.DocumentEncryptionDialog;
import org.ragna.front.DocumentEncryptionDialog.TerminationType;
import org.ragna.front.GUIService;
import org.ragna.front.PreferencesDialog;
import org.ragna.front.PrintSelectorDialog;
import org.ragna.front.ResolveMirrorDialog;
import org.ragna.front.util.DialogButtonBar;
import org.ragna.front.util.FontChooser;
import org.ragna.front.util.HtmlBrowserDialog;
import org.ragna.front.util.MessageDialog;
import org.ragna.front.util.MessageDialog.MessageType;
import org.ragna.front.util.ResourceLoader;
import org.ragna.front.util.SetStackMenu;
import org.ragna.io.IO_Manager;
import org.ragna.io.IO_Manager.StreamDirection;
import org.ragna.io.IO_Manager.SystemFileType;
import org.ragna.util.ActionManager;
import org.ragna.util.PersistentOptions;

import kse.utilclass.misc.Log;
import kse.utilclass.misc.UUID;
import kse.utilclass.misc.UnixColor;
import kse.utilclass.misc.Util;
import kse.utilclass2.misc.MirrorFileManager;
import kse.utilclass2.misc.MirrorFileManager.Mirrorable;

public class ActionHandler extends ActionManager {
   
//   private static final int NUMBER_OF_WORKERS = 2;

   // *** STATIC HANDLING *** 
   
   // Singleton-Handling
   private static ActionHandler instance;
   
   public static ActionHandler get () {
      if ( instance == null ) {
         instance = new ActionHandler();
      }
      return instance;
   }

   // *** MEMBER VARIABLES  ***
   
   private ActionListener executor = new ActionExecutor();
//   private Map<UUID, MirrorFileAdapter> mirrorAdapterMap = new Hashtable<>();
   private MirrorHandler mirrorHandler = new MirrorHandler();
   private PadArticle[] articleStack;

   private ActionHandler () {
      super();
      init();
   }
   
   private void init () {
      Global.getDocumentRegistry().addPropertyChangeListener(mirrorHandler);
      
      try {
         // define cardinal actions
    	 Action action;
         addAction(ActionNames.SYSTEM_EXIT);
         
         addAction(ActionNames.FILE_NEW);
         addAction(ActionNames.FILE_OPEN, true);
         addAction(ActionNames.FILE_OPEN_SESSION);
         addAction(ActionNames.FILE_OPEN_URL);
         addAction(ActionNames.FILE_CLOSE);
         addAction(ActionNames.FILE_CLOSE_ALL);
         addAction(ActionNames.FILE_OPEN_RECENT, true);
         addAction(ActionNames.FILE_DELETE);
         
         addAction(ActionNames.FILE_SAVE);
         addAction(ActionNames.FILE_SAVE_ALL);
         addAction(ActionNames.FILE_SAVE_AS);
         addAction(ActionNames.FILE_SAVE_COPY);
         addAction(ActionNames.FILE_RELOAD);
         
         addAction(ActionNames.EDIT_COPY);
         addAction(ActionNames.EDIT_CUT);
         addAction(ActionNames.EDIT_PASTE);
         addAction(ActionNames.EDIT_DELETE);
         addAction(ActionNames.EDIT_UNDO);
         addAction(ActionNames.EDIT_REDO);
         addAction(ActionNames.EDIT_INSERT_DATE);
         addAction(ActionNames.EDIT_INSERT_TIME);
         addAction(ActionNames.EDIT_INSERT_TABLE);
         addAction(ActionNames.EDIT_INSERT_IMAGE);
         addAction(ActionNames.EDIT_INSERT_FILE);
         addAction(ActionNames.EDIT_EXPORT_FILE);
         addAction(ActionNames.EDIT_CHOOSE_FONT);
         addAction(ActionNames.EDIT_COLOR_BGD);
         addAction(ActionNames.EDIT_COLOR_FGR);

         addAction(ActionNames.ORDER_EDIT_COPY);
         addAction(ActionNames.ORDER_EDIT_CUT);
         addAction(ActionNames.ORDER_EDIT_PASTE);
         addAction(ActionNames.ORDER_EDIT_DELETE);

         addAction(ActionNames.ORDER_CREATE_CHILD);
         addAction(ActionNames.ORDER_CREATE_SIBLING);
         addAction(ActionNames.TREE_MOVE_UP);
         addAction(ActionNames.ORDER_MOVE_DOWN);
         addAction(ActionNames.ORDER_MOVE_INDENT);
         addAction(ActionNames.ORDER_MOVE_OUTDENT);
         addAction(ActionNames.ORDER_DOC_TOOLBOX);

         addAction(ActionNames.ACTION_PRINT);
         addAction(ActionNames.ACTION_SORT);
         action = addAction(ActionNames.ACTION_SEARCH);
         action.setEnabled(false);
         addAction(ActionNames.ACTION_SEARCH_WEB);
         addAction(ActionNames.ACTION_TRANSLATE_WEB);
         addAction(ActionNames.ACTION_LINEWRAP_HARD);
         addAction(ActionNames.ACTION_DOCUMENT_MODIFY);

         addAction(ActionNames.VIEW_MAIN_TOOLBAR);
         addAction(ActionNames.VIEW_TREE_TOOLBAR);
         addAction(ActionNames.VIEW_ARTICLE_TOOLBAR);
         addAction(ActionNames.VIEW_DISPLAY_SWITCH);
         addAction(ActionNames.VIEW_DISPLAY_ALL);
         addAction(ActionNames.VIEW_DISPLAY_ARTICLE);
         addAction(ActionNames.VIEW_DISPLAY_TREE);
         addAction(ActionNames.VIEW_FONT_ENLARGE);
         addAction(ActionNames.VIEW_FONT_DECREASE);
         
         addAction(ActionNames.MANAGE_BACKUP);
         addAction(ActionNames.MANAGE_RESTORE);
         addAction(ActionNames.MANAGE_ENCRYPTION);
         addAction(ActionNames.MANAGE_PREFERENCES);
         addAction(ActionNames.MANAGE_DELETE_HISTORY);
//         addAction(ActionNames.MANAGE_IMPORT_RAGNA);
//         addAction(ActionNames.MANAGE_IMPORT_TEXT);
//         addAction(ActionNames.MANAGE_IMPORT_TREE_FILE);
//         addAction(ActionNames.MANAGE_IMPORT_TREE_CLIPBOARD);
//         addAction(ActionNames.MANAGE_EXPORT_RAGNA);
//         addAction(ActionNames.MANAGE_EXPORT_TEXT);
//         addAction(ActionNames.MANAGE_EXPORT_TREE_FILE);

         addAction(ActionNames.HELP_ABOUT);
         addAction(ActionNames.HELP_INFO_SYSTEM);
         addAction(ActionNames.HELP_USER_MANUAL);
         addAction(ActionNames.HELP_LICENSE);

      } catch ( Exception e ) {
         e.printStackTrace();
      }
   }

   
   @SuppressWarnings("unused")
   private Action addIconAction ( String actionName, String title, String iconFile ) 
         throws DuplicateException {
      ImageIcon icon = new ImageIcon( iconFile ); 
      Action a = super.addAction(actionName, title, actionName, false);
      a.putValue(Action.SMALL_ICON, icon);
      return a;
   }
   
   @Override
   public ActionListener getActionListener() {
      return executor;
   }
   
   @Override
   public Timer getTimer () {
      return Global.getTimer();
   }

   /** Terminates action execution and releases resources of the ActionHandler.
    * (Actions in the state "executing" will still run to their end.)
    */
   @Override
   public void exit () {
      super.exit();
      Global.getDocumentRegistry().removePropertyChangeListener(mirrorHandler);
   }
   
//   public static JMenuItem getStartBrowserAction (URL url, boolean b) {
//      return null;
//   }
//
//   public static JMenuItem getStartEmailAction (String text, boolean b) {
//      return null;
//   }

   /** Creates and returns an Action which tries to load a pad-document from
    * the given filepath and displays it in ALTERNATE modus. 
    * This action can be used for setup in a menu and runs synchronously.
    *  
    * @param file File serialisation file of a pad-document
    * @param origDoc original document in case a copy is loaded; may be null
    * @return {@code Action}
    */
   public Action getLoadDocumentAction (File file, PadDocument origDoc) {
	   Objects.requireNonNull(file);
	   Runnable run = new Runnable () {
		  @Override
		  public void run() {
			  viewDocumentAsAlternate(file, origDoc);
		  }
	   };
	   String title = Util.localeTimeString(file.lastModified());
	   Action act = new SynchronAction("action.load.document", title, run);
	   return act;
   }
   
   /** Closes all globally registered documents. Also stores a session-list
    * to global options.
    * Function may get interrupted by user interaction with consequence that
    * the document registry is not empty after completion of this method.
    * 
    * @return boolean true == all closed, false == some still open
    */
   public boolean collectiveDocumentClose () {
	  DocumentRegistry registry = Global.getDocumentRegistry();
      SetStackMenu sessionList = new SetStackMenu();
      Global.getMirrorFileManager().pause();
      
      try {
      // STEP I
      // define session list and close unmodified documents
      for (PadDocument document : registry) {
          String filepath = document.getExternalPath();
          
          // if document unmodified, close unchanged
          if (!document.isModified()) {
        	  closeDocument(document, false, false);
          }

          // if file is new (no path) then try to save it (define path)
          else if (filepath == null) {
        	  // ask user whether to save file
        	  String text = displayText("msg.ask.save.doc").concat(document.getTitle());
        	  int choice = GUIService.confirmMessage2(null, "dlg.title.closing.doc", text, GUIService.YES_NO_CANCEL_OPTION);

        	  // save or close unsaved (remove) depending on user choice
        	  switch (choice) {
 	          case GUIService.YES_OK_BUTTON_PRESSED:
 	             if (!saveDocumentToFileSystem(document, true)) return false;
 	        	 filepath = document.getExternalPath();
 	          case GUIService.NO_BUTTON_PRESSED:
 	        	 closeDocument(document, false, false);
 	             break;
 	          default: return false;
        	  }
          }

          // add the document's path to the session list
          if (filepath != null && !document.isBackupCopy()) {
             sessionList.add(filepath);
          }
      }

      // STEP II
      // close modified non-new documents
      for (PadDocument document : registry) {
    	  boolean allOption = document.isModified() && registry.size() > 1;
          if (!closeDocument(document, true, allOption)) {
             // break the operation if user opted for cancel in dialog
             // or a dialog thread was interrupted
             return false;
          }
      }
      } finally {
          Global.getMirrorFileManager().resume();
      }

      if (!sessionList.isEmpty()) {
	      // put the session list to program options
	      String content = sessionList.getStringContent(';');
	      Global.getOptions().setOption("recentSessionContent", content);
	      Log.debug(10, "(ActionHandler) saving session content: ".concat(content));
	      Global.saveSystemOptions();
      }

      // all went well
      return true;
   }
   
   /** This task controls all registered (open in display) documents for
    * any mirror files and handles cases via GUI interaction.
    */
   private class ControlDocumentMirrorsTask implements Runnable {

	  @Override
	  public void run() {
		  for (PadDocument doc : Global.getDocumentRegistry()) {
			 if (doc.hasExternalFile()) {
				 controlDocumentMirrors(doc);
			 }
		  }
	  }
   }
   
   /** This task opens a document from a serialisation file and brings it
    * to display, while it remains silent if the file does not exist or
    * is already open in the registry.
    */
   private class OpenDocumentFromPathTask implements Runnable {
      private String filepath;
      private boolean controlMirrors;

      /** Creates a new open task for the given document file. 
       * 
       * @param filepath String document file to open
       * @param controlMirrors boolean if true the document's mirrors 
       *        are controlled when this task is executed
       */
      OpenDocumentFromPathTask (String filepath, boolean controlMirrors) {
    	 Objects.requireNonNull(filepath);
         this.filepath = filepath;
         this.controlMirrors = controlMirrors;
      }

      @Override
      public void run () {
    	 // open and register the document file
         File file = new File(filepath);
         if (file.isFile() && !Global.getDocumentRegistry().isRegisteredDocument(filepath)) {
            openDocumentFromPath(filepath, null, controlMirrors);
         }
      }
   }
   
   /** Task to save a single PadDocument to its serialisation file.
    * No confirmation message is given. 
    */
   private class SaveDocumentTask implements Runnable {
	   private PadDocument document;
	   
	   SaveDocumentTask (PadDocument doc) {
		   Objects.requireNonNull(doc);
		   document = doc;
	   }
	   
       @Override
       public void run () {
          saveDocumentToFileSystem(document, false);
       }
   }
   
   /** Opens the documents of the most recent program session if they are not
    * yet open. This method schedules worker-tasks and returns immediately.
    */
   public void openSession () {
	  // retrieve the list of session files
      String content = Global.getOptions().getOption("recentSessionContent");
      SetStackMenu sessionList = new SetStackMenu();
      sessionList.loadStringContent(content, ';');
      int size = sessionList.size();
      
      // create and start open tasks for each session file (worker threads) 
      Log.debug(10, "(ActionHandler) opening session content: ".concat(content));
      for (Object obj : sessionList.getList()) {
         String path = (String)obj;
         Runnable task = new OpenDocumentFromPathTask(path, size == 1);
         scheduleWorkerTask(task, "open session document: ".concat(path));
      }
      
      // if several files are scheduled, start a watchdog to inject a mirror control task
      // after all outstanding worker tasks are finished
      if (size > 1) {
    	  TimerTask task = new TimerTask() {
			 @Override
			 public void run() {
				 if (!isOperating()) {
			         scheduleWorkerTask(new ControlDocumentMirrorsTask(), "control session document mirrors");
			         cancel();
				 }
			 }
    	  };
    	  Global.getTimer().schedule(task, 100, 50);
      }
   }
   
   /** Closes the given document from the GUI display and saves it to
    *  original location if user opts for. This method is GUI-interactive
    *  if 'saveOption' is 'true'. If 'saveOption' is 'false' and the document 
    *  is modified, it will be closed unsaved and without user interaction. 
    *  
    * @param document <code>PadDocument</code>, may be null
    * @param saveOption boolean whether a modified document can be saved
    *        (user interaction)
    * @param allOption boolean if true a button to save all is available
    * @return boolean true == went good, false == user break or interrupted
    */
   public boolean closeDocument (PadDocument document, boolean saveOption, boolean allOption) {
      if (document == null) return true;
      
      // end editing and save document preferences
	  DisplayManager.get().endEditDocument(document);
      document.savePreferences();
      
      // obtain user confirmation for saving and save a modified document
      if (saveOption && document.isModified()) {
    	  // special operation case: modified BACKUP file (no sense saving to temporary file)
    	  if (document.isBackupCopy()) {
    		  String text = displayText("msg.ask.save.backup");
    		  text = Util.substituteText(text, "$name", document.getShortTitle());
    		  if (GUIService.userConfirm(text)) {
   	          	 if (!saveDocumentCopy(document)) return false;
    		  }
    		  
    	  } else {
	    	  // display a confirm request message (user decision)
	    	  int choice = 0;
	    	  String text = displayText("msg.ask.save.doc").concat(document.getTitle());
			  MessageDialog msg = new MessageDialog(null, "dlg.title.closing.doc", text, 
					  MessageType.question, DialogButtonBar.YES_NO_CANCEL_BUTTON, true);
			  
			  if (allOption) {
				  Action a = new AbstractAction(Global.res.getCommand("action.file.save.all")) {
					  @Override
					  public void actionPerformed(ActionEvent e) {
						  closeWhenAllSaved(msg);
						  saveAllToFileSystem();
					  }
					  
					  /** Closes the given dialog when all currently registered 
					  * documents are unmodified. This method issues a periodic 
					  * TimerTask to delegate the described task.
					  * 
					  * @param dlg JDialog
					  */
					  private void closeWhenAllSaved (JDialog dlg) {
						  TimerTask task = new TimerTask() {
							 @Override
							 public void run() {
								boolean allSaved = true;
						        for (PadDocument doc : Global.getDocumentRegistry()) {
						           allSaved &= !doc.isModified();
						        }
						        if (allSaved) {
						        	dlg.dispose();
						        	cancel();
						        }
							 }
						  };
						  getTimer().schedule(task, 50, 50);
					  }
				  };
				  
				  JButton saveBut = new JButton(a);
				  msg.getButtonBar().add(saveBut);
			  }
			  
			  choice = msg.showDialog(); 
			  Log.debug(10, "(ActionHandler.closeDocument) user choice == " + choice);
	    	  
	         // save document
	   	  	 if (document.isModified()) {
		         switch (choice) {
		         case GUIService.YES_OK_BUTTON_PRESSED:
		            if (!saveDocumentToFileSystem(document, true)) return false;
		            break;
		         case GUIService.NO_BUTTON_PRESSED:
		            break;
		         default:
		            return false;
		         }
	   	  	 }
    	  }
      }

      // de-register document
      Global.getDocumentRegistry().remove(document);
      return true;
   }
   
   /** Removes the given document from the GUI display and deletes it in its
    *  persistent storage location.
    *  
    * @param document <code>PadDocument</code>, may be null
    * @return boolean true == went good, false == user break, interrupted or
    *         operation error
    */
   public boolean deleteDocument (PadDocument document) {
	   boolean removed = false;
	   String text;
	   if (document != null) {
		  // info if there is no file registered for doc
		  String path = document.getExternalPath();
		  String funcTitle = displayText("dlg.title.file.delete");
		  if (path == null) {
			  text = displayText("msg.ask.file.delete.1");
	          text = Util.substituteTextS(text, "$name", document.getTitle());
	          
	          if (GUIService.userConfirm(funcTitle, text)) {
       	       	  removeHistoryMirrorsOfDocument(document);
	        	  Global.getDocumentRegistry().remove(document);
	        	  removed = true;
	          }

		  } else {
			  // ask user whether to terminally delete this file 
			  String filepath = IO_Manager.get().getExternalFileReference(document.getUUID());
			  long time = new File(filepath).lastModified();
	          text = displayText("msg.ask.file.delete.2");
	          text = Util.substituteTextS(text, "$name", document.getTitle());
	          text = Util.substituteTextS(text, "$path", path);
	          text = Util.substituteTextS(text, "$time", Util.localeTimeString(time));
	          
	          if (GUIService.userConfirm(funcTitle, text)) {
	        	  try {
					removed = IO_Manager.get().deleteDocumentFile(document);
	        	    if (removed) {
	        	       document.removePreferences();
	        	       removeHistoryMirrorsOfDocument(document);
	        	       
		        	   // remove document from global registers
		        	   Global.getDocumentRegistry().remove(document);
		        	   Global.getRecentFilesStack().remove(path);

		        	   // confirm deletion to user
		        	   GUIService.infoMessage("dlg.title.filemanager", "confirm.file.deletion");
		        	}
				  } catch (InterruptedException e) {
					GUIService.failureMessage("msg.failure.file-deletion", e);
				  }
	          }
		  }
	   }
	   return removed;
   }

   /** Equivalent to {@code "ResourceLoader.get().getDisplay(token)"}.
    * 
    * @param token String text token
    * @return String text or "FIXME" if unknown
    */
   public static String displayText (String token) {
	   return ResourceLoader.get().getDisplay(token);
   }
   
   /** Lets the user select a file from the file system and attempts to
    * open it as a pad-document into the global <code>DocumentRegistry</code>.
    * Normally this brings the document to display.
    * 
    * @return <code>PadDocument</code> or null if function is broken
    * @throws IOException 
    */
   public PadDocument openDocumentFromFileSystem () throws IOException {
      PadDocument document = null;
      
      // obtain user selection of input file
      File file = GUIService.getUserSelectedFile(SystemFileType.DOCUMENT_FILE, 
            StreamDirection.INPUT, null);
      
      
      // if user selection is valid 
      if (file != null) {
         document = openDocumentFromPath(file.getPath(), null, true);
      }
      return document;
   }
   
   /** Opens and returns a pad document from the given path of a local file.
    * The open file is registered at the global document registry.
    * Error conditions and unresolved mirror files (optional) are indicated via
    * GUI dialogs. 
    * 
    * 
    * @param filepath String file path in local file system
    * @param key byte[] optional decryption passphrase, may be null
    * @param controlMirrors boolean if true this method controls mirror files
    *        after opening the document into display 
    * @return <code>PadDocument</code> or null if the file could not be
    *          found or was unable to process
    */
   public static PadDocument openDocumentFromPath (String filepath,	byte[] key, 
		   		 boolean controlMirrors) {
	  Objects.requireNonNull(filepath, "filepath is null");
      if (filepath.isEmpty()) return null;
      PadDocument document = null;
      try {
    	  File file = new File(filepath).getCanonicalFile();
    	  filepath = file.getAbsolutePath();
      
	      // if user selection is valid 
	      if (GUIService.checkFilepathPermitted(file)) {
	            // attempt open the document
	            document = IO_Manager.get().openDocument(file, key);
	            if (document != null) {;
	            	// register document 
	            	registerAndDisplayDocument(document, filepath, controlMirrors);
	            }
	      }
      } catch (IOException | InterruptedException e) {
          GUIService.infoMessage("dlg.title.fail.loading.file", "msg.failure.open.document", e);
          Global.getRecentFilesStack().remove(filepath);
      }
      return document;
   }
   
   /** Registers the given document at the document registry and thus brings it 
    * to display. Optionally the mirrors for the document are controlled.
    * If the document is already registered, a GUI message is shown and mirrors
    * are not controlled. The filepath is assigned to the document iff this
    * parameter is not null.
    * 
    * @param document {@code PadDocument}
    * @param filepath String path of external file to be associated w/ document;
    *        may be null
    * @param controlMirrors boolean whether mirror file shall be controlled
    */
   private static void registerAndDisplayDocument (PadDocument document, String filepath, boolean controlMirrors) {
	   Objects.requireNonNull(document);
	   DocumentRegistry registry = Global.getDocumentRegistry();
	   try {
		   if (registry.isRegisteredDocument(document) || registry.isRegisteredDocument(filepath)) {
	           String text = displayText("msg.failure.already.open") + document.getTitle();
	           GUIService.infoMessage("dlg.title.fail.loading.file", text);
		   } else {
			   if (filepath != null) {
				   IO_Manager.get().setExternalFileReference(document, filepath);
				   if (!document.isBackupCopy()) {
					   Global.getRecentFilesStack().push(filepath);
				   }
			   }
			   registry.add(document);
	           
	           // optionally control mirror files
	           if (controlMirrors) {
	           		get().controlDocumentMirrors(document);
	           }
           }
        } catch (IllegalArgumentException e) {
        	e.printStackTrace();
	    } 
   }
   
   private String mirrorAdapterIdentifier (PadDocument document) {
	   return document.getUUID().toHexString();
   }
   
   private void addDocumentToMirrorSystem (PadDocument document) {
      MirrorFileAdapter adapter = new MirrorFileAdapter(document);
      Global.getMirrorFileManager().addMirrorable(adapter);
   }
   
   private void removeDocumentFromMirrorSystem (PadDocument document) {
      if (document != null) {
          Global.getMirrorFileManager().removeMirrorable(mirrorAdapterIdentifier(document));
      }
   }
   
   /** Removes the current mirror of the given document in the mirror system.
    * 
    * @param document {@code PadDocument}
    */
   public void removeMirrorOfDocument (PadDocument document) {
	   String name = mirrorAdapterIdentifier(document);
       Global.getMirrorFileManager().removeCurrentMirror(name);
       Global.getMirrorFileManager().setMirrorableSaved(name);
   }

   /** Removes all history mirrors of the given document in the mirror system.
    * 
    * @param document {@code PadDocument}
    */
   public void removeHistoryMirrorsOfDocument (PadDocument document) {
	   Global.getMirrorFileManager().removeHistoryMirrors(mirrorAdapterIdentifier(document));
   }

   /** Checks whether history mirrors are available for the given document and
    * performs GUI active reactions which enable recovery of the latest saved 
    * state. If the user opts to replace a document with its mirror the display
    * is arranged accordingly.
    * 
    * @param document {@code PadDocument}
    */
   public void controlDocumentMirrors (PadDocument document) {
	   IO_Manager ioMan = IO_Manager.get();
       String title = ActionHandler.displayText("dlg.mirror.resolve");
	   
	   // checkout history mirror files for document from mirror-handler
	   String identifier = mirrorAdapterIdentifier(document);
       List<File> mirrors = Global.getMirrorFileManager().getHistoryMirrors(identifier);
       if (mirrors.isEmpty()) return;
       Log.debug(4, "(ActionHandler.controlDocumentMirrors) history mirrors detected: " 
                + mirrors.size() + " for document " + identifier);
	   
       // get the latest history mirror file
       File topMirror = mirrors.get(0);
       
       // call mirror resolve dialog and receive answers
       ResolveMirrorDialog dialog;
       int option = 0;
       File origFile = null;
       try {
           Global.getDocumentRegistry().setSelectedDocument(document.getUUID());
    	   dialog = new ResolveMirrorDialog(document, topMirror);
    	   dialog.show();
           option = dialog.getUserOption();
           origFile = dialog.getOriginalFile();
       } catch (IllegalArgumentException | IOException e1) {
    	   e1.printStackTrace();
    	   GUIService.failureMessage(title, "msg.failure.mirror.handle", e1);
    	   return;
       }
       
       // action to take upon answer
       switch (option) {
       case ResolveMirrorDialog.REPLACE_ACTION:
           try {
              // replace the document file with its latest history mirror file
          	  ioMan.copyFile(topMirror, origFile);

              // de-register document
              Global.getDocumentRegistry().remove(document);

              // re-open the document in registry and GUI
              document = openDocumentFromPath(origFile.getAbsolutePath(), document.getPassphrase(), false);

              // remove all history mirrors of the document
              removeHistoryMirrorsOfDocument(document);
              
              // treat mirror file as entry in document history
              ioMan.pushFileHistory(origFile);
           } catch (IOException | InterruptedException e) {
        	  GUIService.failureMessage(title, displayText("msg.failure.mirror.copy"), e);
           }
    	   break;
       case ResolveMirrorDialog.DELETE_ACTION:
           removeHistoryMirrorsOfDocument(document);
    	   break;
       case ResolveMirrorDialog.SHOW_ACTION:
    	   viewDocumentAsAlternate(topMirror, document);
       }
   } // controlDocumentMirrors
   
   /** Loads to display the document from the given serialisation file so that
    * its UUID appears as new and display is set to READ-ONLY. The document
    * file is not modified by this action, its filepath is not registered and
    * file mirrors are not checked. A temporary file can be assigned to the
    * document in which case a copy of the original is registered and can be 
    * worked on by the user.
    * 
    * @param file File document file to view
    * @param origDoc original document in case a copy is loaded; may be null
    */
   private void viewDocumentAsAlternate (File file, PadDocument origDoc) {
	  Log.log(10,  "(ActionHandler.viewDocumentAsAlternate) start, source = " + file.getAbsolutePath());
  	  try {
  		 // get information from the original document
  		 byte[] key = origDoc == null ? null : origDoc.getPassphrase();
  		 String encoding = origDoc == null ? null : origDoc.getEncoding();
  		 
  		 // load the given source file (document) and modify some properties
		 PadDocument mdoc = IO_Manager.get().openDocument(file, encoding, key);
		 mdoc.setUUID(new UUID());
		 mdoc.setReadOnly(true);
		 mdoc.setBackupFile(file.lastModified());
		 mdoc.resetModified();
		 
		 // store the source document in a temporary file
		 File dataF = File.createTempFile("npad-", ".tmp");
		 IO_Manager.get().saveDocument(mdoc, dataF, mdoc.getEncoding());
		 
		 registerAndDisplayDocument(mdoc, dataF.getAbsolutePath(), false);
	  } catch (Exception e) {
		 GUIService.failureMessage(null, e);
	  }
	  Log.log(10,  "(ActionHandler.viewDocumentAsAlternate) end");
   }

   /** Initiates file-save tasks for all modified documents into the worker
    * thread system (ActionHandler). Does not wait for execution to complete
    * but returns immediately.
    */
   public void saveAllToFileSystem () {
       for (PadDocument doc : Global.getDocumentRegistry()) {
           if (doc.isModified()) {
              scheduleWorkerTask(new SaveDocumentTask(doc), "save document: ".concat(doc.getShortTitle()));
           }
        }
   }
   
   /** Saves the given document to its original file location if it is
    * modified. If no file location is known for the document, a save 
    * destination is requested from the user via dialog. Does nothing if
    * the parameter is null or the document is not modified. 
    * 
    * @param document <code>PadDocument</code>, may be null
    * @param confirm boolean if true new file destination is confirmed via info
    * @return boolean true == file is saved, false == error or broken
    */
   public boolean saveDocumentToFileSystem (PadDocument document, boolean confirm) {
	  if (document == null) return false;
	  
	  // save preferences (may be separate from file storage)
	  DisplayManager.get().endEditDocument(document);
	  document.savePreferences();
	  
      if (document.isModified()) {
         try {
            // save the document (may include user selection dialog)
            Log.log(4, "(ActionHandler.saveDocumentToFileSystem) start saving document: "
                       .concat(document.getShortTitle()));
            String iomFilepath = document.getExternalPath();
            boolean isNewFile = iomFilepath == null;
            File file = IO_Manager.get().saveDocument(document, document.getEncoding());
            if (file == null) return false;
            
           // remove the mirror file of the document
           removeMirrorOfDocument(document);
           
           // write message to statusline
           String path = file.getAbsolutePath();
           String text = displayText("msg.confirm.save");
           String msg = Util.substituteTextS(text, "$name", document.getShortTitle());
           Global.getStatusBar().putMessage(msg.concat(path), UnixColor.ForestGreen);
           
           try {
	           // reset a MODIFIED property
	           document.resetModified();
	       } catch (Throwable e) {
        	   e.printStackTrace();
           }
   
           // if user input was involved (new file)
           if (isNewFile) {
		   	  // remove all history mirrors of the document
			  removeHistoryMirrorsOfDocument(document);
			  removeFromNewFileList(mirrorAdapterIdentifier(document));

              // notify filepath in recent files stack (menu)
              Global.getRecentFilesStack().push(path);

              // repeat saving preferences bc. filepath has been defined
        	  document.savePreferences();
        	  
              if (confirm) {
            	  // display confirm message
            	  msg += "<br><font color=\"green\">" + path; 
            	  GUIService.infoMessage("dlg.title.confirmation", msg);
              }
           }

           // if doc is encrypted, offer to remove a corresponding cleartext file
           if (document.isEncrypted()) {
        	   path = path.substring(0, path.length() - 4);
        	   File f0 = new File(path);
        	   if (f0.isFile()) {
                   // ask user for removal of cleartext file
                   msg = displayText("msg.ask.delete.cleartext") + path; 
        		   if (GUIService.userConfirm("dlg.title.disposition", msg)) {
        			   try {
        				   IO_Manager.get().deleteFileIfNotRegistered(f0);
        				   Global.getRecentFilesStack().remove(path);
        				   IO_Manager.get().deleteFileHistory(f0);
        			   } catch (IOException e) {
        				   GUIService.failureMessage("msg.failure.file-deletion", e);
        			   }
        		   }
        	   }
           }
            
         } catch (Exception e) {
            e.printStackTrace();
            String text = displayText("msg.failure.save");
            text = Util.substituteText(text, "$name", document.getShortTitle());
            GUIService.failureMessage(null, text, e);
         }
      }

      // return true to indicate that save went OK
      // or document was not modified
      return true;
   }

   /** Saves a copy of the given document to the file system by asking the 
    * user to select a destination. The copy owns a new UUID.
    * 
    * @param document {@code PadDocument}
    * @return boolean boolean true == file is saved, false == error or broken
    */
   private boolean saveDocumentCopy (PadDocument document) {
       PadDocument copy = document.getShallowCopy();
       copy.setBackupFile(0);
       return saveDocumentToFileSystem(copy, true);
   }
   
   /** Removes the given identifier of a new document from the new-document-list
    * in system options.
    * 
    * @param ident String mirror adapter identifier
    */
   private void removeFromNewFileList (String ident) {
       // remove ID-entry in NEW-DOC-LIST (system options)
       List<String> idlist = Global.getOptions().getStringList("list-new-docs");
       boolean rem;
       do { rem = idlist.remove(ident); } while (rem);
       if ( Global.getOptions().setStringList("list-new-docs", idlist) ) {
       	   Log.debug(6, "(ActionHandler.removeFromNewFileList) removed document UUID from NEW-DOC-LIST (system options): " 
  	                 + ident);
       }
   }

/** Removes the given pad-article from its document and puts it into the
    * article-stack of the display-manager. Does nothing if parameter is null
    * or the article does not feature a pad-document.
    * 
    * @param article <code>PadArticle</code>, may be null
    */
   public void cutoutArticleToStack (PadArticle article) {
	  int index;
      if (article != null && (index = article.getIndexPosition()) > -1) {
    	 PadDocument document = article.getDocument();
         if (clipboardSecurityConfirmed(document)) {
            articleStack = document.copyArticlesAt(index);
            PadArticle[] removedArticle = document.cutoutArticleAt(index);
            
            document.getUndoManager().addEdit(new DocumentUndoableEdit(
            	DocumentEditType.CUT, document, removedArticle, index));
         }
      }
   }
   
   /** Creates a copy of the given pad-article and all of its descendants and 
    * puts it into the article-stack of the display-manager. 
    * Does nothing if parameter is null.
    * 
    * @param article <code>PadArticle</code>, may be null
    */
   public void copyArticleToStack (PadArticle article) {
	  PadDocument document; 
      if (article != null && (document = article.getDocument()) != null &&
    	  clipboardSecurityConfirmed(document)) {
         int index = article.getIndexPosition();
         PadArticle[] copy = article.getDocument().copyArticlesAt(index);
         if (copy.length > 0) {
            articleStack = copy; 
         }
      }
   }
   
   /** Inserts all articles from the manager's article-stack into the 
    * currently selected document at the currently selected position in
    * order-view. The current position will be the new articles' ancestor
    * and placed directly below it.
    */
   public void insertArticlesFromStack () {
      // verify target
      PadDocument document = DisplayManager.get().getSelectedDocument();
      if (document != null && articleStack != null) {
         PadArticle parent = document.getSelectedArticle();
         if (parent == null && !document.isEmpty()) return;
         
         int index = parent == null ? 0 : parent.getIndexPosition() + 1;
	     document.insertArticleAt(parent, index, articleStack);
	     document.getUndoManager().addEdit(new DocumentUndoableEdit(
	           	DocumentEditType.PASTE, document, articleStack, index));
	
	     articleStack = null;
	     document.setSelectedIndex(index);
      }
   }
   
   /** Deletes parameter article from its document.
    * 
    * @param article <code>PadArticle</code>, may be null
    */
   public void dialogDeleteArticle (PadArticle article) {
	  int index;
      if (article != null && (index = article.getIndexPosition()) > -1) {
         PadDocument document = article.getDocument();
         int count = document.countArticlesAt(index);
         if (count <= 0) return;

         // ask user to delete article and delete
         String text = displayText("msg.ask.art.delete").concat(article.getTitle());
         text = Util.substituteText(text, "$count", String.valueOf(count));

         // execute if user confirmed
         if (GUIService.userConfirm(text)) {
            PadArticle[] arts = document.cutoutArticleAt(index);

            document.getUndoManager().addEdit(new DocumentUndoableEdit(
                	DocumentEditType.DELETE, document, arts, index));
         }
      }
   }
   
// -------------------------------------------------------
   
   private void sortArticles (PadArticle article) {
	  if (article == null) return;
	  PadDocument document = article.getDocument();
	  int index = document.indexOf(article);
	  int depth = document.getBranchDepth(index);
      PadArticle[] arts = document.getArticlesAt(index);
	  if (arts.length > 2) {
		  String msg = displayText("msg.confirm.sort.articles");
		  msg = Util.substituteText(msg, "$amount", String.valueOf(arts.length-1));
	      if (!GUIService.userConfirm("dlg.sort.articles", msg)) return;
	  	  boolean recurse = false;
		  if (depth > 1) {
		  	 recurse = GUIService.userConfirm("dlg.sort.articles", "msg.ask.extend.sorting");
		  }
		  sortArticle(article, recurse);
		  UndoableEdit edit = new DocumentUndoableEdit(DocumentEditType.SORT, document, arts, index);
		  document.getUndoManager().addEdit(edit);
		  document.setSelectedArticle(article);
	  }
   }
   
   /** Sorts the children of the given article while optionally recursing into
    * all descendants. The article must be element of a document.
    * 
    * @param article {@code PadArticle} parent article
    * @param recurse boolean true = recurse into all descendants, 
    *        false = only children
    */
 	private void sortArticle (PadArticle article, boolean recurse) {
		PadDocument document = article.getDocument();
		int index = document.indexOf(article);
		PadArticle[] arts = document.getChildrenOf(index);

		if (arts.length > 1) {
 			// cut out all children article branches and store in array
 			PadArticle[][] childs = new PadArticle[arts.length][];
 			int i = 0;
 			for (PadArticle a : arts) {
 				childs[i++] = document.cutoutArticleAt(document.indexOf(a));
 			}
 			
 			// sort the children articles
 			Comparator<PadArticle[]> comp = new Comparator<PadArticle[]>() {
				@Override
				public int compare (PadArticle[] o1, PadArticle[] o2) {
					if (o1.length == 0 || o2.length == 0)
						throw new IllegalArgumentException("empty article array");
					return o1[0].compareTo(o2[0]);
				}
			};
 			Arrays.sort(childs, comp);

 			// re-integrate the sorted complex
 			for (PadArticle[] arr : childs) {
 	  			document.insertArticleAt(article, index+1, arr);
 	  			index += arr.length;
 			}
 		}
 		
 		if (recurse) {
 			// recursion into children of article (if opted)
 			for (PadArticle a : arts) {
 				if (document.hasChildren(a)) {
 					sortArticle(a, recurse);
 				}
 			}
 		}
 	}
 	
 	private void sortTextLines (ArticleEditor editor) {
 		if (editor == null) return;
 		JTextComponent viewEditor = editor.getView();
 		String text = editor.getSelectedText();
 		
 		// if we have a selection and it encompasses multiple lines
 		if (text != null && text.indexOf('\n') > -1) {
 			try {
	 			int start = viewEditor.getSelectionStart();
	 			int end = viewEditor.getSelectionEnd();
//	 			Log.debug(10, "--- SELECTION: " + start + ", " + end);
	 			
	 			// extend start position if feasible
	 			Document doc = editor.getDocument();
	 			while (start > 0 && !doc.getText(start-1, 1).equals("\n")) start--;
	 			
	 			// extend end position if feasible
	 			int limit = doc.getLength();
	 			while (end < limit && !doc.getText(end, 1).equals("\n")) end++;
	
	 			// set new selection and get lines from the document
	 			viewEditor.setSelectionStart(start);
	 			viewEditor.setSelectionEnd(end);
	 			text = viewEditor.getSelectedText();
	 			String[] arr = text.split("\n");
	 			
	 			// let user confirm operation
	 			String msg = displayText("msg.confirm.sort.text");
	 			msg = Util.substituteText(msg, "$amount", String.valueOf(arr.length));
	 			if (GUIService.userConfirm("dlg.sort.text", msg)) {
		 			// sort lines and create new text
		 			Arrays.sort(arr);
		 			StringBuffer sbuf = new StringBuffer(text.length());
		 			for (String s : arr) {
		 				sbuf.append(s);
		 				sbuf.append("\n");
		 			}
		 			sbuf.deleteCharAt(sbuf.length()-1);
		 			text = sbuf.toString();
		
		 			// insert text into document
		 			doc.remove(start, text.length());
		 			doc.insertString(start, text, null);
	 			}
	 			
 			} catch (BadLocationException e) {
 				GUIService.failureMessage("msg.failure.general", e);
 			}
 		}
 	}
 	
   /** Returns whether it is secure to overwrite the clipboard content.
 * This decision may, but does not have to, involve user interaction.
 *  
 * @return boolean true = save to delete, false = don't delete clipboard
 */
private boolean clipboardSecurityConfirmed (PadDocument document) {
	int length;
	PadDocument doc;
	if (articleStack != null && (length = articleStack.length) > 0 && 
		!document.equals((doc = articleStack[0].getDocument()))) {
		String hs = displayText("msg.ask.art.clipboard");
		hs = Util.substituteText(hs, "$art", String.valueOf(length));
		hs = Util.substituteText(hs, "$doc", doc == null ? "null" : doc.getTitle());
		return GUIService.confirmMessage("dlg.title.conflict.clipboard", hs, GUIService.YES_NO_OPTION);
	}
	return true;
}

   /** Displays a HTML browser dialog with the content of a list of Java runtime 
    *  environment properties.
    */  
   public void showSystemInfo () {
      HtmlBrowserDialog systemInfoDlg;
      StringBuffer sbuf;
      Map.Entry<Object,Object> entry;
      TreeSet<Entry<Object,Object>> sort;
      Iterator<Entry<Object,Object>> it;
      String hstr, title;
      long time;
      boolean special;

      // allow only one system info panel
      title = displayText( "dlg.systeminfo" );
      systemInfoDlg = new HtmlBrowserDialog( Global.getActiveFrame(), title, false );
      if (!systemInfoDlg.markSingleton( "SystemInfoDialog" )) return;
      systemInfoDlg.moveRelatedTo( Global.getActiveFrame() );
      systemInfoDlg.setAutonomous( true );
      if ( Toolkit.getDefaultToolkit().getScreenSize().width > 800 ) {
         systemInfoDlg.setSize( new Dimension( 600, 400 ) );
      }
      PersistentOptions options = Global.getOptions();

      // sort system properties
      sort = new TreeSet<Entry<Object,Object>>( new Comparator<Entry<Object,Object>>() {
        @Override
		public int compare ( Map.Entry<Object,Object> k1, Map.Entry<Object,Object> k2 ) {
            if ( k1 == null | k2 == null )
               throw new IllegalArgumentException();
            return k1.getKey().toString().compareTo( (String)k2.getKey() ); 
        }
      });
      
      for (it = System.getProperties().entrySet().iterator(); it.hasNext();) {
         sort.add(it.next());
      }
      
      // compile text about application globals
      sbuf = new StringBuffer(2048);
      sbuf.append( "<html><body><h3><u>Application Properties</u></h3>" );
      sbuf.append( "<font color=\"blue\"><b>Operating System</b></font> = " );
      sbuf.append( Global.isWindows() ? "WINDOWS" : "UNIX/LINUX/MAC" );
      
      sbuf.append( "<br><font color=\"blue\"><b>Locale</b></font> = " );
      sbuf.append( Global.getLocaleString() );
      sbuf.append( "<br><font color=\"blue\"><b>LAF</b></font> = " );
      sbuf.append( UIManager.getLookAndFeel().getClass().getName() );

//      // option file if available
//      sbuf.append( "<br><font color=\"blue\"><b>Option File</b></font> = " );
//      f = options.getPersistentFile();
//      sbuf.append( f == null ? "- undefined -" : f.getFilepath() );

      if ( !(hstr = Global.cmlHandler.getCommandline()).isEmpty()) {
    	  sbuf.append( "<br><font color=\"blue\"><b>Command Line</b></font> = " );
    	  sbuf.append(hstr);
      }

//      if ( Global.isPortable() ) {
//         sbuf.append( "<br><font color=\"blue\"><b>Portable Root</b></font> = " );
//         sbuf.append( Global.portableDir.getAbsolutePath() );
//      }
      
      sbuf.append( "<br><font color=\"blue\"><b>Application Home</b></font> = " );
      sbuf.append( Global.getApplicationDirectory().getAbsolutePath() );
      sbuf.append( "<br><font color=\"blue\"><b>Program Dir</b></font> = " );
      sbuf.append( Global.getProgramDirectory().getAbsolutePath() );
      sbuf.append( "<br><font color=\"blue\"><b>History Dir</b></font> = " );
      sbuf.append( Global.getHistoryDirectory().getAbsolutePath() );
      sbuf.append( "<br><font color=\"blue\"><b>Current Dir</b></font> = " );
      sbuf.append( Global.getCurrentDirectory().getAbsolutePath() );
      sbuf.append( "<br><font color=\"blue\"><b>Exchange Dir</b></font> = " );
      sbuf.append( Global.getExchangeDirectory().getAbsolutePath() );
      if (Global.isRestrictedFilepath()) {
          sbuf.append( "<br><font color=\"blue\"><b>Restricted Filepath</b></font> = " );
          sbuf.append( Global.getRestrictedFilepath() );
      }
      if ( (time = (long)options.getIntOption("lastAccessTime") * 1000) > 0 ) {
         sbuf.append( "<br><font color=\"blue\"><b>Previous Session Time: </b></font> = " );
         sbuf.append(Util.localeTimeString(time));
      }
      
      // compile text from system properties
      sbuf.append( "<h3><u>Runtime System Properties</u></h3>" );
      for ( it = sort.iterator(); it.hasNext(); ) {
         special = false;
         entry = it.next();
         sbuf.append( "<font color=\"maroon\"><b>" );
         hstr = (String)entry.getKey();
         sbuf.append( Util.htmlEncoded(hstr) ); 
         sbuf.append( "</b></font> = " );
         
         if ( hstr.startsWith( "user." ) ) {
            sbuf.append( "<font color=\"green\"><b>" );
            special = true;
         }
         
         if ( hstr.startsWith( "java.vm." ) || hstr.startsWith( "os." ) ) {
            sbuf.append( "<font color=\"#005FBF\"><b>" );
            special = true;

         } else if ( hstr.startsWith( "java." ) ) {
            sbuf.append( "<font color=\"black\"><b>" );
            special = true;
         }
         
         sbuf.append( Util.htmlEncoded((String)entry.getValue()) );
         if (special) {
            sbuf.append("</b></font>");
         }
         sbuf.append("<br>");
      }
      sbuf.append("#</p></body></html>");

      // show the results

      systemInfoDlg.setText( sbuf.toString() );
      systemInfoDlg.show();
   }  // showSystemInfo

   public static class ActionNames {
	  public static final String FILE_NEW = "file.new";
      public static final String FILE_OPEN = "file.open";
      public static final String FILE_OPEN_SESSION = "file.open.session";
      public static final String FILE_OPEN_URL = "file.open.url";
      public static final String FILE_OPEN_RECENT = "file.open.recent";
      public static final String FILE_OLD_VERSIONS = "file.old.versions";
      public static final String FILE_RELOAD = "file.reload";
      public static final String FILE_SAVE = "file.save";
      public static final String FILE_SAVE_ALL = "file.save.all";
      public static final String FILE_SAVE_AS = "file.save.as";
      public static final String FILE_SAVE_COPY = "file.save.copy";
      public static final String FILE_CLOSE = "file.close";
      public static final String FILE_DELETE = "file.delete";
      public static final String FILE_CLOSE_ALL = "file.close.all";
      public static final String SYSTEM_EXIT = "system.exit";

      public static final String EDIT_COPY = "edit.copy";
      public static final String EDIT_CUT = "edit.cut";
      public static final String EDIT_PASTE = "edit.paste";
      public static final String EDIT_DELETE = "edit.delete";
      public static final String EDIT_UNDO = "edit.undo";
      public static final String EDIT_REDO = "edit.redo";
      public static final String EDIT_INSERT_DATE = "edit.insert.date";
      public static final String EDIT_INSERT_TIME = "edit.insert.time";
      public static final String EDIT_INSERT_TABLE = "edit.insert.table";
      public static final String EDIT_INSERT_IMAGE = "edit.insert.image";
      public static final String EDIT_INSERT_FILE = "edit.insert.file";
      public static final String EDIT_EXPORT_FILE = "edit.export.file";
      public static final String EDIT_CHOOSE_FONT = "edit.choose.font";
      public static final String EDIT_COLOR_BGD = "edit.color.background";
      public static final String EDIT_COLOR_FGR = "edit.color.foreground";
      
      public static final String ACTION_SEARCH = "action.search";
      public static final String ACTION_SEARCH_WEB = "action.search.web";
      public static final String ACTION_TRANSLATE_WEB = "action.translate.web";
      public static final String ACTION_PRINT = "action.print";
      public static final String ACTION_SORT = "action.sort";
      public static final String ACTION_LINEWRAP_HARD = "action.linewrap.hard";
      public static final String ACTION_DOCUMENT_MODIFY = "action.document.modify";
      

      public static final String ORDER_EDIT_COPY = "order.edit.copy";
      public static final String ORDER_EDIT_CUT = "order.edit.cut";
      public static final String ORDER_EDIT_PASTE = "order.edit.paste";
      public static final String ORDER_EDIT_DELETE = "order.edit.delete";
      public static final String ORDER_CREATE_CHILD = "order.create.child";
      public static final String ORDER_CREATE_SIBLING = "order.create.sibling";
      public static final String TREE_MOVE_UP = "order.move.up";
      public static final String ORDER_MOVE_DOWN = "order.move.down";
      public static final String ORDER_MOVE_INDENT = "order.move.indent";
      public static final String ORDER_MOVE_OUTDENT = "order.move.outdent";
      public static final String ORDER_DOC_TOOLBOX = "order.doc.toolbox";

      public static final String VIEW_MAIN_TOOLBAR = "view.visible.maintoolbar";
      public static final String VIEW_TREE_TOOLBAR = "view.visible.treetoolbar";
      public static final String VIEW_ARTICLE_TOOLBAR = "view.visible.arttoolbar";
//      public static final String VIEW_TOOLBARS_FLOATABLE = "view.toolbars.floatable";
      public static final String VIEW_DISPLAY_SWITCH = "view.display.switch";
      public static final String VIEW_DISPLAY_ALL = "view.display.all";
      public static final String VIEW_DISPLAY_ARTICLE = "view.display.article";
      public static final String VIEW_DISPLAY_TREE = "view.display.tree";
      public static final String VIEW_FONT_ENLARGE = "view.font.enlarge";
      public static final String VIEW_FONT_DECREASE = "view.font.decrease";
      
      public static final String MANAGE_BACKUP = "admin.file.backup";
      public static final String MANAGE_RESTORE = "admin.file.restore";
      public static final String MANAGE_ENCRYPTION = "admin.file.encryption";
      public static final String MANAGE_PREFERENCES = "admin.edit.preferences";
      public static final String MANAGE_DELETE_HISTORY = "admin.history.delete";
//      public static final String MANAGE_IMPORT_RAGNA = "admin.import.ragna";
//      public static final String MANAGE_IMPORT_TEXT = "admin.import.text";
//      public static final String MANAGE_IMPORT_TREE_FILE = "admin.import.tree.file";
//      public static final String MANAGE_IMPORT_TREE_CLIPBOARD = "admin.import.tree.clipboard";
//      public static final String MANAGE_EXPORT_RAGNA = "admin.export.ragna";
//      public static final String MANAGE_EXPORT_TEXT = "admin.export.text";
//      public static final String MANAGE_EXPORT_TREE_FILE = "admin.export.tree.file";

      public static final String HELP_LICENSE = "help.info.license";
      public static final String HELP_INFO_SYSTEM = "help.info.system";
      public static final String HELP_USER_MANUAL = "help.user.manual";
      public static final String HELP_ABOUT = "help.about";
   }
   
   private class ActionExecutor implements ActionListener {
      DisplayManager displayManager = DisplayManager.get();
      
      @Override
      public void actionPerformed (ActionEvent evt)  {
         // standard values from GUI elements
         String cmd = evt.getActionCommand();
         DocumentRegistry docRegistry = Global.getDocumentRegistry();
         PersistentOptions options = Global.getOptions();
         DocumentDisplay display = displayManager.getSelectedDisplay();
         ArticleEditor editor = display == null ? null : display.getArticleEditor();
         PadDocument document = display == null ? null : display.getDocument();
         final PadArticle article = display == null ? null : display.getSelectedArticle();
         final DocumentOrderView orderView = display == null ? null : display.getOrderView();
         boolean swt;

         try {
        	 
         if ( cmd.equals( ActionNames.SYSTEM_EXIT ) ) {
            Global.exit();
         }
         
         else if ( cmd.equals( ActionNames.FILE_NEW ) ) {
            document = new DefaultPadDocument();
            docRegistry.add(document);
            Util.sleep(50);
            document.setModified();
            
            // request a document name from user
            String name = GUIService.userInput(null, "dlg.title.doc.create", "dlg.enter.doc.title", null);
            if (name != null) {
            	PadArticle art = document.newArticle(null, true);
            	art.setTitle(name);
            }
         }

         else if ( cmd.equals( ActionNames.FILE_OPEN ) ) {
            openDocumentFromFileSystem();
         }

         else if ( cmd.equals( ActionNames.FILE_OPEN_SESSION ) ) {
            openSession();
         }

         else if ( cmd.equals( ActionNames.FILE_SAVE ) ) {
            saveDocumentToFileSystem(document, true);
         }

         else if ( cmd.equals( ActionNames.FILE_CLOSE ) ) {
            Global.getMirrorFileManager().pause();
            closeDocument(document, true, false);
            Global.getMirrorFileManager().resume();
         }

         else if ( cmd.equals( ActionNames.FILE_CLOSE_ALL ) ) {
            collectiveDocumentClose();
         }

         else if ( cmd.equals( ActionNames.FILE_DELETE ) ) {
             deleteDocument(document);
          }

         else if ( cmd.equals( ActionNames.FILE_SAVE_ALL ) ) {
        	saveAllToFileSystem();
         }

         else if ( cmd.equals( ActionNames.FILE_SAVE_COPY ) ) {
            if (document != null) {
            	saveDocumentCopy(document);
            }
         }
         
         else if ( cmd.equals( ActionNames.FILE_SAVE_AS ) ) {
            if (document != null) {
               PadDocument copy = document.getShallowCopy();
               copy.setReadOnly(false);
               copy.setBackupFile(0);
               if (saveDocumentToFileSystem(copy, true)) {
                  String filepath = copy.getExternalPath();

                  Global.getMirrorFileManager().pause();
                  document.resetModified();
                  closeDocument(document, true, false);
                  openDocumentFromPath(filepath, document.getPassphrase(), false);
                  Global.getMirrorFileManager().resume();
               }
            }
         }

         else if ( cmd.equals( ActionNames.FILE_OPEN_RECENT ) ) {
            Log.log(8, "(ActionHandler.ActionExecutor) FILE-OPEN-RECENT with index " +
                  evt.getID());
            SetStackMenu menu = (SetStackMenu)evt.getSource();
            String filepath = (String)menu.getContent()[evt.getID()];
            openDocumentFromPath(filepath, null, true);
         }

         else if ( cmd.equals( ActionNames.FILE_RELOAD ) ) {
             if (document != null) {
            	 reloadDocument(document);
             }
         }

         else if ( cmd.equals( ActionNames.EDIT_UNDO ) ) {
        	 if (display.getLastFocusedView() == FocusableView.Article) {
        		 if (editor != null) {
        			 editor.undo();
        		 }
        	 } else {
        		 UndoManager mg = document.getUndoManager();
        		 if (!document.isReadOnly() && mg.canUndo()) {
        			 mg.undo();
        		 }
        	 }
         }

         else if ( cmd.equals( ActionNames.EDIT_REDO ) ) {
        	 if (display.getLastFocusedView() == FocusableView.Article) {
        		 if (editor != null) {
        			 editor.redo();
        		 }
        	 } else {
        		 UndoManager mg = document.getUndoManager();
        		 if (!document.isReadOnly() && mg.canRedo()) {
        			 mg.redo();
        		 }
        	 } 
         }

         else if ( cmd.equals( ActionNames.EDIT_CUT) ) {
//        	 Log.log(8, "(ActionHandler) EDIT CUT");
        	 if (editor != null && !editor.isReadOnly()) {
        		 editor.getActionByCommand(ArticleEditor.ACTION_CUT_NAME).actionPerformed(null);
        	 }
         }
         
         else if ( cmd.equals( ActionNames.EDIT_COPY) ) {
//        	 Log.log(8, "(ActionHandler) EDIT COPY");
        	 if (editor != null) {
        		 editor.getActionByCommand(ArticleEditor.ACTION_COPY_NAME).actionPerformed(null);
        	 }
         }
         
         else if ( cmd.equals( ActionNames.EDIT_PASTE) ) {
        	 if (editor != null && !editor.isReadOnly()) {
        		 editor.getActionByCommand(ArticleEditor.ACTION_PASTE_NAME).actionPerformed(null);
        	 }
         }
         
         else if ( cmd.equals( ActionNames.EDIT_DELETE) ) {
        	 if (editor != null && !editor.isReadOnly()) {
        		 editor.getActionByCommand(ArticleEditor.ACTION_DELETE_NAME).actionPerformed(null);
        	 }
         }
         
         else if ( cmd.equals( ActionNames.EDIT_INSERT_FILE) ) {
        	if (editor != null && !editor.isReadOnly()) {
	           JFileChooser fc = new JFileChooser(Global.getExchangeDirectory());
	           int opted = fc.showOpenDialog(editor.getView());
		       Global.setExchangeDirectory(fc.getCurrentDirectory());
		       
	           if (opted == JFileChooser.APPROVE_OPTION) {
	        	 File file = fc.getSelectedFile();
	        	 try {
	        		// choose a character set and read file
	        		String cs = GUIService.getUserSelectedCharset(null, null, null);
	        		if (cs == null) return;
					String text = Util.readTextFile(file, cs);
					Document doc = article.getEditorDocument();
						
					// investigate current text selection (optionally remove)
	               	Dimension sel = article.getSelection();
	             	int slen = sel == null ? 0 : sel.height - sel.width;
	             	if (slen > 0 && GUIService.userConfirm("msg.ask.replace.text")) {
	             		doc.remove(sel.width, slen);
	             	}
	             	// write imported text
	             	int cpo = editor.getCaretPosition();
	             	assert cpo >= 0;
				    editor.insertText(text, cpo);
					editor.setCaretPosition(cpo);
	             	display.restoreFocus();
	             	Log.log(5, "(ActionExecutor.actionPerformed) inserted external text (" + text.length() + " char)");
	               	 	
				} catch (IOException e) {
					e.printStackTrace();
					String hs = displayText("msg.error.readfile") + file.getAbsolutePath();
					GUIService.failureMessage(hs, e);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
	         }
            }
         }
         
         else if ( cmd.equals( ActionNames.EDIT_EXPORT_FILE) ) {
        	   // get selected text from article
        	   Dimension sel = article.getSelection();
        	   int length = sel == null ? 0 : sel.height - sel.width;
        	   if (length > 0) {
	        	  File file = null;
        		  try {
	        		 String text = article.getEditorDocument().getText(sel.width, length);
		        	 JFileChooser fc = new JFileChooser(Global.getExchangeDirectory());
		        	 int opted = fc.showSaveDialog(editor.getView());
		        	 Global.setExchangeDirectory(fc.getCurrentDirectory());
		        	 if (opted == JFileChooser.APPROVE_OPTION) {
		        		 file = fc.getSelectedFile();
		        		 String cs = GUIService.getUserSelectedCharset(null, null, null);
		        		 if (cs == null) return;
						 Util.writeTextFile(file, text, cs);
		        	 }
		        	 
				  } catch (IOException e) {
					 e.printStackTrace();
					 String hs = displayText("msg.error.writefile") + file.getAbsolutePath();
					 GUIService.failureMessage(hs, e);
				  } catch (BadLocationException e) {
					 e.printStackTrace();
				  }
        	   }
         }
         
         else if ( cmd.equals( ActionNames.EDIT_CHOOSE_FONT) ) {
        	 if (article != null) {
        		 String title = displayText("dlg.title.choose.font");
        		 Font font = FontChooser.showDialog(Global.mainframe, title, article.getDefaultFont());
        		 if (font != null) {
        			 editor.setFont(font);
        		 }
        	 }
         }
         
         else if ( cmd.equals( ActionNames.EDIT_COLOR_BGD) ) {
			if (article != null) {
				Color color = ColorChooserDialog.showDialog(editor.getView(), null, 
						Global.getBackgroundColorSet(), article.getBackgroundColor());
				if (color != null) {
					editor.setBackground(color);
				}
			}					
         }
         
         else if ( cmd.equals( ActionNames.EDIT_COLOR_FGR) ) {
			if (article != null) {
				Color color = ColorChooserDialog.showDialog(editor.getView(), null, 
						Global.getBackgroundColorSet(), article.getForegroundColor());
				if (color != null) {
					editor.setForeground(color);
				}
			}					
         }
         
         else if ( cmd.equals( ActionNames.VIEW_MAIN_TOOLBAR ) ) {
            swt = options.isOptionSet("hasMainToolbar");
            options.setOption("hasMainToolbar", !swt);
         }

         else if ( cmd.equals( ActionNames.VIEW_TREE_TOOLBAR ) ) {
            swt = options.isOptionSet("hasTreeToolbar");
            options.setOption("hasTreeToolbar", !swt);
         }

         else if ( cmd.equals( ActionNames.VIEW_ARTICLE_TOOLBAR ) ) {
            swt = options.isOptionSet("hasArticleToolbar");
            options.setOption("hasArticleToolbar", !swt);
         }

         else if ( cmd.equals( ActionNames.VIEW_DISPLAY_SWITCH ) ) {
            displayManager.switch_document_display();
         }

         else if ( cmd.equals( ActionNames.VIEW_DISPLAY_ALL ) ) {
            displayManager.setSelectedDisplayModus(DisplayModus.ComleteDisplay);
         }

         else if ( cmd.equals( ActionNames.VIEW_DISPLAY_ARTICLE ) ) {
            displayManager.setSelectedDisplayModus(DisplayModus.ArticleDisplay);
         }

         else if ( cmd.equals( ActionNames.VIEW_DISPLAY_TREE ) ) {
            displayManager.setSelectedDisplayModus(DisplayModus.OrderDisplay);
         }

         else if ( cmd.equals( ActionNames.VIEW_FONT_ENLARGE ) ) {
            if (article != null) {
                Log.log(10, "(ActionHandler.Executor) -- enlarge font");
               if (display.getLastFocusedView() == FocusableView.Order) {
	               Font font = orderView.getView().getFont();
	               if (font != null) {
	                  Float size = font.getSize2D() + 1;
	                  orderView.getView().setFont(font.deriveFont(size));
	               }
               } else {
	               Font font = article.getDefaultFont();
	               if (font != null) {
	                  Float size = font.getSize2D() + 1;
	                  editor.setFont(font.deriveFont(size));
	               }
               }
            }
         }

         else if ( cmd.equals( ActionNames.VIEW_FONT_DECREASE ) ) {
            if (article != null) {
               Log.log(10, "(ActionHandler.Executor) -- decrease font");
               if (display.getLastFocusedView() == FocusableView.Order) {
	               Font font = orderView.getView().getFont();
	               if (font != null) {
	                  Float size = font.getSize2D() - 1;
	                  orderView.getView().setFont(font.deriveFont(size));
	               }
               } else {
	               Font font = article.getDefaultFont();
	               if (font != null) {
	                  Float size = font.getSize2D() - 1;
	                  editor.setFont(font.deriveFont(size));
	               }
               }
            }
         }

         else if ( cmd.equals( ActionNames.ORDER_CREATE_CHILD ) ) {
        	PadArticle art = orderView.createChild(article);
        	if (art != null) {
        		int index = art.getIndexPosition();
	        	document.getUndoManager().addEdit(new DocumentUndoableEdit(
	        			DocumentEditType.CREATE, document, new PadArticle[] {art}, index));
        	}
         }

         else if ( cmd.equals( ActionNames.ORDER_CREATE_SIBLING ) ) {
            if (article != null && article.getParent() != null) {
               PadArticle art = orderView.createSibling(article);
               if (art != null) {
            	   int index = art.getIndexPosition();
            	   document.getUndoManager().addEdit(new DocumentUndoableEdit(
            			   DocumentEditType.CREATE, document, new PadArticle[] {art}, index));
               }
            }
         }

         else if ( cmd.equals( ActionNames.TREE_MOVE_UP ) ) {
        	PadArticle art = orderView.getSelectedElement();
            if (art != null && orderView.moveUp()) {
               int index = art.getIndexPosition();
   	           document.getUndoManager().addEdit(new DocumentUndoableEdit(
  		             	DocumentEditType.MOVE_UP, document, new PadArticle[] {art}, index));
            }
         }

         else if ( cmd.equals( ActionNames.ORDER_MOVE_DOWN ) ) {
         	PadArticle art = orderView.getSelectedElement();
            if (art != null && orderView.moveDown()) {
               int index = art.getIndexPosition();
   	           document.getUndoManager().addEdit(new DocumentUndoableEdit(
  		             	DocumentEditType.MOVE_DOWN, document, new PadArticle[] {art}, index));
            }
         }

         else if ( cmd.equals( ActionNames.ORDER_MOVE_INDENT ) ) {
          	PadArticle art = orderView.getSelectedElement();
            if (art != null && orderView.indent()) {
               int index = art.getIndexPosition();
   	           document.getUndoManager().addEdit(new DocumentUndoableEdit(
  		             	DocumentEditType.INDENT, document, new PadArticle[] {art}, index));
            }
         }

         else if ( cmd.equals( ActionNames.ORDER_MOVE_OUTDENT ) ) {
          	PadArticle art = orderView.getSelectedElement();
            if (art != null && orderView.outdent()) {
               int index = art.getIndexPosition();
   	           document.getUndoManager().addEdit(new DocumentUndoableEdit(
  		             	DocumentEditType.OUTDENT, document, new PadArticle[] {art}, index));
            }
         }

         else if ( cmd.equals( ActionNames.ORDER_EDIT_COPY ) ) {
       		copyArticleToStack(article);
         }

         else if ( cmd.equals( ActionNames.ORDER_EDIT_CUT ) ) {
         	if (!orderView.isReadOnly()) {
         		cutoutArticleToStack(article);
         	}
         }

         else if ( cmd.equals( ActionNames.ORDER_EDIT_PASTE ) ) {
          	if (!orderView.isReadOnly()) {
          		insertArticlesFromStack();
          	}
         }

         else if ( cmd.equals( ActionNames.ORDER_EDIT_DELETE ) ) {
          	if (!orderView.isReadOnly()) {
          		dialogDeleteArticle(article);
          	}
         }

         else if ( cmd.equals( ActionNames.ORDER_DOC_TOOLBOX ) ) {
       	    new DialogDocumentToolbox(document).show();
         }

         else if ( cmd.equals( ActionNames.ACTION_PRINT ) ) {
            if (document != null && document.getArticleCount() > 0) {
           	   PrintSelectorDialog dlg = new PrintSelectorDialog();
           	   dlg.show();

           	   if (dlg.isOkPressed()) {
           		   display.printArticles(dlg.getPrintParameters());
           	   }
            }
         }

         else if ( cmd.equals( ActionNames.ACTION_SORT ) ) {
        	if (display != null) {
        		if (display.getLastFocusedView() == FocusableView.Article) {
        			sortTextLines(editor);
        		} else {
        			sortArticles(article);
        		}
        	}
         }
         
         else if ( cmd.equals( ActionNames.ACTION_SEARCH_WEB ) ||
        		   cmd.equals( ActionNames.ACTION_TRANSLATE_WEB )) {
        	 
        	String searchCmd = options.getOption(cmd.equals(ActionNames.ACTION_SEARCH_WEB) ?  
        				"startSearchExp" : "startTranslateExp");
        	String text = "";
        	
        	// catch selected text from current article display
            if (display != null && display.getSelectedText() != null) {
               text = display.getSelectedText().trim();
               if (text != null && !text.isEmpty()) {
                  text = Util.uriEncoded(text, null);
                  text = Util.substituteText(text, "%20", "+");
               }
            }
            
            // start browser with search command incl. search expression
            try {
                searchCmd = Util.substituteText(searchCmd, "$text", text);
                Global.startBrowser(new URL(searchCmd));
             } catch (MalformedURLException e) {
                e.printStackTrace();
             }
         }

         else if ( cmd.equals( ActionNames.MANAGE_ENCRYPTION ) ) {
        	 if (document != null && !document.isReadOnly()) {
				// verify access by confirming current passphrase
				if (document.isEncrypted()) {
					TerminationType terminated = DocumentEncryptionDialog.
						verifyPassphrase(Global.getActiveFrame(), document.getPassphrase());
					switch (terminated) {
					case FAILED:
						GUIService.failureMessage("msg.failure.input.passphrase", null);
					case CANCELED:
						return;
					case CONFIRMED:
					}
				}
				
				// input dialog for new passphrase (assigns new value to document)
				new DocumentEncryptionDialog().show();
        	 }
         }
                         
         else if ( cmd.equals( ActionNames.MANAGE_DELETE_HISTORY ) ) {
             if (document != null) {
            	 String text = displayText("msg.ask.history.delete");
            	 text = Util.substituteText(text, "$name", document.getShortTitle());
            	 if (GUIService.userConfirm(text) && !IO_Manager.get().deleteFileHistory(document)) {
           			 GUIService.infoMessage(null, "msg.warning.delete-files");
           		 }
             }
          }

         else if ( cmd.equals( ActionNames.MANAGE_PREFERENCES ) ) {
        	 new PreferencesDialog().setVisible(true);
         }
         
         else if ( cmd.equals( ActionNames.HELP_ABOUT ) ) {
        	 try {
        		 new AboutDialog().show();
			} catch (org.ragna.exception.DuplicateException e) {
			}
         }

         else if ( cmd.equals( ActionNames.HELP_INFO_SYSTEM ) ) {
       		 showSystemInfo();
         }

         else if ( cmd.equals( ActionNames.HELP_USER_MANUAL ) ) {
       		 InputStream input = ResourceLoader.get().getResourceStream("#system/user_manual.hjt");
       		 if (input != null) {
       			 PadDocument doc = IO_Manager.get().openDocument(input, "UTF-8");
       			 input.close();
       			 registerAndDisplayDocument(doc, null, false);
       		 } else {
       			 GUIService.infoMessage(null, "msg.failure.user-manual");
       		 }
         }

	     // catch left-over errors of any execution    
	     }  catch (Throwable e) {
	        GUIService.failureMessage(null, e);
	        e.printStackTrace();
	     }
      }
   } // ActionExecutor

   private class MirrorHandler implements PropertyChangeListener {

      @Override
      public void propertyChange (PropertyChangeEvent evt) {
         String key = evt.getPropertyName();
         if (key == "documentAdded") { 
        	// add document to mirror system (if not it is marked as BACKUP)
            PadDocument doc = (PadDocument)evt.getNewValue();
            if (doc.isBackupCopy()) return;
            addDocumentToMirrorSystem(doc);
            
            // if document is new (no file reference), note UUID into system options
            if (doc.getExternalPath() == null) {
            	PersistentOptions options = Global.getOptions();
            	List<String> list = options.getStringList("list-new-docs");
            	list.add(doc.getUUID().toHexString());
            	options.setStringList("list-new-docs", list);
            	Log.debug(6, "(ActionHandler.MirrorHandler) added document UUID to NEW-DOC-LIST (system options): " 
            	          + doc.getShortTitle());
            }
         }
         else if (key == "documentRemoving") {
            PadDocument doc = (PadDocument)evt.getNewValue();
      	  	if (!doc.hasExternalFile()) {
      	  		removeHistoryMirrorsOfDocument(doc);
      	  	}
         }
         else if (key == "documentRemoved") {
            PadDocument doc = (PadDocument)evt.getNewValue();
       	  	removeMirrorOfDocument(doc);
            removeDocumentFromMirrorSystem(doc);
         }
      }
      
   }

   private class MirrorFileAdapter implements MirrorFileManager.Mirrorable {
      private PadDocument document;
      private int modifyNumber;
      private long modifyTime;
      
      /** This is the regular adapter constructor used for new and existing
       * documents.
       * 
       * @param document {@code PadDocument}
       */
      public MirrorFileAdapter (PadDocument document) {
    	 Objects.requireNonNull(document, "document is null");
         this.document = document;
      }
   
      /** Creates an adapter with an empty Treepad document of the given
      * identifier UUID. This constructor creates the document and is used for
      * reconstruction processes of NEW DOCUMENTS during program startup.
      * 
      * @param id String UUID hexadecimal expression (32 char)
      * @throws IllegalArgumentException if UUID expression is invalid
      */
      public MirrorFileAdapter (String id) {
   	  Objects.requireNonNull(id);
        document = new DefaultPadDocument(DocumentType.TreePad, new UUID(id));
      }
  
      @Override
      public String getIdentifier () {
         return mirrorAdapterIdentifier(document);
      }
   
      @Override
      public int getModifyNumber () {
         long time = document.getModifyTime(); 
         if (time != modifyTime) {
            modifyNumber++;
            modifyTime = time;
         }
         return modifyNumber;
      }
   
      @Override
      public Mirrorable getMirrorableClone () {return null;}
   
      @Override
      public void mirrorWrite (OutputStream out) throws IOException {
    	 document.savePreferences();
         IO_Manager.get().saveDocument(document, out, document.getEncoding());
         String text = displayText("msg.mirror.written").concat(document.getShortTitle());
         Global.getStatusBar().putMessage(text, 10000, UnixColor.Indigo);
      }
   
      @Override
      public void mirrorsDetected (List<File> files) {
    	  if (document.hasExternalFile()) return;
    	  
    	  // we operate on mirrors of NEW orphan files (w/o external file) only
    	  // and load them as reconstructions into the display
    	  if (!files.isEmpty()) {
    		  File mir = files.get(0);
    		  
              // open the document in registry and GUI
			  document = openDocumentFromPath(mir.getAbsolutePath(), null, false);
			  String path = document.getExternalPath();
			  
			  // remove unwanted file references
			  IO_Manager.get().removeExternalFileReference(document);
			  document.setModified();
			  Global.getRecentFilesStack().remove(path);
    	  }
      }
   }
   
   enum DocumentEditType {CUT, PASTE, DELETE, CREATE, SORT, MOVE_UP, MOVE_DOWN, INDENT, OUTDENT;}
   
   private class DocumentUndoableEdit extends AbstractUndoableEdit {
	   private DocumentEditType type;
	   private PadDocument document;
	   private PadArticle parent;
	   private PadArticle[] art, art2;
	   private int position;
	   
	   public DocumentUndoableEdit (DocumentEditType type, 
			   						PadDocument doc, 
			   						PadArticle[] articles, 
			   						int index) {
		   Objects.requireNonNull(type);
		   Objects.requireNonNull(doc);
		   Objects.requireNonNull(articles);
		   Util.requirePositive(index);
		   if (index > doc.getArticleCount()) 
			   throw new IllegalArgumentException("index out of bounds: " + index);
		   if (articles.length == 0) 
			   throw new IllegalArgumentException("empty article array");
		   
		   document = doc;
		   parent = articles[0].getParent();
		   art = articles;
		   position = index;
		   this.type = type;
	   }

	@Override
	public void undo() throws CannotUndoException {
		super.undo();
		
		DocumentOrderView orderView = DisplayManager.get().getSelectedOrderView();
		if (orderView == null) return;
		
		switch (type) {
		case SORT:
			art2 = document.cutoutArticleAt(position);
		case CUT:
		case DELETE:
			document.insertArticleAt(parent, position, art);
			document.setSelectedIndex(position);
			break;
		case CREATE:
		case PASTE:
			document.removeArticles(art);
			break;
		case INDENT:
			orderView.setSelectedElement(art[0]);
			orderView.outdent();
			break;
		case MOVE_DOWN:
			orderView.setSelectedElement(art[0]);
			orderView.moveUp();
			break;
		case MOVE_UP:
			orderView.setSelectedElement(art[0]);
			orderView.moveDown();
			break;
		case OUTDENT:
			orderView.setSelectedElement(art[0]);
			orderView.indent();
			break;
		default:
			break;
		}
	}

	@Override
	public void redo() throws CannotRedoException {
		super.redo();
		
		DocumentOrderView orderView = DisplayManager.get().getSelectedOrderView();
		if (orderView == null) return;
		
		switch (type) {
		case SORT:
			document.cutoutArticleAt(position);
			document.insertArticleAt(parent, position, art2);
			document.setSelectedIndex(position);
			break;
		case CUT:
			if (clipboardSecurityConfirmed(document)) {
				articleStack = document.copyArticlesAt(position);
			}
		case DELETE:
			document.cutoutArticleAt(position);
			break;
		case CREATE:
		case PASTE:
			document.insertArticleAt(parent, position, art);
			document.setSelectedIndex(position);
			break;
		case INDENT:
			orderView.setSelectedElement(art[0]);
			orderView.indent();
			break;
		case MOVE_DOWN:
			orderView.setSelectedElement(art[0]);
			orderView.moveDown();
			break;
		case MOVE_UP:
			orderView.setSelectedElement(art[0]);
			orderView.moveUp();
			break;
		case OUTDENT:
			orderView.setSelectedElement(art[0]);
			orderView.outdent();
			break;
		default:
			break;
		}
	}

	@Override
	public String getPresentationName() {
		String hs = "";
		switch (type) {
		case CUT: hs = "CUT article"; break;
		case DELETE: hs = "DELETE article"; break;
		case PASTE: hs = "PASTE article"; break;
		case CREATE: hs = "CREATE article"; break;
		case INDENT: hs = "INDENT article"; break;
		case MOVE_DOWN: hs = "MOVE-DOWN article"; break;
		case MOVE_UP: hs = "MOVE-UP article"; break;
		case OUTDENT: hs = "OUTDENT article"; break;
		case SORT: hs = "SORT article"; break;
		}
		return hs;
	}
	   
	   
   }

   /** Controls the given list of document identifiers for mirror occurrences
    * and opens these mirrors as new documents (lacking an external file 
    * definition) into the display. At this time the mirrors will not be removed.
    * (The mirrors will only be removed when these documents are saved to files.)
    *   
    * @param olist {@code List<String>} list of hexadecimal string values
    */
	public void controlOrphanMirrors (List<String> olist) {
		MirrorFileManager man = Global.getMirrorFileManager();
		for (String oid : olist) {
			// adds a special Mirrorable for orphans
			// this will trigger reactions (callback method) which ensure
			// treatment of orphan mirrors
			Mirrorable mirable = new MirrorFileAdapter(oid);
			boolean added = man.addMirrorable(mirable);

			// if no history mirrors are available for the orphan entry 
			if (man.getHistoryMirrors(oid).isEmpty()) {
                // remove ID-entry in NEW-DOC-LIST (system options)
				removeFromNewFileList(oid);
                
                // remove orphan Mirrorable
//                man.removeMirrorable(oid);
			} else if (added) {
	     	    Log.debug(6, "(ActionHandler.controlOrphanMirrors) added ORPHAN mirror adapter for document UUID from NEW-DOC-LIST (system options): " 
	 	                 + oid);
			}
		}
	}

/** Reloads the given document and returns the new instance or null if the
 * loading failed.
 * 	
 * @param document {@code PadDocument}
 * @return {@code PadDocument}
 */
public PadDocument reloadDocument (PadDocument document) {
	if (!document.hasExternalFile()) return null;
	boolean ok = true;
	if (document.isModified()) {
		// inform user about dangers and ask to confirm reloading
		String title = displayText("dlg.title.reload");
		title = Util.substituteText(title, "$name", document.getShortTitle());
		String text = displayText("msg.ask.reloading.doc"); 
		ok = GUIService.userConfirm(title, text);
	}
	if (ok) {
		// reload the document (losing modifications)
		int selected = document.getSelectedIndex();
		String path = document.getExternalPath();
		closeDocument(document, false, false);
		document = openDocumentFromPath(path, document.getPassphrase(), false);
		document.setSelectedIndex(selected);
		return document; 
	}
	return null;
}

@Override
public boolean overagedTask (Action action) {
	String name = (String) action.getValue(ACTION_KEY_PROPERTY);
	if (name == null) name = "?";
	
	String text = displayText("msg.ask.overaged-task");
	text = Util.substituteText(text, "$name", name);
	return GUIService.userConfirm(text);
}

}
