/******************************************************************
 * File:        FBLPRuleInfGraph.java
 * Created by:  Dave Reynolds
 * Created on:  26-Jul-2003
 * 
 * (c) Copyright 2003, Hewlett-Packard Development Company, LP
 * [See end of file]
 * $Id: FBLPRuleInfGraph.java,v 1.3 2003-12-08 10:48:27 andy_seaborne Exp $
 *****************************************************************/
package com.hp.hpl.jena.reasoner.rulesys.impl.oldCode;

import com.hp.hpl.jena.reasoner.rulesys.*;
import com.hp.hpl.jena.reasoner.rulesys.impl.*;
import com.hp.hpl.jena.reasoner.*;
import com.hp.hpl.jena.graph.*;

import java.util.*;

import com.hp.hpl.jena.util.OneToManyMap;
import com.hp.hpl.jena.util.iterator.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An inference graph that uses a mixture of forward and backward
 * chaining rules. This is a temporary harness for testing the LP engine.
 * When that works the content of this class will be folded into FBRuleInfGraph
 * and this one will disappear
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.3 $ on $Date: 2003-12-08 10:48:27 $
 */
public class FBLPRuleInfGraph  extends FBRuleInfGraph {
    
    /** The core backward rule engine which includes all the memoized results */
    protected LPBRuleEngine lpbEngine;
    
    static Log logger = LogFactory.getLog(FBLPRuleInfGraph.class);

//  =======================================================================
//  Constructors

    /**
     * Constructor.
     * @param reasoner the reasoner which created this inf graph instance
     * @param schema the (optional) schema graph to be included
     */
    public FBLPRuleInfGraph(Reasoner reasoner, Graph schema) {
        super(reasoner, schema);
        initLP(schema);
    }

    /**
     * Constructor.
     * @param reasoner the reasoner which created this inf graph instance
     * @param rules the rules to process
     * @param schema the (optional) schema graph to be included
     */
    public FBLPRuleInfGraph(Reasoner reasoner, List rules, Graph schema) {
        super(reasoner, rules, schema);
        initLP(schema);
    }

    /**
     * Constructor.
     * @param reasoner the reasoner which created this inf graph instance
     * @param rules the rules to process
     * @param schema the (optional) schema graph to be included
     * @param data the data graph to be processed
     */
    public FBLPRuleInfGraph(Reasoner reasoner, List rules, Graph schema, Graph data) {
        super(reasoner, rules, schema, data);
        initLP(schema);
    }

    /**
     * Initialize the LP engine, based on an optional schema graph.
     */    
    private void initLP(Graph schema) {
        if (schema != null && schema instanceof FBLPRuleInfGraph) {
            LPRuleStore newStore = new LPRuleStore();
            newStore.addAll(((FBLPRuleInfGraph)schema).lpbEngine.getRuleStore());
            lpbEngine = new LPBRuleEngine(this, newStore);
        } else {
            lpbEngine = new LPBRuleEngine(this);
        }
    }
    
//  =======================================================================
//   Interface between infGraph and the goal processing machinery

               
    /**
     * Process a call to a builtin predicate
     * @param clause the Functor representing the call
     * @param env the BindingEnvironment for this call
     * @param rule the rule which is invoking this call
     * @return true if the predicate succeeds
     */
    public boolean processBuiltin(Object clause, Rule rule, BindingEnvironment env) {
        throw new ReasonerException("Internal error in FBLP rule engine, incorrect invocation of building in rule " + rule); 
    }
    
    /**
     * Adds a new Backward rule as a rusult of a forward rule process. Only some
     * infgraphs support this.
     */
    public void addBRule(Rule brule) {
//        logger.debug("Adding rule " + brule);
        lpbEngine.addRule(brule);
        lpbEngine.reset();
    }
       
    /**
     * Deletes a new Backward rule as a rules of a forward rule process. Only some
     * infgraphs support this.
     */
    public void deleteBRule(Rule brule) {
//        logger.debug("Deleting rule " + brule);
        lpbEngine.deleteRule(brule);
        lpbEngine.reset();
    }
    
    /**
     * Adds a set of new Backward rules
     */
    public void addBRules(List rules) {
        for (Iterator i = rules.iterator(); i.hasNext(); ) {
            Rule rule = (Rule)i.next();
//            logger.debug("Adding rule " + rule);
            lpbEngine.addRule(rule);
        }
        lpbEngine.reset();
    }
    
