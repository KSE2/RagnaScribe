/*
*  File: DefaultOptions.java
* 
*  Project Ragna Scribe
*  @author Wolfgang Keller
*  Created 
* 
*  Copyright (c) 2022 by Wolfgang Keller, Munich, Germany
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

import java.util.Properties;

/** This class defines the default values and names of SYSTEM OPTIONS 
 * (as available through Gobal.getOptions()).
 */
public class DefaultOptions extends Properties {
   
   private static DefaultOptions instance = new DefaultOptions();
   
   public static final DefaultOptions get () {
      return instance;
   }
   
   DefaultOptions ()   {
      init();
   }

   private void init () {
      setProperty("hasMainToolbar", "true");
      setProperty("hasTreeToolbar", "true");
      setProperty("hasArticleToolbar", "true");
      setProperty("isToolbarsFloatable", "false");
      setProperty("useDefaultFileExtensions", "true");
      setProperty("useFileHistory", "true");
      setProperty("useMirroring", "true");
      setProperty("defaultEditorLinewrap", "true");
      setProperty("showFilePathInsteadOfTitle", "false");
      setProperty("isAutoOpenRecentFile", "true");
      setProperty("isAutoOpenSession", "false");

      setProperty("defaultSliderPosition", "300");
      setProperty("defaultDocumentType", "html");
      setProperty("defaultDocumentDisplayModus", "CompleteDisplay");
      setProperty("defaultTreepadEncoding", "UTF-8");
      setProperty("maxShortTitleLength", "60");
      setProperty("workerThreads", "2");
      setProperty("startSearchExp", "https://www.google.com/search?q=$text&hl=en");
      setProperty("startTranslateExp", "https://www.babelfish.de");
      
      setProperty("printHeaders", "false");
      setProperty("printIgnoreEmpty", "true");
      setProperty("printLineWrap", "true");
      setProperty("printOutlineTitles", "true");
      setProperty("rememberScreen", "true");

   }
}
