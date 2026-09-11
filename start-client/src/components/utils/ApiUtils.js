import get from 'lodash/get'
import querystring from 'querystring'
import set from 'lodash/set'

import Extend from '../../Extend.json'
import {
  translateDependency,
  translateDependencyGroup,
} from '../../i18n/deps-zh'
import {applyArchitectureDependencies} from './Architecture'
import {
  isEntitiesPanelVisible,
  parseEntitiesFromParam,
  serializeEntitiesForApi,
} from './Entities'
import { isTemplateVisible, normalizeTemplate } from './Template'
import {isInRange, parseReleases, parseVersion} from './Version'

const PROPERTIES_MAPPING_URL = {
  type: 'project',
  language: 'language',
  platformVersion: 'boot',
  packaging: 'meta.packaging',
  configurationFileFormat: 'meta.configurationFileFormat',
  jvmVersion: 'meta.java',
  groupId: 'meta.group',
  artifactId: 'meta.artifact',
  name: 'meta.name',
  description: 'meta.description',
  packageName: 'meta.packageName',
  dependencies: 'dependencies',
}


/**
 * Shared template + entities query extras for Explore AND Generate.
 * Must stay identical so preview zip matches downloaded zip.
 */
export const buildTemplateEntitiesQuery = values => {
  let extra = ''
  const architecture = get(values, 'architecture')
  const entities = get(values, 'entities') || []
  const template = normalizeTemplate(
    architecture,
    get(values, 'template'),
    entities
  )
  if (isTemplateVisible(architecture)) {
    const tpl =
      template ||
      normalizeTemplate(architecture, undefined, entities) ||
      ''
    if (tpl) {
      extra += `&template=${encodeURIComponent(tpl)}`
    }
  }
  if (isEntitiesPanelVisible(architecture, template) && entities.length > 0) {
    extra += `&entities=${encodeURIComponent(
      JSON.stringify(serializeEntitiesForApi(entities))
    )}`
  }
  return extra
}

/**
 * Canonical starter.zip query string (same for Explore + Generate).
 * @param values Initializr values
 * @param dependencyList full dependency catalog for version-range filtering
 */
export const buildStarterQuery = (values, dependencyList = []) => {
  const artifact = get(values, 'meta.artifact')
  const params = querystring.stringify({
    type: get(values, 'project'),
    language: get(values, 'language'),
    bootVersion: get(values, 'boot'),
    baseDir: artifact,
    groupId: get(values, 'meta.group'),
    artifactId: artifact,
    name: get(values, 'meta.name') || artifact || 'demo',
    description:
      get(values, 'meta.description') || 'Demo project for Spring Boot',
    packageName: get(values, 'meta.packageName'),
    packaging: 'jar',
    javaVersion: get(values, 'meta.java'),
    configurationFileFormat: get(values, 'meta.configurationFileFormat'),
  })
  let paramsDependencies = applyArchitectureDependencies(values)
    .map(dependency => {
      const dep = (dependencyList || []).find(it => it.id === dependency)
      return isValidDependency(get(values, 'boot'), dep) ? dependency : null
    })
    .filter(dep => !!dep)
    .join(',')
  if (paramsDependencies) {
    paramsDependencies = `&dependencies=${paramsDependencies}`
  }
  return `${params}${paramsDependencies}${buildTemplateEntitiesQuery(values)}`
}

export const getInfo = function getInfo(url) {
  return new Promise((resolve, reject) => {
    fetch(`${url}`, {
      method: 'GET',
      headers: {
        Accept: 'application/vnd.initializr.v2.3+json',
      },
    })
      .then(
        response => response.json(),
        () => {
          reject()
          return null
        }
      )
      .then(data => {
        if (data) {
          resolve(data)
        }
      })
  })
}

export const getShareUrl = values => {
  const props = {}
  Object.keys(PROPERTIES_MAPPING_URL).forEach(key => {
    const key2 = get(PROPERTIES_MAPPING_URL, key)
    const value = get(values, key2)
    if (key !== 'dependencies') {
      set(props, key, value)
    }
  })
  set(props, 'packaging', 'jar')
  let params = `${querystring.stringify(props)}`
  const deps = applyArchitectureDependencies(values)
  if (deps.length > 0) {
    params = `${params}&dependencies=${deps.join(',')}`
  } else {
    params = `${params}&dependencies=`
  }
  // Same template/entities encoding as getProject / Explore
  const extras = buildTemplateEntitiesQuery(values)
  if (extras.startsWith('&')) {
    params = `${params}${extras}`
  }
  return params
}

export const isValidParams = params => {
  return (
    Object.keys(params)
      .map(entry => {
        return !!get(PROPERTIES_MAPPING_URL, entry, null)
      })
      .filter(item => !!item).length > 0
  )
}

