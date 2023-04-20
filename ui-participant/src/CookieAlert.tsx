import { faCookieBite } from '@fortawesome/free-solid-svg-icons'
import React from 'react'
import { Link } from 'react-router-dom'

import { Alert } from './Alert'

type CookieAlertProps = {
  onDismiss: () => void
}

export const CookieAlert = (props: CookieAlertProps) => {
  const { onDismiss } = props
  return (
    <Alert
      className="mb-0"
      icon={faCookieBite}
      level="warning"
      role="alert"
      style={{
        position: 'fixed',
        bottom: 0
      }}
      title="Cookies"
      onDismiss={onDismiss}
    >
      <p className="mb-0">
        This site uses internet tokens called &quot;cookies&quot; to enable the
        proper functioning and security of our website, and to improve your
        experience while you use it. All of the cookies we use are strictly
        necessary cookies. This type of cookie does not collect any personally
        identifiable information about you and does not track your browsing
        habits. To learn more,{' '}
        <Link to="/privacy">read our Privacy Policy</Link>.
      </p>
    </Alert>
  )
}