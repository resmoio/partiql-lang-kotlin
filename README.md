


# IonSQL++ Sandbox
This package is a basic implementation of Ion SQL++, a generalized extended SQL language
that uses the Ion type system natively, but is designed to be used over sparse, hierarchical
data in a relational way.

## Getting Started
Building the package requires a standard Brazil workspace and should be buildable against `live`.

```
$ brazil-build release
```

Building a self-contained JAR with a very basic read-eval-print loop (REPL), can be done
using the `single-jar` target:

```
$ brazil-build single-jar
```

Once this is done the REPL can be used to experiment with Ion SQL.

## Using the REPL
Once built, the REPL can be run by invoking the `com.amazon.ionsql.tools.REPL` program.
If the `single-jar` was built, this can be run as follows:

```
$ java -jar build/lib/ionsql-sandbox-single.jar
```

It may be useful to use `rlwrap` to get a command history support.

You will get a prompt that looks as follows:

```
ionsql> 
```

At this point you can type in Ion SQL and press *double enter* to execute SQL:

```
ionsql> SELECT id FROM `[{id: 5, name:"bill"}, {id: 6, name:"bob"}]` WHERE name = 'bob'
      | 
======'
{
  id:6
}
------

OK!
```

The previous expression is stored in the variable named `_`, so you can then run subsequent
expressions based on the last one.

```
ionsql> SELECT id + 4 AS name FROM _
      | 
======'
{
  name:10
}
------
```

#### Initial Environment
The initial environment for the REPL can be setup with a configuration file, which should be
an IonSQL file with a single `struct` containing the initial *global environment*.

For example a file named `config.sql`, containing the following:

```
{
  'animals':`[
    {name: "Kumo", type: "dog"},
    {name: "Mochi", type: "dog"},
    {name: "Lilikoi", type: "unicorn"},
  ]`,
  'types':`[
    {id: "dog", is_magic: false},
    {id: "cat", is_magic: false},
    {id: "unicorn", is_magic: true},
  ]`,
}
```

Could be loaded into the REPL with `animals` and `types` bound list of `struct` values.

The REPL could be started up with:

```
$ rlwrap java -jar build/lib/ionsql-sandbox-single.jar config.sql
```

And expressions can use the bound names.

```
ionsql> SELECT name, type, is_magic FROM animals, types WHERE type = id
      | 
======'
{
  name:"Kumo",
  type:"dog",
  is_magic:false
}
{
  name:"Mochi",
  type:"dog",
  is_magic:false
}
{
  name:"Lilikoi",
  type:"unicorn",
  is_magic:true
}
------

OK!
```

#### Working with Structure
Let's consider the following *initial environment*:

```
{
  'stores':`[
    {
     id: "5",
     books: [
       {title:"A", price: 5.0, categories:["sci-fi", "action"]},
       {title:"B", price: 2.0, categories:["sci-fi", "comedy"]},
       {title:"C", price: 7.0, categories:["action", "suspense"]},
       {title:"D", price: 9.0, categories:["suspense"]},
     ]
    },
    {
     id: "6",
     books: [
       {title:"A", price: 5.0, categories:["sci-fi", "action"]},
       {title:"E", price: 9.5, categories:["fantasy", "comedy"]},
       {title:"F", price: 10.0, categories:["history"]},
     ]
    }
  ]`
}
```

If we wanted to find all books *as their own rows* with a price greater than `7`
we can use paths on the `FROM` for this:

```
ionsql> SELECT * FROM stores[*].books[*] AS b WHERE b.price > 7
      | 
======'
{
  title:"D",
  price:9.0,
  categories:[
    "suspense"
  ]
}
{
  title:"E",
  price:9.5,
  categories:[
    "fantasy",
    "comedy"
  ]
}
{
  title:"F",
  price:10.0,
  categories:[
    "history"
  ]
}
------

OK!
```

If you wanted to also de-normalize the store ID and title into the above rows:

```
ionsql> SELECT s.id AS store, b.title AS title FROM stores AS s, @s.books AS b WHERE b.price > 7
      | 
======'
{
  store:"5",
  title:"D"
}
{
  store:"6",
  title:"E"
}
{
  store:"6",
  title:"F"
}
------

OK!
```

We can also use sub-queries with paths to predicate on sub-structure without changing the
cardinality.  So if we wanted to find all stores with books having prices greater than
`9.5`

```
ionsql> SELECT * FROM stores AS s
      | WHERE EXISTS(
      |   SELECT * FROM @s.books AS b WHERE b.price > 9.5
      | )
      | 
======'
{
  id:"6",
  books:[
    {
      title:"A",
      price:5.0,
      categories:[
        "sci-fi",
        "action"
      ]
    },
    {
      title:"E",
      price:9.5,
      categories:[
        "fantasy",
        "comedy"
      ]
    },
    {
      title:"F",
      price:10.0,
      categories:[
        "history"
      ]
    }
  ]
}
------

OK!
```

#### Reading/Writing Files
The REPL provides the `read_file` function to stream data from a file. For example:

```
ionsql> SELECT city FROM read_file('data.ion') AS c, `["HI", "NY"]` AS s WHERE c.state = s
      | 
======'
{
  city:"Honolulu"
}
{
  city:"Rochester"
}
------

OK!
```

The REPL also has the capability to write files with the `write_file` function:

```
ionsql> write_file('out.ion', SELECT * FROM _)
      | 
======'
true
------

OK!
```

Functions and expressions can be used in the *global configuration* as well.  Consider
the following `config.ion`:

```
{
  'data': read_file('data.ion')
}
```

The `data` variable will now be bound to file containing Ion:

```
ionsql> SELECT * FROM data
      | 
======'
{
  city:"Seattle",
  state:"WA"
}
{
  city:"Bellevue",
  state:"WA"
}
{
  city:"Honolulu",
  state:"HI"
}
{
  city:"Rochester",
  state:"NY"
}
------

OK!
```

##### TSV/CSV Data
The `read_file` function supports an optional struct argument to add additional parsing options.
Parsing delimited files can be specified with the `type` field with a string `tsv` or `csv`
to parse tab or comma separated values respectively.

```
ionsql> read_file('simple.tsv', `{type:"tsv"}`)
      | 
======' 
{
  _0:"title",
  _1:"category",
  _2:"price"
}
{
  _0:"harry potter",
  _1:"book",
  _2:"7.99"
}
{
  _0:"dot",
  _1:"electronics",
  _2:"49.99"
}
{
  _0:"echo",
  _1:"electronics",
  _2:"99.99"
}
------- 

OK! 
```

The options `struct` can also define if the first row for delimited data should be the
column names with the `header` field.

```
ionsql> read_file('simple.tsv', `{type:"tsv", header:true}`)
      | 
======' 
{
  title:"harry potter",
  category:"book",
  price:"7.99"
}
{
  title:"dot",
  category:"electronics",
  price:"49.99"
}
{
  title:"echo",
  category:"electronics",
  price:"99.99"
}
------- 

OK!
```

Auto conversion can also be specified numeric and timestamps in delimited data.

```
ionsql> read_file('simple.tsv', `{type:"tsv", header:true, conversion:"auto"}`)
      | 
======' 
{
  title:"harry potter",
  category:"book",
  price:7.99
}
{
  title:"dot",
  category:"electronics",
  price:49.99
}
{
  title:"echo",
  category:"electronics",
  price:99.99
}
------- 

OK!
```

Writing TSV/CSV data can be done by specifying the optional `struct` argument to specify output
format to the `write_file` function.  Similar to the `read_file` function, the `type` field
can be used to specify `tsv`, `csv`, or `ion` output.

```
ionsql> write_file('out.csv', `{type:"csv"}`, SELECT name, type FROM animals)
      | 
======' 
true
-------
```

This would produce the following file:

```
$ cat out.csv
Kumo,dog
Mochi,dog
Lilikoi,unicorn
```

The options `struct` can also specify a `header` Boolean field to indicate whether the output
TSV/CSV should have a header row.

