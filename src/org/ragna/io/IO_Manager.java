/*
*  File: IO_Manager.java
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import org.ragna.core.ActionHandler;
import org.ragna.core.DefaultOptions;
import org.ragna.core.DefaultPadDocument;
import org.ragna.core.Global;
import org.ragna.core.PadArticle;
import org.ragna.core.PadDocument;
import org.ragna.core.PadDocument.DocumentType;
import org.ragna.exception.FileInUseException;
import org.ragna.exception.UnknownFileFormatException;
import org.ragna.front.GUIService;
import org.ragna.util.OptionBag;

import kse.com.fhash.main.CipherInputFile;
import kse.com.fhash.main.CipherOutputFile;
import kse.com.fhash.main.FHL_FileInfo;
import kse.com.fhash.main.Functions;
import kse.utilclass.io.IOService;
import kse.utilclass.misc.DirectByteOutputStream;
import kse.utilclass.misc.IntResult;
import kse.utilclass.misc.Log;
import kse.utilclass.misc.UUID;
import kse.utilclass.misc.Util;
import kse.utilclass2.io.LayeredFileSafe;

/** IO_Manager is a singleton fundamental system class and handles basic 
 * pad-document opening and saving as well as general file access controlling.
 * Furthermore the manager organises the saving of history files to pad-
 * documents whenever such a file-type is saved.  
 * <p>For all files controlled by this manager it holds that they can be accessed
 * only by a single thread for a single IO-direction at a time. Concurring 
 * threads have to wait until the current operation on a file has finished.
 * 
 * <p>For pad-documents this manager keeps a mapping of their related external 
 * filepaths.
 * 
 * <p>This class issues property change events to indicate changes in the 
 * mapping of pad-documents. 
 * <p><table width=650>
 * <tr><th align=left>KEY</th><th align=left>VALUE NEW</th><th align=left>VALUE OLD</th><th align=left>EVENT</th></tr>
 * <tr><td>pathAdded</td><td><code>String</code></td><td><code>null</code></td><td>document path was added</td></tr>
 * <tr><td>pathRemoved</td><td><code>null</code></td><td><code>String</code></td><td>document path has been removed</td></tr>
 * <tr><td>pathReplaced</td><td><code>String</code></td><td><code><code>String</code></code></td><td>document path replaced</td></tr>
 * <tr><td>documentSaved</td><td><code>PadDocument</code></td><td><code>null</code></td><td>document saved to external</td></tr>
 * <tr><td>documentDeleted</td><td><code>null</code></td><td><code>PadDocument</code></td><td>document file removed</td></tr>
 * </table> 
 */
public class IO_Manager {

   private static IO_Manager instance = new IO_Manager();
   public static IO_Manager get () {
      return instance;
   }

   private static final int STREAM_BUFFER_SIZE = 4096;
   private static final int COMPRESSION_THRESHOLD = 10000;
   private static final int HISTORY_DAYS = 10;
   private static final int HISTORY_MONTHS = 6;
   private static final int HISTORY_YEARS = 3;
   
   public static enum StreamDirection {INPUT, OUTPUT};
   public static enum SystemFileType {ANY_FILE, ANY_PAD_FILE, ANY_DOCUMENT_FILE, 
                DOCUMENT_FILE, DOCUMENT_BACKUP, TREEPAD_DOCUMENT, TREEPAD_ENCRYPTED, 
                RAGNA_DOCUMENT};
   
   private DocumentPropertyChangeListener listener = new DocumentPropertyChangeListener();
   private PropertyChangeSupport support = new PropertyChangeSupport(this);

   private Map<UUID, String> externMap = new Hashtable<UUID, String>();   
   private LayeredFileSafe         	fileSafe;
   
   // these locks are used to serialise parallel document open and save operations
   private Object 					saveLock = new Object();
   private Object 					openLock = new Object();

   
   private IO_Manager () {
	   setFileHistoryDir(Global.getHistoryDirectory());
   }
                
   public void addPropertyChangeListener (PropertyChangeListener listener) {
      List<?> list = Arrays.asList(support.getPropertyChangeListeners());
      if (!list.contains(listener)) {
         support.addPropertyChangeListener(listener);
      }
   }

   public void removePropertyChangeListener (PropertyChangeListener listener) {
      support.removePropertyChangeListener(listener);
   }

   private void firePropertyChange (String property, Object oldValue, Object newValue) {
	      Log.log(10, "(IO_Manager) firing change event: " + property);
	      support.firePropertyChange(property, oldValue, newValue);
	   }
	   
   public String getFileTypeExtensions (SystemFileType fileType) {
      if (fileType == null)
         throw new NullPointerException();
      
      String values;
      switch (fileType) {
      case DOCUMENT_FILE:  
         values = "hjt,dat,fhl";
         break;
      case TREEPAD_DOCUMENT:  
         values = "hjt,fhl";
         break;
      case TREEPAD_ENCRYPTED:  
          values = "hjt.fhl,fhl";
          break;
      case RAGNA_DOCUMENT:  
         values = "dat,fhl";
         break;
      case DOCUMENT_BACKUP:  
         values = "bak,sav,hjt~";
         break;
      case ANY_PAD_FILE:  
      case ANY_DOCUMENT_FILE:  
         values = "bak,sav,hjt~,hjt,dat,fhl";
         break;
      default : values = "*";
      }
      return values;
   }
   
