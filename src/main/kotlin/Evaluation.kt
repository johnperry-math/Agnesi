import kotlin.math.*
import Nary_Node_Type.*
import Unary_Node_Type.*
import Binary_Node_Type.*
import kotlin.math.pow

/**
 * Interface for enumeration classes that indicate the arity of an [Evaluation_Tree_Node]
 *
 * All nodes descend from this one.
 *
 */
interface Node_Type {
    val repr: String
}

/**
 *
 * Arity identification for a node with no arguments: indeterminate, constant, error.
 *
 * @param representation string representation of the data in this node
 *
 */
enum class Nary_Node_Type(representation: String): Node_Type {
    INDET("x"),         /// indeterminate
    CONST("constant"),  /// constant
    ERR("error");       /// error
    override fun toString() = repr
    override val repr = representation
}

/**
 * Arity identification for a node with one argument;
 * for instance, -(x+3) turns into NEGATE, with one child, PLUS (a binary node),
 * which itself has two children, INDET and CONSTANT.
 *
 * @param representation string representation of the data in this node
 *
 */
enum class Unary_Node_Type(representation: String): Node_Type {
    NEGATE("-"),        /// negation of child
    GROUP("("),         /// grouping: (...)
    SIN("sin"),         /// sine
    COS("cos"),         /// cosine
    TAN("tan"),         /// tangent
    COT("cot"),         /// cotangent
    SEC("sec"),         /// secant
    CSC("csc"),         /// cosecant
    ASIN("arcsin"),     /// arcsin
    ACOS("arccos"),     /// arccos
    ATAN("arctan"),     /// arctan
    ACOT("arccot"),     /// arccot
    ASEC("arcsec"),     /// arcsec
    ACSC("arccsc"),     /// arccsc
    LOG("log"),         /// log_10
    LN("ln"),           /// ln
    EXP("exp"),         /// e^x
    FLOOR("floor"),     /// floor(x)
    SQRT("sqrt"),       /// sqrt(x)
    ABS("abs"),         /// |x|
    RECIPROCAL("1/");   /// reciprocal of the child
    override fun toString() = repr
    override val repr = representation
}

/**
 * Arity identification for a node with two arguments.
 *
 * @param representation string representation of the data in this node
 *
 */
enum class Binary_Node_Type(representation: String): Node_Type {
    PLUS("+"),          /// addition
    MINUS("-"),         /// subtraction
    TIMES("×"),         /// multiplication
    DIV("÷"),           /// division
    POW("pow");         /// exponentiation
    override fun toString() = repr
    override val repr = representation
}

enum class Polynomial_Node_Type: Node_Type {
    TRADITIONAL;

    override val repr = "polynomial"
}

/**
 *
 * Node in an evaluation tree. Its children can be accessed with [] syntax, and size() will also indicate the arity.
 *
 * @property type the node's arity and operation
 * @property children the number of children, which should correspond to the arity
 *
 */
abstract class Evaluation_Tree_Node {
    abstract val type: Node_Type
    internal abstract val children: Array<Evaluation_Tree_Node>
    override fun toString(): String = type.repr
    abstract fun toLaTeXString(): String
    operator fun get(i: Int) = children[i]
    operator fun set(i: Int, operand: Evaluation_Tree_Node) {
        children[i] = operand
    }
    fun size() = children.size
    open fun is_polynomial() = false
}

/**
 *
 * A node needing no children. Initializes `children` to 0.
 *
 */
abstract class Nary_Node(node_type: Nary_Node_Type) : Evaluation_Tree_Node() {
    override val type: Nary_Node_Type = node_type
    override val children = arrayOf<Evaluation_Tree_Node>()
    override fun is_polynomial() = type != ERR
}

/**
 *
 * Representation of a constant.
 *
 * @param value the constant this node stores
 *
 */
class Constant_Node(val value: Double)
    : Nary_Node(CONST)
{
    override fun toString(): String = value.toString()
    override fun toLaTeXString(): String = value.toString()
    override fun is_polynomial() = true
}

/**
 *
 * Representation of an indeterminate: i.e., x, t, ...
 *
 * @param name the indeterminate's name: i.e., x, t, ...
 *
 */
class Indeterminate_Node(private val name: String)
    : Nary_Node(INDET)
{
    override fun toString(): String = name
    override fun toLaTeXString(): String = name
    override fun is_polynomial() = true
}

/**
 *
 * A node needing one child.
 *
 * @param node_type one of the values in [Unary_Node_Type]
 * @param child the node's one child; e.g., for -(x+3) the, `node_type` should be `[NEGATE]`
 *      and `child` should represent x+3
 *
 */
