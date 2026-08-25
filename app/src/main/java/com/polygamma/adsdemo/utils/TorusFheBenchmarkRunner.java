package com.polygamma.adsdemo.utils;

import com.polygamma.adsdemo.conf.BenchmarkConfig;

import org.polygamma.android.origin.crypt.Csprng;
import org.polygamma.android.origin.crypt.TorusFhe;
import org.polygamma.android.origin.util.Bits;

import java.util.Arrays;
import java.util.Random;

import static org.polygamma.android.origin.util.Bits.storeLong;
import static org.polygamma.android.origin.crypt.UnsignedMath.divideCeil;

public class TorusFheBenchmarkRunner {

    private static final long RANDOM_SEED = 44L;

    public BenchmarkResult run(
            BenchmarkConfig config
    ) throws Exception {

        /*
         * 1. Correctness
         */
        boolean correctnessPassed =
                runCorrectnessTest(config);

        if (!correctnessPassed) {
            BenchmarkResult result =
                    new BenchmarkResult();

            result.setCorrectnessPassed(false);

            return result;
        }

        /*
         * 2. Prepare benchmark environment.
         *
         * Key generation is intentionally outside
         * the timed benchmark.
         */
        BenchmarkContext context =
                createBenchmarkContext(config);

        /*
         * 3. Warmup
         */
        for (int i = 0;
             i < config.getWarmupIterations();
             i++) {

            executeOperation(config, context);
        }

        /*
         * 4. Benchmark
         */
        int iterations =
                config.getBenchmarkIterations();

        long[] samples =
                new long[iterations];

        long totalStart =
                System.nanoTime();

        for (int i = 0; i < iterations; i++) {

            context.plaintext =
                    i % context.plaintextSpace;

            long start =
                    System.nanoTime();

            executeOperation(
                    config,
                    context
            );

            long end =
                    System.nanoTime();

            if (config.getMode() == BenchmarkConfig.Mode.FULL
                    && context.decrypted != context.plaintext) {

                throw new IllegalStateException(
                        "FHE round-trip failed at iteration "
                                + i
                                + ": plaintext="
                                + context.plaintext
                                + ", decrypted="
                                + context.decrypted
                );
            }

            samples[i] = end - start;
        }

        long totalEnd =
                System.nanoTime();

        /*
         * 5. Calculate statistics
         */
        Arrays.sort(samples);

        BenchmarkResult result =
                new BenchmarkResult();

        result.setCorrectnessPassed(true);

        result.setMinNs(samples[0]);

        result.setMaxNs(
                samples[samples.length - 1]
        );

        result.setAvgNs(
                calculateAverage(samples)
        );

        result.setP50Ns(
                percentile(samples, 50)
        );

        result.setP95Ns(
                percentile(samples, 95)
        );

        result.setTotalNs(
                totalEnd - totalStart
        );

        return result;
    }

    private boolean runCorrectnessTest(
            BenchmarkConfig config
    ) throws Exception {

        /*
         * This follows TorusFheTest.testEncryptAndDecrypt().
         */

        BenchmarkContext context =
                createBenchmarkContext(config);

        TorusFhe fhe =
                context.fhe;

        int plaintextCount =
                fhe.messageModulus()
                        * fhe.carryModulus();

        /*
         * Test every valid plaintext value.
         */
        for (int plaintext = 0;
             plaintext < plaintextCount;
             plaintext++) {

            long ctBody =
                    fhe.encrypt(
                            context.buffer,
                            context.ctMaskOffset,
                            context.buffer,
                            context.pkMaskOffset,
                            context.buffer,
                            context.pkBodyOffset,
                            plaintext
                    );

            long decrypted =
                    fhe.decrypt(
                            context.buffer,
                            context.skOffset,
                            context.buffer,
                            context.ctMaskOffset,
                            ctBody
                    );

            if (decrypted != plaintext) {
                return false;
            }
        }

        /*
         * Make sure the key material wasn't modified.
         */
        if (!Arrays.equals(
                context.secretKey,
                Arrays.copyOfRange(
                        context.buffer,
                        context.skOffset,
                        context.skOffset
                                + context.secretKey.length
                )
        )) {
            return false;
        }

        if (!Arrays.equals(
                context.pkMask,
                Arrays.copyOfRange(
                        context.buffer,
                        context.pkMaskOffset,
                        context.pkMaskOffset
                                + context.pkMask.length
                )
        )) {
            return false;
        }

        if (!Arrays.equals(
                context.pkBody,
                Arrays.copyOfRange(
                        context.buffer,
                        context.pkBodyOffset,
                        context.pkBodyOffset
                                + context.pkBody.length
                )
        )) {
            return false;
        }

        return true;
    }