   public String getFileTypeDescription (SystemFileType fileType) {
      if (fileType == null)
         throw new NullPointerException();
      
      String value;
      switch (fileType) {
      case DOCUMENT_FILE:  
         value = ActionHandler.displayText("filetype.document");
         break;
      case DOCUMENT_BACKUP:  
         value = ActionHandler.displayText("filetype.backup");
         break;
      case TREEPAD_DOCUMENT:  
         value = ActionHandler.displayText("filetype.treepad.doc");
         break;
      case RAGNA_DOCUMENT:  
         value = ActionHandler.displayText("filetype.ragna.doc");
         break;
      case ANY_PAD_FILE:  
      case ANY_DOCUMENT_FILE:  
         value = ActionHandler.displayText("filetype.any.pad");
         break;
      default : value = ActionHandler.displayText("filetype.any");
      }
      return value;
   }
   
   /** Writes the given pad-document to the given external file by applying
    * the appropriate writer module. This produces a cleartext or an 
    * encrypted output, depending on the "encryption" property of the document. 
    * <p>NOTE: This method does not update the file history or the external 
    * file map and does not reset the 'isModified' property of the document.
    * 
    * @param doc <code>PadDocument</code>
    * @param file File output destination
    * @param encoding String charset name or null for global default
    * @throws IllegalCharsetNameException
    * @throws UnsupportedCharsetNameException
    * @throws IOException
    */
   public void saveDocument (PadDocument doc, File file, String encoding) throws IOException {
	   Objects.requireNonNull(doc, "document is null");
	   Objects.requireNonNull(file, "file is null");
	   OutputStream output = null;
       try {
	      // obtain basic IO file access (output stream)
		  try {
			 output = IOService.get().getBlockingFileOutputStream(file);
		  } catch (InterruptedException e) {
			 throw new IOException("waiting for file-access interrupted, e");
		  }
		  
	      // create output stream and write file
	      Log.debug(8, "(IO_Manager.saveDocument) saving document to file: ".concat(file.getAbsolutePath()) );
	      saveDocument(doc, output, encoding);
	      
       } catch (IOException e) {
    	  IOService.get().releaseFileAccess(file);
    	  throw e;
       } finally {
    	   if (output != null) {
   		      output.close();
    	   }
       }
   }
   
   /** Attempts to retrieve document options, walking from the given external 
    * filepath. Document options are located in Java preferences and
    * are accessible through either the filepath or the document's UUID value.
    * The UUID value is attempted to be read from the input stream.
    * This method performs to search for both if necessary. 
    * 
    * @param in InputStream serialisation data stream of a pad-document 
    *           (decrypted)
    * @return {@code OptionBag} or null if not successful
    * @throws IOException
    */
   private OptionBag detectDocumentOptions (InputStream in, String filepath) throws IOException {
	  OptionBag options = DefaultPadDocument.getDocumentOptionsFromPath(filepath);
	  if (options != null) {
		  Log.log(8, "(IO_Manager.detectDocumentOptions) retrieved document options from path: " + filepath);
		  return options;
	  }

	  // attempt at UUID hook
	  UUID uuid = TreepadReader.readUUID(in);
	  if (uuid != null) {
		  options = DefaultPadDocument.getDocumentOptionsFromID(uuid);
		  if (options != null) {
			  Log.log(8, "(IO_Manager.detectDocumentOptions) retrieved document options from UUID: " + uuid);
			  return options;
		  }
	  }
	  return null;
   }
   
   /** Writes the given pad-document to the given output stream by applying
    * the appropriate writer module. This produces a cleartext or an 
    * encrypted output, depending on the "encryption" property of the document. 
    * Does not close the output stream.
    * <p>NOTE: This method is ignorant of a file, hence cannot update the file
    * history or the external file map. It does not reset the 'isModified' 
    * property of the document.
    * 
    * @param doc <code>PadDocument</code>
    * @param output OutputStream
    * @param encoding String charset name or null for global default
    * @throws IllegalCharsetNameException
    * @throws UnsupportedCharsetNameException
    * @throws IOException
    */
   public void saveDocument (PadDocument doc, OutputStream output, String encoding) 
		   		throws IOException {
	  if (doc.isEncrypted()) {
		  writeDocumentEncrypted(doc, output, encoding, doc.getPassphrase());
	  } else {
		  writeDocumentCleartext(doc, output, encoding);
	  }
   }
   
