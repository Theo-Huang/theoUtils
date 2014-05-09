package tools.office;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.dom4j.DocumentException;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.io.SAXReader;

import tools.file.FileUtils;

public class XmlUtils {
  public static final DOMDocument getDOMDoc(File file, Boolean... sync) throws DocumentException, IOException {
    return getDOMDoc(FileUtils.readFileData(file, sync));
  }

  public static final DOMDocument getDOMDoc(List<String> strList) throws DocumentException {
    String xmlString = "";
    for (String str : strList) {
      xmlString += str;
    }
    return getDOMDoc(xmlString);
  }

  public static final DOMDocument getDOMDoc(String xmlString) throws DocumentException {
    SAXReader sr = new SAXReader();
    sr.setDocumentFactory(new DOMDocumentFactory());
    return (DOMDocument) sr.read(new java.io.StringReader(xmlString));
  }
}
