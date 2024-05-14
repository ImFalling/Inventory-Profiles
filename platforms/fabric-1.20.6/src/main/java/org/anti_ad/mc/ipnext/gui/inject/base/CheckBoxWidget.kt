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

package org.anti_ad.mc.ipnext.gui.inject.base

import org.anti_ad.mc.common.gui.NativeContext
import org.anti_ad.mc.ipnext.config.ModSettings

class CheckBoxWidget : SortButtonWidget {

    constructor(clickEvent: (button: Int) -> Unit) : super(clickEvent)
    constructor(clickEvent: () -> Unit) : super(clickEvent)
    constructor() : super()

    var highlightTx = 0
    var highlightTy = 0
    var highlightTooltip: String = ""
    var highlightEnabled: Boolean = true

    override fun render(context: NativeContext,
                        mouseX: Int,
                        mouseY: Int,
                        partialTicks: Float) {
        if (highlightEnabled) {
            val oldTx = tx
            val oldTy = ty
            val oldTooltipText = tooltipText
            if (ModSettings.INCLUDE_HOTBAR_MODIFIER.isPressing()) {
                tx = highlightTx
                ty = highlightTy
                tooltipText = highlightTooltip
            }
            super.render(context,
                         mouseX,
                         mouseY,
                         partialTicks)
            tx = oldTx
            ty = oldTy
            tooltipText = oldTooltipText
        } else {
            super.render(context,
                         mouseX,
                         mouseY,
                         partialTicks)
        }
    }
}
