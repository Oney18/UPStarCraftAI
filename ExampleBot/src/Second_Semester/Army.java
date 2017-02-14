package Second_Semester;

import java.util.ArrayList;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;


public class Army {

	private ArrayList<Troop> army;
	
	public Army(Player self, Game game){
		army = new ArrayList();
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
	
	public void attack(Unit target)
	{
		for(Troop troop : army)
			troop.attack(target);
	}
	
}
