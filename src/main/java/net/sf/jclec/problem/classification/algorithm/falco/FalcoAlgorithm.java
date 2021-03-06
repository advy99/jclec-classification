package net.sf.jclec.problem.classification.algorithm.falco;

import java.util.ArrayList;
import java.util.List;

import net.sf.jclec.IConfigure;
import net.sf.jclec.IIndividual;
import net.sf.jclec.IMutator;
import net.sf.jclec.IRecombinator;
import net.sf.jclec.ISelector;
import net.sf.jclec.base.FilteredMutator;
import net.sf.jclec.base.FilteredRecombinator;
import net.sf.jclec.problem.classification.base.ClassificationAlgorithm;
import net.sf.jclec.problem.classification.base.Rule;
import net.sf.jclec.problem.classification.crisprule.CrispRuleBase;
import net.sf.jclec.problem.classification.syntaxtree.SyntaxTreeRuleIndividual;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationRuntimeException;
import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * Classifier algorithm for Falco et al. 2002 - Discovering interesting classification rules with genetic programming<p/>
 *
 * The Falco et al. algorithm performs reproduction, recombination and mutation.
 * Its execution is repeated as many times as the number of data classes.
 * At each execution, a population of rules is evolved for a particular data class.
 * When evolution is finished, the best individual is included in the classification rule base (one rule per class).
 *
 * The configure() method set ups the algorithm according to the parameters from the configuration file.
 * The doSelection() method selects the parents from the current population via tournament selection.
 * The doGeneration() method applies the reproduction, recombination and mutation operators and evaluates the fitness of the offspring.
 * The doUpdate() method selects the best individuals from the current population and the offspring for the next generation.
 * The doControl() method defines the stop criterion that is the maximum number of generations, and controls the execution for each data class.
 *
 * @author Amelia Zafra
 * @author Sebastian Ventura
 * @author Jose M. Luna
 * @author Alberto Cano
 * @author Juan Luis Olmo
 */

public class FalcoAlgorithm extends ClassificationAlgorithm
{
	// ///////////////////////////////////////////////////////////////
	// --------------------------------------- Serialization constant
	// ///////////////////////////////////////////////////////////////

	/** Generated by Eclipse */

	private static final long serialVersionUID = -8711970425735016406L;

	// ///////////////////////////////////////////////////////////////
	// --------------------------------------------------- Properties
	// ///////////////////////////////////////////////////////////////

	/** Parents selector */

	protected ISelector parentsSelector;

	/** Individuals recombinator */

	protected FilteredRecombinator recombinator;

	/** Mutation operator */

	protected FilteredMutator mutator;

	/** Copy probability */

	double copyProb;

	// ///////////////////////////////////////////////////////////////
	// ------------------------------------------------- Constructors
	// ///////////////////////////////////////////////////////////////

	/**
	 * Empty (default) constructor
	 */

	public FalcoAlgorithm() {
		super();
	}

	// ///////////////////////////////////////////////////////////////
	// ------------------------------------------------ Public methods
	// ///////////////////////////////////////////////////////////////

	/**
	 * Get the parents selector
	 *
	 * @return parents selector
	 */

	public ISelector getParentsSelector() {
		return parentsSelector;
	}

	/**
	 * Set the parents selector
	 *
	 * @param parentsSelector the parents selector
	 */

	public void setParentsSelector(ISelector parentsSelector) {
		// Set parents selector
		this.parentsSelector = parentsSelector;
		// Contextualize selector
		parentsSelector.contextualize(this);
	}

	/**
	 * Access to parents recombinator
	 *
	 * @return Actual parents recombinator
	 */

	public FilteredRecombinator getRecombinator() {
		return recombinator;
	}

	/**
	 * Sets the parents recombinator.
	 *
	 * @param recombinator the parents recombinator
	 */

	public void setRecombinator(IRecombinator recombinator) {
		if(this.recombinator == null)
			this.recombinator = new FilteredRecombinator(this);

		this.recombinator.setDecorated(recombinator);
	}

	/**
	 * Access to individuals mutator.
	 *
	 * @return Individuals mutator
	 */

	public FilteredMutator getMutator() {
		return mutator;
	}

	/**
	 * Set individuals mutator.
	 *
	 * @param mutator the individuals mutator
	 */

	public void setMutator(IMutator mutator) {
		if(this.mutator == null)
			this.mutator = new FilteredMutator(this);

		this.mutator.setDecorated(mutator);
	}

	/**
	 * Access to "copyProb" property.
	 *
	 * @return Current copy probability
	 */

	public double getCopyProb() {
		return copyProb;
	}

	/**
	 * Set the "copyProb" property.
	 *
	 * @param copyProb the copy probability
	 */

	public void setCopyProb(double copyProb) {
		this.copyProb = copyProb;
	}

	// ///////////////////////////////////////////////////////////////
	// ---------------------------- Implementing IConfigure interface
	// ///////////////////////////////////////////////////////////////

	/**
	 * Configuration method.
	 *
	 * @param settings the configuration settings
	 */

