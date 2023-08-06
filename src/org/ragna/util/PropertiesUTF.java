/*
*  File: PropertiesUTF.java
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *  This class is an extension of <code>java.util.Properties</code> that allows
 *  different character encodings for persistent files. Additional
 *  to the features of <code>Properties</code>, which are all present, 
 *  it allows to store and load property files that are encoded
 *  in any available character encoding with specialised load and store
 *  functions. As a compensation, the provision for Unicode escaping is not present
 *  in these functions.
 */
public class PropertiesUTF extends Properties
{

   private static final String whiteSpaceChars = " \t\r\n\f";
   private static final String keyValueSeparators = "=: \t\r\n\f";
   private static final String strictKeyValueSeparators = "=:";
   private static final String specialSaveChars = "=: \t\r\n\f#!";



/**
 * 
 */
public PropertiesUTF ()
{
}

/**
 * @param defaults
 */
public PropertiesUTF ( Properties defaults )
{
   super( defaults );
}

public void setDefaults ( Properties def )
{
   defaults = def;
}

/*
 * Returns true if the given line is a line that must
 * be appended to the next line
 */
private boolean continueLine(String line) 
{
    int slashCount = 0;
    int index = line.length() - 1;
    while ((index >= 0) && (line.charAt(index--) == '\\'))
        slashCount++;
    return (slashCount % 2 == 1);
}

   /**
     * Reads a property list (key and element pairs) from the input
     * stream.  The stream is assumed to be using the 
     * character encoding as specified by the <code>charset</code> parameter.
     *
     * @param      inStream the input stream.
     * @param      charset the character set
     * @exception  IOException  if an error occurred when reading from the
     *               input stream.
     */
    public synchronized void load(InputStream inStream, String charset ) throws IOException {
   
        BufferedReader in;
        String key, value, nextLine, loppedLine;
        int len, keyStart, startIndex, separatorIndex, valueIndex;
        char currentChar;
        
        in = new BufferedReader(new InputStreamReader(inStream, charset));
        while (true) {
            // Get next line
            String line = in.readLine();
            if (line == null)
                return;
   
            if (line.length() > 0) {
                
                // Find start of key
                len = line.length();
                for (keyStart=0; keyStart<len; keyStart++)
                    if (whiteSpaceChars.indexOf(line.charAt(keyStart)) == -1)
                        break;
   
                // Blank lines are ignored
                if (keyStart == len)
                    continue;
   
                // Continue lines that end in slashes if they are not comments
                char firstChar = line.charAt(keyStart);
                if ((firstChar != '#') && (firstChar != '!')) 
                {
                    while (continueLine(line)) 
                    {
                        nextLine = in.readLine();
                        if (nextLine == null)
                            nextLine = "";
                        loppedLine = line.substring(0, len-1);

                        // Advance beyond whitespace on new line
                        for (startIndex=0; startIndex<nextLine.length(); startIndex++)
                            if (whiteSpaceChars.indexOf(nextLine.charAt(startIndex)) == -1)
                                break;
                        nextLine = nextLine.substring(startIndex,nextLine.length());
                        line = new String(loppedLine+nextLine);
                        len = line.length();
                    }
   
                    // Find separation between key and value
                    for (separatorIndex=keyStart; separatorIndex<len; separatorIndex++) 
                    {
                        currentChar = line.charAt(separatorIndex);
                        if (currentChar == '\\')
                            separatorIndex++;
                        else if (keyValueSeparators.indexOf(currentChar) != -1)
                            break;
                    }
   
                    // Skip over whitespace after key if any
                    for (valueIndex=separatorIndex; valueIndex<len; valueIndex++)
                        if (whiteSpaceChars.indexOf(line.charAt(valueIndex)) == -1)
                            break;

                    // Skip over one non whitespace key value separators if any
                    if (valueIndex < len)
                        if (strictKeyValueSeparators.indexOf(line.charAt(valueIndex)) != -1)
                            valueIndex++;

                    // Skip over white space after other separators if any
                    while (valueIndex < len) {
                        if (whiteSpaceChars.indexOf(line.charAt(valueIndex)) == -1)
                            break;
                        valueIndex++;
                    }

                    // extract key and value     
                    key = line.substring(keyStart, separatorIndex);
                    value = (separatorIndex < len) ? line.substring(valueIndex, len) : "";
                    
                    // Convert then store key and value
                    key = loadConvert(key);
                    value = loadConvert(value);
                    put(key, value);
                }
            }
   }
    }

