package net.runelite.client.plugins.objecthighlighter;

import com.google.common.eventbus.Subscribe;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ObjectHighlighterPlugin
{
    private static final String TAG = "Highlight";

    private static final MenuAction GAME_OBJECT_MENU_OPTION = MenuAction.GAME_OBJECT_FIRST_OPTION;

    private static final Integer OBJECT_IDS_COVERED = 33456;
    private static final Integer OBJECT_ID_CACHE_DEPTH = 128;

    @Inject
    private ObjectHighlighterConfig config;

    @Inject
    private ObjectHighlighterOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private Client client;

    @Inject
    private MenuManager menuManager;

    private boolean hotKeyPressed = false;

    private WorldPoint lastPlayerLocation;

    /**
     * Tracks every single object upon spawn/despawn
     */
    private final GameObject[][] objectCache = new GameObject[OBJECT_IDS_COVERED][OBJECT_ID_CACHE_DEPTH];

    public GameObject[] getCachedObjects(int objectId)
    {
        if (objectId <= OBJECT_ID_CACHE_DEPTH)
        {
            return objectCache[objectId];
        }
        else
        {
            throw new IllegalArgumentException("Object ID out of range when accessing Object Cache: " + objectId);
        }
    }

    /**
     * NPC ids marked with the Tag option
     */
    private final Set<Integer> objectTags = new HashSet<>();

    private final List<GameObject> highlightedObjects = new ArrayList<>();

    void updateNpcMenuOptions(boolean pressed)
    {
        if (!config.isTaggingEnabled())
        {
            return;
        }

        if (pressed)
        {
            menuManager.addNpcMenuOption(TAG);
        }
        else
        {
            menuManager.removeNpcMenuOption(TAG);
        }

        hotKeyPressed = pressed;
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject object = event.getGameObject();

        if (object.getId() != -1)
        {
            GameObject[] objects = objectCache[object.getId()];
            for (int i = 0; i < objects.length; i++)
            {
                if (objects[i] == null)
                {
                    objects[i] = object;

                    return;
                }
            }
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectSpawned event)
    {
        GameObject object = event.getGameObject();

        if (object.getId() != -1)
        {
            GameObject[] objects = objectCache[object.getId()];
            for (int i = 0; i < objects.length; i++)
            {
                if (objects[i].getHash() == object.getHash())
                {
                    objects[i] = null;

                    return;
                }
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked click)
    {
        if (!config.isTaggingEnabled())
        {
            return;
        }

        if (click.getMenuOption().equals(TAG) && GAME_OBJECT_MENU_OPTION.equals(click.getMenuAction()))
        {
            final int id = click.getId();
            final boolean removed = objectTags.remove(id);
            final GameObject[] cachedObjects = this.getCachedObjects(id);
            final GameObject object = cachedObjects[id];

            if (object != null)
            {
//                if (removed)
//                {
//                    highlightedObjects.remove(npc);
//                    memorizedNpcs.remove(npc.getIndex());
//                }
//                else
//                {
//                    memorizeNpc(npc);
//                    objectTags.add(id);
//                    highlightedObjects.add(npc);
//                }

                click.consume();
            }
        }
    }

    private void memorizeObject(GameObject object)
    {

    }

}
