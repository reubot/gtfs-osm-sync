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
package edu.usf.cutr.go_sync.osm;

import edu.usf.cutr.go_sync.io.OsmPrinter;
import edu.usf.cutr.go_sync.io.WriteFile;

import java.util.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import edu.usf.cutr.go_sync.object.Stop;
import edu.usf.cutr.go_sync.tools.parser.*;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import javax.swing.JTextArea;
import edu.usf.cutr.go_sync.object.RelationMember;
import edu.usf.cutr.go_sync.object.Route;
import edu.usf.cutr.go_sync.object.Session;
import org.xml.sax.SAXException;
//import sun.misc.BASE64Encoder;
import edu.usf.cutr.go_sync.tag_defs;
/**
 *
 * @author Khoa Tran
 */
public class HttpRequest {
    private static final int SLEEP_TIME = 500;
    private static final String API_VERSION ="0.6";
    private static final String OSM_HOST = "https://openstreetmap.org/api/0.6/";
    private static final String[] overpass_hosts = {"http://overpass-api.de/api/interpreter","http://api.openstreetmap.fr/oapi/interpreter","http://overpass.osm.rambler.ru/cgi/interpreter",};



    private HashMap<String,AttributesImpl> existingNodes = new HashMap<>();
    private HashMap<String,AttributesImpl> existingStations = new HashMap<>();
    private ArrayList<AttributesImpl> existingRelations = new ArrayList<AttributesImpl>();

    private HashMap<String,HashMap<String,String>> existingNodesTags = new HashMap<>();
    private HashMap<String,HashMap<String,String>> existingStationTags = new HashMap<>();
    private ArrayList<HashMap> existingRelationTags = new ArrayList<HashMap>();

    private HashMap<String,tag_defs.primative_type> existingStopTypes = new HashMap<>();
    private HashMap<String,tag_defs.primative_type> existingStationTypes = new HashMap<>();


    private HashMap<String,HashSet<RelationMember>> existingStationMembers = new HashMap<String,HashSet<RelationMember>>();
    private ArrayList<HashSet<RelationMember>> existingRelationMembers = new ArrayList<HashSet<RelationMember>>();
    private HashMap<String,ArrayList<String>> existingStationWayNodes = new HashMap<String,ArrayList<String>>();

    private HashSet<Stop> revertDelete = new HashSet<Stop>();
    private HashSet<Stop> revertModify = new HashSet<Stop>();
    private HashSet<Stop> revertUpload = new HashSet<Stop>();

    private boolean isSupportVersion = false;

    private OsmPrinter oprinter = new OsmPrinter();

    private String cSetID="";

    public static final String FILE_NAME_OUT_UPLOAD = "OSM_CHANGE_XML.txt";

    private JTextArea taskOutput;

    public HttpRequest(JTextArea to){
        taskOutput = to;
    }

