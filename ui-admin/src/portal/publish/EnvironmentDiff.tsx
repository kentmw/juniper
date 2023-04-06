import React, { useEffect, useState } from 'react'
import Api, {
  ConfigChangeRecord, ListChangeRecord,
  NotificationConfig, NotificationConfigChangeRecord,
  Portal,
  PortalEnvironment,
  PortalEnvironmentChangeRecord,
  VersionedEntityChangeRecord
} from '../../api/api'
import { useParams } from 'react-router-dom'
import LoadingSpinner from '../../util/LoadingSpinner'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faArrowRight } from '@fortawesome/free-solid-svg-icons'

type EnvironmentDiffProps = {
  portal: Portal,
  portalEnv: PortalEnvironment,
}

/**
 * loads and displays the differences between two portal environments
 * */
export default function EnvironmentDiff({ portal, portalEnv }: EnvironmentDiffProps) {
  const params = useParams()
  const sourceEnvName: string | undefined = params.sourceEnvName
  const [isLoading, setIsLoading] = useState(true)
  const [diffResult, setDiffResult] = useState<PortalEnvironmentChangeRecord | null>(null)

  useEffect(() => {
    if (!sourceEnvName) {
      alert('no source environment specified')
      return
    }
    Api.fetchEnvDiff(portal.shortcode, sourceEnvName, portalEnv.environmentName).then(result => {
      setDiffResult(result)
      setIsLoading(false)
    }).catch(e => {
      alert(e)
      setIsLoading(false)
    })
  }, [])

  return <div className="container mt-3">
    <LoadingSpinner isLoading={isLoading}/>
    {diffResult && <div>
      <div className="bg-white p-3 my-2">
        <h2 className="h5">Environment config</h2>
        <ConfigChanges configChanges={diffResult.configChanges}/>
      </div>
      <div className="bg-white p-3 my-2">
        <h2 className="h5">Site content</h2>
        <VersionChangeView record={diffResult.siteContentChange}/>
      </div>
      <div className="bg-white p-3 my-2">
        <h2 className="h5">Prereg survey
          <span className="fst-italic text-muted fs-6 ms-3">
            Note this is pre-registration for the Portal as a whole, not a
            particular study
          </span>
        </h2>
        <VersionChangeView record={diffResult.preRegSurveyChanges}/>
      </div>
      <div className="bg-white p-3 my-2">
        <h2 className="h5">Notification Configs</h2>
        <NotificationChangeListView configChangeList={diffResult.notificationConfigChanges}/>
      </div>
    </div>}
  </div>
}

/**
 * returns html for displaying the differences in versions.  this does not yet include support
 * for links to the versions
 */
const VersionChangeView = ({ record }: {record: VersionedEntityChangeRecord}) => {
  if (!record.changed) {
    return <span className="fst-italic">no changes</span>
  }
  return <div>
    {versionDisplay(record.oldStableId, record.oldVersion)}
    <FontAwesomeIcon icon={faArrowRight} className="mx-2"/>
    {versionDisplay(record.newStableId, record.newVersion)}
  </div>
}

/** renders a list of config changes, or "no changes" if empty */
const ConfigChanges = ({ configChanges }: {configChanges: ConfigChangeRecord[]}) => {
  if (!configChanges.length) {
    return <span className="fst-italic">no changes</span>
  }
  return <ul>
    {configChanges.map((configChange, index) => <li key={index}>
      <ConfigChangeView configChange={configChange}/>
    </li>)}
  </ul>
}

/** renders a config change by converting the old and new vals to strings */
const ConfigChangeView = ({ configChange }: {configChange: ConfigChangeRecord}) => {
  const oldValString = configChange.oldValue ? configChange.oldValue.toString() : ''
  const newValString = configChange.newValue ? configChange.newValue.toString() : ''
  return <div>
    {configChange.propertyName}: {oldValString}
    <FontAwesomeIcon icon={faArrowRight} className="mx-2"/>
    {newValString}
  </div>
}

/**
 * returns html for displaying a single version, and 'not present' if null
 */
const versionDisplay = (stableId: string, version: number) => {
  if (!stableId) {
    return <span className="fst-italic text-muted">not present</span>
  }
  return <span>{stableId} v{version}</span>
}

/** Summary of notification config changes -- doesn't show any detail yet */
function NotificationChangeListView({ configChangeList }:
                            {configChangeList: ListChangeRecord<NotificationConfig, NotificationConfigChangeRecord>}) {
  return <ul>
    <li>Added: {configChangeList.addedItems.length}</li>
    <li>Removed: {configChangeList.removedItems.length}</li>
    <li>Changed: {configChangeList.changedItems.length}</li>
  </ul>
}