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

package it.acsoftware.hyperiot.rule.service.actions.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.mail.api.MailSystemApi;
import it.acsoftware.hyperiot.mail.util.MailConstants;
import it.acsoftware.hyperiot.rule.api.RuleEngineSystemApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.actions.EventRuleAction;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import org.apache.cxf.transport.commons_text.StringEscapeUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.DiscriminatorValue;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component(service = RuleAction.class, immediate = true, property = {"it.acsoftware.hyperiot.rule.action.type=EVENT"})
@DiscriminatorValue("rule.action.name.sendMail")
public class SendMailAction extends EventRuleAction implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SendMailAction.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final RuleType ruleType = RuleType.EVENT;

    /*
     * Define placeholders which will be in mail body.
     * Pay attention: their name must match the ones given on frontend
     */
    final String RULE_NAME_PLACEHOLDER = "RULE_NAME";
    final String RULE_DESCRIPTION_PLACEHOLDER = "RULE_DESCRIPTION";
    final String RULE_DEFINITION_PLACEHOLDER = "RULE_RULEDEFINITION";
    final String EVENT_TRIGGER_TIME_PLACEHOLDER = "EVENT_TRIGGER_TIME";
    final String EVENT_VALUES_PLACEHOLDER = "EVENT_VALUES";
    private String mailSender;
    private String recipients;
    private String ccRecipients;
    private String subject;
    private String body;

    /**
     * MailSystemApi and RuleEngineSystemApi reference must be explicitly set by
     * calling setBundleContext
     */
    private MailSystemApi mailService;
    private RuleEngineSystemApi ruleEngineService;

    public SendMailAction() {
        super();
    }

    @Override
    public void setBundleContext(BundleContext context) {
        if (context != null) {
            logger.debug("In SendMailAction.setBundleContext ");
            super.setBundleContext(context);
            ServiceReference<MailSystemApi> mailReference = bundleContext
                    .getServiceReference(MailSystemApi.class);
            mailService = (MailSystemApi) bundleContext.getService(mailReference);
            ServiceReference<RuleEngineSystemApi> ruleEngineReference = bundleContext
                    .getServiceReference(RuleEngineSystemApi.class);
            ruleEngineService = (RuleEngineSystemApi) bundleContext.getService(ruleEngineReference);
            // Set sender of events e-mail
            mailSender = "no-reply@hyperiot.cloud";
            Object sender = HyperIoTUtil.getHyperIoTProperty(MailConstants.MAIL_EVENT_SENDER);
            if (sender != null && !sender.toString().isEmpty()) {
                mailSender = sender.toString();
            }
        }
    }

    @Override
    public String droolsDefinition() {
        return this.droolsAsJson();
    }

    @Override
    public void run() {
        logger.debug("Starting SendMail Action ....");
        Rule rule = ruleEngineService.find(this.getRuleId(), null);
        if (rule != null) {
            logger.debug("Rule Found: " + rule.getDescription());
            List<String> recipientList = Arrays.asList(recipients.split(";"));
            List<String> ccRecipientList = ccRecipients.isEmpty() ?
                    null : Arrays.asList(ccRecipients.split(";"));
            // add template variables
            HashMap<String, Object> templateVariables = new HashMap<>();
            Instant instant = Instant.ofEpochMilli(this.getFireTimestamp());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:SS Z").withZone(ZoneId.of("UTC"));
            String dateFormatted = formatter.format(instant);
            //Setting templates variables
            String ruleName = (rule.getName() != null)?rule.getName():"";
            String ruleDesc = (rule.getDescription() != null)?rule.getDescription():"";
            String ruleDefinition = (rule.getRulePrettyDefinition() != null)?rule.getRulePrettyDefinition():"";
            //creating a base html content for the payload to be used inside the mail text
            String jsonPayload = convertSpecialCharToHtml((this.getFirePayload() != null) ? this.getFirePayload() : "");
            // the whole objects
            templateVariables.put("rule", rule);
            templateVariables.put("project", rule.getProject());
            templateVariables.put(RULE_NAME_PLACEHOLDER, ruleName);
            templateVariables.put(RULE_DESCRIPTION_PLACEHOLDER, ruleDesc);
            templateVariables.put(RULE_DEFINITION_PLACEHOLDER, ruleDefinition);
            templateVariables.put(EVENT_TRIGGER_TIME_PLACEHOLDER, dateFormatted);
            templateVariables.put(EVENT_VALUES_PLACEHOLDER, jsonPayload);
            try {
                String mailBody =
                        mailService.generateTextFromStringTemplate(decodeBase64String(body), templateVariables);
                mailService.sendMail(mailSender, recipientList, ccRecipientList,
                        new ArrayList<>(), decodeBase64String(subject), mailBody, null);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private String decodeBase64String(String base64String) {
        Base64.Decoder dec = Base64.getDecoder();
        return new String(dec.decode(base64String));
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
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

    public String getCcRecipients() {
        return ccRecipients;
    }

    public void setCcRecipients(String ccRecipients) {
        this.ccRecipients = ccRecipients;
    }

    private String convertSpecialCharToHtml(String input){
        //todo import specific library with more complex logic
        return input.replaceAll("\\n","<br/>").replaceAll("\\t"," &ensp; ");
    }
}
