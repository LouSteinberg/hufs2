package hufs2;

import java.util.ArrayList;
import java.util.function.BinaryOperator;

public class Hufs {
	public static final int NUMLEVELS = 3; // number of levels
	public static final BinaryOperator<Double> U0 = (score, tau) -> tau>=0 ? score : 0.0;
//	public static final BinaryOperator<Double> U0 = (score, tau) -> slope(5.0, score, tau);
	public static final int KIDSPERLEVEL = 3;
	public static final double STARTTAU = KIDSPERLEVEL * (NUMLEVELS-1);
	public static final Distribution TOPSCOREDISTRIBUTION = new NormalDistribution(10.0, 2.0);
	public static final Distribution TOPERRORDISTRIBUTION = new NormalDistribution(0.0, 1.0);	
	public static final int TESTREPS = 100;
	public static boolean TRACE = Hufs.TESTREPS < 5;
	
	public static void main(String [ ] args) {
//		testReuseHufs(1);
		testWaterfallHufs(Hufs.TESTREPS);
//		Design.nextId = 0;
//		testHufs(Hufs.TESTREPS);
//		testSlope( );
	}
	public static double slope(double begin, double score, double tau) {
		double utility;
		if (tau > begin) {
			utility = score;
		} else if (tau <= 0.0) {
			utility = 0.0;
		} else {
			utility = score*tau/begin;
		}
		return utility;
	}
	public static void testSlope( ) {
		for (double x = -2.0; x<13; x+=1.0) {
			System.out.format("%f:  %f3.2%n",x, slope(10, 100, x));
		}
	}
	public static void testWaterfallHufs(int repetitions) {
		System.out.println("testWaterfallHufs:");
		Level [ ] levels = Level.initializedLevels(NUMLEVELS);
//		ArrayList<Design> results = new ArrayList<Design>( );
//		ArrayList<Double> scores = new ArrayList<Double>( );
		ArrayList<Double> wfUtilities = new ArrayList<Double>( );
		ArrayList<Double> hufsUtilities = new ArrayList<Double>( );
		for (int r = 0; r < repetitions; r++) {
			Design specs = new Design(levels[levels.length-1], STARTTAU);
			Design wfResult = waterfall(specs, levels, (int) STARTTAU/(NUMLEVELS-1), STARTTAU);
			wfUtilities.add(wfResult.quality);
			specs.clean( );
			Design hufsResult = hufs(specs, levels, STARTTAU);
			hufsUtilities.add(hufsResult.quality);
		}
		ArrayList<Double> differences = new ArrayList<Double>( );
		int hufsBetterCt=0;
		Stats.printMeanStDev(" waterfall:", wfUtilities);
		Stats.printMeanStDev(" hufs:", hufsUtilities);
		int listSize = wfUtilities.size();
		for (int n = 0; n< listSize; n++) {
			differences.add(hufsUtilities.get(n)-wfUtilities.get(n));
			if (differences.get(n) > 0) {
				hufsBetterCt++;
			}
		}
		Stats.printMeanStDev(" differences:", differences);
		System.out.println("count of hufs better: "+((0.0+hufsBetterCt)/listSize));
	 } 
	
	
	public static void testWaterfall(int repetitions) {
		System.out.println("testWaterfall:");
		Level [ ] levels = Level.initializedLevels(NUMLEVELS);
		ArrayList<Design> results = new ArrayList<Design>( );
		ArrayList<Double> scores = new ArrayList<Double>( );
		ArrayList<Double> utilities = new ArrayList<Double>( );
		for (int r = 0; r < repetitions; r++) {
			Design specs = new Design(levels[levels.length-1], STARTTAU);
			Design result = waterfall(specs, levels, (int)STARTTAU/(NUMLEVELS-1), STARTTAU);
			results.add(result);
			scores.add(result.score);
			utilities.add(Hufs.U0.apply(result.score,0.0));
		}
		Stats.printMeanStDev(" scores:", scores);
		Stats.printMeanStDev(" utilities:",utilities);
	}
	public static int numLevel0Kids(Level [ ] levels, int level1Kids, double startTau) {
		int level0Kids = (int) Math.floor((startTau - level1Kids * levels[2].genTime)/levels[1].genTime);
		return level0Kids;
	}
	
