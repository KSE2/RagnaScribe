/*
*  File: PadDocument.java
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
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Iterator;

import javax.swing.AbstractListModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;

import org.ragna.front.DisplayManager.DisplayModus;
import org.ragna.io.IO_Manager.SystemFileType;
import org.ragna.util.PersistentOptions;

import kse.utilclass.misc.UUID;

/**
 * <p><b>Property Change Events</b>
 * <p>A PadDocument dispatches property change events to registered <code>
 * PropertyChangeListener</code> classes.
 * <br>The following events are issued.
 * <table width=650>
 * <tr><th align=left>KEY</th><th align=left>VALUE NEW</th><th align=left>EVENT</th></tr>
 * <tr><td>articleAdded</td><td><code>PadArticle</code></td><td>pad-article was added</td></tr>
 * <tr><td>articleRemoved</td><td><code>PadArticle</code></td><td>pad-article was removed</td></tr>
 * <tr><td>articleTitleChanged</td><td><code>PadArticle</code></td><td>pad-article title value changed</td></tr>
 * <tr><td>articleModified</td><td><code>PadArticle</code></td><td>pad-article editable content modified</td></tr>
 * <tr><td>titleChanged</td><td><code>null</code></td><td>document title value changed</td></tr>
 * <tr><td>documentModified</td><td><code>Boolean</code></td><td>document changed its MODIFIED state</td></tr>
 * <tr><td>selectionChanged</td><td><code>UUID, null</code></td><td>new selection of pad-article (singular)</td></tr>
 * <tr><td>typeChanged</td><td><code>DocumentType</code></td><td>setting for document type changed</td></tr>
 * <tr><td>focusChanged</td><td>"order-view" or "article"</td><td>setting for display focus changed</td></tr>
 * <tr><td>displayModusChanged</td><td><code>DisplayManager. DisplayModus</code></td><td>setting for display modus changed</td></tr>
 * <tr><td>layoutChanged</td><td><code>String</code></td><td>some base layout value like Font etc. changed</td></tr>
 * 
 * </table> 
 */
public interface PadDocument extends Iterable<PadArticle> {
   
   enum DocumentType { TreePad, Ragna };
   
   /** Returns the human readable title for this document.
    * This may be a longer expression.
    * 
    * @return String title
    */
   String getTitle ();

   /** Sets the title for this document. If the title changed a
    * "titleChanged" property change event is issued.
    * 
    * @param title String, may be null
    */
   void setTitle (String title);
   
   /** Sets the short title for this document. If the short-title changed a
    * "titleChanged" property change event is issued.
    * 
    * @param title String, may be null
    * @return boolean true = short title changed, false = short title unchanged 
    */
   boolean setShortTitle (String title);
   
   /** Returns the human readable short title for this document
    * or null if no short title has been defined.
    * 
    * @return String short title or null
    */
   String getShortTitle ();
   
//   /** Returns a gross estimation of this document about its expected 
//    * serialisation size. This can be larger or smaller than the real size
//    * when serialised. The value is used for categorical algorithmic decisions.
//    * 
//    * @return long expected serialisation size
//    */
//   long getSizeHint ();
   
   /** Returns the path definition of a serialisation file for  this document
    * or null if no such path is defined.
    * 
    * @return String external filepath or null
    */
   String getExternalPath ();
   
   /** Returns a short one-line description about this document
    * to be used as tooltip text in the display. Returns null
    * if not defined.
    *  
    * @return String or null
    */
   String getToolTipText ();

   /** Returns the identifier for this document.
    * 
    * @return UUID
    */
   UUID getUUID ();
   
   /** Whether the given article has children in the order-tree.
    * 
    * @param article {@code PadArticle}
    * @return boolean true = has children, false = is leaf
    * @throws IllegalArgumentException if article is unknown in document
    */
   boolean hasChildren (PadArticle article);
   
   /** Sets the given UUID value for this document.
    * 
    * @param uuid {@code UUID}
    */
   void setUUID (UUID uuid);

