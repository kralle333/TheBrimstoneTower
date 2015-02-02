package com.brimstonetower.game.helpers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.brimstonetower.game.gameobjects.*;
import com.brimstonetower.game.gui.GameConsole;
import com.brimstonetower.game.gui.Inventory;
import com.brimstonetower.game.gameobjects.items.*;
import com.brimstonetower.game.managers.AssetManager;
import com.brimstonetower.game.managers.ItemManager;
import com.brimstonetower.game.map.DungeonMap;
import com.brimstonetower.game.map.Tile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

public class GameStateUpdater
{

    private class DamageIndicator
    {
        public float fallSpeedX;
        public float fallSpeedY;
        public Vector2 position;
        public String textToShow;
        public Color color;
        public float secondsActive;
        public float timeLeft;
        public boolean isVisible(){return timeLeft>0;}

        public DamageIndicator(float fallSpeedX, float fallSpeedY,Vector2 position, String textToShow, Color color, float secondsActive)
        {
            this.textToShow = textToShow;
            this.fallSpeedX = fallSpeedX;
            this.fallSpeedY = fallSpeedY;
            this.position =position;
            this.color = color;
            this.secondsActive = secondsActive;
            this.timeLeft = secondsActive;
        }

        public void update()
        {
            position.x+=fallSpeedX;
            position.y+=fallSpeedY;
            timeLeft-=Gdx.app.getGraphics().getDeltaTime();
        }
        public void draw(BitmapFont font, SpriteBatch batch)
        {
            font.setColor(color.r, color.g, color.b, timeLeft / secondsActive);
            font.draw(batch, textToShow, (int) position.x, (int) position.y);
            font.setColor(Color.WHITE);
        }
    }

    private Player _player;
    private ArrayList<Monster> _monsters;
    private ArrayList<Gas> _gasClouds = new ArrayList<Gas>();
    private ArrayList<DamageIndicator> _damageIndicators = new ArrayList<DamageIndicator>();
    private ArrayList<Trap> _traps = new ArrayList<Trap>();
    private Inventory _inventory;
    private DungeonMap _playedMap;

    private HashMap<GameCharacter, Integer> _characterTime = new HashMap<GameCharacter, Integer>();
    private PriorityQueue<GameCharacter> _characterTurn;

    public class CharacterSpeedComparator implements Comparator<GameCharacter>
    {
        @Override
        public int compare(GameCharacter lhs, GameCharacter rhs)
        {
            return lhs.getCostOfNextAction() - rhs.getCostOfNextAction();
        }
    }

    private static int _turn = 0;

    public static int getTurn()
    {
        return _turn;
    }

    private boolean _showingThrowingAnimation = false;

    public boolean isShowingAnimation()
    {
        return _showingThrowingAnimation;
    }

    //Thrown item variables
    private final float _throwAnimationSpeed = 16;
    private float _thrownObjectX;
    private float _thrownObjectY;
    private Item _thrownObject;
    private Tile _targetTile;
    private float _angleToTarget;

    //Select item dialog
    private boolean _selectItemDialog = false;
    public boolean isSelectingItemToIdentify()
    {
        return _selectItemDialog;
    }

    public void setGameState(ArrayList<Monster> monsters, Player player, Inventory inventory, DungeonMap playedMap, ArrayList<Trap> traps)
    {
        _turn = 1;
        _player = player;
        _monsters = monsters;

        _characterTime.clear();
        _characterTime.put(_player, 0);
        for (Monster monster : _monsters)
        {
            _characterTime.put(monster, 0);
        }
        _characterTurn = new PriorityQueue<GameCharacter>(_monsters.size() + 1, new CharacterSpeedComparator());

        _inventory = inventory;
        _playedMap = playedMap;
        _traps = traps;
        _gasClouds.clear();
        _player.getCurrentTile().setLight(Tile.LightAmount.Shadow, _player.getLanternStrength()*2, _player.getLanternStrength()*2);
        _player.getCurrentTile().setLight(Tile.LightAmount.Light, _player.getLanternStrength(), _player.getLanternStrength());
    }

    public void resumeGameStateUpdating()
    {
        if (_selectItemDialog)
        {
            _selectItemDialog = false;
        }
        else if (_showingThrowingAnimation)
        {
            _showingThrowingAnimation = false;
        }
    }

