package net.runelite.client.plugins.redwoods;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.AudioPlayerUtil;

import javax.inject.Inject;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.geom.Area;
import java.io.IOException;
import java.util.*;

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

    private static final int WOODCUTTING_ANIMATION_ID = 2846;

    private static final int TICKS_BEFORE_IDLE = 8;
    private static final int TICKS_BEFORE_IDLE_FULL_INV = 2;
    private static final int TICKS_BEFORE_WARN_LOGOUT = 465;

    @Getter
    private boolean atRedwoodsLocation = false;

    @Getter
    private boolean idle = false;

    @Getter
    private boolean nearlyFiveMinuteLogged = false;

    @Getter
    private boolean fullInventory = false;

    private int idleAnimationTickCount = 0;
    private int woodcuttingAnimationTickCount = 0;
    private LocalPoint lastTickLocation = null;

    @Inject
    private RedwoodsOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private Client client;

    // Debugging system state variables
    private static final int DEBUG_PRINT_PERIODICITY = 4;
    private int debugPrintCycle = 0;


    // =================== HIGHLIGHT-ABLE OBJECTS + GETTERS ========================
    private final Set<GameObject> barks = new HashSet<>();
    @Getter
    private final List<GameObject> barksRenderable = new ArrayList<>();

    private GameObject bankDeposit;
    private GameObject bankDepositRenderable;
    protected Area getBankDepositRenderable()
    {
        if (bankDepositRenderable != null && bankDepositRenderable.getClickbox() != null)
        {
            return bankDepositRenderable.getClickbox();
        }
        return null;
    }

    private GameObject bankChest;
    private GameObject bankChestRenderable;
    protected Area getBankChestRenderable()
    {
        if (bankChestRenderable != null && bankChestRenderable.getClickbox() != null)
        {
            return bankChestRenderable.getClickbox();
        }
        return null;
    }

    private GameObject ladderTop;
    private GameObject ladderTopRenderable;
    protected Area getLadderTopRenderable()
    {
        if (ladderTopRenderable != null && ladderTopRenderable.getClickbox() != null)
        {
            return ladderTopRenderable.getClickbox();
        }
        return null;
    }

    private GameObject ladderBottom;
    private GameObject ladderBottomRenderable;
    protected Area getLadderBottomRenderable()
    {
        if (ladderBottomRenderable != null && ladderBottomRenderable.getClickbox() != null)
        {
            return ladderBottomRenderable.getClickbox();
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
        this.barksRenderable.clear();
        overlayManager.remove(this.overlay);
    }

    @Subscribe
    private void onGameTick(GameTick event)
    {
        this.atRedwoodsLocation = this.checkIfAtRedwoods();

        setRenderTargets();
        removeCorruptedBarks();

        boolean wasPreviouslyIdle = this.idle;
        determineIfIdle();
        this.fullInventory = getInventoryFullState();
        if (!wasPreviouslyIdle && this.idle)
        {
            //playAlertSound();
        }


        // Debugging
        this.debugPrintCycle++;
        if (this.debugPrintCycle == DEBUG_PRINT_PERIODICITY)
        {
            this.debugPrintCycle = 0;
            this.showSystemState();
        }

    }

    // TODO: Doesn't work (makes very weird noise)
    private void playAlertSound()
    {
        try
        {
            AudioInputStream ais = AudioSystem.getAudioInputStream(this.getClass().getResourceAsStream("/util/generic_alert.wav"));
            AudioPlayerUtil.play(ais);

        }
        catch (UnsupportedAudioFileException | IOException e)
        {
            e.printStackTrace();
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
            for (Object aBarkArray : barkArray)
            {
                WorldPoint wp = ((GameObject) (aBarkArray)).getWorldLocation();
                if (worldPointsEqual(wp, object.getWorldLocation()))
                {
                    duplicate = true;
                }
            }
            if (!duplicate)
            {
                this.barks.add(object);
            }

        }
        else if ((object.getId() == BARK_LEFT_DEAD) || (object.getId() == BARK_RIGHT_DEAD))
        {
            // DEBUG
            //System.out.println("Dead bark spawned at :" + gameObjectFriendlyString(object));

            // Kill any zombie living barks on the same world tile
            Object[] barkArray = this.barks.toArray();
            boolean zombieBarkKilled = false;
            for (Object aBarkArray : barkArray)
            {
                if (!zombieBarkKilled)
                {
                    WorldPoint wp = ((GameObject) (aBarkArray)).getWorldLocation();
                    if (worldPointsEqual(wp, object.getWorldLocation()))
                    {
                        // DEBUG
                        //System.out.println("Killed zombie bark: " + gameObjectFriendlyString((GameObject)(barkArray[i])));

                        this.barks.remove(aBarkArray);
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
        this.bankChestRenderable = (isApparentToPlayer(this.bankChest, true) ?
                this.bankChest : null);

        this.bankDepositRenderable = (isApparentToPlayer(this.bankDeposit, true) ?
                this.bankDeposit : null);

        this.ladderTopRenderable = (isApparentToPlayer(this.ladderTop, true) ?
                this.ladderTop : null);

        this.ladderBottomRenderable = (isApparentToPlayer(this.ladderBottom, true) ?
                this.ladderBottom : null);

        this.barksRenderable.clear();
        for (GameObject barkObject : this.barks)
        {
            if (isApparentToPlayer(barkObject, true))
            {
                this.barksRenderable.add(barkObject);
            }
        }
    }

    private void removeCorruptedBarks()
    {
        Object[] barkArray = this.barks.toArray();
        for (Object aBarkArray : barkArray)
        {
            WorldPoint wp = ((GameObject) (aBarkArray)).getWorldLocation();

            if ((wp.getX() > BARK_AREA_UPPER_X) || (wp.getX() < BARK_AREA_LOWER_X) ||
                    (wp.getY() > BARK_AREA_UPPER_Y) || (wp.getY() < BARK_AREA_LOWER_Y))
            {
                this.barks.remove(aBarkArray);
            }
        }
    }

    private boolean checkIfAtRedwoods()
    {
        return client.getLocalPlayer().getWorldLocation().getRegionID() == REDWOODS_REGION;
    }

    // TODO: subscribe to 'Chat message type FILTERED: You get some redwood logs.' and refresh woodcuttingTicks...

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
            this.woodcuttingAnimationTickCount = 0;
        }

        if (client.getLocalPlayer().getAnimation() == WOODCUTTING_ANIMATION_ID)
        {
            this.woodcuttingAnimationTickCount++;
        }
        else
        {
            this.woodcuttingAnimationTickCount = 0;
        }

        this.lastTickLocation = client.getLocalPlayer().getLocalLocation();
        this.idle = idleAnimationTickCount >=
                (this.fullInventory ? TICKS_BEFORE_IDLE_FULL_INV : TICKS_BEFORE_IDLE);

        this.nearlyFiveMinuteLogged = woodcuttingAnimationTickCount >= TICKS_BEFORE_WARN_LOGOUT;
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

    private boolean worldPointsEqual(WorldPoint a, WorldPoint b)
    {
        return ((a.getX() == b.getX()) && (a.getY() == b.getY()) && (a.getPlane() == b.getPlane()));
    }



    // ================================= Inventory util =============================
    private boolean getInventoryFullState()
    {
        return getInventorySlotsFree() == 0;
    }

    private int getInventorySlotsFree()
    {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        Item[] inventoryItems = new Item[0];
        if (inventory != null) {
            inventoryItems = inventory.getItems();
        }
        int itemCount = 0;
        for (Item inventoryItem : inventoryItems) {
            if (inventoryItem.getId() != -1) {
                itemCount++;
            }
        }

        return 28 - itemCount;
    }



    // ================================= Debugging =================================

    // Debug
    private void showSystemState()
    {
        // print state information here...
    }

    private String gameObjectFriendlyString(GameObject go)
    {
        String out = "";
        out += go.getWorldLocation();
        return out;
    }

}
