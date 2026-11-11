# Project

Status: active

Project ID: yungsbettertfc-v1
Mod ID: yungsbettertfc

## Platform

Minecraft: 1.21.1
NeoForge: 21.1.264
Java: 21

## Purpose

Minecraft 1.21.1 / NeoForge 向けの既存MOD「TFC x YUNG's Better Series」`v0.4.3` を、MOD ID `yungsbettertfc` を維持したまま `v1.0.0` に改修する。

YUNG's Better Series の構造物をTerraFirmaCraft（TFC）の地形に生成し、構造物由来のバニラ建材・設備・装備・戦利品をTFCに適合させる。旧版のBeneath連携を完全に削除し、TFC Overdimsを必須依存とする。Overworldでは旧版の既存動作を原則維持する。NetherではTFC Basalt、TFC OxisolおよびOverdimsのCrimson/Warped木材を用いる。Endには本MODのブロック・エンティティ変換を適用しない。

本書は実装仕様の正本である。実装者は過去の会話を参照できないため、本書の要件・例外・変換表・完成条件だけで仕様を判断すること。旧版の挙動を調べる際は、文書の記述だけでなく、指定した公開タグのJavaコードとJSONを直接照合する。

## Features

### F-001: 起動・依存関係

#### Description

配布バージョンは `1.0.0`。Minecraftは `1.21.1` のみ、Javaは21、初期NeoForge基準は `21.1.264` とする。必須依存は `tfc`、`yungsapi`、`tfcoverdims`。任意依存は `betterdungeons`、`bettermineshafts`、`betterstrongholds`、`betteroceanmonuments`、`betterfortresses` の5種類。`beneath` は依存定義、コード、構造物対象名前空間および独自マップから除去する。任意MODの有無で起動が失敗してはならない。同じMinecraft 1.21.1向けの後続依存MOD更新に対応する設計とする。Mod IDは引き継ぐが、事実上の新規実装として開発を始める。

#### Acceptance Criteria

##### AC-F001-001: Beneathなしで起動する

Preconditions:
Minecraft 1.21.1、NeoForge、Java 21、TFC、YUNG's API、TFC Overdimsを導入し、Beneathを導入していない。対象MODのビルド版は `1.0.0` である。

Action:
ゲームまたは専用サーバーを起動し、`yungsbettertfc` のロード結果を確認する。

Expected Result:
`yungsbettertfc` が正常にロードされる。Beneath欠落による警告、依存関係エラー、クラスロードエラーが発生しない。MOD IDは変更されない。

##### AC-F001-002: 必須依存関係を宣言する

Preconditions:
TFCとYUNG's APIが導入され、TFC Overdimsが導入されていない。

Action:
NeoForgeのMOD読込を開始する。

Expected Result:
`tfcoverdims` が必須依存として認識され、NeoForgeが依存欠落を報告する。Beneathは必須依存にも任意依存にも含まれない。

##### AC-F001-003: 任意YUNG MODの構成差に耐える

Preconditions:
必須依存MODは導入済みである。任意のYUNG MODについて、0種類・1種類・5種類導入の構成を準備する。

Action:
各構成でゲームを起動し、構造物が存在する構成では対象構造物を生成する。

Expected Result:
すべての構成でクラッシュしない。導入済みYUNG MODの構造物のみが互換処理の対象となる。未導入MODへのMixin参照によって起動が失敗しない。

### F-002: ディメンション別処理範囲

#### Description

Overworldは周辺のTFC岩石・土壌に対応した変換を実施する。Netherは構造物中のOverworld由来岩石にBasalt、土壌にOxisolを使用する。Nether固有ブロックは個別例外表に従う。Endでは本MODのブロック、Block Entity、額縁・防具立ての装備変換を一切実施しない。`WorldGenLevel`からディメンションを正しく解決し、取得失敗時にEndなどへ誤ってFULL変換を適用しない。

#### Acceptance Criteria

##### AC-F002-001: Overworldの周辺地質を反映する

Preconditions:
OverworldのTFC岩石が存在する場所に、`minecraft:stone` と木材を含む対象YUNG構造物を用意する。

Action:
構造物を生成し、石材と木材の配置結果を確認する。

Expected Result:
石材が検出された周辺TFC岩石種に変換され、木材が本書の木材表に従う。

##### AC-F002-002: Netherの専用変換を適用する

Preconditions:
Netherの対象YUNG構造物に `stone`、`dirt`、`crimson_planks`、`blackstone` が含まれる。

Action:
構造物を生成し、各ブロックを確認する。

Expected Result:
`stone` はTFC Basalt、`dirt` はTFC Oxisol、`crimson_planks` はOverdims木材になる。`blackstone` は変更されない。

##### AC-F002-003: Endに変換を適用しない

Preconditions:
End Cityのテンプレートにバニラの建材、設備および防具立てが存在する。

Action:
End Cityを生成し、本MODによる変換前後の状態を調べる。

Expected Result:
本MODはブロック、Block Entity、エンティティ装備を変化させない。`minecraft:end_city/*` に本MODのプロセッサを注入しない。ただしOverdimsが独立して適用するEnd戦利品修飾は妨げない。

### F-003: Overworldの岩石・砂岩・土壌変換

#### Description

旧版の岩石検出と石材・砂岩・土壌変換を継承し、`granite`、`diorite`、`tuff` と対応する研磨石・階段・ハーフブロック・壁・石レンガ等を追加する。`deepslate`系一般建材と`blackstone`系は維持する。変換マップの完全な内容は「Global Requirements」の対応表に定義する。

#### Acceptance Criteria

##### AC-F003-001: 岩石系の追加ブロックを変換する

Preconditions:
Overworldに既知のTFC岩石層がある。対象構造物に `granite`、`diorite`、`tuff`、それぞれの階段・ハーフブロック・研磨石、`tuff_bricks` が含まれる。

Action:
構造物を生成し、ブロックIDおよび階段等の状態を検査する。

Expected Result:
通常ブロックは `tfc:rock/raw/<rock>`、研磨石は `tfc:rock/smooth/<rock>`、石レンガは `tfc:rock/bricks/<rock>` に変換される。対応する形状と引き継ぎ可能な向き等の状態を保持する。

