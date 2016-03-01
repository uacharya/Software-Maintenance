package vsm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * @author Ujjwal Acharya This is the main class that does the whole process of
 *         creating matrix till generating ranked list of methods
 */
public class VSM {

	private LinkedList<Term> l1;
	private ArrayList<String> names;
	private double[][] term_document;
	private int[] maxValue;
	private double[][] idf;
	private double[][] weightedMatrix;
	private double[][] queryVector;
	private HashMap<Integer, ArrayList<Integer>> map;
	private HashMap<Integer, ArrayList<String>> methodsForDynamicImplementation;
	private static final ArrayList<EffectivenessMeasure> FLTS = new ArrayList<>();

	/**
	 * Constructor for the class that initializes the data structures
	 */
	public VSM() {
		this.l1 = new LinkedList<Term>();
		this.names = new ArrayList<String>();
	}

	/**
	 * this is the main class that does all the computation
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		int docNumber = 0;

		long start = System.currentTimeMillis();

		try {

			BufferedReader read = new BufferedReader(
					new FileReader("src/CorpusMethods-jEdit4.3-AfterSplitStopStem.txt"));

			VSM terms = new VSM();
			// to generate execution trace
			terms.findExecutionTrace();

			String x;
			// in order to get all of the unique terms
			while ((x = read.readLine()) != null && x.length() > 0) {
				StringTokenizer token = new StringTokenizer(x, " ");
				while (token.hasMoreTokens()) {
					terms.addUniqueTerms(token.nextToken());
				}
				docNumber++;

			}
			read.close();
			// generates query vector with respect to the unique terms from
			// names list
			int queryLength = terms.generateQueryVector();

			// Initializing the matrix and array dimensions
			terms.term_document = new double[docNumber][terms.l1.size()];
			terms.maxValue = new int[docNumber];
			terms.idf = new double[1][terms.l1.size()];
			terms.weightedMatrix = new double[docNumber][terms.l1.size()];

			BufferedReader frequency = new BufferedReader(
					new FileReader("src/CorpusMethods-jEdit4.3-AfterSplitStopStem.txt"));

			docNumber = 0;
			String y;
			// generating the term-document matrix from the corpus
			while ((y = frequency.readLine()) != null && y.length() > 0) {
				for (int j = 0; j < terms.names.size(); j++) {
					int count = 0;
					StringTokenizer token = new StringTokenizer(y, " ");
					while (token.hasMoreTokens()) {
						if ((token.nextToken()).equalsIgnoreCase(terms.names.get(j))) {
							count++;
							if (count == 1) {
								terms.l1.get(j).increaseCount();
							}

						}
					}

					terms.term_document[docNumber][j] = count;
					// calculates the maximum count value in a document vector
					// to normalize it
					terms.calculateMaxValue(count, docNumber);

				}
				docNumber++;
			}

			frequency.close();

			// normalizing the term document matrix
			for (int i = 0; i < docNumber; i++) {
				for (int j = 0; j < terms.l1.size(); j++) {
					if (terms.term_document[i][j] != 0.0) {
						terms.term_document[i][j] = (double) terms.term_document[i][j] / terms.maxValue[i];
					}
				}
			}
			// creating inverse term-document frequency
			terms.calculateInverseDocumentFrequency(docNumber);

			// Generating the weighted matrix multiplying normalized
			// term-document matrix with inverse term-frequency
			terms.generateWeightedMatrix(docNumber);

			// generates the ranked list of methods based on cosine similarity
			// of query and documents
			terms.generateRankedMatrix(terms.cosineSimilarity(terms.queryVector, terms.weightedMatrix));

			// output new document corpus for dynamic implementation and
			// generate csv file for VSM static comparing it with gold set
			ArrayList<String> methodNamesMapping = terms.generateID(queryLength, "VSM");
			terms.generateDynamicFLTCorpus(methodNamesMapping);
			// generates ranked methods for Dynamic VSM
			terms.generateRankedMethodsForDynamicMethods(methodNamesMapping);
			// generate csv file for VSM Dynamic comparing it with gold set
			terms.generateID(queryLength, "VSM+Dyn");

			// new object for LSI implementation
			VSM LSI = new VSM();
			// create ranked methods from given LSI files for each feature id
			LSI.generateRankedMethodsForLSI(queryLength, docNumber);

			// generate csv file for LSI static comparing it with gold set
			LSI.generateID(queryLength, "LSI");
			// using the same unique methods from execution traces that are in
			// corpus for LSI as well
			LSI.methodsForDynamicImplementation = terms.methodsForDynamicImplementation;
			// generates ranked methods for Dynamic LSI
			LSI.generateRankedMethodsForDynamicMethods(methodNamesMapping);
			// generate csv file for LSI Dynamic comparing it with gold set
			LSI.generateID(queryLength, "LSI+Dyn");

			EffectivenessMeasure pValue = new EffectivenessMeasure();

			int i = 0; // first FLT for comparison
			int j = 1; // second FLT for comparison
			String percent = "%"; // for printing the % sign in output

			// runs the loop until the first FLT to compare is the second last
			// one
			while (i != FLTS.size() - 1) {
				// gets two values for both sides comparisons
				double result = pValue.generatePercentageBetterEffectiveness(FLTS.get(i).getList(),
						FLTS.get(j).getList());
				// printing the output of better effectiveness comparison
				System.out.printf(
						"%s performed better than %s in %.2f %s of cases and %s performed better than %s in %.2f %s of cases",
						FLTS.get(i).getType(), FLTS.get(j).getType(), result * 100, percent, FLTS.get(j).getType(),
						FLTS.get(i).getType(), (100 - (result * 100)), percent);
				System.out.println();
				// increase the FLT until the last one in the list
				if (j != FLTS.size() - 1) {
					j++;
				} // if end of the list is reached get the new FLT to compare
					// every other with
				else if (j == FLTS.size() - 1) {
					i++;
					// initialize the position of second FLT in comparison next
					// to the first FLT
					if (i != j - 1) {
						j = i + 1;
					}
				}

			}

			// System.out.println("p-value for Wilcoxon signed ranked test
			// between VSM and VSM+Dyn is "
			// + pValue.generateWilcoxonBetweenFLTs(FLTS.get(0).getList(),
			// FLTS.get(1).getList()));
			//
			// System.out.println("p-value for Wilcoxon signed ranked test
			// between VSM and LSI is "
			// + pValue.generateWilcoxonBetweenFLTs(FLTS.get(0).getList(),
			// FLTS.get(2).getList()));
			//
			// System.out.println("p-value for Wilcoxon signed ranked tes
			// between VSM and LSI+Dyn is "
			// + pValue.generateWilcoxonBetweenFLTs(FLTS.get(0).getList(),
			// FLTS.get(3).getList()));

			System.out.println("Done!!");
			long end = System.currentTimeMillis();

			System.out.println((double) (end - start) / 60000);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * extract the execution traces and add them to hash map
	 * 
	 */
	private void findExecutionTrace() throws IOException {
		// initializing the hashmap for dynamic VSM
		methodsForDynamicImplementation = new HashMap<Integer, ArrayList<String>>();
		BufferedReader read = new BufferedReader(new FileReader("src/jEdit4.3ListOfFeatureIDs.txt"));
		String x;
		int featureID = 0;
		while ((x = read.readLine()) != null && x.length() > 0) {
			// list of methods for each featureID from execution traces
			ArrayList<String> executionTraces = new ArrayList<String>();
			BufferedReader traceID = new BufferedReader(new FileReader("src/jEdit4.3Traces/" + x + ".trace"));
			String y;
			while ((y = traceID.readLine()) != null && y.length() > 0) {
				if (y.length() > 0 && y.contains("--")) {
					String[] methodAndClass = y.split("--");
					String className = methodAndClass[1].trim().replace('$', '.');
					String methodName = methodAndClass[0].trim().substring(
							methodAndClass[0].trim().lastIndexOf("\t") + 1, methodAndClass[0].trim().length());
					if (methodName.contains("$")) {
						methodName = methodName.substring(0, methodName.indexOf('$'));
					} else if (methodName.equalsIgnoreCase("<init>") || methodName.equalsIgnoreCase("<clinit>")) {
						methodName = className.substring(className.lastIndexOf(".") + 1, className.length());
					}
					// adding all of the unique methods from execution traces
					if (!executionTraces.contains(className + "." + methodName)) {
						executionTraces.add(className + "." + methodName);
					}
				}
			}
			// adding all the unique methods from execution trace for each
			// featureID
			methodsForDynamicImplementation.put(featureID, executionTraces);
			// increment the featureID
			featureID++;

			traceID.close();
		}
		// System.out.println(traces);
		read.close();
	}

