/*
*  File: Create_Lang_Bundle.java
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.ragna.front.util.ResourceLoader;

/**
 *  Test_PropertyFiles in org.jpws.front.util
 */
public class Create_Lang_Bundle {

private void test_system_files () {
	String path1, path2, path3;

	try {
      ResourceLoader.get().addResourcePath( "" );
		
//      // load ENGLISH master ACTION
//	  path1 = "#bundles/action.properties";
//	  
//      // load slave GERMAN + compare
//      path2 = "#bundles/action_de.properties";
//      build_resources(path1, path2);
      
      // load ENGLISH master DISPLAY
	  path1 = "#bundles/display.properties";
	  
      // load slave GERMAN + compare
      path2 = "#bundles/display_de.properties";
      path3 = "#bundles/display2_de.properties";
      build_resources(path1, path2, path3);
      
      System.out.println( "\r\n*** end of  investigation" );
	} catch (IOException e) {
		e.printStackTrace();
	}
}

private void build_resources (String path1, String path2, String path3) throws IOException {
	Properties master, slave, newSlave;
	InputStream input;
	OutputStream output;
	PrintWriter wrt;
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
    
    // load SLAVE
    URL slaveUrl = ResourceLoader.get().getResourceURL(path2);
	if (slaveUrl == null) {
		   throw new FileNotFoundException(path2);
	}
	input = slaveUrl.openStream();
	  
    if ( slaveIsUtf ) {
       slave =  new PropertiesUTF();
       ((PropertiesUTF)slave).load( input, "utf-8" );
    } else {
       slave =  new Properties();
       slave.load( input );
    }
    input.close();
	
    // create NEW-SLAVE and output stream
    String hs = path2.charAt(0) == '#' ? path2.substring(1) : path2;
    String base = slaveUrl.getPath().substring(0, slaveUrl.getPath().indexOf(hs));
    String outPath = base + (path3.charAt(0) == '#' ? path3.substring(1) : path3);
    output = new FileOutputStream(outPath);
    wrt = new PrintWriter(new OutputStreamWriter(output, "iso-8859-1"));

    if ( slaveIsUtf ) {
    	newSlave =  new PropertiesUTF();
     } else {
        newSlave =  new Properties();
     }
     
    // compare two properties files + create new one
    create_properties(master, slave, newSlave, path1, path2, outPath);
    
    // create a list of key
    List<String> list = new ArrayList<>();
    for (Enumeration en = newSlave.keys(); en.hasMoreElements();) {
        String key = (String)en.nextElement();
        list.add(key);
    }

    // sort list of keys 
    Collections.sort(list);
    String mem = "";
    for (String key : list) {
  	  String lead = key.substring(0, Math.min(key.length(), 3));
  	  if (!mem.equals(lead)) {
  		  wrt.println();
  		  mem = lead;
  	  }
        wrt.println( key + " = " + (String)newSlave.get(key) );
    }
    
    
//    if ( slaveIsUtf ) {
//    	((PropertiesUTF)newSlave).store(output, null, "utf-8");
//    } else {
//    	newSlave.store(output, null);
//    }
    wrt.close();
}

private void create_properties (Properties master, Properties slave, Properties newSlave, 
		                         String path1, String path2, String path3) {
   Enumeration<?> en;
   String key;
   newSlave.clear();
   
   try {
      System.out.println( "\r\n*** CREATING STRING RESOURCES (Properties Files) ***" );
      System.out.println( "File Master: " + path1 );
      System.out.println( "File Slave: " + path2 );
      System.out.println( "File Creation: " + path3 );
      int ct1 = 0, ct2 = 0;

      // transfer entry from MASTER to NEW-SLAVE
      // value depending on whether key is contained in SLAVE
      for (en = master.keys(); en.hasMoreElements();) {
         key = (String)en.nextElement();
         if ( !slave.containsKey( key ) ) {
            newSlave.put(key, master.get(key));
            ct1++;
         } else {
        	newSlave.put(key, slave.get(key));
        	ct2++;
         }
      }
      
      System.out.println( "*** created new Properties: values " + ct1 + " from master, " + ct2 + " from slave" );

   } catch ( Exception e ) {
      System.out.println( "*** Processing Error: "  );
      e.printStackTrace();
      return;
   }
}

public static void main ( String[] args ) {

	new Create_Lang_Bundle().test_system_files();
}

}
