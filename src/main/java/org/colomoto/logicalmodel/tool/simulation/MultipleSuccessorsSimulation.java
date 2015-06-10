package org.colomoto.logicalmodel.tool.simulation;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;;

/**
 * A simple simulation engine for updaters with a single successor.
 * It will stop when reaching a stable state, but has otherwise no memory and will
 * not detect cycles, just stop after reaching a limit on the number of iterations.
 *
 * @author Aurelien Naldi
 */
public class MultipleSuccessorsSimulation implements Simulation {

    private final MultipleSuccessorsUpdater updater;
    private final byte[] init;

    private final int max_steps;

    public MultipleSuccessorsSimulation(MultipleSuccessorsUpdater updater, byte[] init, int max_steps) {
        this.updater = updater;
        this.init = init;
        this.max_steps = max_steps;
    }

    @Override
    public StateIterator iterator() {
        return new StateIteratorAsync(init, updater, max_steps);
    }
}

class StateIteratorAsync implements StateIterator {

    private byte[] state;
    private final MultipleSuccessorsUpdater updater;
    private int steps;
    
	protected LinkedList<byte[]> queue; // exploration queue


    public StateIteratorAsync(byte[] state, MultipleSuccessorsUpdater updater, int max_steps) {
        this.state = state;
        this.updater = updater;
        this.steps = max_steps;
        
        queue = new LinkedList<byte[]>();
        queue.add (state);

    }
    
    public void continueOnThisNode () {
    	queue.addAll(updater.getSuccessors(state));
    }
    

    @Override
    public boolean hasNext() {
        return state != null;
    }

    @Override
    public byte[] next() {
    	try {
    		state = queue.pop ();
    	} catch (NoSuchElementException e) {
    		state = null;
    	}
    	return state;
    }
}