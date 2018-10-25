package net.runelite.client.plugins.redwoods;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@PluginDescriptor(
        name = "Redwoods Helper"
)
public class RedwoodsPlugin extends Plugin
{
    private static final int BARK_LEFT = 29668;
    private static final int BARK_LEFT_DEAD = 29669;
    private static final int BARK_RIGHT = 29670;
    private static final int BARK_RIGHT_DEAD = 29671;
    private static final int BARK_AREA_UPPER_X = 1574;
    private static final int BARK_AREA_LOWER_X = 1567;
    private static final int BARK_AREA_UPPER_Y = 3496;
    private static final int BARK_AREA_LOWER_Y = 3479;

    private static final int LADDER_BOTTOM = 28857;
    private final WorldPoint LADDER_BOTTOM_LOCATION = new WorldPoint(1575, 3483, 0);
    private static final int LADDER_TOP = 28858;
    private final WorldPoint LADDER_TOP_LOCATION = new WorldPoint(1575, 3483, 1);
    private static final int BANK_CHEST = 28861;
    private final WorldPoint BANK_CHEST_LOCATION = new WorldPoint(1592, 3475, 0);
    private static final int BANK_DEPOSIT = 26254;
    private final WorldPoint BANK_DEPOSIT_LOCATION = new WorldPoint(1589, 3476, 0);

    private static final int HIGHLIGHT_DISTANCE = 2250;

    private static final int REDWOODS_REGION = 6198;

    private static final int TICKS_BEFORE_IDLE = 10;

    @Getter
    private boolean atRedwoodsLocation = false;

    @Getter
    private boolean idleWoodcutting = true;

    private int idleAnimationTickCount = 0;
    private int playerStandingStillCount = 0;
    private LocalPoint lastTickLocation = null;

    @Inject
    private RedwoodsOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private Client client;

    private static final int DEBUG_PRINT_PERIODICITY = 4;
    private int debugPrintCycle = 0;

    private final Set<GameObject> barks = new HashSet<>();
    @Getter
    private final List<GameObject> barks_Renderable = new ArrayList<>();

    private GameObject bankDeposit;
    private GameObject bankDeposit_Renderable;
    protected Area getBankDeposit_Renderable() {
        if (bankDeposit_Renderable != null && bankDeposit_Renderable.getClickbox() != null)
        {
            return bankDeposit_Renderable.getClickbox();
        }
        return null;
    }

    private GameObject bankChest;
    private GameObject bankChest_Renderable;
    protected Area getBankChest_Renderable() {
        if (bankChest_Renderable != null && bankChest_Renderable.getClickbox() != null)
        {
            return bankChest_Renderable.getClickbox();
        }
        return null;
    }

    private GameObject ladderTop;
    private GameObject ladderTop_Renderable;
    protected Area getLadderTop_Renderable() {
        if (ladderTop_Renderable != null && ladderTop_Renderable.getClickbox() != null)
        {
            return ladderTop_Renderable.getClickbox();
        }
        return null;
    }

    private GameObject ladderBottom;
    private GameObject ladderBottom_Renderable;
    protected Area getLadderBottom_Renderable() {
        if (ladderBottom_Renderable != null && ladderBottom_Renderable.getClickbox() != null)
        {
            return ladderBottom_Renderable.getClickbox();
        }
        return null;
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(this.overlay);
    }

    @Override
    protected void shutDown()
    {
        this.barks.clear();
        this.barks_Renderable.clear();
        overlayManager.remove(this.overlay);
    }
    // TODO: Add a pie graph overlay at redwoods maybe?

    // TODO: Add some sort of a screen glow effect when not woodcutting?

    @Subscribe
    private void onGameTick(GameTick event)
    {
        this.atRedwoodsLocation = this.checkIfAtRedwoods();

        setRenderTargets();
        determineIfIdle();
        removeCorruptedBarks();

        // debug shit
        this.debugPrintCycle++;
        if (this.debugPrintCycle == DEBUG_PRINT_PERIODICITY) {
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
            // Ensure a duplicate (same WorldPoint) bark is not being tracked already
            Object[] barkArray = this.barks.toArray();
            boolean duplicate = false;
            for (int i = 0; i < barkArray.length; i++) {
                WorldPoint wp = ((GameObject)(barkArray[i])).getWorldLocation();
                if (worldPointsEqual(wp, object.getWorldLocation())) {
                    duplicate = true;
                }
            }
            if (!duplicate) {
                this.barks.add(object);
            }

        }
        else if ((object.getId() == BARK_LEFT_DEAD) || (object.getId() == BARK_RIGHT_DEAD))
        {
            System.out.println("Dead bark spawned at :" + gameObjectFriendlyString(object));
            // Kill any zombie living barks on the same world tile
            Object[] barkArray = this.barks.toArray();
            boolean zombieBarkKilled = false;
            for (int i = 0; i < barkArray.length; i++) {

                if (!zombieBarkKilled) {
                    WorldPoint wp = ((GameObject) (barkArray[i])).getWorldLocation();
                    if (worldPointsEqual(wp, object.getWorldLocation())) {
                        System.out.println("killed zombie bark: " + gameObjectFriendlyString((GameObject)(barkArray[i])));
                        this.barks.remove(barkArray[i]);
                        zombieBarkKilled = true;
                    }
                }
            }
        }
        else if ((object.getId() == LADDER_BOTTOM) && (object.getWorldLocation().equals(LADDER_BOTTOM_LOCATION)))
        {
            this.ladderBottom = object;
        }
        else if ((object.getId() == LADDER_TOP) && (object.getWorldLocation().equals(LADDER_TOP_LOCATION)))
        {
            this.ladderTop = object;
        }
        else if ((object.getId() == BANK_CHEST) && (object.getWorldLocation().equals(BANK_CHEST_LOCATION)))
        {
            this.bankChest = object;
        }
        else if ((object.getId() == BANK_DEPOSIT) && (object.getWorldLocation().equals(BANK_DEPOSIT_LOCATION)))
        {
            this.bankDeposit = object;
        }
    }

