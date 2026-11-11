import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { gzipSync } from 'node:zlib';

// A Vanilla input palette, written only into the disposable E2E world. Loading
// through StructureTemplateManager exercises the normal YUNG namespace hook.
export function writePalette(world, namespace, nether) {
  const blocks = [];
  const wood = nether ? 'crimson' : 'spruce';
  const add = (x, y, z, name, properties = {}, nbt) => blocks.push({ pos: [x, y, z], name, properties, nbt });
  for (let x = 0; x < 22; x++) for (let z = 0; z < 5; z++) add(x, 0, z, 'minecraft:stone_bricks');
  add(1, 1, 1, 'minecraft:stone_brick_stairs', { facing: 'east', half: 'top' });
  add(3, 1, 1, `minecraft:${wood}_planks`);
  add(5, 1, 1, `minecraft:${wood}_door`, { facing: 'south', half: 'lower' });
  add(5, 2, 1, `minecraft:${wood}_door`, { facing: 'south', half: 'upper' });
  add(7, 1, 2, `minecraft:${wood}_planks`);
  add(7, 1, 1, `minecraft:${wood}_wall_sign`, { facing: 'north' });
  add(9, 1, 1, 'minecraft:furnace', { facing: 'east' });
  add(11, 1, 1, 'minecraft:chest', { facing: 'south' }, { id: 'minecraft:chest' });
  add(13, 1, 1, 'minecraft:barrel', { facing: 'east' }, { id: 'minecraft:barrel', LootTable: 'minecraft:chests/abandoned_mineshaft' });
  add(15, 1, 1, 'minecraft:water_cauldron', { level: '2' });
  add(17, 1, 1, 'minecraft:lantern');
  add(19, 1, 1, 'minecraft:campfire', { lit: 'false' });
  add(21, 1, 1, 'minecraft:blackstone');
  if (!nether) {
    // Enclose the water, and support both plant bases through neighbor updates.
    for (let x = 0; x <= 2; x++) for (let z = 2; z <= 4; z++) {
      if (x !== 1 || z !== 3) for (let y = 1; y <= 3; y++) add(x, y, z, 'minecraft:glass');
    }
    add(1, 1, 3, 'minecraft:sand');
    add(1, 2, 3, 'minecraft:tall_seagrass', { half: 'lower' });
    add(1, 3, 3, 'minecraft:tall_seagrass', { half: 'upper' });
    add(4, 1, 3, 'minecraft:sand');
    for (let y = 2; y <= 4; y++) add(4, y, 3, 'minecraft:cactus');
  }
  const palette = blocks.map(b => compound([
    named(8, 'Name', string(b.name)),
    named(10, 'Properties', compound(Object.entries(b.properties).map(([k, v]) => named(8, k, string(v))))),
  ]));
  const entries = blocks.map((b, i) => compound([
    named(9, 'pos', list(3, b.pos.map(int))), named(3, 'state', int(i)),
    ...(b.nbt ? [named(10, 'nbt', compound(Object.entries(b.nbt).map(([k, v]) => named(8, k, string(v)))))] : []),
  ]));
  const root = named(10, '', compound([
    named(3, 'DataVersion', int(3955)), named(9, 'size', list(3, [22, 5, 5].map(int))),
    named(9, 'palette', list(10, palette)), named(9, 'blocks', list(10, entries)), named(9, 'entities', list(10, [])),
  ]));
  const dir = join(world, 'generated', namespace, 'structures');
  mkdirSync(dir, { recursive: true });
  writeFileSync(join(dir, 'm07_e2e.nbt'), gzipSync(root));
}

const int = value => { const b = Buffer.alloc(4); b.writeInt32BE(value); return b; };
const string = value => { const b = Buffer.from(value); const size = Buffer.alloc(2); size.writeUInt16BE(b.length); return Buffer.concat([size, b]); };
const named = (type, name, data) => Buffer.concat([Buffer.from([type]), string(name), data]);
const compound = entries => Buffer.concat([...entries, Buffer.from([0])]);
const list = (type, entries) => Buffer.concat([Buffer.from([type]), int(entries.length), ...entries]);
