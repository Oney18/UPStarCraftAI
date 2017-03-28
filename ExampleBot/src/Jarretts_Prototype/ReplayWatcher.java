package Jarretts_Prototype;


import java.util.ArrayList;
import java.util.List;

import bwapi.Color;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;

public class ReplayWatcher extends DefaultBWListener{
	private Mirror mirror = new Mirror();
	private Game game;
	private Player self;

	private boolean speed22;
	private boolean speed0;
	private TilePosition enemyBase;
	private Unit zergling;
	private int frames;



	public static void main(String[] args) {
		new ReplayWatcher().run();
	}

	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}

	@Override
	public void onStart() {
		frames = 0;
		game = mirror.getGame();
		BWTA.readMap();
		BWTA.analyze();	

		for(Player p : game.getPlayers())
		{
			System.out.println("Player " + p.getID() + " is called " + p.getName());
			if(p.getName().equals("AI"))
				self = p;
			else if(!p.getName().equals("Neutral"))
				enemyBase = p.getStartLocation();
		}

		game.setVision(self);

		game.setLocalSpeed(0);
		speed0 = true;
		speed22 = false;
	}	


	@Override
	public void onFrame() {
		if(game.isPaused())
		{
			System.out.println("Frame count is " + frames);
			return;
		}
		
		frames++;
		
		if(zergling != null && !zergling.exists())
			zergling = null;

		List<Unit> zergs = new ArrayList<Unit>();

		for(Unit unit : self.getUnits())
			if(zergling == null && unit.getType() == UnitType.Zerg_Zergling && unit.exists() && !unit.isIdle()) //a zerglign exists to follow
				zergs.add(unit);
			else if (zergling != null) //if both exist no need for more search
				break;

		int maxDist = Integer.MAX_VALUE;

		for(Unit unit : zergs)
			if(unit.getTilePosition().getDistance(enemyBase) < maxDist)
			{
				zergling = unit;
				maxDist = (int) unit.getTilePosition().getDistance(enemyBase);
			}

		if(zergling != null && !zergling.isIdle())
		{
			int x = Math.max(zergling.getPosition().getX() - 325, 0);
			int y = Math.max(zergling.getPosition().getY() - 200, 0);
			game.setScreenPosition(new Position(x, y));

			if(speed0)
			{
				game.setLocalSpeed(7);
				speed0 = false;
				speed22 = true;
			}
		}
		else if (speed22)
		{
			game.setLocalSpeed(0);
			speed0 = true;
			speed22 = false;
		}



	}
}
