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

package edu.usf.cutr.go_sync.object;

import java.util.*;

import edu.usf.cutr.go_sync.tools.OsmDistance;
import edu.usf.cutr.go_sync.tag_defs;
/**
 *
 * @author Khoa Tran
 */

public class Stop extends OsmPrimitive implements Comparable{

    public static final Comparator<Stop> COMPARE_PADDED = new ComparePaddedStopGtfsID();
    public static class ComparePaddedStopGtfsID implements Comparator<Stop> {

        @Override
        public int compare(Stop k, Stop j) {
            return k.getPaddedStopID().compareToIgnoreCase(j.getPaddedStopID());

        }
    }
	private final double ERROR_TO_ZERO = 0.5;
	private final String GTFS_STOP_ID_KEY	= tag_defs.GTFS_STOP_ID_KEY;
    private final String GTFS_OPERATOR_KEY	= tag_defs.GTFS_OPERATOR_KEY;
    private final String GTFS_NAME_KEY		= tag_defs.GTFS_NAME_KEY;


    private Agency ai;

    private String lat, lon, agencyName, paddedStopID;
    private HashSet<Route> routes;
    private HashSet<RelationMember> osmMembers = new HashSet<>();
    private ArrayList<String> osmWayNodes = new ArrayList<String>();
    public Stop(String stopID, String operatorName, String stopName, String lat, String lon) {
        osmTags = new java.util.concurrent.ConcurrentHashMap();
        if (operatorName == null || operatorName.isEmpty()) operatorName="none";
        if (stopID == null || stopID.isEmpty()) stopID="none";
        paddedStopID = String.format("%1$" + 10 + "s", stopID);
        if (stopName == null || stopName.isEmpty()) stopName="none";
//        osmTags.put("highway", "bus_stop");
        osmTags.put(GTFS_STOP_ID_KEY, stopID);
        osmTags.put(GTFS_OPERATOR_KEY, operatorName);
        osmTags.put(GTFS_NAME_KEY, stopName);

        //        osmTags.put("bus", "yes");
//        osmTags.put("public_transport", "plaform");

//        if (!stopID.contains("place")) osmTags.put("url", "http://translink.com.au/stop/"+stopID);
//        osmTags.put("url", "http://translink.com.au/stop/"+stopID);
//        osmTags.put("source", "http://translink.com.au/about-translink/reporting-and-publications/public-transport-performance-data");

//       osmTags.put("network", getOperatorName());
//      osmTags.put(GTFS_OPERATOR_KEY, "");
        this.lat = lat;
        this.lon = lon;
        this.setType(tag_defs.primative_type.NODE); //default to node
        routes = new HashSet<Route>();
    }

    public Stop(Stop s) {
        this.osmTags = new java.util.concurrent.ConcurrentHashMap();
        this.osmTags.putAll(s.osmTags);
//        this.osmTags.put("highway", "bus_stop");

        this.osmTags.put(GTFS_STOP_ID_KEY, s.getStopID());
        this.osmTags.put(GTFS_OPERATOR_KEY, s.getOperatorName());
        this.osmTags.put(GTFS_NAME_KEY, s.getStopName());

//        this.osmTags.put("url", "http://translink.com.au/stop/"+s.getStopID());
//        if (!s.getStopID().contains("place")) this.osmTags.put("url", "http://translink.com.au/stop/"+s.getStopID());
//        this.osmTags.put("url", s.getTag("source_ref"));
//        System.out.println(s.getTag("source_ref"));
//        this.osmTags.put("source", "http://translink.com.au/about-translink/reporting-and-publications/public-transport-performance-data");


//        this.osmTags.put("network", s.getOperatorName());
//        this.osmTags.put(GTFS_OPERATOR_KEY, "");
        this.lat = s.lat;
        this.lon = s.lon;
        this.setOsmId(s.getOsmId());
        this.setOsmVersion(s.getOsmVersion());
        this.setReportCategory(s.getReportCategory());
//        this.setReportCategoryEnum(s.getReportCategoryEnum());
        this.setReportText(s.getReportText());
        this.setStatus(s.getStatus());
        this.setLastEditedOsmDate(s.getLastEditedOsmDate());
        this.setLastEditedOsmUser(s.getLastEditedOsmUser());
        this.setType(s.getType());
        this.setOsmData(s);
        //if(s.getOsmNodes()!=null) this.addOsmNodes(s.getOsmNodes());
        routes = new HashSet<Route>();
        routes.addAll(s.routes);
        this.ai = s.ai;
        this.paddedStopID = s.paddedStopID;
    }

