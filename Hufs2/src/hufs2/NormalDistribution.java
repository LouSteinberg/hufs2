package hufs2;

import java.util.Random;
import java.util.ArrayList;

public class NormalDistribution extends Distribution {

	public Random fRandom = new Random();
	
	public NormalDistribution(double mean, double stDev){
		this.mean = mean;
		this.stDev = stDev;
		this.sup = mean + 4*stDev;
		this.sub = mean - 4*stDev;
		double variance = stDev * stDev;
		this.pdf = x -> Math.exp(-(x-mean)*(x-mean)/(2*variance))/Math.sqrt(2*Math.PI*variance);
	}	
	public double draw( ) {
		return mean + fRandom.nextGaussian() * stDev;
	}	
	
	public static void main(String [ ] args) {
		NormalDistribution dist = new NormalDistribution(5.0, 1.0);
		NormalDistribution diste = new NormalDistribution(0.0, 2.0);
		double ct = 10000;
		int sig1=0;
		int sig2=0;
		int sig3=0;
		ArrayList<Double> nums = new ArrayList<Double>( );
		for (int j = 0; j<ct; j++) {
			double drawn = dist.draw( );
			double drawne = diste.draw( );
			nums.add(drawn);
			drawn += drawne;
			// System.out.format("%g\n",drawn);
			double err = drawn-4.96;
			if (Math.abs(err)< 1.0*2.237) {
				sig1=sig1+1;
			}
			if (Math.abs(err)< 2.0*2.237) {
				sig2=sig2+1;
			}
			if (Math.abs(err)< 3.0*2.237) {
				sig3=sig3+1;
			}
		}
//		double f1 = dist.pdf.apply(5.0);
//		double f2 = dist.pdf.apply(6.0);
		double halfInt = Integral.integrate(dist.pdf, 5.0, 10.0, 100);
		System.out.println("half integral "+halfInt);
		double mean = Stats.mean(nums);
		double stDev = Stats.stDev(nums);
		System.out.println("ct: "+ct);
		System.out.println("mean: "+mean);
		System.out.println("stDev: "+stDev);
		System.out.println("sig1: "+sig1/ct);
		System.out.println("sig2: "+sig2/ct);
		System.out.println("sig3: "+sig3/ct);
	}
}
