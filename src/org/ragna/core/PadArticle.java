/*
*  File: PadArticle.java
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

package org.ragna.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;

import javax.swing.text.Document;
import javax.swing.tree.TreePath;

import kse.utilclass.misc.UUID;

/**
 * <p><b>Property Change Events</b>
 * <p>A PadArticle dispatches property change events to registered <code>
 * PropertyChangeListener</code> classes.
 * <br>The following events are issued.
 * <table width=650>
 * <tr><th align=left>KEY</th><th align=left>VALUE NEW</th><th align=left>VALUE OLD</th><th align=left>EVENT</th></tr>
 * <tr><td>contentModified</td><td><code>PadArticle</code></td><td><code>null</code></td><td>editor-document was modified</td></tr>
 * <tr><td>uuidChanged</td><td><code>PadArticle</code></td><td><code>UUID</code></td><td>UUID identifier changed</td></tr>
 * <tr><td>titleChanged</td><td><code>PadArticle</code></td><td><code>null</code></td><td>title value changed</td></tr>
 * <tr><td>dataTypeChanged</td><td><code>PadArticle</code></td><td><code>null</code></td><td>new type of editor-document was selected</td></tr>
 * <tr><td>layoutChanged</td><td><code>PadArticle</code></td><td><code>null</code></td><td>some setting for GUI layout changed</td></tr>
 * <tr><td>selectionChanged</td><td><code>PadArticle</code></td><td><code>null</code></td><td>text selection changed (programmatically)</td></tr>
 * <tr><td>orderChanged</td><td><code>PadArticle</code></td><td><code>null</code></td><td>relation of article to other articles changed</td></tr>
 * 
 * </table> 
 */
public interface PadArticle extends Comparable<PadArticle> {

   enum ArticleType { PlainText, HTMLText };
   
   /** Returns the identifier for this article.
    * 
    * @return UUID
    */
   UUID getUUID ();
   
   /** Returns the human readable title for this article.
    * This may be a longer not-null expression.
    * 
    * @return String title
    */
   String getTitle ();

   /** Sets the human readable title for this article.
    * This may be a longer not-null expression.
    * 
    * @param title String
    */
   void setTitle (String title);
   
   /** Returns the human readable short title for this article.
    * The short title is an abbreviation of the full title available at 
    * <code>getTitle()</code>.
    * 
    * @return String short title
    */
   String getShortTitle ();

   /** Sets the text content of this article.
    * 
    * @param text String text
    */
   void setContent (String text);
   
   /** Returns the text content of this article; an empty string if 
    * no content is defined.
    * 
    * @return String text
    */
   String getContent ();
   
   /** Returns a short description about this article
    * to be used as tooltip text in the display. Returns null
    * if not defined.
    *  
    * @return String or null
    */
   String getToolTipText ();
   
   /** Sets a short description for this article
    * to be used as tooltip text in the display. Apply null
    * for undefined.
    *  
    * @param tooltip String tooltip text, may be null 
    */
   void setToolTipText (String tooltip);

   /** Returns the editable document of this pad-document. If no content is 
    * defined, a valid neutral value is returned.
    *  
    * @return <code>javax.swing.text.Document</code>
    */
   Document getEditorDocument ();

   /** Sets the editable document for this pad-document. Null may be used
    * to set an empty document.
    *  
    * @param document <code>javax.swing.text.Document</code>, may be null
    */
   void setEditorDocument (Document document);
   
   /** Returns the pad-document to which this article is a member or null
    * if this article is not a member of any document.
    * 
    * @return <code>PadDocument</code> or null
    */
   PadDocument getDocument ();

   /** Returns the order relation value of this article as the 
    * related parent article or null if this article is the root node.
    *  
    * @return PadArticle order parent article or null if root node
    */
   PadArticle getParent ();

   /** Whether this article is the root article in its enclosing document.
    * 
    * @return boolean true == is root, false = is not root
    */
   default boolean isRoot () {return getParent() == null;} 
   
   /** Returns the depth value of this article in a Graph (or Tree) ordering
    * of the document. The root element has value zero and the child nodes
    * of any node have the value of parent + 1. The value is zero if this
    * article is not member of a document.
    *  
    * @return int depth value
    */
   int getOrderDepth ();
   
   /** Sets the order relation value for this article as  
    * related parent article. The parameter may be null if this article is
    * the root node. 
    *  
    * @param parent PadArticle parent article or null
    */
   void setParent (PadArticle parent);
   
   /** Sets the pad-document to which this article is a member or null
    * if this article is marked not a member of any document. If this
    * article was a member of another document, it is automatically removed
    * from the previous document. However and notably, at this point it is 
    * not added to the given document!
    * 
    * @param padDocument <code>PadDocument</code> new enclosing document 
    *                    or null
    */
   void setDocument (PadDocument padDocument);
   
   /** Returns the content type of this article.
    *   
    * @return <code>ArticleType</code>
    */
   ArticleType getArticleType ();
   
   /** Returns this article's listener for a JTextComponent's property change
    * support.
    * 
    * @return {@code PropertyChangeListener}
    */
   PropertyChangeListener getEditorListener ();
   
