package Second_Semester;

import java.util.ArrayList;

import bwapi.Position;
import bwapi.Unit;


public class Troop {

	private ArrayList<Unit> units = new ArrayList<Unit>();
	
	public Troop(){
		
	}
	
	public void move(Position p)
	{
		for(Unit unit : units)
			unit.move(p);
	}
	
	public void attackMove(Position p)
	{
		for(Unit unit : units)
			unit.attack(p);
	}
	
	public void attack(Unit target)
	{
		for(Unit unit : units)
			unit.attack(target);
	}
	
}