    //Update:
    public void updateGameState()
    {
        if (!_selectItemDialog && !_showingThrowingAnimation)
        {
            handleExecutionOfActions();
        }
        else if (_showingThrowingAnimation)
        {
            //Move the object closer to the target
            _thrownObjectX += (float) Math.cos(_angleToTarget) * _throwAnimationSpeed;
            _thrownObjectY += (float) Math.sin(_angleToTarget) * _throwAnimationSpeed;

            //If the object is close enough-> do effects
            if (Math.abs(_thrownObjectX - (_targetTile.getX() * 32)) <= _throwAnimationSpeed &&
                    Math.abs(_thrownObjectY - (_targetTile.getY() * 32)) <= _throwAnimationSpeed)
            {

                if (_thrownObject instanceof Potion)//Use the potion on the tile
                {
                    usePotion((Potion) _thrownObject, _targetTile.getCharacter(), _targetTile);
                }
                else if (_targetTile.getCharacter() != null && _thrownObject instanceof Weapon)
                {
                    Weapon thrownWeapon = (Weapon) _thrownObject;
                    int damage = 0;
                    if (thrownWeapon.isRanged() && RandomGen.getRandomInt(1, 100) > 20 + _targetTile.getCharacter().getDodgeRate())//Get ranged damage
                    {
                        damage = thrownWeapon.getRandomDamage();
                    }
                    else if (RandomGen.getRandomInt(1, 100) > 50 + _targetTile.getCharacter().getDodgeRate())//Get damage from a sword etc being thrown
                    {
                        damage = thrownWeapon.getRandomDamage() / 2;
                    }

                    if (damage > 0)
                    {
                        GameConsole.addMessage(_targetTile.getCharacter().getName() + " got " + damage + " from thrown " + thrownWeapon.getName());
                        _targetTile.getCharacter().damage(damage);

                    }
                    else
                    {
                        _targetTile.addItem(thrownWeapon);
                        GameConsole.addMessage(thrownWeapon.getName() + " landed on the floor");
                    }
                }
                else //Otherwise the item lands on the tile
                {
                    _targetTile.addItem(_thrownObject);
                    GameConsole.addMessage(_thrownObject.getName() + " landed on the floor");
                }
                _showingThrowingAnimation = false;
                _targetTile = null;
                _thrownObject = null;
                _thrownObjectX = 0;
                _thrownObjectY = 0;
            }
        }
    }

    //
    // ACTION EXECUTION
    //
    private float timeToNextTurn = 0.0f;
    private final float turnDurationInSeconds = 0.08f;

    private void handleExecutionOfActions()
    {
        GameAction playerAction;
        if (timeToNextTurn <= 0 && (playerAction = _player.getNextAction()) != null)
        {
            timeToNextTurn = turnDurationInSeconds;
            //Record the time of the next action of characters
            int playerActionCost = playerAction.getCost();
            executeAction(playerAction);
            for (Monster monster : _monsters)
            {
                monster.setNextAction(_player);
                //Let the time a monster has to act be the time the player's action take
                _characterTime.put(monster, _characterTime.get(monster) + playerActionCost);
                _characterTurn.add(monster);
            }

            //Execute actions, starting with the quickest ones, but only if the character has enough time
            while (!_characterTurn.isEmpty())
            {
                GameCharacter character = _characterTurn.peek();
                if (_characterTime.get(character) > 0 && !character.isDead())
                {

                    _characterTime.put(character, _characterTime.get(character) - character.getCostOfNextAction());
                    executeAction(character.getNextAction());
                    //Can only be a monster if there is time left,
                    // because the cost of the player's action = the time given to characters
                    if (_characterTime.get(character) > 0)
                    {
                        ((Monster) character).setNextAction(_player);
                    }
                }

                if (_characterTime.get(character) <= 0 || character.isDead())
                {
                    _characterTurn.poll();
                }
            }
            _turn++;

            //Update clouds and remove ones that are not yet active
            final ArrayList<Gas> _gassesToRemove = new ArrayList<Gas>();
            for (Gas gas : _gasClouds)
            {
                gas.update();
                if (gas.hasDisappeared())
                {
                    _gassesToRemove.add(gas);
                }
            }
            for (Gas gas : _gassesToRemove)
            {
                _gasClouds.remove(gas);
            }

            //Apply effects
            _player.updateEffects();
            for (Monster monster : _monsters)
            {
                monster.updateEffects();
            }
        }
        else if (timeToNextTurn > 0)
        {
            timeToNextTurn -= Gdx.graphics.getDeltaTime();
        }

    }

