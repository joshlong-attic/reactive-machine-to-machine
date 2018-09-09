package com.example.helloworld.client;

import com.example.helloworld.GreeterGrpc;
import com.example.helloworld.GreeterReply;
import com.example.helloworld.GreeterRequest;
import com.example.helloworld.ReactorGreeterGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Log4j2
@SpringBootApplication
public class Application implements ApplicationListener<ApplicationReadyEvent> {

		private final Executor executor = Executors.newFixedThreadPool(10);

		public static void main(String args[]) throws Exception {
				SpringApplication.run(Application.class, args);
				Thread.currentThread().join();
		}

		@Bean(destroyMethod = "shutdown")
		ManagedChannel channel() {
				return ManagedChannelBuilder
					.forAddress("localhost", 8000)
					.usePlaintext(true)
					.build();
		}

		@Bean
		GreeterGrpc.GreeterBlockingStub blockingStub() {
				return GreeterGrpc.newBlockingStub(channel());
		}

		@Bean
		ReactorGreeterGrpc.ReactorGreeterStub greeterStub() {
				return ReactorGreeterGrpc.newReactorStub(channel());
		}

		@Override
		public void onApplicationEvent(ApplicationReadyEvent event) {

				GreeterRequest josh = GreeterRequest.newBuilder().setName("josh").build();
				GreeterRequest jane = GreeterRequest.newBuilder().setName("jane").build();

				this.blockingStub().sayHello(jane).forEachRemaining(gr -> log.info("reply: " + gr.getMessage()));
				this.greeterStub().sayHello(josh).subscribe(reply -> log.info("reactor reply: " + reply.getMessage()));

		}
}
