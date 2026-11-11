import assert from 'node:assert/strict';
import { readFileSync, writeFileSync, openSync, closeSync } from 'node:fs';
import { resolve, join } from 'node:path';
import { spawn } from 'node:child_process';
import { writePalette } from './palette-fixture.mjs';
const { scenario, command, screenshot, mct, waitFor } = await import(process.env.HARNESS_E2E_LIB);
const root = process.cwd();
const config = JSON.parse(readFileSync('.harness-config.json')).runtime;
const serverDir = resolve(config.server.directory);
const delay = ms => new Promise(r => setTimeout(r, ms));

// The transport acknowledges sending, not executing. Inspect untranslated keys
// so this works with any client language; report server errors at their source.
async function checked(client, text, key) {
  mct(client, 'chat', 'clear');
  command(client, text);
  const matches = data => [key].flat().some(k => JSON.stringify(data).includes(k));
  const history = await waitFor(() => mct(client, 'chat', 'history', '--last', '20'),
    data => matches(data) || data.messages.some(message => message.raw?.includes('"color":"red"')), 60_000, 250);
  assert.ok(matches(history), `${text}: ${JSON.stringify(history)}`);
  console.log(`OK ${text}`);
  return history;
}

async function view(client, dimension, x, y, z, yaw = 0, pitch = 20) {
  await checked(client, `execute in ${dimension} run tp @s ${x} ${y} ${z} ${yaw} ${pitch}`, 'commands.teleport.success');
  // A server-side loaded predicate handles asynchronous chunk generation.
  await waitFor(async () => {
    command(client, `execute in ${dimension} if loaded ${Math.floor(x)} ${Math.floor(y)} ${Math.floor(z)} run tellraw @s "m07_chunk_ready"`);
    return JSON.stringify(mct(client, 'chat', 'history', '--last', '4'));
  }, text => text.includes('m07_chunk_ready'), 60_000, 500).then(text => assert.ok(text.includes('m07_chunk_ready'), 'Destination chunk did not load'));
  await delay(3000);
}

async function capture(client, ac, label) {
  mct(client, 'gui', 'close');
  mct(client, 'input', 'key', 'press', 'f1');
  await delay(500);
  try { screenshot(client, ac, label); }
  finally { mct(client, 'input', 'key', 'press', 'f1'); }
}

async function paletteStates(client, dimension) {
  await view(client, dimension, 2011, 85, 1985, 0, 14);
  const nether = dimension === 'minecraft:the_nether';
  const wood = nether ? 'tfcoverdims:wood' : 'tfc:wood';
  const species = nether ? 'crimson' : 'spruce';
  const checks = [
    [1, 1, 1, `tfc:rock/bricks/${nether ? 'basalt' : 'granite'}_stairs`, { facing: 'east', half: 'top' }],
    [3, 1, 1, `${wood}/planks/${species}`, {}],
    [5, 1, 1, `${wood}/door/${species}`, { facing: 'south', half: 'lower' }],
    [5, 2, 1, `${wood}/door/${species}`, { facing: 'south', half: 'upper' }],
    [7, 1, 1, `${wood}/wall_sign/${species}`, { facing: 'north' }],
    [9, 1, 1, 'tfc:firepit', { axis: 'x' }],
    [11, 1, 1, `${wood}/chest/${species}`, { facing: 'south' }],
    [13, 1, 1, 'minecraft:barrel', { facing: 'east' }],
    [15, 1, 1, 'minecraft:water_cauldron', { level: '2' }],
    [17, 1, 1, 'minecraft:lantern', {}], [19, 1, 1, 'minecraft:campfire', {}],
    [21, 1, 1, 'minecraft:blackstone', {}],
    ...(!nether ? [[1, 2, 3, 'tfc:plant/eel_grass', {}], [1, 3, 3, 'minecraft:water', {}],
      [4, 2, 3, 'tfc:plant/silken_pincushion_cactus', {}], [4, 3, 3, 'minecraft:air', {}], [4, 4, 3, 'minecraft:air', {}]] : []),
  ];
  const states = [];
  for (const [dx, dy, dz, type, properties] of checks) {
    const state = await waitFor(() => mct(client, 'block', 'get', String(2000 + dx), String(80 + dy), String(2000 + dz)), v => v.type === type, 10_000, 500);
    assert.equal(state.type, type, `${dimension} ${dx},${dy},${dz}`);
    for (const [key, value] of Object.entries(properties)) assert.equal(state.properties[key], value, `${type}.${key}`);
    // Light is time-dependent and is not persisted BlockState data.
    states.push({ type: state.type, properties: state.properties });
  }
  const loot = await checked(client, 'data get block 2013 81 2001 LootTable', 'commands.data.block.query');
  assert.ok(JSON.stringify(loot).includes('minecraft:chests/abandoned_mineshaft'));
  // These reads fail if the converted Block Entity failed to load.
  await checked(client, 'data get block 2011 81 2001', 'commands.data.block.query');
  await checked(client, 'data get block 2009 81 2001', 'commands.data.block.query');
  return states;
}

