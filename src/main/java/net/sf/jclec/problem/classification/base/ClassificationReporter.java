package net.sf.jclec.problem.classification.base;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.sf.jclec.AlgorithmEvent;
import net.sf.jclec.IAlgorithmListener;
import net.sf.jclec.IConfigure;
import net.sf.jclec.IIndividual;
import net.sf.jclec.fitness.IValueFitness;
import net.sf.jclec.problem.classification.IClassifier;
import net.sf.jclec.problem.classification.IClassifierIndividual;
import net.sf.jclec.problem.util.dataset.IDataset;
import net.sf.jclec.problem.util.dataset.instance.IInstance;
import net.sf.jclec.problem.util.dataset.attribute.IAttribute;
import net.sf.jclec.problem.util.dataset.metadata.IMetadata;
import net.sf.jclec.selector.BettersSelector;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * Listener for classification algorithms.<p/>
 *
 * Reports the status of the population of the algorithm along generations in console or text file.
 *
 * Main methods:
 * The doIterationReport() method reports the population status given a report frequency.
 * The doDataReport() method classifies the train/test datasets and shows the classification errors for each data instance.
 * The doClassificationReport() method is abstract to be implemented by each classifier type, e.g. to show the rule-base.
 *
 * @author Amelia Zafra
 * @author Sebastian Ventura
 * @author Jose M. Luna
 * @author Alberto Cano
 * @author Juan Luis Olmo
 */

public abstract class ClassificationReporter implements IAlgorithmListener, IConfigure
{
	/////////////////////////////////////////////////////////////////
	// --------------------------------------- Serialization constant
	/////////////////////////////////////////////////////////////////

	/** Generated by Eclipse */

	private static final long serialVersionUID = -8548482239030974796L;

	/////////////////////////////////////////////////////////////////
	// --------------------------------------------------- Properties
	/////////////////////////////////////////////////////////////////

	/** Report directory name */

	protected String reportDirName;

	/** Global report name */

	protected String globalReportName;

	/** Report frequency */

	protected  int reportFrequency;

	/** Init and end time */

	protected long initTime, endTime;

	/** Report directory */

	protected File reportDirectory;

	/////////////////////////////////////////////////////////////////
	// ------------------------------------------------- Constructors
	/////////////////////////////////////////////////////////////////

	/**
	 * Constructor
	 */

	public ClassificationReporter()
	{
		super();
	}

	/////////////////////////////////////////////////////////////////
	// ------------------------------- Setting and getting properties
	/////////////////////////////////////////////////////////////////

	/**
	 * Get the report directory name
	 *
	 * @return report directory name
	 */

	public final String getReportDirName()
	{
		return reportDirName;
	}

	/**
	 * Set the report directory name
	 *
	 * @param reportDirName directory name
	 */

	public final void setReportDirName(String reportDirName)
	{
		this.reportDirName = reportDirName;
	}

	/**
	 * Get the global report name
	 *
	 * @return global report name
	 */

	public final String getGlobalReportName()
	{
		return globalReportName;
	}

	/**
	 * Set the global report name
	 *
	 * @param globalReportName report name
	 */

	public final void setGlobalReportName(String globalReportName)
	{
		this.globalReportName = globalReportName;
	}

	/**
	 * Get the report frequency
	 *
	 * @return report frequency
	 */

	public final int getReportFrequency()
	{
		return reportFrequency;
	}

	/**
	 * Set the report frequency
	 *
	 * @param reportFrequency frequency
	 */

	public final void setReportFrequency(int reportFrequency)
	{
		this.reportFrequency = reportFrequency;
	}

	/////////////////////////////////////////////////////////////////
	// -------------------- Implementing IAlgorithmListener interface
	/////////////////////////////////////////////////////////////////

	public void algorithmStarted(AlgorithmEvent event)
	{
		initTime = System.currentTimeMillis();

		Date now = new Date(); // java.util.Date, NOT java.sql.Date or java.sql.Timestamp!
		String date = new SimpleDateFormat("yyyy.MM.dd'_'HH.mm.ss.SS").format(now);

		// Init report directory
		reportDirectory = new File(reportDirName + "_" + date);
		if (! reportDirectory.mkdir()) {
			throw new RuntimeException("Error creating report directory");
		}
		// Do report
		doIterationReport((ClassificationAlgorithm) event.getAlgorithm());
	}

	public void algorithmFinished(AlgorithmEvent event)
	{
		endTime = System.currentTimeMillis();
		doDataReport((ClassificationAlgorithm) event.getAlgorithm());
		doClassificationReport((ClassificationAlgorithm) event.getAlgorithm());
	}

	public void iterationCompleted(AlgorithmEvent event)
	{
		doIterationReport((ClassificationAlgorithm) event.getAlgorithm());
	}

	/////////////////////////////////////////////////////////////////
	// ---------------------------- Implementing IConfigure interface
	/////////////////////////////////////////////////////////////////

	/**
	 * Configuration method.
	 */

