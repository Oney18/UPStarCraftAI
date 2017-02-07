package Second_Semester;

import java.util.ArrayList;

import bwapi.Position;
import bwapi.Unit;


public class WorkerManager {
	
	private ArrayList<Unit> workers = new ArrayList<Unit>();
	
	public WorkerManager(){
		
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
	
	public void removeWorker(Unit worker){
		workers.remove(worker);
	}
	
	public int getNumWorkers(){
		return workers.size();
	}
	
	public void manage(){
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
}
