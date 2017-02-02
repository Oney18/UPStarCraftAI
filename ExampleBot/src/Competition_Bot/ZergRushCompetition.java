package Competition_Bot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class ZergRushCompetition extends DefaultBWListener {

	private Mirror mirror = new Mirror();

	private Game game;

	private Player self;
	
	private boolean builtPool;
	private boolean buildingPool;
	private boolean gettingPoolWorker;
	private boolean buildingOverlord;
	private boolean killedMainBase;
	private boolean cheesyStupidBuilding;
	
	private boolean gasMorphingStarted;
	private boolean gasMorphing;
	private boolean gasMorphing2;
	private Unit gasMorpher;
	private Unit scoutOverlord;
	private Unit scoutDrone;
	private Unit poolMorpherDrone;
	private TilePosition poolPos;
	private boolean cheesed;
	private Unit overlord;
	private Unit attackTarget;
	private Unit lastFrameAttackTarget;
	private int minerals;
	
	List<Position> startPoss;
	boolean[] startChecked;
	
	List<Position> basePoss;
	boolean[] baseChecked;
	
	Position nextBasePosition;
	
	private boolean doInitDrone;
	
	int lasti;
	int lastj;

	
	private HashSet<Position> enemyBuildingLocation;
	private List<Unit> larvae;
	private List<Unit> drones;
	private List<Unit> zerglings;
	private Position enemyBase;
	private Position homeBase;
	
	public static void main(String[] args) {
		new ZergRushCompetition().run();
	}
	
	private int[] slowFrames = new int[3];
	
	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}
	
	@Override
	public void onStart() {
		game = mirror.getGame();
		self = game.self();
		
		//inititalize the data structures
		doInitDrone = true; //TOGGLE THIS FOR INITIAL DRONE
		
		builtPool = false;
		buildingPool = false;
		gettingPoolWorker = false;
		buildingOverlord = false;
		killedMainBase=false;
		cheesyStupidBuilding=false;
		
		gasMorphingStarted = false;
		gasMorphing = false;
		gasMorphing2 = false;
		
		cheesed = false;
		
		gasMorpher = null;
		poolMorpherDrone = null;
		poolPos = null;
		scoutOverlord = null;
		scoutDrone = null;
		
		enemyBase = null;
		homeBase = null;
		
		nextBasePosition = null;
		
		lasti = 1;
		lastj = 1;
		
		startPoss = new ArrayList<Position>();
		basePoss = new ArrayList<Position>();
		larvae = new ArrayList<Unit>();
		drones = new ArrayList<Unit>();
		zerglings = new ArrayList<Unit>();
		

		// sets speed to be way faster
		//game.setLocalSpeed(4);

		// Use BWTA to analyze map
		// This may take a few minutes if the map is processed first time!
		//System.out.println("Analyzing map...");
		BWTA.readMap();
		BWTA.analyze();
		//System.out.println("Map data ready");

		enemyBuildingLocation = new HashSet<Position>();
		
		FillStartPositions();
		FillBasePositions();
		
		for(int i = 0; i < startChecked.length; i++)
			startChecked[i] = false;

		//TODO move overlord
		
	}

	@Override
	public void onFrame() {
		
		if(game.isPaused())
			return;
		
		//time checking stuff
		long startTime = System.nanoTime();
		// print framerate and other info
//		game.drawTextScreen(0, 0,  "FPS: " + game.getFPS() );
//	    game.drawTextScreen(0, 20, "Avg FPS: " + game.getAverageFPS() );
//	    game.drawTextScreen(0, 40, "dronecount: "+ drones.size());
//	    game.drawTextScreen(0, 60, "supply used: "+ self.supplyUsed());
//	    game.drawTextScreen(0, 80, "larvaeCount: "+ larvae.size());
//	    game.drawTextScreen(0, 100, "Zergling Size: "+ zerglings.size());
//	    game.drawTextScreen(0, 110, "Minerals: "+ minerals);
//	    game.drawTextScreen(0, 130, "enemyBase: "+ enemyBase);
	   
	    
		// update game information
		updateEnemyLocations();

		// give orders to units based on strat
		doStrategy();
		
		//after all is done, see if latency  is an issue
		long duration = (System.nanoTime() - startTime) / 1000000;
		if(duration > 10000) //10s
		{
			slowFrames[0]++;
			//System.out.println("10s frame; current amount is " + slowFrames[0]); 
		}
		else if(duration > 1000) //1s
		{
			slowFrames[1]++;
			//System.out.println("1s frame; current amount is " + slowFrames[1]); 
		}
		else if(duration > 55) //55ms
		{
			slowFrames[2]++;
			//System.out.println("55ms frame; current amount is " + slowFrames[2]); 
		}
		
	}

	
	
	private void doStrategy() {

		//first and foremost, send all workers to go gather materials and delete dead ones
		for(Unit drone : drones)
		{			
			if(drone.isIdle() && drone.isCompleted())
			{
				Unit closestMineral = findClosestMineral(BWTA.getStartLocation(self).getPosition());
				if(closestMineral != null)
				{
					drone.gather(closestMineral);
				}
			}

			//save dead units for deletion	
			if(!drone.exists())
			{
				drones.remove(drone);
			}
		}

		// grab our resources
		minerals = self.minerals();

		// build a spawning pool when we can
		if ( !buildingPool && minerals >= 180 && drones.size()>0) 
		{
			if(minerals <200 && !gettingPoolWorker) //between 150 and 200 then
			{
				//System.out.println("Starting to find a dude");
				poolMorpherDrone = getWorker();
				//System.out.println("Got Worker: " + poolMorpherDrone.toString());
				if(poolMorpherDrone != null)
				{		
					//System.out.println("Got to 1");
					poolPos = getBuildTile(poolMorpherDrone, UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition());
					//System.out.println("Got position at: " + poolPos.toPosition().toString());
					if(poolPos != null)
					{
						poolMorpherDrone.move(poolPos.toPosition());
						//System.out.println("mmoving");
					}
				}
				//System.out.println("won't do this again");
				gettingPoolWorker = true;
			}
			else if(minerals >=200) //anything over 200
			{
				if(poolMorpherDrone.canBuild(UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition()))
				{
					//System.out.println("trying to build");
					poolMorpherDrone.build(UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition());
					if(self.allUnitCount(UnitType.Zerg_Spawning_Pool) > 0)
					{
						buildingPool = true;
					}
				}
				else if(poolMorpherDrone.getPosition().equals(poolPos.toPosition()) && !poolMorpherDrone.canBuild(UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition()))
				{
					//maybe still moving, let it keep moving to the spot we found works
					poolMorpherDrone.move(homeBase);
					//System.out.println("move home");
					//if you command it to move, it gets mad and won't obey. too many commands
				}
			}
			
		}
		minerals = self.minerals();


		// EXTRACTOR BULLSHIT TO BE ADDED HERE
		if ((gasMorphing && self.supplyUsed() == 16 && !cheesed) || gasMorphing2) 
		{
			gasMorphing2 = true;
			if(gasMorpher.canCancelMorph() && self.supplyUsed() == 18)
			{
				//System.out.println("trying to cancel");
				gasMorpher.cancelMorph();
				gasMorphingStarted = false;
				cheesed = true;
			}
			else
			{
				cheesed = true;
			}
		}
		else if (self.allUnitCount(UnitType.Zerg_Extractor) < 1 && minerals >= 50 
				&& self.supplyUsed() == 18 && !gasMorphingStarted && !cheesed) 
		{
			//System.out.println("starting extractor");
			if(drones.size() > 0)
			{
				makeGas();
			}
			minerals -= 50;
			gasMorphingStarted = true;
		}
		//EXTRACTOR BULLSHIT ENDS HERE



		//else build zerglings all the way up to 9(18 because of 2 multiplier
		while(builtPool && minerals >= 50 && self.supplyUsed() <= self.supplyTotal()-2 && larvae.size() > 0)
		{
			//System.out.println("building zerg");
			Unit larva = larvae.get(0); //get a larvae
			larvae.remove(0); //remove from array
			//System.out.println("removing one");
			larva.morph(UnitType.Zerg_Zergling); //morph
			minerals -= 50;
		}
		
		if((self.supplyTotal() == 2 && !buildingOverlord) || (minerals >= 100+50*larvae.size() && larvae.size()==3 && self.supplyUsed()>=self.supplyTotal()-1))
		{
			if(larvae.size()>0)
			{
				Unit larva = larvae.get(0); //get a larvae
				larvae.remove(0); //remove from array
				//System.out.println("removing one1");
				larva.morph(UnitType.Zerg_Overlord); //morph
				minerals -= 100;
				buildingOverlord=true;
			}
		}else if(self.supplyTotal() > 2)
		{
			buildingOverlord = false;
		}

		//fill lists of all visible enemy units
		List<Unit> enemyBlds = new ArrayList<Unit>();
		List<Unit> enemyWorkers = new ArrayList<Unit>();
		List<Unit> enemyCores = new ArrayList<Unit>();
		List<Unit> enemyProblems = new ArrayList<Unit>();

		
		/**OVERLORD SCOUTING**/
		
		if(enemyBase == null)
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
						overlord.move(startPoss.get(i));
						break;
					}
		}
		else
			overlord.move(homeBase);
		
		/**END OVERLORD SCOUTING**/


		
		for(Unit unit : game.enemy().getUnits())
		{
			if(unit.exists() && unit.isVisible() && unit.isDetected())
			{
				if(unit.getType().isWorker())
				{
					enemyWorkers.add(unit);
				}
				else if(unit.getType().canAttack())
				{
					enemyProblems.add(unit);
				}
				else if(unit.getType().isBuilding())
				{
					if(unit.getType() == UnitType.Zerg_Hatchery 
							|| unit.getType() == UnitType.Terran_Command_Center
							|| unit.getType() == UnitType.Protoss_Nexus)
					{
						enemyCores.add(unit);
						if(enemyBase == null)
							enemyBase = unit.getPosition();
					}
					else
						//TODO make a way to sort buildings based on priority. those that can attack back should be higher.
						enemyBlds.add(unit);
					}
					
				}
			}
			if(zerglings.size()>0)
			{
				if(!enemyProblems.isEmpty()) //target closest threat
				{
					attackTarget = findClosest(enemyProblems, zerglings.get(0));
				}
				else if(!enemyWorkers.isEmpty())//else target closest worker, 
				{
					attackTarget = findClosest(enemyWorkers, zerglings.get(0));			
				}
				else if(!enemyCores.isEmpty())//else target core,
				{
					attackTarget = findClosest(enemyCores, zerglings.get(0));		
				}
				else if(!enemyBlds.isEmpty())//else target buildings.
				{
					attackTarget = findClosest(enemyBlds, zerglings.get(0));		
				}
				else
				{
					attackTarget = null; //will later say then move to overlord
				}
				 game.drawTextScreen(0, 150, "EnemyProbs: "+ enemyProblems.size());
				for(Unit zergling : zerglings)
				{
					//save dead units for deletion	
					if(!zergling.exists())
					{
						zerglings.remove(zergling);
					}
					else if(lastFrameAttackTarget != attackTarget)
					{
						//this will still tell all 'stuck" zergs to do a command every frame
						//which we believe to be the cause of the "stuck bug"(hence does nothing)
						if(attackTarget != null){
							zergling.attack(attackTarget);
						}
						////System.out.println("attack order");
					}
					else if (!zergling.isAttacking())
					{
						if(killedMainBase)
						{
							if(scoutOverlord == null || !scoutOverlord.exists())
							{
								scoutOverlord = overlord;
							}
							if(scoutDrone == null || !scoutDrone.exists())
							{
								scoutDrone = getWorker();
							}
								
							if(cheesyStupidBuilding || !cheesyStupidBuilding)
							{
								if(!game.isVisible(lasti, lastj))
								{
									//System.out.println("i can not see" + lasti + "," + lastj);
									scoutOverlord.move(new TilePosition(lasti,lastj).toPosition());
									scoutDrone.move(new TilePosition(lasti,lastj).toPosition());
									//System.out.println("moving to " + new TilePosition(lasti,lastj));
								}
								else
								{
									//System.out.println("i saw "+ lasti + "," + lastj);
									lasti = lasti +1;
									if(lasti >= game.mapWidth())
									{
										lasti = 1;
										lastj = lastj+20;
									}
									else
									{
										lastj=lastj;
									}
									//System.out.println("so im now off to "+ lasti + "," + lastj);
								}
							}
							if (cheesyStupidBuilding || !cheesyStupidBuilding)
							{
								if (nextBasePosition == null)
								{
									if(basePoss.size() == 0)
									{
										cheesyStupidBuilding = true;
									}
									else
									{
										for(Position base : basePoss)
										{
											if(nextBasePosition==null || base.getDistance(zergling.getPosition()) < nextBasePosition.getDistance(zergling.getPosition()))
											{
												nextBasePosition = base;
											}
										}
										zergling.move(nextBasePosition);
									}
								}
								else if (game.isVisible(nextBasePosition.toTilePosition()) && attackTarget == null)
								{
									basePoss.remove(nextBasePosition);
									nextBasePosition = null;
								}
							}
						}
						else if(attackTarget == null || !attackTarget.exists())
						{
							Position zergSpot = zergling.getPosition();
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
										zergling.move(startPoss.get(i));
										break;
									}
							}
							
							else
							{
								if(game.isVisible(enemyBase.toTilePosition()))
								{
									killedMainBase = true;
								}
								else
								{
									zergling.move(enemyBase);
								}
							}
							
						}
						
					}
				}
			}
			lastFrameAttackTarget = attackTarget;
		}

	
	@Override
	public void onUnitCreate(Unit unit) {

		//oncreate is also called at the beginning when we are given units
		if(!unit.getPlayer().isEnemy(self))
		{
			if (unit.getType() == UnitType.Zerg_Drone) 
			{
				drones.add(unit);
			} 
			else if (unit.getType() == UnitType.Zerg_Larva)
			{
				larvae.add(unit);
			} 
			else if (unit.getType() == UnitType.Zerg_Zergling) 
			{
				zerglings.add(unit);
			} 
			else if(unit.getType() == UnitType.Zerg_Overlord)
			{
				overlord = unit;
			}
			
			//starting condition, build a drone, then never again. can be toggled
			//putting here because it gets called way less than each frame
			if(doInitDrone)
			{
				if(larvae.size() > 0)
				{
					Unit larva = larvae.get(0); //get a larvae
					larvae.remove(0); //remove from array
					//System.out.println("removing one2");
					larva.morph(UnitType.Zerg_Drone); //morph
					doInitDrone = false;
				}
			}
		}
		
	
	}
	
	@Override
	public void onUnitMorph(Unit unit){
		if(unit.getType() == UnitType.Zerg_Extractor)//called even when morphing not done yet
		{ 
			gasMorphing = true; //set flag to check cancel conditions
			gasMorpher = unit;
		}
		else if(unit.getType() == UnitType.Zerg_Spawning_Pool)
		{
			buildingPool = true;
		}
		else if (unit.getType() == UnitType.Zerg_Drone) 
		{
			drones.add(unit);
		} 
		else if (unit.getType() == UnitType.Zerg_Zergling) 
		{
			zerglings.add(unit);
		} 
		else if(unit.getType() == UnitType.Zerg_Overlord)
		{
			overlord = unit;
		}
	}
	
	@Override
	public void onUnitComplete(Unit unit)
	{
		if(unit.getType() == UnitType.Zerg_Spawning_Pool)
		{
			builtPool = true;
		}
		if(unit.getType() == UnitType.Zerg_Extractor)
		{
			cheesed = true;
		}
	}
	
	
	@Override
	public void onUnitDestroy(Unit unit){
		if(unit.getPlayer().isEnemy(self))
		{
			//////System.out.println("got a bitch!");
			/*if(unit.getType().canAttack())
			{
				enemyProblems.remove(unit);
			}
			else if(unit.getType().isBuilding())
			{
				if(unit.getType() == UnitType.Zerg_Hatchery 
						|| unit.getType() == UnitType.Terran_Command_Center
						|| unit.getType() == UnitType.Protoss_Nexus)
				{
					enemyCores.remove(unit);
				}
				else
				{
					//TODO make a way to sort buildings based on priority. those that can attack back should be higher.
					enemyBlds.remove(unit);
				}
			}
			else if(unit.getType().isWorker())
			{
				enemyWorkers.remove(unit);
			}*/
		}
	}
	
	//finds closest thing from list from unit
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

	private void updateEnemyLocations() {
		// Add any buildings we see to list.
		for (Unit u : game.enemy().getUnits()) {
			// if this unit is a building add it to the hash
			if (u.getType().isBuilding()) {
				// check if we have it's position in memory and add it if we
				// don't
				if (!enemyBuildingLocation.contains(u.getPosition())) {
					enemyBuildingLocation.add(u.getPosition());
				}
			}
		}

		ArrayList<Position> buildingsToRemove = new ArrayList<Position>();

		// loop over the visible enemy units that we remember

		for (Position p : enemyBuildingLocation) {
			TilePosition tileCorrespondingToP = new TilePosition(p.getX() / 32, p.getY() / 32);

			// if visible
			if (game.isVisible(tileCorrespondingToP)) {
				// loop over the visible enemy buildings and find out if at
				// least
				// one of them is still at the remembered position
				boolean buildingStillThere = false;
				for (Unit u : game.enemy().getUnits()) {
					if (u.getType().isBuilding() && u.getPosition().equals(p) && u.exists()) {
						buildingStillThere = true;
						break;
					}
				}

				if (!buildingStillThere) {
					buildingsToRemove.add(p);
					break;// TODO check if this is necessary
				}
			}
		}
				
		
		
		//remove all the stuff to remove
		for(Position p : buildingsToRemove)
		{
			enemyBuildingLocation.remove(p);
		}
		
	}
	
	public void FillBasePositions()
	{
		List<BaseLocation> baseLocations = BWTA.getBaseLocations();
		int size = 0;
		for(BaseLocation base : baseLocations)
		{			
			// if start location is not start location and a starting location add it
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
	
	/**
	 * getWorker()
	 * Finds an available worker unit
	 * 
	 * @return - a worker unit
	 */
	public Unit getWorker()
	{
		for(Unit drone : drones)
		{			
//			//make sure no drones are going to morph at the same time
//			if(worker.getOrder().equals(Order.ZergBuildingMorph))
//			{
//				return null;
//			}
			
			//find a free worker
			if(!drone.isMorphing() && drone.isInterruptible() 
					&& drone.isCompleted() && !drone.isCarryingMinerals())
			{
				drones.remove(drone);
				return drone;
			}
		}
		return null;
	}
	
	/**
	 * findClosestMineral()
	 * Finds the closest mineral to the given position
	 * 
	 * @param pos - position of the unit
	 */
	private Unit findClosestMineral(Position pos) 
	{
		if(pos == null)
		{
			return null;
		}
		//init closest to first in list
		Unit closest = null;
		
		//find closest mineral
		for(Unit neutral : game.getNeutralUnits())
		{
			//only check mineral fields
			if(neutral.getType() == UnitType.Resource_Mineral_Field)
			{
				if(closest == null || neutral.getDistance(pos) < closest.getDistance(pos))
				{
					closest = neutral;
				}
			}
		}
		
		return closest;
	}
	
	/** 
	 * getBuildTile
	 * 
	 * Returns a suitable TilePosition to build a given building type near 
	 * Highly inefficient, ripped from SSCAIT tutorial
	 * 
	 * @param builder
	 * @param buildingType
	 * @param aroundTile
	 * @return
	 */
	 private TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
	 	TilePosition ret = null;
	 	int maxDist = 3;
	 	int stopDist = 40;
	 	
	 	while ((maxDist < stopDist) && (ret == null)) {
	 		for (int i=aroundTile.getX()-maxDist; i<=aroundTile.getX()+maxDist; i++) {
	 			for (int j=aroundTile.getY()-maxDist; j<=aroundTile.getY()+maxDist; j++) {
	 				if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false)) {
	 					// units that are blocking the tile
	 					boolean unitsInWay = false;
	 					for (Unit u : game.getAllUnits()) {
	 						if (u.getID() == builder.getID()) continue;
	 						if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
	 					}
	 					if (!unitsInWay) {
	 						return new TilePosition(i, j);
	 					}
	 					// creep for Zerg
	 					if (buildingType.requiresCreep()) {
	 						boolean creepMissing = false;
	 						for (int k=i; k<=i+buildingType.tileWidth(); k++) {
	 							for (int l=j; l<=j+buildingType.tileHeight(); l++) {
	 								if (!game.hasCreep(k, l)) creepMissing = true;
	 								break;
	 							}
	 						}
	 						if (creepMissing) continue; 
	 					}
	 				}
	 			}
	 		}
	 		maxDist += 2;
	 	}
	 	
	 	if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
	 	return ret;
	 }
	 
	 public void makeGas()
		{
			Unit builder = getWorker();
			// make sure the builder is not null
			if(builder != null)
			{	
				//System.out.println("builder is not null, tries to look for geyser");
				Unit closestGeyser = null;
				Position startArea = BWTA.getStartLocation(self).getPosition();
				
				//loop through game's geysers, get closest to our base
				for(Unit geyser : game.getGeysers() ){
					if(game.canBuildHere(geyser.getTilePosition(), UnitType.Zerg_Extractor, builder, true)){
						
						//calculate distance, if less then is closes
						if(closestGeyser == null || closestGeyser.getDistance(startArea) > geyser.getDistance(startArea)){
							closestGeyser = geyser;
						}					
					}
				}
				
				builder.build(UnitType.Zerg_Extractor, closestGeyser.getTilePosition());
				gasMorphingStarted = true;
				//System.out.println("Sent the builder?");
			}
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
}
