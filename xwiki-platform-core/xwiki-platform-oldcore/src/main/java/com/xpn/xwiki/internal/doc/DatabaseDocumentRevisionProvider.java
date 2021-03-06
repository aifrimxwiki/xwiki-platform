/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.internal.doc;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Get document revisions from the database.
 * 
 * @version $Id$
 * @since 9.3RC1
 */
@Component
@Named("database")
@Singleton
public class DatabaseDocumentRevisionProvider implements DocumentRevisionProvider
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public XWikiDocument getRevision(DocumentReference reference, String revision) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument document = xcontext.getWiki().getDocument(reference, xcontext);

        return getRevision(document, revision);
    }

    @Override
    public XWikiDocument getRevision(XWikiDocument document, String revision) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument newdoc;

        String database = xcontext.getWikiId();
        try {
            if (document.getDocumentReference().getWikiReference().getName() != null) {
                xcontext.setWikiId(document.getDocumentReference().getWikiReference().getName());
            }

            if ((revision == null) || revision.equals("")) {
                newdoc = new XWikiDocument(document.getDocumentReference());
            } else if (revision.equals(document.getVersion())) {
                newdoc = document;
            } else {
                newdoc = xcontext.getWiki().getVersioningStore().loadXWikiDoc(document, revision, xcontext);
            }
        } catch (XWikiException e) {
            if (revision.equals("1.1") || revision.equals("1.0")) {
                newdoc = new XWikiDocument(document.getDocumentReference());
            } else {
                throw e;
            }
        } finally {
            xcontext.setWikiId(database);
        }

        return newdoc;
    }
}
