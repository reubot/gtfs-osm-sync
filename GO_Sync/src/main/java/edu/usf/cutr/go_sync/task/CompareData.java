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

package edu.usf.cutr.go_sync.task;

import edu.usf.cutr.go_sync.gui.ReportViewer;
import edu.usf.cutr.go_sync.object.*;
import edu.usf.cutr.go_sync.osm.*;
import edu.usf.cutr.go_sync.io.GTFSReadIn;

import java.awt.Toolkit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JTextArea;
import javax.swing.ProgressMonitor;

import org.xml.sax.helpers.AttributesImpl;

import edu.usf.cutr.go_sync.tools.OsmDistance;
import edu.usf.cutr.go_sync.tools.OsmFormatter;
import edu.usf.cutr.go_sync.tag_defs;

/**
 *
 * @author Khoa Tran
 */

/**
 * This comparison will allow TreeSet to sort members by distance from gtfsStop_
 * Closer osm stops will be earlier in the set
 * Further stops will be later in the set
 */
class CompareStopDistance implements Comparator<Stop> {
    public CompareStopDistance(Stop gtfsStop) {
        gtfsStop_ = gtfsStop;
    }
    public int compare(Stop rhs, Stop lhs) {
        return (int)Math.signum(OsmDistance.distVincenty(gtfsStop_.getLat(), gtfsStop_.getLon(), rhs.getLat(), rhs.getLon()) -
                   OsmDistance.distVincenty(gtfsStop_.getLat(), gtfsStop_.getLon(), lhs.getLat(), lhs.getLon()));
    }
    private Stop gtfsStop_;
}

public class CompareData extends OsmTask{
    long tStart = System.currentTimeMillis();
    private List<Stop> GTFSstops = new ArrayList<Stop>();
    private List<String> GTFSstopsIDs = new ArrayList<String>();
    private HashMap<String, AttributesImpl> OSMNodes = new HashMap<>();
    private HashMap<String,HashMap> OSMTags = new HashMap<>();
    private ArrayList<AttributesImpl> OSMRelations = new ArrayList<AttributesImpl>();
//    private ArrayList<Hashtable<String, String>> OSMRelationTags = new ArrayList<Hashtable<String, String>>();
    private ArrayList<HashMap> OSMRelationTags = new ArrayList<HashMap>();
    private ArrayList<HashSet<RelationMember>> OSMRelationMembers = new ArrayList<HashSet<RelationMember>>();
    private HashMap<String, HashSet<RelationMember>> OSMStationMembers = new HashMap<String, HashSet<RelationMember>>();
    // key is gtfs, value is container of potential osm matches, sorted by distance from gtfs stop
    private ConcurrentHashMap<Stop, TreeSet<Stop>> report =
        new ConcurrentHashMap<>();


    private ConcurrentHashMap.KeySetView<Stop, Boolean> noUpload = ConcurrentHashMap.newKeySet();
    private ConcurrentHashMap.KeySetView<Stop, Boolean> upload = ConcurrentHashMap.newKeySet();
    private ConcurrentHashMap.KeySetView<Stop, Boolean> modify = ConcurrentHashMap.newKeySet();
    private ConcurrentHashMap.KeySetView<Stop, Boolean> delete = ConcurrentHashMap.newKeySet();
//    private HashSet<Stop> noUpload = new HashSet<Stop>();
//    private HashSet<Stop> upload = new HashSet<Stop>();
//    private HashSet<Stop> modify = new HashSet<Stop>();
//    private HashSet<Stop> delete = new HashSet<Stop>();
    private Hashtable<String, Route> routes = new Hashtable<String, Route>();
    private Hashtable<String, Route> agencyRoutes = new Hashtable<String, Route>();
    private Hashtable<String, Route> existingRoutes = new Hashtable<String, Route>();
    private double minLat=0, minLon=0, maxLat=0, maxLon=0;
    private HttpRequest osmRequest;
    private HashSet<String> osmActiveUsers = new HashSet<String>();
    private Hashtable<String,String> osmIdToGtfsId = new Hashtable<String,String>();

    private HashMap<String,tag_defs.primative_type> OSMNodesType = new HashMap<>();
    private HashMap<String,ArrayList<String>> OSMWayNodes = new HashMap<>();

    private final double ERROR_TO_ZERO = 0.5;       // acceptable error while calculating distance ~= consider as 0
    private /*final*/ double DELTA = 0.004;   // ~400m in Lat and 400m in Lon       0.00001 ~= 1.108m in Lat and 0.983 in Lon
    private /*final*/ double RANGE = 400;         // FIXME bus stop is within 400 meters

//    private final double DELTA = 0.001;   // ~400m in Lat and 400m in Lon       0.00001 ~= 1.108m in Lat and 0.983 in Lon
//    private final double RANGE = 100;         // FIXME bus stop is within 400 meters



    private String fileNameInStops;
    private String fileNameInTrips;
    private String fileNameInRoutes;
    private String fileNameInStopTimes;
    private String fileNameInAngency;

    private ProgressMonitor progressMonitor;
    private JTextArea taskOutput;

    private class ReportData
    {
        //    	static enum report_category{
//    		MODIFY,
//    		NOTHING_NEW,
//    		UPLOAD_CONFLICT,
//    		UPLOAD_NO_CONFLICT;
//    	}    	;
        private HashSet<Stop> upload = new HashSet<Stop>();
        private HashSet<Stop> modify = new HashSet<Stop>();
        private HashSet<Stop> delete = new HashSet<Stop>();

        boolean addStop(Stop s)
        {
            if (!(upload.contains(s)
                    || modify.contains(s) || noUpload.contains(s)))
            {

            }

            return false;
        }
    }

    public CompareData(ProgressMonitor pm, JTextArea to){
        super(pm);
        taskOutput = to;
        osmRequest = new HttpRequest(taskOutput);
        String fileSeparator = System.getProperty("file.separator");
        fileNameInStops = OperatorInfo.getFileDirectory()+ fileSeparator + "stops.txt";
        fileNameInTrips = OperatorInfo.getFileDirectory()+ fileSeparator + "trips.txt";
        fileNameInRoutes = OperatorInfo.getFileDirectory()+ fileSeparator + "routes.txt";
        fileNameInStopTimes = OperatorInfo.getFileDirectory()+ fileSeparator + "stop_times.txt";
        fileNameInAngency = OperatorInfo.getFileDirectory()+ fileSeparator + "agency.txt";
        progressMonitor = pm;
    }