class Unary_Node(node_type: Unary_Node_Type, child: Evaluation_Tree_Node)
    : Evaluation_Tree_Node()
{

    override val type: Unary_Node_Type = node_type
    override val children = arrayOf(child)

    /**
     * @return the node's child
     */
    fun operand() = children[0]

    override fun toString(): String =
        when (type) {

            NEGATE -> "-${children[0]}"

            GROUP -> "(${children[0]})"

            SIN, COS, TAN, COT, SEC, CSC,
            ASIN, ACOS, ATAN, ACOT, ASEC, ACSC,
            LN, LOG, FLOOR, SQRT, ABS -> "${type.repr}(${children[0]})"

            EXP -> "e^(${children[0]})"

            RECIPROCAL -> "1/(${children[0]})"

        }

    override fun toLaTeXString(): String =
        when (type) {

            NEGATE -> "-" +
                    ( if ((children[0] !is Unary_Node)) "\\left(" else "" ) +
                    children[0].toLaTeXString() +
                    ( if ((children[0] !is Unary_Node)) "\\right)" else "" )

            GROUP -> "\\left(" + children[0].toLaTeXString() + "\\right)"

            SIN, COS, TAN, COT, SEC, CSC,
            ASIN, ACOS, ATAN, ACOT, ASEC, ACSC,
            LN, LOG -> "\\" + "${type.repr}(" + children[0].toLaTeXString() + ")"

            FLOOR -> "\\lfloor" + children[0].toLaTeXString() + "\\rfloor"

            SQRT -> "\\sqrt{" + children[0].toLaTeXString() + "}"

            ABS -> "\\left|" + children[0].toLaTeXString() + "\\right|"

            EXP -> "e^{" + children[0].toLaTeXString() + "}"

            RECIPROCAL -> "\\frac{1}{" + children[0].toLaTeXString() + "}"

        }

    override fun is_polynomial() =
        when (type) {
            NEGATE, GROUP -> children[0].is_polynomial()
            else -> false
        }
}

/**
 *
 * A node needing two children, `left` and `right`.
 *
 */
class Binary_Node(
    node_type: Binary_Node_Type,
    left: Evaluation_Tree_Node, right: Evaluation_Tree_Node
) : Evaluation_Tree_Node()
{

    override val type = node_type
    override val children = arrayOf(left, right)

    /**
     * @return the left child
     */
    fun first_op() = children[0]

    /**
     * @return the right child
     */
    fun second_op() = children[1]

    override fun toString(): String =
        when (type) {

            PLUS, MINUS, TIMES, DIV -> "${children[0]} ${type.repr} ${children[1]}"

            POW -> "${children[0]} ^ ${children[1]}"

        }

    override fun toLaTeXString(): String =
        when (type) {

            PLUS, MINUS ->
                 children[0].toLaTeXString() + type.repr + children[1].toLaTeXString()

            TIMES -> children[0].toLaTeXString() + "\\cdot " + children[1].toLaTeXString()

            DIV -> "\\frac{" + children[0].toLaTeXString() + "}{" +
                    children[1].toLaTeXString() + "}"

            POW -> children[0].toLaTeXString() + "^{" + children[1].toLaTeXString() + "}"

        }

    override fun is_polynomial() =
        when (type) {
            PLUS, MINUS, TIMES -> children[0].is_polynomial() && children[1].is_polynomial()
            POW -> children[0].is_polynomial() && (children[1].type == CONST) &&
                    (ceil((children[1] as Constant_Node).value) == (children[1] as Constant_Node).value) &&
                    (floor((children[1] as Constant_Node).value) == (children[1] as Constant_Node).value)
            else -> false
        }

}

/**
 *
 * A node used when an error occurs in parsing a string. `children` is empty.
 *
 * @param position indicates the location of the character in the string; for "sin x" it should be 4
 *
 */
class Error_Node(val position: Int) : Nary_Node(ERR) {
    override val children = arrayOf<Evaluation_Tree_Node>()
    override fun toLaTeXString(): String = "error"
}

class Polynomial_Node(
    val indet: String,
    val degree: Int,
    val coefficients: Array<Double> = Array(degree + 1) { 0.0 }
): Evaluation_Tree_Node() {
    override val type = Polynomial_Node_Type.TRADITIONAL
    override fun is_polynomial() = true
    override val children = arrayOf<Evaluation_Tree_Node>()
    override fun toString(): String {
        var result = ""
        var first = true
        for (i in coefficients.indices.reversed()) {
            if (coefficients[i] != 0.0) {
                if (coefficients[i] < 0.0) result += "-"
                else if (!first) result += "+"
                if (first) first = false
                else result += " "
                if (abs(coefficients[i]) != 1.0) result += abs(coefficients[i]).toString()
                if (i > 1) result += " $indet^$i "
                else if (i == 1) result += " $indet "
            }
        }
        return result
    }

    override fun toLaTeXString(): String {
        var result = ""
        var first = true
        for (i in coefficients.indices.reversed()) {
            if (coefficients[i] != 0.0) {
                if (coefficients[i] < 0.0) result += "-"
                else if (!first) result += "+"
                if (first) first = false
                if (abs(coefficients[i]) != 1.0) result += abs(coefficients[i]).toString()
                if (i > 1) result += "$indet^i"
                else if (i == 1) result += indet
            }
        }
        return result
    }
}

/**
 *
 * Detects when `expr` is a power product: i.e., x^a, where x is an indeterminate and a is a constant.
 * Eventually we will use this for simplifying to polynomials.
 *
 * TODO integrate with code and check
 *
 */
@Suppress("unused")
private fun is_power_product(expr: Evaluation_Tree_Node) =
    (expr.type == POW) && (expr[0] is Indeterminate_Node) && (expr[1] is Constant_Node)

/**
 *
 * Detects when `expr` is a constant multiple: i.e,. af(x) where a is a constant.
 *
 */
private fun is_constant_multiple(expr: Evaluation_Tree_Node) =
    (expr.type == TIMES)
            && ( expr[0] is Constant_Node || expr[1]  is Constant_Node )

