/** Default / helpers for multi-entity CRUD panel (P1). */

export const FIELD_TYPE_OPTIONS = [
  { key: 'String', text: 'String' },
  { key: 'Integer', text: 'Integer' },
  { key: 'Long', text: 'Long' },
  { key: 'Boolean', text: 'Boolean' },
  { key: 'BigDecimal', text: 'BigDecimal' },
  { key: 'LocalDate', text: 'LocalDate' },
  { key: 'LocalDateTime', text: 'LocalDateTime' },
]

export const API_FLAGS = [
  { key: 'create', label: '创建' },
  { key: 'detail', label: '详情' },
  { key: 'page', label: '分页查询' },
  { key: 'update', label: '更新' },
  { key: 'delete', label: '删除' },
  { key: 'import', label: '导入' },
  { key: 'export', label: '导出' },
]

export function createDefaultField() {
  return {
    name: 'username',
    type: 'String',
    required: true,
    unique: false,
  }
}

export function createDefaultEntity(overrides = {}) {
  return {
    id: `ent-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    name: 'User',
    table: 'sys_user',
    db: 'postgresql',
    orm: 'mybatis-plus',
    description: '用户',
    fields: [
      { name: 'username', type: 'String', required: true, unique: true },
      { name: 'email', type: 'String', required: false, unique: false },
      { name: 'nickname', type: 'String', required: false, unique: false },
    ],
    apis: {
      create: true,
      detail: true,
      page: true,
      update: true,
      delete: true,
      import: false,
      export: false,
    },
    ...overrides,
  }
}

export function createEmptyEntity() {
  return createDefaultEntity({
    id: `ent-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    name: 'NewEntity',
    table: 'new_entity',
    description: '新实体',
    fields: [createDefaultField()],
  })
}

export function defaultEntities() {
  return [createDefaultEntity({ id: 'ent-default-user' })]
}

/** Compact payload for URL (drop client-only id if needed later). */
export function serializeEntitiesForApi(entities = []) {
  return entities.map(e => ({
    name: e.name,
    table: e.table,
    db: e.db || 'postgresql',
    orm: e.orm || 'mybatis-plus',
    description: e.description || e.name,
    fields: (e.fields || []).map(f => ({
      name: f.name,
      type: f.type || 'String',
      required: !!f.required,
      unique: !!f.unique,
    })),
    apis: {
      create: !!(e.apis && e.apis.create),
      detail: !!(e.apis && e.apis.detail),
      page: !!(e.apis && e.apis.page),
      update: !!(e.apis && e.apis.update),
      delete: !!(e.apis && e.apis.delete),
      import: !!(e.apis && e.apis.import),
      export: !!(e.apis && e.apis.export),
    },
  }))
}

export function parseEntitiesFromParam(raw) {
  if (!raw) {
    return null
  }
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (!Array.isArray(parsed)) {
      return null
    }
    return parsed.map((e, idx) =>
      createDefaultEntity({
        id: `ent-loaded-${idx}`,
        name: e.name || 'Entity',
        table: e.table || 'entity',
        db: e.db || 'postgresql',
        orm: e.orm || 'mybatis-plus',
        description: e.description || e.name || '',
        fields: Array.isArray(e.fields) && e.fields.length
          ? e.fields.map(f => ({
              name: f.name || 'field',
              type: f.type || 'String',
              required: !!f.required,
              unique: !!f.unique,
            }))
          : [createDefaultField()],
        apis: {
          create: e.apis?.create !== false,
          detail: e.apis?.detail !== false,
          page: e.apis?.page !== false,
          update: e.apis?.update !== false,
          delete: e.apis?.delete !== false,
          import: !!e.apis?.import,
          export: !!e.apis?.export,
        },
      })
    )
  } catch (err) {
    return null
  }
}

export function isEntitiesPanelVisible(architecture, template) {
  if (architecture === 'arch-ddd-service') {
    return true
  }
  if (
    architecture === 'arch-platform' &&
    template === 'platform-enhanced'
  ) {
    return true
  }
  return false
}
