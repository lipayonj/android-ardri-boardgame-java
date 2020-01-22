package com.thesis.ArdRi.models;

import java.io.Serializable;

/**
 * Move class that represent one move of player.
 * Source is selected ball and destination is empty square.
 * @author jan.krizan
 *
 */

public class Move implements Serializable{
	
	// All these field are public because it is simpler to access them
	// that way and because this do not break encapsulation in some 
	// horrible way. :)

	public Coordinate sourceMove = new Coordinate();
	public Coordinate destinationMove = new Coordinate();

	public State player = State.EMPTY;
	
	public Move() {
		
	}
	
	public Move(Coordinate sourceMove, Coordinate destinationMove) {
		this.sourceMove = sourceMove;
		this.destinationMove = destinationMove;		
	}
	
	public void delete() {
		this.sourceMove.delete();
		this.destinationMove.delete();		
	}
	
	public boolean isEmpty() {
		return this.sourceMove.isEmpty() && this.destinationMove.isEmpty();
	}

	@Override
	public String toString() {
		return "Source: ("+sourceMove.i + "," + sourceMove.j + ") " +
				"Dest: ("+destinationMove.i + "," + destinationMove.j + ")";
	}
}