	/**
	 * this method matches the traces with corpus and removes the ones not in
	 * trace from corpus for each featureID
	 */
	private void generateDynamicFLTCorpus(ArrayList<String> corpus) {
		HashSet<String> uniqueCorpusMethods = new HashSet<String>();
		// generating unique methods from corpus without parameters for
		// comparison with execution traces
		for (String overLoadedMethods : corpus) {
			String check = overLoadedMethods.substring(0, overLoadedMethods.indexOf('('));

			uniqueCorpusMethods.add(check);
		}
		// checking if the methods in execution traces for each featureID is
		// present in the corpus or not
		for (int i = 0; i < methodsForDynamicImplementation.size(); i++) {
			for (int j = 0; j < methodsForDynamicImplementation.get(i).size(); j++) {
				// if corpus contains the method in traces keep it else remove
				// it
				if (!uniqueCorpusMethods.contains(methodsForDynamicImplementation.get(i).get(j))) {
					methodsForDynamicImplementation.get(i).remove(j);
					j--; // decrementing the counter so that the new string in
							// the same index will be checked as well

				}
			}
		}

	}

	/**
	 * THis method generates the ranked list of methods from execution traces
	 * for each query
	 * 
	 * @param methodNames
	 */
	private void generateRankedMethodsForDynamicMethods(ArrayList<String> methodNames) {

		// loop for checking each ranked list of documents for each query with
		// execution traces for that query
		for (int i = 0; i < map.size(); i++) {
			for (int j = 0; j < map.get(i).size(); j++) {
				// get the method name for the highest rank methodID for each
				// query and so on
				String method = methodNames.get(map.get(i).get(j));
				method = method.substring(0, method.indexOf("("));
				// checking if the ranked method is in execution traces or not
				if (!methodsForDynamicImplementation.get(i).contains(method)) {
					map.get(i).remove(j);
					j--; // decrementing the counter so that the new string in
					// the same index will be checked as well
				}

			}

		}

	}

