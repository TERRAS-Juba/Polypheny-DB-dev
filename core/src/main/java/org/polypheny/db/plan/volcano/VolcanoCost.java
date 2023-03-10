/*
 * Copyright 2019-2021 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file incorporates code covered by the following terms:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.plan.volcano;


import java.util.Objects;
import org.polypheny.db.plan.AlgOptCost;
import org.polypheny.db.plan.AlgOptCostFactory;
import org.polypheny.db.plan.AlgOptUtil;


/**
 * <code>VolcanoCost</code> represents the cost of a plan node.
 *
 * This class is immutable: none of the methods modify any member variables.
 */
public class VolcanoCost implements AlgOptCost {

    static final VolcanoCost INFINITY =
            new VolcanoCost( Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY ) {
                public String toString() {
                    return "{inf}";
                }
            };

    static final VolcanoCost HUGE =
            new VolcanoCost( Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE ) {
                public String toString() {
                    return "{huge}";
                }
            };

    static final VolcanoCost ZERO =
            new VolcanoCost( 0.0, 0.0, 0.0 ) {
                public String toString() {
                    return "{0}";
                }
            };

    static final VolcanoCost TINY =
            new VolcanoCost( 1.0, 1.0, 0.0 ) {
                public String toString() {
                    return "{tiny}";
                }
            };

    public static final AlgOptCostFactory FACTORY = new Factory();


    final double cpu;
    final double io;
    final double rowCount;


    VolcanoCost( double rowCount, double cpu, double io ) {
        this.rowCount = rowCount;
        this.cpu = cpu;
        this.io = io;
    }


    @Override
    public double getCpu() {
        return cpu;
    }


    @Override
    public boolean isInfinite() {
        return (this == INFINITY)
                || (this.rowCount == Double.POSITIVE_INFINITY)
                || (this.cpu == Double.POSITIVE_INFINITY)
                || (this.io == Double.POSITIVE_INFINITY);
    }


    @Override
    public double getCosts() {
        return getRows() + getCpu() + getIo();
    }


    @Override
    public double getIo() {
        return io;
    }


    @Override
    public boolean isLe( AlgOptCost other ) {
        VolcanoCost that = (VolcanoCost) other;
        if ( true ) {
            return this == that || this.rowCount <= that.rowCount;
        }
        return (this == that)
                || ((this.rowCount <= that.rowCount)
                && (this.cpu <= that.cpu)
                && (this.io <= that.io));
    }


    @Override
    public boolean isLt( AlgOptCost other ) {
        if ( true ) {
            VolcanoCost that = (VolcanoCost) other;
            return this.rowCount < that.rowCount;
        }
        return isLe( other ) && !equals( other );
    }


    @Override
    public double getRows() {
        return rowCount;
    }


    @Override
    public int hashCode() {
        return Objects.hash( rowCount, cpu, io );
    }


    @Override
    public boolean equals( AlgOptCost other ) {
        return this == other
                || other instanceof VolcanoCost
                && (this.rowCount == ((VolcanoCost) other).rowCount)
                && (this.cpu == ((VolcanoCost) other).cpu)
                && (this.io == ((VolcanoCost) other).io);
    }


    @Override
    public boolean equals( Object obj ) {
        if ( obj instanceof VolcanoCost ) {
            return equals( (VolcanoCost) obj );
        }
        return false;
    }


    @Override
    public boolean isEqWithEpsilon( AlgOptCost other ) {
        if ( !(other instanceof VolcanoCost) ) {
            return false;
        }
        VolcanoCost that = (VolcanoCost) other;
        return (this == that)
                || ((Math.abs( this.rowCount - that.rowCount ) < AlgOptUtil.EPSILON)
                && (Math.abs( this.cpu - that.cpu ) < AlgOptUtil.EPSILON)
                && (Math.abs( this.io - that.io ) < AlgOptUtil.EPSILON));
    }


    @Override
    public AlgOptCost minus( AlgOptCost other ) {
        if ( this == INFINITY ) {
            return this;
        }
        VolcanoCost that = (VolcanoCost) other;
        return new VolcanoCost(
                this.rowCount - that.rowCount,
                this.cpu - that.cpu,
                this.io - that.io );
    }


    @Override
    public AlgOptCost multiplyBy( double factor ) {
        if ( this == INFINITY ) {
            return this;
        }
        return new VolcanoCost( rowCount * factor, cpu * factor, io * factor );
    }


    @Override
    public double divideBy( AlgOptCost cost ) {
        // Compute the geometric average of the ratios of all of the factors which are non-zero and finite.
        VolcanoCost that = (VolcanoCost) cost;
        double d = 1;
        double n = 0;
        if ( (this.rowCount != 0)
                && !Double.isInfinite( this.rowCount )
                && (that.rowCount != 0)
                && !Double.isInfinite( that.rowCount ) ) {
            d *= this.rowCount / that.rowCount;
            ++n;
        }
        if ( (this.cpu != 0)
                && !Double.isInfinite( this.cpu )
                && (that.cpu != 0)
                && !Double.isInfinite( that.cpu ) ) {
            d *= this.cpu / that.cpu;
            ++n;
        }
        if ( (this.io != 0)
                && !Double.isInfinite( this.io )
                && (that.io != 0)
                && !Double.isInfinite( that.io ) ) {
            d *= this.io / that.io;
            ++n;
        }
        if ( n == 0 ) {
            return 1.0;
        }
        return Math.pow( d, 1 / n );
    }


    @Override
    public AlgOptCost plus( AlgOptCost other ) {
        VolcanoCost that = (VolcanoCost) other;
        if ( (this == INFINITY) || (that == INFINITY) ) {
            return INFINITY;
        }
        return new VolcanoCost(
                this.rowCount + that.rowCount,
                this.cpu + that.cpu,
                this.io + that.io );
    }


    public String toString() {
        return "{" + rowCount + " rows, " + cpu + " cpu, " + io + " io}";
    }


    /**
     * Implementation of {@link AlgOptCostFactory} that creates {@link org.polypheny.db.plan.volcano.VolcanoCost}s.
     */
    private static class Factory implements AlgOptCostFactory {

        @Override
        public AlgOptCost makeCost( double dRows, double dCpu, double dIo ) {
            return new VolcanoCost( dRows, dCpu, dIo );
        }


        @Override
        public AlgOptCost makeHugeCost() {
            return VolcanoCost.HUGE;
        }


        @Override
        public AlgOptCost makeInfiniteCost() {
            return VolcanoCost.INFINITY;
        }


        @Override
        public AlgOptCost makeTinyCost() {
            return VolcanoCost.TINY;
        }


        @Override
        public AlgOptCost makeZeroCost() {
            return VolcanoCost.ZERO;
        }

    }

}

