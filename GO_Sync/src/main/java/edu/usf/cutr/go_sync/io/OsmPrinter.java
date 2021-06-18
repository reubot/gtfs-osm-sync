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
package edu.usf.cutr.go_sync.io;

import java.util.HashSet;
import java.util.Iterator;

import edu.usf.cutr.go_sync.object.*;
import edu.usf.cutr.go_sync.tag_defs;
import edu.usf.cutr.go_sync.tools.OsmFormatter;

public class OsmPrinter {

    public static final String DEFAULT_API_VERSION = "0.6";
    private final String APPLICATION_CREATOR_KEY = "source";
    private final String APPLICATION_CREATOR_NAME = "GO_Sync";

    public String header() {
        String s = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<osm version='" + DEFAULT_API_VERSION + "' generator='"+Session.getUserName()+"'>\n";
        return s;
    }
    public String footer() {
        return "</osm>";
    }
    public String osmChangeCreate(){
        return "<osmChange version='0.6'" + " generator='"+Session.getUserName()+"'>\n" +
                "<create>\n";
    }
    public String osmChangeModify(){
        return "</create>\n" +
                "<modify>\n";
    }

    public String osmChangeDelete(){
        return "</modify>\n" +
                "<delete>\n";
    }

    public String osmChangeDeleteClose(){
        return "</delete>\n" +
                "</osmChange>\n";
    }

    public String writeChangeSet() {

        return "<changeset>\n" +
                "<tag k='created_by' v='"+OsmFormatter.getValidXmlText(Session.getUserName())+"'/>\n" +
                "<tag k='comment' v='"+OsmFormatter.getValidXmlText(Session.getChangeSetComment())+"'/>\n" +
                "</changeset>\n";
    }
    //FIXME handle null changeIDs properly
    public String writeDeleteNode(String nodeID, String changeSetID, String version) {
        String changesetText = " ";
        if (!changeSetID.equals("DUMMY"))
            changesetText = "changeset='" + changeSetID+ '\'';
        return "<node id='"+nodeID+ "' " + changesetText +  " version='"+ version +"' />\n";
    }

    public String writeBusStop(String changeSetID, String lat, String lon) {
        String changesetText = " ";
        if (!changeSetID.equals("DUMMY"))
            changesetText = "changeset='" + changeSetID+ '\'';
        return "<node " + changesetText +  " lat='" + lat + "' lon='" + lon + "'>\n" +
                "<tag k='highway' v='bus_stop'/>\n" +
                "</node>";
    }

    public String writeBusStop(String changeSetID, String nodeID, Stop st) {return writeBusStop(changeSetID, nodeID, st, false);}

    public String writeBusStop(String changeSetID, String nodeID, Stop st, boolean modify) {
        String changesetText = " ";
        if (!changeSetID.equals("DUMMY"))
            changesetText = "changeset='" + changeSetID+ '\'';
        String text="";
        Stop s = new Stop(st);

        if (st.getType()== null)
            st.setType(tag_defs.primative_type.NODE);
        switch (st.getType()) {
            case NODE:
                text += "<node " + changesetText + " id='" + nodeID
                        + "' lat='" + st.getLat() + "' lon='" + st.getLon();
                break;
            case WAY:
                text += "<way " + changesetText + " id='" + nodeID;
                break;
            case RELATION:
                text += "<relation " + changesetText + " id='" + nodeID;
        }

        // if modify, we need version number
        if(st.getOsmVersion()!=null) {
            String oVersion = st.getOsmVersion();
            if (modify)
                oVersion= Integer.toString(Integer.parseInt(oVersion)+1);
            text += "' version='"+oVersion;
            text += "'>\n";
        }
        // mainly for create new node
        else {
            text += "'>\n";
            if(st.getTag(APPLICATION_CREATOR_KEY)!=null && !st.getTag(APPLICATION_CREATOR_KEY).equals("none")) {
                text += "<tag k='"+APPLICATION_CREATOR_KEY+"' v='"+APPLICATION_CREATOR_NAME+"' />\n";
            }
        }
        //add tag
        HashSet<String> keys = new HashSet<String>(s.keySet().size());
        keys.addAll(s.keySet());
        for (String k : keys) {
            if (!s.getTag(k).equals("none")) {
                text += "<tag k='" + OsmFormatter.getValidXmlText(k) + "' v='" + OsmFormatter.getValidXmlText(s.getTag(k)) + "' />\n";
            }
        }
        switch (st.getType()) {
            case WAY:
                for (String nd:st.getOsmNodes())
                    text += "<nd ref='" + nd + "' />\n";
                text += "</way>\n";
                break;
            case RELATION:
                text += writeMembers(st.getOsmMembers());
                text += "</relation>\n";
                break;
            case NODE:
            default:
                text += "</node>\n";

        }

        return text;
    }
    public String writeMembers(HashSet<RelationMember> members){
        StringBuilder text= new StringBuilder();
        for (RelationMember rm : members) {
            text.append("<member type='")
                .append(rm.getType())
                .append("' ref='")
                .append(rm.getRef())
                .append("' role='");
            if (rm.getRole() != null)
                 text.append(OsmFormatter.getValidXmlText(rm.getRole()));
            text.append("' />\n");
        }
        return text.toString();
    }

    public String writeBusRoute(String changeSetID, String routeID, Route r) {
        String changesetText = " ";
        if (!changeSetID.equals("DUMMY"))
            changesetText = "changeset='" + changeSetID+ '\'';
        String text="";
        Route route = new Route(r);
        // if modify, we need version number
        if(r.getOsmVersion()!=null) {
            String oVersion = r.getOsmVersion();
                if(r.getStatus()== OsmPrimitive.status.MODIFY)
                   oVersion= Integer.toString(Integer.parseInt(oVersion)+1);
                text += "<relation " + changesetText + " id='" + routeID
                    + "' version='"+oVersion + "'>\n";
        }
        // mainly for create new relation
        else {
            text += "<relation  " + changesetText + "  id='" + routeID
                    + "' version='"+ routeID +"'>\n";
            text += "<tag k='"+APPLICATION_CREATOR_KEY+"' v='"+APPLICATION_CREATOR_NAME+"' />\n";
        }
        //add member
        HashSet<RelationMember> members = route.getOsmMembers();
        Iterator it = members.iterator();
        while (it.hasNext()){
            RelationMember rm = (RelationMember) it.next();
            if(rm.getRole()!=null) text += "<member type='"+rm.getType()+"' ref='"+rm.getRef()+
                    "' role='"+OsmFormatter.getValidXmlText(rm.getRole())+"' />\n";
            else text += "<member type='"+rm.getType()+"' ref='"+rm.getRef()+"' role='' />\n";
        }
        //add tag
        HashSet<String> keys = new HashSet<String>(route.keySet().size());
        keys.addAll(route.keySet());
        it = keys.iterator();
        while (it.hasNext()){
            String k = (String) it.next();
            if (!route.getTag(k).equals("none")) {
                text += "<tag k='"+OsmFormatter.getValidXmlText(k)+
                        "' v='"+OsmFormatter.getValidXmlText(route.getTag(k))+"' />\n";
            }
        }
        text += "</relation>\n";
        return text;
    }
}