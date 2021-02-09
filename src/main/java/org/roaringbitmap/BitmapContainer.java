/*
 * (c) Daniel Lemire, Owen Kaser, Samy Chambi, Jon Alvarado, Rory Graves, Björn Sperber
 * Licensed under the Apache License, Version 2.0.
 */

package org.roaringbitmap;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Simple bitset-like container.
 * container需要转载32bit的空间
 */
public final class BitmapContainer extends Container implements Cloneable {
    protected static final int MAX_CAPACITY = 0x7fffffff;

    // 需要 2的24次方长度的long数组 TODO 评估内存使用情况和稀疏性，是否通过增大阈值解决
    // 原有逻辑： 1024个long提供65536个比特，1024 * 64 = 65536  固定占据8kb内存空间
    long[] bitmap;

    int cardinality;

    /**
     * Create a bitmap container with all bits set to false
     */
    public BitmapContainer() {
        this.cardinality = 0;
        // 固定大小容量
        this.bitmap = new long[MAX_CAPACITY / 64];
    }

    /**
     * Create a bitmap container with a run of ones from firstOfRun to
     * lastOfRun, inclusive caller must ensure that the range isn't so small
     * that an ArrayContainer should have been created instead
     *
     * @param firstOfRun first index
     * @param lastOfRun  last index (range is inclusive)
     */
    public BitmapContainer(final int firstOfRun, final int lastOfRun) {
        this.cardinality = lastOfRun - firstOfRun + 1;
        // 固定大小
        this.bitmap = new long[MAX_CAPACITY / 64];
        if (this.cardinality == MAX_CAPACITY) // perhaps a common case
            Arrays.fill(bitmap, -1L);
        else {
            final int firstWord = firstOfRun / 64;
            final int lastWord = lastOfRun / 64;
            final int zeroPrefixLength = firstOfRun & 63;
            final int zeroSuffixLength = 63 - (lastOfRun & 63);

            Arrays.fill(bitmap, firstWord, lastWord + 1, -1L);
            bitmap[firstWord] ^= ((1L << zeroPrefixLength) - 1);
            final long blockOfOnes = (1L << zeroSuffixLength) - 1;
            final long maskOnLeft = blockOfOnes << (64 - zeroSuffixLength);
            bitmap[lastWord] ^= maskOnLeft;
        }
    }

    private BitmapContainer(int newCardinality, long[] newBitmap) {
        this.cardinality = newCardinality;
        this.bitmap = Arrays.copyOf(newBitmap, newBitmap.length);
    }

    @Override
    public Container add(int x) {
        final long previous = bitmap[x / 64];
        bitmap[x / 64] |= (1l << x);
        cardinality += (previous ^ bitmap[x / 64]) >>> x;
        return this;
    }

    @Override
    public void clear() {
        if (cardinality != 0) {
            cardinality = 0;
            Arrays.fill(bitmap, 0);
        }
    }

    @Override
    public BitmapContainer clone() {
        return new BitmapContainer(this.cardinality, this.bitmap);
    }

    @Override
    public boolean contains(final int i) {
        final int x = i;
        return (bitmap[x / 64] & (1l << x)) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BitmapContainer) {
            BitmapContainer srb = (BitmapContainer) o;
            if (srb.cardinality != this.cardinality)
                return false;
            return Arrays.equals(this.bitmap, srb.bitmap);
        }
        return false;
    }

    /**
     * Fill the array with set bits
     *
     * @param array container (should be sufficiently large)
     */
    protected void fillArray(final int[] array) {
        int pos = 0;
        for (int k = 0; k < bitmap.length; ++k) {
            long bitset = bitmap[k];
            while (bitset != 0) {
                long t = bitset & -bitset;
                array[pos++] = k * 64 + Long.bitCount(t - 1);
                bitset ^= t;
            }
        }
    }


    @Override
    protected int getArraySizeInBytes() {
        return MAX_CAPACITY / 8;
    }

    @Override
    public int getCardinality() {
        return cardinality;
    }

    @Override
    public IntegerIterator getIntegerIterator() {
        return new IntegerIterator() {
            int i = BitmapContainer.this.nextSetBit(0);
            int j;

            @Override
            public boolean hasNext() {
                return i >= 0;
            }

            @Override
            public int next() {
                j = i;
                i = BitmapContainer.this.nextSetBit(i + 1);
                return (short) j;
            }

            @Override
            public void remove() {
                BitmapContainer.this.remove((short) j);
            }
        };

    }

    @Override
    public int getSizeInBytes() {
        return this.bitmap.length * 8;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bitmap);
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            final IntegerIterator si = BitmapContainer.this.getIntegerIterator();

            @Override
            public boolean hasNext() {
                return si.hasNext();
            }

            @Override
            public Integer next() {
                return si.next();
            }

            @Override
            public void remove() {
                si.remove();
            }
        };
    }

    protected void loadData(final ArrayContainer arrayContainer) {
        this.cardinality = arrayContainer.cardinality;
        for (int k = 0; k < arrayContainer.cardinality; k++) {
            final int x = arrayContainer.content[k];
            bitmap[x / 64] |= (1l << x);
        }
    }

    /**
     * Find the index of the next set bit greater or equal to i, returns -1
     * if none found.
     *
     * @param i starting index
     * @return index of the next set bit
     */
    public int nextSetBit(final int i) {
        int x = i / 64;
        if (x >= bitmap.length)
            return -1;
        long w = bitmap[x];
        w >>>= i;
        if (w != 0) {
            return i + Long.numberOfTrailingZeros(w);
        }
        ++x;
        for (; x < bitmap.length; ++x) {
            if (bitmap[x] != 0) {
                return x * 64 + Long.numberOfTrailingZeros(bitmap[x]);
            }
        }
        return -1;
    }


    @Override
    public Container remove(final int i) {
        final int x = i;
        if (cardinality == ArrayContainer.DEFAULT_MAX_SIZE) { // this is
            // the
            // uncommon
            // path
            if ((bitmap[x / 64] & (1l << x)) != 0) {
                --cardinality;
                bitmap[x / 64] &= ~(1l << x);
                return this.toArrayContainer();
            }
        }
        cardinality -= (bitmap[x / 64] & (1l << x)) >>> x;
        bitmap[x / 64] &= ~(1l << x);
        return this;
    }

    @Override
    public int serializedSizeInBytes() {
        return MAX_CAPACITY / 8;
    }

    /**
     * Copies the data to an array container
     *
     * @return the array container
     */
    public ArrayContainer toArrayContainer() {
        ArrayContainer ac = new ArrayContainer(cardinality);
        ac.loadData(this);
        return ac;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int i = this.nextSetBit(0);
        while (i >= 0) {
            sb.append(i);
            i = this.nextSetBit(i + 1);
            if (i >= 0)
                sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void trim() {
    }
}
