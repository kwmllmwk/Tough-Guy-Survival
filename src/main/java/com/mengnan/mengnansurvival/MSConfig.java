/*
 * 猛男生存 (Mengnan Survival) —— Minecraft 26.3 / NeoForge 移植版
 *
 * 本文件是自由软件：你可以依据 GNU 通用公共许可证第 3 版（GPL-3.0）的条款
 * 重新分发和/或修改它。完整条款见项目根目录的 LICENSE 文件。
 *
 * 本文件按“原样”分发，不附带任何担保。
 */
package com.mengnan.mengnansurvival;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 本 mod 的全部可调参数。
 * 使用 NeoForge 内置的 ModConfigSpec，游戏内可在「模组列表 -> 猛男生存 -> Config」中直接调整。
 * 修改后保存即生效，无需重启游戏。
 */
public final class MSConfig {

    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    /** 纯配置定义类，不允许实例化。 */
    private MSConfig() {}

    /**
     * 食物回血模式。
     *
     * <p>原版的回血来自 {@code FoodData}，有两条互斥的分支：</p>
     * <pre>
     *   if (饱食度 &gt;= 20 且 饱和度 &gt; 0)  -&gt; 快速回血（每 10 刻，消耗饱和度）
     *   else if (饱食度 &gt;= 18)           -&gt; 自然回血（每 80 刻，消耗饥饿）
     * </pre>
     */
    public enum FoodRegenMode {
        /** 原版行为。 */
        NORMAL,
        /** 只关闭饱和度的快速回血，保留自然回血。 */
        NO_SATURATION,
        /** 关闭所有饥饿 / 饱和度回血，只能靠药水效果回血。 */
        NONE
    }

    public static final class Common {

        public final ModConfigSpec.IntValue fatigueAmplifier;
        public final ModConfigSpec.IntValue fatigueDurationTicks;

        public final ModConfigSpec.DoubleValue projectileHomingRange;
        public final ModConfigSpec.BooleanValue projectilePerfectTracking;
        public final ModConfigSpec.DoubleValue projectileTurnRate;

        public final ModConfigSpec.DoubleValue oreNoDropChance;

        public final ModConfigSpec.DoubleValue infestedStoneChance;
        public final ModConfigSpec.DoubleValue fangChancePerEntry;
        public final ModConfigSpec.IntValue fangCount;
        public final ModConfigSpec.IntValue fangWarmupTicks;
        public final ModConfigSpec.BooleanValue fangLeadTarget;

        public final ModConfigSpec.DoubleValue chargedCreeperConvertChance;
        public final ModConfigSpec.DoubleValue chargedCreeperIgniteRange;
        public final ModConfigSpec.IntValue chargedCreeperInvisibilityTicks;

        public final ModConfigSpec.IntValue phantomsOnSleep;
        public final ModConfigSpec.BooleanValue phantomsEveryNight;

        public final ModConfigSpec.IntValue damageDropCount;
        public final ModConfigSpec.IntValue damageDropCooldownTicks;
        public final ModConfigSpec.IntValue walkDropMinSeconds;
        public final ModConfigSpec.IntValue walkDropMaxSeconds;

        public final ModConfigSpec.IntValue armorSlowAmplifier;
        public final ModConfigSpec.DoubleValue portalBreakChance;
        public final ModConfigSpec.IntValue endermiteCount;
        public final ModConfigSpec.BooleanValue corpseZombieEnabled;

        public final ModConfigSpec.DoubleValue mobArmorChanceMultiplier;
        public final ModConfigSpec.DoubleValue mobArmorEnchantChance;

        public final ModConfigSpec.DoubleValue skeletonTrapMultiplier;
        public final ModConfigSpec.DoubleValue skeletonSpawnMultiplier;

        public final ModConfigSpec.BooleanValue piglinIgnoreGoldArmor;
        public final ModConfigSpec.DoubleValue piglinAngerRange;

        public final ModConfigSpec.DoubleValue illusionerPatrolChance;

        public final ModConfigSpec.DoubleValue pigToHoglinRange;
        public final ModConfigSpec.DoubleValue farmAnimalToRabbitRange;

        public final ModConfigSpec.IntValue hostileStrengthAmplifier;
        public final ModConfigSpec.DoubleValue hostileStrengthRange;
        public final ModConfigSpec.IntValue hostileStrengthDurationTicks;

        public final ModConfigSpec.IntValue drownWitherDurationTicks;
        public final ModConfigSpec.IntValue drownBlindnessDurationTicks;
        public final ModConfigSpec.IntValue drownNauseaDurationTicks;