    public void checkVersion() throws InterruptedException{
        String[] hosts = {OSM_HOST};
        String s = sendRequest(hosts, "capabilities/", "GET", "");
        try {
            InputSource inputSource = new InputSource(new StringReader(s));
            OsmVersionParser vp = new OsmVersionParser("0.6");
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, vp);
            isSupportVersion = vp.isSupportVersion();
            if (!isSupportVersion) {
                System.out.println("The current api does not support version " + API_VERSION);
            }
        } catch(IOException e) {
            e.printStackTrace();
        } catch(SAXException e) {
            e.printStackTrace();
        } catch(ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, AttributesImpl> getExistingBusStops(String left, String bottom, String right, String top) throws InterruptedException{
        //http://open.mapquestapi.com/xapi/
        //"http://www.informationfreeway.org"
//        String urlSuffix = "/api/0.6/node[highway=bus_stop][bbox="+left+","+bottom+","+right+","+top+"]";
//        String[] hosts = {"http://open.mapquestapi.com/xapi","http://www.informationfreeway.org"};
//    	String urlSuffix = "?node[highway=bus_stop][bbox="+left+","+bottom+","+right+","+top+"]";
//        String[] hosts = {"http://api.openstreetmap.fr/xapi","http://www.informationfreeway.org"};

    	String content = "<union>"+
      			"<query type='node'>" +
    			"<has-kv k='highway' v='bus_stop'/>"+
    			"<bbox-query w='left' e='right' s='bottom' n='north'/>"+
    			"</query>"+

    			"<query type='node'>" +
    			"<has-kv k='public_transport' v='platform'/>"+
    			"<bbox-query w='left' e='right' s='bottom' n='north'/>"+
    			"</query>"+

    			"<query type='node'>" +
    			"<has-kv k='public_transport' v='station'/>"+
    			"<bbox-query w='left' e='right' s='bottom' n='north'/>"+
    			"</query>"+

    			"<query type='node'>" +
    			"<has-kv k='amenity' v='bus_station'/>"+
    			"<bbox-query w='left' e='right' s='bottom' n='north'/>"+
    			"</query>"+
/*
    			"<query type=\"node\">" +
    "<has-kv k=\"highway\" v=\"bus_stop\"/>"+
    "<bbox-query w=\""+left+"\" e=\""+right+"\" s=\""+bottom+"\" n=\""+top+"\"/>"+
    " </query>"+
  "<query type=\"node\">" +
  	" <has-kv k=\"public_transport\" v=\"platform\"/>"+
    "<bbox-query w=\""+left+"\" e=\""+right+"\" s=\""+bottom+"\" n=\""+top+"\"/>"+
   "</query>"+
   "<query type=\"node\">" +
 	" <has-kv k=\"public_transport\" v=\"station\"/>"+
   "<bbox-query w=\""+left+"\" e=\""+right+"\" s=\""+bottom+"\" n=\""+top+"\"/>"+
  "</query>"+  */
"</union>"+
"<print mode=\"meta\"/>";

    	content = content.replace("left", left).replace("right",right).replace("bottom", bottom).replace("north",top);
//      String[] hosts = {"http://overpass-api.de/api/interpreter","http://api.openstreetmap.fr/oapi/interpreter","http://overpass.osm.rambler.ru/cgi/interpreter",};

      System.out.println(content);
        try {
            // get data from server
            //String s = sendRequest(hosts, urlSuffix, "GET", "");
        	String s = sendRequest(overpass_hosts, "", "POST", content);

            InputSource inputSource = new InputSource(new StringReader(s));
            // get data from file - need to remove this for REAL APPLICATION
//            InputSource inputSource = new InputSource("DataFromServer.osm");
            BusStopParser par = new BusStopParser();
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, par);

            existingNodes.putAll(par.getNodesMap());
            existingNodesTags.putAll(par.getTagsMap());
            existingStopTypes.putAll(par.getTypesMap());

        } catch(IOException e) {
            e.printStackTrace();
        } catch(SAXException e) {
            e.printStackTrace();
        } catch(ParserConfigurationException e) {
            e.printStackTrace();
        }
        if (!existingNodes.isEmpty()) return existingNodes;
        System.out.println("null nodes");
        return null;
    }

    // this method needs to be invoked after getExistingBusStops
    public HashMap<String, HashMap<String, String>> getExistingBusStopsTags(){
        System.out.println("tags = "+ existingNodesTags.size());
        if (!existingNodesTags.isEmpty() )
            return existingNodesTags;
        return null;
    }

