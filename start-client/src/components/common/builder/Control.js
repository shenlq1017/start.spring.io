import PropTypes from 'prop-types'
import React from 'react'

function Control({ text, children, labelFor, className }) {
  return (
    <div className={`control ${className}`.trim()}>
      <label className='label' htmlFor={labelFor}>
        {text}
      </label>
      <div className='control-element'>{children}</div>
    </div>
  )
}

Control.defaultProps = {
  children: null,
  labelFor: '',
  className: '',
}

Control.propTypes = {
  children: PropTypes.node,
  labelFor: PropTypes.string,
  text: PropTypes.string.isRequired,
  className: PropTypes.string,
}

export default Control
