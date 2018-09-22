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

import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@PluginDescriptor(
        name = "Wintertodt2"
)
@Slf4j
public class Wintertodt2Plugin extends Plugin
{
    @Inject
    private Notifier notifier;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private Wintertodt2Overlay overlay;

    @Inject
    private Wintertodt2Config config;

    @Getter
    private GameObject brazier;

    @Getter
    private boolean playerHasBrumaRoot;

    // A compromise between the distance from the farthest rendered objects on the screen
    // and the minimum distance required to see the brazier from the bruma root (or vice-versa)
    protected static final int MAX_DISTANCE = 1500;

    // The state of the nearest brazier
    private BrazierState brazierState = BrazierState.UNLIT;

    // The pyromancer
    private NPC pyromancer = null;

    private boolean inventoryFull = false;

    // The state of the pyromancer : true = healed, false = dead
    private boolean pyromancerState = true;

    // The animating state of the player
    private PlayerAnimatingState playerAnimatingState = PlayerAnimatingState.IDLE;

    // A list of all relevant, current brazier objects
    private final ArrayList<GameObject> currentBrazierObjects = new ArrayList<>();

    // A list of all relevant, current bruma root objects
    private final ArrayList<GameObject> currentBrumaRootObjects = new ArrayList<>();

    private final List<GameObject> unassignedSnowfallTiles = new ArrayList<>();

    // This list is populated by the graphics objects that precede a snowfall
    // (either an AOE damage or a brazier-breaking event).
    // The contents of this list is destroyed after it is read, by the onGameTick subscriber method
    private final ArrayList<GraphicsObject> impendingSnowfallWarnings = new ArrayList<>();

    // This is how we keep track of the snowfall events. We keep track of the location
    // (LocalPoint) and the age in ticks of the events
    // [ Why does this need to be protected from concurrent writes?
    //      It should only be written by onGameTick ]
    protected final List<SnowfallPoint> snowfallEventsWithDamageToEscape = new CopyOnWriteArrayList<>();
    private final List<LocalPoint> brazierBreakCentres = new ArrayList<>();
    private final List<LocalPoint> snowfallDamageEventCentres = new ArrayList<>();

    // The safe square to run to; this will only be populated if the player is in danger
    protected final List<LocalPoint> safeSquares = new ArrayList<>();

    private final List<LocalPoint> pathToBrazier = new ArrayList<>();
    private final List<LocalPoint> pathToBruma = new ArrayList<>();
    protected final List<LocalPoint> highlightPathToBrazier = new ArrayList<>();
    protected final List<LocalPoint> highlightPathToBruma = new ArrayList<>();

    // Animations
    private static final int ANIMATION_IDLE = -1;
    private static final int ANIMATION_EATING = 829;
    private static final int ANIMATION_FIREMAKING = 832;
    private static final int ANIMATION_WOODCUTTING = 2846;
    private static final int ANIMATION_RELIGHTING_BRAZIER = 733;
    private static final int ANIMATION_REPAIRING = 3676;

    private static final int SNOWFALL_LIFE = 8; // TODO
    private static final int BRAZIER_BREAK_EVENT_LIFE = 4;

    private static final int WINTERTODT_AREA_LOWER_BOUND_X = 1610;
    private static final int WINTERTODT_AREA_UPPER_BOUND_X = 1650;
    private static final int WINTERTODT_AREA_LOWER_BOUND_Y = 3986;
    private static final int WINTERTODT_AREA_UPPER_BOUND_Y = 4027;

    private static final int GAME_STARTING_SOON_SECONDS = 5;

    private int currentAnimation = 0;

    protected boolean canSeeTimeTillStartWidget = false;

    private int wintertodtHp = 0;

    // Flag set to true when a player has yet to complete a full firemaking aninmation after
    // clicking on the brazier - we track this because the first animation cycle is followed by
    // a longer than normal idle animation delay and we need to know this
    private int initialFiremakingAnimationDelay = 0;
    private boolean movedSinceLastFiremaking = false;
    private LocalPoint locationLastSeenPlayerOn = null;

