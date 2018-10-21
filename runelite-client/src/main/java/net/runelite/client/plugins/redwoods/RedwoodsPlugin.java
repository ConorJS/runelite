package net.runelite.client.plugins.redwoods;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@PluginDescriptor(
        name = "Redwoods Helper"
)
public class RedwoodsPlugin extends Plugin
{
    private static final int BARK_LEFT = 29668;
    private static final int BARK_RIGHT = 29670;
    private static final int LADDER_BOTTOM = 28857;
    private final WorldPoint LADDER_BOTTOM_LOCATION = new WorldPoint(1575, 3483, 0);
    private static final int LADDER_TOP = 28858;
    private final WorldPoint LADDER_TOP_LOCATION = new WorldPoint(1575, 3483, 1);
    private static final int BANK_CHEST = 28861;
    private final WorldPoint BANK_CHEST_LOCATION = new WorldPoint(1592, 3475, 0);
    private static final int BANK_DEPOSIT = 26254;
    private final WorldPoint BANK_DEPOSIT_LOCATION = new WorldPoint(1589, 3476, 0);

    private static final int HIGHLIGHT_DISTANCE = 1500;

    @Getter
    private final List<GameObject> barks = new ArrayList<>();

    private GameObject bankDeposit;
    protected Polygon getBankDeposit_Renderable() {
        if (bankDeposit != null && bankDeposit.getConvexHull() != null)
        {
            return bankDeposit.getConvexHull();
        }
        return null;
    }

    private GameObject bankChest;
    protected Polygon getBankChest_Renderable() {
        if (bankChest != null && bankChest.getConvexHull() != null)
        {
            return bankChest.getConvexHull();
        }
        return null;
    }

    private GameObject ladderTop;
    protected Polygon getLadderTop_Renderable() {
        if (ladderTop != null && ladderTop.getConvexHull() != null)
        {
            return ladderTop.getConvexHull();
        }
        return null;
    }

    private GameObject ladderBottom;
    protected Polygon getLadderBottom_Renderable() {
        if (ladderBottom != null && ladderBottom.getConvexHull() != null)
        {
            return ladderBottom.getConvexHull();
        }
        return null;
    }

    @Getter
    private boolean atRedwoodsLocation = false;

    @Inject
    private RedwoodsOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private Client client;

    private static final int DEBUG_PRINT_PERIODICITY = 4;
    private int debugPrintCycle = 0;

    @Override
    protected void startUp()
    {
        overlayManager.add(this.overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(this.overlay);
    }

    @Subscribe
    private void onGameTick(GameTick event)
    {
        this.atRedwoodsLocation = this.checkIfAtRedwoods();
//        this.cleanseObjectReferences();



        // debug shit
        this.debugPrintCycle++;
        if (this.debugPrintCycle == this.DEBUG_PRINT_PERIODICITY) {
            this.debugPrintCycle = 0;
            this.showSystemState();
        }
    }

    @Subscribe
    private void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject object = event.getGameObject();

        if ((object.getId() == BARK_LEFT) || (object.getId() == BARK_RIGHT))
        {
            this.barks.add(object);
        }
        else if ((object.getId() == LADDER_BOTTOM) && (object.getWorldLocation().equals(this.LADDER_BOTTOM_LOCATION)))
        {
            this.ladderBottom = object;
        }
        else if ((object.getId() == LADDER_TOP) && (object.getWorldLocation().equals(this.LADDER_TOP_LOCATION)))
        {
            this.ladderTop = object;
        }
        else if ((object.getId() == BANK_CHEST) && (object.getWorldLocation().equals(this.BANK_CHEST_LOCATION)))
        {
            this.bankChest = object;
        }
        else if ((object.getId() == BANK_DEPOSIT) && (object.getWorldLocation().equals(this.BANK_DEPOSIT_LOCATION)))
        {
            this.bankDeposit = object;
        }
    }

    @Subscribe
    private void onGameObjectDespawned(GameObjectDespawned event)
    {
        GameObject object = event.getGameObject();

        if ((object.getId() == BARK_LEFT) || (object.getId() == BARK_RIGHT))
        {
            this.barks.remove(object);
        }
        else if ((object.getId() == LADDER_BOTTOM) && (object.getWorldLocation().equals(this.LADDER_BOTTOM_LOCATION)))
        {
            this.ladderBottom = null;
        }
        else if ((object.getId() == LADDER_TOP) && (object.getWorldLocation().equals(this.LADDER_TOP_LOCATION)))
        {
            this.ladderTop = null;
        }
        else if ((object.getId() == BANK_CHEST) && (object.getWorldLocation().equals(this.BANK_CHEST_LOCATION)))
        {
            this.bankChest = null;
        }
        else if ((object.getId() == BANK_DEPOSIT) && (object.getWorldLocation().equals(this.BANK_DEPOSIT_LOCATION)))
        {
            this.bankDeposit = null;
        }
    }

    private boolean checkIfAtRedwoods()
    {
//        Player player = client.getLocalPlayer();
//        player.getWorldLocation();

        return true;
    }

    private boolean isApparentToPlayer(LocalPoint point)
    {
        LocalPoint playerPoint = client.getLocalPlayer().getLocalLocation();

        return (
                (Math.abs(point.getX() - playerPoint.getX()) <= HIGHLIGHT_DISTANCE)
                &&
                (Math.abs(point.getY() - playerPoint.getY()) <= HIGHLIGHT_DISTANCE)
        );
    }

    // debug
    private void showSystemState() {
        System.out.println("Barks: " + this.barks.size() +
                ", Ladder top: " + (this.ladderTop != null) +
                ", Ladder bottom: " + (this.ladderBottom != null) +
                ", Bank chest: " + (this.bankChest != null) +
                ", Bank deposit: " + (this.bankDeposit != null));
    }

}
