package org.colomoto.logicalmodel.perturbation;

import org.colomoto.logicalmodel.LogicalModel;
import org.colomoto.logicalmodel.NodeInfo;

/**
 * Simple perturbation to fix the value of a component.
 * 
 * @author Aurelien Naldi
 */
public class FixedValuePerturbation extends AbstractPerturbation {

	private final int value;
	private final NodeInfo component;

	/**
	 * Create a simple KO perturbation.
	 * 
	 * @param target the blocked component.
	 */
	public FixedValuePerturbation(NodeInfo target) {
		this(target, 0);
	}
	
	/**
	 * Create a simple perturbation to fix the value of a component.
	 * 
	 * @param target the blocked component
	 * @param value the fixed value
	 */
	public FixedValuePerturbation(NodeInfo target, int value) {
		if (value < 0 || value > target.getMax()) {
			throw new RuntimeException("Invalid perturbation settings for "+target+": "+value);
		}
		this.component = target;
		this.value = value;
	}
	
	@Override
	public void update(LogicalModel model) {
		int idx = -1;
		int[] functions = null;
		
		idx = model.getNodeOrder().indexOf(component);
		if (idx >= 0) {
			functions = model.getLogicalFunctions();
		} else {
			idx = model.getExtraComponents().indexOf(component);
			if (idx >= 0) {
				functions = model.getExtraLogicalFunctions();
			}
		}
		
		if (idx < 0) {
			throw new RuntimeException("Perturbation.update(): Could not find the target component");
		}
		
		int oldValue = functions[idx];
		functions[idx] = value;
		
		model.getMDDManager().free(oldValue);
	}
}