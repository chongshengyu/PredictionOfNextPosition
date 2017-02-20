package com.yu.draw.entity;

/**
 * 表示<rp,rn,t>的类
 * @author YCS
 *
 */
public class RegionModel {
	private Region pre;
	private Region next;
	private String nextTime;//到达region next的时间
	public RegionModel(Region pre, Region next, String nextTime) {
		super();
		this.pre = pre;
		this.next = next;
		this.nextTime = nextTime;
	}
	public Region getPre() {
		return pre;
	}
	public Region getNext() {
		return next;
	}
	public String getNextTime() {
		return nextTime;
	}
	@Override
	public String toString() {
		if(pre == null){
			return "<null,"+next.getLabel()+","+nextTime+">";
		}
		return "<"+pre.getLabel()+","+next.getLabel()+","+nextTime+">";
	}
	
	
}
