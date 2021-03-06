/*
 * Copyright 2019 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.collector.receiver.grpc.service;

import com.google.protobuf.Empty;
import com.navercorp.pinpoint.collector.receiver.DispatchHandler;
import com.navercorp.pinpoint.grpc.AgentHeaderFactory;
import com.navercorp.pinpoint.grpc.server.AgentInfoContext;
import com.navercorp.pinpoint.grpc.trace.PAgentStat;
import com.navercorp.pinpoint.grpc.trace.PAgentStatBatch;
import com.navercorp.pinpoint.grpc.trace.StatGrpc;
import com.navercorp.pinpoint.io.header.Header;
import com.navercorp.pinpoint.io.header.HeaderEntity;
import com.navercorp.pinpoint.io.header.v2.HeaderV2;
import com.navercorp.pinpoint.io.request.DefaultMessage;
import com.navercorp.pinpoint.io.request.DefaultServerRequest;
import com.navercorp.pinpoint.io.request.Message;
import com.navercorp.pinpoint.io.request.ServerRequest;
import com.navercorp.pinpoint.thrift.io.DefaultTBaseLocator;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class StatService extends StatGrpc.StatImplBase {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private DispatchHandler dispatchHandler;

    public StatService(DispatchHandler dispatchHandler) {
        this.dispatchHandler = dispatchHandler;
    }

    @Override
    public StreamObserver<PAgentStat> sendAgentStat(StreamObserver<Empty> responseObserver) {
        StreamObserver<PAgentStat> observer = new StreamObserver<PAgentStat>() {
            @Override
            public void onNext(PAgentStat agentStat) {
                final Header header = new HeaderV2(Header.SIGNATURE, HeaderV2.VERSION, DefaultTBaseLocator.AGENT_STAT);
                final HeaderEntity headerEntity = new HeaderEntity(new HashMap<String, String>());
                final Message<PAgentStat> message = new DefaultMessage<PAgentStat>(header, headerEntity, agentStat);
                send(responseObserver, message);
            }

            @Override
            public void onError(Throwable throwable) {
                logger.warn("Failed to send agent stat", throwable);
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            }
        };

        return observer;
    }

    @Override
    public StreamObserver<PAgentStatBatch> sendAgentStatBatch(StreamObserver<Empty> responseObserver) {
        StreamObserver<PAgentStatBatch> observer = new StreamObserver<PAgentStatBatch>() {
            @Override
            public void onNext(PAgentStatBatch agentStatBatch) {
                final Header header = new HeaderV2(Header.SIGNATURE, HeaderV2.VERSION, DefaultTBaseLocator.AGENT_STAT);
                final HeaderEntity headerEntity = new HeaderEntity(new HashMap<String, String>());
                final Message<PAgentStatBatch> message = new DefaultMessage<PAgentStatBatch>(header, headerEntity, agentStatBatch);
                send(responseObserver, message);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            }
        };

        return observer;
    }

    private void send(StreamObserver<Empty> responseObserver, final Message<?> message) {
        final AgentHeaderFactory.Header header = AgentInfoContext.agentInfoKey.get();
        if (header == null) {
            logger.warn("Not found request header");
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Not found request header").asException());
            return;
        }
        // TODO remoteAddress, remotePort
        ServerRequest request = new DefaultServerRequest(message, header.getRemoteAddress(), header.getRemotePort());
        if (dispatchHandler != null) {
            dispatchHandler.dispatchSendMessage(request);
        }
    }
}
