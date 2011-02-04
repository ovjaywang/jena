/*
 * (c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.algebra.op;

import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.sparql.algebra.Op ;
import com.hp.hpl.jena.sparql.algebra.OpVisitor ;
import com.hp.hpl.jena.sparql.algebra.Transform ;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg ;
import com.hp.hpl.jena.sparql.sse.Tags ;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap ;

/** Property functions (or any OpBGP replacement)
 *  Execution will be per-engine specific */
public class OpPropFunc extends Op1
{
    // c.f. OpProcedure which is similar except for the handling of arguments.
    // Safer to have two (Ops are mainly abstract syntax, not executional).
    private Node uri ;
    private PropFuncArg subjectArgs ;
    private PropFuncArg objectArgs2 ;

    public OpPropFunc(Node uri, PropFuncArg args1 , PropFuncArg args2, Op op)
    {
        super(op) ;
        this.uri = uri ;
        this.subjectArgs = args1 ;
        this.objectArgs2 = args2 ;
    }
    
    public PropFuncArg getSubjectArgs()
    {
        return subjectArgs ;
    } 
    
    public PropFuncArg getObjectArgs()
    {
        return objectArgs2 ;
    } 
    
    @Override
    public Op apply(Transform transform, Op subOp)
    {
        return transform.transform(this, subOp) ;
    }

    public void visit(OpVisitor opVisitor)
    { opVisitor.visit(this) ; }

    public Node getProperty() { return uri ; }
    
    @Override
    public Op copy(Op op)
    {
        return new OpPropFunc(uri, subjectArgs, objectArgs2, op) ;
    }

    @Override
    public int hashCode()
    {
        return uri.hashCode() ^ getSubOp().hashCode() ;
    }

    @Override
    public boolean equalTo(Op other, NodeIsomorphismMap labelMap)
    {
        if ( ! ( other instanceof OpPropFunc ) ) return false ;
        OpPropFunc procFunc = (OpPropFunc)other ;
        
        
        return getSubOp().equalTo(procFunc.getSubOp(), labelMap) ;
    }

    public String getName()
    {
        return Tags.tagPropFunc ;
    }
}


/*
 * (c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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