# 猛男生存 (Mengnan Survival) — NeoForge 26.3 移植版

把籽岷《猛男生存》数据包的残酷规则移植到 **Minecraft 26.3**。

- **模组 ID**：`mengnansurvival`　**目标**：Minecraft 26.3 / NeoForge `26.3.0.0-beta+`
- **Java**：25（26.3 本身要求）　**前置**：无，配置界面用 NeoForge 内置系统
- 客户端与服务端**都要装**（逻辑在服务端执行）

---

# 一、玩法一览

> 下表是本模组**所有**偏离原版的行为，各项的关键说明已并入表内。
> 「可调」表示该项数值能在游戏内配置界面修改（见第二节）。

| # | 改动 | 具体行为 | 可调 |
|---|---|---|---|
| 1 | 出生 / 重生 | 自带 **1 级挖掘疲劳**，时长近乎永久 | ✅ |
| 2 | 追踪弹射物 | 64 格内所有箭矢与三叉戟持续**朝玩家转向**；只旋转方向、**速率恒定不加速**。默认严格转向（每秒最多 720°），可切换为每刻直接对准（命中率≈100%） | ✅ |
| 3 | 挖石头 / 矿石 | **50%** 概率不掉落 | ✅ |
| 4 | 虫蛀的石头 | 触及范围内 **1/4** 概率把石头变成虫蛀石头；**每次踏上**时从方块中心冒出唤魔者尖牙。踩到即夹、**站着不动不会被连续夹**、离开后再踏上才会再触发；默认预判走位以便走动时也能夹到 | ✅ |
| 5 | 苦力怕 | **100%** 变成闪电苦力怕；玩家靠近 **3 格**即被点燃，且**不会因远离而熄灭** | ✅ |
| 5b | 闪电苦力怕 | 自带 **1 小时隐身**（持续补满） | ✅ |
| 6 | 完成一次睡眠 | 头顶生成 **4 只幻翼**（自带防火）。以「真的睡到天亮」为准，被中途叫醒不算 | ✅ |
| 7 | 受到伤害 | 随机掉落 **1 格**物品；带 **10 刻冷却**，防止着火等持续伤害瞬间清空背包 | ✅ |
| 8 | 行走 / 疾跑 / 跳跃 / 划船 | 每 **5~15 秒**随机掉落 1 格物品（按实际位移判定移动） | ✅ |
| 9 | 穿铁套或钻石套 | 获得**缓慢 I** | ✅ |
| 10 | 穿过地狱门 | **1/2** 概率地狱门损坏（清除传送门方块，保留黑曜石框架） | ✅ |
| 11 | 末影人死亡 | 生成 **3 只末影螨** | ✅ |
| 12 | 玩家死亡 | 原地生成戴**皮革头盔**、以玩家命名的僵尸 | ✅ |
| 13 | 可装备生物生成 | 自带装备概率提高，且**已有装备必定附魔**（不凭空新增装备） | ✅ |
| 14 | 雷暴天气 | 骷髅陷阱马概率提升、骷髅概率下降；并会主动在玩家附近生成骷髅陷阱马 | ✅ |
| 15 | 穿戴金制装备 | 猪灵**依然**主动攻击玩家（绕过原版的金装记忆判定） | ✅ |
| 16 | 夜晚 | 无论上次何时睡觉，幻翼都**必定**正常生成 | ✅ |
| 17 | 灾厄巡逻队 | 额外生成**幻术师** | ✅ |
| 18 | 玩家靠近猪 | 猪变成**疣猪兽**，原掉落物**立即清除** | ✅ |
| 19 | 玩家靠近鸡 / 牛 / 羊 | 变成**兔子**，原掉落物**立即清除** | ✅ |
| 20 | 分解原木 | 只产出 **2 个木板**（由随附的配方文件实现，见第三节） | ❌ |
| 21 | 玩家周围 64 格内的敌对生物 | 获得**力量 I**（1 小时，持续刷新） | ✅ |
| 22 | 氧气耗尽窒息 | 获得**凋零 I + 失明 I + 反胃 I** | ✅ |
| 23 | 与末影人距离 < 10 格 | **直接激怒**末影人 | ✅ |
| 24 | 手持岩浆桶 | 会被**点燃** | ✅ |
| 24b | 背包 / 快捷栏中有水桶 | 移动时按概率**倒出**（脚下生成水源，该水桶变空桶） | ✅ |
| 25 | 进入试炼密室 | 每 **9 秒**获得 **10 秒**不祥之兆 | ✅ |
| 26 | 雷暴天气 | 附近无有效避雷针时**必定劈玩家**：**无视头顶遮挡**（地下/屋内也会被劈），并每 5 秒主动落雷一道 | ✅ |
| 27 | 摔落伤害 > 1 点 | 获得 **20 秒缓慢 I + 10 秒失明 I** | ✅ |
| 28 | 骆驼尸壳骑士 / 僵尸鹦鹉螺骑士 | 生成概率提升到 **4 倍** | ✅ |
| 29 | 身处硫磺池（受其反胃效果影响） | 额外获得 **5 秒中毒** | ✅ |
| 30 | 食物回血 | **默认全部关闭**（饥饿与饱和度都不回血），玩家只能靠**药水效果**回血 | ✅ |
| 31 | 海洋生物群系 | 自然生成**守卫者** | ✅ |
| 31b | 被守卫者攻击 | 获得 **20 秒饥饿 V** | ✅ |
| 32 | 铁 / 铜 / 青金石矿石 | 石镐及以下**无法采集**，必须用**铜镐**或更高级（挖掉但不掉落） | ✅ |
| 33 | 天气 | 只要在下雨，就**必定**是雷暴 | ✅ |
| 34 | 初次进入世界 | 获得一本成书：书名 `README`、作者 `DeepSeek-V4.1-Flash`、内容为本 README；每个玩家只发一次 | ✅ |

