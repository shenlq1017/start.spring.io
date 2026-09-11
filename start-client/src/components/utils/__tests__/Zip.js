import JSZip from 'jszip'
import fs from 'fs'
import get from 'lodash/get'
import path from 'path'

import { createTree, findRoot, getLanguage } from '../Zip'

/**
 * Function getLanguage
 */
describe('getLanguage', () => {
  it('should return the extention', () => {
    expect(getLanguage('index.md')).toBe('markdown')
    expect(getLanguage('index.spec.md')).toBe('markdown')
    expect(getLanguage('index.js')).toBe('javascript')
    expect(getLanguage('index.kt')).toBe('kotlin')
    expect(getLanguage('index.kts')).toBe('kotlin')
    expect(getLanguage('index.gradle')).toBe('groovy')
    expect(getLanguage('index.gitignore')).toBe('git')
    expect(getLanguage('index.gitattributes')).toBe('git')
    expect(getLanguage('index.java')).toBe('java')
    expect(getLanguage('index.xml')).toBe('xml')
    expect(getLanguage('index.properties')).toBe('properties')
    expect(getLanguage('index.groovy')).toBe('groovy')
    expect(getLanguage('index.yml')).toBe('yaml')
    expect(getLanguage('index.yaml')).toBe('yaml')
  })
  it('should not return the extention', () => {
    expect(getLanguage('index.php')).toBe(null)
    expect(getLanguage('index.html')).toBe(null)
    expect(getLanguage('index.css')).toBe(null)
    expect(getLanguage('index.scss')).toBe(null)
    expect(getLanguage('mvnw')).toBe(null)
    expect(getLanguage('command.cmd')).toBe(null)
  })
})

/**
 * Function createTree
 */
