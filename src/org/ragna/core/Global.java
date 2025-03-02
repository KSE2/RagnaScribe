/*
*  File: Global.java
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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import org.ragna.front.DisplayManager;
import org.ragna.front.GUIService;
import org.ragna.front.Ragna;
import org.ragna.front.StatusBar;
import org.ragna.front.util.ResourceLoader;
import org.ragna.front.util.SetStackMenu;
import org.ragna.io.IO_Manager;
import org.ragna.util.OptionBag;
import org.ragna.util.PersistentOptions;

import kse.utilclass.io.IOService;
import kse.utilclass.misc.Log;
import kse.utilclass.misc.PropertySupport;
import kse.utilclass.misc.SystemProperties;
import kse.utilclass.misc.UnixColor;
import kse.utilclass.misc.Util;
import kse.utilclass2.misc.CommandlineHandler;
import kse.utilclass2.misc.MirrorFileManager;


public class Global {

   public static final String APPLICATION_TITLE = "Ragna Scribe 1.2.1";
   public static final String DEFAULT_OPTIONFILE_NAME = "ragnasc.pref";
   public static final String DEFAULT_APPLICATION_DIR_NAME = ".ragnasc";
   public static final String DEFAULT_HISTORY_DIR_NAME = "safe";
   public static final String DEFAULT_MIRROR_DIR_NAME = "mirrors";
   public static final int DEFAULT_MIRROR_CHECK_PERIOD = 120;
   
   /** The application's default frame size. */
   public static final Dimension DEFAULT_FRAME_DIM = new Dimension(600, 480);
   public static final int DEBUG_LEVEL = 0;
   private static final Font DEFAULT_EDITOR_FONT = Font.decode("Dialog-PLAIN-13");
   private static final Color DEFAULT_BACKGROUND_COLOR = UnixColor.CornSilk;
   private static final Color DEFAULT_FOREGROUND_COLOR = new Color(0x323232);

   private static final Color[] BACKGROUND_COLORS = new Color[] {
		    // light
		    DEFAULT_BACKGROUND_COLOR, UnixColor.AntiqueWhite, UnixColor.Ivory,
		    new Color(0xFFAE89), UnixColor.Lavender, UnixColor.LightCyan,
		    UnixColor.PaleGreen, UnixColor.PowderBlue, UnixColor.GreenYellow, 
		    UnixColor.MistyRose, UnixColor.Thistle, 
		    UnixColor.Pink, UnixColor.Khaki, UnixColor.Moccasin, UnixColor.Tan,
		    
		    // medium
		    UnixColor.RoyalBlue, Color.cyan, Color.green, UnixColor.Crimson, 
		    Color.yellow, Color.magenta, UnixColor.Orange, UnixColor.Plum,
		    new Color(0xFF6435), UnixColor.LimeGreen,UnixColor.ForestGreen, 
		    UnixColor.Gold, UnixColor.Violet, UnixColor.DeepSkyBlue, UnixColor.GoldenRod,
		    
			// strong
		    UnixColor.SaddleBrown, UnixColor.DarkBlue, UnixColor.DarkGreen,
			UnixColor.DarkMagenta, UnixColor.SlateBlue, UnixColor.SlateGray,
			UnixColor.FireBrick, UnixColor.SeaGreen, UnixColor.Chocolate, UnixColor.DarkOrchid,
			
			Color.lightGray, Color.darkGray, Color.black, Color.white,
	};

   /** The application UI main window (JFrame). */ 
   public static Ragna                     mainframe; 

   /** Makes commandline options available to the program. */
   public static CommandlineHandler         cmlHandler;

   /** The resource loading instance. */
   public static  ResourceLoader            res;
   private static DocumentRegistry          documentRegistry;
   private static MirrorFileManager         mirrorFileManager;
   
   private static SystemProperties          systemProperties;
   private static OptionBag                 systemOptions; 
   private static OptionHandler				optionHandler = new OptionHandler();
   private static RecentFilesStack          recentFilesMenu;
   private static Timer                     timer;
   private static Locale 					locale = Locale.getDefault();

   // Application parameters
   private static boolean                 isWindows;
   private static boolean                 isRestrictedFilepath;
   private static boolean                 isFixedHistoryDir;
   private static File                    mirrorDir;
   private static File                    programDir;
   private static File                    applicationDir;
   private static File                    historyDir;
   private static File                    currentDir;
   private static File                    exchangeDir;
   private static File                    homeDir;
   private static String                  optionFilepath;
   private static String                  restrictedFilepath;
   private static int                     mirrorCheckPeriod = DEFAULT_MIRROR_CHECK_PERIOD;     
   
   
   public static File getCurrentDirectory () {
      return currentDir;
   }
   
   public static File getExchangeDirectory () {
      return exchangeDir;
   }
	   
   public static File getHistoryDirectory () {
      return historyDir;
   }
	   
   public static File getDefaultHistoryDirName () {
       return new File(applicationDir, DEFAULT_HISTORY_DIR_NAME);
   }
	   
   public static File getApplicationDirectory () {
      return applicationDir;
   }
	   
   public static File getProgramDirectory () {
      return programDir;
   }
	   
   public static void setCurrentDirectory (File dir) throws IOException {
      if (dir != null && !currentDir.equals(dir)) {
         currentDir = dir.getCanonicalFile();
      }
   }
                
   public static void setExchangeDirectory (File dir) throws IOException {
      if (dir != null && !exchangeDir.equals(dir)) {
         exchangeDir = dir.getCanonicalFile();
      }
   }
	   
   /** Sets a new value for the file history directory. 
    * 
    * @param dir File
    * @throws IOException
    */
   public static void setHistoryDirectory (File dir) throws IOException {
      if (dir != null && !historyDir.equals(dir) && Util.ensureDirectory(dir, null)) {
         historyDir = dir.getCanonicalFile();
 		 systemOptions.setOption("historyDir", historyDir.getAbsolutePath());
 		 IO_Manager.get().setFileHistoryDir(historyDir);
      }
   }
	                
   public static File getUserDirectory () {
      return homeDir;
   }
   
   public static MirrorFileManager getMirrorFileManager () {
      return mirrorFileManager;
   }
   
   public static void consoleLn ( String message ) {
      System.out.println( message );
   }
   
   public static void consoleLn () {
      System.out.println();
   }
   
   public static Timer getTimer () {
      return timer;
   }
   
   public static boolean isDebug () {
	   return DEBUG_LEVEL > 0;
   }
   
   /** Returns the application system's option store.
    * 
    * @return <code>PersistentOptions</code>
    */
   public static PersistentOptions getOptions () {
      return systemOptions;
   }
   
   /** Returns the application system's properties.
    * This serves as a communication device for the various modules
    * of the program.
    * 
    * @return <code>SystemProperties</code>
    */
   public static SystemProperties getSystemProperties () {
      return systemProperties;
   }
   
   /** The size of the console screen device we are working with. */
   public static Dimension getScreenSize () {
      return Toolkit.getDefaultToolkit().getScreenSize();
   }
   
   /** The currently active default character set of the Java Virtual Machine. */
   public static String getDefaultCharset () {
      return Charset.defaultCharset().name();
   }

   /** The currently active default character set for <i>Treepad</i> document
    * files. (This value can be manipulated by user interaction in the 
    * preferences dialog.)  
    */
   public static String getDefaultTreepadEncoding () {
	  String result = systemOptions.getOption("defaultTreepadEncoding");
      return result.isEmpty() ? getDefaultCharset() : result;
   }

   /** The global default text editor font.
    *  
    * @return {@code Font}
    */
   public static Font getDefaultFont() {
		return DEFAULT_EDITOR_FONT;
	}
	   
   /** The global default editor background colour for article content.
    *  
    * @return {@code Color}
    */
   public static Color getDefaultBackgroundColor() {
		return DEFAULT_BACKGROUND_COLOR;
	}

   /** The global default editor background colour for article content.
    *  
    * @return {@code Color}
    */
   public static Color getDefaultForegroundColor() {
		return DEFAULT_FOREGROUND_COLOR;
	}

   public static Color[] getBackgroundColorSet () {
	   return BACKGROUND_COLORS;
   }
   
   public static Locale getLocale () {return locale;}
   
   public static SetStackMenu getRecentFilesStack () {
      return recentFilesMenu;
   }
   
   public static StatusBar getStatusBar () {
      return DisplayManager.get().getStatusBar();
   }
   
   /** Starts the RAGNA application.
    * 
    * @param args
    */
   public static void main( String[] args ) {
      // run the application
      StartupHandler.startup_background( args );
      StartupHandler.startup_front();
      StartupHandler.start_autorun();
      saveSystemOptions();
   }

   public static void exit() {
      consoleLn();
      consoleLn("# APPLICATION EXIT");
      
      // attempt close of all registered documents
      if ( !ActionHandler.get().collectiveDocumentClose() ) {
         consoleLn("# exit interrupted!");
         return;
      }
      
      if (mainframe != null) {
    	  mainframe.exit();
      }
      
      ActionHandler.get().exit();
      timer.cancel();
      
      systemOptions.setIntOption("lastAccessTime", (int)(System.currentTimeMillis() / 1000));
      saveSystemOptions();
      
      consoleLn("# Terminated");
      System.exit(0);
   }

   public static void saveSystemOptions () {
       OptionHandler.saveSystemOptions(systemOptions);
   }
   
//  ****************  INNER CLASSES   **********************
   
private static class StartupHandler {


   private static void startup_background ( String[] args ) {
      File userDir=null;
   
      // primary global utility structures
      timer  = new Timer();
      systemProperties = new PropertySupport();
      documentRegistry = new DocumentRegistry();
      
      // perk operation
      consoleLn("# " + APPLICATION_TITLE + "  " + Util.technicalTimeString(System.currentTimeMillis()));
      
      // control compliance of running Java Virtual Machine
      controlJavaVersion();
      
      // system properties and default directories
      isWindows = System.getProperty("os.name","").toLowerCase().indexOf("windows") > -1;
      programDir = new File(getProgramPath());
      try {
         userDir = Util.getUserDir();
         homeDir = new File( System.getProperty("user.home", userDir.getPath()) ).getCanonicalFile();
         currentDir = userDir;
         applicationDir = new File(homeDir, DEFAULT_APPLICATION_DIR_NAME).getCanonicalFile();
   	  	 historyDir = getDefaultHistoryDirName();
      } catch ( Exception e ) {
         e.printStackTrace();
         System.exit(2);
      }

      // initialise low-levels
      boolean isDebug = DEBUG_LEVEL > 0;
      Log.setDebugLevel(DEBUG_LEVEL);
      Log.setLogLevel(DEBUG_LEVEL);
      Log.setDebug(isDebug);
      Log.setLogging(isDebug);
      Log.setThreadIds(true);
      Log.setExcludeList(new String[] {"ButtonBar", "ButtonBarDialog", "DialogButtonBar", "StatusBar", 
    		    "JMenuReader", "MightyMenuBar"});
      StatusBar.setTimer(timer);
      
      // interpret the command line 
      digest_commandline(args);

      // control application directory
      if (Util.ensureDirectory(applicationDir, null)) {
          consoleLn( "# Application Directory = ".concat( applicationDir.getPath() ));
      } else {
          consoleLn( "*** INOPERABLE APPLICATION DIRECTORY: ".concat( applicationDir.getPath() ) );
          System.exit(2);
      }
	  assert applicationDir != null;
      
      // load program options (including user preferences)
      systemOptions = OptionHandler.loadSystemOptions();
	  assert systemOptions != null;
      systemOptions.addPropertyChangeListener(optionHandler);
      
      // report ORPHAN documents memory
      List<String> list = systemOptions.getStringList("list-new-docs");
      if (list.isEmpty()) {
    	  consoleLn("# no Orphan documents listed");  
      } else {
    	  String hstr =  "# ORPHAN documents:";
    	  for (String s : list) hstr += " ".concat(s);
    	  consoleLn(hstr);
      }

      // set individual history directory (if not set by command-line option)
      if (!isFixedHistoryDir) {
	      String hstr = systemOptions.getOption("historyDir");
	      if (!hstr.isEmpty()) {
			try {
				historyDir = new File(hstr).getCanonicalFile();
			} catch (IOException e) {
                consoleLn( "*** INVALID HISTORY DIRECTORY IN PREFERENCES" );
				e.printStackTrace();
			}
	      }
      }
      
      // control history directory
      // if inoperable setting then assume default path (under appl-dir)
	  if (Util.ensureDirectory(historyDir, null)) {
          consoleLn( "# History Directory = ".concat(historyDir.getAbsolutePath()));
	  } else {
          consoleLn( "*** INOPERABLE HISTORY DIRECTORY: ".concat(historyDir.getPath()));
          
          // activate default value
   	  	  historyDir = getDefaultHistoryDirName();
          consoleLn( "*** assuming default path: ".concat(historyDir.getPath()));
    	  if (!Util.ensureDirectory(historyDir, null)) {
    		  System.exit(2);
    	  }
	  }
	  assert historyDir != null;
      
      // set individual current directory (if not set by command-line option)
      if (!isRestrictedFilepath) {
	      String hstr = systemOptions.getOption("currentDir");
	      if (!hstr.isEmpty()) {
			try {
				File dir = new File(hstr).getCanonicalFile();
				if (!dir.isDirectory()) {
					throw new IOException("directory not found: " + dir);
				}
				currentDir = dir;
			} catch (IOException e) {
                consoleLn( "*** INVALID CURRENT DIRECTORY IN PREFERENCES (ignored)" );
                consoleLn(e.toString());
			}
	      }
      }
	  assert currentDir != null;
	  
      // set exchange directory
      exchangeDir = currentDir;
      String hstr = systemOptions.getOption("exchangeDir");
      if (!hstr.isEmpty()) {
    	  File dir = new File(hstr);
    	  if (dir.isDirectory()) {
    		  exchangeDir = dir;
    	  }
      }
	  assert exchangeDir != null;
      
      // load resources
      res = ResourceLoader.get();
      String resourcePath = "org/ragna/resource/";
      res.addResourcePath( resourcePath );
      res.addResourcePath( "" );
      res.init ();
      
      // basic application classes
      try {
    	  IO_Manager.get();
    	  ActionHandler.get();
      } catch (Throwable e) {
    	  consoleLn("*** ERROR WHILE STARTING SYSTEM CLASSES: ");
    	  e.printStackTrace();
    	  System.exit(4);
      }

      // start task to promote file history system
      Runnable run = new Runnable () {
    	  @Override
    	  public void run() {
    		  IO_Manager.get().promoteHistory();
    	  }
      };
      ActionHandler.get().scheduleWorkerTask(run, "File History Promotion");
      
      // identify the MIRROR directory and create the manager
      if (mirrorDir == null) {
         mirrorDir = new File(applicationDir, DEFAULT_MIRROR_DIR_NAME);
      }
      try {
    	  mirrorFileManager = new MirrorFileManager(mirrorDir, mirrorCheckPeriod);
    	  if (!systemOptions.isOptionSet("useMirroring")) {
    		  mirrorFileManager.setActive(false);
    	  }
      } catch (Exception e) {
    	  consoleLn("*** Mirror-File directory inaccessible: ".concat(mirrorDir.getAbsolutePath()));
    	  consoleLn("    " + e);
    	  System.exit(4);
      }

      // load recent files menu
      recentFilesMenu = new RecentFilesStack("Recent Files", 16);
      recentFilesMenu.setAllowListDeletion(true);
      String content = systemOptions.getOption("recentFilesContent");
      recentFilesMenu.loadStringContent(content, ';');
      IO_Manager.get().addPropertyChangeListener(recentFilesMenu);
      Log.debug(5, "(StartupHandler) loading recent files with: ".concat(content));
   }