   /** Writes the given pad-document to the given output stream by applying
    * the appropriate writer module. This produces an encrypted output 
    * irrespective of the encryption property of the document; the passphrase
    * argument is required. Does not close the output stream.
    * 
    * @param doc <code>PadDocument</code>
    * @param output OutputStream
    * @param encoding String charset name or null for a global default value
    * @param key byte[] encryption key
    * @throws IllegalCharsetNameException
    * @throws UnsupportedCharsetNameException
    * @throws IOException
    */
   private void writeDocumentEncrypted (PadDocument doc, OutputStream output, 
                             String encoding, byte[] key) throws IOException {
	  Objects.requireNonNull(doc, "document is null");
	  Objects.requireNonNull(output, "output is null");
	  Objects.requireNonNull(key, "key is null");
      
      InputStream input = encryptedDocumentInputStream(doc, key, encoding);
      try {
		 Util.transferData(input, output, STREAM_BUFFER_SIZE);
      } catch (InterruptedException e) {
    	  throw new IOException("thread interrupted", e);
      } finally {
    	  input.close();
      }
   }
   
   /** Writes the given pad-document to the given output stream by applying
    * the appropriate writer module. This produces a cleartext output, 
    * irrespective of the "encryption" property of the document. 
    * Does not close the output stream.
    * 
    * @param doc <code>PadDocument</code>
    * @param output OutputStream
    * @param encoding String charset name or null for a global default value
    * @throws IllegalCharsetNameException
    * @throws UnsupportedCharsetNameException
    * @throws IOException
    */
   private void writeDocumentCleartext (PadDocument doc, OutputStream output, 
                             String encoding) throws IOException {
	  Objects.requireNonNull(doc, "document is null");
	  Objects.requireNonNull(output, "output is null");
      
      DocumentWriter writer;
      if (doc.getDocType() == DocumentType.TreePad) {
         writer = new TreepadWriter(encoding);
      } else {
         throw new UnsupportedOperationException("no WRITER class available for this type of file");
      }
      
      writer.write(output, doc);
   }
   
   /** Encrypts the given document with the given key material and renders
    * the encrypted results as an input-stream. The temporary file containing
    * this stream gets deleted when the resulting input stream is closed.
    * <p>Currently the size limit is 2.1 G.
    * 
    * @param doc {@code PadDocument}
    * @param key byte[]
    * @param encoding String charset name or null for VM default
    * @return {@code InputStream}
    * @throws IOException 
    */
   @SuppressWarnings("resource")
   private InputStream encryptedDocumentInputStream (PadDocument doc, byte[] key, 
		   			   String encoding) throws IOException {
	   Objects.requireNonNull(doc, "document is null");
	   Objects.requireNonNull(key, "key is null");
	   
	   InputStream input;
	   OutputStream out;
	   DirectByteOutputStream bout;
	   FHL_FileInfo info = new FHL_FileInfo();
	   info.fileName = "noname";
	   info.storeTime = System.currentTimeMillis();
	   info.fileTime = info.storeTime;

	   // create the document cleartext serialisation (internal)
//	   File clrOutF = File.createTempFile("ragna-", ".tmp");
//	   out = new FileOutputStream(clrOutF);
	   bout = new DirectByteOutputStream(STREAM_BUFFER_SIZE);
	   writeDocumentCleartext(doc, bout, encoding);
//	   out.close();
	   long origLength = bout.size();
	   info.fileLength = origLength;
//	   input = new FileInputStream(clrOutF);
	   input = new ByteArrayInputStream(bout.getBuffer(), 0, bout.size());

	   if (origLength > COMPRESSION_THRESHOLD) {
		   // create a ZIP input from cleartext stream
		   IntResult ires = new IntResult();
		   input = Util.getZipInputStream(input, origLength, ires);
		   info.compression = 1;
		   info.fileCrc = ires.getValue();
		   info.comment = "compressed Ragna Scribe text document: " + doc.getTitle();
	   } else {
		   info.comment = "Ragna Scribe text document: " + doc.getTitle();
	   }
	   
	   // remove cleartext file
//	   clrOutF.delete();
	   
	   // create the ciphertext output file (intermediary in TEMP)
	   File outF = File.createTempFile("ragna-", ".enc");
	   CipherOutputFile coutFile = new CipherOutputFile(outF, info, key);
	   out = coutFile.getOutputStream();
	   try {
		   Util.transferData(input, out, STREAM_BUFFER_SIZE);
	   } catch (InterruptedException e) {
		   throw new IOException("interrupted while writing");
	   } finally {
		   out.close();
	   }

	   // open an input stream of encrypted data
	   input = new FileInputStream(outF) {
		  @Override
		  public void close() throws IOException {
			 super.close();
			 outF.delete();
		  }
	   };
	   return input;
   }
   
