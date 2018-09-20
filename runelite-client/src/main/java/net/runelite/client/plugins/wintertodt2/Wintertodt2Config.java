/*
 * Copyright (c) 2018, b krzanich <krzanichb@protonmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.wintertodt2;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.Color;

@ConfigGroup("wintertodtplugin")
public interface Wintertodt2Config extends Config
{
    @ConfigItem(
            position = 1,
            keyName = "highlightActionObjectsColor",
            name = "Brazier/bruma highlight color",
            description = "Determines the color that the brazier and bruma roots are highlighted in"
    )
    default Color getHighlightActionObjectsColor() {
        return Color.RED;
    }

    @ConfigItem(
            position = 2,
            keyName = "highlightSafeSquares",
            name = "Show safe squares when endangered",
            description = "Highlights the best tiles to avoid snowfalls and exploding brazier events"
    )
    default boolean highlightSafeSquares() {
        return true;
    }

    @ConfigItem(
            position = 3,
            keyName = "highlightSafeSquareColor",
            name = "Safe square color",
            description = "Determines the color of safe tiles when a player is endangered"
    )
    default Color getHighlightSafeSquareColor() {
        return Color.RED;
    }

    @ConfigItem(
            position = 4,
            keyName = "highlightEndangeredSquares",
            name = "Show dangerous squares",
            description = "Highlights tiles that snowfalls/exploding brazier events are hitting"
    )
    default boolean highlightEndangeredSquares() {
        return true;
    }

    @ConfigItem(
            position = 5,
            keyName = "highlightEndangeredSquareColor",
            name = "Endangered square color",
            description = "Determines the color of dangerous tiles"
    )
    default Color getHighlightEndangeredSquareColor() {
        return Color.GREEN;
    }

    @ConfigItem(
            position = 6,
            keyName = "highlightPathways",
            name = "Highlight paths to bruma/brazier",
            description = "Might improve decision making time when playing in peripheral vision"
    )
    default boolean highlightPathways() {
        return true;
    }

    @ConfigItem(
            position = 7,
            keyName = "highlightPathColor",
            name = "Path color",
            description = "Determines the color that path tiles are highlighted in"
    )
    default Color getHighlightPathColor() {
        return Color.ORANGE;
    }


    @ConfigItem(
            position = 8,
            keyName = "countdownGameStart",
            name = "Display countdown",
            description = "Displays a clearly visible countdown on screen just before the game starts"
    )
    default boolean countdownGameStart() {
        return false;
    }


    @ConfigItem(
            position = 9,
            keyName = "alertBrazierUnlitSoon",
            name = "[NOT DONE] Glow burning-out brazier",
            description = "Highlights the brazier when it is about to go out"
    )
    default boolean alertBrazierDying() {
        return false;
    }

    @ConfigItem(
            position = 10,
            keyName = "alertChopTreeOptimal",
            name = "[NOT DONE] Alert optimal return to tree",
            description = "Plays an alert when Wintertodt's HP is not low, and player should chop the tree"
    )
    default boolean alertReturnToTreeOptimal() {
        return false;
    }

    @ConfigItem(
            position = 11,
            keyName = "alertReturnToBrazierOptimal",
            name = "[NOT DONE] Alert optimal return to brazier",
            description = "Directs player to return to brazier before full inv, near game end"
    )
    default boolean alertReturnToBrazierOptimal() {
        return false;
    }










    // These might get used later
    /*@ConfigItem(
            position = n,
            keyName = "highlightBrazierBroken",
            name = "Highlight broken brazier",
            description = "Highlights the brazier when it is broken"
    )
    default boolean highlightBrazierBroken() {
        return false;
    }*/
}