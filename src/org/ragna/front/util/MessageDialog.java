/*
*  File: MessageDialog.java
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.ragna.core.Global;
import org.ragna.front.GUIService;

public class MessageDialog extends ButtonBarDialog {
	
   public static enum MessageType { noIcon, info, question, warning, error }

   // return values for confirmMessage2
   public static final int NON_MODAL_DIALOG = -1;
   public static final int BUTTONLESS_CLOSE = 0;
   public static final int YES_OK_BUTTON_PRESSED = 1;
   public static final int NO_BUTTON_PRESSED = 2;
   public static final int CANCEL_BUTTON_PRESSED = 3;
   
   private static final Color warningColor = new Color( 0xff, 0x7f, 0x50 ); // netscape.coral
   private static final Color errorColor = new Color( 0xf0, 0x80, 0x80 ); // netscape.lightcoral

   private MessageContentPanel   dialogPanel;
   
/**
 * Creates an empty, non-modal info message dialog with the active mainframe
 * as owner and OK + CANCEL buttons. 
 */ 
public MessageDialog () throws HeadlessException {
   super();
   init(MessageType.info, null, null);
}

/**
 * Creates a modal info message dialog with OK_BUTTON 
 * and the given title and text content.
 *   
 * @param owner <code>Window</code> the parent window (Dialog or Frame);
 *        may be <b>null</b>
 * @param text <code>Object</code> the text content (<code>String</code> 
 *        or <code>Component</code>); may be <b>null</b>
 *        
 * @throws HeadlessException
 */
public MessageDialog (Window owner, String title, Object text) throws HeadlessException {
   this(owner, title, text, MessageType.info, DialogButtonBar.OK_BUTTON, true);
}

/**
 * Creates an empty info message dialog 
 * of the given dialog type and modality.
 *  
 * @param owner <code>Window</code> the parent window (Dialog or Frame);
 *        may be <b>null</b>
 * @param dlgType int, dialog type declares standard buttons used 
 *        (values from class <code>DialogButtonBar</code>,  
 *        use <code>DialogButtonBar.BUTTONLESS</code> for void button bar) 
 * @param modal boolean, whether this dialog is operating modal
 * 
 * @throws HeadlessException
*/ 
public MessageDialog (Window owner, int dialogType, boolean modal) throws HeadlessException {
   this(owner, null, null, MessageType.info, dialogType, true);
}

/**
 * Creates a message dialog with given content and settings.
 * 
 * @param owner <code>Window</code> the parent window (Dialog or Frame);
 *        may be <b>null</b>
 * @param text Object the text content; may be <b>null</b> 
 * @param msgType <code>MessageType</code> determines display outfit (e.g. icon)
 * @param dlgType int, dialog type declares standard buttons used 
 *        (values from class <code>DialogButtonBar</code>,  
 *        use <code>DialogButtonBar.BUTTONLESS</code> for void button bar) 
 * @param modal boolean, whether this dialog is operating modal
 * 
 * @throws HeadlessException
 */
public MessageDialog (Window owner, String title, Object text, 
      MessageType msgType, int dlgType, boolean modal) throws HeadlessException {
   super(owner, dlgType, modal);
   init(msgType, title, text);
}

private void init (MessageType mType, String title, Object text) {
   // set dialog title, if null then default value of message type
   if (title == null) {
	   switch (mType) {
		case error: title = "dlg.error"; break;
		case question: title = "dlg.confirm"; break;
		case warning: title = "dlg.warning"; break;
		case info:
		case noIcon:
		default: title = "dlg.information";
	   }
   }
   setTitle(Global.res.codeOrRealDisplay(title));
   
   // body panel (base)
   dialogPanel = new MessageContentPanel() ;
   dialogPanel.setMessageType(mType);
   dialogPanel.setText(text);
   setDialogPanel(dialogPanel);

   Component target = getParent();
   moveRelatedTo(target);
   setSynchronous(true);
   setAutonomous(true);
   pack();
}

/** Sets the message display type for this dialog.
 * 
 * @param type <code>MessageType</code> new message type
 */
public void setMessageType ( MessageType type ) {
   dialogPanel.setMessageType( type );
}

/** Returns the <code>MessageType</code> of this dialog.
 *  
 * @return <code>MessageType</code>
 */
public MessageType getMessageType () {
   return dialogPanel.getMessageType();
}

/**
 * Sets a text label to this message dialog and resizes
 * it if required.
 * 
 * @param text <code>Object</code> new text content
 *        (an object of type <code>String</code> or <code>Component</code>)
 */
public void setText ( Object text ) {
   dialogPanel.setText(text);
   if (isShowing()) {
      pack();
   }
}