	/**
	 * Generates the ranked list of methods for all feature id using LSI
	 */
	private void generateRankedMethodsForLSI(int queryLength, int docNumber) throws IOException {
		double[][] similaritiesLSI = new double[queryLength][docNumber];
		// to read the featureIDs first
		BufferedReader read = new BufferedReader(new FileReader("src/jEdit4.3ListOfFeatureIDs.txt"));

		String x;
		int queryID = 0;
		while ((x = read.readLine()) != null && x.length() > 0) {
			BufferedReader LSISimilarites = new BufferedReader(
					new FileReader("src/jEdit4.3LSISimilarities/" + x + ".similaritiesLSI"));
			int docID = 0;
			String y;
			while ((y = LSISimilarites.readLine()) != null && y.length() > 0) {
				String input[] = y.split("\\s+");
				similaritiesLSI[queryID][docID] = Double.parseDouble(input[1]);
				docID++;
			}

			queryID++;

			LSISimilarites.close();
		}

		read.close();

		// sends to next method for sorting

		generateRankedMatrix(similaritiesLSI);

	}

	/**
	 * Generates query vector for each query with same dimension document vector
	 * 
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */

	private int generateQueryVector() throws FileNotFoundException, IOException {
		int num = 0;

		Scanner scan = new Scanner(new FileReader("src/CorpusQueries-jEdit4.3-AfterSplitStopStem.txt"));
		while (scan.hasNextLine()) {
			scan.nextLine();
			num++;
		}

		scan.close();

		queryVector = new double[num][l1.size()];

		BufferedReader read = new BufferedReader(new FileReader("src/CorpusQueries-jEdit4.3-AfterSplitStopStem.txt"));

		num = 0;
		String y;
		while ((y = read.readLine()) != null && y.length() > 0) {
			for (int j = 0; j < names.size(); j++) {
				int count = 0;
				StringTokenizer token = new StringTokenizer(y, " ");
				while (token.hasMoreTokens()) {
					if ((token.nextToken()).equalsIgnoreCase(names.get(j))) {
						count++;
					}
				}
				queryVector[num][j] = (double) count;
			}
			num++;

		}

		read.close();

		return num;

	}

