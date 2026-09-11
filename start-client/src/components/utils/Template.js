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
