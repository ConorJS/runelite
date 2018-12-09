package net.runelite.client.plugins.objecthighlighter;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ObjectIndicatorsInput implements KeyListener
{
    private static final int HOTKEY = KeyEvent.VK_SHIFT;

    @Inject
    private ObjectHighlighterPlugin plugin;

    @Override
    public void keyTyped(KeyEvent e)
    {

    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == HOTKEY)
        {
            plugin.updateNpcMenuOptions(true);
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        if (e.getKeyCode() == HOTKEY)
        {
            plugin.updateNpcMenuOptions(false);
        }
    }
}