##### AC-F003-002: 旧版の石材変換を維持する

Preconditions:
対象構造物に `stone`、`andesite`、`cobblestone`、`mossy_cobblestone`、`stone_bricks`、`smooth_stone`、`gravel` が含まれる。

Action:
Overworldで構造物を生成する。

Expected Result:
各ブロックが本書の対応表に示すTFC岩石のraw、cobble、mossy_cobble、bricks、smooth、gravelへ変換される。周辺岩石が検出できない場合は `granite` を使用する。

##### AC-F003-003: 砂岩と土壌の対応を維持する

Preconditions:
Overworldの対象構造物に赤・黄の各種砂岩、`dirt`、`coarse_dirt`、`grass_block`、`rooted_dirt`、`farmland`、`dirt_path`、`sand`、`red_sand` が含まれる。

Action:
構造物を生成する。

Expected Result:
砂岩の赤・黄と加工形状が本書の対応表どおりに維持される。土壌が周辺のTFC土壌種に置換される。道の対象IDはMinecraft 1.21.1の `dirt_path` とする。検出できない土壌の既定値は `mollisol` である。

##### AC-F003-004: DeepslateとBlackstoneの建材を維持する

Preconditions:
Overworldの対象構造物に `deepslate`、`cobbled_deepslate`、`deepslate_bricks`、`blackstone`、`polished_blackstone` が含まれる。

Action:
構造物を生成する。

Expected Result:
これら一般建材のブロックIDが変更されない。`deepslate_*_ore` は別途定める鉱石変換規則の対象となる。

### F-004: Nether地質と構造物の硬化

#### Description

Netherに生成された構造物のOverworld由来石材はTFC Basalt系へ、土壌はTFC Oxisol系へ変換する。`minecraft:netherrack` は通常維持し、真下が空気なら `tfcoverdims:hardened_netherrack` にする。TFC raw、cobble、mossy_cobbleのフルブロックも真下が空気なら対応するhardened rockにする。Blackstone、Deepslate系一般建材、Nether固有鉱石等には指定外の変換を追加しない。Overdims側の崩落タグは変更しない。

#### Acceptance Criteria

##### AC-F004-001: Netherの石材をBasalt系に変換する

Preconditions:
Netherの対象構造物に `stone`、`cobblestone`、`stone_bricks`、`andesite`、`granite`、`diorite`、`tuff`、`gravel` が含まれる。

Action:
構造物を生成する。

Expected Result:
対応する `tfc:rock/raw/basalt`、`cobble/basalt`、`bricks/basalt`、`gravel/basalt` 等へ変換される。元のgranite/diorite/tuffの石種を保持しない。

##### AC-F004-002: Netherの土壌をOxisolに変換する

Preconditions:
Netherの対象構造物に `dirt`、`coarse_dirt`、`grass_block`、`rooted_dirt`、`farmland`、`dirt_path` が含まれる。

Action:
構造物を生成する。

Expected Result:
`tfc:dirt/oxisol`、`coarse_dirt/oxisol`、`grass/oxisol`、`rooted_dirt/oxisol`、`farmland/oxisol`、`grass_path/oxisol` の各対応ブロックを使用する。砂は赤・黄を維持する。

##### AC-F004-003: 真下の空気だけでhardenedを決定する

Preconditions:
Netherの対象構造物に `netherrack`、石材のraw/cobble/mossy_cobbleのフルブロック、階段、ハーフブロックが存在する。各対象について、真下が空気の場合と空気でない場合を準備する。

Action:
各状態でブロックを配置・生成する。

Expected Result:
真下が空気のNetherrackは `tfcoverdims:hardened_netherrack` となる。真下が空気の対象TFC岩石フルブロックは `tfc:rock/hardened/basalt` となる。真下が空気でない場合、または階段・ハーフブロック・壁は硬化しない。追加の支持条件は評価しない。

##### AC-F004-004: Nether固有ブロックを維持する

Preconditions:
Netherの対象構造物に `blackstone`、`polished_blackstone`、`basalt`、`polished_basalt`、`soul_sand`、`soul_soil`、`nether_gold_ore`、`nether_quartz_ore`、`ancient_debris` が含まれる。

Action:
構造物を生成し、該当ブロックを検査する。

Expected Result:
本MODによって各ブロックが変換されない。BlackstoneについてOverdimsの崩落挙動を無効化する変更を行わない。

### F-005: OverworldとNetherの木材変換

#### Description

Overworldの9種類のバニラ木材を旧版と同じTFC木材種へ対応付ける。NetherのCrimson/WarpedのStem、Hyphae、板材、階段、ハーフブロック、ドア、トラップドア、フェンス、フェンスゲート、ボタン、感圧板、看板等は `tfcoverdims:wood/...` に変換する。変換先が存在しないHanging Signは維持する。無印Chest等は構造物内の木材ヒントを用いる。旧版の `dark_oak` ヒント検出の不具合を修正する。

#### Acceptance Criteria

##### AC-F005-001: Overworld木材種と形状を維持する

Preconditions:
Overworldの対象構造物にOak、Spruce、Birch、Jungle、Acacia、Dark Oak、Mangrove、Cherry、Bambooの板材、原木、階段、ドア、フェンス等が含まれる。

Action:
構造物を生成する。

Expected Result:
旧版の木材種対応表に従ってTFC木材に変換され、ドアの上下、階段の向き、ハーフブロックの種類等の合法な状態を引き継ぐ。`dark_oak` を木材ヒントとして認識できる。

##### AC-F005-002: Crimson/WarpedをOverdims木材に変換する

Preconditions:
Netherの対象構造物にCrimsonとWarpedのStem、Hyphae、Stripped Stem/Hyphae、Planks、Stairs、Slab、Door、Trapdoor、Fence、Fence Gate、Button、Pressure Plate、Signが含まれる。

Action:
構造物を生成する。

Expected Result:
対応する登録済みの `tfcoverdims:wood/*/crimson` または `tfcoverdims:wood/*/warped` 系ブロックに変換される。BeneathのブロックIDを生成しない。

