// SPDX-License-Identifier: MIT OR Apache-2.0

package org.polygamma.android.origin.util;

import android.icu.text.Normalizer2;
import android.os.Build;
import android.util.LruCache;

import androidx.core.util.Supplier;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility definitions for reflection.
 *
 * @since 1.1
 */
@SuppressWarnings({ "RedundantSuppression", "rawtypes" })
public class Reflection {

	/**
	 * Class member name pattern.
	 */
	private static final Pattern MEMBER_NAME_PATTERN =
		Pattern.compile("^(L[$_/a-zA-Z0-9]+)\\.([$_a-zA-Z0-9]+|<init>)(?:\\(((?:\\[*(?:Z|B|C|S|I|J|F|D|L[$_/a-zA-Z0-9]+;))*)\\)(\\[*(?:Z|B|C|S|I|J|F|D|V|L[$_/a-zA-Z0-9]+;)))?$");

	/**
	 * Cache mapping member signatures to their resolved values.
	 */
	private static final LruCache<String, Member> RESOLVE_CACHE = new LruCache<>(128);

	/**
	 * Resolve primitive class.
	 *
	 * @param sig class signature
	 * @return primitive class
	 * @throws ClassNotFoundException {@code sig} is not a valid primitive class signature
	 */
	private static Class<?> resolvePrimitiveClass(char sig) throws ClassNotFoundException {
		switch (sig) {
		case 'Z':
			return boolean.class;
		case 'B':
			return byte.class;
		case 'C':
			return char.class;
		case 'S':
			return short.class;
		case 'I':
			return int.class;
		case 'J':
			return long.class;
		case 'F':
			return float.class;
		case 'D':
			return double.class;
		case 'V':
			return void.class;
		default:
			throw new ClassNotFoundException();
		}
	}

	/**
	 * Resolve class from binary signature.
	 *
	 * @param sig class signature
	 * @return resolved class
	 * @throws IllegalArgumentException {@code sig} is malformed
	 * @throws ClassNotFoundException no class for {@code sig} was found
	 */
	private static Class<?> resolveClass(String sig) throws ClassNotFoundException {
		if (sig.startsWith("["))
			return Class.forName(sig.replace('/', '.'));
		if (sig.length() == 1)
			return resolvePrimitiveClass(sig.charAt(0));
		Preconditions.checkArgument(
			!sig.isEmpty() &&
			sig.charAt(0) == 'L' &&
			sig.charAt(sig.length() - 1) == ';'
		);
		return Class.forName(sig.substring(1, sig.length() - 1).replace('/', '.'));
	}

	/**
	 * Resolve field, constructor, or method by signature, bypassing {@linkplain #RESOLVE_CACHE
	 * cache}.
	 *
	 * @param sig signature to resolve
	 * @return resolved member
	 * @throws IllegalArgumentException {@code sig} is malformed
	 * @throws ClassNotFoundException class referenced in {@code sig} was not found
	 * @throws NoSuchFieldException {@code sig} references a non-existant field
	 * @throws NoSuchMethodException {@code sig} references a non-existant constructor or method
	 */
	@SuppressWarnings("DataFlowIssue")
	private static Member doResolveMember(String sig)
	throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
		Matcher matcher = MEMBER_NAME_PATTERN.matcher(sig);

		Preconditions.checkArgument(matcher.matches());

		Class<?> klass = resolveClass(Preconditions.checkNotNull(matcher.group(1)) + ';');
		String member = Preconditions.checkNotNull(matcher.group(2));
		String params = matcher.group(3);
		String ret = matcher.group(4);

		if (params == null) {
			try {
				return klass.getDeclaredField(member);
			} catch (NoSuchFieldException ignored) {
				return klass.getField(member);
			}
		}

		List<Class<?>> paramTypes = new ArrayList<>();

		while (!params.isEmpty()) {
			int idx = 0;

			while (params.charAt(idx) == '[')
				idx++;
			if (params.charAt(idx) == 'L')
				idx = params.indexOf(';');
			idx++;
			paramTypes.add(resolveClass(params.substring(0, idx)));
			params = params.substring(idx);
		}

		Class<?>[] paramTypesArr = paramTypes.toArray(new Class[0]);

		if (member.equals("<init>")) {
			try {
				return klass.getDeclaredConstructor(paramTypesArr);
			} catch (NoSuchMethodException ignored) {
				return klass.getConstructor(paramTypesArr);
			}
		}

		Method rv;

		try {
			rv = klass.getDeclaredMethod(member, paramTypesArr);
		} catch (NoSuchMethodException ignored) {
			rv = klass.getMethod(member, paramTypesArr);
		}

