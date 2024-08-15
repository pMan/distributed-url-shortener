package com.pman.distributedurlshortener.jmh;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.pman.distributedurlshortener.server.CustomBase64Encoder;

@BenchmarkMode({ Mode.Throughput, Mode.AverageTime })
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = { "-Xms1G", "-Xmx2G" })
public class CustomBase64EncoderBenchmark {
    private static int SIZE;
    long[] data;

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder().include(CustomBase64EncoderBenchmark.class.getSimpleName()).forks(1).build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        SIZE = 1000_000;

        Random random = new Random();
        data = new long[SIZE];
        for (int i = 0; i < SIZE; i++) {
            data[i] = random.nextLong(SIZE);
        }
    }

    @Benchmark
    @Warmup(iterations = 3)
    @Measurement(iterations = 5)
    public void longToBase64(Blackhole bh) {
        for (int i = 0; i < SIZE; i++) {
            String s = CustomBase64Encoder.longToBase64(data[i]);
            bh.consume(s);
        }
    }

}