    public void setRangeThreshold(double newRange)
    {
        RANGE= newRange;
        DELTA = RANGE/100000;
    }

    public List<Stop> convertToStopObject(List<AttributesImpl> eNodes, String agencyName){
        List<Stop> stops = new ArrayList<Stop>(eNodes.size());
        for (AttributesImpl s : eNodes) {
            stops.add(new Stop(s.getValue("id"), agencyName, s.getValue("user"), s.getValue(tag_defs.LAT), s.getValue(tag_defs.LON)));
        }
        return stops;
    }

    public void getBoundingBox(){
        // Get bound
        Iterator<Stop> it = GTFSstops.iterator();
        boolean isFirst = true;
        while (it.hasNext()) {
            Stop tempStop = it.next();
            if (isFirst) {
                isFirst = false;
                minLat = Double.parseDouble(tempStop.getLat());
                maxLat = Double.parseDouble(tempStop.getLat());
                minLon = Double.parseDouble(tempStop.getLon());
                maxLon = Double.parseDouble(tempStop.getLon());
            }
            else {
                double currLat=0, currLon=0;
                try{
                    currLat = Double.parseDouble(tempStop.getLat());
                    currLon = Double.parseDouble(tempStop.getLon());
                } catch(Exception e){
                    this.flagIsDone = true;
                    setMessage("GTFS latitude and longitude have errors. Please check!");
                    updateProgress(this.getCurrentProgress()+1);
                    break;
                }
                if(currLat < minLat){
                    minLat = currLat;
                } else if(currLat > maxLat) {
                    maxLat = currLat;
                }

                if(currLon < minLon){
                    minLon = currLon;
                } else if(currLon > maxLon) {
                    maxLon = currLon;
                }
            }
        }
        minLat -= DELTA;
        minLon -= DELTA;
        maxLat += DELTA;
        maxLon += DELTA;

        System.out.println("Lon, Lat format = "+minLon+","+minLat + "      " + maxLon + "," + maxLat);

        List<Stop> boundList = new ArrayList<Stop>(2);
        boundList.add(new Stop("-1","Min Lat Min Lon", "UNKNOWN", Double.toString(minLat),Double.toString(minLon)));
        boundList.add(new Stop("-1","Max Lat Max Lon", "UNKNOWN",Double.toString(maxLat),Double.toString(maxLon)));

    }

    /**
      * add osm stop to list of potential matches
      * by using TreeSort, each tree is sorted by distance from potentially match osm stop
      * @param gtfs GTFS stop
      * @param osm add this stop to array of potential matches to gtfsn
      * @param found if true, then osm is only match
      *                        otherwise, append stop to list
      */

    public void addToReport(Stop gtfs, Stop osm, boolean found) {
        Stop gtfsStop = new Stop(gtfs);
        if (osm!=null) {
            Stop osmStop = new Stop(osm);
            TreeSet<Stop> tree = null;
            if (report.containsKey(gtfsStop)) {
                tree = report.get(gtfsStop);
                if (found) {
                    tree.clear();
                }
                report.remove(gtfsStop);
            }

            if (tree == null) {
                tree = new TreeSet<Stop>(new CompareStopDistance(gtfsStop));
            }
            tree.add(osmStop);

            // set stop value to osm value as default (only affect modify category)
            if(gtfsStop.getReportCategory().equals(OsmPrimitive.RC.MODIFY)){
                gtfsStop.setLat(osmStop.getLat());
                gtfsStop.setLon(osmStop.getLon());
                gtfsStop.addAndOverwriteTags(osmStop.getTags());
                // some stops id are accidentally overwritten. We need to keep the original stop_id
                gtfsStop.addAndOverwriteTag(tag_defs.GTFS_STOP_ID_KEY, gtfs.toString());
            }
            if(gtfsStop.toString().equals("none") || gtfsStop.getStopID().equals("") || gtfsStop.getTag(tag_defs.GTFS_STOP_ID_KEY).equals("none")){
                System.out.println();
            }

            report.put(gtfsStop, tree);
        }
        // successfully added
        else {
            if (report.containsKey(gtfsStop)) {
                report.remove(gtfsStop);
            }
            report.put(gtfsStop, new TreeSet<Stop>(new CompareStopDistance(gtfsStop)));
        }
    }

    /** Remove duplicates between upload set and noUpload set
     * Otherwise, many noUpload nodes will be in upload set
     * */
    public void reviseUpload(){
        Iterator<Stop> it = noUpload.iterator();
        while (it.hasNext()) {
            Stop s = (Stop)it.next();
            if (upload.contains(s)) {
                upload.remove(s);
            }
        }
        it = modify.iterator();
        while (it.hasNext()) {
            Stop s = (Stop)it.next();
            if (upload.contains(s)) {
                upload.remove(s);
            }
        }
    }

    /** Remove duplicates between modify set and noUpload set
     * Otherwise, all modify nodes will be in noUpload set
     * since our algorithm add node into noUpload set everytime a gtfs node matches an OSM node
     * */
    public void reviseNoUpload(){
        Iterator<Stop> it = modify.iterator();
        while (it.hasNext()) {
            Stop s = (Stop)it.next();
            if (noUpload.contains(s)) {
                noUpload.remove(s);
            }
        }
    }

    /** compare osmtag with new gtfs tag
     */
    public Hashtable<String,String> compareOsmTags(HashMap<String,String> osmtag, OsmPrimitive p) {
        Hashtable<String,String> diff = new Hashtable<String,String>();
        Hashtable<String,String> t = new Hashtable<String,String>();
        Iterator<String> it = p.keySet().iterator();
        while (it.hasNext()){
            String k = (String)it.next();
            String v = p.getTag(k);
            if (osmtag.containsKey(k)) {
                String osmValue = (String)osmtag.get(k);
                if(!osmValue.toUpperCase().equals(v.toUpperCase())){
                    if (osmValue.indexOf(v)==-1) {
                        diff.put(k, v+";"+osmValue);
                    } else {
                        t.put(k, osmValue);
                    }
                }
            } else {
                diff.put(k, v);
            }
        }
        if(diff.size()>0 && t.size()>0) {
            diff.putAll(t);
        }
        return diff;
    }