		Preconditions.checkArgument(
			rv.getReturnType()
				.equals(resolveClass(Preconditions.checkNotNull(ret)))
		);
		return rv;
	}

	/**
	 * Normalize member signature.
	 *
	 * @param sig signature to normalize
	 * @return normalized signature
	 */
	private static String normalizeSignature(String sig) {
		StringBuilder norm = new StringBuilder(sig.length());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
			Normalizer2.getNFKDInstance().normalize(sig, norm);
		else
			norm.append(Normalizer.normalize(sig, Normalizer.Form.NFKD));

		while (norm.length() > 0 && Character.isWhitespace(norm.charAt(0)))
			norm.deleteCharAt(0);
		while (norm.length() > 0 && Character.isWhitespace(norm.charAt(norm.length() - 1)))
			norm.deleteCharAt(norm.length() - 1);
		for (int i = 0; i < norm.length(); i++) {
			char c = norm.charAt(i);

			if (c <= 31 || c == 127) {
				norm.deleteCharAt(i--);
				if (c == 127 && i >= 0)
					norm.deleteCharAt(i--);
			}
		}
		return norm.toString();
	}

	/**
	 * Resolve field, constructor, or method by signature.
	 * <p>If {@code sig} was previously resolved, its cached resolution is returned; otherwise,
	 * member referenced by {@code sig} is resolved, cached, and returned.
	 * {@snippet lang="java" :
	 * Method a = resolveMember("Ljava/lang/String.indexOf(I)I");
	 * // equivalent to:
	 * Method b = String.class.getMethod("indexOf", int.class);
	 * }
	 *
	 * @param sig binary member signature
	 * @return resolved member
	 * @throws IllegalArgumentException {@code sig} could not be resolved
	 * @since 1.1
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static Member resolveMember(String sig) {
		sig = normalizeSignature(sig);

		Member mbr = RESOLVE_CACHE.get(sig);

		if (mbr == null) {
			try {
				mbr = doResolveMember(sig);
			} catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
				throw new IllegalArgumentException(sig, e);
			}
			RESOLVE_CACHE.put(sig, mbr);
		}
		return mbr;
	}

	/**
	 * Resolve constructor or method by signature.
	 * <p>This resolves {@code sig} to a {@linkplain #resolveMember(String) member}, failing if,
	 * and only if, {@code sig} could not be resolved <i>or</i> it did not resolve to a
	 * {@linkplain Constructor constructor} or {@linkplain Method method}.
	 *
	 * @param sig binary member signature
	 * @return resolved executable
	 * @throws IllegalArgumentException {@code sig} could not be resolved or resolved to a
	 * non-executable member
	 * @since 1.1
	 * @see #resolveMember(String)
	 */
	public static Member resolveExecutable(String sig) {
		Member exec = resolveMember(sig);

		Preconditions.checkArgument(
			exec instanceof Constructor<?> || exec instanceof Method,
			"%s is not an executable",
			sig
		);
		return exec;
	}

	/**
	 * Resolve field by signature.
	 * <p>This resolves {@code sig} to a {@linkplain #resolveMember(String) member}, failing if,
	 * and only if, {@code sig} could not be resolved <i>or</i> it did not resolve to a field.
	 *
	 * @param sig binary member signature
	 * @return resolved field
	 * @throws IllegalArgumentException {@code sig} could not be resolved or resolved to a
	 * non-field member
	 * @since 1.1
	 * @see #resolveMember(String)
	 */
	public static Field resolveField(String sig) {
		Member field = resolveMember(sig);

		Preconditions.checkArgument(field instanceof Field, "%s is not a field", sig);
		//noinspection DataFlowIssue
		return (Field) field;
	}

	/**
	 * Test whether a member is an instance member.
	 *
	 * @param mbr member to test
	 * @return {@code true} if, and only if, {@code member} is not {@linkplain Modifier#STATIC
	 * static}
	 * @since 1.1
	 */
	public static boolean isInstanceMember(Member mbr) {
		return (mbr.getModifiers() & Modifier.STATIC) == 0;
	}

	/**
	 * Adapt value to a type.
	 *
	 * @param type type to adapt value to
	 * @param val value to adapt
	 * @return adapted value
	 */
	private static Object adaptValue(Class<?> type, Object val) {
		if (boolean.class.equals(type) || Boolean.class.equals(type))
			return val instanceof Boolean ? val : ((Number) val).longValue() != 0L;
		if (byte.class.equals(type) || Byte.class.equals(type))
			return ((Number) val).byteValue();
		if (short.class.equals(type) || Short.class.equals(type))
			return ((Number) val).shortValue();
		if (char.class.equals(type) || Character.class.equals(type))
			return val instanceof Character ? val : (char) ((int) adaptValue(int.class, val));
		if (int.class.equals(type) || Integer.class.equals(type))
			return ((Number) val).intValue();
		if (long.class.equals(type) || Long.class.equals(type))
			return ((Number) val).longValue();
		if (float.class.equals(type) || Float.class.equals(type)) {
			return (
				val instanceof Double || val instanceof Float ? ((Number) val).floatValue() :
				Float.intBitsToFloat((int) adaptValue(int.class, val))
			);
		}
		if (double.class.equals(type) || Double.class.equals(type)) {
			return (
				val instanceof Double || val instanceof Float ? ((Number) val).doubleValue() :
				Double.longBitsToDouble((long) adaptValue(long.class, val))
			);
		}
		return val;
	}

	/**
	 * Attempt to mark a possibly {@linkplain AccessibleObject accessible} member as accessible.
	 *
	 * @param member member to mark
	 */
	private static void trySetAccessible(Member member) {
		if (member instanceof AccessibleObject) {
			AccessibleObject acc = (AccessibleObject) member;

			if (!acc.isAccessible()) {
				try {
					acc.setAccessible(true);
				} catch (Exception ignored) {
				}
			}
		}
	}

	/**
	 * Invoke an executable.
	 * <p>Any parameters, including instance parameter, required by {@code exec} is <i>polled</i>
	 * from {@code params}, starting from the last parameter to first, including instance
	 * parameter. For non-instance parameters, the parameter value is <i>adapted</i> to the
	 * parameter type expected by {@code exec}.
	 * <p>Note that only <i>primitive</i> values are adapted. This includes upcasting and
	 * downcasting the polled value to the expected value type. In the case of {@code float}
	 * and {@code double} parameters, if an integer is polled, then the integer value is assumed
	 * to be the <i>bit</i> representation.
	 *
	 * @param exec executable to invoke
	 * @param params supplier of parameters to invoke with
	 * @return value executable exited with
	 * @throws IllegalArgumentException {@code exec} is not an executable
	 * @throws RuntimeException error encountered while invoking {@code exec}
	 * @since 1.1
	 */
	public static Object invoke(Member exec, Supplier<Object> params) {
		Preconditions.checkArgument(
			Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
			exec instanceof Executable
		);

		Method meth = exec instanceof Method ? (Method) exec : null;
		Constructor<?> ctor = exec instanceof Constructor<?> ? (Constructor<?>) exec : null;

		Preconditions.checkArgument(meth != null || ctor != null);

		@SuppressWarnings("DataFlowIssue")
		Class<?>[] paramTypes =
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
			((Executable) exec).getParameterTypes() :
			meth != null ? meth.getParameterTypes() :
			ctor.getParameterTypes();
		Object[] paramVals = new Object[paramTypes.length];

		for (int i = paramVals.length - 1; i >= 0; i--)
			paramVals[i] = adaptValue(paramTypes[i], params.get());
		trySetAccessible(exec);
		if (ctor != null) {
			try {
				return ctor.newInstance(paramVals);
			} catch (Exception err) {
				throw new RuntimeException(err);
			}
		}

		@SuppressWarnings("DataFlowIssue")
		Object ref = isInstanceMember(meth) ? params.get() : meth.getDeclaringClass();

		try {
			return meth.invoke(ref, paramVals);
		} catch (Exception err) {
			throw new RuntimeException(err);
		}
	}

	/**
	 * Resolve executable member and invoke it.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * invoke( // @link substring="invoke" target="#invoke(Member, Supplier)"
	 *     resolveExecutable(sig), // @link substring="resolveExecutable" target="#resolveExecutable(String)"
	 *     params
	 * )
	 * }
	 *
	 * @param sig binary signature of member to resolve
	 * @param params supplier of parameters to invoke with
	 * @return value executable exited with
	 * @throws IllegalArgumentException {@code sig} could not be resolved or {@code sig} resolved
	 * to a non-executable member
	 * @throws RuntimeException error encountered while invoking member
	 * @since 1.1
	 * @see #resolveExecutable(String)
	 * @see #invoke(Member, Supplier)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static Object invoke(String sig, Supplier<Object> params) {
		return invoke(resolveExecutable(sig), params);
	}

	/**
	 * Read value of a field.
	 * <p>If {@code field} is an {@linkplain #isInstanceMember(Member) instance member}, then the
	 * target instance is <i>polled</i> from {@code params}.
	 *
	 * @param field field to read value of
	 * @param params supplier of parameters to read with
	 * @return value of field
	 * @throws RuntimeException error encountered while reading {@code field}
	 * @since 1.1
	 */
	public static Object read(Field field, Supplier<Object> params) {
		trySetAccessible(field);
		try {
			return field.get(isInstanceMember(field) ? params.get() : field.getDeclaringClass());
		} catch (Exception err) {
			throw new RuntimeException(err);
		}
	}

	/**
	 * Resolve field member and read its value.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * read( // @link substring="read" target="#read(Field, Supplier)"
	 *     resolveField(sig), // @link substring="resolveField" target="#resolveField(String)"
	 *     params
	 * )
	 * }
	 *
	 * @param sig binary signature of member to resolve
	 * @param params supplier of parameters to read with
	 * @return value of field
	 * @throws IllegalArgumentException {@code sig} could not be resolved or {@code sig} resolved
	 * to a non-field member
	 * @throws RuntimeException error encountered while reading value
	 * @since 1.1
	 * @see #resolveField(String)
	 * @see #read(Field, Supplier)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static Object read(String sig, Supplier<Object> params) {
		return read(resolveField(sig), params);
	}

	/**
	 * Write value to a field.
	 * <p>This writes a value, <i>polled</i> from {@code params}, to {@code field}. If {@code
	 * field} is an {@linkplain #isInstanceMember(Member) instance member}, then the target
	 * instance is also polled from {@code params}, <i>before</i> write value is polled. The
	 * write value is adapted similar to {@link #invoke(Member, Supplier)}.
	 *
	 * @param field field to write value to
	 * @param params supplier of parameters to write with
	 * @throws RuntimeException error encountered while reading {@code field}
	 * @since 1.1
	 */
	public static void write(Field field, Supplier<Object> params) {
		trySetAccessible(field);
		try {
			field.set(
				isInstanceMember(field) ? params.get() : field.getDeclaringClass(),
				adaptValue(field.getType(), params.get())
			);
		} catch (Exception err) {
			throw new RuntimeException(err);
		}
	}

	/**
	 * Resolve field member and write to it.
	 * <p>Shorthand for:
	 * {@snippet lang="java" :
	 * write( // @link substring="write" target="#write(Field, Supplier)"
	 *     resolveField(sig), // @link substring="resolveField" target="#resolveField(String)"
	 *     params
	 * )
	 * }
	 *
	 * @param sig binary signature of member to resolve
	 * @param params supplier of parameters to write with
	 * @throws IllegalArgumentException {@code sig} could not be resolved or {@code sig} resolved
	 * to a non-field member
	 * @throws RuntimeException error encountered while writing value
	 * @since 1.1
	 * @see #resolveField(String)
	 * @see #write(Field, Supplier)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static void write(String sig, Supplier<Object> params) {
		write(resolveField(sig), params);
	}

	/**
	 * Construct a supplier which returns the value an executable member returns with.
	 * <p>Efficient equivalent of:
	 * {@snippet lang="java" :
	 * () -> invoke(sig, params) // @link substring="invoke" target="#invoke(String, Supplier)"
	 * }
	 *
	 * @param sig binary signature of member to invoke
	 * @param params supplier of parameters to invoke with
	 * @return resulting supplier
	 * @throws IllegalArgumentException {@code sig} could not be resolved or {@code sig} resolved
	 * to a non-executable member
	 * @since 1.1
	 * @see #invoke(String, Supplier)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static Supplier<Object> invokerOf(String sig, Supplier<Object> params) {
		Member exec = resolveExecutable(sig);

		return () -> invoke(exec, params);
	}

	/**
	 * Construct a supplier which returns the value read from a field.
	 * <p>Efficient equivalent of:
	 * {@snippet lang="java" :
	 * () -> read(sig, params) // @link substring="read" target="#read(String, Supplier)"
	 * }
	 *
	 * @param sig binary signature of member to read from
	 * @param params supplier of parameters to read with
	 * @return resulting supplier
	 * @throws IllegalArgumentException {@code sig} could not be resolved or {@code sig} resolved
	 * to a non-field member
	 * @since 1.1
	 * @see #read(String, Supplier)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static Supplier<Object> readerOf(String sig, Supplier<Object> params) {
		Field field = resolveField(sig);

		return () -> read(field, params);
	}

	/**
	 * Construct a runnable which writes value to a field.
	 * <p>Efficient equivalent of:
	 * {@snippet lang="java" :
	 * () -> write(field, params) // @link substring="write" target="#write(String, Supplier)"
	 * }
	 *
	 * @param sig binary signature of member to write to
	 * @param params supplier of parameters to write with
	 * @return resulting supplier
	 * @throws IllegalArgumentException {@code sig} could not be resolved or {@code sig} resolved
	 * to a non-field member
	 * @since 1.1
	 * @see #write(String, Supplier)
	 */
	@SuppressWarnings("JavadocDeclaration")
	public static Runnable writerOf(String sig, Supplier<Object> params) {
		Field field = resolveField(sig);

		return () -> write(field, params);
	}

	private Reflection() {
	}
}
