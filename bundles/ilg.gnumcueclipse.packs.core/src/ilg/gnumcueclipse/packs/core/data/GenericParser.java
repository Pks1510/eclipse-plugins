/*******************************************************************************
 * Copyright (c) 2014 Liviu Ionescu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Liviu Ionescu - initial implementation.
 *******************************************************************************/

package ilg.gnumcueclipse.packs.core.data;

import ilg.gnumcueclipse.core.Xml;
import ilg.gnumcueclipse.packs.core.tree.Leaf;
import ilg.gnumcueclipse.packs.core.tree.Node;
import ilg.gnumcueclipse.packs.core.tree.Property;
import ilg.gnumcueclipse.packs.core.tree.Type;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Very simple parser, to convert any complicated XML into a more regular and
 * compact representation. The output is a tree, with properties and children.
 * <p>
 * Original attributes are turned into properties, keeping the original name;
 * the string content is turned into a special property (Property.XML_CONTENT).
 * <p>
 * Simple (meaning no children) children elements (like <description> for
 * selected nodes) are also turned into properties.
 * <p>
 * Properties are trimmed by the putProperty() function, so no need to do it
 * again when consuming them.
 * <p>
 * All other children elements are turned into children nodes, recursively.
 * 
 */
public class GenericParser {

	public GenericParser() {
		;
	}

	/**
	 * Callback to be defined in derived classes, to define elements that
	 * generate properties instead of new nodes.
	 * 
	 * @param name
	 *            the current xml element name.
	 * @param node
	 *            the current tree node.
	 * @return true if the element should be turned into a node property.
	 */
	public boolean isProperty(String name, Leaf node) {
		return false;
	}

	/**
	 * Parse the xml document.
	 * 
	 * @param document
	 *            the xml document generated by the standard dom parser.
	 * @return a tree starting with a ROOT node.
	 */
	public Node parse(Document document) {

		Element packageElement = document.getDocumentElement();

		Node tree = new Node(Type.ROOT);
		tree.setPackType(Leaf.PACK_TYPE_CMSIS);
		parseRecusive(packageElement, tree);

		return tree;
	}

	/**
	 * Parse the current xml element and its children, adding a new subtree to
	 * the given parent node.
	 * 
	 * @param el
	 *            the current xml element to parse.
	 * @param parent
	 *            the destination node where the subtree will be added.
	 */
	private void parseRecusive(Element el, Node parent) {

		String type = el.getNodeName();

		Leaf node = null;
		List<Element> children = Xml.getChildrenElementsList(el);
		if (!children.isEmpty()) {

			node = Node.addNewChild(parent, type);
			node.setPackType(Leaf.PACK_TYPE_CMSIS);

			// The element has children, some can be optimised as properties,
			// the rest will generate children nodes.
			for (Element child : children) {

				String childName = child.getNodeName();

				if (isProperty(childName, node)) {

					// Turn simple elements into properties
					String content = Xml.getElementContent(child);
					node.putNonEmptyProperty(childName, content);

				} else {
					parseRecusive(child, (Node) node);
				}
			}
		} else {
			String content = Xml.getElementContent(el);

			NamedNodeMap attributes = el.getAttributes();
			if (attributes == null || attributes.getLength() == 0) {
				if ("description".equals(type)) {
					parent.setDescription(content);
				} else {
					parent.putNonEmptyProperty(type, content);
				}
				return;
			}
			node = Leaf.addNewChild(parent, type);
			node.setPackType(Leaf.PACK_TYPE_CMSIS);

			node.putNonEmptyProperty(Property.XML_CONTENT, content);
			// System.out.println();
		}

		// Add all element attributes as node properties.
		NamedNodeMap attributes = el.getAttributes();
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); ++i) {
				String name = attributes.item(i).getNodeName();
				node.putProperty(name, el.getAttribute(name));
			}
		}
	}
}