    /**
     * Only consider uploaded bus stops
     * e.g., stops in noUpload and (modify sets - modified stops in osm)
     * We can use report hashtable for convenience
     * ALWAYS Invoke this method AFTER compareBusStopData()
     * *//*
    public void compareRouteData() throws InterruptedException{
        //20% of task includes:
        //10% for reading GTFS routes from modify and nothing_new sets
        //10% for compare with existing OSM routes

        updateProgress(10);
        this.setMessage("Reading GTFS routes from modify and noupload sets...");
        // get all the routes and its associated bus stops
        ArrayList<Stop> reportKeys = new ArrayList<Stop>();
        reportKeys.addAll(report.keySet());
        HashSet<String> gtfsRoutes = new HashSet<String>();
        gtfsRoutes.addAll(GTFSReadIn.getAllRoutesID());
        for (int i=0; i<reportKeys.size(); i++) {
            if(this.flagIsDone) return;
            Stop st = reportKeys.get(i);
            String category = st.getReportCategory();
            if (category.equals(ReportCategory.MODIFY) || category.equals(ReportCategory.NOTHING_NEW)) {
                ArrayList<Route> routeInOneStop = new ArrayList<Route>();
                if(st.getRoutes()!=null) {
                    routeInOneStop.addAll(st.getRoutes());
                    for(int j=0; j<routeInOneStop.size(); j++){
                        if(this.flagIsDone) return;
                        Route rios = routeInOneStop.get(j);
                        Route r;
                        if(!routes.containsKey(rios.getRouteId())){
                            r = new Route(rios);
                            //add tag
                            r.addTag("name", OperatorInfo.getAbbreviateName()+
                                    " route "+ r.getRouteRef());
                            r.addTag(tag_defs.GTFS_OPERATOR_KEY,OperatorInfo.getFullName());
//                            r.addTag("network",OperatorInfo.getFullName());
                            r.addTag("ref", r.getRouteRef());
                            r.addTag("route", "bus");
                            r.addTag("type", "route");
                        }
                        else {
                            r = new Route((Route)routes.get(rios.getRouteId()));
                            routes.remove(rios.getRouteId());
                        }
                        //add member
                        //Route rt = (Route)routes.get(routeArray[j]);
                        r.addOsmMembers(rios.getOsmMembers());
                        String osmNodeId = st.getOsmId();
                        RelationMember rm = new RelationMember(osmNodeId,"node","stop");
                        rm.setStatus("GTFS dataset");
                        rm.setGtfsId(st.getStopID());
                        r.addOsmMember(rm);
                        r.setStatus("n");
                        routes.put(rios.getRouteId(), r);
                    }
                }
            }
        }

        agencyRoutes.putAll(routes);

        updateProgress(10);
        this.setMessage("Comparing GTFS routes with OSM routes...");
        //compare with existing OSM relation
        ArrayList<String> routeKeys = new ArrayList<String>();
        routeKeys.addAll(routes.keySet());
        for(int osm=0; osm<OSMRelations.size(); osm++){
            if(this.flagIsDone) return;
            AttributesImpl osmRelation = OSMRelations.get(osm);
            Hashtable<String,String> osmtag = new Hashtable<String,String>();
            osmtag.putAll(OSMRelationTags.get(osm));
            String routeName = osmtag.get("ref");
            String routeId 	 = osmtag.get("gtfs_route_id");
            String operator  = osmtag.get(tag_defs.GTFS_OPERATOR_KEY);
//            String network   = osmtag.get(tag_defs.GTFS_NETWORK_KEY);
            if(routeKeys.contains(routeId) && operator!=null && OperatorInfo.isTheSameOperator(operator)) {
                HashSet<RelationMember> em = OSMRelationMembers.get(osm);
                Route r = new Route((Route)routes.get(routeId));
                Route er = new Route(routeId, routeName, operator);
                ArrayList<RelationMember> tempem = new ArrayList<RelationMember>();
                tempem.addAll(em);
                for(int i=0; i<em.size(); i++) {
                    if(this.flagIsDone) return;
                    RelationMember m = tempem.get(i);
                    m.setGtfsId((String)osmIdToGtfsId.get(m.getRef()));
                    er.addOsmMember(m);

                    RelationMember matchMember = r.getOsmMember(m.getRef());
                    if(matchMember!=null) {
                        matchMember.setStatus("both GTFS dataset and OSM server");
                    } else {
                        r.addOsmMember(new RelationMember(m));
                    }
                }
                er.addTags(osmtag);
                er.setOsmVersion(osmRelation.getValue("version"));

                Hashtable<String, String> diff = compareOsmTags(osmtag, r);
                if(!em.containsAll(r.getOsmMembers()) || diff.size()!=0){
                    r.setStatus("m");
                    r.setOsmVersion(osmRelation.getValue("version"));
                    r.setOsmId(osmRelation.getValue("id"));
                    r.addOsmMembers(em);
                    r.addTags(osmtag);
                }
                else {
                    r.setStatus("e");
                }

                routes.remove((String)r.getRouteId());
                routes.put(r.getRouteId(), r);

                existingRoutes.put(routeId, er);
            }
        }
        System.out.println("There are "+routeKeys.size()+" in total!");
    }
*/
    public void compareRouteData() throws InterruptedException{
        //20% of task includes:
        //10% for reading GTFS routes from modify and nothing_new sets
        //10% for compare with existing OSM routes

        updateProgress(10);
        this.setMessage("Reading GTFS routes from modify and noupload sets...");
        System.out.println("Reading GTFS routes from modify and noupload sets...");

        // get all the routes and its associated bus stops
        ArrayList<Stop> reportKeys = new ArrayList<Stop>(report.keySet());
        HashSet<String> gtfsRoutes = new HashSet<String>(GTFSReadIn.getAllRoutesID());
        for (Stop st : reportKeys) {
            if(this.flagIsDone) return;
//            Stop st = reportKeys.get(i);
            OsmPrimitive.RC category = st.getReportCategory();
            if (category.equals(OsmPrimitive.RC.MODIFY) || category.equals(OsmPrimitive.RC.NOTHING_NEW)) {
                ArrayList<Route> routeInOneStop = new ArrayList<Route>();
                if(st.getRoutes()!=null) {
                    routeInOneStop.addAll(st.getRoutes());
//                    for(int j=0; j<routeInOneStop.size(); j++){
                    for(Route rios :routeInOneStop){
                        if(this.flagIsDone) return;
//                        Route rios = routeInOneStop.get(j);
                        Route r;
                        if(!routes.containsKey(rios.getRouteId())){
                            r = new Route(rios);
                            //add tag
                            r.addTag("name", OperatorInfo.getAbbreviateName()+
                                    " Route "+ r.getRouteRef()); //TODO use long route name instead of creating own
                            r.addTag(tag_defs.GTFS_OPERATOR_KEY,OperatorInfo.getFullName());
//                            r.addTag("network",OperatorInfo.getFullName());
                            r.addTag("ref", r.getRouteRef());
//                            r.addTag("route", "bus"); //TODO handle type from gtfs value
                            r.addTag("type", "route");
                        }
                        else {
                            r = new Route((Route)routes.get(rios.getRouteId()));
                            routes.remove(rios.getRouteId());
                        }
                        //add member
                        //Route rt = (Route)routes.get(routeArray[j]);
                        r.addOsmMembers(rios.getOsmMembers());
                        String osmNodeId = st.getOsmId();
                        RelationMember rm = new RelationMember(osmNodeId,"node","stop");
                        rm.setStatus("GTFS dataset");
                        rm.setGtfsId(st.getStopID());
                        r.addOsmMember(rm);
                        r.setStatus(OsmPrimitive.status.NEW);
                        routes.put(rios.getRouteId(), r);
                    }
                }
            }
        }

        agencyRoutes.putAll(routes);

        updateProgress(10);
        System.out.println ("routes " + routes.size());
        this.setMessage("Comparing GTFS routes with OSM routes...");
        System.out.println("Comparing GTFS routes with OSM routes...");

        //compare with existing OSM relation
        ArrayList<String> routeKeys = new ArrayList<String>(routes.keySet());
        System.out.println ("routeKeys " + routeKeys.size()  +" OSMRelations " + OSMRelations.size());

        Hashtable<String, Route> routesByShortName = new Hashtable<String, Route>();


        Iterator<Route> it = routes.values().iterator();
        while (it.hasNext())
//        	        for (Route ri:routes.values())
        {
            Route ri = it.next();
            try{


                ri.getRouteRef();
//        		if (ri.getTag(ri.getTag("gtfs_route_short_name"))!= null)
                routesByShortName.put(ri.getRouteRef(),ri);}
            catch (Exception e)
            {
                e.printStackTrace();
            }
//        	        	routesByShortName.put(ri.getTag("route_short_name"),ri);
//        	            System.out.println(ri.getTag("route_short_name"));
        }



        System.out.println ("routes " + routes.size()  + "routesByShortName" + routesByShortName.size());

        //TODO parralise in Java 8
        ArrayList<String> routeNameKeys = new ArrayList<String>(routesByShortName.keySet());

        for(int osm=0; osm<OSMRelations.size(); osm++){
            if(this.flagIsDone) return;
            AttributesImpl osmRelation = OSMRelations.get(osm);
            HashMap osmtag = new HashMap(OSMRelationTags.get(osm));
            String routeLongName = (String)osmtag.get("name");
            String routeId = (String)osmtag.get("gtfs_route_id");
            String routeShortName = (String)osmtag.get("ref");
            String operator = (String)osmtag.get("operator");  //tag_defs.GTFS_OPERATOR_KEY); //FIXME use tag_defs
            String network = (String)osmtag.get(tag_defs.OSM_NETWORK_KEY);    //FIXME use tag_defs
//            System.out.println(osm + " routeId " + routeId + routeKeys.contains(routeId) +  "routeShortName " + routeShortName + routeNameKeys.contains(routeShortName) + operator + network);
            if((routeKeys.contains(routeId) ||routeNameKeys.contains(routeShortName))
                    && (
                    (operator!=null && OperatorInfo.isTheSameOperator(operator))||
                            (network!=null && OperatorInfo.isTheSameOperator(network))
            )
                    ) {
//                System.out.println(routeId +"\t" + operator);
                HashSet<RelationMember> em = OSMRelationMembers.get(osm);
                Route r;
                String ostring,idstring,refstring;

                if (network != null)
                    ostring = network;
                else ostring = operator;
                if (routeKeys.contains(routeId))
                    r = new Route(routes.get(routeId));
                else

                {
                    r = new Route(routesByShortName	.get(routeShortName));
                    routeId = r.getRouteId();
                }
                Route er = new Route(routeId, routeShortName, ostring);
                ArrayList<RelationMember> tempem = new ArrayList<RelationMember>(em);
                for(int i=0; i<em.size(); i++) {
                    if(this.flagIsDone) return;
                    RelationMember m = tempem.get(i);
                    m.setGtfsId((String)osmIdToGtfsId.get(m.getRef()));
                    er.addOsmMember(m);

                    RelationMember matchMember = r.getOsmMember(m.getRef());
                    if(matchMember!=null) {
                        matchMember.setStatus("both GTFS dataset and OSM server");
                    } else {
                        r.addOsmMember(new RelationMember(m));
                    }
                }
                er.addTags(osmtag);
                er.setOsmVersion(osmRelation.getValue("version"));

                Hashtable diff = compareOsmTags(osmtag, r);
                if(!em.containsAll(r.getOsmMembers()) || diff.size()!=0){
                    r.setStatus(OsmPrimitive.status.MODIFY);
                    r.setOsmVersion(osmRelation.getValue("version"));
                    r.setOsmId(osmRelation.getValue("id"));
                    r.addOsmMembers(em);
                    r.addTags(osmtag);
                }
                else {
                    r.setStatus(OsmPrimitive.status.EMPTY);
                }

                routes.remove((String)r.getRouteId());
                routes.put(r.getRouteId(), r);

                existingRoutes.put(routeId, er);

            }
        }
        System.out.println("There are "+routeKeys.size()+" routeKeys in total!");
    }

