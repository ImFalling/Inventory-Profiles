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

package org.anti_ad.mc.ipnext.inventory.sandbox.diffcalculator

import org.anti_ad.mc.ipnext.Log
import org.anti_ad.mc.ipnext.inventory.data.ItemStat
import org.anti_ad.mc.ipnext.inventory.data.ItemTracker
import org.anti_ad.mc.ipnext.inventory.data.stat
import org.anti_ad.mc.ipnext.inventory.sandbox.ContainerSandbox
import org.anti_ad.mc.ipnext.inventory.sandbox.diffcalculator.SingleType.Button.LEFT
import org.anti_ad.mc.ipnext.inventory.sandbox.diffcalculator.SingleType.Button.RIGHT
import org.anti_ad.mc.ipnext.inventory.sandbox.toList
import org.anti_ad.mc.ipnext.item.ItemType
import org.anti_ad.mc.ipnext.item.isEmpty
import org.anti_ad.mc.ipnext.item.maxCount
import org.anti_ad.mc.ipnext.item.rule.CountSink
import org.anti_ad.mc.ipnext.item.rule.CountSource
import org.anti_ad.mc.ipnext.util.MutableBucket

class ScoreBasedDualDiffCalculatorInstance(sandbox: ContainerSandbox,
                                           goalTracker: ItemTracker) :
    SimpleDiffCalculatorInstance(sandbox,
                                 goalTracker) {
    init {
        untilEqualsTypeOnly = true // until equal types or now empty
    }

    override fun run() {
        super.run()
        val clicks = sandbox.clickNode.toList()
        val lclick = clicks.count { it.button == 0 }
        val rclick = clicks.count { it.button == 1 }
        Log.debug("Stage A click count total ${sandbox.clickCount}. $lclick left. $rclick right.")
        runFinal()
    }

    /*
      rank 2 -> 1 -> 0
                     0  n == g
        (exactly 1)  1  n + 1 == g || n / 2 == g  [i.e. g * 2 == n || g * 2 + 1 == n] || g == 0
        (at least 1) 2  n < g
        (exactly 2)  3  n / 2 + 1 == g || (g == 1 && n > g) || n / 2 / 2 == g [i.e. n / 4 == g]
        (at least 2) 4  n > g

      |          .------------.
      | (4) -> (2) -> (1) -> (0)
      |  `---- (3) ---'

      drop rank priority (unused)
        2 -> 0  \
        2 -> 1   min -> exact
        4 -> 3  /
        1 -> 0  - exact -> exact
        3 -> 1  /
        4 -> 2  - min -> min
        2 -> 2
        4 -> 4

      score (unused)
        rank 2 prefer closer to goal
        rank 4 prefer closer to goal (?)
     */

    val statGoal: ItemStat = goalTracker.slots.stat()

    fun runFinal() { // stage b
        // all equals type, but cursor may not be empty
        if (!cursorNow.isEmpty()) doItemType(cursorNow.itemType)
        statGoal.itemTypes.forEach { doItemType(it) }
    }

    fun doItemType(itemType: ItemType) {
        val entry = statGoal.itemGroups.getValue(itemType)
        val indices = entry.slotIndices.filter { !CompareSlotDsl(it).equals }
        if (indices.isEmpty()) return
        val maxCount = itemType.maxCount
        val start = SingleType.Node(maxCount).apply {
            c = cursorNow.count
            for (index in indices) {
                identities.add(CompareSlotDsl(index).toSlot())
            }
        }
        val clicks = SingleType.solve(start)
        for (click in clicks) {
            val index = indices.firstOrNull { CompareSlotDsl(it).toSlot() == click.slot }
                ?: error("target slot ${click.slot} not found")
            CompareSlotDsl(index).run {
                when (click.button) {
                    LEFT -> leftClick()
                    RIGHT -> rightClick()
                }
            }
        }
    }

    fun CompareSlotDsl.toSlot(): SingleType.Slot {
        return SingleType.Slot(n,
                               g)
    }
}

private const val MAX_LOOP = 100_000

object SingleType : DiffCalculatorUtil {
    val rankAfterAllowed = listOf(
        listOf(),         // rank 0
        listOf(0),        // rank 1
        listOf(0,
               1,
               2),  // rank 2
        listOf(1),        // rank 3
        listOf(2,
               3,
               4),  // rank 4
    )

    val nodeComparator
        get() = compareBy<Node> { it.fScore }
            .thenBy { it.upperTotal }
//    .thenByDescending { it.clickCount }
//    .thenBy { it.identities.entrySet.size }

