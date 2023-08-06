/*
*  File: PersistentOptions.java
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

import java.awt.Font;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;

public interface PersistentOptions {

   /** Returns the mapped option integer value or 0 if the option is
    *  undefined.
    * @param token the option name
    * @return int
    */
   int getIntOption ( String token );

   /** Returns the mapped option long integer value or 0 if the option is
    *  undefined.
    * @param token the option name
    * @return long
    */
   long getLongOption ( String token );
   
   /** Returns the mapped option string value or empty string if the option is
    *  undefined.
    * 
    * @param token the option name
    * @return option value string or "" if undefined
    */
   String getOption ( String token );
   
   /** Returns the mapped option string value or the given default value if the
    * option-key is undefined.
    * 
    * @param token String the option name
    * @param def String default value if no mapping found 
    * @return String option value or def if undefined
    */
   String getOption (String token, String def);
   
   /** Returns the mapped bounds object (e.g. for a window) or 
    * <b>null</b> if this option is undefined.
    *  
    * @param token the option name
    * @return <code>Rectangle</code> specifying position and size of a window
    */
   Rectangle getBounds ( String token );

   /** Whether the specified (boolean) option is set to "true". */
   boolean isOptionSet ( String token );
   
   /** Sets an integer value for the parameter option name.
    * 
    * @param token the option name
    * @param value assigned int value
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    */
   boolean setIntOption ( String token, int value );

   /** Sets a long integer value for the parameter option name.
    * 
    * @param token the option name
    * @param value assigned long value
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    */
   boolean setLongOption ( String token, long value );

   /** Sets a boolean value for the parameter option name.
    * 
    * @param token the option name
    * @param value assigned boolean value
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    */
   boolean setOption ( String token, boolean value );

   /** Sets a string value for the parameter option name.
    * 
    * @param token the option name
    * @param value assigned string value, may be <b>null</b> to clear
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    */
   boolean setOption ( String token, String value );
   
   /** Sets a bounds object (e.g. from a window or dialog) to be
    * associated with the token string.
    * 
    * @param token the option name
    * @param bounds <code>Rectangle</code>; may be <b>null</b> to clear
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    */
   boolean setBounds ( String token, Rectangle bounds );
   
   /**
    * Stores a collection of string expressions into this options.
    * 
    * @param name String name of the collection to store
    * @param ls List list of String objects, use <b>null</b> to clear
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    */
   boolean setStringList ( String name, Collection<String> ls );
   
//   /**
//    * Stores a list of string representations of objects
//    * in options. The <code>toString()</code> method is used on each 
//    * object to derive its representation value. (NOTE:
//    * this is not necessarily a serialization of objects themselves!)    
//    * 
//    * @param name String name of the list to store
//    * @param ls List list of objects, use <b>null</b> to clear
//    * @return boolean <b>true</b> if and only if this operation modified
//    *         the option value in the underlying data resource
//    */
//   boolean setStringList ( String name, List<Object> ls );
   
   /**
    * Retrieves a list of strings that has been previously stored
    * into options by <code>setStringList()</code>. If no value was
    * found for <b>name</b>, an empty list is returned.
    * 
    * @param name String name of the list
    * @return List a list of retrieved String values (may be empty)
    */
   List<String> getStringList ( String name );

   /** Returns the Font associated with the given key or null if the key
    * was not found.
    * 
    * @param key String
    * @return Font or null
    */
   Font getFontOption (String key);

   /** Returns the Font associated with the given key or the given default 
    * value if the key was not found.
    * 
    * @param key String
    * @param def Font default font
    * @return Font or null
    */
   Font getFontOption (String key, Font def);

	/** Maps the given Font to the given key.
	 * 
	 * @param key String
	 * @param font Font or null to clear
	 */
	void setFontOption (String key, Font font);

   /** Adds a listener to the changing of any property. Listeners can be 
    * added multiple times.
    * 
    * @param listener {@code PropertyChangeListener}
    */
   void addPropertyChangeListener (PropertyChangeListener listener);
   
   /** Adds a listener to the changing of the given property. Listeners can be 
    * added multiple times.
    * 
    * @param listener {@code PropertyChangeListener}
    */
   void addPropertyChangeListener (String propertyName, 
         PropertyChangeListener listener);
   
   void removePropertyChangeListener (PropertyChangeListener listener);
   
   void removePropertyChangeListener (String propertyName, 
         PropertyChangeListener listener);

   /** Whether this option-bag contains a mapping for the given key.
    * Returns <b>false</b> for argument null.
    * 
    * @param key String, may be null
    * @return boolean true == key present, false == key unknown
    */
   boolean contains (String key);

   
}
