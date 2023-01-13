package edu.usf.cutr.go_sync.object;

public class Agency {

    private  String agencyName, agencyUrl;
    public Agency(String aName, String aUrl){
        agencyName = aName;
        agencyUrl  = aUrl;
    }

    public String getName() {
        return agencyName;
    }
}
