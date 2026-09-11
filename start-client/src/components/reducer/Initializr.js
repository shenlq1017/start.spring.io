import PropTypes from 'prop-types'
import get from 'lodash/get'
import set from 'lodash/set'
import React, {useReducer} from 'react'

import {getShareUrl, parseParams} from '../utils/ApiUtils'
import {
  DEFAULT_ARCHITECTURE,
  deriveArchitecture,
  isArchitectureMarker,
} from '../utils/Architecture'
import { defaultEntities } from '../utils/Entities'
import { normalizeTemplate } from '../utils/Template'

export const defaultInitializrContext = {
  values: {
    project: '',
    language: '',
    boot: '',
    architecture: DEFAULT_ARCHITECTURE,
    template: '',
    entities: defaultEntities(),
    meta: {
      group: '',
      artifact: '',
      name: '',
      description: '',
      packaging: 'jar',
      packageName: '',
      java: '',
      configurationFileFormat: '',
    },
    dependencies: [],
  },
  share: '',
  errors: {},
  warnings: {},
}

const localStorage =
  typeof window !== 'undefined'
    ? window.localStorage
    : {
        getItem: () => {},
        setItem: () => {},
      }

const sanitizeArchitecture = values => {
  const deps = get(values, 'dependencies', [])
  const hasMarker = deps.some(isArchitectureMarker)
  let architecture = get(values, 'architecture') || DEFAULT_ARCHITECTURE
  if (hasMarker) {
    architecture = deriveArchitecture(deps)
  }
  return {
    ...values,
    architecture,
    dependencies: deps.filter(id => !isArchitectureMarker(id)),
  }
}

const getPersistedOrDefault = json => {
  const values = {
    project:
      localStorage.getItem('project') || get(json, 'defaultValues').project,
    language:
      localStorage.getItem('language') || get(json, 'defaultValues').language,
    boot: get(json, 'defaultValues').boot,
    architecture:
      localStorage.getItem('architecture') || DEFAULT_ARCHITECTURE,
    template: '',
    entities: defaultEntities(),
    meta: {
      group: get(json, 'defaultValues.meta').group,
      artifact: get(json, 'defaultValues.meta').artifact,
      name:
        get(json, 'defaultValues.meta').name ||
        get(json, 'defaultValues.meta').artifact ||
        'demo',
      description:
        get(json, 'defaultValues.meta').description ||
        'Demo project for Spring Boot',
      packageName: get(json, 'defaultValues.meta').packageName,
      packaging: 'jar',
      java:
        localStorage.getItem('java') || get(json, 'defaultValues.meta').java,
      configurationFileFormat:
        localStorage.getItem('configurationFileFormat') || get(json, 'defaultValues.meta').configurationFileFormat,
    },
    dependencies: [],
  }
  const checks = ['project', 'language', 'meta.java', 'meta.packaging', 'meta.configurationFileFormat']
  checks.forEach(key => {
    const item = get(json, `lists.${key}`)?.find(
      it => it.key === get(values, key)
    )
    if (!item) {
      set(values, key, get(json, `defaultValues.${key}`))
    }
  })
  if (
    ['arch-single', 'arch-ddd-service', 'arch-platform'].indexOf(
      values.architecture
    ) === -1
  ) {
    values.architecture = DEFAULT_ARCHITECTURE
  }
  values.template = normalizeTemplate(
    values.architecture,
    values.template,
    values.entities
  )
  return values
}

const persist = changes => {
  if (get(changes, 'project')) {
    localStorage.setItem('project', get(changes, 'project'))
  }
  if (get(changes, 'language')) {
    localStorage.setItem('language', get(changes, 'language'))
  }
  if (get(changes, 'architecture')) {
    localStorage.setItem('architecture', get(changes, 'architecture'))
  }
  if (get(changes, 'meta.packaging')) {
    localStorage.setItem('packaging', 'jar')
  }
  if (get(changes, 'meta.java')) {
    localStorage.setItem('java', get(changes, 'meta.java'))
  }
  if (get(changes, 'meta.configurationFileFormat')) {
    localStorage.setItem('configurationFileFormat', get(changes, 'meta.configurationFileFormat'))
  }
}