   /** Deletes the external data-file of the given document, if it exists,
    * and also deletes any history files associated to the argument. 
    * <p>This does not remove the pad-document from any container and does not
    * remove the filepath entry in this manager.
    * 
    * @param doc {@code PadDocument}, may be null
    * @return boolean true = file was removed, false = file possibly still 
    *         exists or was not found
    * @throws InterruptedException if the calling thread was interrupted while 
    *         waiting for file access
    */
   public boolean deleteDocumentFile (PadDocument doc) throws InterruptedException {
	   boolean ok = false;
	   if (doc != null) {
		   String path = getExternalFileReference(doc.getUUID());
		   if (path != null) {
			   File file = new File(path); 
			   IOService.get().acquireFileAccess(file);
			   try {
				   ok = file.delete();
				   fileSafe.clearFile(file);
			   } catch (IOException e) {
				   e.printStackTrace();
			   } finally {
				   IOService.get().releaseFileAccess(file);
				   firePropertyChange("documentDeleted", doc, null);
			   }
		   }
	   }
	   return ok;
   }
   
   /** Deletes the given file of the file system if it is not registered as
    * an open document file. Otherwise an exception is thrown.
    *  
    * @param file File file to remove
    * @return boolean true = file was deleted, false = file was not deleted
    *         due to system failures (the return value of 'File.delete()')
    * @throws FileInUseException (IOException) if the file cannot be deleted
    *  		  because it is in a systemic use context
    * @throws IOException 
    */
   public boolean deleteFileIfNotRegistered (File file) throws IOException {
	   if (isExternalFileReference(file.getCanonicalPath())) 
		   throw new FileInUseException();
	   return file.delete();
   }
   
   /** Deletes all history files of the given pad-document.
    * 
    * @param document {@code PadDocument}
    * @return boolean true = all files deleted, false = some files remain
    * @throws IOException 
    */
   public boolean deleteFileHistory (PadDocument document) throws IOException {
	   String path = getExternalFileReference(document.getUUID());
	   return path == null ? true : deleteFileHistory(new File(path));
   }
   
   /** Deletes all history files of the given cardinal file.
    * 
    * @param file File cardinal (original) file reference
    * @return boolean true = all files deleted, false = some files remain
    * @throws IOException 
    */
   public boolean deleteFileHistory (File file) throws IOException {
	   fileSafe.clearFile(file);
	   return true;
	   
//	   boolean allFiles = true;
//	   List<File> files = fileSafe.getFiles(file);
//  	   if (files != null) {
//		 for (File f : files) {
//			 try {
//				 allFiles &= deleteFileIfNotRegistered(f);
//			 } catch (IOException e) {
//				 allFiles = false;
//			 }
//		 }
//  	   }
// 	   return allFiles;
   }
   
   /** The only systematic document save operation. Writes the given pad-document
    * to the destination that is associated to it in this manager. If no 
    * filepath is associated, then the user is asked to select an output file 
    * via GUI interaction, and the new path will become associated. 
    * <p>For <i>Treepad</i> files the given character encoding is used, if given,
    * otherwise the global default value for <i>Treepad</i> files.
    * This method updates the external file reference in this manager and the 
    * file history with a copy of the saved file.
    * 
    * @param doc <code>PadDocument</code>
    * @param encoding String charset name or null for global default
    * @return File the descriptor of the saved file or null if this 
    *         function was broken
    * @throws IllegalCharsetNameException
    * @throws UnsupportedCharsetNameException
    * @throws IllegalArgumentException
    * @throws IOException
    * @throws InterruptedException if the calling thread was interrupted while 
    *         waiting for file access
    */
   public File saveDocument (PadDocument doc, String encoding) throws IOException, InterruptedException {
      
      // obtain the output file path
      File outFile;
      String filepath = externMap.get(doc.getUUID());
      boolean isNewFile = filepath == null;
      if (isNewFile) {
     	 synchronized (saveLock) {
	        while (filepath == null) {
	           // call for user input on file selection
	           outFile = GUIService.getUserSelectedFile(doc.getFileType(), StreamDirection.OUTPUT, doc.getShortTitle());
	           if (outFile == null) return null;
	         
	           // check validity of output file
	           filepath = outFile.getAbsolutePath();
	           if (isExternalFileReference(filepath)) {
	        	   String text = Global.res.getDisplay("msg.illegal.filepath").concat(filepath);
	        	   GUIService.failureMessage("dlg.operrejected", text, null);
	        	  filepath = null;
	           }
	        }
    	 }
      }
      
   	  // take the registered or chosen file path
      outFile = new File(filepath);
      
      // assert: an output file address was defined
      
	  // modify out-file name if necessitated by encryption property
	  if (doc.isEncrypted() && !filepath.endsWith(".fhl") && !isFileEncrypted(outFile)) {
		  filepath += ".fhl";
		  outFile = new File(filepath);
	  }
	  
      // save the document, basic stream method
	  saveDocument(doc, outFile, encoding);
//      try {
//    	  saveDocument(doc, outFile, encoding);
//      } catch (IOException e) {
//    	  if (isNewFile) {
//    		  // avoid a NEW file is assigned a filepath if saving was no success
//    	      externMap.remove(doc.getUUID());
//    	  }
//    	  throw e;
//      }
     
      // after writing the file, update file history
      pushFileHistory(outFile);

      // assuming success, note the file address in manager
	  setExternalFileReference(doc, outFile.getCanonicalPath());
	  
	  firePropertyChange("documentSaved", null, doc);
      return outFile;
   }