##### AC-F005-003: 木材ヒントと非対応形状を処理する

Preconditions:
木材種を示すブロックと無印の `chest`、`trapped_chest`、`lectern`、`crafting_table` が同じ対象構造物にある。Crimson/Warped Hanging Signも含まれる。

Action:
OverworldとNetherで構造物を生成する。

Expected Result:
無印の木製設備が検出した木材種に対応する登録済みTFC/Overdims木材系へ変換される。ヒント不明時はOakへフォールバックする。Overdimsに対応先のないHanging SignはVanillaのまま維持する。ChestのNBT互換性を本MOD独自に検証・変換しない。

### F-006: 設備・光源・金属ブロック

#### Description

Furnace、Blast Furnaceおよび新たにSmokerを `tfc:firepit` に変換し、元のBlock Entity NBTを破棄する。Cauldron4種、Barrel、Lantern2種、Campfire2種、Stonecutter等は変換しない。Candle、Torch、旧版の金属ブロックと木製設備の変換は継承する。元のNBTの消去はFirepit変換のみに限定し、それ以外は旧版と同様に渡す。Chestの詳細なNBT互換性の追加検証は実施しない。

#### Acceptance Criteria

##### AC-F006-001: かまど3種類をFirepitにする

Preconditions:
対象構造物に中身を持つ `furnace`、`blast_furnace`、`smoker` が異なる水平方向で含まれる。

Action:
構造物を生成し、ブロックID、方向およびBlock Entity NBTを検査する。

Expected Result:
3種類すべてが `tfc:firepit` となり、元のBlock Entity NBTは引き継がない。設定可能な場合は元の水平方向からFirepitの軸を設定する。

##### AC-F006-002: CauldronとBarrelを維持する

Preconditions:
対象構造物に `cauldron`、`water_cauldron`、`lava_cauldron`、`powder_snow_cauldron` と、戦利品テーブルを持つ `barrel` が存在する。

Action:
構造物を生成し、ブロックID、Cauldronの状態、Barrelの戦利品テーブルを確認する。

Expected Result:
すべてVanillaブロックのまま維持される。Cauldronの内容物・水位に相当する状態が失われず、Barrelの戦利品テーブルが保持される。Large Vesselへの置換は行われない。

##### AC-F006-003: 光源と装飾の例外を維持する

Preconditions:
対象構造物に `torch`、`wall_torch`、`candle`、色付きCandle、`lantern`、`soul_lantern`、`campfire`、`soul_campfire` が含まれる。

Action:
構造物を生成する。

Expected Result:
TorchとCandle系は登録済みTFCブロックに変換される。Lantern2種とCampfire2種は変換されない。色付きCandle等の候補IDが存在しない場合は元のブロックを維持する。

##### AC-F006-004: 金属ブロックの旧版規則を維持する

Preconditions:
対象構造物に `iron_bars`、`chain`、`iron_block`、`iron_trapdoor`、`gold_block`、`raw_gold_block`、各種Copper Blockが含まれる。

Action:
構造物を生成する。

Expected Result:
Wrought Iron、Gold、Copperの旧版対応表に従って変換される。Copperの既存の酸化段階と対応する形状は維持される。Anvil、Smithing Table、Stonecutterの新規変換は追加されない。

### F-007: 植物・鉱石・展示エンティティ

#### Description

植物については旧版の実装に存在する変換だけを継承する。Overworldの通常鉱石とDeepslate鉱石は旧版のTFC鉱石マップを継承する一方、Nether Gold OreとNether Quartz Oreは変換対象から除外する。額縁・発光額縁・防具立ての旧版装備変換はOverworld/Netherの対象テンプレートで維持し、Endには適用しない。

#### Acceptance Criteria

##### AC-F007-001: 水生植物とCactusを旧版どおり変換する

Preconditions:
対象構造物に `kelp`、`kelp_plant`、`seagrass`、上下2段の`tall_seagrass`、`sea_pickle`、`vine`、複数段の`cactus`、`dead_bush` が存在する。

Action:
構造物を生成する。

Expected Result:
Kelpは水、SeagrassはTFC Eel Grass、Tall Seagrassの上段は水、Sea Pickleは対応TFC、VineはTFC Ivy、CactusはTFC Silken Pincushion Cactusの基部、Dead Bushは対応TFCになる。旧版のCactus上段除去を維持する。

##### AC-F007-002: 旧版の対象外植物を増やさない

Preconditions:
対象構造物に草、シダ、キノコ、普通の花、葉、苗木、`potted_*` がある。

Action:
構造物を生成する。

Expected Result:
草・シダ・キノコ・普通の花・葉・苗木に新規変換を追加しない。`potted_*` は登録済みの対応TFC鉢植えがある場合だけ変換する。

##### AC-F007-003: 一般鉱石をTFCに変換する

Preconditions:
対象構造物にCoal、Iron、Copper、Gold、Lapis、Diamond、Emerald、RedstoneのVanilla鉱石および対応する`deepslate_*_ore`が含まれる。

Action:
Overworldで構造物を生成し、鉱石ブロックIDを確認する。

Expected Result:
本書の鉱石対応表に従って登録済みTFC鉱石に変換される。岩石別鉱石が存在しない場合は旧版のGraniteフォールバック等を使用し、無効なIDを設置しない。

##### AC-F007-004: Nether固有鉱石を維持する

Preconditions:
Netherの対象構造物に `nether_gold_ore`、`nether_quartz_ore`、`ancient_debris` がある。

Action:
構造物を生成する。

Expected Result:
これらが本MODによって変換されない。Beneath鉱石へ置換する経路が残らない。

##### AC-F007-005: 額縁・防具立ての装備を置換する

Preconditions:
OverworldまたはNetherの対象テンプレートに `item_frame`、`glow_item_frame`、`armor_stand` があり、Vanilla装備を保持している。

Action:
構造物を配置し、エンティティが保持するアイテムを確認する。

Expected Result:
旧版のWrought Iron系の武器・工具・防具への置換を継承する。置換時に不適合な元アイテム用コンポーネントを除去する。Endでは置換を行わない。

