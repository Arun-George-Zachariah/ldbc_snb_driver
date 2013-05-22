/**                                                                                                                                                                                
 * Copyright (c) 2010 Yahoo! Inc. All rights reserved.                                                                                                                             
 *                                                                                                                                                                                 
 * Licensed under the Apache License, Version 2.0 (the "License"); you                                                                                                             
 * may not use this file except in compliance with the License. You                                                                                                                
 * may obtain a copy of the License at                                                                                                                                             
 *                                                                                                                                                                                 
 * http://www.apache.org/licenses/LICENSE-2.0                                                                                                                                      
 *                                                                                                                                                                                 
 * Unless required by applicable law or agreed to in writing, software                                                                                                             
 * distributed under the License is distributed on an "AS IS" BASIS,                                                                                                               
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or                                                                                                                 
 * implied. See the License for the specific language governing                                                                                                                    
 * permissions and limitations under the License. See accompanying                                                                                                                 
 * LICENSE file.                                                                                                                                                                   
 */

package com.ldbc.driver.generator.ycsb;

import org.apache.commons.math3.random.RandomDataGenerator;

import com.ldbc.driver.generator.Generator;
import com.ldbc.driver.util.NumberHelper;

/**
 * A generator of a zipfian distribution. It produces a sequence of items, such
 * that some items are more popular than others, according to a zipfian
 * distribution. When you construct an instance of this class, you specify the
 * number of items in the set to draw from, either by specifying an itemcount
 * (so that the sequence is of items from 0 to itemcount-1) or by specifying a
 * min and a max (so that the sequence is of items from min to max inclusive).
 * After you construct the instance, you can change the number of items by
 * calling nextInt(itemcount) or nextLong(itemcount).
 * 
 * Note that the popular items will be clustered together, e.g. item 0 is the
 * most popular, item 1 the second most popular, and so on (or min is the most
 * popular, min+1 the next most popular, etc.) If you don't want this
 * clustering, and instead want the popular items scattered throughout the item
 * space, then use ScrambledZipfianGenerator instead.
 * 
 * Be aware: initializing this generator may take a long time if there are lots
 * of items to choose from (e.g. over a minute for 100 million objects). This is
 * because certain mathematical values need to be computed to properly generate
 * a zipfian skew, and one of those values (zeta) is a sum sequence from 1 to n,
 * where n is the itemcount. Note that if you increase the number of items in
 * the set, we can compute a new zeta incrementally, so it should be fast unless
 * you have added millions of items. However, if you decrease the number of
 * items, we recompute zeta from scratch, so this can take a long time.
 * 
 * The algorithm used here is from
 * "Quickly Generating Billion-Record Synthetic Databases", Jim Gray et al,
 * SIGMOD 1994.
 */
public class YcsbZipfianNumberGenerator<T extends Number> extends Generator<T>
{
    public static final double ZIPFIAN_CONSTANT = 0.99;

    final T numberOfItems;
    final T minItem;
    final double zipfianConstant;
    // Computed parameters for generating the distribution.
    double alpha, zetan, eta, theta, zeta2theta;
    // The number of items used to compute zetan the last time.
    T countForZeta;

    final NumberHelper<T> number;

    /**
     * Flag to prevent problems. If you increase the number of items the zipfian
     * generator is allowed to choose from, this code will incrementally compute
     * a new zeta value for the larger itemcount. However, if you decrease the
     * number of items, the code computes zeta from scratch; this is expensive
     * for large itemsets. Usually this is not intentional; e.g. one thread
     * thinks the number of items is 1001 and calls "nextLong()" with that item
     * count; then another thread who thinks the number of items is 1000 calls
     * nextLong() with itemcount=1000 triggering the expensive recomputation.
     * (It is expensive for 100 million items, not really for 1000 items.) Why
     * did the second thread think there were only 1000 items? maybe it read the
     * item count before the first thread incremented it. So this flag allows
     * you to say if you really do want that recomputation. If true, then the
     * code will recompute zeta if the itemcount goes down. If false, the code
     * will assume itemcount only goes up, and never recompute.
     */
    boolean allowItemCountDecrease = false;

