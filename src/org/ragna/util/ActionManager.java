/*
*  File: ActionManager.java
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

package org.ragna.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.ragna.core.Global;
import org.ragna.front.util.ResourceLoader;

import kse.utilclass.misc.Log;
import kse.utilclass.misc.Util;

public abstract class ActionManager {
   
   public static final String ACTION_KEY_PROPERTY = "ACTION_KEY";
   public static final String MULTIRUN_PROPERTY = "MULTI_RUN";
   public static final long MAX_WORKER_OPERATION_TIME = 180000;
   private static final int THREAD_QUEUE_CAPACITY = 50;
   private static final int THREAD_WATCH_PERIOD = 10000;
   private static final boolean USE_WATCH_DOG = true;

   // *** MEMBER VARIABLES  ***
   
   /**  */
   private Hashtable<String, Action>  actionMap 
           = new Hashtable<String, Action>();

   private ThreadHaven threadHaven;
   private Executor executor = new TaskExecutor();
   private ResourceLoader resource = Global.res;
   

   /** Creates a new ActionManager with a number of worker threads as given
    * by the system options, running with priority equal to the caller.
    */
   public ActionManager () {
	  // create nr of workers as given in system options
	  int wrk = Global.getOptions().getIntOption("workerThreads");
	  wrk = Math.min(5, Math.max(1, wrk));
      init(wrk, Thread.currentThread().getPriority());
   }
   
   private void init (int workers, int priority) {
      threadHaven = new ThreadHaven(workers, priority);
      System.out.println("# ActionManager init with " + workers + " worker threads");
   }

   public void exit () {
	  threadHaven.closeAndAwaitTermination();
      threadHaven.exit();
      System.out.println("# ActionManager exited");
   }
   
   public Executor getExecutor () {
	   return executor;
   }

   /** Whether manager owned threads are currently performing tasks.
    * 
    * @return boolean true = operating, false = not operating
    */
   public boolean isOperating () {
	   return threadHaven.isOperating();
   }
   
   public abstract Timer getTimer ();
   
   /** Returns the <code>ActionListener</code> which executes the command codes
    * defined on this handler's actions. Execution performs synchronously.
    * 
    * @return <code>ActionListener</code>
    */
   public abstract ActionListener getActionListener ();
   
   /** Informs a subclass that an over-aged task in worker-thread execution
    * exists and a decision is required whether to forcibly terminate this
    * execution or continue waiting for another wait period 
    * (MAX_WORKER_OPERATION_TIME).
    *  
    * @param action Action
    * @return boolean true = terminate thread, false = continue waiting 
    */
   public abstract boolean overagedTask (Action action);

   /** Returns an action from the action handler's registered action list.
    * 
    * @param key String name of the action
    * @param asynchron boolean if <b>true</b> the action will perform on a separate 
    *        thread, synchronous with executing thread otherwise
    * @return <code>Action</code>
    * @throws NullPointerException if key is <b>null</b>
    * @throws UnknownActionException if action key is unknown in this manager
    */
   public Action getAction (String key, boolean asynchron) throws UnknownActionException {
      Action sAction = retrieveAction(key);
      if (sAction == null) {
         throw new UnknownActionException( "unknown action KEY: ".concat(key) );
      }
      if (asynchron) {
         AsynchronAction aAction = new AsynchronAction(sAction);
         return aAction;
      }
      return sAction;
   }

   /** Returns a synchronously performing action from the action handler's 
    * registered action list.
    * 
    * @param key String name of the action
    * @return <code>Action</code>
    * @throws NullPointerException if key is <b>null</b>
    * @throws UnknownActionException if action key is unknown in this manager
    */
   public Action getAction ( String key ) throws UnknownActionException {
      return getAction(key, false);
   }

   /** Returns a list of (synchronously performing) actions which are identified
    * through the given comma-separated list of action names. The list is 
    * created new with each call of this method and can be modified.
    * 
    * @param actions String list of action names
    * @return {@code List<Action>}
    */
   public List<Action> getActionList (String actions) {
 	   Objects.requireNonNull(actions);
	   List<Action> list = new ArrayList<>();
       String[] values = actions.split(",");
       for (String name : values) {
          if (!name.isEmpty()) {
        	  try {
             	 list.add(getAction(name));
        	  } catch (UnknownActionException e) {
        	  }
          }
       }
       return list;
   }
   
   /** Whether the given action KEY is registered in this action manager.
    *  
    * @param key String action name
    * @return boolean <b>true</b> if key has a mapping in this manager
    */
   public boolean hasAction ( String key ) {
      return key != null && actionMap.containsKey(key);
   }
   
   /** Renders the registered <code>Action</code> for the given key 
    * or <b>null</b> if the request has no mapping.
    * 
    * @param key String name of the action
    * @return <code>Action</code>
    * @throws NullPointerException if key is <b>null</b>
    */
   private Action retrieveAction (String key) {
      return actionMap.get(key);
   }

   /** Creates a new HandlerAction. Parameters 'key' and 'title' are
    * mandatory. The returned action performs synchronous. 
    *  
    * @param key String action technical name
    * @param title String action display title
    * @param command String action execution command (optional, may be null)
    * @return <code>SynchronAction</code>
    * @throws IllegalArgumentException
    */
   private HandlerAction createAction (String key, String title, String command) {
      // define action command after KEY if not specified
      SynchronAction action = new SynchronAction(key, title, command);
      return action;
   }
   
   /** Defines values of an Action beside of 'key', 'title' and 'command'.
    * 
    * @param key String 
    * @param action Action
    */
   private void defaultDefineAction (String key, Action action) {
      // insert the KEY property into the action
      action.putValue(ACTION_KEY_PROPERTY, key);
      
      // add icon and tooltip from program resources (if available)
      Icon icon = resource.getImageIcon(key.concat(".icon"));
      if (icon != null) {
         action.putValue(Action.SMALL_ICON, icon);
      }
      String tooltip = resource.getCommand(key.concat(".tooltip"));
      if (tooltip != null && !tooltip.equals("FIXME")) {
         action.putValue(Action.SHORT_DESCRIPTION, tooltip);
      }
   }
   
   /**
    * Adds an Action definition to this action manager.
    * The mandatory parameters must not be empty. The created action runs
    * synchronous.
    * 
    * @param key String identity of this action in the manager 
    * @param title String action title used for display purposes 
    * @param command String action command ID 
    *        (may be null for default value == key)
    * @param multiRun boolean whether action can run parallel in multiple threads       
    * @return <code>Action</code>
    * @throws DuplicateException if KEY is already registered in this manager
    * @throws IllegalArgumentException
    */
   public Action addAction(  String key, String title, String command, 
                             boolean multiRun ) throws DuplicateException {
      if (actionMap.containsKey(key)) 
          throw new DuplicateException();
      
      HandlerAction action = createAction(key, title, command);
      action.setMultiRun(multiRun);
      actionMap.put(key, action);
      return action;
   }
   
