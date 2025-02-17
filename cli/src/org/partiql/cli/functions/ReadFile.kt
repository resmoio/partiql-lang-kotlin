/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.cli.functions

import com.amazon.ion.IonStruct
import org.apache.commons.csv.CSVFormat
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.io.DelimitedValues
import org.partiql.lang.eval.io.DelimitedValues.ConversionMode
import org.partiql.lang.eval.stringValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.asIonStruct
import org.partiql.lang.util.booleanValue
import org.partiql.lang.util.stringValue
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader

internal class ReadFile(valueFactory: ExprValueFactory) : BaseFunction(valueFactory) {
    override val signature = FunctionSignature(
        name = "read_file",
        requiredParameters = listOf(StaticType.STRING),
        optionalParameter = StaticType.STRUCT,
        returnType = StaticType.BAG
    )

    private fun conversionModeFor(name: String) =
        ConversionMode.values().find { it.name.toLowerCase() == name }
            ?: throw IllegalArgumentException("Unknown conversion: $name")

    private fun fileReadHandler(csvFormat: CSVFormat): (InputStream, IonStruct) -> ExprValue = { input, options ->
        val encoding = options["encoding"]?.stringValue() ?: "UTF-8"
        val reader = InputStreamReader(input, encoding)
        val conversion = options["conversion"]?.stringValue() ?: "none"

        val hasHeader = options["header"]?.booleanValue() ?: false
        val ignoreEmptyLine = options["ignore_empty_line"]?.booleanValue() ?: true
        val ignoreSurroundingSpace = options["ignore_surrounding_space"]?.booleanValue() ?: true
        val trim = options["trim"]?.booleanValue() ?: true
        val delimiter = options["delimiter"]?.stringValue()?.first() // CSVParser library only accepts a single character as delimiter
        val record = options["line_breaker"]?.stringValue()
        val escape = options["escape"]?.stringValue()?.first() // CSVParser library only accepts a single character as escape
        val quote = options["quote"]?.stringValue()?.first() // CSVParser library only accepts a single character as quote

        val csvFormatWithOptions = csvFormat.withIgnoreEmptyLines(ignoreEmptyLine)
            .withIgnoreSurroundingSpaces(ignoreSurroundingSpace)
            .withTrim(trim)
            .let { if (hasHeader) it.withFirstRecordAsHeader() else it }
            .let { if (delimiter != null) it.withDelimiter(delimiter) else it }
            .let { if (record != null) it.withRecordSeparator(record) else it }
            .let { if (escape != null) it.withEscape(escape) else it }
            .let { if (quote != null) it.withQuote(quote) else it }

        DelimitedValues.exprValue(valueFactory, reader, csvFormatWithOptions, conversionModeFor(conversion))
    }

    private fun ionReadHandler(): (InputStream, IonStruct) -> ExprValue = { input, _ ->
        valueFactory.newBag(valueFactory.ion.iterate(input).asSequence().map { valueFactory.newFromIonValue(it) })
    }

    private val readHandlers = mapOf(
        "ion" to ionReadHandler(),
        "csv" to fileReadHandler(CSVFormat.DEFAULT),
        "tsv" to fileReadHandler(CSVFormat.DEFAULT.withDelimiter('\t')),
        "excel_csv" to fileReadHandler(CSVFormat.EXCEL),
        "mysql_csv" to fileReadHandler(CSVFormat.MYSQL),
        "postgresql_csv" to fileReadHandler(CSVFormat.POSTGRESQL_CSV),
        "postgresql_text" to fileReadHandler(CSVFormat.POSTGRESQL_TEXT),
        "customized" to fileReadHandler(CSVFormat.DEFAULT)
    )

    override fun callWithRequired(env: Environment, required: List<ExprValue>): ExprValue {
        val fileName = required[0].stringValue()
        val fileType = "ion"
        val handler = readHandlers[fileType] ?: throw IllegalArgumentException("Unknown file type: $fileType")
        val seq = Sequence {
            // TODO we should take care to clean up this `FileInputStream` properly
            //  https://github.com/partiql/partiql-lang-kotlin/issues/518
            val fileInput = FileInputStream(fileName)
            handler(fileInput, valueFactory.ion.newEmptyStruct()).iterator()
        }
        return valueFactory.newBag(seq)
    }

    override fun callWithOptional(env: Environment, required: List<ExprValue>, opt: ExprValue): ExprValue {
        val options = opt.ionValue.asIonStruct()
        val fileName = required[0].stringValue()
        val fileType = options["type"]?.stringValue() ?: "ion"
        val handler = readHandlers[fileType] ?: throw IllegalArgumentException("Unknown file type: $fileType")
        val seq = Sequence {
            // TODO we should take care to clean up this `FileInputStream` properly
            //  https://github.com/partiql/partiql-lang-kotlin/issues/518
            val fileInput = FileInputStream(fileName)
            handler(fileInput, options).iterator()
        }
        return valueFactory.newBag(seq)
    }
}
