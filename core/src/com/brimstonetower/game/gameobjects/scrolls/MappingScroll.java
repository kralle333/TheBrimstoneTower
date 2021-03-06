package com.brimstonetower.game.gameobjects.scrolls;


import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.brimstonetower.game.gamestateupdating.GameStateUpdater;
import com.brimstonetower.game.gui.GameConsole;
import com.brimstonetower.game.map.Tile;

public class MappingScroll extends Scroll
{

    public MappingScroll(TextureRegion region, String unidentifiedName)
    {
        super("Scroll of Magic Mapping","Magically maps the whole dungeon",false,region,unidentifiedName,Type.Instant,1);
    }

    @Override
    public void use()
    {

        GameStateUpdater.playedMap.revealAll();
        GameConsole.addMessage("Map has been revealed");

        Tile playerTile = GameStateUpdater.player.getCurrentTile();
        playerTile.updateLight(GameStateUpdater.player);
    }
}
