package org.anti_ad.mc.ipnext.item

import net.minecraftforge.registries.ForgeRegistries
import org.anti_ad.mc.common.Log
import org.anti_ad.mc.common.extensions.ifTrue
import org.anti_ad.mc.common.vanilla.alias.Enchantment
import org.anti_ad.mc.common.vanilla.alias.EnchantmentHelper
import org.anti_ad.mc.common.vanilla.alias.Identifier
import org.anti_ad.mc.common.vanilla.alias.ItemGroup
import org.anti_ad.mc.common.vanilla.alias.Items
import org.anti_ad.mc.common.vanilla.alias.PotionUtil
import org.anti_ad.mc.common.vanilla.alias.Registry
import org.anti_ad.mc.common.vanilla.alias.StatusEffectInstance
import org.anti_ad.mc.common.vanilla.alias.glue.I18n
import org.anti_ad.mc.common.vanilla.alias.items.BucketItem
import org.anti_ad.mc.common.vanilla.alias.items.MilkBucketItem
import org.anti_ad.mc.common.vanilla.alias.items.MushroomStewItem
import org.anti_ad.mc.common.vanilla.alias.items.SuspiciousStewItem
import org.anti_ad.mc.ipnext.ingame.`(getIdentifier)`
import org.anti_ad.mc.ipnext.ingame.`(getRawId)`
import org.anti_ad.mc.common.vanilla.alias.ItemStack as VanillaItemStack

// ============
// vanillamapping code depends on mappings
// ============

fun ItemType.fullItemInfoAsJson(): String {
    var res = "{" + "\n\t\"id\" : \"" + this.itemId+ "\",\n"
    tag?.allKeys?.forEach {
        res += "\t{\n\t\t\"$it\" : ${tag.get(it)}\n},"
    }
    return "$res\n}"
}

fun ItemType.toNamespacedString(): String { // like ItemType.toString() but with namespace
    return itemId + (tag ?: "")
}

inline val ItemType.Companion.EMPTY
    get() = ItemType(Items.AIR,
                     null)

fun ItemType.isEmpty() : Boolean {
    return (item == Items.AIR).ifTrue {
        if (this.tag?.equals(null) == false) Log.warn("Informal item type $this")
    }
}

inline val ItemType.maxCount: Int
    get() = vanillaStack.maxStackSize

inline val ItemType.vanillaStack: VanillaItemStack
    get() = VanillaItemStack(this.item).apply { tag = this@vanillaStack.tag }

fun ItemType.vanillaStackWithCount(count: Int): VanillaItemStack =
    VanillaItemStack(this.item,
                     count).apply { tag = this@vanillaStackWithCount.tag }

inline val ItemType.identifier: Identifier
    get() = Registry.ITEM.`(getIdentifier)`(item)

inline val ItemType.namespace: String
    get() = identifier.namespace

//region ItemType String Relative

inline val ItemType.hasCustomName: Boolean
    get() = vanillaStack.hasCustomHoverName() //hasCustomHoverName() //hasCustomName()
inline val ItemType.customName: String
    get() = if (hasCustomName) displayName else ""
inline val ItemType.displayName: String
    get() = vanillaStack.displayName.string
inline val ItemType.translatedName: String
    get() = I18n.translate(translationKey)
inline val ItemType.itemId: String
    get() = identifier.toString()
inline val ItemType.translationKey: String
    get() = vanillaStack.descriptionId //translationKey //descriptionId
inline val ItemType.isStackable: Boolean
    get() = vanillaStack.isStackable

inline val ItemType.customOrTranslatedName: String
    get() = if (hasCustomName) displayName else I18n.translate(translationKey)

//endregion

//region ItemType Number Relative

inline val ItemType.groupIndex: Int
    get() = item.itemCategory?.id  ?: when { //itemCategory?.id //group?.index
        item === Items.ENCHANTED_BOOK -> ItemGroup.TAB_TOOLS.id //TOOLS.index // TAB_TOOLS.id
        namespace == "minecraft" -> ItemGroup.TAB_MISC.id  //MISC.index // TAB_MISC.id
        else -> ItemGroup.TABS.size // GROUPS.size // TABS.size
    }
