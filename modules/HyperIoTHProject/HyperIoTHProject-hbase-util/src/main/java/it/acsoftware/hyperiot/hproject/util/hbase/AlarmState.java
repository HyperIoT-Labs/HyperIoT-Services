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

package it.acsoftware.hyperiot.hproject.util.hbase;

import java.util.stream.Stream;

/**
 * @author Francesco Salerno
 */
public enum AlarmState {

    UP("UP") ,
    DOWN ("DOWN"),
    HANDLED("HANDLED");


    private final String name;

    AlarmState(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public static AlarmState fromString(String name) {
        return Stream.of(AlarmState.values())
                .filter(e -> e.getName().equals(name))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public static boolean isValidName(String name){
        for(AlarmState alarmState : AlarmState.values()){
            if (alarmState.getName().equals(name)){
                return true;
            }
        }
        return false;
    }

}