   /** Sets the directory for the file-history service for pad-documents.
    * 
    * @param dir File directory
    * @throws IllegalArgumentException if the directory is invalid or cannot
	*         be written
    */
   public void setFileHistoryDir (File dir) {
       fileSafe = new LayeredFileSafe(dir, HISTORY_DAYS, HISTORY_MONTHS, HISTORY_YEARS);
   }
   
   /** Puts the given filepath expression to memorise in this IO-manager as 
    * related to the document's UUID. A previously assigned value is discarded.
    * If the path is already mapped to a different document, an exception is
    * thrown.
    * 
    * @param document <code>PadDocument</code>
    * @param path String file path associated to the given document
    * @throws IllegalArgumentException if the path is already in use
    */
   public void setExternalFileReference (PadDocument document, String path) {
	  Objects.requireNonNull(document, "document is null");
	  Objects.requireNonNull(path, "path is null");
	  String oldPath = externMap.get(document.getUUID());
	  if (path.equals(oldPath)) return;
	  if (isExternalFileReference(path)) 
		  throw new IllegalArgumentException("path already in use: " + path);
	  
	  externMap.put(document.getUUID(), path);
	  document.addPropertyChangeListener("uuidChanged", listener);
	  document.addPropertyChangeListener("encryptionChanged", listener);
	  
	  if (oldPath != null) {
		  firePropertyChange("pathReplaced", oldPath, path);
	  } else {
		  firePropertyChange("pathAdded", null, path);
	  }
   }
   
   /** Erases the external file reference to the given document from the
    * IO-manager's memory.
    * 
    * @param document <code>PadDocument</code>
    */
   public void removeExternalFileReference (PadDocument document) {
      String path = externMap.remove(document.getUUID());
      document.removePropertyChangeListener("uuidChanged", listener);
      document.removePropertyChangeListener("encryptionChanged", listener);
      if (path != null) {
		  firePropertyChange("pathRemoved", path, null);
      }
   }
   
   /** Updates the filepath information stored in this manager for the given
    * pad-document, according to its encryption property. This ensures that
    * the external filename is correctly set even when encryption status of a
    * document changes. Does nothing if the document has no external file 
    * reference stored.
    * 
    * @param document {@code PadDocument}
    * @return String the registered external path for the argument
    */
   private String updateExternalFileReference (PadDocument document) {
	   UUID uuid = document.getUUID();
	   boolean isEncrypted = document.isEncrypted();
	   String path = getExternalFileReference(uuid);
	   if (path != null) {
		   String encSuffix = ".fhl";
		   boolean pathEndsWithSuffix = path.endsWith(encSuffix);
		   if (isEncrypted && !pathEndsWithSuffix) {
			   path = path.concat(encSuffix);
			   setExternalFileReference(document, path);
		   } else if (!isEncrypted && pathEndsWithSuffix) {
			   path = path.substring(0, path.indexOf(encSuffix));
			   setExternalFileReference(document, path);
		   }
	   }
	   return path;
   }
   
   /** Returns the file path expression associated with the given document
    * UUID or null if no path is assigned.
    * 
    * @param uuid <code>UUID</code> document identifier
    * @return String file path or null if not found
    * @throws NullPointerException if docId is null
    */
   public String getExternalFileReference (UUID uuid) {
	   Objects.requireNonNull(uuid);
       return externMap.get(uuid);
   }
   
   /** Whether the given filepath is registered as external file of any pad-
    * document in this manager.
    * 
    * @param path String absolute filepath
    * @return boolean true = is registered, false = is not registered
    */
   public boolean isExternalFileReference (String path) {
	      return externMap.containsValue(path);
   }

   /** Reads a pad-document from the given input stream at any available 
    * non-encrypted file format. For encrypted streams use the file oriented 
    * open methods! This does not close the input stream.
    * <p>A Treepad compatible encoding may be given as second parameter or
    * null for the global default encoding for Treepad files.
    *  
    * @param input InputStream stream of document serialisation
    * @param encoding String character set to be used (Treepad only), 
    *        may be null
    * @return <code>PadDocument</code>
    * @throws IllegalCharsetNameException
    * @throws UnsupportedCharsetException
    * @throws UnknownFileFormatException if the file format could not be recognised
    * @throws StreamCorruptedException if the stream contained false formatting
    * @throws IOException
    */
   public PadDocument openDocument (InputStream input, String encoding) throws IOException {
	  Objects.requireNonNull(input, "input is null");
      PadDocument doc = null;
      
      // ensure a buffered input stream
      BufferedInputStream in;
      in = input instanceof BufferedInputStream ? (BufferedInputStream)input :
           new BufferedInputStream(input, STREAM_BUFFER_SIZE);

      // attempt at the TREEPAD reader first
      try {
         TreepadReader reader = new TreepadReader(encoding);
         in.mark(STREAM_BUFFER_SIZE);
         doc = reader.read(in);
         return doc;
         
      } catch (UnknownFileFormatException e) {
         Log.debug(3, "(IO_Manager.openDocument) failure: not a TREEPAD 2.7 format\n" +
               "     message = " + e);
      } catch (IOException e) {
         Log.debug(3, "(IO_Manager.openDocument) stream failed at TREEPAD 2.7 format\n" +
               "     message = " + e);
         throw e;
      }

//      // attempt at the Ragna-1 format (JBQ database)
//      try {
////         in.reset();
//         throw new IOException("-- don't have a Ragna-1-Reader! --");
//      } catch (IOException e) {
//         Log.debug(3, "(IO_Manager.openDocument) stream failed at Ragna-1 format\n" + 
//               "     message = " + e);
//      }

      throw new UnknownFileFormatException("the file is not a recognisable pad-document");
   }