inline val ItemType.rawId: Int
    get() = Registry.ITEM.`(getRawId)`(item)
inline val ItemType.damage: Int
    get() = vanillaStack.damageValue //damage // .damageValue
inline val ItemType.enchantmentsScore: Double
    //  get() = EnchantmentHelper.get(vanillaStack).toList().fold(0.0) { acc, (enchantment, level) ->
    get() = EnchantmentHelper.getEnchantments(vanillaStack).toList()
        .fold(0.0) { acc, (enchantment, level) ->
            acc + if (enchantment.isCurse) -0.001 else level.toDouble() / enchantment.maxLevel
        } // cursed enchantments +0 scores

inline val ItemType.enchantments: MutableMap<Enchantment, Int>
    get() = EnchantmentHelper.getEnchantments(vanillaStack)

inline val ItemType.isDamageable: Boolean
    get() = vanillaStack.isDamageableItem //isDamageable  // .isDamageableItem
inline val ItemType.maxDamage: Int
    get() = vanillaStack.maxDamage
inline val ItemType.durability: Int
    get() = maxDamage - damage

inline val ItemType.isBucket: Boolean
    get() = item is BucketItem || item is MilkBucketItem
inline val ItemType.isStew: Boolean
    get() = item is MushroomStewItem || item is SuspiciousStewItem // SoupItem = MushroomStewItem
//endregion

//region ItemType Potion Relative

inline val ItemType.hasPotionName: Boolean
    get() = tag?.contains("Potion",
                          8) ?: false
inline val ItemType.potionName: String
    // getPotionTypeFromNBT().getNamePrefixed() = getPotion().finishTranslationKey()
    get() = if (hasPotionName) PotionUtil.getPotion(tag).getName("") else "" //getPotionTypeFromNBT(tag).getNamePrefixed("") else "" // getPotion(tag).getName("") else ""
inline val ItemType.hasPotionEffects: Boolean
    get() = PotionUtil.getCustomEffects(tag)  //getEffectsFromTag(tag) // getCustomEffects(tag)
        .isNotEmpty() // forge getEffectsFromTag() = getPotionEffects()
inline val ItemType.hasCustomPotionEffects: Boolean
    get() = PotionUtil.getAllEffects(tag) //getFullEffectsFromTag(tag)// getAllEffects(tag)
        .isNotEmpty() // getCustomPotionEffects() = getFullEffectsFromTag
inline val ItemType.potionEffects: List<org.anti_ad.mc.common.vanilla.alias.StatusEffectInstance>
    get() = PotionUtil.getCustomEffects(tag) //getFullEffectsFromTag(tag) // getCustomEffects(tag)
inline val ItemType.comparablePotionEffects: List<PotionEffect>
    get() = potionEffects.map { it.`(asComparable)` }

@Suppress("ObjectPropertyName")
inline val StatusEffectInstance.`(asComparable)`: PotionEffect
    get() = PotionEffect(
        //this.effect.descriptionId
        ForgeRegistries.MOB_EFFECTS.getKey(this.effect).toString(),
        ////Registry.MOB_EFFECT.getId(this.effect).toString(), // forge EFFECTS = STATUS_EFFECT  == MOB_EFFECT | effectType = potion = effect
        this.amplifier,
        this.duration
    )

data class PotionEffect(inline val effect: String,
                        inline val amplifier: Int,
                        inline val duration: Int) : Comparable<PotionEffect> {
    override fun compareTo(other: PotionEffect): Int { // stronger first
        this.effect.compareTo(other.effect).let { if (it != 0) return it }
        other.amplifier.compareTo(this.amplifier).let { if (it != 0) return it }
        other.duration.compareTo(this.duration).let { if (it != 0) return it }
        return 0
    }
}

//endregion
