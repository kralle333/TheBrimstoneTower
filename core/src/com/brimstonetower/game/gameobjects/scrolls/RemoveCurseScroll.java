package com.brimstonetower.game.gameobjects.scrolls;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.brimstonetower.game.gamestateupdating.GameStateUpdater;
import com.brimstonetower.game.gui.GameConsole;

public class RemoveCurseScroll extends Scroll
{


    public RemoveCurseScroll(TextureRegion region, String unidentifiedName)
    {
        super("Scroll of Exorcism","Removes all curses from the backpack",false,region,unidentifiedName,Type.Instant,2);
    }

    @Override
    public void use()
    {
        GameStateUpdater.inventory.removeCurses();
        GameConsole.addMessage("A magical light cleanse the items in your backpack");
    }


}
