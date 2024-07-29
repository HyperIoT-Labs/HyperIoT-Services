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

package it.acsoftware.hyperiot.rule.model.facts;

import java.util.Date;

public class FiredRule {

    private long ruleId;
    private boolean fired;
    private Date lastFiredTimestamp;

    public FiredRule() {
    }

    public FiredRule(long ruleId, boolean fired, Date lastFiredTimestamp) {
        this.ruleId = ruleId;
        this.fired = fired;
        this.lastFiredTimestamp = lastFiredTimestamp;
    }

    public long getRuleId() {
        return ruleId;
    }

    public void setRuleId(long ruleId) {
        this.ruleId = ruleId;
    }

    public boolean isFired() {
        return fired;
    }

    public void setFired(boolean fired) {
        this.fired = fired;
    }

    public void setLastFiredTimestamp(Date lastFiredTimestamp) {
        this.lastFiredTimestamp = lastFiredTimestamp;
    }

    public Date getLastFiredTimestamp() {
        return lastFiredTimestamp;
    }
}
