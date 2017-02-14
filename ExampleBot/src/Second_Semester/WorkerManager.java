package Second_Semester;

import java.util.ArrayList;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwta.BWTA;


public class WorkerManager {
	
	private ArrayList<Unit> workers = new ArrayList<Unit>();
	
	public WorkerManager(Player self, Game game){
		
	}
	
	public void addWorker(Unit worker){
		workers.add(worker);
	}
	
	public Unit getWorker(){
		for(Unit worker : workers)
		{
			if(!worker.isMorphing() && worker.isInterruptible() && worker.isCompleted() && 
					!worker.isCarryingMinerals() && !worker.isCarryingGas())
			{
				workers.remove(worker);
				return worker;
			}
		}
		return null;
	}
	
	public ArrayList<Unit> getWorkerList(){
		return workers;
	}
	
//	public void removeWorker(Unit worker){
//		workers.remove(worker);
//	}
	
	public int getNumWorkers(){
		return workers.size();
	}
	
	public void manage(){
		for(Unit drone : workers)
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
				workers.remove(drone);
			}
		}
		
		
		//move a drone
		
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
}
