package Jarretts_Prototype;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import bwapi.Game;
import bwapi.Color;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;


public class Army {

	private List<Troop> army;
	private Player self;
	private Game game;
	private UPStarcraft controller;
	private List<Unit> scouts;
	private Unit baseScout;
	private Unit helperScout;
	
	private Position enemyBase;
	private List<Unit> enemyBlds;
	private List<Unit> enemyWorkers;
	private List<Unit> enemyCores;
	private List<Unit> enemyProblems;
	private boolean killedBase;
	
	private List<Unit> trolls;
	private Hashtable<Troop, Unit> lastTargets;
	private Hashtable<Troop, Integer> frameCounts;

	List<Position> startPoss;
	boolean[] startChecked;
	List<Position> basePoss;
	boolean[] baseChecked;
	Position nextBasePosition;

	//Position nextBasePosition;
	private Position homeBase;
	private int lasti;
	private int lastj;
	private boolean forwardMarch;
	private int frames;
	private int scoutCounter;
	private int scoutDist;

	public Army(Player self, Game game, UPStarcraft controller){
		army = new ArrayList<Troop>();
		this.self = self;
		this.game = game;
		this.controller = controller;
		frames = 0;
		scoutCounter = 0;
		scoutDist = 1;
		enemyBlds = new ArrayList<Unit>();
		enemyWorkers = new ArrayList<Unit>();
		enemyCores = new ArrayList<Unit>();
		enemyProblems = new ArrayList<Unit>();
		scouts = new ArrayList<Unit>();
		
		startPoss = new ArrayList<Position>();
		basePoss = new ArrayList<Position>();
		trolls = new ArrayList<Unit>();
		lastTargets = new Hashtable<Troop, Unit>();
		frameCounts = new Hashtable<Troop, Integer>();
		
		
		enemyBase = null;
		killedBase = false;
		nextBasePosition = null;
		FillStartPositions();
		FillBasePositions();
		lasti = 1;
		lastj = 1;
		forwardMarch = true;

	}