### 两点实现细节（想自己改动时看）

- **第 2 项**：每刻读取速度向量并记下**速率**，只把朝向按 Rodrigues 公式旋转一个受限角度，再乘回**原速率**写入 —— 因此速率自始至终不变，弹道是可见的弧线而不是「凭空加速」。
- **第 4 项**：原版尖牙在蓄力结束后还要**固定再等 8 刻**才咬合，原地不动才容易被夹到。因此默认开启预判：把尖牙放在**咬合那一刻玩家将会在的位置**（按速度推算，最多 3 格并校验落点可站立）。设为 `leadTarget=false` 即恢复成固定在方块正中心。

---

# 二、修改概率与数值

改完**保存即生效**，不需要重启游戏。

## 方式 A：游戏内图形界面（推荐）

**模组列表（Mods）** → **猛男生存 (Mengnan Survival)** → **Config / 配置**。
该界面由 NeoForge 自带，**不需要额外前置模组**。

## 方式 B：编辑配置文件

```
<游戏目录>/config/mengnansurvival-common.toml
```

每项都带中文注释，例如：

```toml
[general.oreDrop]
    #挖掘石头或任意矿石时，不掉落任何物品的概率（0.5 = 50%）
    noDropChance = 0.5
```

## 速查表

| # | 配置项 | 默认值 |
|---|---|---|
| 1 | `general.fatigue.amplifier` / `durationTicks` | `0` / `2147483647` |
| 2 | `general.homing.range` / `perfectTracking` / `turnRateDegreesPerSecond` | `64.0` / `false` / `720.0` |
| 3 | `general.oreDrop.noDropChance` | `0.5` |
| 4 | `general.infested.convertChance` / `fangChancePerEntry` / `fangCount` / `warmupTicks` / `leadTarget` | `0.25` / `1.0` / `1` / `10` / `true` |
| 5 | `general.creeper.convertChance` / `igniteRange` / `invisibilityTicks` | `1.0` / `3.0` / `72000` |
| 6 | `general.phantom.countOnSleep` | `4` |
| 7 | `general.itemDrop.countOnDamage` / `damageDropCooldownTicks` | `1` / `10` |
| 8 | `general.itemDrop.walkIntervalMinSeconds` / `MaxSeconds` | `5` / `15` |
| 9 | `general.armorSlow.amplifier` | `0` |
| 10 | `general.portal.breakChance` | `0.5` |
| 11 | `general.endermite.count` | `3` |
| 12 | `general.corpse.enabled` | `true` |
| 13 | `general.mobGear.spawnChanceMultiplier` / `enchantChance` | `3.0` / `1.0` |
| 14 | `general.thunder.skeletonTrapMultiplier` / `skeletonSpawnMultiplier` | `8.0` / `0.25` |
| 15 | `general.piglin.ignoreGoldArmor` / `angerRange` | `true` / `16.0` |
| 16 | `general.phantom.spawnEveryNight` | `true` |
| 17 | `general.illusioner.patrolChance` | `1.0` |
| 18 | `general.transform.pigToHoglinRange` | `8.0` |
| 19 | `general.transform.farmAnimalToRabbitRange` | `8.0` |
| 21 | `general.hostile.strengthAmplifier` / `strengthRange` / `strengthDurationTicks` | `0` / `64.0` / `72000` |
| 22 | `general.drown.witherDurationTicks` / `blindnessDurationTicks` / `nauseaDurationTicks` | `200` / `200` / `200` |
| 23 | `general.enderman.angerRange` | `10.0` |
| 24 | `general.bucket.lavaBucketIgnites` / `waterSpillChancePerTick` | `true` / `0.004` |
| 25 | `general.trialChamber.intervalTicks` / `durationTicks` | `180` / `200` |
| 26 | `general.lightning.targetPlayer` / `rodRange` / `strikeIntervalTicks` | `true` / `128` / `100` |
| 27 | `general.fall.slowTicks` / `blindnessTicks` | `400` / `200` |
| 28 | `general.rider.multiplier` | `4.0` |
| 29 | `general.sulfur.poisonDurationTicks` / `detectRange` | `100` / `4.0` |
| 30 | `general.food.regenMode`（`NORMAL` / `NO_SATURATION` / `NONE`） | `NONE` |
| 31 | `general.guardian.oceanSpawnWeight` / `oceanSpawnMin` / `oceanSpawnMax` | `12` / `1` / `2` |
| 31b | `general.guardian.hungerDurationTicks` / `hungerAmplifier` | `400` / `4` |
| 32 | `general.mining.copperTierForIronOre` | `true` |
| 33 | `general.weather.thunderWhileRaining` | `true` |
| 34 | `general.welcomeBook.enabled` | `true` |