   private static void startup_front () {
      swingInit();
      
      consoleLn("# RAGNA initialised, starting GUI ..");
      
      // start Action system
      ActionHandler.get();
      
      DisplayManager.get().init();
      
      // start periodic system services
      SystemService.get();
   }

   private static void start_autorun () {
	  PersistentOptions options = getOptions();
	  
	  try {
	      // auto-loading of recently open documents
	      if (options.isOptionSet("isAutoOpenSession")) {
	         ActionHandler.get().openSession();
	      }
	      else if (options.isOptionSet("isAutoOpenRecentFile")) {
	         String path = (String)Global.getRecentFilesStack().peek();
	         if (path != null) {
	            ActionHandler.openDocumentFromPath(path, null, true);
	         }
	      }
	      
	      // control orphan documents (unsaved NEW)
	      List<String> olist = options.getStringList("list-new-docs");
	      for (String uuid : olist) {
	    	  Log.debug(5, "(Global.autorun) ** ORPHAN MIRROR marker = " + uuid);
	      }
	      ActionHandler.get().controlOrphanMirrors(olist);
	      
	      // control identity of history directory
	      String option = systemOptions.getOption("historyDir");
	      if (!option.isEmpty() && !option.equals(historyDir.getAbsolutePath())) {
	    	  String text = "<html>The path for HISTORY-DIR has been reset to default<br>because the following entry was erroneous:<br><font color=\"red\">$path</font>";
	    	  text = Util.substituteText(text, "$path", option);
	    	  GUIService.failureMessage("OPERATOR", text, null);
	    	  systemOptions.setOption("historyDir", historyDir.getAbsolutePath());
	      }
	      
//	      // TEST task
//	      Runnable neverEnding = new Runnable() {
//
//			@Override
//			public void run() {
//				Util.sleep(120000);
//			}
//	      };
//	      ActionHandler.get().scheduleWorkerTask(neverEnding, "Never-Ending-Task");
	      
	  } catch (Throwable e) {
		  GUIService.failureMessage("msg.failure.autorun", e);
		  e.printStackTrace();
	  }
   }

