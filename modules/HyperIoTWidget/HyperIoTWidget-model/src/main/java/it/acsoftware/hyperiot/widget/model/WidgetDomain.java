/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.acsoftware.hyperiot.widget.model;

public enum WidgetDomain {
    INDUSTRY_40(0, "industry-40", "outline-view_list-24px.svg"),
    SMART_FIELDS(1, "smart-fields", "outline-view_list-24px.svg"),
    HEALTH(2, "health", "outline-view_list-24px.svg"),
    IOT(3, "iot", "outline-view_list-24px.svg");

    private int id;
    private String name;
    private String icon;

    WidgetDomain(int id, String name, String icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }
}
