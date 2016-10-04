package Jarretts_Prototype;
//package StarcraftAI;
import java.util.*;

import javax.sound.midi.ControllerEventListener;

import bwapi.*;
import bwta.BWTA;
/**
 * The production manager is responsible for building units that the strategy manager requests. 
 * ProductionManager uses the WorkerManager and the BuildingManager to handle build and research 
 * orders passed to it from the StrategyManager.
 * 
 * @author Kenny Trowbridge
 * @author Alex Bowns
 * @author Max Robinson
 * @author Casey Sigelmann
 * 
 * This is ported to Zerg
 * @author Jarrett Oney
 */
public class ProductionManager {
	
	private Game game;
	private Player self; 
	
	private Hashtable<UnitType, ArrayList<UnitType>> techPaths; 
	
	private ArrayList<UnitType> productionQueue = new ArrayList<UnitType>(); 
	private ArrayList<UnitType> goals = new ArrayList<UnitType>();
	private ArrayList<UnitType> newGoal = new ArrayList<UnitType>();
	
	private WorkerManager workerManager;
	
	private Hashtable<UnitType, UnitType> buildingsForUnits = new Hashtable<UnitType, UnitType>();
	private List<Unit> damagedBuildings = new ArrayList<Unit>();
	
	/**
	 * Ctor
	 * Sets up the needed instance variables for the class and sets up the game and player objects needed 
	 * to reference the game.
	 * 
	 * @param game
	 * @param self
	 */
	public ProductionManager(Game game, Player self){
		this.game = game;
		this.self = self;
		
		this.techPaths = new Hashtable<UnitType, ArrayList<UnitType>>();
		
		this.workerManager = new WorkerManager(self, game.getNeutralUnits());
		
		this.productionQueue = new ArrayList<UnitType>();
		this.goals = new ArrayList<UnitType>();
		this.newGoal = new ArrayList<UnitType>();
		
		//add starting workers to worker list
		for(Unit u : game.self().getUnits())
		{
			if(u.getType() == UnitType.Zerg_Drone
					|| u.getType() == UnitType.Zerg_Larva)
			{
				workerManager.addUnit(u);
			}
		}
		
		
		techPaths = initTechPaths();		
	}

	/**
	 * addUnit()
	 * This checks the type of the given unit and then calls the appropriate 
	 * addUnit method in either the BuildingManager or WorkerManager.
	 * 
	 * @param unit - the specific unit that we are checking
	 */
	public void addUnit(Unit unit)
	{
		//dont add null or units that do not belong to the agent		if (unit == null || unit.getPlayer() != self)
			return;
		
		if(unit.getType() == UnitType.Zerg_Drone
				|| unit.getType() == UnitType.Zerg_Larva)
		{
			workerManager.addUnit(unit);
		}
		
	}
	
	/**
	 * setGoal()
	 * This method sets the newGoal instance variable to the specified parameter.
	 * 
	 * @param newGoal - the new goal instance variable
	 */
	public void setGoal(ArrayList<UnitType> newGoal)
	{
		this.newGoal = newGoal;
	}
	