    protected boolean playerInWintertodtArea = false;

    protected boolean gameActive = false;

    //debug
    private int tickCounter = 0;

    protected boolean gameStartingSoon = false;
    protected String stringTimeTillGameStarts = "";



    @Provides
    Wintertodt2Config provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(Wintertodt2Config.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
    }



    // TODO: Break down method into sub-methods
    @Subscribe
    public void onGameTick(GameTick event)
    {
        determineIfPlayerInWintertodtArea();
        getInfoFromWidgets();

        if ((this.locationLastSeenPlayerOn != null) && ((!this.locationLastSeenPlayerOn.equals(client.getLocalPlayer().getLocalLocation()))))
        {
            this.movedSinceLastFiremaking = true;
        }

        if (isInventoryFull() != this.inventoryFull) {
            this.inventoryFull = isInventoryFull();
        }

        this.pathToBrazier.clear();
        this.pathToBruma.clear();
        if (!this.currentBrazierObjects.isEmpty()) {
            this.pathToBrazier.addAll(generatePathToPlayer(this.currentBrazierObjects.get(0).getLocalLocation()));
        }
        if (!this.currentBrumaRootObjects.isEmpty()) {
            this.pathToBruma.addAll(generatePathToPlayer(this.currentBrumaRootObjects.get(0).getLocalLocation()));
        }

        // Clear the initialFiremakingAnimationDelay flag to give us an extra tick (see comments above initialFiremakingAnimationDelay decl)
        if (this.initialFiremakingAnimationDelay > 0)
        {
            this.initialFiremakingAnimationDelay--;

            if (this.initialFiremakingAnimationDelay == 0) {
                this.playerAnimatingState = PlayerAnimatingState.IDLE_AFTER_FIREMAKING_TRANSIENT_1;
            }
        }
        else
        {
            // The order of these two if blocks must be preserved (or we could just use a switch statement
            // to make sure both don't get executed in the same tick)
            if (this.playerAnimatingState == PlayerAnimatingState.IDLE_AFTER_FIREMAKING_TRANSIENT_2) {
                this.playerAnimatingState = PlayerAnimatingState.IDLE_AFTER_FIREMAKING;
            }
            if (this.playerAnimatingState == PlayerAnimatingState.IDLE_AFTER_FIREMAKING_TRANSIENT_1) {
                this.playerAnimatingState = PlayerAnimatingState.IDLE_AFTER_FIREMAKING_TRANSIENT_2;
            }
        }

        this.playerHasBrumaRoot = doesInventoryHaveBrumaRoot();
        // Snowfall logic should, at the least, be put into another method
        // TODO: As we only look at snowfall events as they spawn, they can be LocalPoints, not SnowfallPoints
        // TODO: as we don't need to track their age here
        // Organise snowfalls into sets
        // 4 snowfalls in a +-shape will appear around a brazier centre point preceding a brazier
        // 5 snowfalls in a x-shape will appear anywhere not on a brazier preceding a snowfall damage event
        for (GameObject unassignedEvent : this.unassignedSnowfallTiles)
        {
            int twoHorizontalOrVerticalMatches = 0;
            int diagonalMatches = 0;
            LocalPoint opposingPoint = null;
            for (GameObject otherEvent : this.unassignedSnowfallTiles)
            {
                int[] vector = getAbsoluteVector(unassignedEvent.getLocalLocation(), otherEvent.getLocalLocation());
                // direct object comparison, as this will happen at start of list
                if (unassignedEvent == otherEvent)
                {
                    // do nothing
                }
                // a 128, 128 vector means diagonally adjacent
                else if ((vector[0] == 128) && (vector[1] == 128))
                {
                    diagonalMatches++;
                }
                // a 0, 128 vector means diagonally adjacent
                else if (((vector[0] == 256) && (vector[1] == 0)) || ((vector[0] == 0) && (vector[0] == 256)))
                {
                    opposingPoint = otherEvent.getLocalLocation();
                    twoHorizontalOrVerticalMatches++;
                }
            }
            if ((twoHorizontalOrVerticalMatches == 1) && (diagonalMatches == 2))
            {
                System.out.println("Found brazier break at: " +
                        getVectorAsString(client.getLocalPlayer().getLocalLocation(),
                                calculateMidpoint(opposingPoint, unassignedEvent.getLocalLocation())));
                this.brazierBreakCentres.add(calculateMidpoint(opposingPoint, unassignedEvent.getLocalLocation()));
            }
            // a snowfall that has a snowfall on each of its 4 diagonals is the centre of a snowfall damage event
            if (diagonalMatches == 4)
            {
                System.out.println("Found snowfall damage event at: " +
                        getVectorAsString(client.getLocalPlayer().getLocalLocation(),
                                unassignedEvent.getLocalLocation()));
                this.snowfallDamageEventCentres.add(unassignedEvent.getLocalLocation());
            }
        }

        this.unassignedSnowfallTiles.clear();

        // Start tracking all squares that are endangered because of new snowfall damage events
        this.snowfallEventsWithDamageToEscape.addAll(this.snowfallDamageEventCentres.stream()
                .filter(point -> isApparentToPlayer(point))
                .flatMap(point -> {
                    List<SnowfallPoint> endangeringEvents = new ArrayList<>();
                    List<LocalPoint> endangeredPoints = adjacentPointsOf(point);
                    endangeredPoints.add(point);

                    for (int i = 0; i < endangeredPoints.size(); i++)
                    {
                        endangeringEvents.add(new SnowfallPoint(endangeredPoints.get(i), SNOWFALL_LIFE));
                    }

                    return endangeringEvents.stream();
                })
                .collect(Collectors.toList())
        );


        // Start tracking all squares that are endangered because of new brazier break events
        this.snowfallEventsWithDamageToEscape.addAll(this.brazierBreakCentres.stream()
                .filter(point -> isApparentToPlayer(point))
                .flatMap(point -> {
                    List<SnowfallPoint> endangeringEvents = new ArrayList<>();
                    List<LocalPoint> endangeredPoints = adjacentBrazierTilesFromBrazierCenter(point);

                    for (int i = 0; i < endangeredPoints.size(); i++) {
                        endangeringEvents.add(new SnowfallPoint(endangeredPoints.get(i), BRAZIER_BREAK_EVENT_LIFE));
                    }

                    return endangeringEvents.stream();
                })
                .collect(Collectors.toList())
        );


        // Clear all event centres after we've found what squares they endanger
        this.snowfallDamageEventCentres.clear();
        this.brazierBreakCentres.clear();

        // increment age of events
        for (SnowfallPoint snowfallPoint : this.snowfallEventsWithDamageToEscape)
        {
            snowfallPoint.decrementLife();
            if (snowfallPoint.getRemainingLife() == 0) {
                snowfallEventsWithDamageToEscape.remove(snowfallPoint);
            }
        }

        this.locationLastSeenPlayerOn = client.getLocalPlayer().getLocalLocation();
        calculateSafeSquares();
    }

