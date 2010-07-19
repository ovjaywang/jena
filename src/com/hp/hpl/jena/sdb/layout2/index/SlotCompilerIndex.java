/*
 * (c) Copyright 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sdb.layout2.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sdb.SDBException;
import com.hp.hpl.jena.sdb.compiler.QuadBlock;
import com.hp.hpl.jena.sdb.compiler.SqlBuilder;
import com.hp.hpl.jena.sdb.core.AliasesSql;
import com.hp.hpl.jena.sdb.core.SDBRequest;
import com.hp.hpl.jena.sdb.core.sqlexpr.S_Equal;
import com.hp.hpl.jena.sdb.core.sqlexpr.SqlColumn;
import com.hp.hpl.jena.sdb.core.sqlexpr.SqlConstant;
import com.hp.hpl.jena.sdb.core.sqlexpr.SqlExpr;
import com.hp.hpl.jena.sdb.core.sqlexpr.SqlExprList;
import com.hp.hpl.jena.sdb.core.sqlnode.SqlNode;
import com.hp.hpl.jena.sdb.core.sqlnode.SqlTable;
import com.hp.hpl.jena.sdb.layout2.NodeLayout2;
import com.hp.hpl.jena.sdb.layout2.SlotCompiler2;
import com.hp.hpl.jena.sdb.layout2.TableDescNodes;
import com.hp.hpl.jena.sdb.layout2.TableDescTriples;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.util.FmtUtils;

public class SlotCompilerIndex extends SlotCompiler2
{
    private static Logger log = LoggerFactory.getLogger(SlotCompilerIndex.class) ;
    
    private static final String NodeConstBase = AliasesSql.NodesConstantAliasBase ;
    
    Map<Node, SqlColumn> constantCols = new HashMap<Node, SqlColumn>() ;
    
    // Could be a set but it's convenient to keep thing in order for debugging.
    private List<Node> constants ; // = new ArrayList<Node>() ;
    private List<Var>  vars ; // = new ArrayList<Var>() ;
    
    protected TableDescTriples tripleTableDesc ;
    protected TableDescNodes   nodeTableDesc ;
    
    private SqlNode constantsSqlNode ;
    
    public SlotCompilerIndex(SDBRequest request)
    { 
        super(request) ;
        tripleTableDesc = request.getStore().getTripleTableDesc() ;
        nodeTableDesc = request.getStore().getNodeTableDesc() ;
    }
    
    @Override
    public SqlNode start(QuadBlock quads)
    {
        // Need to work out when constants are in-scope from earlier.
        
        // Reset context.
        constants = new ArrayList<Node>() ;
        vars = new ArrayList<Var>() ;
        
        classify(quads, constants, vars) ;
        constantsSqlNode = insertConstantAccesses(constants) ;
        // can be hold this back until the end of a block?
        return constantsSqlNode ;
    }
    
    @Override
    protected void constantSlot(SDBRequest request, Node node, SqlColumn thisCol, SqlExprList conditions)
    {
        SqlColumn colId = constantCols.get(node) ;
        if ( colId == null )
        {
            log.warn("Failed to find id col for "+node) ;
            return ;
        }
        SqlExpr c = new S_Equal(thisCol, colId) ;
        c.addNote("Const condition: "+FmtUtils.stringForNode(node, getRequest().getPrefixMapping())) ;
        conditions.add(c) ;
        return ; 
    }

    protected SqlNode insertConstantAccesses(Collection<Node> constants)
    {
        SqlNode sqlNode = null ;
        for ( Node n : constants )
        {
            long hash = NodeLayout2.hash(n);
            SqlConstant hashValue = new SqlConstant(hash) ;

            // Access nodes table.
            SqlTable nTable = new SqlTable(getRequest().genId(NodeConstBase), 
                                           nodeTableDesc.getTableName()) ;
            
            nTable.addNote("Const: "+FmtUtils.stringForNode(n, getRequest().getPrefixMapping())) ; 
            SqlColumn cHash = new SqlColumn(nTable, nodeTableDesc.getHashColName()) ;
            // Record 
            constantCols.put(n, new SqlColumn(nTable, nodeTableDesc.getIdColName())) ;
            SqlExpr c = new S_Equal(cHash, hashValue) ;
            sqlNode = SqlBuilder.innerJoin(getRequest(), sqlNode, nTable) ;
            sqlNode = SqlBuilder.restrict(getRequest(), sqlNode, c)  ;
        }
        return sqlNode ;
    }
    
    protected void classify(QuadBlock quadBlock, Collection<Node> constants, Collection<Var>vars)
    {
        for ( Quad quad : quadBlock )
        {
            // Some constants are only markers and are not stored in the database.
            if ( ! Quad.isDefaultGraph(quad.getGraph()) && ! quad.isUnionGraph() )  // quad.isDefaultGraph ARQ 2.8.4 and later
                acc(constants, vars, quad.getGraph()) ;
            acc(constants, vars, quad.getSubject()) ;
            acc(constants, vars, quad.getPredicate()) ;
            acc(constants, vars, quad.getObject()) ;
        }
    }

    private static void acc(Collection<Node>constants,  Collection<Var>vars, Node node)
    { 
        if ( node.isLiteral() || node.isBlank() || node.isURI() )
        {
            if ( ! constants.contains(node) )
                constants.add(node) ;
            return ;
        }
        if ( Var.isVar(node) )
        {
            vars.add(Var.alloc(node)) ;
            return ;
        }
        if ( node.isVariable() )
        {
            log.warn("Node_Varable but not a Var; bodged") ;
            vars.add(Var.alloc(node)) ;
            return ;
        }
        log.error("Unknown Node type: "+node) ;
        throw new SDBException("Unknown Node type: "+node) ;
    }
}

/*
 * (c) Copyright 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */