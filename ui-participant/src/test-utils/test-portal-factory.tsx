import {
  HtmlPage,
  LocalSiteContent,
  Portal,
  PortalEnvironment,
  PortalEnvironmentConfig,
  SiteContent
} from '@juniper/ui-core'

/** mock portal object with one environment */
export const mockPortal = (): Portal => {
  return {
    name: 'mock portal',
    id: 'portal123',
    shortcode: 'mockportal',
    portalStudies: [],
    portalEnvironments: [mockPortalEnvironment()]
  }
}

/** mock environment with a siteContent */
export const mockPortalEnvironment = (): PortalEnvironment => {
  return {
    environmentName: 'sandbox',
    portalEnvironmentConfig: mockPortalEnvironmentConfig(),
    siteContent: mockSiteContent()
  }
}

/** mock config with everything open */
export const mockPortalEnvironmentConfig = (): PortalEnvironmentConfig => {
  return {
    acceptingRegistration: true,
    initialized: true,
    password: 'password',
    passwordProtected: false
  }
}

/** mock site content with one localization */
export const mockSiteContent = (): SiteContent => {
  return {
    id: 'fakeID1',
    defaultLanguage: 'en',
    localizedSiteContents: [mockLocalSiteContent()],
    stableId: 'mockContent',
    version: 1,
    createdAt: 0
  }
}

/** mock local content with a single empty landing page */
export const mockLocalSiteContent = (): LocalSiteContent => {
  return {
    language: 'en',
    navbarItems: [],
    landingPage: mockHtmlPage(),
    navLogoCleanFileName: 'navLogo.png',
    navLogoVersion: 1
  }
}

/** mock empty page */
export const mockHtmlPage = (): HtmlPage => {
  return {
    title: 'mock home page',
    path: '/',
    sections: []
  }
}
