/*
*  File: TreepadWriter.java
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Iterator;

import org.ragna.core.AbstractPadArticle;
import org.ragna.core.PadArticle;
import org.ragna.core.PadDocument;

/**
 * Writes a pad-document as a Treepad file.
 *
 */
public class TreepadWriter implements DocumentWriter {
	
    private Charset encoding = Charset.defaultCharset();

    /**
     * Creates a new Treepad Writer with the VM default character encoding.
     *  
     */
    public TreepadWriter () {
    }

    /**
     * Creates a new Treepad Writer with an encoding charset.
     *  
     * @param encoding String charset name or null for default
     * @throws IllegalCharsetNameException
     * @throws UnsupportedCharsetException
     */
    public TreepadWriter (String encoding) {
       if (encoding != null) {
          this.encoding = Charset.forName(encoding);
       }
    }

    @Override
    public void write (OutputStream out, PadDocument document)
        throws IOException
    {
        Writer writer = new OutputStreamWriter(out, encoding);
        writer.write("<Treepad version 2.7 UUID=" + 
              document.getUUID().toHexString() + ">\n");

        // depending on whether there are articles in the document ..
        if (document.getArticleCount() > 0) {
           // write the list of articles
           for (PadArticle article : document ) {
              writeArticle(writer, article, article.getOrderDepth());
           }
        } else {
           // write a fictional root article
           PadArticle root = new AbstractPadArticle();
           root.setTitle(document.getTitle());
           writeArticle(writer, root, 0);
        }
        
        writer.flush();
    }

    private void writeArticle (Writer writer, PadArticle article, int depth)
        throws IOException
    {
        writer.write("dt=Text\n<node>\n");
        writer.write(article.getTitle());
        writer.write("\n");
        writer.write(depth + "\n");  // TODO article.getLayer()
        String content = article.getContent();
        writer.write(content);
        if (!content.isEmpty()) {
           writer.write("\n");
        }
        writer.write("<end node> 5P9i0s8y19Z\n");
    }
}