	public void configure(Configuration settings)
	{
		settings.addProperty("species[@type]", "net.sf.jclec.problem.classification.algorithm.falco.FalcoSyntaxTreeSpecies");
		settings.addProperty("evaluator[@type]", "net.sf.jclec.problem.classification.algorithm.falco.FalcoEvaluator");
		settings.addProperty("evaluator.alpha", settings.getDouble("alpha",0.9));
		settings.addProperty("provider[@type]", "net.sf.jclec.syntaxtree.SyntaxTreeCreator");
		settings.addProperty("parents-selector[@type]", "net.sf.jclec.selector.TournamentSelector");

		settings.addProperty("recombinator[@type]", "net.sf.jclec.syntaxtree.SyntaxTreeRecombinator");
		settings.addProperty("recombinator[@rec-prob]", settings.getDouble("recombination-prob",0.8));
		settings.addProperty("recombinator.base-op[@type]", "net.sf.jclec.syntaxtree.rec.SelectiveCrossover");

		settings.addProperty("mutator[@type]", "net.sf.jclec.syntaxtree.SyntaxTreeMutator");
		settings.addProperty("mutator[@mut-prob]", settings.getDouble("mutation-prob",0.1));
		settings.addProperty("mutator.base-op[@type]", "net.sf.jclec.problem.classification.algorithm.falco.FalcoMutator");

		// Call super.configure() method
		super.configure(settings);

		classifier = new CrispRuleBase();

		// Establishes the metadata for the species
		((FalcoSyntaxTreeSpecies) species).setMetadata(getTrainSet().getMetadata());

		((FalcoEvaluator) evaluator).setDataset(getTrainSet());

		String fitness = settings.getString("fitness");
		((FalcoEvaluator) evaluator).setFitness(fitness);


		// Parents selector
		setParentsSelectorSettings(settings);

		// Recombinator
		setRecombinatorSettings(settings);

		// Mutator
		setMutatorSettings(settings);

		//Get max-tree-depth
		int maxDerivSize = settings.getInt("max-deriv-size");
		((FalcoSyntaxTreeSpecies) species).setGrammar();
		((FalcoSyntaxTreeSpecies) species).setMaxDerivSize(maxDerivSize);

		// Set copy probability
		double copyProb = settings.getDouble("copy-prob",0.1);
		setCopyProb(copyProb);
	}

	////////////////////////////////////////////////////////////////
	// --------------------------------------------- Private methods
	////////////////////////////////////////////////////////////////

	/**
	 * Set the mutator settings
	 *
	 * @param settings the configuration settings
	 */

	@SuppressWarnings("unchecked")
	private void setMutatorSettings(Configuration settings) {
		try {
			// Mutator classname
			String mutatorClassname =
				settings.getString("mutator[@type]");
			// Mutator class
			Class<? extends IMutator> mutatorClass =
				(Class<? extends IMutator>) Class.forName(mutatorClassname);
			// Mutator instance
			IMutator mutator = mutatorClass.newInstance();
			// Configure mutator if necessary
			if (mutator instanceof IConfigure) {
				// Extract mutator configuration
				Configuration mutatorConfiguration = settings.subset("mutator");
				// Configure mutator
				((IConfigure) mutator).configure(mutatorConfiguration);
			}
			// Set mutator
			setMutator(mutator);
		}
		catch (ClassNotFoundException e) {
			throw new ConfigurationRuntimeException("Illegal mutator classname");
		}
		catch (InstantiationException e) {
			throw new ConfigurationRuntimeException("Problems creating an instance of mutator", e);
		}
		catch (IllegalAccessException e) {
			throw new ConfigurationRuntimeException("Problems creating an instance of mutator", e);
		}
		// Mutation probability
		double mutProb = settings.getDouble("mutator[@mut-prob]",0.05);
		setMutationProb(mutProb);
	}

	/**
	 * Set the recombinator settings
	 *
	 * @param settings the configuration settings
	 */
	@SuppressWarnings("unchecked")
	private void setRecombinatorSettings(Configuration settings) {
		try {
			// Recombinator classname
			String recombinatorClassname =
				settings.getString("recombinator[@type]");
			// Recombinator class
			Class<? extends IRecombinator> recombinatorClass =
				(Class<? extends IRecombinator>) Class.forName(recombinatorClassname);
			// Recombinator instance
			IRecombinator recombinator = recombinatorClass.newInstance();
			// Configure recombinator if necessary
			if (recombinator instanceof IConfigure) {
				// Extract recombinator configuration
				Configuration recombinatorConfiguration = settings.subset("recombinator");
				// Configure species
				((IConfigure) recombinator).configure(recombinatorConfiguration);
			}
			// Set species
			setRecombinator(recombinator);
		}
		catch (ClassNotFoundException e) {
			throw new ConfigurationRuntimeException("Illegal recombinator classname");
		}
		catch (InstantiationException e) {
			throw new ConfigurationRuntimeException("Problems creating an instance of recombinator", e);
		}
		catch (IllegalAccessException e) {
			throw new ConfigurationRuntimeException("Problems creating an instance of recombinator", e);
		}
		// Recombination probability
		double recProb = settings.getDouble("recombinator[@rec-prob]",0.7);
		setRecombinationProb(recProb);
	}