    // TODO: Need to handle brazier objects differently; should track (and dispose of) all
    // TODO: loaded brazier objects and (per tick) maintain a reference
    @Subscribe
    public void onGameObjectSpawned(final GameObjectSpawned event)
    {
        GameObject gameObject = event.getGameObject();
        // turn this into a switch statement
        if ((gameObject.getId() == ObjectID.BRAZIER_29312) && (isApparentToPlayer(gameObject.getLocalLocation())))
        {
            if (brazierState != BrazierState.UNLIT) {
                brazierState = BrazierState.UNLIT;
                currentBrazierObjects.clear();
            }
            currentBrazierObjects.add(gameObject);
        }
        else if ((gameObject.getId() == ObjectID.BRAZIER_29313) && (isApparentToPlayer(gameObject.getLocalLocation())))
        {
            if (brazierState != BrazierState.BROKEN) {
                brazierState = BrazierState.BROKEN;
                currentBrazierObjects.clear();
            }
            currentBrazierObjects.add(gameObject);
        }
        else if ((gameObject.getId() == ObjectID.BURNING_BRAZIER_29314) && (isApparentToPlayer(gameObject.getLocalLocation())))
        {
            if (brazierState != BrazierState.LIT) {
                brazierState = BrazierState.LIT;
                currentBrazierObjects.clear();
            }
            currentBrazierObjects.add(gameObject);
        }
        else if ((gameObject.getId() == ObjectID.BRUMA_ROOTS) && (isApparentToPlayer(gameObject.getLocalLocation())))
        {
            currentBrumaRootObjects.clear();
            currentBrumaRootObjects.add(gameObject);
        }
        else if ((gameObject.getId() == 26690) && (isApparentToPlayer(gameObject.getLocalLocation())))
        {
            this.unassignedSnowfallTiles.add(gameObject);
        }
    }

