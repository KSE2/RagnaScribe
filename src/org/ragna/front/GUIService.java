/*
*  File: GUIService.java
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
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Objects;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.ragna.core.Global;
import org.ragna.front.util.ButtonBarDialog;
import org.ragna.front.util.DialogButtonBar;
import org.ragna.front.util.MessageDialog;
import org.ragna.front.util.ResourceLoader;
import org.ragna.front.util.MessageDialog.MessageType;
import org.ragna.io.IO_Manager;
import org.ragna.io.IO_Manager.StreamDirection;
import org.ragna.io.IO_Manager.SystemFileType;
import org.ragna.util.PersistentOptions;

import kse.utilclass.gui.VerticalFlowLayout;
import kse.utilclass.misc.UnixColor;
import kse.utilclass.misc.Util;

public class GUIService {

   public static final int YES_NO_OPTION = DialogButtonBar.YES_NO_BUTTON;
   public static final int YES_NO_CANCEL_OPTION = DialogButtonBar.YES_NO_CANCEL_BUTTON;
   public static final int OK_CANCEL_OPTION = DialogButtonBar.OK_CANCEL_BUTTON;
   public static final int OK_OPTION = DialogButtonBar.OK_BUTTON;
   
   // return values for confirmMessage2
   public static final int BUTTONLESS_CLOSE = MessageDialog.BUTTONLESS_CLOSE;
   public static final int YES_OK_BUTTON_PRESSED = MessageDialog.YES_OK_BUTTON_PRESSED;
   public static final int NO_BUTTON_PRESSED = MessageDialog.NO_BUTTON_PRESSED;
   public static final int CANCEL_BUTTON_PRESSED = MessageDialog.CANCEL_BUTTON_PRESSED;
   
   /** Ensures the parameter executable block is run on the AWT at a later time.
    * Event Dispatching Thread. If the current thread is an AWT
    * Event Dispatching Thread, the block is called directly
    * without transferring it via "SwingUtilities.invokeLater".
    * 
    * @param r Runnable code block to execute
    */
   public static void executeOnEDT ( Runnable r ) {
      if ( !SwingUtilities.isEventDispatchThread() )
         SwingUtilities.invokeLater( r );
      else
         r.run();
   }

   /** Ensures the parameter executable block is run on the AWT
    * Event Dispatching Thread. If the current thread is an AWT
    * Event Dispatching Thread, the block is called directly,
    * otherwise it is loaded via "SwingUtilities.invokeAndWait".
    * This command blocks until r has been run and then continues.
    * 
    * @param r Runnable code block to execute
    * @throws InvocationTargetException 
    * @throws InterruptedException 
    */
   public static void executeOnEDT_Wait ( Runnable r ) 
         throws InterruptedException, InvocationTargetException {
      if ( !SwingUtilities.isEventDispatchThread() )
         SwingUtilities.invokeAndWait( r );
      else
         r.run();
   }

   public static void toggleHelpDialog(ButtonBarDialog dialog, String helpCode) {
   }

   /** Returns the first top level <code>Window</code> component
    * that is the ancestor of the given component. 
    *  
    * @param c <code>Component</code>; may be <b>null</b>
    * @return <code>Window</code> or <b>null</b> if parameter was <b>null</b>
    */
   public static Window getAncestorWindow ( Component c ) {
      while ( c != null && !(c instanceof Window) ) { 
         c = c.getParent();
      }
      return (Window)c;
   }

   /** Displays an information message centred within the given parent component.
    * 
    * @param owner parent component; if <b>null</b> the current
    *        mainframe window is used
    * @param title title of dialog; text or token; may be <b>null</b> in which 
    *        case a standard title is used 
    * @param text message text; text or token
    */
   public static void infoMessage (Component owner, String title, String text) {
      MessageDialog.showInfoMessage(owner, title, text, MessageType.info);
   }

   /** Displays an information message without parent component. It will get 
    *  centred within the application's mainframe.
    */
   public static void infoMessage ( String title, String text ) {
      infoMessage( null, title, text );
   }

   /** Displays an information message in the application mainframe and with an
    * optional Throwable message text.
    *  
    * @param title title of dialog; text or token; may be <b>null</b> in which 
    *        case a standard title is used 
    * @param text message text; text or token
    * @param e {@code Throwable}, may be null
    */
   public static void infoMessage ( String title, String text, Throwable e ) {
	  Objects.requireNonNull(text, "text is null");
	  if (e != null && e.getMessage() != null) {
		  String hs = ResourceLoader.get().getDisplay(text);
		  if (!"FIXME".equals(hs)) {
			  text = hs;
		  }
		  text = text + "<p><font color=\"red\">" + e.getMessage();
	  }
      infoMessage( null, title, text );
   }

   /** Displays a confirm message centred within the given parent component.
    * The user can choose from either a positive or negative option (dual). 
    * The options can be YES/NO or OK/CANCEL or OK depending on the type parameter.
    * (In case of thread interruption false is returned.)
    * 
    * @param owner Component parent component; if <b>null</b> the current
    *        mainframe window is used
    * @param title String title of dialog; text or token; may be <b>null</b> 
    *        in which case a standard title is used 
    * @param text String message text; text or token
    * @param optionType int  button text type (YES_NO_OPTION/OK_CANCEL_OPTION)
    * @return boolean user choice 
    */
   public static boolean confirmMessage ( Component owner, String title, String text,
                                          int optionType) {
      // verify text parameter
      if ( text == null || text.isEmpty())
         throw new IllegalArgumentException("text is null or empty");

      // control button types
      if (optionType != YES_NO_OPTION && optionType != OK_CANCEL_OPTION )  
         throw new IllegalArgumentException("illegal optionType");
      
      // fetch text values from resources if applicable
      title = Global.res.codeOrRealDisplay( title );
      text = Global.res.codeOrRealDisplay( text );

      boolean ok = MessageDialog.showConfirmMessage(owner, title, text, optionType);
      return ok;
   }

   /** Displays a confirm message centred within the application mainframe.
    * The user can choose from either a positive or negative option (dual). 
    * The options can be YES/NO or OK/CANCEL or OK depending on the type parameter.
    * (In case of thread interruption false is returned.)
    * 
    * @param title String title of dialog; text or token; may be <b>null</b> 
    *        in which case a standard title is used 
    * @param text String message text; text or token
    * @param optionType int  button text type (YES_NO_OPTION/OK_CANCEL_OPTION)
    * @return boolean user choice 
    */
   public static boolean confirmMessage ( String title, String text, int optionType) {
       return confirmMessage(null, title, text, optionType);
   }

   /** Asks the user for text input in a message dialog and returns the answer.
    *  
    * @param owner parent component; if <b>null</b> the current
    *        mainframe window is used
    * @param title , may be <b>null</b>
    * @param text message text displayed in dialog
    * @param initial initial value of the text input field, may be <b>null</b>
    * @return String user input or <b>null</b> if cancelled
    */
   public static String userInput ( Component owner, String title, String text, String initial ) {
	  Objects.requireNonNull(text, "text is null");
      title = Global.res.codeOrRealDisplay(title == null ? "dlg.input" : title);
      
      // create message content panel
      JPanel cp = new JPanel( new VerticalFlowLayout( 10, true ) );
      cp.add( MessageDialog.createMessageTextLabel( text ) );
      JTextField fld = new JTextField( initial );
      cp.add( fld );
      
      // call the message display (question)
      boolean ok = MessageDialog.showConfirmMessage( owner, title, cp, DialogButtonBar.OK_CANCEL_BUTTON );
      return ok ? fld.getText() : null;
   }
   
   /** Asks the user for a password input in a message dialog and returns the 
    * answer as array of characters.
    *  
    * @param owner parent component; if <b>null</b> the current
    *        mainframe window is used
    * @param title , may be <b>null</b>
    * @param text message text displayed in dialog
    * @param initial initial value of the input field, may be <b>null</b>
    * @return char[] user input or <b>null</b> if cancelled
    */
   public static char[] userPasswordInput ( Component owner, String title, String text, String initial ) {
	  Objects.requireNonNull(text, "text is null");
      title = Global.res.codeOrRealDisplay(title == null ? "dlg.input" : title);
      
      // create message content panel
      JPanel cp = new JPanel(new VerticalFlowLayout(10, true));
      cp.add( MessageDialog.createMessageTextLabel(text));
      JPasswordField fld = new JPasswordField(initial);
      cp.add(fld);
      
      // call the message display (question)
      boolean ok = MessageDialog.showConfirmMessage(owner, title, cp, DialogButtonBar.OK_CANCEL_BUTTON);
      return ok ? fld.getPassword() : null;
   }
   
   private static FileNameExtensionFilter addFileTypeFilter (
                  JFileChooser chooser, SystemFileType fileType) {
      IO_Manager iom = IO_Manager.get();
      String hstr = iom.getFileTypeExtensions(fileType);
      String desc = iom.getFileTypeDescription(fileType);
      String[] extensions = hstr.split(",");
      FileNameExtensionFilter filter = new FileNameExtensionFilter(desc, extensions);
      chooser.addChoosableFileFilter(filter);
      return filter;
   }
   
   /** Checks whether the given file is permitted under a global filepath
    * restriction. If no restriction exists, true is returned. Method is
    * dialog active; a negative result is reflected to the user by a failure
    * message. There is no message for a positive result.
    * 
    * @param file File file to investigate, may be null
    * @return boolean true == file is permitted or was null
    */
   public static boolean checkFilepathPermitted (File file) {
      boolean ok = true;
      if (Global.isRestrictedFilepath() & file != null) {
         String fpath = file.getAbsolutePath(); 
         String restrictedPath = Global.getRestrictedFilepath();
         if (!fpath.startsWith(restrictedPath)) {
            // reject user selection if not under restricted filepath
            String text = Global.res.getDisplay("msg.failure.filepath.restricted") + restrictedPath;
            failureMessage("dlg.operrejected", text, null);
            ok = false;
         }
      }
      return ok;
   }
   
   /** Opens a file chooser dialog in the global current directory for the 
    * specified IO-direction and system file type.
    *  
    * @param fileType <code>IO_Manager.SystemFileType</code>
    * @param direction <code>IO_Manager.StreamDirection</code>
    * @param defValue String sets a pre-selected filename (possibly 
    *        non-existing); null for no setting
    * @return the selected file or null if no file was selected or the
    *         operation was broken
    */
   public static File getUserSelectedFile (
         SystemFileType fileType, StreamDirection direction, String defValue) {
      
      PersistentOptions options = Global.getOptions();
      IO_Manager iom = IO_Manager.get();
      File currentDir = Global.getCurrentDirectory();
      boolean restrictionCase = false;
      File file = null;
      JFileChooser fCh[] = new JFileChooser[1];
      
      int[] option = new int[1];
      Runnable run = new Runnable () {
    	 @Override
		 public void run () {
    		  fCh[0] = new PadFileChooser(currentDir);
    		  JFileChooser fChooser = fCh[0];
    	      String title = direction == StreamDirection.INPUT ? "Opening " : "Saving ";
    	      fChooser.setDialogTitle(title + (defValue == null ? "" : defValue));
    	      
    	      // add a file filter according to parameter file type setting
    	      if (fileType != SystemFileType.ANY_FILE) {
    	         FileNameExtensionFilter filter = addFileTypeFilter(fChooser, fileType);
    	         fChooser.setFileFilter(filter);
    	         
    	         if (direction == StreamDirection.INPUT & 
    	             fileType != SystemFileType.ANY_DOCUMENT_FILE  ) {
    	            addFileTypeFilter(fChooser, SystemFileType.ANY_DOCUMENT_FILE);
    	         }
    	      }

    	 	  // an optional pre-selection of the filename (under current dir)
    	      if (defValue != null && !defValue.isEmpty()) {
    	         File defaultFile = new File(currentDir, defValue);
    	         fChooser.setSelectedFile(defaultFile);
    	      }

		     // call the file chooser according to IO-direction
		     if (direction == StreamDirection.INPUT) {
		        option[0] = fChooser.showOpenDialog(Global.getActiveFrame()); 
		     } else {
		        option[0] = fChooser.showSaveDialog(Global.getActiveFrame()); 
		     }
    	 }
      };
      try {
		 GUIService.executeOnEDT_Wait(run);
	  } catch (InvocationTargetException | InterruptedException e) {
		 e.printStackTrace();
	  }
      
      // if user has approved his file selection
      if (option[0] == JFileChooser.APPROVE_OPTION) {
         // receive the file choice
         file = fCh[0].getSelectedFile();

         // if global settings demand file path restriction
         // break operation if selection does not meet the restriction 
         if (!checkFilepathPermitted(file)) {
            file = null;
            restrictionCase = true;
         }

         // apply controls and services for WRITING files
         if (file != null && direction == StreamDirection.OUTPUT) {
            
            // if global options demand default extension, supply it if needed
            if ( options.isOptionSet("useDefaultFileExtensions")) {
               String filename = file.getName();
               if (filename.indexOf(".") == -1) {
                  // add the first name extension found in filter extensions
                  String hstr = iom.getFileTypeExtensions(fileType);
                  String ext = hstr.split(",")[0];  
                  filename = filename + "." + ext;
                  file = new File(file.getParent(), filename);
               }
            }
            
            // confirm overwriting of existing file
            if (file.isFile()) {
               // ask user with confirm dialog box
               String text = Global.res.getDisplay("msg.ask.overwritefile");
               text = Util.substituteText(text, "$file", file.getAbsolutePath());
               text = Util.substituteText(text, "$time", Util.localeTimeString(file.lastModified()));
               boolean confirm = confirmMessage(null, text, YES_NO_OPTION);
               if (!confirm) {
                  file = null;
               }
            }
         }
      }
      
      // update global current directory
      File dir = restrictionCase ? new File(Global.getRestrictedFilepath()) :
    	         fCh[0].getCurrentDirectory();
      try {
		 Global.setCurrentDirectory(dir);
	  } catch (IOException e) {
		 e.printStackTrace();
		 return null;
	  }
      
      return file;
   }

   /** Shows a OK-CANCEL dialog with the given title and message and the
    * request to select a character-set from a combo with the values which
    * are supported by the current JVM. Returns the user selection or null
    * if CANCEL was pressed or the dialog closed undecided.
    * 
    * @param title String dialog title or null
    * @param text String message text or null
    * @param preset String preset value or null
    * @return String selected charset or null for user break
    */
   public static String getUserSelectedCharset (String title, String text, String preset) {
	   // create defaults
	   if (title == null) title = Global.res.getDisplay("label.charset");
	   if (text == null) text = Global.res.getDisplay("dlg.select.charset");
	   if (preset == null) preset = Charset.defaultCharset().name();
	   
	   VerticalFlowLayout layout = new VerticalFlowLayout(6);
	   layout.setAlignment(VerticalFlowLayout.CENTER);
	   JPanel panel = new JPanel(layout);
	   JLabel label = new JLabel(Global.res.codeOrRealDisplay(text));
	   panel.add(label);
	   
	   JComboBox<String> combo = new JComboBox<>(Global.getVMSupportedCharsets());
	   if (preset != null) {
		   combo.setSelectedItem(preset);
	   }
	   panel.add(combo);
	   
	   String result = null;
	   if (MessageDialog.showConfirmMessage(null, title, panel, OK_CANCEL_OPTION)) {
		   result = (String) combo.getSelectedItem();
	   }
	   return result;
   }
   
   /** Displays an error message dialog referring to the parameter exception.
    * 
    * @param owner Component owner of the message dialog
    * @param title the dialog title (text or token); if <b>null</b> a standard 
    *        title is used
    * @param text the message (text or token); if <b>null</b> a standard 
    *        message is used
    * @param e <code>Throwable</code> if not <b>null</b> this exception is 
    *        reported in the message text 
    *        
    */ 
   public static void failureMessage ( Component owner, String title, 
                                       String text, Throwable e ) {
      // install default display values on null parameter
      if (title == null)
         title = "dlg.operfailure";
      if (text == null)
         text = "msg.failure.general";
      
      title = Global.res.codeOrRealDisplay(title);
      text = Global.res.codeOrRealDisplay(text);

      if (!text.startsWith("<html>")) {
         text = "<html><font color=\"white\">" + text + "</font>";
      }
      text = appendExceptionMessage(text, e, UnixColor.LemonChiffon);
      
      MessageDialog.showInfoMessage(owner, title, text, MessageType.error);
   }

   /** Displays an error message dialog, optionally referring to the parameter
    *  exception message. The dialog is centred in the mainframe.
    *
    * @param title String window title or null (token or plain text); if <b>null</b> 
    *        a standard title is used
    * @param text String message (token or plain text); if <b>null</b> a standard 
    *        message is used
    * @param e if not <b>null</b> this exception is reported in the message text 
    *        
    */ 
   public static void failureMessage ( String title, String text, Throwable e ) {
      failureMessage( null, title, text, e );
   }

   /** Displays an error message dialog with a standard window title, 
    *  optionally referring to the parameter exception message. 
    *  The dialog is centred in the mainframe.
    *
    * @param text String message (token or plain text); if <b>null</b> a standard 
    *        message is used
    * @param e if not <b>null</b> this exception is reported in the message text 
    *        
    */ 
   public static void failureMessage ( String text, Throwable e ) {
      failureMessage( null, null, text, e );
   }

   /** Returns a concatenation of parameter 'text' and an excerpt
    *  from the given <code>Throwable</code> making it an information to the 
    *  user. If 'e' is <b>null</b>, unmodified 'text' is returned. 
    *  ('text' should best be HTML encoded; the result may contain HTML tags!)
    *      
    * @param text String preceding message text; may be <b>null</b> 
    * @param e <code>Throwable</code>, may be <b>null</b>
    * @param color {@code Color} color of text rendered
    * @return String Html encoded message 
    */
   private static String appendExceptionMessage (String text, Throwable e, Color color) {
      String colorVal = "black";
      if (color != null) {
         long cval = color.getRGB() & 0xFFFFFFL;
         colorVal = "#".concat(Util.number(cval, 6, 16).toUpperCase()); 
      }

	  StringBuffer buf = new StringBuffer(128);
	  int line = 0;
      if (text != null) {
    	  buf.append(text);
    	  line++;
      }
      if (e != null) {
    	  buf.append("<font color=\"" + colorVal + "\">");
      }
      
      // append adapted textual representation of exception if supplied 
      Throwable e1 = e;
      while (e1 != null) {
    	 if (line > 0) {
    		 buf.append("<br>");
    	 }
    	 buf.append(": ");
         String emsg = e1.getMessage(); 
         buf.append(emsg == null ? e1.getClass().getName() : emsg);
         line++;
         e1 = e1.getCause();
      }
      if (e != null) {
    	  buf.append("</font>");
      }
      return buf.toString();
   }

   /** Asks the user with a dialog to confirm a given question with "Yes" or "No".
    * (In case of thread interruption false is returned.)
    * 
    * @param owner Component the parent component for the dialog; if <b>null</b> the 
    *        mainframe window is used
    * @param text String the question as text or token
    * @return boolean the user decision 
    */ 
   public static boolean userConfirm ( Component owner, String text ) {
      boolean ok = confirmMessage(owner, null, text, YES_NO_OPTION);
      return ok;
   }

   /** Asks the user (dialog) to confirm a given question with "Yes" or "No". 
    * (In case of thread interruption false is returned.)
    * 
    * @param text String the question as text or resource token
    * @return boolean the user choice
    */
   public static boolean userConfirm ( String text )  {
      return userConfirm((String)null, text);
   }

   /** Asks the user (dialog) to confirm a given question with "Yes" or "No". 
    * (In case of thread interruption false is returned.)
    * 
    * @param text String the question as text or resource token
    * @return boolean the user choice
    */
   public static boolean userConfirm ( String title, String text )  {
      boolean ok = confirmMessage(null, title, text, YES_NO_OPTION);
      return ok;
   }

   /** Displays a confirm message centred within the given parent component.
    * The user can choose from either a positive, a negative or a cancel 
    * option. 
    * The options can be YES, OK, NO or CANCEL depending on the type parameter.
    * In case of thread interruption BUTTONLESS_CLOSE is returned.
    * 
    * @param owner Component parent component; if <b>null</b> the current
    *        mainframe window is used
    * @param title String title of dialog; text or token; may be <b>null</b> 
    *        in which case a standard title is used 
    * @param text String message text; text or token
    * @param optionType int  button text type (YES_NO_OPTION / 
    *             YES_NO_CANCEL_OPTION / OK_CANCEL_OPTION)
    * @return int user choice (YES_OK_BUTTON_PRESSED, NO_BUTTON_PRESSED, 
    *             CANCEL_BUTTON_PRESSED, BUTTONLESS_CLOSE) 
    */
   public static int confirmMessage2 ( String title, String text, int optionType) {
      return confirmMessage2(Global.getActiveFrame(), title, text, optionType);
   }
   
     /** Displays a confirm message centered within the given parent component.
       * The user can choose from either a positive, a negative or a cancel 
       * option. 
       * The options can be YES, OK, NO or CANCEL depending on the type parameter.
       * In case of thread interruption BUTTONLESS_CLOSE is returned.
       * 
       * @param owner Component parent component; if <b>null</b> the current
       *        mainframe window is used
       * @param title String title of dialog; text or token; may be <b>null</b> 
       *        in which case a standard title is used 
       * @param text String message text; text or token
       * @param optionType int  button text type (YES_NO_OPTION / 
       *             YES_NO_CANCEL_OPTION / OK_CANCEL_OPTION)
       * @return int user choice (YES_OK_BUTTON_PRESSED, NO_BUTTON_PRESSED, 
       *             CANCEL_BUTTON_PRESSED, BUTTONLESS_CLOSE) 
       */
      public static int confirmMessage2 ( Component owner, String title, String text,
                                             int optionType) {
         // verify text parameter
         if ( text == null || text.isEmpty())
            throw new IllegalArgumentException("text is null or empty");
   
         // control button types
         if (optionType != YES_NO_CANCEL_OPTION &&
             optionType != OK_CANCEL_OPTION &&
             optionType != YES_NO_OPTION ) {  
            throw new IllegalArgumentException("illegal optionType");
         }
         
         int result = MessageDialog.showConfirmMessage2(owner, title, text, optionType);
         return result;
      }

      public static void showDialogOnEDT (final JDialog dlg) 
    	      throws InterruptedException, InvocationTargetException {
    	   
    	   Runnable task = new Runnable () {
    	      @Override
    	      public void run () {
    	         dlg.setVisible(true);
    	      }
    	   };
    	   
    	   if (dlg.isModal()) {
    	      GUIService.executeOnEDT_Wait(task);
//    	      Log.debug(10, "(GUIService.showDialogOnEDT) ---- terminated WAIT for MODAL dialog");
    	   } else {
    	      GUIService.executeOnEDT(task);
//    	      Log.debug(10, "(GUIService.showDialogOnEDT) displayed NON-MODAL dialog");
    	   }
    	}

}
