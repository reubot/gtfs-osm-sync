/**
 * Copyright (C) 2023 University of South Florida and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package edu.usf.cutr.go_sync.object;

public class NetexQuay extends NetexStopElement {

    public NetexQuay(String id) {
        super(id);
    }

    public String getIdAsGtfs() {
        return id.split(":")[3];
    }

    public String printContent() {
        return String.format("id: [%s] name: [%s] altNames: %s town: [%s]", id, name, altNames.toString(), town);
    }

}