### F-008: YUNG構造物生成とプロセッサ適用

#### Description

旧版の12件のバイオームタグ追加とStructure Template用プロセッサ、Better MineshaftsおよびBetter Dungeons Spider DungeonのJava直接配置用Mixinを維持する。対象名前空間は `betterstrongholds`、`betterdungeons`、`betteroceanmonuments`、`betterfortresses`。BeneathとVanilla End Cityの注入を削除する。Nether Fortressの独自生成処理および`structure_set`の上書きは追加しない。

#### Acceptance Criteria

##### AC-F008-001: バイオームタグを継承する

Preconditions:
TFCと対象YUNG MODを導入し、各構造物のバイオームタグを読み込める。

Action:
ゲームを起動し、Better Dungeons4種、Better Strongholds1種、Better Ocean Monuments1種、Better Mineshafts6種のタグを確認する。

Expected Result:
合計12件の`replace:false`タグ追加が維持される。TFCの対象バイオームで構造物を生成でき、依存MODが定義する元のバイオームは削除されない。

##### AC-F008-002: MineshaftとSpider Dungeonの直接配置を変換する

Preconditions:
Better MineshaftsおよびBetter DungeonsのSpider Dungeonが導入されている。

Action:
Mineshaftの支柱・通路・Dungeon Chest、およびSpider DungeonのJava `StructurePiece` 生成部分を配置する。

Expected Result:
テンプレートだけでなく、対象Java `StructurePiece`から直接配置される石材・木材・設備にも本書の変換が適用される。MineshaftのChest・Chest Minecart・Barrelが参照する `minecraft:chests/abandoned_mineshaft` の戦利品設定が維持される。

##### AC-F008-003: Better Fortresses側の生成を使用する

Preconditions:
Better Fortressesが導入されたNetherワールドがある。

Action:
Better Fortressesの構造物を生成する。

Expected Result:
YUNG's Better Fortressesが生成した構造物に本MODのブロック変換が適用される。本MODがNether Fortressの重複する構造物定義や`structure_set`を追加しない。

##### AC-F008-004: BeneathとEnd Cityへの注入を削除する

Preconditions:
Beneath未導入環境で、各YUNGテンプレートおよびVanilla End Cityテンプレートを配置できる。

Action:
対象テンプレートの適用プロセッサと生成結果を確認する。

Expected Result:
YUNG対象テンプレートでは既存プロセッサの後に本MODのプロセッサが一度だけ追加される。`beneath:*`と`minecraft:end_city/*`には追加されない。

### F-009: 戦利品テーブルとOverdimsへの委譲

#### Description

元のloot tableを上書きせず、プール数、rolls、bonus_rolls、entryの重み・条件・個数・品質・関数による抽選結果を維持する。旧版の対象28件のYUNGテーブルと5件のVanilla名義テーブルには、NeoForgeグローバル戦利品修飾子で抽選済みアイテムだけを置換し、装備カテゴリと金属tierを維持する。旧版90ファイルの上書きと55件の共通テーブルは提供しない。`minecraft:chests/nether_bridge` と `minecraft:chests/end_city_treasure` には本MODの修飾子を置かず、OverdimsのNeoForgeグローバル戦利品修飾子へ委ねる。

#### Acceptance Criteria

##### AC-F009-001: YUNG構造物の割り当てを継承する

Preconditions:
指定した旧版と対応するYUNG依存MODのMinecraft 1.21.1向け戦利品テーブルが参照できる。

Action:
Better Dungeons8件、Better Fortresses9件、Better Strongholds10件、Better Ocean Monuments1件のテーブルを抽選する。

Expected Result:
対象28件で元テーブルのプール・抽選回数・エントリー重み・条件・個数による結果を維持し、旧版のTFC置換カテゴリ・tierで抽選済みアイテムを置換する。Overdimsへ委譲する2件以外の既存戦利品を意図せず変更しない。

##### AC-F009-002: Vanilla名義の上書きを整理する

Preconditions:
旧版の `data/minecraft/loot_table/chests` および本MODの共通loot tableがある。

Action:
v1.0のデータを読み込み、同名上書きを列挙する。

Expected Result:
本MODはloot tableを上書きしない。`abandoned_mineshaft`、`simple_dungeon`、`stronghold_corridor`、`stronghold_crossing`、`stronghold_library` の5件とYUNG28件に本MODの修飾子が1件ずつあり、合計33件となる。Nether BridgeとEnd Cityには本MODの修飾子がない。

##### AC-F009-003: Overdimsの戦利品処理と重複しない

Preconditions:
Overdimsを導入し、Vanilla Nether BridgeとEnd Cityの戦利品を生成できる。

Action:
各戦利品テーブルを実行し、適用されるテーブル上書きとNeoForgeグローバル戦利品修飾子を確認する。

Expected Result:
本MODの古い2件のJSON上書きが介入せず、Overdimsの対応する修飾子が既定どおりに適用される。Overdimsの処理結果が旧版の抽選結果と完全一致することは要求しない。

##### AC-F009-004: データ生成が委譲対象を復活させない

Preconditions:
Minecraft 1.21.1と各YUNG MODの元JAR、TFCのJARを用意する。

Action:
`./gradlew runData` を実行する。

Expected Result:
各元MODのオリジナルloot tableからエンチャント関数を読み取って修飾子が生成され、追跡中の生成物と差分がない。loot tableと委譲した2件の修飾子を生成しない。

### F-010: 描画と回帰動作

#### Description

構造物が自然なTFC/Overdimsブロックで生成され、階段・ドア・看板・Firepit・植物等が正しい状態で表示されることを検証する。新規の専用テクスチャ、独自GUI、不要な設定画面を追加しない。

#### Acceptance Criteria

##### AC-F010-001: 構造物の見た目を維持する

Preconditions:
MinecraftクライアントでTFC、Overdimsおよび対象YUNG MODを有効にし、OverworldのMineshaft・Stronghold・Ocean MonumentとNetherのFortressを生成している。

Action:
各構造物の主要な石材・木材・設備・植物を実際に描画して確認する。