**关闭某项的办法**：概率类设 `0.0`；开关类设 `false`；倍率类设 `1.0`；权重类设 `0`。
**恢复原版**：第 30 项设为 `NORMAL`；第 32 项设为 `false`。

### 第 30 项三种模式

| 取值 | 效果 |
|---|---|
| `NORMAL` | 原版行为（饱和度快速回血 + 自然回血） |
| `NO_SATURATION` | 只关饱和度的快速回血，保留自然回血 |
| `NONE`（默认） | 关掉所有食物回血，只能靠药水 |

判别逻辑：食物回血每次只有 1 点或更少 → **治疗量 > 1 的一律放行**（瞬间治疗药水是 4 点）；
**有再生效果时放行**（再生药水、金苹果靠它）；**饱食度 < 18 时放行**。因此药水回血不受影响。

---

# 三、其余内容

## 安装

1. 装好 **Minecraft 26.3** 与 **NeoForge 26.3.0.23-beta 或更高**；
2. 把 `mengnansurvival-1.0.0.jar` 放进 `.minecraft/mods/`（**服务端也要放**）。

## 第 20 项：原木只出 2 个木板

由 jar 内 **12 个配方文件**实现（`data/minecraft/recipe/*_planks.json`），覆盖全部原木种类，
产出统一为 2。竹子木板来自竹块而非原木，保持原版。

配方是数据驱动的，所以这一项**不在**配置界面里；要改回 4，删掉 jar 内对应的 `data/` 目录即可。

## 第 34 项：赠书的分页

客户端书页**每页最多显示 14 行**（每行 114 像素），超出会被静默截断。
因此模组按**实际像素宽度**分页（中日韩字符按 9 像素、其余 6 像素估算），
确保每页都能完整显示。书页无法渲染 Markdown，所以只做了轻度清理（去掉 `**` 与行首 `#`）。

## 已知限制