   /** Returns the identifier of the article which was last selected in
    * the document's article tree or listing view.
    *  
    * @return {@code PadArticle} selected article or null if undefined
    */
   PadArticle getSelectedArticle ();
   
   /** Returns the index value of the currently selected article or -1 if
    * no article is selected.
    *  
    * @return int article index or -1 if undefined
    */
   int getSelectedIndex ();
   
   /** Whether this document is modified.
    * 
    * @return boolean
    */
   boolean isModified ();
   
   /** Whether this document has no articles.
    * 
    * @return boolean true = no articles, false = at least one article
    */
   default boolean isEmpty () {return getArticleCount() == 0;}
   
   /** Whether this document has an external serialisation file definition 
    * associated (via IO_Manager).
    *  
    * @return boolean
    */
   default boolean hasExternalFile () {return getExternalPath() != null;}

   /** Called to reset the MODIFIED property of this document. (This will
    * cause a property change event if the document was MODIFIED.)
    */
   void resetModified ();
   
   /** Sets this document into MODIFIED status.
    */
   void setModified ();

   /** Whether this file is encrypted on its external storage.
    * 
    * @return boolean true = encrypted, false = not encrypted
    */
   boolean isEncrypted ();
   
   /** Whether this document is a BACKUP of another document. BACKUP forms
    * a category of documents which receive special treatment in some display
    * and IO contexts. This property can be set by assigning a time value
    * larger zero with 'setBackupFile()'.
    *  
    * @return boolean true = is BACKUP, false = is not BACKUP
    */
   boolean isBackupCopy ();
   
   /** Whether this document is displayed in the framework of the 
    * {@code DisplayManager}.
    * 
    * @return boolean true = is showing, false = is not showing
    */
   boolean isShowing ();

   /** Sets this file to encrypt or not encrypt its external storage form with 
    * the next file-save operation. The argument is the machine-near encryption
    * key (not the user key!), argument null switches off encryption.
    * <p>The machine-near encryption key is to be derived from user input
    * through {@code Functions.makeEncryptKey(char[])} (FHL class). 
    * This method can be restated if a new passphrase is to be established.
    * 
    * @param key byte[] encryption key or null to switch off
    */
   void setEncrypted (byte[] key);
   
   /** Returns the encryption key material or null if this document is not
    * encrypted.
    * 
    * @return byte[] encryption key or null
    */
   byte[] getPassphrase ();
   
   /** Sets the currently selected article in this document's article tree.
    *  
    * @param article PadArticle selected article or null for undefined
    */
   void setSelectedArticle (PadArticle article);
   
   /** Marks this document as a BACKUP to another document. The time given is
    * the last modification time of the backup; if the value is zero then the
    * BACKUP mark is unset.
    * 
    * @param time long file time in milliseconds
    */
   void setBackupFile (long time);

   /** Returns the BACKUP modify time of this document. A value larger zero
    * indicates that this document is marked as a BACKUP. This may have 
    * consequences for the display.
    * 
    * @return long time in milliseconds or zero
    */
   public long getBackupTime ();
   
   /** The preferred display modus of this document.
    * 
    * @return <code>DisplayModus</code>
    */
   DisplayModus getPreferredDisplayModus ();

   /** Sets the preferred display modus of this document.
    * 
    * @param displayModus <code>DisplayModus</code>
    * @throws NullPointerException
    */
   void setPreferredDisplayModus (DisplayModus displayModus);

   /** Returns the preferred JSplitPane divider position used in 
    * "CompleteDisplay" modus.
    * 
    * @return int divider position
    */
   int getPreferredDividerPosition ();

   /** Sets the preferred divider position for this document used in 
    * "CompleteDisplay" modus.
    * 
    * @param position int
    */
   void setPreferredDividerPosition (int position);

   /** The preferred editor background colour for article content.
    *  
    * @return {@code Color}
    */
   Color getPreferredBackgroundColor ();
   
