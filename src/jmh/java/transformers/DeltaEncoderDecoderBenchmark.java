package transformers;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DeltaEncoderDecoderBenchmark {

    @State(Scope.Thread)
    public static class MyState {
        @Param({"1000", "10000", "100000"})
        private int sampleSize;
        private  List<Number> samples;
        private  DeltaEncoderDecoder underBenchmark;
        private DeltaEncoderDecoder.Result<Number> result;

        @Setup(Level.Trial)
        public void setup() {
            underBenchmark = new DeltaEncoderDecoder();
            samples = new ArrayList<>();
            Random r = new Random();
            int limit = sampleSize;
            System.out.println("Generating random data points of size " + limit);
            List<Number> samples = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                samples.add(r.nextLong());
            }
            // precompute some encoded data before the benchmarking so that
            // decode can also be benchmarked
            result = underBenchmark.encode(samples);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            System.out.println("Cleaning up random samples");
            samples.clear();
            samples = null;
            result = null;
        }

        public List<Number> getSamples() {
            return samples;
        }

        public DeltaEncoderDecoder getUnderBenchmark() {
            return underBenchmark;
        }

        public DeltaEncoderDecoder.Result<Number> getEncoded() {
            return result;
        }
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Fork(value = 3, warmups = 2)
    @Warmup(iterations =  5, time = 60, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 4, time = 60, timeUnit = TimeUnit.MILLISECONDS)
    public void encode(MyState state) {
        state.getUnderBenchmark().encode(state.getSamples());
    }


    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Fork(value = 3, warmups = 2)
    @Warmup(iterations =  5, time = 60, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 4, time = 60, timeUnit = TimeUnit.MILLISECONDS)
    public void decode(MyState state) {
        state.getUnderBenchmark().decode(state.getEncoded());
    }

}