Expected Result:
置換済みブロックにmissing texture、モデル崩れ、明らかな向きの不整合、浮遊した上段植物が発生しない。Vanilla維持が指定されたブロックを変更しない。

##### AC-F010-002: 保存と再読み込み後の状態を維持する

Preconditions:
本MODで変換済みの構造物がOverworldとNetherに存在し、チェスト・Barrel・Firepit・ドア等を含む。

Action:
ワールドを保存して終了し、再起動して同一構造物を読み込む。

Expected Result:
変換後のブロックID、合法な向き、Barrelの戦利品テーブル、変換対象外のCauldron状態が保持される。ブロックエンティティの読み込みによりクラッシュしない。ChestのNBTについて本MOD独自の移行処理や追加の互換性保証を要求しない。

## Global Requirements

### 根拠の優先順位

1. **v1.0の要求仕様は本書が最優先。** 実装担当者に会話履歴は提供されない。本書にない仕様を想像で追加してはならない。
2. **旧版から継承すると記した動作は、公開タグ `v0.4.3` の実際のJavaコードとJSONを基準とする。** 古い`AGENTS.md`、`PROJECT.md`、README、コードコメントは意図を調べる参考資料であり、実装を証明する資料ではない。
3. **Overdimsの仕様は対象バージョンの実際のコード、レジストリ登録、JSONで確認する。** 確認済みの基準は `v0.1.4`。後続の1.21.1対応版を使う際は差分を検証する。
4. **YUNG側はMinecraft 1.21.1対応ソースおよび利用する公開JARを検証する。** 古いテンプレート名、Mixin注入先、Java `StructurePiece` の変更に注意する。

### 参照リポジトリ

- 旧MOD：<https://github.com/lau-mods/YUNGs-better-TFC/tree/v0.4.3>
- 旧版の主要ブロック変換実装：<https://github.com/lau-mods/YUNGs-better-TFC/blob/v0.4.3/src/main/java/net/claustra01/yungsbettertfc/world/processor/TfcBlockReplacementProcessor.java>
- 旧版テンプレートMixin：<https://github.com/lau-mods/YUNGs-better-TFC/blob/v0.4.3/src/main/java/net/claustra01/yungsbettertfc/mixin/StructureTemplateMixin.java>
- 旧版Mineshaft直接配置Mixin：<https://github.com/lau-mods/YUNGs-better-TFC/blob/v0.4.3/src/main/java/net/claustra01/yungsbettertfc/mixin/BetterMineshaftStructurePieceMixin.java>
- 旧版戦利品再生成スクリプト：<https://github.com/lau-mods/YUNGs-better-TFC/blob/v0.4.3/scripts/regenerate_loot_tables.py>
- TFC Overdims：<https://github.com/lau-mods/TFC-Overdims/tree/v0.1.4>
- Overdims木材登録：<https://github.com/lau-mods/TFC-Overdims/blob/v0.1.4/src/main/java/net/claustra01/tfcoverdims/common/wood/OverdimsWoodBlocks.java>
- Overdims戦利品修飾子：<https://github.com/lau-mods/TFC-Overdims/blob/v0.1.4/src/main/java/net/claustra01/tfcoverdims/loot/TfcLootReplacementModifier.java>
- Overdims戦利品変換規則：<https://github.com/lau-mods/TFC-Overdims/blob/v0.1.4/src/main/java/net/claustra01/tfcoverdims/loot/TfcLootRules.java>
- Better Dungeons：<https://github.com/YUNG-GANG/YUNGs-Better-Dungeons/tree/1.21.1>
- Better Mineshafts：<https://github.com/YUNG-GANG/YUNGs-Better-Mineshafts/tree/1.21.1>
- Better Strongholds：<https://github.com/YUNG-GANG/YUNGs-Better-Strongholds/tree/1.21.1>
- Better Ocean Monuments：<https://github.com/YUNG-GANG/YUNGs-Better-Ocean-Monuments/tree/1.21.1>
- Better Fortresses：<https://github.com/YUNG-GANG/YUNGs-Better-Fortresses/tree/1.21.1>
- Harnessテンプレート：<https://github.com/lau-mods/modding-harness/blob/main/templates/PROJECT.md>

### 共通のブロック変換規則

- 本MODの通常の入力対象は `minecraft:` 名前空間のブロックのみ。`tfc:`、`tfcoverdims:`、他MODのブロックを再変換しない。Airはスキップする。
- 出力先のブロックIDはMinecraft/NeoForgeのレジストリで検証する。存在しない場合、Airへ置き換えず元ブロックを維持する。
- 元BlockStateのプロパティと同名・同値で設定可能なプロパティだけをコピーする。設定不可能な値を強制せず、出力ブロックの既定値を使用する。
- Overworldでは配置基準位置から下方向に最大64ブロック調べ、TFC岩石と土壌を検出し、配置基準位置ごとに記憶する。岩石不明時は `granite`、土壌不明時は `mollisol`、木材種不明時は `oak`。
- NetherのOverworld由来岩石では `<rock> = basalt`、土壌では `<soil> = oxisol` を固定使用する。Nether固有ブロックまで一律Basaltに変えてはならない。
- `infested_*` の石材は `infested_` を除いた通常石材として処理する。
- 旧版の`final_state` NBT中のVanilla鉱石IDの置換を継承する。ただしNether固有鉱石の除外規則を優先する。
- FULL/UTILITY_ONLYという旧版のディメンション条件だけでNetherを処理しない。Overworld/Nether/Endの条件と個別除外対象を明示的に実装する。

### 岩石と石材の完全対応表

`<rock>`はOverworldで検出したTFC岩石種、Netherでは`basalt`である。