   /** Tests the given key material for validity to open the given document
    * file. If the file is not encrypted, false is returned.
    * 
    * @param file File
    * @param key byte[]
    * @return boolean true = file is encrypted and key is valid, false = other
    * @throws IOException 
    */
   public boolean isValidSecretKey (File file, byte[] key) throws IOException {
	   InputStream stream = null;
	   try {
		   stream = openDecryptedInputStream(file, new byte[][] {key}, false);
	   } catch (InterruptedException e) {
	   }

	   if (stream == null) return false;
	   stream.close();
	   return isFileEncrypted(file);
   }
   
   public Object getOpenLock () {return openLock;}
   
   /** Returns an input stream of decrypted data from the given file reference.
    * The function is 
    * to return the file as is if it is not encrypted or to decrypt the file
    * before returning the data stream in case it is found encrypted. 
    * <p>Function optionally includes GUI activity to obtain key material from 
    * the user in case no or no valid material is given by parameter. The
    * machine-near key material is returned via the return parameter, if 
    * decryption succeeds.
    * 
    * @param file File input file
    * @param keyarr byte[][] key material at keyarr[0] or null if unknown;
    *            this is a return parameter in case it is given not null
    * @param useGUI boolean whether user input shall be used if input key fails
    * @return {@code InputStream} or null if user-cancelled input or input key 
    *         failed in no-GUI option
    * @throws FileNotFoundException 
    * @throws IOException 
    * @throws InterruptedException 
    */
   private InputStream openDecryptedInputStream (File file, byte[][] keyarr, boolean useGUI) 
		   		throws IOException, InterruptedException {
	  Objects.requireNonNull(file, "file is null");
	  Objects.requireNonNull(keyarr, "keyarr is null");
      if (!file.isFile())
		  throw new FileNotFoundException(file.getAbsolutePath());
	
	  InputStream input = null;
	  byte[] passphrase = null;
	  
	  // detect file encryption status
	  boolean isEncrypted = isFileEncrypted(file);
	  
	  // decide on file type (encryption)
	  if (isEncrypted) {
		  FHL_FileInfo info = new FHL_FileInfo();
		  passphrase = keyarr[0];

		  // attempt at the given key
		  if (passphrase != null) {
			  // create a cipher input stream (rendering decrypted file data)
			  input = openCipherInputStream(file, passphrase, info);
		  }
		  
		  if (useGUI) synchronized (openLock) {
		  while (input == null) {
			  // obtain key material from user
			  String hstr = Global.res.getDisplay("msg.input.password");
			  hstr = Util.substituteText(hstr, "$name", file.getAbsolutePath());
			  char[] passwd = GUIService.userPasswordInput(null, "dlg.password.access", hstr, null);
			  if (passwd == null) break;
			  passphrase = Functions.makeEncryptKey(passwd);
			  Util.destroy(passwd);
	
			  // create cipher input stream
			  input = openCipherInputStream(file, passphrase, info);
			  
			  // report error condition
			  if (input == null) {
				  String text;
				  
				  // TODO evaluate FHL info for errors
				  switch (info.errorCode) {
				  case 0: text = "unknown error"; break;
				  case 6: text = "msg.failure.input.passphrase"; break;
				  default: text = info.getErrorMsg(); 
				  }
				  GUIService.failureMessage(text, null);
			  }
		  } // while
		  }

		  // return null if decryption (key) failed
		  if (input == null) return null;
		  
	      // otherwise return key material in parameter
		  keyarr[0] = passphrase;
	      
	      // install ZIP input-stream if coded in file
	      if (info.compression == 1) {
	         input = new GZIPInputStream(input, STREAM_BUFFER_SIZE);
	      }
	      
	  // non-encrypted file
	  } else {
		  input = new FileInputStream(file);
	  }
	  return input;
   }
   