/**
 * transforms `current` into a [Polynomial_Node] when possible
 */
private fun make_polynomial_if_possible(current: Evaluation_Tree_Node): Evaluation_Tree_Node =
    if (!current.is_polynomial()) {
        current
    } else {
        when (current.type) {
            CONST -> {
                val coefficients = arrayOf((current as Constant_Node).value)
                Polynomial_Node("x", 0, coefficients)
            }
            INDET -> {
                val coefficients = arrayOf( 0.0, 1.0 )
                Polynomial_Node((current as Indeterminate_Node).toString(), 1, coefficients)
            }
            GROUP -> make_polynomial_if_possible(current.children[0])
            NEGATE -> {
                val child = make_polynomial_if_possible(current.children[0]) as Polynomial_Node
                val d = child.degree
                val coefficients = Array(d + 1) { i -> -child.coefficients[i] }
                Polynomial_Node(child.indet, d, coefficients)
            }
            PLUS, MINUS -> {
                val first = make_polynomial_if_possible(current[0]) as Polynomial_Node
                val second = make_polynomial_if_possible(current[1]) as Polynomial_Node
                val indet = if (first.degree > second.degree) first.indet else second.indet
                var d = max( first.degree, second.degree )
                val coefficients = Array(d + 1) { 0.0 }
                for (i in 0..first.degree) coefficients[i] = first.coefficients[i]
                when (current.type) {
                    PLUS -> for (i in 0..second.degree) coefficients[i] += second.coefficients[i]
                    MINUS -> for (i in 0..second.degree) coefficients[i] -= second.coefficients[i]
                }
                while (d > 0 && coefficients[d] == 0.0) d -= 1
                Polynomial_Node(indet, d, coefficients)
            }
            TIMES -> {
                val first = make_polynomial_if_possible(current[0]) as Polynomial_Node
                val second = make_polynomial_if_possible(current[1]) as Polynomial_Node
                val indet = if (first.degree > second.degree) first.indet else second.indet
                var d = first.degree + second.degree
                val coefficients = Array(d + 1) { 0.0 }
                for (i in 0..first.degree)
                    for (j in 0..second.degree)
                        coefficients[i + j] += first.coefficients[i] * second.coefficients[j]
                while (d > 0 && coefficients[d] == 0.0) d -= 1
                Polynomial_Node(indet, d, coefficients)
            }
            POW -> when (current[0].type) {
                INDET -> {
                    val d = (current[1] as Constant_Node).value.toInt()
                    val coefficients = Array(d + 1) { 0.0 }
                    coefficients[d] = 1.0
                    Polynomial_Node((current[0] as Indeterminate_Node).toString(), d, coefficients)
                }
                CONST -> {
                    Polynomial_Node(
                        "x",
                        0,
                        arrayOf( 0.0, ((current[0] as Constant_Node).value).pow((current[1] as Constant_Node).value) )
                    )
                }
                else -> {
                    val base = make_polynomial_if_possible(current[0]) as Polynomial_Node
                    val power = (current[1] as Constant_Node).value.toInt()
                    val coefficients = base.coefficients
                    var prev = base.coefficients
                    var next = Array(2 * prev.size - 1) { 0.0 }
                    for (each in 2..power) {
                        for (i in coefficients.indices)
                            for (j in prev.indices) {
                                next[i + j] += coefficients[i] * prev[j]
                            }
                        prev = next
                        if (each < power) {
                            next = Array(next.size + coefficients.size - 1) { 0.0 }
                        }
                    }
                    var d = power * base.degree
                    while (next[d] == 0.0) d -= 1
                    Polynomial_Node(base.indet, d, next)
                }
            }
            else -> Error_Node(-1)
        }
    }

/**
 *
 * Simplifies `current`'s children recursively.
 * Implemented but not yet integrated or even checked.
 *
 * The current version performs the following algorithm:
 * 1. simplifies the children
 * 1. if `this` has the form ( ( f(x) ) ) , returns ( f(x) )
 * 1. else if `this` is addition:
 *    a. a+0 -> a
 *    a. 0+a -> a
 *    a. simplifies sum of constants
 * 1. else if `this` is multiplication:
 *    a. a*1 -> a
 *    a. 1*a -> a
 *    a. a*0 -> 0
 *    a. 0*a -> 0
 *    a. constant multiple with representation as multiplication converted to constant multiple, or even simplified
 *    a. simplifies product of constants
 *
 * TODO integrate with code and check
 *
 */
