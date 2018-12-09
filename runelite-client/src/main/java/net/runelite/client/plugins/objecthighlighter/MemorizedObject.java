package net.runelite.client.plugins.objecthighlighter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;

@AllArgsConstructor
public class MemorizedObject
{
    @Getter
    @Setter
    private WorldPoint worldPoint;

    @Getter
    @Setter
    private Integer objectIdNumber;
}
