package Jarretts_Prototype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Chokepoint;

public class ZergRush extends DefaultBWListener {

	private Mirror mirror = new Mirror();

	private Game game;

	private Player self;

	private ProductionManager productionManager;
	private MilitaryManager militaryManager;

	private boolean isScouting = false;
	
	private boolean buildingPool = false;
	
	private boolean gasMorphing = false;
	private boolean gasMorphingStarted = false;
	private Unit gasMorpher;
	private boolean cheesed = false;
	
	private boolean initDrone = false;
	private boolean doInitDrone = true; //TOGGLE THIS FOR INITIAL DRONE

	
	private HashSet<Position> enemyBuildingLocation;
	private Position enemyBase;
	
	private int[] slowFrames = new int[3];
	
	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}

	@Override
	public void onUnitCreate(Unit unit) {

		if (unit.getType().isWorker()) 
		{
			System.out.println("new worker from oncreate");
			productionManager.addUnit(unit);
			
		} 
		else if (unit.getType() == UnitType.Zerg_Larva)
		{
			productionManager.addUnit(unit);
		} 
		else if (!unit.getType().isNeutral()) 
		{
			// Military Unit
			militaryManager.addUnit(unit);
		} 
		else if(unit.getType() == UnitType.Zerg_Spawning_Pool)
		{
			buildingPool = false; 
		} 
		else if(unit.getType() == UnitType.Zerg_Extractor)
		{
			
		}
		
		
	}
	
	@Override
	public void onUnitMorph(Unit unit){
		if(unit.getType() == UnitType.Zerg_Extractor){
			System.out.println("extractor is here");
			gasMorphing = true; //set flag to cancel check
			gasMorpher = unit;
		}
		else if (unit.getType().isWorker()) {
			System.out.println("new worker from onMorph");
			productionManager.addUnit(unit);
			
		} else if (unit.getType() == UnitType.Zerg_Larva) {
			productionManager.addUnit(unit);
			
		} else if (!unit.getType().isNeutral()) {
			// Military Unit
			militaryManager.addUnit(unit);
		}
		
		//System.out.println("This unit proc'd onUnitMorph: " + unit.getType());
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

		
		
		militaryManager = new MilitaryManager(game, self);
		try {
			productionManager = new ProductionManager(game, self);
		} catch (Exception e) {
			System.out.println("******PRODUCTION MANAGER ERROR******");
			e.printStackTrace();
		}
		enemyBuildingLocation = new HashSet<Position>();
		
		for(Unit u : self.getUnits())
		{
			if(u.getType() == UnitType.Zerg_Overlord)
				militaryManager.addUnit(u);
			
			else if(u.getType() == UnitType.Zerg_Drone
					|| u.getType() == UnitType.Zerg_Larva)
			{
				productionManager.addUnit(u);
			}
		}
		
	}

	@Override
	public void onFrame() {
		//time checking stuff
		long startTime = System.nanoTime();
		// print framerate
		game.drawTextScreen(0, 0,  "FPS: " + game.getFPS() );
	    game.drawTextScreen(0, 20, "Avg FPS: " + game.getAverageFPS() );
	    game.drawTextScreen(0, 40, "Supply Used: " + self.supplyUsed());
		
		
		try{
			// update game information
			updateEnemyLocations();

			// give orders to lower tier classes
			doStrategy();

			// update lower tier classes with new information from game
			productionManager.update();
			//militaryManager.update();
		}
		catch(Exception e){
			System.out.println("******PRODUCTION MANAGER ERROR******");
			e.printStackTrace();
		}
		
		long duration = (System.nanoTime() - startTime) / 1000000;
		//System.out.println(duration);
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
		if(game.isPaused())
			return;
		
		int armyCount = militaryManager.getArmyCount();

		ArrayList<UnitType> productionGoal = new ArrayList<UnitType>();

		// grab our resources
		int minerals = self.minerals();

		//starting condition, build a drone, then never again. can be toggled
		if(!initDrone && doInitDrone)
		{
			productionGoal.add(UnitType.Zerg_Drone);
			minerals -= 50;
			initDrone = true;
		}
		
		// build a spawning pool
		if (self.allUnitCount(UnitType.Zerg_Spawning_Pool) < 1 && minerals >= 200
				&& !buildingPool) {
			productionGoal.add(UnitType.Zerg_Spawning_Pool);
			minerals -= 200;
			buildingPool = true;
		}
		
		while(self.allUnitCount(UnitType.Zerg_Spawning_Pool) > 0 && minerals >= 50 && self.supplyUsed() < 18)
		{
			// build zerglings if possible
				productionGoal.add(UnitType.Zerg_Zergling);
				minerals -= 50;
		}
		
		
		// EXTRACTOR BULLSHIT TO BE ADDED HERE
		if (gasMorphing) 
		{
			System.out.println("checking to cancel");
			System.out.println("supply used: "+ self.supplyUsed());
			if(self.supplyUsed() == 18)
			{
				//productionManager.cancelGas();
				productionManager.cancelGas(gasMorpher);
				gasMorphing = false;
				gasMorphingStarted = false;
			}
				
		}
		if (self.allUnitCount(UnitType.Zerg_Extractor) < 1 && minerals >= 50 && self.supplyUsed() == 18
				&& !gasMorphingStarted) 
		{
			System.out.println("starting extractor");
			productionManager.makeGas();
			minerals -= 50;
			gasMorphingStarted = true;
		}
		
		
		
		//EXTRACTOR BULLSHIT ENDS HERE
		
		List<Unit> enemyBlds = new ArrayList<Unit>();
		List<Unit> enemyWorkers = new ArrayList<Unit>();
		List<Unit> enemyCores = new ArrayList<Unit>();
		List<Unit> enemyProblems = new ArrayList<Unit>();
		Unit overlord = null;
		for(Unit unit : game.getAllUnits())
		{
			if(!unit.getPlayer().isEnemy(self) && unit.exists())
			{
				if(unit.getType() == UnitType.Zerg_Overlord)
				{
					overlord = unit;
				}
			}
			else if(unit.exists() && unit.isVisible() && unit.getPlayer().isEnemy(self) && unit.isDetected())
			{
				game.drawTextScreen(0, 60, "Unit: " + unit.toString());
				if(unit.getType().isBuilding())
				{
					if(unit.getType() == UnitType.Zerg_Hatchery 
							|| unit.getType() == UnitType.Terran_Command_Center
							|| unit.getType() == UnitType.Protoss_Nexus)
					{
						enemyCores.add(unit);
					}
					else
					{
						enemyBlds.add(unit);
					}
				}
				else if(unit.getType().isWorker())
				{
					enemyWorkers.add(unit);
				}
				
				else if(unit.getType().canAttack())
				{
					//game.pauseGame();
					enemyProblems.add(unit);
				}
			}
		}
		game.drawTextScreen(0, 80, "workercount: "+ WorkerManager.getDroneCount());
		
		Unit target = null;
		for(Unit unit : game.self().getUnits())
		{
			if(unit.getType().isBuilding()) { /**do nothing with buildings**/ }
			if(unit.getType().isWorker()) { /**should already be handled in Production Manager**/ }
			if(unit.getType() == UnitType.Zerg_Zergling) { 
				if(!enemyProblems.isEmpty())//attack the closest "threat",
				{
					unit.attack(findClosest(enemyProblems, unit));
				}
				else if(!enemyWorkers.isEmpty())//else attack closest worker, 
				{
					unit.attack(findClosest(enemyWorkers, unit));
				}
				else if(!enemyCores.isEmpty())//else attack core,
				{
					unit.attack(findClosest(enemyCores, unit));
				}
				else if(!enemyBlds.isEmpty())//else attack buildings.
				{
					unit.attack(findClosest(enemyBlds, unit));
				}
				else//else go to overlord, 
				{
					unit.move(overlord.getPosition());
				}
			}
			if(unit.getType() == UnitType.Zerg_Overlord) { /**handled in scouting**/  }
		}


//		//System.out.println("supply total - supply used = " + (self.supplyTotal() - self.supplyUsed()));
//		if(self.supplyTotal() - self.supplyUsed() == 0 && gasMorpher == null
//				&& minerals >= 50){
//			System.out.println("It added the extractor!");
//			productionGoal.add(UnitType.Zerg_Extractor);
//			minerals -= 50;
//		}
//		
//		
//		if(self.supplyTotal() - self.supplyUsed() == 0
//				&& !cheesed && gasMorpher != null){
//			System.out.println("it tries to cancel!");
//			gasMorpher.cancelMorph();
//			cheesed = true;
//		}
		
		

		// send the goals down the chain
		productionManager.setGoal(productionGoal);

		// attack the unit
		if (armyCount >= 6) {
			// pick a building to attack and order an attack
//			if(!enemyUnitLocation.isEmpty())
//			{
//				for (Position pos : enemyUnitLocation) {
//					militaryManager.command(Command.Attack, 1.0, pos);
//					break;
//				}
//			}
//			else if(!enemyPeonLocation.isEmpty())
//			{
//				
//				for (Position pos : enemyPeonLocation) {
//					militaryManager.command(Command.Attack, 1.0, pos);
//					break;
//				}
//			}
			
			if(!enemyBuildingLocation.isEmpty())
			{
				for (Position pos : enemyBuildingLocation) {
					militaryManager.command(Command.Attack, pos);
					break;
				}
			}
		}

		// if we can't find the enemy continue scouting
		// lol what is this supposed to do? -Will-
		if (enemyBuildingLocation.isEmpty() && armyCount > 20) {
			isScouting = false;
		}

		// make sure we are scouting
		if (!isScouting) {
			militaryManager.command(Command.Scout, null);
			isScouting = true;
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

	public static void main(String[] args) {
		new ZergRush().run();
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
}
