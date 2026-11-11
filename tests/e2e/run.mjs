// Run the registered scenarios with the configured Harness runtime, without
// starting implementation/review agents or retrying failed tests automatically.
import { access, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';
import { withLock } from '../../.harness/dist/core/lock.js';
import { run } from '../../.harness/dist/core/process.js';
import { runE2E } from '../../.harness/dist/e2e/e2e.js';

const root = fileURLToPath(new URL('../../', import.meta.url));
process.chdir(root);
await withLock(root, async () => {
  await rm('.tmp/e2e/latest-summary.json', { force: true });
  const config = JSON.parse(await readFile('.harness-config.json', 'utf8')).runtime;
  const mappings = JSON.parse(await readFile('tests/acceptance.json', 'utf8')).e2e;
  const properties = await readFile('gradle.properties', 'utf8');
  const property = key => properties.match(new RegExp(`^${key}=(.+)$`, 'm'))[1].trim();
  const jar = join(root, 'build/libs', `${property('mod_id')}-${property('mod_version')}.jar`);
  await access(jar); // Run ./gradlew build first.
  // YUNG start heights and room layouts depend on the world seed. Keep the
  // disposable rendering world reproducible across runs and camera captures.
  const serverProperties = join(root, config.server.directory, 'server.properties');
  const serverSettings = (await readFile(serverProperties, 'utf8'))
    .split('\n').filter(line => !line.startsWith('level-seed=')).join('\n');
  await writeFile(serverProperties, serverSettings.trimEnd() + '\nlevel-seed=731031\n');
  const logDir = join(root, '.tmp/e2e', new Date().toISOString().replaceAll(':', '-'));
  await mkdir(logDir, { recursive: true });
  console.log(`E2E logs: ${logDir}`);
  const summary = await runE2E(root, config, mappings, jar, logDir, run);
  await writeFile(join(logDir, 'summary.json'), JSON.stringify(summary, null, 2) + '\n');
  await writeFile('.tmp/e2e/latest-summary.json', JSON.stringify(summary, null, 2) + '\n');
  for (const result of summary.scenarios) {
    console.log(`${result.success ? 'PASS' : 'FAIL'} ${result.scenarioId}: ${result.screenshots.length} screenshots`);
    if (result.error) console.error(result.error);
  }
  process.exitCode = summary.scenarios.every(result => result.success) ? 0 : 1;
});
