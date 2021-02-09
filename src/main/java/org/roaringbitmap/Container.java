/*
 * (c) Daniel Lemire, Owen Kaser, Samy Chambi, Jon Alvarado, Rory Graves, Björn Sperber
 * Licensed under the Apache License, Version 2.0.
 */

package org.roaringbitmap;

import java.io.IOException;

/**
 * Base container class.
 * 改造为32位的container
 * @author gwk
 */
public abstract class Container implements Iterable<Integer>, Cloneable {

    /**
     * Create a container initialized with a range of consecutive values
     *
     * @param start first index
     * @param last  last index (range in inclusive)
     * @return a new container initialized with the specified values
     */
    public static Container rangeOfOnes(final int start, final int last) {
        // 超过该阈值，会将ArrayContainer替换成BitmapContainer，大约8kb
        if (last - start + 1 > ArrayContainer.DEFAULT_MAX_SIZE)
            return new BitmapContainer(start, last);
        return new ArrayContainer(start, last);
    }

    /**
     * Add a int to the container. May generate a new container.
     *
     * @param x integer to be added
     * @return the new container
     */
    public abstract Container add(int x);

    /**
     * Empties the container
     */
    public abstract void clear();

    @Override
    public abstract Container clone();

    /**
     * Checks whether the contain contains the provided value
     *
     * @param x value to check
     * @return whether the value is in the container
     */
    public abstract boolean contains(int x);

    /**
     * Fill the least significant 16 bits of the integer array, starting at
     * index index, with the short values from this container. The caller is
     * responsible to allocate enough room. The most significant 16 bits of
     * each integer are given by the most significant bits of the provided
     * mask.
     * 改为32位整形
     * @param x    provided array
     * @param i    starting index
     * @param mask indicates most significant bits
     */
    // public abstract void fillLeastSignificant16bits(int[] x, int i, int mask);

    /**
     * Size of the underlying array
     *
     * @return size in bytes
     */
    protected abstract int getArraySizeInBytes();

    /**
     * Computes the distinct number of short values in the container. Can be
     * expected to run in constant time.
     *
     * @return the cardinality
     */
    public abstract int getCardinality();

    /**
     * Iterator to visit the short values in the container
     *
     * @return iterator
     */
    public abstract IntegerIterator getIntegerIterator();

    /**
     * Computes an estimate of the memory usage of this container. The
     * estimate is not meant to be exact.
     *
     * @return estimated memory usage in bytes
     */
    public abstract int getSizeInBytes();

    /**
     * Remove the short from this container. May create a new container.
     *
     * @param x to be removed
     * @return New container
     */
    public abstract Container remove(int x);

    /**
     * Report the number of bytes required to serialize this container.
     *
     * @return the size in bytes
     */
    public abstract int serializedSizeInBytes();

    /**
     * If possible, recover wasted memory.
     */
    public abstract void trim();

    /**
     * Write just the underlying array.
     *
     * @param out output stream
     * @throws IOException
     */
    // protected abstract void writeArray(DataOutput out) throws IOException;
}
