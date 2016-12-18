package Jarretts_Prototype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Chokepoint;

public class ZergRush2 extends DefaultBWListener {

	private Mirror mirror = new Mirror();

	private Game game;

	private Player self;

	private boolean isScouting = false;
	
	private boolean builtPool = false;
	
	private boolean gasMorphingStarted = false;
	private boolean gasMorphing = false;
	private Unit gasMorpher;
	private Unit poolMorpherDrone;
	private TilePosition poolPos;
	private boolean cheesed = false;
	private Unit overlord;
	private Unit attackTarget;
	
	private boolean doInitDrone = true; //TOGGLE THIS FOR INITIAL DRONE

	
	private HashSet<Position> enemyBuildingLocation;
	private List<Unit> larvae = new ArrayList<Unit>();
	private List<Unit> drones = new ArrayList<Unit>();
	private List<Unit> zerglings = new ArrayList<Unit>();
	private Position enemyBase;
	
	public static void main(String[] args) {
		new ZergRush2().run();
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
		
		//sets the ability to manually control bot during replay
		game.enableFlag(1);

		// sets speed to be way faster
		game.setLocalSpeed(10);

		// Use BWTA to analyze map
		// This may take a few minutes if the map is processed first time!
		System.out.println("Analyzing map...");
		BWTA.readMap();
		BWTA.analyze();
		System.out.println("Map data ready");

		enemyBuildingLocation = new HashSet<Position>();
		
		for(Unit u : self.getUnits())
		{
			if(u.getType() == UnitType.Zerg_Overlord)
				overlord = u;
			else if(u.getType() == UnitType.Zerg_Drone)
				drones.add(u);
			else if(u.getType() == UnitType.Zerg_Larva)
				larvae.add(u);
		}
		
		//starting condition, build a drone, then never again. can be toggled
		if(doInitDrone)
		{
			if(larvae.size() > 0)
			{
				Unit larva = larvae.get(0); //get a larvae
				larvae.remove(0); //remove from array
				larva.morph(UnitType.Zerg_Drone); //morph
			}
		}
		
	}

	@Override
	public void onFrame() {
		
		if(game.isPaused())
			return;
		
		//time checking stuff
		long startTime = System.nanoTime();
		// print framerate and other info
		game.drawTextScreen(0, 0,  "FPS: " + game.getFPS() );
	    game.drawTextScreen(0, 20, "Avg FPS: " + game.getAverageFPS() );
	    game.drawTextScreen(0, 40, "workercount: "+ WorkerManager.getDroneCount());
	    
		// update game information
		updateEnemyLocations();

		// give orders to units based on strat
		doStrategy();
		
		//after all is done, see if latency  is an issue
		long duration = (System.nanoTime() - startTime) / 1000000;
		if(duration > 10000) //10s
		{
			slowFrames[0]++;
			System.out.println("10s frame; current amount is " + slowFrames[0]); 
		}
		else if(duration > 1000) //1s
		{
			slowFrames[1]++;
			System.out.println("1s frame; current amount is " + slowFrames[1]); 
		}
		else if(duration > 55) //55ms
		{
			slowFrames[2]++;
			System.out.println("55ms frame; current amount is " + slowFrames[2]); 
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
		int minerals = self.minerals();
		
		// build a spawning pool when we can
		if ( !builtPool && minerals >= 150 && drones.size() >0) {
			if(minerals <200) //between 150 and 170 then
			{
				poolMorpherDrone = getWorker();
				if(poolMorpherDrone != null && game.canMake(UnitType.Zerg_Spawning_Pool, poolMorpherDrone))
				{		
					poolPos = getBuildTile(poolMorpherDrone, UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition());
					if(poolPos != null)
					{
						poolMorpherDrone.move(poolPos.toPosition());
					}
				}
			}
			else //anything over 200
			{
				poolMorpherDrone.build(UnitType.Zerg_Spawning_Pool, poolPos);
				minerals -= 200;
				//return;
			}
		}
		
		
		// EXTRACTOR BULLSHIT TO BE ADDED HERE
		if (gasMorphing && self.supplyUsed() == 18) 
		{
			if(gasMorpher.canCancelMorph())
			{
				System.out.println("trying to cancel");
				gasMorpher.cancelMorph();
				gasMorphingStarted = false;
			}
			else
			{
				cheesed = true;
			}
			
				
		}
		else if (self.allUnitCount(UnitType.Zerg_Extractor) < 1 && minerals >= 50 
				&& self.supplyUsed() == 18 && !gasMorphingStarted && !cheesed) 
		{
			System.out.println("starting extractor");
			if(drones.size() > 0)
			{
				makeGas();
				minerals -= 50;
			}
			minerals -= 50;
			gasMorphingStarted = true;
			cheesed = true;
		}
		//EXTRACTOR BULLSHIT ENDS HERE

		
		
		//else build zerglings all the way up to 9(18 because of 2 multiplier
		while(builtPool && minerals >= 50 && self.supplyUsed() < 18 && larvae.size() > 0)
		{
			Unit larva = larvae.get(0); //get a larvae
			larvae.remove(0); //remove from array
			larva.morph(UnitType.Zerg_Zergling); //morph
			minerals -= 50;
		}
		
		//fill lists of all visible enemy units
		List<Unit> enemyBlds = new ArrayList<Unit>();
		List<Unit> enemyWorkers = new ArrayList<Unit>();
		List<Unit> enemyCores = new ArrayList<Unit>();
		List<Unit> enemyProblems = new ArrayList<Unit>();
		for(Unit unit : game.enemy().getUnits())
		{
			if(unit.exists() && unit.isVisible() && unit.isDetected())
			{
				if(unit.getType().canAttack())
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
					}
					else
					{
						//TODO make a way to sort buildings based on priority. those that can attack back should be higher.
						enemyBlds.add(unit);
					}
				}
				else if(unit.getType().isWorker())
				{
					enemyWorkers.add(unit);
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
			
			for(Unit zergling : zerglings)
			{
				//save dead units for deletion	
				if(!zergling.exists())
				{
					zerglings.remove(zergling);
				}
				else
				{
					if(attackTarget == null || !attackTarget.exists())
					{
						zergling.move(overlord.getPosition());
					}
					else
					{
						if(zerglings.size() > 6)
						zergling.attack(attackTarget);
					}
				}
			}
		}
		
		//TODO move Overlord.
		
	}
	
	@Override
	public void onUnitCreate(Unit unit) {

		if (unit.getType() == UnitType.Zerg_Drone) 
		{
			System.out.println("got a drone from create");
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
			builtPool = true;
		}
		else if (unit.getType() == UnitType.Zerg_Drone) 
		{
			System.out.println("got a drone from morph");
			drones.add(unit);
		} 
	}
	
	@Override
	public void onUnitDestroy(Unit unit){
		if(unit.getPlayer().isEnemy(self))
		{
			System.out.println("got a bitch!");
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
	
	/**
	 * getWorker()
	 * Finds an available worker unit
	 * 
	 * @return - a worker unit
	 */
	public Unit getWorker()
	{
		Unit availableWorker = null;
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
				availableWorker = drone;
			}
		}
		drones.remove(availableWorker);
		return availableWorker;
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
				System.out.println("builder is not null, tries to look for geyser");
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
				System.out.println("Sent the builder?");
			}
		}
}