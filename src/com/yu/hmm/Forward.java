package com.yu.hmm;

/*
 *1.计算t=1时的局部概率；
 *2.根据t时刻的局部概率计算t+1时刻的局部概率；
 *3.把T时刻的局部概率求和，即为最终的结果。
 */
public class Forward extends HMM{
	public Forward(int stateNum, int obsNum){
		super(stateNum, obsNum);
	}
	
	//ob是已知观察序列，返回值为该序列的概率
	public double[] forwardQiao(int[] ob){
		double[][] alpha = null;
		return forwardQiao(ob, alpha);
	}
	//ob是已知观察序列；alpha输出中间结果局部概率；返回观察序列概率
	public double[] forwardQiao(int[] ob, double[][] alpha){
		alpha = new double[ob.length][N];
		//1.初始化，计算初始时刻所有状态的局部概率
		for(int i=0;i<N;i++){
			alpha[0][i] = PI[i]*B[i][ob[0]];
//				System.out.println("alpha[0]["+i+"]:"+alpha[0][i]);
		}
		//2.归纳，计算每个时间点的局部概率
		for(int i=1;i<ob.length;i++){//第i次观察
			for(int j=0;j<N;j++){//该次观察的第j个状态
				double sum = 0;
				for(int k=0;k<N;k++){
					sum += alpha[i-1][k] * A[k][j];
				}
				alpha[i][j] = sum * B[j][ob[i]];
//					System.out.println("alpha["+i+"]["+j+"]:"+alpha[i][j]);
			}
		}
		//3.返回alpha最后一列
		double[] result = new double[N];
		for(int i=0;i<N;i++){
			result[i] = alpha[ob.length-1][i];
		}
		return result;
	}
	
	
	//ob是已知观察序列，返回值为该序列的概率
	public double forward(int[] ob){
		double[][] alpha = null;
		return forward(ob, alpha);
	}
	//ob是已知观察序列；alpha输出中间结果局部概率；返回观察序列概率
	public double forward(int[] ob, double[][] alpha){
		alpha = new double[ob.length][N];
		//1.初始化，计算初始时刻所有状态的局部概率
		for(int i=0;i<N;i++){
			alpha[0][i] = PI[i]*B[i][ob[0]];
//			System.out.println("alpha[0]["+i+"]:"+alpha[0][i]);
		}
		//2.归纳，计算每个时间点的局部概率
		for(int i=1;i<ob.length;i++){//第i次观察
			for(int j=0;j<N;j++){//该次观察的第j个状态
				double sum = 0;
				for(int k=0;k<N;k++){
					sum += alpha[i-1][k] * A[k][j];
				}
				alpha[i][j] = sum * B[j][ob[i]];
//				System.out.println("alpha["+i+"]["+j+"]:"+alpha[i][j]);
			}
		}
		//3.终止，该观察序列的概率等于最终所有N个局部概率之和
		double prob = 0.0;
		for(int i=0;i<N;i++){
			prob += alpha[ob.length-1][i];
		}
		return prob;
	}
}
