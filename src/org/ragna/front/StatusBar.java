/*
*  File: StatusBar.java
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
import java.awt.Dimension;
import java.awt.Font;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.ragna.front.util.ResourceLoader;

import kse.utilclass.misc.Log;

/**
 *  A status bar component (JPanel) with multiple separately addressable display 
 *  elements. These elements are "Text", "File Format" and "Program Activity".
 *  <p>The status bar is a constitutional part of the program mainframe.
 *  
 *  <p>StatusBar operates display activity safely on the EDT. 
 */
public class StatusBar extends JPanel {
	
   /** Max. message display duration time; 30 seconds. */
   public static final int MAX_TEXT_DISPLAY_TIME = 30000;
   public static final int MIN_TEXT_DISPLAY_TIME = 4000;
   public static final int MSG_QUEUE_CAPACITY = 100;
   public static final Color DEFAULT_TEXT_COLOR = Color.BLACK;;
   public static final int ACTIVE = 1;
   public static final int PASSIVE = 0;
   
   private static Timer timer;
   
   /** Marker to identify the operation target for this status-bar. */
   private enum OperationType { message, counter, dataformat, activity, font, activeCell }; 

   private Queue<MessageOrder> msgQueue = 
                    new ArrayBlockingQueue<MessageOrder>(MSG_QUEUE_CAPACITY);
   
