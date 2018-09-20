package net.runelite.client.plugins.wintertodt2;

import lombok.Getter;
import net.runelite.api.coords.LocalPoint;

public class SnowfallPoint {

    @Getter
    private int remainingLife;

    // TODO: Rename to something more appropriate
    @Getter
    private LocalPoint snowfallPoint;

    public SnowfallPoint(LocalPoint point, int remainingLife) {
        this.remainingLife = remainingLife;
        this.snowfallPoint = point;
    }

    public void decrementLife() {
        this.remainingLife--;
    }
}
