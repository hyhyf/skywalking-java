 /*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.testcase.spring.jms;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jms.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class CaseServlet extends HttpServlet {
    private static final Logger LOGGER = LogManager.getLogger(CaseServlet.class);
    private static final String USER_NAME = ActiveMQConnection.DEFAULT_USER;
    private static final String PASSWORD = ActiveMQConnection.DEFAULT_PASSWORD;
    private String brokenUrl = System.getProperty("activemq.server");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // your codes
        PrintWriter printWriter = resp.getWriter();

        Session session = null;
        Connection connection = null;
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(USER_NAME, PASSWORD, brokenUrl);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(Boolean.TRUE, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue("test");
            MessageProducer messageProducer = session.createProducer(destination);
            TextMessage message = session.createTextMessage("test");
            messageProducer.send(message);
            session.commit();
            session.close();
            connection.close();
        } catch (Exception ex) {
            LOGGER.error(ex);
            try {
                session.close();
                connection.close();
            } catch (JMSException e) {
                LOGGER.error(e);
            }
        }
        new ConsumerThread().start();

        printWriter.write("success");
        printWriter.flush();
        printWriter.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    public class ConsumerThread extends Thread {
        @Override
        public void run() {
            Session session = null;
            Connection connection = null;
            try {
                ConnectionFactory factory = new ActiveMQConnectionFactory(USER_NAME, PASSWORD, brokenUrl);
                connection = factory.createConnection();
                connection.start();
                session = connection.createSession(Boolean.TRUE, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue("test");
                MessageConsumer messageConsumer = session.createConsumer(destination);
                messageConsumer.receive();
                session.close();
                connection.close();
            } catch (Exception ex) {
                LOGGER.error(ex);
                try {
                    session.close();
                    connection.close();
                } catch (JMSException e) {
                    LOGGER.error(e);
                }
            }
        }
    }

}
