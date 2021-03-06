package net.sf.jclec.problem.classification.crisprule;

import java.util.ArrayList;
import java.util.List;

import net.sf.jclec.problem.classification.base.Rule;
import net.sf.jclec.problem.classification.base.RuleBase;
import net.sf.jclec.problem.util.dataset.IDataset;
import net.sf.jclec.problem.util.dataset.instance.IInstance;
import net.sf.jclec.problem.util.dataset.metadata.IMetadata;

/**
 * Implements a crisp rule-base for classification problems.<p/>
 * 
 * The rule-base implements a classifier which comprises a set of crisp classification rules.
 * 
 * Main methods:
 *    The classify() method classifies a particular instance or a complete dataset and returns the class predictions for the instances.
 *    It may return the default class prediction in case that any of the rules covers an instance. 
 * 
 * @author Sebastian Ventura
 * @author Amelia Zafra
 * @author Jose M. Luna 
 * @author Alberto Cano 
 * @author Juan Luis Olmo
 */

public class CrispRuleBase extends RuleBase
{	
	/////////////////////////////////////////////////////////////////
	// --------------------------------------- Serialization constant
	/////////////////////////////////////////////////////////////////

	/** Generated by Eclipse */

	private static final long serialVersionUID = 1L;
	
	/////////////////////////////////////////////////////////////////
	// ------------------------------------------------- Constructors
	/////////////////////////////////////////////////////////////////

	/**
	 * Empty constructor
	 */
	
	public CrispRuleBase() {
		super();
	}
	
	/**
	 * Constructor
	 * 
	 * @param defaultC default class
	 */
	
	public CrispRuleBase(double defaultC) {
		super(defaultC);
	}
	
	/**
	 * Constructor
	 * 
	 * @param rules list of rules
	 * @param defaultC default class
	 */
	
	public CrispRuleBase(List<Rule> rules, double defaultC) {
		super(rules,defaultC);
	}
	
	/////////////////////////////////////////////////////////////////
	// ----------------------------------------------- Public methods
	/////////////////////////////////////////////////////////////////
	
	/**
	 * Implementation of copy()
	 * 
	 * {@inheritDoc}
	 */
	
	@Override
	public CrispRuleBase copy()
	{
		ArrayList<Rule> newRules = new ArrayList<Rule>();
		
		for(Rule rule : rules)
			newRules.add(rule.copy());
		
		return new CrispRuleBase(newRules, defaultClass);
	}
	
	/**
	 * Return the estimated values by the classifier to the dataset
	 * 
	 * @param dataset the dataset
	 * @return array of class predictions
	 */
	
	public double[] classify(IDataset dataset) 
	{
		/** Get the dataset instances */
		ArrayList<IInstance> instances = dataset.getInstances();
		
        int numInstances = instances.size();
        int numRules = rules.size();
        double [] result = new double[numInstances];
		boolean cover = false;
        
        for(int i=0; i<numInstances; i++)
        {
        	for(int j=0; j<numRules; j++)
			{	            		
				if((Boolean) rules.get(j).covers(instances.get(i)))
				{
					result[i] = rules.get(j).getConsequent();
					cover = true;
					break;
				}
			}
		
			if(cover == false)
				result[i] = defaultClass;
			
			cover = false;
        }
    	return result;
	}

	/**
	 *  Return the estimated value to the instance
	 *  
	 *  @return the predicted class
	 */
	
	public double classify(IInstance instance) 
	{	
		for(int i = 0; i < rules.size(); i++)
			if((Boolean) rules.get(i).covers(instance))
				return rules.get(i).getConsequent();
		
		return defaultClass;
	}
	
	@Override
	public String toString(IMetadata metadata)
	{
		String result = new String();
		
		int numRules = rules.size();
		
		if(numRules > 0)
		{
			result = " 1 Rule: " + rules.get(0).toString(metadata) + "\n";	
	
			for (int i = 1; i < numRules; i++)
			   result += " " + (i+1) + " Rule: ELSE " + rules.get(i).toString(metadata) + "\n";
	
			result += " " + (numRules+1) + " Rule: ELSE ("+metadata.getAttribute(metadata.getClassIndex()).getName()+" = "+ metadata.getAttribute(metadata.getClassIndex()).show(defaultClass)+")\n";
		}
		
		return result;
	}
}