    /**
     * Return an ordered list of all registered backward rules. Includes those
     * generated by forward productions.
     */
    public List getBRules() {
        return lpbEngine.getAllRules();
    }
       
    /**
     * Set a predicate to be tabled/memoized by the LP engine. 
     */
    public void setTabled(Node predicate) {
        lpbEngine.tablePredicate(predicate);
        if (traceOn) {
            logger.info("LP TABLE " + predicate);
        }
    }
    
//  =======================================================================
//  Core inf graph methods
    
    /**
     * Cause the inference graph to reconsult the underlying graph to take
     * into account changes. Normally changes are made through the InfGraph's add and
     * remove calls are will be handled appropriately. However, in some cases changes
     * are made "behind the InfGraph's back" and this forces a full reconsult of
     * the changed data. 
     */
    public void rebind() {
        if (lpbEngine != null) lpbEngine.reset();
        isPrepared = false;
    }
    
    /**
     * Set the state of the trace flag. If set to true then rule firings
     * are logged out to the Log at "INFO" level.
     */
    public void setTraceOn(boolean state) {
        super.setTraceOn(state);
        lpbEngine.setTraceOn(state);
    }

    /**
     * Set to true to enable derivation caching
     */
    public void setDerivationLogging(boolean recordDerivations) {
        this.recordDerivations = recordDerivations;
        engine.setDerivationLogging(recordDerivations);
        lpbEngine.setDerivationLogging(recordDerivations);
        if (recordDerivations) {
            derivations = new OneToManyMap();
        } else {
            derivations = null;
        }
    }
   
    /**
     * Return the number of rules fired since this rule engine instance
     * was created and initialized
     */
    public long getNRulesFired() {
        return engine.getNRulesFired();
    }
    
    /**
     * Extended find interface used in situations where the implementator
     * may or may not be able to answer the complete query. It will
     * attempt to answer the pattern but if its answers are not known
     * to be complete then it will also pass the request on to the nested
     * Finder to append more results.
     * @param pattern a TriplePattern to be matched against the data
     * @param continuation either a Finder or a normal Graph which
     * will be asked for additional match results if the implementor
     * may not have completely satisfied the query.
     */
    public ExtendedIterator findWithContinuation(TriplePattern pattern, Finder continuation) {
        checkOpen();
//        System.out.println("FBLP find called on: " + pattern); 
        if (!isPrepared) prepare();
        ExtendedIterator result = new UniqueExtendedIterator(lpbEngine.find(pattern));
        if (continuation != null) {
            result = result.andThen(continuation.find(pattern));
        }
        return result.filterDrop(Functor.acceptFilter);
    }
   
    /**
     * Flush out all cached results. Future queries have to start from scratch.
     */
    public void reset() {
        lpbEngine.reset();
        isPrepared = false;
    }

    /**
     * Add one triple to the data graph, run any rules triggered by
     * the new data item, recursively adding any generated triples.
     */
    public synchronized void performAdd(Triple t) {
        fdata.getGraph().add(t);
        if (useTGCCaching) {
            if (transitiveEngine.add(t)) isPrepared = false;
        }
        if (isPrepared) {
            engine.add(t);
        }
        lpbEngine.reset();
    }

    /** 
     * Removes the triple t (if possible) from the set belonging to this graph. 
     */   
    public void performDelete(Triple t) {
        fdata.getGraph().delete(t);
        if (useTGCCaching) {
            if (transitiveEngine.delete(t)) {
                if (isPrepared) {
                    bEngine.deleteAllRules();
                }
                isPrepared = false;
            }
        } 
        if (isPrepared) {
            getDeductionsGraph().delete(t);
            engine.delete(t);
        }
        lpbEngine.reset();
    }
    
//  =======================================================================
//  Support for LP engine profiling
    
    /**
     * Reset the LP engine profile.
     * @param enable it true then profiling will continue with a new empty profile table,
     * if false profiling will stop all current data lost.
     */
    public void resetLPProfile(boolean enable) {
        lpbEngine.resetProfile(enable);
    }
    
    /**
     * Print a profile of LP rules used since the last reset.
     */
    public void printLPProfile() {
        lpbEngine.printProfile();
    }
        
}



/*
    (c) Copyright 2003 Hewlett-Packard Development Company, LP
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.

    3. The name of the author may not be used to endorse or promote products
       derived from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
    IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
    IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
    NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
    THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/