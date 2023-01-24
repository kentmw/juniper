import { Survey } from 'api/api'

/** simplest survey.  one page, no interactive elements */
export function generateSurvey(overrideObj?: any): Survey { // eslint-disable-line @typescript-eslint/no-explicit-any
  const surveyContent = {
    pages: [
      {
        elements: [
          { type: 'html', html: '<span>You are on page1</span>' }
        ]
      }
    ]
  }
  const survey = {
    content: JSON.stringify(surveyContent),
    stableId: '3pageSurvey',
    version: 1
  }
  return Object.assign(survey, overrideObj)
}

/** three page survey, question on first page, static text on the rest */
export function generateThreePageSurvey(overrideObj?: any): Survey { // eslint-disable-line @typescript-eslint/no-explicit-any, max-len
  const surveyContent = {
    pages: [
      {
        elements: [
          { type: 'html', html: '<span>You are on page1</span>' },
          {
            type: 'radiogroup', title: 'radio input', name: 'radio1',
            choices: [{ text: 'Green', value: 'green' }, { text: 'Blue', value: 'blue' }]
          }
        ]
      },
      {
        elements: [
          { type: 'html', html: '<span>You are on page2</span>' }
        ]
      },
      {
        elements: [
          { type: 'html', html: '<span>You are on page3</span>' }
        ]
      }
    ]
  }
  const survey = generateSurvey({ content: JSON.stringify(surveyContent) })
  return Object.assign(survey, overrideObj)
}

/** survey with question templates */
export function generateTemplatedQuestionSurvey(overrideObj?: any) { // eslint-disable-line @typescript-eslint/no-explicit-any, max-len
  const QUESTION_TEMPLATE_TEST_SURVEY = {
    questionTemplates: [
      {
        type: 'radiogroup', title: 'what is their favorite color?', name: 'colorPicker',
        choices: [{ text: 'Green', value: 'green' }, { text: 'Blue', value: 'blue' }]
      }
    ],
    pages: [
      {
        elements: [
          { type: 'html', html: '<span>Talk about your brother</span>' },
          { name: 'brotherFavoriteColor', questionTemplateName: 'colorPicker' }
        ]
      },
      {
        elements: [
          { type: 'html', html: '<span>Talk about your sister</span>' },
          { name: 'sisterFavoriteColor', questionTemplateName: 'colorPicker' }
        ]
      }
    ]
  }
  const survey = generateSurvey({
    content: JSON.stringify(QUESTION_TEMPLATE_TEST_SURVEY)
  })
  return Object.assign(survey, overrideObj)
}