@Suppress("unused")
private fun simplify(current: Evaluation_Tree_Node): Evaluation_Tree_Node {
    return if (current.is_polynomial() && ( current.type == PLUS || current.type == MINUS || current.type == POW ) )
        make_polynomial_if_possible(current)
    else {
        for (i in current.children.indices)
            current[i] = simplify(current[i])
        // eliminate redundant groups
        if (current.type == GROUP) {
            if (
                current[0].type == GROUP || current[0] is Nary_Node ||
                (current[0] is Polynomial_Node && (current[0] as Polynomial_Node).degree == 0)
            )
                current[0]
            else current
        }
        // eliminate addition by 0 and simplify constant addition
        else if (current.type == PLUS) {
            if (current[0] is Constant_Node && ((current[0] as Constant_Node).value == 0.0))
                current[1]
            else if (current[1] is Constant_Node && ((current[1] as Constant_Node).value == 0.0))
                current[0]
            else if (current[0] is Constant_Node && current[1] is Constant_Node)
                Constant_Node((current[0] as Constant_Node).value + (current[1] as Constant_Node).value)
            else current
        }
        // eliminate multiplication by 0 or 1 and simplify constant multiplication
        else if (current.type == TIMES) {
            if (current[0] is Constant_Node && ((current[0] as Constant_Node).value == 1.0))
                return current[1]
            else if (current[1] is Constant_Node && ((current[1] as Constant_Node).value == 1.0))
                return current[0]
            else if (current[0] is Constant_Node && current[1] is Constant_Node)
                return Constant_Node((current[0] as Constant_Node).value * (current[1] as Constant_Node).value)
            else if (current[0] is Constant_Node && (current[0] as Constant_Node).value == 0.0)
                return Constant_Node(0.0)
            else if (current[1] is Constant_Node && (current[1] as Constant_Node).value == 0.0)
                return Constant_Node(0.0)
            else if (current[0] is Constant_Node && is_constant_multiple(current[1])) {
                if (current[1][0].type == CONST)
                    return Binary_Node(TIMES,
                        Constant_Node(
                            (current[0] as Constant_Node).value
                                    * (current[1][0] as Constant_Node).value
                        ),
                        current[1][1]
                    )
                else
                    return Binary_Node(TIMES,
                        Constant_Node(
                            (current[0] as Constant_Node).value
                                    * (current[1][1] as Constant_Node).value
                        ),
                        current[1][0]
                    )
            } else if (current[1] is Constant_Node && is_constant_multiple(current[0])) {
                if (current[0][0].type == CONST)
                    return Binary_Node(TIMES,
                        Constant_Node(
                            (current[1] as Constant_Node).value
                                    * (current[0][0] as Constant_Node).value
                        ),
                        current[0][1]
                    )
                else
                    return Binary_Node(TIMES,
                        Constant_Node(
                            (current[1] as Constant_Node).value
                                    * (current[0][1] as Constant_Node).value
                        ),
                        current[0][0]
                    )
            } else current
        } else current
    }
}

/**
 * @return a node representing the reciprocal of `child`
 */
private fun reciprocal(child: Evaluation_Tree_Node) = Unary_Node(RECIPROCAL, child)

/**
 * @return a node representing the square of `child`
 */
private fun square(child: Evaluation_Tree_Node) = Binary_Node(POW, child, Constant_Node(2.0))

/**
 * Parses a constant located at `position` in `expression`.
 *
 * EBNF: [ `-` ] { `0` | `1` | ... | `9` } [ `.` { `0` | `1` | ... | `9` } ]
 *
 * @return a node to store the constant and the next symbol's position
 *
 */
fun constant(expression: String, position: Int): Pair<Constant_Node, Int> {
    var value = 0.0
    var i = position
    val is_negative = expression[i] == '-'
    if (is_negative) i += 1
    while ((i < expression.length) && (expression[i] in '0'..'9')) {
        value *= 10
        value += expression[i].toInt() - '0'.toInt()
        ++i
    }
    if ((i < expression.length) && (expression[i] == '.')) {
        ++i
        var denominator = 1.0
        while (expression[i] in '0'..'9') {
            denominator /= 10.0
            value += denominator * ( expression[i].toInt() - '0'.toInt() )
            ++i
        }
    }
    if (is_negative) value *= -1
    return Pair( Constant_Node(value), i)
}

/**
 * @return true if and only if grouping begins: the character at position `pos` in `expr` is '('
 */
private fun group_begins(expr: String, pos: Int): Boolean = expr[pos] == '('

/**
 * @return true if and only if grouping ends: the character at position `pos` in `expr` is ')'
 */
private fun group_ends(expr: String, pos: Int): Boolean = expr[pos] == ')'

/**
 *
 * Recognizes if `x` appears `expr` at `pos`; i.e., if the indeterminate appears here.
 *
 * This allows indeterminates with names longer than one character.
 *
 */
private fun indeterminate(x: String, expr: String, pos: Int) = expr.substring(pos, pos + x.length) == x

/**
 *
 * Parses a factor located at position `pos` in `expr`.
 *
 * Grouping of terms within a factor is recognized either via (...) or |...|.
 * The former becomes a [GROUP]; the latter, an [ABS].
 * This is also where we pick up functions such as [SIN], [EXP], etc.
 *
 * EBNF: constant | `-` expression | `(` expression `)` | `|` expressions `|`
 *      | unary_function `(` expression `)` | indeterminate
 *
 * @param x the current indeterminate
 *
 * @return a node to store the factor and the next symbol's position
 *
 */
