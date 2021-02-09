package org.roaringbitmap;

/**
 * 整形 Iterator
 */
public interface IntegerIterator {
    /**
     * @return whether there is another value
     */
    boolean hasNext();

    /**
     * @return next short value
     */
    int next();

    /**
     * remove current value
     */
    void remove();
}
