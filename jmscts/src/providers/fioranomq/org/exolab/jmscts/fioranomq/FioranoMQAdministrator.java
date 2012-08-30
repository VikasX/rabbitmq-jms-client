/**
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "Exolab" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Exoffice Technologies.  For written permission,
 *    please contact jima@intalio.com.
 *
 * 4. Products derived from this Software may not be called "Exolab"
 *    nor may "Exolab" appear in their names without prior written
 *    permission of Exoffice Technologies. Exolab is a registered
 *    trademark of Exoffice Technologies.
 *
 * 5. Due credit should be given to the Exolab Project
 *    (http://www.exolab.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY EXOFFICE TECHNOLOGIES AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * EXOFFICE TECHNOLOGIES OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2001, 2003 (C) Exoffice Technologies Inc. All Rights Reserved.
 *
 * $Id: FioranoMQAdministrator.java,v 1.1 2003/05/04 14:12:39 tanderson Exp $
 */
package org.exolab.jmscts.fioranomq;

import java.util.Hashtable;
import java.util.Properties;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import fiorano.jms.md.QueueMetaData;
import fiorano.jms.md.TopicMetaData;
import fiorano.jms.runtime.admin.MQAdminConnection;
import fiorano.jms.runtime.admin.MQAdminConnectionFactory;
import fiorano.jms.runtime.admin.MQAdminService;
import fiorano.jms.runtime.naming.FioranoInitialContextFactory;

import org.exolab.jmscts.provider.Administrator;


/**
 * This class provides methods for obtaining and manipulating administered 
 * objects managed by the FioranoMQ implementation of JMS
 *
 * @version     $Revision: 1.1 $ $Date: 2003/05/04 14:12:39 $
 * @author      <a href="mailto:tma@netspace.net.au">Tim Anderson</a>
 * @see         FioranoMQProvider
 */
class FioranoMQAdministrator implements Administrator {

    /**
     * Returns the name of the QueueConnectionFactory bound in JNDI
     *
     * @return the default QueueConnectionFactory name
     */
    public String getQueueConnectionFactory() {
        return "primaryQCF";
    }

    /**
     * Returns the name of the TopicConnectionFactory bound in JNDI
     *
     * @return the default TopicConnectionFactory name
     */
    public String getTopicConnectionFactory() {
        return "primaryTCF";
    }

    /**
     * Returns the JNDI context used to look up administered objects
     *
     * @return the JNDI context under which administered objects are bound
     * @throws NamingException if the context cannot be obtained
     */
    public Context getContext() throws NamingException {
        Hashtable properties = new Hashtable ();
        properties.put(Context.SECURITY_PRINCIPAL, "anonymous");
        properties.put(Context.SECURITY_CREDENTIALS, "anonymous");
        properties.put(Context.PROVIDER_URL,"http://localhost:1856");
        properties.put(Context.INITIAL_CONTEXT_FACTORY, 
                       FioranoInitialContextFactory.class.getName());
        return new InitialContext(properties);
    }
    
    /**
     * Create an administered destination
     *
     * @param name the destination name
     * @param queue if true, create a queue, else create a topic
     * @throws JMSException if the destination cannot be created
     */
    public void createDestination(String name, boolean queue)
        throws JMSException {

        try {
            MQAdminConnectionFactory factory = 
                (MQAdminConnectionFactory) getContext().lookup("primaryACF");
            MQAdminConnection connection = factory.createMQAdminConnection(
                "admin", "passwd");
            MQAdminService admin = connection.getMQAdminService();
            if (queue) {
                QueueMetaData metadata = new QueueMetaData();
                metadata.setName(name);
                admin.createQueue(metadata);
            } else {
                TopicMetaData metadata = new TopicMetaData();
                metadata.setName(name);
                admin.createTopic(metadata);
            }
        } catch (NamingException exception) {
            JMSException error = new JMSException(exception.getMessage());
            error.setLinkedException(exception);
            throw error;
        }
    }

    /**
     * Destroy an administered destination
     *
     * @param name the destination name
     * @throws JMSException if the destination cannot be destroyed
     */
    public void destroyDestination(String name)
        throws JMSException {

        try {
            MQAdminConnectionFactory factory = 
                (MQAdminConnectionFactory) getContext().lookup("primaryACF");
            MQAdminConnection connection = factory.createMQAdminConnection(
                "admin", "passwd");
            Context context = getContext();
            Destination destination = (Destination) context.lookup(name);
            MQAdminService admin = connection.getMQAdminService();
            if (destination instanceof Queue) {
                admin.deleteQueue(name);
            } else {
                admin.deleteTopic(name);
            }
        } catch (NamingException exception) {
            JMSException error = new JMSException(exception.getMessage());
            error.setLinkedException(exception);
            throw error;
        }
    }

    /**
     * Returns true if an administered destination exists
     *
     * @param name the destination name
     * @throws JMSException for any internal JMS provider error
     */
    public boolean destinationExists(String name)
        throws JMSException {

        boolean exists = false;
        try {
            getContext().lookup(name);
            exists = true;
        } catch (NameNotFoundException ignore) {
        } catch (Exception exception) {
            JMSException error = new JMSException(exception.getMessage());
            error.setLinkedException(exception);
            throw error;
        }
        return exists;
    }

} //-- FioranoMQAdministrator