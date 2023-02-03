import React, { useEffect, useContext } from 'react'
import { Portal, Study, StudyEnvironment } from 'api/api'
import { StudyParams } from 'study/StudyRouter'

import { Link, Route, Routes, useParams } from 'react-router-dom'
import { NavBreadcrumb } from '../navbar/AdminNavbar'
import { NavbarContext } from '../navbar/NavbarProvider'
import StudyEnvironmentSidebar from './StudyEnvironmentSidebar'
import { PortalContext } from '../portal/PortalProvider'
import SurveyView from './surveys/SurveyView'
import ConsentView from './surveys/ConsentView'
import PreEnrollView from './surveys/PreEnrollView'
import StudyContent from './StudyContent'
import ParticipantsRouter from './participants/ParticipantsRouter'


export type StudyEnvContextT = { study: Study, currentEnv: StudyEnvironment, currentEnvPath: string, portal: Portal }

/** Base page for configuring the content and integrations for a study environment */
function StudyEnvironmentRouter({ study }: {study: Study}) {
  const params = useParams<StudyParams>()
  const envName: string | undefined = params.studyEnv
  const navContext = useContext(NavbarContext)
  const portal = useContext(PortalContext).portal as Portal

  if (!envName) {
    return <span>no environment selected</span>
  }
  const currentEnv = study.studyEnvironments.find(env => env.environmentName === envName.toLowerCase())
  if (!currentEnv) {
    return <span>invalid environment {envName}</span>
  }

  const currentEnvPath = `/${portal.shortcode}/studies/${study.shortcode}/env/${currentEnv.environmentName}`
  useEffect(() => {
    navContext.setSidebarContent(<StudyEnvironmentSidebar study={study}
      portalShortcode={portal.shortcode}
      currentEnv={currentEnv}
      currentEnvPath={currentEnvPath}
      setShow={navContext.setShowSidebar}/>)
  }, [])

  const studyEnvContext: StudyEnvContextT = { study, currentEnv, currentEnvPath, portal }
  return <div className="StudyView">
    <NavBreadcrumb>
      <Link className="text-white" to={currentEnvPath}>
        {envName}</Link>
    </NavBreadcrumb>
    <Routes>
      <Route path="surveys">
        <Route path=":surveyStableId">
          <Route index element={<SurveyView studyEnvContext={studyEnvContext}/>}/>
        </Route>
        <Route path="*" element={<div>Unknown survey page</div>}/>
      </Route>
      <Route path="consentForms">
        <Route path=":consentStableId">
          <Route index element={<ConsentView studyEnvContext={studyEnvContext}/>}/>
        </Route>
        <Route path="*" element={<div>Unknown consent page</div>}/>
      </Route>
      <Route path="preEnroll">
        <Route path=":surveyStableId" element={<PreEnrollView studyEnvContext={studyEnvContext}/>}/>
        <Route path="*" element={<div>Unknown prereg page</div>}/>
      </Route>
      <Route path="participants/*" element={<ParticipantsRouter studyEnvContext={studyEnvContext}/>}/>
      <Route index element={<StudyContent studyEnvContext={studyEnvContext}/>}/>
      <Route path="*" element={<div>Unknown study environment page</div>}/>
    </Routes>
  </div>
}

export default StudyEnvironmentRouter