        public final ModConfigSpec.DoubleValue endermanAngerRange;

        public final ModConfigSpec.BooleanValue lavaBucketIgnites;
        public final ModConfigSpec.DoubleValue waterBucketSpillChance;

        public final ModConfigSpec.IntValue badOmenIntervalTicks;
        public final ModConfigSpec.IntValue badOmenDurationTicks;

        public final ModConfigSpec.BooleanValue lightningTargetsPlayer;
        public final ModConfigSpec.IntValue lightningRodRange;
        public final ModConfigSpec.IntValue lightningIntervalTicks;
        public final ModConfigSpec.BooleanValue thunderWhileRaining;

        public final ModConfigSpec.IntValue fallSlowTicks;
        public final ModConfigSpec.IntValue fallBlindnessTicks;

        public final ModConfigSpec.DoubleValue riderSpawnMultiplier;

        public final ModConfigSpec.IntValue sulfurPoisonDurationTicks;
        public final ModConfigSpec.DoubleValue sulfurDetectRange;

        public final ModConfigSpec.EnumValue<FoodRegenMode> foodRegenMode;

        public final ModConfigSpec.IntValue guardianOceanSpawnWeight;
        public final ModConfigSpec.IntValue guardianOceanSpawnMin;
        public final ModConfigSpec.IntValue guardianOceanSpawnMax;
        public final ModConfigSpec.IntValue guardianHungerDurationTicks;
        public final ModConfigSpec.IntValue guardianHungerAmplifier;

        public final ModConfigSpec.BooleanValue copperTierForIronOre;
        public final ModConfigSpec.BooleanValue giveWelcomeBook;