```
ionsql> write_file('out.csv', `{type:"csv", header:true}`, SELECT name, type FROM animals)
      | 
======' 
true
-------
```

Which would produce the following file:

```
$ cat out.csv 
name,type
Kumo,dog
Mochi,dog
Lilikoi,unicorn
```

## Builtin Functions

### CHAR_LENGTH, CHARACTER_LENGTH

Counts the number of characters in the specified string, where 'character' is defined as a single unicode codepoint.

Note: `CHAR_LENGTH` and `CHARACTER_LENGTH` have the same syntax and functionality.
  
    CHAR_LENGTH(<str>)
    CHARACTER_LENGTH(<str>)
 
 Returns:
  
 - `NULL` if `<str>` is `NULL` 
 - `MISSING` if `<str>` is `MISSING`.  
 - Otherwise, returns the number of characters in `<str>`.
  
#### Examples

    CHAR_LENGTH('') -- Returns 0 
    CHAR_LENGTH('abcdefg') -- Returns 7
    CHAR_LENGTH('😁😞😸😸') -- Returns 4 (non-BMP unicode characters)
    CHAR_LENGTH('eࠫ') -- Returns 2 because 'eࠫ' is two codepoints: the letter 'e' and combining character U+032B

### EXISTS

Indicates if the specified `IonSequence` is empty.  Always return `false` if the value specified is not a 
sequence.

 - `EXISTS(<sequence>)`
 
#### Examples

`<sequence>` is an empty sequence:

    EXISTS([]) -- Returns false

`<sequence>` is an `IonSequence` containing 3 values:

    EXISTS([1, 2, 3]) -- Returns true

`<sequence>` is not a sequence:

    EXISTS(1) -- Returns false

### LOWER 

Converts uppercase letters in the specified string to lowercase, leaving non-uppercase characters unchanged.
This operation currently relies on the default locale as defined by Java's official 
[String.toLowerCase()](https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#toLowerCase()) documentation.
See [IONSQL-110](https://i.amazon.com/issues/IONSQL-110), which will allow IonSQL++ to have client-specifiable locales.

    LOWER(<str>)

Examples:

    LOWER('AbCdEfG!@#$') -- Returns 'abcdefg!@#$'
    
### SUBSTRING

Extracts part of a string.  

 - `SUBSTRING(<str> FROM <start pos> [FOR <length>])`
 - `SUBSTRING(<str>, <start pos> [, <length>])`
 
Where:

 - `<str>` is the string containing the part to be extracted.
 - `<start pos>` is the 1-based position of the first character (unicode codepoint) to be extracted.
 - `<length>` is the count of characters (unicode codepoints) of the part to be extracted. 

Returns `NULL` if any arugment is null, or `MISSING` if any argument is missing.

#### Examples

    SUBSTRING('abcdefghi' from 3 for 4) -- Returns 'cdef'
    SUBSTRING('abcdefghi', -1, 4) -- Returns 'ab'

### UPPER 

Converts lowercase letters in the specified string to uppercase, leaving non-lowercase characters unchanged. This 
operation currently relies on the default locale as defined by Java's official 
[String.toUpperCase()](https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#toUpperCase()) documentation.
See [IONSQL-110](https://i.amazon.com/issues/IONSQL-110), which will allow IonSQL++ to have client-specifiable locales.

    UPPER(<str>)

Examples:

    UPPER('AbCdEfG!@#$') -- Returns 'ABCDEFG!@#$'
    
## Helpful Links

 - [Hyperlinked SQL-92 BNF](https://ronsavage.github.io/SQL/sql-92.bnf.html) - this is much easier to navigate than the official ISO standard!
 - [sqlfiddle.com](http://sqlfiddle.com/) - Use this to experiment with SQL as implemented by Postgres, MySQL, Oracle, MS SQL Server and SQLite. 

## TODO

* Implement more of the "standard" functions.
* Implement aggregation, sort, grouping.
  
[ionjava]: https://code.amazon.com/packages/IonJava

