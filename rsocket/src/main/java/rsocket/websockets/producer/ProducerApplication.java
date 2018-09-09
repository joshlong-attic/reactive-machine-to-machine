package rsocket.websockets.producer;

import io.rsocket.*;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@SpringBootApplication
@Log4j2
public class ProducerApplication implements ApplicationListener<ApplicationReadyEvent> {

		public static void main(String args[]) {
				SpringApplication.run(ProducerApplication.class, args);
		}

		@Override
		public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
				AbstractRSocket rSocket = new AbstractRSocket() {

						@Override
						public Flux<Payload> requestStream(Payload payload) {
								String name = payload.getDataUtf8();
								return Flux.<String>generate(sink -> sink.next("Hello " + name +
									" @ " + Instant.now().toString()))
									.delayElements(Duration.ofSeconds(1))
									.map(DefaultPayload::create);
						}
				};
				SocketAcceptor socketAcceptor = (connectionSetupPayload, rs) -> Mono.just(rSocket);
				RSocketFactory
					.receive()
					.acceptor(socketAcceptor)
					.transport(WebsocketServerTransport.create("localhost", 7000))
					.start()
					.onTerminateDetach()
					.subscribe();
		}
}