    private void executeAction(GameAction action)
    {
        switch (action.getType())
        {
            case Move:
                executeMoveAction(action);
                break;
            case Attack:
                executeAttack(action);
                break;
            case Equip:
                Item equipment = action.getTargetItem();
                action.getOwner().equip(equipment);
                _inventory.equip(equipment);
                break;
            case Unequip:
                Item unequipment = action.getTargetItem();
                action.getOwner().unequip(unequipment);
                _inventory.unequip(unequipment);
                break;
            case Wait:
                break;
            case PickUp:
                Item item = action.getTargetTile().pickupItem();
                _inventory.addItem(item);
                GameConsole.addMessage("Picked up item " + item.getName());
                break;
            case Drop:
                Item droppedItem = action.getTargetItem();
                _inventory.removeItem(droppedItem);
                _player.getCurrentTile().addItem(droppedItem);
                break;
            case Throw:
                _thrownObject = action.getTargetItem();
                _targetTile = action.getTargetTile();
                _showingThrowingAnimation = true;
                _thrownObjectX = action.getOwner().getCurrentTile().getX() * DungeonMap.TileSize;
                _thrownObjectY = action.getOwner().getCurrentTile().getY() * DungeonMap.TileSize;
                _angleToTarget = (float) Math.atan2(action.getTargetTile().getY() * DungeonMap.TileSize - _thrownObjectY, action.getTargetTile().getX() * 32 - _thrownObjectX);
                if (_angleToTarget < 0)
                {
                    _angleToTarget += 2 * Math.PI;
                }
                if (_thrownObject instanceof Weapon && ((Weapon) _thrownObject).isRanged())
                {
                    ((Weapon) _thrownObject).decreaseRangedAmmo();
                    if (((Weapon) _thrownObject).getAmmoCount() == 0)
                    {
                        _inventory.removeItem(_thrownObject);
                    }
                }
                else
                {
                    _inventory.removeItem(_thrownObject);
                }
                break;
            case Use:
                executeUseAction(action);
                break;
            case Search:
                GameConsole.addMessage(action.getOwner().getName() + " searches the floor");
                for (Tile tile : action.getTargetTile().getWalkableNeighbours())
                {
                    Trap trap = tile.getTrap();
                    if (trap != null && !trap.hasBeenActivated())
                    {
                        trap.reveal();
                        GameConsole.addMessage("A trap was spotted");
                    }
                }
                break;
        }
        //Remove the action as being the next action
        action.getOwner().clearCurrentAction();
    }

    private void executeAttack(GameAction action)
    {
        GameCharacter defender = action.getTargetCharacter();
        GameCharacter attacker = action.getOwner();
        attacker.attack(defender);
        Color indicatorColor = Color.MAGENTA;

        if (defender instanceof Monster)
        {
            indicatorColor = Color.GREEN;
            if (defender.isDead())
            {
                _player.retrieveExperience((Monster) (defender));
                _monsters.remove(defender);
            }
        }
        else if (defender instanceof Player)
        {
            indicatorColor = Color.RED;
            if (defender.isDead())
            {
                _player.setKilledBy(attacker.getName());
            }
        }
        _damageIndicators.add(new DamageIndicator(0,0.8f,new Vector2(defender.getPosition().x+(DungeonMap.TileSize/2),defender.getPosition().y),String.valueOf(attacker.getDealtDamage()), indicatorColor, 0.5f));

    }

