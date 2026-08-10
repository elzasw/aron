package cz.aron.mapper;

import com.esotericsoftware.kryo.Kryo;

import cz.aron.api.rest.model.StructuredResult;

/**
 * Kryo-based (de)serialization of a {@link StructuredResult}, stored as a {@code byte[]}
 * blob in {@code cz.aron.domain.ApuEntity#getResult()}.
 */
public final class StructuredResultSerializer {

	private static final int BUFFER_SIZE = 1024;

	private StructuredResultSerializer() {
	}

	public static byte[] serialize(StructuredResult result) {
		if (result == null) {
			return null;
		}
		return KryoSerializer.serialize(result, BUFFER_SIZE);
	}

	public static StructuredResult deserialize(byte[] data) {
		if (data == null || data.length == 0) {
			return null;
		}
		return KryoSerializer.deserialize(data, StructuredResult.class);
	}

	/**
	 * Serializes with a caller-supplied {@link Kryo}, e.g. one held for a whole batch
	 * instead of borrowed per result. The caller owns the instance and must not share it
	 * across threads.
	 */
	public static byte[] serialize(Kryo kryo, StructuredResult result) {
		if (result == null) {
			return null;
		}
		return KryoSerializer.serialize(kryo, result, BUFFER_SIZE);
	}

	/**
	 * Deserializes with a caller-supplied {@link Kryo}; see
	 * {@link #serialize(Kryo, StructuredResult)}.
	 */
	public static StructuredResult deserialize(Kryo kryo, byte[] data) {
		if (data == null || data.length == 0) {
			return null;
		}
		return KryoSerializer.deserialize(kryo, data, StructuredResult.class);
	}

}
