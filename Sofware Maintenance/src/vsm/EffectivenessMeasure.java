package vsm;

import java.util.ArrayList;

/**
 * @author Ujjwal Acharya
 * This class was made so that generate p values and percentage better effectiveness but couldn't find 
 * a good API for wilcoxon and so this class basically only gives the percentage better effectiveness
 */
public class EffectivenessMeasure {
	private ArrayList<Integer> bestRanks; // Array list of all the best for a
											// particular implementation
	private String identifier; // identifier for the type of implementation

	/**
	 * default constructor just for generating results
	 */
	public EffectivenessMeasure() {
	}

	/**
	 * overloaded constructor for creating an object with type
	 * 
	 * @param type
	 *            is the name of the FLT
	 */
	public EffectivenessMeasure(String type) {
		identifier = type;
		bestRanks = new ArrayList<Integer>();
	}

	/**
	 * This method returns the list of the ranks that have been added
	 * 
	 * @return Array list of all best ranks for a particular implementation
	 */
	public ArrayList<Integer> getList() {
		return bestRanks;
	}

	/**
	 * This method returns the name of the object
	 * 
	 * @return String that is the name of identifier
	 */
	public String getType() {
		return identifier;
	}

	/**
	 * This method the percentage of time first method is better than second one
	 * 
	 * @param first
	 *            list of best methods for first FLT
	 * @param second
	 *            list of best methods for second FLT
	 * @return
	 */
	public double generatePercentageBetterEffectiveness(final ArrayList<Integer> first,
			final ArrayList<Integer> second) {

		int total = 0; // total time of fair comparisons
		int firstBeingBetter = 0; // times first FLT better than second
		// loops and checks number of times when first FLT is better than second
		for (int i = 0; i < first.size(); i++) {
			if (first.get(i) != 0 && second.get(i) != 0 && first.get(i) != second.get(i)) {
				total++;
				if (first.get(i) < second.get(i)) {
					firstBeingBetter++;
				}
			}
		}

		return (double) firstBeingBetter / total;
	}

	/**
	 * This method generates the p value calculated by wilcoxon signed ranked
	 * test between two samples
	 * 
	 * @param first
	 *            sample data for comparison
	 * @param second
	 *            sample data for comparison
	 * @return p-value calculated according to the given parameters and sample
	 *         datas
	 */
	public double generateWilcoxonBetweenFLTs(final ArrayList<Integer> first, final ArrayList<Integer> second) {

		double[] sample1 = new double[first.size()];
		double[] sample2 = new double[second.size()];
		// generating new sample for using wilcoxon signed method from API
		for (int i = 0; i < sample1.length; i++) {
			sample1[i] = first.get(i);
			sample2[i] = second.get(i);
		}

		return 0.0;

	}

}