    public HashMap<String, HashMap<String, String>> getExistingStationTags(){
        System.out.println("existingStationTags tags = "+existingStationTags.size());
        if (!existingStationTags.isEmpty() )
            return existingStationTags;
        return null;
    }
    public HashMap<String, AttributesImpl> getExistingStopWaysRelations(String left, String bottom, String right, String top) throws InterruptedException{
//        String urlSuffix = "/api/0.6/relation[route=bus][bbox="+left+","+bottom+","+right+","+top+"]";
//        String[] hosts = {"http://open.mapquestapi.com/xapi","http://www.informationfreeway.org"};
//        String urlSuffix = "?relation[route=bus][bbox="+left+","+bottom+","+right+","+top+"]";

        String content = "[bbox:"+bottom+ ',' +left+ ',' +top+ ',' +right+"]; ( " +
                "relation[public_transport=station];" +
                "relation[public_transport=platform];" +
                "way[public_transport=station];"+
                "way[public_transport=platform];"+
//                "way[amenity=bus_station];"+
//                "way[amenity=ferry_terminal];"+
            "); (._;>;); out meta;";
//        String[] hosts = {"http://www.overpass-api.de/api/xapi_meta","http://overpass.openstreetmap.ru/cgi/xapi_meta"};

        System.out.println(content);
        try {
            // get data from server

            String s = sendRequest(overpass_hosts, "", "POST", content);
            InputSource inputSource = new InputSource(new StringReader(s));
            // get data from file - need to remove this for REAL APPLICATION
//            InputSource inputSource = new InputSource("DataFromServerRELATION.osm");
            StationParser par = new StationParser();
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, par);
            existingStations.putAll(par.getRelationsMap());
            existingStationTags.putAll(par.getTagsMap());
            existingStationTypes.putAll(par.getTypesMap());
            existingStationMembers.putAll(par.getMembers());
            existingStationWayNodes.putAll(par.getWayNodes());
//            existingNodes.addAll(existingStations);
//            existingBusTags.addAll(existingStationTags);
//            existingStopTypes.addAll(existingStationTypes);
//            existingNodes=existingStations;
//            existingBusTags=(existingStationTags);
//            existingStopTypes=(existingStationTypes);
        } catch(IOException e) {
            System.out.println(e.getStackTrace());
        } catch(SAXException e) {
            System.out.println(e.getStackTrace());
        } catch(ParserConfigurationException e) {
            System.out.println(e.getStackTrace());
        }
        if (existingStations.isEmpty()){
            System.out.println("null relations");
            return null;
        }