    /**
     * FIXME: why is wrong gtfs_id included?
     *
     */
    public void compareBusStopData() throws InterruptedException {
        //Compare the OSM stops with GTFS data

        //this method takes 50% of this compare task
        int totalOsmNode = OSMNodes.size();
        int timeToUpdate, progressToUpdate;
        if(totalOsmNode>=50) {
            timeToUpdate = totalOsmNode/50;
            progressToUpdate = 1;
        } else {
            timeToUpdate = 1;
            progressToUpdate = 50/totalOsmNode;
        }
        int currentTotalProgress=0;
//        for (int osmindex=0; osmindex<totalOsmNode; osmindex++){
        AtomicInteger osmindex= new AtomicInteger();
        OSMTags.entrySet().parallelStream().
                forEach(osmtagEntry -> {
            if(this.flagIsDone)
                return;
            HashMap osmtag = osmtagEntry.getValue();
            String osmid = osmtagEntry.getKey();
            osmindex.getAndIncrement();
            if((osmindex.get()%timeToUpdate)==0) {
//                currentTotalProgress += progressToUpdate;
                updateProgress(progressToUpdate);
                this.setMessage("Comparing "+osmindex.get()+"/"+totalOsmNode+" ...");
            }
//            Hashtable<String, String> osmtag = new Hashtable<String, String>(OSMTags.get(osmindex));
            String osmOperator = (String)osmtag.get(tag_defs.GTFS_OPERATOR_KEY);
            String osmStopID = (String)osmtag.get(tag_defs.GTFS_STOP_ID_KEY);
            //add leading 0's
            if(osmStopID!=null) {
                if (!osmStopID.equals("missing")) {
                    osmStopID = OsmFormatter.getValidBusStopId(osmStopID);
                    osmtag.put(tag_defs.GTFS_STOP_ID_KEY, osmStopID);
                }
            }

            boolean fixme = osmtag.containsKey("FIXME");
            boolean isOp;
            if (osmOperator!=null) {
                if (osmOperator.equals("missing")) {
                    isOp=true;
                } else {
                    isOp = OperatorInfo.isTheSameOperator(osmOperator);
                    // spell out the operator name
                    // osmOperator = _operatorName; //can't do this since it deletes the other operator
                }
            }
            // osmOperator == null --> isOp is true since we need to get to the for loop
            else {
                isOp=true;
                // set operator field to missing
                osmOperator = "missing";
            }
            String osmStopName = (String)osmtag.get("name");
// FIXME This breaks if there are nodes with identical tags
            AttributesImpl node = OSMNodes.get(osmid);
            String osmID = node.getValue("id");
            String version = Integer.toString(Integer.parseInt(node.getValue("version")));
            if (isOp) {
                for (Stop gtfsStop : GTFSstops){
                    if(this.flagIsDone) return;
//                    Stop gtfsStop = GTFSstops.get(gtfsindex);
                    double distance = OsmDistance.distVincenty(node.getValue(tag_defs.LAT), node.getValue(tag_defs.LON),
                            gtfsStop.getLat(), gtfsStop.getLon());

                    if ((distance<RANGE) && !(noUpload.contains(gtfsStop)) ){
                        //&& (!matched.contains(gtfsStop))){
                        // if has same stop_id
                        /**
                         * OsmPrimitive.RC.MODIFY
                         */
                        if ((osmStopID!= null) && (!osmStopID.equals("missing")) && (osmStopID.equals(gtfsStop.getStopID()))){
                            noUpload.add(gtfsStop);
                            osmIdToGtfsId.put(node.getValue("id"), gtfsStop.getStopID());  //EXISTING STOP WITH UPDATE
                            Stop ns = new Stop(gtfsStop);
                            ns.addTags(osmtag);
                            ns.setOsmId(node.getValue("id"));
                            ns.setType(OSMNodesType.get(osmID));
                            ns.setOsmVersion(version);

                            osmActiveUsers.add(node.getValue("user"));

                            // existing OSM Stop
                            Stop es = new Stop(osmStopID, osmOperator, osmStopName, node.getValue(tag_defs.LAT), node.getValue(tag_defs.LON));
                            es.addTags(osmtag);
                            es.setOsmId(node.getValue("id"));
                            es.setType(OSMNodesType.get(osmID));
                            if (es.getType()== tag_defs.primative_type.WAY)
                                es.addOsmNodes(OSMWayNodes.get(osmID));
                            if (es.getType()== tag_defs.primative_type.RELATION)
                                es.addOsmMembers(OSMStationMembers.get(osmID));
                            es.setLastEditedOsmUser(node.getValue("user"));
                            es.setLastEditedOsmDate(node.getValue("timestamp"));
                            // for comparing tag
                            Hashtable<String, String> diff = compareOsmTags(osmtag, gtfsStop);
                            if (distance>ERROR_TO_ZERO) {
//                                Stop ns = new Stop(gtfsStop);
//                                ns.addTags(osmtag);
//                                ns.setOsmId(node.getValue("id"));
//                                ns.setOsmVersion(version);

//                                osmActiveUsers.add(node.getValue("user"));

//                                Stop es = new Stop(osmStopID, osmOperator, osmStopName, node.getValue(tag_defs.LAT), node.getValue(tag_defs.LON));
//                                es.addTags(osmtag);
//                                es.setOsmId(node.getValue("id"));
//                                es.setLastEditedOsmUser(node.getValue("user"));
//                                es.setLastEditedOsmDate(node.getValue("timestamp"));

//                                // for comparing tag
//                                Hashtable<String, String> diff = compareOsmTags(osmtag, gtfsStop);
                                if (diff.isEmpty()) {
                                    es.setReportText("Stop already exists in OSM but with different location." +
                                            "\n ACTION: Modify OSM stop with new location!");
                                } else {
                                    ns.addAndOverwriteTags(diff);
                                    es.setReportText("Stop already exists in OSM but with different location.\n" +
                                            "\t   Some stop TAGs are also different." +
                                            "\n ACTION: Modify OSM stop with new location and stop tags!");
                                }

                                if (modify.contains(ns)) {
                                    modify.remove(ns);
                                }
                                modify.add(ns);
                                ns.setReportCategory(OsmPrimitive.RC.MODIFY);
                                addToReport(ns, es, true);
                                break;
                            }
                            else {
//                                Stop ns = new Stop(gtfsStop);
//                                ns.setOsmId(node.getValue("id"));
//                                ns.addTags(osmtag);
//                                ns.setOsmVersion(version);

//                                osmActiveUsers.add(node.getValue("user"));

//                                Stop es = new Stop(osmStopID, osmOperator, osmStopName, node.getValue(tag_defs.LAT), node.getValue(tag_defs.LON));
//                                es.addTags(osmtag);
//                                es.setOsmId(node.getValue("id"));
//                                es.setLastEditedOsmUser(node.getValue("user"));
//                                es.setLastEditedOsmDate(node.getValue("timestamp"));

//                                // for comparing tag
//                                Hashtable<String, String> diff = compareOsmTags(osmtag, gtfsStop);
                                if (diff.isEmpty()) {
                                    /**
                                     * OsmPrimitive.RC.NOTHING_NEW
                                     * "Existing Stops"
                                     */
                                    es.setReportText("Stop already exists in OSM. Nothing new from last upload.\n" +
                                            "\t   " + es.printOSMStop() +
                                            "\n ACTION: No upload!");
                                    ns.setReportCategory(OsmPrimitive.RC.NOTHING_NEW);
                                    addToReport(ns, es, true);
                                    noUpload.add(ns);
                                    osmIdToGtfsId.put(node.getValue("id"), gtfsStop.getStopID());
                                } else {
                                    ns.addAndOverwriteTags(diff);
                                    es.setReportText("Stop already exists in OSM but some TAGs are different.\n" +
                                            "\t   " + es.printOSMStop() + "\n ACTION: Modify OSM stop with new tags!");
                                    ns.setOsmVersion(version);
                                    if (modify.contains(ns)) {
                                        modify.remove(ns);
                                    }
                                    modify.add(ns);
                                    osmActiveUsers.add(node.getValue("user"));
                                    ns.setReportCategory(OsmPrimitive.RC.MODIFY);
                                    addToReport(ns, es, true);
                                }
                                break;
                            }
                        }
                        // stop_id == null OR OSMnode does NOT have same stop id
                        else {
                            Stop ns = new Stop(gtfsStop);
                            osmActiveUsers.add(node.getValue("user"));
                            ns.setType(OSMNodesType.get(osmID));

                            Stop es = new Stop(osmStopID, osmOperator, osmStopName, node.getValue(tag_defs.LAT), node.getValue(tag_defs.LON));
                            es.addTags(osmtag);
                            es.setOsmId(node.getValue("id"));
                            es.setType(OSMNodesType.get(osmID));
                            if (es.getType()== tag_defs.primative_type.WAY)
                                es.addOsmNodes(OSMWayNodes.get(osmID));
                            if (es.getType()== tag_defs.primative_type.RELATION)
                                es.addOsmMembers(OSMStationMembers.get(osmID));
                            es.setLastEditedOsmUser(node.getValue("user"));
                            es.setLastEditedOsmDate(node.getValue("timestamp"));
                            es.setOsmVersion(version);

                            if (distance>ERROR_TO_ZERO) {
//                                Stop ns = new Stop(gtfsStop);
                                ns.addTag("FIXME", "This bus stop could be redundant");
                                upload.add(ns);

//                                osmActiveUsers.add(node.getValue("user"));

//                                Stop es = new Stop(osmStopID, osmOperator, osmStopName, node.getValue(tag_defs.LAT), node.getValue(tag_defs.LON));
//                                es.addTags(osmtag);
//                                es.setOsmId(node.getValue("id"));
//                                es.setLastEditedOsmUser(node.getValue("user"));
//                                es.setLastEditedOsmDate(node.getValue("timestamp"));
//                                es.setOsmVersion(version);

                                if ((osmStopID == null) || (osmStopID.equals("missing"))) {
                                    es.setReportText("Possible redundant stop. Please check again!");
                                    if ((!fixme) && (!modify.contains(es))) {
/*                                        Stop osms = new Stop(es);
                                        osms.addTag("FIXME", "This bus stop could be redundant");
                                        if (osmOperator==null || osmOperator.equals("missing")) {
                                            osms.addTag("note", "Please add gtfs_id and operator after removing FIXME");
                                            if (osmOperator==null) osms.addTag(tag_defs.GTFS_OPERATOR_KEY,"missing");
                                        }
                                        else {
                                            osms.addTag("note", "Please add gtfs_id after removing FIXME");
                                        }
                                        if (osmStopID==null) {
                                            osms.addAndOverwriteTag("gtfs_id","missing");
                                        }
                                        osms.setOsmVersion(version);

                                        modify.add(osms);*/

                                        es.addTag("FIXME", "This bus stop could be redundant");
                                        if (osmOperator==null || osmOperator.equals("missing")) {
                                            es.addTag("note", "Please add gtfs_id and operator after removing FIXME");
                                            if (osmOperator==null) es.addTag(tag_defs.GTFS_OPERATOR_KEY,"missing");
//                                            if (osmOperator==null) es.addTag("network","missing");
                                        }
                                        else {
                                            es.addTag("note", "Please add gtfs_id after removing FIXME");
                                        }
                                        if (osmStopID==null) {
                                            es.addAndOverwriteTag(tag_defs.GTFS_STOP_ID_KEY,"missing");
                                        }

                                        modify.add(es);

                                        osmActiveUsers.add(node.getValue("user"));
                                    }
                                }


                                else {
                                    // FIXME: should this be potential match if gtfs_id does not match?
                                    es.setReportText("Different gtfs_id but in range. Possible redundant stop. Please check again!\n" +
                                            " ACTION: No modified with FIXME!");
                                }
                                // check for osm id in gtfs db here,  if not in gtfs stops, add as potential match TODO bounding boxes still show up
                                if (!GTFSstopsIDs.contains(osmStopID)) {
                                    ns.setReportCategory(OsmPrimitive.RC.UPLOAD_CONFLICT);
                                    addToReport(ns, es, false);
                                }
                            }
                            // if same lat and lon --> possible same exact stop --> add gtfs_id, operator, stop_name
                            else {
//                                Stop ns = new Stop(gtfsStop);
                                ns.addTags(osmtag);
                                ns.setOsmId(node.getValue("id"));
                                ns.setOsmVersion(version);

                                modify.add(ns);

//                                osmActiveUsers.add(node.getValue("user"));

                                noUpload.add(ns);
                                osmIdToGtfsId.put(node.getValue("id"), gtfsStop.getStopID());
//                                Stop es = new Stop(osmStopID, osmOperator, osmStopName, node.getValue(tag_defs.LAT), node.getValue(tag_defs.LON));
//                                es.addTags(osmtag);
//                                es.setOsmId(node.getValue("id"));
//                                es.setLastEditedOsmUser(node.getValue("user"));
//                                es.setLastEditedOsmDate(node.getValue("timestamp"));
//                                es.setOsmVersion(version);
                                es.setReportText("Possible redundant stop with gtfs_id = "+gtfsStop.getStopID() +
                                        "\n ACTION: Modify OSM stop["+node.getValue("id")+"] with expected gtfs_id and operator!");


                                ns.setReportCategory(OsmPrimitive.RC.MODIFY);
                                addToReport(ns, es, true);
                                break;
                            }
                        }
                    }
                }
            }
        });
        // set OSM value to all stops in modify category
        setStopWithOsmDataDefault();
        //make sure is 50% overall
        int tempProgress=50-currentTotalProgress;
        updateProgress(Math.max(0, tempProgress));
        this.setMessage("Finish comparing bus stop data...");
        if(this.flagIsDone) return;
        reviseUpload();
        reviseNoUpload();
        // Add everything else without worries
        HashSet<Stop> reportKeys = new HashSet<Stop>(report.size());
        reportKeys.addAll(report.keySet());

        HashSet<String> reportIDs = new HashSet<String>();
        System.out.println("reportkeys " + reportKeys.size() + "reportIDs " + reportIDs.size());
//JAVA8        reportKeys.forEach(element -> reportIDs.add(element.getStopID()));
        for (Stop rk:reportKeys)
        {
            reportIDs.add(rk.getStopID());
        }
        // find GTFS stops not in report and mark as new
        for (int i=0; i<GTFSstops.size(); i++) {
            if(this.flagIsDone) return;
//            if ((!noUpload.contains((GTFSstops.get(i)))) && (!reportKeys.contains(GTFSstops.get(i))) ) {
            if ((!noUpload.contains((GTFSstops.get(i)))) && (!reportIDs.contains(GTFSstops.get(i).getStopID())) ) {
                Stop n = new Stop(GTFSstops.get(i));
                n.setType(OSMNodesType.get(tag_defs.primative_type.NODE));
                n.setReportText("New upload with no conflicts");
                n.setReportCategory(OsmPrimitive.RC.UPLOAD_NO_CONFLICT);
                upload.add(n);

                addToReport(n, null, false);
            }
        }

        System.out.println(osmActiveUsers.toString());
    }

