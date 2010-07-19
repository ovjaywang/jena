/*
 * (c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sdb.core.sqlnode;

import org.openjena.atlas.io.IndentedLineBuffer;
import org.openjena.atlas.io.IndentedWriter;
import com.hp.hpl.jena.sdb.core.JoinType;

public class GenerateSQLMySQL extends GenerateSQL
{
    @Override
    protected SqlNodeVisitor makeVisitor(IndentedLineBuffer buff)
    {
        return new GeneratorVisitorMySQL(buff) ;
    }
}

class GeneratorVisitorMySQL extends GenerateSQLVisitor
{
    // STRAIGHT_JOIN stops the optimizer reordering inner join
    // It requires that the left table and right table are kept as left and right,
    // so a sequence of joins can not be reordered. 
    
    static final String InnerJoinOperatorStraight = "STRAIGHT_JOIN" ;
    static final String InnerJoinOperatorDefault = JoinType.INNER.sqlOperator() ;
    
    public GeneratorVisitorMySQL(IndentedWriter out) { super(out) ; }

    @Override
    public void visit(SqlJoinInner join)
    { 
        join = rewrite(join) ;
        visitJoin(join, InnerJoinOperatorDefault) ;
    }   
    
    @Override
    protected void genLimitOffset(SqlSelectBlock sqlSelectBlock)
    {
        
        if ( sqlSelectBlock.getLength() >= 0 || sqlSelectBlock.getStart() >= 0 )
        {
            // MySQL synatx issue - need LIMIT even if only OFFSET
            long length = sqlSelectBlock.getLength() ;
            if ( length < 0 )
            {
                sqlSelectBlock.addNote("Require large LIMIT") ;
                length = Long.MAX_VALUE ;
            }
            out.println("LIMIT "+length) ;
            if ( sqlSelectBlock.getStart() >= 0 )
                out.println("OFFSET "+sqlSelectBlock.getStart()) ;
        }
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