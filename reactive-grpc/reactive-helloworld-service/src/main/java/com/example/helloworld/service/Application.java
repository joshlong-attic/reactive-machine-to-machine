package com.example.helloworld.service;


import com.example.helloworld.GreeterGrpc;
import com.example.helloworld.GreeterReply;
import com.example.helloworld.GreeterRequest;
import com.example.helloworld.ReactorGreeterGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@SpringBootApplication
public class Application {

		@Bean
		ScheduledExecutorService executorService() {
				return Executors.newScheduledThreadPool(5);
		}

		@Bean
		Server server(GreeterService greeterService, @Value("${server.port:8000}") int port) {
				return ServerBuilder
					.forPort(port)
					.addService(greeterService)
					.build();
		}

		public static void main(String args[]) throws Exception {
				SpringApplication.run(Application.class, args);
		}
}

@Service
class GreeterService extends ReactorGreeterGrpc.GreeterImplBase {

		@Override
		public Flux<GreeterReply> sayHello(Mono<GreeterRequest> request) {
				return request.flatMapMany(greeterRequest -> this.reply(greeterRequest.getName()));
		}

		private Flux<GreeterReply> reply(String name) {
				return Flux
					.<String>generate(sink -> sink.next("Hello " + name + " @ " + Instant.now().toString() + "!"))
					.map(n -> GreeterReply.newBuilder().setMessage(n).build())
					.take(10)
					.delayElements(Duration.ofSeconds(1));
		}
}

@Component
@Log4j2
class Runner implements Runnable {

		private final Executor executor;
		private final Server server;

		public Runner(Executor executor, Server server) {
				this.executor = executor;
				this.server = server;
		}

		@EventListener(ApplicationReadyEvent.class)
		public void start(ApplicationReadyEvent event) {
				try {
						logMethod("start()");
						this.server.start();
						this.executor.execute(this);
				}
				catch (IOException e) {
						log.error(e);
						ReflectionUtils.rethrowRuntimeException(e);
				}
		}

		@EventListener(ContextStoppedEvent.class)
		public void stop(ContextStoppedEvent stopped) {
				this.server.shutdown();
		}

		private static void logMethod(String method) {
				log.info(Application.class.getName() + "#" + method);
		}

		@Override
		public void run() {
				try {
						this.server.awaitTermination();
				}
				catch (InterruptedException e) {
						ReflectionUtils.rethrowRuntimeException(e);
				}
		}
}