	public void configure(Configuration settings)
	{
		// Get report-dir-name
		String reportDirName = settings.getString("report-dir-name", "report");
		// Set reportDirName
		setReportDirName(reportDirName);
		// Get global-report-name
		String globalReportName = settings.getString("global-report-name", "global-report");
		// Set globalReportName
		setGlobalReportName(globalReportName);
		// Get report-frequency
		int reportFrequency = settings.getInt("report-frequency", 1);
		// Set reportFrequency
		setReportFrequency(reportFrequency);
	}

	/////////////////////////////////////////////////////////////////
	// ------------------------- Overwriting java.lang.Object methods
	/////////////////////////////////////////////////////////////////

	public boolean equals(Object other)
	{
		if (other instanceof ClassificationReporter) {
			ClassificationReporter cother = (ClassificationReporter) other;
			EqualsBuilder eb = new EqualsBuilder();
			eb.append(reportDirName, cother.reportDirName);
			eb.append(reportFrequency, cother.reportFrequency);
			return eb.isEquals();
		}
		else
			return false;
	}

	/////////////////////////////////////////////////////////////////
	// -------------------------------------------- Protected methods
	/////////////////////////////////////////////////////////////////

	/**
	 * Make a report with individuals and their fitness for this generation
	 *
	 * @param algorithm Algorithm
	 */
	protected void doIterationReport(ClassificationAlgorithm algorithm)
	{
		// Population individuals
		List<IIndividual> inds = algorithm.getInhabitants();
		// Actual generation
		int generation = algorithm.getGeneration();

		if (generation%reportFrequency == 0)
		{
			String reportFilename = String.format("Iteration_%d_%d.rep", algorithm.getExecution(), generation);

			try {
				// Report file
				File reportFile = new File(reportDirectory, reportFilename);
				// Report writer
				FileWriter reportWriter = null;

				try {
					reportFile.createNewFile();
					reportWriter = new FileWriter (reportFile);
				}
				catch(IOException e3){
					e3.printStackTrace();
				}

				StringBuffer buffer = new StringBuffer();
				BettersSelector b_selector = new BettersSelector();
				b_selector.contextualize(algorithm);

				//Obtains the best individuals
				inds = b_selector.select(inds);

				//Prints individuals
				for(int i=0; i<inds.size(); i++)
				{
					IClassifier ind = ((IClassifierIndividual) inds.get(i)).getPhenotype();
					buffer.append(ind.toString(algorithm.getTrainSet().getMetadata()) + "; Fitness: " + ((IValueFitness) inds.get(i).getFitness()).getValue()+"\n");
				}

				reportWriter.append(buffer.toString());
				reportWriter.close();
			}
			catch (IOException e) {
				throw new RuntimeException("Error writing report file");
			}
		}
	}