    @Subscribe
    public void onNpcSpawned(final NpcSpawned event)
    {
        NPC npc = event.getNpc();
        if ((npc.getId() == NpcID.PYROMANCER) && (isApparentToPlayer(npc.getLocalLocation())))
        {
            if (!pyromancerState) {
                pyromancerState = true;
                System.out.println("DEBUG WT: Pyromancer state switching to: " + pyromancerState);
            }
        }
        else if ((npc.getId() == NpcID.INCAPACITATED_PYROMANCER) && (isApparentToPlayer(npc.getLocalLocation())))
        {
            if (pyromancerState) {
                pyromancerState = false;
                System.out.println("DEBUG WT: Pyromancer state switching to: " + pyromancerState);
            }
        }
        this.pyromancer = npc;
    }

    @Subscribe
    public void onGameObjectDespawned(final GameObjectDespawned event)
    {
        /*GameObject gameObject = event.getGameObject();
        if (gameObject.getId() == ObjectID.BRAZIER_29312)
        {
            System.out.println("Unlit brazier despawned");
        }
        else if (gameObject.getId() == ObjectID.BRAZIER_29313)
        {
            System.out.println("Broken brazier despawned");
        }
        else if (gameObject.getId() == ObjectID.BURNING_BRAZIER_29314)
        {
            System.out.println("Lit brazier despawned");
        }*/
    }

    @Subscribe
    public void onGameObjectChanged(final GameObjectChanged event)
    {
        /*GameObject gameObject = event.getGameObject();
        if ((gameObject.getId() >= 1276) && (gameObject.getId() <= 1280))
        {

        }

        if (gameObject.getId() == ObjectID.BRAZIER_29312)
        {
            System.out.println("Unlit brazier changed");
        }
        else if (gameObject.getId() == ObjectID.BRAZIER_29313)
        {
            System.out.println("Broken brazier changed");
        }
        else if (gameObject.getId() == ObjectID.BURNING_BRAZIER_29314)
        {
            System.out.println("Lit brazier changed");
        }*/
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event)
    {
        final GraphicsObject go = event.getGraphicsObject();

        // TODO: Are we using this anymore?
        if ((go.getId() == GraphicID.WINTERTODT_AURA) && (isApparentToPlayer(go.getLocation())))
        {
            //System.out.println("Observed Wintertodt Aura graphics object creation");
            // if brazier tile highlight brazier and alert

            // if non-brazier tile then calc area of effect, then calculate + highlight safe squares
            // if player is in calculated area
            // deprecated
            impendingSnowfallWarnings.add(event.getGraphicsObject());
        }
    }

