import PropTypes from 'prop-types'
import React from 'react'

import { Radio } from '../form'

function FieldRadio({ id, text, value, onChange, disabled, options, className }) {
  return (
    <div className={`control control-inline ${className || ''}`.trim()}>
      <label htmlFor={id}>{text}</label>
      <Radio
        name={id}
        disabled={disabled}
        selected={value}
        options={options}
        onChange={onChange}
      />
    </div>
  )
}

FieldRadio.defaultProps = {
  disabled: false,
  options: [],
  className: '',
}

FieldRadio.propTypes = {
  id: PropTypes.string.isRequired,
  text: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
  className: PropTypes.string,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string,
      text: PropTypes.string,
    })
  ),
}

export default FieldRadio