- **第 4 项**的虫蛀石头采用每 10 刻随机采样（兼顾性能），不会瞬间铺满极大范围。
- **第 13 项**只强化生物**已有**的装备槽，不为裸装生物凭空新增装备。
- **第 25 项**无法直接识别试炼密室结构时，退化为检测铜类方块。
- **第 28 项**原版骑手概率写死在 `Husk` / `Drowned` 内部，无法改常量，故采用「按倍率补足额外概率」。
- **第 30 项**通过拦截回血实现；若玩家同时拥有再生效果则不拦截，避免误伤药水回血。
- **第 32 项**采用「取消掉落」实现，因此石镐敲铁矿石会像原版工具不匹配时一样**挖掉但不掉落**。
- 第 21、26、31 项涉及较大范围的周期性扫描，在极多玩家 / 极高生物密度下可能有性能开销。

## 技术说明

**为什么生物生成相关逻辑用 `EntityJoinLevelEvent`**：
NeoForge 26.3 中 `FinalizeSpawnEvent` **只在刷怪笼生成生物时触发**，
自然生成的生物不会触发（`Mob.finalizeSpawn` 已不含事件派发）。
因此第 13、14、17、18 项改挂在 `EntityJoinLevelEvent` 上。

**26.3 的若干 API 变更**（便于对照旧教程）：

| 旧写法 | 26.3 写法 |
|---|---|
| `EntityType.ZOMBIE` | `EntityTypes.ZOMBIE` |
| `entity.moveTo(...)` | `entity.snapTo(...)` |
| `level.isDay()` / `getDayTime()` | `isBrightOutside()` / `getOverworldClockTime()` |
| `player.drop(s, b, boolean)` | `player.drop(s, b, Prediction.SERVER_ONLY)` |
| `BlockTags.COAL_ORES` 等 | `BlockTags.ORES` 或 `BlockItemTags.X.block()` |
| `ItemTags.BOWS` | `ItemTags.BOW_ENCHANTABLE` |
| `Blocks.LIGHTNING_ROD` / `EXPOSED_COPPER` | `BlockTags.LIGHTNING_RODS` / `BlockTags.COPPER` |
| `EnderMan` | `Enderman` |

## 代码维护说明

源码中**所有类与所有方法都带中文注释**，重点逻辑（追踪弹射物的旋转公式、
回血分支判别、原版尖牙咬合时序、书页像素分页等）都有详细说明。

```
src/main/java/com/mengnan/mengnansurvival/
├── MengnanSurvival.java    入口：注册配置、配置界面、事件总线
├── MSConfig.java           全部可调参数（含 FoodRegenMode 枚举）
├── PlayerEvents.java       疲劳、掉落、水桶、试炼、硫磺池、回血、幻翼、传送门、死亡、赠书
├── WorldEvents.java        挖掘、虫蛀石头与尖牙、末影螨、装备、骑手、守卫者、变形、力量
├── CombatEvents.java       闪电苦力怕、猪灵、末影人、落雷、雨天雷暴
├── HomingProjectiles.java  追踪弹射物 + 避雷针判定
├── MobGear.java            生物装备附魔强化
└── WelcomeBook.java        README 成书（含分页逻辑）
```

## 构建说明

上游 `NeoForm 2.0.27`（NeoForge 26.3.0.23-beta 依赖）在重编译 Minecraft 源码时**未传递 `--enable-preview`**，
而 26.3 源码使用了 Java 25 的未命名变量语法，因此 `createMinecraftArtifacts` 会失败（上游问题，与模组代码无关）。
可改为直接对着游戏本体 jar 编译：

```
javac -nowarn -proc:none --enable-preview --release 25 \
      -cp "<minecraft-client-patched-26.3.0.23-beta.jar>;<neoforge-26.3.0.23-beta-universal.jar>;<其余依赖>" \
      -d build/classes src/main/java/com/mengnan/mengnansurvival/*.java
```

## 许可

本项目采用 **GNU 通用公共许可证第 3 版（GPL-3.0）**，完整条款见根目录的 [LICENSE](LICENSE) 文件，
许可证全文也随 jar 一起分发。

简单说：

- **可以**自由使用、修改、再分发本模组（包括用于整合包、服务器）。
- **再分发时必须一并提供源代码**（GPL 第 6 条），并且**衍生作品也必须以 GPL 授权**。
- 自己游玩或在自己服务器上使用，**没有任何额外义务**。

> 提示：由于是 GPL，若你把编译好的 jar 发给别人，记得同时附上本项目的源码
> （或提供可获取源码的地址）。