    public void setStopWithOsmDataDefault(){
        ArrayList<Stop> reportKeys = new ArrayList<Stop>(report.keySet());
        for(int i=0; i<reportKeys.size(); i++){
            if(this.flagIsDone) return;
            Stop s = new Stop(reportKeys.get(i));
            OsmPrimitive.RC category = s.getReportCategory();
            if(category.equals(OsmPrimitive.RC.MODIFY)){
                TreeSet<Stop> arr = report.get(s);
                if(arr.size()==1) {
                    String tempStopId=null;
                    report.remove(s);
                    // empty all the value of the tags
                    Hashtable<String, String> newTags = s.getTags();
                    ArrayList<String> newTagKeys = new ArrayList<String>(newTags.keySet());
                    for (String newTagKey : newTagKeys) {
                        if (newTagKey.equals(tag_defs.GTFS_STOP_ID_KEY)) tempStopId = newTags.get(tag_defs.GTFS_STOP_ID_KEY);

                        newTags.put(newTagKey, "");
                    }
                    s.addAndOverwriteTags(newTags);
                    // add Osm tags, the rest remains empty
                    s.addAndOverwriteTags(arr.first().getTags());
                    if(tempStopId!=null) s.addAndOverwriteTag(tag_defs.GTFS_STOP_ID_KEY, tempStopId);
                    report.put(s, arr);
                }
            }
        }
    }

