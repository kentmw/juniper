package bio.terra.pearl.api.admin.controller.forms;

import bio.terra.pearl.api.admin.api.ConfiguredConsentApi;
import bio.terra.pearl.api.admin.model.ConfiguredConsentDto;
import bio.terra.pearl.api.admin.service.AuthUtilService;
import bio.terra.pearl.api.admin.service.forms.ConsentFormExtService;
import bio.terra.pearl.core.model.EnvironmentName;
import bio.terra.pearl.core.model.admin.AdminUser;
import bio.terra.pearl.core.model.consent.StudyEnvironmentConsent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ConfiguredConsentController implements ConfiguredConsentApi {
  private AuthUtilService authUtilService;
  private HttpServletRequest request;
  private ObjectMapper objectMapper;
  private ConsentFormExtService consentFormExtService;

  public ConfiguredConsentController(
      AuthUtilService authUtilService,
      HttpServletRequest request,
      ObjectMapper objectMapper,
      ConsentFormExtService consentFormExtService) {
    this.request = request;
    this.objectMapper = objectMapper;
    this.consentFormExtService = consentFormExtService;
    this.authUtilService = authUtilService;
  }

  @Override
  public ResponseEntity<ConfiguredConsentDto> patch(
      String portalShortcode,
      String studyShortcode,
      String envName,
      UUID configuredConsentId,
      ConfiguredConsentDto body) {
    AdminUser adminUser = authUtilService.requireAdminUser(request);
    EnvironmentName environmentName = EnvironmentName.valueOfCaseInsensitive(envName);
    StudyEnvironmentConsent configuredForm =
        objectMapper.convertValue(body, StudyEnvironmentConsent.class);
    var savedConfig =
        consentFormExtService.updateConfiguredConsent(
            portalShortcode, environmentName, configuredForm, adminUser);
    return ResponseEntity.ok(objectMapper.convertValue(savedConfig, ConfiguredConsentDto.class));
  }
}