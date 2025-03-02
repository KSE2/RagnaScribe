/*
*  File: OptionBag.java
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import kse.utilclass.misc.Util;

public class OptionBag implements PersistentOptions {
	
   private static final String LISTMARK = "LIST.";
   
   private PropertiesUTF  properties = new PropertiesUTF();
   private Properties  defaults = new Properties();
   private PropertyChangeSupport support = new PropertyChangeSupport(this); 
   private String head;
   private boolean modified;

   /** Empty constructor. */
   public OptionBag () {
   }
   
   /** Constructs an empty option bag with a set of default options. 
    * 
    * @param defaults <code>Properties</code> containing default value mappings
    */
   public OptionBag ( Properties defaults ) {
      setDefaults( defaults );
   }
   
   /** Constructs an OptionBag with a string as input resource.
    * 
    * @param options String option list as a "Properties" text
    * @throws IOException
    */ 
   public OptionBag ( String options ) throws IOException {
      load( options );
   }
   
   /** Returns a deep copy of this option-bag with a new (empty) listener base.
    *  The "head" text is identical, the "modified" marker is set to false.
    *  
    *  @return {@code OptionBag}
    */
   public synchronized OptionBag copy () {
	   OptionBag copy = new OptionBag(defaults);
	   copy.properties = (PropertiesUTF) properties.clone();
	   copy.head = head;
	   return copy;
   }
   
   /** Replaces the content of this OptionBag with a Properties list
    *  contained in a string.
    * 
    * @param options String option list as a "Properties" text
    * @throws IOException
    */ 
   public synchronized void load ( String options ) throws IOException {
      ByteArrayInputStream in;
      
      in = new ByteArrayInputStream( options.getBytes( "UTF-8" ) );
      properties.load( in, "UTF-8" );
      modified = false;
   }
   
   /** Constructs an OptionBag from an inputstream resource. 
    * 
    * @param options <code>InputStream</code> containing option list as a 
    *        "Properties" text
    * @param charset name of the character set valid for the input stream         
    * @throws IOException
    */ 
   public OptionBag ( InputStream options, String charset ) throws IOException {
      properties.load( options, charset );
   }
   
   /** Sets the default properties for this OptionBag. (A default value for
    * key K becomes the return value of requesting methods of this class when 
    * there is no value mapped for K in the primary list of an instance.)
    * 
    * @param defaults <code>Properties</code>, may be null
    */
   public void setDefaults ( Properties defaults ) {
      properties.setDefaults( defaults );
      if ( (this.defaults = defaults) == null )
         this.defaults = new Properties(); 
   }
   
   public void setHeadText ( String text ) {
      head = text;
   }
   
   public void clear () {
      properties.clear();
      modified = true;
   }
   
   public boolean isEmpty () {
      return properties.isEmpty();
   }
   
   public synchronized void load ( InputStream options, String charset ) throws IOException {
      properties.load( options, charset );
      modified = false;
   }
   
   /** Saves the contents of the primary list of this option bag to the
    *  specified output stream, using the specified character set for encoding.
    *   
    *  @param out OutputStream
    *  @param charset name of the character set valid for the output stream 
    */
   public synchronized void store ( OutputStream out, String charset ) throws IOException {
      storeInternal( out, charset );
      modified = false;
   }
   
   /** Saves the contents of the primary list of this option bag to the
    *  specified output stream, using the specified character set for encoding.
    *   
    *  @param out OutputStream
    *  @param charset name of the character set valid for the output stream 
    */
   private synchronized void storeInternal ( OutputStream out, String charset )
      throws IOException {
      properties.store( out, head, charset );
   }
   
   /** Returns a byte array representing the mapping contents of this OptionBag.
    * 
    *  @param charset character set definition for the resulting sequence of bytes
    *  @return byte array
    */
   public byte[] toByteArray ( String charset ) {
      ByteArrayOutputStream out;
      
      out = new ByteArrayOutputStream();
      try { 
         storeInternal( out, charset );
         out.close();
         return out.toByteArray();
      }
      catch ( IOException e )
      { throw new IllegalStateException(); }
   }
   
   /** Returns a string representation of all the mappings in this OptionBag.
    *  (The result largely corresponds to the content of a "Properties" file
    *  except that transformations of special characters into escape encodings
    *  are not performed. Instead all characters are rendered in their natural
    *  state (Java UTF).)
    *  
    *  @return <code>String</code> containing key-value mappings
    */
   @Override
   public String toString () {
      try {
      return new String( toByteArray( "UTF-8" ), "UTF-8" );
      }
      catch ( UnsupportedEncodingException e )
      { return null; }
   }
   
   /** Elementary property retrieve function. 
    * 
    * @return String mapped value for token or <b>null</b> if not available
    * @throws NullPointerException if parameter is <b>null</b> 
    */
   public String getProperty ( String token ) {
      return properties.getProperty( token );
   }

   /** Elementary property function. This circumvents
    * the property change event dispatching! Neither key nor value
    * may be null.
    * 
    * @param token String key
    * @param value String mapped value
    * @return Object the previous assigned value or null if nothing 
    *         was assigned
    * @return boolean <b>true</b> if and only if this operation modified
    *         the option value in the underlying data resource
    * @throws NullPointerException if key or value is null        
    * */
   public String setProperty ( String token, String value ) {
      Object previous = properties.setProperty( token, value );
      boolean mod = previous == null || !previous.equals( value );
      modified |= mod;
      return (String)previous;
   }

   @Override
   public int getIntOption ( String token ) {
      String p;
      int i;

      i = 0;
      p = getProperty( token );
      if ( p != null )
      try {
         i = Integer.parseInt( p );
      }
      catch ( Exception e )
      {
         // meaning: look for default if mapped value is invalid (unparsable)
         p = defaults.getProperty( token );
         try {
            i = Integer.parseInt( p );
         }
         catch ( Exception e2 )
         {}
      }
      return i;
   }  // getIntOption

   @Override
   public long getLongOption ( String token ) {
      String p;
      long i;

      i = 0;
      p = getProperty( token );
      if ( p != null )
      try { i = Long.parseLong( p ); }
      catch ( Exception e )
      {
         // meaning: look for default if mapped value is invalid (unparsable)
         p = defaults.getProperty( token );
         try { i = Long.parseLong( p ); }
         catch ( Exception e2 )
         {}
      }
      return i;
   }  // getLongOption

   @Override
   public String getOption ( String token ) {
      String hstr= getProperty( token );
      if ( hstr == null ) hstr = "";
      return hstr;
   }

   @Override
   public String getOption ( String token, String def ) {
	   String hstr = getProperty(token);
	   if (hstr == null) hstr = def;
	   return hstr;
   }
   
   @Override
   public boolean isOptionSet ( String token ) {
      String p = getProperty( token );
      return p != null && p.equals( "true" );
   }

   @Override
   public boolean setIntOption ( String token, int value ) {
      return setOption( token, String.valueOf( value ) );
   }

   @Override
   public boolean setLongOption ( String token, long value ) {
      return setOption( token, String.valueOf( value ) );
   }

   @Override
   public boolean setOption ( String token, boolean value ) {
      String hstr = value ? "true" : "false";
      return setOption( token, hstr );
   }

   @Override
   public boolean setOption ( String token, String value ) {
      if ( token == null )
         throw new NullPointerException("token is null");

      // treat empty string as null value
      if ( value != null && value.isEmpty() ) {
         value = null;
      }
      
      // investigate the previous assigned value
      String previous = getProperty(token);
      boolean mod = Util.notEqual(previous, value);
      if ( mod ) {
      
         // remove the mapping if value is found to be eligible
         // (we avoid double entries in DEFAULTS and PROPERTIES)
         boolean removeInLayer = value == null || 
                 value.equals( defaults.getProperty( token ));
         
         if ( removeInLayer )  {
            // removing the assignment in the properties map
            properties.remove( token );
   
         } else  {
            // set new assignment in properties map
            setProperty( token, value );
         }
         
         // fire change event 
         support.firePropertyChange(token, previous, value);
      }
      
      modified |= mod;
      return mod;
   }

   @Override
   public boolean setBounds ( String token, Rectangle bounds ) {
      Dimension dim;
      Point pos;
      String nameA, nameB;
      boolean b1, b2;
      
      nameA = token.concat( "-framedim" );
      nameB = token.concat( "-framepos" );
      
      // delete bounds info in options 
      if ( bounds == null ) {
         b1 = setOption( nameA, null );
         b2 = setOption( nameB, null );
      }
      // store parameter bounds info
      else {
         dim = bounds.getSize();
         b1 = setIntOption( nameA, (dim.width << 16) | (dim.height & 0xFFFF) );
         pos = bounds.getLocation();
         b2 = setIntOption( nameB, (pos.x << 16) | (pos.y  & 0xFFFF) );
      }
      
      return b1 | b2;
   }
   
   @Override
   public Rectangle getBounds ( String token ) {
      Rectangle bounds;
      Dimension frameDim;
      Point framePos;
      int i;

      if ( token == null )
         throw new NullPointerException();
      
      // read frame dimensions from option file
      if ( (i = getIntOption( token.concat( "-framedim" ) )) > 0 )
         frameDim = new Dimension( i >> 16, (short)i );
      else
         return null;
      
      // read frame position from option file
      i = getIntOption( token.concat( "-framepos") );
      framePos = new Point( i >> 16, (short)i );

      // create result
      bounds = new Rectangle( framePos, frameDim );
      return bounds;
   }

   @Override
   public Font getFontOption (String key) {
	   String hstr = getOption(key);
	   return hstr.isEmpty() ? null : Font.decode(hstr);
   }
   
   @Override
   public Font getFontOption (String key, Font defaultFont) {
	   Font f = getFontOption(key);
	   return f == null ? defaultFont : f;
   }
   
   @Override
   public void setFontOption (String key, Font font) {
	   String value = font == null ? null : 
		   font.getFontName() + "-" + fontStyleName(font.getStyle()) + "-" + font.getSize();
	   setOption(key, value);
   }

   private String fontStyleName (int style) {
	   String n;
	   switch (style) {
	   case Font.BOLD: n = "BOLD"; break;
	   case Font.ITALIC: n = "ITALIC"; break;
	   case Font.BOLD + Font.ITALIC: n = "BOLDITALIC"; break;
	   default: n = "PLAIN";
	   }
	   return n;
   }

   @Override
   public boolean setStringList ( String name, Collection<String> ls ) {
      Objects.requireNonNull(name, "name is null");
      Iterator<String> it;

      List<String> oldList = getStringList(name);
      if (oldList.equals(ls)) return false;
      
      String prefix = LISTMARK.concat(name).concat("-");

      // remove all previous values of this list name
      boolean eol = false;
      int i = 0;
      while ( !eol ) {
         String key = prefix.concat( String.valueOf( i++ ) );
         eol = getProperty( key ) == null;
         if ( !eol ) {
            setOption( key, null );
         }
      }
      
      // if list is available, add strings of list to file
      if ( ls != null && ls.size() > 0 )
      for ( it = ls.iterator(), i=0; it.hasNext(); i++ ) {
         setOption( prefix.concat( String.valueOf( i ) ), it.next().toString() );
      }
      return true;
   }
   
   @Override
   public List<String> getStringList ( String name ) {
      Objects.requireNonNull(name, "name is null");
      
      ArrayList<String> list = new ArrayList<String>();
      String prefix = LISTMARK.concat(name).concat("-");

      // find all stored values of this list name
      boolean eol = false;
      int i = 0;
      while ( !eol ) {
         String key = prefix.concat( String.valueOf( i++ ) );
         String value = getProperty( key );
         eol = value == null;
         if ( !eol ) {
            list.add( list.size(), value );
         }
      }
      return list;
   }

   /** Whether this <tt>OptionBag</tt> has been modified since last being
    *  loaded or stored.
    * @return boolean
    */
   public boolean isModified () {
      return modified;
   }
   
   /** Resets the "modified" marker to <b>false</b>. */
   public void resetModified () {
      modified = false;
   }
   
   /** Sets the "modified" marker to <b>true</b>. */
   public void setModified () {
	   modified = true;
   }
   
   /** Sets this option bag to the MODIFIED state (without 
    * modifying anything).
    */
   public void touch () {
      modified = true;
   }
   
   @Override
   public void addPropertyChangeListener(PropertyChangeListener listener) {
      support.addPropertyChangeListener(listener);
   }
   
   @Override
   public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
      support.addPropertyChangeListener(propertyName, listener);
   }
   
   @Override
   public void removePropertyChangeListener(PropertyChangeListener listener) {
      support.removePropertyChangeListener(listener);
   }
   
   @Override
   public void removePropertyChangeListener(String propertyName,
         PropertyChangeListener listener) {
      support.removePropertyChangeListener(propertyName, listener);
   }

   @Override
   public boolean contains (String key) {
	   return key == null ? false : properties.containsKey(key);
   }
   
   /*      
   // TEST
   List list = Options.getStringList( "test.1" );
   if ( list != null )
   for ( Iterator it = list.iterator(); it.hasNext(); )
      System.out.println( "- TEST LIST 1 retrieve value: [" + it.next() + "]" );

   if ( GUIService.userConfirm( "REMOVE TEST LIST?" ))
   {
      Options.setStringList( "test.1", null );
   }
   
   if ( GUIService.userConfirm( "WRITE NEW TEST LIST?" ))
   {
      String[] arr = new String[] {"karina" };
      list = new ArrayList();
      for ( int i = 0; i < arr.length; i++ )
         list.add( arr[i] );
      Options.setStringList( "test.1", list );
   }
*/      

}