    /*
     * compare stops and routes information between GTFS dataset and OSM data
     */
    public void startCompare() throws InterruptedException{
        updateProgress(1);
        this.setMessage("Getting bounding box...");
        getBoundingBox();
        try{
            if(this.flagIsDone) return;
            //Get the existing bus stops from the server
            updateProgress(1);
            this.setMessage("Checking API version...");
            System.out.println("Initializing...");
            osmRequest.checkVersion();
            if(this.flagIsDone) return;

            updateProgress(5);
            this.setMessage("Getting existing bus stops...");
            progressMonitor.setNote("This might take several minutes...");
            HashMap<String, AttributesImpl> tempOSMNodes = osmRequest.getExistingBusStops(Double.toString(minLon), Double.toString(minLat),
                    Double.toString(maxLon), Double.toString(maxLat));
            if(this.flagIsDone) return;
            progressMonitor.setNote("");
            updateProgress(10);
            this.setMessage("Getting existing stations...");
            progressMonitor.setNote("This might take several minutes...");
            HashMap<String, AttributesImpl> tempOSMstations = osmRequest.getExistingStopWaysRelations(Double.toString(minLon), Double.toString(minLat),
                    Double.toString(maxLon), Double.toString(maxLat));
            if(this.flagIsDone) return;
            progressMonitor.setNote("");
            updateProgress(15);
            this.setMessage("Getting existing bus routes...");
            progressMonitor.setNote("This might take several minutes...");
            ArrayList<AttributesImpl> tempOSMRelations = osmRequest.getExistingBusRelations(Double.toString(minLon), Double.toString(minLat),
                    Double.toString(maxLon), Double.toString(maxLat));
            if(this.flagIsDone) return;
            long tDelta = System.currentTimeMillis() - tStart;
            System.out.println("OSM Downloads Completed in "+ tDelta /1000.0 + "seconds");
            tStart = System.currentTimeMillis();
            progressMonitor.setNote("");
            if (tempOSMNodes!=null) {
                OSMNodes.putAll(tempOSMNodes);
                OSMTags.putAll(osmRequest.getExistingBusStopsTags());
                OSMNodesType.putAll(osmRequest.getExistingNodesTypes());
//                FIXME station comparison is broken
                OSMNodes.putAll(tempOSMstations);
                OSMTags.putAll(osmRequest.getExistingStationTags());
                OSMNodesType.putAll(osmRequest.getExistingStationTypes());
                OSMWayNodes.putAll(osmRequest.getExistingStationWayNodes());
                OSMStationMembers.putAll(osmRequest.getExistingStationMembers());
                System.out.println("Existing Nodes = "+OSMNodes.size());
                System.out.println("New Nodes = "+GTFSstops.size());
                compareBusStopData();
                if(this.flagIsDone) return;

                if(tempOSMRelations!=null) {
                    OSMRelations.addAll(tempOSMRelations);
                    OSMRelationTags.addAll(osmRequest.getExistingBusRelationTags());
                    OSMRelationMembers.addAll(osmRequest.getExistingBusRelationMembers());
                }
                compareRouteData();
                if(this.flagIsDone) return;
            }
            else {
                System.out.println("There's no bus stop in the region "+minLon+", "+minLat+", "+maxLon+", "+maxLat);
                // add all gtfs stops to report as new stops
                for (int i=0; i<GTFSstops.size(); i++) {
                    if(this.flagIsDone) return;
                    Stop n = new Stop(GTFSstops.get(i));
                    n.setReportText("New upload with no conflicts");
                    n.setReportCategory(OsmPrimitive.RC.UPLOAD_NO_CONFLICT);
                    upload.add(n);
                    addToReport(n, null, false);
                }
            }
        } catch (InterruptedException e){
            this.flagIsDone = true;
            return;
        }
    }