	/**
	 * Set the parents selector settings
	 *
	 * @param settings the configuration settings
	 */
	@SuppressWarnings("unchecked")
	private void setParentsSelectorSettings(Configuration settings) {
		try {
			// Selector classname
			String parentsSelectorClassname =
				settings.getString("parents-selector[@type]");
			// Species class
			Class<? extends ISelector> parentsSelectorClass =
				(Class<? extends ISelector>) Class.forName(parentsSelectorClassname);
			// Species instance
			ISelector parentsSelector = parentsSelectorClass.newInstance();

			// Configure species if necessary
			if (parentsSelector instanceof IConfigure) {
				// Extract species configuration
				Configuration parentsSelectorConfiguration = settings.subset("parents-selector");
				// Configure species
				((IConfigure) parentsSelector).configure(parentsSelectorConfiguration);
			}

			// Set species
			setParentsSelector(parentsSelector);
		}
		catch (ClassNotFoundException e) {
			throw new ConfigurationRuntimeException("Illegal parents selector classname");
		}
		catch (InstantiationException e) {
			throw new ConfigurationRuntimeException("Problems creating an instance of parents selector", e);
		}
		catch (IllegalAccessException e) {
			throw new ConfigurationRuntimeException("Problems creating an instance of parents selector", e);
		}
	}

	/**
	 * Set the recombinator probability
	 *
	 * @param recProb the recombination probability
	 */

	private void setRecombinationProb(double recProb)
	{
		((FilteredRecombinator) this.recombinator).setRecProb(recProb);
	}

	/**
	 * Set the mutator probability
	 *
	 * @param mutProb the mutation probability
	 */

	private void setMutationProb(double mutProb)
	{
		((FilteredMutator) this.mutator).setMutProb(mutProb);
	}

	// ///////////////////////////////////////////////////////////////
	// ------------------------- Overwriting java.lang.Object methods
	// ///////////////////////////////////////////////////////////////

	public boolean equals(Object other) {
		if (other instanceof FalcoAlgorithm) {
			FalcoAlgorithm cother = (FalcoAlgorithm) other;
			EqualsBuilder eb = new EqualsBuilder();
			// Call super method
			eb.appendSuper(super.equals(other));
			// Parents selector
			eb.append(parentsSelector, cother.parentsSelector);
			// Mutator
			eb.append(mutator, cother.mutator);
			// Recombinator
			eb.append(recombinator, cother.recombinator);
			// Return test result
			return eb.isEquals();
		} else {
			return false;
		}
	}

	// ///////////////////////////////////////////////////////////////
	// ---------------------------- Overwriting BaseAlgorithm methods
	// ///////////////////////////////////////////////////////////////

	@Override
	protected void doSelection() {
		pset = parentsSelector.select(bset, populationSize);
	}

	@Override
	protected void doGeneration() {

		// Recombine parents
		rset = recombinator.recombine(pset);
		// Add non-recombined inds
		rset.addAll(recombinator.getSterile());
		// Mutate filtered inds
		List<IIndividual> mset = new ArrayList<IIndividual>();
		mset = mutator.mutate(rset);
		// Add non-mutated inds
		mset.addAll(mutator.getSterile());

		evaluator.evaluate(mset);

		cset = mset;

		// Do the copy
		for (IIndividual ind : bset)
			if (randgen.coin(copyProb))
				cset.add(ind.copy());
	}

	@Override
	protected void doReplacement() {
	}

	@Override
	protected void doUpdate() {

		// Select the best individuals
		if (cset.size() > populationSize)
			bset = bettersSelector.select(cset, populationSize);
		else
			bset = bettersSelector.select(cset);

		// Clears parents and offsprings
		cset = pset = rset = null;
	}

	protected void doControl() {

		// Sets the rule consequent to the current execution
		for(IIndividual ind : bset)
			((SyntaxTreeRuleIndividual) ind).getPhenotype().setConsequent(execution);

		// If maximum number of generations is exceeded, evolution is finished
		if (generation >= maxOfGenerations)
		{
			execution++;

			// Select the best classifier for this class
			Rule rule = (Rule) ((SyntaxTreeRuleIndividual) bset.get(0)).getPhenotype();
			rule.setFitness(bset.get(0).getFitness());

			CrispRuleBase classifier = (CrispRuleBase) this.classifier;

			boolean added = false;

			// Add the rule according to it fitness
			if(classifier.getClassificationRules() != null)
			for(int i = 0; i < classifier.getClassificationRules().size(); i++)
			{
				if(getEvaluator().getComparator().compare(classifier.getClassificationRule(i).getFitness(),rule.getFitness()) <= 0)
				{
					(classifier).addClassificationRule(i, rule);
					added = true;
					break;
				}
			}

			if(!added)
				classifier.addClassificationRule(rule);

			// If all classes have been covered then finish
			if (execution == getTrainSet().getMetadata().numberOfClasses())
			{
				state = FINISHED;
				return;
			}
			else
			{
				// Execute the algorithm with other class
				((FalcoEvaluator) evaluator).setClassifiedClass(execution);
				generation = 0;
				doInit();
			}
		}
	}
}
