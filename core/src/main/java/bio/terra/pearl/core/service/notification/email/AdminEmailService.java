package bio.terra.pearl.core.service.notification.email;

import bio.terra.pearl.core.model.admin.AdminUser;
import bio.terra.pearl.core.model.notification.EmailTemplate;
import bio.terra.pearl.core.model.portal.Portal;
import bio.terra.pearl.core.service.notification.NotificationContextInfo;
import bio.terra.pearl.core.service.notification.substitutors.AdminEmailSubstitutor;
import bio.terra.pearl.core.shared.ApplicationRoutingPaths;
import com.sendgrid.Mail;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AdminEmailService {
  private static final String WELCOME_EMAIL_TEMPLATE_STABLEID = "broad_admin_welcome";
  private static final int WELCOME_EMAIL_TEMPLATE_VERSION = 1;
  private static final Logger logger = LoggerFactory.getLogger(EnrolleeEmailService.class);
  private EmailTemplateService emailTemplateService;
  private ApplicationRoutingPaths routingPaths;
  private SendgridClient sendgridClient;



  public AdminEmailService( EmailTemplateService emailTemplateService,
                           ApplicationRoutingPaths routingPaths, SendgridClient sendgridClient) {
    this.emailTemplateService = emailTemplateService;
    this.routingPaths = routingPaths;
    this.sendgridClient = sendgridClient;
  }

  @Async
  public void sendWelcomeEmail(Portal portal, AdminUser adminUser) {
    NotificationContextInfo contextInfo = loadContextInfo(WELCOME_EMAIL_TEMPLATE_STABLEID, WELCOME_EMAIL_TEMPLATE_VERSION, portal);
    sendEmail(contextInfo, adminUser);
  }

  public void sendEmail(NotificationContextInfo contextInfo, AdminUser adminUser) {
    if (!shouldSendEmail(contextInfo)) {
      return;
    }
    try {
      buildAndSendEmail(contextInfo, adminUser.getUsername());
      logger.info("Email sent: adminUsername: {}, subject: {}", adminUser.getUsername(), contextInfo.template().getSubject());
    } catch (Exception e) {
      logger.error("Email failed: adminUsername: {}, subject: {}, {} ",
          adminUser.getUsername(), contextInfo.template().getSubject(), e.getMessage());
    }
  }

  /**
   * skips processing, checks, and logging, and just sends the email. Should only be used for debugging and
   * test emails, since we want all regular emails to be logged.
   * */
  public void sendTestNotification(Portal portal, String templateStableId, int version, String adminUsername) throws Exception {
    NotificationContextInfo contextInfo = loadContextInfo(templateStableId, version, portal);
    buildAndSendEmail(contextInfo, adminUsername);
  }

  protected void buildAndSendEmail(NotificationContextInfo contextInfo, String adminUsername) throws Exception {
    StringSubstitutor substitutor = AdminEmailSubstitutor.newSubstitutor(adminUsername,
        contextInfo, routingPaths);
    String fromAddress = "info@ourhealthstudy.org"; // this should be replaced with support@juniper.terra.bio once we have it
    Mail mail = sendgridClient.buildEmail(contextInfo, adminUsername, fromAddress, substitutor);
    sendgridClient.sendEmail(mail);
  }

  public boolean shouldSendEmail(NotificationContextInfo contextInfo) {
    if (contextInfo.template() == null) {
      logger.error("no email template configured: skipping send");
      return false;
    }
    return true;
  }

  /**
   * loads the context information needed to send a notification (things not specific to a user)
   * for now, this is trivial, but later admin emails may contain other context
   */
  public NotificationContextInfo loadContextInfo(String templateStableId, int version, Portal portal) {
    EmailTemplate emailTemplate = emailTemplateService.findByStableId(templateStableId, version).get();
    return new NotificationContextInfo(
        portal,
        null,
        null,
        null,
        emailTemplate
    );
  }
}
