package edu.usf.cutr.go_sync;

//
public class tag_defs {

    public final static String XML_RELATION = "relation";
    public final static String XML_TAG      = "tag";
    public final static String XML_MEMBER   = "member";
    public final static String XML_NODE     = "node";
    public final static String XML_WAY      = "way";

    public final static String LAT      = "lat";
    public final static String LON      = "lon";


    //https://developers.google.com/transit/gtfs/reference/
//https://wiki.openstreetmap.org/wiki/Proposed_features/Public_Transport
	public final static String GTFS_STOP_ID_KEY = "gtfs_id";
    public final static String GTFS_OPERATOR_KEY = "network";
    public final static String GTFS_NETWORK_NAME = "agency_name";
    public final static String GTFS_NETWORK_URL = "agency_url";

    public final static String GTFS_NETWORK_ID_KEY  = "agency_id";
    public final static String  OSM_NETWORK_KEY  = "network";
    public final static String  TEMP_NETWORK_KEY  = "temp_network";

    public final static String GTFS_ZONE_KEY  = "zone_id";
    public final static String  OSM_ZONE_KEY  = "transport:zone";
    public final static String GTFS_PLATFORM_KEY  = "platform_code";
    public final static String  OSM_PLATFORM_KEY  = "ref";

    //	public final static String GTFS_OPERATOR_KEY = "operator"
//
//
// ;
    public final static String GTFS_ROUTE_NUM   = "route_short_name";
    public final static String  OSM_ROUTE_NUM   = "ref";
    public final static String GTFS_ROUTE_NAME  = "route_long_name";
    public final static String  OSM_ROUTE_NAME  = "name";
    public final static String GTFS_COLOR_KEY   = "route_color";
    public final static String GTFS_COLOUR_KEY  = "route_colour";
    public final static String  OSM_COLOUR_KEY  = "colour";
    public final static String GTFS_WHEELCHAIR_KEY = "wheelchair_boarding";
    public final static String OSM_WHEELCHAIR_KEY = "wheelchair";

    public final static String GTFS_STOP_TYPE_KEY  = "location_type";
    public final static String  OSM_STOP_TYPE_KEY  = "public_transport";
    public final static String GTFS_NAME_KEY = "name";
    public final static String ROUTE_KEY = "ref";
    public final static String OSM_ROUTE_TYPE_KEY = "route";

    public final static String GTFS_STOP_URL_KEY  = "stop_url";
    public final static String GTFS_ROUTE_URL_KEY  = "route_url";
    public final static String GTFS_ROUTE_DESC_KEY  = "route_desc";
    public final static String GTFS_STOP_DESC_KEY  = "stop_desc";

    public final static String  OSM_URL_KEY  = "website";
    public final static String  OSM_DESC_KEY  = "description";

    //https://developers.google.com/transit/gtfs/reference#tripstxt
    public final static String GTFS_ROUTE_ID_KEY    = "route_id";
    public final static String GTFS_SERVICE_ID_KEY  = "service_id";
    public final static String GTFS_TRIP_ID_KEY     = "trip_id";
    public final static String GTFS_TRIPS_STOP_ID_KEY = "stop_id";

    public enum primative_type {
        NODE, RELATION, WAY
    }

}

/*
public enum tag_def {


    GTFS_STOP_ID_KEY(12323)
}*/