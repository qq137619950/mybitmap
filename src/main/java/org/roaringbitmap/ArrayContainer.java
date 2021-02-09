/*
 * (c) Daniel Lemire, Owen Kaser, Samy Chambi, Jon Alvarado, Rory Graves, Björn Sperber
 * Licensed under the Apache License, Version 2.0.
 */
package org.roaringbitmap;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Simple container made of an array of 16-bit integers
 * container改造为32bit
 */
public final class ArrayContainer extends Container implements Cloneable {
    private static final int DEFAULT_INIT_SIZE = 4;

    // container数据结构转化阈值
    static final int DEFAULT_MAX_SIZE = 4096;

    private static final long serialVersionUID = 1L;

    protected int cardinality = 0;

    // 改为存储32位整形
    int[] content;

    /**
     * Create an array container with default capacity
     */
    public ArrayContainer() {
        this(DEFAULT_INIT_SIZE);
    }

    /**
     * Create an array container with specified capacity
     *
     * @param capacity The capacity of the container
     */
    public ArrayContainer(final int capacity) {
        content = new int[capacity];
    }

    /**
     * Create an array container with a run of ones from firstOfRun to
     * lastOfRun, inclusive. Caller is responsible for making sure the range
     * is small enough that ArrayContainer is appropriate.
     *
     * @param firstOfRun first index
     * @param lastOfRun  last index (range is inclusive)
     */
    public ArrayContainer(final int firstOfRun, final int lastOfRun) {
        final int valuesInRange = lastOfRun - firstOfRun + 1;
        this.content = new int[valuesInRange];
        for (int i = 0; i < valuesInRange; i++) {
            content[i] = firstOfRun + i;
        }
        cardinality = valuesInRange;
    }

    private ArrayContainer(int newCard, int[] newContent) {
        this.cardinality = newCard;
        this.content = Arrays.copyOf(newContent, newCard);
    }

    protected ArrayContainer(int[] newContent) {
        this.cardinality = newContent.length;
        this.content = newContent;
    }

    /**
     * running time is in O(n) time if insert is not in order.
     * 改为存储32bit
     */
    @Override
    public Container add(final int x) {
        // Transform the ArrayContainer to a BitmapContainer
        // when cardinality = DEFAULT_MAX_SIZE
        if (cardinality >= DEFAULT_MAX_SIZE) {
            // 超过阈值则转化为bitmap
            BitmapContainer a = this.toBitmapContainer();
            a.add(x);
            return a;
        }
        if ((cardinality == 0) || x > content[cardinality - 1]) {
            // 扩容
            if (cardinality >= this.content.length)
                increaseCapacity();
            content[cardinality++] = x;
            return this;
        }
        // 二分查找
        int loc = Util.binarySearch(content, 0, cardinality, x);
        // 没有查询到
        if (loc < 0) {
            // 扩容
            if (cardinality >= this.content.length)
                increaseCapacity();
            // insertion : shift the elements > x by one position to
            // the right
            // and put x in it's appropriate place
            // 确保有序，否则无法使用二分查找
            System.arraycopy(content, -loc - 1, content, -loc,cardinality + loc + 1);
            content[-loc - 1] = x;
            ++cardinality;
        }
        return this;
    }

    @Override
    public boolean contains(final int x) {
        return Util.binarySearch(content, 0, cardinality, x) >= 0;
    }

    @Override
    public void clear() {
        cardinality = 0;
    }

    @Override
    public ArrayContainer clone() {
        return new ArrayContainer(this.cardinality, this.content);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ArrayContainer) {
            ArrayContainer srb = (ArrayContainer) o;
            if (srb.cardinality != this.cardinality)
                return false;
            for (int i = 0; i < this.cardinality; ++i) {
                if (this.content[i] != srb.content[i])
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected int getArraySizeInBytes() {
        return cardinality * 2;
    }

    @Override
    public int getCardinality() {
        return cardinality;
    }

    @Override
    public IntegerIterator getIntegerIterator() {
        return new IntegerIterator() {
            int pos = 0;

            @Override
            public boolean hasNext() {
                return pos < ArrayContainer.this.cardinality;
            }

            @Override
            public int next() {
                return ArrayContainer.this.content[pos++];
            }

            @Override
            public void remove() {
                ArrayContainer.this.remove((short) (pos - 1));
                pos--;
            }
        };
    }

    @Override
    public int getSizeInBytes() {
        return this.cardinality * 2 + 4;

    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (int k = 0; k < cardinality; ++k)
            hash += 31 * content[k];
        return hash;
    }

    /**
     * 扩容
     * TODO 评估下数据量，定制化扩容规则
     */
    private void increaseCapacity() {
        int newCapacity = this.content.length < 64 ? this.content.length * 2
                : this.content.length < 1024 ? this.content.length * 3 / 2
                : this.content.length * 5 / 4;
        if (newCapacity > ArrayContainer.DEFAULT_MAX_SIZE)
            newCapacity = ArrayContainer.DEFAULT_MAX_SIZE;
        this.content = Arrays.copyOf(this.content, newCapacity);
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            short pos = 0;

            @Override
            public boolean hasNext() {
                return pos < ArrayContainer.this.cardinality;
            }

            @Override
            public Integer next() {
                return ArrayContainer.this.content[pos++];
            }

            @Override
            public void remove() {
                ArrayContainer.this.remove((short) (pos - 1));
                pos--;
            }
        };
    }

    protected void loadData(final BitmapContainer bitmapContainer) {
        this.cardinality = bitmapContainer.cardinality;
        bitmapContainer.fillArray(content);
    }

    @Override
    public Container remove(final int x) {
        final int loc = Util.binarySearch(content, 0, cardinality, x);
        if (loc >= 0) {
            // insertion
            System.arraycopy(content, loc + 1, content, loc, cardinality - loc - 1);
            --cardinality;
        }
        return this;
    }

    @Override
    public int serializedSizeInBytes() {
        return cardinality * 2 + 2;
    }

    /**
     * Copies the data in a bitmap container.
     *
     * @return the bitmap container
     */
    public BitmapContainer toBitmapContainer() {
        BitmapContainer bc = new BitmapContainer();
        bc.loadData(this);
        return bc;
    }

    @Override
    public String toString() {
        if (this.cardinality == 0)
            return "{}";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < this.cardinality - 1; i++) {
            sb.append(this.content[i]);
            sb.append(",");
        }
        sb.append(this.content[this.cardinality - 1]);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void trim() {
        this.content = Arrays.copyOf(this.content, this.cardinality);
    }

//    @Override
//    protected void writeArray(DataOutput out) throws IOException {
//        // little endian
//        for (int k = 0; k < this.cardinality; ++k) {
//            out.write((this.content[k]) & 0xFF);
//            out.write((this.content[k] >>> 8) & 0xFF);
//        }
//    }

}
