package com.bzsoft.oworld.util.map;

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.bzsoft.oworld.util.math.MathUtils;

/**
 * An unordered map that uses int keys. This implementation is a cuckoo hash map
 * using 3 hashes, random walking, and a small stash for problematic keys. Null
 * values are allowed. No allocation is done except when growing the table size.
 * <br>
 * <br>
 * This map performs very fast get, containsKey, and remove (typically O(1),
 * worst case O(log(n))). Put may be a bit slower, depending on hash collisions.
 * Load factors greater than 0.91 greatly increase the chances the map will have
 * to rehash to the next higher POT size.
 *
 * @author Nathan Sweet
 */
public class IntMap<V> implements Iterable<IntMap.Entry<V>> {
	private static final int PRIME1 = 0xbe1f14b1;
	private static final int PRIME2 = 0xb4b82e39;
	private static final int PRIME3 = 0xced1c241;
	private static final int EMPTY = 0;

	public int size;

	int[] keyTable;
	V[] valueTable;
	int capacity, stashSize;
	V zeroValue;
	boolean hasZeroValue;

	private float loadFactor;
	private int hashShift, mask, threshold;
	private int stashCapacity;
	private int pushIterations;

	private Entries<V> entries1, entries2;
	private Values<V> values1, values2;
	private Keys<V> keys1, keys2;

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
	 */
	public IntMap() {
		this(51, 0.8f);
	}

	/**
	 * Creates a new map with a load factor of 0.8.
	 *
	 * @param initialCapacity
	 *            If not a power of two, it is increased to the next nearest power
	 *            of two.
	 */
	public IntMap(int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This
	 * map will hold initialCapacity items before growing the backing table.
	 *
	 * @param initialCapacity
	 *            If not a power of two, it is increased to the next nearest power
	 *            of two.
	 */
	@SuppressWarnings("unchecked")
	public IntMap(int initialCapacity, float loadFactor) {
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("initialCapacity must be >= 0: " + initialCapacity);
		}
		initialCapacity = MathUtils.nextPowerOfTwo((int) Math.ceil(initialCapacity / loadFactor));
		if (initialCapacity > 1 << 30) {
			throw new IllegalArgumentException("initialCapacity is too large: " + initialCapacity);
		}
		capacity = initialCapacity;

		if (loadFactor <= 0) {
			throw new IllegalArgumentException("loadFactor must be > 0: " + loadFactor);
		}
		this.loadFactor = loadFactor;

		threshold = (int) (capacity * loadFactor);
		mask = capacity - 1;
		hashShift = 31 - Integer.numberOfTrailingZeros(capacity);
		stashCapacity = Math.max(3, (int) Math.ceil(Math.log(capacity)) * 2);
		pushIterations = Math.max(Math.min(capacity, 8), (int) Math.sqrt(capacity) / 8);

		keyTable = new int[capacity + stashCapacity];
		valueTable = (V[]) new Object[keyTable.length];
	}

	/** Creates a new map identical to the specified map. */
	public IntMap(IntMap<? extends V> map) {
		this((int) Math.floor(map.capacity * map.loadFactor), map.loadFactor);
		stashSize = map.stashSize;
		System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
		System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
		size = map.size;
		zeroValue = map.zeroValue;
		hasZeroValue = map.hasZeroValue;
	}

	public V put(int key, V value) {
		if (key == 0) {
			final V oldValue = zeroValue;
			zeroValue = value;
			if (!hasZeroValue) {
				hasZeroValue = true;
				size++;
			}
			return oldValue;
		}

		final int[] keyTable = this.keyTable;

		// Check for existing keys.
		final int index1 = key & mask;
		final int key1 = keyTable[index1];
		if (key1 == key) {
			final V oldValue = valueTable[index1];
			valueTable[index1] = value;
			return oldValue;
		}

		final int index2 = hash2(key);
		final int key2 = keyTable[index2];
		if (key2 == key) {
			final V oldValue = valueTable[index2];
			valueTable[index2] = value;
			return oldValue;
		}

		final int index3 = hash3(key);
		final int key3 = keyTable[index3];
		if (key3 == key) {
			final V oldValue = valueTable[index3];
			valueTable[index3] = value;
			return oldValue;
		}

		final int index4 = hash4(key);
		final int key4 = keyTable[index4];
		if (key4 == key) {
			final V oldValue = valueTable[index4];
			valueTable[index4] = value;
			return oldValue;
		}

		// Update key in the stash.
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (keyTable[i] == key) {
				final V oldValue = valueTable[i];
				valueTable[i] = value;
				return oldValue;
			}
		}