/**
 * Sets an icon for the message display.
 * 
 * @param icon <code>Icon</code> new message logo, use <b>null</b> to clear
 */
public void setIcon ( Icon icon ) {
   dialogPanel.setIcon( icon );
   if ( isShowing() )
      pack();
}

/** Shows this message dialog and returns a value indicating the type of dialog
 * or the cause for termination if it is a MODAL dialog.
 * <p>Possible return values are: NON_MODAL_DIALOG, BUTTONLESS_CLOSE,
 * YES_OK_BUTTON_PRESSED, CANCEL_BUTTON_PRESSED, NO_BUTTON_PRESSED.
 *  
 * @return int dialog or termination type
 */
public int showDialog () {
	   int result = isModal() ? NON_MODAL_DIALOG : BUTTONLESS_CLOSE;
	   try {
		   GUIService.showDialogOnEDT(this);
		   
		   // detect result value
		   if (isOkPressed()) {
		      result = YES_OK_BUTTON_PRESSED;
		   } else if (isCancelPressed()) {
		      result = CANCEL_BUTTON_PRESSED;
		   } else if (isNoPressed()) {
		      result = NO_BUTTON_PRESSED;
		   }
	   } catch (InterruptedException e) {
	      e.printStackTrace();
	   } catch (InvocationTargetException e) {
	      e.printStackTrace();
	   }
	   return result;
}

// ****************** STATIC SERVICE OFFER *****************

/** Displays a modal question message which the user can answer with "Yes" or "No".
 * 
 * @param parent <code>Component</code> the component this message's 
 *               display is related to
 * @param title <code>String</code> dialog title; may be <b>null</b>              
 * @param text <code>Object</code> text to display (<code>Component</code> 
 *             or <code>String</code>)
 * @param dlgType int dialog type, one of DialogButtonBar.YES_NO_BUTTON, DialogButtonBar.OK_CANCEL_BUTTON           
 * @return boolean <b>true</b> == "Yes" (confirmed), <b>false</b> == "No" (rejected/unanswered) 
 */
public static boolean showConfirmMessage (Component parent, String title, Object text, int dlgType) {
   if ( dlgType != DialogButtonBar.YES_NO_BUTTON & dlgType != DialogButtonBar.OK_CANCEL_BUTTON )
      throw new IllegalArgumentException("false dialog type");

   Window owner = GUIService.getAncestorWindow( parent );
   MessageDialog dlg = new MessageDialog(owner, title, text, MessageType.question, dlgType, true);
   dlg.showDialog();
   
   boolean result = dlg.isOkPressed();
   dlg.dispose();
   return result;
}

/** Returns a <code>JPanel</code> with a message display format and content.
 *   
 * @param text <code>Object</code> String or Component
 * @param type <code>MessageType</code> layout appearance
 * @return <code>MessageContentPanel</code>
 */
public static MessageContentPanel createMessageContentPanel 
			  (Object text, MessageType type) {
   MessageContentPanel p = new MessageContentPanel();
   p.setMessageType( type );
   p.setText( text );
   return p;
}

public static JLabel createMessageTextLabel ( String text ) {
   Objects.requireNonNull(text, "text is null");
   String hstr = Global.res.codeOrRealDisplay( text );
   if (!hstr.toLowerCase().startsWith("<html>")) {
	   hstr = "<html>" + hstr;
   }
   JLabel label = new JLabel( hstr );
   label.setOpaque( false );
   return label;
}

/** Displays a simple modal info message that the user can click away
 * through "OK" button.
 *  
 * @param parent <code>Component</code> the component this message's 
 *               display is related to
 * @param title <code>String</code> dialog title; may be <b>null</b>              
 * @param text <code>Object</code> text to display (<code>Component</code> 
 *             or <code>String</code>)
 * @param type <code>MessageType</code> appearance quality            
 */
public static void showInfoMessage (Component parent, String title, Object text, MessageType type) {
   Objects.requireNonNull(text, "text is null");
   Window owner = GUIService.getAncestorWindow(parent);
   MessageDialog dlg = new MessageDialog(owner, title, text, type, DialogButtonBar.OK_BUTTON, true);
   dlg.showDialog();
}

