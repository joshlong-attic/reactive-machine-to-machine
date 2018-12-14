package rsocket.channel;

import io.rsocket.*;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@SpringBootApplication
public class PingPong {

	static String reply(String incoming) {
		if (incoming.equalsIgnoreCase("ping")) return "pong";
		if (incoming.equalsIgnoreCase("pong")) return "ping";
		throw new IllegalArgumentException("incoming must be either 'ping' or 'pong'");
	}

	public static void main(String[] args) {
		SpringApplication.run(PingPong.class, args);
	}
}

@Log4j2
@Component
class Ping implements Ordered, ApplicationListener<ApplicationReadyEvent> {


	@Override
	public void onApplicationEvent(ApplicationReadyEvent evt) {

		log.info("starting " + getClass().getName());

		RSocketFactory
			.connect()
			.transport(TcpClientTransport.create("localhost", 7000))
			.start()
			.flatMapMany(socket ->
				socket
					.requestChannel(Flux.interval(Duration.ofMillis(1000)).map(i -> DefaultPayload.create("ping")))
					.map(Payload::getDataUtf8)
					.doOnNext(str -> log.info("received " + str + " in " + getClass().getName()))
					.take(10)
					.doFinally(signalType -> socket.dispose()))
			.then()
			.block();

	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
}

@Log4j2
@Component
class Pong implements SocketAcceptor, Ordered,
	ApplicationListener<ApplicationReadyEvent> {

	@Override
	public Mono<RSocket> accept(ConnectionSetupPayload connectionSetupPayload, RSocket rSocket) {

		return Mono.just(new AbstractRSocket() {

			@Override
			public Flux<Payload> requestChannel(Publisher<Payload> payloads) {

				return Flux
					.from(payloads)
					.map(Payload::getDataUtf8)
					.doOnNext(str -> log.info("received " + str + " in " + getClass().getName()))
					.map(PingPong::reply)
					.map(DefaultPayload::create);
			}
		});
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
		log.info("starting " + getClass().getName());
		RSocketFactory
			.receive()
			.acceptor(this)
			.transport(TcpServerTransport.create("localhost", 7000))
			.start()
			.subscribe();
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