    private void executeMoveAction(GameAction action)
    {
        Tile newTile = action.getTargetTile();
        GameCharacter character = action.getOwner();
        if (character == _player)
        {
            Trap trapOnTile = newTile.getTrap();
            if (trapOnTile != null && !trapOnTile.hasBeenActivated())
            {
                int chanceToBeat = RandomGen.getRandomInt(1, 100);
                if (chanceToBeat >= 5)
                {
                    GameConsole.addMessage(_player.getName() + " spotted a trap");
                    _player.clearNextActions();
                    return;
                }
                else
                {
                    String trapMessage = _player.getName() + " stepped on a trap ";

                    chanceToBeat = RandomGen.getRandomInt(0, 100);
                    if (_player.getDodgeRate() >= chanceToBeat)
                    {
                        trapMessage += ", but didn't activate it";
                    }
                    else
                    {
                        trapMessage += "and activated it";
                        trapOnTile.activate();
                        if (trapOnTile.hasCreatedGas())
                        {
                            _gasClouds.add(trapOnTile.retrieveCreatedGas());
                        }
                        //Player was hurt by trap
                        if (_player.isDead())
                        {
                            _player.setKilledBy("trap");
                        }
                    }
                    GameConsole.addMessage(trapMessage);
                }
            }
            _inventory.step();
            _player.moveTo(newTile);
        }
        else
        {
            character.moveTo(newTile);
        }

    }

    private void executeUseAction(GameAction action)
    {
        if (action.getOwner() == _player)
        {
            //Remove the item from the inventory
            _inventory.removeItem(action.getTargetItem());

            //Identify items similar to the used one
            if (!action.getTargetItem().isIdentified())
            {
                //Identify the item as it has been used
                action.getTargetItem().identify();

                _inventory.identifyItems(action.getTargetItem());
                ItemManager.identifyItem(action.getTargetItem());
            }

            GameConsole.addMessage("Player used " + action.getTargetItem().getName());
        }
        if (action.getTargetItem() instanceof Scroll)
        {
            useScroll((Scroll) action.getTargetItem());
        }
        else if (action.getTargetItem() instanceof Potion)
        {
            usePotion((Potion) action.getTargetItem(), action.getOwner(), action.getTargetTile());
        }
    }

    //
    // Scroll and potion activation
    //
    private void useScroll(Scroll scroll)
    {
        switch (scroll.getType())
        {
            case Identify:
                _selectItemDialog = true;
                _inventory.show();
                break;
            case Teleport:
                _player.moveTo(_playedMap.getRandomEmptyTile());
                GameConsole.addMessage("Player was teleported to a random tile");
                break;
            case RemoveCurse:
                _inventory.removeCurses();
                GameConsole.addMessage("A magical light cleanses your backpack");
                break;
            case Mapping:
                _playedMap.revealAll();
                for (Trap trap : _traps)
                {
                    trap.reveal();
                }
                GameConsole.addMessage("Map has been revealed");
                break;
            default:
                throw new IllegalArgumentException("Invalid scroll type: " + scroll.getName());
        }
    }

    private void usePotion(Potion potion, GameCharacter target, Tile tile)
    {
        String potionColor = ColorHelper.convertColorToString(potion.getColor());
        if (potion.getEffect().isGas())
        {
            GameConsole.addMessage("A " + ColorHelper.convertColorToString(potion.getEffect().getColor()) + " gas spreads from the bottle");
            _gasClouds.add(new Gas(tile, potion.getEffect()));
        }
        else if (target != null)
        {
            if (tile == null)
            {
                GameConsole.addMessage(target.getName() + " drank the " +potionColor+ " potion");
            }
            target.giveEffect(potion.getEffect());
        }
        else
        {
            GameConsole.addMessage(potionColor + " colored liquid runs out of the vial");
        }

    }


    //
    //Drawing
    //
    public void drawGameState(SpriteBatch batch)
    {
        _playedMap.draw(batch);

        for (Trap trap : _traps)
        {
            trap.draw(batch);
        }

        for (Gas gas : _gasClouds)
        {
            gas.draw(batch);
        }
        _player.draw(batch);
        for (Monster monster : _monsters)
        {
            monster.draw(batch);
        }
        if (_showingThrowingAnimation)
        {
            _thrownObject.draw(batch, _thrownObjectX, _thrownObjectY);
        }
        final ArrayList<DamageIndicator> damageIndicatorsToRemove = new ArrayList<DamageIndicator>();
        damageIndicatorsToRemove.clear();
        for (DamageIndicator damageIndicator : _damageIndicators)
        {
            damageIndicator.update();
            if(damageIndicator.isVisible())
            {
                damageIndicator.draw(AssetManager.getFont("description"),batch);
            }
            else
            {
                damageIndicatorsToRemove.add(damageIndicator);
            }
        }
        for(DamageIndicator toRemove : damageIndicatorsToRemove)
        {
            _damageIndicators.remove(toRemove);
        }

    }

}