    // todo fix compare returns 0 makes sortedMap treats as equals
    fun solve(start: Node): List<Click> { // ref: A* algorithm
        val closedSet = mutableSetOf<Node>()
//    val openSet = mutableMapOf(start to start)
        val openSet = sortedMapOf(start to start)
        var minUpper = start.upperTotal
        var minLowerOfMinUpper = start.lowerTotal
        var loopCounter = 0
        var skippedCount = 0
        while (openSet.isNotEmpty()) {
            if (++loopCounter > MAX_LOOP)
                error("Too many loop. $loopCounter > $MAX_LOOP")
            val x = openSet.firstKey() ?: break
            if (x.isGoal) {
                Log.trace("loopCounter $loopCounter")
                Log.trace("skippedCount $skippedCount")
                return constructClickPath(x)
            }
            openSet.remove(x)
            closedSet.add(x)
            if (x.lowerTotal > minUpper
                || x.lowerTotal == minUpper && (minUpper > minLowerOfMinUpper || x.upperTotal > x.lowerTotal)
            )
                error("emm x.lowerTotal > minUpper etc")
//        continue
            for (y in x.neighbor()) {
                if (y in closedSet)
                    continue
                if (y !in openSet || y < openSet.getValue(y)) {
                    if (y.lowerTotal > minUpper
                        || y.lowerTotal == minUpper && (minUpper > minLowerOfMinUpper || y.upperTotal > y.lowerTotal)
                        || y.skipThis
                    ) {
                        skippedCount++
                        openSet.remove(y)
                        closedSet.add(y)
                        continue
                    }
                    openSet[y] = y
                    if (y.upperTotal < minUpper) {
                        minUpper = y.upperTotal
                        minLowerOfMinUpper = y.lowerTotal
                    } else if (y.upperTotal == minUpper && y.lowerTotal < minLowerOfMinUpper) {
                        minLowerOfMinUpper = y.lowerTotal
                    }
                }
            }
            // remove all lower bound >= min upperbound
//      var removedCount = 0
//      openSet.keys.removeAll {
//        (it.lowerBound >= minUpper && minUpper > minLowerOfMinUpper)
//          .ifTrue { closedSet.add(it); removedCount++ }
//      }
//      if (removedCount != 0)
//        Log.trace("removed $removedCount at $loopCounter")
        }
        Log.trace("loopCounter $loopCounter")
        Log.trace("skippedCount $skippedCount")
        error("solve failure")
    }

    fun constructClickPath(node: Node): List<Click> {
        return node.clickNode.toList()
    }

    enum class Button {
        LEFT, RIGHT
    }

    data class Slot(val n: Int,
                    val g: Int): CountSink<Slot> {
        val isGoal
            get() = n == g
        val rank
            get() = calcRank(n,
                             g)

        fun click(c: Int,
                  button: Button,
                  maxCount: Int): Pair<Slot, Int> { // slot after, c after
            return when (c) {
                0 -> when (button) { // empty cur
                    LEFT -> copy(n = 0) to n
                    RIGHT -> copy(n = n / 2) to n - n / 2
                }
                else -> {
                    val nAfter = when (button) { // has cur
                        LEFT -> (n + c).coerceAtMost(maxCount)
                        RIGHT -> n + 1
                    }
                    copy(n = nAfter) to c - (nAfter - n)
                }
            }
        }

        override fun setCountSource(source: CountSource<Slot>) {
        }
    }