    @Override
    public Void doInBackground(){
/*        GTFSReadIn datadbg = new GTFSReadIn();
        OperatorInfo.setFullName(datadbg.readAgency(fileNameInAngency));
        List<Stop> stdbg = datadbg.readBusStop(fileNameInStops, OperatorInfo.getFullName(), null, null, null);

        System.out.println (stdbg.size());*/
        try{
            setProgress(0);
            updateProgress(1);
            this.setMessage("Reading GTFS files ... ");
            GTFSReadIn data = new GTFSReadIn();
            String aName = data.readAgency(fileNameInAngency);
            System.out.println("Agency Name: " + aName);
            if (aName!= null)
                OperatorInfo.setFullName(aName);
            List<Stop> st = data.readBusStop(fileNameInStops, OperatorInfo.getFullName(), fileNameInRoutes, fileNameInTrips, fileNameInStopTimes);
            if(this.flagIsDone){
                updateProgress(100);
                done();
            }
            if(st!=null && !this.flagIsDone) {
                GTFSstops.addAll(st);
                Iterator<Stop> it = GTFSstops.iterator();

                while (it.hasNext())
                    GTFSstopsIDs.add(it.next().getStopID());
                try {
                    startCompare();
                } catch(InterruptedException e){
                    this.flagIsDone = true;
                }
                if(this.flagIsDone) done();
            }
            else {
                this.setMessage("No GTFS stops to be processed");
            }
        }  catch (Exception e)     {         System.err.println(e + " " + e.getStackTrace());    }
        finally {
            //make sure it's a complete task
            if(this.flagIsDone) firePropertyChange("progress", this.getCurrentProgress(), 100);
            else updateProgress(100);
            this.setMessage("Done...");
            System.out.println("Done...!!");
        }
        return null;
    }

