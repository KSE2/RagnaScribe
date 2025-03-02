/*
*  File: AbstractPadArticle.java
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
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.TreePath;

import org.ragna.util.OptionBag;

import kse.utilclass.misc.Log;
import kse.utilclass.misc.UUID;
import kse.utilclass.misc.Util;

public class AbstractPadArticle implements PadArticle, Cloneable {
   private static final Point ZERO_POINT = new Point();
   private static int counter;
   private int counterValue = counter++;
   
   private PropertyChangeSupport support = new PropertyChangeSupport(this);
   private DocumentListener documentListener = new DocumentListener();
   private EditorListener editorListener = new EditorListener();
   private UUID uuid = new UUID(); //*
   private ArticleType articleType = ArticleType.PlainText;
   private Document editorDocument;
   private String title = "";
   private String shortTitle = "";
   private String tooltip; //*
   private int cursorPosition; //*
   private int selectionMark; //*
   private Font defaultFont; //*
   private Color backgroundColor; //*
   private Color foregroundColor; //*
   private boolean lineWrap; //*

   // alien system references
   private PadDocument parentDocument;
   private PadArticle parentArticle;
   private Rectangle editorVisibleRect = new Rectangle();
   
   /** Creates a new article with a PlainText editor document.
    */
   public AbstractPadArticle () {
      this(ArticleType.PlainText);
   }
   
   /** Creates a new article with the given type of editor document.
    * 
    * @param type <code>ArticleType</code>
    * @throws NullPointerException
    */
   public AbstractPadArticle (ArticleType type) {
	  Objects.requireNonNull(type);

	  // set a default title
	  String title = "article ".concat(String.valueOf(counterValue));
      setTitle(title);
      lineWrap = Global.getOptions().isOptionSet("defaultEditorLinewrap");
      
      // create editor document according to parameter
      if (type == ArticleType.HTMLText) {
         editorDocument = new HTMLDocument();
      } else {
         editorDocument = new PlainDocument();
      }
      editorDocument.addDocumentListener(documentListener);
   }
   
   @Override
   public String toString () {
      return shortTitle;
   }

   @Override
   public UUID getUUID () {
      return uuid;
   }

   @Override
   public String getTitle () {
      return title;
   }

   @Override
   public void setTitle (String title) {
	  Objects.requireNonNull(title);
      String oldTitle = this.title;
      this.title = title;
      setShortTitle(title);
      
      if (Util.notEqual(title, oldTitle)) {
         firePropertyChange("titleChanged");
      }
   }

   @Override
   public String getShortTitle () {
      return shortTitle;
   }

   private void setShortTitle (String title) {
      if (title == null) {
         shortTitle = "";
      } else {
         int maxLength = Math.max(0, Global.getOptions().getIntOption(
               "maxShortTitleLength")-2);
         shortTitle = title.substring(0, Math.min(maxLength, title.length()));
         if (shortTitle.length() == maxLength) {
            shortTitle = shortTitle.concat("..");
         }
      }
   }

   private void firePropertyChange (String property) {
      support.firePropertyChange(property, null, this);
   }
   
   private void firePropertyChange (String property, Object oldValue) {
      support.firePropertyChange(property, oldValue, this);
   }
	   
   @Override
   public String getToolTipText () {
      return tooltip;
   }

   @Override
   public void setToolTipText (String tooltip) {
      this.tooltip = tooltip;
   }

   @Override
   public Document getEditorDocument () {
      return editorDocument;
   }

   @Override
   public PadDocument getDocument () {
      return parentDocument;
   }

   @Override
   public TreePath getTreePath () {
	   PadDocument document = getDocument();
	   return document == null ? null : document.getArticleTreePath(this);
   }
   
   @Override
   public String getPathID () {
	   TreePath tp = getTreePath();
	   if (tp == null) return null;
	   String hs1 = tp.toString();
	   String artId = Util.bytesToHex(Util.arraycopy(Util.fingerPrint(hs1), 8));
	   Log.debug(10, "(AbstractPadArticle.getArticlePathID) article path-ID = " + artId + " for path: " + hs1);
	   return artId;
   }
   
   @Override
   public ArticleType getArticleType () {
      return articleType;
   }

   @Override
   public int getCursorPosition () {
      return cursorPosition;
   }

   @Override
   public int getSelectionMark () {
      return selectionMark;
   }

   @Override
   public void setSelectionMark (int mark) {
	  if (selectionMark != mark) {
		 selectionMark = mark;
	     firePropertyChange("layoutChanged");
	  }
   }

   @Override
   public Font getDefaultFont () {
      return defaultFont;
   }

   @Override
   public Color getBackgroundColor () {
      return backgroundColor;
   }

   @Override
   public Color getForegroundColor () {
      return foregroundColor;
   }

   @Override
   public void setDefaultFont (Font font) {
      Font oldFont = defaultFont;
      if (Util.notEqual(font, oldFont)) {
         defaultFont = font;
         firePropertyChange("layoutChanged");
      }
   }

   @Override
   public void setBackgroundColor (Color c) {
      Color oldColor = backgroundColor;
      if (Util.notEqual(oldColor, c)) {
         backgroundColor = c;
         firePropertyChange("layoutChanged");
      }
   }

   @Override
   public void setForegroundColor (Color c) {
      Color oldColor = foregroundColor;
      if (Util.notEqual(oldColor, c)) {
         foregroundColor = c;
         firePropertyChange("layoutChanged");
      }
   }


   @Override
   public PadArticle copy () {
      try {
         // create clone and update required values
         // PARENT values are reset to null
         AbstractPadArticle clone = (AbstractPadArticle)clone();
         clone.uuid = new UUID();
         clone.documentListener = clone.new DocumentListener();
         clone.editorListener = clone.new EditorListener();
         clone.support = new PropertyChangeSupport(clone);
         clone.editorVisibleRect = new Rectangle(editorVisibleRect);
         
         // clone the editor document and text content
         if (articleType == ArticleType.HTMLText) {
            clone.editorDocument = new HTMLDocument();
         } else {
            clone.editorDocument = new PlainDocument();
         }
         String text = editorDocument.getText(0, editorDocument.getLength());
         clone.editorDocument.insertString(0, text, null);
         clone.editorDocument.addDocumentListener(clone.documentListener);
         
         return clone;
      } catch (CloneNotSupportedException | BadLocationException e) {
         e.printStackTrace();
         return null;
      }
   }

   @Override
   public void setDocument (PadDocument document) {
      PadDocument oldDocument = getDocument();
      if (Util.notEqual(oldDocument, document)) {
         if (oldDocument != null) {
            oldDocument.removeArticle(uuid);
         }
         firePropertyChange("orderChanged");
      }
      this.parentDocument = document;
   }

   @Override
   public void setEditorDocument (Document document) {
      if (document == null) {
         // case to erase content of editor-document
         try {
            editorDocument.remove(0, editorDocument.getLength());
         } catch (BadLocationException e) {
            e.printStackTrace();
         }
      } else {
         // case to change editor-document
         Document oldDocument = editorDocument;
         if (Util.notEqual(document, oldDocument)) {
            editorDocument = document;
            editorDocument.addDocumentListener(documentListener);
            oldDocument.removeDocumentListener(documentListener);
            
            // issue events if type of document changed (== static class)
            if (Util.notEqual( editorDocument.getClass(), 
                oldDocument.getClass()) ) {
                firePropertyChange("dataTypeChanged");
            }
            firePropertyChange("contentModified");
         }
      }
   }

   @Override
   public void setCursorPosition (int position) {
	  if (cursorPosition != position) {
		  cursorPosition = position;
	      firePropertyChange("layoutChanged");
	  }
   }

   @Override
   public PadArticle getParent () {
      return parentArticle;
   }

   @Override
   public void setParent (PadArticle parent) {
      PadArticle oldParent = parentArticle;
      if (Util.notEqual(parent, oldParent)) {
         parentArticle = parent;
         firePropertyChange("orderChanged");
      }
   }

   @Override
   public Rectangle getVisibleRect () {
      return (Rectangle)editorVisibleRect.clone();
   }

   @Override
   public void setDocumentVisibleRect (Rectangle rect) {
      if (rect == null) {
         editorVisibleRect.x = 0;
         editorVisibleRect.y = 0;
         editorVisibleRect.height = 0;
         editorVisibleRect.width = 0;
      } else {
         editorVisibleRect.setBounds(rect);
      }
   }

   @Override
   public void setUUID (UUID uuid) {
   	  Objects.requireNonNull(uuid);
   	  UUID old = this.uuid;
   	  this.uuid = uuid;
   	  Log.debug(8, "(AbstractPadArticle.setUUID) modified UUID of article from " + old + " to new " + uuid);
   	  firePropertyChange("uuidChanged", old);
   }

   @Override
   public void setContent (String text) {
      try {
         editorDocument.remove(0, editorDocument.getLength());
         editorDocument.insertString(0, text, null);
      } catch (BadLocationException e) {
         e.printStackTrace();
      }
   }

   @Override
   public String getContent () {
      try {
         return editorDocument.getText(0, editorDocument.getLength());
      } catch (BadLocationException e) {
         e.printStackTrace();
         return null;
      }
   }

   @Override
   public int hashCode () {
      return uuid.hashCode();
   }

   @Override
   public boolean equals (Object obj) {
      try { return uuid.equals(((PadArticle)obj).getUUID());
      } catch (Exception e) {
         return false;
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
   public void addPropertyChangeListener (String property,
         PropertyChangeListener listener) {
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

   /** This listener listens to content changes in the text document of this
    * article and fires property change events of name "contentModified" to
    * property change listeners.
    */
   private class DocumentListener implements javax.swing.event.DocumentListener {
      @Override
      public void insertUpdate (DocumentEvent e) {
         firePropertyChange("contentModified");
      }

      @Override
      public void removeUpdate (DocumentEvent e) {
         firePropertyChange("contentModified");
      }

      @Override
      public void changedUpdate (DocumentEvent e) {
         firePropertyChange("contentModified");
      }
   }

   private class EditorListener implements PropertyChangeListener {
	   @Override
	   public void propertyChange (PropertyChangeEvent evt) {
		   if (evt == null || parentDocument.isReadOnly()) return;
		   String name = evt.getPropertyName();
		   Object newValue = evt.getNewValue();
		   
		   Log.log(10, "(AbstractPadArticle.EditorListener)  -- property change: [" + name + "] " + newValue);
		   if ("font".equals(name)) {
			   setDefaultFont((Font) newValue);
		   } else if ("background".equals(name)) {
			   setBackgroundColor((Color) newValue);
		   } else if ("foreground".equals(name)) {
			   setForegroundColor((Color) newValue);
		   } else if ("lineWrap".equals(name)) {
			   setLineWrap((Boolean) newValue);
		   }
	   }
   }
   
   @Override
   public int getOrderDepth () {
	  int depth = 0;
	  PadArticle p = parentArticle;
	  while (p != null) {
		  depth++;
		  p = p.getParent();
	  }
      return depth;
   }

   @Override
   public int getIndexPosition () {
	   PadDocument document = getDocument();
	   return document == null ? -1 : document.indexOf(this);	   
   }

   @Override
   public Dimension getSelection() {
	   Dimension res = null;
	   if (selectionMark != cursorPosition) {
		   res = new Dimension(Math.min(selectionMark, cursorPosition),
				   Math.max(selectionMark, cursorPosition));
	   }
	   return res;
   }

	@Override
	public boolean getLineWrap () {return lineWrap;}

	@Override
	public boolean getWrapStyleWord() {return true;}

	@Override
	public void setLineWrap (boolean v) {
		if (lineWrap != v) {
			lineWrap = v;
	        firePropertyChange("layoutChanged");
	    }
	}

	@Override
	public int compareTo (PadArticle o) {
		return getTitle().compareTo(o.getTitle());
	}

	/** Encodes the identity of the given font so that the result can serve as
	 * an input to the static method 'Font.decode()'.
	 * 
	 * @param f Font
	 * @return String
	 */
	public static String encodeFont (Font f) {
		String style = "PLAIN";
		switch (f.getStyle()) {
		case Font.BOLD: style = "BOLD"; break; 
		case Font.ITALIC: style = "ITALIC"; break; 
		case Font.BOLD + Font.ITALIC: style = "BOLDITALIC"; break; 
		}
		String code = f.getFamily() + "-" + style + "-" + f.getSize();
		return code;
	}
	
	@Override
	public String getPropertySerial() {
		OptionBag bag = new OptionBag();
		
		bag.setOption("tooltip", tooltip);
		bag.setOption("font", defaultFont == null ? null : Util.encodeFont(defaultFont));
		bag.setIntOption("bgdcolor", backgroundColor == null ? null : backgroundColor.getRGB());
		bag.setIntOption("fgrcolor", foregroundColor == null ? null : foregroundColor.getRGB());
		if (cursorPosition > 0) {
			bag.setIntOption("cursor", cursorPosition);
		}
		if (selectionMark > 0 && selectionMark != cursorPosition) {
			bag.setIntOption("selmark", selectionMark);
		}
		bag.setOption("linewrap", lineWrap);
		if (!editorVisibleRect.getLocation().equals(ZERO_POINT)) {
			bag.setBounds("visibleRect", editorVisibleRect);
		} else {
			bag.setBounds("visibleRect", null);
		}
		
		// return serialisation of option-bag
		String ser = bag.toString();
		ser = Util.removeComments(ser, "#");
//		Log.debug(10, "(AbstractPadArticle.getPropertySerial) serial = ".concat(ser));
		return ser;
	}

	@Override
	public void putPropertySerial (String serial) {
		Log.log(6, "(AbstractPadArticle.putPropertySerial) setting article properties: " + serial);
		// create option-bag from serialisation 
		OptionBag bag;
		try {
			bag = new OptionBag(serial);
		} catch (IOException e) {
			throw new IllegalArgumentException("invalid OptionBag serialisation", e);
		}
		
		// re-create serialised properties
		if (bag.contains("linewrap")) {
			setLineWrap(bag.isOptionSet("linewrap"));
		}
		if (bag.contains("cursor")) {
			setCursorPosition(bag.getIntOption("cursor"));
		}
		if (bag.contains("selmark")) {
			setSelectionMark(bag.getIntOption("selmark"));
		} else {
			setSelectionMark(cursorPosition);
		}
		if (bag.contains("bgdcolor")) {
			Color color = new Color(bag.getIntOption("bgdcolor"));
			setBackgroundColor(color);
		}
		if (bag.contains("fgrcolor")) {
			Color color = new Color(bag.getIntOption("fgrcolor"));
			setForegroundColor(color);
		}
		if (bag.contains("tooltip")) {
			tooltip = bag.getOption("tooltip");
		}
		if (bag.contains("font")) {
			Font f = Font.decode(bag.getOption("font"));
			setDefaultFont(f);
		}
		Rectangle rect = bag.getBounds("visibleRect");
		if (rect != null) {
			setDocumentVisibleRect(rect);
		}
	}

	@Override
	public PropertyChangeListener getEditorListener() {
		return editorListener;
	}

}