   /** The preferred editor foreground colour for article content.
    *  
    * @return {@code Color}
    */
   Color getPreferredForegroundColor ();
   
   /** The default text font for articles. This is the default font of the
    * root article of this document, or if the document is empty the global
    * default text font.
    * 
    * @return {@code Font}
    */
   Font getDefaultTextFont ();

   /** Returns the font for the order-view display component or null if this
    * property is not defined.
    * 
    * @return {@code Font} or null
    */
   Font getOrderviewFont ();
   
   /** Sets the font for the order-view display component. The value may be 
    * null to remove the definition.
    *  
    * @param {@code Font} or null to clear
    */
   void setOrderviewFont (Font font);
   
   /** Returns the type of this pad-document.
    *  
    * @return <code>DocumentType</code>
    */
   DocumentType getDocType ();
   
   /** The external storage file of this document or null if there is no path
    * defined.
    * 
    * @return File or null
    */
   File getFile ();
   
   /** Returns the system file type of this pad-document.
    *  
    * @return <code>SystemFileType</code>
    */
   SystemFileType getFileType ();
   
   PersistentOptions getOptions ();
   
   /** Sets the document type. (The document type serves for externalisation
    * purposes, like saving and reading of the document.)
    * 
    * @param type DocumentType
    */
   void setDocType (DocumentType type);

   /** Sets READ-ONLY property of this document. If this property is TRUE then
    * no article text can be modified and the outline of the document cannot be
    * changed.
    *  
    * @param readOnly boolean true = READ-ONLY, false = not READ-ONLY
    */
   void setReadOnly (boolean readOnly);
   
   /** Whether the READ-ONLY property of this document is set.
    * 
    * @return boolean READ-ONLY property (on/off)
    */
   boolean isReadOnly ();
   
   /** The number of articles in this pad-document.
    * 
    * @return int number of articles
    */
   int getArticleCount ();
   
   /** Returns the {@code UndoManager} of the order-view of this document.
    * 
    * @return {@code UndoManager}
    */
   UndoManager getUndoManager ();
   
   /** Adds a property change listener to this document for all properties.
    * Any argument is only added once.
    * 
    * @param listener {@code PropertyChangeListener}
    */
   void addPropertyChangeListener (PropertyChangeListener listener);
   
   /** Adds a property change listener to this document, bound to the given
    * property name. Any combination of arguments is only added once.
    * 
    * @param property String 
    * @param listener {@code PropertyChangeListener}
    */
   void addPropertyChangeListener (String property, PropertyChangeListener listener);

   void removePropertyChangeListener (PropertyChangeListener listener);
   
   void removePropertyChangeListener (String property, PropertyChangeListener listener);
   
//   * <p>TREE VIEW: If a  
//   * sorting is set to the relevant folder level, the new entry is sorted
//   * accordingly, otherwise it is inserted as successor to 'parent' as sibling 
//   * and as the last entry in the folder as child.
//   * <p>LIST VIEW: The article is added to the end of the list as a sibling
//   * and as successor to 'parent' as a child.
   
   /** Adds an article to this document at a position given by 
    * the 'parent' and 'child' parameters. The article can be added as a 
    * child or a sibling of the given parent according to the boolean 'child' 
    * parameter. The "child" relation organises a Tree (Graph) ordering. The 
    * article is always added as the first possible successor of the
    * parent article according to the ordering command.
    * If 'parent' is null, the article is added to the root node or inserted
    * as root node if the document is empty. The 
    * combination of 'parent'=null & 'child'=false is illegal and prompted
    * by an exception.
    * <p><small>A specific article can only be present in one document. When
    * an article is added to a document A and is already member of another
    * document B, it is automatically removed from B while added to A.
    * To add a copy of an article, 'article.copy()' can be given as argument;
    * it is then an article with new identity.
    * </small>
    * 
    * @param article <code>PadArticle</code> article to add
    * @param parent UUID parent of article or null
    * @param child boolean whether the article is inserted as a child to parent;
    *               true = child, false = sibling
    * @throws IllegalArgumentException if article is null, parent is unknown 
    *          or an illegal parameter setting is given for the root node
    */
   void addArticle (PadArticle article, PadArticle parent, boolean child);
   
