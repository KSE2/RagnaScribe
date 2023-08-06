/*
*  File: PrintParameters.java
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

import java.awt.Font;
import java.util.Objects;

public class PrintParameters {
	/** The set type of articles from a document to print (starting with the
	 * selected article or an index). 
	 * DOCUMENT, BRANCH or ARTICLE.
	 */
	public enum PrintScope {
		DOCUMENT, BRANCH, ARTICLE;
	}

	/** The way a set of ordered articles is printed. 
	 * CONTINUOUS (one result document), NEWPAGE (articles start on new page), 
	 * SEPARATE (multiple results documents).
	 */
	public enum ArticlePrintModus {
		CONTINUOUS, NEWPAGE, SEPARATE;
	}
	
	public PadDocument document;
	public PadArticle article;
	public PrintScope printScope = PrintScope.ARTICLE;
	public ArticlePrintModus printModus = ArticlePrintModus.CONTINUOUS;
	public Font font;
	public boolean printHeaders;
	public boolean printTitles;
	public boolean ignoreEmpty;
	public boolean wrapLines;
	public boolean wrapWords;
	public String headerText;
	
	public PrintParameters (PadDocument document, PadArticle article) {
		Objects.requireNonNull(document);
		Objects.requireNonNull(article);
		if (document.indexOf(article) == -1)
			throw new IllegalArgumentException("article not contained");

		this.document = document;
		this.article = article;
		this.font = article.getDefaultFont();
		if (font == null) {
			font = document.getDefaultTextFont();
		}
		this.wrapLines = article.getLineWrap();
		this.wrapWords = article.getWrapStyleWord();
		this.headerText = article.getTitle();
	}
}
