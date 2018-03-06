package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*

/**
 * Coalesce built in function. Takes in one ore more expression as arguments and returns the first non unknown value
 *
 * ```
 * COALESCE(EXPRESSION, [EXPRESSION...])
 * ```
 */
internal class CoalesceExprFunction(val ion: IonSystem) : ExprFunction {
    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        checkArity(args)

        return args.filterNot { it.type == ExprValueType.NULL || it.type == ExprValueType.MISSING }
                   .firstOrNull() ?: return nullExprValue(ion)
    }

    private fun checkArity(args: List<ExprValue>) {
        if (args.isEmpty()) {
            val errorContext = PropertyValueMap()
            errorContext[Property.EXPECTED_ARITY_MIN] = 1
            errorContext[Property.EXPECTED_ARITY_MAX] = Int.MAX_VALUE

            throw EvaluationException("coalesce requires at least one argument",
                                      ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                      errorContext,
                                      internal = false)
        }
    }
}