    @Subscribe
    public void onAnimationChanged(final AnimationChanged event)
    {
        Player local = client.getLocalPlayer();

        if (event.getActor() != local)
        {
            return;
        }

        int newAnimation = local.getAnimation();
        if (newAnimation != this.currentAnimation)
        {
            switch (newAnimation)
            {
                case ANIMATION_EATING :
                case ANIMATION_IDLE :
                    // The PlayerAnimatingState enumerator is used here for a little more than just what
                    // the name states. It is also responsible for tracking the nature of an idle
                    // animation (i.e. what action the player went idle from) and also facilitates tracking
                    // of long and short post-firemaking idle animations; a short (1-tick) idle animation
                    // following firemaking occurs while the player is putting logs on the brazier -
                    // see the onGameTick() method for more on this
                    switch (this.currentAnimation)
                    {
                        case ANIMATION_FIREMAKING :
                            this.locationLastSeenPlayerOn = client.getLocalPlayer().getLocalLocation();
                            this.playerAnimatingState = PlayerAnimatingState.IDLE_AFTER_FIREMAKING_TRANSIENT_1;
                            break;
                        case ANIMATION_WOODCUTTING :
                            this.playerAnimatingState = PlayerAnimatingState.IDLE_AFTER_WOODCUTTING;
                            break;
                        default :
                            this.playerAnimatingState = PlayerAnimatingState.IDLE;
                    }

                    break;
                case ANIMATION_FIREMAKING :
                    if (this.movedSinceLastFiremaking)
                    {
                        // We've just started firemaking again, reset moved flag and
                        // set up first animation cycle delay allowance
                        this.movedSinceLastFiremaking = false;
                        this.initialFiremakingAnimationDelay = 4;
                    }
                    this.playerAnimatingState = PlayerAnimatingState.FIREMAKING;
                    break;
                case ANIMATION_WOODCUTTING :
                    this.playerAnimatingState = PlayerAnimatingState.WOODCUTTING;
                    break;
                case ANIMATION_REPAIRING :
                    this.playerAnimatingState = PlayerAnimatingState.REPAIRING;
                    break;
                case ANIMATION_RELIGHTING_BRAZIER :
                    this.playerAnimatingState = PlayerAnimatingState.RELIGHTING_BRAZIER;
                    break;
                default :
                    this.playerAnimatingState = PlayerAnimatingState.UNKNOWN;
                    break;
            }

            // Don't remember eating animation
            if (newAnimation != ANIMATION_EATING)
            {
                this.currentAnimation = newAnimation;
            }
        }
    }

    public List<NPC> getHighlightNpcs()
    {
        List<NPC> highlightNpcs = new ArrayList<>();
        if ((brazierState == BrazierState.UNLIT) && (pyromancer != null) && (!pyromancerState))
        {
            highlightNpcs.add(this.pyromancer);
            System.out.println("in if{} statement in getHighlightNpcs, populating list of size: " + highlightNpcs.size());
        }

        return highlightNpcs;
    }

    public List<GameObject> getHighlightObjects()
    {
        // Rest the highlight buffers for pathways
        this.highlightPathToBruma.clear();
        this.highlightPathToBrazier.clear();

        List<GameObject> highlightObjects = new ArrayList<>();
        if (((brazierState == BrazierState.UNLIT) || (brazierState == BrazierState.BROKEN)) && playerFiremaking()
                && playerHasBrumaRoot)
        {
            // Highlight the brazier if the player was using it and it broke or went out
            highlightObjects.addAll(this.currentBrazierObjects);
        }
        else if ((playerAnimatingState == PlayerAnimatingState.IDLE_AFTER_FIREMAKING) && playerHasBrumaRoot)
        {
            // Highlight the brazier if the player has been interrupted while firemaking (and still has bruma)
            highlightObjects.addAll(this.currentBrazierObjects);
        }
        else if (!playerHasBrumaRoot && playerFiremaking())
        {
            // Highlight the bruma root if the player has been firemaking but no longer has bruma roots
            highlightObjects.addAll(this.currentBrumaRootObjects);
            this.highlightPathToBruma.addAll(this.pathToBruma);
        }
        else if (playerAnimatingState == PlayerAnimatingState.IDLE_AFTER_WOODCUTTING)
        {
            if (this.inventoryFull)
            {
                // Highlight the brazier + pathway after a player finishes an inventory of bruma roots
                highlightObjects.addAll(currentBrazierObjects);
                this.highlightPathToBrazier.addAll(this.pathToBrazier);
            }
            // Highlighting the bruma root if the player clicks off of the bruma root while cutting
            else
            {
                highlightObjects.addAll(this.currentBrumaRootObjects);
            }
        }

        return highlightObjects;
    }

