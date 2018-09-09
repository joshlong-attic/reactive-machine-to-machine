package com.example.helloworld.service;


import com.example.helloworld.GreeterGrpc;
import com.example.helloworld.GreeterReply;
import com.example.helloworld.GreeterRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
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

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.*;

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

@Service
@Log4j2
class GreeterService extends GreeterGrpc.GreeterImplBase {

		private final ScheduledExecutorService executorService;

		GreeterService(ScheduledExecutorService executorService) {
				this.executorService = executorService;
		}

		@Override
		public void sayHello(GreeterRequest request, StreamObserver<GreeterReply> response) {
				Runnable todo = () -> {
						String message = "hello " + request.getName() + " @ " + Instant.now().toString() + "!";
						log.info("message: " + message);
						response.onNext(GreeterReply.newBuilder().setMessage(message).build());
				};
				ScheduledFuture<?> fixedRate = this.executorService.scheduleAtFixedRate(todo, 0, 1, TimeUnit.SECONDS);
				Runnable done = () -> {
						fixedRate.cancel(false);
						log.info("cancelling the " + this.executorService.getClass().getName() + ".");
						response.onCompleted();
				};
				this.executorService.schedule(done, 10, TimeUnit.SECONDS);
		}
}
