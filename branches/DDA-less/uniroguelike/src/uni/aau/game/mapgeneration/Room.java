package uni.aau.game.mapgeneration;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import uni.aau.game.gameobjects.Monster;
import uni.aau.game.helpers.AssetManager;
import uni.aau.game.helpers.TileSetCoordinate;

import java.util.ArrayList;

public class Room
{
    private Tile[][] _tiles;
    private ArrayList<Tile> _doors = new ArrayList<Tile>();
    public ArrayList<Tile> getDoors(){return _doors;}
    private ArrayList<Corridor> _corridors = new ArrayList<Corridor>();
    public void addCorridor(Corridor corridor)
    {
        _corridors.add(corridor);
    }
    public ArrayList<Corridor> getCorridors(){return _corridors;}
    private int _x;
    public int getX(){return _x;}
    private int _y;
    public int getY(){return _y;}
    private int _width;
    public int getWidth(){return _width;}
    private int _height;
    public int getHeight(){return _height;}
    public int getRightSide(){return _x+_width-1;}
    public int getBottomSide(){return _y+_height-1;}

    private Tile.LightAmount _lighting;
    public Tile.LightAmount getLighting(){return  _lighting;}
    private Tile _playerTile;
    public boolean isLeftOf(Room otherRoom)
    {
        return getRightSide()<otherRoom.getX();
    }

    public boolean isAboveOf(Room otherRoom)
    {
        return getBottomSide()<otherRoom.getY();
    }

    public Room(int x, int y, int width, int height)
    {
        _x = x;
        _y = y;
        _width = width;
        _height = height;
        _lighting = Tile.LightAmount.Non;
        _tiles = new Tile[_width][_height];
    }

    public ArrayList<Monster> getMonsters()
    {
        final ArrayList<Monster> monstersInRoom = new ArrayList<Monster>();
        monstersInRoom.clear();
        for(int x = 0;x<_width;x++)
        {
            for(int y = 0;y<_height;y++)
            {
                if(_tiles[x][y].getCharacter() != null && _tiles[x][y].getCharacter() instanceof Monster)
                {
                    monstersInRoom.add((Monster)_tiles[x][y].getCharacter());
                }
            }
        }
        return monstersInRoom;
    }
    public ArrayList<Tile> getTilesWithItems()
    {
        final ArrayList<Tile> tilesWithItems = new ArrayList<Tile>();
        tilesWithItems.clear();
        for(int x = 0;x<_width;x++)
        {
            for(int y = 0;y<_height;y++)
            {
                if(_tiles[x][y].containsItem())
                {
                    tilesWithItems.add(_tiles[x][y]);
                }
            }
        }
        return tilesWithItems;
    }
    public Tile getRandomTile()
    {
        Tile randomTile = _tiles[RandomGen.getRandomInt(1, _width - 2)][RandomGen.getRandomInt(1, _height - 2)];
        return randomTile;
    }
    public Tile getStairCase()
    {
        for(int x = 0;x<_width;x++)
        {
            for(int y = 0; y < _height; y++)
            {
                if(_tiles[x][y].getType() == Tile.Types.StairCase)
                {
                    return _tiles[x][y];
                }
            }
        }
        return null;
    }



    private void setLightingOnTiles(Tile.LightAmount light)
    {
        _lighting = light;
        for(int x = 0;x<_width;x++)
        {
            for(int y = 0;y<_height;y++)
            {
                _tiles[x][y].setLight(_lighting);
            }
        }
    }

    public void reveal()
    {
        if(_lighting == Tile.LightAmount.Non)
        {
            setLightingOnTiles(Tile.LightAmount.Shadow);
        }
    }

    public void setWallsAndFloor()
    {
        for(int x = 0;x<_width;x++)
        {
            for(int y=0;y<_height;y++)
            {
                if(y==0)
                {
                    if(x==0){ _tiles[x][y] = new Tile(Tile.Types.Wall,x+_x,y+_y,this, AssetManager.getTileSetPosition("nwWall"));}
                    else if(x==1){_tiles[x][y] = new Tile(Tile.Types.Wall,x+_x,y+_y,this, AssetManager.getTileSetPosition("nwRoomWall"));}
                    else if(x==_width-1){_tiles[x][y] = new Tile(Tile.Types.Wall,x+_x,y+_y,this, AssetManager.getTileSetPosition("neWall"));}
                    else if(x==_width-2){_tiles[x][y] = new Tile(Tile.Types.Wall,x+_x,y+_y,this, AssetManager.getTileSetPosition("neRoomWall"));}
                    else{_tiles[x][y] = new Tile(Tile.Types.Wall,x+_x,y+_y,this, AssetManager.getTileSetPosition("nRoomWall"));}

                }
                else if(y==_height-1)
                {
                    if(x==0){ _tiles[x][y] = new Tile(Tile.Types.Wall,x+_x,y+_y,this,AssetManager.getTileSetPosition("swWall"));}
                    else if(x==_width-1){_tiles[x][y] = new Tile(Tile.Types.Wall,x+_x,y+_y,this, AssetManager.getTileSetPosition("seWall"));}
                    else{_tiles[x][y] = new Tile(Tile.Types.Wall,x+_x,y+_y,this, AssetManager.getTileSetPosition("sWall"));}
                }
                else if(x==0)
                {
                    _tiles[x][y] = new Tile(Tile.Types.Wall,x+_x,y+_y,this, AssetManager.getTileSetPosition("wWall"));
                }
                else if(x==_width-1)
                {
                    _tiles[x][y] = new Tile(Tile.Types.Wall,x+_x,y+_y,this,AssetManager.getTileSetPosition("eWall"));
                }
                else
                {
                    //Random floor
                    _tiles[x][y] = new Tile(Tile.Types.Floor,x+_x,y+_y,this,new TileSetCoordinate(3+RandomGen.getRandomInt(0,2),3));
                }
            }
        }
        for(int x=0;x<getWidth();x++)
        {
            for(int y= 0;y<getHeight();y++)
            {
                setNeighbours(_tiles[x][y]);
            }
        }
    }

