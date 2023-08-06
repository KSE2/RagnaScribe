/*
*  File: DefaultPadDocument.java
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractListModel;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;

import org.ragna.front.DisplayManager;
import org.ragna.front.DisplayManager.DisplayModus;
import org.ragna.io.IO_Manager;
import org.ragna.io.IO_Manager.SystemFileType;
import org.ragna.util.OptionBag;
import org.ragna.util.PersistentOptions;

import kse.utilclass.misc.Log;
import kse.utilclass.misc.UUID;
import kse.utilclass.misc.Util;
import kse.utilclass.sets.ArraySet;

/** Basic implementation of {@code PadDocument} (currently <i>Treepad</i>).
 * 
 * <p>MINOR DOCUMENT PROPERTIES are:
 * <br><b>Selected-Article, Divider-Position, Display-Modus, Text-Font, Bgd-Color.</b>
 * <br>These values are not stored in the document file by default but in a
 * local database belonging to the user's application installation.
 * 
 * <p>The document issues property change events via a 
 * {@code PropertyChangeListener}. Available properties are:
 * <br>documentModified		- (old, new modified)
 * <br>uuidChanged			- (old, new UUID)
 * <br>articleAdded			- (null, added article)
 * <br>articleRemoved		- (null, removed article)
 * <br>articleModified		- (null, article)
 * <br>articleTitleChanged	- (null, article)
 * <br>articleUuidChanged	- (old UUID, article)
 * <br>selectionChanged		- (old, new PadArticle)
 * <br>typeChanged			- (old, new DocumentType)
 * <br>displayModusChanged	- (old, new DisplayModus)
 * <br>layoutChanged		- (null, (font, backgroundColor, dividerPosition))
 * <br>titleChanged			- (null, null)readOnlyChanged
 * <br>readOnlyChanged		- (old, new readOnly)
 */
public class DefaultPadDocument implements PadDocument, Cloneable {
   private static Preferences documentPrefs = Preferences.userRoot().node( "/ragnasc/document/prop" );
   private static int counter;
   static { cleanJavaPreferences(); }

   private Map<UUID, PadArticle> articleMap = new HashMap<UUID, PadArticle>();
   private List<PadArticle> articleList = new ArrayList<PadArticle>();
   private PropertyChangeSupport support = new PropertyChangeSupport(this);
   private DocumentListModel listModel = new DocumentListModel();
   private DocumentTreeModel treeModel = new DocumentTreeModel();
   private OptionBag options = new OptionBag(); 
   private UndoManager undoManager = new UndoManager();
   
   // display formatting elements
   private int dividerPosition;
   private DisplayModus prefDisplayModus = DisplayModus.ComleteDisplay;

   private UUID uuid = new UUID();
   private int name = counter++;
   private DocumentType documentType = DocumentType.TreePad;
   private String title;
   private String shortTitle;
   private String tooltip;
   private PadArticle selectedArticle;
   private ArticleListener articleListener = new ArticleListener();
   private byte[] passwd;
   private boolean modified;
   private long modifyTime;
   private long backupModifyTime;
   
   /** Creates a new document of type TreePad and a new UUID.
    */
   public DefaultPadDocument () {
      setTitle("Document " + name);
      init();
   }
   
   /** Creates a new document of type TreePad with a new UUID and the given
    * title and tooltip properties.
    */
   public DefaultPadDocument (String title, String tooltip) {
      this.tooltip = tooltip;
      setTitle(title);
      init();
   }
   
   /** Creates a new document of the given type and with a new UUID.
    * 
    * @param type {@code DocumentType}
    */
   public DefaultPadDocument (DocumentType type) {
      this();
      setDocType(type);
   }

   /** Creates a new pad-document of the given type and an optional UUID
    * value.
    * 
    * @param type <code>DocumentType</code>
    * @param uuid <code>UUID</code> document UUID to assume or null for
    *              random creation
    */
   public DefaultPadDocument (DocumentType type, UUID uuid) {
      setTitle("Document " + name);
      setDocType(type);
      if (uuid != null) {
         this.uuid = uuid;
      }
      init();
   }

   /** Tries to obtain the document option-bag from a filepath key.
    * 
    * @param filepath String full path of document serialisation file
    * @return {@code OptionBag} or null if no such entry was found
    * @throws IOException
    */
   public static OptionBag getDocumentOptionsFromPath (String filepath) throws IOException {
	   Objects.requireNonNull(filepath);
       String hstr = documentPrefs.get(filepath, null);
       if (hstr != null) {
    	   try {
    		   Log.debug(5, "(DefaultPadDocument.getDocumentOptionsFromPath) get preferences for " + filepath);
    		   return getDocumentOptionsFromID(new UUID(hstr));
    	   } catch (NumberFormatException e) {
    	   }
       } 
       return null;
   }
   
	/** Tries to obtain the document option-bag from a document UUID entry.
	 * 
	 * @param uuid UUID document UUID
	 * @return {@code OptionBag} or null if no such entry was found
	 * @throws IOException
	 */
	public static OptionBag getDocumentOptionsFromID (UUID uuid) throws IOException {
		Objects.requireNonNull(uuid);
		String idStr = uuid.toHexString();
	    String hstr = documentPrefs.get(idStr, null);
	    OptionBag bag = hstr == null ? null : new OptionBag(hstr);
		Log.debug(5, "(DefaultPadDocument.getDocumentOptionsFromID) preferences read for " + idStr + " :\n" + hstr);
		return bag;
	}
	
