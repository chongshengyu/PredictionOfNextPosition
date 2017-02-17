package com.yu.draw.util;

import org.junit.Test;

public class StringUtil {
	private StringUtil(){
		
	}
	public static String padLeft(String s, int l, char c){
		StringBuilder sb = new StringBuilder();
		for(int i = 0;i<l-s.length();i++){
			sb.append(c);
		}
		sb.append(s);
		return sb.toString();
	}
	@Test
	public void Test(){
		System.out.println(padLeft("11", 2, '0'));
	}
}
