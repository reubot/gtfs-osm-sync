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
import java.util.HashSet;
import java.util.Hashtable;

import edu.usf.cutr.go_sync.object.OsmPrimitive;
import edu.usf.cutr.go_sync.object.RelationMember;
import edu.usf.cutr.go_sync.object.Stop;
import org.xml.sax.helpers.DefaultHandler;
import edu.usf.cutr.go_sync.tag_defs;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Khoa Tran
 */
public class ChangesetDownloadParser extends DefaultHandler{
    private Stop.status status= null;
    private HashSet<Stop> upload, modify, delete;

    private final static String qNameCreate    = "create";
    private final static String qNameModify    = "modify";
    private final static String qNameDelete    = "delete";


    public ChangesetDownloadParser(){
        upload = new HashSet<Stop>();
        modify = new HashSet<Stop>();
        delete = new HashSet<Stop>();
    }

    @Override public void startElement(String namespaceURI, String localName, String qname, Attributes attributes) throws SAXException {
        if (qname.equals(qNameCreate)) {
            status=Stop.status.NEW;
        } else if (qname.equals(qNameModify)) {
            status=Stop.status.MODIFY;
        } else if (qname.equals(qNameDelete)) {
            status=Stop.status.DELETE;
        } else if (qname.equals(tag_defs.XML_NODE)) {
            AttributesImpl attImpl = new AttributesImpl(attributes);
            String osmid = attImpl.getValue("id");
            String version = attImpl.getValue("version");
            Stop s = new Stop(osmid, null,null,"0","0");
            s.setOsmId(osmid);
            s.setOsmVersion(version);
            if (status.equals(Stop.status.NEW)) {
                delete.add(s);
            } else if (status.equals(Stop.status.MODIFY)) {
                modify.add(s);
            } else if (status.equals(Stop.status.DELETE)) {
                upload.add(s);
            }
        }
    }

    @Override public void endElement (String uri, String localName, String qName) throws SAXException {
        if (qName.equals(qNameCreate) || qName.equals(qNameModify) || qName.equals(qNameDelete)) {
            status = null;
        }
    }

    public HashSet<Stop> getToBeDeletedStop(){
        return delete;
    }

    public HashSet<Stop> getToBeModifiedStop(){
        return modify;
    }

    public HashSet<Stop> getToBeUploadedStop(){
        return upload;
    }
}