    private void executeOperation(
            BenchmarkConfig config,
            BenchmarkContext context
    ) throws Exception {

        context.plaintext =
                config.getBenchmarkIterations() % context.plaintextSpace;

        switch (config.getMode()) {

            case ENCRYPT:
                benchmarkEncrypt(context);
                break;

            case DECRYPT:
                benchmarkDecrypt(context);
                break;

            case FULL:
                benchmarkFullRoundTrip(context);
                break;
        }
    }

    private void benchmarkEncrypt(
            BenchmarkContext context
    ) throws Exception {

        /*
         * Real TorusFhe Encrypt API.
         *
         * The returned long is the ciphertext body.
         * The ciphertext mask is written into ctMask.
         */
        context.ctBody =
                context.fhe.encrypt(
                        context.buffer,
                        context.ctMaskOffset,
                        context.buffer,
                        context.pkMaskOffset,
                        context.buffer,
                        context.pkBodyOffset,
                        context.plaintext
                );
    }

    private void benchmarkDecrypt(
            BenchmarkContext context
    ) throws Exception {

        /*
         * Decrypt the ciphertext prepared before
         * the benchmark.
         */
        context.decrypted =
                context.fhe.decrypt(
                        context.buffer,
                        context.skOffset,
                        context.buffer,
                        context.ctMaskOffset,
                        context.ctBody
                );
    }

    private void benchmarkFullRoundTrip(
            BenchmarkContext context
    ) throws Exception {
        context.ctBody =
                context.fhe.encrypt(
                        context.buffer,
                        context.ctMaskOffset,
                        context.buffer,
                        context.pkMaskOffset,
                        context.buffer,
                        context.pkBodyOffset,
                        context.plaintext
                );
        context.decrypted =
                context.fhe.decrypt(
                        context.buffer,
                        context.skOffset,
                        context.buffer,
                        context.ctMaskOffset,
                        context.ctBody
                );

        if (context.decrypted != context.plaintext) {
            throw new IllegalStateException(
                    "FHE round-trip failed: plaintext="
                            + context.plaintext
                            + ", decrypted="
                            + context.decrypted
            );
        }
    }

