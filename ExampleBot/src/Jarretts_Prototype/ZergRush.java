package Jarretts_Prototype;

import java.util.ArrayList;
import java.util.HashSet;

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
	private boolean cheesed = false;
	
	private boolean initDrone = false;
	private boolean doInitDrone = true; //TOGGLE THIS FOR INITIAL DRONE

	
	private HashSet<Position> enemyBuildingLocation;
	private Position enemyBase;

	//just commit ALLLLLLLL your crap
	
	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}

	@Override
	public void onUnitCreate(Unit unit) {

		if (unit.getType().isWorker()) {
			productionManager.addUnit(unit);
			
		} else if (unit.getType() == UnitType.Zerg_Larva) {
			productionManager.addUnit(unit);
			
		} else if (!unit.getType().isNeutral()) {
			// Military Unit
			militaryManager.addUnit(unit);
		} else if(unit.getType() == UnitType.Zerg_Spawning_Pool)
			buildingPool = false;
		
	}
	
	@Override
	public void onUnitMorph(Unit unit){
		if(unit.getType() == UnitType.Zerg_Extractor){
			
		}
		else if (unit.getType().isWorker()) {
			productionManager.addUnit(unit);
			
		} else if (unit.getType() == UnitType.Zerg_Larva) {
			productionManager.addUnit(unit);
			
		} else if (!unit.getType().isNeutral()) {
			// Military Unit
			militaryManager.addUnit(unit);
		}
		
		System.out.println("This unit proc'd onUnitMorph: " + unit.getType());
	}

	@Override
	public void onStart() {
		game = mirror.getGame();
		self = game.self();

		// slow is bad
		game.setLocalSpeed(5);

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
		try{
			// update game information
			updateEnemyLocations();

			// give orders to lower tier classes
			doStrategy();

			// update lower tier classes with new information from game
			productionManager.update();
			militaryManager.update();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	
	
	private void doStrategy() {
		// TODO put the meat of logic in here
		int armyCount = militaryManager.getArmyCount();

		ArrayList<UnitType> productionGoal = new ArrayList<UnitType>();

		// grab our resources
		int minerals = self.minerals();

		//starting condition, build a drone
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
		
		while(self.allUnitCount(UnitType.Zerg_Spawning_Pool) > 0 && minerals >= 50){
		// build zerglings if possible
			productionGoal.add(UnitType.Zerg_Zergling);
			minerals -= 50;
		}

		// send the goals down the chain
		// Keegan
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
		if (enemyBuildingLocation.isEmpty() && armyCount > 20) {
			isScouting = false;
		}

		// make sure we are scouting
		if (!isScouting) {
			militaryManager.command(Command.Scout, null);
			isScouting = true;
		}

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