    private static void writeln(BufferedWriter bw, String s) throws IOException 
    {
       bw.write(s);
       bw.newLine();
   }

   /**
     * Writes the property list (key and element pairs) in this
     * <code>Properties</code> table to the output stream in a format suitable
     * for loading into a <code>Properties</code> table using the
     * {@link #load(InputStream,String) load} method.
     * The stream is written using the character encoding specified by parameter
     * <code>charset</code>. Output mappings are sorted to the natural order
     * of their key values. 
     * <p>
     * After the entries have been written, the output stream is flushed.  The
     * output stream remains open after this method returns.
     *
     * @param   out      an output stream.
     * @param   header   a description of the property list (may be <b>null</b>)
     * @exception  IOException if writing this property list to the specified
     *             output stream throws an <tt>IOException</tt>.
     * @exception  ClassCastException  if this <code>Properties</code> object
     *             contains any keys or values that are not <code>Strings</code>.
     * @exception  NullPointerException  if <code>out</code> is null.
     */
    public synchronized void store( OutputStream out, String header, String charset )
    throws IOException
    {
       SortedMap<Object, Object> sortMap;
       Map.Entry<Object, Object> entry;
       BufferedWriter awriter;
       String key, val;
       
       // prepare sorted map
       sortMap = new TreeMap<Object, Object>( this );
       
       awriter = new BufferedWriter(new OutputStreamWriter(out, charset));
       if (header != null)
           writeln(awriter, "#" + header);
        
       writeln(awriter, "#" + new Date().toString());
       for ( Iterator<Map.Entry<Object, Object>> it = sortMap.entrySet().iterator(); it.hasNext(); ) 
       {
          entry = it.next();
          key = (String)entry.getKey();
          val = (String)entry.getValue();
          key = saveConvert(key, true);
   
       /* No need to escape embedded and trailing spaces for value, hence
        * pass false to flag.
        */
           val = saveConvert(val, false);
           writeln(awriter, key + "=" + val);
       }
       awriter.flush();
    }

   /*
     * Converts 
     * and writes out any of the characters in specialSaveChars
     * with a preceding slash
     */
    private String saveConvert(String theString, boolean escapeSpace) 
    {
       StringBuffer outBuffer;
       int x, len; 
       char aChar;
        
        len = theString.length();
        outBuffer = new StringBuffer(len*2);
   
        for( x=0; x<len; x++ ) 
        {
            aChar = theString.charAt(x);
            switch (aChar) {
            	case ' ':
            	    if (x == 0 || escapeSpace) 
                      outBuffer.append('\\');
            
            	    outBuffer.append(' ');
            	    break;
                case '\\':outBuffer.append('\\'); outBuffer.append('\\');
                          break;
                case '\t':outBuffer.append('\\'); outBuffer.append('t');
                          break;
                case '\n':outBuffer.append('\\'); outBuffer.append('n');
                          break;
                case '\r':outBuffer.append('\\'); outBuffer.append('r');
                          break;
                case '\f':outBuffer.append('\\'); outBuffer.append('f');
                          break;
                default:
                        if (specialSaveChars.indexOf(aChar) != -1)
                            outBuffer.append('\\');
                        outBuffer.append(aChar);
            }
        }
        return outBuffer.toString();
    }

   /*
     * Converts 
     * and changes special saved chars to their original forms
     */
    private String loadConvert(String theString) {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len);
   
        for (int x=0; x<len; ) 
        {
            aChar = theString.charAt(x++);
            if (aChar == '\\') 
            {
                aChar = theString.charAt(x++);
                if (aChar == 't') aChar = '\t';
                else if (aChar == 'r') aChar = '\r';
                else if (aChar == 'n') aChar = '\n';
                else if (aChar == 'f') aChar = '\f';
                outBuffer.append(aChar);
            } else
                outBuffer.append(aChar);
        }
        return outBuffer.toString();
    }
}