        return existingStations;
    }

    public ArrayList<AttributesImpl> getExistingBusRelations(String left, String bottom, String right, String top) throws InterruptedException{
//        String urlSuffix = "/api/0.6/relation[route=bus][bbox="+left+","+bottom+","+right+","+top+"]";
//        String[] hosts = {"http://open.mapquestapi.com/xapi","http://www.informationfreeway.org"};
    	String urlSuffix = "?relation[route=bus][bbox="+left+ ',' +bottom+ ',' +right+ ',' +top+ ']';
        String content = "[bbox:"+bottom+ ',' +left+ ',' +top+ ',' +right+"]; ( relation[\"route\"=\"ferry\"];" +
                "  relation[\"route\"=\"bus\"];" +
                "); out meta;";
//        String[] hosts = {"http://www.overpass-api.de/api/xapi_meta","http://overpass.openstreetmap.ru/cgi/xapi_meta"};

        System.out.println(content);
        try {
            // get data from server

            String s = sendRequest(overpass_hosts, "", "POST", content);
            InputSource inputSource = new InputSource(new StringReader(s));
            // get data from file - need to remove this for REAL APPLICATION
//            InputSource inputSource = new InputSource("DataFromServerRELATION.osm");
            RouteParser par = new RouteParser();
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, par);
            existingRelations.addAll(par.getRelations());
            existingRelationTags.addAll(par.getTags());
            existingRelationMembers.addAll(par.getMembers());

        } catch(IOException e) {
            e.printStackTrace();
        } catch(SAXException e) {
            e.printStackTrace();
        } catch(ParserConfigurationException e) {
            e.printStackTrace();
        }
        if (!existingRelations.isEmpty()) return existingRelations;
        System.out.println("null bus relations");
        return null;
    }

    // this method needs to be invoked after getExistingBusRelations
    public ArrayList<HashMap> getExistingBusRelationTags(){
        System.out.println("relation tags = "+existingRelationTags.size());
        if (!existingRelationTags.isEmpty() )
            return existingRelationTags;
        return null;
    }

    // this method needs to be invoked after getExistingBusRelations
    public ArrayList<HashSet<RelationMember>> getExistingBusRelationMembers(){
        System.out.println("tags = "+existingRelationMembers.size());
        if (!existingRelationMembers.isEmpty() )
            return existingRelationMembers;
        return null;
    }

    public void downloadChangeSet(String cs) throws InterruptedException{
        String[] hosts = {OSM_HOST};
        String urlSuffix = "changeset/"+cs+"/download";
        try {
            // get data from server
            String s = sendRequest(hosts, urlSuffix, "GET", "");
            InputSource inputSource = new InputSource(new StringReader(s));
            // get data from file - need to remove this for REAL APPLICATION
//            InputSource inputSource = new InputSource("DataFromServerRELATION.osm");
            ChangesetDownloadParser par = new ChangesetDownloadParser();
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, par);

            revertDelete.addAll(par.getToBeDeletedStop());

            ArrayList<Stop> toBeModified = new ArrayList<Stop>();
            toBeModified.addAll(par.getToBeModifiedStop());
            for (Stop ts : toBeModified) {
                Integer versionNumber = (Integer.parseInt(ts.getOsmVersion()) - 1);
                Stop ns = getNodeByVersion(ts.getOsmId(), versionNumber.toString(), false);
                ns.setOsmVersion(ts.getOsmVersion());
                revertModify.add(ns);
            }

            ArrayList<Stop> toBeUploaded = new ArrayList<Stop>();
            toBeUploaded.addAll(par.getToBeUploadedStop());
            for (Stop ts : toBeUploaded) {
                Stop ns = getNodeByVersion(ts.getOsmId(), ts.getOsmVersion(), true);
                ns.setOsmVersion("-1");
                revertUpload.add(ns);
            }
        } catch(IOException e) {
            e.printStackTrace();
        } catch(SAXException e) {
            e.printStackTrace();
        } catch(ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public HashSet<Stop> getRevertUpload(){
        return revertUpload;
    }

    public HashSet<Stop> getRevertModify(){
        return revertModify;
    }

    public HashSet<Stop> getRevertDelete(){
        return revertDelete;
    }

    private Stop getNodeByVersion(String osmid, String version, boolean isNew) throws InterruptedException{
        Stop st=null;
        String urlSuffix = "node/"+osmid+ '/' +version;
        String[] hosts = {OSM_HOST};
        System.out.println("Retrieving node "+osmid+" with version "+version+"...");
        try {
            // get data from server
            String s = sendRequest(hosts, urlSuffix, "GET", "");
            InputSource inputSource = new InputSource(new StringReader(s));
            // get data from file - need to remove this for REAL APPLICATION
//            InputSource inputSource = new InputSource("DataFromServerRELATION.osm");
            BusStopParser par = new BusStopParser();
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, par);
            AttributesImpl attImplNode = par.getOneNode();
            HashMap tags = par.getTagsOneNode();
            st = new Stop(null,(String)tags.get(tag_defs.GTFS_OPERATOR_KEY),(String)tags.get("name"),
                    attImplNode.getValue("lat"),attImplNode.getValue("lon"));
            st.addTags(tags);
            if (!isNew) {
                st.setOsmId(attImplNode.getValue("id"));
            }
            else {
                st.setOsmId('-' +attImplNode.getValue("id"));
            }
        } catch(IOException e) {
            e.printStackTrace();
        } catch(SAXException e) {
            e.printStackTrace();
        } catch(ParserConfigurationException e) {
            e.printStackTrace();
        }
        return st;
    }

    public static String getApiVersion() {
        return API_VERSION;
    }

    private String getRequestContents() {
        String text = oprinter.header() +
                oprinter.writeChangeSet() +
                oprinter.footer();
        return text;
    }

    private String getRequestContents(String changeSetID, String lat, String lon) {
        String text = oprinter.header() +
                oprinter.writeBusStop(changeSetID, lat, lon) +
                oprinter.footer();
        return text;
    }

    public String getRequestContents(String changeSetID, HashSet<Stop> addStop, HashSet<Stop> modifyStop, HashSet<Stop> deleteStop, Hashtable r) {
        oprinter = new OsmPrinter();
        Hashtable routes = new Hashtable();
        if (r!=null) routes.putAll(r);
        ArrayList<String> routeKeys = new ArrayList<String>();
        if (r!=null) routeKeys.addAll(routes.keySet());
        String text = "";
        List<Stop> stops = new ArrayList<Stop>();
        stops.addAll(addStop);
        text += oprinter.osmChangeCreate();
        int id=0;
        for(int i=0; i<stops.size(); i++){
            id = (-1)*(i+1);
            text += oprinter.writeBusStop(changeSetID, Integer.toString(id), stops.get(i));
        }
        int k=0;
        while (k<routeKeys.size()){
            Route tRoute = (Route)routes.get(routeKeys.get(k));
            if(tRoute.getStatus().equals("n")){
                id--;
                text += oprinter.writeBusRoute(changeSetID, Integer.toString(id), tRoute);
                routeKeys.remove(k);
            }
            else {
                k++;
            }
        }
        text += oprinter.osmChangeModify();
        stops = new ArrayList<Stop>();
        stops.addAll(modifyStop);
        for (Stop stop : stops) {
            String nodeid = stop.getOsmId();
            text += oprinter.writeBusStop(changeSetID, nodeid, stop);
//            System.out.println(stops.get(i).getOsmId()+","+stops.get(i).getStopID()+","+stops.get(i).getOsmVersion());
        }
        //all routes should be modified. Thus, k=0 after while loop
        k=0;
        while (k<routeKeys.size()){
            Route tRoute = (Route)routes.get(routeKeys.get(k));
            if(tRoute.getStatus().equals("m")){
                String routeid = tRoute.getOsmId();
                text += oprinter.writeBusRoute(changeSetID, routeid, tRoute);
                routeKeys.remove(k);
            }
            else {
                k++;
            }
        }
        stops = new ArrayList<Stop>();
        stops.addAll(deleteStop);
        text += oprinter.osmChangeDelete();
        for (Stop stop : stops) {
            String nodeid = stop.getOsmId();
            String nodeVersion = stop.getOsmVersion();
            text += oprinter.writeDeleteNode(nodeid, changeSetID, nodeVersion);
        }
        text += oprinter.osmChangeDeleteClose();
        return text;
    }

    public void createChangeSet() throws InterruptedException{
        String urlSuffix = "changeset/create";
        String[] hosts = {OSM_HOST};

        String responseMessage = "";
        if (isSupportVersion) {
            String s = getRequestContents();
            responseMessage = sendRequest(hosts, urlSuffix, "PUT", getRequestContents());
            System.out.println(responseMessage);
            cSetID = responseMessage.substring(0, responseMessage.lastIndexOf('\n'));
            System.out.println("ChangeSet ID = "+cSetID);
        }
    }

    public void closeChangeSet() throws InterruptedException{
        String urlSuffix = "changeset/"+cSetID+"/close";
        String[] hosts = {OSM_HOST};

        String responseMessage = "";
        if (isSupportVersion) {
            if (!cSetID.isEmpty()) {
                responseMessage = sendRequest(hosts, urlSuffix, "PUT", getRequestContents());
                System.out.println(responseMessage);
            }
            else {
                System.out.println("Changeset ID is not obtained yet!");
                taskOutput.append("Changeset ID is not obtained yet!\n");
            }
        }
    }

    public void createSingleBusStop(String lat, String lon) throws InterruptedException{
        String urlSuffix = "node/create";
        String[] hosts = {OSM_HOST};

        String responseMessage = "";
        if (isSupportVersion) {
            if (!cSetID.isEmpty()) {
                responseMessage = sendRequest(hosts, urlSuffix, "PUT", getRequestContents(cSetID, lat, lon));
                System.out.println(responseMessage);
            }
            else {
                System.out.println("Changeset ID is not obtained yet!");
                taskOutput.append("Changeset ID is not obtained yet!\n");
            }
        }
    }

    public void createChunks(HashSet<Stop> n, HashSet<Stop> m, HashSet<Stop> d, Hashtable r) throws InterruptedException{
        HashSet<Stop> newStops = new HashSet<Stop>();
        HashSet<Stop> modifyStops = new HashSet<Stop>();
        HashSet<Stop> deleteStops = new HashSet<Stop>();

        if(n!=null)newStops.addAll(n);
        if(m!=null)modifyStops.addAll(m);
        if(d!=null)deleteStops.addAll(d);

        Hashtable routes = new Hashtable();
        if (r!=null) routes.putAll(r);

        String urlSuffix = "changeset/"+cSetID+"/upload";
        String[] hosts = {OSM_HOST};

        String responseMessage = "";
        if (isSupportVersion) {
            if (!cSetID.isEmpty()) {
                String osmChangeText = getRequestContents(cSetID, newStops, modifyStops, deleteStops, routes);
                new WriteFile(FILE_NAME_OUT_UPLOAD, osmChangeText);
                try{
                    responseMessage = sendRequest(hosts, urlSuffix, "POST", osmChangeText);
                    System.out.println("Message: "+responseMessage);
                } catch(InterruptedException e){
                    throw new InterruptedException();
                }
            }
            else {
                System.out.println("Changeset ID is not obtained yet!");
                taskOutput.append("Changeset ID is not obtained yet!\n");
            }
        }
    }

    public String sendRequest(String[] hosts, String urlSuffix, String method, String content) throws InterruptedException {
        HttpURLConnection conn = null;
        StringBuffer responseText = new StringBuffer();
        URL serverAddress = null;
        int retry = 1;
        int maxRetryPerHost = 5;
        int hostIndex=0;
        if(hosts==null) return "";
        while (true) {
            if(retry>maxRetryPerHost) {
                hostIndex ++;
                retry = 1;
                if(hostIndex==hosts.length) {
                    if(conn!=null) {
                        conn.disconnect();
                        conn = null;
                    }
                    taskOutput.append("All hosts are busy, check 'http://wiki.openstreetmap.org/wiki/Xapi#Servers' for more status information\n");
                    break;
                }
            }
            String url = hosts[hostIndex]+urlSuffix;
            try {
                System.out.println("Connecting "+url+" using method "+method+ ' ' +retry);
//                try {
                    Thread.sleep(SLEEP_TIME);
                    taskOutput.append("Connecting "+url+" using method "+method+ ' ' +retry+ '\n');
//                } catch (InterruptedException e) {
//                    throw new InterruptedException();
//                }

                serverAddress = new URL(url);

                // set the initial connection
                conn = (HttpURLConnection) serverAddress.openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(15000);

                if (method.equals("PUT") || method.equals("POST") || method.equals("DELETE")) {
                    //BASE64Encoder enc = new sun.misc.BASE64Encoder();
                    Base64.Encoder enc = java.util.Base64.getEncoder();
                    String usernamePassword = Session.getUserName()+ ':' +Session.getPassword();
                    String encodedAuthorization = enc.encode(usernamePassword.getBytes()).toString();
                    conn.setRequestProperty("Authorization", "Basic "+ encodedAuthorization);

                    conn.setRequestProperty("Content-type", "text/xml");
                    conn.setDoOutput(true);

                    if(content!=null) {
                        OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
                        osw.write(content);
                        osw.flush();
                    }
                }

                conn.connect();

                int responseCode = conn.getResponseCode();
                System.out.println("Response Code: "+responseCode);
                System.out.println("Response Message: "+conn.getResponseMessage());

                if (responseCode >= 500) {
                    System.out.println("response code >=500");
                    taskOutput.append("response code >=500\n");
                    retry++;
                    continue;
                }

                BufferedReader response;
                String s;
                if(responseCode==HttpURLConnection.HTTP_OK) {
                    response = new BufferedReader(new InputStreamReader (conn.getInputStream(),"UTF-8"));
                    char[] cbuf = new char[]{};
                    response.read(cbuf);
                    s = response.readLine();
                    while(s != null) {
                        responseText.append(s);
                        responseText.append('\n');
                        s = response.readLine();
                    }
                    System.out.println("End response");
                    break;
                } else {
                    // get error message
                    response = new BufferedReader(new InputStreamReader (conn.getErrorStream()));
                    s = response.readLine();
                    while(s != null) {
                        responseText.append(s);
                        responseText.append('\n');
                        s = response.readLine();
                    }

                    // Look for a detailed error message from the server
                    String errMess = conn.getHeaderField("Error");
                    if (errMess != null) {
                        System.err.println("Error: " + errMess);
                        taskOutput.append("Error: "+ errMess + '\n');
                    } else if (responseText.length()>0) {
                        System.err.println("Error: " + responseText);
                        taskOutput.append("Error: "+ errMess + '\n');
                    }
                    break;
                }

            } catch (ConnectException e) {
                e.printStackTrace();
                System.out.println(e.toString());
                taskOutput.append(e.toString()+ '\n');
                retry ++;
                continue;
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                System.out.println(e.toString());
                taskOutput.append(e.toString()+ '\n');
                retry ++;
                continue;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e){
                throw new InterruptedException();
            } finally {
                //close the connection, set to null
                if(conn!=null) {
                    conn.disconnect();
                    conn = null;
                }
            }
        }
        return responseText.toString();
    }

    public HashMap<String, tag_defs.primative_type> getExistingNodesTypes() {
        return existingStopTypes;
    }

    public HashMap<String, tag_defs.primative_type> getExistingStationTypes() {
        return existingStationTypes;
    }

    public HashMap<String, ArrayList<String>> getExistingStationWayNodes () {return existingStationWayNodes;}

}
