import PropTypes from 'prop-types'
import get from 'lodash/get'
import React, { useContext } from 'react'

import List from './List'
import useWindowsUtils from '../../utils/WindowsUtils'
import { AppContext } from '../../reducer/App'
import { Button } from '../form'
import { InitializrContext } from '../../reducer/Initializr'
import { filterVisibleDependencies } from '../../utils/Architecture'
import { t } from '../../../i18n/zh'

function Dependency({ refButton }) {
  const { dispatch, list } = useContext(AppContext)
  const { values } = useContext(InitializrContext)
  const windowsUtils = useWindowsUtils()
  const visibleCount = filterVisibleDependencies(
    get(values, 'dependencies', [])
  ).length
  return (
    <div className='control'>
      <div className='dependency-header'>
        <span className='label'>{t('Dependencies')}</span>
        <Button
          id='explore-dependencies'
          onClick={event => {
            event.preventDefault()
            dispatch({
              type: 'UPDATE',
              payload: { list: !list },
            })
          }}
          hotkey={`${windowsUtils.symb} + b`}
          refButton={refButton}
        >
          {t('Add dependencies')}
          <span className='desktop-only'>...</span>
        </Button>
      </div>
      {visibleCount > 0 ? (
        <List />
      ) : (
        <div className='no-dependency'>{t('No dependency selected')}</div>
      )}
    </div>
  )
}

Dependency.propTypes = {
  refButton: PropTypes.oneOfType([
    PropTypes.func,
    PropTypes.shape({ current: PropTypes.instanceOf(Element) }),
  ]).isRequired,
}

export default Dependency
