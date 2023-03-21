import { faPlus } from '@fortawesome/free-solid-svg-icons/faPlus'
import { faXmark } from '@fortawesome/free-solid-svg-icons/faXmark'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import _ from 'lodash'
import React from 'react'
import ReactMarkdown from 'react-markdown'

import { getSectionStyle } from 'util/styleUtils'

const idFor = (question: string): string => {
  return _.kebabCase(question)
}

const targetFor = (question: string): string => {
  return `#${idFor(question)}`
}

type FaqQuestion = {
  question: string,
  answer: string
}

type FrequentlyAskedQuestionsConfig = {
  blurb?: string, //  text below the title
  questions?: FaqQuestion[], // the questions
  title?: string, // large heading text
}

type FrequentlyAskedQuestionsProps = {
  anchorRef?: string
  config: FrequentlyAskedQuestionsConfig
}

/**
 * Template for rendering a Frequently Asked Questions block.
 */
function FrequentlyAskedQuestionsTemplate(props: FrequentlyAskedQuestionsProps) {
  const { anchorRef, config } = props
  const {
    blurb,
    questions,
    title = 'Frequently Asked Questions'
  } = config

  return <div id={anchorRef} className="row mx-0 justify-content-center" style={getSectionStyle(config)}>
    <div className="col-12 col-sm-8 col-lg-6">
      {!!title && (
        <h1 className="fs-1 fw-normal lh-sm mt-5 mb-4 text-center">{title}</h1>
      )}
      {!!blurb && (
        <div className="fs-4 mb-4 text-center">
          {blurb && <ReactMarkdown>{blurb}</ReactMarkdown>}
        </div>
      )}
      <ul className="mx-0 px-0 border-top" style={{ listStyle: 'none' }}>
        {
          _.map(questions, ({ question, answer }, i) => {
            return <li key={i} className="border-bottom">
              <button
                type="button"
                className={classNames(
                  'btn btn-link btn-lg',
                  'w-100 py-3 px-0 px-sm-2',
                  'd-flex',
                  'text-black fw-bold text-start text-decoration-none'
                )}
                data-bs-toggle="collapse" data-bs-target={targetFor(question)}
                aria-expanded="false" aria-controls={targetFor(question)}
              >
                <span className="me-2 text-center" style={{ width: 20 }}>
                  <FontAwesomeIcon className="hidden-when-expanded" icon={faPlus} />
                  <FontAwesomeIcon className="hidden-when-collapsed" icon={faXmark} />
                </span>
                {question}
              </button>
              <div className="collapse px-0 px-sm-2 ms-2" id={idFor(question)}>
                <div className="fs-5" style={{ marginLeft: 20 }}>
                  <ReactMarkdown>
                    {answer}
                  </ReactMarkdown>
                </div>
              </div>
            </li>
          })
        }
      </ul>
    </div>
  </div>
}

export default FrequentlyAskedQuestionsTemplate
