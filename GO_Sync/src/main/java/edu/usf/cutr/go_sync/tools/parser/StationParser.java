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

import edu.usf.cutr.go_sync.object.OsmPrimitive;
import edu.usf.cutr.go_sync.object.RelationMember;
import edu.usf.cutr.go_sync.tag_defs;
import edu.usf.cutr.go_sync.tools.EuclideanDoublePoint;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Khoa Tran
 */
public class StationParser extends DefaultHandler {
//TODO use OsmPrimative for nodes , only use EuclideanDoublePoint for calculating centroid
    private HashMap<String, String> tempTag;
    private HashSet<RelationMember> tempMembers;
    private ArrayList<String> tempMembersID;
    private ArrayList<AttributesImpl> xmlRelations;
    //    private ArrayList<String> xmlRelationID = new String[];
    private HashMap<String, EuclideanDoublePoint> xmlNodes = new HashMap<>();
    private HashMap<String, OsmPrimitive> xmlNodesPrimative = new HashMap<>();
    private HashMap<String, HashSet<EuclideanDoublePoint>> xmlWays = new HashMap<>();
    private HashSet<EuclideanDoublePoint> xmlWayMemberNodes;
    private HashSet<OsmPrimitive> tempWayMemberNodes;
    private HashSet<EuclideanDoublePoint> tempWayMembers;

    private ArrayList<tag_defs.primative_type> xmlTypes;
    //xmlTags<String, String> ----------- xmlMembers<String(refID), AttributesImpl>
    private ArrayList<HashMap<String, String>> xmlTags;
    private HashMap<String, AttributesImpl> xmlStationsMap = new HashMap<>();
    private HashMap<String, HashMap<String, String>> xmlTagsMap = new HashMap<>();
    private HashMap<String, tag_defs.primative_type> xmlTypesMap = new HashMap<>();

    private HashMap<String, HashSet<RelationMember>> xmlMembers;
    private HashMap<String, HashSet<OsmPrimitive>> xmlWayMembers;
    AttributesImpl tempattImpl;
    String id;

    public StationParser() {
        xmlRelations = new ArrayList<AttributesImpl>();
        xmlTags = new ArrayList<HashMap<String, String>>();
        xmlMembers = new HashMap<String, HashSet<RelationMember>>();
        xmlTypes = new ArrayList<tag_defs.primative_type>();
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qname, Attributes attributes) throws SAXException {
        AttributesImpl attImpl = new AttributesImpl(attributes);
        if (qname.equals(tag_defs.XML_RELATION)) {
            id = attImpl.getValue("id");
            xmlRelations.add(attImpl);
            xmlStationsMap.put(id, attImpl);
            tempTag = new HashMap<String, String>();      // start to collect tags of that relation
            tempMembers = new HashSet<RelationMember>();
            tempMembersID = new ArrayList<String>();
            xmlTypes.add(tag_defs.primative_type.RELATION);
            xmlTypesMap.put(id, tag_defs.primative_type.RELATION);

        }

        if (qname.equals(tag_defs.XML_WAY)) {
            id = attImpl.getValue("id");
            tempattImpl = new AttributesImpl(attributes);
            tempWayMembers = new HashSet<EuclideanDoublePoint>();
        }

        if (qname.equals("nd")) {
            String ndId = attImpl.getValue("ref");
            if (xmlNodes.containsKey(ndId))
                tempWayMembers.add(xmlNodes.get(ndId));
        }
        if (qname.equals(tag_defs.XML_NODE) || qname.equals("changeset")) {
            double[] geopoint = new double[2];
            geopoint[0] = Double.parseDouble(attImpl.getValue(tag_defs.LAT));
            geopoint[1] = Double.parseDouble(attImpl.getValue(tag_defs.LON));
            xmlNodes.put(attImpl.getValue("id"), new EuclideanDoublePoint(geopoint));
            tempTag = new HashMap<String, String>();      // start to collect tags of that node
            //    xmlTypes.add(tag_defs.primative_type.NODE);
        }
        if (tempTag != null && qname.equals(tag_defs.XML_TAG)) {
            tempTag.put(attImpl.getValue("k"), attImpl.getValue("v"));         // insert key and value of that tag into HashMap
        }
        if (tempMembers != null && qname.equals(tag_defs.XML_MEMBER)) {
            RelationMember rm = new RelationMember(attImpl.getValue("ref"), attImpl.getValue("type"), attImpl.getValue("role"));
            rm.setStatus("OSM server");
            tempMembersID.add(attImpl.getValue("ref"));
            tempMembers.add(rm);
        }
    }

