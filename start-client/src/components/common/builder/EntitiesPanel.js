import PropTypes from 'prop-types'
import React, { useState } from 'react'

import EntitiesEditor from './EntitiesEditor'
import { createEmptyEntity } from '../../utils/Entities'
import { t } from '../../../i18n/zh'

function EntitiesPanel({ entities, onChange }) {
  const list = entities || []
  const [editorOpen, setEditorOpen] = useState(false)
  const [editIndex, setEditIndex] = useState(0)

  const removeAt = index => {
    const copy = list.filter((_, i) => i !== index)
    onChange(copy.length ? copy : [createEmptyEntity()])
  }

  const addEntity = () => {
    const next = [...list, createEmptyEntity()]
    onChange(next)
    setEditIndex(next.length - 1)
    setEditorOpen(true)
  }

  const openEditor = index => {
    setEditIndex(index)
    setEditorOpen(true)
  }

  return (
    <div className='entities-panel entities-panel-list'>
      <ul className='entity-summary-list'>
        {list.map((entity, index) => (
          <li key={entity.id || `entity-${index}`} className='entity-chip-row'>
            <button
              type='button'
              className='entity-chip'
              onClick={() => openEditor(index)}
              title={t('entities.edit')}
            >
              <span className='entity-chip-name'>
                {entity.name || t('entities.unnamed')}
              </span>
              {entity.table ? (
                <span className='entity-summary-meta'> · {entity.table}</span>
              ) : null}
              {entity.description ? (
                <span className='entity-summary-meta'>
                  {' '}
                  — {entity.description}
                </span>
              ) : null}
            </button>
            <span className='entity-chip-actions'>
              <button
                type='button'
                className='entity-link-btn'
                onClick={() => openEditor(index)}
              >
                {t('entities.edit')}
              </button>
              <button
                type='button'
                className='entity-link-btn entity-remove'
                onClick={() => removeAt(index)}
              >
                {t('entities.remove')}
              </button>
            </span>
          </li>
        ))}
      </ul>
      <div className='entity-list-actions'>
        <button type='button' className='entity-add-btn' onClick={addEntity}>
          {t('entities.add')}
        </button>
        {list.length > 0 ? (
          <button
            type='button'
            className='entity-add-btn entity-open-editor-btn'
            onClick={() => openEditor(0)}
          >
            {t('entities.edit')}
          </button>
        ) : null}
      </div>
      <EntitiesEditor
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        entities={list}
        onChange={onChange}
        initialIndex={editIndex}
      />
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
