package com.example.rsocketclient.streaming;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.java.Log;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@SpringBootApplication
public class StreamingApplication {

		public static void main(String args[]) {
				SpringApplication.run(StreamingApplication.class, args);
		}
}

@Log
@Configuration
class Server {

		@Bean
		InitializingBean serve() {

				SocketAcceptor sa = (connectionSetupPayload, rSocket) ->
					Mono.just(new AbstractRSocket() {

							@Override
							public Flux<Payload> requestStream(Payload payload) {
									return Flux
										.interval(Duration.ofMillis(100))
										.map(aLong -> DefaultPayload.create("Interval: " + aLong));
							}
					});

				return () ->
					RSocketFactory
						.receive()
						.acceptor(sa)
						.transport(TcpServerTransport.create("localhost", 7000))
						.start()
						.subscribe();
		}
}

@Log
@Configuration
class Client implements ApplicationListener<ApplicationReadyEvent> {

		@Override
		public void onApplicationEvent(ApplicationReadyEvent evt) {

				RSocketFactory
					.connect()
					.transport(TcpClientTransport.create("localhost", 7000))
					.start()
					.flatMapMany(socket ->
						socket
							.requestStream(DefaultPayload.create("Hello"))
							.map(Payload::getDataUtf8)
							.take(10)
							.doFinally(signal -> socket.dispose())
					)
					.subscribe(name -> log.info("consuming " + name + "."));
		}
}
