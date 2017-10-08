package com.yu.hmm;
import java.util.ArrayList;
import java.util.List;

public class TestHMM {
	enum Algae {dry,dryish,damp,soggy };  // 可见状态（海藻的形态：干燥、微湿、湿、湿透）
	enum Weather{sunny,cloudy,rainy}; //隐藏状态（天气状况：晴朗、多云、下雨）

    public static void main(String[] args)
    {
        // 测试前向算法和后向算法
        CheckForwardAndBackward();
        // 测试维特比算法
        CheckViterbi();
        // 测试监督学习算法
        CheckLearn();
    }

    // 测试前向算法和后向算法
    static void CheckForwardAndBackward()
    {
        // 状态转移矩阵
        double[][] A = 
        {
            {0.500, 0.375, 0.125},
            {0.250, 0.125, 0.625},
            {0.250, 0.375, 0.375}
        };

        // 混淆矩阵
        double[][] B = 
        {
            {0.60, 0.20, 0.15, 0.05},
            {0.25, 0.25, 0.25, 0.25},
            {0.05, 0.10, 0.35, 0.50}
        };

        // 初始概率向量
        double[] PI = {0.63,0.17,0.20};

        // 观察序列
        int[] OB = {Algae.dry.ordinal(), Algae.damp.ordinal(), Algae.soggy.ordinal()};
        System.out.println("--------------------------前向算法测试--------------------------");
        System.out.println("状态转移概率矩阵：");
        for(int i=0;i<A.length;i++)
        {
        	for(int j=0;j<A[0].length;j++)
        	{
        		System.out.print(A[i][j]+"\t");
        	}
        	System.out.println();
        }
        System.out.println("符号观测概率矩阵：");
        for(int i=0;i<B.length;i++)
        {
        	for(int j=0;j<B[0].length;j++)
        	{
        		System.out.print(B[i][j]+"\t");
        	}
        	System.out.println();
        }
        System.out.println("初始概率向量：{"+PI[0]+" "+PI[1]+" "+PI[2]+"}");
        System.out.println("隐藏状态序列：{"+Weather.sunny+" "+Weather.cloudy+" "+Weather.rainy+"}");
        System.out.println("观测序列：{"+Algae.dry+" "+Algae.damp+" "+Algae.soggy+"}");

        // 初始化HMM模型
        Forward forward = new Forward(A.length, B[0].length);
        forward.A=A;
        forward.B=B;
        forward.PI=PI;   

        // 观察序列的概率
        double probability = forward.forward(OB);
        System.out.println("观测序列概率："+probability);
    }       

    // 测试维特比算法
    static void CheckViterbi()
    {
        // 状态转移矩阵
        double[][] A = 
        {
            {0.500, 0.250, 0.250},
            {0.375, 0.125, 0.375},
            {0.125, 0.675, 0.375}
        };

        // 混淆矩阵
        double[][] B = 
        {
            {0.60, 0.20, 0.15, 0.05},
            {0.25, 0.25, 0.25, 0.25},
            {0.05, 0.10, 0.35, 0.50}
        };

        // 初始概率向量
        double[] PI = { 0.63, 0.17, 0.20 };
        // 观察序列
        int[] OB = {Algae.dry.ordinal(), Algae.damp.ordinal(), Algae.soggy.ordinal()};
        System.out.println("--------------------------维特比算法测试--------------------------");
        System.out.println("状态转移概率矩阵：");
        for(int i=0;i<A.length;i++)
        {
        	for(int j=0;j<A[0].length;j++)
        	{
        		System.out.print(A[i][j]+"\t");
        	}
        	System.out.println();
        }
        System.out.println("符号观测概率矩阵：");
        for(int i=0;i<B.length;i++)
        {
        	for(int j=0;j<B[0].length;j++)
        	{
        		System.out.print(B[i][j]+"\t");
        	}
        	System.out.println();
        }
        System.out.println("初始概率向量：{"+PI[0]+" "+PI[1]+" "+PI[2]+"}");
        System.out.println("隐藏状态序列：{"+Weather.sunny+" "+Weather.cloudy+" "+Weather.rainy+"}");
        System.out.println("观测序列：{"+Algae.dry+" "+Algae.damp+" "+Algae.soggy+"}");
        
        // 初始化HMM模型
        Viterbi viterbi = new Viterbi(A.length, B[0].length);
        viterbi.A=A;
        viterbi.B=B;
        viterbi.PI=PI;           

        // 找出最有可能的隐藏状态序列
        double probability = 0;

        List list=viterbi.viterbi(OB,probability);
        int[] Q = (int[]) list.get(0);//返回隐藏状态序列
        System.out.print("最可能的隐藏状态序列为：{");
        for(int value:Q)
        {
        	System.out.print(Weather.values()[value]+" ");
        }
        System.out.println("}");
        System.out.println("最大可能性为："+list.get(1));
    }

