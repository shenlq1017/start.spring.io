import { DEFAULT_ARCHITECTURE } from './Architecture'

export const TEMPLATE_DDD_STANDARD = 'ddd-standard'
export const TEMPLATE_DDD_ENHANCED = 'ddd-enhanced'
export const TEMPLATE_PLATFORM_STANDARD = 'platform-standard'
export const TEMPLATE_PLATFORM_ENHANCED = 'platform-enhanced'

export const DDD_TEMPLATE_OPTIONS = [
  { key: TEMPLATE_DDD_STANDARD, text: '企业DDD标准·骨架' },
  { key: TEMPLATE_DDD_ENHANCED, text: '企业DDD增强·含CRUD切片' },
]

export const PLATFORM_TEMPLATE_OPTIONS = [
  { key: TEMPLATE_PLATFORM_STANDARD, text: '平台标准·骨架' },
  { key: TEMPLATE_PLATFORM_ENHANCED, text: '平台增强·含示例服务' },
]

/**
 * Sensible dependency kits shown as selected in the Dependencies panel
 * when a template is active. Architecture markers (ddd-six-module /
 * platform-monorepo) stay separate — applied at share/generate time.
 */
export const TEMPLATE_KIT_DEPS = {
  [TEMPLATE_DDD_STANDARD]: [
    'web',
    'validation',
    'mybatis-plus',
    'postgresql',
    'flyway',
    'knife4j',
  ],
  [TEMPLATE_DDD_ENHANCED]: [
    'web',
    'validation',
    'mybatis-plus',
    'postgresql',
    'flyway',
    'knife4j',
    'easyexcel',
  ],
  [TEMPLATE_PLATFORM_STANDARD]: [
    'web',
    'validation',
    'mybatis-plus',
    'postgresql',
    'knife4j',
  ],
  [TEMPLATE_PLATFORM_ENHANCED]: [
    'web',
    'validation',
    'mybatis-plus',
    'postgresql',
    'flyway',
    'knife4j',
  ],
}

/** Flat set of every id that belongs to any template kit. */
export const ALL_TEMPLATE_KIT_DEP_IDS = Array.from(
  new Set(
    Object.keys(TEMPLATE_KIT_DEPS).reduce(
      (acc, key) => acc.concat(TEMPLATE_KIT_DEPS[key]),
      []
    )
  )
)

export function kitDepsFor(template) {
  if (!template || !TEMPLATE_KIT_DEPS[template]) {
    return []
  }
  return [...TEMPLATE_KIT_DEPS[template]]
}

export function isTemplateKitDep(id) {
  return ALL_TEMPLATE_KIT_DEP_IDS.indexOf(id) > -1
}

/**
 * Strip previous kit deps, keep user extras, then ensure the active
 * template's kit deps are present (no duplicates). Re-apply on every
 * architecture/template change so the right panel stays in sync.
 */
export function applyTemplateKitDependencies(dependencies = [], template = '') {
  const base = (dependencies || []).filter(id => !isTemplateKitDep(id))
  const kit = kitDepsFor(template)
  const merged = [...base]
  kit.forEach(id => {
    if (merged.indexOf(id) === -1) {
      merged.push(id)
    }
  })
  return merged
}

/** Default template for an architecture + entities presence. */
export function defaultTemplateFor(architecture, entities = []) {
  if (architecture === 'arch-ddd-service') {
    return entities && entities.length > 0
      ? TEMPLATE_DDD_ENHANCED
      : TEMPLATE_DDD_STANDARD
  }
  if (architecture === 'arch-platform') {
    return TEMPLATE_PLATFORM_STANDARD
  }
  return ''
}

export function templateOptionsFor(architecture) {
  if (architecture === 'arch-ddd-service') {
    return DDD_TEMPLATE_OPTIONS
  }
  if (architecture === 'arch-platform') {
    return PLATFORM_TEMPLATE_OPTIONS
  }
  return []
}

export function isTemplateVisible(architecture) {
  return (
    architecture === 'arch-ddd-service' || architecture === 'arch-platform'
  )
}

export function normalizeTemplate(architecture, template, entities) {
  const arch = architecture || DEFAULT_ARCHITECTURE
  if (!isTemplateVisible(arch)) {
    return ''
  }
  const options = templateOptionsFor(arch).map(o => o.key)
  if (template && options.indexOf(template) > -1) {
    return template
  }
  return defaultTemplateFor(arch, entities)
}