	/**
	 * To add terms name
	 * 
	 * @param x
	 */
	private void addUniqueTerms(String x) {
		if (names.contains(x)) {
			return;
		} else {
			Term t = new Term(x);
			names.add(x);
			l1.add(t);
		}

	}

	/**
	 * Calculates the max value for each vector of document
	 * 
	 * @param x
	 * @param index
	 */
	private void calculateMaxValue(int x, int index) {
		if (maxValue[index] > 0) {
			int temp = maxValue[index];
			if (temp >= x) {
				return;
			} else {
				maxValue[index] = x;
			}
		} else {
			maxValue[index] = x;
		}

	}

	/**
	 * Calculates inverse document frequency
	 * 
	 * @param N
	 */
	private void calculateInverseDocumentFrequency(int N) {
		for (int i = 0; i < l1.size(); i++) {
			idf[0][i] = Math.log((double) N / l1.get(i).getCount());
		}

	}

	/**
	 * Generates weighted matrix
	 * 
	 * @param total
	 */
	private void generateWeightedMatrix(int total) {
		for (int i = 0; i < total; i++) {
			for (int j = 0; j < l1.size(); j++) {
				if (term_document[i][j] != 0 && idf[0][j] != 0) {
					weightedMatrix[i][j] = term_document[i][j] * idf[0][j];
				}
			}
		}

	}

	/**
	 * Generates cosine similarity between queries and documents
	 * 
	 * @param query
	 * @param matrix
	 * @return
	 */
	private double[][] cosineSimilarity(double[][] query, double[][] matrix) {

		double[][] cosineMatrix = new double[query.length][matrix.length];
		// finds the cosine similarity between query matrix represented by term
		// document matrix and document matrix represented by weighted term
		// matrix
		for (int i = 0; i < query.length; i++) {
			int docNumber = 0;
			while (docNumber < matrix.length) {
				double dotProduct = 0.0;
				double magnitude_one = 0.0;
				double magnitude_two = 0.0;
				for (int j = 0; j < matrix[0].length; j++) {
					dotProduct += query[i][j] * matrix[docNumber][j];
					magnitude_one += Math.pow(query[i][j], 2);
					magnitude_two += Math.pow(matrix[docNumber][j], 2);
				}

				magnitude_one = Math.sqrt(magnitude_one);
				magnitude_two = Math.sqrt(magnitude_two);

				if (dotProduct != 0.0 && magnitude_one != 0.0 && magnitude_two != 0.0) {
					cosineMatrix[i][docNumber] = dotProduct / (magnitude_one * magnitude_two);
				}
				docNumber++;
			}

		}

		return cosineMatrix;
	}