   /** Adds an article to this document at a position given by 
    * the 'index' and 'child' parameters. 'index' must denote an
    * existing element or otherwise be -1 for inserting the root node.
    * The root node, which is an article, has index value 0.
    * The "child" relation organises a Tree (Graph) ordering of the elements of
    * a PadDocument. For the sequence of an index, the Tree ordering is 
    * sequentially listed in a traversing modus called "breadth first".
    * <p>The given article can be added as a child or a sibling to the article 
    * named by the index, according to the boolean 'child' parameter. 
    * The article is always added at index position 'index' + 1, i.e. the
    * the first element in the sequence of children of 'index' if 'child' is
    * the option (true).
    * <p>The combination of 'index'=0 & 'child'=false is illegal and prompted 
    * by an exception. For index -1 'child' is irrelevant. 
    * <p><small>NOTE: A specific article can only be present in one document. 
    * When an article is added to a document A and is already member of another
    * document B, it is automatically removed from B while added to A.
    * To add a copy of an article, 'article.copy()' can be given as argument;
    * it is then an article with a new identity.
    * </small>
    * 
    * @param article <code>PadArticle</code> article to add
    * @param index int list index of parent article, ranging from 0; 
    *               -1 for inserting root node 
    * @param child boolean whether the article is inserted as a child to parent;
    *               true = child, false = sibling
    * @throws IllegalArgumentException if article is null or index is out 
    *          of range
    */
   void addArticle (PadArticle article, int index, boolean child);
   
   /** Adds a series of articles to this document at a position starting with 
    * 'index' + 1. 'index' must denote an existing element or otherwise be -1 
    * for starting with the root node. The root node has index value 0.
    * <p>The given articles can be added as children or siblings to the article 
    * named by 'index'. 
    * <p>The combination of 'index'=0 & 'children'=false is illegal and prompted 
    * by an exception. For index == -1 'children' is irrelevant. 
    * <p><small>NOTE: A specific article can only be present in one document. 
    * When an article is added to a document A and is already member of another
    * document B, it is automatically removed from B while added to A.
    * To add a copy of an article, 'article.copy()' can be given as argument;
    * it is then an article with a new identity.
    * </small>
    * 
    * @param articles <code>PadArticle[]</code> array of articles to add
    * @param index int list index of parent article, ranging from 0, 
    *               -1 for inserting the root node 
    * @param children boolean whether articles are inserted as a children to 
    *               parent; true = children, false = siblings
    * @throws IllegalArgumentException if article is null or index is out 
    *          of range
    */
   void addArticles (PadArticle[] article, int index, boolean children);
   
   /** Creates a new article at the given position in this document.
    * The article is added as a child or a sibling of the given parent 
    * according to the 'child' parameter.
    * If no parent is given (null), the article is added as a child to the 
    * root node of the document or as root if this document is empty.
    * 
    * @param parent <code>PadArticle</code> parent of the new article or null
    * @param child boolean true = add as child, false = add as sibling
    * @return <code>PadArticle</code>
    * @throws IllegalArgumentException if parent is unknown 
    *          or an illegal parameter setting is given for the root node
    * @throws ResourceAllocationException
    */
   PadArticle newArticle (PadArticle parent, boolean child);

   /** Returns a shallow clone of this document with identical reference to 
    * data sections and listeners, but with a new UUID. A shallow copy
    * e.g. is used for saving a document to a different path.
    *  
    * @return <code>PadDocument</code>
    */
   PadDocument getShallowCopy ();
   
   /** Returns a deep clone of this document with all article data copied in new
    * instances and listeners stripped off. The result bears a new UUID. 
    *  
    * @return <code>PadDocument</code>
    */
   PadDocument getFullCopy ();
   
