/*
 * (c) Copyright 2006 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sdb.layout2;

import java.sql.*;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import com.hp.hpl.jena.query.util.Utils;
import com.hp.hpl.jena.sdb.sql.SDBConnection;
import com.hp.hpl.jena.sdb.sql.SDBConnectionHolder;
import com.hp.hpl.jena.sdb.sql.SDBExceptionSQL;
import com.hp.hpl.jena.sdb.sql.SQLUtils;
import com.hp.hpl.jena.sdb.store.StoreLoader;


public abstract class LoaderTriplesNodes
    extends SDBConnectionHolder
    implements StoreLoader, LoaderFmt
{
    private static Log log = LogFactory.getLog(LoaderTriplesNodes.class);
    private static final String classShortName = Utils.classShortName(LoaderTriplesNodes.class)  ;
    
    // Delayed initialization until first bulk load.
    private boolean initialized = false ;

    Thread commitThread = null ;
    PreparedTriple flushSignal = new PreparedTriple();
    boolean threading = false;
    ArrayBlockingQueue<PreparedTriple> queue ;
    
    int count;
    int chunkSize = 10000;
    
    private boolean autoCommit ;                    // State of the connection
    protected PreparedStatement insertLoaderTable;
    protected PreparedStatement insertObjects;
    protected PreparedStatement insertPredicates;
    protected PreparedStatement insertSubjects;
    protected PreparedStatement insertTriples;
    protected PreparedStatement clearLoaderTable;

    public LoaderTriplesNodes(SDBConnection connection)
    {
        super(connection) ;
    }
    
    public void startBulkLoad()
    {
        init() ;
        
        try
        { 
            // Record the state of the JDBC connection as we start 
            autoCommit = connection().getSqlConnection().getAutoCommit() ;
            
            if ( autoCommit )
                connection().getSqlConnection().setAutoCommit(false) ;
        }
        catch (SQLException ex)
        { throw new SDBExceptionSQL("Failed to set auto committ state", ex) ; }
    }
    
    public void finishBulkLoad()
    {
        flushTriples() ;
        try {
            if ( autoCommit )
                connection().getSqlConnection().setAutoCommit(autoCommit) ;
        } catch (SQLException ex)
        { throw new SDBExceptionSQL("Failed to reset connection", ex) ; }
    }
    
    private void init()
    {
        if ( initialized ) return ;
        initialized = true ;
        
        createLoaderTable();
        createPreparedStatements() ; 
//        try
//        {
//            Connection conn = connection().getSqlConnection();
//            this.clearLoaderTable = conn.prepareStatement("DELETE FROM NTrip;");
//            this.insertLoaderTable = conn
//                .prepareStatement("INSERT INTO NTrip VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
//            initStatements();
//        }
//        catch (SQLException e)
//        {
//            log.error("Problem creating bulk loader", e);
//        }

        count = 0;

        if (threading)
        {
            queue = new ArrayBlockingQueue<PreparedTriple>(chunkSize);
            commitThread = new Thread(new Commiter());
            commitThread.start();
            log.info("Threading");
        }
    }
    
    // Work done else where.
    
    public void deleteTriple(Triple triple) { LoaderOneTriple.deleteTriple(connection(), triple) ; }

    public void addTriple(Triple triple)
    {
        // Prepare our triple for loading. Helps with threaded loader.
        PreparedTriple pTriple = new PreparedTriple();
        pTriple.subject = new PreparedNode(triple.getSubject());
        pTriple.predicate = new PreparedNode(triple.getPredicate());
        pTriple.object = new PreparedNode(triple.getObject());

        if (threading)
        {
            if (!commitThread.isAlive())
                log.warn("Loader thread has died");
            else
                try
                {
                    queue.put(pTriple);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
        }
        else
            addOneTriple(pTriple);
    }

    // Queue up a triple, committing if we have enough chunks
    private void addOneTriple(PreparedTriple triple)
    {
        try
        {
            count++;
            addToInsert(insertLoaderTable, triple.subject, 1, false);
            addToInsert(insertLoaderTable, triple.predicate, 4, false);
            addToInsert(insertLoaderTable, triple.object, 7, true);
            insertLoaderTable.addBatch();
            if (count >= chunkSize)
                commitTriples();
        }
        catch (BatchUpdateException e)
        {
            e.printStackTrace();
            e.getNextException().printStackTrace();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    private void addToInsert(PreparedStatement s, PreparedNode node,
                             int offset, boolean fill)
        throws SQLException
    {
        s.setLong(offset, node.hash);
        s.setString(offset + 1, node.lex);

        if (!fill) // s or p, very small
        {
            s.setInt(offset + 2, node.typeId);
            return;
        }

        s.setString(offset + 2, node.lang);
        s.setString(offset + 3, node.datatype);
        s.setInt(offset + 4, node.typeId);
        s.setInt(offset + 5, node.valInt);
        s.setDouble(offset + 6, node.valDouble);
        s.setTimestamp(offset + 7, node.valDateTime);
    }
    
    // Put the queued triples into the database
    private void commitTriples() throws SQLException
    {
        count = 0;
        insertLoaderTable.executeBatch();
        insertObjects.execute();
        insertPredicates.execute();
        insertSubjects.execute();
        insertTriples.execute();
        clearLoaderTable.execute() ;
        if ( autoCommit )
            // Commit the transaction if started outside of a transaction 
            connection().getSqlConnection().commit();
    }

    // ----
    
    public void flushTriples()
    {
        try
        {
            if (threading && !commitThread.isAlive())
                log.warn("Loader thread has died");
            else if (!threading)
                commitTriples();
            else
            { // finish up threaded load
                queue.put(flushSignal);
                while (commitThread.isAlive())
                    Thread.sleep(100);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    
    public void setChunkSize(int chunkSize)            { this.chunkSize = chunkSize ; }

    public int getChunkSize()                          { return this.chunkSize ; }

    public void setUseThreading(boolean useThreading)  { this.threading = useThreading ; }

    public boolean getUseThreading()                   { return this.threading ; }

    
    // ---- Bulk loader
    
    

    /**
     * We use these so the preparation (especially hashing) happens away from
     * the db load thread
     */

    class PreparedTriple
    {
        PreparedNode subject;
        PreparedNode predicate;
        PreparedNode object;
    }

    class PreparedNode
    {
        long hash;
        String lex;
        String lang;
        String datatype;
        int typeId;
        int valInt;
        double valDouble;
        Timestamp valDateTime;

        PreparedNode(Node node)
        {
            lex = TableNodes.nodeToLex(node);
            ValueType vType = ValueType.lookup(node);
            typeId = TableNodes.nodeToType(node);

            lang = "";
            datatype = "";

            if (node.isLiteral())
            {
                lang = node.getLiteralLanguage();
                datatype = node.getLiteralDatatypeURI();
                if (datatype == null)
                    datatype = "";
            }
            // Value of the node
            valInt = 0;
            if (vType == ValueType.INTEGER)
                valInt = Integer.parseInt(lex);

            valDouble = 0;
            if (vType == ValueType.DOUBLE)
                valDouble = Double.parseDouble(lex);

            if (vType == ValueType.DATETIME)
            {
                String dateTime = SQLUtils.toSQLdatetimeString(lex);
                valDateTime = Timestamp.valueOf(dateTime);
            }
            else
                valDateTime = new Timestamp(0);

            hash = TableNodes.hash(lex, lang, datatype, typeId);
        }
    }

    /**
     * The (very minimal) thread code
     */

    class Commiter implements Runnable
    {

        public void run()
        {
            log.info("Running loader thread");
            try
            {
                while (true)
                {
                    PreparedTriple triple = queue.take();
                    if (triple == flushSignal)
                    {
                        commitTriples(); // force commit
                        break;
                    }
                    else
                        addOneTriple(triple);
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }
}

/*
 * (c) Copyright 2006 Hewlett-Packard Development Company, LP
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