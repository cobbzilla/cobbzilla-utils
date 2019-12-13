package org.cobbzilla.util.xml.main;

import lombok.Cleanup;
import org.cobbzilla.util.xml.XPathUtil;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class XPath {

    public static void main (String[] args) throws Exception {

        final String file = args[0];
        final String expr = args[1];

        @Cleanup FileInputStream in = new FileInputStream(file);

        final Transformer serializer = TransformerFactory.newInstance().newTransformer();
        serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        final XPathUtil xp = new XPathUtil(expr, true);
        final List<Node> nodes = xp.applyXPaths(in).get(expr);
        System.out.println("Found "+nodes.size()+" matching nodes:");
        for (int i=0; i<nodes.size(); i++) {
            Node n = nodes.get(i);
            System.out.print("match #"+i+": '");
            if (XPathUtil.isTextNode(n)) {
                System.out.print(n.getTextContent());
            } else {
                serializer.transform(new DOMSource(n), new StreamResult(new OutputStreamWriter(System.out)));
            }
            System.out.println("' (end match #"+i+")");
        }
        System.out.println("DONE");

    }

}
