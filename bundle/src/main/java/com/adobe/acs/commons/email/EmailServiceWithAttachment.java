package com.adobe.acs.commons.email;

import javax.activation.DataSource;
import javax.mail.internet.InternetAddress;
import java.util.List;
import java.util.Map;

/**
 * Created by shamalroy on 12/7/15.
 *
 * A Generic Email service that sends an email with attachments to a given list of recipients.
 * The mailType is set to HTMLEmail by default if there are any attachments. Since we are using
 * the template to determine the mailType, the template name has to be *.html.
 *
 * Here is an example to send an email with attachments:
 *
 *      String attachment1 = "This text should be in the attache txt file."
 *      Map<String, DataSource> attachments = new HashMap<>();
 *      attachments.put("attachment1.txt", new ByteArrayDataSource(attachment1, "text/plain"));
 *      ...
 *      ...
 *      List<String> participantList = emailService.sendEmail(htmlEmailTemplatePath, emailParams, attachments, key);
 */


public interface EmailServiceWithAttachment {
    List<InternetAddress> sendEmail(String var1, Map<String, String> var2, Map<String, DataSource> attachments, InternetAddress... var3);

    List<String> sendEmail(String var1, Map<String, String> var2, Map<String, DataSource> attachments, String... var3);
}
