/*
 * Copyright 2019-2021 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file incorporates code covered by the following terms:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.plan.volcano;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.plan.AlgOptListener;
import org.polypheny.db.plan.AlgOptRuleCall;
import org.polypheny.db.plan.AlgOptRuleOperand;
import org.polypheny.db.plan.AlgTraitPropagationVisitor;
import org.polypheny.db.plan.AlgTraitSet;


/**
 * <code>VolcanoRuleCall</code> implements the {@link AlgOptRuleCall} interface for VolcanoPlanner.
 */
public class VolcanoRuleCall extends AlgOptRuleCall {

    protected final VolcanoPlanner volcanoPlanner;

    /**
     * List of {@link AlgNode} generated by this call. For debugging purposes.
     */
    private List<AlgNode> generatedRelList;


    /**
     * Creates a rule call, internal, with array to hold bindings.
     *
     * @param planner Planner
     * @param operand First operand of the rule
     * @param algs Array which will hold the matched relational expressions
     * @param nodeInputs For each node which matched with {@code matchAnyChildren} = true, a list of the node's inputs
     */
    protected VolcanoRuleCall( VolcanoPlanner planner, AlgOptRuleOperand operand, AlgNode[] algs, Map<AlgNode, List<AlgNode>> nodeInputs ) {
        super( planner, operand, algs, nodeInputs );
        this.volcanoPlanner = planner;
    }


    /**
     * Creates a rule call.
     *
     * @param planner Planner
     * @param operand First operand of the rule
     */
    VolcanoRuleCall( VolcanoPlanner planner, AlgOptRuleOperand operand ) {
        this( planner, operand, new AlgNode[operand.getRule().operands.size()], ImmutableMap.of() );
    }


    // implement RelOptRuleCall
    @Override
    public void transformTo( AlgNode alg, Map<AlgNode, AlgNode> equiv ) {
        if ( LOGGER.isDebugEnabled() ) {
            LOGGER.debug( "Transform to: rel#{} via {}{}", alg.getId(), getRule(), equiv.isEmpty() ? "" : " with equivalences " + equiv );
            if ( generatedRelList != null ) {
                generatedRelList.add( alg );
            }
        }
        try {
            // It's possible that alg is a subset or is already registered. Is there still a point in continuing? Yes, because we might discover that two sets of
            // expressions are actually equivalent.

            // Make sure traits that the new alg doesn't know about are propagated.
            AlgTraitSet algs0Traits = algs[0].getTraitSet();
            new AlgTraitPropagationVisitor( getPlanner(), algs0Traits ).go( alg );

            if ( LOGGER.isTraceEnabled() ) {
                // Cannot call AlgNode.toString() yet, because alg has not been registered. For now, let's make up something similar.
                String algDesc = "rel#" + alg.getId() + ":" + alg.getAlgTypeName();
                LOGGER.trace( "call#{}: Rule {} arguments {} created {}", id, getRule(), Arrays.toString( algs ), algDesc );
            }

            if ( volcanoPlanner.listener != null ) {
                AlgOptListener.RuleProductionEvent event = new AlgOptListener.RuleProductionEvent( volcanoPlanner, alg, this, true );
                volcanoPlanner.listener.ruleProductionSucceeded( event );
            }

            // Registering the root relational expression implicitly registers its descendants. Register any explicit equivalences first, so we don't register twice and cause churn.
            for ( Map.Entry<AlgNode, AlgNode> entry : equiv.entrySet() ) {
                volcanoPlanner.ensureRegistered( entry.getKey(), entry.getValue(), this );
            }
            volcanoPlanner.ensureRegistered( alg, algs[0], this );
            algs[0].getCluster().invalidateMetadataQuery();

            if ( volcanoPlanner.listener != null ) {
                AlgOptListener.RuleProductionEvent event = new AlgOptListener.RuleProductionEvent( volcanoPlanner, alg, this, false );
                volcanoPlanner.listener.ruleProductionSucceeded( event );
            }
        } catch ( Exception e ) {
            throw new RuntimeException( "Error occurred while applying rule " + getRule(), e );
        }
    }


