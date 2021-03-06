package hufs2;

import java.util.ArrayList;
import java.util.function.BinaryOperator;
//import java.util.function.Function;
import java.lang.IllegalArgumentException;

public class Design {

	public static int nextId = 0;  // id to be given to next Design generated

	Level level;
	double quality;
	Design parent; 
	ArrayList<Design> kids = new ArrayList<Design>(); 
	ArrayList<Design> cachedKids = new ArrayList<Design>();
	int cachedKidsUsed; // number of cached kids that have been reused so far in this run, also index of next cached kid to use
	Distribution childScoreDistribution;
	Distribution childErrorDistribution;
	double score;
	double error;
	int id;
	double tauCreated;
	
	public String toString( ) {
		return String.format("Design=%d;level=%d;parent= %d;sc=%1.1f", id, level.number, parent.id, score);
	}
	
	// reuse an existing Design object, or create a new one if can't reuse
	public Design generate(double tau) {
		Design parent = this;
		if (parent.cachedKidsUsed > parent.cachedKids.size( )) { // more used than exist: impossible
			throw new IllegalArgumentException( );	
		}
		Design newChild;
		if (parent.cachedKidsUsed >= parent.cachedKids.size()) {  // if have used all cached kids
			newChild = new Design(this, tau);
			parent.cachedKids.add(newChild);
		} else {  // use next cached child
			newChild = parent.cachedKids.get(parent.cachedKidsUsed); 
			newChild.clean( );			
			parent.kids.add(newChild);
			traceReuse(newChild, tau);
		}
		parent.cachedKidsUsed ++;  // we have either reused a child or created, cached, and used a new one
		return newChild;
	}
	
	public void clean( ) { // restore design to just-initialized state.  does NOT forget cached kids, but restarts their use
		this.kids = new ArrayList<Design>( );
		this.cachedKidsUsed = 0;
	}
	// create a non-top-level Design object
	public Design(Design parent, double tau) {
		Design child = this;
		child.parent = parent;
		parent.kids.add(child);
		child.level = parent.level.levelDown;
		child.score = parent.childScoreDistribution.draw( );
		if (child.level.isBottomLevel()) {
			child.error = 0;
		} else {
			child.error = parent.childErrorDistribution.draw( );	
		}
		initialize(child, tau-parent.level.genTime);
	}
	// create a top-level Design object
	public Design(Level level,double tau) {
		this.parent = null;
		this.level = level;
		this.score = level.topScoreDistribution.draw( );
		this.error = level.topErrorDistribution.draw( );
		initialize(this, tau);
	}
	// initialize does the part of the initialization of a newly created Design object
	// that is common between top-level and non-top-level designs.
	public static void initialize(Design newDesign, double tau){
		newDesign.id = nextId;
		nextId++;
		newDesign.quality = newDesign.score - newDesign.error;
		//newDesign.score = newDesign.level.scoreFromQuality(newDesign.quality);

		double cSDMean = newDesign.level.cSDMeanB + newDesign.level.cSDMeanM * newDesign.quality;
		double cSDStDev = newDesign.level.cSDStDev;
		newDesign.childScoreDistribution =  new NormalDistribution(cSDMean, cSDStDev);				

		double cEDMean = newDesign.level.cEDMeanB + newDesign.level.cEDMeanM * newDesign.quality;
		double cEDStDev = newDesign.level.cEDStDev;
		newDesign.childErrorDistribution =  new NormalDistribution(cEDMean, cEDStDev);				

		newDesign.tauCreated = tau;
		
		traceCreation(newDesign, tau);
	}
	public static void traceCreation(Design design, double tau) {
		if (Hufs.TRACE) {
			if (design.level.isTopLevel()) {
				System.out.format("top level design created, id: %d", design.id);
			} else {
				System.out.format("Design created. id: %d, parent %d", design.id, design.parent.id);
			}
			System.out.format(", level: %d, score: %8.1f, utility at %5.2f = %8.2f%n",
					design.level.number, design.score, tau, design.utility(tau,Hufs.U0, false));
		}
	}
	public static void traceReuse(Design design, double tau) {
		if (Hufs.TRACE) {
			if (design.level.isTopLevel()) {
				System.out.format("top level design reused, id: %d", design.id);
			} else {
				System.out.format("Design reused. id: %d, parent %d", design.id, design.parent.id);
			}
			System.out.format(", level: %d, score: %8.1f, utility at %5.2f = %8.2f%n",
					design.level.number, design.score, tau, design.utility(tau,Hufs.U0, false));
		}
	}
	// utility of having this design (with score this.score) starting at time tau with given U_0
	public double utility(double tau, BinaryOperator<Double> u0,boolean trace) {
//		double childDoneTime = tau - this.level.genTime;
		if ((tau == 1) && this.level.number == 1) {
			tau = 1; // for debugging		
		}
		double utility =  level.utility(this.score, tau, u0);
//		System.out.format("tau:  %3.1f, level: %d%n", tau, level.number);
		return utility;
	}
	public void printKids(boolean cached) {
		for (Design d : (cached?this.kids:this.cachedKids)){
			System.out.println(d);
		}
	}
	public static void main(String [ ] args) {
		Level [ ] levels = Level.initializedLevels(Hufs.NUMLEVELS);
		Design specs = new Design(levels[levels.length-1], Hufs.STARTTAU);
		specs.generate(0);
		specs.generate(0);
		specs.printKids(false) ;
		specs.clean();
		specs.generate(0);
		specs.generate(0);
		specs.generate(0);
		specs.printKids(false );
					
	}
}