    /******************************* Constructors **************************************/

    /**
     * Create a zipfian generator for items between min and max (inclusive) for
     * the specified zipfian constant.
     * 
     * @param min The smallest integer to generate in the sequence.
     * @param max The largest integer to generate in the sequence.
     * @param zipfianConstant The zipfian constant to use.
     */
    public YcsbZipfianNumberGenerator( RandomDataGenerator random, T min, T max, double zipfianConstant )
    {
        // this( random, min, max, zipfianConstant, zetaStatic( max - min + 1,
        // zipfianConstant ) );
        this( random, min, max, zipfianConstant, zetaStatic( computeN( min, max ), zipfianConstant ) );
    }

    /**
     * Create a zipfian generator for items between min and max (inclusive) for
     * the specified zipfian constant, using the precomputed value of zeta.
     * 
     * @param min The smallest integer to generate in the sequence.
     * @param max The largest integer to generate in the sequence.
     * @param zipfianConstant The zipfian constant to use.
     * @param zetan The precomputed zeta constant.
     */
    public YcsbZipfianNumberGenerator( RandomDataGenerator random, T min, T max, double zipfianConstant, double zetan )
    {
        super( random );
        this.number = NumberHelper.createNumberHelper( min.getClass() );
        T two = number.inc( number.one() );

        this.numberOfItems = computeN( min, max );
        this.minItem = min;
        this.zipfianConstant = zipfianConstant;
        this.theta = this.zipfianConstant;
        this.zeta2theta = zeta( two, theta );
        this.alpha = 1.0 / ( 1.0 - theta );
        this.zetan = zetan;
        this.countForZeta = numberOfItems;
        this.eta = ( 1 - Math.pow( 2.0 / numberOfItems.doubleValue(), 1 - theta ) ) / ( 1 - zeta2theta / this.zetan );
        next();
    }

    /**************************************************************************/

    static <T1 extends Number> T1 computeN( T1 min, T1 max )
    {
        // n = max - min + 1
        NumberHelper<T1> number = NumberHelper.createNumberHelper( min.getClass() );
        return number.sum( number.sub( min, max ), number.one() );
    }

    /**
     * Compute the zeta constant needed for the distribution. Do this from
     * scratch for a distribution with n items, using the zipfian constant
     * theta. Remember the value of n, so if we change the itemcount, we can
     * recompute zeta.
     * 
     * @param n The number of items to compute zeta over.
     * @param theta The zipfian constant.
     */
    double zeta( T n, double theta )
    {
        countForZeta = n;
        return zetaStatic( n, theta );
    }

    /**
     * Compute the zeta constant needed for the distribution. Do this from
     * scratch for a distribution with n items, using the zipfian constant
     * theta. This is a static version of the function which will not remember
     * n.
     * 
     * @param n The number of items to compute zeta over.
     * @param theta The zipfian constant.
     */
    static <T1 extends Number> double zetaStatic( T1 n, double theta )
    {
        NumberHelper<T1> number = NumberHelper.createNumberHelper( n.getClass() );
        return zetaStatic( number.zero(), n, theta, 0 );
    }

    /**
     * Compute the zeta constant needed for the distribution. Do this
     * incrementally for a distribution that has n items now but used to have st
     * items. Use the zipfian constant theta. Remember the new value of n so
     * that if we change the itemcount, we'll know to recompute zeta.
     * 
     * @param st The number of items used to compute the last initialsum
     * @param n The number of items to compute zeta over.
     * @param theta The zipfian constant.
     * @param initialsum The value of zeta we are computing incrementally from.
     */
    double zeta( T st, T n, double theta, double initialsum )
    {
        countForZeta = n;
        return zetaStatic( st, n, theta, initialsum );
    }

