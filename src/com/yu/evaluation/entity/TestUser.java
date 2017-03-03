package com.yu.evaluation.entity;

import java.util.ArrayList;

public class TestUser {
	private String userId;
	private ArrayList<String> effectiveTraNo;//该用户有效轨迹的轨迹号
	
	public TestUser(String userId, ArrayList<String> effectiveTraNo){
		this.userId = userId;
		this.effectiveTraNo = effectiveTraNo;
	}

	public ArrayList<String> getEffectiveTraNo() {
		return effectiveTraNo;
	}
	
	public String getUserId() {
		return userId;
	}

	@Override
	public String toString() {
		return "userId:"+userId+",effectiveTraCnt:"+effectiveTraNo.size()+";";
	}
}
