package Jarretts_Prototype;
//package StarcraftAI;
import java.util.*;
import bwapi.*;
import bwta.BWTA;

/**
 * This class tracks and manages all of the worker units 
 * the agent owns.
 * 
 * @author Kenny Trowbridge
 *
 */
public class WorkerManager{
	
	private Player self = null;
	private List<Unit> neutralUnits = new ArrayList<Unit>();
	private List<Unit> workerList = new ArrayList<Unit>(10);
	private List<Unit> larvae = new ArrayList<Unit>(10);
	
	/**
	 * constructor
	 * @param self  Player object for bot
	 * @param neutralUnits  List of neutral units in the game (Only used to task workers to gather)
	 */
	public WorkerManager(Player self, List<Unit> neutralUnits)
	{
		this.self = self;
		this.neutralUnits = neutralUnits;
	}
	
	/**
	 * update()
	 * This method maintains the list of worker units by pruning
	 * units that no longer exist. Also assigns idle units tasks
	 */
	public void update()
	{ 
		List<Unit> workersToRemove = new ArrayList<Unit>();
		List<Unit> larvaeToRemove = new ArrayList<Unit>();
		
		boolean gatheringGas = false;
		
		for(Unit worker : workerList)
		{			
			if(worker.isIdle() && worker.isCompleted())
			{
				//assign a task
				// protect against not finding any closest minerals. 
				// -- ie. don't pass null to u.gather(); that is a bad thing. 
				Unit closestMineral = findClosestMineral(BWTA.getStartLocation(self).getPosition());
				if(closestMineral != null)
				{
					worker.gather(closestMineral);
				}
			}
			
			//save dead units for deletion	
			if(!worker.exists())
			{
				workersToRemove.add(worker);
			}
			
			if(worker.isGatheringGas())
			{
				gatheringGas = true;
			}
		}
		
		if(!gatheringGas && self.completedUnitCount(UnitType.Zerg_Extractor)>=1)
		{
			Unit worker = getWorker();
			if(worker != null)
			{
				worker.gather(findClosestRefinery(BWTA.getStartLocation(self).getPosition()));
			}
		}
		
		for(Unit larva : larvae)
		{
			if(!larva.exists() 
					|| larva.getType() != UnitType.Zerg_Larva)
			{
				larvaeToRemove.add(larva);
			}
		}
		
		//remove dead workers
		for(Unit u : workersToRemove)
		{
			workerList.remove(u);
		}
		
		for(Unit u : larvaeToRemove)
		{
			larvae.remove(u);
		}
		
		System.out.println("There are "+larvae.size()+" larva in the list");
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
		for(Unit worker : workerList)
		{			
			//make sure no drones are going to morph at the same time
			if(worker.getOrder().equals(Order.ZergBuildingMorph))
			{
				return null;
			}		
			
			//find a free worker
			if(!worker.isMorphing() && worker.isInterruptible() 
					&& worker.isCompleted())
			{
				availableWorker = worker;
			}
		}
		
		return availableWorker;
	}
	
	/**
	 * getLarva()
	 * 
	 * @return a larva
	 */
	public Unit getLarva()
	{
		if(!larvae.isEmpty()){
			Unit larva = larvae.get(0);
			//larvae.remove(larva);
			return larva;
		}
		return null; //no larva available
	}
	
	/**
	 * getDroneCount()
	 * 
	 * @return the number of drones controlled by the player
	 */
	public int getDroneCount()
	{
		return workerList.size();
	}
	
	/**
	 * getLarvaCount()
	 * 
	 * @return the number of larva controlled by the player
	 */
	public int getLarvaCount(){
		return larvae.size();
	}
	
	/**
	 * addUnit
	 * Adds a unit to the workerList or larvae 
	 * 
	 * @param unit - unit to be added
	 */
	public void addUnit(Unit unit)
	{ 
		//add only worker units
		if (unit != null && unit.getType() == UnitType.Zerg_Drone)
		{
			workerList.add(unit);
		}
		else if(unit != null && unit.getType() == UnitType.Zerg_Larva)
		{
			larvae.add(unit);
		}
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
		for(Unit neutral : neutralUnits)
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
	 * findClosestExtractor()
	 * Finds the closest extractor to the given position
	 * 
	 * @param pos
	 * @return
	 */
	private Unit findClosestRefinery(Position pos){
		if(pos == null)
		{
			return null;
		}
		//init closest to first in list
		Unit closest = null;
		
		//find closest mineral
		for(Unit unit : self.getUnits())
		{
			//only check mineral fields
			if(unit.getType() == UnitType.Zerg_Extractor)
			{
				if(closest == null || unit.getDistance(pos) < closest.getDistance(pos))
				{
					closest = unit;
				}
			}
		}
		
		return closest;
	}
}

