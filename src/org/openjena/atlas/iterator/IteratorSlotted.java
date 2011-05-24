/*
 * (c) Copyright 2011 Epimorphics Ltd.
 * All rights reserved.
 * [See end of file]
 */

package org.openjena.atlas.iterator;

import java.util.Iterator ;
import java.util.NoSuchElementException ;

import com.hp.hpl.jena.sparql.util.Utils ;

/** An Iterator with a one slot lookahead. */  
abstract class IteratorSlotted<T> implements Iterator<T>
{
    // Could move in the async abort part of QueryIterator
    private boolean finished = false ;
    private T slot = null ; 

    protected IteratorSlotted() { }

    // -------- The contract with the subclasses 
    
    /** Implement this, not next() or nextBinding()
        Returning null is turned into NoSuchElementException 
        Does not need to call hasNext (can presume it is true) */
    protected abstract T moveToNext() ;
    
    /** Can return true here then null from moveToNext() to indicate end. */ 
    protected abstract boolean hasMore() ;
    // altter add a flag to say if null is a legal value.
    
    /** Close the iterator. */
    protected void closeIterator() { }
   
    // -------- The contract with the subclasses 

    protected boolean isFinished() { return finished ; }

    /** final - subclasses implement hasNextBinding() */
    //@Override
    public final boolean hasNext()
    {
        if ( finished )
            return false ;
        if ( slot != null )
            return false ;

        boolean r = hasMore() ;
        if ( ! r )
        {
            close() ;
            return false ;
        }
        
        slot = moveToNext() ;
        if ( slot == null )
        {
            close() ;
            return false ;
        }
        return true ;
    }
    
    /** final - autoclose and registration relies on it - implement moveToNextBinding() */
    //@Override
    public final T next()
    {
        if ( ! hasNext() ) throw new NoSuchElementException(Utils.className(this)) ;
        
        T obj = slot ;
        slot = moveToNext() ;
        return obj ;
    }
    
    public final T peek()
    {
        return slot ;
    }
    
    
    //@Override
    public final void remove()
    {
        throw new UnsupportedOperationException(Utils.className(this)+".remove") ;
    }
    
    public void close()
    {
        if ( finished )
            return ;
        closeIterator() ;
        finished = true ;
    }
}
/*
 * (c) Copyright 2011 Epimorphics Ltd.
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