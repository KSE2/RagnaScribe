/*
*  File: DocumentRegistry.java
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.ragna.front.util.SetStackMenu;
import org.ragna.io.IO_Manager;

import kse.utilclass.misc.Log;
import kse.utilclass.misc.UUID;
import kse.utilclass.misc.Util;

/**
 * Class to hold a set of <code>PadDocument</code>s. This class is free of GUI
 * activity, is thread-safe and can be 
 * addressed from anywhere. If a GUI is listening to this registry, it must be
 * assumed that a registered document is graphically displayed and modifications
 * to documents are reflected into the display. 
 * <p><u>Document Selection</u> datum is available in a single-selection modus. 
 * A selection value of <code>null</code> is legal and the original state in
 * the registry. Selection of documents is driven by outside agents except for
 * removing the selected element, which results in a selection value of null.
 * <p><u>Note</u>: There exists a "central document registry" in <code>
 * Global</code>, but this class is not a Singleton.
 * 
 * <p><code>PropertyChangeEvent</code>s are dispatched to registered 
 * listeners:
 * <p><table width=650>
 * <tr><th align=left>KEY</th><th align=left>VALUE NEW</th><th align=left>EVENT</th></tr>
 * <tr><td>documentAdded</td><td><code>PadDocument</code></td><td>document was added to registry</td></tr>
 * <tr><td>documentRemoving</td><td><code>PadDocument</code></td><td>document is about to be removed from registry</td></tr>
 * <tr><td>documentRemoved</td><td><code>PadDocument</code></td><td>document has been removed from registry</td></tr>
 * <tr><td>documentModified</td><td><code>PadDocument</code></td><td>document changed 'modified' status</td></tr>
 * <tr><td>documentSelected</td><td><code>PadDocument</code></td><td>document selection changed (single)</td></tr>
 * 
 * </table> 
 */
public class DocumentRegistry implements Iterable<PadDocument>{

   // Member Variables
   private Map<UUID, PadDocument> docMap = new LinkedHashMap<UUID, PadDocument>();
   private PropertyChangeSupport support = new PropertyChangeSupport(this);
   private DocumentListener documentListener = new DocumentListener();
   private UUID selectedDocumentId;

   public DocumentRegistry () {
   }
   
   
   /** Adds a document to this registry.
    * 
    * @param doc <code>PadDocument</code>
    * @throws NullPointerException
    * @throws IllegalArgumentException if the document is already contained
    */
   public synchronized void add (PadDocument doc) {
	   Objects.requireNonNull(doc);
	   synchronized (docMap) {
	       if (docMap.containsKey(doc.getUUID())) 
	          throw new IllegalArgumentException("document already contained!");
	      
	       // add a map entry for the document
	       docMap.put(doc.getUUID(), doc);
	       Log.debug(8, "(DocumentRegistry.add) added document: " + doc.getTitle() +
	    		  ", UUID=" + doc.getUUID().toHexString());
	   }

	  doc.addPropertyChangeListener("documentModified", documentListener);
      firePropertyChange("documentAdded", null, doc);
      storeSessionList();
   }
   
   /** Returns the number of modified documents in this registry.
    *  
    * @return int
    */
   public int numberModified () {
	   int count = 0;
	   for (PadDocument doc : this) {
		   if (doc.isModified()) count++;
	   }
	   return count;
   }
   
   public void remove (PadDocument doc) {
      if ( doc != null ) {
         remove(doc.getUUID());
      }
   }
   
   public synchronized PadDocument remove (UUID uuid) {
	  PadDocument doc = getDocument(uuid);
	   
      if (doc != null) {
	     firePropertyChange("documentRemoving", null, doc);
    	 synchronized (docMap) {
    		doc.removePropertyChangeListener("documentModified", documentListener);
		    docMap.remove(uuid);
	     }

    	 IO_Manager.get().removeExternalFileReference(doc);
         Log.debug(8, "(DocumentRegistry.remove) removed document: " + doc.getTitle() +
        		 ", UUID=" + doc.getUUID().toHexString());
         firePropertyChange("documentRemoved", null, doc);

         if (uuid.equals(selectedDocumentId)) {
            setSelectedDocument(null);
         }
         
         storeSessionList();
      }
      return doc; 
   }
   
   /** Returns the document with the given UUID or null if such an object
    * is not available.
    * 
    * @param uuid UUID document UUID
    * @return <code>PadDocument</code> or null
    * @throws NullPointerException if parameter is null
    */
   public PadDocument getDocument (UUID uuid) {
	   synchronized (docMap) {
		   return docMap.get(uuid);
	   }
   }
   