	/** Removes stale entries in the document related Java Preferences node.
	 */
	public static void cleanJavaPreferences () {
//		System.out.println("# cleaning Java Preferences");
		try {
			for (String key : documentPrefs.keys().clone()) {
				if ((key.endsWith(".tmp") || key.endsWith(".tmp.fhl")) && !new File(key).exists()) {
					String hs = documentPrefs.get(key, null);
					if (hs != null) {
						documentPrefs.remove(hs);
					}
					documentPrefs.remove(key);
					Global.getRecentFilesStack().remove(key);
//					System.out.println("  -- ".concat(key));
				}
			}
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}
	
   private void init () {
	  Log.log(4, "(DefaultPadDocument.init) new document init, UUID = " + getUUID());

      // read document preferences from system prefs
      try {
    	 OptionBag opt = getDocumentOptionsFromID(getUUID());
    	 if (opt != null) {
    		 options = opt;
    	 }
		 readPreferences();
	  } catch (IOException e) {
		 e.printStackTrace();
	  }
      
      // ensure encoding setting
      if (!options.contains("text-encoding")) {
    	  options.setOption("text-encoding", getEncoding());
      }
   }
   
   @Override
   public void savePreferences () {
	   // compile article preferences
	   compileArticleProperties();

	   if (options.isModified()) {
		   String content = options.toString();

		   // primary storage under UUID
		   String key = getUUID().toHexString();
		   documentPrefs.put(key, content);
		   
		   // secondary storage under filepath (if available)
		   String path = getExternalPath();
		   if (path != null) {
			   documentPrefs.put(path, key);
		   }
		   
		   options.resetModified();
		   Log.debug(4, "-- document preferences assigned for " + path);
		   Log.debug(4, "-- document preferences written (" + getShortTitle() + "): " + key);
		   Log.debug(5, "(DefaultPadDocument.savePreferences) value preferences : \n" + content);
	   }
   }
   
   @Override
   public void removePreferences () {
	   // primary storage under UUID
	   String key = getUUID().toHexString();
	   documentPrefs.remove(key);
	   
	   // secondary storage under filepath (if available)
	   String path = getExternalPath();
	   if (path != null) {
		   documentPrefs.remove(path);
	   }
   }
   
   private void readPreferences () {
  	   if (options.contains("divider-pos")) {
  		   dividerPosition = options.getIntOption("divider-pos");
  	   }
  	   String hs = options.getOption("display-modus");
  	   if (!hs.isEmpty()) {
  		   prefDisplayModus = DisplayModus.fromString(hs);
  	   }
   }
   
   private void compileArticleProperties () {
	   Set<String> storeList = new ArraySet<>();
	   
	   for (PadArticle a : this) {
		   // article key
		   String artID = a.getPathID();
		   
		   // article property serialisation
		   String artSerial = a.getPropertySerial();
		   String hs2 = artID + " = " + artSerial;
		   storeList.add(hs2);
		   Log.debug(10, "(DefaultPadDocument.compileArticleProperties) article properties entry = " + hs2);
	   }
	   
	   options.setStringList("ARTOPT", storeList);
   }
   
   @Override
   public String getTitle () {
      return title;
   }

   @Override
   public String getShortTitle () {
      return shortTitle;
   }

   @Override
   public String getToolTipText () {
      return tooltip;
   }

   @Override
   public UUID getUUID () {
      return uuid;
   }

   @Override
   public void setUUID (UUID uuid) {
   	  Objects.requireNonNull(uuid);
   	  UUID old = this.uuid;
   	  this.uuid = uuid;
   	  firePropertyChange("uuidChanged", old, uuid);
   }

   @Override
   public void setReadOnly (boolean readOnly) {
	   boolean oldSetting = isReadOnly();
	   if (oldSetting != readOnly) {
		   options.setOption("isReadOnly", readOnly);
	
	       // issue property change event(s)
	       support.firePropertyChange("readOnlyChanged", oldSetting, readOnly);
	   }
   }

   @Override
   public boolean isReadOnly() {
   	   return options.isOptionSet("isReadOnly");
   }

   @Override
   public int hashCode () {
      return uuid.hashCode();
   }

   @Override
   public boolean equals (Object obj) {
      try { return uuid.equals(((PadDocument)obj).getUUID());
      } catch (Exception e) {
         return false;
      }
   }

   @Override
   public AbstractListModel<PadArticle> getListModel() {
   		return listModel;
   }

   @Override
   public TreeModel getTreeModel () {
   		return treeModel;
   }

   @Override
   public DisplayModus getPreferredDisplayModus () {
      return prefDisplayModus;
   }

   @Override
   public void setPreferredDisplayModus (DisplayModus displayModus) {
      if (displayModus == null) 
         throw new NullPointerException();

      if (displayModus != prefDisplayModus) {
         DisplayModus oldModus = prefDisplayModus;
         prefDisplayModus = displayModus;

         // store to document preferences
    	 options.setOption("display-modus", displayModus.name());

         // issue property change event(s)
         support.firePropertyChange("displayModusChanged", oldModus, displayModus);
//         setModified(true);
      }
   }

   @Override
   public int getPreferredDividerPosition () {
      return dividerPosition;
   }

   @Override
   public void setPreferredDividerPosition (int position) {
	  position = Math.max(0,  position);
      if (position != dividerPosition) {
         dividerPosition = position;
         
         // store to document preferences
    	 options.setIntOption("divider-pos", position);

         // issue property change event(s)
         support.firePropertyChange("layoutChanged", null, "dividerPosition");
      }
   }

   @Override
   public DocumentType getDocType () {
      return documentType;
   }

   @Override
   public int getArticleCount () {
      return articleMap.size();
   }

   @Override
   public synchronized PadArticle getArticle (UUID uuid) {
      return uuid == null ? null : articleMap.get(uuid);
   }

   @Override
   public synchronized PadArticle getArticle (int index) {
      return articleList.get(index);
   }
   
   @Override
   public synchronized Iterator<PadArticle> iterator () {
      return articleList.iterator();
   }

   public synchronized boolean contains (UUID uuid) {
	   return uuid == null ? false : articleMap.containsKey(uuid);
   }
   
   public synchronized boolean contains (PadArticle article) {
	   return contains(article.getUUID());
   }
   
   @Override
   public UndoManager getUndoManager() {return undoManager;}

   @Override
   public PersistentOptions getOptions() {return options;}
   
//   @Override
//   public int getDisplayFocus () {
//      return focusValue;
//   }
//
//   @Override
//   public void setDisplayFocus (int focusObject) {
//      if (Util.notEqual(focusObject, focusValue)) {
//         int oldFocus = focusValue;
//         focusValue = focusObject;
//
//         // issue property change event
//         String sval = focusValue == 0 ? "order-view" : "article";
//         support.firePropertyChange("focusChanged", oldFocus, sval);
//         setModified();
//      }
//   }
//
   @Override
   public PadArticle getSelectedArticle () {
	   // if undefined, try to retrieve UUID from options and define thereof
	   if (selectedArticle == null) {
		   String hs = options.getOption("selected-article");
		   if (!hs.isEmpty()) {
			   UUID id = new UUID(hs);
			   selectedArticle = getArticle(id);
		   }
	   }
       return selectedArticle;
   }

   @Override
   public synchronized void setSelectedArticle (PadArticle article) {
       if (article != null && !contains(article) ||
          Util.equal(article, selectedArticle) ) return;
       
       PadArticle oldSelected = selectedArticle;
       selectedArticle = article;

       // store to document preferences
       String value = article == null ? null : article.getUUID().toHexString();
  	   options.setOption("selected-article", value);
  	   
       // issue property change event(s)
       support.firePropertyChange("selectionChanged", oldSelected, article);
   }

   @Override
   public void setSelectedIndex (int index) {
      PadArticle article = index == -1 ? null : getArticle(index);
      setSelectedArticle(article);
   }

   @Override
   public int getSelectedIndex () {
	   PadArticle a = getSelectedArticle();
	   return indexOf(a); 
   }

   private void updateTitleFromFirstArticle () {
      if (getDocType() == DocumentType.TreePad && getArticleCount() > 0 ) {
         // set new root element
         PadArticle root = articleList.get(0);
         String title = root.getTitle();
         String shortTitle = root.getShortTitle();

         // update document title / shortTitle from root
         if (!title.isEmpty()) {
            setTitle(title);
         }
         if (!shortTitle.isEmpty()) {
            setShortTitle(shortTitle);
         }
      }
   }
   
//   private void fireLayoutChanged () {
//      
//   }
   
   private void firePropertyChange (String property, Object oldValue, Object newValue) {
      support.firePropertyChange(property, oldValue, newValue);
   }
   
   @Override
   public int indexOf (PadArticle article) {
      if (article != null) {
         return articleList.indexOf(article);
      }
      return -1;
   }
   
   @Override
   public int indexOf (UUID articleUuid) {
	   return indexOf(getArticle(uuid));
   }

   @Override
   public synchronized void addArticle (PadArticle article, PadArticle parent, boolean asChild) {
      // search for parent index and exclude illegal parent
      int index;
      if (getArticleCount() == 0 & parent == null) {
         index = -1;
      } else {
         index = parent == null ? 0 : indexOf(parent);
         if (index == -1) {
            throw new IllegalArgumentException("parent is unknown");
         }
      }
      
      // resort to indexed version
      addArticle(article, index, asChild);
   }

//   /**
//    * Finds the next available article from a start index which owns the 
//    * given depth value and returns its index value.
//    * 
//    * @param startIndex
//    * @param depth
//    * @return
//    */
//   private int findNextArticleIndex (int startIndex, int depth) {
//      
//   }
   
   @Override
   public synchronized void addArticle (PadArticle article, int index, boolean asChild) {
      // verify parameters
      Objects.requireNonNull(article, "article is null");
      int size = getArticleCount();
      if (index < -1 | index > size-1)
         throw new IllegalArgumentException("parent index out of range: ".concat(
               String.valueOf(index)));
      if (index == -1 & size > 0) 
         throw new IllegalArgumentException("illegal insertion of root node (root exists)");
      if (index == 0 & !asChild) 
         throw new IllegalArgumentException("illegal 'child' value FALSE for parent root");
         
      UUID uuid = article.getUUID();
      PadArticle parent = index == -1 ? null : articleList.get(index);
      if (index == -1) {
         asChild = true;
      }

      // if article UUID is unknown in this document (ignore known articles)
      if (!articleMap.containsKey(uuid)) {
         // add article into map
         articleMap.put(uuid, article);
         int insertPos = index+1;
         Log.log(6, "(DefaultPadDocument.addArticle) added to article map: " + uuid + ", " + article.getTitle());

         if (parent != null) {
            // find and realise sorting position
            // find index of next available sibling position to parent
            // for this we just skip all possible child positions
            int childDepth = parent.getOrderDepth() + 1;
            int i = index, len = articleList.size();
            while (++i < len && articleList.get(i).getOrderDepth() >= childDepth) ;
            insertPos = i;
         }
         
         // insert article to sorted list at found position
         articleList.add(insertPos, article);

         // insert article to sorted list
         if (asChild) {
            article.setParent(parent);
         } else {
            article.setParent(parent.getParent());
         }
         
         // set new ownership (this also removes from previous owner)
         article.setDocument(this);
         article.addPropertyChangeListener(articleListener);
         
         if (articleMap.size() == 1) {
            updateTitleFromFirstArticle();
         }

         // issue property change events
         listModel.fireIntervalAdded(this, insertPos, insertPos);
         treeModel.fireTreeNodeInserted(article);
         support.firePropertyChange("articleAdded", null, article);
         setModified();
      }
   }

   @Override
   public void addArticles (PadArticle[] articles, int index, boolean children) {
	   Objects.requireNonNull(articles);
	   if (articles.length == 0) return;
	   
	   // insert first element
	   addArticle(articles[0], index, children);
	   
	   for (int i = 1; i < articles.length; i++) {
		   index++;
		   addArticle(articles[i], index, index == 0 ? true : false);
	   }
   }
   
   @Override
   public PadArticle[] getArticlesAt (int index) {
      // create the list of articles in sequence of articleList
      ArrayList<PadArticle> list = new ArrayList<PadArticle>();
      PadArticle art = articleList.get(index);
      list.add(art);
      int depth = art.getOrderDepth();
      int size = articleList.size();
      int i = index;
      while (++i < size && ((art = articleList.get(i)).getOrderDepth() > depth)) {
         list.add(art);
      }
      return list.toArray(new PadArticle[list.size()]);
   }
   
   /** Returns an array of children (depth value == index+1) under the given 
    * article list index. 
    * 
    * @param index int article index
    * @return {@code PadArticle[]}
    * @throws IndexOutOfBoundsException
    */
   @Override
   public PadArticle[] getChildrenOf (int index) {
	   if (index < 0 | index > articleMap.size()-1)
		   throw new IndexOutOfBoundsException("index = " + index);
	   
      // create the list of articles in sequence of articleList
      ArrayList<PadArticle> list = new ArrayList<PadArticle>();
      PadArticle art = articleList.get(index);
      int depth = art.getOrderDepth();
      int size = articleList.size();
      int i = index;
      while (++i < size && 
            ((art = articleList.get(i)).getOrderDepth() > depth)) {
    	 if (art.getOrderDepth() == depth + 1) {
    		 list.add(art);
    	 }
      }
      return list.toArray(new PadArticle[list.size()]);
   }
   
   /** Returns the child index of the given article under its parent or zero
    * if the article has no parent.
    * 
    * @param a {@code PadArticle}
    * @return int child index (relative to parent)
    */
   @Override
   public int getChildIndex (PadArticle a) {
	   Objects.requireNonNull(a);
	   int index;
	   PadArticle parent = a.getParent();
	   // if parent is null or does not belong to document 
	   if ((index = indexOf(parent)) == -1) return 0;
	   
	   Object[] chld = getChildrenOf(index);
	   for (int i = 0; i < chld.length; i++) {
		   if (a.equals(chld[i])) return i;
	   }
	   throw new IllegalStateException("child not under its parent: " + a);
   }
   
   @Override
   public PadArticle getParent (int index) {
   	  TreePath parentPath = getArticleTreePath(getArticle(index)).getParentPath();
   	  PadArticle parent = parentPath == null ? null : (PadArticle) parentPath.getLastPathComponent();
   	  return parent;
   }

   /** Returns the {@code TreePath} of the given article in the tree-model
    * or null if the argument was null or not an element of the article list.
    * 
    * @param art {@code PadArticle}
    * @return {@code TreePath} or null
    */
   @Override
   public TreePath getArticleTreePath (PadArticle art) {
	   int index = indexOf(art);
	   if (index == -1) return null;
	   
	   List<PadArticle> list = new ArrayList<>();
	   PadArticle a = art;
	   while (a != null) {
		   list.add(a);
		   a = a.getParent();
	   }
	   
	   // transform list into tree path
	   Collections.reverse(list);
	   Object[] objects = list.toArray();
	   TreePath path = new TreePath(objects);
	   return path;
   }
   
   @Override
   public int countArticlesAt (int index) {
      PadArticle[] arr = getArticlesAt (index);
      return arr.length;
   }
   
   @Override
   public synchronized PadArticle[] copyArticlesAt (int index) {
      PadArticle[] pack = getArticlesAt(index);
      PadArticle[] result = new PadArticle[pack.length];
      PadArticle oldParent = pack[0];
      PadArticle newParent = oldParent.copy();
      result[0] = newParent;
      
      // we create the copy array
      // we re-create the parent-child structure of original in the copy array
      int i = 0;
      for (PadArticle art : pack) {
    	 if (i++ == 0) continue;
         PadArticle newArt = art.copy();
         result[i-1] = newArt;
         
         // set the parent article of the copy
         if (oldParent != art.getParent()) {
        	 oldParent = art.getParent();
        	 // search for index position of old parent
        	 int j = 0;
        	 while (j < pack.length && pack[j] != oldParent) j++;
        	 if (j == pack.length) {
        		 throw new IllegalStateException("article parent not found in 'copyArticlesAt()'");
        	 }
        	 
        	 // new parent must be at same position
        	 newParent = result[j];
         }
         
    	 newArt.setParent(newParent);
      }
      return result;
   }
   
   @Override
   public synchronized PadArticle copyArticle (int index) {
      PadArticle art = getArticle(index);
   	  return art.copy();
   }

   @Override
   public synchronized PadArticle[] cutoutArticleAt (int index) {
      PadArticle[] pack = getArticlesAt(index);
      removeArticles(pack);
      return pack;
   }
   
   @Override
   public void removeArticles (PadArticle[] arr) {
	   if (arr != null) {
	      // remove found articles from this document in reverse order of package
	      for (int i = arr.length; i > 0; i--) {
	         PadArticle a = arr[i-1];
	         if (a != null) {
	            removeArticle(a.getUUID());
	         }
	      }
	   }
   }

   /** Inserts the given single article at the given index position in this 
    * document. All existing articles in the document with index value equal
    * or higher than the given, increment their index value ("moved up") by one. 
    * Values for DOCUMENT and PARENT are set automatically on the inserted
    * article. If parent is null on the article, it will be inserted as root 
    * or as sibling to index-1. Otherwise parent must be index-1 or a value in 
    * the ancestor line of index-1.
    * <p>The root article can be marked with index == 0 if and only if 
    * the document is empty. It is not allowed to replace the root node by
    * insertion.
    * 
    * @param index int target index position in document (0..n, w/ 
    *                  n = getArticleCount())
    * @param article <code>PadArticle</code>
    * @throws IllegalArgumentException if article is already contained or 
    * 		  there is an illegal attempt to 
    *         replace the root node or the parent of the article is unknown
    * @throws IndexOutOfBoundsException
    */
   private void insertArticleAt (int index, PadArticle article) {
	  Objects.requireNonNull(article, "article is null");
      if (contains(article))
          throw new IllegalArgumentException("article is already contained");
      if (index < 0 | index > articleList.size())
         throw new IndexOutOfBoundsException();
      if (index == 0 && getArticleCount() > 0) {
          throw new IllegalArgumentException("cannot replace the root article");
      }
      
      // investigate the given parent if any
      PadArticle parent = article.getParent();
      if (parent != null) {
    	  if (!contains(parent))
              throw new IllegalArgumentException("article parent is unknown in document");
    	  if ((indexOf(parent) >= index) || 
    	      (index > 0 && !isDescendantOf(articleList.get(index-1), parent)))
              throw new IllegalArgumentException("index is misplaced to parent: " + index);
      }

      // set article parent if required and not set 
      if (index > 0 && article.getParent() == null) { 
          // determine parent article of current index position
          // (index 0 is always null, index == size is always root)
          PadArticle indexParent;
    	  if (index == articleList.size()) {
	    	  indexParent = articleList.get(0);  
	      } else {
	    	  indexParent = articleList.get(index).getParent();
	      }

   		  article.setParent(indexParent);
      }
      
      // add article into map and list
      articleMap.put(article.getUUID(), article);
      articleList.add(index, article);
      Log.log(6, "(DefaultPadDocument.insertArticleAt) added to article map: " + article.getUUID() + ", " + article.getTitle());

      // set the DOCUMENT value and property change listener
      article.setDocument(this);
      article.addPropertyChangeListener(articleListener);
      
      // update the document title on size == 1
      if (articleMap.size() == 1) {
         updateTitleFromFirstArticle();
      }

      // issue property change event(s)
      listModel.fireIntervalAdded(this, index, index);
      treeModel.fireTreeNodeInserted(article);
      support.firePropertyChange("articleAdded", null, article);
      setModified();
   }
   
   /** Whether article a is equal to or a descendant of ancestor. If any of the
    * arguments is null then 'false' is returned.
    * 
    * @param a PadArticle, may be null
    * @param ancestor PadArticle,  may be null
    * @return boolean true = a is equal to or descendant of ancestor
    */
   private boolean isDescendantOf (PadArticle a, PadArticle ancestor) {
      while (a != null) {
         if (a.equals(ancestor)) { 
            return true;
         }
         a = a.getParent();
      }
      return false;
   }
   
   @Override
   public synchronized void insertArticleAt (PadArticle parent, int index, PadArticle[] arr) {
	  if (index > 0) 
		  Objects.requireNonNull(parent, "parent is null");
      Objects.requireNonNull(arr, "article array is null");
      if (index < 0 | index > articleList.size())
         throw new IndexOutOfBoundsException("undefined article index: " + index);
      if (index == 0 && !isEmpty())
         throw new IllegalArgumentException("illegal attempt to replace the root node");
      int pi = indexOf(parent);
      if (parent != null && (pi == -1 || pi >= index))
          throw new IllegalArgumentException("parent not contained or misplaced, index = " + pi);

      if (arr.length > 0) {
         // verify argument article array
     	 PadArticle firstArt = arr[0];
    	 for (int i = 1; i < arr.length; i++) {
    		 PadArticle art = arr[i];
             if (!isDescendantOf(art, firstArt)) {
                throw new IllegalArgumentException("illegal article array: illegal ancestors at index " + i);
             }
             if (!Util.arrayContains(arr, art.getParent())) {
                 throw new IllegalArgumentException("illegal article array: illegal parent at index " + i);
             }
    	 }
         firstArt.setParent(parent);

         // verify target position (argument parent must govern index position)
         if (index > 0) {
            PadArticle pred = articleList.get(index-1);
            if (!isDescendantOf(pred, parent)) {
               throw new IllegalArgumentException("target index not in line with parent");
            }
         } 
         
         // insert the article array
         int tIndex = index;
         for (PadArticle a : arr) {
            insertArticleAt(tIndex++, a);
         }
      }
   }
   
   @Override
   public synchronized PadArticle newArticle (PadArticle parent, boolean asChild) {
      PadArticle article = new AbstractPadArticle();

      // set default layout values for article
      if (parent != null) {
         article.setDefaultFont(parent.getDefaultFont());
         article.setBackgroundColor(parent.getBackgroundColor());
         article.setForegroundColor(parent.getForegroundColor());
         article.setLineWrap(parent.getLineWrap());
         
      } else {
          article.setDefaultFont(getDefaultTextFont());
   		  article.setBackgroundColor(getPreferredBackgroundColor());
   		  article.setForegroundColor(getPreferredForegroundColor());
      }

      // add article to document with location after parent
      addArticle(article, parent, asChild);
      return article;
   }

   @Override
   public PadArticle removeArticle (PadArticle article) {
      return article == null ? null : removeArticle(article.getUUID());
   }
   
   @Override
   public synchronized PadArticle removeArticle (UUID uuid) {
      // do not operate if parameter is null OR not contained in document OR
      // attempt to remove the root element while there are more than 1 elements
      if (uuid == null || isEmpty() || !contains(uuid)) return null;

      boolean isRootElement = articleList.get(0).getUUID().equals(uuid);
      if (isRootElement && getArticleCount() > 1) {
         throw new IllegalArgumentException("illegal attempt to remove the root node");
      }
      
      PadArticle article = articleMap.remove(uuid);
      if (article != null) {
    	 int index = indexOf(article);
    	 int childIndex = getChildIndex(article);
    	 PadArticle selectedArticle = getSelectedArticle();
         boolean wasSelected = article.equals(selectedArticle);
    	 
         article.removePropertyChangeListener(articleListener);
         articleList.remove(article);
         Log.log(6, "(DefaultPadDocument.removeArticle) removed article from map: " + uuid + ", " + article.getTitle());

         // issue property change events
         listModel.fireIntervalRemoved(this, index, index);
         if (articleList.size() == 0) {
        	 treeModel.fireTreeStructureChanged();
         } else {
        	 treeModel.fireTreeNodeRemoved(article, childIndex);
         }
         support.firePropertyChange("articleRemoved", null, article);

         // ensure next article selection (if there was a selection)
         if (wasSelected) {
            index = Math.min(Math.max(0, index-1), getArticleCount()-1);
            setSelectedIndex(index);
         } else {
        	setSelectedArticle(selectedArticle);
         }
         
         setModified();
      }
      return article;
   }

   @Override
   public Color getPreferredBackgroundColor () {
	  Color color = null;
	  if (getArticleCount() > 0) {
		  color = getArticle(0).getBackgroundColor();
	  }
      return color == null ? Global.getDefaultBackgroundColor() : color;
   }

   @Override
   public Color getPreferredForegroundColor () {
	  Color color = null;
	  if (getArticleCount() > 0) {
		  color = getArticle(0).getForegroundColor();
	  }
      return color == null ? Global.getDefaultForegroundColor() : color;
   }

//   @Override
//   public void setPreferredBackgroundColor (Color color) {
//      if (Util.notEqual(color, defaultBackgroundColor)) {
//         defaultBackgroundColor = color;
//         
//         // store to document preferences
//         if (color == null) {
//        	 options.setOption("background-color", null);
//         } else {
//        	 options.setIntOption("background-color", color.getRGB());
//         }
//   
//         // issue property change event
//         support.firePropertyChange("layoutChanged", null, "backgroundColor");
//      }
//   }
//
   @Override
   public Font getDefaultTextFont () {
	  Font font = null;
	  if (getArticleCount() > 0) {
		  font = getArticle(0).getDefaultFont();
	  }
      return font == null ? Global.getDefaultFont() : font;
   }

//   @Override
//   public void setDefaultTextFont (Font font) {
//      if (Util.notEqual(font, defaultFont)) {
//         defaultFont = font;
//   
//         // store to document preferences
//       	 options.setFontOption("text-font", font);
//   
//         // issue property change event(s)
//         support.firePropertyChange("layoutChanged", null, "font");
//      }
//   }

   @Override
   public void setTitle (String title) {
      String oldTitle = this.title;
      if (Util.notEqual(title, oldTitle)) {
         this.title = title == null ? "" : title;
         if (shortTitle == null) {
            setShortTitle(title);
         }
         
         // issue property change event(s)
         support.firePropertyChange("titleChanged", null, null);
         setModified();
      }
   }

   @Override
   public void setShortTitle (String title) {
      String target = null;
      if (title != null) {
         int maxLength = Global.getOptions().getIntOption("maxShortTitleLength");
         target = title.substring(0, Math.min(maxLength, title.length()));
         if (target.length() == maxLength) {
            target = target.concat("..");
         }
      }

      if (Util.notEqual(target, shortTitle)) {
         shortTitle = target;
         support.firePropertyChange("titleChanged", null, null);
         setModified();
      }
   }

   @Override
   public void setDocType (DocumentType type) {
      if (type == null)
         throw new IllegalArgumentException("type is null");
      
      if (type != documentType) {
         DocumentType oldType = documentType;
         documentType = type;

         // issue property change event(s)
         support.firePropertyChange("typeChanged", oldType, type);
         setModified();
      }
   }

   @Override
   public void addPropertyChangeListener (PropertyChangeListener listener) {
      List<?> list = Arrays.asList(support.getPropertyChangeListeners());
      if (!list.contains(listener)) {
         support.addPropertyChangeListener(listener);
      }
   }

   @Override
   public void removePropertyChangeListener (PropertyChangeListener listener) {
      support.removePropertyChangeListener(listener);
   }

   @Override
   public void addPropertyChangeListener (String property, PropertyChangeListener listener) {
      List<?> list = Arrays.asList(support.getPropertyChangeListeners(property));
      if (!list.contains(listener)) {
         support.addPropertyChangeListener(property, listener);
      }
   }

   @Override
   public void removePropertyChangeListener (String property,
         PropertyChangeListener listener) {
      support.removePropertyChangeListener(property, listener);
   }
   
   @Override
   public boolean isModified () {return modified;}

   @Override
   public void resetModified () {
      boolean oldValue = modified;
      modified = false;
      if (oldValue != modified) {
         support.firePropertyChange("documentModified", oldValue, modified);
      }
   }

   /** Mark document MODIFIED and report listeners if value changed.
    */
   @Override
   public void setModified () {
      boolean oldValue = modified;
      modified = true;
      modifyTime = System.currentTimeMillis();
      if (oldValue != modified) {
         support.firePropertyChange("documentModified", oldValue, modified);
      }
   }
   
   @Override
   public String getEncoding () {
	  String enc = options.getOption("text-encoding");
	  String defEnc = Global.getOptions().getOption("defaultTreepadEncoding", "UTF-8");
	  try {
		  return Charset.isSupported(enc) ? enc : defEnc;
	  } catch (Throwable e) {
		  return defEnc;
	  }
   }

   @Override
   public void setEncoding (String charset) {
	   String oldSet = options.getOption("text-encoding");
	   if (charset != null && !charset.equals(oldSet)) {
		   options.setOption("text-encoding", charset);
		   Log.debug(5, "(DefaultPadDocument.setEncoding) setting encoding = ".concat(charset));

		   // issue property change event(s)
	       support.firePropertyChange("encodingChanged", null, null);
	   }
   }
   
   @Override
   public long getModifyTime () {
      return modifyTime;
   }

   @Override
   public PadDocument getShallowCopy () {
      try {
         DefaultPadDocument copy = (DefaultPadDocument)clone();
         copy.uuid = new UUID();
         copy.modified = true;
         return copy;
      } catch (CloneNotSupportedException e) {
         e.printStackTrace();
         return null;
      }
   }

// ------------ inner classes ----------------
   
   private class ArticleListener implements PropertyChangeListener {

      @Override
      public void propertyChange (PropertyChangeEvent evt) {

         if (evt.getPropertyName() == "uuidChanged") {
              PadArticle article = (PadArticle)evt.getNewValue();
              UUID oldId = (UUID)evt.getOldValue();
              UUID newId = article.getUUID();
              if (contains(oldId) && !oldId.equals(newId)) {
            	  if (articleMap.containsKey(newId)) {
            		  throw new IllegalStateException("attempt to insert duplicate article-ID: " + newId);
            	  }
            	  articleMap.remove(oldId);
            	  articleMap.put(newId, article);
                  firePropertyChange("articleUuidChanged", oldId, article);
                  Log.debug(10, "(DefaultPadDocument.ArtcileListener) replacing article UUID, old=" + oldId +
                		  ", new=" + newId);
                  
                  // set document modified
                  setModified();
              }
         }
         else if (evt.getPropertyName() == "contentModified") {
            PadArticle article = (PadArticle)evt.getNewValue();
            
            // fire change on ARTICLE-MODIFIED
            firePropertyChange("articleModified", null, article);
            
            // set document modified
            setModified();
         }         
         else if (evt.getPropertyName() == "titleChanged") {
            PadArticle article = (PadArticle)evt.getNewValue();
            int index = indexOf(article);

            if (index == 0) {
            	updateTitleFromFirstArticle();
            }
            
            listModel.fireContentsChanged(this, index, index);
            treeModel.fireTreeNodeChanged(article);
            firePropertyChange("articleTitleChanged", null, article);
            
            // set document modified
            setModified();
         }
         
      } 
   }  // ArticleListener
   
   private class DocumentListModel extends AbstractListModel<PadArticle> {

	  @Override
	  public int getSize() {
		  return articleList.size();
	  }

	  @Override
	  public PadArticle getElementAt (int index) {
		  return articleList.get(index);
	  }

	  @Override
	  public void fireContentsChanged (Object source, int index0, int index1) {
		 super.fireContentsChanged(source, index0, index1);
	  }
	
	  @Override
	  public void fireIntervalAdded (Object source, int index0, int index1) {
		 super.fireIntervalAdded(source, index0, index1);
	  }
	
	  @Override
	  public void fireIntervalRemoved (Object source, int index0, int index1) {
		 super.fireIntervalRemoved(source, index0, index1);
	  }
   }

   private class DocumentTreeModel implements TreeModel {
	   
	   private ArraySet<TreeModelListener> listeners = new ArraySet<>();

		@Override
		public Object getRoot() {
			return getArticleCount() == 0 ? null : getArticle(0);
		}
	
		@Override
		public Object getChild (Object parent, int index) {
			int parentIndex = indexOf((PadArticle)parent);
			if (parentIndex > -1) {
				PadArticle[] arr = getChildrenOf(parentIndex);
				if (index >= 0 && index < arr.length) {
					PadArticle a = arr[index];
					return a;
				}
			}
			return null;
		}
	
		@Override
		public int getChildCount (Object parent) {
			int parentIndex = indexOf((PadArticle)parent);
			int count = parentIndex < 0 ? 0 : getChildrenOf(parentIndex).length;
			return count;
		}
	
		@Override
		public boolean isLeaf (Object node) {
			return getChildCount(node) == 0;
		}
	
		@Override
		public void valueForPathChanged (TreePath path, Object newValue) {
			// this is triggered from the tree-cell-editor after change
			if (path == null || newValue == null) return;
			Log.debug(8, "(DefaultPadDocument) -- NEW VALUE FOR TREE-PATH: " + newValue);
			
			PadArticle art = (PadArticle) path.getLastPathComponent();
			art.setTitle((String) newValue);
		}
	
		@Override
		public int getIndexOfChild (Object parent, Object child) {
			if (parent == null || child == null) return -1;
			int parentIndex = indexOf((PadArticle)parent);
			if (parentIndex > -1) {
				PadArticle[] arr = getChildrenOf(parentIndex);
				for (int i = 0; i < arr.length; i++) {
					PadArticle a = arr[i];
					if (a.equals(child)) return i;
				}
			}
			return -1;
		}
	
		@Override
		public void addTreeModelListener (TreeModelListener li) {
			synchronized(listeners) {
				listeners.add(li);
			}
		}
	
		@Override
		public void removeTreeModelListener (TreeModelListener li) {
			synchronized(listeners) {
				listeners.remove(li);
			}
		}
	   
		@SuppressWarnings("unchecked")
		private Collection<TreeModelListener> getListeners () {
			Collection<TreeModelListener> copy;
			synchronized(listeners) {
				copy = (Collection<TreeModelListener>) listeners.clone();
			}
			return copy;
		}
		
		public void fireTreeStructureChanged () {
			Object root = getRoot();
			Object[] path = root == null ? null : new Object[] {getRoot()};
			TreeModelEvent evt = new TreeModelEvent(this, path);
			Collection<TreeModelListener> list = getListeners();
			for (TreeModelListener li : list) {
				li.treeStructureChanged(evt);
			}
		}

		/** Fired to indicate that the given article has changed attributes. 
		 * Does nothing if the argument is null or not an element of the 
		 * document.
		 * 
		 * @param a {@code PadArticle}, may be null
		 */
		public void fireTreeNodeChanged (PadArticle a) {
			if (a == null || indexOf(a) == -1) return;
			
			TreePath parentPath = getArticleTreePath(a).getParentPath();
			int index = getChildIndex(a);
			TreeModelEvent evt;
			if (parentPath == null) {
				evt = new TreeModelEvent(this, parentPath, null, null);
			} else {
				evt = new TreeModelEvent(this, parentPath, new int[] {index}, new Object[] {a});
			}
			Collection<TreeModelListener> list = getListeners();
			for (TreeModelListener li : list) {
				li.treeNodesChanged(evt);
			}
		}

		/** Fired to indicate that the given article has been inserted into the
		 * order of articles of this document. Does nothing if the argument is 
		 * null or not an element of the document.
		 * 
		 * @param a {@code PadArticle}, may be null
		 */
		public void fireTreeNodeInserted (PadArticle a) {
			if (a == null || indexOf(a) == -1) return;
			
			TreePath parentPath = getArticleTreePath(a).getParentPath();
			int index = getChildIndex(a);
			TreeModelEvent evt = new TreeModelEvent(this, parentPath, new int[] {index}, new Object[] {a});
			Collection<TreeModelListener> list = getListeners();
			for (TreeModelListener li : list) {
				if (parentPath == null) {
					// for root insertion
					li.treeStructureChanged(evt);
				} else  {
					li.treeNodesInserted(evt);
				}
			}
		}

		/** Fired to indicate that the given article has been removed from the
		 * order of articles of this document. Does nothing if the argument is 
		 * null.
		 * 
		 * @param a {@code PadArticle}, may be null
		 * @param index int index position of the removed article under its 
		 * 		  parent before the remove
		 */
		public void fireTreeNodeRemoved (PadArticle a, int index) {
			if (a == null) return;

			TreeModelEvent evt;
			PadArticle parent = a.getParent();
			TreePath parentPath = getArticleTreePath(parent);

			Collection<TreeModelListener> list = getListeners();
			if (parent == null) {
				evt = new TreeModelEvent(this, new Object[] {a}, null, new Object[] {a});
				for (TreeModelListener li : list) {
					li.treeStructureChanged(evt);
				}
			} else {
				evt = new TreeModelEvent(this, parentPath, new int[] {index}, new Object[] {a});
				for (TreeModelListener li : list) {
					li.treeNodesRemoved(evt);
				}
			}
		}
   }

@Override
public void setBackupFile(long time) {
	backupModifyTime = time;
}

@Override
public long getBackupTime () {return backupModifyTime;}

@Override
public int getBranchDepth (int index) {
	PadArticle art = getArticle(index);
	int base = art.getOrderDepth();
	int v = 0;
	
	for (PadArticle a : getArticlesAt(index)) {
		v = Math.max(v, a.getOrderDepth() - base);
	}
	return v;
}

@Override
public String getExternalPath () {
	return IO_Manager.get().getExternalFileReference(uuid);
}

@Override
public boolean hasChildren (PadArticle article) {
    int size = articleList.size();
    int index = indexOf(article);
    if (index == -1) {
    	throw new IllegalArgumentException("unknown article: " + article);
    }
    return ++index < size && articleList.get(index).getParent() == article;
}

@Override
public boolean isShowing () {
	return DisplayManager.get().isDocumentShowing(this);
}

@Override
public boolean isBackupCopy () {
	return backupModifyTime > 0;
}

@Override
public boolean isEncrypted () {return passwd != null;}

@Override
public void setEncrypted (byte[] passphrase) {
	byte[] oldValue = passwd;
	passwd = passphrase;
	if ((passphrase != null && oldValue != null && !Util.equalArrays(oldValue, passphrase))
		|| ((passphrase == null) != (oldValue == null))) {
		
		String path = IO_Manager.get().updateExternalFileReference(this);
		setModified();
		
		// push this document's path in the recent-files-stack if it is showing in display
		if (path != null && isShowing()) {
			Global.getRecentFilesStack().push(path);
		}
	}
}

@Override
public byte[] getPassphrase() {return passwd == null ? null : Util.arraycopy(passwd);}

@Override
public Font getOrderviewFont() {
	Font font = options.getFontOption("orderview-font");
	return font;
}

@Override
public void setOrderviewFont (Font font) {
	options.setFontOption("orderview-font", font);
}

@Override
public SystemFileType getFileType() {
	return isEncrypted() ? SystemFileType.TREEPAD_ENCRYPTED : SystemFileType.TREEPAD_DOCUMENT;
}

}