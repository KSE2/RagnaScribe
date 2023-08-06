/*
*  File: CommandlineHandler.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kse.utilclass.misc.Log;


public class CommandlineHandler {
	
   /** Signal that option code starts with */
   public static final String OPTION_SIGNAL = "-";
   /** Command-line parameter organisation (leading, trailing, mixed) */
   public enum Organisation { leading, trailing, mixed } 

   private Map<String, String> options = new HashMap<String, String>();
   private ArrayList<String> arguments = new ArrayList<String>();
   private ArrayList<String> unitaries = new ArrayList<String>();

   private Organisation organisation = Organisation.mixed;
   private Locale locale;
   private String[] args = new String[0];

   String languageArg;
   String countryArg;

   /** Creates a new commandline handler in MIXED organisation
    * and without initial argument digestion.
    */
   public CommandlineHandler () {
   }

   /** Creates a new commandline handler with the given organisation
    * and without initial argument digestion.
    * 
    * @param org <code>Organisation</code> argument organisation in the line 
    *        (null for default)
    */
   public CommandlineHandler ( Organisation org ) {
      if ( org != null ) {
         organisation = org;
      }
   }

   /** Creates a new commandline handler with a MIXED organisation
    * and initial argument digestion.
    * 
    * @param args String[] arguments
    * @throws IllegalStateException if commandline is organised badly
    */
   public CommandlineHandler ( String[] args ) {
      this( args, null );
   }
   
   /** Creates a new commandline handler with the given organisation
    *  (defaults to MIXED).
    * 
    * @param args String[] arguments
    * @param org <code>Organisation</code> argument organisation in the line 
    *        (null for default)
    * @throws IllegalStateException if commandline is organised badly
    */
   public CommandlineHandler ( String[] args, Organisation org ) {
      if ( args == null )
         throw new IllegalArgumentException( "args is null" );
   
      this.args = args;
      if ( org != null ) {
         this.organisation = org;
      }
      digest( args );
   }
   
/** Sets the list of unitary options in a text string
 * with values separated by blanks.
 * (Unitary options are option codes which don't demand a 
 * following value argument.)
 * 
 * @param list String blank separated option codes
 */
public void setUnitaryOptions ( String list ) {
   String[] vs = list.split(" ");
   unitaries.clear();
   for (String s : vs) {
      unitaries.add(s);
   }
}

public void reset () {
   arguments.clear();
   options.clear();
   unitaries.clear();
   args = new String[0];
}

/** The start argument commandline (concatenation of CL-arguments).
 * 
 * @return String, startup argument commandline or empty string if no arguments
 */
public String getCommandline () {
   StringBuffer b = new StringBuffer();
   for ( String a : args ) {
      b.append( a.concat( " " ) );
   }
   return b.toString();
}

/** Returns a copy of the array of commandline arguments
 * as stated by the caller.
 * 
 * @return String[]
 */
public String[] getOriginalArgs () {
   return Arrays.copyOf(args, args.length);
}

/** Returns a list of the PLAIN arguments of the commandline
 * in the order of their appearance. PLAIN arguments are
 * commandline arguments that are neither an option code
 * nor an option value.
 * 
 * @return <code>List</code> of strings
 */
@SuppressWarnings("unchecked")
public List<String> getArguments () {
   return (List<String>) arguments.clone();
}

/** Returns the option value for the option code supplied
 * or <b>null</b> if no value was supplied for this option
 * in the commandline.
 * 
 * @param code String option code including signal (e.g. "--verify")
 * @return String option value
 */
public String getOption ( String code ) {
   return options.get(code);
}

/** Returns whether the given option code has been stated in
 * the command line arguments.
 * 
 * @param code String option code
 * @return boolean
 */
public boolean hasOption ( String code ) {
   return options.containsKey( code );
}

/**
 * Digests a set of arguments. (This replaces any previous
 * interpretations.)
 * 
 * @param args String[] all arguments
 * @throws IllegalStateException if list of arguments is not
 *         correctly organised
 */
public void digest (  String[] args ) {
   String code=null;
   boolean startsPlain=false, startsOption=false;

   options.clear();
   arguments.clear();
   
   // fill our databases
   for ( String a : args ) {
      
      // react to new option code
      if ( a.startsWith(OPTION_SIGNAL) ) {
         // control line org
         if ( organisation == Organisation.leading & startsPlain ) 
            throw new IllegalStateException("improper command line organisation: ".concat(a));
         
         // if have previous option code (non-unitary)
         if ( code != null ) {
            Log.debug(8, "(CommandlineHandler.digest) ignoring NULL-OPTION: ".concat(code));
         } 

         code = a;
         startsOption = true;

         // if new code is unitary option
         if ( unitaries.contains(code) ) {
            options.put(code, null);
            Log.debug(8, "(CommandlineHandler.digest) identified UNITARY-OPTION: ".concat(code));
            code = null;
         }
      }

      // react to new value
      else {
         // control line org
         if ( organisation == Organisation.trailing & startsOption & code == null ) 
            throw new IllegalStateException("improper command line organisation: ".concat(a));
         
         // if have previous option code
         if ( code != null ) {
            options.put(code, a);
            Log.debug(8, "(CommandlineHandler.digest) identified VALUE-OPTION: " + code + " == " + a);
         }
         // if plain value (not option bound)
         else {
            arguments.add(a);
            Log.debug(8, "(CommandlineHandler.digest) identified ARGUMENT: ".concat(a));
         }
         code = null;
         startsPlain = true;
      }
   }

   // make a standard interpretation for a Locale
   languageArg = getOption( "-l" );
   countryArg = getOption( "-c" );
}

/** Returns a locale representing any locale specific arguments
 * the caller may have supplied. Unspecified elements of the 
 * newly created locale are substituted from the VM default locale.
 * 
 * @return <code>Locale</code> locale implied by arguments 
 *         or <b>null</b> if no locale relevant argument was supplied
 */
public Locale getLocale () {
   if (locale != null) return locale;
   
   // if we have some locale arguments, construct a new locale
   if ( languageArg != null | countryArg != null ) {
      locale = new Locale( 
            languageArg == null ? Locale.getDefault().getLanguage() : languageArg,
            countryArg == null ? Locale.getDefault().getCountry() : countryArg  );
   }
   return locale;
}


}