	public void manage()
	{
		scoutOverlord();
		
		getSeenEnemies();
		for(int q = 0; q < army.size(); q++)
		{
			Troop t = army.get(q);
			if(t.units.size() > 0)
			{
				Object target = null;
				
				if(!enemyProblems.isEmpty()) //target closest threat
					target = findClosest(enemyProblems, t.units.get(0));
				
				else if(!enemyWorkers.isEmpty()) //else target closest worker, 
					target = findClosest(enemyWorkers, t.units.get(0));

				else if(!enemyCores.isEmpty()) //else target core,
					target = findClosest(enemyCores, t.units.get(0));	
				
				else if(!enemyBlds.isEmpty()) //else target buildings.
					target = findClosest(enemyBlds, t.units.get(0));	
				
				
				else if(target == null && !killedBase)
				{
					Position zergSpot = t.getMeanPos();
					if(getUnchecked() == 1)
					{
						for(int i=0; i<startPoss.size(); i++)
						{
							if(!startChecked[i])
								enemyBase = startPoss.get(i); //zergling.move(startPoss.get(i));
						}
					}
					if(enemyBase == null)
					{
						if(startPoss.contains(zergSpot))
							startChecked[startPoss.indexOf(zergSpot)] = true;
						for(int i = startPoss.size() - 1; i > -1; i--)
							if(!startChecked[i])
							{
								//System.out.println("move order");
								target = startPoss.get(i);
								break;
							}
					}
					//TODO sample unity values, determine if this is even good or needed
					else if(t.getUnity() < 0)
					{
						target = t.getMeanPos();
					}
					else
					{
						
						if(game.isVisible(enemyBase.toTilePosition()))
						{
							killedBase = true;
							startChecked[startPoss.indexOf(enemyBase)] = true;
							
						}
						else
						{
							target = enemyBase;
						}
					}

				}
				else if(killedBase)
				{
					frames++;
					if(frames == 90 + scoutDist*15)
					{
						frames = 0;
						scoutCounter++;
						if(scoutCounter == 4)
						{
							scoutCounter = 0;
							scoutDist++;
						}
					}
					
					switch(scoutCounter)
					{
					case 0:
						target = new TilePosition(enemyBase.toTilePosition().getX() + scoutDist, enemyBase.toTilePosition().getY()).toPosition();
						break;
						
					case 1:
						target = new TilePosition(enemyBase.toTilePosition().getX(), enemyBase.toTilePosition().getY() + scoutDist).toPosition();
						break;
						
					case 2:
						target = new TilePosition(enemyBase.toTilePosition().getX() - scoutDist, enemyBase.toTilePosition().getY()).toPosition();
						break;
						
					case 3:
						target = new TilePosition(enemyBase.toTilePosition().getX(), enemyBase.toTilePosition().getY() - scoutDist).toPosition();
						break;
					}
					
					
					if(baseScout == null || !baseScout.exists())
						baseScout = controller.getWorker();
					
					//Base Scouting
					if (nextBasePosition == null)
					{
						if(basePoss.size() != 0)
						{
							for(Position base : basePoss)
							{
								if(nextBasePosition==null || base.getDistance(t.units.get(0).getPosition()) < nextBasePosition.getDistance(t.units.get(0).getPosition()))
								{
									nextBasePosition = base;
									System.out.println("Assigned next base target");
								}
							}
							if(baseScout != null && baseScout.exists())
								baseScout.move(nextBasePosition);
						}
					}
					else if (game.isVisible(nextBasePosition.toTilePosition()))
					{
						basePoss.remove(nextBasePosition);
						nextBasePosition = null;
						System.out.println("Deleted the seen base, set to null");
					}
					
					
					//Grid Scouting
					
					if(helperScout == null || !helperScout.exists())
						helperScout = controller.getWorker();
					
					if(!game.isVisible(lasti, lastj))
					{
						
						
						//System.out.println("i can not see" + lasti + "," + lastj);
						for(Unit scout : scouts)
							scout.move(new TilePosition(lasti,lastj).toPosition());
						
						if(helperScout == null || !helperScout.exists())
							helperScout = controller.getWorker();
						
						if(helperScout != null && helperScout.exists())
							helperScout.move(new TilePosition(lasti,lastj).toPosition());
						//System.out.println("moving to " + new TilePosition(lasti,lastj));
					}
					else
					{
						//System.out.println("i saw "+ lasti + "," + lastj);
						
						if(forwardMarch){
							lasti = lasti + 1;
							if(lasti >= game.mapWidth())
							{
								lasti = game.mapWidth()-1;
								lastj = lastj+10;
								forwardMarch = !forwardMarch;
							}
						}
						else{
							lasti = lasti - 1;
							if(lasti < 1){
								lasti = 1;
								lastj = lastj+10;
								forwardMarch = !forwardMarch;
							}
							
						}
						
						if(lasti >= game.mapWidth())
						{
							lasti = 1;
							lastj = lastj+20;
						}
						
						for(Unit scout : scouts)
							scout.move(new TilePosition(lasti,lastj).toPosition());
						
						if(helperScout != null && helperScout.exists())
							helperScout.move(new TilePosition(lasti,lastj).toPosition());
						
						//System.out.println("so im now off to "+ lasti + "," + lastj);
					}

				}

				if(target instanceof Position)
					game.drawCircleMap((Position)target, 10, Color.White, true);
				//System.out.println(target);
				t.setAttackTarget(target);
				if(target instanceof Unit)
				{
					Unit unit = (Unit) target;
					if(lastTargets.get(t) == null)
						lastTargets.put(t, unit);
					else if(lastTargets.get(t) == unit)
						if(unit.isAttacking())
							frameCounts.replace(t,  0);
						else
							frameCounts.replace(t, frameCounts.get(t) + 1);

					else //not equal
					{
						lastTargets.replace(t, unit);
						frameCounts.replace(t,  0);
					}
					
					if(!unit.getType().isBuilding()) //is mobile
						if(frameCounts.get(t) > 250)
							trolls.add(unit);
				}
			}
			t.manage();
		}



		//Clear the list each time as garbage will occur otherwise
		enemyProblems.clear();
		enemyWorkers.clear();
		enemyCores.clear();
		enemyBlds.clear();
	}