    /**
     * Compute the zeta constant needed for the distribution. Do this
     * incrementally for a distribution that has n items now but used to have st
     * items. Use the zipfian constant theta. Remember the new value of n so
     * that if we change the itemcount, we'll know to recompute zeta.
     * 
     * @param st The number of items used to compute the last initialsum
     * @param n The number of items to compute zeta over.
     * @param theta The zipfian constant.
     * @param initialsum The value of zeta we are computing incrementally from.
     */
    static <T1 extends Number> double zetaStatic( T1 st, T1 n, double theta, double initialsum )
    {
        NumberHelper<T1> number = NumberHelper.createNumberHelper( st.getClass() );
        double sum = initialsum;
        for ( T1 i = st; number.lt( i, n ); i = number.inc( i ) )
        {
            sum += 1 / ( Math.pow( i.doubleValue() + 1, theta ) );
        }
        return sum;
    }

    /****************************************************************************************/

    /**
     * Generate the next item. this distribution will be skewed toward lower
     * integers; e.g. 0 will be the most popular, 1 the next most popular, etc.
     * 
     * @param itemCount The number of items in the distribution.
     * @return The next item in the sequence.
     */
    private T next( T itemCount )
    {
        // from "Quickly Generating Billion-Record Synthetic Databases", Jim
        // Gray et al, SIGMOD 1994

        if ( false == itemCount.equals( countForZeta ) )
        {

            // have to recompute zetan and eta, since they depend on itemcount
            synchronized ( this )
            {
                if ( number.gt( itemCount, countForZeta ) )
                {
                    // System.err.println("WARNING: Incrementally recomputing Zipfian distribtion. (itemcount="+itemcount+" countforzeta="+countforzeta+")");

                    // we have added more items. can compute zetan
                    // incrementally, which is cheaper
                    zetan = zeta( countForZeta, itemCount, theta, zetan );
                    eta = ( 1 - Math.pow( 2.0 / numberOfItems.doubleValue(), 1 - theta ) ) / ( 1 - zeta2theta / zetan );
                }
                else if ( ( number.lt( itemCount, countForZeta ) ) && ( allowItemCountDecrease ) )
                {
                    // have to start over with zetan
                    // note : for large itemsets, this is very slow. so don't do
                    // it!

                    // TODO: can also have a negative incremental computation,
                    // e.g. if you decrease the number of items, then just
                    // subtract
                    // the zeta sequence terms for the items that went away.
                    // This would be faster than recomputing from scratch when
                    // the number of items
                    // decreases

                    System.err.println( "WARNING: Recomputing Zipfian distribtion. This is slow and should be avoided. (itemcount="
                                        + itemCount + " countforzeta=" + countForZeta + ")" );

                    zetan = zeta( itemCount, theta );
                    eta = ( 1 - Math.pow( 2.0 / numberOfItems.doubleValue(), 1 - theta ) ) / ( 1 - zeta2theta / zetan );
                }
            }
        }

        double u = getRandom().nextUniform( 0, 1 );
        double uz = u * zetan;

        if ( uz < 1.0 )
        {
            return number.zero();
        }

        if ( uz < 1.0 + Math.pow( 0.5, theta ) )
        {
            return number.one();
        }

        T ret = number.round( minItem.doubleValue() + itemCount.doubleValue() * Math.pow( eta * u - eta + 1, alpha ) );
        return ret;
    }

    /**
     * Return the next value, skewed by the Zipfian distribution. The 0th item
     * will be the most popular, followed by the 1st, followed by the 2nd, etc.
     * (Or, if min != 0, the min-th item is the most popular, the min+1th item
     * the next most popular, etc.) If you want the popular items scattered
     * throughout the item space, use ScrambledZipfianGenerator instead.
     */
    @Override
    protected T doNext()
    {
        return next( numberOfItems );
    }
}