    private Vector2[] directions = new Vector2[]{new Vector2(-1,0),new Vector2(0,-1),
                                            new Vector2(1,0),new Vector2(0,1),
                                            new Vector2(-1,-1),new Vector2(-1,1),
                                            new Vector2(1,-1),new Vector2(1,1)};
    private void setNeighbours(Tile tile)
    {
        int x = (int)tile.getX()-_x;
        int y = (int)tile.getY()-_y;

        ArrayList<Tile> walkableNeighbours = new ArrayList<Tile>();
        ArrayList<Tile> nonWalkableNeighbours = new ArrayList<Tile>();
        for(int d = 0;d<directions.length;d++)
        {
            int nX=x+(int)directions[d].x;
            int nY=y+(int)directions[d].y;
            if(nX>0 && nX<_width-1 && nY>0 && nY<_height-1)
            {
                walkableNeighbours.add(_tiles[nX][nY]);
            }
            else if(nX>=0 && nX<_width && nY>=0 && nY<_height)
            {
                nonWalkableNeighbours.add(_tiles[nX][nY]);
            }
        }
        tile.setNonWalkableNeighbours(nonWalkableNeighbours);
        tile.setWalkableNeighbours(walkableNeighbours);
    }
    public enum WallSide{West,East,South,North}
    public Tile getRandomEmptyWall(WallSide side)
    {
        Tile returnedTile = null;
        switch(side)
        {
            case West:
                do
                {
                    returnedTile = _tiles[0][RandomGen.getRandomInt(1,_height-2)];
                }while(returnedTile.getType() != Tile.Types.Wall);
                break;
            case East:
                do
                {
                    returnedTile = _tiles[_width-1][RandomGen.getRandomInt(1,_height-2)];
                }while(returnedTile.getType() != Tile.Types.Wall);
                break;
            case South:
                do
                {
                    returnedTile = _tiles[RandomGen.getRandomInt(1,_width-2)][_height-1];
                }while(returnedTile.getType() != Tile.Types.Wall);
                break;
            case North:
                do
                {
                    returnedTile = _tiles[RandomGen.getRandomInt(1,_width-2)][0];
                }while(returnedTile.getType() != Tile.Types.Wall);
                break;
        }
        return returnedTile;
    }


    public void createDoorAndConnect(Tile doorTile,Tile corridorTile,WallSide orientation)
    {
        if(doorTile.getRoom() != this)
        {
            throw new RuntimeException("Door tile did not match room");
        }

        switch (orientation)
        {
            case West:doorTile.setTextureRegion(new Vector2(1,4));break;
            case East:doorTile.setTextureRegion(new Vector2(1,3));break;
            case North:doorTile.setTextureRegion(new Vector2(0,3));break;
            case South:doorTile.setTextureRegion(new Vector2(0,4));break;
        }

        //Get local coordinates
        int doorX = (int)doorTile.getX()-_x;
        int doorY = (int)doorTile.getY()-_y;

        //Find the tile that is adjacent to the door
        Tile adjacentToDoor = null;
        if(doorX == 0)//Left side
        {
            adjacentToDoor = _tiles[doorX+1][doorY];
        }
        else if(doorY == 0)//Top side
        {
            adjacentToDoor = _tiles[doorX][doorY+1];
        }
        else if(doorX == _width-1)
        {
            adjacentToDoor=_tiles[doorX-1][doorY];
        }
        else if(doorY == _height-1)
        {
            adjacentToDoor= _tiles[doorX][doorY-1];
        }
        //Add neighbours
        adjacentToDoor.addWalkableNeighbour(doorTile);
        doorTile.addWalkableNeighbour(adjacentToDoor);
        doorTile.addWalkableNeighbour(corridorTile);
        corridorTile.addWalkableNeighbour(doorTile);
        doorTile.setType(Tile.Types.Door);
        _doors.add(doorTile);

    }

    public Tile getTile(int worldX, int worldY)
    {
        int localX = worldX-_x;
        int localY = worldY-_y;
        if(localX>=0 && localY >= 0 && localX<_tiles.length && localY<_tiles[0].length)
        {
            return _tiles[localX][localY];
        }
        else
        {
            return null;
        }
    }
    public void draw(SpriteBatch batch)
    {
        for(int x = 0;x<_width;x++)
        {
            for(int y= 0;y<_height;y++)
            {
                _tiles[x][y].draw(batch);
            }
        }
        for(Tile door : _doors)
        {
            door.draw(batch);
        }

        batch.setColor(Color.WHITE);
    }
}