private fun factor(x: String, expr: String, pos: Int): Pair<Evaluation_Tree_Node, Int> {

    var (result, i) =
        when (expr[pos]) {
            in '0'..'9' -> constant(expr, pos)
            '-' -> {
                val (child, j) = factor(x, expr, pos + 1)
                Pair(Unary_Node(NEGATE, child), j)
            }
            '(' -> {
                var (child, j) = expression(x, expr, pos + 1)
                child = Unary_Node(GROUP, child)
                if (expr[j] == ')') Pair(child, j + 1)
                else Pair(Error_Node(j), expr.length)
            }
            '|' -> {
                var (child, j) = expression(x, expr, pos + 1)
                child = Unary_Node(ABS, child)
                if (expr[j] == '|') Pair(child, j + 1)
                else Pair(Error_Node(j), expr.length)
            }
            in 'a'..'z' -> {
                if (indeterminate(x, expr, pos)) Pair(Indeterminate_Node(x), pos + x.length)
                else when(expr[pos]) {
                    'a' ->
                        if (pos + 6 < expr.length) {
                            when (expr.substring(pos, pos + 6)) {
                                "arcsin" ->
                                    if (group_begins(expr, pos + 6) && expr.length >= pos + 8) {
                                        val (child, j) = expression(x, expr, pos + 7)
                                        if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(ASIN, child), j + 1)
                                        else Pair(Error_Node(j), expr.length)
                                    } else Pair(Error_Node(pos + 6), expr.length)
                                "arccos" ->
                                    if (group_begins(expr, pos + 6) && expr.length >= pos + 8) {
                                        val (child, j) = expression(x, expr, pos + 7)
                                        if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(ACOS, child), j + 1)
                                        else Pair(Error_Node(j), expr.length)
                                    } else Pair(Error_Node(pos + 6), expr.length)
                                "arctan" ->
                                    if (group_begins(expr, pos + 6) && expr.length >= pos + 8) {
                                        val (child, j) = expression(x, expr, pos + 7)
                                        if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(ATAN, child), j + 1)
                                        else Pair(Error_Node(j), expr.length)
                                    } else Pair(Error_Node(pos + 6), expr.length)
                                "arccot" ->
                                    if (group_begins(expr, pos + 6) && expr.length >= pos + 8) {
                                        val (child, j) = expression(x, expr, pos + 7)
                                        if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(ACOT, child), j + 1)
                                        else Pair(Error_Node(j), expr.length)
                                    } else Pair(Error_Node(pos + 6), expr.length)
                                "arcsec" ->
                                    if (group_begins(expr, pos + 6) && expr.length >= pos + 8) {
                                        val (child, j) = expression(x, expr, pos + 7)
                                        if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(ASEC, child), j + 1)
                                        else Pair(Error_Node(j), expr.length)
                                    } else Pair(Error_Node(pos + 6), expr.length)
                                "arccsc" ->
                                    if (group_begins(expr, pos + 6) && expr.length >= pos + 8) {
                                        val (child, j) = expression(x, expr, pos + 7)
                                        if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(ACSC, child), j + 1)
                                        else Pair(Error_Node(j), expr.length)
                                    } else Pair(Error_Node(pos + 6), expr.length)
                                else -> Pair(Error_Node(pos), expr.length)
                            }
                        } else Pair(Error_Node(pos), expr.length)
                    'c' ->
                        if ((pos + 3 < expr.length) and (expr.substring(pos, pos + 3) == "cos")) {
                            if (group_begins(expr, pos + 3) and (expr.length >= pos + 5)) {
                                val (child, j) = expression(x, expr, pos + 4)
                                if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(COS, child), j + 1)
                                else Pair(Error_Node(j), expr.length)
                            } else Pair(Error_Node(pos + 3), expr.length)
                        } else if ((pos + 3 < expr.length) and (expr.substring(pos, pos + 3) == "cot")) {
                            if (group_begins(expr, pos + 3) and (expr.length >= pos + 5)) {
                                val (child, j) = expression(x, expr, pos + 4)
                                if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(COT, child), j + 1)
                                else Pair(Error_Node(j), expr.length)
                            } else Pair(Error_Node(pos + 3), expr.length)
                        } else if ((pos + 3 < expr.length) and (expr.substring(pos, pos + 3) == "csc")) {
                            if (group_begins(expr, pos + 3) and (expr.length >= pos + 5)) {
                                val (child, j) = expression(x, expr, pos + 4)
                                if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(CSC, child), j + 1)
                                else Pair(Error_Node(j), expr.length)
                            } else Pair(Error_Node(pos + 3), expr.length)
                        } else if (x[0] == 'c')
                            Pair(Indeterminate_Node(x), pos + 1)
                        else Pair(Error_Node(pos), expr.length)
                    'e' ->
                        if ((pos + 1 < expr.length) && (expr[pos + 1] == '^')) {
                            if ((pos + 2 < expr.length) && group_begins(
                                    expr,
                                    pos + 2
                                ) && (expr.length >= pos + 4)
                            ) {
                                val (child, j) = expression(x, expr, pos + 3)
                                if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(EXP, child), j + 1)
                                else Pair(Error_Node(j), expr.length)
                            } else {
                                val (child, j) = factor(x, expr, pos + 2)
                                Pair(Unary_Node(EXP, child), j)
                            }
                        } else Pair(Unary_Node(EXP, Constant_Node(1.0)), pos + 1)
                    'f' ->
                        if ((pos + 5 < expr.length) and (expr.substring(pos, pos + 5) == "floor")) {
                            if (group_begins(expr, pos + 5) and (expr.length >= pos + 7)) {
                                val (child, j) = expression(x, expr, pos + 6)
                                if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(FLOOR, child), j + 1)
                                else Pair(Error_Node(j), expr.length)
                            } else Pair(Error_Node(pos + 5), expr.length)
                        } else Pair(Error_Node(pos), expr.length)
                    'l' ->
                        if ((pos + 2 < expr.length) and (expr.substring(pos, pos + 2) == "ln")) {
                            if (group_begins(expr, pos + 2) and (expr.length >= pos + 4)) {
                                val (child, j) = expression(x, expr, pos + 3)
                                if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(LN, child), j + 1)
                                else Pair(Error_Node(j), expr.length)
                            } else Pair(Error_Node(pos + 2), expr.length)
                        } else if ((pos + 3 < expr.length) and (expr.substring(pos, pos + 3) == "log")) {
                            if (group_begins(expr, pos + 3) and (expr.length >= pos + 5)) {
                                val (child, j) = expression(x, expr, pos + 4)
                                if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(LOG, child), j + 1)
                                else Pair(Error_Node(j), expr.length)
                            } else Pair(Error_Node(pos + 3), expr.length)
                        } else Pair(Error_Node(pos), expr.length)
                    'p' ->
                        if ((pos + 1 < expr.length) && (expr.substring(pos, pos + 1) == "pi"))
                            Pair(Constant_Node(PI), pos + 2)
                        else Pair(Error_Node(pos), expr.length)
                    's' ->
                        if ((pos + 3 < expr.length) && (expr.substring(pos, pos + 3) == "sin")) {
                            if (group_begins(expr, pos + 3) and (expr.length >= pos + 5)) {
                                val (child, j) = expression(x, expr, pos + 4)
                                if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(SIN, child), j + 1)
                                else Pair(Error_Node(j), expr.length)
                            } else Pair(Error_Node(pos + 3), expr.length)
                        } else if ((pos + 3 < expr.length) && (expr.substring(pos, pos + 3) == "sec")) {
                            if (group_begins(expr, pos + 3) && (expr.length >= pos + 5)) {
                                val (child, j) = expression(x, expr, pos + 4)
                                if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(SEC, child), j + 1)
                                else Pair(Error_Node(j), expr.length)
                            } else Pair(Error_Node(pos + 3), expr.length)
                        } else if ((pos + 4 < expr.length) && (expr.substring(pos, pos + 4) == "sqrt")) {
                            if (group_begins(expr, pos + 4) && (expr.length >= pos + 6)) {
                                val (child, j) = expression(x, expr, pos + 5)
                                if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(SQRT, child), j + 1)
                                else Pair(Error_Node(j), expr.length)
                            } else Pair(Error_Node(pos + 4), expr.length)
                        } else if (x[0] == 's')
                            Pair(Indeterminate_Node(x), pos + 1)
                        else Pair(Error_Node(pos), expr.length)
                    't' ->
                        if ((pos + 3 < expr.length) and (expr.substring(pos, pos + 3) == "tan")) {
                            if (group_begins(expr, pos + 3) and (expr.length >= pos + 5)) {
                                val (child, j) = expression(x, expr, pos + 4)
                                if (j < expr.length && group_ends(expr, j)) Pair(Unary_Node(TAN, child), j + 1)
                                else Pair(Error_Node(j), expr.length)
                            } else Pair(Error_Node(pos + 3), expr.length)
                        } else if (x[0] == 't')
                            Pair(Indeterminate_Node(x), pos + 1)
                        else Pair(Error_Node(pos), expr.length)
                    else -> Pair(Error_Node(pos), expr.length)
                }
            }
            //x[0] -> Pair(Indeterminate_Node(x), pos + 1)
            else -> Pair(Error_Node(pos), expr.length)
        }

    if ((i < expr.length) && (expr[i] == '^')) {
        val (child, j) = factor(x, expr, i+1)
        result = Binary_Node(POW, result, child)
        i = j
    }

    return Pair(result, i)
}

