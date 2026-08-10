package cz.aron.mapper;

import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;

import cz.aron.api.rest.model.ApuPart;
import cz.aron.api.rest.model.ApuPartItem;

/**
 * Kryo-based (de)serialization of an APU's parts. The parts (REST model
 * {@link ApuPart} / {@link ApuPartItem}) are stored as a single {@code byte[]} blob in
 * {@code cz.aron.domain.ApuEntity#getData()} instead of being persisted in their own
 * tables.
 */
public final class ApuSerializer {

	private static final int BUFFER_SIZE = 4096;

	private ApuSerializer() {
	}

	public static byte[] serialize(List<ApuPart> parts) {
		if (parts == null || parts.isEmpty()) {
			return null;
		}
		return KryoSerializer.serialize(new ArrayList<>(parts), BUFFER_SIZE);
	}

	@SuppressWarnings("unchecked")
	public static List<ApuPart> deserialize(byte[] data) {
		if (data == null || data.length == 0) {
			return new ArrayList<>();
		}
		return KryoSerializer.deserialize(data, ArrayList.class);
	}

	/**
	 * Serializes with a caller-supplied {@link Kryo}, e.g. one held for a whole batch
	 * instead of borrowed per APU. The caller owns the instance and must not share it
	 * across threads.
	 */
	public static byte[] serialize(Kryo kryo, List<ApuPart> parts) {
		if (parts == null || parts.isEmpty()) {
			return null;
		}
		return KryoSerializer.serialize(kryo, new ArrayList<>(parts), BUFFER_SIZE);
	}

	/**
	 * Deserializes with a caller-supplied {@link Kryo}; see {@link #serialize(Kryo, List)}.
	 */
	@SuppressWarnings("unchecked")
	public static List<ApuPart> deserialize(Kryo kryo, byte[] data) {
		if (data == null || data.length == 0) {
			return new ArrayList<>();
		}
		return KryoSerializer.deserialize(kryo, data, ArrayList.class);
	}

}