    // TODO remove this
    @Subscribe
    private void onGameObjectChanged(GameObjectChanged event) {
        GameObject object = event.getGameObject();

        if ((object.getId() == BARK_LEFT) || (object.getId() == BARK_RIGHT))
        {
            System.out.println("LOGGER: Bark changed: " + gameObjectFriendlyString(object));
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
        else if ((object.getId() == LADDER_BOTTOM) && (object.getWorldLocation().equals(LADDER_BOTTOM_LOCATION)))
        {
            this.ladderBottom = null;
        }
        else if ((object.getId() == LADDER_TOP) && (object.getWorldLocation().equals(LADDER_TOP_LOCATION)))
        {
            this.ladderTop = null;
        }
        else if ((object.getId() == BANK_CHEST) && (object.getWorldLocation().equals(BANK_CHEST_LOCATION)))
        {
            this.bankChest = null;
        }
        else if ((object.getId() == BANK_DEPOSIT) && (object.getWorldLocation().equals(BANK_DEPOSIT_LOCATION)))
        {
            this.bankDeposit = null;
        }
    }

    private void setRenderTargets()
    {
        this.bankChest_Renderable = (isApparentToPlayer(this.bankChest, true) ?
                this.bankChest : null);

        this.bankDeposit_Renderable = (isApparentToPlayer(this.bankDeposit, true) ?
                this.bankDeposit : null);

        this.ladderTop_Renderable = (isApparentToPlayer(this.ladderTop, true) ?
                this.ladderTop : null);

        this.ladderBottom_Renderable = (isApparentToPlayer(this.ladderBottom, true) ?
                this.ladderBottom : null);

        this.barks_Renderable.clear();
        for (GameObject barkObject : this.barks)
        {
            if (isApparentToPlayer(barkObject, true))
            {
                this.barks_Renderable.add(barkObject);
            }
        }
    }

    private void removeCorruptedBarks() {
        Object[] barkArray = this.barks.toArray();
        for (int i = 0; i < barkArray.length; i++) {
            WorldPoint wp = ((GameObject)(barkArray[i])).getWorldLocation();

            if ((wp.getX() > BARK_AREA_UPPER_X) || (wp.getX() < BARK_AREA_LOWER_X) ||
                    (wp.getY() > BARK_AREA_UPPER_Y) || (wp.getY() < BARK_AREA_LOWER_Y))
            {
                System.out.println("Removing floating bark");
                this.barks.remove(barkArray[i]);
            }
        }
    }

    private boolean checkIfAtRedwoods()
    {
        return client.getLocalPlayer().getWorldLocation().getRegionID() == REDWOODS_REGION;
    }

    // Called once per tick
    private void determineIfIdle()
    {
        if (client.getLocalPlayer().getLocalLocation().equals(this.lastTickLocation))
        {
            if (client.getLocalPlayer().getAnimation() == -1)
            {
                this.idleAnimationTickCount++;
            }
            else
            {
                this.idleAnimationTickCount = 0;
            }
        }
        else
        {
            this.idleAnimationTickCount = 0;
        }

        this.lastTickLocation = client.getLocalPlayer().getLocalLocation();
        this.idleWoodcutting = idleAnimationTickCount >= TICKS_BEFORE_IDLE;
    }

    private boolean isApparentToPlayer(GameObject object, boolean checkSameLevel)
    {
        if ((checkSameLevel) &&
                (object.getWorldLocation().getPlane() != client.getLocalPlayer().getWorldLocation().getPlane()))
        {
            return false;
        }

        return isApparentToPlayer(object.getLocalLocation());
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

    private boolean worldPointsEqual(WorldPoint a, WorldPoint b) {
        return ((a.getX() == b.getX()) && (a.getY() == b.getY()) && (a.getPlane() == b.getPlane()));
    }

    // debug
    private void showSystemState()
    {
        Object[] barkArray = this.barks.toArray();

        for (int i = 0; i < barkArray.length; i++) {
            System.out.print(" B" + i + ":" + gameObjectFriendlyString((GameObject)(barkArray[i])));
        }

        System.out.println();
//        for (GameObject barkObject : this.barks)
//        System.out.print();
//        System.out.print();

//        System.out.println("idle for : " + idleAnimationTickCount);
//
//        System.out.println("Barks: " + this.barks.size() +
//                ", Ladder top: " + (this.ladderTop != null) +
//                ", Ladder bottom: " + (this.ladderBottom != null) +
//                ", Bank chest: " + (this.bankChest != null) +
//                ", Bank deposit: " + (this.bankDeposit != null));
    }

    private String gameObjectFriendlyString(GameObject go) {
        String out = "";
        out += go.getWorldLocation();
        return out;
    }

}
