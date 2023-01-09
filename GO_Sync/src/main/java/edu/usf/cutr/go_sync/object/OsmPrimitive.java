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

import edu.usf.cutr.go_sync.tag_defs;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Khoa Tran
 */
public class OsmPrimitive {
    ConcurrentHashMap<String,String> osmTags;
    private String statusString, osmVersion, osmid, reportText, lastEditedOsmUser="", lastEditedOsmDate="";

    /**
     * For stop:
     *      1) MODIFY
     *      2) NOTHING_NEW
     *      3) UPLOAD_CONFLICT
     *      4) UPLOAD_NO_CONFLICT
     * */


    public enum RC {
        UPLOAD_CONFLICT, UPLOAD_NO_CONFLICT, MODIFY, NOTHING_NEW
    }
    /** only has 4 possible value: n=new; m=modify; d=delete; e=empty
     * used for upload osmchange
     */
    public enum status {
        NEW, MODIFY, DELETE, EMPTY
    }

    private tag_defs.primative_type type = null;
    private RC reportCategoryEnum;
    private status statusEnum;
    public OsmPrimitive(){
        osmTags = new ConcurrentHashMap();
    }

    public void addTag(String k, String v){
        if(!osmTags.containsKey(k)) {
            if (v!=null && !v.isEmpty()) {
                osmTags.put(k, v);
            }
            else {
                osmTags.put(k, "none");
            }
        }
    }

    /*
     * Cannot use osmTags.putAll(h)
     * since we don't want the new data overwrite the old one. Use addAndOverwriteTags instead
     * */
    public void addTags(ConcurrentHashMap<String,String> h){
        ArrayList<String> keys = new ArrayList<String>();
        keys.addAll(h.keySet());
        for (int i=0; i<keys.size(); i++){
            String k = keys.get(i);
            if(!osmTags.containsKey(k)) {
                osmTags.put(k,h.get(k));
            }
        }
    }

    public void addTags(Map<String,String> h){
        ArrayList<String> keys = new ArrayList<String>();
        keys.addAll(h.keySet());
        for (int i=0; i<keys.size(); i++){
            String k = keys.get(i);
            if(!osmTags.containsKey(k)) {
                osmTags.put(k,h.get(k));
            }
        }
    }

    public void addAndOverwriteTag(String k, String v){
        if (v!=null && !v.isEmpty()) {
            osmTags.put(k, v);
        }
        else {
            osmTags.put(k, "none");
        }
    }

    public void addAndOverwriteTags(ConcurrentHashMap h){
        osmTags.putAll(h);
    }

    public String getTag(String k){
        if (osmTags.containsKey(k))
            return (String)osmTags.get(k);
        return null;
    }

    public ConcurrentHashMap<String,String> getTags(){
        return osmTags;
    }

    public HashSet<String> keySet(){
        HashSet<String> keys = new HashSet<String>(osmTags.size());
        keys.addAll(osmTags.keySet());
        return keys;
    }

    public boolean containsKey(String k){
        return osmTags.containsKey(k);
    }

    public void removeTag(String k){
        if (osmTags.containsKey(k)) osmTags.remove(k);
    }

    /* only has 4 possible value: n=new; m=modify; d=delete; e=empty
     * used for upload osmchange
     * */
//    public void setStatus(String v){
//        statusString = v;
//    }
//
//    public String getStatus(){
//        return statusString;
//    }

    public void setStatus/*Enum*/(status statusEnum) {
        this.statusEnum = statusEnum;
    }

    public status getStatus/*Enum*/() {
        return statusEnum;
    }
    public void setOsmVersion(String v){
        osmVersion = v;
    }

    public String getOsmVersion(){
        return osmVersion;
    }

    public void setOsmId(String v){
        osmid = v;
    }

    public String getOsmId(){
        return osmid;
    }

    /*
     * For stop:
     *      1) MODIFY
     *      2) NOTHING_NEW
     *      3) UPLOAD_CONFLICT
     *      4) UPLOAD_NO_CONFLICT
     * */

    public void setReportCategory/*Enum*/(RC e) {
        reportCategoryEnum = e;
    }

    public RC getReportCategory/*Enum*/() {
        return reportCategoryEnum;
    }

//    public void setReportCategory(String v){
//        reportCategory = v;
//    }
//
//    public String getReportCategory(){
//        return reportCategory;
//    }


    public void setReportText(String v){
        reportText = v;
    }

    public String getReportText(){
        return reportText;
    }

    public void setLastEditedOsmUser(String v){
        if(v!=null) lastEditedOsmUser = v;
    }

    public String getLastEditedOsmUser(){
        return lastEditedOsmUser;
    }

    public void setLastEditedOsmDate(String v){
        if(v!=null) lastEditedOsmDate = v;
    }

    public String getLastEditedOsmDate(){
        return lastEditedOsmDate;
    }

    public void setType(tag_defs.primative_type t) {
        type = t;
    }

    public tag_defs.primative_type getType() {
        return type;
    }

}
