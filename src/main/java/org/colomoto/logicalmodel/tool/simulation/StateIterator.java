package org.colomoto.logicalmodel.tool.simulation;

public interface StateIterator {
    public void continueOnThisNode ();
    public boolean hasNext();
    public byte[] next();
}
