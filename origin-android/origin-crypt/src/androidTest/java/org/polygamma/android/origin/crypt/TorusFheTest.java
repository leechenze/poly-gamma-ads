// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.crypt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.polygamma.android.origin.crypt.UnsignedMath.divideCeil;
import static org.polygamma.android.origin.util.Bits.loadLong;
import static org.polygamma.android.origin.util.Bits.storeLong;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.polygamma.android.origin.util.Bits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * {@link TorusFhe} tests.
 */
@RunWith(Parameterized.class)
public class TorusFheTest {

	private static final class TestCompactListEncryption extends TorusFhe.CompactListEncryption {

		private static final int NUM_PAD = 3;

		final List<byte[]> ciphertextBins;
		final byte[] padding;
		final byte[] buffer;
		final int bufferCiphertextMaskOffset;
		final int bufferCiphertextBodyListOffset;
		int numWriteCiphertextBody;

		TestCompactListEncryption(
			TorusFhe engine,
			@TorusFhe.ScalarVector byte[] pkMask, int pkMaskOff,
			@TorusFhe.ScalarVector byte[] pkBody, int pkBodyOff,
			Random rand
		) {
			super(engine, pkMask, pkMaskOff, pkBody, pkBodyOff);

			int ns = engine.sizeOfScalarVector();
			byte[] pad = new byte[NUM_PAD];
			int buffCtMaskOff = NUM_PAD;
			int buffCtBodyListOff = buffCtMaskOff + ns + NUM_PAD;
			byte[] buff = new byte[buffCtBodyListOff + ns + NUM_PAD];

			rand.nextBytes(pad);
			System.arraycopy(pad, 0, buff, 0, NUM_PAD);
			System.arraycopy(pad, 0, buff, buffCtMaskOff + ns, NUM_PAD);
			System.arraycopy(pad, 0, buff, buffCtBodyListOff + ns, NUM_PAD);
			this.ciphertextBins = new ArrayList<>();
			this.padding = pad;
			this.buffer = buff;
			this.bufferCiphertextMaskOffset = buffCtMaskOff;
			this.bufferCiphertextBodyListOffset = buffCtBodyListOff;
		}

		@Override
		protected void beginEncryptingBin() {
			super.setCurrentCiphertextMask(this.buffer, this.bufferCiphertextMaskOffset);
			this.numWriteCiphertextBody = 0;
		}

		@Override
		protected void writeCiphertextBody(int i, long ctBody) {
			assertEquals(this.numWriteCiphertextBody++, i);
			storeLong(this.buffer, this.bufferCiphertextBodyListOffset + (i * 8), ctBody);
		}

		@Override
		protected void
		writeCiphertextMask(@TorusFhe.ScalarVector byte[] ctMask, int ctMaskOff, int ptLen) {
			int ns = super.engine().sizeOfScalarVector();

			assertSame(this.buffer, ctMask);
			assertEquals(ctMaskOff, this.bufferCiphertextMaskOffset);
			assertEquals(this.numWriteCiphertextBody, ptLen);
			assertArrayEquals(this.padding, Arrays.copyOfRange(this.buffer, 0, NUM_PAD));
			assertArrayEquals(this.padding, Arrays.copyOfRange(
				this.buffer,
				this.bufferCiphertextMaskOffset + ns,
				this.bufferCiphertextMaskOffset + ns + NUM_PAD
			));
			assertArrayEquals(this.padding, Arrays.copyOfRange(
				this.buffer,
				this.bufferCiphertextBodyListOffset + ns,
				this.bufferCiphertextBodyListOffset + ns + NUM_PAD
			));

			int nb = this.numWriteCiphertextBody * 8;
			byte[] bin = new byte[ns + nb];

			System.arraycopy(this.buffer, this.bufferCiphertextMaskOffset, bin, 0, ns);
			System.arraycopy(this.buffer, this.bufferCiphertextBodyListOffset, bin, ns, nb);
			this.numWriteCiphertextBody = 0;
			this.ciphertextBins.add(bin);
		}
	}

	private static final class TestCompactListDecryption extends TorusFhe.CompactListDecryption {

		private static final int NUM_PAD = 3;

		final List<byte[]> bins;
		final byte[] padding;
		final byte[] buffer;
		final int bufferCiphertextMaskOffset;
		final int bufferCiphertextBodyListOffset;
		int numCiphertext;
		int numReadCiphertextBody;