    static void CheckLearn(){
    	ArrayList lists = new ArrayList();
    	
    	List list1 = new ArrayList<Pair>();
    	List list2 = new ArrayList<Pair>();
    	List list3 = new ArrayList<Pair>();
    	
    	list1.add(new Pair(Weather.sunny.ordinal(),Algae.dry.ordinal()));
    	list1.add(new Pair(Weather.sunny.ordinal(),Algae.dry.ordinal()));
    	list1.add(new Pair(Weather.cloudy.ordinal(),Algae.dryish.ordinal()));

    	list2.add(new Pair(Weather.sunny.ordinal(),Algae.dry.ordinal()));
    	list2.add(new Pair(Weather.cloudy.ordinal(),Algae.dry.ordinal()));
    	list2.add(new Pair(Weather.cloudy.ordinal(),Algae.dryish.ordinal()));
    	list2.add(new Pair(Weather.rainy.ordinal(),Algae.damp.ordinal()));
    	
    	list3.add(new Pair(Weather.cloudy.ordinal(),Algae.dryish.ordinal()));
    	list3.add(new Pair(Weather.rainy.ordinal(),Algae.damp.ordinal()));
    	list3.add(new Pair(Weather.rainy.ordinal(),Algae.damp.ordinal()));
    	list3.add(new Pair(Weather.rainy.ordinal(),Algae.soggy.ordinal()));
    	
    	lists.add(list1);lists.add(list2);lists.add(list3);
    	Learn learn = new Learn(3, 4, lists);
    	System.out.println("--------------------------学习算法测试--------------------------");
        System.out.println("状态转移概率矩阵：");
        for(int i=0;i<learn.A.length;i++)
        {
        	for(int j=0;j<learn.A[0].length;j++)
        	{
        		System.out.print(learn.A[i][j]+"\t");
        	}
        	System.out.println();
        }
        System.out.println("混淆概率矩阵：");
        for(int i=0;i<learn.B.length;i++)
        {
        	for(int j=0;j<learn.B[0].length;j++)
        	{
        		System.out.print(learn.B[i][j]+"\t");
        	}
        	System.out.println();
        }
        System.out.println("初始状态矩阵：");
        for(int i=0;i<learn.PI.length;i++)
        {
        	System.out.print(learn.PI[i]+"\t");
        }
        System.out.println();
        System.out.println("--------------------------学习算法测试+前向算法--------------------------");
        Forward forward = new Forward(learn.A.length, learn.B[0].length);
        forward.A=learn.A;
        forward.B=learn.B;
        forward.PI=learn.PI;   

        // 观察序列的概率
        int[] OB1 = {Algae.dry.ordinal(), Algae.dryish.ordinal(), Algae.damp.ordinal()};
        double probability1 = forward.forward(OB1);
        System.out.println("观测序列概率1："+probability1);
        int[] OB2 = {Algae.dry.ordinal(), Algae.dryish.ordinal(), Algae.soggy.ordinal()};
        double probability2 = forward.forward(OB2);
        System.out.println("观测序列概率2："+probability2);
        int[] OB3 = {Algae.dry.ordinal(), Algae.soggy.ordinal(), Algae.dryish.ordinal()};
        double probability3 = forward.forward(OB3);
        System.out.println("观测序列概率3："+probability3);
        System.out.println("--------------------------学习算法测试+viterbi算法--------------------------");
        Viterbi viterbi = new Viterbi(learn.A.length, learn.B[0].length);
        viterbi.A=learn.A;
        viterbi.B=learn.B;
        viterbi.PI=learn.PI;           

        // 找出最有可能的隐藏状态序列
        double probability = 0;
        int[] OB11 = {Algae.dry.ordinal(), Algae.dryish.ordinal(), Algae.damp.ordinal()};
        List list11=viterbi.viterbi(OB11,probability);
        int[] Q1 = (int[]) list11.get(0);//返回隐藏状态序列
        System.out.print("最可能的隐藏状态序列为：{");
        for(int value:Q1)
        {
        	System.out.print(Weather.values()[value]+" ");
        }
        System.out.println("}");
        System.out.println("最大可能性为："+list11.get(1));
        int[] OB22 = {Algae.dry.ordinal(), Algae.dryish.ordinal(), Algae.soggy.ordinal()};
        List list22=viterbi.viterbi(OB22,probability);
        int[] Q2 = (int[]) list22.get(0);//返回隐藏状态序列
        System.out.print("最可能的隐藏状态序列为：{");
        for(int value:Q2)
        {
        	System.out.print(Weather.values()[value]+" ");
        }
        System.out.println("}");
        System.out.println("最大可能性为："+list22.get(1));
        int[] OB33 = {Algae.dry.ordinal(), Algae.soggy.ordinal(), Algae.dryish.ordinal()};
        List list33=viterbi.viterbi(OB33,probability);
        int[] Q3 = (int[]) list33.get(0);//返回隐藏状态序列
        System.out.print("最可能的隐藏状态序列为：{");
        for(int value:Q3)
        {
        	System.out.print(Weather.values()[value]+" ");
        }
        System.out.println("}");
        System.out.println("最大可能性为："+list33.get(1));
    }
}

