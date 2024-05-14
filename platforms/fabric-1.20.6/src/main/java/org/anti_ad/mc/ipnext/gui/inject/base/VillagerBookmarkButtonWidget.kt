/*
 * Inventory Profiles Next
 *
 *   Copyright (c) 2023 Plamen K. Kosseff <p.kosseff@gmail.com>
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

package org.anti_ad.mc.ipnext.gui.inject.base

import org.anti_ad.mc.common.gui.NativeContext
import org.anti_ad.mc.common.math2d.Point
import org.anti_ad.mc.common.math2d.Rectangle
import org.anti_ad.mc.common.vanilla.render.glue.Sprite
import org.anti_ad.mc.common.vanilla.render.glue.rDrawSprite
import org.anti_ad.mc.common.vanilla.render.glue.rFillRect

open class VillagerBookmarkButtonWidget: SortButtonWidget {

    var visibleOverride: ((Boolean) -> Boolean) = { it }

    val colorSource: () -> Int

    var ctx: Int = 0
    var cty: Int = 0

    open var checked = { false }

    override val hoveringTexturePt: Point
        get() = Point(tx, ty)

    open val checkedPt: Point
        get() = Point(ctx, cty)

    constructor(colorSource: () -> Int, clickEvent: (button: Int) -> Unit) : super(clickEvent) {
        this.colorSource = colorSource
    }
    constructor(colorSource: () -> Int, clickEvent: () -> Unit) : super(clickEvent) {
        this.colorSource = colorSource
    }
    constructor(colorSource: () -> Int) : super() {
        this.colorSource = colorSource
    }

    override fun renderButton(context: NativeContext,
                              hovered: Boolean) {
        super.renderButton(context,
                           hovered)
        rFillRect(context,
                  absoluteBounds,
                  colorSource())
        if (checked()) {
            rDrawSprite(context,
                        Sprite(texture,
                               Rectangle(checkedPt,
                                         size)),
                        screenX,
                        screenY)
        }
    }

}
