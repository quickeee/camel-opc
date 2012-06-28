/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.summit.camel.opc;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.common.AlreadyConnectedException;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.ItemState;

/**
 * The opcda2 consumer.
 */
public class Opcda2Consumer extends ScheduledPollConsumer {
    private final Opcda2Endpoint endpoint;
    
    private boolean forceHardwareRead;
    
    public Opcda2Consumer(Opcda2Endpoint endpoint, Processor processor) throws IllegalArgumentException, UnknownHostException, JIException, AlreadyConnectedException {
        super(endpoint, processor);
        this.endpoint = endpoint;
        
        forceHardwareRead = endpoint.isForceHardwareRead();
    }

    @Override
    protected int poll() throws Exception {
        Exchange exchange = endpoint.createExchange();

        Map<String,ItemState> data = new TreeMap<String, ItemState>();

        for(String key : endpoint.getOpcItemIds()){
            Item item = endpoint.getOpcItem(key);
            //TODO this is not serializable... we'll need our own source for this. Dumb.
            ItemState is = item.read(isForceHardwareRead());
            
            data.put(key, is);
        }
        
        exchange.getOut().setBody(data);
        
        try {
            // send message to next processor in the route
            getProcessor().process(exchange);
            return 1; // number of messages polled
        } finally {
            // log exception if an exception occurred and was not handled
            if (exchange.getException() != null) {
                getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
            }
        }
    }

    /**
     * @return the forceHardwareRead
     */
    public boolean isForceHardwareRead() {
        return forceHardwareRead;
    }
}