	/**
	 * This method creates ranked list of the documents based on cosine
	 * similarity
	 * 
	 * @param cosine
	 */
	private void generateRankedMatrix(double[][] cosine) {
		
		this.map = new HashMap<Integer, ArrayList<Integer>>();

		for (int i = 0; i < cosine.length; i++) {
			ArrayList<double[]> ranking = new ArrayList<double[]>();

			for (int j = 0; j < cosine[0].length; j++) {
				double[] attr = new double[2];
				attr[0] = j;
				attr[1] = cosine[i][j];
				ranking.add(attr);
			}

			// to sort the list of documents based on cosine similarity value
			Collections.sort(ranking, new Comparator<double[]>() {
				public int compare(double[] a, double[] b) {
					return Double.compare(b[1], a[1]);
				}
			});

			// int[] rank = new int[cosine[0].length];
			ArrayList<Integer> rank = new ArrayList<Integer>();
			// getting the document id based on similarity
			for (int x = 0; x < ranking.size(); x++) {
				// rank[x] = (int) ranking.get(x)[0];
				rank.add((int) ranking.get(x)[0]);
			}

			map.put(i, rank);

		}

	}

	/**
	 * this method generates query ID and methodID names for each document
	 * 
	 * @param queryLength
	 * @param docNumber
	 * @throws IOException
	 */
	private ArrayList<String> generateID(int queryLength, String type) throws IOException {
		int[] queryMapping = new int[queryLength];
		ArrayList<String> methodMapping = new ArrayList<String>();

		BufferedReader read = new BufferedReader(new FileReader("src/jEdit4.3ListOfFeatureIDs.txt"));

		String x;
		int index = 0;
		while ((x = read.readLine()) != null && x.length() > 0) {
			queryMapping[index] = Integer.parseInt(x);
			index++;
		}
		read.close();

		BufferedReader methodRead = new BufferedReader(new FileReader("src/CorpusMethods-jEdit4.3.mapping"));

		while ((x = methodRead.readLine()) != null && x.length() > 0) {
			methodMapping.add(x);
		}

		methodRead.close();
		// generating gold set rank for each query from returned ranked results
		generateGoldSetRank(queryMapping, methodMapping, type);

		// return methodMapping;

		if (type.equalsIgnoreCase("VSM") || type.equalsIgnoreCase("LSI")) {
			// returns for generating unique methods from execution traces that
			// appear in corpus as well
			return methodMapping;
		} else {
			return null; // return null when no return value needed
		}
	}

	/**
	 * writing the ranked result in the required csv format as output
	 * 
	 * @param query
	 * @param methodMapping
	 * @throws IOException
	 */
	private void generateGoldSetRank(int[] query, ArrayList<String> methodMapping, String type) throws IOException {
		File csv = new File("src/Effectiveness" + type + ".csv");

		if (!csv.exists()) {
			csv.createNewFile();
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(csv));
		// creating object for a particular FLT
		EffectivenessMeasure dataForWilcoxon = new EffectivenessMeasure(type);
		// adding that object to the list of FLTs
		FLTS.add(dataForWilcoxon);

		bw.write("FeatureID\tGoldSet MethodID Position\tGoldSetMethodID\t" + type
				+ " GoldSetMethodID Rank - All Ranks\t" + type + " GoldSetMethodID Rank - Best Rank\n");

		for (int j = 0; j < query.length; j++) {
			// get the ranked list of methods for each query
			ArrayList<Integer> x = map.get(j);

			int num = query[j];

			// gets methodID of the methods from the goldset from corpus
			ArrayList<Integer> goldSetNum = getGoldSetNumbers(num, methodMapping);

			// generates full method name from gold set
			ArrayList<String> goldSet = getGoldSetID(num);

			// generates the rank of all methods in the gold set in the returned
			// ranked result for each query
			ArrayList<Integer> allRanks = generateAllRanks(goldSetNum, x);
			// generate best rank for all the ranks generated for each query
			int best = generateBestRank(allRanks);
			// adds the best rank for a particular implementation type according
			// to the query identifier
			dataForWilcoxon.getList().add(best);

			// writing every information to the csv file for each FLTs
			if (goldSet.size() > 1) {
				bw.write(num + "\t");
				for (int i = 0; i < goldSet.size(); i++) {
					bw.write(goldSetNum.get(i) + "\t" + goldSet.get(i) + "\t");
					if (allRanks.get(i) != null) {
						bw.write(allRanks.get(i) + "\t");
					} else {
						bw.write("\t");
					}

					if (i > 0 && i < (goldSet.size() - 1)) {
						bw.write("\n \t");
					} else if (i == 0) {
						if (best != 0) {
							bw.write(best + "\n \t");
						} else {
							bw.write("\n \t");
						}
					} else if (i == (goldSet.size() - 1)) {
						bw.write("\n");
					}
				}
			} else {
				bw.write(num + "\t");
				bw.write(goldSetNum.get(0) + "\t" + goldSet.get(0) + "\t");
				if (allRanks.get(0) != null) {
					bw.write(allRanks.get(0) + "\t");
				} else {
					bw.write("\t");
				}
				if (best != 0) {
					bw.write(best + "\n");
				} else {
					bw.write("\n");
				}
			}

		}

		bw.flush();
		bw.close();

	}

