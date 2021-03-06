package org.openlca.ecospold2;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdom2.Element;
import org.jdom2.Namespace;

class Out {

	static Element addChild(Element parent, String name) {
		Element child = new Element(name, parent.getNamespace());
		parent.addContent(child);
		return child;
	}

	static Element addChild(Element parent, String name, String text) {
		Element child = new Element(name, parent.getNamespace());
		parent.addContent(child);
		child.setAttribute("lang", "en", Namespace.XML_NAMESPACE);
		child.setText(text);
		return child;
	}

	static void addIndexedText(Element parent, String text) {
		Element child = new Element("text", parent.getNamespace());
		child.setText(text);
		child.setAttribute("index", "1");
		child.setAttribute("lang", "en", Namespace.XML_NAMESPACE);
		parent.addContent(child);
	}

	static String date(Date date, String pattern) {
		if (date == null)
			return null;
		SimpleDateFormat format = new SimpleDateFormat(pattern);
		return format.format(date);
	}

	static String integer(int i) {
		return Integer.toString(i);
	}
}
