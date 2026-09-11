/** Marker dependency ids driven by the Architecture radio (hidden from picker). */
export const ARCHITECTURE_MARKERS = ['ddd-six-module', 'platform-monorepo']

export const ARCHITECTURE_OPTIONS = [
  { key: 'arch-single', text: '单应用' },
  { key: 'arch-ddd-service', text: '业务微服务' },
  { key: 'arch-platform', text: '平台工程' },
]

export const DEFAULT_ARCHITECTURE = 'arch-single'

const ARCH_TO_DEP = {
  'arch-ddd-service': 'ddd-six-module',
  'arch-platform': 'platform-monorepo',
}

export function isArchitectureMarker(id) {
  return ARCHITECTURE_MARKERS.indexOf(id) > -1
}

/** Map architecture value → marker dependency id (or null for single-app). */
export function architectureToDependency(architecture) {
  return ARCH_TO_DEP[architecture] || null
}

/** Infer architecture from a dependency id list (share/URL restore). */
export function deriveArchitecture(dependencies = []) {
  if (dependencies.indexOf('platform-monorepo') > -1) {
    return 'arch-platform'
  }
  if (dependencies.indexOf('ddd-six-module') > -1) {
    return 'arch-ddd-service'
  }
  return DEFAULT_ARCHITECTURE
}

/**
 * Strip architecture markers from user deps, then ensure the marker that
 * matches `values.architecture` is present (arch-single → neither).
 */
export function applyArchitectureDependencies(values) {
  const arch = values?.architecture || DEFAULT_ARCHITECTURE
  const base = (values?.dependencies || []).filter(id => !isArchitectureMarker(id))
  const marker = architectureToDependency(arch)
  if (marker) {
    return [...base, marker]
  }
  return base
}

export function filterVisibleDependencies(list = []) {
  return list.filter(item => {
    const id = typeof item === 'string' ? item : item?.id
    return !isArchitectureMarker(id)
  })
}