		// Check for empty buckets.
		if (key1 == EMPTY) {
			keyTable[index1] = key;
			valueTable[index1] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return null;
		}

		if (key2 == EMPTY) {
			keyTable[index2] = key;
			valueTable[index2] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return null;
		}

		if (key3 == EMPTY) {
			keyTable[index3] = key;
			valueTable[index3] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return null;
		}

		if (key4 == EMPTY) {
			keyTable[index4] = key;
			valueTable[index4] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return null;
		}

		push(key, value, index1, key1, index2, key2, index3, key3, index4, key4);
		return null;
	}

	public void putAll(IntMap<V> map) {
		for (final Entry<V> entry : map.entries()) {
			put(entry.key, entry.value);
		}
	}

	/** Skips checks for existing keys. */
	private void putResize(int key, V value) {
		if (key == 0) {
			zeroValue = value;
			hasZeroValue = true;
			return;
		}

		// Check for empty buckets.
		final int index1 = key & mask;
		final int key1 = keyTable[index1];
		if (key1 == EMPTY) {
			keyTable[index1] = key;
			valueTable[index1] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		final int index2 = hash2(key);
		final int key2 = keyTable[index2];
		if (key2 == EMPTY) {
			keyTable[index2] = key;
			valueTable[index2] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		final int index3 = hash3(key);
		final int key3 = keyTable[index3];
		if (key3 == EMPTY) {
			keyTable[index3] = key;
			valueTable[index3] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		final int index4 = hash4(key);
		final int key4 = keyTable[index3];
		if (key4 == EMPTY) {
			keyTable[index4] = key;
			valueTable[index4] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		push(key, value, index1, key1, index2, key2, index3, key3, index4, key4);
	}

	private void push(int insertKey, V insertValue, int index1, int key1, int index2, int key2, int index3, int key3,
			int index4, int key4) {
		final int[] keyTable = this.keyTable;

		final V[] valueTable = this.valueTable;
		final int mask = this.mask;

		// Push keys until an empty bucket is found.
		int evictedKey;
		V evictedValue;
		int i = 0;
		final int pushIterations = this.pushIterations;
		do {
			// Replace the key and value for one of the hashes.
			switch (MathUtils.random(3)) {
			case 0:
				evictedKey = key1;
				evictedValue = valueTable[index1];
				keyTable[index1] = insertKey;
				valueTable[index1] = insertValue;
				break;
			case 1:
				evictedKey = key2;
				evictedValue = valueTable[index2];
				keyTable[index2] = insertKey;
				valueTable[index2] = insertValue;
				break;
			case 2:
				evictedKey = key3;
				evictedValue = valueTable[index3];
				keyTable[index3] = insertKey;
				valueTable[index3] = insertValue;
				break;
			default:
				evictedKey = key4;
				evictedValue = valueTable[index4];
				keyTable[index4] = insertKey;
				valueTable[index4] = insertValue;
				break;
			}

			// If the evicted key hashes to an empty bucket, put it there and stop.
			index1 = evictedKey & mask;
			key1 = keyTable[index1];
			if (key1 == EMPTY) {
				keyTable[index1] = evictedKey;
				valueTable[index1] = evictedValue;
				if (size++ >= threshold) {
					resize(capacity << 1);
				}
				return;
			}

			index2 = hash2(evictedKey);
			key2 = keyTable[index2];
			if (key2 == EMPTY) {
				keyTable[index2] = evictedKey;
				valueTable[index2] = evictedValue;
				if (size++ >= threshold) {
					resize(capacity << 1);
				}
				return;
			}

			index3 = hash3(evictedKey);
			key3 = keyTable[index3];
			if (key3 == EMPTY) {
				keyTable[index3] = evictedKey;
				valueTable[index3] = evictedValue;
				if (size++ >= threshold) {
					resize(capacity << 1);
				}
				return;
			}

			index4 = hash4(evictedKey);
			key4 = keyTable[index4];
			if (key4 == EMPTY) {
				keyTable[index4] = evictedKey;
				valueTable[index4] = evictedValue;
				if (size++ >= threshold) {
					resize(capacity << 1);
				}
				return;
			}

			if (++i == pushIterations) {
				break;
			}

			insertKey = evictedKey;
			insertValue = evictedValue;
		} while (true);

		putStash(evictedKey, evictedValue);
	}

	private void putStash(int key, V value) {
		if (stashSize == stashCapacity) {
			// Too many pushes occurred and the stash is full, increase the table size.
			resize(capacity << 1);
			put(key, value);
			return;
		}
		// Store key in the stash.
		final int index = capacity + stashSize;
		keyTable[index] = key;
		valueTable[index] = value;
		stashSize++;
		size++;
	}

	public V get(int key) {
		if (key == 0) {
			if (!hasZeroValue) {
				return null;
			}
			return zeroValue;
		}
		int index = key & mask;
		if (keyTable[index] != key) {
			index = hash2(key);
			if (keyTable[index] != key) {
				index = hash3(key);
				if (keyTable[index] != key) {
					index = hash4(key);
					if (keyTable[index] != key) {
						return getStash(key, null);
					}
				}
			}
		}
		return valueTable[index];
	}

	public V get(int key, V defaultValue) {
		if (key == 0) {
			if (!hasZeroValue) {
				return defaultValue;
			}
			return zeroValue;
		}
		int index = key & mask;
		if (keyTable[index] != key) {
			index = hash2(key);
			if (keyTable[index] != key) {
				index = hash3(key);
				if (keyTable[index] != key) {
					index = hash4(key);
					if (keyTable[index] != key) {
						return getStash(key, defaultValue);
					}
				}
			}
		}
		return valueTable[index];
	}

	private V getStash(int key, V defaultValue) {
		final int[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (keyTable[i] == key) {
				return valueTable[i];
			}
		}
		return defaultValue;
	}

	public V remove(int key) {
		if (key == 0) {
			if (!hasZeroValue) {
				return null;
			}
			final V oldValue = zeroValue;
			zeroValue = null;
			hasZeroValue = false;
			size--;
			return oldValue;
		}

		int index = key & mask;
		if (keyTable[index] == key) {
			keyTable[index] = EMPTY;
			final V oldValue = valueTable[index];
			valueTable[index] = null;
			size--;
			return oldValue;
		}

		index = hash2(key);
		if (keyTable[index] == key) {
			keyTable[index] = EMPTY;
			final V oldValue = valueTable[index];
			valueTable[index] = null;
			size--;
			return oldValue;
		}

		index = hash3(key);
		if (keyTable[index] == key) {
			keyTable[index] = EMPTY;
			final V oldValue = valueTable[index];
			valueTable[index] = null;
			size--;
			return oldValue;
		}

		index = hash4(key);
		if (keyTable[index] == key) {
			keyTable[index] = EMPTY;
			final V oldValue = valueTable[index];
			valueTable[index] = null;
			size--;
			return oldValue;
		}

		return removeStash(key);
	}

	V removeStash(int key) {
		final int[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (keyTable[i] == key) {
				final V oldValue = valueTable[i];
				removeStashIndex(i);
				size--;
				return oldValue;
			}
		}
		return null;
	}

	void removeStashIndex(int index) {
		// If the removed location was not last, move the last tuple to the removed
		// location.
		stashSize--;
		final int lastIndex = capacity + stashSize;
		if (index < lastIndex) {
			keyTable[index] = keyTable[lastIndex];
			valueTable[index] = valueTable[lastIndex];
			valueTable[lastIndex] = null;
		} else {
			valueTable[index] = null;
		}
	}

	/**
	 * Reduces the size of the backing arrays to be the specified capacity or less.
	 * If the capacity is already less, nothing is done. If the map contains more
	 * items than the specified capacity, the next highest power of two capacity is
	 * used instead.
	 */
	public void shrink(int maximumCapacity) {
		if (maximumCapacity < 0) {
			throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
		}
		if (size > maximumCapacity) {
			maximumCapacity = size;
		}
		if (capacity <= maximumCapacity) {
			return;
		}
		maximumCapacity = MathUtils.nextPowerOfTwo(maximumCapacity);
		resize(maximumCapacity);
	}

	/**
	 * Clears the map and reduces the size of the backing arrays to be the specified
	 * capacity if they are larger.
	 */
	public void clear(int maximumCapacity) {
		if (capacity <= maximumCapacity) {
			clear();
			return;
		}
		zeroValue = null;
		hasZeroValue = false;
		size = 0;
		resize(maximumCapacity);
	}

	public void clear() {
		if (size == 0) {
			return;
		}
		final int[] keyTable = this.keyTable;
		final V[] valueTable = this.valueTable;
		for (int i = capacity + stashSize; i-- > 0;) {
			keyTable[i] = EMPTY;
			valueTable[i] = null;
		}
		size = 0;
		stashSize = 0;
		zeroValue = null;
		hasZeroValue = false;
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the
	 * entire map and compares every value, which may be an expensive operation.
	 *
	 * @param identity
	 *            If true, uses == to compare the specified value with values in the
	 *            map. If false, uses {@link #equals(Object)}.
	 */
	public boolean containsValue(Object value, boolean identity) {
		final V[] valueTable = this.valueTable;
		if (value == null) {
			if (hasZeroValue && zeroValue == null) {
				return true;
			}
			final int[] keyTable = this.keyTable;
			for (int i = capacity + stashSize; i-- > 0;) {
				if (keyTable[i] != EMPTY && valueTable[i] == null) {
					return true;
				}
			}
		} else if (identity) {
			if (value == zeroValue) {
				return true;
			}
			for (int i = capacity + stashSize; i-- > 0;) {
				if (valueTable[i] == value) {
					return true;
				}
			}
		} else {
			if (hasZeroValue && value.equals(zeroValue)) {
				return true;
			}
			for (int i = capacity + stashSize; i-- > 0;) {
				if (value.equals(valueTable[i])) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean containsKey(int key) {
		if (key == 0) {
			return hasZeroValue;
		}
		int index = key & mask;
		if (keyTable[index] != key) {
			index = hash2(key);
			if (keyTable[index] != key) {
				index = hash3(key);
				if (keyTable[index] != key) {
					index = hash4(key);
					if (keyTable[index] != key) {
						return containsKeyStash(key);
					}
				}
			}
		}
		return true;
	}

	private boolean containsKeyStash(int key) {
		final int[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (keyTable[i] == key) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the key for the specified value, or <tt>notFound</tt> if it is not in
	 * the map. Note this traverses the entire map and compares every value, which
	 * may be an expensive operation.
	 *
	 * @param identity
	 *            If true, uses == to compare the specified value with values in the
	 *            map. If false, uses {@link #equals(Object)}.
	 */
	public int findKey(Object value, boolean identity, int notFound) {
		final V[] valueTable = this.valueTable;
		if (value == null) {
			if (hasZeroValue && zeroValue == null) {
				return 0;
			}
			final int[] keyTable = this.keyTable;
			for (int i = capacity + stashSize; i-- > 0;) {
				if (keyTable[i] != EMPTY && valueTable[i] == null) {
					return keyTable[i];
				}
			}
		} else if (identity) {
			if (value == zeroValue) {
				return 0;
			}
			for (int i = capacity + stashSize; i-- > 0;) {
				if (valueTable[i] == value) {
					return keyTable[i];
				}
			}
		} else {
			if (hasZeroValue && value.equals(zeroValue)) {
				return 0;
			}
			for (int i = capacity + stashSize; i-- > 0;) {
				if (value.equals(valueTable[i])) {
					return keyTable[i];
				}
			}
		}
		return notFound;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number
	 * of additional items. Useful before adding many items to avoid multiple
	 * backing array resizes.
	 */
	public void ensureCapacity(int additionalCapacity) {
		final int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded >= threshold) {
			resize(MathUtils.nextPowerOfTwo((int) Math.ceil(sizeNeeded / loadFactor)));
		}
	}

	@SuppressWarnings("unchecked")
	private void resize(int newSize) {
		final int oldEndIndex = capacity + stashSize;

		capacity = newSize;
		threshold = (int) (newSize * loadFactor);
		mask = newSize - 1;
		hashShift = 31 - Integer.numberOfTrailingZeros(newSize);
		stashCapacity = Math.max(3, (int) Math.ceil(Math.log(newSize)) * 2);
		pushIterations = Math.max(Math.min(newSize, 8), (int) Math.sqrt(newSize) / 8);

		final int[] oldKeyTable = keyTable;
		final V[] oldValueTable = valueTable;

		keyTable = new int[newSize + stashCapacity];
		valueTable = (V[]) new Object[newSize + stashCapacity];

		final int oldSize = size;
		size = hasZeroValue ? 1 : 0;
		stashSize = 0;
		if (oldSize > 0) {
			for (int i = 0; i < oldEndIndex; i++) {
				final int key = oldKeyTable[i];
				if (key != EMPTY) {
					putResize(key, oldValueTable[i]);
				}
			}
		}
	}

	private int hash4(int h) {
		h *= PRIME1;
		return (h ^ h >>> hashShift) & mask;
	}

	private int hash2(int h) {
		h *= PRIME2;
		return (h ^ h >>> hashShift) & mask;
	}

	private int hash3(int h) {
		h *= PRIME3;
		return (h ^ h >>> hashShift) & mask;
	}

	@Override
	public int hashCode() {
		int h = 0;
		if (hasZeroValue && zeroValue != null) {
			h += zeroValue.hashCode();
		}
		final int[] keyTable = this.keyTable;
		final V[] valueTable = this.valueTable;
		for (int i = 0, n = capacity + stashSize; i < n; i++) {
			final int key = keyTable[i];
			if (key != EMPTY) {
				h += key * 31;

				final V value = valueTable[i];
				if (value != null) {
					h += value.hashCode();
				}
			}
		}
		return h;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof IntMap)) {
			return false;
		}
		@SuppressWarnings("unchecked")
		final IntMap<V> other = (IntMap<V>) obj;
		if (other.size != size) {
			return false;
		}
		if (other.hasZeroValue != hasZeroValue) {
			return false;
		}
		if (hasZeroValue) {
			if (other.zeroValue == null) {
				if (zeroValue != null) {
					return false;
				}
			} else {
				if (!other.zeroValue.equals(zeroValue)) {
					return false;
				}
			}
		}
		final int[] keyTable = this.keyTable;
		final V[] valueTable = this.valueTable;
		for (int i = 0, n = capacity + stashSize; i < n; i++) {
			final int key = keyTable[i];
			if (key != EMPTY) {
				final V value = valueTable[i];
				if (value == null) {
					if (!other.containsKey(key) || other.get(key) != null) {
						return false;
					}
				} else {
					if (!value.equals(other.get(key))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public String toString() {
		if (size == 0) {
			return "[]";
		}
		final StringBuilder buffer = new StringBuilder(32);
		buffer.append('[');
		final int[] keyTable = this.keyTable;
		final V[] valueTable = this.valueTable;
		int i = keyTable.length;
		if (hasZeroValue) {
			buffer.append("0=");
			buffer.append(zeroValue);
		} else {
			while (i-- > 0) {
				final int key = keyTable[i];
				if (key == EMPTY) {
					continue;
				}
				buffer.append(key);
				buffer.append('=');
				buffer.append(valueTable[i]);
				break;
			}
		}
		while (i-- > 0) {
			final int key = keyTable[i];
			if (key == EMPTY) {
				continue;
			}
			buffer.append(", ");
			buffer.append(key);
			buffer.append('=');
			buffer.append(valueTable[i]);
		}
		buffer.append(']');
		return buffer.toString();
	}

	@Override
	public Iterator<Entry<V>> iterator() {
		return entries();
	}

	/**
	 * Returns an iterator for the entries in the map. Remove is supported. Note
	 * that the same iterator instance is returned each time this method is called.
	 * Use the {@link Entries} constructor for nested or multithreaded iteration.
	 */
	public Entries<V> entries() {
		if (entries1 == null) {
			entries1 = new Entries<>(this);
			entries2 = new Entries<>(this);
		}
		if (!entries1.valid) {
			entries1.reset();
			entries1.valid = true;
			entries2.valid = false;
			return entries1;
		}
		entries2.reset();
		entries2.valid = true;
		entries1.valid = false;
		return entries2;
	}

	/**
	 * Returns an iterator for the values in the map. Remove is supported. Note that
	 * the same iterator instance is returned each time this method is called. Use
	 * the {@link Entries} constructor for nested or multithreaded iteration.
	 */
	public Values<V> values() {
		if (values1 == null) {
			values1 = new Values<>(this);
			values2 = new Values<>(this);
		}
		if (!values1.valid) {
			values1.reset();
			values1.valid = true;
			values2.valid = false;
			return values1;
		}
		values2.reset();
		values2.valid = true;
		values1.valid = false;
		return values2;
	}

	/**
	 * Returns an iterator for the keys in the map. Remove is supported. Note that
	 * the same iterator instance is returned each time this method is called. Use
	 * the {@link Entries} constructor for nested or multithreaded iteration.
	 */
	public Keys<V> keys() {
		if (keys1 == null) {
			keys1 = new Keys<>(this);
			keys2 = new Keys<>(this);
		}
		if (!keys1.valid) {
			keys1.reset();
			keys1.valid = true;
			keys2.valid = false;
			return keys1;
		}
		keys2.reset();
		keys2.valid = true;
		keys1.valid = false;
		return keys2;
	}

	static public class Entry<V> {
		public int key;
		public V value;

		@Override
		public String toString() {
			return key + "=" + value;
		}
	}

	static private class MapIterator<V> {
		static final int INDEX_ILLEGAL = -2;
		static final int INDEX_ZERO = -1;

		public boolean hasNext;

		final IntMap<V> map;
		int nextIndex, currentIndex;
		boolean valid = true;

		public MapIterator(IntMap<V> map) {
			this.map = map;
			reset();
		}

		public void reset() {
			currentIndex = INDEX_ILLEGAL;
			nextIndex = INDEX_ZERO;
			if (map.hasZeroValue) {
				hasNext = true;
			} else {
				findNextIndex();
			}
		}

		void findNextIndex() {
			hasNext = false;
			final int[] keyTable = map.keyTable;
			for (final int n = map.capacity + map.stashSize; ++nextIndex < n;) {
				if (keyTable[nextIndex] != EMPTY) {
					hasNext = true;
					break;
				}
			}
		}

		public void remove() {
			if (currentIndex == INDEX_ZERO && map.hasZeroValue) {
				map.zeroValue = null;
				map.hasZeroValue = false;
			} else if (currentIndex < 0) {
				throw new IllegalStateException("next must be called before remove.");
			} else if (currentIndex >= map.capacity) {
				map.removeStashIndex(currentIndex);
				nextIndex = currentIndex - 1;
				findNextIndex();
			} else {
				map.keyTable[currentIndex] = EMPTY;
				map.valueTable[currentIndex] = null;
			}
			currentIndex = INDEX_ILLEGAL;
			map.size--;
		}
	}

	static public class Entries<V> extends MapIterator<V> implements Iterable<Entry<V>>, Iterator<Entry<V>> {
		private final Entry<V> entry = new Entry<>();

		public Entries(IntMap<V> map) {
			super(map);
		}

		/** Note the same entry instance is returned each time this method is called. */
		@Override
		public Entry<V> next() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			final int[] keyTable = map.keyTable;
			if (nextIndex == INDEX_ZERO) {
				entry.key = 0;
				entry.value = map.zeroValue;
			} else {
				entry.key = keyTable[nextIndex];
				entry.value = map.valueTable[nextIndex];
			}
			currentIndex = nextIndex;
			findNextIndex();
			return entry;
		}

		@Override
		public boolean hasNext() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return hasNext;
		}

		@Override
		public Iterator<Entry<V>> iterator() {
			return this;
		}

		@Override
		public void remove() {
			super.remove();
		}
	}

	static public class Values<V> extends MapIterator<V> implements Iterable<V>, Iterator<V> {
		public Values(IntMap<V> map) {
			super(map);
		}

		@Override
		public boolean hasNext() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return hasNext;
		}

		@Override
		public V next() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			V value;
			if (nextIndex == INDEX_ZERO) {
				value = map.zeroValue;
			} else {
				value = map.valueTable[nextIndex];
			}
			currentIndex = nextIndex;
			findNextIndex();
			return value;
		}

		@Override
		public Iterator<V> iterator() {
			return this;
		}

		@Override
		public void remove() {
			super.remove();
		}
	}

	static public class Keys<V> extends MapIterator<V> {
		public Keys(IntMap<V> map) {
			super(map);
		}

		public int next() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			final int key = nextIndex == INDEX_ZERO ? 0 : map.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

	}
}