	/** 
	 * buildBuilding()
	 * This method will retrieve a worker from the Worker Manager,
	 * then using that worker, issue a build command to the Building Manager to construct 
	 * the building type specified.
	 *  
	 * @param unitType - type of building to build
	 * @return true if order is sent
	 */
	private void buildBuilding(UnitType buildingType)
	{
		if(buildingType.isBuilding())
		{
			Unit builder = workerManager.getWorker();
			
			// make sure the builder is not null
			if(builder != null && game.canMake(buildingType, builder))
			{
				if(buildingType == UnitType.Zerg_Extractor){
				
					Unit closestGeyser = null;
					Position startArea = BWTA.getStartLocation(self).getPosition();
					
					//loop through game's geysers, get closest to our base
					for(Unit geyser : game.getGeysers() ){
						if(game.canBuildHere(geyser.getTilePosition(), buildingType, builder, true)){
							
							//calculate distance, if less then is closes
							if(closestGeyser == null || closestGeyser.getDistance(startArea) > geyser.getDistance(startArea)){
								closestGeyser = geyser;
							}					
						}
					}
					builder.build(buildingType, closestGeyser.getTilePosition());
				}
				//average building, need not geyser
				else{					
					TilePosition buildSpot = getBuildTile(builder, buildingType, BWTA.getStartLocation(self).getTilePosition());
					
					if(buildSpot != null){
						builder.build(buildingType, buildSpot);
					}
				}
			}
		}
	}
	
	
	/**
	 * update()
	 * This method is responsible for calling the update methods in the WorkerManager.It checks if goal and newGoal are not the same and if so,
	 * it sets goal to newGoal and updates the productionQueue. 
	 * It then calls processQueue to initiate building construction, unit training, 
	 * and technology research using the productionQueue.
	 * 
	 */
	public void update()
	{
		try
		{
			workerManager.update();
			
			//if goal and new goal are the same, 
			if(!Arrays.deepEquals(goals.toArray(), newGoal.toArray()))
			{
				goals = newGoal;
				
				productionQueue.clear();
				
				//find paths for all of the goals
				//update production queue
				for(UnitType u : goals)
				{
					productionQueue.add(u);
				}
				
			}
			

			
			//issue builder orders
			processQueue();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	

	
	/**
	 * processQueue()
	 * The process queue method is responsible for sending out build orders based on the 
	 * contents of the priority queue. 
	 * 
	 */
	private void processQueue()
	{
		for(UnitType goal : productionQueue)
		{

			
			if(goal.isBuilding())
			{
				buildBuilding(goal);
			}
			else
			{
				//is unit, get a larva
				Unit larva = workerManager.getLarva();
				System.out.println("Goal is " + goal.toString());
				if(larva != null)
				{
					//TODO if we create different units, we will need to add logic checking here
					
					if(!larva.morph(goal)){
						System.out.println("The larva tried to morph but BWAPI said no!");
					}
					
				}
			}
		}
	}
	
	/**
	 * findTechPath()
	 * This method is responsible for taking a desired unit type and constructing a
	 * dependency list based on the tech DAG. We will use Dijkstra’s shortest path algorithm 
	 * in order to implement this. 
	 * 
	 * @param goalUnit - end point of the path
	 * @return a dependency list of what to construct (in what order) for the specific unit
	 */
	private ArrayList<UnitType> findTechPath(UnitType goalUnit)
	{
		return new ArrayList<UnitType>(techPaths.get(goalUnit));
	}
	
//	/**
//	 * examinePath()
//	 * This method is responsible for examining a tech path list and determining if a 
//	 * subsequence of the dependency list is already in the priority queue.
//	 * If there is a subsequence that is found, that sequence will be removed from the list.
//	 * 
//	 * @param path - path to examine
//	 * @return path of buildings that have not been constructed yet
//	 */
//	private ArrayList<UnitType> examinePath(ArrayList<UnitType> path)
//	{
//		//remove units from path that we already have. 
//		ArrayList<UnitType> toRemove = new ArrayList<UnitType>();
//		for(UnitType uType : path)
//		{
//			if(buildingManager.getBuilding(uType, false) != null)
//			{
//				// If it is the last element, don't remove it. 
//				if(path.lastIndexOf(uType) == path.size()-1){
//					break;
//				}
//				toRemove.add(uType);
//			}
//			else
//			{
//				break;
//			}
//		}
//		
//		for(UnitType ut : toRemove)
//		{
//			path.remove(ut);
//		}
//		
//		return path; 
//	}
	
//	/**
//	 * onEnd()
//	 * Execute code when a game ends
//	 * 
//	 * @param isWinner  If the bot is the winner
//	 * @param elapsedTime  Game time length
//	 */
//	public void onEnd(boolean isWinner, long elapsedTime)
//	{
//		buildingManager.onEnd(isWinner, elapsedTime);		
//	}

	/**
	 * reduceCrossover()
	 * Examines all build paths and removes redundant build orders. This prevents the building of 
	 * multiple required buildings for different path destinations.
	 * 
	 * @param ProdQueue current production queue
	 * @return the altered production queue
	 */
	private ArrayList<List<UnitType>> reduceCrossover(ArrayList<List<UnitType>> ProdQueue)
	{
		ArrayList<List<UnitType>> queue = new ArrayList<List<UnitType>>(ProdQueue);
		
		ArrayList<UnitType> seen = new ArrayList<UnitType>();
		for(int i = 0; i<queue.size(); i++)
		{
			for(int j = 0; j<queue.get(i).size(); j++)
			{
				UnitType ut = queue.get(i).get(j); 
				if(!seen.contains(ut))
				{
					seen.add(ut);
				}
				else{
					// already been seen, remove from this list.
					// unless it is the last element.
					if(j != queue.get(i).size()-1 ){
						queue.get(i).remove(j);
						
						// Decrement the index to relook at the same j spot. 
						j--;
					}
				}
			}
		}
		
		return queue;
	}
	
	/**
	 * getValueAtIndex()
	 * Finds the unit type at the specified index in all of the build paths
	 * 
	 * @param idx
	 * @param queue production queue to examine
	 * @return array of UnitTypes the size of the number of paths in queue
	 */
	public UnitType[] getValueAtIndex(int idx, ArrayList<List<UnitType>> queue)
	{
		UnitType[] values = new UnitType[queue.size()];
		for(int list_idx = 0; list_idx < queue.size(); list_idx++)
		{
			// if the index is "off the end" of one of the queue arrays, that entry is null
			if(idx > queue.get(list_idx).size()-1)
			{
				values[list_idx] = null; 
			}
			else{
				values[list_idx] = queue.get(list_idx).get(idx);
			}
		}
		return values;
	}
	
	/**
	 * getTechPaths()
	 * Getter method for Tech paths. 
	 * @return Hashtable of tech paths. 1 path for each unit type. 
	 */
	public Hashtable<UnitType, ArrayList<UnitType>> getTechPaths()
	{
		return techPaths;
	}
	
	/**
	 * printProductionQueue
	 * prints the production queue as it currently is. 
	 */
	public void printProcutionQueue()
	{
		if(productionQueue.size() > 0)
		{
			System.out.println("Production QUEUE");
			for(UnitType list : productionQueue)
			{
				System.out.println(list.toString());
			}
		}
	}

	/**
	 * initTechPaths()
	 * creates a Hashtable of all of the tech paths needed to get to a given unit.  
	 * This runs once at the instantiation of the class and never again. 
	 * 
	 */
	//TODO fix to zerg if we wanna build more than zerglings
	private Hashtable<UnitType, ArrayList<UnitType>> initTechPaths()
	{
		Hashtable<UnitType, ArrayList<UnitType>> techPaths = new Hashtable<UnitType, ArrayList<UnitType>>();
		// command center
		ArrayList<UnitType> cc = new ArrayList<UnitType>();
		cc.add(UnitType.Terran_Command_Center);
		techPaths.put(UnitType.Terran_Command_Center, cc);
		
		// Supply Depot
		ArrayList<UnitType> supply = new ArrayList<UnitType>();
		supply.add(UnitType.Terran_Supply_Depot);
		techPaths.put(UnitType.Terran_Supply_Depot, supply);
		
		// Refinery
		ArrayList<UnitType> refinery = new ArrayList<UnitType>();
		refinery.add(UnitType.Terran_Refinery);
		techPaths.put(UnitType.Terran_Refinery, refinery);
		
		/* Branch ONE */
		// Engineering bay
		ArrayList<UnitType> ebay = new ArrayList<UnitType>(cc);
		ebay.add(UnitType.Terran_Engineering_Bay);
		techPaths.put(UnitType.Terran_Engineering_Bay, ebay);
		
		// Missile turret 
		ArrayList<UnitType> turret = new ArrayList<UnitType>(ebay);
		turret.add(UnitType.Terran_Missile_Turret);
		techPaths.put(UnitType.Terran_Missile_Turret, turret);
		
		/* Branch TWO */
		// Barracks
		ArrayList<UnitType> racks = new ArrayList<UnitType>(cc);
		racks.add(UnitType.Terran_Barracks);
		techPaths.put(UnitType.Terran_Barracks, racks);
		
		// bunker
		ArrayList<UnitType> bunker = new ArrayList<UnitType>(racks);
		bunker.add(UnitType.Terran_Bunker);
		techPaths.put(UnitType.Terran_Bunker, bunker);
		
		// academy
		ArrayList<UnitType> academy = new ArrayList<UnitType>(racks);
		academy.add(UnitType.Terran_Academy);
		techPaths.put(UnitType.Terran_Academy, academy);
		
		// Factory
		ArrayList<UnitType> factory = new ArrayList<UnitType>(racks);
		factory.add(UnitType.Terran_Factory);
		techPaths.put(UnitType.Terran_Factory, factory);
		
		// Armory 
		ArrayList<UnitType> armory = new ArrayList<UnitType>(factory);
		armory.add(UnitType.Terran_Armory);		
		techPaths.put(UnitType.Terran_Armory, armory);
		
		// Starport	
		ArrayList<UnitType> starport = new ArrayList<UnitType>(factory);
		starport.add(UnitType.Terran_Starport);		
		techPaths.put(UnitType.Terran_Starport, starport);
		
		// Science facility
		ArrayList<UnitType> science = new ArrayList<UnitType>(starport);
		science.add(UnitType.Terran_Science_Facility);		
		techPaths.put(UnitType.Terran_Starport, starport);
		
		/* Add-ons */ 
		
		// ComSat --> Dependent, academy
		ArrayList<UnitType> comSat = new ArrayList<UnitType>(academy);
		comSat.add(UnitType.Terran_Comsat_Station);		
		techPaths.put(UnitType.Terran_Comsat_Station, comSat);
		
		// Machine Shop --> Dependent, factory
		ArrayList<UnitType> machineShop = new ArrayList<UnitType>(factory);
		machineShop.add(UnitType.Terran_Machine_Shop);		
		techPaths.put(UnitType.Terran_Machine_Shop, machineShop);
		
		// Control Tower --> Dependent, starport
		ArrayList<UnitType> tower = new ArrayList<UnitType>(starport);
		tower.add(UnitType.Terran_Control_Tower);		
		techPaths.put(UnitType.Terran_Control_Tower, tower);
		
		// Physics Lab --> Dependent, Science Facility
		ArrayList<UnitType> physics = new ArrayList<UnitType>(science);
		physics.add(UnitType.Terran_Physics_Lab);		
		techPaths.put(UnitType.Terran_Physics_Lab, physics);
		
		// Covert Ops --> Dependent, Science Facility
		ArrayList<UnitType> ops = new ArrayList<UnitType>(science);
		ops.add(UnitType.Terran_Covert_Ops);		
		techPaths.put(UnitType.Terran_Covert_Ops, ops);
		
		// nuclear Silo --> Dependent, Covert Ops
		ArrayList<UnitType> nuke = new ArrayList<UnitType>(ops);
		nuke.add(UnitType.Terran_Nuclear_Silo);		
		techPaths.put(UnitType.Terran_Nuclear_Silo, nuke);
		
		/* Non-Building Units */
		ArrayList<UnitType> scv = new ArrayList<UnitType>(cc);
		scv.add(UnitType.Terran_SCV);		
		techPaths.put(UnitType.Terran_SCV, scv);
		
		ArrayList<UnitType> marine = new ArrayList<UnitType>(racks);
		marine.add(UnitType.Terran_Marine);		
		techPaths.put(UnitType.Terran_Marine, marine);
		
		ArrayList<UnitType> medic = new ArrayList<UnitType>(academy);
		medic.add(UnitType.Terran_Medic);		
		techPaths.put(UnitType.Terran_Medic, medic);
		
		ArrayList<UnitType> fireBat = new ArrayList<UnitType>(racks);
		fireBat.add(UnitType.Terran_Firebat);		
		techPaths.put(UnitType.Terran_Firebat, fireBat);
		
		ArrayList<UnitType> ghost = new ArrayList<UnitType>(ops);
		ghost.add(UnitType.Terran_Academy);
		ghost.add(UnitType.Terran_Ghost);		
		techPaths.put(UnitType.Terran_Ghost, ghost);
		
		ArrayList<UnitType> vulture = new ArrayList<UnitType>(factory);
		vulture.add(UnitType.Terran_Vulture);		
		techPaths.put(UnitType.Terran_Vulture, vulture);
		
		ArrayList<UnitType> spiderMine = new ArrayList<UnitType>(vulture);
		spiderMine.add(UnitType.Terran_Vulture_Spider_Mine);		
		techPaths.put(UnitType.Terran_Vulture_Spider_Mine, spiderMine);
		
		ArrayList<UnitType> tank = new ArrayList<UnitType>(machineShop);
		tank.add(UnitType.Terran_Siege_Tank_Tank_Mode);	
		techPaths.put(UnitType.Terran_Siege_Tank_Tank_Mode, tank);
		
		ArrayList<UnitType> goliath = new ArrayList<UnitType>(armory);
		goliath.add(UnitType.Terran_Goliath);		
		techPaths.put(UnitType.Terran_Goliath, goliath);
		
		ArrayList<UnitType> wraith = new ArrayList<UnitType>(starport);
		wraith.add(UnitType.Terran_Wraith);		
		techPaths.put(UnitType.Terran_Wraith, wraith);
		
		ArrayList<UnitType> dropship = new ArrayList<UnitType>(tower);
		dropship.add(UnitType.Terran_Dropship);		
		techPaths.put(UnitType.Terran_Dropship, dropship);
		
		ArrayList<UnitType> scienceVessel = new ArrayList<UnitType>(tower);
		scienceVessel.add(UnitType.Terran_Science_Facility);
		scienceVessel.add(UnitType.Terran_Science_Vessel);		
		techPaths.put(UnitType.Terran_Science_Vessel, scienceVessel);
		
		ArrayList<UnitType> BattleCruiser = new ArrayList<UnitType>(physics);
		BattleCruiser.add(UnitType.Terran_Control_Tower);
		BattleCruiser.add(UnitType.Terran_Battlecruiser);
		techPaths.put(UnitType.Terran_Battlecruiser, BattleCruiser);
		
		ArrayList<UnitType> valkyrie = new ArrayList<UnitType>(tower);
		valkyrie.add(UnitType.Terran_Armory);
		valkyrie.add(UnitType.Terran_Valkyrie);
		techPaths.put(UnitType.Terran_Valkyrie, valkyrie);
	
		return techPaths;
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
	 public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
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
}
