import {
  TEMPLATE_DDD_ENHANCED,
  TEMPLATE_DDD_STANDARD,
  TEMPLATE_PLATFORM_ENHANCED,
  TEMPLATE_PLATFORM_STANDARD,
  ALL_TEMPLATE_KIT_DEP_IDS,
  applyTemplateKitDependencies,
  kitDepsFor,
  isTemplateKitDep,
  normalizeTemplate,
} from '../Template'

describe('Template kits', () => {
  it('defines kits for all template ids', () => {
    expect(kitDepsFor(TEMPLATE_DDD_STANDARD).length).toBeGreaterThan(0)
    expect(kitDepsFor(TEMPLATE_DDD_ENHANCED).length).toBeGreaterThan(0)
    expect(kitDepsFor(TEMPLATE_PLATFORM_STANDARD).length).toBeGreaterThan(0)
    expect(kitDepsFor(TEMPLATE_PLATFORM_ENHANCED).length).toBeGreaterThan(0)
    expect(kitDepsFor('')).toEqual([])
    expect(kitDepsFor(undefined)).toEqual([])
  })

  it('includes expected stack deps for ddd-enhanced', () => {
    const kit = kitDepsFor(TEMPLATE_DDD_ENHANCED)
    ;['web', 'validation', 'mybatis-plus', 'postgresql', 'flyway', 'knife4j'].forEach(
      id => {
        expect(kit).toContain(id)
      }
    )
  })

  it('applyTemplateKitDependencies merges without duplicates and strips old kit', () => {
    const withUser = applyTemplateKitDependencies(
      ['web', 'r2dbc', 'lombok', 'flyway'],
      TEMPLATE_DDD_ENHANCED
    )
    expect(withUser).toContain('r2dbc')
    expect(withUser).toContain('lombok')
    expect(withUser).toContain('web')
    expect(withUser).toContain('mybatis-plus')
    expect(withUser.filter(id => id === 'web').length).toBe(1)

    const cleared = applyTemplateKitDependencies(withUser, '')
    expect(cleared).toContain('r2dbc')
    expect(cleared).toContain('lombok')
    expect(cleared).not.toContain('mybatis-plus')
    expect(cleared).not.toContain('web')
  })

  it('marks kit ids', () => {
    expect(isTemplateKitDep('mybatis-plus')).toBe(true)
    expect(isTemplateKitDep('r2dbc')).toBe(false)
    expect(ALL_TEMPLATE_KIT_DEP_IDS.length).toBeGreaterThan(3)
  })

  it('normalizeTemplate still works for ddd', () => {
    expect(normalizeTemplate('arch-ddd-service', '', [{ name: 'User' }])).toBe(
      TEMPLATE_DDD_ENHANCED
    )
    expect(normalizeTemplate('arch-single', TEMPLATE_DDD_STANDARD, [])).toBe('')
  })
})
