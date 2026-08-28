/*
 * SPDX-License-Identifier: MIT OR Apache-2.0
 *
 * This is ported from `tfhe` [1].
 *
 * [1] https://github.com/zama-ai/tfhe-rs/tree/main/tfhe
 */

package org.polygamma.android.origin.crypt;

import static org.polygamma.android.origin.crypt.Prime32.P0;
import static org.polygamma.android.origin.crypt.Prime32.P01_INVERSE_MOD_P2;
import static org.polygamma.android.origin.crypt.Prime32.P0_BARRETT_FACTOR;
import static org.polygamma.android.origin.crypt.Prime32.P0_BITS;
import static org.polygamma.android.origin.crypt.Prime32.P0_CAN_USE_FAST_REDUCTION;
import static org.polygamma.android.origin.crypt.Prime32.P0_INVERSE_MOD_P1;
import static org.polygamma.android.origin.crypt.Prime32.P0_LIMB_COUNT;
import static org.polygamma.android.origin.crypt.Prime32.P0_RECIPROCAL128_HIGH;
import static org.polygamma.android.origin.crypt.Prime32.P0_RECIPROCAL128_LOW;
import static org.polygamma.android.origin.crypt.Prime32.P0_RECIPROCAL64;
import static org.polygamma.android.origin.crypt.Prime32.P1;
import static org.polygamma.android.origin.crypt.Prime32.P1_BARRETT_FACTOR;
import static org.polygamma.android.origin.crypt.Prime32.P1_BITS;
import static org.polygamma.android.origin.crypt.Prime32.P1_CAN_USE_FAST_REDUCTION;
import static org.polygamma.android.origin.crypt.Prime32.P1_LIMB_COUNT;
import static org.polygamma.android.origin.crypt.Prime32.P1_RECIPROCAL128_HIGH;
import static org.polygamma.android.origin.crypt.Prime32.P1_RECIPROCAL128_LOW;
import static org.polygamma.android.origin.crypt.Prime32.P1_RECIPROCAL64;
import static org.polygamma.android.origin.crypt.Prime32.P2;
import static org.polygamma.android.origin.crypt.Prime32.P2_BARRETT_FACTOR;
import static org.polygamma.android.origin.crypt.Prime32.P2_BITS;
import static org.polygamma.android.origin.crypt.Prime32.P2_CAN_USE_FAST_REDUCTION;
import static org.polygamma.android.origin.crypt.Prime32.P2_LIMB_COUNT;
import static org.polygamma.android.origin.crypt.Prime32.P2_RECIPROCAL128_HIGH;
import static org.polygamma.android.origin.crypt.Prime32.P2_RECIPROCAL128_LOW;
import static org.polygamma.android.origin.crypt.Prime32.P2_RECIPROCAL64;
import static org.polygamma.android.origin.crypt.UnsignedMath.divideCeil;
import static org.polygamma.android.origin.crypt.UnsignedMath.divideRound;
import static org.polygamma.android.origin.crypt.UnsignedMath.gt;
import static org.polygamma.android.origin.crypt.UnsignedMath.isPow2;
import static org.polygamma.android.origin.crypt.UnsignedMath.l;
import static org.polygamma.android.origin.crypt.UnsignedMath.log2;
import static org.polygamma.android.origin.crypt.UnsignedMath.multiplyMod;
import static org.polygamma.android.origin.crypt.UnsignedMath.u;
import static org.polygamma.android.origin.util.Bits.loadInt;
import static org.polygamma.android.origin.util.Bits.loadLong;
import static org.polygamma.android.origin.util.Bits.storeInt;
import static org.polygamma.android.origin.util.Bits.storeLong;

import androidx.annotation.IntRange;
import androidx.annotation.VisibleForTesting;

import org.polygamma.android.origin.util.Preconditions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Engine for performing Fully Homomorphic Encryption (FHE) over the real Torus.
 * <p>This implements a discretized Torus cryptosystem based on {@code TFHE Public-Key Encryption
 * Revisited}, by M. Joye. The underlying continuous torus interval, {@code [0, 1)}, is mapped
 * directly onto {@code long} values.
 *
 * @since 1.2
 * @see <a href="https://eprint.iacr.org/2018/421">TFHE: Fast Fully Homomorphic Encryption over the Torus</a>
 * @see <a href="https://eprint.iacr.org/2023/603">TFHE Public-Key Encryption Revisited</a>
 */
public final class TorusFhe {

	// The upper limit of the range is not a hard limit. We set it to ensure OOM doesn't occur.

	/**
	 * Minimum, inclusive, supported Learning With Errors (LWE) dimension.
	 *
	 * @since 1.2
	 * @see #dimension()
 	 */
	public static final int MIN_DIMENSION = 2;

	/**
	 * Maximum, inclusive, supported Learning With Errors (LWE) dimension.
	 *
	 * @since 1.2
	 * @see #dimension()
	 */
	public static final int MAX_DIMENSION = 32768;

	// Size, in bytes, of a scalar.
	private static final int SCALAR_SIZE = 8;

	// Size, in bits, of a scalar.
	private static final int BITS_PER_SCALAR = SCALAR_SIZE * 8;