   private JPanel          rightPanel;
   private JLabel          textField = new JLabel();
   private JLabel          activeLabel = new JLabel();
   private JLabel          formatLabel = new JLabel();
   private JLabel          counterLabel = new JLabel();
   private RemoverTask     statusTextRemover;
   private RemoverTask     activeCellRemover;
   private MessageOrder    currentMsgOrder;
   private Color           defaultTextColor = DEFAULT_TEXT_COLOR;
   private Color           defaultBgdColor = getBackground();
   private int             maxTextDisplayTime = MAX_TEXT_DISPLAY_TIME;
   private int             minTextDisplayTime = MIN_TEXT_DISPLAY_TIME;
   private int		   	   activityDisplayed;

public StatusBar () {
   super( new BorderLayout() );
   init();
}

private void init () {
   JPanel panel;
   
   // determine fonts
   setFont( textField.getFont() );
   
   // init message text output
   add( textField, BorderLayout.CENTER );
   textField.setBorder( BorderFactory.createCompoundBorder(  
         BorderFactory.createMatteBorder( 1, 1, 1, 1, Color.gray ),
         BorderFactory.createEmptyBorder( 3, 8, 3, 2 ) ));

   // init right side parts
   rightPanel = new JPanel( new BorderLayout() );
   add( rightPanel, BorderLayout.EAST );
   panel = new JPanel( new BorderLayout() );
   rightPanel.add( panel, BorderLayout.EAST );

   // program activity icon
   activeLabel.setOpaque(true);
   panel.add( activeLabel, BorderLayout.EAST );
   activeLabel.setPreferredSize( new Dimension( 22, 16 ) );
   activeLabel.setBorder( BorderFactory.createCompoundBorder(  
         BorderFactory.createMatteBorder( 1, 0, 1, 0, Color.gray ),
         BorderFactory.createEmptyBorder( 2, 3, 2, 0 ) ));
   
   // record counter cell
   panel.add( counterLabel, BorderLayout.CENTER );
   counterLabel.setVisible( false );
   counterLabel.setBorder( BorderFactory.createCompoundBorder(  
         BorderFactory.createMatteBorder( 1, 0, 1, 1, Color.gray ),
         BorderFactory.createEmptyBorder( 2, 4, 2, 4 ) ));

   // file format cell
   rightPanel.add( formatLabel, BorderLayout.CENTER );
   formatLabel.setVisible( false );
   formatLabel.setBorder( BorderFactory.createCompoundBorder(  
         BorderFactory.createMatteBorder( 1, 0, 1, 1, Color.gray ),
         BorderFactory.createEmptyBorder( 2, 4, 2, 4 ) ));
   
}  // init

/** Sets the {@code Timer} thread for this status-bar. If the value is null
 * this class will create its own timer.
 * 
 * @param timer {@code Timer}, may be null
 */
public static void setTimer (Timer timer) {
	StatusBar.timer = timer;
}

/** Drops a text message into the status line.
 * 
 * @param text String message text or null to clear
 */  
public void putMessage ( String text ) {
   putMessage(text, -1, null);
}

/** Drops a text message into the status line with the given text color.
 * 
 * @param text String message text or null to clear
 * @param textColor Color text color, may be null for default
 */  
public void putMessage ( String text, Color textColor ) {
   putMessage(text, -1, textColor);
}

/** Drops a text message into the status line with the given values for
 * display duration and text color.
 * 
 * @param text String message text or null to clear
 * @param time int milliseconds of display, -1 for default
 * @param textColor Color text color, may be null for default
 */  
public synchronized void putMessage ( String text, int time, Color textColor ) {
//   Log.log(10, "(StatusBar.putMessage) -- entering with text=" + text +", time=" + time);
   
   // get hold of parameter and create msg-order
   text = text == null ? "" : ResourceLoader.get().codeOrRealDisplay( text );
   MessageOrder order = new MessageOrder(text, time, textColor);

   // if no message is showing, display order immediately
   if (!isMessageShowing()) {
//      Log.log(10, "(StatusBar.putMessage) -- IMMEDIATE DISPLAY (branch), text=".concat(text));
      startOperation( OperationType.message, order );

   // if message is showing
   } else {
      // if msg-queue is not empty, only append the new order
      if (!msgQueue.isEmpty()) {
//         Log.log(10, "(StatusBar.putMessage) -- STORE-TO-QUEUE (branch), text=".concat(text));
         msgQueue.offer(order);
      
      // if msg-queue is empty ..   
      } else {
         // investigate and correct the rest display time of the current msg 
         // cancel current REMOVER task
         if (statusTextRemover != null) {
            statusTextRemover.cancel();
         }
         
         // calculate remaining display time of current message
         int alreadyShowing = (int)(System.currentTimeMillis() - 
                               currentMsgOrder.getCreationTime());   // FIXME something wrong?
         int restTime = minTextDisplayTime - alreadyShowing;
   
         // if time has run out ..
         if (restTime-250 < 0) {
            // ..show new message immediately
//            Log.log(10, "(StatusBar.putMessage) -- IMMEDIATE DISPLAY (time-runout), text=".concat(text));
            startOperation( OperationType.message, order );
   
         } else {
            // put order of new message into queue
            // and schedule new REMOVER task
//            Log.log(10, "(StatusBar.putMessage) -- STORE-TO-QUEUE (rest-time = " +
//                        restTime + "), text=".concat(text));
            if ( msgQueue.offer(order) ) {
               statusTextRemover = new RemoverTask(0);
               getTimer().schedule( statusTextRemover, restTime );
            }
         }
      }
   }
}

/** Sets the content of the "Format" cell of the status line. 
 *  If <b>null</b> the cell will be invisible. 
 * 
 *  @param text new content of Format cell
 */
public void setFormatCell ( String text ) {
//   Log.log( 10, "(StatusBar.setFormatCell) set FORMAT with " + text); 
   startOperation( OperationType.dataformat, text );
}

/** Sets the content of the "Text Encoding" cell of the status line. 
 *  If <b>null</b> the cell will be invisible. 
 * 
 *  @param text new content of Encoding cell
 */
public void setRecordCounterCell ( String text ) {
//   Log.log( 10, "(StatusBar.setRecordCounterCell) set COUNTER with " + text ); 
   startOperation( OperationType.counter, text );
}

/** Informs StatusBar about the current program activity modus. This will set 
 *  the content of the "Program Activity" cell.
 * 
 *  @param activity program activity constant (ACTIVE or PASSIVE)   
 * */
public synchronized void setActivity ( int activity ) {
//   Log.log( 10, "(StatusBar.setActivity) set ACTIVITY with " + activity ); 
   if (activityDisplayed != activity) {
	   startOperation(OperationType.activity, new Integer(activity));
	   activityDisplayed = activity;
   }
}

/** Sets the background color of the Activity cell, optionally for a duration
 * time given.
 * 
 * @param color {@code Color} or null for default
 * @param duration int time in milliseconds, -1 for unlimited
 */
public void setActivityCellColor (Color color, int duration) {
	if (color == null && defaultBgdColor != null) {
		color = defaultBgdColor;
	} 
	
	ActiveCellOrder order = new ActiveCellOrder(duration, color);
	startOperation(OperationType.activeCell, order);
	
}

@Override
public void setFont ( Font font ) {
   startOperation( OperationType.font, font );
}

@Override
public Font getFont () {
   return textField == null ? super.getFont() : textField.getFont();
}

public static Timer getTimer () {
	if (timer == null) {
		timer = new Timer();
	}
	return timer;
}

/** Starts a display operation executed on the EDT asynchronously. 
 * 
 * @param operation <code>OperationType</code>
 * @param param Object operation parameter suited for the given type;
 *        if null the operation performs a clearing or its target
 */
protected synchronized void startOperation ( OperationType operation, Object param ) {
   // order handling for operation type 'message' 
   if (operation == OperationType.message) {
      // cancel an already scheduled REMOVER task
      if ( statusTextRemover != null ) {
         statusTextRemover.cancel();
      }

      // set new values for members 'currentMsgOrder' and 'statusTextRemover'
      currentMsgOrder = (MessageOrder)param;
      if (currentMsgOrder != null) {
         // get display duration from order and limit to maximum time
         currentMsgOrder.duration = Math.min(currentMsgOrder.getDuration(), maxTextDisplayTime);

         // create new REMOVER task, scheduled according to this message order
         statusTextRemover = new RemoverTask(0);
         getTimer().schedule( statusTextRemover, currentMsgOrder.duration );
      }
   }
   
   if (operation == OperationType.activeCell) {
      // cancel an already scheduled REMOVER task
      if ( activeCellRemover != null ) {
    	  activeCellRemover.cancel();
      }

      ActiveCellOrder cellOrder = (ActiveCellOrder) param;
	  if (cellOrder != null && cellOrder.duration > -1) {
         // create new REMOVER task, scheduled according to this message order
         activeCellRemover = new RemoverTask(1);
         getTimer().schedule( activeCellRemover, cellOrder.duration );
	  }
   }
   
   GUIService.executeOnEDT( new SwingOperation(operation, param) );
}

/** Clears the current message display including any messages waiting in the
 * queue. */
public synchronized void clearMessage () {
   msgQueue.clear();
   startOperation(OperationType.message, null);
}

public void shutdown () {
   if (statusTextRemover != null) {
      statusTextRemover.cancel();
   }
   if (activeCellRemover != null) {
	  activeCellRemover.cancel();
   }
}

/** Returns the text color that is active when no color is specified
 * in calling the <code>setMessage()</code> method.
 * 
 * @return Color
 */
public Color getDefaultTextColor () {
   return defaultTextColor;
}

/** Sets the text color that is active when no color is specified
 * in calling the <code>setMessage()</code> method.
 * 
 * @param defaultTextColor <code>Color</code>
 */
public void setDefaultTextColor (Color defaultTextColor) {
   this.defaultTextColor = defaultTextColor == null ? DEFAULT_TEXT_COLOR 
                            : defaultTextColor;
}

/** Returns the current setting of the maximum time in milliseconds for a 
 * single message to show.
 * 
 * @return int milliseconds
 */
public int getMaxTextDisplayTime () {
   return maxTextDisplayTime;
}

/** Sets the maximum time in milliseconds for a single message to show.
 * The minimum value for this property is 1000. If the parameter is
 * below the minimum value, the default value MAX_TEXT_DISPLAY_TIME is
 * assigned.
 * 
 * @param maxTextDisplayTime int milliseconds
 */
public void setMaxTextDisplayTime (int maxTextDisplayTime) {
   this.maxTextDisplayTime = maxTextDisplayTime < 1000 ? MAX_TEXT_DISPLAY_TIME  
                              : maxTextDisplayTime;
}

/** Returns the minimum time in milliseconds for a single message to show in
 * case of "overrun" message ordering conditions. This property poses no
 * limitation to the 'duration' parameter of the <code>setMessage()</code> 
 * method.
 * 
 * @return int milliseconds
 */
public int getMinTextDisplayTime () {
   return minTextDisplayTime;
}

/** Whether there is currently a message showing int this status line.
 * 
 * @return boolean true == message showing
 */
public boolean isMessageShowing () {
   return currentMsgOrder != null;
}

/** Sets the minimum time in milliseconds for a single message to show in
 * case of "overrun" message ordering conditions. This property poses no
 * limitation to the 'duration' parameter of the <code>setMessage()</code> 
 * method.
 * The minimum value for this property is 0. If the parameter is
 * below the minimum value, the default value MIN_TEXT_DISPLAY_TIME is
 * assigned.
 * 
 * @param minTextDisplayTime int milliseconds
 */
public void setMinTextDisplayTime (int minTextDisplayTime) {
   this.minTextDisplayTime = minTextDisplayTime < 0 ? MIN_TEXT_DISPLAY_TIME 
                              : minTextDisplayTime;
}

// -------------- inner classes --------------

private class MessageOrder {
   String text;
   int duration = -1;
   Color textColor;
   long creationTime;
   
