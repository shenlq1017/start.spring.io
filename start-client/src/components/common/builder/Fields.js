import PropTypes from 'prop-types'
import get from 'lodash/get'
import React, {useContext, useEffect, useRef, useState} from 'react'

import Actions from './Actions'
import Control from './Control'
import EntitiesPanel from './EntitiesPanel'
import FieldError from './FieldError'
import FieldInput from './FieldInput'
import FieldRadio from './FieldRadio'
import Warnings from './Warnings'
import useWindowsUtils from '../../utils/WindowsUtils'
import {AppContext} from '../../reducer/App'
import {Button, Radio} from '../form'
import {Dependency} from '../dependency'
import {InitializrContext} from '../../reducer/Initializr'
import {ARCHITECTURE_OPTIONS, DEFAULT_ARCHITECTURE} from '../../utils/Architecture'
import {
  isEntitiesPanelVisible,
  defaultEntities,
} from '../../utils/Entities'
import {
  isTemplateVisible,
  normalizeTemplate,
  templateOptionsFor,
} from '../../utils/Template'
import {t} from '../../../i18n/zh'

function Fields({
  onSubmit,
  onExplore,
  onShare,
  onFavoriteAdd,
  refExplore,
  refSubmit,
  refDependency,
  generating,
}) {
  const wrapper = useRef(null)
  const windowsUtils = useWindowsUtils()
  const [dropdown, setDropdown] = useState(false)
  const { config, dispatch, dependencies } = useContext(AppContext)
  const {
    values,
    dispatch: dispatchInitializr,
    errors,
  } = useContext(InitializrContext)
  const update = args => {
    dispatchInitializr({ type: 'UPDATE', payload: args })
  }

  const architecture = get(values, 'architecture') || DEFAULT_ARCHITECTURE
  const template = get(values, 'template') || ''
  const entities = get(values, 'entities') || []
  const showTemplate = isTemplateVisible(architecture)
  const showEntities = isEntitiesPanelVisible(architecture, template)

  useEffect(() => {
    const clickOutside = event => {
      const children = get(wrapper, 'current')
      if (children && !children.contains(event.target)) {
        setDropdown(false)
      }
    }
    document.addEventListener('mousedown', clickOutside)
    return () => {
      document.removeEventListener('mousedown', clickOutside)
    }
  }, [])

  const onArchitectureChange = value => {
    const nextEntities =
      value === 'arch-ddd-service' ||
      (value === 'arch-platform' && template === 'platform-enhanced')
        ? entities.length
          ? entities
          : defaultEntities()
        : entities
    const nextTemplate = normalizeTemplate(value, undefined, nextEntities)
    update({
      architecture: value,
      template: nextTemplate,
      entities: nextEntities,
    })
  }

  const onTemplateChange = value => {
    let nextEntities = entities
    if (
      (architecture === 'arch-ddd-service' || value === 'platform-enhanced') &&
      (!nextEntities || !nextEntities.length)
    ) {
      nextEntities = defaultEntities()
    }
    update({ template: value, entities: nextEntities })
  }

  return (
    <>
      <div className='colset colset-main'>
        <div className='left'>
          <Warnings />
          <div className='col-sticky'>
            <div className='colset colset-project-lang'>
              <div className='left'>
                <Control text={t('Project')}>
                  <Radio
                    name='project'
                    selected={get(values, 'project')}
                    options={get(config, 'lists.project')}
                    onChange={value => {
                      update({ project: value })
                    }}
                  />
                </Control>
              </div>
              <div className='right'>
                <Control text={t('Language')}>
                  <Radio
                    name='language'
                    selected={get(values, 'language')}
                    options={get(config, 'lists.language')}
                    onChange={value => {
                      update({ language: value })
                    }}
                  />
                </Control>
              </div>
            </div>

            <Control text={t('Spring Boot')} className='control-boot'>
              <Radio
                name='boot'
                selected={get(values, 'boot')}
                error={get(errors, 'boot.value', '')}
                options={get(config, 'lists.boot')}
                onChange={value => {
                  dispatchInitializr({
                    type: 'UPDATE',
                    payload: { boot: value },
                    config: get(dependencies, 'list'),
                  })
                  dispatch({
                    type: 'UPDATE_DEPENDENCIES',
                    payload: { boot: value },
                  })
                }}
              />
              {get(errors, 'boot') && (
                <FieldError>
                  {t('boot.unsupported', { value: get(errors, 'boot.value') })}
                </FieldError>
              )}
            </Control>

            <Control text={t('Architecture')} className='control-architecture'>
              <Radio
                name='architecture'
                selected={architecture}
                options={ARCHITECTURE_OPTIONS}
                onChange={onArchitectureChange}
              />
            </Control>

            <div
              className={`fields-slot fields-slot-template${
                showTemplate ? '' : ' is-reserved'
              }`}
              aria-hidden={!showTemplate}
            >
              {showTemplate ? (
                <Control text={t('Template')} className='control-template'>
                  <Radio
                    name='template'
                    selected={
                      normalizeTemplate(architecture, template, entities)
                    }
                    options={templateOptionsFor(architecture)}
                    onChange={onTemplateChange}
                  />
                </Control>
              ) : (
                <div className='fields-slot-spacer' />
              )}
            </div>

            <Control text={t('Project Metadata')} className='control-metadata'>
              <FieldInput
                id='input-group'
                value={get(values, 'meta.group')}
                text={t('Group')}
                onChange={event => {
                  update({ meta: { group: event.target.value } })
                }}
              />
              <FieldInput
                id='input-artifact'
                value={get(values, 'meta.artifact')}
                text={t('Artifact')}
                onChange={event => {
                  update({ meta: { artifact: event.target.value } })
                }}
              />
              <FieldInput
                id='input-name'
                value={get(values, 'meta.name')}
                text={t('Name')}
                onChange={event => {
                  update({ meta: { name: event.target.value } })
                }}
              />
              <FieldInput
                id='input-description'
                value={get(values, 'meta.description')}
                text={t('Description')}
                onChange={event => {
                  update({ meta: { description: event.target.value } })
                }}
              />
              <FieldInput
                id='input-packageName'
                value={get(values, 'meta.packageName')}
                text={t('Package name')}
                onChange={event => {
                  update({ meta: { packageName: event.target.value } })
                }}
              />
              <div className='colset colset-meta-row'>
                <div className='left'>
                  <FieldRadio
                    id='input-configurationFileFormat'
                    value={get(values, 'meta.configurationFileFormat')}
                    text={t('Configuration')}
                    options={get(config, 'lists.meta.configurationFileFormat')}
                    onChange={value => {
                      update({ meta: { configurationFileFormat: value } })
                    }}
                  />
                </div>
                <div className='right'>
                  <FieldRadio
                    id='input-java'
                    value={get(values, 'meta.java')}
                    text={t('Java')}
                    options={get(config, 'lists.meta.java')}
                    onChange={value => {
                      update({ meta: { java: value } })
                    }}
                  />
                </div>
              </div>
            </Control>

            <div
              className={`fields-slot fields-slot-entities${
                showEntities ? '' : ' is-reserved'
              }`}
              aria-hidden={!showEntities}
            >
              {showEntities ? (
                <Control text={t('Entities')} className='control-entities'>
                  <EntitiesPanel
                    entities={entities}
                    onChange={next => update({ entities: next })}
                  />
                </Control>
              ) : (
                <div className='fields-slot-spacer fields-slot-spacer-entities' />
              )}
            </div>
          </div>
        </div>
        <div className='right'>
          <Dependency refButton={refDependency} />
        </div>
      </div>
      <Actions>
        {generating ? (
          <span className='placeholder-button placeholder-button-submit placeholder-button-special'>
            {t('Generating')}
          </span>
        ) : (
          <Button
            id='generate-project'
            variant='primary'
            onClick={onSubmit}
            hotkey={`${windowsUtils.symb} + ⏎`}
            refButton={refSubmit}
            disabled={generating}
          >
            {t('Generate')}
          </Button>
        )}
        <Button
          id='explore-project'
          onClick={onExplore}
          hotkey='Ctrl + Space'
          refButton={refExplore}
        >
          {t('Explore')}
        </Button>

        <span className='dropdown' ref={wrapper}>
          <Button
            className={`last-child ${dropdown ? 'clicked' : ''}`}
            id='more-button'
            onClick={() => {
              setDropdown(prev => !prev)
            }}
          >
            ...
          </Button>
          {dropdown && (
            <div className='dropdown-items'>
              <Button
                id='favorite-add'
                onClick={() => {
                  onFavoriteAdd()
                  setDropdown(false)
                }}
              >
                {t('Bookmark')}
              </Button>
              <Button
                id='share-project'
                onClick={() => {
                  onShare()
                  setDropdown(false)
                }}
              >
                {t('Share')}
              </Button>
            </div>
          )}
        </span>
      </Actions>
    </>
  )
}

Fields.propTypes = {
  generating: PropTypes.bool.isRequired,
  onSubmit: PropTypes.func.isRequired,
  onExplore: PropTypes.func.isRequired,
  onShare: PropTypes.func.isRequired,
  onFavoriteAdd: PropTypes.func.isRequired,
  refExplore: PropTypes.oneOfType([
    PropTypes.func,
    PropTypes.shape({ current: PropTypes.instanceOf(Element) }),
  ]).isRequired,
  refSubmit: PropTypes.oneOfType([
    PropTypes.func,
    PropTypes.shape({ current: PropTypes.instanceOf(Element) }),
  ]).isRequired,
  refDependency: PropTypes.oneOfType([
    PropTypes.func,
    PropTypes.shape({ current: PropTypes.instanceOf(Element) }),
  ]).isRequired,
}

export default Fields
