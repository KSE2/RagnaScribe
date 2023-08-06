/*
*  File: Test_PropertyFiles.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.ragna.front.util.ResourceLoader;

/**
 *  Test_PropertyFiles in org.jpws.front.util
 */
public class Test_PropertyFiles {

private void test_system_files () {
	String path1, path2;

	try {
      ResourceLoader.get().addResourcePath( "" );
		
      // load ENGLISH master ACTION
	  path1 = "#bundles/action.properties";
	  
      // load slave GERMAN + compare
      path2 = "#bundles/action_de.properties";
      compare_resources(path1, path2);
      
      // load ENGLISH master DISPLAY
	  path1 = "#bundles/display.properties";
	  
      // load slave GERMAN + compare
      path2 = "#bundles/display_de.properties";
      compare_resources(path1, path2);
      
      System.out.println( "\r\n*** end of  investigation" );
	} catch (IOException e) {
		e.printStackTrace();
	}
}

private void compare_resources (String path1, String path2) throws IOException {
	Properties master, slave;
	InputStream input;
	boolean masterIsUtf = false, slaveIsUtf = false;
	
	// load MASTER
	input = ResourceLoader.get().getResourceStream(path1);
	if (input == null) {
	   throw new FileNotFoundException(path1);
	}
	  
    if ( masterIsUtf ) {
       master =  new PropertiesUTF();
       ((PropertiesUTF)master).load( input, "utf-8" );
    } else {
       master =  new Properties();
       master.load( input );
    }
    input.close();
    
	input = ResourceLoader.get().getResourceStream(path2);
	if (input == null) {
	   throw new FileNotFoundException(path1);
	}
	  
    if ( slaveIsUtf ) {
       slave =  new PropertiesUTF();
       ((PropertiesUTF)slave).load( input, "utf-8" );
    } else {
       slave =  new Properties();
       slave.load( input );
    }
    input.close();
	
    // compare two properties files
    compare_properties(master, slave, path1, path2);
}

private void compare_properties (Properties master, Properties slave, String path1, String path2) {
   Enumeration<?> en;
   String key;
   int missing, additional;
   
   try {
      System.out.println( "\r\n*** COMPARING STRING RESOURCES (Properties Files) ***" );
      System.out.println( "File Master: " + path1 );
      System.out.println( "File Slave: " + path2 );

      // check for missing in slave
      List<String> list1 = new ArrayList<>();
      for ( en = master.keys(); en.hasMoreElements(); ) {
         key = (String)en.nextElement();
         if ( !slave.containsKey( key ) ) {
            list1.add( key );
         }
      }
      
      missing = list1.size();
      if (list1.size() > 0) {
    	  Collections.sort(list1);
          System.out.println( "*** Missing keys in Slave:" );
          String mem = "";
          for (String hs : list1) {
        	  String lead = hs.substring(0, Math.min(hs.length(), 3));
        	  if (!mem.equals(lead)) {
        		  System.out.println();
        		  mem = lead;
        	  }
              System.out.println( "   " + hs + " = " + (String)master.get(hs) );
          }
      }
      
      // check for additional in slave
      List<String> list2 = new ArrayList<>();
      for ( en = slave.keys(); en.hasMoreElements(); ) {
         key = (String)en.nextElement();
         if ( !master.containsKey( key ) ) {
            list2.add( key );
         }
      }
      
      additional = list2.size();
      if (list2.size() > 0) {
    	  Collections.sort(list2);
          System.out.println( "*** Missing keys in Master:" );
          String mem = "";
          for (String hs : list2) {
        	  String lead = hs.substring(0, Math.min(hs.length(), 3));
        	  if (!mem.equals(lead)) {
        		  System.out.println();
        		  mem = lead;
        	  }
              System.out.println( "   " + hs + " = " + (String)slave.get(hs) );
          }
      }
      
      if ( missing == 0 & additional == 0 ) {
         System.out.println( "*** no problems found in previous file compare!" );
      }
         
   } catch ( Exception e ) {
      System.out.println( "*** Processing Error: "  );
      e.printStackTrace();
      return;
      
   }
}

public static void main ( String[] args ) {

	new Test_PropertyFiles().test_system_files();
}

}
