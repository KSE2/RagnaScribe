/*
*  File: MemoryDocumentStorage.java
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
import java.util.HashMap;
import java.util.Map;

import org.ragna.core.PadDocument;

import kse.utilclass.misc.UUID;

public class MemoryDocumentStorage extends AbstractDocumentStorage {
   private Map<UUID, PadDocument> docMap = new HashMap<>();
   
   
   public MemoryDocumentStorage() {
   }

   @Override
   public boolean hasDocument (UUID uuid) {
      if (uuid == null)
         throw new IllegalArgumentException("uuid is null");
      return docMap.containsKey(uuid);
   }

   @Override
   protected PadDocument basicRead (UUID uuid) throws IOException {
      PadDocument doc = docMap.get(uuid);
      if (doc == null) {
         throw new FileNotFoundException("unavailable document: ".concat(
               uuid.toString()));
      }
      return doc;
   }

   @Override
   protected void basicWrite (PadDocument document) throws IOException {
      docMap.put(document.getUUID(), document);
   }

}
