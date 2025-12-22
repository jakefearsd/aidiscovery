package com.jakefear.aidiscovery;

import com.jakefear.aidiscovery.cli.AiDiscoveryCommand;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication
public class AiDiscoveryApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(AiDiscoveryApplication.class, args)));
    }

    @Bean
    public CommandLineRunner commandLineRunner(ObjectProvider<AiDiscoveryCommand> commandProvider,
                                                ObjectProvider<IFactory> factoryProvider) {
        return args -> {
            AiDiscoveryCommand command = commandProvider.getObject();
            IFactory factory = factoryProvider.getObject();
            exitCode = new CommandLine(command, factory).execute(args);
        };
    }

    @Bean
    public ExitCodeGenerator exitCodeGenerator() {
        return () -> exitCode;
    }

    private int exitCode;
}