	public static Design waterfall(Design specs, Level [ ] levels, int level1Kids, double tau) { 
		Design parent = specs;
		int level0Kids = numLevel0Kids(levels, level1Kids, tau);
		int [ ] levelKids = {level0Kids, level1Kids};  
		for (int levelNum = NUMLEVELS - 1; levelNum > 0; levelNum--) {
			for (int j = 0; j < levelKids[levelNum - 1]; j++) {
				Design child = parent.generate(tau);
				tau -= parent.level.genTime;
				parent.kids.add(child);
			}
			parent = bestByScore(parent.kids);
		}
//		System.out.println("tau "+tau+", score "+parent.score+ ", U0 "+ U0.apply(parent.score, tau));
		return parent;
	}
	
	public static void testReuseHufs(int repetitions) {
		System.out.println("testReuseHufs:");
		Level [ ] levels = Level.initializedLevels(NUMLEVELS);
		Design specs = new Design(levels[levels.length-1], STARTTAU);
		Design result1 = hufs(specs, levels, STARTTAU);
		specs.clean( );
		System.out.println( );
		Design result2 = hufs(specs, levels, STARTTAU);
		System.out.println(result1+" "+result2);
	}
	
	public static void testHufs(int repetitions) {
		System.out.println("testHufs:");
		Level [ ] levels = Level.initializedLevels(NUMLEVELS);
		ArrayList<Design> results = new ArrayList<Design>( );
		ArrayList<Double> scores = new ArrayList<Double>( );
		for (int r = 0; r < repetitions; r++) {
//			System.out.println("x");
			Design specs = new Design(levels[levels.length-1], STARTTAU);
			Design result = hufs(specs, levels, STARTTAU);
			results.add(result);
			scores.add(result.score);
//			System.out.println(r);
		}
		Stats.printMeanStDev(" scores", scores);
	}
	public static Design hufs(Design specs, Level [ ] levels, double tau) {
		ArrayList<Design> allDesigns = new ArrayList<Design>( );
		Design parent = specs;
		allDesigns.add(parent);
		while (! parent.level.isBottomLevel() && tau > 0.0) {
			Design child = parent.generate(tau);
			tau -= parent.level.genTime;
			allDesigns.add(child);
			parent = bestByUtility(allDesigns, tau, U0);
		}
//		System.out.println("tau "+tau+", score "+parent.score+ ", U0 "+ U0.apply(parent.score, tau));
		return parent;}
	public static Design bestByUtility(ArrayList<Design> designs, double tau, BinaryOperator<Double> u0) {
		Design bestDesign = designs.get(0);
		double bestUtility = bestDesign.utility(tau, u0, true);
		traceUtility(bestDesign.id, bestDesign.level.number, tau, bestUtility);
		for (int d = 1; d < designs.size( ); d++) {
			Design nextDesign = designs.get(d);
			double nextUtility = nextDesign.utility(tau, u0, true);
 			traceUtility(nextDesign.id, nextDesign.level.number, tau, nextUtility);
			if (nextUtility > bestUtility) {
				bestUtility = nextUtility;
				bestDesign = nextDesign;
			}
		}
		return bestDesign;
	}
	public static void traceUtility(int id, int levelNum, double tau, double utility) {
		if (Hufs.TRACE) {
			System.out.format("  at %5.1f, utility of design %d level %d, is %8.2f%n", tau, id, levelNum, utility);
		}
	}
		
		public static Design bestByScore(ArrayList<Design> designs) {
		Design bestDesign = designs.get(0);
		double bestscore = bestDesign.score;
		for (int d = 1; d < designs.size( ); d++) {
			Design nextDesign = designs.get(d);
			double nextscore = nextDesign.score;
			if (nextscore > bestscore) {
				bestscore = nextscore;
				bestDesign = nextDesign;
			}
		}
		return bestDesign;
	}
}
