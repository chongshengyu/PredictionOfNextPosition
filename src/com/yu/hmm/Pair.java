package com.yu.hmm;

public class Pair {
	private int hidden;
	private int observation;
	public int getHidden() {
		return hidden;
	}
	public void setHidden(int hidden) {
		this.hidden = hidden;
	}
	public int getObservation() {
		return observation;
	}
	public void setObservation(int observation) {
		this.observation = observation;
	}
	public Pair(int hidden, int observation) {
		super();
		this.hidden = hidden;
		this.observation = observation;
	}
	
	
}
