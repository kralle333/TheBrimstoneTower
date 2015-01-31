package com.brimstonetower.game.gameobjects;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.brimstonetower.game.helpers.AssetManager;
import com.brimstonetower.game.helpers.Effect;
import com.brimstonetower.game.helpers.TileSetCoordinate;
import com.brimstonetower.game.mapgeneration.DungeonMap;
import com.brimstonetower.game.mapgeneration.RandomGen;
import com.brimstonetower.game.mapgeneration.Tile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Gas
{
    private GameCharacter.StatusEffect _statusEffect;
    private Effect _effect;
    private Color _color;
    private HashSet<Tile> _previousGasTiles = new HashSet<Tile>();
    private HashMap<Tile, Integer> _gasDensityMap = new HashMap<Tile, Integer>();
    private HashMap<Tile, TextureRegion> _gasTextures = new HashMap<Tile, TextureRegion>();

    private HashMap<Tile, Integer> _toAdd = new HashMap<Tile, Integer>();
    private HashSet<Tile> _tilesToRemove = new HashSet<Tile>();

    private final int _reductionAmount = 3;
    private final int _getSmallerChance = 40;
    private final int _spreadChance = 75;

    public boolean hasDisappeared()
    {
        return _gasDensityMap.isEmpty();
    }

    public Gas(Tile tile, Effect effect)
    {
        _effect = effect;
        _gasDensityMap.put(tile, 10);
        _gasTextures.put(tile, AssetManager.getTextureRegion("gas", getGasDensityTexture(10), 32, 32));
        _color = effect.getColor();
    }
    public Gas(Tile tile, GameCharacter.StatusEffect effect, int density)
    {
        _gasDensityMap.put(tile, density);
        _gasTextures.put(tile, AssetManager.getTextureRegion("gas", getGasDensityTexture(density), 32, 32));

        _statusEffect = effect;
        switch (_statusEffect)
        {
            case Poisoned:
                _color = new Color(0.87f, 0, 1f, 1);
                break;
            case Paralysed:
                _color = new Color(0.9f, 0.9f, 0, 1);
                break;
            default:
                Gdx.app.log("Gas", "Unknown cloud type! " + _statusEffect);
        }
    }

    public void update()
    {

        for (Tile gasTile : _gasDensityMap.keySet())
        {
            if(gasTile.getCharacter()!=null)
            {
                gasTile.getCharacter().giveEffect(_effect);
            }
            for (Tile neighbour : gasTile.getWalkableNeighbours())
            {
                if (_gasDensityMap.get(neighbour) == null && !_previousGasTiles.contains(neighbour))
                {
                    if (_gasDensityMap.get(gasTile) > _reductionAmount && RandomGen.getRandomInt(0, 100) <= _spreadChance)
                    {
                        _toAdd.put(neighbour, (_gasDensityMap.get(gasTile) - _reductionAmount));
                    }
                }
            }
            if (_gasDensityMap.get(gasTile) > 0)
            {
                //Chance of getting smaller
                if (RandomGen.getRandomInt(0, 100) <= _getSmallerChance)
                {
                    _gasDensityMap.put(gasTile, _gasDensityMap.get(gasTile) - _reductionAmount);
                    if (_gasDensityMap.get(gasTile) <= 0)
                    {
                        _tilesToRemove.add(gasTile);
                    }
                    else
                    {
                        _gasTextures.put(gasTile, AssetManager.getTextureRegion("gas", getGasDensityTexture(_gasDensityMap.get(gasTile)), 32, 32));
                    }
                }
            }
        }
        for (Map.Entry<Tile, Integer> entry : _toAdd.entrySet())
        {
            _gasDensityMap.put(entry.getKey(), entry.getValue());
            _gasTextures.put(entry.getKey(), AssetManager.getTextureRegion("gas", getGasDensityTexture(entry.getValue()), 32, 32));
        }
        _toAdd.clear();
        for (Tile toRemove : _tilesToRemove)
        {
            _gasDensityMap.remove(toRemove);
            _previousGasTiles.add(toRemove);
            _gasTextures.remove(toRemove);
        }
        _tilesToRemove.clear();
    }

    private TileSetCoordinate getGasDensityTexture(int density)
    {
        if (density >= 1 && density <= 3)
        {
            return AssetManager.getTileSetPosition("lvl1Gas");
        }
        else if (density >= 4 && density <= 8)
        {
            return AssetManager.getTileSetPosition("lvl2Gas");
        }
        else
        {
            return AssetManager.getTileSetPosition("lvl3Gas");
        }
    }

    public void draw(SpriteBatch batch)
    {
        batch.setColor(_color);
        for (Tile tile : _gasDensityMap.keySet())
        {
            batch.draw(_gasTextures.get(tile), tile.getX() * DungeonMap.TileSize, tile.getY() * DungeonMap.TileSize, DungeonMap.TileSize, DungeonMap.TileSize);
        }
        batch.setColor(Color.WHITE);
    }
}
