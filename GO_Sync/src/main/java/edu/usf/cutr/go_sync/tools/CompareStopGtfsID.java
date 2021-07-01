package edu.usf.cutr.go_sync.tools;

import edu.usf.cutr.go_sync.object.Stop;
import java.util.Comparator;

public class CompareStopGtfsID implements  Comparator<Stop>{

    @Override
    public int compare(Stop k, Stop j) {
        int result = 0;
        try{
            int ki = Integer.parseInt(k.getStopID());
            int ji = Integer.parseInt(j.getStopID());
            if (ki > ji)
                return 1;
            return -1;

        } catch (NumberFormatException e) {

        }
        /*
        try{
            //try using osmid
            if  (!(k.getOsmId().isEmpty() ||j.getOsmId().isEmpty())) {
                int ki = Integer.parseInt(k.getOsmId());
                int ji = Integer.parseInt(j.getOsmId());
                if (ki > ji)
                    return 1;
                return -1;
            }

        }catch (Exception e) {}
*/
        if ((k).getStopID().hashCode() > (j).getStopID().hashCode())
            return 1;
        return -1;

//             (k.getStopID().hashCode() - (j).getStopID().hashCode());
    }


}