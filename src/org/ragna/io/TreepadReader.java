/*
*  File: TreepadReader.java
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

package org.ragna.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.StreamCorruptedException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Stack;

import org.ragna.core.DefaultOptions;
import org.ragna.core.DefaultPadDocument;
import org.ragna.core.Global;
import org.ragna.core.PadArticle;
import org.ragna.core.PadDocument;
import org.ragna.core.PadDocument.DocumentType;
import org.ragna.exception.UnknownFileFormatException;

import kse.utilclass.misc.Log;
import kse.utilclass.misc.UUID;

/**
 * Reads the Treepad 2.x file format and renders a pad-document.
 *
 */
public class TreepadReader implements DocumentReader {

    private boolean autoDetectHtmlArticles;
    private Charset encoding;


    /** Creates a new TreepadReader with the global default character encoding
     * for Treepad documents.
     * 
     * @throws IllegalCharsetNameException
     * @throws UnsupportedCharsetException
     */
    public TreepadReader () {
       this.autoDetectHtmlArticles = false;
       String hs = Global.getOptions().getOption("defaultTreepadEncoding");
       try {
    	   encoding = Charset.forName(hs);
       } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
    	   hs = DefaultOptions.get().getProperty("defaultTreepadEncoding", "UTF-8");
    	   encoding = Charset.forName(hs);
       }
    }

    /** Creates a new TreepadReader with a specified character encoding. If
     * the argument is null, the global default encoding for Treepad documents
     * is used. 
     * 
     * @param encoding String charset name, may be null
     * @throws IllegalCharsetNameException
     * @throws UnsupportedCharsetException
     */
    public TreepadReader (String encoding) {
       this();
       if (encoding != null) {
           if (!Global.isTreepadCompatibleCharset(encoding)) { 
        	   throw new IllegalArgumentException("encoding not compatible w/ Treepad: " + encoding);
           }
          this.encoding = Charset.forName(encoding);
       }
    }

    /** Creates a new TreepadReader with a specified character encoding. If
     * the argument is null, the global default encoding for Treepad documents
     * is used. The second parameter is currently not in use.
     * 
     * @param encoding String charset name, may be null
     * @param autoDetectHtmlArticles boolean
     * @throws IllegalCharsetNameException
     * @throws UnsupportedCharsetException
     */
    public TreepadReader (String encoding, boolean autoDetectHtmlArticles) {
       this(encoding);
       this.autoDetectHtmlArticles = autoDetectHtmlArticles;
    }

    @Override
    /** Reads a pad-document from the input stream which is formatted as
     * a TREEPAD 2.7 document file.
     * 
     * @param in InputStream
     * @return <code>PadDocument</code>
     * @throws UnknownFileFormatException if the format could not be recognised initially
     * @throws StreamCorruptedException if the stream contained false formatting
     * @throws IOException
     */
    public PadDocument read (InputStream in) throws IOException {
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(in, encoding));
        Stack<PadArticle> stack = new Stack<PadArticle>();
        
        // check first line for TREEPAD file format
        String line = reader.readLine();
        if ( line == null || line.toLowerCase().indexOf("treepad version 2.7") == -1 ) 
           throw new UnknownFileFormatException("not a Treepad 2.7 file!");

        // read an optional UUID value
        UUID uuid = null;
        int index = line.toLowerCase().indexOf("uuid="); 
        if (index > -1) {
           String ustr = line.substring(index+5, index+37);
           uuid = new UUID(ustr);
        }
        String hstr = "(TreepadReader) document has no identifier (UUID)";
        if (uuid != null) {
        	hstr = "(TreepadReader) document identified UUID=" + uuid.toHexString();
        }
        Log.debug(4, hstr);
        
        // create a pad document
        PadDocument document = new DefaultPadDocument(DocumentType.TreePad, uuid);
        document.setEncoding(getEncoding());
        boolean docTitleIsSet = false;
      
        int depthMarker = -1;
        String dtLine, nodeLine, titleLine, depthLine, currentLine;
        StringBuffer currentContent;
        PadArticle parent = null;

        // iterate over all article definitions in source text
        while ((dtLine = reader.readLine()) != null
            && (nodeLine = reader.readLine()) != null && (titleLine = reader.readLine()) != null
            && (depthLine = reader.readLine()) != null)
        {
           boolean depthBroken = false;
           boolean contentTerminated = false;
           currentContent = new StringBuffer();
           int depthMarkerDelta = 0;
           try {
              // verify the depth line content
              int oldDepth = depthMarker;
              depthMarker = Integer.parseInt(depthLine);
              depthMarkerDelta = depthMarker - oldDepth;
              
              // read the article content (loop until we find "<end node> 5P9i0s8y19Z")
              while (!contentTerminated && (currentLine = reader.readLine()) != null) {
                 contentTerminated = currentLine.startsWith("<end node> 5P9i0s8y19Z");
                 if (!contentTerminated) {
                    currentContent.append(currentLine);
                    currentContent.append("\n");
                 }
              }
              
           } catch (Exception e) {
              depthBroken = true;
           }

           // probe for proper article format
           boolean brokenFormat =
              // verify "dt=text" 
              !dtLine.toLowerCase().equals("dt=text") ||
              // verify "<node>" 
              !nodeLine.toLowerCase().startsWith("<node>") ||
              // verify DEPTH value    
              depthBroken ||   
              // verify content is terminated with the "<end node>" marker
              !contentTerminated;
            
           // throw StreamCorruptedException if file format is found broken 
           if (brokenFormat) {
               hstr = "the Treepad file is not version-2.7-compatible"; 
               throw new StreamCorruptedException(hstr);
            }

           // TODO establish two article types (plain + html) 
           // Turn it into a HTML-mode node if it matches "<html> ... </html>"
//           String compareContent = newNode.getContent().toLowerCase().trim();
//           int newArticleMode = (autoDetectHtmlArticles && compareContent.startsWith("<html>") && compareContent
//               .endsWith("</html>")) ? JreepadArticle.ARTICLEMODE_HTML : JreepadArticle.ARTICLEMODE_ORDINARY;
//           newNode.getArticle().setArticleMode(newArticleMode);

            // create a new pad-article for the document
            // "parent" is the previous article (0 and 1 depth delta) 
            // or retrieved from stack (negative depth delta)
            // "isChild" is decided by delta == 1
            // the rest of Tree definition does the logic in PadDocument
            String content = currentContent.substring(0, Math.max(currentContent.length()-1, 0));
            boolean isChild = depthMarkerDelta == 1;
            
            // pop parent stack for back-step of depth-order
            if (depthMarkerDelta < 0) {
               // find latest article to match the required parent depth
               for (int i = depthMarkerDelta; i < 0; i++) {
                  parent = stack.pop();
                  Log.debug(10, "(TreepadReader) popped article from STACK : " + parent); 
               }
            }

            // file format error check related to depth value (not > 1)
            else if (depthMarkerDelta > 1) {
               // depth delta of larger than 1 detected (illegal)
               hstr = "illegal article depth step, delta = ".concat(String.valueOf(depthMarkerDelta)); 
               throw new StreamCorruptedException(hstr);
            }

            // create new article object including its tree order
            PadArticle article = document.newArticle(parent, isChild);
            article.setContent(content);
            article.setTitle(titleLine);
            Log.debug(10, "(TreepadReader) creating ARTICLE parent=" + 
                  parent + ", child=" + isChild + ", title=" + article + ", delta=" + depthMarkerDelta);
            article.getPropertySerial();
            
            // update PARENT reference
            if (depthMarkerDelta == 1) {
               if (parent != null) {
                  stack.push(parent);
                  Log.debug(10, "(TreepadReader) pushing article to STACK : " + parent); 
               }
            }
            parent = article;
            
            // control depth-marker 0 uniqueness and set document title
            if (depthMarker == 0) {
                if (!docTitleIsSet) {
                   if (!titleLine.isEmpty()) {
                      document.setTitle(titleLine);
                      document.setShortTitle(titleLine);
                   }
                   docTitleIsSet = true;
                } else {
                   hstr = "illegal multiple occurrence of DEPTH 0"; 
                   throw new StreamCorruptedException(hstr);
                }
            } 
        }

        document.resetModified();
        return document;
    }

    public boolean isAutoDetectHtmlArticles() {
        return autoDetectHtmlArticles;
    }

    public void setAutoDetectHtmlArticles(boolean autoDetectHtmlArticles) {
        this.autoDetectHtmlArticles = autoDetectHtmlArticles;
    }

    public String getEncoding() {
        return encoding.name();
    }

}
