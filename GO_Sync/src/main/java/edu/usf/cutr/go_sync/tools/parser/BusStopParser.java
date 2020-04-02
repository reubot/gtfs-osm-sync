/**
Copyright 2010 University of South Florida

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

**/

package edu.usf.cutr.go_sync.tools.parser;

import java.util.ArrayList;
import java.util.HashMap;
import edu.usf.cutr.go_sync.tag_defs;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Khoa Tran
 */
public class BusStopParser extends DefaultHandler{
    private HashMap<String,String> tempTag;
    private AttributesImpl attImplNode;
    private ArrayList<AttributesImpl> xmlNodes;
    private ArrayList<HashMap<String,String>> xmlTags;
    private ArrayList<tag_defs.primative_type> xmlTypes;
    public BusStopParser(){
        xmlNodes = new ArrayList<AttributesImpl>();
        xmlTags = new ArrayList<HashMap<String,String>>();
        xmlTypes = new ArrayList<tag_defs.primative_type>();
    }

    @Override public void startElement(String namespaceURI, String localName, String qname, Attributes attributes) throws SAXException {
        if (qname.equals(tag_defs.XML_NODE) || qname.equals("changeset")) {
            attImplNode = new AttributesImpl(attributes);
            xmlNodes.add(attImplNode);
            tempTag = new HashMap<String,String>();      // start to collect tags of that node
            xmlTypes.add(tag_defs.primative_type.NODE);
        }
        if (qname.equals(tag_defs.XML_TAG)) {
            AttributesImpl attImpl = new AttributesImpl(attributes);
            //                System.out.println(attImpl.getValue("k") + attImpl.getValue("v"));
            tempTag.put(attImpl.getValue("k"), attImpl.getValue("v"));         // insert key and value of that tag into HashMap
        }
    }

    @Override public void endElement (String uri, String localName, String qName) throws SAXException {
        if (qName.equals(tag_defs.XML_NODE)) {
            xmlTags.add(tempTag);
        }
    }

    public AttributesImpl getOneNode(){
        return attImplNode;
    }

    public HashMap<String,String> getTagsOneNode(){
        return tempTag;
    }

    public ArrayList<AttributesImpl> getNodes(){
        return xmlNodes;
    }

    public ArrayList<HashMap<String, String>> getTags(){
        return xmlTags;
    }

    public ArrayList<tag_defs.primative_type> getTypes(){
        return xmlTypes;
    }

}