   private static void digest_commandline ( String args[] ) {
      
      // interpret the command line (catch program arguments) 
      cmlHandler = new CommandlineHandler( CommandlineHandler.Organisation.TRAILING );
      cmlHandler.setUnaryOptions("-h -help --h --help -m");
      try { 
         cmlHandler.digest(args); 
      } catch ( IllegalStateException e ) {
         consoleLn( "*** ILLEGAL COMMAND-LINE ORGANISATION: \r\n" + e );
         consoleLn( "*** COMMAND-LINE PARAMETERS IGNORED !!" );
         consoleLn( "*** OPTIONS MUST BE TRAILING ONLY" );
         consoleLn();
         printHelpMessage();
         cmlHandler.reset();
      }
      
      // print help message on request through option
      if ( cmlHandler.hasOption("-h") || cmlHandler.hasOption("-help") || 
           cmlHandler.hasOption("--h") || cmlHandler.hasOption("--help") ) {
         consoleLn();
         printHelpMessage();
         consoleLn();
         System.exit(0);
      }
      
      // a language parameter
      String lng = cmlHandler.getOption("-l");
      if (lng != null) {
    	  lng = lng.toLowerCase();
    	  locale = lng.equals("de") ? Locale.GERMAN : Locale.ENGLISH;
          Locale.setDefault(locale);
      }
      
      // isolate option file path parameter
      optionFilepath = cmlHandler.getOption("-o");
      if (optionFilepath != null) {
          try {
	    	  File file = new File(optionFilepath).getCanonicalFile();
	          if (!Util.ensureFilePath(file, null)) {
	              throw new IOException("invalid path: " + optionFilepath);
	          }
          } catch (IOException e) {
              consoleLn( "*** INVALID OPTION FILE-PATH SUPPLIED" );
              consoleLn(optionFilepath);
              System.exit(3);
          }
      }

      // isolate restricted directory path parameter
      String path = cmlHandler.getOption("-d");
      if (path != null) {
         try {
            currentDir = new File(path).getCanonicalFile();
            restrictedFilepath = currentDir.getPath();
            isRestrictedFilepath = true;
            if (!Util.ensureDirectory(applicationDir, null)) {
               throw new IOException("unreachable directory: " + path);
            }
         } catch (IOException e) {
            consoleLn( "*** INVALID RESTRICTED DIRECTORY SUPPLIED" );
            consoleLn(path);
            System.exit(3);
         }
      }

      // isolate application directory path parameter
      path = cmlHandler.getOption("-a");
      if (path != null) {
         try {
            applicationDir = new File(path).getCanonicalFile();
            if (!Util.ensureDirectory(applicationDir, null)) {
           	   throw new IOException("unreachable directory: " + path);
            }
         } catch (IOException e) {
            consoleLn( "*** INVALID APPLICATION DIRECTORY SUPPLIED" );
            consoleLn(path);
            System.exit(3);
         }
      }

      // isolate history directory path parameter
      if ( cmlHandler.hasOption("-s") ) {
          path = cmlHandler.getOption("-s");
          if (path != null) {
	          try {
	             historyDir = new File(path).getCanonicalFile();
	             isFixedHistoryDir = true;
	             if (!Util.ensureDirectory(historyDir, null)) {
	            	 throw new IOException("unreachable directory: " + path);
	             }
	          } catch (IOException e) {
	             consoleLn( "*** INVALID HISTORY DIRECTORY SUPPLIED" );
	             consoleLn(path);
	             System.exit(3);
	          }
          }
       }
   } // digest_commandline

   private static void printHelpMessage () {
      consoleLn( "RAGNA Optional Commandline Arguments:" );
      consoleLn( "-a [directory]\t\tspecial application directory" );
      consoleLn( "-d [directory]\t\trestrict document usage path" );
      consoleLn( "-h, -help \t\tprint this help message and exit" );
      consoleLn( "-l [de|en]\t\tGUI language selection" );
      consoleLn( "-o [file]\t\trefer to/create the given option file" );
      consoleLn( "-s [directory]\t\tspecial file-safety directory" );
   }
   
   /** Error-tolerant check for Java VM version to comply with this software. 
    *  If clearly fails, program terminates with error message.
    */
   private static void controlJavaVersion () {
      String ver, n[];
      int i, num1, num2, num3;
      
      // show Java version and default encoding
      ver = System.getProperty("java.runtime.version");
      if ( ver == null  || ver.isEmpty() )
         ver = System.getProperty("java.version");
      
      System.out.println( "# Java Version " + ver );
      System.out.println( "# Default Encoding: " + getDefaultCharset() );
         
      try {
         
         i = ver.indexOf('_');
         if ( i > 0 )
            ver = ver.substring( 0, i );
         
         n = ver.split( "[.]" );
         num1 = Integer.parseInt( n[0] ) * 10000;
         num2 = Integer.parseInt( n[1] ) * 100;
         num3 = Integer.parseInt( n[2] );
         i = num1 + num2 + num3;
         
         if ( i < 10800 ) {
            consoleLn( "*** !! INCOMPATIBLE JAVA VIRTUAL MACHINE !! ***" ); 
            consoleLn( "*** Minimum Java version: 1.8.0 required" ); 
            consoleLn();
            printHelpMessage();
            System.exit(1);
         }
      }
      catch ( Exception e )
      {e.printStackTrace();}
   }
   
