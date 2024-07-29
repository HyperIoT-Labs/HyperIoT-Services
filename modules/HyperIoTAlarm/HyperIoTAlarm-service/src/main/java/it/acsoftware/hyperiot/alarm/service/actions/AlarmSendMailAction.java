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

package it.acsoftware.hyperiot.alarm.service.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.rule.service.actions.events.SendMailAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlarmSendMailAction extends AlarmAction implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(AlarmSendMailAction.class.getName());

    private static final ObjectMapper mapper = new ObjectMapper();

    private String mailSender;

    private String recipients;

    private String ccRecipients;

    private String subject;

    private String body;

    @Override
    public void run() {
        SendMailAction sendMailAction = new SendMailAction();
        sendMailAction.setRecipients(this.recipients);
        sendMailAction.setBody(this.body);
        sendMailAction.setRecipients(this.recipients);
        sendMailAction.setCcRecipients(this.ccRecipients);
        sendMailAction.setSubject(this.getSubject());
        sendMailAction.setBundleContext(this.getBundleContext());
        sendMailAction.setRuleId(this.getRuleId());
        sendMailAction.setRuleName(this.getRuleName());
        sendMailAction.setActive(this.isActive());
        sendMailAction.run();
    }

    @Override
    public String droolsDefinition() {
        return this.droolsAsJson();
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public String getCcRecipients() {
        return ccRecipients;
    }

    public void setCcRecipients(String ccRecipients) {
        this.ccRecipients = ccRecipients;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }


}