describe('createTree', () => {
  // eslint-disable-next-line
  it('should create a tree', async () => {
    const zipJs = new JSZip()
    await fs.readFile(
      path.resolve('./dev/starter.mock.zip'),
      async (err, blob) => {
        const { files } = await zipJs.loadAsync(blob).catch(() => {
          throw Error(`Could not load the ZIP project.`)
        })
        const pathZ = `${findRoot({ files })}/`
        const result = await createTree(files, pathZ, pathZ, zipJs).catch(
          () => {
            throw Error(`Could not read the ZIP project.`)
          }
        )
        expect(get(result, 'tree', null) !== null).toBe(true)
        expect(get(result, 'tree.type', null)).toBe('folder')
        expect(get(result, 'tree.filename', null)).toBe('demo')
        expect(get(result, 'tree.path', null)).toBe('/demo/')
        expect(get(result, 'tree.hidden', null)).toBe(null)
        expect(get(result, 'tree.children').length).toBe(7)
        expect(get(result, 'selected.type')).toBe('file')
        expect(get(result, 'selected.filename')).toBe('pom.xml')
        expect(get(result, 'selected.path')).toBe('/demo/pom.xml')
        expect(get(result, 'selected.language')).toBe('xml')
      }
    )
  })

  it('keeps DDD hierarchy: dirs first, nested service not flattened', async () => {
    const zip = new JSZip()
    const root = 'demo-service'
    const files = [
      `${root}/pom.xml`,
      `${root}/demo-service-application/pom.xml`,
      `${root}/demo-service-application/src/main/java/com/example/demo/application/advice/GlobalExceptionHandler.java`,
      `${root}/demo-service-application/src/main/java/com/example/demo/application/assembler/UserAssembler.java`,
      `${root}/demo-service-application/src/main/java/com/example/demo/application/config/package-info.java`,
      `${root}/demo-service-application/src/main/java/com/example/demo/application/controller/UserController.java`,
      `${root}/demo-service-application/src/main/java/com/example/demo/application/service/UserApplicationService.java`,
      `${root}/demo-service-application/src/main/java/com/example/demo/application/service/UserQueryService.java`,
      `${root}/demo-service-application/src/test/java/com/example/demo/application/service/UserApplicationServiceTest.java`,
      `${root}/demo-service-domain/src/main/java/com/example/demo/domain/service/package-info.java`,
      `${root}/demo-service-domain/pom.xml`,
    ]
    files.forEach(f => zip.file(f, `// ${f}\n`))
    // Explicit dirs so Explore depth mirrors Initializr output
    ;[
      `${root}/`,
      `${root}/demo-service-application/`,
      `${root}/demo-service-application/src/`,
      `${root}/demo-service-application/src/main/`,
      `${root}/demo-service-application/src/main/java/`,
      `${root}/demo-service-application/src/main/java/com/`,
      `${root}/demo-service-application/src/main/java/com/example/`,
      `${root}/demo-service-application/src/main/java/com/example/demo/`,
      `${root}/demo-service-application/src/main/java/com/example/demo/application/`,
      `${root}/demo-service-application/src/main/java/com/example/demo/application/advice/`,
      `${root}/demo-service-application/src/main/java/com/example/demo/application/assembler/`,
      `${root}/demo-service-application/src/main/java/com/example/demo/application/config/`,
      `${root}/demo-service-application/src/main/java/com/example/demo/application/controller/`,
      `${root}/demo-service-application/src/main/java/com/example/demo/application/service/`,
      `${root}/demo-service-application/src/test/`,
      `${root}/demo-service-application/src/test/java/`,
      `${root}/demo-service-application/src/test/java/com/`,
      `${root}/demo-service-application/src/test/java/com/example/`,
      `${root}/demo-service-application/src/test/java/com/example/demo/`,
      `${root}/demo-service-application/src/test/java/com/example/demo/application/`,
      `${root}/demo-service-application/src/test/java/com/example/demo/application/service/`,
      `${root}/demo-service-domain/`,
      `${root}/demo-service-domain/src/`,
      `${root}/demo-service-domain/src/main/`,
      `${root}/demo-service-domain/src/main/java/`,
      `${root}/demo-service-domain/src/main/java/com/`,
      `${root}/demo-service-domain/src/main/java/com/example/`,
      `${root}/demo-service-domain/src/main/java/com/example/demo/`,
      `${root}/demo-service-domain/src/main/java/com/example/demo/domain/`,
      `${root}/demo-service-domain/src/main/java/com/example/demo/domain/service/`,
    ].forEach(d => zip.folder(d))

    const pathZ = `${findRoot(zip)}/`
    expect(pathZ).toBe('demo-service/')
    const { tree } = await createTree(zip.files, pathZ, pathZ, zip)

    const names = tree.children.map(c => c.filename)
    expect(names[0]).toBe('demo-service-application')
    expect(names[1]).toBe('demo-service-domain')
    expect(names[names.length - 1]).toBe('pom.xml')

    const appMod = tree.children.find(c => c.filename === 'demo-service-application')
    const src = appMod.children.find(c => c.filename === 'src')
    expect(src.children.map(c => c.filename)).toEqual(['main', 'test'])

    let node = appMod
    for (const seg of [
      'src',
      'main',
      'java',
      'com',
      'example',
      'demo',
      'application',
    ]) {
      node = node.children.find(c => c.filename === seg)
      expect(node).toBeTruthy()
    }
    // children under application: dirs first, stable alpha
    expect(node.children.map(c => c.filename)).toEqual([
      'advice',
      'assembler',
      'config',
      'controller',
      'service',
    ])
    const appService = node.children.find(c => c.filename === 'service')
    expect(appService.path).toBe(
      '/demo-service/demo-service-application/src/main/java/com/example/demo/application/service/'
    )
    expect(appService.children.map(c => c.filename).sort()).toEqual([
      'UserApplicationService.java',
      'UserQueryService.java',
    ].sort())
    // no impl/ folder when using concrete services
    expect(appService.children.find(c => c.filename === 'impl')).toBeFalsy()

    // second "service" is domain/service — distinct path, not a sibling root
    const domainMod = tree.children.find(c => c.filename === 'demo-service-domain')
    let dnode = domainMod
    for (const seg of [
      'src',
      'main',
      'java',
      'com',
      'example',
      'demo',
      'domain',
      'service',
    ]) {
      dnode = dnode.children.find(c => c.filename === seg)
      expect(dnode).toBeTruthy()
    }
    expect(dnode.path).toBe(
      '/demo-service/demo-service-domain/src/main/java/com/example/demo/domain/service/'
    )
    expect(dnode.path).not.toBe(appService.path)

    const serviceNodes = []
    const walk = n => {
      if (n.filename === 'service') serviceNodes.push(n.path)
      ;(n.children || []).forEach(walk)
    }
    walk(tree)
    expect(serviceNodes).toHaveLength(3) // application/service, test/.../service, domain/service
    expect(new Set(serviceNodes).size).toBe(3)
  })
})

/**
 * Function findRoot
 */
describe('findRoot', () => {
  // eslint-disable-next-line
  it('should return the right value', async () => {
    const zipJs = new JSZip()
    await fs.readFile(
      path.resolve('./dev/starter.mock.zip'),
      async (err, blob) => {
        const { files } = await zipJs.loadAsync(blob).catch(() => {
          throw Error(`Could not load the ZIP project.`)
        })
        const pathZ = `${findRoot({ files })}/`
        expect(pathZ).toBe('demo/')
      }
    )
  })
})