	/**
	 * This method generates the best rank from all ranks generated for each
	 * query
	 * 
	 * @param -
	 *            allRanks that gives rank of every methodID present in gold set
	 *            in the returned ranked result by the model
	 * @return-
	 */
	private int generateBestRank(ArrayList<Integer> allRanks) {
		int best = 0;
		ArrayList<Integer> numbers = new ArrayList<Integer>();
		// checks all rank and adds them to sort
		for (int i = 0; i < allRanks.size(); i++) {
			if (allRanks.get(i) != null) {
				numbers.add(allRanks.get(i));
			}
		}
		// sorts all ranks to get the best rank
		if (numbers.size() > 0) {
			Collections.sort(numbers);
			best = numbers.get(0);
		}

		return best;
	}

	/**
	 * This method generates all ranks of returned result for each query
	 * 
	 * @param goldSetNum
	 * @param x
	 * @return
	 */
	private ArrayList<Integer> generateAllRanks(ArrayList<Integer> goldSetNum, ArrayList<Integer> x) {
		ArrayList<Integer> rank = new ArrayList<Integer>();
		// generates rank for each methodID that was present in gold set and
		// also in returned ranked result by the system
		for (int i = 0; i < goldSetNum.size(); i++) {
			int check = goldSetNum.get(i);
			if (check != -1) {
				int counter = 0;
				for (int j = 0; j < x.size(); j++) {
					int output = x.get(j) + 1;
					// add the rank of the methodID in the execution trace
					if (check == output) {
						rank.add(j + 1);
						counter++;
						break;
					}
				} // add null if the methodID is not present in the execution
					// trace
				if (counter == 0) {
					rank.add(null);
				}

			} else {
				rank.add(null);
			}
		}
		return rank;
	}

	/**
	 * This method returns gold set methods for each query from corpus
	 * 
	 * @param num
	 * @return
	 * @throws IOException
	 */
	private ArrayList<String> getGoldSetID(int num) throws IOException {
		String file = "src/jEdit4.3GoldSets/GoldSet" + num + ".txt";

		ArrayList<String> id = new ArrayList<String>();

		BufferedReader read = new BufferedReader(new FileReader(file));
		String x;
		while ((x = read.readLine()) != null) {
			id.add(x);
		}

		read.close();

		return id;

	}

	/**
	 * This method returns gold set method ID for each query that is in original
	 * corpus
	 * 
	 * @param num
	 *            -The featureID of a query
	 * @param methodMapping
	 *            - original corpus for getting methodID
	 * @return - Results with methodID for each method in gold set for each
	 *         query
	 * @throws IOException
	 */
	private ArrayList<Integer> getGoldSetNumbers(int num, ArrayList<String> methodMapping) throws IOException {
		String file = "src/jEdit4.3GoldSets/GoldSet" + num + ".txt";

		BufferedReader read = new BufferedReader(new FileReader(file));

		String x;
		ArrayList<Integer> result = new ArrayList<Integer>();
		// gets method name in gold set for each query and returns it methodID
		// if present in the corpus
		while ((x = read.readLine()) != null && x.length() > 0) {
			if (!methodMapping.contains(x)) {
				result.add(-1);
			} else {
				int temp = methodMapping.indexOf(x);
				result.add(temp + 1);
			}
		}

		read.close();
		return result;
	}

}
