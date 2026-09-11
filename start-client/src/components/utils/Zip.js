import get from 'lodash/get'

const FILE_EXTENSION = {
  js: 'javascript',
  md: 'markdown',
  kt: 'kotlin',
  kts: 'kotlin',
  gradle: 'groovy',
  gitignore: 'git',
  gitattributes: 'git',
  java: 'java',
  xml: 'xml',
  properties: 'properties',
  groovy: 'groovy',
  yaml: 'yaml',
  yml: 'yaml',
}

export const getLanguage = file => {
  if (!file.includes(`.`)) {
    return null
  }
  const extension = file.split(`.`).pop()
  return get(FILE_EXTENSION, extension, null)
}

const sortChildren = children =>
  children.sort((a, b) => {
    // Directories first, then files; localeCompare within each group
    if (a.type !== b.type) {
      return a.type === 'folder' ? -1 : 1
    }
    return a.filename.localeCompare(b.filename)
  })

/**
 * Direct children only: one path segment (files) or one segment + trailing slash (dirs).
 * Ignores the folder's own empty relativePath entry when present.
 */
const isDirectChild = (relativePath, file) => {
  if (!relativePath) {
    return false
  }
  const segments = relativePath.split('/').filter(Boolean)
  if (file.dir) {
    return segments.length === 1
  }
  return segments.length === 1
}

export const createTree = (files, path, fileName, zip) => {
  return new Promise(resolve => {
    const recursive = (pfiles, ppath, pfileName, pzip, pdepth) => {
      // Synthesize folder node when zip omitted explicit directory entries
      if (!pfiles[ppath] && ppath.endsWith('/')) {
        pfiles[ppath] = { dir: true, async: () => Promise.resolve('') }
      }
      const type = pfiles[ppath].dir ? 'folder' : 'file'
      const displayName = type === 'folder' ? pfileName.replace(/\/$/, '') : pfileName
      const item = {
        type,
        filename: displayName,
        path: `/${ppath}`,
        hidden: pdepth === 1 && type === 'folder' ? true : null,
      }
      if (type === 'folder') {
        const children = []
        const seen = new Set()
        pzip.folder(ppath).forEach((relativePath, file) => {
          if (!isDirectChild(relativePath, file)) {
            return
          }
          const childPath = ppath + relativePath
          if (seen.has(childPath)) {
            return
          }
          seen.add(childPath)
          children.push(
            recursive(
              pfiles,
              childPath,
              relativePath,
              pzip,
              pdepth + 1
            )
          )
        })
        item.children = sortChildren(children)
      } else {
        item.language = getLanguage(item.filename)
        if (item.language) {
          pfiles[ppath].async('string').then(content => {
            item.content = content
          })
        }
      }
      return item
    }
    const tree = recursive(files, path, fileName, zip, 0)
    const selected = tree.children.find(
      item =>
        ['pom.xml', 'build.gradle', 'build.gradle.kts'].indexOf(item.filename) >
        -1
    )
    if (selected) {
      files[selected.path.substring(1)].async('string').then(content => {
        selected.content = content
        resolve({ tree, selected })
      })
    } else {
      resolve({ tree, selected: null })
    }
  })
}

/**
 * Find the single project root folder inside a zip.
 * Prefers an explicit top-level directory entry; falls back to a unique
 * first path segment (handles zips that omit directory entries). Skips META-INF.
 */
export const findRoot = zip => {
  const keys = Object.keys(zip.files).filter(
    name => name && !name.startsWith('META-INF')
  )
  const topDirs = keys.filter(filename => {
    const pathArray = filename.split('/')
    return zip.files[filename].dir && pathArray.length === 2
  })
  if (topDirs.length === 1) {
    return topDirs[0].substring(0, topDirs[0].length - 1)
  }
  const tops = new Set()
  keys.forEach(name => {
    const seg = name.split('/')[0]
    if (seg) {
      tops.add(seg)
    }
  })
  if (tops.size === 1) {
    return [...tops][0]
  }
  if (topDirs.length > 0) {
    // Multiple module folders without wrapper — use first explicit dir (legacy)
    return topDirs[0].substring(0, topDirs[0].length - 1)
  }
  if (tops.size > 0) {
    return [...tops][0]
  }
  throw new Error('Could not find project root in ZIP')
}
