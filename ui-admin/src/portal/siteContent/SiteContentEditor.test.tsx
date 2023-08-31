import React from 'react'

import SiteContentEditor from './SiteContentEditor'
import { setupRouterTest } from 'test-utils/router-testing-utils'
import { render, screen, waitFor } from '@testing-library/react'
import { emptyApi, mockSiteContent } from 'test-utils/mock-site-content'
import userEvent from '@testing-library/user-event'

test('enables live-preview text editing', async () => {
  const siteContent = mockSiteContent()
  const createNewVersionFunc = jest.fn()
  const { RoutedComponent } = setupRouterTest(
    <SiteContentEditor siteContent={siteContent} previewApi={emptyApi}
      loadSiteContent={jest.fn()} createNewVersion={createNewVersionFunc} portalShortcode="foo"/>)
  render(RoutedComponent)

  expect(screen.getByText('Landing page')).toBeInTheDocument()

  const sectionInput = screen.getByRole('textbox')
  const aboutUsHeading = screen.queryAllByRole('heading')
    .find(el => el.textContent === 'about us')
  expect(aboutUsHeading).toBeInTheDocument()
  userEvent.pointer({ target: sectionInput, offset: 22, keys: '[MouseLeft]' })
  userEvent.keyboard('!!')

  await waitFor(() => {
    const aboutUsNewHeading = screen.queryAllByRole('heading')
      .find(el => el.textContent === 'about us!!')
    return expect(aboutUsNewHeading).toBeInTheDocument()
  })

  await userEvent.click(screen.getByText('Save'))
  const expectedSaveObj = { ...siteContent }
  expectedSaveObj.localizedSiteContents[0].landingPage.sections[0].sectionConfig = JSON.stringify({
    title: 'about us!!', blurb: 'we are the best'
  }, null, 2)
  expect(createNewVersionFunc).toHaveBeenCalledWith(expectedSaveObj)
})