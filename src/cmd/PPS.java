package cmd;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class PPS {
	
	public static void main(String[] args) throws FileNotFoundException {
		
		/** 控制台命令行参数，在Paper\WebRoot\WEB-INF\classes目录下执行。
		 * 结果输出到目录Paper\WebRoot\WEB-INF\classes下的文件中
		 * 1.每个用户轨迹过滤后有用数据的占比。
		 * ----java cmd.PPS DataFilterEvaluator
		 * 
		 * 2.16个用户不同lamda_max_w参数值下的region数量
		 * ----java cmd.PPS RegionCnt
		 * 
		 * 3.8个用户不同lamda_max_w参数值下的预测精度
		 * ----java cmd.PPS PredictionEvaluator -lamda 'vl' -tH 1.2 -tL 1.0
		 * --------value of vl: 1,2,3,4,5
		 * 
		 * 4.8个用户不同t值下的预测精度
		 * ----java cmd.PPS PredictionEvaluator -dataset 1 -lamda 2 -tH 'tH' -tL 'tL'
		 * --------value of tH/tL: 1.5/1.0, 1.2/1.0, 1.0/1.0
		 * 
		 * 5.8个测试用户不同预测算法下的预测精度
		 * ----java cmd.PPS PredictionEvaluator -dataset 2 -lamda 2 -tH 1.2 -tL 1.0
		 * 
		 * 6. 8个用户HMM位置预测
		 * ----java cmd.PPS Hmm
		 */
		//
		System.out.println("=======================================");
		switch (args[0]) {
		case "DataFilterEvaluator":
			//1.
			System.out.println("Computing proportions of useful position points in original trajectories.");
			System.setOut(new PrintStream(new File("./DataFilterEvaluator")));//重定向到文件
			System.out.println("hh");
			System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));//重定向到标准输出
			System.out.println("Done. The result has been stored in the file classes/DataFilterEvaluator");
			break;
		case "RegionCnt":
			//2.
			System.out.println("Computing the total number of regions based on different parameter lamda_max_w.");
			System.setOut(new PrintStream(new File("./RegionCnt")));//重定向到文件
			System.out.println("hh");
			System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));//重定向到标准输出
			System.out.println("Done. The result has been stored in the file classes/RegionCnt");
			break;
		case "PredictionEvaluator":
			//3.4.5
			break;
		case "Hmm":
			//6.
			System.out.println("Predict the next position using HMM algorithm.");
			System.setOut(new PrintStream(new File("./Hmm")));//重定向到文件
			System.out.println("hh");
			System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));//重定向到标准输出
			System.out.println("Done. The result has been stored in the file classes/Hmm");
			break;
		default:
			System.out.println("error parameter.");
			break;
		}
	}

}
