package com.example.rsocketclient.requestresponse;

import io.rsocket.*;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/

@SpringBootApplication
public class RequestResponseApplication {

		public static void main(String args[]) {
				SpringApplication.run(RequestResponseApplication.class, args);
		}
}

@Configuration
class Server {


		static final String PING = "ping";
		static final String PONG = "pong";

		@Bean
		InitializingBean serve() {

				AbstractRSocket myRSocket = new AbstractRSocket() {

						private final AtomicLong count = new AtomicLong();

						@Override
						public Mono<Payload> requestResponse(Payload p) {
								return this.count.incrementAndGet() > 5 ?
									Mono.error(new RuntimeException("failing on purpose!")) :
									Mono.just(DefaultPayload.create(p.getDataUtf8().equalsIgnoreCase(PONG) ? PING : PONG));
						}
				};

				return () ->
					RSocketFactory
						.receive()
						.acceptor((setupPayload, reactiveSocket) -> Mono.just(myRSocket))
						.transport(TcpServerTransport.create("localhost", 7000))
						.start()
						.subscribe();
		}
}

@Configuration
class Client implements ApplicationListener<ApplicationReadyEvent> {

		private final Log log = LogFactory.getLog(getClass());

		@Override
		public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
				RSocketFactory
					.connect()
					.transport(TcpClientTransport.create("localhost", 7000))
					.start()
					.subscribe(this::exchange);
		}

		private void exchange(RSocket socket) {
				log.info("starting " + this.getClass().getName() + " @ " + Instant.now().toString());

				requestResponse(socket, Server.PING)
					.thenMany(requestResponse(socket, Server.PONG))
					.thenMany(requestResponse(socket, Server.PING))
					.subscribe();
		}

		private Mono<String> requestResponse(RSocket socket,
																																							String message) {
				return socket
					.requestResponse(DefaultPayload.create(message))
					.map(Payload::getDataUtf8)
					.onErrorReturn("error")
					.doOnNext(this.log::info);
		}
}