async function restart() {
  const { stopClients, stopServer, startClients } = await import('../../.harness/dist/e2e/runtime.js');
  const { run } = await import('../../.harness/dist/core/process.js');
  await stopClients(root, config, run);
  await stopServer(root); // Wait for the process to exit and region files to close.
  // startServer deliberately deletes the test world, so restart its configured
  // command directly and keep the PID record used by Harness cleanup in sync.
  const log = join(process.env.HARNESS_SCREENSHOT_DIR, '..', 'restart-server.log');
  const fd = openSync(log, 'w');
  const [executable, ...args] = config.server.command;
  const child = spawn(executable, args, { cwd: serverDir, detached: true, stdio: ['ignore', fd, fd] });
  await new Promise((resolve, reject) => { child.once('spawn', resolve); child.once('error', reject); });
  closeSync(fd);
  child.unref();
  writeFileSync('.harness-state/runtime/server.json', JSON.stringify({ pid: child.pid }));
  const ready = await waitFor(() => readFileSync(log, 'utf8'), text => /Done \([\d.,]+s\)!/.test(text), 180_000);
  assert.match(ready, /Done \([\d.,]+s\)!/, 'Restarted server did not finish loading');
  await startClients(root, config, run);
}

await scenario(async ({ clients }) => {
  const client = clients[0];
  await checked(client, 'gamerule sendCommandFeedback true', 'commands.gamerule.set');
  command(client, 'gamemode spectator');
  const mode = await waitFor(() => mct(client, 'status', 'gamemode'),
    data => JSON.stringify(data).includes('spectator'), 60_000, 250);
  assert.ok(JSON.stringify(mode).includes('spectator'), 'Client did not enter spectator mode');
  await checked(client, 'gamerule doMobSpawning false', 'commands.gamerule.set');
  await checked(client, 'difficulty peaceful', 'commands.difficulty');
  await checked(client, 'effect give @s minecraft:night_vision infinite 0 true',
    ['commands.effect.give.success', 'commands.effect.give.failed']);
  assert.ok(JSON.stringify(mct(client, 'status', 'effects')).includes('minecraft:night_vision'));
  await checked(client, 'gamerule randomTickSpeed 0', 'commands.gamerule.set');

  for (const [id, dimension, x, z, label] of [
    ['bettermineshafts:mineshaft_oak', 'minecraft:overworld', 400, 400, 'better-mineshaft'],
    ['betterstrongholds:stronghold', 'minecraft:overworld', 800, 800, 'better-stronghold'],
    ['betteroceanmonuments:ocean_monument', 'minecraft:overworld', 1200, 1200, 'better-ocean-monument'],
    ['betterfortresses:fortress', 'minecraft:the_nether', 400, 400, 'better-fortress'],
  ]) {
    await view(client, dimension, x, 84, z);
    await checked(client, `execute in ${dimension} run forceload add ${x - 120} ${z - 120} ${x + 120} ${z + 120}`, 'commands.forceload.added.multiple');
    // Each YUNG start can span up to 116 blocks from the chunk center.
    // forceload only schedules work: check all 256 chunks, not just corners.
    const loaded = [];
    for (let cx = Math.floor((x - 120) / 16); cx <= Math.floor((x + 120) / 16); cx++) {
      for (let cz = Math.floor((z - 120) / 16); cz <= Math.floor((z + 120) / 16); cz++) {
        loaded.push(`if loaded ${cx * 16} 80 ${cz * 16}`);
      }
    }
    const marker = `m07_region_ready_${label}`;
    const ready = await waitFor(() => {
      command(client, `execute in ${dimension} ${loaded.join(' ')} run tellraw @s "${marker}"`);
      return JSON.stringify(mct(client, 'chat', 'history', '--last', '4'));
    }, text => text.includes(marker), 120_000, 500);
    assert.ok(ready.includes(marker), `Chunks did not load for ${id}`);
    await checked(client, `execute in ${dimension} run place structure ${id} ${x} 80 ${z}`, 'commands.place.structure.success');
    const cameraY = label === 'better-mineshaft' ? -20 : label === 'better-stronghold' ? 30 : label === 'better-fortress' ? 105 : 85;
    await view(client, dimension, x - 12, cameraY, z - 16, -35, 60);
    await capture(client, 'AC-F010-001', label);
  }

  const before = [];
  for (const [dimension, namespace] of [['minecraft:overworld', 'betterstrongholds'], ['minecraft:the_nether', 'betterfortresses']]) {
    writePalette(join(serverDir, config.server.world), namespace, dimension.endsWith('the_nether'));
    await view(client, dimension, 2009, 84, 1993);
    await checked(client, `execute in ${dimension} run fill 1998 79 1983 2024 88 2006 minecraft:air`, ['commands.fill.success', 'commands.fill.failed']);
    if (dimension === 'minecraft:overworld') {
      // StructureTemplate inherits fluid from the destination, as in an ocean.
      await checked(client, 'fill 2001 82 2003 2001 83 2003 minecraft:water', 'commands.fill.success');
    }
    await checked(client, `execute in ${dimension} run place template ${namespace}:m07_e2e 2000 80 2000`, 'commands.place.template.success');
    before.push(await paletteStates(client, dimension));
    await capture(client, 'AC-F010-001', `${namespace}-converted-palette`);
  }
  await checked(client, 'save-all flush', 'commands.save.success');
  await restart();
  await checked(client, 'gamerule sendCommandFeedback true', 'commands.gamerule.set');
  for (const [index, dimension] of ['minecraft:overworld', 'minecraft:the_nether'].entries()) {
    assert.deepEqual(await paletteStates(client, dimension), before[index], `${dimension} changed after restart`);
    await capture(client, 'AC-F010-002', `${dimension.split(':')[1]}-after-restart`);
  }
});
