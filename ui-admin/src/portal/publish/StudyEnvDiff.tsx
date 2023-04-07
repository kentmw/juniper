import React from 'react'
import { StudyEnvironmentChange } from 'api/api'
import { ConfigChangeListView, ConfigChanges, VersionChangeView } from './diffComponents'

const StudyEnvDiff = ({ studyEnvChange }: {studyEnvChange: StudyEnvironmentChange}) => {
  return <div className="px-3">
    <div className="my-1">
      <h3 className="h6"><input className="me-2" type={'checkbox'} checked={true} readOnly={true}/>
        Environment config</h3>
      <ConfigChanges configChanges={studyEnvChange.configChanges}/>
    </div>
    <div className="my-1">
      <h3 className="h6"><input className="me-2" type={'checkbox'} checked={true} readOnly={true}/>
        PreEnroll survey</h3>
      <VersionChangeView record={studyEnvChange.preEnrollSurveyChanges}/>
    </div>
    <div className="my-1">
      <h3 className="h6"><input className="me-2" type={'checkbox'} checked={true} readOnly={true}/>
        Consents</h3>
      <ConfigChangeListView configChangeList={studyEnvChange.consentChanges}/>
    </div>
    <div className="my-1">
      <h3 className="h6"><input className="me-2" type={'checkbox'} checked={true} readOnly={true}/>
        Surveys</h3>
      <ConfigChangeListView configChangeList={studyEnvChange.surveyChanges}/>
    </div>
    <div className="my-1">
      <h3 className="h6"><input className="me-2" type={'checkbox'} checked={true} readOnly={true}/>
        Notification Configs</h3>
      <ConfigChangeListView configChangeList={studyEnvChange.notificationConfigChanges}/>
    </div>
  </div>
}

export default StudyEnvDiff