/**
 *
 * Parses a term located at position `pos` in `expr`.
 *
 * EBNF: factor { ( * | / ) factor }
 *
 * @param x the current indeterminate
 *
 * @return a node to store the factor and the next symbol's position
 *
 */
private fun term(x: String, expr: String, pos: Int): Pair<Evaluation_Tree_Node, Int> {

    var (result, i) = factor(x, expr, pos)
    while ((i < expr.length) && ((expr[i] == '*') or (expr[i] == '/'))) {

        val (child, j) = factor(x, expr, i+1)
        result = Binary_Node(
            if (expr[i] == '*') TIMES else DIV ,
            result ,
            child
        )
        i = j
    }

    return Pair(result, i)
}

/**
 *
 * Parses an expression located at position `pos` in `expr`.
 *
 * EBNF: term { ( + | - ) term }
 *
 * @param x the current indeterminate
 *
 * @return a node to store the factor and the next symbol's position
 *
 */
private fun expression(x: String, expr: String, pos: Int): Pair<Evaluation_Tree_Node, Int> {
    var (result, i) = term(x, expr, pos)
    while ( (i < expr.length) && (expr[i] != ')') && (expr[i] != '|') ) {
        result = when (expr[i]) {
            '+' -> {
                val (child, j) = term(x, expr, i+1)
                i = j
                Binary_Node(PLUS, result, child)
            }
            '-' -> {
                val (child, j) = term(x, expr, i+1)
                i = j
                Binary_Node(MINUS, result, child)
            }
            else -> {
                i = expr.length
                Error_Node(i)
            }

        }
    }
    return Pair(result, i)
}