   private static void swingInit () {
      Keymap keymap;
      Action action;
      KeyStroke keystroke;
      String lookAndFeel;
      
      // report actual running LAF
      lookAndFeel = null;
      if ( UIManager.getLookAndFeel() != null ) {
         lookAndFeel = UIManager.getLookAndFeel().getClass().getName();
      }
      consoleLn( "# Active Look-and-Feel: " + lookAndFeel );   
      
      /// CHANGES TO GLOBAL JTextComponent
      keymap = JTextComponent.getKeymap( JTextComponent.DEFAULT_KEYMAP );

      // add "CTRL-INS" to "clipboard copy" functionality
      action = new DefaultEditorKit.CopyAction();
      keystroke = KeyStroke.getKeyStroke( KeyEvent.VK_INSERT, Event.CTRL_MASK );
      keymap.addActionForKeyStroke( keystroke, action );
      

      // add "SHIFT-DEL" to "clipboard cut" functionality
      action = new DefaultEditorKit.CutAction();
      keystroke = KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, Event.SHIFT_MASK );
      keymap.addActionForKeyStroke( keystroke, action );

      // add "SHIFT-INS" to "clipboard paste" functionality
      action = new DefaultEditorKit.PasteAction();
      keystroke = KeyStroke.getKeyStroke( KeyEvent.VK_INSERT, Event.SHIFT_MASK );
      keymap.addActionForKeyStroke( keystroke, action );
   }

   /** Returns the (best guess) for the location of the program files.
    * @return String directory path of application in canonical form
    */   
   private static String getProgramPath () {
      String path, hstr;
      int i;
      
      if ( (path = System.getProperty( "java.class.path" )) != null ) {
//          System.out.println( "# CLASS PATH: [" + path + "]");
          consoleLn( "# VM Name: " + System.getProperty( "java.vm.name" ) + 
                     " " + System.getProperty( "java.vm.version" ) );
//          System.out.println( "# VM Vendor: " + System.getProperty( "java.vm.vendor" ) );
          
         // extract first path string
         char sep = isWindows() ? ';' : ':';
         if ( (i = path.indexOf(sep)) != -1 )
            path = path.substring( 0, i );
         path = path.trim();
         
         // get canonical value 
         if ( path != null && path.length() > 0 ) {
            // get canonical (this may resolve a symbolic link of caller)
            try { 
               path = new File( path ).getCanonicalPath(); 
            } catch ( IOException e ) { 
               e.printStackTrace(); 
            }
         
            // take parent dir if path denotes JAR or EXE file
            hstr = path.toLowerCase();
            if ( hstr.endsWith(".jar") || hstr.endsWith(".exe") )
               path = new File( path ).getParent();
         }
      }
      
      // if no path found in java.class.path then take user.dir 
      if ( path == null || path.isEmpty() || !new File( path ).isDirectory() ) {
         path = System.getProperty("user.dir");
         consoleLn( "# *** no valid class path found; assuming USER DIR as program dir ***");
      }
   
      Log.debug( 8, "(Global.getProgramPath)--- rendered path: [" + path + "]");
      return path;
   }
}

private static class OptionHandler implements PropertyChangeListener {
   static Preferences systemUserPrefs = Preferences.userRoot().node( "/ragnasc/system/options" );

