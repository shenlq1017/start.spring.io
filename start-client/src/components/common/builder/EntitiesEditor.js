import '../../../styles/explore.scss'

import PropTypes from 'prop-types'
import get from 'lodash/get'
import React, { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { CSSTransition, TransitionGroup } from 'react-transition-group'
import { clearAllBodyScrollLocks, disableBodyScroll } from 'body-scroll-lock'

import {
  API_FLAGS,
  FIELD_TYPE_OPTIONS,
  createDefaultField,
  createEmptyEntity,
  normalizeField,
} from '../../utils/Entities'
import { Radio } from '../form'
import { t } from '../../../i18n/zh'

const DB_OPTIONS = [
  { key: 'postgresql', text: 'PostgreSQL' },
  { key: 'mysql', text: 'MySQL' },
  { key: 'h2', text: 'H2' },
]

const ORM_OPTIONS = [
  { key: 'mybatis-plus', text: 'MyBatis-Plus' },
  { key: 'jpa', text: 'JPA' },
]

function DeleteIconButton({ onClick, title }) {
  return (
    <button
      type='button'
      className='entity-icon-btn entity-remove'
      onClick={onClick}
      title={title}
      aria-label={title}
    >
      ×
    </button>
  )
}

DeleteIconButton.propTypes = {
  onClick: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
}

function FieldRow({ field, onChange, onRemove }) {
  return (
    <div className='entity-field-row'>
      <input
        className='input entity-input'
        value={field.name || ''}
        placeholder={t('entities.field.name')}
        onChange={e => onChange({ ...field, name: e.target.value })}
        aria-label={t('entities.field.name')}
      />
      <select
        className='entity-select'
        value={field.type || 'String'}
        onChange={e => onChange({ ...field, type: e.target.value })}
        aria-label={t('entities.field.type')}
      >
        {FIELD_TYPE_OPTIONS.map(opt => (
          <option key={opt.key} value={opt.key}>
            {opt.text}
          </option>
        ))}
      </select>
      <input
        className='input entity-input entity-field-desc'
        value={field.description || ''}
        placeholder={t('entities.field.description')}
        onChange={e => onChange({ ...field, description: e.target.value })}
        aria-label={t('entities.field.description')}
      />
      <label className='entity-check'>
        <input
          type='checkbox'
          checked={!!field.required}
          onChange={e => onChange({ ...field, required: e.target.checked })}
        />
        {t('entities.field.required')}
      </label>
      <label className='entity-check'>
        <input
          type='checkbox'
          checked={!!field.unique}
          onChange={e => onChange({ ...field, unique: e.target.checked })}
        />
        {t('entities.field.unique')}
      </label>
      <label className='entity-check' title='@Schema'>
        <input
          type='checkbox'
          checked={field.swagger !== false}
          onChange={e => onChange({ ...field, swagger: e.target.checked })}
        />
        {t('entities.field.swagger')}
      </label>
      <DeleteIconButton onClick={onRemove} title={t('entities.field.remove')} />
    </div>
  )
}

FieldRow.propTypes = {
  field: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  onRemove: PropTypes.func.isRequired,
}

function EntityForm({ entity, onChange }) {
  const update = patch => onChange({ ...entity, ...patch })
  const updateField = (fi, next) => {
    const fields = [...(entity.fields || [])]
    fields[fi] = normalizeField(next)
    update({ fields })
  }
  const removeField = fi => {
    const fields = [...(entity.fields || [])]
    fields.splice(fi, 1)
    update({ fields: fields.length ? fields : [createDefaultField()] })
  }
  const addField = () => {
    update({
      fields: [
        ...(entity.fields || []),
        {
          name: 'field',
          type: 'String',
          required: false,
          unique: false,
          description: '',
          swagger: true,
        },
      ],
    })
  }
  const toggleApi = key => {
    const apis = { ...(entity.apis || {}) }
    apis[key] = !apis[key]
    update({ apis })
  }

  return (
    <div className='entity-editor-form'>
      <div className='entity-meta-grid'>
        <div className='control control-inline'>
          <label>{t('entities.name')}</label>
          <input
            className='input'
            value={entity.name || ''}
            onChange={e => update({ name: e.target.value })}
          />
        </div>
        <div className='control control-inline'>
          <label>{t('entities.table')}</label>
          <input
            className='input'
            value={entity.table || ''}
            onChange={e => update({ table: e.target.value })}
          />
        </div>
        <div className='control control-inline entity-meta-wide'>
          <label>{t('entities.db')}</label>
          <Radio
            selected={entity.db || 'postgresql'}
            options={DB_OPTIONS}
            onChange={value => update({ db: value })}
          />
        </div>
        <div className='control control-inline entity-meta-wide'>
          <label>{t('entities.orm')}</label>
          <Radio
            selected={entity.orm || 'mybatis-plus'}
            options={ORM_OPTIONS}
            onChange={value => update({ orm: value })}
          />
        </div>
        <div className='control control-inline entity-meta-wide'>
          <label>{t('entities.description')}</label>
          <input
            className='input'
            value={entity.description || ''}
            onChange={e => update({ description: e.target.value })}
          />
        </div>
        <label className='entity-check entity-meta-wide entity-swagger-toggle'>
          <input
            type='checkbox'
            checked={entity.swagger !== false}
            onChange={e => update({ swagger: e.target.checked })}
          />
          {t('entities.swagger')}
        </label>
      </div>

      <div className='entity-section-label'>{t('entities.fields')}</div>
      {(entity.fields || []).map((field, fi) => (
        <FieldRow
          key={`f-${fi}`}
          field={normalizeField(field)}
          onChange={next => updateField(fi, next)}
          onRemove={() => removeField(fi)}
        />
      ))}
      <button type='button' className='entity-link-btn' onClick={addField}>
        + {t('entities.field.add')}
      </button>

      <div className='entity-section-label'>{t('entities.apis')}</div>
      <div className='entity-apis'>
        {API_FLAGS.map(flag => (
          <label key={flag.key} className='entity-check'>
            <input
              type='checkbox'
              checked={!!get(entity, `apis.${flag.key}`)}
              onChange={() => toggleApi(flag.key)}
            />
            {flag.label}
          </label>
        ))}
      </div>
    </div>
  )
}

EntityForm.propTypes = {
  entity: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
}

function EntitiesEditor({ open, onClose, entities, onChange, initialIndex }) {
  const list = entities || []
  const [activeIndex, setActiveIndex] = useState(initialIndex || 0)
  const scrollRef = useRef(null)

  useEffect(() => {
    if (open) {
      setActiveIndex(
        Math.min(Math.max(initialIndex || 0, 0), Math.max(list.length - 1, 0))
      )
    }
  }, [open, initialIndex, list.length])

  useEffect(() => {
    if (get(scrollRef, 'current') && open) {
      disableBodyScroll(get(scrollRef, 'current'))
    }
    return () => {
      clearAllBodyScrollLocks()
    }
  }, [open])

  useEffect(() => {
    if (!open) {
      return undefined
    }
    const onKey = e => {
      if (e.key === 'Escape') {
        e.preventDefault()
        onClose()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onClose])

  useEffect(() => {
    if (typeof document === 'undefined') {
      return undefined
    }
    document.body.classList.toggle('entities-editor-open', !!open)
    return () => {
      document.body.classList.remove('entities-editor-open')
    }
  }, [open])

  const updateAt = (index, next) => {
    const copy = [...list]
    copy[index] = next
    onChange(copy)
  }

  const removeAt = index => {
    const copy = list.filter((_, i) => i !== index)
    const next = copy.length ? copy : [createEmptyEntity()]
    onChange(next)
    setActiveIndex(Math.min(index, next.length - 1))
  }

  const addEntity = () => {
    const next = [...list, createEmptyEntity()]
    onChange(next)
    setActiveIndex(next.length - 1)
  }

  const active = list[activeIndex] || list[0]

  const overlay = (
    <TransitionGroup component={null}>
      {open && (
        <CSSTransition classNames='explorer' timeout={500}>
          <div
            className='explorer entities-editor'
            role='dialog'
            aria-modal='true'
            aria-label={t('Entities')}
          >
            <div className='colset-explorer entities-editor-layout'>
              <div className='left'>
                <div className='head'>
                  <strong>{t('Entities')}</strong>
                </div>
                <div className='explorer-content entities-editor-tabs'>
                  <ul className='entity-tab-list'>
                    {list.map((entity, index) => (
                      <li key={entity.id || `entity-${index}`}>
                        <button
                          type='button'
                          className={`entity-tab ${
                            index === activeIndex ? 'active' : ''
                          }`}
                          onClick={() => setActiveIndex(index)}
                        >
                          {entity.name || t('entities.unnamed')}
                        </button>
                        <DeleteIconButton
                          onClick={() => removeAt(index)}
                          title={t('entities.remove')}
                        />
                      </li>
                    ))}
                  </ul>
                  <button
                    type='button'
                    className='entity-add-btn'
                    onClick={addEntity}
                  >
                    {t('entities.add')}
                  </button>
                </div>
              </div>
              <div className='right'>
                <div className='head entities-editor-head'>
                  <strong>
                    {(active && active.name) || t('entities.unnamed')}
                  </strong>
                  <a
                    href='/#'
                    className='button entities-back-btn'
                    onClick={e => {
                      e.preventDefault()
                      onClose()
                    }}
                  >
                    <span className='button-content' tabIndex='-1'>
                      <span>{t('entities.back')}</span>
                      <span className='secondary desktop-only'>ESC</span>
                    </span>
                  </a>
                </div>
                <div className='explorer-content' ref={scrollRef}>
                  {active ? (
                    <EntityForm
                      entity={active}
                      onChange={next => updateAt(activeIndex, next)}
                    />
                  ) : null}
                </div>
              </div>
              <div className='explorer-actions'>
                <a
                  href='/#'
                  onClick={e => {
                    e.preventDefault()
                    onClose()
                  }}
                  className='button primary'
                >
                  <span className='button-content' tabIndex='-1'>
                    <span>{t('entities.done')}</span>
                    <span className='secondary desktop-only'>ESC</span>
                  </span>
                </a>
              </div>
            </div>
          </div>
        </CSSTransition>
      )}
    </TransitionGroup>
  )

  if (typeof document === 'undefined') {
    return null
  }
  return createPortal(overlay, document.body)
}

EntitiesEditor.propTypes = {
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  entities: PropTypes.arrayOf(PropTypes.object),
  onChange: PropTypes.func.isRequired,
  initialIndex: PropTypes.number,
}

EntitiesEditor.defaultProps = {
  entities: [],
  initialIndex: 0,
}

export default EntitiesEditor