/**
 *
 * Entry point for parsing an expression.
 *
 * @param ind_var string representing the independent variable; this will be recognized as an indeterminate; i.e., x, t, ...
 * @param expression an expression according to the EBNF outlined in other procedures
 *
 * @return a [Evaluation_Tree_Node] and the position of the first unprocessed symbol in `expression`;
 *      if this latter is not `expression.size()` then an error has occurred
 *
 */
fun parse(ind_var: String, expression: String) =
    expression(ind_var, expression.replace(" ", ""), 0).first

/**
 * Recursively evaluates `expr` for x=`value` and returns the result.
 */
fun evaluate(expr: Evaluation_Tree_Node, value: Double): Double =

    when(expr) {

        is Nary_Node ->
            when (expr.type) {
                INDET -> value
                CONST -> (expr as Constant_Node).value
                ERR -> throw Exception(
                    "Error node encountered at ${(expr as Error_Node).position}; must have received bad input"
                )
            }

        is Unary_Node ->
            when (expr.type) {
                NEGATE -> -evaluate(expr[0], value)
                GROUP -> evaluate(expr[0], value)
                SIN -> sin(evaluate(expr[0], value))
                COS -> cos(evaluate(expr[0], value))
                TAN -> tan(evaluate(expr[0], value))
                COT -> tan(PI/2 - evaluate(expr[0], value))
                SEC -> 1.0 / cos(evaluate(expr[0], value))
                CSC -> 1.0 / sin(evaluate(expr[0], value))
                ASIN -> asin(evaluate(expr[0], value))
                ACOS -> acos(evaluate(expr[0], value))
                ATAN -> atan(evaluate(expr[0], value))
                ACOT -> PI/2 - atan(evaluate(expr[0], value))
                ASEC -> acos(1.0 / evaluate(expr[0], value))
                ACSC -> asin(1.0 / evaluate(expr[0], value))
                LOG -> log(evaluate(expr[0], value), 10.0)
                LN -> ln(evaluate(expr[0], value))
                EXP -> exp(evaluate(expr[0], value))
                FLOOR -> floor(evaluate(expr[0], value))
                SQRT -> sqrt(evaluate(expr[0], value))
                ABS -> abs(evaluate(expr[0], value))
                RECIPROCAL -> 1.0 / evaluate(expr[0], value)
            }

        is Binary_Node ->
            when (expr.type) {
                PLUS -> evaluate(expr[0], value) + evaluate(expr[1], value)
                MINUS -> evaluate(expr[0], value) - evaluate(expr[1], value)
                TIMES -> evaluate(expr[0], value) * evaluate(expr[1], value)
                DIV -> evaluate(expr[0], value) / evaluate(expr[1], value)
                POW -> evaluate(expr[0], value).pow((evaluate(expr[1], value)))
            }

        else -> {
            throw Exception("Unknown node type encountered in evaluation; terminating")
        }

    }

/**
 * @return `true` if and only if `expr` represents a constant;
 *      this should work even on expressions such as 2^(3+|-5|)*arcsin(4)
 */
fun is_constant(expr: Evaluation_Tree_Node): Boolean =
    when (expr.type) {
        CONST -> true
        else ->
            if (expr.size() == 0) false
            else when (expr) {
                is Unary_Node -> is_constant(expr.operand())
                is Binary_Node -> is_constant(expr.first_op()) && is_constant(expr.second_op())
                else -> false
            }
    }

/**
 * A convenience function to help simplify easy cases of the chain rule f(u).
 *
 * @param first represents f'(u)
 * @param second u
 *
 * @return f'(u) * u', simplified for easy cases such as `u` being a constant or an indeterminate
 *
 */
fun chain_rule(first: Evaluation_Tree_Node, second: Evaluation_Tree_Node): Evaluation_Tree_Node =
    when {
        is_constant(second) -> Constant_Node(0.0)
        second is Indeterminate_Node -> first
        else -> Binary_Node(TIMES, first, Unary_Node(GROUP, derivative(second)))
    }

/**
 *
 * Computes and returns the derivative of the expression in `expr`.
 *
 */