    private boolean isBrazierOnTile(LocalPoint localPoint)
    {
        boolean isBrazierOnTile = false;
        for (GameObject brazier : currentBrazierObjects)
        {
            if (Math.abs(brazier.getLocalLocation().getX() - localPoint.getX()) <= 128 &&
                    Math.abs(brazier.getLocalLocation().getY() - localPoint.getY()) <= 128)
            {
                isBrazierOnTile = true;
            }
        }
        return isBrazierOnTile;
    }

    private boolean isAdjacentToPlayer(LocalPoint point)
    {
        LocalPoint playerPoint = client.getLocalPlayer().getLocalLocation();
        return (
                (Math.abs(point.getX() - playerPoint.getX()) <= 128) &&
                        (Math.abs(point.getY() - playerPoint.getY()) <= 128));
    }

    private boolean isAtPlayer(LocalPoint point)
    {
        LocalPoint playerPoint = client.getLocalPlayer().getLocalLocation();
        return ((Math.abs(point.getX() - playerPoint.getX()) <= 64) &&
                (Math.abs(point.getY() - playerPoint.getY()) <= 64));
    }

    private boolean isApparentToPlayer(LocalPoint point)
    {
        LocalPoint playerPoint = client.getLocalPlayer().getLocalLocation();
        return ((Math.abs(point.getX() - playerPoint.getX()) <= MAX_DISTANCE) && (Math.abs(point.getY() - playerPoint.getY()) <= MAX_DISTANCE));
    }

    private ArrayList<LocalPoint> adjacentPointsOf(LocalPoint point)
    {
        return adjacentTilesOfRadiusX(point, 1);
    }

    private ArrayList<LocalPoint> adjacentBrazierTilesFromBrazierCenter(LocalPoint point)
    {
        return adjacentTilesOfRadiusX(point, 2);
    }

    private ArrayList<LocalPoint> adjacentTilesOfRadiusX(LocalPoint point, int radius)
    {
        ArrayList<LocalPoint> adjacentPoints = new ArrayList<>();

        // Start drawing the square at {radius, radius}
        int xPos = radius;
        int yPos = radius;

        // Draw the east edge of the square
        while (yPos + radius > 0)
        {
            adjacentPoints.add(new LocalPoint(point.getX() + (xPos * 128), point.getY() + (yPos * 128)));
            yPos--;
        }

        // Draw the south edge of the square
        while (xPos + radius > 0)
        {
            adjacentPoints.add(new LocalPoint(point.getX() + (xPos * 128), point.getY() + (yPos * 128)));
            xPos--;
        }

        // Draw the west edge of the square
        while (radius - yPos > 0)
        {
            adjacentPoints.add(new LocalPoint(point.getX() + (xPos * 128), point.getY() + (yPos * 128)));
            yPos++;
        }

        // Draw the north edge of the square
        while (radius - xPos > 0)
        {
            adjacentPoints.add(new LocalPoint(point.getX() + (xPos * 128), point.getY() + (yPos * 128)));
            xPos++;
        }

        return adjacentPoints;
    }

    private String getVectorAsString(LocalPoint origin, LocalPoint destination)
    {
        return (Math.abs(origin.getX() - destination.getX()) / 128) + " "
                + ((origin.getX() - destination.getX() < 0) ? "EAST" : "WEST") + ", "
                + Math.abs((origin.getY() - destination.getY()) / 128) + " "
                + ((origin.getY() - destination.getY() < 0) ? "NORTH" : "SOUTH");
    }