   /** Creates a new message order with display text, a time amount to
    * display the message and an optional text color.
    * 
    * @param text String message text (may be null for clear)
    * @param showDuration int intended duration to show the message in 
    *         milliseconds, -1 for maximum allowed time
    * @param textColor Color text color, null for the default text color
    */
   MessageOrder (String text, int showDuration, Color textColor) {
      this.text = text;
      this.textColor = textColor;
      this.duration = Math.max(showDuration, -1);
      creationTime = System.currentTimeMillis();
   }

   public long getCreationTime () {
      return creationTime;
   }

//   /** Creates a new message order with the given text, the maximum allowed
//    * display time and the default text color.
//    * 
//    * @param text String message text (may be null for clear)
//    */
//   MessageOrder (String text) {
//      this.text = text;
//   }

   String getText () {
      return text;
   }
   
   int getDuration () {
      return duration == -1 ? maxTextDisplayTime : duration;
   }
   
   Color getTextColor () {
      return textColor;
   }
   
   @Override
   public String toString () {
      return text;
   }
}

private class ActiveCellOrder {
   int duration;
   Color color;

   ActiveCellOrder (int duration, Color color) {
	   Objects.requireNonNull(color);
	   this.duration = duration;
	   this.color = color;
   }
}

/** This class runs Swing relevant operations and is intended to run on the EDT.  
 */
private class SwingOperation implements Runnable {
	
