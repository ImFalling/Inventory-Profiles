/*
 * Inventory Profiles Next
 *
 *   Copyright (c) 2019-2020 jsnimda <7615255+jsnimda@users.noreply.github.com>
 *   Copyright (c) 2021-2022 Plamen K. Kosseff <p.kosseff@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.anti_ad.mc.ipnext.item.rule.file

import org.anti_ad.mc.ipnext.Log
import org.anti_ad.mc.common.annotation.ThrowsCaught
import org.anti_ad.mc.common.annotation.WontThrow
import org.anti_ad.mc.common.extensions.ifTrue
import org.anti_ad.mc.common.extensions.ordinalName
import org.anti_ad.mc.common.extensions.usefulName
import org.anti_ad.mc.ipnext.item.rule.EmptyRule
import org.anti_ad.mc.ipnext.item.rule.Parameter
import org.anti_ad.mc.ipnext.item.rule.Rule
import org.anti_ad.mc.ipnext.item.rule.natives.NativeRules
import org.anti_ad.mc.ipnext.item.rule.parameter.NativeParameters

/*
  rulesFiles: [a.txt] -> [b.txt] -> [c.txt] -> ... -> [z.txt]
              low priority                     high priority (i.e. z.txt overrides a.txt)

  find rule [my_rule]: find in cached ?-> find from [z.txt] to [a.txt]
    found: add to cached, not found: denoted as not found

*/
object RuleFileRegister {
    val loadedFileNames = mutableSetOf<String>()
    private val ruleFiles = mutableListOf<RuleFile>()
    private val cachedRules = mutableMapOf<String, RuleDefinition?>() // store RuleDefinition of SUCCESS

    fun reloadRuleFiles(ruleFiles: List<RuleFile>) {
        Log.trace("[-] Rule file parsing...")
        Log.trace("    step: (1) parse indent -> (2) parse rule -> syntax ok")
        Log.indent()
        ruleFiles.forEach { it.parseContent() }
        Log.unindent()
        clear()
        this.ruleFiles.addAll(ruleFiles)
        checkOverrides()
        validateRules()
    }

    private fun clear() {
        loadedFileNames.clear()
        ruleFiles.clear()
        cachedRules.clear()
        names.clear()
    }

    private val names = mutableSetOf<String>()
    private fun checkOverrides() { // and log
        Log.trace("[-] Check overrides...")
        for (ruleFile in ruleFiles) {
            for (name in ruleFile.rulesMap.keys) {
                if (name in names) {
                    Log.info("Rule @$name overrode by file ${ruleFile.fileName}")
                }
                names.add(name)
            }
        }
    }

    private fun validateRules() {
        Log.trace("[-] Validate rules...")
        Log.indent()
        for (name in names) {
            Log.trace("[-] Validating rule @$name")
            Log.indent()
            getCustomRule(name) ?: Log.debug("rule @$name failed to parse")
            Log.unindent()
        }
        Log.unindent()
    }

    fun getCustomRuleOrEmpty(ruleName: String): Rule =
        getCustomRule(ruleName) ?: EmptyRule
            .also { Log.warn("Rule @$ruleName not found") }


    // ============
    // ~.~ methods
    // ============
    fun getCustomRule(ruleName: String): CustomRule? {
        val ruleDefinition =
            if (cachedRules.containsKey(ruleName)) cachedRules.getValue(ruleName) else searchAndPutCustomRule(ruleName)
        @WontThrow
        return ruleDefinition?.createCustomRule() // should not throw
    }

    fun getNativeRule(ruleName: String): Rule? =
        NativeRules.map[ruleName]?.invoke()

    fun getParameter(parameterName: String): Parameter<*>? =
        NativeParameters.map[parameterName]

    // ============
    // private
    // ============
    private fun searchAndPutCustomRule(ruleName: String): RuleDefinition? {
        Log.trace("[-] Searching rule @$ruleName...")
        Log.indent()
        val ruleDefinition = RuleFinder(ruleName).searchCustomRule()
        Log.unindent()
        if (cachedRules.containsKey(ruleName)) {
            Log.trace(">> rule $ruleName already exist in cached map... skip putting")
        } else {
            cachedRules[ruleName] = ruleDefinition
        }
        // then remove empty file
        ruleFiles.removeAll { it.rulesMap.isEmpty().ifTrue { Log.trace("Remove validated file ${it.fileName}") } }
        return ruleDefinition
    }

    private class RuleFinder(val ruleName: String) {
        fun searchCustomRule(): RuleDefinition? {
            for (ruleFile in ruleFiles.reversed()) { // asReversed here might cause no such element (list update)
                val rulesMap = ruleFile.rulesMap
                if (!rulesMap.containsKey(ruleName)) continue
                val fileName = ruleFile.fileName
                Log.trace("Searching rule @$ruleName at file $fileName")
                val list = rulesMap.getValue(ruleName)
                Log.indent()
                val ruleDefinition = findUsableRule(list.asReversed(),
                                                    fileName) // list won't update
                Log.unindent()
                // result found, -> remove key
                rulesMap.remove(ruleName)
                if (ruleDefinition != null) {
                    Log.trace("    > Found @$ruleName at file $fileName")
                    loadedFileNames.add(fileName)
                    return ruleDefinition
                } else {
                    Log.trace("    > None of @$ruleName usable at file $fileName")
                }
            }
            Log.trace(">> @$ruleName not found in all files")
            return null
        }

        @ThrowsCaught
        private fun findUsableRule(list: List<RuleDefinition>,
                                   fileName: String): RuleDefinition? { // from 0+
            var count = list.size
            for (ruleDefinition in list) {
                Log.trace("Instantiating rule @$ruleName#$count")
                Log.indent()
                try {
                    @ThrowsCaught
                    ruleDefinition.createCustomRule()
                    if (ruleDefinition.status == RuleDefinition.Status.SUCCESS) {
                        return ruleDefinition // no throws, meaning success
                    }
                    Log.error("interesting rule @$ruleName#$count (at file $fileName)") // shouldn't go here
                } catch (e: Exception) {
                    Log.warn("Error in ${count.ordinalName} '@$ruleName' (at file $fileName)")
                    Log.warn("  > ${e.javaClass.usefulName}: ${e.message}")
                    when (e) {
                        is NoSuchElementException,
                        is SelfReferenceException,
                        is MissingParameterException,
                        -> Unit // do nothing
                        else -> e.printStackTrace()
                    }
                    continue // next rule definition
                } finally {
                    Log.unindent()
                    count--
                }
            }
            return null // no search
        }
    }
}
