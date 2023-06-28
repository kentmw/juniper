import classNames from 'classnames'
import React, { useId } from 'react'

export type TextareaProps = Omit<JSX.IntrinsicElements['textarea'], 'onChange'> & {
  description?: string
  label: string
  onChange?: (value: string) => void
}

/** A textarea with label and description. */
export const Textarea = (props: TextareaProps) => {
  const { description, label, ...inputProps } = props
  const { className, disabled, id, onChange } = inputProps

  const generatedId = useId()
  const inputId = id || generatedId
  const descriptionId = `${generatedId}-help`

  return (
    <>
      <label className="form-label" htmlFor={inputId}>{label}</label>
      <textarea
        {...inputProps}
        aria-describedby={description ? descriptionId : undefined}
        aria-disabled={disabled}
        className={classNames('form-control', { disabled }, className)}
        disabled={undefined}
        id={inputId}
        onChange={
          disabled
            // Noop because providing a value without an onChange handler causes a React warning
            ? () => { /* noop */ }
            : e => { onChange?.(e.target.value) }
        }
      />
      {description && (
        <p
          className="form-text"
          id={descriptionId}
        >
          {description}
        </p>
      )}
    </>
  )
}