        Common(ModConfigSpec.Builder b) {
            b.comment("猛男生存 - 通用设置").push("general");

            b.comment("【1】出生/重生时获得的挖掘疲劳").push("fatigue");
            fatigueAmplifier = b.comment("挖掘疲劳的等级（0 = 一级/最弱）")
                    .defineInRange("amplifier", 0, 0, 255);
            fatigueDurationTicks = b.comment("持续时间（游戏刻）。2147483647 视为「近乎无尽」")
                    .defineInRange("durationTicks", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
            b.pop();

            b.comment("【2】箭矢/三叉戟追踪玩家").push("homing");
            projectileHomingRange = b
                    .comment("追踪生效的最大距离（格）。范围内所有箭矢与三叉戟的速度方向都会朝玩家转动")
                    .defineInRange("range", 64.0D, 1.0D, 256.0D);
            projectilePerfectTracking = b
                    .comment("false（默认）= 严格转向：每刻只把朝向旋转一个受上限约束的角度，由下面的转速决定，",
                             "弹道会画出可见的弧线逐渐咬住目标；",
                             "true = 每刻直接把方向对准玩家，命中率接近 100%",
                             "两种模式都只改变方向，绝不改变速度大小")
                    .define("perfectTracking", false);
            projectileTurnRate = b
                    .comment("仅在 perfectTracking=false 时生效：每秒最大转向角度（度）。720 = 每刻 36 度")
                    .defineInRange("turnRateDegreesPerSecond", 720.0D, 5.0D, 3600.0D);
            b.pop();

            b.comment("【3】挖掘石头/矿石不掉落").push("oreDrop");
            oreNoDropChance = b.comment("挖掘石头或任意矿石时，不掉落任何物品的概率（0.5 = 50%）")
                    .defineInRange("noDropChance", 0.5D, 0.0D, 1.0D);
            b.pop();

            b.comment("【4】虫蛀的石头与唤魔者尖牙").push("infested");
            infestedStoneChance = b
                    .comment("玩家可触及范围内的石头被替换为虫蛀的石头的概率（0.25 = 四分之一）")
                    .defineInRange("convertChance", 0.25D, 0.0D, 1.0D);
            fangChancePerEntry = b
                    .comment("【每次踏上虫蛀石头】触发尖牙的概率（1.0 = 必定触发）。",
                             "每次「踏上去」只判定一次：站住不动不会被连续夹，",
                             "离开后再踏上来才会重新触发")
                    .defineInRange("fangChancePerEntry", 1.0D, 0.0D, 1.0D);
            fangCount = b.comment("每次触发生成的尖牙数量")
                    .defineInRange("fangCount", 1, 1, 16);
            fangWarmupTicks = b.comment("尖牙的蓄力时长（刻）。原版唤魔者为 20；",
                             "数值越小咬合越快，越难走位躲开")
                    .defineInRange("warmupTicks", 10, 0, 60);
            fangLeadTarget = b.comment("true = 预判玩家走位，把尖牙放在「咬合那一刻玩家会在的位置」，",
                             "这样一边走一边踩上去也会被夹到（推荐）",
                             "false = 固定在虫蛀石头的正中心")
                    .define("leadTarget", true);
            b.pop();

            b.comment("【5】苦力怕变成闪电苦力怕").push("creeper");
            chargedCreeperConvertChance = b
                    .comment("自然生成的苦力怕转化为闪电苦力怕的概率（1.0 = 全部转化）")
                    .defineInRange("convertChance", 1.0D, 0.0D, 1.0D);
            chargedCreeperIgniteRange = b
                    .comment("玩家进入多少格内时闪电苦力怕被点燃（原版苦力怕点燃距离为 3 格）")
                    .defineInRange("igniteRange", 3.0D, 1.0D, 64.0D);
            chargedCreeperInvisibilityTicks = b
                    .comment("闪电苦力怕自带隐身效果的持续时间（72000 刻 = 1 小时）")
                    .defineInRange("invisibilityTicks", 72000, 1, Integer.MAX_VALUE);
            b.pop();

            b.comment("【6】【16】幻翼").push("phantom");
            phantomsOnSleep = b.comment("玩家完成一次睡眠后，头顶生成的幻翼数量")
                    .defineInRange("countOnSleep", 4, 0, 32);
            phantomsEveryNight = b
                    .comment("true = 无论上次睡觉时间，每晚都会正常生成幻翼")
                    .define("spawnEveryNight", true);
            b.pop();

            b.comment("【7】【8】物品掉落").push("itemDrop");
            damageDropCount = b.comment("玩家每次受伤时，掉落的物品格数")
                    .defineInRange("countOnDamage", 1, 0, 64);
            damageDropCooldownTicks = b
                    .comment("两次「受伤掉落」之间的最短间隔（游戏刻），防止着火等持续伤害瞬间清空背包")
                    .defineInRange("damageDropCooldownTicks", 10, 0, 1200);
            walkDropMinSeconds = b.comment("走动时掉落物品的最小间隔（秒）")
                    .defineInRange("walkIntervalMinSeconds", 5, 1, 600);
            walkDropMaxSeconds = b.comment("走动时掉落物品的最大间隔（秒）")
                    .defineInRange("walkIntervalMaxSeconds", 15, 1, 600);
            b.pop();

            b.comment("【9】铁套/钻石套缓慢").push("armorSlow");
            armorSlowAmplifier = b.comment("穿戴铁制或钻石制装备时获得的缓慢等级（0 = 缓慢 I）")
                    .defineInRange("amplifier", 0, 0, 255);
            b.pop();

            b.comment("【10】地狱门损坏").push("portal");
            portalBreakChance = b
                    .comment("玩家穿过地狱门时，地狱门损坏的概率（0.5 = 二分之一）")
                    .defineInRange("breakChance", 0.5D, 0.0D, 1.0D);
            b.pop();

            b.comment("【11】末影人死亡生成末影螨").push("endermite");
            endermiteCount = b.comment("末影人死亡时生成的末影螨数量")
                    .defineInRange("count", 3, 0, 64);
            b.pop();

            b.comment("【12】玩家死亡生成僵尸").push("corpse");
            corpseZombieEnabled = b
                    .comment("玩家死亡时是否在原地生成戴着皮革头盔、以玩家命名的僵尸")
                    .define("enabled", true);
            b.pop();

            b.comment("【13】生物装备强化").push("mobGear");
            mobArmorChanceMultiplier = b
                    .comment("可携带装备的生物生成时，自带装备概率的倍率（1.0 = 原版）")
                    .defineInRange("spawnChanceMultiplier", 3.0D, 0.0D, 100.0D);
            mobArmorEnchantChance = b
                    .comment("自带装备必定附魔的概率（1.0 = 100% 必定附魔）")
                    .defineInRange("enchantChance", 1.0D, 0.0D, 1.0D);
            b.pop();

            b.comment("【14】雷暴骷髅陷阱马 / 骷髅生成").push("thunder");
            skeletonTrapMultiplier = b
                    .comment("雷暴时骷髅陷阱马生成概率的倍率（1.0 = 原版）")
                    .defineInRange("skeletonTrapMultiplier", 8.0D, 0.0D, 1000.0D);
            skeletonSpawnMultiplier = b
                    .comment("雷暴时骷髅生成概率的倍率（0.25 = 降低到四分之一）")
                    .defineInRange("skeletonSpawnMultiplier", 0.25D, 0.0D, 100.0D);
            b.pop();

            b.comment("【15】猪灵无视金装").push("piglin");
            piglinIgnoreGoldArmor = b
                    .comment("true = 即使玩家穿戴金制装备，猪灵依然会攻击玩家")
                    .define("ignoreGoldArmor", true);
            piglinAngerRange = b.comment("猪灵在多远范围内会主动把玩家设为攻击目标")
                    .defineInRange("angerRange", 16.0D, 1.0D, 64.0D);
            b.pop();

            b.comment("【17】幻术师加入灾厄巡逻队").push("illusioner");
            illusionerPatrolChance = b
                    .comment("灾厄巡逻队生成时，额外加入幻术师的概率")
                    .defineInRange("patrolChance", 1.0D, 0.0D, 1.0D);
            b.pop();

            b.comment("【18】【19】靠近变形").push("transform");
            pigToHoglinRange = b.comment("玩家进入多少格内时，猪会变成疣猪兽")
                    .defineInRange("pigToHoglinRange", 8.0D, 0.5D, 64.0D);
            farmAnimalToRabbitRange = b
                    .comment("玩家进入多少格内时，鸡、牛、羊会变成兔子")
                    .defineInRange("farmAnimalToRabbitRange", 8.0D, 0.5D, 64.0D);
            b.pop();

            b.comment("【21】敌对生物力量").push("hostile");
            hostileStrengthAmplifier = b.comment("敌对生物获得的力量等级（0 = 力量 I）")
                    .defineInRange("strengthAmplifier", 0, 0, 255);
            hostileStrengthRange = b
                    .comment("玩家周围多少格范围内的敌对生物会获得力量")
                    .defineInRange("strengthRange", 64.0D, 1.0D, 256.0D);
            hostileStrengthDurationTicks = b
                    .comment("力量的持续时间（72000 刻 = 1 小时），会被持续刷新")
                    .defineInRange("strengthDurationTicks", 72000, 20, Integer.MAX_VALUE);
            b.pop();

            b.comment("【22】窒息惩罚").push("drown");
            drownWitherDurationTicks = b.comment("因氧气耗尽窒息时获得的凋零持续时间（游戏刻）")
                    .defineInRange("witherDurationTicks", 200, 1, 72000);
            drownBlindnessDurationTicks = b.comment("因氧气耗尽窒息时获得的失明持续时间（游戏刻）")
                    .defineInRange("blindnessDurationTicks", 200, 1, 72000);
            drownNauseaDurationTicks = b.comment("因氧气耗尽窒息时获得的反胃持续时间（游戏刻）")
                    .defineInRange("nauseaDurationTicks", 200, 1, 72000);
            b.pop();

            b.comment("【23】末影人激怒距离").push("enderman");
            endermanAngerRange = b
                    .comment("玩家与末影人距离小于多少格时直接激怒末影人")
                    .defineInRange("angerRange", 10.0D, 0.5D, 64.0D);
            b.pop();

            b.comment("【24】岩浆桶 / 水桶").push("bucket");
            lavaBucketIgnites = b.comment("手持岩浆桶时是否会被点燃")
                    .define("lavaBucketIgnites", true);
            waterBucketSpillChance = b
                    .comment("背包（含快捷栏）中带有水桶且玩家移动时，每刻倒出水并把该水桶变成空桶的概率")
                    .defineInRange("waterSpillChancePerTick", 0.004D, 0.0D, 1.0D);
            b.pop();

            b.comment("【25】试炼密室不祥之兆").push("trialChamber");
            badOmenIntervalTicks = b.comment("在试炼密室中，每隔多少刻获得一次不祥之兆（180 刻 = 9 秒）")
                    .defineInRange("intervalTicks", 180, 1, 72000);
            badOmenDurationTicks = b.comment("不祥之兆的持续时间（200 刻 = 10 秒）")
                    .defineInRange("durationTicks", 200, 1, 72000);
            b.pop();

            b.comment("【26】闪电必定劈玩家").push("lightning");
            lightningTargetsPlayer = b
                    .comment("雷暴天气时，只要附近没有有效避雷针，闪电就必定劈向玩家",
                             "（不再要求玩家头顶露天，因此地下或屋内也会被劈）")
                    .define("targetPlayer", true);
            lightningRodRange = b
                    .comment("避雷针的生效半径（格）。原版为 128 格且避雷针需位于地表；",
                             "该范围内存在避雷针时，闪电不会被引向玩家")
                    .defineInRange("rodRange", 128, 8, 512);
            lightningIntervalTicks = b
                    .comment("雷暴期间每隔多少刻主动在玩家头顶落下一道雷（100 刻 = 5 秒）。",
                             "设为 0 表示只在自然生成的雷上做转向，不主动落雷")
                    .defineInRange("strikeIntervalTicks", 100, 0, 72000);
            b.pop();

            b.comment("【33】雨天必定雷暴").push("weather");
            thunderWhileRaining = b
                    .comment("true = 只要正在下雨，就必定同时是雷暴天气")
                    .define("thunderWhileRaining", true);
            b.pop();

            b.comment("【27】摔落伤害惩罚").push("fall");
            fallSlowTicks = b.comment("摔落伤害大于 1 点时施加的缓慢 I 持续时间（400 刻 = 20 秒）")
                    .defineInRange("slowTicks", 400, 1, 72000);
            fallBlindnessTicks = b.comment("摔落伤害大于 1 点时施加的失明 I 持续时间（200 刻 = 10 秒）")
                    .defineInRange("blindnessTicks", 200, 1, 72000);
            b.pop();

            b.comment("【28】骑手生成倍率（骆驼尸壳骑士 / 僵尸鹦鹉螺骑士等）").push("rider");
            riderSpawnMultiplier = b
                    .comment("骑手类生物生成概率的倍率（4.0 = 4 倍）")
                    .defineInRange("multiplier", 4.0D, 0.0D, 1000.0D);
            b.pop();

            b.comment("【29】硫磺池惩罚").push("sulfur");
            sulfurPoisonDurationTicks = b
                    .comment("受硫磺池反胃效果影响时，额外施加的中毒持续时间（100 刻 = 5 秒）")
                    .defineInRange("poisonDurationTicks", 100, 1, 72000);
            sulfurDetectRange = b
                    .comment("判定「处于硫磺池」的检测半径（格）")
                    .defineInRange("detectRange", 4.0D, 1.0D, 16.0D);
            b.pop();

            b.comment("【30】食物回血（饥饿 / 饱和度）").push("food");
            foodRegenMode = b
                    .comment("NORMAL        = 原版行为（饱和度快速回血 + 自然回血）",
                             "NO_SATURATION = 只关闭饱和度的快速回血，保留自然回血",
                             "NONE          = 关闭所有由饱和度与饱食度产生的回血（默认），",
                             "                玩家只能靠药水效果（再生、瞬间治疗等）回血")
                    .defineEnum("regenMode", FoodRegenMode.NONE);
            b.pop();

            b.comment("【31】守卫者").push("guardian");
            guardianOceanSpawnWeight = b
                    .comment("守卫者在海洋生物群系自然生成时的权重（0 = 关闭自然生成）")
                    .defineInRange("oceanSpawnWeight", 12, 0, 1000);
            guardianOceanSpawnMin = b.comment("每次生成的最小数量")
                    .defineInRange("oceanSpawnMin", 1, 1, 16);
            guardianOceanSpawnMax = b.comment("每次生成的最大数量")
                    .defineInRange("oceanSpawnMax", 2, 1, 16);
            guardianHungerDurationTicks = b
                    .comment("被守卫者攻击后获得的饥饿持续时间（400 刻 = 20 秒）")
                    .defineInRange("hungerDurationTicks", 400, 1, 72000);
            guardianHungerAmplifier = b
                    .comment("被守卫者攻击后获得的饥饿等级（4 = 饥饿 V）")
                    .defineInRange("hungerAmplifier", 4, 0, 255);
            b.pop();

            b.comment("【32】挖掘等级").push("mining");
            copperTierForIronOre = b
                    .comment("true = 石镐及以下无法采集铁矿石/铜矿石/青金石矿石，必须使用铜镐或更高级的镐子")
                    .define("copperTierForIronOre", true);
            b.pop();

            b.comment("【34】初次进入世界赠书").push("welcomeBook");
            giveWelcomeBook = b
                    .comment("true = 玩家第一次进入世界时，获得一本内容为本模组 README 的成书",
                             "（每个玩家只发一次，之后重进世界不会再发）")
                    .define("enabled", true);
            b.pop();

            b.pop();
        }
    }
}
