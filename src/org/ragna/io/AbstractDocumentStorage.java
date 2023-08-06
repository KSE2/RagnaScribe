/*
*  File: AbstractDocumentStorage.java
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import org.ragna.core.PadDocument;
import org.ragna.exception.UnknownPathException;

import kse.utilclass.misc.UUID;

public abstract class AbstractDocumentStorage implements DocumentStorage {

   protected Map<UUID, String> map = new HashMap<>();

   private String rootPath;
   
   
   @Override
   public synchronized void storeDocument (PadDocument document) throws IOException {
	  Objects.requireNonNull(document, "document is null");
      UUID uuid = document.getUUID();

      if (requiresPaths()) {
         String path = map.get(uuid);
         if (path == null) {
            throw new UnknownPathException("no path for document: ".concat(
                  document.getShortTitle()));
         }
         basicWrite(document, path);
      } else {
         basicWrite(document);
      }
   }

   @Override
   public synchronized void storeDocument (PadDocument document, String path) throws IOException {
	  Objects.requireNonNull(document, "document is null");
	  Objects.requireNonNull(path, "path is null");
      if (!supportsPaths())
         throw new UnsupportedOperationException();
      
      UUID uuid = document.getUUID();
      if (requiresPaths()) {
         if (!isValidPath(path)) {
            throw new IllegalArgumentException("illegal path format");
         }
         basicWrite(document, path);
      } else {
         basicWrite(document);
      }
      
      // associate path to UUID
      map.put(uuid, path);
   }

   @Override
   public synchronized PadDocument retrieveDocument (UUID uuid, String path) throws IOException {
      if (path == null & uuid == null)
         throw new IllegalArgumentException("parameters are null");

      PadDocument doc = null;
      boolean isRead = false;

      // first rank: look for UUID reading if parameter available
      if (uuid != null) {
         try {
            doc = basicRead(uuid);
            isRead = true;
         } catch (UnsupportedOperationException e) {
         }
      }

      // second rank: read from path device if parameter is available
      if (!isRead & path != null && supportsPaths()) {
         doc = retrieveDocument(path);
      }
      
      // throw exception if file was not retrieved
      if (doc == null) {
         throw new FileNotFoundException();
      }
      return doc;
   }

   @Override
   public synchronized PadDocument retrieveDocument (String path) 
          throws IOException {
      if (path == null)
         throw new IllegalArgumentException("path is null");
      if (!supportsPaths())
         throw new UnsupportedOperationException();

      path = storageFullPath(path);
      if (!isValidPath(path)) { 
         throw new IllegalArgumentException("illegal path format");
      }
      
      // read document
      PadDocument document = basicRead(path);
      
      // associate path to UUID
      if (document != null) {
         map.put(document.getUUID(), path);
      }
      return document;
   }

   @Override
   public boolean isFullPath (String path) {
      File f = new File(path);
      return f.isAbsolute();
   }
   
   /** If the given path is a relative path then a complete full-path is
    * returned by prepending the root path (defined in this class) to the given
    * path. If the given path is a full-path then the argument is returned .
    * 
    * @param path String storage path (full or relative)
    * @return String full storage path
    */
   private String storageFullPath (String path) {
      if (rootPath == null || isFullPath(path)) {
         return path;
      }
      return rootPath.concat(path);
   }
   
   @Override
   public void setRootPath (String path) {
      if (path == null) {
         rootPath = null;
      } else {
         if (!isFullPath(path))
            throw new IllegalArgumentException();
         
         // TODO nachdenken über normalisierung
         rootPath = path;
      }
   }

   @Override
   public String getRootPath () {
      return rootPath;
   }

   @Override
   public synchronized String getDocumentPath (UUID uuid) {
      if (uuid == null)
         throw new IllegalArgumentException("uuid is null");
      
      return map.get(uuid);
   }

   @Override
   public synchronized void setDocumentPath (UUID uuid, String path) {
      if (uuid == null)
         throw new IllegalArgumentException("uuid is null");
      if (path == null)
         throw new IllegalArgumentException("path is null");
      
      // associate path to UUID
      map.put(uuid, path);
   }

   @Override
   public synchronized Iterator<UUID> iterator () {
      return map.keySet().iterator();
   }

   /** Fundamental read-by-UUID operation to be implemented by derived 
    * storage classes. If the class does not support read-by-UUID (mapping
    * UUIDs to storage objects) it must throw the <code>
    * UnsupportedOperationException</code>. 
    * <p>The basic read operation reads and reconstructs the document safely 
    * from the medium it is designed for. The addressed object has been prior
    * written by a basic write operation.
    * 
    * @param uuid UUID <code>PadDocument</code> identifier
    * @throws FileNotFoundException if the given UUID is unknown
    * @throws IOException
    * @throws UnsupportedOperationException if this service does not support
    *         read-by-UUID    
    */
   abstract protected PadDocument basicRead (UUID uuid) throws IOException;
   
   /** Fundamental read-by-filepath operation to be implemented by derived 
    * storage classes. If the class does not support read-by-filepath (mapping
    * UUIDs to storage objects) it must throw the <code>
    * UnsupportedOperationException</code>. 
    * <p>The basic read operation reads and reconstructs the document safely 
    * from the medium it is designed for. The addressed object has been prior
    * written by a basic write operation.
    * 
    * @param path String <code>PadDocument</code> path identifier
    * @throws FileNotFoundException if the given path is unknown
    * @throws IOException
    * @throws UnsupportedOperationException if this service does not support
    *         read-by-filepath    
    */
   protected PadDocument basicRead (String path) throws IOException {
	   throw new UnsupportedOperationException();
   }
   
   /** Fundamental write-by-UUID operation to be implemented by derived 
    * storage classes. If the class does not support write-by-UUID (mapping
    * UUIDs to storage objects) it must throw the <code>
    * UnsupportedOperationException</code>. 
    * <p>The basic write operation stores the document safely to the medium
    * it is designed for in a way that the document can be completely retrieved 
    * from this, possibly persistent, state at a later time.
    * 
    * @param document <code>PadDocument</code>
    * @throws IOException
    * @throws UnsupportedOperationException if this service does not support
    *         write-by-UUID    
    */
   abstract protected void basicWrite (PadDocument document) throws IOException;

   /** Fundamental write-by-filepath operation to be implemented by derived 
    * storage classes. If the class does not support write-by-filepath (mapping
    * file paths to storage objects) it must throw the <code>
    * UnsupportedOperationException</code>.
    * <p>The basic write operation stores the document safely to the medium
    * it is designed for in a way that the document can be completely retrieved 
    * from this, possibly persistent, state at a later time.
    * 
    * @param document <code>PadDocument</code>
    * @param path String file path
    * @throws IOException
    * @throws UnsupportedOperationException if this service does not support
    *         write-by-filepath    
    */
   protected void basicWrite (PadDocument document, String path) throws IOException {
	   throw new UnsupportedOperationException();
   }
   
}
