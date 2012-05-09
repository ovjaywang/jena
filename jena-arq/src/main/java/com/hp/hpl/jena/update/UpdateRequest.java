/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.hpl.jena.update;

import java.util.ArrayList ;
import java.util.Collections ;
import java.util.Iterator ;
import java.util.List ;

import org.openjena.atlas.io.IndentedWriter ;
import org.openjena.atlas.io.PrintUtils ;
import org.openjena.atlas.io.Printable ;

import com.hp.hpl.jena.sparql.core.Prologue ;
import com.hp.hpl.jena.sparql.modify.request.UpdateWriter ;

/** A SPARQL Update consists of a number of operations (e.g. INSERT, CLEAR).
 *  A request is the unit of execution.
 */
public class UpdateRequest extends Prologue implements Printable, Iterable<Update>
{
    private List<Update> operations = new ArrayList<Update>() ;
    private List<Update> operationsView = Collections.unmodifiableList(operations) ;

    public UpdateRequest() { super() ; }
    public UpdateRequest(Update update)
    {
        this() ;
        add(update) ;
    }

    /** @deprecated Use @link{#add(Update)} */
    @Deprecated
    public void addUpdate(Update update) { add(update) ; } 

    public UpdateRequest add(Update update) { operations.add(update) ; return this ; } 
    public UpdateRequest add(String string)
    { 
        UpdateFactory.parse(this, string) ;
        return this ;
    }

    public List<Update> getOperations() { return operationsView ; }
    
    @Deprecated
    /** @deprecated Use @link{#getOperations()} instead. */
    public List<Update> getUpdates() { return operationsView ; }
    
    @Override
    public Iterator<Update> iterator()
    {
        return operationsView.iterator() ;
    }
    
    @Override
    public String toString()
    { return PrintUtils.toString(this) ; } 
    
    @Override
    public void output(IndentedWriter out)
    { UpdateWriter.output(this, out) ; }
}
