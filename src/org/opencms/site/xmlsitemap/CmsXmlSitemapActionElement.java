/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.site.xmlsitemap;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsFileUtil;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;

/**
 * Action element class for displaying the XML sitemap from a JSP.<p>
 */
public class CmsXmlSitemapActionElement extends CmsJspActionElement {

    /** The logger instance for this class. */
    private static final Log LOG = CmsLog.getLog(CmsXmlSitemapActionElement.class);

    /** The configuration bean. */
    protected CmsXmlSeoConfiguration m_configuration;

    /**
     * Constructor, with parameters.
     * 
     * @param pageContext the JSP page context object
     * @param request the JSP request 
     * @param response the JSP response 
     */
    public CmsXmlSitemapActionElement(PageContext pageContext, HttpServletRequest request, HttpServletResponse response) {

        super(pageContext, request, response);
    }

    /**
     * Writes the XML sitemap to the response.<p>
     * 
     * @throws Exception if something goes wrong 
     */
    public void renderXmlSitemap() throws Exception {

        CmsObject cms = getCmsObject();
        String baseFolderRootPath = CmsFileUtil.removeTrailingSeparator(CmsResource.getParentFolder(cms.getRequestContext().addSiteRoot(
            cms.getRequestContext().getUri())));
        CmsXmlSitemapGenerator xmlSitemapGenerator = new CmsXmlSitemapGenerator(baseFolderRootPath);
        xmlSitemapGenerator.setComputeContainerPageDates(m_configuration.shouldComputeContainerPageModificationDates());
        CmsPathIncludeExcludeSet inexcludeSet = xmlSitemapGenerator.getIncludeExcludeSet();
        for (String include : m_configuration.getIncludes()) {
            inexcludeSet.addInclude(include);
        }
        for (String exclude : m_configuration.getExcludes()) {
            inexcludeSet.addExclude(exclude);
        }
        String xmlSitemap = xmlSitemapGenerator.renderSitemap();
        getResponse().getWriter().print(xmlSitemap);
    }

    /**
     * Displays either the generated sitemap.xml or the generated robots.txt, depending on the configuration.<p>
     * 
     * @throws Exception if something goes wrong 
     */
    public void run() throws Exception {

        CmsObject cms = getCmsObject();
        String seoFilePath = cms.getRequestContext().getUri();
        CmsResource seoFile = cms.readResource(seoFilePath);
        m_configuration = new CmsXmlSeoConfiguration();
        m_configuration.load(cms, seoFile);
        String mode = m_configuration.getMode();
        if (mode.equals(CmsXmlSeoConfiguration.MODE_ROBOTS_TXT)) {
            showRobotsTxt();
        } else if (mode.equals(CmsXmlSeoConfiguration.MODE_XML_SITEMAP)) {
            renderXmlSitemap();
        }
    }

    /**
     * Renders the robots.txt data containing the sitemaps automatically.<p>
     * 
     * @throws Exception if something goes wrong 
     */
    private void showRobotsTxt() throws Exception {

        CmsObject cms = getCmsObject();
        StringBuffer buffer = new StringBuffer();
        I_CmsResourceType seoFileType = OpenCms.getResourceManager().getResourceType(
            CmsXmlSeoConfiguration.SEO_FILE_TYPE);
        List<CmsResource> seoFiles = cms.readResources(
            "/",
            CmsResourceFilter.DEFAULT_FILES.addRequireVisible().addRequireType(seoFileType.getTypeId()));
        for (CmsResource seoFile : seoFiles) {
            try {
                CmsXmlSeoConfiguration seoFileConfig = new CmsXmlSeoConfiguration();
                seoFileConfig.load(cms, seoFile);
                if (seoFileConfig.isXmlSitemapMode()) {
                    buffer.append("Sitemap: " + OpenCms.getLinkManager().getOnlineLink(cms, cms.getSitePath(seoFile)));
                    buffer.append("\n");
                }
            } catch (CmsException e) {
                LOG.error("Error while generating robots.txt : " + e.getLocalizedMessage(), e);
            }
        }
        buffer.append("\n");
        buffer.append(m_configuration.getRobotsTxtText());
        buffer.append("\n");
        getResponse().getWriter().print(buffer.toString());
    }

}