fun derivative(expr: Evaluation_Tree_Node): Evaluation_Tree_Node =

    when (expr) {

        is Nary_Node ->
            when (expr.type) {
                INDET -> Constant_Node(1.0)
                CONST -> Constant_Node(0.0)
                ERR -> throw Exception("Error node encountered; must have received bad input")
            }

        is Unary_Node ->
            when(expr.type) {
                NEGATE -> Unary_Node(NEGATE, derivative(expr[0]))
                GROUP -> Unary_Node(GROUP, derivative(expr[0]))
                SIN -> chain_rule(Unary_Node(COS, expr[0]), expr[0])
                COS -> Unary_Node(NEGATE,
                    chain_rule(Unary_Node(SIN, expr[0]), expr[0])
                )
                TAN -> chain_rule(
                    square(Unary_Node(SEC, expr[0])),
                    expr[0]
                )
                COT -> Unary_Node(NEGATE,
                    chain_rule(
                        square(Unary_Node(CSC, expr[0])),
                        expr[0]
                    )
                )
                SEC -> chain_rule(
                    Binary_Node(TIMES, Unary_Node(SEC, expr[0]), Unary_Node(TAN, expr[0])),
                    expr[0]
                )
                CSC -> Unary_Node(NEGATE,
                    chain_rule(
                        Binary_Node(TIMES, Unary_Node(CSC, expr[0]), Unary_Node(COT, expr[0])),
                        expr[0]
                    )
                )
                ASIN -> chain_rule(
                    Binary_Node(
                        POW,
                        Binary_Node(MINUS, Constant_Node(1.0), square(expr[0])),
                        Constant_Node(-0.5)
                    ),
                    expr[0]
                )
                ACOS -> Unary_Node(NEGATE,
                    chain_rule(
                        Binary_Node(POW,
                            Binary_Node(MINUS, Constant_Node(1.0), square(expr[0])),
                            Constant_Node(-0.5)
                        ),
                        expr[0]
                    )
                )
                ATAN -> chain_rule(
                    reciprocal(Binary_Node(PLUS, Constant_Node(1.0), square(expr[0]))),
                    expr[0]
                )
                ACOT -> Unary_Node(NEGATE,
                    chain_rule(
                        reciprocal(Binary_Node(PLUS, Constant_Node(1.0), square(expr[0]))),
                        expr[0]
                    )
                )
                ASEC -> chain_rule(
                    Binary_Node(
                        POW,
                        Binary_Node(MINUS, square(expr[0]), Constant_Node(1.0)),
                        Constant_Node(-0.5)
                    ),
                    expr[0]
                )
                ACSC -> Unary_Node(NEGATE,
                    chain_rule(
                        Binary_Node(
                            POW,
                            Binary_Node(MINUS, square(expr[0]), Constant_Node(1.0)),
                            Constant_Node(-0.5)
                        ),
                        expr[0]
                    )
                )
                LOG -> chain_rule(
                    Binary_Node(
                        TIMES,
                        Unary_Node(RECIPROCAL, expr[0]),
                        Constant_Node(ln(10.0))
                    ),
                    expr[0]
                )
                LN -> chain_rule(
                    Unary_Node(RECIPROCAL, expr[0]),
                    expr[0]
                )
                EXP -> chain_rule(Unary_Node(EXP, expr[0]), expr[0])
                RECIPROCAL -> chain_rule(
                    Unary_Node(NEGATE, square(Unary_Node(RECIPROCAL, expr[0]))),
                    expr[0]
                )
                FLOOR -> Constant_Node(0.0)
                SQRT -> chain_rule(Unary_Node(NEGATE,
                    Unary_Node(RECIPROCAL,
                        Binary_Node(TIMES,
                            Constant_Node(2.0), Unary_Node(SQRT, expr[0])))),
                    expr[0]
                )
                ABS -> chain_rule(Binary_Node(DIV, Unary_Node(ABS, expr[0]), expr[0]), expr[0])
            }

        is Binary_Node ->
            when(expr.type) {
                PLUS -> Binary_Node(PLUS, derivative(expr[0]), derivative(expr[1]))
                MINUS -> Binary_Node(MINUS, derivative(expr[0]), derivative(expr[1]))
                TIMES -> Binary_Node(PLUS,
                    Binary_Node(TIMES, derivative(expr[0]), expr[1]),
                    Binary_Node(TIMES, expr[0], derivative(expr[1]))
                )
                DIV -> Binary_Node(DIV,
                    Binary_Node(PLUS,
                        Binary_Node(PLUS, derivative(expr[0]), expr[1]),
                        Binary_Node(TIMES, expr[0], derivative(expr[1]))
                    ),
                    square(expr[1])
                )
                POW ->
                    if (is_constant(expr[1]))
                        chain_rule(
                            Binary_Node(
                                TIMES, expr[1],
                                Binary_Node(
                                    POW,
                                    expr[0],
                                    Constant_Node((expr[1] as Constant_Node).value - 1.0)
                                )
                            ),
                            expr[0]
                        )
                    else {
                        Binary_Node(
                            TIMES,
                            Binary_Node(POW, expr[0], expr[1]),
                            Binary_Node(
                                PLUS,
                                Binary_Node(
                                    TIMES,
                                    derivative(expr[1]),
                                    Unary_Node(LN, expr[0])),
                                Binary_Node(
                                    DIV,
                                    Binary_Node(TIMES, expr[1], derivative(expr[0])),
                                    expr[0]
                                )
                            )
                        )
                    }

            }

        else -> throw Exception("Unknown node type encountered while computing derivative; terminating")
    }

/*fun main() {
    println("Hello")

    val expr = parse("x", "x + sin(2*x)")
    val b = PI/4
    println("f(x) = $expr")
    println("f($b) = ${evaluate(expr, b)}")
    println()

    val expr2 = parse("x", "(3*x^4+2)^5")
    val a = -2.0
    println("g(x) = $expr2")
    println("g($a) = ${evaluate(expr2, a)}")
    println()

    val deriv = derivative(expr)
    println("f'(x) = $deriv")
    println("f'($b) = ${evaluate(deriv, b)}")
    println()
    println("simplified: ${simplify(deriv)}")
    println()

    val deriv2 = derivative(expr2)
    println("g'(x) = $deriv2")
    println("g'($a) = ${evaluate(deriv2, a)}")
    println()
    println("simplified: ${simplify(deriv2)}")
    println()

    val expr3 = parse("x", "e^(x/2) - 3")
    println("h(x) = $expr3")
    val c = 1.0
    println("g($c) = ${evaluate(expr3, c)}")
    println()

    val unexpanded = parse("t", "(t - 4)^3")
    println("$unexpanded = ${make_polynomial_if_possible(unexpanded)}")
} */