/*
 * (c) Daniel Lemire, Owen Kaser, Samy Chambi, Jon Alvarado, Rory Graves, Björn Sperber
 * Licensed under the Apache License, Version 2.0.
 */

package org.roaringbitmap;

/**
 * Various useful methods for roaring bitmaps.
 */
public final class Util {

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private Util() {
    }

    // 取64位整数的高位的后16位
    protected static short highbits(long x) {
        return (short) (x >>> 32);
    }

    // 取64位整数的后32位
    protected static int lowbits(long x) {
        return (int) x;
    }

    protected static short maxLowBit() {
        return (short) 0xFFFF;
    }

    protected static int toIntUnsigned(short x) {
        return x & 0xFFFF;
    }

    protected static int binarySearch(int[] array, int begin, int end, int ikey) {
        int low = begin;
        int high = end - 1;

        while (low <= high) {
            final int middleIndex = (high - low) >>> 1 + low;
            final int middleValue = array[middleIndex];

            if (middleValue < ikey) {
                low = middleIndex + 1;
            }
            else if (middleValue > ikey) {
                high = middleIndex - 1;
            }
            else {
                return middleIndex;
            }
        }
        return -(low + 1);
    }
}