   static synchronized OptionBag loadSystemOptions () {
      OptionBag obg = new OptionBag();
      
      // read options from a file
      if ( optionFilepath != null ) {
    	 InputStream in = null;
         try {
        	File file = new File(optionFilepath).getCanonicalFile();
            in = IOService.get().getBlockingFileInputStream(file);
            obg.load( in, "UTF-8" );
            consoleLn( "# read system options from: ".concat(file.getAbsolutePath()) );
         } catch (FileNotFoundException | InterruptedException e) {
            consoleLn( "# unable to find options file: ".concat(optionFilepath) );
         } catch (IOException e) {
            consoleLn( "# ERROR on reading option file: ".concat(optionFilepath) );
            e.printStackTrace();
         } finally {
        	if (in != null) {
        		try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
         }
      }

      // alternatively, read options from Java Preferences Device
      if ( obg.isEmpty() ) {
         String content = systemUserPrefs.get("properties", null);
         if ( content != null ) {
            try {
               obg.load(content);
               consoleLn( "# reading system options from Java Preferences" );
            } catch (IOException e) {
               consoleLn( "# ERROR reading system options from JAVA PREFERENCES" );
               e.printStackTrace();
            }
         }
      }

      if ( obg.isEmpty() ) {
         consoleLn();
         consoleLn( "# RELYING ON DEFAULT OPTIONS" );
         obg.setOption( "optionkind", "0001" );
         obg.setHeadText( "RAGNA SCRIBE PREFERENCES V. 0001" );
      }
      
      // to make sure, load the default options as well
      obg.setDefaults(DefaultOptions.get());
      
      // distribute global data
      // define a CURRENT DIRECTORY if suitable
      String currentPath = obg.getOption("currentDir");
      if (!currentPath.isEmpty() && new File(currentPath).isDirectory()) {
         try {
            if (!isRestrictedFilepath() || 
               currentPath.startsWith(currentDir.getAbsolutePath())) {
               currentDir = new File(currentPath).getCanonicalFile();
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      
      return obg;
   }
   
   /** Saves the parameter system options if and only if they are modified.
    *  
    * @param options <code>OptionBag</code>
    */
   static synchronized void saveSystemOptions ( OptionBag options ) {

//      Log.debug(10, "(OptionHandler) checking save system options");
      if ( options == null ) return;
      boolean written = false;
         
      // collect global data
      options.setOption("currentDir", currentDir.getAbsolutePath());
      options.setOption("exchangeDir", exchangeDir.getAbsolutePath());
      String content = recentFilesMenu.getStringContent(';');
      options.setOption("recentFilesContent", content);
//      Log.debug(6, "(OptionHandler) saving recent files with: ".concat(content));
      
      if (options.isModified() ) {
         // save to file if we have one defined
         if (optionFilepath != null) {
        	OutputStream out = null;
            try {
               out = IOService.get().getBlockingFileOutputStream(new File(optionFilepath));
               options.store( out, "UTF-8" );
               written = true;
               Log.log(3, "# system options written to file: ".concat(optionFilepath) );
            } catch (FileNotFoundException | InterruptedException e) {
               consoleLn( "# unable to write system options to file: ".concat(optionFilepath) );
               consoleLn(e.toString());
            } catch (IOException e) {
               e.printStackTrace();
            } finally {
            	if (out != null) {
                    try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
            	}
            }
         }
         
         // alternately, save to Java Preferences
         if ( !written ) {
            content = options.toString();
            systemUserPrefs.put( "properties", content );
            try {
               systemUserPrefs.flush();
               options.resetModified();
               Log.log(3, "# system options written to Java Preferences" );
            } catch (BackingStoreException e) {
               e.printStackTrace();
            }
         }
      }
   }

   @Override
   public void propertyChange(PropertyChangeEvent evt) {
       String key = evt.getPropertyName();

       // switch mirror manager activity (ON/OFF)
       if (key == "useMirroring") {
    	   if (systemOptions.isOptionSet(key)) {
    		   mirrorFileManager.setActive(true);
    		   Log.log(10, "(Global.OptionHandler) switching ON mirror-file-manager");
    	   } else {
    		   mirrorFileManager.setActive(false);
    		   Log.log(10, "(Global.OptionHandler) switching OFF mirror-file-manager");
    	   }
       }	
   }
}

public static boolean isWindows () {
   return isWindows;
}

/** The locale specific rendering for "Yes" or "No", depending on the argument
 * value.
 * 
 * @param v boolean
 * @return String
 */
public static String yesNoText (boolean v) {
	String token = v ? "label.yes" : "label.no";
	return res.getDisplay(token);
}

private static String[] treepadCompatibleCS = new String[] {
		"ISO-8859-1", "ISO-8859-2", "ISO-8859-3", "ISO-8859-4", "ISO-8859-5", "ISO-8859-6",  
		"ISO-8859-7", "ISO-8859-8", "ISO-8859-9", "ISO-8859-10", "ISO-8859-11", "ISO-8859-13", 
		"ISO-8859-14", "ISO-8859-15", "ISO-8859-16", "windows-1250", "windows-1251", "windows-1252",  
		"windows-1253", "windows-1254", "windows-1255", "windows-1256", "windows-1257", 
		"windows-1258", "UTF-8", "US-ASCII"
};

/** Whether the given charset is compatible with Treepad documents (US-ASCII)
 * and also supported by the current JVM.
 * 
 * @param charset String character set name
 * @return boolean true == supported and compatible
 */
public static boolean isTreepadCompatibleCharset (String charset) {
	return Util.arrayContains(treepadCompatibleCS, charset) && Charset.isSupported(charset);
}

/** Returns a set of character set names which are compatible with US-ASCII
 * and supported by the active JVM.
 * 
 * @return String[] charset names
 */
public static String[] getTreepadCompatibleCharsets () {
	List<String> list = new ArrayList<>();

	// check validity of elements under current JVM
	// and filter out invalid names
	for (String cs : treepadCompatibleCS) {
		if (Charset.isSupported(cs)) {
			list.add(cs);
		}
	}
	return list.toArray(new String[list.size()]);
}

/** Returns a set of character set names which are supported by the active JVM.
 * 
 * @return String[] charset names
 */
public static String[] getVMSupportedCharsets () {
	Set<String> set = Charset.availableCharsets().keySet();
	return set.toArray(new String[set.size()]);
}

/** Whether the application is set to restricted document file paths.
 * 
 * @return boolean true == restricted file paths
 */
public static boolean isRestrictedFilepath () {
   return isRestrictedFilepath;
}

/** Returns the restricted filepath for this application session or null
 * if no restriction is active.
 * 
 * @return String filepath in file system
 */
public static String getRestrictedFilepath () {
   return restrictedFilepath;
}

/** Returns the application's currently active Frame window.
 *  (We only have one form now.)
 * 
 * @return <code>Frame</code>
 */
public static Frame getActiveFrame() {
   return mainframe;
}

/** Returns the application's central document registry. (This is the
 * registry which hold the documents that are going to be displayed in GUI.)
 * 
 * @return <code>DocumentRegistry</code>
 */
public static DocumentRegistry getDocumentRegistry() {
   return documentRegistry;
}

/** Initiates termination of the program after display of the parameter message
 *  at <code>System.err</code>.
 *  @param message text to be displayed
 *   */
public static void exit ( String message, boolean fatal ) {
   if ( fatal ) {
     System.out.println();
     System.out.println( "# RAGNA  fatal error condition" );
   }
   System.err.println( message );
   exit();
}  // exit

/** Start any task after a delay time.
 * 
 * @param run <code>Runnable</code> task to run
 * @param delay int milliseconds of delay
 */
public static void startTaskDelayed (Runnable run, String name, int delay) {
   ActionHandler.get().scheduleWorkerTaskDelayed(run, name, delay);
}

/** This class comprises small tasks automatically run in the background of 
 * the program.
 */
private static class SystemService {
   static final int STATUS_REPORT = 0;
   static final int GARBAGE_COLLECT = 1;
   static final int SYSTEM_SAVE = 2;
   static final int ACTIVITY_REPORT = 3;

   private static SystemService instance;

   public static SystemService get () {
      if ( instance == null ) {
         instance = new SystemService();
      }
      return instance;
   }
   
   private SystemService () {
      init();
   }

   private void init () {
//      new ServiceTask(STATUS_REPORT, 20000, 20000, true);
      new ServiceTask(GARBAGE_COLLECT, 120000, 60000, true);
      new ServiceTask(SYSTEM_SAVE, 60000, 3000, false);
      new ServiceTask(ACTIVITY_REPORT, 1000, 0, false);
   }
   
   private static class TaskBody implements Runnable {
      private int taskType;

      /** Creates a new Task-Runnable of the given type.
       * 
       * @param type int task name
       */
      TaskBody ( int type ) {
         taskType = type;
      }
      
      @Override
      public void run () {
         switch (taskType) {
         case STATUS_REPORT:
            int freeMem = (int)(Runtime.getRuntime().freeMemory() / 1000);
            int totalMem = (int)(Runtime.getRuntime().totalMemory() / 1000);
            String text = "JVM Memory: Free KB " + freeMem + " of " + totalMem;
            Global.getStatusBar().putMessage(text);
            break;

         case GARBAGE_COLLECT:
            System.gc();
            freeMem = (int)(Runtime.getRuntime().freeMemory() / Util.KILO);
            totalMem = (int)(Runtime.getRuntime().totalMemory() / Util.KILO);
            text = Util.dottedNumber((totalMem - freeMem) / 1000);
            getStatusBar().setRecordCounterCell(text);
//            Global.getStatusBar().putMessage("Garbage collected", 4000, UnixColor.firebrick);
            break;

         case SYSTEM_SAVE:
            // check/save program options
            saveSystemOptions();
            break;
            
         case ACTIVITY_REPORT:
        	 boolean isActive = ActionHandler.get().isOperating();
        	 getStatusBar().setActivity(isActive ? StatusBar.ACTIVE : StatusBar.PASSIVE);
         }
      }
   }
   
   /** Periodic task that runs on the EDT or in the timer's thread
    * depending on caller option during creation.
    */
   private class ServiceTask extends TimerTask implements Runnable {

      private Runnable runner;
      private boolean runOnEDT;
      
      /** Creates a new ServiceTask of the given type, delay and interval of 
       * execution. 
       * 
       * @param type int task name
       * @param period int milliseconds of interval; if zero, the task is
       *        performed only once
       * @param delay int milliseconds of delay before first execution
       * @param edt boolean if true this task will be run on the EDT
       *            otherwise on the SystemService's timer thread
       * @throws IllegalArgumentException if type is illegal or delay/period
       *         settings negative              
       */
      ServiceTask (int type, int period, int delay, boolean runOnEDT) {
         if (type < 0 | type > 3)
            throw new IllegalArgumentException("illlegal task name: "
                      .concat(String.valueOf(type)));
         
         runner = new TaskBody(type);
         this.runOnEDT = runOnEDT;
         timer.schedule(this, delay, period);
      }
      
      @Override
      public void run () {
         if ( runOnEDT ) {
            GUIService.executeOnEDT(runner);
         } else {
            runner.run();
         }
      }
   }

}

/** Starts the program's standard browser to launch an URL
 * address. If no browser is defined in program options,
 * the operating system's default browser is launched. 
 * 
 * @param url URL target address
 */
public static void startBrowser ( URL url )  {
   // get browser application from program options
   String browser = getOptions().getOption( "browserApplication" );
   boolean noBrowserDef = browser.isEmpty(); 
   
   // display warning if browser is defined but not operative
   boolean ok = noBrowserDef || new File( browser ).isFile();
   if ( !ok  ) {
      GUIService.failureMessage( "prefmsg.browser", null );
   }
   
   // if there is no valid browser definition, start OS default browser
   if (noBrowserDef | !ok) {
      startDefaultBrowser(url);
   }
   
   // if browser option is defined and available, start this application
   else {
      try {
         String cmd = browser + " " + url.toExternalForm();
         Log.debug( 4, "(Global.ServiceAction.startBrowser) starting EXTERNAL EXEC: ".concat( cmd ) );
         Runtime.getRuntime().exec( cmd );
         ok = true;
      } catch ( IOException e ) {
         e.printStackTrace();
         GUIService.failureMessage( "msg.failure.browseservice", e );
      }
   }
}

/** Starts the operating system's standard browser
 *  with an URL address to launch.
 *  
 * @param url URL target address
 */
public static void startDefaultBrowser ( URL url ) {
   // if DESKTOP available
   if ( Desktop.isDesktopSupported() ) {
      Desktop sysDesk = Desktop.getDesktop();
      try {
         if ( sysDesk.isSupported( Desktop.Action.BROWSE )) {
            Log.debug( 6, "(Global.ServiceAction.startDefaultBrowser) starting system default browser with "
                  .concat( url.toExternalForm() ));
            sysDesk.browse( url.toURI() );
         } else {
            // browser not supported by DESKTOP   
            GUIService.failureMessage( "msg.failure.nobrowseservice", null );
         }
      } catch ( Exception e ) {
         // some error in running application
         e.printStackTrace();
         GUIService.failureMessage( "msg.failure.browseservice", e );
      }

   // no DESKTOP service
   } else {
      GUIService.failureMessage( "msg.failure.missingdesktop", null );
   }
}

/** Starts the operating system's standard email client
 * with a recipient's address to "mailto".
 * 
 * @param address String target mail address
 */
public static void startEmailClient ( String address ) {
   // approve email address
   if ( !Util.isEmailAddress(address) ) {
      GUIService.failureMessage( "msg.failure.emailaddr", null );
      return;
   }

   // if DESKTOP available
   if ( Desktop.isDesktopSupported() ) {
      Desktop sysDesk = Desktop.getDesktop();
      try {
         if ( sysDesk.isSupported( Desktop.Action.MAIL )) {
            sysDesk.mail( new URI( "mailto", address, null) );
         } else {
            // no MAIL service
            GUIService.failureMessage( "msg.failure.nomailservice", null );
         }
      } catch ( Exception e ) {
         // some error in running application
         e.printStackTrace();
         GUIService.failureMessage( "msg.failure.mailservice", e );
      }
   // no DESKTOP service   
   } else {
      GUIService.failureMessage( "msg.failure.missingdesktop", null );
   }
}  // startEmailClient

   /** A human readable text expression for the default locale.
    */
   public static String getLocaleString () {
      String hstr = Locale.getDefault().getDisplayLanguage( Locale.ENGLISH ) + ", " +
             Locale.getDefault().getDisplayCountry( Locale.ENGLISH );
      return hstr;
   }

   /** Whether the given filepath points to a location within the mirror
    * file system.
    *  
    * @param path String filepath (local file system)
    * @return boolean true = location within mirror-system
    */
   public static boolean isMirrorFilepath (String path) {
   		String prefix = getMirrorFileManager().getMirrorRootDirectory().getAbsolutePath();
   		return path != null && path.startsWith(prefix);
   }
   
//  *************  INNER CLASSES  **********************

public static HyperlinkListener createHyperLinkListener () {
	
   return new HyperlinkListener() {
      private Stack<URL> stack = new Stack<URL>();
      
      @Override
	  public void hyperlinkUpdate ( HyperlinkEvent e ) {
         URL url;
         JEditorPane epane;
         String path, file, fname;
         int i;
         
         if ( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED ) {
            epane = (JEditorPane)e.getSource();
            if ( e instanceof HTMLFrameHyperlinkEvent ) {
               ((HTMLDocument)epane.getDocument())
                     .processHTMLFrameHyperlinkEvent( (HTMLFrameHyperlinkEvent)e );
            } else {
               try {
                  url = e.getURL();
                  
            //System.out.println( "--- protocol: " + url.getProtocol() );
                  // take special care of local files  
                  if ( url.getProtocol().equals("file") ||
                       url.getProtocol().equals("jar") ) {
            //System.out.println( "--- primary url: " + url );
            
                     // unmodified url for anchor references
                     if ( url.getRef() != null ) {
                        ;
                     
                     // interpreted file references
                     } else {
                        // extract noted filename
                        path = url.getPath();
                        i = path.lastIndexOf( '/' ) + 1;
                        fname = path.substring( i );
                        
                        // look for RETURN (step-back) command
                        if ( fname.equals( "$RETURN" )) {
                           if ( !stack.isEmpty() )
                              url = stack.pop();
                        } else {
                           // resolve filename against language specific setting 
                           // in ACTIONS.PROPERTIES
                           file = ResourceLoader.get().getCommand("html.file." + fname);
                           path = path.substring( 0, i ) + file;
                           url = new URL( url.getProtocol(), url.getHost(), path );
                        }
                     }
   
                     // start resulting file URL
                     if ( url != null ) {
               //System.out.println( "--- secondary url: " + url );
                        stack.push( epane.getPage() );
                        epane.setPage( url );
                     }
                  } 

                  // this attempts to start external files
                  else {
                     Global.startBrowser( url );
                  }

               } catch ( IOException ioe ) {
                  ioe.printStackTrace();
               }
            }
         }
      }
   };
}

private static class RecentFilesStack extends SetStackMenu implements PropertyChangeListener {
	
    public RecentFilesStack (String name, int maxEntries) {
		super(name, maxEntries, true);
	}

	@Override
    public synchronized boolean add (Object e) {
    	Objects.requireNonNull(e);
    	String filepath;
    	
    	// derive the filepath from object
    	if (e instanceof File) {
    		filepath = ((File)e).getAbsolutePath();
    	} else {
    		if (!(e instanceof String)){
    			throw new IllegalArgumentException("illegal argument type");
        	}
    		filepath = (String) e;
    	}

    	// filter out paths of the mirror file service (ignore)
    	if (isMirrorFilepath(filepath)) return false; 
    	return super.add(filepath);
    }

	@Override
	public void propertyChange (PropertyChangeEvent evt) {
		String name = evt.getPropertyName();
		Log.log(10, "(RecentFilesStack.propertyChange) --- property change: " + name);
		
		if ( "pathAdded".equals(name) ) {
			push(evt.getNewValue());

		} else if ( "pathReplaced".equals(name) ) {
			remove(evt.getOldValue());
			push(evt.getNewValue());

		} else if ( "documentSaved".equals(name) ) {
			String path = ((PadDocument)evt.getNewValue()).getExternalPath();
			if (path != null) {
				push(path);
			}

		} else if ( "documentDeleted".equals(name) ) {
			String path = ((PadDocument)evt.getOldValue()).getExternalPath();
			if (path != null) {
				remove(path);
			}
		}
	}
} // RecentFilesStack

   
}