   /** Returns an array of articles starting with the article at the given index
    * and stretching to all of its descendant articles. The returned articles
    * are unmodified originals as stored in the document.
    *  
    * @param index int starting article
    * @return {@code PadArticle[]}
    * @throws IndexOutOfBoundsException
    */
   PadArticle[] getArticlesAt (int index);
   
   /** Returns an array of children (depth value == index+1) under the given 
    * article list index. The returned articles are originals.
    * 
    * @param index int article index
    * @return {@code PadArticle[]}
    * @throws IndexOutOfBoundsException
    */
   PadArticle[] getChildrenOf (int index);
   
   /** Returns the insertion index of the next sibling to the given index.
    * The value ranges from zero to {@code getArticleCount()}. The 
    * length-of-list is an unreal but valid insertion position.
    * 
    * @param index int index of an existing article 
    * @return int sibling index
    */
   int getNextSiblingIndex (int index);
   
   /** Returns the depth of the article branch starting with the given index.
    * A value of zero indicates the article has no child, 1 means it has
    * leaf children only, 2 and higher mean it has at least one child which
    * has again descendants.
    * 
    * @param index int article position in document
    * @return int depth value (0..n) 
    * @throws IndexOutOfBoundsException
    */
   int getBranchDepth (int index);
   
   /** Returns the child index of the given article under its parent or zero
    * if the article has no parent.
    * 
    * @param a {@code PadArticle}
    * @return int child index (relative to parent)
    */
   public int getChildIndex (PadArticle a);
   
   /** Returns the parent article of the given index or null if the article at
    * index has no parent.
    *  
    * @param index int article index
    * @return {@code PadArticle} or null
    * @throws IndexOutOfBoundsException
    */
   public PadArticle getParent (int index);
   
   /**
    * Removes the article, and all of its descendants, at the given index 
    * position from this document and returns them as an article package 
    * (array of pad-articles). The removed articles are stripped off their
    * "document" property, which is set to null. The returned array mirrors
    * the sequence of articles that was in place in this document. 
    * 
    * @param index int article to remove
    * @return array of <code>PadArticle</code> article package starting with
    *          index article
    * @throws IndexOutOfBoundsException
    * @throws IllegalArgumentException if index position is not permitted
    */
   PadArticle[] cutoutArticleAt (int index);
   
   /**
    * Renders a list of copies of articles which reside under the given 
    * article index. The list comprises the referenced index article and all 
    * of its descendants. The copies carry new UUIDs but keep their pad-document
    * reference. Their parent-child relations are structurally copied within the 
    * set of new instances. The first article in the resulting array has the 
    * same parent as the original.
    * 
    * @param index int position of article
    * @return array of <code>PadArticle</code>
    * @throws IndexOutOfBoundsException
    */
   PadArticle[] copyArticlesAt (int index);
   
   /** Returns a copy of the article at the given index position.
    * <p>NOTE: This copy does not comprise descendant articles. 
    * 
    * @param index int position of article
    * @return <code>PadArticle</code>
    * @throws IndexOutOfBoundsException
    */
   PadArticle copyArticle (int index);

   /**
    * Inserts an article (and the set of its descendant articles) at the given
    * index position. The index position is replaced and pushed further down
    * the article list. All articles, except the first, must show the first
    * in their ancestor list and all of the ancestors of their path to first 
    * must be contained in the array. The first article's parent is set to the
    * given parent argument.
    * 
    * @param parent PadArticle
    * @param index int position of insertion (0..getArticleCount())
    * @param arr array of <code>PadArticle</code>, set of article + descendants
    * @throws IndexOutOfBoundsException
    * @throws IllegalArgumentException if array is null or some logic error
    *          occurs
    */
   void insertArticleAt (PadArticle parent, int index, PadArticle[] arr);
   
   /** Returns the list index of the given article or -1 if the
    * parameter is <code>null</code> or not found in the article list.
    * <p><small>The graph ordering of articles in a document is flattened
    * to a list view by traversing the tree in modus "breadth-first". This
    * flattened view is relevant for the index value.</small> 
    * 
    * @param article <code>PadArticle</code>, may be null
    * @return int article index or -1
    */
   public int indexOf (PadArticle article);
   
