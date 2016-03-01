package vsm;

/**
 * @author Ujjwal Acharya
 * Term class is used to store each unique term in the corpus with its term frequency
 */

public class Term {
	private String name;
	private int termFrequency;

	/**
	 * @param name that is name of the term
	 * Initializes object with the name and term frequency as zero
	 */
	public Term(String name) {
		this.name = name;
		this.termFrequency = 0;
	}

	/**
	 * increases term frequency for each term
	 */
	public void increaseCount() {
		termFrequency++;
	}

	/**
	 * @return returns term frequency of each term
	 */
	public int getCount() {
		return termFrequency;
	}

	/**
	 * @return gives name of each term
	 */
	public String getName() {
		return name;
	}

}