    private int[] getAbsoluteVector(LocalPoint origin, LocalPoint destination)
    {
        int[] vector = new int[2];
        vector[0] = Math.abs(origin.getX() - destination.getX());
        vector[1] = Math.abs(origin.getY() - destination.getY());
        return vector;
    }

    private boolean doesInventoryHaveBrumaRoot()
    {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        Item[] inventoryItems;
        boolean hasBrumaRoot = false;

        if (inventory != null)
        {
            inventoryItems = inventory.getItems();
            for (Item item : inventoryItems)
            {
                if (item.getId() == ItemID.BRUMA_ROOT)
                {
                    hasBrumaRoot = true;
                }
            }
        }

        return hasBrumaRoot;
    }

    private int getInventorySlotsFree()
    {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        Item[] inventoryItems = inventory.getItems();
        return 28 - inventoryItems.length;
    }

    private boolean isInventoryFull()
    {
        return getInventorySlotsFree() == 0;
    }

    private boolean playerFiremaking()
    {
        if ((this.playerAnimatingState == PlayerAnimatingState.IDLE_AFTER_FIREMAKING) ||
                (this.playerAnimatingState == PlayerAnimatingState.IDLE_AFTER_FIREMAKING_TRANSIENT_1) ||
                (this.playerAnimatingState == PlayerAnimatingState.IDLE_AFTER_FIREMAKING_TRANSIENT_2) ||
                (this.playerAnimatingState == PlayerAnimatingState.FIREMAKING))
        {
            return true;
        }
        return false;
    }

    // Not a comprehensive pathfinding algo, just basic Y axis first, X axis second
    private List<LocalPoint> generatePath(LocalPoint origin, LocalPoint destination)
    {
        List<LocalPoint> pathPoints = new ArrayList<>();
        int originX = origin.getX();
        int originY = origin.getY();

        // Calculate N/S path first
        while (Math.abs(originY - destination.getY()) > 128)
        {
            pathPoints.add(new LocalPoint(originX, originY));
            if (originY > destination.getY())
            {
                originY -= 128;
            }
            else
            {
                originY += 128;
            }
        }
        // Then W/E path
        while (Math.abs(originX - destination.getX()) > 128)
        {
            pathPoints.add(new LocalPoint(originX, originY));
            if (originX > destination.getX())
            {
                originX -= 128;
            }
            else
            {
                originX += 128;
            }
        }

        return pathPoints;
    }

    private List<LocalPoint> generatePathToPlayer(LocalPoint destination)
    {
        return generatePath(client.getLocalPlayer().getLocalLocation(), destination);
    }

    private LocalPoint calculateMidpoint(LocalPoint point1, LocalPoint point2)
    {
        int centreX = point1.getX() > point2.getX() ?
                point2.getX() + (point1.getX() - point2.getX()) / 2
                : point1.getX() + (point2.getX() - point1.getX()) / 2;

        int centreY = point1.getY() > point2.getY() ?
                point2.getY() + (point1.getY() - point2.getY()) / 2
                : point1.getY() + (point2.getY() - point1.getY()) / 2;

        return new LocalPoint(centreX, centreY);
    }