		TestCompactListDecryption(
			TorusFhe engine, @TorusFhe.BinaryVector byte[] sk, int skOff,
			List<byte[]> bins, Random rand
		) {
			super(engine, sk, skOff);

			int ns = engine.sizeOfScalarVector();
			byte[] pad = new byte[NUM_PAD];
			int buffCtMaskOff = NUM_PAD;
			int buffCtBodyListOff = buffCtMaskOff + ns + NUM_PAD;
			byte[] buff = new byte[buffCtBodyListOff + ns + NUM_PAD];

			rand.nextBytes(pad);
			System.arraycopy(pad, 0, buff, 0, NUM_PAD);
			System.arraycopy(pad, 0, buff, buffCtMaskOff + ns, NUM_PAD);
			System.arraycopy(pad, 0, buff, buffCtBodyListOff + ns, NUM_PAD);

			this.bins = bins;
			this.padding = pad;
			this.buffer = buff;
			this.bufferCiphertextMaskOffset = buffCtMaskOff;
			this.bufferCiphertextBodyListOffset = buffCtBodyListOff;
		}

		@Override
		protected void beginDecryptingBin() {
			assertFalse(this.bins.isEmpty());

			byte[] bin = this.bins.remove(0);
			int ns = super.engine().sizeOfScalarVector();
			int nb = bin.length - ns;

			System.arraycopy(bin, 0, this.buffer, this.bufferCiphertextMaskOffset, ns);
			System.arraycopy(bin, ns, this.buffer, this.bufferCiphertextBodyListOffset, nb);
			this.numCiphertext = nb / 8;
			this.numReadCiphertextBody = 0;
			super.setCurrentCiphertextMask(this.buffer, this.bufferCiphertextMaskOffset);
		}

		@Override
		protected long readCiphertextBody(int i) {
			assertTrue(i >= 0 && i < this.numCiphertext);
			assertEquals(this.numReadCiphertextBody++, i);
			return loadLong(this.buffer, this.bufferCiphertextBodyListOffset + (i * 8));
		}
	}

	@Parameterized.Parameters(name = "n={0},msgMod={1},carryMod={2},logNoiseB={3}")
	public static Iterable<Object[]> generateParameters() {
		return Arrays.asList(
			new Object[] {  256, 2, 2, 46 },
			new Object[] {  256, 4, 4, 46 },
			new Object[] {  256, 4, 1, 46 },
			new Object[] {  256, 8, 1, 46 },
			new Object[] {  512, 2, 2, 46 },
			new Object[] {  512, 4, 4, 46 },
			new Object[] { 1024, 2, 2, 17 },
			new Object[] { 1024, 4, 4, 17 },
			new Object[] { 1024, 8, 1, 17 },
			new Object[] { 2048, 2, 2, 17 },
			new Object[] { 2048, 4, 4, 17 },
			new Object[] { 2048, 8, 1, 17 }
		);
	}

	private final Random random;
	private final TorusFhe fhe;

	public TorusFheTest(int n, int msgMod, int carryMod, int logNoiseB) {
		byte[] seed = new byte[Csprng.INPUT_ENTROPY_SIZE];

		this.random = new Random(44);
		this.random.nextBytes(seed);
		this.fhe = TorusFhe.ofDimension(
			n, msgMod, carryMod,
			logNoiseB, Csprng.ofSeed(seed, 0, seed.length)
		);

		assertEquals(n, this.fhe.dimension());
		assertEquals(n * 8, this.fhe.sizeOfScalarVector());
		assertEquals(divideCeil(n, 8), this.fhe.sizeOfBinaryVector());
		assertEquals(msgMod, this.fhe.messageModulus());
		assertEquals(carryMod, this.fhe.carryModulus());
		assertEquals(logNoiseB, this.fhe.logNoiseBound);
	}

	private long[] newRandomScalarCoefficients() {
		long[] rv = new long[this.fhe.dimension()];

		for (int i = 0; i < rv.length; i++)
			rv[i] = this.random.nextLong();
		return rv;
	}

	private byte[] newRandomBinaryCoefficients() {
		byte[] rv = new byte[this.fhe.sizeOfBinaryVector()];

		this.random.nextBytes(rv);
		return rv;
	}

