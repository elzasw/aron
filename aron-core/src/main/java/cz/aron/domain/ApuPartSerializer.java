package cz.aron.domain;

import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;

import cz.aron.api.rest.model.ApuPart;
import cz.aron.api.rest.model.ApuPartItem;
import cz.aron.api.rest.model.ResultRowItem;
import cz.aron.api.rest.model.ResultRowItemValue;
import cz.aron.api.rest.model.StructuredResult;

/**
 * Kryo-based (de)serialization of an APU's parts. The parts (REST model
 * {@link ApuPart} / {@link ApuPartItem}) are stored as a single {@code byte[]} blob in
 * {@link ApuEntity#getData()} instead of being persisted in their own tables.
 *
 * <p>{@link Kryo} instances are not thread-safe, so they are borrowed from a {@link Pool}.
 */
public final class ApuPartSerializer {

	private static final Pool<Kryo> KRYO_POOL = new Pool<Kryo>(true, false, 16) {
		@Override
		protected Kryo create() {
			Kryo kryo = new Kryo();
			// data model may evolve; do not force explicit registration of every class
			kryo.setRegistrationRequired(false);
			kryo.setReferences(true);
			kryo.register(ArrayList.class);
			kryo.register(ApuPart.class);
			kryo.register(ApuPartItem.class);
			kryo.register(StructuredResult.class);
			kryo.register(ResultRowItem.class);
			kryo.register(ResultRowItemValue.class);
			return kryo;
		}
	};

	private ApuPartSerializer() {
	}

	public static byte[] serialize(List<ApuPart> parts) {
		if (parts == null || parts.isEmpty()) {
			return null;
		}
		Kryo kryo = KRYO_POOL.obtain();
		try (Output output = new Output(4096, -1)) {
			kryo.writeObject(output, new ArrayList<>(parts));
			output.flush();
			return output.toBytes();
		} finally {
			KRYO_POOL.free(kryo);
		}
	}

	@SuppressWarnings("unchecked")
	public static List<ApuPart> deserialize(byte[] data) {
		if (data == null || data.length == 0) {
			return new ArrayList<>();
		}
		Kryo kryo = KRYO_POOL.obtain();
		try (Input input = new Input(data)) {
			return kryo.readObject(input, ArrayList.class);
		} finally {
			KRYO_POOL.free(kryo);
		}
	}

	public static byte[] serializeStructuredResult(StructuredResult result) {
		if (result == null) {
			return null;
		}
		Kryo kryo = KRYO_POOL.obtain();
		try (Output output = new Output(1024, -1)) {
			kryo.writeObject(output, result);
			output.flush();
			return output.toBytes();
		} finally {
			KRYO_POOL.free(kryo);
		}
	}

	public static StructuredResult deserializeStructuredResult(byte[] data) {
		if (data == null || data.length == 0) {
			return null;
		}
		Kryo kryo = KRYO_POOL.obtain();
		try (Input input = new Input(data)) {
			return kryo.readObject(input, StructuredResult.class);
		} finally {
			KRYO_POOL.free(kryo);
		}
	}

}
