/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.IonValue
import com.amazon.ion.facet.Faceted

/**
 * Representation of a value within the context of an [Expression].
 */
interface ExprValue : Iterable<ExprValue>, Faceted {
    /** The type of value independent of its implementation. */
    val type: ExprValueType

    /**
     * Materializes the expression value as an [IonValue].
     *
     * The returned value may or may not be tethered to a container, so it is
     * the callers responsibility to deal with that accordingly (e.g. via `clone`).
     */
    val ionValue: IonValue

    /**
     * Returns the [Bindings] over this value.
     *
     * This is generally used for operations that scope over the *result*
     * of some computation.
     */
    val bindings: Bindings

    /**
     * Iterates over this value.
     *
     * If the underlying value is an *aggregate* type, this is a simple delegation.
     * If the underlying value is a *scalar* type, this produces a singleton.
     */
    operator override fun iterator(): Iterator<ExprValue>
}