| 元ブロック名・系列 | 変換先 | 備考 |
| --- | --- | --- |
| `stone`、`andesite`、`granite`、`diorite`、`tuff` | `tfc:rock/raw/<rock>` | Granite/Diorite/Tuffはv1追加 |
| 上記の`*_stairs`、`*_slab`、`*_wall` | `tfc:rock/raw/<rock>_stairs`等 | 実在するバニラ形状のみ |
| `polished_andesite`、`polished_granite`、`polished_diorite`、`polished_tuff` | `tfc:rock/smooth/<rock>` | 研磨石 |
| 上記の研磨石`*_stairs`、`*_slab` | `tfc:rock/smooth/<rock>_stairs`等 | 実在する形状のみ |
| `cobblestone` | `tfc:rock/cobble/<rock>` | 継承 |
| `mossy_cobblestone` | `tfc:rock/mossy_cobble/<rock>` | 継承 |
| 上記丸石の階段・ハーフブロック・壁 | 対応する`cobble`/`mossy_cobble`形状 | 継承 |
| `stone_bricks`、`tuff_bricks` | `tfc:rock/bricks/<rock>` | Tuff Bricks追加 |
| `stone_brick_stairs/slab/wall`、`tuff_brick_stairs/slab/wall` | 対応する`bricks`形状 | 継承・追加 |
| `mossy_stone_bricks`とその階段・ハーフブロック・壁 | `tfc:rock/mossy_bricks/<rock>`系 | 継承 |
| `cracked_stone_bricks` | `tfc:rock/cracked_bricks/<rock>` | 継承 |
| `chiseled_stone_bricks`、`chiseled_tuff`、`chiseled_tuff_bricks` | `tfc:rock/chiseled/<rock>` | Tuff形状追加 |
| `smooth_stone`、`smooth_stone_slab` | `tfc:rock/smooth/<rock>`系 | 継承 |
| `gravel` | `tfc:rock/gravel/<rock>` | NetherではBasalt |
| `stone_button` | `tfc:rock/button/<rock>` | 継承 |
| `stone_pressure_plate` | `tfc:rock/pressure_plate/<rock>` | 継承 |
| Deepslate系・Blackstone系の一般建材 | 変換しない | 鉱石は別表 |

出力候補が `tfc:rock/raw/<rock>`、`tfc:rock/cobble/<rock>`、`tfc:rock/mossy_cobble/<rock>` の**フルブロック**で、置換位置の真下が空気の場合だけ `tfc:rock/hardened/<rock>` を使う。階段・ハーフブロック・壁には適用しない。これ以外の支持力判定を加えない。

Netherの `minecraft:netherrack` は通常Vanillaのまま維持し、対象構造物内で真下が空気の場合だけ `tfcoverdims:hardened_netherrack` にする。OverdimsのHardenUndersideFeatureは構造物範囲を除外するため、構造物内でのこの硬化処理が必要である。Blackstoneは変換しないが、Overdims自身の崩落タグ・挙動は変更しない。

### 砂岩と土壌の完全対応表

| 元ブロック | 出力 |
| --- | --- |
| `sandstone`、`sandstone_stairs`、`sandstone_slab`、`sandstone_wall` | `tfc:raw_sandstone/yellow`と対応形状 |
| `cut_sandstone`、`chiseled_sandstone`、`cut_sandstone_slab` | `tfc:cut_sandstone/yellow`と対応形状 |
| `smooth_sandstone`、`smooth_sandstone_stairs`、`smooth_sandstone_slab` | `tfc:smooth_sandstone/yellow`と対応形状 |
| 上記のRed Sandstone系列 | 対応する`red`系列 |
| `dirt` | `tfc:dirt/<soil>` |
| `coarse_dirt` | `tfc:coarse_dirt/<soil>` |
| `grass_block` | `tfc:grass/<soil>` |
| `rooted_dirt` | `tfc:rooted_dirt/<soil>` |
| `farmland` | `tfc:farmland/<soil>` |
| `dirt_path` | `tfc:grass_path/<soil>`。旧版の古い入力名`grass_path`を修正する |
| `sand` | `tfc:sand/yellow` |
| `red_sand` | `tfc:sand/red` |

Netherでも砂岩の赤・黄、砂の赤・黄を保持し、これらをBasalt石材へ一律変換しない。`<soil>`はNetherでは常に`oxisol`である。

### Overworld木材の完全対応表

| Vanilla木材種 | TFC木材種 |
| --- | --- |
| `oak` | `oak` |
| `spruce` | `spruce` |
| `birch` | `birch` |
| `jungle` | `acacia` |
| `acacia` | `acacia` |
| `dark_oak` | `oak` |
| `mangrove` | `mangrove` |
| `cherry` | `oak` |
| `bamboo` | `oak` |

対象となる形状はPlanks・Stairs・Slab、Log・Wood、Stripped Log・Stripped Wood、Fence・Fence Gate、Door・Trapdoor、Button・Pressure Plate、Sign・Wall Signとする。`<wood>_planks`は`tfc:wood/planks/<wood>`の形式、その他は`tfc:wood/<block_type>/<wood>`の形式を基準とし、具体的な登録名は旧コードで確認する。木材種は明示された元のIDから決定し、種が名前に含まれないChest等だけを木材ヒントから決定する。旧版の`dark_oak`を先頭の`_`で切り詰める検出不具合を修正する。

### Nether木材の完全対応表

`<nether_wood>`は`crimson`または`warped`。

| 元ブロック | 出力 |
| --- | --- |
| `<nether_wood>_planks` | `tfcoverdims:wood/planks/<nether_wood>` |
| `<nether_wood>_stairs` | `tfcoverdims:wood/planks/<nether_wood>_stairs` |
| `<nether_wood>_slab` | `tfcoverdims:wood/planks/<nether_wood>_slab` |
| `<nether_wood>_stem` | `tfcoverdims:wood/log/<nether_wood>` |
| `<nether_wood>_hyphae` | `tfcoverdims:wood/wood/<nether_wood>` |
| `stripped_<nether_wood>_stem` | `tfcoverdims:wood/stripped_log/<nether_wood>` |
| `stripped_<nether_wood>_hyphae` | `tfcoverdims:wood/stripped_wood/<nether_wood>` |
| `<nether_wood>_fence`、`_fence_gate` | `tfcoverdims:wood/fence/<nether_wood>`、`fence_gate`系 |
| `<nether_wood>_door`、`_trapdoor` | `tfcoverdims:wood/door/<nether_wood>`、`trapdoor`系 |
| `<nether_wood>_button`、`_pressure_plate` | 対応するOverdims木材 |
| `<nether_wood>_sign`、`_wall_sign` | 対応するOverdims木材 |
| `<nether_wood>_hanging_sign`、`_wall_hanging_sign` | 変換しない。Overdims v0.1.4に対応木材ブロックなし |