	private void getSeenEnemies()
	{
		for(Unit unit : game.enemy().getUnits())
		{
			if(unit.exists() && unit.isVisible() && unit.isDetected())
			{
				if(unit.getType().isWorker())
				{
					enemyWorkers.add(unit);
				}
				else if(unit.getType().canAttack() 	|| unit.getType() == UnitType.Terran_Bunker
													|| unit.getType() == UnitType.Zerg_Sunken_Colony
													|| unit.getType() == UnitType.Protoss_Photon_Cannon)
				{
					//if(unit.isAttacking() || unit.isStartingAttack() || unit.isAttackFrame())
					enemyProblems.add(unit);
					
				}
				else if(unit.getType().isBuilding())
				{
					if(unit.getType() == UnitType.Zerg_Hatchery 
							|| unit.getType() == UnitType.Terran_Command_Center
							|| unit.getType() == UnitType.Protoss_Nexus)
					{
						enemyCores.add(unit);
						
						if(enemyBase == null && startPoss.contains(unit.getPosition()))
							enemyBase = unit.getPosition();
					}
					else
						enemyBlds.add(unit);
				}

			}
		}
		
		List<Unit> trollsToRemove = new ArrayList<Unit>();
		//test kiters to see if still kiting
		for(Unit troll : trolls)
		{
			if(troll.isVisible())
				if(troll.isAttacking() || troll.isGatheringGas() || troll.isGatheringMinerals())
					trollsToRemove.add(troll);
					
		}
		trolls.removeAll(trollsToRemove);
		
		//remove kiters
		enemyProblems.removeAll(trolls);
		enemyWorkers.removeAll(trolls);
	}

	private void scoutOverlord()
	{
		//rid ourselves of dead scouts
		List<Unit> deadScouts = new ArrayList<Unit>();
		for(Unit scout : scouts)
			if(!scout.exists())
				deadScouts.add(scout);
		
		scouts.removeAll(deadScouts);
				
		
		
		if(enemyBase == null && scouts.size() > 0)
		{

			//refresh the lists by seeing what is visible
			for(Position pos : startPoss)
			{
				if(game.isVisible(pos.toTilePosition()) && !startChecked[startPoss.indexOf(pos)])
					startChecked[startPoss.indexOf(pos)] = true;
			}

			for(int i = 0; i < startPoss.size(); i++)
				if(!startChecked[i])
				{
					for(Unit scout : scouts)
						scout.move(startPoss.get(i));
					break;
				}
		}
		else if(scouts.size() > 0)
			for(Unit scout : scouts)
				scout.move(homeBase);
	}

	public void FillStartPositions()
	{
		List<BaseLocation> startLocations = BWTA.getStartLocations();
		int size = 0;
		for(BaseLocation base : startLocations)
		{			
			// if start location is not start location and a starting location add it
			if (!base.getPosition().equals(BWTA.getStartLocation(self).getPosition()))
			{
				startPoss.add(base.getPosition());
				size++;
			}
			else
			{
				homeBase=base.getPosition();
			}
		}
		//System.out.println("startPosSize: "+ startPoss.size());
		//System.out.println("startPoses: "+ startPoss);
		startChecked = new boolean[size];
	}

	public void FillBasePositions()
	{
		List<BaseLocation> baseLocations = BWTA.getBaseLocations();
		int size = 0;
		for(BaseLocation base : baseLocations)
		{			
			// if base location is not start location and a starting location add it
			if (!base.getPosition().equals(BWTA.getStartLocation(self).getPosition()) && !startPoss.contains(base))
			{
				basePoss.add(base.getPosition());
				size++;
			}
		}
		basePoss.remove(homeBase);
		//System.out.println("basePosSize: "+ basePoss.size());
		//System.out.println("basePoses: "+ basePoss);
		baseChecked = new boolean[size];
	}

	public static Unit findClosest(List<Unit> list, Unit u)
	{
		Unit returnUnit = null;
		int distance = Integer.MAX_VALUE;
		for(Unit unit : list)
		{
			if(unit.getPosition().getApproxDistance(u.getPosition()) < distance)
			{
				distance = unit.getPosition().getApproxDistance(u.getPosition());
				returnUnit = unit;
			}
		}
		return returnUnit;
	}

	public int getUnchecked()
	{
		int counter = 0;
		for(Boolean bool : startChecked)
		{
			if(!bool)
				counter++;
		}
		return counter;
	}

	public void move(Position p)
	{
		for(Troop troop : army)
			troop.move(p);
	}

	public void attackMove(Position p)
	{
		for(Troop troop : army)
			troop.attackMove(p);
	}

	public void addTroop(Troop troop)
	{
		army.add(troop);
		//lastTargets.put(troop, null);
		frameCounts.put(troop, 0);
	}

	public void addScout(Unit scout)
	{
		scouts.add(scout);
	}
	
	public int size()
	{
		int size = 0;
		for(Troop t : army)
			size += t.units.size();
		return size;
	}

}
