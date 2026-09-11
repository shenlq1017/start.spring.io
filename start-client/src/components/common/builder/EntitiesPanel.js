import PropTypes from 'prop-types'
import get from 'lodash/get'
import React from 'react'

import {
  API_FLAGS,
  FIELD_TYPE_OPTIONS,
  createDefaultField,
  createEmptyEntity,
  normalizeField,
} from '../../utils/Entities'
import { t } from '../../../i18n/zh'

function FieldRow({ field, onChange, onRemove }) {
  return (
    <div className='entity-field-row'>
      <input
        className='entity-input'
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
        className='entity-input entity-field-desc'
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
      <button
        type='button'
        className='entity-link-btn'
        onClick={onRemove}
        title={t('entities.field.remove')}
      >
        ×
      </button>
    </div>
  )
}

FieldRow.propTypes = {
  field: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  onRemove: PropTypes.func.isRequired,
}

function EntityBlock({ entity, index, onChange, onRemove }) {
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
    <details className='entity-details' open={index === 0}>
      <summary className='entity-summary'>
        <span className='entity-summary-title'>
          <span className='entity-summary-caret' aria-hidden='true' />
          {entity.name || t('entities.unnamed')}
          {entity.table ? (
            <span className='entity-summary-meta'> · {entity.table}</span>
          ) : null}
          {entity.description ? (
            <span className='entity-summary-meta'>
              {' '}
              — {entity.description}
            </span>
          ) : null}
        </span>
        <button
          type='button'
          className='entity-link-btn entity-remove'
          onClick={e => {
            e.preventDefault()
            e.stopPropagation()
            onRemove()
          }}
        >
          {t('entities.remove')}
        </button>
      </summary>
      <div className='entity-body'>
        <div className='entity-meta-grid'>
          <label>
            {t('entities.name')}
            <input
              className='entity-input'
              value={entity.name || ''}
              onChange={e => update({ name: e.target.value })}
            />
          </label>
          <label>
            {t('entities.table')}
            <input
              className='entity-input'
              value={entity.table || ''}
              onChange={e => update({ table: e.target.value })}
            />
          </label>
          <label>
            {t('entities.db')}
            <select
              className='entity-select'
              value={entity.db || 'postgresql'}
              onChange={e => update({ db: e.target.value })}
            >
              <option value='postgresql'>PostgreSQL</option>
              <option value='mysql'>MySQL</option>
              <option value='h2'>H2</option>
            </select>
          </label>
          <label>
            {t('entities.orm')}
            <select
              className='entity-select'
              value={entity.orm || 'mybatis-plus'}
              onChange={e => update({ orm: e.target.value })}
            >
              <option value='mybatis-plus'>MyBatis-Plus</option>
              <option value='jpa'>JPA</option>
            </select>
          </label>
          <label className='entity-meta-wide'>
            {t('entities.description')}
            <input
              className='entity-input'
              value={entity.description || ''}
              onChange={e => update({ description: e.target.value })}
            />
          </label>
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
    </details>
  )
}

EntityBlock.propTypes = {
  entity: PropTypes.object.isRequired,
  index: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  onRemove: PropTypes.func.isRequired,
}

function EntitiesPanel({ entities, onChange }) {
  const list = entities || []

  const updateAt = (index, next) => {
    const copy = [...list]
    copy[index] = next
    onChange(copy)
  }

  const removeAt = index => {
    const copy = list.filter((_, i) => i !== index)
    onChange(copy.length ? copy : [createEmptyEntity()])
  }

  const addEntity = () => {
    onChange([...list, createEmptyEntity()])
  }

  return (
    <div className='entities-panel'>
      {list.map((entity, index) => (
        <EntityBlock
          key={entity.id || `entity-${index}`}
          entity={entity}
          index={index}
          onChange={next => updateAt(index, next)}
          onRemove={() => removeAt(index)}
        />
      ))}
      <button type='button' className='entity-add-btn' onClick={addEntity}>
        {t('entities.add')}
      </button>
    </div>
  )
}

EntitiesPanel.propTypes = {
  entities: PropTypes.arrayOf(PropTypes.object),
  onChange: PropTypes.func.isRequired,
}

EntitiesPanel.defaultProps = {
  entities: [],
}

export default EntitiesPanel