Crimson/Warped Fungus、Nylium、Roots、Vinesなど、木材ではないNether植生への新規変換を追加しない。Overdims自身の成長・土壌・気候機能へ委ねる。ネザーに存在するOak等のOverworld木材は、上記のTFC木材表に従う。

### 設備・光源・装飾・金属の対応表

| 元ブロック | 出力・動作 |
| --- | --- |
| `furnace`、`blast_furnace`、`smoker` | `tfc:firepit`、元のBlock Entity NBTを破棄。水平向きから設定可能なFirepitの軸を継承 |
| `chest`、`trapped_chest` | 木材ヒントで選択したTFC/Overdimsの対応木材チェスト。追加のNBT互換性検証を行わない |
| `lectern` | 木材ヒントに対応するTFC/OverdimsのLectern |
| `crafting_table` | 木材ヒントに対応するTFC/OverdimsのWorkbench |
| `cauldron`、`water_cauldron`、`lava_cauldron`、`powder_snow_cauldron` | 変換しない。内容物・水位・機能を保持 |
| `barrel` | 変換しない。元のloot table設定を保持 |
| `torch`、`wall_torch` | `tfc:torch`、`tfc:wall_torch` |
| `candle`、各色`*_candle` | `tfc:candle`、登録済みの対応色Candles |
| `candle_cake`、各色`*_candle_cake` | 登録済みの対応TFC Cake/Candle Blockがあれば変換 |
| `lantern`、`soul_lantern` | 変換しない |
| `campfire`、`soul_campfire` | 変換しない |
| `soul_torch`、`soul_wall_torch` | 旧版どおり変換しない |
| `iron_bars` | `tfc:metal/bars/wrought_iron` |
| `chain` | `tfc:metal/chain/wrought_iron` |
| `iron_block` | `tfc:metal/block/wrought_iron` |
| `iron_trapdoor` | `tfc:metal/trapdoor/wrought_iron` |
| `gold_block`、`raw_gold_block` | `tfc:metal/block/gold` |
| `copper_block`、`cut_copper`と対応形状 | `tfc:metal/block/copper`と形状違い |
| `exposed_*`、`weathered_*`、`oxidized_*`の旧版対応Copper系列 | `tfc:metal/exposed_block/copper`、`weathered_block`、`oxidized_block`の対応形状 |
| `coal_block` | 旧版のNether用変換を維持し、`tfc:bituminous_coal`。Overworldに新しい変換を追加しない |
| Anvil系、Smithing Table、Stonecutter、Spawner、Rail、Bed | 変換しない。Stonecutterはv1.0の対象外 |

### 植物の対応表

旧版 `v0.4.3` の実装を継承する。記載のない草、シダ、キノコ、花、葉、苗木などは変換しない。

| 元ブロック | 出力・処理 |
| --- | --- |
| `kelp`、`kelp_plant` | `minecraft:water` |
| `seagrass` | `tfc:plant/eel_grass` |
| `tall_seagrass`の下半分 | `tfc:plant/eel_grass` |
| `tall_seagrass`の上半分 | `minecraft:water` |
| `sea_pickle` | `tfc:sea_pickle` |
| `vine` | `tfc:plant/ivy` |
| `cactus` | 基部を`tfc:plant/silken_pincushion_cactus`、上段を旧版規則に沿ってAirにする |
| `dead_bush` | `tfc:plant/dead_bush` |
| `potted_*` | `tfc:plant/potted/<plant>`が登録済みの場合のみ変換 |

### 鉱石の対応表

| 元ブロック | 出力 |
| --- | --- |
| `coal_ore`、`deepslate_coal_ore` | 最初に存在する`tfc:lignite`または`tfc:bituminous_coal` |
| `iron_ore`、`deepslate_iron_ore` | `tfc:ore/normal_hematite/<rock>`、代替`normal_magnetite`、`normal_limonite` |
| `copper_ore`、`deepslate_copper_ore` | `tfc:ore/normal_native_copper/<rock>` |
| `gold_ore`、`deepslate_gold_ore` | `tfc:ore/normal_native_gold/<rock>` |
| `lapis_ore`、`deepslate_lapis_ore` | `tfc:ore/lapis_lazuli/<rock>` |
| `diamond_ore`、`deepslate_diamond_ore` | `tfc:ore/diamond/<rock>` |
| `emerald_ore`、`deepslate_emerald_ore` | `tfc:ore/emerald/<rock>` |
| `redstone_ore`、`deepslate_redstone_ore` | `tfc:ore/cinnabar/<rock>` |
| `nether_gold_ore`、`nether_quartz_ore`、`ancient_debris` | 本MODでは変換しない |

鉱石選択では旧版のレジストリ存在判定とGranite岩石フォールバックを継承する。Nether固有鉱石は、旧版に変換規則が存在してもv1.0では例外として維持する。

### 展示エンティティ

対象テンプレート内のVanilla `item_frame`、`glow_item_frame`の`Item`、`armor_stand`の`ArmorItems`・`HandItems`について、旧版と同様にWrought Iron相当へ置換する。工具、武器、Armor、Shield、Bow/Crossbow/TridentのJavelin代替等の分類を維持する。変換後の装備に不適合な`components`、`tag`、`Damage`、`damage`を除去する。対象外エンティティとEndでは行わない。

### 構造物とバイオームタグ