   /** Returns the currently selected document or null if no document is
    * available in this registry.
    *  
    * @return <code>PadDocument</code> or null
    */
   public PadDocument getSelectedDocument () {
      return selectedDocumentId == null ? null :  getDocument(selectedDocumentId);
   }
   
   /** Whether the given file path represents a currently registered document.
    * For a null path false is returned.
    * 
    * @param path String document file path, may be null
    * @return boolean true == referred document is registered, false == path unknown
    */
   public boolean isRegisteredDocument (String path) {
      if (path != null) { 
         for (Iterator<PadDocument> it = iterator(); it.hasNext();) {
            PadDocument doc = it.next();
            String docPath = doc.getExternalPath();
            if (path.equals(docPath)) {
               return true;
            }
         }
      }
      return false;
   }
   
   /** Whether the given document is registered in this registry.
    * 
    * @param document {@code PadDocument}
    * @return boolean
    */
   public boolean isRegisteredDocument (PadDocument document) {
	   return getDocument(document.getUUID()) != null;
   }
   
   /** Sets the currently selected document by its UUID value. Null stands
    * for no document selected (which should be equivalent to no document
    * loaded).
    * 
    * @param documentId UUID selected document, may be null
    */
   public void setSelectedDocument (UUID documentId) {
	  Log.log(6, "(DocumentRegistry.setSelectedDocument) selected document = " + documentId);
      PadDocument document = getDocument(documentId);
      if (documentId != null && document == null) 
         throw new IllegalArgumentException("unknown document: ".concat(documentId.toString()));

      UUID oldSelected = selectedDocumentId;
      selectedDocumentId = documentId;
      if (Util.notEqual(oldSelected, selectedDocumentId)) {
         firePropertyChange("documentSelected", null, document);
      }
   }
   
   private void firePropertyChange (String property, Object oldValue, Object newValue) {
      Log.debug(8, "(DocumentRegistry) firing property change: ".concat(property));
      support.firePropertyChange(property, oldValue, newValue);
   }
   
   /** Returns the number of documents available in this registry.
    * 
    * @return int number of documents
    */
   public int size () {
      return docMap.size();
   }
   
   /** Returns an iterator comprising all registered documents in the order
    * of insertion. The iterator refers to a snapshot of the current registry
    * state; modifications to the registry while traversing the iterator are
    * possible, even outside of the iterator.
    * 
    * @return <code>Iterator</code> of <code>PadDocument</code>
    */
   @Override
   public Iterator<PadDocument> iterator () {
	   synchronized (docMap) {
		   ArrayList<PadDocument> list = new ArrayList<PadDocument>(docMap.values());
	       return list.iterator();
	   }
   }
   
   
   public void addPropertyChangeListener (PropertyChangeListener listener) {
      synchronized (support) {
         List<?> list = Arrays.asList(support.getPropertyChangeListeners());
         if (!list.contains(listener)) {
            support.addPropertyChangeListener(listener);
         }
      }
   }
   
   public void addPropertyChangeListener (String propertyName, PropertyChangeListener listener) {
      synchronized (support) {
         List<?> list = Arrays.asList(support.getPropertyChangeListeners(propertyName));
         if (!list.contains(listener)) {
            support.addPropertyChangeListener(propertyName, listener);
         }
      }
   }
   
   public void removePropertyChangeListener (PropertyChangeListener listener) {
      synchronized (support) {
         support.removePropertyChangeListener(listener);
      }
   }
   
   public void removePropertyChangeListener (String propertyName, PropertyChangeListener listener) {
      synchronized (support) {
         support.removePropertyChangeListener(propertyName, listener);
      }
   }
   
   private void storeSessionList () {
       SetStackMenu sessionList = new SetStackMenu();

       // collect filepaths from open documents
       for (PadDocument document : this) {
           String filepath = document.getExternalPath();
           if (filepath != null) {
               sessionList.add(filepath);
           }
       }
       
       if (!sessionList.isEmpty()) {
	       // put the session list to program options
	       String content = sessionList.getStringContent(';');
	       Global.getOptions().setOption("recentSessionContent", content);
	       Log.debug(10, "(DocumentRegistry) saving session content: ".concat(content));
       }
   }
   
// -------------- inner classes ----------------
   private class DocumentListener implements PropertyChangeListener {

   @Override
   public void propertyChange (PropertyChangeEvent evt) {
      if (evt.getPropertyName() == "documentModified") {
         PadDocument document = (PadDocument) evt.getSource();
//         Log.log(6, "(DocumentRegistry.DocumentListener) ------ EVENT: document modified");

         // fire property change event for DOCUMENT MODIFIED
         firePropertyChange("documentModified", null, document);
      }
   }
      
   }
   
}