    private void calculateSafeSquares()
    {
        this.safeSquares.clear();

        boolean isEndangered = false;
        for (SnowfallPoint snowfallPoint : this.snowfallEventsWithDamageToEscape)
        {

            if ((isAtPlayer(snowfallPoint.getSnowfallPoint())) && (!isEndangered))
            {
                // Once we know the player is endangered, we do not need to keep iterating through this list
                isEndangered = true;
            }
        }

        if (isEndangered)
        {
            // Spiral outwards from centre until we find a square that isn't in danger
            boolean safeFound = false;
            int radius = 1; // n
            while (!safeFound)
            {
                List<LocalPoint> squaresToCheck = adjacentTilesOfRadiusX(
                        client.getLocalPlayer().getLocalLocation(), radius);

                // For each square n (radius) squares from player, we check to see if it is safe
                for (LocalPoint square : squaresToCheck)
                {
                    safeFound = true;
                    for (SnowfallPoint dangerSquare : this.snowfallEventsWithDamageToEscape)
                    {
                        // If safeFound is never set to false during this for loop, then we have found a safe square
                        int[] proximityVector = getAbsoluteVector(dangerSquare.getSnowfallPoint(), square);
                        if ((proximityVector[0] == 0) && (proximityVector[1] == 0))
                        {
                            safeFound = false;
                        }
                    }

                    // As we have found a safe square, set it to the class' safeSquare variable (for this tick)
                    // Following this, we will break out of the outer for loop (through inaction), and then
                    // break out of the while loop and then execution will leave this method
                    if (safeFound)
                    {
                        this.safeSquares.add(square);
                    }
                }

                // Expand the spiral outwards (n++)
                radius++;
            }
            System.out.println();
        }
    }

    private void determineIfPlayerInWintertodtArea()
    {
        int xLoc = client.getLocalPlayer().getWorldLocation().getX();
        int yLoc = client.getLocalPlayer().getWorldLocation().getY();

        this.playerInWintertodtArea = (xLoc >= WINTERTODT_AREA_LOWER_BOUND_X && xLoc <= WINTERTODT_AREA_UPPER_BOUND_X &&
                yLoc >= WINTERTODT_AREA_LOWER_BOUND_Y && yLoc <= WINTERTODT_AREA_UPPER_BOUND_Y);
    }

    private void getInfoFromWidgets()
    {
        // 396 child 3 = time left, 7 = points, 21 = energy
        try
        {
            Widget timeTillWintertodtGame = client.getWidget(396, 3);
            if ((timeTillWintertodtGame != null) && (timeTillWintertodtGame.getText() != null))
            {
                String widgetMessage = timeTillWintertodtGame.getText();
                if (!widgetMessage.isEmpty())
                {
                    this.canSeeTimeTillStartWidget = true;
                    widgetMessage = widgetMessage.split(": ")[1];
                    if (!widgetMessage.contains("min"))
                    {
                        widgetMessage = widgetMessage.split("sec")[0];
                    }
                    else
                    {
                        String[] minAndSec = widgetMessage.split("min ");
                        minAndSec[1] = minAndSec[1].split("sec")[0];
                        widgetMessage = "" + ((Integer.parseInt(minAndSec[0]) * 60) + Integer.parseInt(minAndSec[1]));
                    }

                    if (Integer.parseInt(widgetMessage) <= GAME_STARTING_SOON_SECONDS)
                    {
                        this.gameStartingSoon = true;
                        this.stringTimeTillGameStarts = widgetMessage;
                    }
                    else
                    {
                        this.gameStartingSoon = false;
                        this.stringTimeTillGameStarts = "";
                    }
                }
                else
                {
                    this.canSeeTimeTillStartWidget = false;
                }
            }


            Widget wintertodtEnergy = client.getWidget(396, 21);
            if ((wintertodtEnergy != null) && (wintertodtEnergy.getText() != null))
            {
                String widgetMessage = wintertodtEnergy.getText();
                if (!widgetMessage.isEmpty())
                {
                    widgetMessage = widgetMessage.split(": ")[1].split("%")[0];
                    this.wintertodtHp = Integer.parseInt(widgetMessage);

                    if (this.wintertodtHp == 0 && this.gameActive)
                    {
                        this.gameActive = false;
                    }

                    if (!this.gameActive && this.wintertodtHp > 0)
                    {
                        this.gameActive = true;
                    }
                }
            }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            log.error("String splitting failure in Wintertodt widget, perhaps a widget message format changed?");
        }
    }
}