   /** Attempts to open a {@code CipherInputFile} with the given key material
    * and render a cleartext input-stream of its data.
    * This loops for two attempts concerning possible divergent file formats.
    * 
    * @param file File encrypted input file
    * @param key byte[] passphrase
    * @param info {@code FHL_FileInfo}
    * @return {@code InputStream} or null if not successful
    */
   private InputStream openCipherInputStream (File file, byte[] key, FHL_FileInfo info) {
	   Objects.requireNonNull(info, "info is null");
	   InputStream input;
	   FHL_FileInfo info2;
	   int ct = 0;
	   do { 
		   CipherInputFile cinF = new CipherInputFile(file, key, ct==0 ? 0 : 2);
		   input = cinF.getInputStream();
		   info2 = cinF.getFileInfo();
		   Log.log(5, "(IO_Manager.openCipherInputStream) open CipherInputFile, loop = " + ct + ", success = " + (input != null));
	   } while (++ct == 1 && input == null);
	   
	   info.readFrom(info2);
	   return input;
   }
   
   /** Attempts to open a pad-document from a local file descriptor.
    * If a decryption key is given, it is attempted first before user input is 
    * requested for an encrypted file. If encoding is null then first the
    * referenced document's options are searched for an option, if that fails
    * the global default encoding for <i>Treepad</i> files is used. 
    * 
    * @param file <code>File</code>
    * @param encoding String character set for a Treepad file, may be null
    * @param key byte[] machine-near encryption key, may be null
    * @return <code>PadDocument</code> or null if user broken
    * @throws IllegalCharsetNameException
    * @throws UnsupportedCharsetException
    * @throws UnknownFileFormatException if the file format could not be recognised
    * @throws StreamCorruptedException if the stream contained false formatting
    * @throws InterruptedException if the calling thread was interrupted while 
    *         waiting for file access
    * @throws IOException
    * @throws InterruptedException 
    */
   public PadDocument openDocument (File file, String encoding, byte[] key) 
		   		throws IOException, InterruptedException {
      // file must exist
	  Objects.requireNonNull(file, "file is null");
      if (!file.isFile())
		  throw new FileNotFoundException(file.getAbsolutePath());
      
	  // acquire file access (semaphore)
      file = file.getCanonicalFile();
	  IOService.get().acquireFileAccess(file);
	  BufferedInputStream input = null;
	  
	  try {
	      // open the file and obtain a decrypted data input stream 
	      String filepath = file.getAbsolutePath();
	      byte[][] keyarr = new byte[][] {key};
	      InputStream in = openDecryptedInputStream(file, keyarr, true);
	      if (in == null) return null;
	      byte[] passphrase = keyarr[0];
	      input = new BufferedInputStream(in);
	      input.mark(1000);
	      
	      // obtain document related options (if available)
		  OptionBag options = detectDocumentOptions(input, filepath);
		  
		  // if there is no encoding argument given
		  // obtain the document's encoding from options or default encoding of the type (Treepad only)
	      if (encoding == null) {
	    	  // get encoding setting of document from its system stored options
	    	  if (options != null) {
	    		  encoding = options.getOption("text-encoding");
	    		  if (encoding != null) {
	    			  Log.debug(8, "(IO_Manager.openDocument) detected Treepad encoding " + encoding);
	    		  }
	    	  }
	
	    	  // otherwise get encoding from global option for the Treepad type
	    	  if (encoding == null || encoding.isEmpty()) {
	    		  encoding = Global.getOptions().getOption("defaultTreepadEncoding", "UTF-8");
				  Log.debug(8, "(IO_Manager.openDocument) assumed global Treepad default encoding: " + encoding);
	    	  }
	      }
	      
	      // verify encoding, use a default in case of failure
	      if (!Global.isTreepadCompatibleCharset(encoding)) {
	    	  String enc2 = DefaultOptions.get().getProperty("defaultTreepadEncoding");
	    	  System.err.println("** WARNING: illegal encoding setup: " + encoding + " for document, using default " + enc2);
	    	  System.err.println("            " + file.getAbsolutePath());
	    	  Log.debug(8, "(IO_Manager.openDocument) assumed Treepad default encoding as given encoding is invalid: " 
					  + enc2 + ", given=" + encoding);
			  encoding = enc2;
	      }
		  
	      // read document via input stream and set encryption status
	      input.reset();
	      PadDocument document = openDocument(input, encoding);
	      document.setEncrypted(passphrase);
	      
	      // restore article properties from document options
	      if (options != null) {
	    	  // create a Map from article-IDs to properties expressions (lists of properties)
	    	  // (article-IDs are derived from their document treepaths)
	    	  List<String> apList = options.getStringList("ARTOPT");
	    	  HashMap<String, String> apMap = new HashMap<>();
	    	  for (String s : apList) {
	    		  int index = s.indexOf(" = ");
	    		  if (index > 0) {
	    			  String k = s.substring(0, index);
	    			  String v = s.substring(index + 3);
	    			  apMap.put(k, v);
	    		  }
	    	  }
	    	  
	    	  // go through all articles and modify their properties according to expressions
	    	  for (PadArticle art : document) {
	    		  String k = art.getPathID();
	    		  String expr = apMap.get(k);
	    		  if (expr != null) {
	    			  art.putPropertySerial(expr);
	    		  }
	    	  }
	      }
	      
	      document.resetModified();
		  Log.log(3, "(IO_Manager.openDocument) opened document " + document.getUUID().toHexString() + ", path=" + filepath);
	      return document;
	      
      } finally {
    	  // close source file input-stream
    	  if (input != null) {
    		  input.close();
    	  }
    	  
    	  // release file access (semaphore)
    	  IOService.get().releaseFileAccess(file);
      }
   } // openDocument