    @Override
    public void done() {
        long tDelta = System.currentTimeMillis() - tStart;
//        this.setMessage("Completed in "+ tDelta /1000.0 + "seconds");
        System.out.println("Completed in "+ tDelta /1000.0 + "seconds");
        Toolkit.getDefaultToolkit().beep();
        boolean isCanceled = progressMonitor.isCanceled();
        progressMonitor.close();
        if(!isCanceled && !report.isEmpty() && this.getCurrentProgress()>=100)
            generateReport();
        else {
            this.flagIsDone = true;
            return;
        }
    }

    public void generateReport(){

        System.out.println("GTFSstops "+ GTFSstops.size() + " report:" + report.size() + " upload:" + upload.size() + " modify:" + modify.size() + " delete:" + delete.size()  );

        // copy report, where values are TreeSets,
        // to reportArrays, where values are ArrayLists
        // which is what ReportViewer wants
        Hashtable<Stop, ArrayList<Stop>> reportArrays = new Hashtable<Stop, ArrayList<Stop>>();

        for (Map.Entry<Stop, TreeSet<Stop>> entry : report.entrySet()) {
            Stop key = entry.getKey();
            ArrayList<Stop> arr = new ArrayList<Stop>(entry.getValue());
            reportArrays.put(key, arr);
        }
        ReportViewer rv = new ReportViewer(GTFSstops, reportArrays,
                new HashSet<Stop>(upload),
                new HashSet<Stop>(modify),
                new HashSet<Stop>(delete),
                routes, agencyRoutes, existingRoutes, taskOutput);
//    	ReportViewer rv = new ReportViewer(GTFSstops, reportArrays, upload, modify, delete, routes, agencyRoutes, existingRoutes, taskOutput);
        String info = "Active OSM bus stop mappers:\n"+osmActiveUsers.toString()+"\n\n";
        info += "There are currently "+OSMNodes.size()+" OSM stops in the region\n\n";
        info += "Transit agency GTFS dataset has "+GTFSstops.size()+" stops";


        rv.SetGeneralInformationToStopTextArea(info);

        info = "There are currently "+existingRoutes.size()+" OSM routes in the region\n\n";
        info += "Transit agency GTFS dataset has "+agencyRoutes.size()+" routes";
        rv.SetGeneralInformationToRouteTextArea(info);
        rv.setResizable(true);
        rv.setVisible(true);
    }
}

//TODO parallelise comparebusdata using https://stackoverflow.com/questions/11366330/waiting-for-multiple-swingworkers/11372932#11372932
