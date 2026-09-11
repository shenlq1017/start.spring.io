/**
 * zh-CN strings for the main Initializr builder flow (P0).
 * Use t('key') for lookups; unknown keys fall back to the key itself.
 */
const zh = {
  Project: '项目',
  Language: '语言',
  'Spring Boot': 'Spring Boot',
  Architecture: '架构',
  'Project Metadata': '项目元数据',
  Group: 'Group',
  Artifact: 'Artifact',
  'Package name': '包名',
  Packaging: '打包',
  Configuration: '配置格式',
  Java: 'Java',
  Dependencies: '依赖',
  'Add dependencies': '添加依赖',
  'No dependency selected': '未选择依赖',
  Generate: '生成',
  Generating: '生成中…',
  Explore: '探索',
  Bookmark: '收藏',
  Share: '分享',
  Close: '关闭',
  Download: '下载',
  Copy: '复制',
  'View source': '查看源码',
  Preview: '预览',
  'arch-single': '单应用',
  'arch-ddd-service': '业务微服务',
  'arch-platform': '平台工程',
  'boot.unsupported': 'Spring Boot {value} 不受支持，请选择有效版本。',
  'deps.search.placeholder': 'Web、Security、JPA、Actuator、Devtools…',
  'deps.multi.help': '按住 {symb} 可多选添加',
}

export function t(key, vars) {
  let text = Object.prototype.hasOwnProperty.call(zh, key) ? zh[key] : key
  if (vars) {
    Object.keys(vars).forEach(k => {
      text = text.replace(new RegExp(`\\{${k}\\}`, 'g'), vars[k])
    })
  }
  return text
}

export default zh
