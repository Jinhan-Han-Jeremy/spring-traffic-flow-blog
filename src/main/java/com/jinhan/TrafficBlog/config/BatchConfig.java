package com.jinhan.TrafficBlog.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
// Spring Batch Bean들을 주입받기 위해 사용
@EnableBatchProcessing
@Profile("batch")
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public BatchConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Job exampleJob() {
        return new JobBuilder("exampleJob", jobRepository)
                .start(exampleStep())
                .build();
    }

    @Bean
    public Step exampleStep() {
        return new StepBuilder("exampleStep", jobRepository)
                .tasklet(exampleTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet exampleTasklet() {
        return (contribution, chunkContext) -> {
            System.out.println("Hello, Spring Batch!");
            return RepeatStatus.FINISHED;
        };
    }
}