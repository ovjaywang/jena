/*
 * (c) Copyright 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * (c) Copyright 2010 Talis System Ltd
 * All rights reserved.
 * [See end of file]
 */

package dev;

import java.io.IOException ;
import java.io.InputStream ;

import org.openjena.atlas.io.IO ;
import org.openjena.atlas.iterator.Filter ;
import org.openjena.atlas.lib.Sink ;
import org.openjena.atlas.lib.SinkCounting ;
import org.openjena.atlas.lib.SinkPrint ;
import org.openjena.atlas.lib.SinkWrapper ;
import org.openjena.atlas.lib.Tuple ;
import org.openjena.atlas.logging.Log ;
import org.openjena.riot.RiotReader ;
import org.openjena.riot.lang.LangRIOT ;

import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.graph.Triple ;
import com.hp.hpl.jena.query.Dataset ;
import com.hp.hpl.jena.rdf.model.Model ;
import com.hp.hpl.jena.sparql.sse.SSE ;
import com.hp.hpl.jena.tdb.TDB ;
import com.hp.hpl.jena.tdb.TDBFactory ;
import com.hp.hpl.jena.tdb.nodetable.NodeTable ;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB ;
import com.hp.hpl.jena.tdb.store.NodeId ;
import com.hp.hpl.jena.tdb.sys.SystemTDB ;
import com.hp.hpl.jena.util.FileManager ;

public class RunTDB
{
    static { Log.setLog4j() ; }
    static String divider = "----------" ;
    static String nextDivider = null ;
    static void divider()
    {
        if ( nextDivider != null )
            System.out.println(nextDivider) ;
        nextDivider = divider ;
    }

    public static void main(String[] args) throws IOException
    {
        Dataset ds = TDBFactory.createDataset() ;
        DatasetGraphTDB dsg = (DatasetGraphTDB)(ds.asDatasetGraph()) ;
        
        Node gn = Node.createURI("http://example/graph1") ;
        
        dsg.deleteAny(gn, null, null, null) ;
        
        SSE.write(dsg) ;
        System.exit(0) ;
        
        
        tdb.tdbloader.main("--loc=DB", "D.rdf") ;
        System.exit(0) ;
        
        tdb.tdbquery.main("--set=tdb:logExec=true", "--query=Q.rq") ; System.exit(0) ;
    }
    
    static void tupleFilter()
    {
        Dataset ds = TDBFactory.createDataset("DB") ;
        DatasetGraphTDB dsg = (DatasetGraphTDB)(ds.asDatasetGraph()) ;

        final NodeTable nodeTable = dsg.getQuadTable().getNodeTupleTable().getNodeTable() ;
        final NodeId target = nodeTable.getNodeIdForNode(Node.createURI("http://example/graph2")) ;

        System.out.println("Filter: "+target) ;
        
        Filter<Tuple<NodeId>> filter = new Filter<Tuple<NodeId>>() {
            public boolean accept(Tuple<NodeId> item)
            {
                // Reverse the lookup as a demo
                Node n = nodeTable.getNodeForNodeId(target) ;
                //System.err.println(item) ;
                if ( item.size() == 4 && item.get(0).equals(target) )
                {
                    System.out.println("Reject: "+item) ;
                    return false ;
                }
                System.out.println("Accept: "+item) ;
                return true ;
            } } ;
            
            
        TDB.getContext().set(SystemTDB.symTupleFilter, filter) ;

        String qs = "SELECT * { { ?s ?p ?o } UNION { GRAPH ?g { ?s ?p ?o } }}" ;
        //String qs = "SELECT * { GRAPH ?g { ?s ?p ?o } }" ;
        
        DevCmds.tdbquery("--tdb=tdb.ttl", qs) ;
    }
    
    static class SinkInsertText<T> extends SinkWrapper<T>
    {
        String string ;
        public SinkInsertText(Sink<T> sink, String string)
        {
            super(sink) ;
            this.string = string ;
        }
        
        @Override
        public void send(T item)
        {
            super.send(item) ;
            System.out.print(string) ;
        }
    }
    
    public static void streamInference()
    {
        Model m = FileManager.get().loadModel("V.ttl") ;
        
        SinkCounting<Triple> outputSink = new SinkCounting<Triple>(new SinkPrint<Triple>()) ;
        
        SinkCounting<Triple> inputSink1 = new SinkCounting<Triple>(new InferenceExpander(outputSink, m)) ;
        // Add gaps between parser triples. 
        Sink<Triple> inputSink2 = new SinkInsertText<Triple>(inputSink1, "--\n") ;
        
        Sink<Triple> inputSink = inputSink2 ;
        
        InputStream input = IO.openFile("D.ttl") ;
        
        LangRIOT parser = RiotReader.createParserTurtle(input, "http://base/", inputSink) ;
        parser.parse() ;
        inputSink.flush() ;

        System.out.println() ;
        System.out.printf("Input  =  %d\n", inputSink1.getCount()) ;
        System.out.printf("Total  =  %d\n", outputSink.getCount()) ;
    }
}

/*
 * (c) Copyright 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * (c) Copyright 2010 Talis System Ltd
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