   /** Returns the index position of the article with the given UUID identifier
    * or -1 if the article was not found.
    * 
    * @param articleUuid {@code UUID}
    * @return int index value or -1 
    */
   public int indexOf (UUID articleUuid);
   
   /** Removes the article with the given UUID from this document. Does 
    * nothing if the argument is null or not contained.
    * 
    * @param article UUID article identifier, may be null
    * @return <code>PadArticle</code> the removed article or null if 
    *          nothing was removed
    * @throws IllegalArgumentException if there is an attempt to remove the
    *          root article while the document has more than one elements       
    */
   PadArticle removeArticle (UUID article);
   
   /** Removes the given article from this document. The document relation
    * of the article is removed (set to null). Does nothing if the article is 
    * null or not contained.
    * 
    * @param article <code>PadArticle</code> article, may be null
    * @return <code>PadArticle</code> the removed article or null if 
    *          nothing was removed
    * @throws IllegalArgumentException if there is an attempt to remove the
    *          root article while the document has more than one elements       
    */
   PadArticle removeArticle (PadArticle article);

   /** Removes all articles of the given article array from this document.
    * Array positions of value null or which point to an article not contained 
    * in this document are ignored. The actually removed articles are stripped
    * off of their document relation (set to null).
    * 
    * @param articles {@code PadArticle[]} may be null
    */
   void removeArticles (PadArticle[] articles);
   
   /** Returns the article with the given UUID or null if such an article
    * is not defined.
    * 
    * @param uuid <code>UUID</code>, may be null
    * @return <code>PadArticle</code> or null
    */
   PadArticle getArticle (UUID uuid);
   
   AbstractListModel<PadArticle> getListModel ();
   
   /** Returns an iterator over all articles in this pad-document.
    * 
    * @return <code>Iterator</code> of <code>PadArticle</code>
    */
   @Override
   Iterator<PadArticle> iterator ();

   /** Returns the character encoding used to serialise this document. If the
    * stored value is not supported by the current JVM a default encoding is
    * returned.
    *  
    * @return String charset name
    */
   String getEncoding ();

   /** Sets the character-set used to serialise this document.
    * There are no controls performed on the value. It is undefined whether
    * this document becomes "modified" by changing this setting.
    * 
    * @param charset String
    */
   void setEncoding (String charset);
   
   /** Returns the time mark for the latest save-worthy modification on this
    * document.
    * 
    * @return long time in milliseconds
    */
   long getModifyTime ();

   /** Returns the number of articles under the given index position.
    * The counter covers the given index position and all its descendant
    * articles.
    * 
    * @param index int article position
    * @return int size of article package
    * @throws IndexOutOfBoundsException
    */
   int countArticlesAt (int index);

   /** Sets the currently selected article by its index position in this 
    * document.
    * 
    * @param index int position of article or -1 for undefined
    * @throws IndexOutOfBoundsException
    */
   void setSelectedIndex (int index);

   /** Returns the article at the given index position in this document.
    * The returned article is an original.
    * 
    * @param index int position
    * @return {@code PadArticle}
    * @throws IndexOutOfBoundsException
    */
   PadArticle getArticle (int index);
   
   /** Returns the {@code TreePath} of the given article in the tree-model
    * or null if the argument was null or not an element of the article list.
    * 
    * @param art {@code PadArticle}
    * @return {@code TreePath} or null
    */
   public TreePath getArticleTreePath (PadArticle art);

   TreeModel getTreeModel();

   /** Save document preferences if they are modified or if called as
    * unconditional.
    * 
    *  @param unconditional boolean true = always save, false = save if modified
    */
   void savePreferences(boolean unconditional);

   /** Annihilates the document preferences. This should be called when the
    * document file is deleted and the document removed.
    */
   void removePreferences ();
   
}
