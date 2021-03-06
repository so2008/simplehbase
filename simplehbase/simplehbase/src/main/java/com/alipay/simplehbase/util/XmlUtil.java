package com.alipay.simplehbase.util;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alipay.simplehbase.exception.SimpleHBaseException;

/**
 * XmlUtil.
 * 
 * @author xinzhi
 * */
public class XmlUtil {
    /** log. */
    private static Logger log = Logger.getLogger(XmlUtil.class);

    /**
     * Find top level node.
     * */
    public static Node findTopLevelNode(String filePath, String nodeName) {
        Util.checkEmptyString(filePath);
        Util.checkEmptyString(nodeName);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setIgnoringComments(true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(filePath));
            Node rootNode = doc.getDocumentElement();
            NodeList childNodes = rootNode.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i).getNodeName().equals(nodeName)) {
                    return childNodes.item(i);
                }
            }
        } catch (Exception e) {
            log.error("parse error.", e);
            throw new SimpleHBaseException("parse error.", e);
        }

        return null;
    }

    /**
     * Get attribute node value of node or null if attribute doesn't exist.
     * */
    public static String getAttr(Node node, String attrName) {

        Util.checkNull(node);
        Util.checkEmptyString(attrName);

        NamedNodeMap columnAttrs = node.getAttributes();
        if (columnAttrs == null) {
            return null;
        }
        Node attrNode = columnAttrs.getNamedItem(attrName);
        if (attrNode == null) {
            return null;
        }
        return attrNode.getNodeValue();
    }

    private XmlUtil() {
    }
}
