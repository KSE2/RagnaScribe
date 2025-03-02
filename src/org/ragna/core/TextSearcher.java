/*
*  File: TextSearcher.java
* 
*  Project Ragna Scribe
*  @author Wolfgang Keller
*  Created 
* 
*  Copyright (c) 2024 by Wolfgang Keller, Munich, Germany
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.ragna.front.DisplayManager;

import kse.utilclass.misc.Util;
import kse.utilclass.sets.ArraySet;

/** Utility to find and identify text positions within a {@code PadArticle} 
 * or a {@code PadDocument}.
 */
public class TextSearcher {

	int maximum;
	boolean caseSense;
	boolean wholeWords;
	
	/** Creates a text-searcher for the given pad-document and a maximum
	 * number of result positions. Options for case-sensitive search and
	 * relevance of whole words can be given.
	 * 
	 * @param doc {@code PadDocument}
	 * @param maximum int max result positions
	 * @param sensitive boolean true = search case-sensitive
	 * @param wholeWords boolean true = search only whole words
	 */
	public TextSearcher (int maximum, boolean sensitive, boolean wholeWords) {
		Util.requirePositive(maximum);
		this.maximum = maximum;
		this.caseSense = sensitive;
		this.wholeWords = wholeWords;
	}
	
	/** Returns an array of cursor positions for token occurrences with the
	 * given pad-article. Returns an empty array if no occurrence was found.
	 * This function is not limited by a result size maximum.
	 * 
	 * @param article {@code PadArticle}
	 * @param token String character sequence to search
	 * @param sensitive boolean option for case-sensitive search
	 * @param wholeWords boolean option for whole-words search
	 * @return int[] cursor positions
	 */
	public static int[] findArticleTextPositions (PadArticle article, String token, boolean sensitive, boolean wholeWords) {
		Objects.requireNonNull(article, "article is null");
		Objects.requireNonNull(token, "token is null");
		Set<Integer> list = new ArraySet<>();

		if (!token.trim().isEmpty()) { 
			String text = article.getContent();
			int length = token.length();
			if (!sensitive) {
				text = text.toLowerCase();
				token = token.toLowerCase();
			}
			
			int cursor = 0;
			while (cursor > -1) {
				// find the token from a position
				
				cursor = text.indexOf(token, cursor);
				if (cursor > -1) {
					int position = cursor;
					cursor +=  length;
					
					// check "whole-words" option for found position
					if (wholeWords) {
						boolean leftWhite = position == 0 || !Character.isLetterOrDigit(text.charAt(position-1));
						boolean rightWhite = position+length >= text.length() || 
								!Character.isLetterOrDigit(text.charAt(position+length));
						if (!(leftWhite & rightWhite)) continue;
					}

					// add find position
					list.add(position);
				}
			}
		}
		
		int[] result = new int[list.size()];
		int i = 0;
		for (Integer a : list) {
			result[i++] = a; 
		}
		return result;
	}
	
	public List<DocumentTextPosition> findPositionsInSession (String token) {
		Objects.requireNonNull(token, "token is null");
		List<DocumentTextPosition> rlist = new ArrayList<>();
		
		// iterate over all articles
		for (PadDocument doc : DisplayManager.get().getOpenDocuments()) {
			List<DocumentTextPosition> list = findPositionsInDocument(doc, token);
			rlist.addAll(list);
		}
		return rlist;
	}
	
	public List<DocumentTextPosition> findPositionsInDocument (PadDocument doc, String token) {
		Objects.requireNonNull(doc, "document is null");
		Objects.requireNonNull(token, "token is null");
		List<DocumentTextPosition> rlist = new ArrayList<>();
		
		// iterate over all articles
		for (PadArticle art : doc) {
			int[] posArr = findArticleTextPositions(art, token, caseSense, wholeWords);
			
			for (int cursor : posArr) {
				DocumentTextPosition textPos = new DocumentTextPosition(doc, art, cursor);
				rlist.add(textPos);
			}
		}
		return rlist;
	}
	
	public List<DocumentTextPosition> findPositionsInArticle (PadArticle article, String token) {
		Objects.requireNonNull(article, "article is null");
		Objects.requireNonNull(token, "token is null");
		PadDocument doc = article.getDocument();
		if (doc == null) {
			throw new IllegalArgumentException("article without document");
		}
		
		List<DocumentTextPosition> rlist = new ArrayList<>();
		int[] posArr = findArticleTextPositions(article, token, caseSense, wholeWords);
		for (int cursor : posArr) {
			DocumentTextPosition textPos = new DocumentTextPosition(doc, article, cursor);
			rlist.add(textPos);
		}
		return rlist;
	}
	
//  *********  INNER CLASSES  *********	
	
	/** A structure to describe a text find position within a 
	 * {@code PadDocument}. The information contains the {@code PadArticle} 
	 * and the cursor position with its content. The size of the searched
	 * text is not contained.
	 */
	public static class DocumentTextPosition {
		private PadDocument document;
		private PadArticle article;
		private int cursorPos;
		
		/** Creates a new document text position defined by an article and
		 * a cursor position.
		 *  
		 * @param document {@code PadDocument} 
		 * @param article {@code PadArticle}
		 * @param position int cursor position
		 */
		public DocumentTextPosition (PadDocument document, PadArticle article, int position) {
			Objects.requireNonNull(document, "document is null");
			Objects.requireNonNull(article, "article is null");
			Util.requirePositive(position, "position");
			
			this.document = document;
			this.article = article;
			this.cursorPos = position;
		}

		public PadArticle getArticle() {return article;}

		public int getCursorPos() {return cursorPos;}
		
		public PadDocument getDocument () {return document;}
	}
	
}