   /** Attempts to open a pad-document from a local file descriptor. For the
    * character encoding of a <i>Treepad</i> file, the referenced document's 
    * preferences are searched for an option, if that fails the global default 
    * encoding for <i>Treepad</i> is used. 
    * If a decryption key is given, it is attempted first before user input is
    * requested for an encrypted file.
    * 
    * @param file <code>File</code> the source file
    * @param key byte[] encryption passphrase, may be null
    * @return <code>PadDocument</code> or null if user broken
    * @throws UnknownFileFormatException if the file format could not be recognised
    * @throws StreamCorruptedException if the stream contained false formatting
    * @throws InterruptedException if the calling thread was interrupted while 
    *         waiting for file access
    * @throws IOException
    */
   public PadDocument openDocument (File file, byte[] key) 
		   		throws IOException, InterruptedException {
      return openDocument(file, null, key);
   }

   /** Copies a source file to a target file while ensuring data security
    * via CRC checking and file access control (IOService).
    *  
    * @param source File 
    * @param target File
    * @throws IOException
    * @throws InterruptedException if the calling thread was interrupted while 
    *         waiting for file access
    */
   public void copyFile (File source, File target) throws IOException, InterruptedException {
	   // ensure file security
	   IOService ios = IOService.get();
	   ios.acquireFileAccess(source);
	   ios.acquireFileAccess(target);
	   
	   try {
		   Util.copyFile2(source, target, true);
	   } finally {
		   ios.releaseFileAccess(source);
		   ios.releaseFileAccess(target);
	   }
   }

   /** Whether the given file is an encrypted format of our parlance.
    * This performs an investigation of the file's content by reading a section
    * of it. If the file does not exist or an IO error occurs, false is 
    * returned.
    *  
    * @param file File file to investigate
    * @return boolean true = is encrypted, false = not encrypted  
    */
   public boolean isFileEncrypted (File file) throws IOException {
	  // open file to detect encryption status
	  try {
		  byte[] buf = Util.readFileSpace(file, 0, 4);
		  return Util.equalArrays(new byte[] {'P', 'W', 'S', '3'}, buf);
	  } catch (IOException e) {
		  return false;
	  }
   }
   
   /** Returns an array of files in descending time order (youngest first) of
    * the security file copies available assigned to the given original file.
    * The files returned by this method are unique.
    *  
    * @param f File object file identity
    * @return {@code File[]}
    * @throws IOException 
    */
   public File[] getFileHistory (File f) throws IOException {
       return fileSafe.getHistory(f);
   }
   
   /** Returns an array of files in descending time order (youngest first) of 
    * the security file copies available for the given pad-document.
    * The files returned by this method are unique and in descending order of
    * their file-time (last modified).
    *  
    * @param document {@code PadDocument} 
    * @return {@code File[]} or null if the argument is unknown
    * @throws IOException 
    */
   public File[] getFileHistory (PadDocument document) throws IOException {
	   String path = getExternalFileReference(document.getUUID());
       return path == null ? null : fileSafe.getHistory(new File(path));
   }
   
   /** Triggers the file history storage system to update its state to current 
    * time.
    */
   public void promoteHistory () {
	   try {
		   fileSafe.promote();
	   } catch (IOException e) {
		   e.printStackTrace();
	   }
   }

   /** Writes a history copy of the given file if system option "useFileHistory"
    * is set, otherwise does nothing.
    * 
    * @param f File file to copy
    * @throws IOException
    */
   public void pushFileHistory (File f) throws IOException {
	   if (Global.getOptions().isOptionSet("useFileHistory")) {
		   fileSafe.storeFile(f);
	   }
   }

   /** Listens to property changes in registered documents.
    * We react to: "uuidChanged", "encryptionChanged".
    */
   private class DocumentPropertyChangeListener implements PropertyChangeListener {
	  @Override
	  public void propertyChange (PropertyChangeEvent evt) {
		  if (evt == null) return;
		  String name = evt.getPropertyName();
		  PadDocument document = (PadDocument) evt.getSource();
		  
		  if (name.equals("uuidChanged")) {
			 UUID oldValue = (UUID) evt.getOldValue();
			 UUID newValue = (UUID) evt.getNewValue();
			 if (oldValue != null) {
				String path = externMap.remove(oldValue);
				if (path != null) {
					externMap.put(newValue, path);
					String hs = newValue.toHexString().substring(0, 8);
					Log.debug(6, "(IO_Manager.DocumentListener) mapping new document path: " + hs + " -> " + path);
				}
			 }
	      
		  } if (name.equals("encryptionChanged")) {
			  updateExternalFileReference(document);
		  }
	  }
	   
   }
}
