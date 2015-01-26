package uni.aau.game.helpers;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import uni.aau.game.dda.Bot;
import uni.aau.game.gameobjects.Character;
import uni.aau.game.gameobjects.*;
import uni.aau.game.gui.GameConsole;
import uni.aau.game.gui.Inventory;
import uni.aau.game.items.*;
import uni.aau.game.mapgeneration.DungeonMap;
import uni.aau.game.mapgeneration.RandomGen;
import uni.aau.game.mapgeneration.Tile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GameStateUpdater
{
    private Player _player;
    private ArrayList<Monster> _monsters;
    private ArrayList<Gas> _gasClouds = new ArrayList<Gas>();
    private ArrayList<Trap> _traps = new ArrayList<Trap>();
    private Inventory _inventory;
    private DungeonMap _playedMap;

    private HashMap<Character,Integer> _charactersToAct = new HashMap<Character, Integer>();
    private final ArrayList<Monster> _monstersToRemove = new ArrayList<Monster>();
    private static int _turn = 0;

    public static int getTurn()
    {
        return _turn;
    }

    private int _costOfPlayerAction = 0;
    private boolean _showingBattleAnimation = false;

    public boolean isShowingAnimation()
    {
        return _showingBattleAnimation;
    }

    //Animation
    private final float _animationSpeed = 16;
    private float _currentAnimationX;
    private float _currentAnimationY;
    private Tile _targetTile;
    private Item _animatedItem;
    private float _angleToTarget;

    //Select item dialog
    private static boolean _selectItemDialog = false;

    public static boolean isSelectingItem()
    {
        return _selectItemDialog;
    }

    public void setGameState(ArrayList<Monster> monsters, Player player, Inventory inventory, DungeonMap playedMap, ArrayList<Trap> traps)
    {
        _turn = 0;
        _player = player;
        _monsters = monsters;
        for(Monster monster : _monsters)
        {
            _charactersToAct.put(monster, 0);
        }
        _charactersToAct.put(_player,0);
        _inventory = inventory;
        _playedMap = playedMap;
        _traps = traps;
        _gasClouds.clear();
        _player.getCurrentTile().setLight(Tile.LightAmount.Shadow, _player.getLanternStrength() + 2, _player.getLanternStrength() + 2);
        _player.getCurrentTile().setLight(Tile.LightAmount.Light,_player.getLanternStrength(), _player.getLanternStrength());
    }

    public void updateGameState()
    {
        if (_showingBattleAnimation)
        {
            handleAnimation();
        }
        else if (!_selectItemDialog)
        {
            handleActionExecution();
        }
    }
    private void updateGasses()
    {
        for (Gas gas : _gasClouds)
        {
            gas.update();
        }
    }

    private void handleAnimation()
    {
        _currentAnimationX += (float) Math.cos(_angleToTarget) * _animationSpeed;
        _currentAnimationY += (float) Math.sin(_angleToTarget) * _animationSpeed;
        if (Math.abs(_currentAnimationX - (_targetTile.getX() * 32)) <= _animationSpeed &&
                Math.abs(_currentAnimationY - (_targetTile.getY() * 32)) <= _animationSpeed)
        {

            if (_animatedItem instanceof Potion)//Use the potion on the tile
            {
                usePotion((Potion) _animatedItem, _targetTile.getCharacter(), _targetTile);
            }
            else if (_targetTile.getCharacter() != null && //Damage if ranged weapon
                    _animatedItem instanceof Weapon &&
                    (((Weapon) _animatedItem).isRanged()))
            {
                _targetTile.getCharacter().damage(((Weapon) _animatedItem).getIdentifiedMaxDamage());
            }
            else //Otherwise the item lands on the tile
            {
                _targetTile.addItem(_animatedItem);

            }
            _showingBattleAnimation = false;
            _targetTile = null;
            _animatedItem = null;
            _currentAnimationX = 0;
            _currentAnimationY = 0;
        }
    }

    public void tap(float x, float y)
    {
        if (_selectItemDialog)
        {
            _inventory.tap(x, y);
            if (!_inventory.isOpen())
            {
                _selectItemDialog = false;
                _player.clearQueue();
            }
            else
            {
                Item item = _inventory.retrieveItem();
                if (item != null && !item.isIdentified())//(item instanceof Armor || item instanceof Weapon))
                {
                    String oldName = item.getName();
                    item.identify();
                    String newName = item.getName();
                    GameConsole.addMessage(oldName + " was identified to be " + newName);
                    _inventory.identifyItems(item);
                    ItemManager.identifyItem(item);
                    _selectItemDialog = false;
                    _inventory.hide();
                }

            }
        }
    }

    //Returns true if trap activated
    private boolean checkForAndActivateTrap(Tile tile)
    {
        Trap trapOnTile = tile.getTrap();
        if (trapOnTile != null && !trapOnTile.hasBeenActivated())
        {
            trapOnTile.activate();
            if (trapOnTile.hasCreatedGas())
            {
                _gasClouds.add(trapOnTile.retrieveCreatedGas());
            }
            return true;
        }
        return false;
    }
    private void handleActionExecution()
    {
        GameAction playerAction = _player.getNextAction();

        if (playerAction != null)
        {
            _costOfPlayerAction=playerAction.getExpectedDuration();
            for (Map.Entry<Character,Integer> monsterEntry: _charactersToAct.entrySet())
            {
                _charactersToAct.put(monsterEntry.getKey(),monsterEntry.getValue()+_costOfPlayerAction);
            }
        }
        boolean wasPlayerAttacked = false;
        GameAction monsterAction;
        if(_costOfPlayerAction>0)
        {
            int monstersLeft = _charactersToAct.size();
            while (monstersLeft > 0)
            {
                for (Map.Entry<Character, Integer> characterEntry : _charactersToAct.entrySet())
                {
                    if (characterEntry.getValue() > 0)
                    {
                        if(characterEntry.getKey() == _player)
                        {
                            executeAction(playerAction);
                            _charactersToAct.put(characterEntry.getKey(), characterEntry.getValue() - _costOfPlayerAction);
                        }
                        else
                        {
                            Monster monster = (Monster)characterEntry.getKey();
                            monsterAction = monster.createNextAction(_player);
                            if (monsterAction == null)
                            {
                                _monstersToRemove.add(monster);
                                monstersLeft--;
                            }
                            else
                            {
                                _charactersToAct.put(characterEntry.getKey(), characterEntry.getValue() - monsterAction.getExpectedDuration());
                                executeAction(monsterAction);
                                if(!wasPlayerAttacked && monsterAction.getType() == GameAction.Type.Attack && monsterAction.getTargetTile()==_player.getCurrentTile())
                                {
                                    wasPlayerAttacked = true;
                                }
                            }
                        }
                    }
                    else
                    {
                        monstersLeft--;
                    }
                }
                for(Monster toRemove : _monstersToRemove)
                {
                    _monsters.remove(toRemove);
                    _charactersToAct.remove(toRemove);
                }
                _monstersToRemove.clear();

                if(monstersLeft>0)
                {
                    _turn++;
                    updateGasses();
                }
            }
            if(wasPlayerAttacked && _player.isMoving())
            {
                //Clear the queue if the player was attacked while moving around
                _player.clearQueue();
            }
        }
    }

    private void executeAction(GameAction action)
    {

        //Check status effects
        if (action.getOwner() != null && action.getOwner().getCurrentStatusEffect() != null)
        {
            switch (action.getOwner().getCurrentStatusEffect())
            {
                case Paralysed:
                    if (RandomGen.getRandomInt(0, 100) > 50)
                    {
                        GameConsole.addMessage(action.getOwner().getName() + " is paralysed and cannot act");
                        action.getOwner().decreaseStatusEffectTimer();
                        return;
                    }
                    else
                    {
                        action.getOwner().decreaseStatusEffectTimer();
                        break;
                    }
                case Poisoned:
                    GameConsole.addMessage(action.getOwner().getName() + " is poisoned");
                    action.getOwner().damage(RandomGen.getRandomInt(action.getOwner().getMaxHitPoints() / 10, action.getOwner().getMaxHitPoints() / 8));
                    action.getOwner().decreaseStatusEffectTimer();
                    break;
            }
        }

        switch (action.getType())
        {
            case Move:
                executeMoveAction(action);
                break;
            case Attack:
                executeAttack(action);
                break;
            case Equip:
                action.getOwner().equip(action.getTargetItem());
                break;
            case Unequip:
                action.getOwner().unequip(action.getTargetItem());
                break;
            case Wait:
                break;
            case PickUp:
                Item item =action.getTargetTile().pickupItem();
                _inventory.addItem(item);
                GameConsole.addMessage("Picked up item " + item.getName());
                break;
            case Drop:
                _player.getCurrentTile().addItem(action.getTargetItem());
                break;
            case Throw:
                _animatedItem = action.getTargetItem();
                _targetTile = action.getTargetTile();
                _showingBattleAnimation = true;
                _currentAnimationX = action.getOwner().getCurrentTile().getX() * 32;
                _currentAnimationY = action.getOwner().getCurrentTile().getY() * 32;
                _angleToTarget = (float) Math.atan2(action.getTargetTile().getY() * 32 - _currentAnimationY, action.getTargetTile().getX() * 32 - _currentAnimationX);
                if (_angleToTarget < 0)
                {
                    _angleToTarget += 2 * Math.PI;
                }
                if (_animatedItem instanceof Weapon && ((Weapon) _animatedItem).isRanged())
                {
                    ((Weapon) _animatedItem).decreaseRangedAmmo();
                    if (((Weapon) _animatedItem).getAmmoCount() == 0)
                    {
                        _inventory.removeItem(_animatedItem);
                    }
                }
                else
                {
                    _inventory.removeItem(_animatedItem);
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
    }

    private void executeAttack(GameAction action)
    {
        Character defender = action.getTargetTile().getCharacter();
        Character attacker = action.getOwner();
        attacker.attack(defender);
        if (defender.isDead())
        {
            if (defender instanceof Monster)
            {
                _player.retrieveExperience((Monster) (defender));
            }
            else if (defender instanceof Player || defender instanceof Bot)
            {
                _player.setKilledBy(attacker.getName());
            }
        }
    }

    private void executeMoveAction(GameAction action)
    {
        Tile newTile = action.getTargetTile();
        Character character = action.getOwner();
        if (character == _player)
        {
            _inventory.step();

            if (checkForAndActivateTrap(newTile))
            {
                _player.clearQueue();
                if (_player.isDead())
                {
                    _player.setKilledBy("trap");
                }
            }
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

    private void usePotion(Potion potion, Character target, Tile tile)
    {
        if (target != null && potion.getType() == Potion.PotionType.Healing)
        {
            target.heal(potion.getPotency());
        }
        else if (target != null && potion.getType() == Potion.PotionType.Experience)
        {
            if (target == _player)
            {
                _player.retrieveExperience(potion);

            }
        }
        else if (target != null && potion.getName() == "Potion of Hurt")
        {
            target.damage(potion.getPotency());
        }
        else if (potion.getType() == Potion.PotionType.PoisonGas)
        {
            GameConsole.addMessage("A poisonous gas spreads from the bottle");
            _gasClouds.add(new Gas(tile, Character.StatusEffect.Poisoned, potion.getPotency()));
        }
        else if (potion.getType() == Potion.PotionType.ParaGas)
        {
            GameConsole.addMessage("A paralysing gas spreads from the bottle");
            _gasClouds.add(new Gas(tile, Character.StatusEffect.Paralysed, potion.getPotency() / 2));
        }
        else
        {
            GameConsole.addMessage(potion.getStringColor() + " colored liquid runs out of the vial");
        }

    }

    //Drawing
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
    }

    public void drawBattleAnimations(SpriteBatch batch)
    {
        if (_showingBattleAnimation)
        {
            _animatedItem.draw(batch, _currentAnimationX, _currentAnimationY);
        }
    }

    public void drawSelectItemDialog(SpriteBatch batch, ShapeRenderer shapeRenderer)
    {
        if (_selectItemDialog)
        {
            _inventory.draw(batch, shapeRenderer);
        }
    }
}
