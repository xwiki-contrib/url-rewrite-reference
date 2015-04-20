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

import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.tuckey.web.filters.urlrewrite.extend.RewriteMatch;
import org.tuckey.web.filters.urlrewrite.extend.RewriteRule;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;

/**
 * A Dynamic {@code class-rule} implementation for Tuckey's Rewrite Filter, that converts Entity URLs written in the
 * "reference" format into the "standard" format. The "reference" format is the following:
 * <ul>
 *   <li>UC1: {@code entity/<action>/<entity reference type>/<entity reference>}</li>
 *   <li>UC2: {@code entity/<action>/<entity reference>} ==> type = page</li>
 *   <li>UC3: {@code entity/<entity reference>} ==> type = page, action = view</li>
 *   <li>UC4: {@code <entity reference>} ==> type = page, action = view</li>
 * </ul>
 * Examples (with XWiki deployed in the ROOT context):
 * <ul>
 *   <li>http://localhost:8080/entity/view/page/wiki:space.page</li>
 *   <li>http://localhost:8080/entity/export/attach/wiki:space.page@image.png</li>
 *   <li>http://localhost:8080/entity/export/attach/wiki:space.page@image.png?format=xar|pdf|html</li>
 *   <li>http://localhost:8080/entity/view/wiki:space.page</li>
 *   <li>http://localhost:8080/entity/wiki:space.page</li>
 *   <li>http://localhost:8080/wiki:space.page</li>
 * </ul>
 *
 * @version $Id$
 * @since 7.1M1
 */
public class ReferenceURLSchemeRewriteRule extends RewriteRule
{
    private ComponentManager rootComponentManager;

    private ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver;

    /**
     * @param servletContext the Servlet Context used to initialize the URL Rewrite Filter
     * @return true if the initialization was done correctly
     */
    public boolean init(ServletContext servletContext)
    {
        // Get the Component Manager which has been initialized first in a Servlet Context Listener.
        this.rootComponentManager = (ComponentManager) servletContext.getAttribute(ComponentManager.class.getName());

        // Get the Document Reference Resolver that we'll use to resolve references passed as String in the URL (see
        // below).
        try {
            this.resourceReferenceResolver = this.rootComponentManager.getInstance(new DefaultParameterizedType(null,
                ResourceReferenceResolver.class, ExtendedURL.class), "reference/entity");
        } catch (ComponentLookupException e) {
            throw new RuntimeException(String.format("Failed to initialize [%s]", getClass().getName()), e);
        }

        return true;
    }

    @Override
    public RewriteMatch matches(HttpServletRequest request, HttpServletResponse response)
    {
        try {
            ExtendedURL extendedURL = new ExtendedURL(new URL(request.getRequestURL().toString()),
                request.getContextPath());
            List<String> segments = extendedURL.getSegments();
            if (segments.size() == 1 || (segments.size() > 1 && segments.get(0).equals("entity"))) {
                if (segments.size() > 1) {
                    segments.remove(0);
                }
                EntityResourceReference resourceReference =
                    (EntityResourceReference) this.resourceReferenceResolver.resolve(extendedURL,
                        EntityResourceReference.TYPE, Collections.<String, Object>emptyMap());
                EntityReference entityReference = resourceReference.getEntityReference();
                // Change the URL
                if (segments.size() == 3) {
                    // UC: view/page/wiki:space.page
                    // Replace the entity type with the Space
                    segments.set(1, entityReference.extractReference(EntityType.SPACE).getName());
                    // Replace entity reference with the page name
                    segments.set(2, entityReference.extractReference(EntityType.DOCUMENT).getName());
                } else if (segments.size() == 2) {
                    // UC: view/wiki:space.page
                    // Replace entity reference with the space and page names
                    segments.set(1, entityReference.extractReference(EntityType.SPACE).getName());
                    segments.add(entityReference.extractReference(EntityType.DOCUMENT).getName());
                } else if (segments.size() == 1) {
                    // UC: wiki:space.page
                    // Replace entity reference with the "view" action
                    segments.set(0, "view");
                    // Add the space and page names
                    segments.add(entityReference.extractReference(EntityType.SPACE).getName());
                    segments.add(entityReference.extractReference(EntityType.DOCUMENT).getName());
                } else {
                    throw new RuntimeException(String.format("Invalid URL format [%s]", extendedURL.toString()));
                }
                segments.add(0, "bin");

                return new ReferenceURLSchemeRewriteMatch(extendedURL);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to process URL [%s]", request.getRequestURL().toString()),
                e);
        }

        return null;
    }
}
