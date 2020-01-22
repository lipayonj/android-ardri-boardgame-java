package com.thesis.ArdRi.models;

import java.io.Serializable;

/**
 * Coordinates...
 *
 */
public class Coordinate implements Serializable{

	// These are public fields because of design decision.	
	public int i = -1;
	public int j = -1;
	
	public Coordinate() {}
			
	public Coordinate(int i, int j) {
		this.i = i;
		this.j = j;
	}
	
	public void delete() {
		i = -1;
		j = -1;
	}
	
	public boolean isNotEmpty() {
		return i != -1 && j != -1;
	}
	
	public boolean isEmpty() {
		return i == -1 || j == -1;
	}

	@Override
	public String toString() {
		return "("+i+","+j+")";
	}
}