    private HashSet<EuclideanDoublePoint> getChildren( ArrayList<String> tempMembersIDlocal) {
        {
            HashSet<EuclideanDoublePoint> tempPoints = new HashSet<>();
            for (String memberID : tempMembersIDlocal) {
                if (xmlNodes.containsKey(memberID))
                    tempPoints.add(xmlNodes.get(memberID));
                if (xmlWays.containsKey(memberID))
                    tempPoints.addAll(xmlWays.get(memberID));

            }
            return tempPoints;
        }

}


    @Override public void endElement (String uri, String localName, String qName) throws SAXException {
//TODO handle way in the same manner as replations
        if (qName.equals(tag_defs.XML_WAY)) {
            xmlWays.put(id,tempWayMembers);
            xmlTagsMap.put(id,tempTag);

//            if (tempTag.containsKey("public_transport")) {
//                EuclideanDoublePoint centroid = new EuclideanDoublePoint(new double[2]).centroidOf(tempWayMembers);
////            AttributesImpl tempImpl =  xmlRelations.get(xmlRelations.size()-1);
//                tempattImpl.addAttribute("",tag_defs.LAT,tag_defs.LAT,"CDATA",Double.toString(centroid.getPoint()[0]));
//                tempattImpl.addAttribute("",tag_defs.LON,tag_defs.LON,"CDATA",Double.toString(centroid.getPoint()[1]));
//                xmlStationsMap.put(id, tempattImpl);
//                xmlTypesMap.put(id, tag_defs.primative_type.WAY);
//            }
            id = null;
            tempWayMembers = null;

        }

        if (qName.equals(tag_defs.XML_RELATION)) {
            xmlTags.add(tempTag);
            xmlTagsMap.put(id,tempTag);
            xmlMembers.put(id,tempMembers);
            HashSet<EuclideanDoublePoint> tempPoints = new HashSet<>();
            for (String memberID : tempMembersID)
            {
                if (xmlNodes.containsKey(memberID))
                    tempPoints.add(xmlNodes.get(memberID));
                if (xmlWays.containsKey(memberID))
                    tempPoints.addAll(xmlWays.get(memberID));

            }
            if (!tempPoints.isEmpty()) {
                //double[] tempDouble = tempPoints.
                EuclideanDoublePoint centroid = new EuclideanDoublePoint( new double[2]).centroidOf(tempPoints);
                AttributesImpl tempImpl =  xmlRelations.get(xmlRelations.size()-1);
                tempImpl.addAttribute("",tag_defs.LAT,tag_defs.LAT,"CDATA",Double.toString(centroid.getPoint()[0]));
                tempImpl.addAttribute("",tag_defs.LON,tag_defs.LON,"CDATA",Double.toString(centroid.getPoint()[1]));
                xmlRelations.remove(xmlRelations.size()-1);
                xmlRelations.add(tempImpl);


            }
            tempMembers = null;
            tempMembersID = null;
            id = null;
        }
    }

    public ArrayList<AttributesImpl> getRelations(){
        return xmlRelations;
    }

    public ArrayList<HashMap<String, String>> getTags(){
        return xmlTags;
    }

    public HashMap<String, HashSet<RelationMember>> getMembers(){
        return xmlMembers;
    }

    public ArrayList<tag_defs.primative_type> getTypes(){
        return xmlTypes;
    }

    public HashMap<String, AttributesImpl> getRelationsMap() {
        return xmlStationsMap;
    }

    public HashMap<String, HashMap<String, String>> getTagsMap() {
        return xmlTagsMap;
    }

    public HashMap<String, tag_defs.primative_type> getTypesMap() {
        return xmlTypesMap;
    }

}