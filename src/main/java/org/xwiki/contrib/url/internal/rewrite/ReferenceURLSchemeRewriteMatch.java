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
package org.xwiki.contrib.url.internal.rewrite;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.tuckey.web.filters.urlrewrite.extend.RewriteMatch;
import org.xwiki.url.ExtendedURL;

/**
 * A Dynamic {@code class-rule} Match implementation for Tuckey's Rewrite Filter.
 *
 * @version $Id$
 * @since 7.1M1
 */
public class ReferenceURLSchemeRewriteMatch extends RewriteMatch
{
    private ExtendedURL extendedURL;

    /**
     * @param extendedURL the URL to forward to
     */
    public ReferenceURLSchemeRewriteMatch(ExtendedURL extendedURL)
    {
        this.extendedURL = extendedURL;
    }

    @Override
    public boolean execute(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        request.getServletContext().getRequestDispatcher(this.extendedURL.serialize()).forward(request, response);
        return true;
    }
}