- Better Dungeons：Skeleton、Small、Spider、Zombieの4種類にTFC Land/Coastalタグを追加する。Small Nether DungeonはYUNG側のNether構造物として維持する。
- Better Strongholds：Land/Coastalタグを追加する。
- Better Ocean Monuments：`tfc_deep_ocean_biomes`タグを追加する。このタグには`deep_ocean`等だけでなく`tfc:ocean`と`tfc:ocean_reef`も含む。名称から深海限定と判断しない。
- Better Mineshafts：Oak、Desert、Red Desert、Mesa、Ice、Spruce Snowyの6種類に対応するTFCバイオームタグを追加する。その他のvariantを新たに有効化しない。
- 合計12件のタグ追加を`replace:false`で維持する。`structure_set`上書きは作成しない。
- Better Fortresses：YUNG側の`yungsapi:yung_jigsaw`を使用する。独自のFortress生成処理を追加しない。
- `StructureTemplateMixin`では対象YUNG名前空間にのみプロセッサを末尾へ一回追加する。BeneathとVanilla End Cityの名前空間・テンプレートを対象から除外する。
- Better MineshaftsとSpider DungeonのJava `StructurePiece`配置、Mineshaftの直接`WorldGenLevel#setBlock`経路を旧版どおり対象とする。Chest Minecart/Barrel/Dungeon Chestは`minecraft:chests/abandoned_mineshaft`を利用する。

### 戦利品の責務分担

旧版の`src/main/resources/data`には戦利品テーブルJSONが計90件ある。v1.0ではこれらを上書きせず、以下の対象に修飾子を置く。

| 名前空間 | v1.0の修飾子数 | 取り扱い |
| --- | ---: | --- |
| `betterdungeons` | 8 | 旧版の割り当てを維持 |
| `betterfortresses` | 9 | 旧版の割り当てを維持 |
| `betteroceanmonuments` | 1 | 旧版と同じ、依存元の内容を維持 |
| `betterstrongholds` | 10 | 旧版の割り当てを維持 |
| `minecraft` | 5 | Mineshaft、Simple Dungeon、Strongholdの3件の置換を維持 |
| `yungsbettertfc` | 0 | 共通テーブルを廃止し、修飾子内で抽選 |
| **合計** | **33** | 委譲2件と共通55件を除く |

削除する2件は`data/minecraft/loot_table/chests/nether_bridge.json`と`data/minecraft/loot_table/chests/end_city_treasure.json`。これらについて本MODの独自上書きを行わず、Overdims `v0.1.4` が提供するNeoForgeグローバル戦利品修飾子に委譲する。Overdimsは元のloot table JSONを置き換えず、抽選後のアイテムを変換する。委譲対象の抽選結果が旧版と完全一致することは要求しない。

共有テーブルの金属tierは旧版どおり、Ordinaryの一般IngotsがCopper/Bronze/Bismuth Bronze/Black Bronze/Wrought Iron、DangerousがWrought Iron/Steel、LategameがSteel/Black Steel。完成装備は既存のOrdinary/Dangerous/Lategameの制約に従う。Black SteelはLategameの一般Ingot候補以外へ拡張せず、Red/Blue Steelを一般戦利品へ追加しない。

旧版の`scripts/regenerate_loot_tables.py`は廃止し、`./gradlew runData`で修飾子を生成する。元loot tableを上書きしないため、pool数・順序、rolls、bonus_rolls、entry順序、weight、quality、conditions、set_count、エンチャント関数等の既存特性は元MODのものが維持され、変換対象外アイテムは保持される。置換先が元アイテムと同一の場合は元スタックを保持する。Vanilla wheatは`tfc:food/wheat`に置換する。委譲した2件の修飾子は生成しない。

### 共通の検証要件

- Javaコンパイル、NeoForge起動、専用サーバー起動でエラーが発生しない。
- 状態変換・loot・構造物生成・ディメンション除外はNeoForge GameTestで検証し、構造物の描画はクライアントE2Eで確認する。
- Overworld/Nether/End、各YUNG MODの有無、構造物テンプレート配置とJava `StructurePiece`直接配置の両方を検証する。
- 元BlockStateの合法なプロパティだけを継承する。変換先が存在しない場合は元ブロックを保持し、Air化・例外を発生させない。
- ワールド保存・再読み込み後の状態が壊れない。
- ChestのBlock Entity NBTに対する本MOD独自の厳密な互換性検証、独自変換、データ移行は実装しない。既存のTFC/Overdims側の互換性に委ねる。
- 依存MODの後続のMinecraft 1.21.1対応版について、バージョン範囲と実際の互換性を別々に検証する。互換性が確認できていない将来版を無条件に保証したと記載しない。

## Constraints

- MOD IDは`yungsbettertfc`、配布バージョンは`1.0.0`。Minecraftは1.21.1のみ。既存のNeoForge/Java 21プロジェクトを引き継ぐ。旧版タグ`v0.4.3`から改修し、不要な全面新規実装を行わない。
- 本書と旧版`AGENTS.md`等の説明が矛盾する場合、本書が優先する。旧版の実際の挙動を確認する場合はコードとリソースを根拠とし、コメントや文書だけで「実装済み」と判断しない。
- Beneath専用実装・依存・プロセッサ対象・出力IDを残さない。NetherのCrimson/Warped木材はTFC Overdimsの実在する登録ブロックを使う。
- Endについては本MODによるブロック・Block Entity・エンティティ装備の変換を全面停止する。ただしOverdims自身の独立したワールド機能や戦利品処理は変更しない。
- Blackstoneを変換しない。Overdimsの崩落対象タグにBlackstoneが含まれることは既知であり、本MODでは崩落挙動を独自修正しない。
- Cauldron4種類、Barrel、Lantern2種類、Campfire2種類、Stonecutter、Anvil系、Smithing Table、Rail、Bed、Spawnerを変換しない。Stonecutterの変換可否はv1.0の開発対象外であり、将来の検討事項とする。
- Furnace/Blast Furnace/SmokerからFirepitへの変換では元のインベントリやBlock Entity NBTを破棄する。Smokerの変換はv1.0の新規処理である。
- 旧版にない一般植物の変換を追加しない。Overworldの木材置換・金属・鉱石・戦利品の既存対応を不必要に変更しない。
- 新規ブロック、GUI、独自テクスチャ、不要な設定項目を追加しない。既存のMinecraft/NeoForge/TFC/Overdimsの機構とレジストリを優先する。
- `PROJECT.md`とHarness自身の仕様・計画を、実装中の判断で無断変更しない。コード上の差異を発見した場合は本書の完成条件に沿って解決する。仕様変更が必要であれば通常のHarness仕様変更手順を用いる。

## Open Questions

None.