	/**
	 * Make a report in train and test for each instance
	 *
	 * @param algorithm Algorithm
	 */
    protected void doDataReport(ClassificationAlgorithm algorithm)
	{
    	// Test report name
		String testReportFilename = "TestDataReport.txt";
		String validationReportFilename = "ValidationDataReport.txt";
		// Train report name
		String trainReportFilename = "TrainDataReport.txt";
		// Test file writer
		FileWriter testFile = null;
		// Train file writer
		FileWriter trainFile = null;
		FileWriter validationFile = null;
		// Classifier
		IClassifier classifier = algorithm.getClassifier();
		// Test Report file
		File testReportFile = new File(reportDirectory, testReportFilename);
		File validationReportFile = new File(reportDirectory, validationReportFilename);
		// Train Report file
		File trainReportFile = new File(reportDirectory, trainReportFilename);

		try {
			testReportFile.createNewFile();
			testFile = new FileWriter (testReportFile);
			trainReportFile.createNewFile();
			trainFile = new FileWriter (trainReportFile);
			validationReportFile.createNewFile();
			validationFile = new FileWriter (validationReportFile);


			printResult(algorithm.getTrainSet(), classifier, trainFile);
			printResult(algorithm.getTestSet(), classifier, testFile);
			printResult(algorithm.getValidationSet(), classifier, validationFile);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * This method shows the instances classified correctly and the failures.
	 *
	 * @param dataset The dataset
	 * @param classifier The classifier
	 * @param file The file to write
	 */
    protected void printResult(IDataset dataset, IClassifier classifier, FileWriter file)
    {
    	IMetadata metadata = dataset.getMetadata();
		int numAttributes = metadata.numberOfAttributes();
		int numInstances = 0;

		double [] predicted = classifier.classify(dataset);

    	try {
    		file.write("DATASET: " + dataset.getName());

    		for (IInstance instance : dataset.getInstances())
        	{
				double value = instance.getValue(metadata.getClassIndex());
				IAttribute attribute = null;
				file.write("\n");

				for(int j=0; j<numAttributes-1; j++)
				{
					attribute = metadata.getAttribute(j);
					file.write(attribute.show(instance.getValue(j)) + ", ");
				}
				attribute = metadata.getAttribute(numAttributes-1);
				file.write(attribute.show(instance.getValue(numAttributes-1)));

				if(predicted[numInstances] == -1)
					file.write("\t Predicted: Unclassified -> FAIL" );
				else if(value != predicted[numInstances])
					file.write("\t Predicted: "+attribute.show(predicted[numInstances]) + " -> FAIL");
				else
					file.write("\t Predicted: "+attribute.show(predicted[numInstances]) + " -> HIT");

				numInstances++;
        	}

			file.close();

		}catch (IOException e)
		{
			e.printStackTrace();
		}
    }

    /**
	 * This method computes the area under the curve
	 *
	 * @param confusionMatrix the confusion matrix
	 * @return the AUC value
	 */
    protected double AUC(int[][] confusionMatrix)
	{
		if(confusionMatrix.length == 2)
		{
			return AUC(confusionMatrix,0,1);
		}
		else
		{
			/** Multi-class AUC **/
			double auc = 0.0;

			for(int i = 0; i < confusionMatrix.length; i++)
				for(int j = 0; j < confusionMatrix.length; j++)
					if(i != j)
						auc += AUC(confusionMatrix,i,j);

			auc = auc / (double) (confusionMatrix.length * (confusionMatrix.length-1));

			return auc;
		}
	}


	protected double OMAE(int [][] confusionMatrix){
		double OMAE_total = 0.0;

		for (int i = 0; i < confusionMatrix.length; i++){

			double OMAE_fila_i = 0.0;
			int num_instancias_i = 0;

			for (int j = 0; j < confusionMatrix[i].length; j++) {
				OMAE_fila_i += Math.abs(i - j)*confusionMatrix[i][j];
				num_instancias_i += confusionMatrix[i][j];
			}

			OMAE_total += OMAE_fila_i / (double) num_instancias_i;

		}

		OMAE_total = OMAE_total / (double) confusionMatrix.length;

		return OMAE_total;
	}

    /**
	 * This method computes the area under the curve for two classes
	 *
	 * @param confusionMatrix the confusion matrix
	 * @param Class1 the class index of the first class
	 * @param Class2 the class index of the second class
	 * @return the AUC value
	 */
    protected double AUC(int[][] confusionMatrix, int Class1, int Class2)
	{
		double auc = 0.0;

		int tp = confusionMatrix[Class1][Class1];
		int fp = confusionMatrix[Class2][Class1];
		int tn = confusionMatrix[Class2][Class2];
		int fn = confusionMatrix[Class1][Class2];

		double tpRate = 1.0, fpRate = 0.0;

		if(tp + fn != 0)
			tpRate = tp / (double) (tp + fn);

		if(fp + tn != 0)
			fpRate = fp / (double) (fp + tn);

		auc = (1.0 + tpRate - fpRate) / 2.0;

		return auc;
	}

    /**
	 * This method computes the cohen's kappa rate
	 *
	 * @param confusionMatrix the confusion matrix
	 * @return the kappa value
	 */
    protected double Kappa(int[][] confusionMatrix)
	{
		int correctedClassified = 0;
		int numberInstancesTotal = 0;
		int[] numberInstances = new int[confusionMatrix.length];
		int[] predictedInstances = new int[confusionMatrix.length];

		for(int i = 0; i < confusionMatrix.length; i++)
		{
			correctedClassified += confusionMatrix[i][i];

			for(int j = 0; j < confusionMatrix.length; j++)
			{
				numberInstances[i] += confusionMatrix[i][j];
				predictedInstances[j] += confusionMatrix[i][j];
			}

			numberInstancesTotal += numberInstances[i];
		}

		double mul = 0;

		for(int i = 0; i < confusionMatrix.length; i++)
			mul += numberInstances[i] * predictedInstances[i];

		if(numberInstancesTotal*numberInstancesTotal - mul  != 0)
			return ((numberInstancesTotal * correctedClassified) - mul) / (double) ((numberInstancesTotal*numberInstancesTotal) - mul);
		else
			return 1.0;
	}

    /**
	 * This method computes the geometric mean
	 *
	 * @param confusionMatrix the confusion matrix
	 * @return the geometric mean
	 */
    protected double GeoMean(int[][] confusionMatrix)
	{
    	int[] numberInstances = new int[confusionMatrix.length];

		for(int i = 0; i < confusionMatrix.length; i++)
		{
			for(int j = 0; j < confusionMatrix.length; j++)
			{
				numberInstances[i] += confusionMatrix[i][j];
			}
		}

		double gm = 1.0;

		for(int i = 0; i < confusionMatrix.length; i++)
			gm *= confusionMatrix[i][i] / (double) numberInstances[i];

		return Math.pow(gm, 1.0 / (double) confusionMatrix.length);
	}

	/**
	 * Make a classifier report in train and test
	 *
	 * @param Algorithm
	 */
    protected abstract void doClassificationReport(ClassificationAlgorithm algorithm);
}
