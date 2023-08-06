/*
*  File: DocumentStorage.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.ragna.core.PadDocument;
import org.ragna.exception.UnknownPathException;

import kse.utilclass.misc.UUID;

/** Service interface to exchange pad-documents with a medium for persistent
 * or intermediary states. 
 * 
 * <p>The policy 
 */
public interface DocumentStorage {

   /** Stores the given document in this storage service. 
    * <p><small>How the document is stored depends on the implementation of
    * the service. However, if an external path is known in this service
    * for the given document, it is used for external storage. If an external
    * path is required but unknown, an exception is thrown.</small>
    * 
    * @param document <code>PadDocument</code>
    * @throws UnknownPathException if this service requires an external 
    *         document path but there is no path associated 
    * @throws IOException if an IO error occurred
    */
   void storeDocument (PadDocument document) throws IOException;
   
   /** Stores the given document under the specified external path. On
    * successful storage, the external path is associated to the UUID value
    * of the document.
    * 
    * @param document <code>PadDocument</code>
    * @param path String storage path
    * @throws IOException if an IO error occurred
    * @throws IllegalArgumentException if the path is falsely formatted
    * @throws UnsupportedOperationException if this service does not support 
    *         external document paths    
    */
   default void storeDocument (PadDocument document, String path) throws IOException {
	   throw new UnsupportedOperationException();
   }

   /** Attempts to retrieve and read a pad-document from this service's
    * environment either by a known UUID identifier or a valid storage path 
    * for the document. 
    * <p><small>UUID is preferred if supplied and available. 
    * The storage path uniquely identifies a document in the service 
    * environment if the service supports external paths.</small>
    * 
    * @param uuid UUID document identifier, may be null
    * @param path String storage path, may be null
    * @return <code>PadDocument</code>
    * @throws FileNotFoundException if the given identifiers are unknown
    * @throws IOException if an IO error occurred
    * @throws IllegalArgumentException if both parameters are null or the 
    *         path is falsely formatted
    * @throws UnsupportedOperationException if a path was requested and
    *         this service does not support external document paths    
    */
   PadDocument retrieveDocument (UUID uuid, String path) throws IOException;
   
   /** Attempts to retrieve and read a pad-document from this service's
    * environment. The storage path uniquely identifies
    * a document in the service environment.
    * <p><small>A successful return of the request overwrites any previous 
    * association of the document's UUID value and the
    * rendered document. Some services may not support document paths
    * and will throw UnsupportedOperationException.</small>
    * 
    * @param path String storage path
    * @return <code>PadDocument</code>
    * @throws FileNotFoundException if the given path is unknown
    * @throws IOException if an IO error occurred
    * @throws IllegalArgumentException if the path is falsely formatted
    * @throws UnsupportedOperationException if this service does not support
    *         external document paths    
    */
   default PadDocument retrieveDocument (String path) throws IOException {
	   throw new UnsupportedOperationException();
   }
   
   /** Sets the root path in this service that is prepended to relative path
    * notifications. The given path must be absolute and end with a separator
    * expression in case such expressions are used to combine path elements.
    *  
    * @param path String root path or null for clearing the current setting
    * @throws IllegalArgumentException if path is not absolute or not properly
    *         formatted
    */
   default void setRootPath (String path) {}
   
   /** Whether the given path is a full path.
    * 
    * @param path String
    * @return boolean true = argument is full path
    */
   default boolean isFullPath (String path) {return false;}
   
   /** Whether the given path is a valid path expression. Valid path expressions
    * can be full or relative paths.
    * 
    * @param path String
    * @return boolean true = argument is valid, false = argument is not valid
    */
   default boolean isValidPath (String path) {return false;}
   
   /** Returns the root path in this service that is prepended to relative
    * path notifications.
    *  
    * @return String root path or null if no root path is set
    */
   default String getRootPath () {return null;}

   /** Returns a storage path for the given pad-document UUID if such a 
    * path is known to exist.
    * <p><small>The storage path is updated when a document is stored or 
    * retrieved in this service. The storage path uniquely identifies
    * a document in the service environment. In some environments the
    * storage path may not be used and null is returned even if the document
    * is available. For probing availability, methods 
    * 'hasDocument()' should be used.</small>
    * 
    * @param uuid UUID document identifier
    * @return String storage path in this device or null if unknown
    */
   default String getDocumentPath (UUID uuid) {return null;}
   
   /** Sets the association of document UUID and external path. A path value
    * of null removes an association.
    * 
    * @param uuid UUID document identifier
    * @param path String external path or null
    * @throws IllegalArgumentException if the path is falsely formatted
    * @throws UnsupportedOperationException if this service does not support 
    *         external document paths    
    */
   default void setDocumentPath (UUID uuid, String path) {}
   
   /** Whether a pad-document with the given UUID is known to exist in this
    * service. This probes whether the document's external state is available
    * but does not test whether it can be read from the medium at this time.
    * If the by-uuid method is not supported, false is returned.
    * 
    * @param uuid UUID document identifier
    * @return boolean true == document is known
    */
   boolean hasDocument (UUID uuid);
   
   /** Whether a pad-document is known to exist under the given path in this
    * service. This probes whether the document's external state is available
    * but does not test whether it can be read from the medium at this time.
    * If the by-filepath method is not supported, false is returned.
    * 
    * @param path String document path
    * @return boolean true == document is available
    */
   default boolean hasDocument (String path) {return false;}
   
   /** Whether this service supports external path associations.
    * @return boolean
    */
   default boolean supportsPaths () {return false;}
   
   /** Whether this service requires external path associations to store 
    * documents.
    * 
    * @return boolean
    */
   default boolean requiresPaths () {return false;}
   
   /** Iterator of all known document UUIDs in this storage service.
    * 
    * @return Iterator of document UUID
    */
   Iterator<UUID> iterator ();
   
}
