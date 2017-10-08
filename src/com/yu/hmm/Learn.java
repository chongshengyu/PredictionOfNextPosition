package com.yu.hmm;
import java.util.ArrayList;
import java.util.List;

/*
 * 有监督学习
 * 李航 统计学习方法
 */
public class Learn extends HMM{
	public Learn(int stateNum, int obsNum, ArrayList<ArrayList<Pair>> records){
		super(stateNum, obsNum);
		int[][] aTemp = new int[N][N];//隐状态转移次数
		int[][] bTemp = new int[N][M];//混淆转移次数
		int[] piTemp = new int[N];//初始状态统计次数
		
		//统计转移次数
		for(List<Pair> list : records){
			if(list.size()<2)
				continue;
			for(int i=1;i<list.size();i++){
				Pair prePair = list.get(i-1);
				Pair thisPair = list.get(i);
				aTemp[prePair.getHidden()][thisPair.getHidden()] += 1;
				bTemp[thisPair.getHidden()][thisPair.getObservation()] += 1;
			}
			Pair headPair = list.get(0);
			bTemp[headPair.getHidden()][headPair.getObservation()] += 1;
			piTemp[headPair.getHidden()] += 1;
		}
		
		//计算a,b,pi的估计值
		for(int i=0;i<N;i++){//a,b
			double thisATotal = 0.0;
			for(int j=0;j<N;j++){
				thisATotal += aTemp[i][j];
			}
			for(int j=0;j<N;j++){
				if(thisATotal == 0.0){
					A[i][j] = 0.0;
					continue;
				}
				A[i][j] = aTemp[i][j] / thisATotal;
			}
			
			double thisBTotal = 0.0;
			for(int j=0;j<M;j++){
				thisBTotal += bTemp[i][j];
			}
			for(int j=0;j<M;j++){
				if(thisBTotal == 0.0){
					B[i][j] = 0.0;
					continue;
				}
				B[i][j] = bTemp[i][j] / thisBTotal;
			}
		}
		//pi
		double piTotal = 0.0;
		for(int i=0;i<N;i++){
			piTotal += piTemp[i];
		}
		for(int i=0;i<N;i++){
			if(piTotal == 0.0){
				PI[i] = 0.0;
				continue;
			}
			PI[i] = piTemp[i] / piTotal;
		}
	}
}
