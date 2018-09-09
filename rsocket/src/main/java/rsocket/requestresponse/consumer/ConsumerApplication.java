package rsocket.requestresponse.consumer;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rsocket.requestresponse.Protocol;

import java.time.Instant;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Log4j2
@SpringBootApplication
public class ConsumerApplication implements ApplicationListener<ApplicationReadyEvent> {

		public static void main(String[] args) {
				SpringApplication.run(ConsumerApplication.class, args);
		}

		@Override
		public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
				RSocketFactory
					.connect()
					.transport(TcpClientTransport.create("localhost", 7000))
					.start()
					.subscribe(this::exchange);
		}

		void exchange(RSocket socket) {
				log.info("starting " + this.getClass().getName() + " @ " + Instant.now().toString());

				Flux<String> pingPongClient =
					this.requestResponse(socket, Protocol.PING)
						.thenMany(this.requestResponse(socket, Protocol.PONG))
						.thenMany(this.requestResponse(socket, Protocol.PING));

				pingPongClient.subscribe();
		}

		Mono<String> requestResponse(RSocket socket,
																															String message) {
				return socket
					.requestResponse(DefaultPayload.create(message))
					.map(Payload::getDataUtf8)
					.onErrorReturn("error")
					.doOnNext(result -> log.info("client: " + result));
		}
}