    /**
     * Called when all operands have matched.
     */
    protected void onMatch() {
        assert getRule().matches( this );
        volcanoPlanner.checkCancel();
        try {
            if ( volcanoPlanner.isRuleExcluded( getRule() ) ) {
                LOGGER.debug( "Rule [{}] not fired due to exclusion filter", getRule() );
                return;
            }

            for ( int i = 0; i < algs.length; i++ ) {
                AlgNode alg = algs[i];
                AlgSubset subset = volcanoPlanner.getSubset( alg );

                if ( subset == null ) {
                    LOGGER.debug( "Rule [{}] not fired because operand #{} ({}) has no subset", getRule(), i, alg );
                    return;
                }

                if ( subset.set.equivalentSet != null ) {
                    LOGGER.debug( "Rule [{}] not fired because operand #{} ({}) belongs to obsolete set", getRule(), i, alg );
                    return;
                }

                final Double importance = volcanoPlanner.algImportances.get( alg );
                if ( (importance != null) && (importance == 0d) ) {
                    LOGGER.debug( "Rule [{}] not fired because operand #{} ({}) has importance=0", getRule(), i, alg );
                    return;
                }
            }

            if ( LOGGER.isDebugEnabled() ) {
                LOGGER.debug( "call#{}: Apply rule [{}] to {}", id, getRule(), Arrays.toString( algs ) );
            }

            if ( volcanoPlanner.listener != null ) {
                AlgOptListener.RuleAttemptedEvent event = new AlgOptListener.RuleAttemptedEvent( volcanoPlanner, algs[0], this, true );
                volcanoPlanner.listener.ruleAttempted( event );
            }

            if ( LOGGER.isDebugEnabled() ) {
                this.generatedRelList = new ArrayList<>();
            }

            getRule().onMatch( this );

            if ( LOGGER.isDebugEnabled() ) {
                if ( generatedRelList.isEmpty() ) {
                    LOGGER.debug( "call#{} generated 0 successors.", id );
                } else {
                    LOGGER.debug( "call#{} generated {} successors: {}", id, generatedRelList.size(), generatedRelList );
                }
                this.generatedRelList = null;
            }

            if ( volcanoPlanner.listener != null ) {
                AlgOptListener.RuleAttemptedEvent event = new AlgOptListener.RuleAttemptedEvent( volcanoPlanner, algs[0], this, false );
                volcanoPlanner.listener.ruleAttempted( event );
            }
        } catch ( Exception e ) {
            throw new RuntimeException( "Error while applying rule " + getRule() + ", args " + Arrays.toString( algs ), e );
        }
    }


    /**
     * Applies this rule, with a given relational expression in the first slot.
     */
    void match( AlgNode alg ) {
        assert getOperand0().matches( alg ) : "precondition";
        final int solve = 0;
        int operandOrdinal = getOperand0().solveOrder[solve];
        this.algs[operandOrdinal] = alg;
        matchRecurse( solve + 1 );
    }


    /**
     * Recursively matches operands above a given solve order.
     *
     * @param solve Solve order of operand (&gt; 0 and &le; the operand count)
     */
    private void matchRecurse( int solve ) {
        assert solve > 0;
        assert solve <= rule.operands.size();
        final List<AlgOptRuleOperand> operands = getRule().operands;
        if ( solve == operands.size() ) {
            // We have matched all operands. Now ask the rule whether it matches; this gives the rule chance to apply side-conditions.
            // If the side-conditions are satisfied, we have a match.
            if ( getRule().matches( this ) ) {
                onMatch();
            }
        } else {
            final int operandOrdinal = operand0.solveOrder[solve];
            final int previousOperandOrdinal = operand0.solveOrder[solve - 1];
            boolean ascending = operandOrdinal < previousOperandOrdinal;
            final AlgOptRuleOperand previousOperand = operands.get( previousOperandOrdinal );
            final AlgOptRuleOperand operand = operands.get( operandOrdinal );
            final AlgNode previous = algs[previousOperandOrdinal];

            final AlgOptRuleOperand parentOperand;
            final Collection<? extends AlgNode> successors;
            if ( ascending ) {
                assert previousOperand.getParent() == operand;
                parentOperand = operand;
                final AlgSubset subset = volcanoPlanner.getSubset( previous );
                successors = subset.getParentRels();
            } else {
                parentOperand = previousOperand;
                final int parentOrdinal = operand.getParent().ordinalInRule;
                final AlgNode parentRel = algs[parentOrdinal];
                final List<AlgNode> inputs = parentRel.getInputs();
                if ( operand.ordinalInParent < inputs.size() ) {
                    final AlgSubset subset = (AlgSubset) inputs.get( operand.ordinalInParent );
                    if ( operand.getMatchedClass() == AlgSubset.class ) {
                        successors = subset.set.subsets;
                    } else {
                        successors = subset.getAlgList();
                    }
                } else {
                    // The operand expects parentRel to have a certain number of inputs and it does not.
                    successors = ImmutableList.of();
                }
            }

            for ( AlgNode alg : successors ) {
                if ( !operand.matches( alg ) ) {
                    continue;
                }
                if ( ascending ) {
                    // We know that the previous operand was *a* child of its parent, but now check that it is the *correct* child.
                    final AlgSubset input = (AlgSubset) alg.getInput( previousOperand.ordinalInParent );
                    List<AlgNode> inputRels = input.set.getRelsFromAllSubsets();
                    if ( !inputRels.contains( previous ) ) {
                        continue;
                    }
                }

                // Assign "childRels" if the operand is UNORDERED.
                switch ( parentOperand.childPolicy ) {
                    case UNORDERED:
                        if ( ascending ) {
                            final List<AlgNode> inputs = Lists.newArrayList( alg.getInputs() );
                            inputs.set( previousOperand.ordinalInParent, previous );
                            setChildRels( alg, inputs );
                        } else {
                            List<AlgNode> inputs = getChildRels( previous );
                            if ( inputs == null ) {
                                inputs = Lists.newArrayList( previous.getInputs() );
                            }
                            inputs.set( operand.ordinalInParent, alg );
                            setChildRels( previous, inputs );
                        }
                }

                algs[operandOrdinal] = alg;
                matchRecurse( solve + 1 );
            }
        }
    }

}
