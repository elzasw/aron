package cz.aron.mapper;

import java.util.ArrayList;

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
 * Shared {@link Kryo} infrastructure for the {@code byte[]} blobs stored on
 * {@code cz.aron.domain.ApuEntity}.
 *
 * <p>{@link Kryo} instances are not thread-safe, so they are borrowed from a {@link Pool}.
 * Use {@link ApuSerializer} / {@link StructuredResultSerializer} rather than this class
 * directly; {@link #getKryo()} is here for callers that want to hold one instance across a
 * batch instead of borrowing per value.
 */
public final class KryoSerializer {

	private static final Pool<Kryo> KRYO_POOL = new Pool<Kryo>(true, false, 16) {
		@Override
		protected Kryo create() {
			return getKryo();
		}
	};

	private KryoSerializer() {
	}

	/**
	 * Creates a new {@link Kryo} configured for the APU model. The instance is owned by the
	 * caller (it does not come from — and is never returned to — the pool) and must not be
	 * shared across threads.
	 */
	public static Kryo getKryo() {
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

	/**
	 * A unit of work carried out with a pooled {@link Kryo}; see
	 * {@link KryoSerializer#doWithKryo(KryoTask)}.
	 *
	 * @param <T> type of the produced value
	 */
	@FunctionalInterface
	public interface KryoTask<T> {

		T run(Kryo kryo);

	}

	/**
	 * Runs the given task with a {@link Kryo} borrowed from the pool and returns it there
	 * afterwards. The instance must not outlive the task — do not let it escape.
	 */
	public static <T> T doWithKryo(KryoTask<T> task) {
		Kryo kryo = KRYO_POOL.obtain();
		try {
			return task.run(kryo);
		} finally {
			KRYO_POOL.free(kryo);
		}
	}

	static byte[] serialize(Object value, int bufferSize) {
		return doWithKryo(kryo -> serialize(kryo, value, bufferSize));
	}

	static <T> T deserialize(byte[] data, Class<T> type) {
		return doWithKryo(kryo -> deserialize(kryo, data, type));
	}

	/**
	 * Serializes using a caller-supplied {@link Kryo}, e.g. one from {@link #getKryo()}
	 * held for a batch of values. The caller owns the instance and must not share it
	 * across threads.
	 */
	static byte[] serialize(Kryo kryo, Object value, int bufferSize) {
		try (Output output = new Output(bufferSize, -1)) {
			kryo.writeObject(output, value);
			output.flush();
			return output.toBytes();
		}
	}

	/**
	 * Deserializes using a caller-supplied {@link Kryo}; see
	 * {@link #serialize(Kryo, Object, int)}.
	 */
	static <T> T deserialize(Kryo kryo, byte[] data, Class<T> type) {
		try (Input input = new Input(data)) {
			return kryo.readObject(input, type);
		}
	}

}
