/*
*  File: ResourceLoader.java
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

/**
 *  Multi-language text and image resources.
 * 
 *  @author Wolfgang Keller
 */
import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.ragna.core.Global;

import kse.utilclass.misc.Log;
import kse.utilclass.misc.Util;

public class ResourceLoader {

/** Image name for the fail default image. */	
public static final String FAIL_IMAGE = "default_fail";	

private static ResourceLoader instance;
   
//public String[][] substTokens;

private List<String> resPaths = new ArrayList<String>(); 
private Hashtable<String, ResourceBundle> bundleTable = new Hashtable<String, ResourceBundle>();
private Properties imageTable;
//private URL defaultFailImage;

private ResourceLoader() {}

//public void setTokens ( String[][] tok ) {
//   substTokens = tok; 
//}

/** Adds a resource path to this loader.
 * 
 * @param path String
 */
public void addResourcePath ( String path ) {
   if ( path != null ) {
      if ( !path.isEmpty() && !path.endsWith("/") ) {
         path += "/";
      }
      resPaths.add( path );
   }
}

/** Opens a bundle of resources at a given base name. The base name is
 * language independent and gets complemented by the current global locale.
 * The bundle is searched in all defined resource paths of this loader.
 * 
 * @param bundle String language name
 * @return {@code ResourceBundle} or null if not found
 */
private ResourceBundle openBundle ( String bundle ) {
   ResourceBundle rbundle = null;
   
   for (Iterator<String>  it = resPaths.iterator(); it.hasNext() && rbundle == null;) {
      String bundlePath = it.next() + "bundles/" + bundle;
      try { 
         rbundle = ResourceBundle.getBundle( bundlePath, Global.getLocale() ); 
      } catch ( Exception e ) {
//         System.out.println("*** failed bundle search: " + bundlePath ); 
      }
   } 

   if ( rbundle == null ) {
      Global.exit( "*** MISSING RESOURCE BUNDLE *** : " + bundle, true );
   }

   bundleTable.put( bundle, rbundle );
   return rbundle;
}  // openBundle

public String getString( String bundle, String key ) {
   if ( key == null ) return "";
   
   ResourceBundle rbundle = bundleTable.get( bundle );
   if ( rbundle == null ) {
      rbundle = openBundle( bundle );
   }

   String hstr;
   try {
      hstr = rbundle.getString( key );
   } catch (Exception ex) {
      hstr = "FIXME";
   }
/*
// interpret global variables
if ( hstr.indexOf( '$' ) > -1 ) {
   for ( i = 0; i < substTokens.length; i++ )
   hstr = Functions.substituteText( hstr, substTokens[i][0], substTokens[i][1] );
}
*/
   return hstr;
}  // getString

/** Returns a text resource from the "action" resource bundle.
 * 
 * @param key String resource name
 * @return String text resource or "FIXME" if not found
 */
public String getCommand ( String key ) {
   return getString( "action", key );
}  

/** Returns a text resource from the "display" resource bundle.
 * 
 * @param key String resource name
 * @return String text resource or "FIXME" if not found
 */
public String getDisplay ( String key ) {
   return getString( "display", key );
}  

/** Returns a text resource from the "message" resource bundle.
 * 
 * @param key String resource name
 * @return String text resource or "FIXME" if not found
 */
public String getMessage ( String key ) {
   return getString( "message", key );
}  

/** Tries to interpret <code>key</code> as string token of resource bundle "message"
*  but renders the input if no such code is defined.
* @return the bundle contained text, if <code>key</code> was a token,
*         <code>key</code> as is, otherwise.
*/
public String codeOrRealMsg ( String key ) {
	if (key == null) return null;
	String text = getString( "message", key );
	if ( text.equals( "FIXME" ) ) {
		text = key;
	}
	return text;
}


/** Tries to interpret <code>key</code> as string token of resource bundle "display"
*  but renders the input if no such code is defined.
*  
*  @return String bundle contained text, if <code>key</code> was a token,
*         <code>key</code> as is, otherwise.
*/
public String codeOrRealDisplay ( String key ) {
	if (key == null) return null;
	String text = getString( "display", key );
	if ( text.equals("FIXME") ) {
		text = key;
	}
	return text;
}

// *****************  IMAGES SECTION  *************************

public void init () {
   getString("action","testvalue");
// getString("message","testvalue");
   getString("display","testvalue");
 
   imageTable = new Properties();
   try {
       imageTable.load( getResourceStream( "#system/imagemap.properties" ));
   } catch ( Exception e ) {
      Global.exit( "*** MISSING RESOURCE *** : imagemap.properties\r\n" + e, true );
   }

//   defaultFailImage = getImageURL(FAIL_IMAGE);
}

//******** FOLLOWS STANDARD RESOURCE RETRIEVAL (file or jar protocol) ***************

/** Returns the URL of an image resource which is defined in the image table
 * of this loader.
 *  
 * @param token String image name
 * @return {@code URL}
 */
public URL getImageURL ( String token ) {
   String path = imageTable.getProperty( token );
   if ( path == null ) {
      Log.debug(10, "*** missing image association: " + token);
      return null;
   }

   return getResourceURL(path);
}


/** Returns the image-icon of the given identifier or null if this name is
 * unknown.
 * 
 * @param token String image name
 * @return {@code ImageIcon} or null
 */
public ImageIcon getImageIcon (String token) {
   URL url = getImageURL( token );
   return url == null ? null : new ImageIcon(url);
}

/** Returns the image-icon of the given identifier (token) or an alternative 
 * (failcase) if this name is not found. Returns null if both names are unknown.
 * 
 * @param token String image name
 * @param failcase String alternative image name
 * @return {@code ImageIcon} or null
 */
public ImageIcon getImageIcon (String token, String failcase) {
   URL url = getImageURL(token);
   if (url == null) {
      url = getImageURL(failcase);;
   }
   return url == null ? null : new ImageIcon(url);
}

/** Returns an image-icon from the UIManager's defaults image set or an 
 * alternative (failcase) from this loader's image-list if this name was not 
 * found. Returns null if both names are unknown.
 * 
 * @param token String default image name (UIManager)
 * @param failcase String alternative image name
 * @return {@code ImageIcon} or null
 */
public ImageIcon getDefaultImageIcon (String token, String failcase) {
   ImageIcon icon = (ImageIcon) UIManager.getIcon(token);
   if (icon == null && failcase != null) {
      icon = getImageIcon(failcase);
   }
   return icon;
}

/** Returns the image of the given identifier or null if the name is unknown.
 * 
 * @param id String image name
 * @return {@code Image}
 */
public Image getImage( String id ) {
   ImageIcon icon = getImageIcon( id );
   return icon == null ? null : icon.getImage();
}

/** Returns the image of the given identifier (token) or an alternative image 
 * if this name is not found. Returns null if both names are unknown.
 * 
 * @param token String image name
 * @param failcase String alternative image name
 * @return {@code Image} or null
 */
public Image getImage( String id, String failcase ) {
   ImageIcon icon = getDefaultImageIcon(id, failcase);
   return icon == null ? null : icon.getImage();
}


// *****************  RESOURCE HANDLING  *******************

/** 
 * Returns the text resource from a given resource file denotation.
 * 
 * @param path the full path and filename of the resource requested. If
 * <code>path</code> begins with "#" it is resolved against the program's
 * standard resource folder after removing "#"
 * @param enc the character encoding to be applied for reading the file
 *        (if <b>null</b> the platform default is used)
 * @return String text decoded with the given character encoding or <b>null</b>
 *         if the resource couldn't be obtained
 * @throws IOException
 */
public String getResourceText ( String path, String enc ) throws IOException {
   InputStream in;
   String res = null;
   
   if ( (in = getResourceStream( path )) != null ) {
	  ByteArrayOutputStream bOut = new ByteArrayOutputStream();
      try {
		 Util.transferData( in, bOut, 2048 );
      } catch (InterruptedException e1) {
		 e1.printStackTrace();
      } finally {
    	 in.close();
      }
      try { 
    	  res = bOut.toString( enc ); 
      } catch ( Exception e ) { 
    	  res = bOut.toString(); 
      }
   }
   return res;
}

/** General use resource InputStream getter.
 * @param path the full path and filename of the resource requested. If
 * <code>path</code> begins with "#" it is resolved against the program's
 * standard resource folder after removing "#"
 * @return an InputStream to read the resource data, or <b>null</b> if the resource
 * could not be obtained
 * @throws java.io.IOException if there was an error opening the input stream
 */
public InputStream getResourceStream ( String path ) throws java.io.IOException {
   URL url = getResourceURL(path);
   return url == null ? null : url.openStream();
}

/** General use resource URL getter.
 * 
 * @param path String full path of the resource requested. If
 * <code>path</code> begins with "#" it is resolved against the program's
 * standard resource folder after removing "#".
 * @return URL or null if the resource was not found
 */
public URL getResourceURL ( String path ) {
   Objects.requireNonNull(path, "path is null");
   URL url = null;

   if ( path.startsWith("#") ) {
      for ( int i = 0; i < resPaths.size() && url == null; i++ ) {
         String rp = resPaths.get(i) + path.substring(1);
         url = ResourceLoader.class.getClassLoader().getResource( rp );
//         url = ClassLoader.getSystemResource( rp );
      } 
   } else {
      url = ResourceLoader.class.getClassLoader().getResource( path );
//      url = ClassLoader.getSystemResource( path );
   }

   if ( url == null ) {
       System.err.println("(ResourceLoader) * failed locating resource: " + path);
//   } else {
//      System.out.println( "resource URL: " +url.getProtocol() + ", path: " + url.getPath() );
   }
   return url;
}

/** Returns the resource paths which were defined with 'addResource()'.
 * 
 * @return String[]
 */
public String[] getResourcePaths () {
    String[] s = resPaths.toArray(new String[resPaths.size()]);
    return s; 
}

/** Returns the Singleton instance of this ResourceLoader class.
 *   
 * @return ResourceLoader
 */
public static ResourceLoader get () {
   if (instance == null) {
      instance = new ResourceLoader();
   }
   return instance;
}

}