    public void fixNetwork(){
        if (osmTags.containsKey(tag_defs.TEMP_NETWORK_KEY))
        {
            osmTags.put(tag_defs.OSM_NETWORK_KEY, osmTags.get(tag_defs.TEMP_NETWORK_KEY));
            osmTags.remove(tag_defs.TEMP_NETWORK_KEY);
        }
    }
    public Agency getAgency(){
        return ai;
    }

    public void setAgency(Agency ail){
        ai= ail;
        agencyName = ail.getName();
    }

    /**
     * Copies id, type and members/nodes from s
     * @param s Stop to copy from
     */
    public void setOsmData(Stop s){
        this.setOsmId(s.getOsmId());
        this.setType(s.getType());
        if(s.osmWayNodes !=null) this.addOsmNodes(s.osmWayNodes);
        if(s.osmMembers !=null) this.osmMembers.addAll(s.osmMembers);

    }


    public void addRoutes(HashSet<Route> r){
        routes.addAll(r);
    }

    public HashSet<Route> getRoutes(){
        return routes;
    }

    public void setLat(String v){
        lat = v;
    }

    public void setLon(String v){
        lon = v;
    }

    public String getStopID(){
        return (String)osmTags.get(GTFS_STOP_ID_KEY);
    }
    public String getPaddedStopID(){
        return paddedStopID;
    }

    public String getOperatorName(){
        return (String)osmTags.get(GTFS_OPERATOR_KEY);
    }

    public String getStopName(){
        return (String)osmTags.get(GTFS_NAME_KEY);
    }

    public String getLat(){
        return lat;
    }

    public String getLon(){
        return lon;
    }

    public boolean compareOperatorName(Stop o) {
        if ((!this.getOperatorName().equals("none")) && (!o.getOperatorName().equals("none"))) {
            return OperatorInfo.isTheSameOperator(this.getOperatorName())
                    && OperatorInfo.isTheSameOperator(o.getOperatorName());
        }
        else if ((this.getOperatorName().equals("none")) && (o.getOperatorName().equals("none"))) {
            return true;
        }
        return false;
    }

    public int compareTo(Object o){
        Stop s = (Stop) o;
        double distance = OsmDistance.distVincenty(this.lat, this.lon,
                s.lat, s.lon);
        if (!(this.getStopID().equals("none")) && !(this.getStopID().equals("missing"))
                && (!(s.getStopID().equals("none"))) && (!(s.getStopID().equals("missing")))
                && (!this.getOperatorName().equals("none")) && (!s.getOperatorName().equals("none"))
                && (!this.getOperatorName().equals("missing")) && (!s.getOperatorName().equals("missing"))) {
            if ((s.getStopID().equals(this.getStopID())) && (this.compareOperatorName(s))) {
                return 0;
            }
        }
        else {
            if (distance < ERROR_TO_ZERO) {
                return 0;
            }
        }
        return 1;
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof Stop) {
            if (this.compareTo((Stop) o)==0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode(){
        String id = this.getStopID();
        return id.hashCode();
    }

    public String printOSMStop(){
        String temp="";
        Stop st = new Stop(this);
        if (st.getOsmId()!=null) {
            temp = temp+"node_id:"+st.getOsmId()+";name:"+st.getStopName()+";lat:"+
                    st.lat +";lon:"+ st.lon;
        }
        else {
            temp = temp+"name:"+st.getStopName()+";lat:"+
                    st.lat +";lon:"+ st.lon;
        }
        HashSet<String> keys = st.keySet();
        Iterator it = keys.iterator();
        while (it.hasNext()){
            String k = (String)it.next();
            temp = temp+";"+k+":"+st.getTag(k);
        }
        return temp;
    }

    @Override
    public String toString(){
        if (this.getStopID().equals("missing")){
            String retname = "missing";
            if (this.osmTags.containsKey("name") && !this.osmTags.get("name").equals("none"))
                retname = this.osmTags.get("name").toString();
            if (this.osmTags.containsKey("ref"))
                retname = retname + " " + osmTags.get("ref");
            return retname;

        }
        return this.getStopID();
    }

    public void addOsmNodes(ArrayList<String> oNodes){
        osmWayNodes.addAll(oNodes);
    }
    public ArrayList<String> getOsmNodes(){
        return osmWayNodes;
    }

    public HashSet<RelationMember> getOsmMembers(){
        return osmMembers;
    }
    public void addOsmMembers(HashSet<RelationMember> oMembers) {
        osmMembers.addAll(oMembers);

    }

}