	private long[] binarySrncAndAccumulateNoise(long[] lhs, byte[] rhs) {
		int n = this.fhe.dimension();
		int[] phi1 = new int[n];
		long[] full = new long[n + n];
		long[] prod = new long[n];
		Csprng noiseGen = this.fhe.noiseGenerator.split();
		int numNoiseBytes = divideCeil(this.fhe.logNoiseBound + 2, 8);

		Bits.expandBitsReverse(phi1, 0, rhs, 0, n);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++)
				full[i + j] += lhs[i] * phi1[j];
		}
		for (int i = 0; i < n; i++) {
			prod[i] =
				this.fhe.noiseScalarOfUniform(noiseGen.nextInteger(numNoiseBytes)) +
				(full[i] - full[i + n]);
		}
		return prod;
	}

	@Test
	public void testNoiseScalarOfUniform() {
		long max = 1L << this.fhe.logNoiseBound;
		long min = -max;

		for (int i = 0; i < 10000; i++) {
			long a = this.random.nextLong();
			long b = this.fhe.noiseScalarOfUniform(a);

			assertTrue(b >= min && b <= max);
		}
	}

	@Test
	public void testBinarySrncAndAccumulateNoise() {
		int n = this.fhe.dimension();
		int ns = this.fhe.sizeOfScalarVector();
		int nb = this.fhe.sizeOfBinaryVector();
		long[] lhs = this.newRandomScalarCoefficients();
		byte[] rhs = this.newRandomBinaryCoefficients();
		long[] prod = this.binarySrncAndAccumulateNoise(lhs, rhs);
		// [pad,prod,pad,lhs,pad,rhs,pad]
		byte[] pad = new byte[3];
		int bufferProdOff = pad.length;
		int bufferLhsOff = bufferProdOff + ns + pad.length;
		int bufferRhsOff = bufferLhsOff + ns + pad.length;
		byte[] buffer = new byte[bufferRhsOff + nb + pad.length];

		this.random.nextBytes(pad);
		System.arraycopy(pad, 0, buffer, 0, pad.length);
		System.arraycopy(pad, 0, buffer, bufferProdOff + ns, pad.length);
		System.arraycopy(pad, 0, buffer, bufferLhsOff + ns, pad.length);
		System.arraycopy(pad, 0, buffer, bufferRhsOff + nb, pad.length);
		System.arraycopy(rhs, 0, buffer, bufferRhsOff, nb);
		for (int i = 0; i < n; i++)
			storeLong(buffer, bufferLhsOff + i * 8, lhs[i]);

		assertThrows(
			IndexOutOfBoundsException.class,
			() -> this.fhe.binarySrncAndAccumulateNoise(
				buffer, buffer.length - 1,
				buffer, bufferLhsOff,
				buffer, bufferRhsOff
			)
		);
		assertThrows(
			IndexOutOfBoundsException.class,
			() -> this.fhe.binarySrncAndAccumulateNoise(
				buffer, bufferProdOff,
				buffer, buffer.length - 1,
				buffer, bufferRhsOff
			)
		);
		assertThrows(
			IndexOutOfBoundsException.class,
			() -> this.fhe.binarySrncAndAccumulateNoise(
				buffer, bufferProdOff,
				buffer, bufferLhsOff,
				buffer, buffer.length - 1
			)
		);
		this.fhe.binarySrncAndAccumulateNoise(
			buffer, bufferProdOff,
			buffer, bufferLhsOff,
			buffer, bufferRhsOff
		);

		for (int i = 0; i < n; i++) {
			String is = Integer.toString(i);

			// make sure lhs wasn't modified
			assertEquals(is, lhs[i], loadLong(buffer, bufferLhsOff + i * 8));
			// make sure product is equal
			assertEquals(is, prod[i], loadLong(buffer, bufferProdOff + i * 8));
		}
		// make sure rhs wasn't modified
		assertArrayEquals(rhs, Arrays.copyOfRange(buffer, bufferRhsOff, bufferRhsOff + nb));
		// make sure padding wasn't screwed up
		assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, pad.length));
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferProdOff + ns, bufferProdOff + ns + pad.length)
		);
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferLhsOff + ns, bufferLhsOff + ns + pad.length)
		);
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferRhsOff + nb, bufferRhsOff + nb + pad.length)
		);
	}

	@Test
	public void testBinaryDotProduct() {
		int n = this.fhe.dimension();
		int ns = this.fhe.sizeOfScalarVector();
		int nb = this.fhe.sizeOfBinaryVector();
		long[] x = this.newRandomScalarCoefficients();
		byte[] y = this.newRandomBinaryCoefficients();
		int[] yE = new int[n];
		long exp = 0L;

		Bits.expandBits(yE, 0, y, 0, n);
		for (int i = 0; i < n; i++)
			exp += x[i] * yE[i];

		// [pad,x,pad,y,pad]
		byte[] pad = new byte[3];
		int bufferXOff = pad.length;
		int bufferYOff = bufferXOff + ns + pad.length;
		byte[] buffer = new byte[bufferYOff + nb + pad.length];

		this.random.nextBytes(pad);
		System.arraycopy(pad, 0, buffer, 0, pad.length);
		System.arraycopy(pad, 0, buffer, bufferXOff + ns, pad.length);
		System.arraycopy(pad, 0, buffer, bufferYOff + nb, pad.length);
		for (int i = 0; i < n; i++)
			storeLong(buffer, bufferXOff + i * 8, x[i]);
		System.arraycopy(y, 0, buffer, bufferYOff, nb);

		assertEquals(exp, this.fhe.binaryDotProduct(buffer, bufferXOff, buffer, bufferYOff));
		for (int i = 0; i < n; i++)
			assertEquals(Integer.toString(i), x[i], loadLong(buffer, bufferXOff + i * 8));
		assertArrayEquals(y, Arrays.copyOfRange(buffer, bufferYOff, bufferYOff + nb));
		assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, pad.length));
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferXOff + ns, bufferXOff + ns + pad.length)
		);
		assertArrayEquals(
			pad,
			Arrays.copyOfRange(buffer, bufferYOff + nb, bufferYOff + nb + pad.length)
		);
	}

	@Test
	public void testEncryptAndDecrypt() {
		byte[] sk = new byte[this.fhe.sizeOfBinaryVector()];
		byte[] pkMask = new byte[this.fhe.sizeOfScalarVector()];
		byte[] pkBody = new byte[this.fhe.sizeOfScalarVector()];
		// [pad,sk,pad,pkMask,pad,pkBody,pad,ctMask,pad]
		byte[] pad = new byte[3];
		int bufferSkOff = pad.length;
		int bufferPkMaskOff = bufferSkOff + sk.length + pad.length;
		int bufferPkBodyOff = bufferPkMaskOff + pkMask.length + pad.length;
		int bufferCtMaskOff = bufferPkBodyOff + pkBody.length + pad.length;
		byte[] buffer = new byte[bufferCtMaskOff + pkMask.length + pad.length];

		this.random.nextBytes(pad);
		System.arraycopy(pad, 0, buffer, 0, pad.length);
		System.arraycopy(pad, 0, buffer, bufferSkOff + sk.length, pad.length);
		System.arraycopy(pad, 0, buffer, bufferPkMaskOff + pkMask.length, pad.length);
		System.arraycopy(pad, 0, buffer, bufferPkBodyOff + pkBody.length, pad.length);
		System.arraycopy(pad, 0, buffer, bufferCtMaskOff + pkMask.length, pad.length);

		this.fhe.generateSecretKey(sk, 0);
		this.fhe.generatePublicKeyMask(pkMask, 0, this.fhe.noiseGenerator.split());
		this.fhe.generatePublicKeyBody(pkBody, 0, pkMask, 0, sk, 0);
		System.arraycopy(sk, 0, buffer, bufferSkOff, sk.length);
		System.arraycopy(pkMask, 0, buffer, bufferPkMaskOff, pkMask.length);
		System.arraycopy(pkBody, 0, buffer, bufferPkBodyOff, pkBody.length);

		for (int pt = this.fhe.messageModulus() * this.fhe.carryModulus(); pt-- > 0;) {
			long ctBody = this.fhe.encrypt(
				buffer, bufferCtMaskOff,
				buffer, bufferPkMaskOff,
				buffer, bufferPkBodyOff,
				pt
			);
			long gotPt = this.fhe.decrypt(buffer, bufferSkOff, buffer, bufferCtMaskOff, ctBody);

			assertEquals(pt, gotPt);
		}
		// make sure sk and pk parts weren't modified
		assertArrayEquals(sk, Arrays.copyOfRange(buffer, bufferSkOff, bufferSkOff + sk.length));
		assertArrayEquals(
			pkMask,
			Arrays.copyOfRange(buffer, bufferPkMaskOff, bufferPkMaskOff + pkMask.length)
		);
		assertArrayEquals(
			pkBody,
			Arrays.copyOfRange(buffer, bufferPkBodyOff, bufferPkBodyOff + pkBody.length)
		);
		// make sure padding wasn't modified
		assertArrayEquals(pad, Arrays.copyOfRange(buffer, 0, pad.length));
		assertArrayEquals(pad, Arrays.copyOfRange(
			buffer,
			bufferSkOff + sk.length,
			bufferSkOff + sk.length + pad.length
		));
		assertArrayEquals(pad, Arrays.copyOfRange(
			buffer,
			bufferPkMaskOff + pkMask.length,
			bufferPkMaskOff + pkMask.length + pad.length
		));
		assertArrayEquals(pad, Arrays.copyOfRange(
			buffer,
			bufferPkBodyOff + pkBody.length,
			bufferPkBodyOff + pkBody.length + pad.length
		));
		assertArrayEquals(pad, Arrays.copyOfRange(
			buffer,
			bufferCtMaskOff + pkMask.length,
			bufferCtMaskOff + pkMask.length + pad.length
		));
	}

	@Test
	public void testCompactListEncryptAndDecrypt() {
		int n = this.fhe.dimension();
		int bSz = this.fhe.sizeOfBinaryVector();
		int sSz = this.fhe.sizeOfScalarVector();
		// [pad,sk,pad,pkMask,pad,pkBody,pad]
		byte[] pad = new byte[3];
		int bufferSkOff = pad.length;
		int bufferPkMaskOff = bufferSkOff + bSz + pad.length;
		int bufferPkBodyOff = bufferPkMaskOff + sSz + pad.length;
		byte[] buffer = new byte[bufferPkBodyOff + sSz + pad.length];

		this.random.nextBytes(pad);
		System.arraycopy(pad, 0, buffer, 0, pad.length);
		System.arraycopy(pad, 0, buffer, bufferSkOff + bSz, pad.length);
		System.arraycopy(pad, 0, buffer, bufferPkMaskOff + sSz, pad.length);
		System.arraycopy(pad, 0, buffer, bufferPkBodyOff + sSz, pad.length);
		this.fhe.generateSecretKey(buffer, bufferSkOff);
		this.fhe.generatePublicKeyMask(buffer, bufferPkMaskOff, this.fhe.noiseGenerator.split());
		this.fhe.generatePublicKeyBody(
			buffer, bufferPkBodyOff,
			buffer, bufferPkMaskOff,
			buffer, bufferSkOff
		);

		for (int i = 1; i < (n * 2); i++) {
			byte[] expPt = new byte[i];
			int[] expIntPt = new int[i];
			long[] expLongPt = new long[i];

			for (int j = 0; j < i; j++) {
				expPt[j] = (byte) this.random.nextInt(
					this.fhe.messageModulus() *
					this.fhe.carryModulus()
				);
				expIntPt[j] = this.random.nextInt();
				expLongPt[j] = this.random.nextLong();
			}

			TestCompactListEncryption enc = new TestCompactListEncryption(
				this.fhe,
				buffer, bufferPkMaskOff,
				buffer, bufferPkBodyOff,
				this.random
			);
			int expPtLen =
				expPt.length +
				this.fhe.plaintextCountOfDecomposedInt() * expIntPt.length +
				this.fhe.plaintextCountOfDecomposedLong() * expLongPt.length;
			int expBinLen = this.fhe.ciphertextMaskListCountOf(expPtLen);

			for (int j = 0; j < i; j++) {
				enc.encrypt(expPt[j] & 0xffL)
					.decomposeAndEncryptUnsignedInt(expIntPt[j])
					.decomposeAndEncryptUnsignedLong(expLongPt[j]);
			}

			enc.flush();
			// flushing again should have no impact
			enc.flush();

			// number of ciphertext bins should be deterministic
			assertEquals(expBinLen, enc.ciphertextBins.size());

			TestCompactListDecryption dec = new TestCompactListDecryption(
				this.fhe,
				buffer, bufferSkOff,
				enc.ciphertextBins,
				this.random
			);

			for (int j = 0; j < i; j++) {
				assertEquals(expPt[j] & 0xffL, dec.decrypt());
				assertEquals(expIntPt[j], dec.decryptAndRecomposeUnsignedInt());
				assertEquals(expLongPt[j], dec.decryptAndRecomposeUnsignedLong());
			}
			assertTrue(dec.bins.isEmpty());
		}
	}
}