    // notice maxCount not in hashCode/equals, only c and identities
    class Node(val maxCount: Int,
               val identities: MutableBucket<Slot> = MutableBucket()) : Comparable<Node> {
        var c = 0 // cursor
        var skipThis = false

        val isGoal
            get() = c == 0 && identities.elementSet.all { it.isGoal }

        val gScore: Int
            get() = clickCount

        val hScore: Int
            get() = lowerBound

        val fScore: Int
            get() = gScore + hScore

        private val lowerBound by lazy(LazyThreadSafetyMode.NONE) {
            identities.entrySet.sumOf { (slot, count) ->
                clickCountLowerBound(slot.n,
                                     slot.g) * count
            }
        }
        private val upperBound by lazy(LazyThreadSafetyMode.NONE) {
            identities.entrySet.sumOf { (slot, count) ->
                clickCountUpperBound(slot.n,
                                     slot.g) * count
            }
//          c.coerceAtMost(identities.entrySet.filter { (slot) -> slot.rank == 2 }
//            .sumBy { (slot, count) -> clickCountUpperBound(slot.n, slot.g) * count })
        }

        val lowerTotal
            get() = clickCount + lowerBound
        val upperTotal
            get() = clickCount + upperBound

        val clickCount
            get() = clickNode?.clickIndex?.plus(1) ?: 0
        var clickNode: Click? = null
            private set

        private fun addClick(slot: Slot,
                             button: Button) {
            Click(clickCount,
                  slot,
                  button,
                  clickNode).also { clickNode = it }
        }

        fun neighbor(): List<Node> {
            val result = mutableListOf<Node>()
            var doneRank10 = false
            for (slot in identities.elementSet) {
                if (slot.isGoal) continue
                if (c == 0 && slot.n == 0) continue
                if (c != 0 && slot.n == maxCount) continue
                val left = copyByAddClick(slot,
                                          LEFT,
                                          maxCount)
                    ?.apply { skipThis = true }
                    ?.also { result.add(it) }
                val right = copyByAddClick(slot,
                                           RIGHT,
                                           maxCount)
                    ?.apply { skipThis = true }
                    ?.also { result.add(it) }
//        left?.skipThis = false
//        if (!slot.banRight)
//          right?.skipThis = false
                val rank = slot.rank
                when (rank) {
                    10 -> if (c == 0) continue
                    11, 12 -> if (c != 0) continue
                    30, 31 -> if (c != 0) continue
//          30, 31, 32 -> if (c != 0) continue
                }
                // try left
                if (rank != 12 && (rank != 10 || slot.banRight)) {
                    if (rank == 10) {
                        if (!doneRank10) {
                            doneRank10 = true
                            left?.skipThis = false
                        }
                    } else {
                        left?.skipThis = false
                    }
                }
                // try right
                if (!slot.banRight && rank != 30) {
                    if (rank == 10) {
                        if (!doneRank10) {
                            doneRank10 = true
                            right?.skipThis = false
                        }
                    } else {
                        right?.skipThis = false
                    }
                }
            }
            return result
        }

        // ============
        // extensions
        // ============

        val Slot.banRight: Boolean // ban right if c == 1 or no cur and n == 1
            get() = c == 1 || c == 0 && n == 1 || c != 0 && maxCount - n == 1

        // ============
        // copy
        // ============
        fun copy() = Node(maxCount,
                          identities.copyAsMutable()).also {
            it.c = this.c
            it.clickNode = this.clickNode
        }

        fun copyByAddClick(slot: Slot,
                           button: Button,
                           maxCount: Int): Node? {
            return when (button) {
                LEFT -> copyByAddClickLeft(slot,
                                           maxCount)
                RIGHT -> copyByAddClickRight(slot,
                                             maxCount)
            }
        }

        fun copyByAddClickLeft(slot: Slot,
                               maxCount: Int): Node? {
//      val rank = slot.rank
            val (slotAfter, cAfter) = slot.click(c,
                                                 LEFT,
                                                 maxCount)
//      if (slotAfter.rank !in rankAfterAllowed[rank]) return null
            return copy().apply {
                addClick(slot,
                         LEFT)
                c = cAfter
                identities.remove(slot)
                identities.add(slotAfter)
            }
        }

        fun copyByAddClickRight(slot: Slot,
                                maxCount: Int): Node? {
            //val rank = slot.rank
            val (slotAfter, cAfter) = slot.click(c,
                                                 RIGHT,
                                                 maxCount)
//      if (slotAfter.rank !in rankAfterAllowed[rank]) return null
            return copy().apply {
                addClick(slot,
                         RIGHT)
                c = cAfter
                identities.remove(slot)
                identities.add(slotAfter)
            }
        }

        override fun compareTo(other: Node): Int {
            return nodeComparator.compare(this,
                                          other)
        }

        // ============
        // equals
        // ============

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Node) return false

            if (c != other.c) return false
            if (identities != other.identities) return false

            return true
        }

        val hashCode by lazy(LazyThreadSafetyMode.NONE,
                             ::genHashCode)

        override fun hashCode(): Int {
            return hashCode
        }

        private fun genHashCode(): Int {
            var result = c
            result = 31 * result + identities.hashCode()
            return result
        }
    }

    // ref: SandboxClick
    data class Click(
        val clickIndex: Int,
        val slot: Slot,
        val button: Button,
        val previousClick: Click? = null
    )

    fun Click?.toList(): List<Click> {
        val list = mutableListOf<Click>()
        var click: Click? = this
        while (click != null) {
            list.add(click)
            click = click.previousClick
        }
        list.reverse()
        return list
    }
}