   private OperationType operation;
   private Object par;

   SwingOperation ( OperationType type, Object param ) {
      operation = type;
      par = param;
   }

   @Override
   public void run () {
      try {
         switch ( operation ) {
         case message:
            MessageOrder order = (MessageOrder)par;
            startTextMessage(order);
            break;
            
         case dataformat:
//            Log.log( 10, "(StatusBar.SwingOperation.run) DATAFORMAT with ".concat( 
//                  par == null ? "null" : par.toString() ));
            if ( par == null ) {
               formatLabel.setVisible( false );
            } else {
               formatLabel.setText( par.toString() );
               formatLabel.setVisible( true );
            }
            break;
            
         case counter:
//            Log.log( 10, "(StatusBar.SwingOperation.run) COUNTER with ".concat( 
//                  par == null ? "null" : par.toString() ));
            if ( par == null ) {
               counterLabel.setVisible( false );
            } else {
               counterLabel.setText( par.toString() );
               counterLabel.setVisible( true );
            }
            break;
            
         case activity:
            boolean activ = par != null && ((Integer)par).intValue() == ACTIVE;
//            Log.log( 10, "(StatusBar.SwingOperation.run) ACTIVITY with ".concat( String.valueOf( activ )));
            Icon icon = activ ? ResourceLoader.get().getImageIcon("system.activity") : null;
            activeLabel.setIcon(icon);
            break;
            
         case font:
            if ( par != null ) {
               Font font = (Font)par;
//               Log.log( 10, "(StatusBar.SwingOperation.run) FONT with ".concat( font.getName() ));
               if ( font != null & counterLabel != null ) {
                  font = font.deriveFont( Font.PLAIN );
                  counterLabel.setFont( font );
                  formatLabel.setFont( font );
                  textField.setFont( font );
               }
               StatusBar.super.setFont( font );
            }
            break;
            
		case activeCell:
			Color color = getBackground();
			if (par != null) {
				color = ((ActiveCellOrder)par).color;
			}
			activeLabel.setBackground(color);
//			Log.log(10, "(StatusBar) -- setting ACTIVE-CELL color = " + color.getRGB());
			break;
			
        }
      } catch ( Exception e ) { 
    	  e.printStackTrace(); 
      }
   }

   /** Shows a message display and starts a new removal task.
    * (This code runs on the EDT!)
    * 
    * @param order MessageOrder message display information,
    *         may be null for clearing the text-field 
    */
   private void startTextMessage (MessageOrder order) {
      Log.log(10, "(StatusBar.startTextMessage) -- (EDT) starting msg display, order=" + order);
   
      // if duration is set to zero, we only clear the text field 
      if (order == null || order.getDuration() == 0) {
         textField.setText(null);
      }
      
      // .. else put the message text to display and schedule REMOVER task
      else {
         // show the text
         String text = order.getText();
         Color textColor = order.getTextColor() == null ? defaultTextColor 
               : order.getTextColor();
         textField.setForeground(textColor);
         textField.setText(text);
      }
   }
}

/** The REMOVER TASK clears the message text field and then activates the
 * next available message in the message queue to display.
 */
private class RemoverTask extends TimerTask {
   private int target; 

   /** Creates a new remover task with target argument.
    * 
    * @param target int 0 = message field. 1 = activity-cell field
    */
   RemoverTask (int target) {
	   this.target = target;
   }
	
   @Override
   public void run () {
	   switch (target) {
	   case 0:	clearMessage(); break;
	   case 1:	clearActiveCell(); break;	
	   }
   }
   
   private void clearActiveCell() {
       // .. start a clear operation for the active-cell field
       startOperation( OperationType.activeCell, null );
   }
   
   private void clearMessage () {
      // look into msg queue and activate next message for display
      MessageOrder order = msgQueue.poll();
      if (order != null) {
         // if there are further waiting messages in queue
         // correct the DURATION value of msg-order to minimum duration  
         if (!msgQueue.isEmpty()) {
            order.duration = Math.min(order.getDuration(), minTextDisplayTime);
         }
         
         // start to display next message order
         Log.log(10, "(StatusBar.RemoverTask) -- scheduling message display (QUEUE-POLL), text="
               .concat(order.getText()));
         startOperation(OperationType.message, order);

      // if there is no msg-order in the queue   
      } else {
         // .. start a clear operation on the message text-field
         startOperation( OperationType.message, null );
      }
   }
}


}