    private BenchmarkContext createBenchmarkContext(
            BenchmarkConfig config
    ) {

        /*
         * Use the same deterministic seed strategy
         * as TorusFheTest.
         */
        byte[] seed =
                new byte[Csprng.INPUT_ENTROPY_SIZE];

        Random random =
                new Random(RANDOM_SEED);

        random.nextBytes(seed);

        Csprng csprng =
                Csprng.ofSeed(
                        seed,
                        0,
                        seed.length
                );

        TorusFhe fhe =
                TorusFhe.ofDimension(
                        config.getN(),
                        config.getMsgMod(),
                        config.getCarryMod(),
                        config.getLogNoiseB(),
                        csprng
                );

        int plaintextSpace =
                fhe.messageModulus()
                        * fhe.carryModulus();

        int plaintext =
                plaintextSpace > 1 ? 1 : 0;

        /*
         * Same basic size calculations as TorusFheTest.
         */
        int skSize =
                fhe.sizeOfBinaryVector();

        int scalarSize =
                fhe.sizeOfScalarVector();

        byte[] secretKey =
                new byte[skSize];

        byte[] pkMask =
                new byte[scalarSize];

        byte[] pkBody =
                new byte[scalarSize];

        /*
         * Generate key material.
         */
        fhe.generateSecretKey(
                secretKey,
                0
        );

        fhe.generatePublicKeyMask(
                pkMask,
                0,
                fhe.noiseGenerator.split()
        );

        fhe.generatePublicKeyBody(
                pkBody,
                0,
                pkMask,
                0,
                secretKey,
                0
        );

        /*
         * Buffer layout:
         *
         * [sk][pkMask][pkBody][ctMask]
         */
        int skOffset = 0;

        int pkMaskOffset =
                skOffset + skSize;

        int pkBodyOffset =
                pkMaskOffset + scalarSize;

        int ctMaskOffset =
                pkBodyOffset + scalarSize;

        byte[] buffer =
                new byte[
                        ctMaskOffset + scalarSize
                        ];

        System.arraycopy(
                secretKey,
                0,
                buffer,
                skOffset,
                skSize
        );

        System.arraycopy(
                pkMask,
                0,
                buffer,
                pkMaskOffset,
                scalarSize
        );

        System.arraycopy(
                pkBody,
                0,
                buffer,
                pkBodyOffset,
                scalarSize
        );

        /*
         * Prepare one ciphertext for the
         * Decrypt benchmark.
         *
         * This happens BEFORE timing starts.
         */
        long ctBody =
                fhe.encrypt(
                        buffer,
                        ctMaskOffset,
                        buffer,
                        pkMaskOffset,
                        buffer,
                        pkBodyOffset,
                        plaintext
                );

        return new BenchmarkContext(
                fhe,
                buffer,
                secretKey,
                pkMask,
                pkBody,
                skOffset,
                pkMaskOffset,
                pkBodyOffset,
                ctMaskOffset,
                plaintextSpace,
                plaintext,
                ctBody
        );
    }

    private double calculateAverage(
            long[] values
    ) {

        long sum = 0;

        for (long value : values) {
            sum += value;
        }

        return (double) sum / values.length;
    }

    private double percentile(
            long[] sortedValues,
            int percentile
    ) {

        if (sortedValues.length == 0) {
            return 0;
        }

        double index =
                percentile / 100.0
                        * (sortedValues.length - 1);

        int lower =
                (int) Math.floor(index);

        int upper =
                (int) Math.ceil(index);

        if (lower == upper) {
            return sortedValues[lower];
        }

        double fraction =
                index - lower;

        return sortedValues[lower]
                + fraction
                * (
                sortedValues[upper]
                        - sortedValues[lower]
        );
    }

    private static final class BenchmarkContext {

        final TorusFhe fhe;

        final byte[] buffer;

        final byte[] secretKey;
        final byte[] pkMask;
        final byte[] pkBody;

        final int skOffset;
        final int pkMaskOffset;
        final int pkBodyOffset;
        final int ctMaskOffset;

        final int plaintextSpace;
        long ctBody;
        long decrypted;
        int plaintext;

        BenchmarkContext(
                TorusFhe fhe,
                byte[] buffer,
                byte[] secretKey,
                byte[] pkMask,
                byte[] pkBody,
                int skOffset,
                int pkMaskOffset,
                int pkBodyOffset,
                int ctMaskOffset,
                int plaintextSpace,
                int plaintext,
                long ctBody
        ) {
            this.fhe = fhe;
            this.buffer = buffer;
            this.secretKey = secretKey;
            this.pkMask = pkMask;
            this.pkBody = pkBody;
            this.skOffset = skOffset;
            this.pkMaskOffset = pkMaskOffset;
            this.pkBodyOffset = pkBodyOffset;
            this.ctMaskOffset = ctMaskOffset;
            this.plaintextSpace = plaintextSpace;
            this.plaintext = plaintext;
            this.ctBody = ctBody;
        }
    }
}