package com.yu.hmm;

/*
HMM
*/
public class HMM{
	protected int N;
	protected int M;
	public double[][] A;//状态转移，N*N
	public double[][] B;//混淆矩阵，N*M
	public double[] PI;//初始状态，N*1

	public HMM(){};

	public HMM(int stateNum, int obsNum){
		N = stateNum;
		M = obsNum;
		A = new double[N][N];
		B = new double[N][M];
		PI = new double[N];
	}
}