//   /**
//    * Adds an Action definition to this action manager and returns it.
//    * Parameters must not be <b>null</b> or empty.
//    * Command, tooltip and icon values of the action are pre-loaded by
//    * default derivation or fetching values from global program resources.
//    * The action is not 'multi-run' entitled. 
//    * 
//    * @param key String identity of new action in the manager 
//    * @param title String title used for display purposes 
//    * @param multiRun boolean whether action can run parallel in multiple 
//    *        threads       
//    * @return <code>Action</code>
//    * @throws DuplicateException if KEY is already registered in this manager
//    * @throws IllegalArgumentException
//    */
//   public Action addAction(  String key, String title, boolean multiRun ) 
//         throws DuplicateException {
//      return addAction(key, title, null, multiRun);
//   }
   
   /**
    * Adds an Action definition to this action manager and returns it.
    * Key argument must not be <b>null</b> or empty.
    * Title, command-key, tooltip and icon values of the action are 
    * automatically derived or fetched from global resources (resource bundles).
    * The action can be set to be 'multi-run' enabled. 
    * 
    * @param key String identity of new action in the manager 
    * @param multiRun boolean whether action can run parallel in multiple 
    *        threads
    * @return <code>Action</code>
    * @throws DuplicateException if KEY is already registered in this manager
    * @throws IllegalArgumentException
    */
   public Action addAction(  String key, boolean multiRun ) throws DuplicateException {
	  String title = ResourceLoader.get().getCommand("action.".concat(key));
      return addAction(key, title, null, multiRun);
   }
   
   /**
    * Adds an Action definition to this action manager and returns it.
    * Argument must not be <b>null</b> or empty.
    * Title, command-key, tooltip and icon values of the action are 
    * automatically derived or fetched from global resources (resource bundles).
    * The action is not 'multi-run' entitled. 
    * 
    * @param key String identity of new action in the manager 
    * @param title String title used for display purposes 
    * @return <code>Action</code>
    * @throws DuplicateException if KEY is already registered in this manager
    * @throws IllegalArgumentException
    */
   public Action addAction( String key ) throws DuplicateException {
      return addAction(key, false);
   }
   
   /** Adds a user defined Action into the register of this manager.
    * If an action was already contained for the given KEY, an exception
    * is thrown.
    * <p>The key value is additionally stored in the action under access-key
    * "ACTION_KEY_PROPERTY".
    *  
    * @param key String identity of this action in the manager 
    * @param action Action user action
    * @return Action the previous registered action or null 
    * @throws DuplicateException if key is already defined
    * @throws IllegalArgumentException if key is null or empty
    */
   public Action addAction ( String key, Action action ) throws DuplicateException {
      // check validity
	  Objects.requireNonNull(action, "action is null");
      if ( key == null || key.isEmpty() ) 
         throw new IllegalArgumentException("invalid KEY argument");
      if ( actionMap.containsKey(key) ) 
         throw new DuplicateException();
      
      // insert the KEY property into the action
      action.putValue(ACTION_KEY_PROPERTY, key);

      // register action 
      return actionMap.put(key, action);
   }
   
   /**
    * Puts an action definition into this action manager.
    * If an action was already contained for the given KEY,
    * it is replaced by the definition given by the parameters. 
    * The action is not 'multi-run' entitled. 
    * Parameters must not be <b>null</b> or empty.
    * 
    * @param key String identity of this action in the manager 
    * @param title String action title used for display purposes 
    * @param command String action command identity (for action listener)
    * @return Action the previous registered action or null 
    * @throws IllegalArgumentException
    */
   public Action putAction(  String key, String title, String command ) {
      Action action = createAction(key, title, command);
      Action prev = actionMap.put(key, action);
      return prev;
   }
   
   /**
    * Puts an action definition into this action manager, including an option
    * for the 'multi-run' quality of the action.
    * If an action was already contained for the given KEY, it is replaced 
    * by the definition given by the parameters. Text parameters must not be 
    * <b>null</b> or empty.
    * 
    * @param key String identity of this action in the manager 
    * @param title String action title used for display purposes 
    * @param command String action command identity (for action listener)
    * @param multiRun boolean whether action can run parallel in multi threads       
    * @return Action the previous registered action or null 
    * @throws IllegalArgumentException
    */
   public Action putAction(  String key, String title, String command,
                             boolean multiRun ) { 
      Action action = putAction(key, title, command);
      if ( multiRun ) {
         action.putValue(MULTIRUN_PROPERTY, "true");
      }
      return action;
   }
   
   /** Puts a user defined Action into register of this manager.
    *  The action can be enabled for 'multi-run' by assigning a property
    *  <code>ActionManager.MULTIRUN_PROPERTY == true</code> via 
    *  <code>action.setClientProperty()</code>. 
    *  
    * @param key String identity of this action in the manager 
    * @param action Action user action
    * @return Action the previous registered action or null 
    */
   public Action putAction ( String key, Action action ) {
      // check validity
      if ( key == null || key.isEmpty() ) 
         throw new IllegalArgumentException("invalid KEY argument");
      if ( action == null ) 
         throw new IllegalArgumentException("action is null");
      
      // insert the KEY property into the action
      action.putValue(ACTION_KEY_PROPERTY, key);

      // register action (replaces 
      return actionMap.put(key, action);
   }
   
   /** Synchronously executes the given registered action 
    * (on the calling thread). The 'source' parameter will appear
    * in the action event for the associated <code>ActionListener</code>.
    * 
    * @param action String action KEY
    * @param source Object the source object to identify the caller
    *               (may be null)
    * @throws NullPointerException if <code>action</code> is <b>null</b>
    * @throws UnknownActionException if action is unknown in this manager
    */
   public void performAction ( String action, Object source ) 
         throws UnknownActionException {
      Action act = getAction( action );
      if (source == null) {
         source = this;
      }
      ActionEvent evt = new ActionEvent( source, 0, (String)act.getValue(
            Action.ACTION_COMMAND_KEY) );
      act.actionPerformed(evt);
   }
   
   /** Synchronously executes the given registered action 
    * (on the calling thread).
    * 
    * @param action String action KEY
    * @throws NullPointerException if <code>action</code> is <b>null</b>
    * @throws UnknownActionException if action is unknown in this manager
    */
   public void performAction ( String action ) throws UnknownActionException {
      performAction(action, this);
   }

   /** Causes the calling thread to wait until no more operations are ongoing
    * in the Worker pool. This also waits for queued tasks.
    */
   public void wait_for_silence () {
	   while (threadHaven.isOperating()) {
		   Util.sleep(20);
	   }
   }
   
   /** Starts the given registered action in a new separate thread after a 
    * given delay time.
    *  
    * @param action String action KEY
    * @param delay int milliseconds of delay to the start of execution
    * @throws NullPointerException if <code>action</code> is <b>null</b>
    * @throws UnknownActionException if action is unknown in this manager
    */
   public TimedThread startActionDelayed ( final String action, int delay ) 
         throws UnknownActionException {
      getAction( action );
      Runnable runner = new Runnable() {

         @Override
         public void run() {
            try {
               performAction( action, null );
            } catch (UnknownActionException e) {
               e.printStackTrace();
            }
         }
         
      };
      return startTaskDelayed( runner, delay );
   }
   
   /** Starts the given registered action in a new separate thread 
    * (outside of the manager's worker thread array).
    *  
    * @param key String action name (technical)
    * @param join boolean if <b>true</b> the calling thread waits for the
    *        action to terminate
    * @return <code>Thread</code> the started thread       
    * @throws NullPointerException if <code>action</code> is <b>null</b>
    * @throws UnknownActionException if action is unknown in this manager
    */
   public Thread startAction ( final String key, int priority, boolean join ) 
         throws UnknownActionException {
      
      getAction( key );
      Thread thread = new Thread( "AMan Started Action: ".concat(key) ) {

         @Override
         public void run() {
            try {
               performAction( key, null );
            } catch (UnknownActionException e) {
               e.printStackTrace();
            }
         }
         
      };
      thread.setPriority(priority);
      thread.start();
      if ( join ) {
         try { thread.join(); } 
         catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
      return thread;
   }
   
   /** Schedules the named registered action for asynchronous execution in 
    * this manager's worker thread array. 
    * The return value indicates whether the action could be scheduled,
    * which may fail in case the internal queue capacity is reached.
    *  
    * @param key String action name (technical)
    * @return boolean true == success, false == failure
    * @throws NullPointerException if <code>action</code> is <b>null</b>
    * @throws UnknownActionException if action is unknown in this manager
    */
   public boolean scheduleAction ( String key ) 
         throws UnknownActionException {
      
      Action action = getAction( key );
      return scheduleAction(action, null);
   }
   
   /** Schedules the given action for asynchronous execution in 
    * this manager's worker thread array. 
    * The return value indicates whether the action could be scheduled,
    * which may fail in case the internal queue capacity is reached.
    * The given action may or may not hold a property <code>ActionManager.
    * ACTION_KEY_PROPERTY</code>, containing the action's name for protocol
    * purposes.
    *  
    * @param action Action action
    * @return boolean true == success, false == failure
    * @throws NullPointerException if <code>action</code> is <b>null</b>
    * @throws UnknownActionException if action is unknown in this manager
    */
   public boolean scheduleAction ( Action action, ActionEvent event ) {
      boolean ok = threadHaven.scheduleAction(action, event);
      return ok;
   }

   /** Schedules the given action for asynchronous execution in 
    * this manager's worker thread array with an optional start delay. 
    * There is no guarantee that the given action can actually be started
    * in the thread array. If this is not the case after the delay
    * time, an IllegalStateException is thrown in the timer's thread. 
    * The given action may or may not hold a property <code>ActionManager.
    * ACTION_KEY_PROPERTY</code>, containing the action's name for protocol
    * purposes.
    *  
    * @param action Action action
    * @param event ActionEvent to be seen by action
    * @param delay in delay before start of the action (milliseconds)
    * @return boolean true == success, false == failure
    * @throws NullPointerException if <code>action</code> is <b>null</b>
    * @throws UnknownActionException if action is unknown in this manager
    */
   public void scheduleActionDelayed ( final Action action, 
                                         final ActionEvent event, 
                                         int delay ) {
      TimerTask task = new TimerTask() {
         @Override
         public void run () {
            boolean ok = threadHaven.scheduleAction(action, event);
            if (!ok) {
               throw new IllegalStateException("unable to schedule ACTION in THREADHAVEN"
                     + action.getValue(ACTION_KEY_PROPERTY));
            }
         }
      };
      getTimer().schedule(task, delay);
   }

   /** Schedules the given Runnable to run on the Event-Dispatching-Thread
    * either by waiting for its performance or for later performance.
    * 
    * @param runnable <code>Runnable</code>
    * @param join boolean if true the calling thread wait until the Runnable
    *         has been executed
    * @throws InterruptedException if we are interrupted while waiting for 
    *         the Runnable to complete
    * 
    */
   public static void runOnEDT (Runnable runnable, boolean join) 
         throws InterruptedException, InvocationTargetException {
      if (SwingUtilities.isEventDispatchThread()) {
         runnable.run();
      } else {
         if (join) {
            SwingUtilities.invokeAndWait(runnable);
         } else {
            SwingUtilities.invokeLater(runnable);
         }
      }
   }
   
   /** Schedules the given Runnable to run on the Event-Dispatching-Thread
    * for later performance. The calling thread returns immediately.
    * 
    * @param runnable <code>Runnable</code>
    */
   public static void runOnEDT (Runnable runnable) { 
      if (SwingUtilities.isEventDispatchThread()) {
         runnable.run();
      } else {
         SwingUtilities.invokeLater(runnable);
      }
   }
   
   /**
    * Returns an <code>Action</code> to start the operating system's
    * standard email client with the given address parameter.
    * 
    * @param address String the email address to start writing to 
    * @originalName boolean if <b>true</b> the first 50 char of the address are 
    *               adopted as name of the action
    * @return <code>Action</code>
    * @since 0-6-0
   public static Action getStartEmailAction ( final String address, boolean originalName )
   {
      String name = originalName ? address : 
                    ResourceLoader.getCommand( "menu.edit.startmail" );
      if ( name.length() > 50 )
         name = name.substring( 0, 50 );
   
      Action action = new AbstractAction( name )
      {
         public void actionPerformed ( ActionEvent e )
         {
            Global.startEmail( address );
         }
      };
      return action;
   }

   /**
    * Returns an <code>Action</code> to start the application's Internet
    * browser with the given URL parameter.
    * 
    * @param url URL the net address to start browsing
    * @originalName boolean if <b>true</b> the first 50 char or the URL are 
    *               taken as name of the action
    * @return <code>Action</code>
    * @since 0-6-0
   @SuppressWarnings("serial")
   public static Action getStartBrowserAction( final URL url, boolean originalName )
   {
      String name = originalName ? url.toString() : 
                    ResourceLoader.getCommand( "menu.edit.starturl" );
      if ( name.length() > 50 )
         name = name.substring( 0, 50 );
      
      Action action = new AbstractAction( name )
      {
         public void actionPerformed ( ActionEvent e )
         {
            Global.startBrowser( url ); 
         }
      };
      return action;
   }
    */

   /**
    * Schedules any <code>Runnable</code> for asynchronous execution in this 
    * manager's worker thread array. The actual start of the task depends on 
    * availability of a free worker and may be delayed unpredictably. 
    * The return value indicates whether the action could be scheduled,
    * which may fail in case the internal queue capacity is reached.
    * 
    * @param run <code>Runnable</code> task to be performed 
    * @param name String name of the task (for protocol) 
    * @return boolean true == success, false == failure
    */
   public void scheduleWorkerTask ( final Runnable run, String name ) {
      Action action = new AbstractAction(name) {
         @Override
         public void actionPerformed (ActionEvent e) {
            run.run();
         }
      };
      action.putValue(ACTION_KEY_PROPERTY, name);
      scheduleAction(action, null);
   }
   
   /**
    * Schedules any <code>Runnable</code> for asynchronous execution in this 
    * manager's worker thread array with an optional start delay. The actual 
    * start of the task depends on availability of a free worker and may be 
    * delayed unpredictably or entirely impossible. 
    * 
    * @param run <code>Runnable</code> task to be performed 
    * @param name String name of the task (for protocol) 
    * @param delay int guaranteed delay before start of the task (milliseconds)
    * @return boolean true == success, false == failure
    */
   public void scheduleWorkerTaskDelayed ( final Runnable run, String name,
                                             int delay )
   {
      Action action = new AbstractAction(name) {
         @Override
         public void actionPerformed (ActionEvent e) {
            run.run();
         }
      };
      action.putValue(ACTION_KEY_PROPERTY, name);
      scheduleActionDelayed(action, null, delay);
   }
   
   /**
    * Starts any <code>Runnable</code> in a new separate thread.
    * 
    * @param run <code>Runnable</code> task to be performed 
    * @param name String name of the task (for protocol) 
    * @return Thread the action thread which is active (or has run)
    */
   public static Thread startTask ( Runnable run, String name )
   {
      Thread thread;
      
      if (name == null) {
         name = "noname"; 
      }
      thread = new Thread( run, "Free Action Task: ".concat(name) );
      thread.start();
      return thread;
   }
   
   /**
    * Start any Runnable in a new separate thread after a wait time (running
    * with the priority of the calling thread).
    * <p><small>The returned thread can be canceled before running or caused to run
    * immediately before its scheduled start time. Furthermore, it can be detected
    * whether it has run already.</small>
    * 
    * @param run Runnable action 
    * @param delay int wait time in milliseconds from now
    * @return <code>TimedThread</code> the action thread which is awaiting activation
    */
   public static TimedThread startTaskDelayed ( Runnable run, int delay )
   {
      return new TimedThread( run, delay );
   }
   
   /**
    * Start any Runnable in a new separate thread after a wait time and with a 
    * specific thread priority.
    * <p><small>The returned thread can be canceled before running or caused to run
    * immediately before its scheduled start time. Furthermore, it can be detected
    * whether it has run already.</small>
    * 
    * @param run Runnable action 
    * @param delay int wait time in milliseconds from now
    * @param prio int thread priority
    * @return <code>TimedThread</code> the action thread which is awaiting activation
    */
   public static TimedThread startTaskDelayed ( Runnable run, int delay, int prio )
   {
      TimedThread t = new TimedThread( run, delay );
      t.setPriority(prio);
      return t;
   }
   
   //  ***********   INNER CLASSES   ***************
   
   public interface HandlerAction extends Action {

      /** Returns the string KEY with which this action is registered and
       * retrievable at the <code>ActionHandler</code> service instance.
       * 
       * @return String action register key
       */
      String getKey ();
      
      /** Whether this action is allowed to run in multiple parallel 
       * performance instances.
       * 
       * @return boolean true == multi run allowed
       */
      boolean isMultiRun ();
      
      /** Sets whether this action is allowed to run in multiple parallel 
       * performance instances.
       * 
       * @param multiRun boolean true == multi run allowed
       */
      void setMultiRun (boolean multiRun);
      
      /** Whether this action is currently running. A <code>HandlerAction</code>
       * is running if any thread has entered its "actionPerformed()" method
       * and is currently executing on the code defined for this action. 
       * If no thread has entered "actionPerformed()" or all threads which 
       * have entered have already left it, this action is NOT running. 
       * 
       * @return boolean true == action is currently executed
       */
      boolean isRunning ();
      
//      /** Executes this action. */
//      public void execute (Object source);
   }
   
   private class ThreadHaven {
      
      /** The task queue consisting of Handler actions, scheduled by handler. */
      BlockingQueue<ActionOrder> queue 
              = new LinkedBlockingQueue<ActionOrder>(THREAD_QUEUE_CAPACITY); 
      
      /** Task executing worker threads, always alive. */
      WorkerThread[] threads;

      AtomicInteger taskCounter = new AtomicInteger();
      
      /** whether new tasks can be accepted */ 
      boolean tasksOpen;
      
      /**
       * Creates a new ThreadHaven with the given amount of executing threads
       * which will run with the given thread priority.
       * 
       * @param workers int number of task executing, separate threads
       * @param priority int <code>Thread</code> priority for worker threads
       */
      public ThreadHaven (int workers, int priority) {
         if (workers < 1)
            throw new IllegalArgumentException("minimum is 1 worker!");
         threads = new WorkerThread[workers];
         
         for (int i = 0; i < workers; i++) {
            new WorkerThread(i, priority);
         }
         tasksOpen = true;
         
         if (USE_WATCH_DOG) {
        	 getTimer().schedule(new WatchDog(), THREAD_WATCH_PERIOD, THREAD_WATCH_PERIOD);
             Log.log(8, "(ActionManager) started ThreadHaven Watch-Dog");
         }
         Log.log(8, "(ActionManager) ThreadHaven started with " + workers + " worker threads");
      }
      
      /** Schedules the given action to run in 
       * this ActionManager's worker thread array. Caller returns immediately.
       * The return value indicates whether the action could be scheduled,
       * which may fail in case the internal queue capacity is reached.
       * The given action may or may not hold a property <code>ActionManager.
       * ACTION_KEY_PROPERTY</code>, containing the action's name. 
       * 
       * @param action <code>HandlerAction</code> (may be null)
       * @param event <code>ActionEvent</code> (may be null)
       * @return boolean true == success, false == failure
       */
      public boolean scheduleAction (Action action, ActionEvent event) {
         if (action == null || !tasksOpen) return false;
         
         // identify an action-key
         String actionKey = (String)action.getValue(ACTION_KEY_PROPERTY);
         if (actionKey == null) {
            actionKey = (String)action.getValue(Action.ACTION_COMMAND_KEY);
         }
         if (actionKey == null) {
             actionKey = "-noname-";
          }

         // create an action-order and offer to execute
         ActionOrder actionOrder = new ActionOrder(action, event);
         boolean ok = queue.offer(actionOrder);
         Log.log(8, "(ThreadHaven) scheduling HandlerAction for execution: "
               .concat(actionKey));
         return ok;
      }
      
      /** Terminates activity and resources of this ThreadHaven.
       * Any currently ongoing task executions will be interrupted (thread 
       * status) and possibly terminate unfinished. Worker threads may still
       * be alive and operating when this method returns. 
       * <p>After a call to this method no further actions can
       * be scheduled. Actions already scheduled but still waiting for
       * execution will never execute.
       */
      public void exit () {
    	 // TODO operation status? run to end or interrupt?
    	 tasksOpen = false;
    	 queue.clear();
         for (WorkerThread wrk : threads) {
            if (wrk != null) {
               wrk.terminate();
            }
         }
      }
      
      /** Closes task input to this thread-haven and waits until all outstanding
       * tasks have been executed naturally.
       */
      public void closeAndAwaitTermination () {
     	 tasksOpen = false;
    	 while (isOperating()) {
    		 Util.sleep(15);
    	 }
      }
      
      /** Forcefully terminates the given Worker thread and initiates a new
       * instance for it.
       * 
       * @param tid int worker thread ID
       */
      public void restartWorker (int tid) {
  		  Log.log(8, "(ActionManager.WorkerThread) restarting Worker-Thread " + tid);
    	  WorkerThread wrk = threads[tid];
    	  if (wrk != null) {
    		  wrk.terminateByForce();
    	  }
		  new WorkerThread(tid, wrk.getPriority());
      }

      /** Whether this thread-haven has ongoing tasks.
       * 
       * @return boolean true = has ongoing tasks
       */
      public boolean isOperating () {
    	  boolean alive = false;
    	  for (WorkerThread thd : threads) {
    		  if (thd != null && thd.isAlive()) {
    			  alive = true;
    			  if (thd.isWorking()) return true;
    		  }
    	  }
    	  return alive && !queue.isEmpty();
      }
      
      /** A Thread to execute any HandlerAction scheduled for execution
       * at the enclosing ThreadHaven. WorkerThread fetches tasks randomly
       * from the haven's singular task (action) queue. 
       */
      private class WorkerThread extends Thread {
    	 private ActionOrder actionOrder;
         private String baseName;
         private int threadID;
         private int taskNr;
         private long startTime;
         private boolean terminate;
         private boolean working;
         
         public WorkerThread ( int id, int priority ) {
            super();
            baseName = "WorkerTHD " + id + ", action=";
            threadID = id;
            setName("WorkerTHD " + id);
            setPriority(priority);

            // install this thread in worker-threads array
            if ( threads[id] != null ) {
               threads[id].terminate();
            }
            threads[id] = this;
            
            // start this thread
            start();
            Log.log(8, "(ThreadHaven.WorkerThread) started new WORKER thread == "
                  .concat(String.valueOf(threadID)));
         }

         @Override
         public void run () {
            while ( !terminate ) {
               try {
                  // fetch next action from task queue
                  actionOrder = queue.take();
                  taskNr = taskCounter.incrementAndGet();
                  working = true;
                  Action action = actionOrder.action;
                  String actionKey = (String)action.getValue(ACTION_KEY_PROPERTY);
                  setName(baseName.concat(actionKey));
                  if (Global.isDebug()) {
                	  System.err.println("**** WORKER started executing TASK (" 
                			  + taskNr + "): ".concat(getName()));
                  }
   
                  try {
                     // execute action
                     ActionEvent event = actionOrder.event;
                     if (event == null) {
                         event =  new ActionEvent( this, 0, 
                         (String)action.getValue(Action.ACTION_COMMAND_KEY) );
                     }
                     startTime = System.currentTimeMillis();
                     action.actionPerformed(event);
                     
                     // after task execution
                     if (Global.isDebug()) {
                    	 System.err.println("**** WORKER-TASK " + taskNr + " finished regular: ".concat(getName()) 
                    			 + ", time = " + workTime() + " ms");
                     }
                     
                  } catch (Throwable e) {
                     System.err.println("**** WORKER-TASK " + taskNr + " termination with THROWABLE: " + getName() + "\n");
                     e.printStackTrace();
                  }
                  
               } catch (InterruptedException e) {
            	  if (working) {
            		  Log.debug(8, "(ThreadHaven.WorkerThread) WORKER-TASK " + taskNr + " interrupted: ".concat(getName()));
            		  interrupted();  
            	  }
            	  
               } finally {
                   working = false;
                   startTime = 0;
                   setName("WorkerTHD " + threadID);
               }
            } // while
            
            Log.log(8, "(ThreadHaven.WorkerThread) death of WORKER thread ".concat(getName()));
         } // run
         
         public int threadID () {return threadID;}
         
//         public int taskID () {return taskNr;}
 
         /** Whether this thread is currently working on a scheduled task.
          * (This returns false if the thread is just waiting for the next
          * task to appear.) 
          * 
          * @return boolean true == working on task
          */
         public boolean isWorking () {return working;}
         
         /** The time in milliseconds this thread has spent on its current task.
          * Returns zero if there is no task currently in work.
          * 
          * @return long time in milliseconds
          */
         public long workTime () {
        	 return startTime == 0 ? 0 : System.currentTimeMillis() - startTime; 
         }

         /** Terminates this worker thread. If this thread is currently 
          * executing on a task, the task will run to its end before
          * the thread stops.
          */
         public void terminate () {
            if (!terminate && isAlive()) {
               Log.log(8, "(ThreadHaven.WorkerThread) terminating (interrupt) WORKER thread "
                     .concat(String.valueOf(threadID)));
               terminate = true;
               interrupt();
            }
         }

         /** Terminates this worker thread and waits until any ongoing task
          * execution of this thread has come to a natural end. This thread 
          * will not look for new tasks again. 
          * 
          * @throws InterruptedException 
          */
         @SuppressWarnings("unused")
		 public void terminateAndWait () throws InterruptedException {
            if (!terminate && isAlive()) {
               Log.log(8, "(ThreadHaven.WorkerThread) terminating (wait-for-end) WORKER thread "
                     .concat(String.valueOf(threadID)));
               terminate = true;
               if (!working) {
            	   interrupt();
               }
               join();
            }
         }
         
         public void terminateByForce () {
        	 try {
        		 this.stop();
        	 } catch (ThreadDeath e) {
        	 }
         }
      }

      private class ActionOrder {
         Action action;
         ActionEvent event;
         
         public ActionOrder (Action action, ActionEvent event) {
            if (action == null)
               throw new IllegalArgumentException("action is null");
      
            this.action = action;
            this.event = event;

            // supply a dummy action-key if there is none present
            String actionKey = (String)action.getValue(ACTION_KEY_PROPERTY);
            if (actionKey == null) {
               action.putValue(ACTION_KEY_PROPERTY, "?");
            }
         }
      }

      private class WatchDog extends TimerTask {
    	  private long minAlarmTime;
    	  
   	   	@Override
   	   	public void run() {
   	   	   // respect minimum target time for alarm
   	   	   if (System.currentTimeMillis() < minAlarmTime) return;
       	   	   
   	   	   // check thread-haven is alive, otherwise cancel this task
   	   	   if (threads == null || Util.isNullArray(threads)) {
   	   		   cancel();
   	   		   Log.log(8, "(ActionManager.WatchDog) cancelled watchdog task (no threads)");
   	   		   return;
   	   	   }

   		   // control all Workers for extended operation time
//   		   Log.log(8, "(ActionManager.WatchDog) watching worker threads)");
   		   for (WorkerThread wrk : threads) {
   			   if (wrk != null && (!wrk.isAlive() || wrk.workTime() > MAX_WORKER_OPERATION_TIME)) {
   				  wrk.suspend();
   				  if (overagedTask(wrk.actionOrder.action)) {
	   	    		  wrk.terminateByForce();
	   	    		  if (tasksOpen) {
	   	    			  restartWorker(wrk.threadID());
	   	    		  }
   	    		  } else {
   	    			  minAlarmTime = System.currentTimeMillis() + MAX_WORKER_OPERATION_TIME;
   	    			  wrk.resume();
   				  }
   			   } 
   		   }
   	   	}
      }
   }  // ThreadHaven
   
   private Map<Action, String> actionStates = new Hashtable<Action, String>();
   
   /** AsynchronAction is a <code>HandlerAction</code> that takes a synchronous 
    * operating <code>SynchronAction</code> as argument and performs its action
    * in a separate thread.
    * <p>At creation time, the caller can decide whether this action thread is
    * performing as exclusive action or multi-performing action.
    */
   protected class AsynchronAction implements HandlerAction {
      private Action synchronAction;
      private String key;
      
      public AsynchronAction (Action action) {
         if ( action == null ) 
            throw new NullPointerException();
         synchronAction = action;
         key = (String)action.getValue(ACTION_KEY_PROPERTY);
      }

      @Override
	public String getKey () {
         return key;
      }

      @Override
      public void actionPerformed( ActionEvent e ) {

         // start the asynchronous action thread
         scheduleAction(synchronAction, e);
      }

      @Override
      public Object getValue(String key) {
         return synchronAction.getValue(key);
      }

      @Override
      public void putValue(String key, Object value) {
         synchronAction.putValue(key, value);
      }

      @Override
      public void setEnabled(boolean b) {
         synchronAction.setEnabled(b);
      }

      @Override
      public boolean isEnabled() {
         return synchronAction.isEnabled();
      }

      @Override
      public void addPropertyChangeListener(PropertyChangeListener listener) {
         synchronAction.addPropertyChangeListener(listener);
      }

      @Override
      public void removePropertyChangeListener(PropertyChangeListener listener) {
         synchronAction.removePropertyChangeListener(listener);
      }

      @Override
      public int hashCode() {
         return synchronAction.hashCode();
      }

      @Override
      public boolean equals(Object obj) {
         return synchronAction.equals(obj);
      }

      @Override
      public boolean isMultiRun () {
         // TODO Auto-generated method stub
         return false;
      }

      @Override
      public void setMultiRun (boolean multiRun) {
         // TODO Auto-generated method stub
         
      }

      @Override
      public boolean isRunning () {
         // TODO Auto-generated method stub
         return false;
      }

//      @Override
//      public void execute (Object source) {
//         synchronAction.execute(source);
//      }
   }  // class AsynchronAction

   
   /** Flexible Action definition device for the ActionManager.
    * Actions can be defined either by name i.e. COMMAND-KEY, which is at
    * execution time later de-referenced by the <b>central ActionListener</b>;
    * or through an individual <code>Action</code> instance given at
    * construction time. The latter supplies execution code by itself and
    * does not run over the central ActionListener. 
    */
   protected class SynchronAction extends AbstractAction  implements HandlerAction {
      private String key;
      private Runnable runnable;
      private boolean multiRun;
      private int runCounter;
      
      
      /** Creates a new SynchronAction with basic settings. The executed
       * action code must be defined at the <b>central ActionListener</b>
       * which is implemented in a subclass as it is defined abstract here. 
       * Parameters are mandatory and must not be empty.
       * 
       * @param key String identity of this action in the manager 
       * @param title String action title for display
       * @param command String action command key (may be null for equal to key)
       * @throws IllegalArgumentException
       */
      public SynchronAction( String key, String title, String command ) {
         super(title);
         
         // check validity
         if ( key == null || key.isEmpty() ) 
            throw new IllegalArgumentException("invalid KEY argument");
         if ( title == null || title.isEmpty() ) 
            throw new IllegalArgumentException("invalid TITLE argument");
         
         this.key = key;
         if (command == null) {
            command = key;
         }
         putValue(ACTION_COMMAND_KEY, command);
         defaultDefineAction(key, this);
      }

      /** Creates a new SynchronAction with a key and a <code>Runnable</code>
       * as executable code.
       * 
       * @param key String identity of this action in the manager
       * @param title String display name of the action 
       * @param run <code>Runnable</code>
       * @throws IllegalArgumentException
       */
      public SynchronAction( String key, String title, Runnable run ) {
         this( key, title, (String)null );
         Objects.requireNonNull(run, "RUN is null");
         runnable = run;
      }
      
      @Override
	  public String getKey () {return key;}
      
      @Override
      public boolean isMultiRun () {return multiRun;}
      
      @Override
      public void setMultiRun (boolean multiRun) {
         this.multiRun = multiRun;
      }
      
      @Override
      public boolean isRunning () {
         return runCounter > 0;
      }
      
      @Override
      public void actionPerformed (ActionEvent e) {
         
         // check whether this action must be broken because it is already running
         if ( !isMultiRun() && isRunning() ) {
            String msg = "*** MULTI-RUN violation for Handler-action: ".concat(key);
            System.err.println(msg);
            return;
         }
         
         try {
            runCounter++;
            if (runnable != null) {
               runnable.run();
            } else {
               getActionListener().actionPerformed( e );
            }
         } finally {
            runCounter--;
         }
      }

      @Override
      public int hashCode () {
         return key.hashCode();
      }

      @Override
      public boolean equals (Object obj) {
         return key.equals(obj);
      }

//      @Override
//      public void execute (Object source) {
//         ActionEvent evt = new ActionEvent( source, 0, (String)getValue(
//               Action.ACTION_COMMAND_KEY) );
//         actionPerformed(evt);
//      }
   }  // class SynchronAction
   
   
   /** Exception to indicate that an attempt was made by the caller to
    * add an Action which was already represented in this action manager.
    */
   public class DuplicateException extends Exception {
      
   }
   /** Exception to indicate that a requested Action type is not currently
    * registered (unknown) in this action manager.
    */
   public class UnknownActionException extends Exception {

      public UnknownActionException(String msg) {
         super( msg );
      }
      
   }

   /**
    *  A subclass of <code>java.lang.Thread</code> that can be scheduled
    *  for future execution.
    *  After the scheduled time is reached this thread will be activated.
    *  The schedule may get terminated before the thread has actually started 
    *  by use of <code>TimedThread.cancel()</code>.
    */
   public static class TimedThread extends Thread {
      private boolean isStarted;
      private TimerTask task;
      
      /**
       * Creates a thread run schedule with the specified delay time. After
       * time has elapsed, this thread will start execution.
       *  
       * @param r <code>Runnable</code> to be run by this thread
       * @param name String (optional) name for this thread; may be <b>null</b>
       * @param delay int, start delay time in milliseconds
       */
      public TimedThread ( final Runnable r, String name, int delay ) {
         super( r, "Free Action (Timed): ".concat( name == null ? "?" : name ) );
         if ( r == null )
            throw new NullPointerException();
         
         task =  new TimerTask() {
            @Override
			public void run() { 
               if ( !isStarted ) {
                  isStarted = true;
                  TimedThread.this.start();
               }
            }
         };

         Global.getTimer().schedule(task, delay);
      }  // constructor
      
      /**
       * Creates a thread run schedule with the specified delay time. After
       * time has elapsed, this thread will start execution.
       *  
       * @param r <code>Runnable</code> to be run by this thread
       * @param delay int, start delay time in milliseconds
       */
      public TimedThread ( final Runnable r, int delay ) {
         this( r, null, delay );
      }
      
      /** Tests whether this thread has already run to its end.
       * 
       * @return boolean true == thread has started and completed execution
       */
      public boolean hasRun () {
         return isStarted && !isAlive();
      }
      
      /** Terminates the thread run schedule. If the thread has already 
       * started, it will run to its end.
       */
      public void cancel () {
         task.cancel();
      }
      
      /** If this thread has not yet been started, it will be scheduled for
       * execution immediately (equal priority to current thread). It will
       * execute asynchronously to the calling thread.
       */
      public void executeNow () {
         task.cancel();
         if ( !isStarted ) {
            isStarted = true;
            start();
         }
      }
      
      /** If this thread has not yet been started, it will be scheduled for
       * execution immediately (equal priority to current thread) and the 
       * current thread waits until this thread has terminated. 
       */
      public void executeAndWait () {
         task.cancel();
         if ( !isStarted ) {
            isStarted = true;
            start();
         }
         try { 
            this.join(); 
         } catch ( InterruptedException e ) { 
            e.printStackTrace(); 
         }
      }
   }  // class TimedTask
   
   /** Action extending {@code AbstractAction} which performs the 
    * {@code Runnable} given with the constructor synchronously in direct 
    * reference.
    */
   protected class TaskAction extends AbstractAction {
	   private Runnable runnable;
	   
	   public TaskAction (Runnable r) {
		   Objects.requireNonNull(r);
		   putValue(ACTION_KEY_PROPERTY, "TaskAction");
		   runnable = r;
	   }
	   
		@Override
		public void actionPerformed (ActionEvent e) {
			runnable.run();
		}
   }
   
   protected class TaskExecutor implements Executor {

		@Override
		public void execute (Runnable task) {
			Objects.requireNonNull(task);
			Action act = new TaskAction(task);
			boolean ok = scheduleAction(act, null);
			if (!ok) {
				throw new RejectedExecutionException();
			}
		}
   }
}
