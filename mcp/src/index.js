#!/usr/bin/env node
/**
 * MCP server: bootstrap Spring projects against a local start-site instance.
 * Env: START_SITE_URL (default http://localhost:8080)
 */
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';
import fs from 'node:fs';
import path from 'node:path';

const BASE = (process.env.START_SITE_URL || 'http://localhost:8080').replace(/\/$/, '');

async function getJson(urlPath) {
  const res = await fetch(`${BASE}${urlPath}`);
  if (!res.ok) throw new Error(`${urlPath} -> ${res.status}`);
  return res.json();
}

function buildStarterUrl(args) {
  const u = new URL(`${BASE}/starter.zip`);
  const set = (k, v) => {
    if (v !== undefined && v !== null && String(v).length) u.searchParams.set(k, String(v));
  };
  set('type', args.type || 'maven-project');
  set('language', args.language || 'java');
  set('bootVersion', args.bootVersion || args.boot);
  set('baseDir', args.baseDir || args.artifactId || 'demo');
  set('groupId', args.groupId || 'com.example');
  set('artifactId', args.artifactId || 'demo');
  set('name', args.name || args.artifactId || 'demo');
  set('packageName', args.packageName || 'com.example.demo');
  set('javaVersion', args.javaVersion || args.java || '21');
  set('packaging', args.packaging || 'jar');
  let deps = args.dependencies || '';
  if (!deps && (args.architecture || args.arch)) {
    deps = `${args.architecture || args.arch},web,validation,mybatis-plus,postgresql,flyway,knife4j`;
  }
  set('dependencies', deps);
  set('template', args.template || 'ddd-enhanced');
  if (args.entities) {
    const json = typeof args.entities === 'string' ? args.entities : JSON.stringify(args.entities);
    set('entities', json);
  }
  return u;
}

const server = new McpServer({ name: 'start-spring-io', version: '1.0.0' });

server.tool(
  'get_capabilities',
  'Describe architectures, templates, entity schema, deps kits, URL convention, examples',
  {},
  async () => {
    try {
      const data = await getJson('/ai/v1/capabilities');
      return { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] };
    } catch (e) {
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify({
              error: String(e),
              hint: `Is start-site running at ${BASE}? Try --application.offline=true`,
              fallback: {
                architectures: ['ddd-six-module', 'platform-monorepo'],
                templates: ['ddd-enhanced'],
                urlConvention: '/{prefix}/v1/{resource}',
              },
            }, null, 2),
          },
        ],
      };
    }
  }
);

server.tool(
  'describe_generator',
  'Alias of get_capabilities with entity JSON schema attached',
  {},
  async () => {
    let caps = {};
    let schema = {};
    try {
      caps = await getJson('/ai/v1/capabilities');
      schema = await getJson('/ai/v1/schemas/entity');
    } catch (e) {
      caps = { error: String(e), base: BASE };
    }
    return { content: [{ type: 'text', text: JSON.stringify({ capabilities: caps, entitySchema: schema }, null, 2) }] };
  }
);

server.tool(
  'get_metadata',
  'Fetch /metadata/client (or simplified error)',
  {},
  async () => {
    try {
      const data = await getJson('/metadata/client');
      return { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] };
    } catch (e) {
      return { content: [{ type: 'text', text: JSON.stringify({ error: String(e), url: `${BASE}/metadata/client` }, null, 2) }] };
    }
  }
);

server.tool(
  'list_dependencies',
  'List dependency ids from metadata client payload',
  {},
  async () => {
    const data = await getJson('/metadata/client');
    const deps = data?.dependencies?.values || data?.dependencies || [];
    return { content: [{ type: 'text', text: JSON.stringify(deps, null, 2) }] };
  }
);

server.tool(
  'validate_request',
  'Dry-run validate a project request via POST /ai/v1/validate',
  {
    body: z.record(z.any()).describe('Project JSON body (groupId, artifactId, template, entities, ...)'),
  },
  async ({ body }) => {
    const res = await fetch(`${BASE}/ai/v1/validate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    const text = await res.text();
    return { content: [{ type: 'text', text }] };
  }
);

server.tool(
  'create_project',
  'Generate a project zip via /starter.zip and write it to outputPath',
  {
    outputPath: z.string().describe('Filesystem path for the .zip (or directory + auto name)'),
    type: z.string().optional(),
    language: z.string().optional(),
    bootVersion: z.string().optional(),
    groupId: z.string().optional(),
    artifactId: z.string().optional(),
    packageName: z.string().optional(),
    javaVersion: z.string().optional(),
    architecture: z.string().optional(),
    template: z.string().optional(),
    dependencies: z.string().optional(),
    entities: z.any().optional().describe('Array of entity specs or JSON string'),
  },
  async (args) => {
    const url = buildStarterUrl(args);
    const res = await fetch(url);
    if (!res.ok) {
      const body = await res.text();
      throw new Error(`starter.zip failed ${res.status}: ${body.slice(0, 500)}`);
    }
    const buf = Buffer.from(await res.arrayBuffer());
    let out = args.outputPath;
    if (!out.endsWith('.zip')) {
      fs.mkdirSync(out, { recursive: true });
      out = path.join(out, `${args.artifactId || 'demo'}.zip`);
    } else {
      fs.mkdirSync(path.dirname(out), { recursive: true });
    }
    fs.writeFileSync(out, buf);
    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({ ok: true, bytes: buf.length, outputPath: out, requestUrl: url.toString() }, null, 2),
        },
      ],
    };
  }
);

const transport = new StdioServerTransport();
await server.connect(transport);
