/*
  (c) Copyright 2002, 2003, Hewlett-Packard Company, all rights reserved.
  [See end of file]
  $Id: TripleMatch.java,v 1.3 2003-06-11 12:49:42 chris-dollin Exp $
*/

package com.hp.hpl.jena.graph;

/**
 * THIS INTERFACE IS BECOMMING OBSOLETE.
 * A TripleMatch evaluates as true against a triple
 * if all four methods evaluate as true.
 * @author Jeremy Carroll
 *
 * 
 */
public interface TripleMatch {
    
    /** If it is known that all triples selected by this filter will
     *  have a common subject, return that node, otherwise return null */    
    Node getMatchSubject();
    
    /** If it is known that all triples selected by this match will
     *  have a common predicate, return that node, otherwise return null */
    Node getMatchPredicate();
    
    /** If it is known that all triples selected by this match will
     *  have a common object, return that node, otherwise return null */
    Node getMatchObject();

    /**
        Answer a Triple capturing this match.
    */
    Triple asTriple();
}

/*
    (c) Copyright Hewlett-Packard Company 2002, 2003
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
