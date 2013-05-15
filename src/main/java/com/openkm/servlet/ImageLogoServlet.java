/**
 *  OpenKM, Open Document Management System (http://www.openkm.com)
 *  Copyright (c) 2006-2013  Paco Avila & Josep Llort
 *
 *  No bytes were intentionally harmed during the development of this application.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.openkm.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.core.MimeTypeConfig;
import com.openkm.util.WebUtils;

/**
 * Image Logo Servlet
 */
public class ImageLogoServlet extends HttpServlet {
	private static Logger log = LoggerFactory.getLogger(ImageLogoServlet.class);
	private static final long serialVersionUID = 1L;
	private static final Map<String, String> logos = new HashMap<String, String>();
	
	static {
		logos.put("/login", "logo_login.gif");
		logos.put("/report", "logo_report.gif");
	}
	
	/**
	 * 
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
			ServletException {
		String img = request.getPathInfo();
		
		try {
			if (img != null && img.length() > 1) {
				String logo = logos.get(img);
				
				if (logo != null) {
					InputStream is = getServletContext().getResource("/img/" + logo).openStream();
					WebUtils.sendFile(request, response, logo, MimeTypeConfig.MIME_GIF, true, is);
				} else {
					sendError(request, response);
				}
			}
		} catch (MalformedURLException e) {
			sendError(request, response);
			log.warn(e.getMessage(), e);
		} catch (IOException e) {
			sendError(request, response);
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Send error image
	 */
	private void sendError(HttpServletRequest request, HttpServletResponse response) throws IOException {
		InputStream is = getServletContext().getResource("/img/error.png").openStream();
		WebUtils.sendFile(request, response, "error.png", "image/png", true, is);
		is.close();
	}
}
