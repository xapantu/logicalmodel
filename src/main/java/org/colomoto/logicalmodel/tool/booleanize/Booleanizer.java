package org.colomoto.logicalmodel.tool.booleanize;

import org.colomoto.logicalmodel.LogicalModel;
import org.colomoto.logicalmodel.LogicalModelImpl;
import org.colomoto.logicalmodel.NodeInfo;
import org.colomoto.mddlib.MDDManager;
import org.colomoto.mddlib.MDDOperator;
import org.colomoto.mddlib.MDDVariable;
import org.colomoto.mddlib.internal.MDDStoreImpl;
import org.colomoto.mddlib.operators.MDDBaseOperators;

import java.util.*;

/**
 * Construct a Boolean version of a multi-valued model.
 * If the model has no multi-valued component, it will be preserved.
 * Otherwise, each multivalued component will be replaced by multiple Boolean components.
 *
 * It implements the Van Ham Boolean mapping to ensure the construction of a compatible model.
 *
 * @author Aurelien Naldi
 */
public class Booleanizer {

    public static LogicalModel booleanize( LogicalModel ori) {

        if ( ori.isBoolean()) {
            return ori;
        }

        Booleanizer bool = new Booleanizer( ori );
        return bool.getModel();
    }

    private final MDDManager ddm, newDDM;
    private final List<NodeInfo> core, extra, newCore, newExtra;
    private final int[] coreFunctions, extraFunctions, newCoreFunctions, newExtraFunctions;
    private final Map<String,NodeInfo[]> mv2bool = new HashMap<String, NodeInfo[]>();


    public Booleanizer(LogicalModel ori) {
        this.ddm = ori.getMDDManager();
        this.core = ori.getNodeOrder();
        this.extra = ori.getExtraComponents();
        this.coreFunctions = ori.getLogicalFunctions();
        this.extraFunctions = ori.getExtraLogicalFunctions();

        this.newCore = getBoolComponents(core);
        this.newExtra = getBoolComponents(extra);

        List<Object> variables = getBoolVariables(ddm);
        this.newDDM = new MDDStoreImpl(variables, ddm.getLeafCount());

        this.newCoreFunctions = new int[ newCore.size() ];
        this.newExtraFunctions = new int[ newExtra.size() ];

        transformFunctions(core, coreFunctions, newCoreFunctions);
        transformFunctions(extra, extraFunctions, newExtraFunctions);
    }

    private List<Object> getBoolVariables( MDDManager ddm) {

        // TODO: retrieve the proper order also if the manager is a proxy
        MDDVariable[] variables = ddm.getAllVariables();
        List<Object> boolVariables = new ArrayList<Object>();

        for (MDDVariable var: variables) {
            if (var.nbval < 3) {
                boolVariables.add( var.key);
            } else {
                NodeInfo[] mapped = getMapped(var.key.toString(), var.nbval);
                for (NodeInfo ni: mapped) {
                    boolVariables.add( ni);
                }
            }
        }

        return boolVariables;
    }


    private NodeInfo[] getMapped( String key, int nbval) {
        if (nbval < 3) {
            return null;
        }

        NodeInfo[] mapped = mv2bool.get(key);
        if (mapped != null) {
            return mapped;
        }

        mapped = new NodeInfo[nbval-1];
        mv2bool.put( key, mapped);
        for (int v=1 ; v<nbval ; v++) {
            mapped[v-1] = new NodeInfo(key+"_b"+v);
        }
        return mapped;
    }

    private List<NodeInfo> getBoolComponents( List<NodeInfo> nodes) {

        List<NodeInfo> newComponents = new ArrayList<NodeInfo>();
        for (NodeInfo ni: nodes) {
            int max = ni.getMax();
            if (max > 1) {
                String name = ni.getNodeID();
                NodeInfo[] mapped = getMapped(name, max+1);
                for (NodeInfo bnode: mapped) {
                    newComponents.add(bnode);
                }
            } else {
                newComponents.add(ni);
            }
        }

        return newComponents;
    }


    private void transformFunctions( List<NodeInfo> nodes, int[] srcFunctions, int[] targetFunctions) {

        int s = 0;
        int t = 0;
        for (NodeInfo ni: nodes) {
            NodeInfo[] bnodes = mv2bool.get(ni.getNodeID());
            int f = srcFunctions[s++];

            if (bnodes == null) {
                targetFunctions[t++] = transform(f, 1);
            } else {
                for (int i=0 ; i<bnodes.length ; i++) {
                    int bf = transform(f, i+1);
                    if (i>0) {transform(f, i+1);
                        // a subvariable can not be activated if the previous one is not active
                        MDDVariable prevVar = newDDM.getVariableForKey( bnodes[i-1]);
                        int prev = prevVar.getNode(0,1);
                        bf = MDDBaseOperators.AND.combine(newDDM, bf, prev);
                    }
                    if (i < bnodes.length-1) {
                        // a subvariable can not be disabled if the next one is active
                        MDDVariable nextVar = newDDM.getVariableForKey( bnodes[i+1]);
                        int next = nextVar.getNode(0,1);
                        bf = MDDBaseOperators.OR.combine(newDDM, bf, next);
                    }
                    targetFunctions[t++] = bf;
                }
            }
        }
    }


    /**
     * Take a Boolean function and transfer it into the new MDDManager,
     * replacing the multi-valued regulators if any.
     *
     * @param f MDD in the original MDDManager
     * @return a new MDD in the Boolean MDDManager
     */
    public int transform(int f, int v) {

        if (ddm.isleaf(f)) {
            if (f >= v) {
                return 1;
            }

            return 0;
        }

        MDDVariable var = ddm.getNodeVariable(f);
        if (var.nbval == 2) {
            MDDVariable newVar = newDDM.getVariableForKey( var.key );
            if (newVar == null) {
                throw new RuntimeException("No matching variable during Boolean conversion");
            }

            int c0 = transform( ddm.getChild(f, 0), v );
            int c1 = transform( ddm.getChild(f, 1), v );
            return newVar.getNodeFree(c0, c1);
        }

        // The current variable is multivalued: replace it by the Boolean placeholders
        NodeInfo[] newVars = mv2bool.get( var.key.toString());
        if (newVars == null) {
            throw new RuntimeException("No mapped bool vars found");
        }
        int[] values = ddm.getChildren(f);
        int[] newValues = new int[ values.length ];
        for (int i=0 ; i<values.length ; i++) {
            newValues[i] = transform(values[i], v);
        }
        int cur = newValues[ newValues.length-1 ];
        for (int i= newVars.length-1 ; i>=0 ; i--) {
            int prev = newValues[i];
            MDDVariable bvar = newDDM.getVariableForKey( newVars[i]);
            cur = bvar.getNodeFree(prev, cur);
        }

        return cur;
    }


    public LogicalModel getModel() {
        return new LogicalModelImpl( newDDM, newCore, newCoreFunctions, newExtra, newExtraFunctions);
    }
}
