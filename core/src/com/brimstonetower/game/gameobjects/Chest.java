package com.brimstonetower.game.gameobjects;


import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.brimstonetower.game.gui.GameConsole;
import com.brimstonetower.game.helpers.RandomGen;
import com.brimstonetower.game.managers.AssetManager;
import com.brimstonetower.game.map.DungeonMap;
import com.brimstonetower.game.map.Tile;

import java.util.ArrayList;

public class Chest extends BreakableObject
{
    private ArrayList<Item> _droppedItems = new ArrayList<Item>();

    public Chest(int type)
    {
        super("Chest",AssetManager.getTextureRegion("tile","chest-"+String.valueOf(type), DungeonMap.TileSize,DungeonMap.TileSize));
    }
    public void addItemToDrop(Item item)
    {
        _droppedItems.add(item);
    }

    @Override
    public void destroy()
    {
        super.destroy();

        GameConsole.addMessage("The chest was smashed open");
        if (_droppedItems.size() > 0)
        {
            for (Item item : _droppedItems)
            {
                tile.addItem(item);
            }

        }
    }
}
