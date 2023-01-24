export type AdminUser = {
  username: string,
  token: string
};

export type Study = {
  name: string,
  shortcode: string,
  studyEnvironments: StudyEnvironment[]
}

export type StudyEnvironmentUpdate = {
  id: string,
  preRegSurveyId: string
}

export type StudyEnvironment = {
  id: string,
  environmentName: string,
  studyEnvironmentConfig: StudyEnvironmentConfig,
  preRegSurvey: Survey,
  preRegSurveyId: string,
  configuredSurveys: StudyEnvironmentSurvey[],
  configuredConsents: StudyEnvironmentConsent[]
}

export type VersionedForm = {
  id: string,
  name: string,
  stableId: string,
  version: number,
  createdAt: string,
  content: string
}

export type Survey = VersionedForm

export type ConsentForm = VersionedForm

export type StudyEnvironmentSurvey = {
  id: string,
  surveyId: string,
  survey: Survey,
  recur: boolean,
  recurrenceIntervalDays: number,
  surveyOrder: number,
  allowAdminEdit: boolean,
  allowParticipantStart: boolean,
  allowParticipantReedit: boolean,
  prepopulate: boolean
}

export type StudyEnvironmentConsent = {
  id: string,
  consentFormId: string,
  consentForm: ConsentForm,
  consentOrder: number,
  allowAdminEdit: boolean,
  allowParticipantStart: boolean,
  allowParticipantReedit: boolean,
  prepopulate: boolean
}

export type StudyEnvironmentConfig = {
  passwordProtected: boolean,
  password: string,
  acceptingEnrollment: boolean,
  initialized: boolean
}


export type PortalStudy = {
  study: Study
}

export type Portal = {
  name: string,
  shortcode: string,
  portalStudies: PortalStudy[]
}


let bearerToken: string | null = null
export const API_ROOT = process.env.REACT_APP_API_ROOT
const participantRootPath = process.env.REACT_APP_PARTICIPANT_APP_ROOT
const participantProtocol = process.env.REACT_APP_PARTICIPANT_APP_PROTOCOL


export default {
  getInitHeaders() {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    }
    if (bearerToken !== null) {
      headers['Authorization'] = `Bearer ${bearerToken}`
    }
    return headers
  },

  getGetInit() {
    return {
      headers: this.getInitHeaders(),
      method: 'GET'
    }
  },

  async processJsonResponse(response: Response) {
    const obj = await response.json()
    if (response.ok) {
      return obj
    }
    return Promise.reject(response)
  },

  async unauthedLogin(username: string): Promise<AdminUser> {
    const url =`${API_ROOT}/current-user/v1/unauthed-login?${  new URLSearchParams({
      username
    })}`
    const response = await fetch(url, {
      method: 'POST',
      headers: this.getInitHeaders()
    })
    return await this.processJsonResponse(response)
  },

  async tokenLogin(token: string): Promise<AdminUser> {
    const url =`${API_ROOT}/current-user/v1/token-login`
    const response = await fetch(url, {
      method: 'POST',
      headers: this.getInitHeaders(),
      body: JSON.stringify({ token })
    })
    return await this.processJsonResponse(response)
  },

  async getPortals(): Promise<Portal[]> {
    const response = await fetch(`${API_ROOT}/portals/v1`, this.getGetInit())
    return await this.processJsonResponse(response)
  },

  async getPortal(portalShortcode: string): Promise<Portal> {
    const response = await fetch(`${API_ROOT}/portals/v1/${portalShortcode}`, this.getGetInit())
    return await this.processJsonResponse(response)
  },

  async createNewSurveyVersion(portalShortcode: string, survey: Survey): Promise<Survey> {
    const url = `${API_ROOT}/portals/v1/${portalShortcode}/surveys/${survey.stableId}/newVersion`

    const response = await fetch(url, {
      method: 'POST',
      headers: this.getInitHeaders(),
      body: JSON.stringify(survey)
    })
    return await this.processJsonResponse(response)
  },

  async createNewConsentVersion(portalShortcode: string, consentForm: ConsentForm): Promise<ConsentForm> {
    const url = `${API_ROOT}/portals/v1/${portalShortcode}/consentForms/`
      + `${consentForm.stableId}/${consentForm.version}/newVersion`

    const response = await fetch(url, {
      method: 'POST',
      headers: this.getInitHeaders(),
      body: JSON.stringify(consentForm)
    })
    return await this.processJsonResponse(response)
  },

  async updateStudyEnvironment(portalShortcode: string, studyShortcode: string, envName: string,
    studyEnvUpdate: StudyEnvironmentUpdate): Promise<StudyEnvironmentUpdate> {
    const url = `${API_ROOT}/portals/v1/${portalShortcode}/studies/${studyShortcode}/env/${envName}`
    const response = await fetch(url, {
      method: 'PATCH',
      headers: this.getInitHeaders(),
      body: JSON.stringify(studyEnvUpdate)
    })
    return await this.processJsonResponse(response)
  },

  async getSurveyVersions(studyShortname: string, stableId: string) {
    const response = await fetch(`${API_ROOT}/studies/${studyShortname}/surveys/${stableId}`, this.getGetInit())
    return await this.processJsonResponse(response)
  },

  async updateConfiguredSurvey(portalShortcode: string, studyShortcode: string, environmentName: string,
    configuredSurvey: StudyEnvironmentSurvey): Promise<StudyEnvironmentSurvey> {
    const url =`${API_ROOT}/portals/v1/${portalShortcode}/studies/${studyShortcode}` +
      `/env/${environmentName}/configuredSurveys/${configuredSurvey.id}`

    const response = await fetch(url, {
      method: 'PATCH',
      headers: this.getInitHeaders(),
      body: JSON.stringify(configuredSurvey)
    })
    return await this.processJsonResponse(response)
  },

  async updateConfiguredConsent(portalShortcode: string, studyShortcode: string, environmentName: string,
    configuredConsent: StudyEnvironmentConsent): Promise<StudyEnvironmentConsent> {
    const url =`${API_ROOT}/portals/v1/${portalShortcode}/studies/${studyShortcode}` +
      `/env/${environmentName}/configuredConsents/${configuredConsent.id}`

    const response = await fetch(url, {
      method: 'PATCH',
      headers: this.getInitHeaders(),
      body: JSON.stringify(configuredConsent)
    })
    return await this.processJsonResponse(response)
  },

  getParticipantLink(portalShortcode: string, envName: string): string {
    const participantHost = `${envName}.${portalShortcode}.${participantRootPath}`
    return `${participantProtocol}://${participantHost}`
  },

  setBearerToken(token: string | null) {
    bearerToken = token
  }


}