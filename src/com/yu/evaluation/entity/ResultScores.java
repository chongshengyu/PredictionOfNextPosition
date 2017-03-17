package com.yu.evaluation.entity;

public class ResultScores {
	private double myScore;//我的算法得分
	private double refScore;//对照算法得分
	public ResultScores(double myScore, double refScore) {
		super();
		this.myScore = myScore;
		this.refScore = refScore;
	}
	public ResultScores() {
		
	}
	public double getMyScore() {
		return myScore;
	}
	public void setMyScore(double myScore) {
		this.myScore = myScore;
	}
	public double getRefScore() {
		return refScore;
	}
	public void setRefScore(double refScore) {
		this.refScore = refScore;
	}
	
	
}