   /** Returns an 8-character hexadecimal expression which identifies the
    * TreePath of this article in its document, or null if this article is
    * not a member of a document.
    * 
    * @return String (8 char) or null
    */
   String getPathID ();
   
   /** Returns the index position of this article in its document or -1
    * if this article is not a member of a document.
    * 
    * @return int index position or -1
    */
   int getIndexPosition();
   
   /** The last noted cursor position for this article as a measure of 
    * text length. If no position has been noted, 0 is returned.
    * 
    * @return int cursor position
    */
   int getCursorPosition ();
   
   /** Returns a text selection MARK present in the editor.
    * If the mark is different to CURSOR it defines a current selection
    * range in the distance between mark and cursor. 
    * <p>This is a persistence value and not necessarily identical with the 
    * current text selection in a corresponding editor pane!
    *   
    * @return int MARK text position (0..n, n == text-length-1) 
    */
   int getSelectionMark ();
   
   /** Notifies this article about a text selection MARK present in the editor.
    * If the mark is different to CURSOR it defines as current selection
    * range in the distance between mark and cursor. 
    *   
    * @param mark int text position (0..n, n == text-length-1) 
    */
   void setSelectionMark (int mark);
   
   /** Returns the current text selection range w/ Dimension.width = start
    * and Dimension.height = end position. Start is lower than end.
    * If there is no selection, null is returned.
    * 
    * @return {@code Dimension} or null
    */
   Dimension getSelection ();
   
   /** Whether line wrapping occurs in the editor.
    * 
    * @return boolean true == line wrapping, false == no line wrapping
    */
   boolean getLineWrap ();
   
   /** Sets whether line wrapping will occur in the editor.
    * 
    * @param v boolean
    */
   void setLineWrap (boolean v);
   
   /** Returns the base text font for this article.
    * 
    * @return <code>Font</code> or null if no font is defined
    */
   Font getDefaultFont ();
   
   /** Returns the editor background colour for this article.
    * 
    * @return <code>Color</code> or null if no colour is defined
    */
   Color getBackgroundColor ();
   
   /** Returns the foreground colour for this article.
    * 
    * @return <code>Color</code> or null if no colour is defined
    */
   Color getForegroundColor ();
   
   /** Sets the base text font for this article.
    * 
    * @param font <code>Font</code> default text font, may be null
    */ 
   void setDefaultFont (Font font);
   
   /** Sets the editor background colour for this article.
    * 
    * @param c <code>Color</code> background colour, may be null
    */ 
   void setBackgroundColor (Color c);

   /** Sets the editor foreground colour for this article.
    * 
    * @param c <code>Color</code> background colour, may be null
    */ 
   void setForegroundColor (Color c);

   /** Sets the last cursor position known in the text.
    * 
    * @param position int cursor position
    */
   void setCursorPosition (int position);
   
   /** Returns the visible Rectangle of the editor scroll-screen as it was
    * last reported by {@code setVisibleRect()}.
    * 
    * @return Rectangle editor visible rectangle
    */
   Rectangle getVisibleRect ();
   
   /** Sets the visible Rectangle of the editor scroll-screen.
    * 
    * @param rect Rectangle editor visible rectangle
    */
   void setDocumentVisibleRect (Rectangle rect);
   
   
   /** Returns a copy of this article featuring a new UUID.
    * External system references (parent document, parent article, etc.) are
    * kept in place, the text document is also copied.
    * <p>The call causes activity in the underlying database and
    * new resources are allocated for a new article which shows the same
    * content and settings as this article. However, the returned copy is
    * an article in its own state and subsequent modifications to it do not 
    * strike through to the origin.
    *  
    * @return <code>PadArticle</code>
    * @throws ResourceAllocationException
    */
   PadArticle copy ();

   /** Renders the short-title of this article as value.
    * 
    * @return String
    */
   @Override
   String toString ();

   void addPropertyChangeListener (PropertyChangeListener listener);

   void removePropertyChangeListener (PropertyChangeListener listener);

   void addPropertyChangeListener (String property,
         PropertyChangeListener listener);

   void removePropertyChangeListener (String property,
         PropertyChangeListener listener);

   /** Returns the {@code TreePath} identifying this article if it is an element
    * of a document or null otherwise. This can be used as a test whether this
    * article is an element of a pad-document.
    * 
    * @return {@code TreePath} or null
    */
   TreePath getTreePath();

   /** Whether line-wrapping uses entire words to break a line. By default this
    * is true.
    * 
    * @return boolean true = break entire words, false = boundary break
    */
   boolean getWrapStyleWord();

   /** Returns a serialisation of the modifiable properties of this article
    * (other than the text).
    * 
    * @return String an OptionBag serialisation
    */
   String getPropertySerial ();
   
   /** Defines the modifiable properties of this article (other than the text)
    * by interpreting a serialisation value which was obtained before through
    * method 'getPropertySerial()'. Properties not contained in the 
    * serialisation keep their current value.
    * 
    * @param serial String an OptionBag serialisation
    */
   void putPropertySerial (String serial);

   /** Sets the UUID identifier for this article.
    * 
    * @param uuid {@code UUID}
    */
   void setUUID (UUID uuid);
   
}