/** Displays a modal question message which the user can answer with 
 * "Yes", "No" or "Cancel". The result is an integer referring to the modus
 * in which the dialog was terminated.
 * 
 * @param parent <code>Component</code> the component this message's 
 *               display is related to
 * @param title <code>String</code> dialog title; may be <b>null</b>              
 * @param text <code>Object</code> text to display (<code>Component</code> 
 *             or <code>String</code>)
 * @param dlgType int dialog type, one of DialogButtonBar.YES_NO_BUTTON, 
 *         DialogButtonBar.OK_CANCEL_BUTTON or DialogButtonBar.YES_NO_CANCEL_BUTTON           
 * @return int YES_OK_BUTTON_PRESSED, NO_BUTTON_PRESSED, CANCEL_BUTTON_PRESSED
 *              or BUTTONLESS_CLOSE
 */
public static int showConfirmMessage2 ( Component parent, String title, 
                                           Object text, int dlgType ) {
   if ( dlgType != DialogButtonBar.YES_NO_BUTTON && 
        dlgType != DialogButtonBar.OK_CANCEL_BUTTON &&
        dlgType != DialogButtonBar.YES_NO_CANCEL_BUTTON )
      throw new IllegalArgumentException("false dialog type");

   Window owner = GUIService.getAncestorWindow( parent );
   MessageDialog dlg = new MessageDialog(owner, title, text, MessageType.question, dlgType, true);
   int result = dlg.showDialog();

   // terminate dialog display
   dlg.dispose();
   return result;
}

// ***********  INNER CLASS  ******************

/**
 * A JPanel for holding message display data and layout
 * consisting of an Icon on the left side and a centered 
 * text block on the right. The text may be represented
 * by either a string or a <code>Component</code>.
 * 
 */
public static class MessageContentPanel extends JPanel {

   private JPanel   textPanel;
   private JLabel   iconLabel;
   private MessageType messageType = MessageType.noIcon;

   /** Creates a new message panel of type <code>noIcon</code>
    * and which is empty of content.
    */
   public MessageContentPanel () {
      super( new BorderLayout( 6, 6 ) );
      init();
   }

   private void init () {
      // body panel (base)
      setBorder( BorderFactory.createEmptyBorder( 12, 10, 5, 25 ) );
   
      // prepare TEXT label (CENTER of display)
      textPanel = new JPanel( new BorderLayout() ) ;
      textPanel.setOpaque( false );
      add( textPanel, BorderLayout.CENTER );
   
      // prepare ICON label (WEST side of display)
      iconLabel = new JLabel();
      iconLabel.setIconTextGap( 0 );
      iconLabel.setHorizontalAlignment( SwingConstants.CENTER );
      iconLabel.setBorder(  BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
      add( iconLabel, BorderLayout.WEST );
   }

   /** Sets the message display type for this dialog.
    * 
    * @param type <code>MessageType</code> new message type
    */
   public void setMessageType ( MessageType type ) {
      String hstr;
      Icon icon;
      
      // avoid unnecessary action
      if ( type == messageType )
         return;
      
      // set text background color
      if ( type == MessageType.warning ) {
         setBackground( warningColor );
         setOpaque( true );

      } else if ( type == MessageType.error ) {
         setBackground( errorColor );
         setOpaque( true );

      } else {
         setOpaque( false );
      }
      
      // select icon from message type
      if ( type == MessageType.info )
         hstr =  "OptionPane.informationIcon";
      else if ( type == MessageType.question )
         hstr = "OptionPane.questionIcon";
      else if ( type == MessageType.error )
         hstr = "OptionPane.errorIcon";
      else
         hstr = "OptionPane.warningIcon";
      
      // obtain UI standard icon for messages
      // and set icon
      if ( type != MessageType.noIcon ) {
         icon = UIManager.getIcon( hstr );
         setIcon( icon );
      } else { 
         setIcon( null );
      }

      messageType = type;
   }

   /**
    * Sets an icon for the message display.
    * 
    * @param icon <code>Icon</code> new message logo, use <b>null</b> to clear
    */
   public void setIcon ( Icon icon ) {
      JLabel icL = getIconLabel();
      icL.setIcon( icon );
   }

   /** Returns the <code>MessageType</code> of this dialog.
    *  
    * @return <code>MessageType</code>
    */
   public MessageType getMessageType () {
      return messageType;
   }

   private JPanel getTextPanel () {
      return textPanel;
   }

   private JLabel getIconLabel () {
      return iconLabel;
   }

   /**
    * Sets a text label to this message dialog and resizes it if required.
    * 
    * @param text <code>Object</code> new text content
    *        (an object of type <code>String</code> or <code>Component</code>)
    */
   public void setText ( Object text ) {
      JPanel panel = getTextPanel();
      panel.removeAll();
      
      if ( text != null ) {
         if ( text instanceof String ) {
        	JLabel label = createMessageTextLabel( (String)text );
            panel.add( label );
         } else if ( text instanceof Component ) {
            panel.add( (Component)text );
         }
      }
   }

}

}