export const parseParams = (values, queryParams, lists) => {
  const errors = {}
  const warnings = {}
  if (isValidParams(queryParams)) {
    Object.keys(queryParams).forEach(entry => {
      const key = get(PROPERTIES_MAPPING_URL, entry)
      if (key) {
        const value = get(queryParams, entry, '').toLowerCase()
        switch (key) {
          case 'project':
          case 'language':
          case 'meta.packaging':
          case 'meta.configurationFileFormat':
          case 'meta.java': {
            const list = get(lists, key, [])
            const res = list.find(a => a.key.toLowerCase() === value)
            if (res) {
              set(values, key, res.key)
            } else {
              const currentValue = list.find(
                a => a.key.toLowerCase() === get(values, key)
              )
              set(warnings, key, {
                value: get(queryParams, entry, ''),
                select: currentValue.text,
              })
            }
            break
          }
          case 'boot': {
            const list = get(lists, key, [])
            const res = list.find(a => a.key.toLowerCase() === value)
            let error = false
            if (res) {
              set(values, key, res.key)
            } else {
              error = true
              let versionMajor = value
              if (versionMajor.indexOf('.x') === -1) {
                versionMajor = get(parseVersion(versionMajor), 'major', '')
              }
              if (versionMajor.indexOf('.x') > -1) {
                const releases = parseReleases(list).filter(
                  release =>
                    release.major.toLowerCase() === versionMajor.toLowerCase()
                )
                if (releases.length > 0) {
                  const release = releases.reduce((p, c) => {
                    if (p.qualify > c.qualify) {
                      return p
                    }
                    if (p.qualify === c.qualify) {
                      if (p.minor > c.minor) {
                        return p
                      }
                    }
                    return c
                  }, releases[0])

                  if (release) {
                    error = false
                    set(values, key, release.version)
                    const currentValue = list.find(
                      a => a.key.toLowerCase() === release.version.toLowerCase()
                    )
                    set(warnings, key, {
                      value: get(queryParams, entry, ''),
                      select: currentValue.text,
                    })
                  }
                }
              }
            }
            if (error) {
              set(errors, 'boot', {
                value: get(queryParams, entry, ''),
              })
            }
            break
          }
          case 'dependencies': {
            const depsWarning = []
            const newVal = value
              .split(',')
              .filter(item => !!item)
              .map(item => {
                const dep = get(lists, 'dependencies').find(
                  d => d.id === item.trim()
                )
                if (dep) {
                  return dep.id
                }
                depsWarning.push(item)
                return null
              })
              .filter(item => !!item)

            if (depsWarning.length > 0) {
              set(warnings, key, {
                value: depsWarning.join(', '),
              })
            }
            set(values, key, newVal)
            break
          }
          default:
            set(values, key, get(queryParams, entry, ''))
        }
      }
    })
    if (queryParams.template) {
      set(values, 'template', queryParams.template)
    }
    if (queryParams.entities) {
      const parsedEntities = parseEntitiesFromParam(queryParams.entities)
      if (parsedEntities) {
        set(values, 'entities', parsedEntities)
      }
    }
  }
  return {
    values,
    errors,
    warnings,
  }
}

export const getLists = json => {
  const deps = []
  get(json, 'dependencies.values', []).forEach(group => {
    group.values.forEach(item => {
      const extend = Extend.find(it => it.id === get(item, 'id', ''))
      const id = `${get(item, 'id', '')}`
      const val = {
        id,
        // Keep official English metadata name; only description is localized.
        name: `${get(item, 'name', '')}`,
        group: translateDependencyGroup(`${group.name}`),
        description: translateDependency(
          id,
          'description',
          `${get(item, 'description', '')}`
        ),
        versionRange: `${get(item, 'versionRange', '')}`,
        versionRequirement: `${get(item, 'versionRange', '')}`,
        weight: get(extend, 'weight', 50),
      }
      deps.push(val)
    })
  })
  return {
    project: get(json, 'type.values', [])
      .filter(type => type.action === '/starter.zip')
      .map(type => ({
        key: `${type.id}`,
        text: `${type.name}`,
      })),
    language: get(json, 'language.values', []).map(language => ({
      key: `${language.id}`,
      text: `${language.name}`,
    })),
    boot: get(json, 'bootVersion.values', []).map(boot => ({
      key: `${boot.id}`,
      text: `${boot.name}`,
    })),
    meta: {
      java: get(json, 'javaVersion.values', []).map(java => ({
        key: `${java.id}`,
        text: `${java.name}`,
      })),
      packaging: get(json, 'packaging.values', []).map(packaging => ({
        key: `${packaging.id}`,
        text: `${packaging.name}`,
      })),
      configurationFileFormat: get(json, 'configurationFileFormat.values', []).map(configurationFileFormat => ({
        key: `${configurationFileFormat.id}`,
        text: `${configurationFileFormat.name}`,
      })),
    },
    dependencies: deps,
  }
}

export const getDefaultValues = json => {
  const artifact = get(json, 'artifactId.default')
  return {
    project: get(json, 'type.default'),
    language: get(json, 'language.default'),
    boot: get(json, 'bootVersion.default'),
    meta: {
      group: get(json, 'groupId.default'),
      artifact,
      name: get(json, 'name.default') || artifact || 'demo',
      description:
        get(json, 'description.default') || 'Demo project for Spring Boot',
      packaging: get(json, 'packaging.default'),
      packageName: get(json, 'packageName.default'),
      java: get(json, 'javaVersion.default'),
      configurationFileFormat: get(json, 'configurationFileFormat.default'),
    },
    dependencies: [],
  }
}

export const getConfig = json => {
  return {
    lists: getLists(json),
    defaultValues: getDefaultValues(json),
  }
}

export const isValidDependency = function isValidDependency(boot, dependency) {
  if (!dependency) {
    return false
  }
  return get(dependency, 'versionRange')
    ? isInRange(boot, get(dependency, 'versionRange'))
    : true
}

export const getProject = function getProject(url, values, config) {
  return new Promise((resolve, reject) => {
    const query = buildStarterQuery(values, config)
    fetch(`${url}?${query}`, {
      method: 'GET',
    }).then(
      response => {
        if (response.status === 200) {
          resolve(response.blob())
          return
        }
        try {
          response.json().then(res => {
            reject(res?.message || '')
          })
        } catch (e) {
          reject()
        }
      },
      () => {
        reject()
      }
    )
  })
}
