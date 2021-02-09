/*
 * (c) Daniel Lemire, Owen Kaser, Samy Chambi, Jon Alvarado, Rory Graves, Björn Sperber
 * Licensed under the Apache License, Version 2.0.
 */

package org.roaringbitmap;

import java.util.Arrays;

/**
 * Specialized array to stored the containers used by a RoaringBitmap.
 */
public final class RoaringArray implements Cloneable {

    protected RoaringArray() {
        this.array = new Element[INITIAL_CAPACITY];
    }

    protected void append(short key, Container value) {
        extendArray(1);
        this.array[this.size++] = new Element(key, value);
    }

    /**
     * Append copy of the one value from another array
     *
     * @param sa    other array
     * @param index index in the other array
     */
    protected void appendCopy(RoaringArray sa, int index) {
        extendArray(1);
        this.array[this.size++] = new Element(sa.array[index].key,
                sa.array[index].value.clone());
    }

    /**
     * Append copies of the values from another array
     *
     * @param sa            other array
     * @param startingIndex starting index in the other array
     * @param end endingIndex (exclusive) in the other array
     */
    protected void appendCopy(RoaringArray sa, int startingIndex, int end) {
        extendArray(end - startingIndex);
        for (int i = startingIndex; i < end; ++i) {
            this.array[this.size++] = new Element(sa.array[i].key,
                    sa.array[i].value.clone());
        }

    }

    /**
     * Append copies of the values from another array, from the start
     *
     * @param sourceArray The array to copy from
     * @param stoppingKey any equal or larger key in other array will terminate
     *                    copying
     */
    protected void appendCopiesUntil(RoaringArray sourceArray, short stoppingKey) {
        int stopKey = Util.toIntUnsigned(stoppingKey);
        for (int i = 0; i < sourceArray.size; ++i) {
            if (Util.toIntUnsigned(sourceArray.array[i].key) >= stopKey)
                break;
            extendArray(1);
            this.array[this.size++] = new Element(sourceArray.array[i].key, sourceArray.array[i].value.clone());
        }
    }

    /**
     * Append copies of the values AFTER a specified key (may or may not be
     * present) to end.
     *
     * @param sa          other array
     * @param beforeStart given key is the largest key that we won't copy
     */
    protected void appendCopiesAfter(RoaringArray sa, short beforeStart) {
        int startLocation = sa.getIndex(beforeStart);
        if (startLocation >= 0)
            startLocation++;
        else
            startLocation = -startLocation - 1;
        extendArray(sa.size - startLocation);

        for (int i = startLocation; i < sa.size; ++i) {
            this.array[this.size++] = new Element(sa.array[i].key, sa.array[i].value.clone());
        }
    }

    protected void clear() {
        this.array = null;
        this.size = 0;
    }

    @Override
    public RoaringArray clone() throws CloneNotSupportedException {
        RoaringArray sa;
        sa = (RoaringArray) super.clone();
        sa.array = Arrays.copyOf(this.array, this.size);
        for (int k = 0; k < this.size; ++k)
            sa.array[k] = sa.array[k].clone();
        sa.size = this.size;
        return sa;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RoaringArray) {
            RoaringArray srb = (RoaringArray) o;
            if (srb.size != this.size)
                return false;
            for (int i = 0; i < srb.size; ++i) {
                Element self = this.array[i];
                Element other = srb.array[i];
                if (self.key != other.key || !self.value.equals(other.value))
                    return false;
            }
            return true;
        }
        return false;
    }

    // make sure there is capacity for at least k more elements
    protected void extendArray(int k) {
        // size + 1 could overflow
        if (this.size + k >= this.array.length) {
            int newCapacity;
            if (this.array.length < 1024) {
                newCapacity = 2 * (this.size + k);
            } else {
                newCapacity = 5 * (this.size + k) / 4;
            }
            this.array = Arrays.copyOf(this.array, newCapacity);
        }
    }

    // involves a binary search
    protected Container getContainer(short x) {
        int i = this.binarySearch(0, size, x);
        if (i < 0)
            return null;
        return this.array[i].value;
    }

    protected Container getContainerAtIndex(int i) {
        return this.array[i].value;
    }

    // involves a binary search
    protected int getIndex(short x) {
        // before the binary search, we optimize for frequent cases
        if ((size == 0) || (array[size - 1].key == x))
            return size - 1;
        // no luck we have to go through the list
        return this.binarySearch(0, size, x);
    }

    protected short getKeyAtIndex(int i) {
        return this.array[i].key;
    }

    @Override
    public int hashCode() {
    	int hashvalue = 0;
    	for(int k = 0; k < this.size; ++k)
    		hashvalue = 31 * hashvalue + array[k].hashCode();
    	return hashvalue;
    }

    // insert a new key, it is assumed that it does not exist
    protected void insertNewKeyValueAt(int i, short key, Container value) {
        extendArray(1);
        System.arraycopy(array, i, array, i + 1, size - i);
        array[i] = new Element(key, value);
        size++;
    }

    protected void resize(int newLength) {
        for (int k = newLength; k < this.size; ++k) {
            this.array[k] = null;
        }
        this.size = newLength;
    }

    protected boolean remove(short key) {
        int i = binarySearch(0, size, key);
        if (i >= 0) { // if a new key
            removeAtIndex(i);
            return true;
        }
        return false;
    }

    protected void removeAtIndex(int i) {
        System.arraycopy(array, i + 1, array, i, size - i - 1);
        array[size - 1] = null;
        size--;
    }

    /**
     * 根据下标插入container
     * @param i   下标
     * @param c   Container
     */
    protected void setContainerAtIndex(int i, Container c) {
        this.array[i].value = c;
    }

    protected int size() {
        return this.size;
    }

    private int binarySearch(int begin, int end, short key) {
        int low = begin;
        int high = end - 1;
        int ikey = Util.toIntUnsigned(key);

        while (low <= high) {
            int middleIndex = (low + high) >>> 1;
            int middleValue = Util.toIntUnsigned(array[middleIndex].key);

            if (middleValue < ikey)
                low = middleIndex + 1;
            else if (middleValue > ikey)
                high = middleIndex - 1;
            else
                return middleIndex;
        }
        return -(low + 1);
    }

    protected Element[] array = null;

    protected int size = 0;

    private static final int INITIAL_CAPACITY = 4;

    protected static final class Element implements Cloneable {
        public final short key;

        public Container value = null;

        public Element(short key, Container value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public int hashCode() {
        	return key * 0xF0F0F0 + value.hashCode();
        }
        
        @Override
        public boolean equals(Object o) {
        	if(o instanceof Element) {
        		Element e = (Element) o;
        		return (e.key == key) && e.value.equals(value);
        	}
        	return false;
        }
        
        @Override
        public Element clone() throws CloneNotSupportedException {
            Element c = (Element) super.clone();
            //c.key copied by super.clone
            c.value = this.value.clone();
            return c;
        }
    }
}
