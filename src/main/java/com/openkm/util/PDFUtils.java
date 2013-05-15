/**
 * OpenKM, Open Document Management System (http://www.openkm.com)
 * Copyright (c) 2006-2013 Paco Avila & Josep Llort
 * 
 * No bytes were intentionally harmed during the development of this application.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.openkm.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PRAcroForm;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

import freemarker.template.TemplateException;

/**
 * http://itextpdf.sourceforge.net/howtosign.html
 * 
 * @author pavila
 */
public class PDFUtils {
	private static Logger log = LoggerFactory.getLogger(PDFUtils.class);
	
	/**
	 * Fill PDF form
	 */
	@SuppressWarnings("rawtypes")
	public static void fillForm(InputStream input, Map<String, Object> values, OutputStream output)
			throws FileNotFoundException, DocumentException, TemplateException, IOException {
		log.debug("fillForm({}, {}, {})", new Object[] { input, values, output });
		PdfReader reader = new PdfReader(input);
		PdfStamper stamper = new PdfStamper(reader, output);
		AcroFields fields = stamper.getAcroFields();
		PRAcroForm form = reader.getAcroForm();
		boolean formFlattening = false;
		
		if (form != null) {
			for (Iterator it = form.getFields().iterator(); it.hasNext();) {
				PRAcroForm.FieldInformation field = (PRAcroForm.FieldInformation) it.next();
				String fieldValue = fields.getField(field.getName());
				log.debug("Field: {}, Value: '{}'", field.getName(), fieldValue);
				
				if (fieldValue != null && !fieldValue.isEmpty()) {
					// if (values.containsKey(field.getName())) {
						String result = TemplateUtils.replace("PDF_FILL_FORM", fieldValue, values);
						log.debug("Field '{}' set to '{}' (by expression)", field.getName(), result);
						fields.setField(field.getName(), result);
						stamper.partialFormFlattening(field.getName());
						formFlattening = true;
					//} else {
						//log.warn("Field '{}' (expression ignored because not included in map)", field.getName());
					//}
				} else {
					Object value = values.get(field.getName());
					
					if (value != null) {
						log.debug("Field '{}' set to '{}' (by field name)", field.getName(), value);
						fields.setField(field.getName(), value.toString());
						stamper.partialFormFlattening(field.getName());
						formFlattening = true;
					} else {
						log.warn("Field '{}' (value ignored because not included in map)", field.getName());
					}
				}
			}
		}
		
		stamper.setFormFlattening(formFlattening);
		stamper.close();
		reader.close();
	}
	
	/**
	 * List form fields
	 */
	@SuppressWarnings("rawtypes")
	public static List<String> listFormFields(String input) throws FileNotFoundException, DocumentException, IOException {
		log.debug("listFormFields({})", input);
		List<String> formFields = new ArrayList<String>();
		PdfReader reader = new PdfReader(input);
		PRAcroForm form = reader.getAcroForm();
		
		if (form != null) {
			for (Iterator it = form.getFields().iterator(); it.hasNext();) {
				PRAcroForm.FieldInformation field = (PRAcroForm.FieldInformation) it.next();
				formFields.add(field.getName());
			}
		}
		
		reader.close();
		log.debug("listFormFields: {}", formFields);
		return formFields;
	}
}
