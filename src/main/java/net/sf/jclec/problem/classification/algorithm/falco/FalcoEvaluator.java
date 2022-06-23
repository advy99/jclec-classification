package net.sf.jclec.problem.classification.algorithm.falco;

import java.util.Comparator;

import net.sf.jclec.IConfigure;
import net.sf.jclec.IFitness;
import net.sf.jclec.IIndividual;
import net.sf.jclec.base.AbstractParallelEvaluator;
import net.sf.jclec.fitness.SimpleValueFitness;
import net.sf.jclec.fitness.ValueFitnessComparator;
import net.sf.jclec.problem.classification.base.Rule;
import net.sf.jclec.problem.classification.syntaxtree.SyntaxTreeRuleIndividual;
import net.sf.jclec.problem.util.dataset.IDataset;
import net.sf.jclec.problem.util.dataset.instance.IInstance;
import net.sf.jclec.problem.util.dataset.metadata.IMetadata;

import org.apache.commons.configuration.Configuration;

/**
 * Evaluator for Falco et al. 2002 - Discovering interesting classification rules with genetic programming<p/>
 *
 * The fitness function evaluates the number of prediction errors for the class of the current algorithm' execution.
 * However, it does not take into account data class distribution. Thus, it is expected to perform worse on imbalanced data.
 * Finally, the fitness is weighted as regards of the length of the rule (number of nodes).
 * Therefore, the evolutionary process is biased to evolve simpler and more comprehensible rules with less absolute errors.
 *
 *
 * @author Amelia Zafra
 * @author Sebastian Ventura
 * @author Jose M. Luna
 * @author Alberto Cano
 * @author Juan Luis Olmo
 */

public class FalcoEvaluator extends AbstractParallelEvaluator implements IConfigure
{
	/////////////////////////////////////////////////////////////////
	// --------------------------------------- Serialization constant
	/////////////////////////////////////////////////////////////////

	/** Generated by Eclipse */

	private static final long serialVersionUID = 3613350191235561000L;

	/////////////////////////////////////////////////////////////////
	// --------------------------------------------------- Properties
	/////////////////////////////////////////////////////////////////

	/** Train Dataset */

	protected IDataset dataset;

	/** Maximize the fitness function */

	private boolean maximize = false;

	/** Take into the simplicity of the rule */

	protected double alpha;

	/** Classified class */

	protected int classifiedClass;

	/** Fitness comparator */

	protected transient ValueFitnessComparator comparator = new ValueFitnessComparator(!maximize);

	/////////////////////////////////////////////////////////////////
	// ------------------------------------------------- Constructors
	/////////////////////////////////////////////////////////////////

	/**
	 * Empty constructor.
	 */

	public FalcoEvaluator()
	{
		super();
	}

	/////////////////////////////////////////////////////////////////
	// ------------------------------- Getting and setting properties
	/////////////////////////////////////////////////////////////////

	/**
	 * Get the dataset
	 *
	 * @return dataset
	 */

	public IDataset getDataset()
	{
		return this.dataset;
	}

	/**
	 * Set the dataset
	 *
	 * @param dataset the dataset
	 */

	public void setDataset(IDataset dataset)
	{
		this.dataset = dataset;

	}

	/**
	 * Get the alpha parameter
	 *
	 * @return alpha
	 */
	public double getAlpha()
	{
		return alpha;
	}

	/**
	 * Set the alpha paramater
	 *
	 * @param alpha alpha parameter
	 */
	public void setAlpha(double alpha)
	{
		this.alpha = alpha;
	}

	/**
	 * Get the actual classified class
	 *
	 * @return class
	 */
	public int getClassifiedClass()
	{
		return classifiedClass;
	}

	/**
	 * Set the actual classified class
	 *
	 * @param classifiedClass class
	 */
	public void setClassifiedClass(int classifiedClass)
	{
		this.classifiedClass = classifiedClass;
	}

	/////////////////////////////////////////////////////////////////
	// ------------------------------- Implementing IConfigure method
	/////////////////////////////////////////////////////////////////

	/**
	 * Configuration method
	 *
	 * @param settings Configuration settings
	 */

	public void configure(Configuration settings) {
		double alpha = settings.getDouble("alpha");
		setAlpha(alpha);
	}

	/////////////////////////////////////////////////////////////////
	// ------------------------ Overwriting AbstractEvaluator methods
	/////////////////////////////////////////////////////////////////

	/**
	 * Evaluates the individual and compute it fitness
	 *
	 * @param individual Individual to evaluate
	 */

	protected void evaluate(IIndividual individual)
	{
		Rule rule = (Rule) ((SyntaxTreeRuleIndividual) individual).getPhenotype();

		int fails = 0;

		IMetadata metadata = getDataset().getMetadata();

		double valorOMAE = 0.0;
		int num_instancias_cubiertas = 0;

		int numClasses = 10;

		//Calculate the confussion matrix
		for(IInstance instance : dataset.getInstances())
		{

			double value = instance.getValue(metadata.getClassIndex());


			if((Boolean) rule.covers(instance))
			{
				valorOMAE += Math.abs(classifiedClass - value);
				num_instancias_cubiertas++;

				if(value != classifiedClass)
					fails++;
			}
			else
			{
				if(value == classifiedClass) {
					fails++;
					// si no la cubro y debería, sumo en cuanto me he equivocado
					valorOMAE += numClasses - Math.abs(classifiedClass - value);
					// num_instancias_cubiertas++;
				}

			}

		}

		if (num_instancias_cubiertas == 0) {
			valorOMAE = Double.POSITIVE_INFINITY;
		} else {
			valorOMAE = valorOMAE / (double) num_instancias_cubiertas;
		}

		int depth = ((SyntaxTreeRuleIndividual) individual).getGenotype().derivSize();
		int numNodes = rule.getAntecedent().size();

		int fs = depth + numNodes;

		//Compute the fitness
		// individual.setFitness(new SimpleValueFitness(2*fails + getAlpha()*fs));

		individual.setFitness(new SimpleValueFitness(valorOMAE));

	}

	/**
	 * {@inheritDoc}
	 */

	public Comparator<IFitness> getComparator()
	{
		return comparator;
	}
}
