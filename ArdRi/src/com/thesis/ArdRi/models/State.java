package com.thesis.ArdRi.models;

/**
 * Enumeration of possible states on board.
 * Maybe plain integer would be faster, but this contributes to
 * readability of implementation. 
 * @author lipayon, jerwin
 *
 */
public enum State {

	// empty cell
	EMPTY(-1),
	// attacker player cell
	ATTACKER(-2),
	// defender player cell
	DEFENDER(-3),
	// selected cell
	SELECTED(-5),
	//King cell
	KING(-6),
	//out of bound
	INVALID(-7);

	private int stateValue;

	private State(int value) {
		stateValue = value;
	}

	public int getValue() {
		return stateValue;
	}

	public static State fromInt(int i) {
		for (State s : values()) {
			if (s.getValue() == i) {
				return s;
			}
		}
		return EMPTY;
	}
}