	/**
	 * Marker for binary vectors.
	 * <p>Binary vectors are {@code byte} arrays whose individual bits form vector elements. The
	 * {@code byte} arrays are expected to have a length equal to {@link #sizeOfBinaryVector()}.
	 *
	 * @since 1.2
	 * @see #sizeOfBinaryVector()
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	public @interface BinaryVector {
	}

	/**
	 * Marker for scalar vectors.
	 * <p>Scalar vectors are {@code byte} arrays which contain unsigned 64-bit scalar elements,
	 * stored in native byte-order. The {@code byte} array backing these vectors are expected to
	 * have a length equal to {@link #sizeOfScalarVector()}.
	 *
	 * @since 1.2
	 * @see #sizeOfScalarVector()
	 */
	@Documented
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE_USE)
	public @interface ScalarVector {
	}

	/**
	 * Compact list encryption.
	 * <p>Compact list encryption splits a list of {@code n} plaintexts into {@code l} bins,
	 * where {@code n} and {@code l} are equal to the plaintext list length and {@code n}
	 * divided by the LWE {@linkplain TorusFhe#dimension() dimension}, rounded up, respectively.
	 * This encryption scheme results in {@code l} ciphertext masks, each with a size equal to
	 * {@link TorusFhe#sizeOfScalarVector()}. The {@link TorusFhe#ciphertextMaskListCountOf(int)}
	 * method of the {@linkplain #engine() owning} FHE engine can be used to determine {@code l},
	 * given some {@code n}.
	 * <h2>Implementation</h2>
	 * <p>Implementations are required to provide the buffer in which the ciphertext mask for some
	 * bin will be stored into, writing ciphertext bodies, and writing ciphertext masks. Before the
	 * encryption of a new bin begins, {@link #beginEncryptingBin()} is invoked. Implementations
	 * must {@linkplain #setCurrentCiphertextMask(byte[], int) set} the buffer in which the
	 * ciphertext mask will be stored into.
	 * <p>For each invocation of {@link #encrypt(long)} with some plaintext, {@link
	 * #writeCiphertextBody(int, long)} will be invoked, eventually with the ciphertext body of the
	 * respective plaintext, in the order the plaintext was encrypted. Once LWE dimension
	 * plaintexts are encrypted, {@link #writeCiphertextMask(byte[], int, int)} is invoked with the
	 * ciphertext mask buffer set within {@link #beginEncryptingBin()}. After a bin is completed,
	 * the next encryption operation will cause {@link #beginEncryptingBin()} to be invoked again.
	 *
	 * @since 1.2
	 */
	public static abstract class CompactListEncryption {

		private final TorusFhe engine;
		private final @ScalarVector byte[] publicKeyMask;
		private final @ScalarVector byte[] publicKeyBody;
		private final int publicKeyMaskOffset;
		private final int publicKeyBodyOffset;
		private int bufferedPlaintextCount;
		private int currentCiphertextMaskOffset;
		private @ScalarVector byte[] currentCiphertextMask;

		/**
		 * Construct new compact list encryption.
		 *
		 * @param engine engine to use for encryption
		 * @param pkMask mask of public key to encrypt with
		 * @param pkMaskOff offset, within {@code pkMask}, to begin loading from
		 * @param pkBody body of public key to encrypt with
		 * @param pkBodyOff offset, within {@code pkBody}, to begin loading from
		 * @throws IndexOutOfBoundsException {@code pkMaskOff} or {@code pkBodyOff} is negative,
		 * or, {@code pkMaskOff + n} or {@code pkBodyOff + n}, where {@code n} is equal to
		 * scalar vector {@linkplain TorusFhe#sizeOfScalarVector() size}, is greater than {@code
		 * pkMask.length} or {@code pkBody.length}, respectively
		 * @since 1.2
		 */
		public CompactListEncryption(
			TorusFhe engine,
			@ScalarVector byte[] pkMask, int pkMaskOff,
			@ScalarVector byte[] pkBody, int pkBodyOff
		) {
			int ns = engine.sizeOfScalarVector();

			this.engine = engine;
			this.publicKeyMask = pkMask;
			this.publicKeyMaskOffset =
				Preconditions.checkFromIndexSize(pkMaskOff, ns, pkMask.length);
			this.publicKeyBody = pkBody;
			this.publicKeyBodyOffset =
				Preconditions.checkFromIndexSize(pkBodyOff, ns, pkBody.length);
		}

		/**
		 * Engine underlying writer.
		 *
		 * @return underlying engine
		 * @since 1.2
		 */
		public final TorusFhe engine() {
			return this.engine;
		}

		/**
		 * Set the buffer the ciphertext mask for current bin will be stored into.
		 * <p>This must be invoked <i>only</i> while a bin is not being encrypted. This may be
		 * invoked once during construction, within {@link #beginEncryptingBin()}, or {@link
		 * #writeCiphertextMask(byte[], int, int)}.
		 *
		 * @param ctMask buffer to store ciphertext mask coefficients into
		 * @param ctMaskOff offset, within {@code ctMask}, to begin storing from
		 * @throws IllegalStateException bin is currently being encrypted
		 * @throws IndexOutOfBoundsException {@code ctMaskOff} is negative or {@code ctMaskOff + n},
		 * where {@code n} is equal to the {@linkplain TorusFhe#sizeOfScalarVector() scalar vector
		 * size} of the {@linkplain #engine() underlying engine}, is greater than {@code
		 * ctMask.length}
		 * @since 1.2
		 */
		protected final void setCurrentCiphertextMask(@ScalarVector byte[] ctMask, int ctMaskOff) {
			Preconditions.checkState(this.bufferedPlaintextCount == 0);
			Preconditions.checkFromIndexSize(
				ctMaskOff,
				this.engine.sizeOfScalarVector(),
				ctMask.length
			);
			this.currentCiphertextMask = ctMask;
			this.currentCiphertextMaskOffset = ctMaskOff;
		}

		/**
		 * Buffer ciphertext mask for current bin is being stored into.
		 *
		 * @return ciphertext mask buffer or {@code null}
		 * @since 1.2
		 */
		protected final @ScalarVector byte[] currentCiphertextMask() {
			return this.currentCiphertextMask;
		}

		/**
		 * Offset, within ciphertext mask buffer, that ciphertext mask coefficients are stored
		 * from.
		 *
		 * @return offset mask coefficients will be stored from
		 * @since 1.2
		 */
		protected final int currentCiphertextMaskOffset() {
			return this.currentCiphertextMaskOffset;
		}

		/**
		 * Begin encrypting next bin.
		 * <p>This is invoked whenever encryption of a new bin begins. Implementations should
		 * {@linkplain #setCurrentCiphertextMask(byte[], int) assign} the buffer in which
		 * the ciphertext mask for the current bin will be stored into. After this is invoked,
		 * it is guaranteed that {@link #writeCiphertextBody(int, long)} will be invoked one or
		 * more times, and, {@link #writeCiphertextMask(byte[], int, int)} will be invoked exactly
		 * once when encryption of the bin is complete.
		 *
		 * @since 1.2
		 */
		protected abstract void beginEncryptingBin();

		/**
		 * Write ciphertext body.
		 * <p>This is invoked one or more times <i>after</i> {@link #beginEncryptingBin()} is
		 * invoked. While this may not be invoked immediately for each invocation of {@link
		 * #encrypt(long)}, it is guaranteed that this will be invoked as many times as {@link
		 * #encrypt(long)} is invoked, with ciphertext bodies in order of the respective plaintext
		 * with which {@link #encrypt(long)} was invoked with. Thus {@code i} is guaranteed to be
		 * monotonically increasing, and equal to the index of the ciphertext within the
		 * <i>current</i> bin.
		 *
		 * @param i index of ciphertext within current bin
		 * @param ctBody ciphertext body
		 * @since 1.2
		 */
		protected abstract void writeCiphertextBody(int i, long ctBody);

		/**
		 * Write ciphertext mask.
		 * <p>This is invoked as soon as all ciphertext bodies for the current bin are {@linkplain
		 * #writeCiphertextBody(int, long) written}. The {@code ctMask} and {@code ctMaskOff}
		 * arguments are the same as the ciphertext mask buffer that was {@linkplain
		 * #setCurrentCiphertextMask(byte[], int) assigned} within {@link
		 * #beginEncryptingBin()}. The first {@linkplain TorusFhe#sizeOfScalarVector() n} bytes
		 * of {@code ctMask}, starting at position {@code ctMaskOff} (inclusive), will contain the
		 * mask coefficients for the respective bin.
		 *
		 * @param ctMask buffer to load ciphertext mask coefficients from
		 * @param ctMaskOff offset, within {@code ctMask}, to begin loading from
		 * @param ptLen number of plaintexts encrypted
		 * @since 1.2
		 */
		protected abstract void
		writeCiphertextMask(@ScalarVector byte[] ctMask, int ctMaskOff, int ptLen);

		// Move forward to next bin.
		private byte[] nextBin() {
			this.beginEncryptingBin();

			byte[] buff = this.currentCiphertextMask;

			Preconditions.checkState(buff != null);
			return buff;
		}

		/**
		 * Encrypt and write any buffered plaintexts which have been buffered.
		 * <p>This should be invoked only <i>once</i> after {@link #encrypt(long)} has been invoked
		 * for all plaintexts. Upon return, if there are any plaintexts within the bin that have
		 * not yet been encrypted, they will be encrypted and the final ciphertext mask will be
		 * written.
		 *
		 * @since 1.2
		 */
		public void flush() {
			int nPt = this.bufferedPlaintextCount;
			int n = this.engine.dimension;

			if (nPt < 1)
				return;

			byte[] r0 = new byte[n * 4];
			byte[] r1 = new byte[n * 4];
			byte[] r2 = new byte[n * 4];
			byte[] p0 = new byte[n * 4];
			byte[] p1 = new byte[n * 4];
			byte[] p2 = new byte[n * 4];
			byte[] buff = this.currentCiphertextMask;
			int buffOff = this.currentCiphertextMaskOffset;

			this.engine.noiseGenerator.nextBytes(r2, 0, this.engine.sizeOfBinaryVector());
			this.engine.prepareRhsForBinarySrnc(r0, r1, r2, r2, 0);

			/*
			 * `buff` at this point contains plaintexts we'll be encrypting. We'll go ahead and
			 * encrypt the plaintext that has been buffered, then we can store the ciphertext mask
			 * into `buff`.
			 *
			 * V(X) = U(X) * s(X) + E'(X) (mod X^{n}+1)
			 */
			this.engine.splitSrnc(
				p0, p1, p2,
				p0, 0, p1, 0, p2, 0,
				this.publicKeyBody, this.publicKeyBodyOffset,
				r0, r1, r2
			);
			for (int i = 0; i < nPt; i++) {
				long e = this.engine.nextNoiseScalar();
				long v = mergeSrncProductCoefficient(
					loadInt(p0, (n - 1 - i) * 4),
					loadInt(p1, (n - 1 - i) * 4),
					loadInt(p2, (n - 1 - i) * 4)
				);

				// c_{i} = v_{n - 1 - i} + e_{body,i} + m_{i} (mod q)
				this.writeCiphertextBody(i, v + e + loadScalar(buff, buffOff, i));
			}

			// Now we can store the ciphertext mask into `buff`.
			this.bufferedPlaintextCount = 0;
			// U(X) = A(X) * R(X) (mod X^{n} + 1)
			this.engine.splitSrncAndAccumulateNoise(
				buff, buffOff,
				r0, r1, p2,
				this.publicKeyMask, this.publicKeyMaskOffset,
				r0, r1, r2
			);
			this.writeCiphertextMask(buff, buffOff, nPt);
		}

		/**
		 * Encrypt plaintext.
		 *
		 * @param pt plaintext to encrypt
		 * @return {@code this}
		 * @throws IllegalStateException implementation is malformed
		 * @since 1.2
		 */
		public final CompactListEncryption encrypt(long pt) {
			byte[] buff = this.currentCiphertextMask;
			int i = this.bufferedPlaintextCount;

			if (i == 0)
				buff = this.nextBin();
			storeScalar(
				buff, this.currentCiphertextMaskOffset, i,
				this.engine.encodePlaintext(pt)
			);
			if (++this.bufferedPlaintextCount == this.engine.dimension)
				this.flush();
			return this;
		}

		/**
		 * Encrypt plaintexts of a decomposed {@code int}.
		 * <p>This decomposes {@code pt} into {@link TorusFhe#plaintextCountOfDecomposedInt() n}
		 * plaintexts and, {@linkplain #encrypt(long) encrypts} each decomposed plaintext.
		 *
		 * @param pt value to decompose
		 * @return {@code this}
		 * @throws IllegalStateException implementation is malformed
		 * @since 1.2
		 */
		public final CompactListEncryption decomposeAndEncryptUnsignedInt(int pt) {
			int nbits = this.engine.bitsPerMessage();
			int ptLen = this.engine.plaintextCountOfDecomposedInt();
			int ptMask = (1 << nbits) - 1;

			do {
				this.encrypt((pt & ptMask) & 0xffffffffL);
				pt >>>= nbits;
			} while (--ptLen > 0);
			return this;
		}

		/**
		 * Encrypt plaintexts of a decomposed {@code long}.
		 * <p>This decomposes {@code pt} into {@link TorusFhe#plaintextCountOfDecomposedLong() n}
		 * plaintexts and, {@linkplain #encrypt(long) encrypts} each decomposed plaintext.
		 *
		 * @param pt value to decompose
		 * @return {@code this}
		 * @throws IllegalStateException implementation is malformed
		 * @since 1.2
		 */
		public final CompactListEncryption decomposeAndEncryptUnsignedLong(long pt) {
			int nbits = this.engine.bitsPerMessage();
			int ptLen = this.engine.plaintextCountOfDecomposedLong();
			long ptMask = (1L << nbits) - 1;

			do {
				this.encrypt(pt & ptMask);
				pt >>>= nbits;
			} while (--ptLen > 0);
			return this;
		}
	}

	/**
	 * Compact list decryption.
	 * <p>Instances of this decrypt ciphertexts within an encrypted compact list. See {@link
	 * CompactListEncryption} for more details.
	 *
	 * @since 1.2
	 */
	public static abstract class CompactListDecryption {

		private final TorusFhe engine;
		private final @BinaryVector byte[] secretKey;
		private final int secretKeyOffset;
		private @ScalarVector byte[] currentCiphertextMask;
		private int currentCiphertextMaskOffset;
		private int nextCiphertextIndex;

		/**
		 * Construct new compact list decryption.
		 *
		 * @param engine engine to decrypt with
		 * @param sk secret key coefficients to decrypt with
		 * @param skOff offset, within {@code sk}, to begin loading from
		 * @throws IndexOutOfBoundsException {@code skOff} is negative or {@code skOff + n}, where
		 * {@code n} is equal to engine's {@linkplain TorusFhe#sizeOfBinaryVector() binary vector
		 * size}, is greater than {@code sk.length}
		 * @since 1.2
		 */
		public CompactListDecryption(TorusFhe engine, @BinaryVector byte[] sk, int skOff) {
			this.engine = engine;
			this.secretKey = sk;
			this.secretKeyOffset =
				Preconditions.checkFromIndexSize(skOff, engine.sizeOfBinaryVector(), sk.length);
			this.nextCiphertextIndex = engine.dimension;
		}

		/**
		 * Engine underlying decryption.
		 *
		 * @return underlying engine
		 * @since 1.2
		 */
		public final TorusFhe engine() {
			return this.engine;
		}

		/**
		 * Buffer ciphertext mask for current bin is being loaded from.
		 *
		 * @return ciphertext mask buffer or {@code null}
		 * @since 1.2
		 */
		protected final byte[] currentCiphertextMask() {
			return this.currentCiphertextMask;
		}

		/**
		 * Offset, within ciphertext mask buffer, that ciphertext mask coefficients are loaded
		 * from.
		 *
		 * @return offset mask coefficients will be loaded from
		 * @since 1.2
		 */
		protected final int currentCiphertextMaskOffset() {
			return this.currentCiphertextMaskOffset;
		}

		/**
		 * Set the buffer ciphertext mask for current bin will be loaded from.
		 * <p>This must be invoked <i>only</i> while a bin is not being decrypted. This may be
		 * invoked once during construction or within {@link #beginDecryptingBin()}.
		 *
		 * @param ctMask buffer to load ciphertext mask coefficients from
		 * @param ctMaskOff offset, within {@code ctMask}, to begin loading from
		 * @throws IllegalStateException bin is currently being decrypted
		 * @throws IndexOutOfBoundsException {@code ctMaskOff} is negative or {@code ctMaskOff + n},
		 * where {@code n} is equal to the {@linkplain TorusFhe#sizeOfScalarVector() scalar vector
		 * size} of the {@linkplain #engine() underlying engine}, is greater than {@code
		 * ctMask.length}
		 * @since 1.2
		 */
		protected final void setCurrentCiphertextMask(@ScalarVector byte[] ctMask, int ctMaskOff) {
			Preconditions.checkState(this.nextCiphertextIndex == this.engine.dimension);
			Preconditions.checkFromIndexSize(
				ctMaskOff,
				this.engine.sizeOfScalarVector(),
				ctMask.length
			);
			this.currentCiphertextMask = ctMask;
			this.currentCiphertextMaskOffset = ctMaskOff;
		}

		/**
		 * Begin decrypting next bin.
		 * <p>This is invoked whenever decryption of a new bin begins. Implementations should
		 * {@linkplain #setCurrentCiphertextMask(byte[], int) assign} the buffer from which the
		 * ciphertext mask coefficients for the current bin will be loaded from. After this is
		 * invoked, it is guaranteed that {@link #readCiphertextBody(int)} will be invoked, one or
		 * more times, to load ciphertext bodies within the current bin.
		 *
		 * @throws IllegalStateException insufficient data remaining to be decrypted
		 * @since 1.2
		 */
		protected abstract void beginDecryptingBin();

		/**
		 * Read ciphertext body.
		 * <p>This is invoked one or more times <i>after</i> {@link #beginDecryptingBin()} is
		 * invoked. For each invocation of this, it is guaranteed {@code i} will be monotonically
		 * increasing, equal to the {@code i}-th ciphertext within the <i>current</i> bin. This
		 * must return the <i>next</i> ciphertext body within the <i>current</i> bin.
		 *
		 * @param i ciphertext index within current bin
		 * @return ciphertext body
		 * @throws IllegalStateException insufficient data remaining to be decrypted
		 * @since 1.2
		 */
		protected abstract long readCiphertextBody(int i);

		// Move forward to next bin.
		private byte[] nextBin() {
			this.beginDecryptingBin();

			byte[] buff = this.currentCiphertextMask;

			Preconditions.checkState(buff != null);
			this.nextCiphertextIndex = 0;
			return buff;
		}

		/**
		 * Decrypt plaintext.
		 *
		 * @return decrypted plaintext
		 * @throws IllegalStateException insufficient data remaining to be decrypted
		 * @since 1.2
		 */
		public final long decrypt() {
			byte[] mask = this.currentCiphertextMask;
			int maskOff = this.currentCiphertextMaskOffset;
			byte[] sk = this.secretKey;
			int skOff = this.secretKeyOffset;
			int n = this.engine.dimension;
			int d = this.nextCiphertextIndex++;
			long dot = 0L;

			if (d == n) {
				this.nextCiphertextIndex = n;
				mask = this.nextBin();
				maskOff = this.currentCiphertextMaskOffset;
				d = 0;
				this.nextCiphertextIndex = 1;
			}
			/*
			 * Our mask is going to be the bin's mask multiplied by the mask within the cyclotomic
			 * ring modulo X^{n} + 1.
			 */
			for (int i = 0; i < d; i++) {
				dot +=
					-loadScalar(mask, maskOff, n - d + i) *
					(((sk[skOff + (i / 8)] & 0xff) >>> (i % 8)) & 1);
			}
			for (int i = d; i < n; i++) {
				dot +=
					loadScalar(mask, maskOff, i - d) *
					(((sk[skOff + (i / 8)] & 0xff) >>> (i % 8)) & 1);
			}
			return this.engine.decodePlaintext(this.readCiphertextBody(d) - dot);
		}

		/**
		 * Decrypt ciphertexts of a decomposed {@code int} and return recomposed {@code int}.
		 * <p>This decomposes {@link TorusFhe#plaintextCountOfDecomposedInt() n} ciphertexts and,
		 * recomposes each plaintext limb into an {@code int}.
		 *
		 * @return recomposed value
		 * @throws IllegalStateException insufficient data remaining to be decrypted
		 * @since 1.2
		 */
		public final int decryptAndRecomposeUnsignedInt() {
			int nbits = this.engine.bitsPerMessage();
			int ptLen = this.engine.plaintextCountOfDecomposedInt();
			int rv = 0;

			for (int i = 0; i < ptLen; i++)
				rv += (int) (this.decrypt() << (nbits * i));
			return rv;
		}

		/**
		 * Decrypt ciphertexts of a decomposed {@code long} and return recomposed {@code int}.
		 * <p>This decomposes {@link TorusFhe#plaintextCountOfDecomposedLong() n} ciphertexts and,
		 * recomposes each plaintext limb into an {@code int}.
		 *
		 * @return recomposed value
		 * @throws IllegalStateException insufficient data remaining to be decrypted
		 * @since 1.2
		 */
		public final long decryptAndRecomposeUnsignedLong() {
			int nbits = this.engine.bitsPerMessage();
			int ptLen = this.engine.plaintextCountOfDecomposedLong();
			long rv = 0;

			for (int i = 0; i < ptLen; i++)
				rv += this.decrypt() << (nbits * i);
			return rv;
		}
	}

	/**
	 * Construct new Torus FHE engine for a Learning With Errors (LWE) dimension.
	 *
	 * @param n LWE dimension
	 * @param msgMod message modulus
	 * @param carryMod carry modulus
	 * @param logNoiseB {@code log2} of T-Uniform noise bound
	 * @param noise generator to sample noise from
	 * @return resulting engine
	 * @throws IllegalArgumentException {@code n} is less than {@link #MIN_DIMENSION}, greater
	 * than {@link #MAX_DIMENSION}, or is not a power-of-2, {@code msgMod} or {@code carryMod}
	 * is less than {@code 1} or {@code msgMod * carryMod} overflows, or {@code logNoiseB} is less
	 * than {@code 1} or greater than {@code 62}
	 * @since 1.2
	 */
	public static TorusFhe ofDimension(
		@IntRange(from = MIN_DIMENSION, to = MAX_DIMENSION) int n,
		@IntRange(from = 1) int msgMod, @IntRange(from = 1) int carryMod,
		@IntRange(from = 1, to = 62) int logNoiseB, Csprng noise
	) {
		Preconditions.checkArgument(n >= MIN_DIMENSION && n <= MAX_DIMENSION && isPow2(n));
		return new TorusFhe(
			n, msgMod, carryMod, logNoiseB, noise,
			NttPrime32.ofExplicit(
				n, P0, P0_BITS,
				P0_RECIPROCAL64, P0_RECIPROCAL128_HIGH, P0_RECIPROCAL128_LOW,
				P0_BARRETT_FACTOR, P0_LIMB_COUNT,
				P0_CAN_USE_FAST_REDUCTION
			),
			NttPrime32.ofExplicit(
				n, P1, P1_BITS,
				P1_RECIPROCAL64, P1_RECIPROCAL128_HIGH, P1_RECIPROCAL128_LOW,
				P1_BARRETT_FACTOR, P1_LIMB_COUNT,
				P1_CAN_USE_FAST_REDUCTION
			),
			NttPrime32.ofExplicit(
				n, P2, P2_BITS,
				P2_RECIPROCAL64, P2_RECIPROCAL128_HIGH, P2_RECIPROCAL128_LOW,
				P2_BARRETT_FACTOR, P2_LIMB_COUNT,
				P2_CAN_USE_FAST_REDUCTION
			)
		);
	}

	// NTT plans for CRT.
	@VisibleForTesting
	final NttPrime32 binaryPolyMulNtt0;
	@VisibleForTesting
	final NttPrime32 binaryPolyMulNtt1;
	@VisibleForTesting
	final NttPrime32 binaryPolyMulNtt2;

	// Generator we source noise from.
	@VisibleForTesting
	public final Csprng noiseGenerator;

	// `TUniform(1, -2^{b_log2}, 2^{b_log2})`
	@VisibleForTesting
	final int logNoiseBound;

	// LWE dimension
	private final int dimension;

	// Factor we need to scale (message + carry) space by to bring it up into msb.
	private final long plaintextScalingFactor;

	// (message + carry) space modulus
	private final int plaintextModulus;
	// message space modulus
	private final int messageModulus;
	// carry space modulus
	private final int carryModulus;

	private TorusFhe(
		int n, int msgMod, int carryMod,
		int logNoiseB, Csprng noise,
		NttPrime32 binaryPolyMulNtt0, NttPrime32 binaryPolyMulNtt1, NttPrime32 binaryPolyMulNtt2
	) {
		int ptMod = msgMod * carryMod;

		Preconditions.checkArgument(
			binaryPolyMulNtt0.length() == n &&
			binaryPolyMulNtt1.length() == n &&
			binaryPolyMulNtt2.length() == n &&
			/*
			 * We compress uniformly generated randoms to map the edges with half probability of
			 * the interior values, so that we don't need to reject a random sample. Because we're
			 * restricted to 64-bit scalars, we need to ensure there is at least 2 bits of space
			 * remaining for our compression to work.
			 */
			logNoiseB >= 1 && (logNoiseB + 2) <= BITS_PER_SCALAR &&
			msgMod > 0 && carryMod > 0 && ptMod >= msgMod && ptMod >= carryMod
		);
		this.dimension = n;
		this.binaryPolyMulNtt0 = binaryPolyMulNtt0;
		this.binaryPolyMulNtt1 = binaryPolyMulNtt1;
		this.binaryPolyMulNtt2 = binaryPolyMulNtt2;
		this.noiseGenerator = noise;
		this.logNoiseBound = logNoiseB;
		this.messageModulus = msgMod;
		this.carryModulus = carryMod;
		this.plaintextModulus = msgMod * carryMod;
		this.plaintextScalingFactor =
			((1L << (BITS_PER_SCALAR - 1 - 1)) / ((long) msgMod * carryMod)) * 2;
	}

	/**
	 * Construct copy of engine with new noise generator.
	 *
	 * @param csprng noise generator to use
	 * @return engine copy
	 * @since 1.2
	 */
	public TorusFhe withNoiseGenerator(Csprng csprng) {
		return new TorusFhe(
			this.dimension, this.messageModulus, this.carryModulus,
			this.logNoiseBound, csprng,
			this.binaryPolyMulNtt0, this.binaryPolyMulNtt1, this.binaryPolyMulNtt2
		);
	}

	/**
	 * Dimensionality of Learning With Errors (LWE) mask or secret key.
	 * <p>This defines the length of the secret key vector and corresponding number of coefficients
	 * in a ciphertext mask. This controls the hardness of the underlying LWE lattice. Increasing
	 * this increases security against primal/dual lattice attacks and allows for more noise
	 * padding at the cost of increasing computation and memory overhead.
	 *
	 * @return encryption dimension
	 * @since 1.2
	 */
	public @IntRange(from = MIN_DIMENSION, to = MAX_DIMENSION) int dimension() {
		return this.dimension;
	}

	/**
	 * Size, in bytes, of scalar vectors of LWE dimension.
	 *
	 * @return scalar vector size
	 * @since 1.2
	 */
	public int sizeOfScalarVector() {
		return this.dimension * SCALAR_SIZE;
	}

	/**
	 * Size, in bytes, of binary vectors of LWE dimension.
	 *
	 * @return binary vector size
	 * @since 1.2
	 */
	public int sizeOfBinaryVector() {
		return divideCeil(this.dimension, 8);
	}

	/**
	 * Plaintext message bit capacity.
	 *
	 * @return message bit capacity
	 * @since 1.2
	 */
	public int messageModulus() {
		return this.messageModulus;
	}

	/**
	 * Bit capacity reserved within plaintext message to absorb carries.
	 *
	 * @return reserved bit capacity
	 * @since 1.2
	 */
	public int carryModulus() {
		return this.carryModulus;
	}

	// Load `i`-th scalar from vector `x` at byte position `xOff`.
	private static long loadScalar(@ScalarVector byte[] x, int xOff, int i) {
		return loadLong(x, xOff + (i * SCALAR_SIZE));
	}

	// Store `a` into `i`-th scalar within vector `x` at byte position `xOff`.
	private static void storeScalar(@ScalarVector byte[] x, int xOff, int i, long v) {
		storeLong(x, xOff + (i * SCALAR_SIZE), v);
	}

	// Number of bits required per noise scalar.
	private int bitsPerNoiseScalar() {
		return this.logNoiseBound + 2;
	}

	// Number of bytes required per noise scalar.
	private int bytesPerNoiseScalar() {
		return divideCeil(this.bitsPerNoiseScalar(), 8);
	}

	// Compress scalar `a` such that it fits T-Uniform distribution.
	@VisibleForTesting
	long noiseScalarOfUniform(long a) {
		long noise = a & (~0L >>> (BITS_PER_SCALAR - this.bitsPerNoiseScalar()));

		return ((noise >>> 1) + (noise & 1)) - (1L << this.logNoiseBound);
	}

	// Sample next noise scalar.
	private long nextNoiseScalar() {
		long a = this.noiseGenerator.nextInteger(this.bytesPerNoiseScalar());

		return this.noiseScalarOfUniform(a);
	}

	// Merge split product coefficients `prod0`, `prod1`, and `prod2`.
	private static long mergeSrncProductCoefficient(int prod0, int prod1, int prod2) {
		final long _0 = u(P0);
		final long _01 = _0 * u(P1);
		final long _012 = _01 * u(P2);

		int v0 = prod0;
		/*
		 * don't use reciprocal version of `multiplyMod()` here, since this *may* get optimized by
		 * ART/Dalvik because the primes are constant and (should be) viable for quick Barrett
		 * reduction.
		 */
		int v1 = multiplyMod(P0_INVERSE_MOD_P1, 2 * P1 + prod1 - v0, P1);
		int v2 =
			multiplyMod(P01_INVERSE_MOD_P2, 2 * P2 + prod2 - (v0 + multiplyMod(P0, v1, P2)), P2);
		long pos = u(v0) + (u(v1) * _0) + (u(v2) * _01);

		return gt(v2, P2 >>> 1) ? (pos - _012) : pos;
	}

	/*
	 * Compute split semi-reverse negacyclic convolution between vector of scalar polynomial
	 * coefficients from `lhs` and split binary coefficients in frequency domain from
	 * `modRhs{0,1,2}`. The split coefficients are stored into `prod{0,1,2}`. The frequency domain
	 * split of `lhs` are stored into `modLhs{0,1,2}`. The `prod{0,1}` buffers are used after lhs
	 * and rhs operands are consumed, while `prod2` is used before lhs and rhs are consumed.
	 */
	private void splitSrnc(
		byte[] prod0, byte[] prod1, byte[] prod2,
		byte[] modLhs0, int modLhs0Off,
		byte[] modLhs1, int modLhs1Off,
		byte[] modLhs2, int modLhs2Off,
		@ScalarVector byte[] lhs, int lhsOff,
		byte[] modRhs0, byte[] modRhs1, byte[] modRhs2
	) {
		int n = this.dimension;

		Preconditions.checkFromIndexSize(lhsOff, this.sizeOfScalarVector(), lhs.length);
		Preconditions.checkFromIndexSize(0, n * 4, prod0.length);
		Preconditions.checkFromIndexSize(0, n * 4, prod1.length);
		Preconditions.checkFromIndexSize(0, n * 4, prod2.length);
		Preconditions.checkFromIndexSize(modLhs0Off, n * 4, modLhs0.length);
		Preconditions.checkFromIndexSize(modLhs1Off, n * 4, modLhs1.length);
		Preconditions.checkFromIndexSize(modLhs2Off, n * 4, modLhs2.length);
		Preconditions.checkFromIndexSize(0, n * 4, modRhs0.length);
		Preconditions.checkFromIndexSize(0, n * 4, modRhs1.length);
		Preconditions.checkFromIndexSize(0, n * 4, modRhs2.length);

		// 1) Split `lhs` into 3, over the respective primes.
		for (int i = 0; i < n; i++) {
			long v = loadScalar(lhs, lhsOff, i);

			storeInt(modLhs0, modLhs0Off + i * 4, l(Long.remainderUnsigned(v, u(P0))));
			storeInt(modLhs1, modLhs1Off + i * 4, l(Long.remainderUnsigned(v, u(P1))));
			storeInt(modLhs2, modLhs2Off + i * 4, l(Long.remainderUnsigned(v, u(P2))));
		}
		this.binaryPolyMulNtt0.forward(modLhs0, modLhs0Off);
		this.binaryPolyMulNtt1.forward(modLhs1, modLhs1Off);
		this.binaryPolyMulNtt2.forward(modLhs2, modLhs2Off);

		// 2) Split multiply lhs (in frequency domain) by rhs (in frequency domain):
		this.binaryPolyMulNtt0.multiplyAndNormalize(prod0, 0, modLhs0, modLhs0Off, modRhs0, 0);
		this.binaryPolyMulNtt1.multiplyAndNormalize(prod1, 0, modLhs1, modLhs1Off, modRhs1, 0);
		this.binaryPolyMulNtt2.multiplyAndNormalize(prod2, 0, modLhs2, modLhs2Off, modRhs2, 0);

		// 3) Get split product coefficeints (no need for normalization, we did it above):
		this.binaryPolyMulNtt0.inverse(prod0, 0);
		this.binaryPolyMulNtt1.inverse(prod1, 0);
		this.binaryPolyMulNtt2.inverse(prod2, 0);
	}

	/*
	 * Compute semi-reverse negacyclic convolution between vector of scalar polynomial coefficients
	 * from `lhs` and split binary coefficients in frequency domain from `modRhs{0,1,2}`. The
	 * resulting product coefficients are stored into `dst` with noise sampled from T-Uniform
	 * distribution. The scratch buffers `prod{0,1}` must have a length `n * 4`, and are used
	 * for final product calculation after lhs and rhs operands are consumed. The `prod2` scratch
	 * buffer is used before lhs and rhs operands are consumed.
	 */
	private void splitSrncAndAccumulateNoise(
		@ScalarVector byte[] dst, int dstOff,
		byte[] prod0, byte[] prod1, byte[] prod2,
		@ScalarVector byte[] lhs, int lhsOff,
		byte[] modRhs0, byte[] modRhs1, byte[] modRhs2
	) {
		int n = this.dimension;

		Preconditions.checkFromIndexSize(dstOff, this.sizeOfScalarVector(), dst.length);

		/*
		 * 1) Compute split product. We'll use first half of `dst` and second half of `dst` to
		 * hold first and second freq domain splits of `lhs` and `prod2` for third split. This
		 * works out since the split product will then go into `prod{0,1,2}`, and then we can
		 * reassemble it into `dst`.
		 */
		this.splitSrnc(
			prod0, prod1, prod2,
			dst, dstOff, dst, dstOff + (n * 4), prod2, 0,
			lhs, lhsOff,
			modRhs0, modRhs1, modRhs2
		);

		// 2) Merge split product coefficients and add noise:
		for (int i = 0; i < n; i++) {
			int p0 = loadInt(prod0, i * 4);
			int p1 = loadInt(prod1, i * 4);
			int p2 = loadInt(prod2, i * 4);
			long p = mergeSrncProductCoefficient(p0, p1, p2);

			storeScalar(dst, dstOff, i, p + this.nextNoiseScalar());
		}
	}

	/*
	 * Split binary coefficients from `src`, and transform them into frequency domain, storing
	 * result in `mod{0,1,2}`. The `src` and, `mod1` *or* `mod2`, buffers may overlap.
	 */
	private void prepareRhsForBinarySrnc(
		byte[] mod0, byte[] mod1, byte[] mod2,
		@BinaryVector byte[] src, int srcOff
	) {
		int n = this.dimension;
		int nFull = n / 8;
		int nPart = n % 8;

		Preconditions.checkFromIndexSize(0, n * 4, mod0.length);
		Preconditions.checkFromIndexSize(0, n * 4, mod1.length);
		Preconditions.checkFromIndexSize(0, n * 4, mod2.length);
		Preconditions.checkFromIndexSize(srcOff, this.sizeOfBinaryVector(), src.length);
		for (int i = 0; i < nPart; i++)
			storeInt(mod0, i * 4, ((src[srcOff + nFull] & 0xff) >>> (nPart - 1 - i)) & 1);
		for(int i = 0; i < nFull; i++) {
			int packed = src[srcOff + (nFull - 1 - i)] & 0xff;
			int mi = (nPart + i * 8) * 4;

			storeInt(mod0, mi +  0, (packed & 0x80) != 0 ? 1 : 0);
			storeInt(mod0, mi +  4, (packed & 0x40) != 0 ? 1 : 0);
			storeInt(mod0, mi +  8, (packed & 0x20) != 0 ? 1 : 0);
			storeInt(mod0, mi + 12, (packed & 0x10) != 0 ? 1 : 0);
			storeInt(mod0, mi + 16, (packed & 0x08) != 0 ? 1 : 0);
			storeInt(mod0, mi + 20, (packed & 0x04) != 0 ? 1 : 0);
			storeInt(mod0, mi + 24, (packed & 0x02) != 0 ? 1 : 0);
			storeInt(mod0, mi + 28, (packed & 0x01) != 0 ? 1 : 0);
		}
		System.arraycopy(mod0, 0, mod1, 0, n * 4);
		System.arraycopy(mod0, 0, mod2, 0, n * 4);
		this.binaryPolyMulNtt0.forward(mod0, 0);
		this.binaryPolyMulNtt1.forward(mod1, 0);
		this.binaryPolyMulNtt2.forward(mod2, 0);
	}

	/*
	 * Compute semi-reverse negacyclic convolution between scalar polynomial coefficients of `lhs`
	 * and binary coefficients of `rhs` into `dst`.
	 */
	@VisibleForTesting
	void binarySrncAndAccumulateNoise(
		@ScalarVector byte[] dst, int dstOff,
		@ScalarVector byte[] lhs, int lhsOff,
		@BinaryVector byte[] rhs, int rhsOff
	) {
		int n = this.dimension;
		byte[] modRhs0 = new byte[n * 4];
		byte[] modRhs1 = new byte[n * 4];
		byte[] modRhs2 = new byte[n * 4];

		this.prepareRhsForBinarySrnc(modRhs0, modRhs1, modRhs2, rhs, rhsOff);
		this.splitSrncAndAccumulateNoise(
			dst, dstOff,
			modRhs0, modRhs1, new byte[n * 4],
			lhs, lhsOff,
			modRhs0, modRhs1, modRhs2
		);
	}

	// Compute dot product of a scalar vector `x` and a binary vector `y`.
	@VisibleForTesting
	long binaryDotProduct(@ScalarVector byte[] x, int xOff, @BinaryVector byte[] y, int yOff) {
		int n = this.dimension;
		int nFull = n / 8;
		int nPart = n % 8;
		long p = 0;

		Preconditions.checkFromIndexSize(xOff, this.sizeOfScalarVector(), x.length);
		Preconditions.checkFromIndexSize(yOff, this.sizeOfBinaryVector(), y.length);
		for (int i = 0; i < nFull; i++) {
			int yPacked = y[yOff + i] & 0xff;
			int xi = i * 8;

			p += loadScalar(x, xOff, xi++) * ((yPacked & 0x01) != 0 ? 1 : 0);
			p += loadScalar(x, xOff, xi++) * ((yPacked & 0x02) != 0 ? 1 : 0);
			p += loadScalar(x, xOff, xi++) * ((yPacked & 0x04) != 0 ? 1 : 0);
			p += loadScalar(x, xOff, xi++) * ((yPacked & 0x08) != 0 ? 1 : 0);
			p += loadScalar(x, xOff, xi++) * ((yPacked & 0x10) != 0 ? 1 : 0);
			p += loadScalar(x, xOff, xi++) * ((yPacked & 0x20) != 0 ? 1 : 0);
			p += loadScalar(x, xOff, xi++) * ((yPacked & 0x40) != 0 ? 1 : 0);
			p += loadScalar(x, xOff, xi++) * ((yPacked & 0x80) != 0 ? 1 : 0);
		}
		for (int i = 0; i < nPart; i++)
			p += loadScalar(x, xOff + nFull * 8, i) * (((y[yOff + nFull] & 0xff) >>> i) & 0x01);
		return p;
	}

	// Shift plaintext into msb.
	private long encodePlaintext(long a) {
		return a * this.plaintextScalingFactor;
	}

	// Shift plaintext into lsb.
	private long decodePlaintext(long l) {
		return Long.remainderUnsigned(
			divideRound(l, this.plaintextScalingFactor),
			this.plaintextModulus * 2L
		);
	}

	/**
	 * Generate secret key.
	 * <p>Upon return, the first {@link #sizeOfBinaryVector() n} bytes of {@code sk}, starting at
	 * position {@code skOff} (inclusive), will be initialized with uniform coefficients sampled
	 * from the underlying noise generator.
	 *
	 * @param sk vector to store coefficients into
	 * @param skOff offset, within {@code sk}, to begin storing from
	 * @throws IndexOutOfBoundsException {@code skOff} is negative or {@code skOff + n} is greater
	 * than {@code sk.length}
	 * @since 1.2
	 */
	public void generateSecretKey(@BinaryVector byte[] sk, int skOff) {
		this.noiseGenerator.nextBytes(sk, skOff, this.sizeOfBinaryVector());
	}

	/**
	 * Generate public key mask.
	 * <p>Upon return, the first {@link #sizeOfScalarVector() n} bytes of {@code pkMask}, starting
	 * at position {@code pkMaskOff} (inclusive), will be initialized with uniform coefficients
	 * sampled from {@code csprng}. The public key mask, as described by M. Joye, can be shared
	 * using the seed used to initialize {@code csprng} in place of the full mask vector, so long
	 * as the state of {@code csprng} has <i>not</i> been modified since initial seeding.
	 *
	 * @param pkMask vector to store mask coefficients into
	 * @param pkMaskOff offset, within {@code pkMask}, to begin storing from
	 * @param csprng generator to sample mask coefficients from
	 * @throws IllegalArgumentException {@code csprng} is the noise generator underlying {@code
	 * this}
	 * @throws IndexOutOfBoundsException {@code pkMaskOff} is negative or {@code pkMaskOff + n} is
	 * greater than {@code pkMask.length}
	 * @since 1.2
	 * @see #generatePublicKeyBody(byte[], int, byte[], int, byte[], int)
	 */
	public void generatePublicKeyMask(@ScalarVector byte[] pkMask, int pkMaskOff, Csprng csprng) {
		Preconditions.checkArgument(csprng != this.noiseGenerator);
		csprng.nextBytes(pkMask, pkMaskOff, this.sizeOfScalarVector());
	}

	/**
	 * Generate public key body given a public key mask and a secret key.
	 * <p>Upon return, the first {@link #sizeOfScalarVector() n} bytes of {@code pkBody}, starting
	 * at position {@code pkBodyOff} (inclusive), will be initialized with Learning With Errors
	 * (LWE) samples generated using the public key {@linkplain
	 * #generatePublicKeyMask(byte[], int, Csprng) mask} and {@linkplain
	 * #generateSecretKey(byte[], int) secret key} {@code sk}, starting at positions {@code
	 * pkMaskOff} (inclusive) and {@code skOff} (inclusive), respectively.
	 *
	 * @param pkBody vector to store LWE samples into
	 * @param pkBodyOff offset, within {@code pkBody}, to begin storing from
	 * @param pkMask vector to load public key mask coefficients from
	 * @param pkMaskOff offset, within {@code pkMask}, to begin loading from
	 * @param sk vector to load secret key coefficients from
	 * @param skOff offset, within {@code sk}, to begin loading from
	 * @throws IndexOutOfBoundsException {@code pkBodyOff}, {@code pkMaskOff}, or {@code skOff} is
	 * negative, or, {@code pkBodyOff + n}, {@code pkMaskOff + n}, or {@link
	 * #sizeOfBinaryVector() skOff + sizeOfBinaryVector()} is greater than {@code pkBody.length},
	 * {@code pkMask.length}, or {@code sk.length}, respectively
	 * @since 1.2
	 * @see #generatePublicKeyMask(byte[], int, Csprng)
	 */
	public void generatePublicKeyBody(
		@ScalarVector byte[] pkBody, int pkBodyOff,
		@ScalarVector byte[] pkMask, int pkMaskOff,
		@BinaryVector byte[] sk, int skOff
	) {
		this.binarySrncAndAccumulateNoise(pkBody, pkBodyOff, pkMask, pkMaskOff, sk, skOff);
	}

	/**
	 * Encrypt plaintext.
	 * <p>This encrypts a plaintext message {@code pt}, returning the ciphertext body as a {@code
	 * long} with the ciphertext mask coefficients stored into {@code ctMask}, starting at position
	 * {@code ctMaskOff} (inclusive). The encryption is performed using the public key {@linkplain
	 * #generatePublicKeyMask(byte[], int, Csprng) mask} and {@linkplain
	 * #generatePublicKeyBody(byte[], int, byte[], int, byte[], int) body} loaded from {@code
	 * pkMask} and {@code pkBody}, starting at position {@code pkMaskOff} (inclusive) and {@code
	 * pkBodyOff} (inclusive), respectively.
	 * <p>Note, resulting ciphertext <i>will</i> be malformed if {@code pt} overflows the full
	 * plaintext message space.
	 *
	 * @param ctMask vector to store ciphertext mask coefficients into
	 * @param ctMaskOff offset, within {@code ctMask}, to begin storing from
	 * @param pkMask vector to load public key mask coefficients from
	 * @param pkMaskOff offset, within {@code pkMask}, to begin loading from
	 * @param pkBody vector to load public key LWE samples from
	 * @param pkBodyOff offset, within {@code pkBody}, to begin loading from
	 * @param pt plaintext message to encrypt
	 * @return resulting ciphertext body
	 * @throws IndexOutOfBoundsException {@code ctMaskOff}, {@code pkMaskOff}, or {@code pkBodyOff}
	 * is negative, or, {@code ctMaskOff + n}, {@code pkMaskOff + n}, or {@code pkBodyOff + n},
	 * where {@code n} is equal to the {@linkplain #sizeOfScalarVector() scalar vector size}, is
	 * greater than {@code ctMask.length}, {@code pkMask.length}, or {@code pkBody.length},
	 * respectively
	 * @since 1.2
	 * @see #decrypt(byte[], int, byte[], int, long)
	 */
	public long encrypt(
		@ScalarVector byte[] ctMask, int ctMaskOff,
		@ScalarVector byte[] pkMask, int pkMaskOff,
		@ScalarVector byte[] pkBody, int pkBodyOff,
		long pt
	) {
		// Push random binary coefficients into `ctMask`, we'll use this as our ephemeral noise.
		this.noiseGenerator.nextBytes(ctMask, ctMaskOff, this.sizeOfBinaryVector());

		/*
		 * <pkBody,r> + e_{2} + pt (mod 2^{64}); compute this here because we'll consume
		 * ephemeral noise below
		 */
		long ctBody =
			this.binaryDotProduct(pkBody, pkBodyOff, ctMask, ctMaskOff) +
			this.nextNoiseScalar() +
			this.encodePlaintext(pt);

		// pkMask *_neg r + e_{1} (mod 2^{64}); where e_{1} is sampled from t-uniform distribution
		this.binarySrncAndAccumulateNoise(ctMask, ctMaskOff, pkMask, pkMaskOff, ctMask, ctMaskOff);
		return ctBody;
	}

	/**
	 * Decrypt ciphertext.
	 * <p>This decrypts the ciphertext defined by the coefficient mask {@code ctMask}, loaded from
	 * position {@code ctMaskOff} (inclusive), and body {@code ctBody} using the secret key
	 * coefficients loaded from {@code sk}, starting at position {@code skOff} (inclusive).
	 *
	 * @param sk vector to load secret key coefficients from
	 * @param skOff offset, within {@code sk}, to begin loading from
	 * @param ctMask vector to load ciphertext mask coefficients from
	 * @param ctMaskOff offset, within {@code ctMask}, to begin loading from
	 * @param ctBody ciphertext body to decrypt
	 * @return resulting plaintext
	 * @throws IndexOutOfBoundsException {@code skOff} or {@code ctMaskOff} is negative, or, {@link
	 * #sizeOfBinaryVector() skOff + sizeOfBinaryVector()} or {@link #sizeOfScalarVector()
	 * ctMaskOff + sizeOfScalarVector()} is greater than {@code sk.length} or {@code
	 * ctMask.length}, respectively
	 * @since 1.2
	 * @see #encrypt(byte[], int, byte[], int, byte[], int, long)
	 */
	public long decrypt(
		@BinaryVector byte[] sk, int skOff,
		@ScalarVector byte[] ctMask, int ctMaskOff,
		long ctBody
	) {
		return this.decodePlaintext(ctBody - this.binaryDotProduct(ctMask, ctMaskOff, sk, skOff));
	}

	/**
	 * Calculate number of ciphertext masks required to encrypt a given number of plaintexts.
	 *
	 * @param ptLen number of plaintexts to be encrypted
	 * @return number of ciphertext masks required
	 * @throws IllegalArgumentException {@code ptLen} is negative
	 * @since 1.2
	 */
	public int ciphertextMaskListCountOf(int ptLen) {
		Preconditions.checkArgument(ptLen >= 0);
		return divideCeil(ptLen, this.dimension);
	}

	// Number of bits in message space.
	private int bitsPerMessage() {
		return log2(this.messageModulus);
	}

	// Calculate number of plaintexts required to represent an `nbits`-bit integer.
	private int plaintextCountOfDecomposed(int nbits) {
		Preconditions.checkArgument(nbits <= BITS_PER_SCALAR);
		return divideCeil(nbits, this.bitsPerMessage());
	}

	/**
	 * Calculate number of plaintexts an {@code int} value will decompose into.
	 * <p>This returns {@code ceil(32 / m)} where {@code m} is equal to log base-2 of the message
	 * {@linkplain #messageModulus() modulus}.
	 *
	 * @return number of plaintext limbs an {@code int} will decompose into
	 * @since 1.2
	 */
	public int plaintextCountOfDecomposedInt() {
		return this.plaintextCountOfDecomposed(32);
	}

	/**
	 * Calculate number of plaintexts a {@code long} value will decompose into.
	 * <p>This returns {@code ceil(64 / m)} where {@code m} is equal to log base-2 of the message
	 * {@linkplain #messageModulus() modulus}.
	 *
	 * @return number of plaintext limbs a {@code long} will decompose into
	 * @since 1.2
	 */
	public int plaintextCountOfDecomposedLong() {
		return this.plaintextCountOfDecomposed(64);
	}
}