export function reducer(state, action) {
  switch (action.type) {
    case 'COMPLETE': {
      const json = get(action, 'payload')
      const values = getPersistedOrDefault(json)
      values.meta.packaging = 'jar'
      return {
        values,
        share: getShareUrl(values),
        errors: {},
        warnings: {},
      }
    }
    case 'UPDATE': {
      const changes = get(action, 'payload')
      let errors = { ...state.errors }
      let meta = { ...get(state, 'values.meta') }
      if (get(changes, 'meta')) {
        meta = { ...meta, ...get(changes, 'meta') }
      }
      meta.packaging = 'jar'
      if (get(changes, 'boot')) {
        const { boot, ...err } = errors
        errors = err
      }
      if (get(changes, 'meta.group') !== undefined) {
        set(
          meta,
          'packageName',
          `${get(meta, 'group')}.${get(meta, 'artifact')}`
        )
      }
      if (get(changes, 'meta.artifact') !== undefined) {
        set(
          meta,
          'packageName',
          `${get(meta, 'group')}.${get(meta, 'artifact')}`
        )
        if (!get(meta, 'name')) {
          set(meta, 'name', get(meta, 'artifact'))
        }
      }
      persist(changes)
      const values = {
        ...get(state, 'values'),
        ...changes,
        meta,
      }
      if (get(changes, 'architecture') || get(changes, 'entities') || get(changes, 'template') !== undefined) {
        values.template = normalizeTemplate(
          values.architecture,
          values.template,
          values.entities
        )
      }
      if (get(changes, 'architecture') === 'arch-ddd-service') {
        if (!values.entities || !values.entities.length) {
          values.entities = defaultEntities()
        }
      }
      return { ...state, values, share: getShareUrl(values), errors }
    }
    case 'LOAD': {
      const params = get(action, 'payload.params')
      const lists = get(action, 'payload.lists')
      const { values: parsed, errors, warnings } = parseParams(
        state.values,
        params,
        lists
      )
      const values = sanitizeArchitecture(parsed)
      if (!values.meta) {
        values.meta = {}
      }
      values.meta.packaging = 'jar'
      if (!values.entities || !values.entities.length) {
        values.entities = defaultEntities()
      }
      values.template = normalizeTemplate(
        values.architecture,
        values.template,
        values.entities
      )
      return { ...state, values, errors, warnings, share: getShareUrl(values) }
    }
    case 'ADD_DEPENDENCY': {
      const dependency = get(action, 'payload.id')
      if (isArchitectureMarker(dependency)) {
        return state
      }
      const values = { ...get(state, 'values') }
      values.dependencies = [...get(values, 'dependencies'), dependency]
      return { ...state, values, share: getShareUrl(values) }
    }
    case 'REMOVE_DEPENDENCY': {
      const dependency = get(action, 'payload.id')
      const values = { ...get(state, 'values') }
      values.dependencies = [
        ...get(values, 'dependencies').filter(dep => dep !== dependency),
      ]
      return { ...state, values, share: getShareUrl(values) }
    }
    case 'CLEAR_WARNINGS': {
      return { ...state, warnings: {} }
    }
    default:
      return state
  }
}

export const InitializrContext = React.createContext({
  ...defaultInitializrContext,
})

export function InitializrProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, { ...defaultInitializrContext })
  return (
    <InitializrContext.Provider value={{ ...state, dispatch }}>
      {children}
    </InitializrContext.Provider>
  )
}

InitializrProvider.defaultProps = {
  children: null,
}

InitializrProvider.propTypes = {
  children: PropTypes.node,
}
