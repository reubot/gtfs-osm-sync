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
import java.util.Hashtable;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Khoa Tran
 */
public class WayParser extends DefaultHandler{
    private Hashtable<String,AttributesImpl> xmlNodes ;
    private Hashtable tempTag;
    
    private AttributesImpl attImplNode;
    private ArrayList<AttributesImpl> xmlWays, tempNodes;
    private ArrayList<ArrayList> xmlWayNodes; //xmlNodes
    private ArrayList<Hashtable> xmlTags;
    boolean open_node;

    public WayParser(){
    	xmlWays = new ArrayList<AttributesImpl>();
    	xmlWayNodes = new ArrayList<ArrayList>();
        xmlNodes = new Hashtable<String,AttributesImpl>();
        xmlTags = new ArrayList<Hashtable>();
    }
    
    @Override public void startElement(String namespaceURI, String localName, String qname, Attributes attributes) throws SAXException {
    	System.out.println("qname: " + qname);
        if (qname.equals("way")){
            attImplNode = new AttributesImpl(attributes);
//    		System.out.println("id  " +attributes.getValue("id"));
            xmlWays.add(attImplNode);
            tempTag = new Hashtable();      // start to collect tags of that way
            tempNodes = new ArrayList<AttributesImpl>();      // start to collect nodes of that way
    		System.out.println("way id  " +attributes.getValue("id") + attributes.getValue("timestamp") +attributes.getValue("changeset"));

        }
    	if (qname.equals("node") || qname.equals("changeset")) {
    	//collect nodes for later use
 
    		attImplNode = new AttributesImpl(attributes);
            xmlNodes.put(attImplNode.getValue("id"),attImplNode);

            //           tempTag = new Hashtable();      // start to collect tags of that node
                		System.out.println("node id  " +attributes.getValue("id") + attributes.getValue("lat") +attributes.getValue("lon"));
    		
    		open_node = true;

        }
        if (qname.equals("tag") &&  !open_node) { //don't care about node tags
            AttributesImpl attImpl = new AttributesImpl(attributes);
                            System.out.println(attImpl.getValue("k") + attImpl.getValue("v"));
            tempTag.put(attImpl.getValue("k"), attImpl.getValue("v"));         // insert key and value of that tag into Hashtable
        }
        if (qname.equals("nd")) {
            AttributesImpl attImpl = new AttributesImpl(attributes);
                            System.out.println("ref: "+ attImpl.getValue("ref"));
                            
                            tempNodes.add(xmlNodes.get(attImpl.getValue("ref")));
                    //        tempNodes.add(attImpl.getValue("ref"));         // insert key and value of that tag into Hashtable
                            
                            
        }        
    }

    @Override public void endElement (String uri, String localName, String qName) throws SAXException {
        if (qName.equals("way")) {
            xmlTags.add(tempTag);
            xmlWayNodes.add(tempNodes);
        }
        if (qName.equals("node")) {
        	open_node = false;
        } 
        if (qName.equals("osm")) {
        	System.out.println("osm");
        } 
        
    }

    public AttributesImpl getOneNode(){
        return attImplNode;
    }

    public Hashtable getTagsOneNode(){
        return tempTag;
    }

    public ArrayList<AttributesImpl> getNodes(){
       // return xmlNodes;
    	return null;
    }
    
    public ArrayList<AttributesImpl> getWays(){
        return xmlWays;
    }
    

    public ArrayList<Hashtable> getTags(){
        return xmlTags;
    }
}