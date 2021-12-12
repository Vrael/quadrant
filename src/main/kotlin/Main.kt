import java.math.BigDecimal

data class Matrix(
    val columnHeaders: List<String>,
    val rowHeaders: List<String>,
    val rows: ArrayList<ArrayList<BigDecimal>>
) : Cloneable {
    public override fun clone(): Any {
        val clonedRows = rows.map { it.clone() as java.util.ArrayList<BigDecimal> }.toCollection(ArrayList())
        return Matrix(columnHeaders, rowHeaders, clonedRows)
    }
}

fun main(args: Array<String>) {
    println("Hungarian algorithm, aka Munkres assignment algorithm")

    val matrix = Matrix(
        listOf("Delhi", "Kerala", "Mumbai"),
        listOf("Jaipur", "Pune", "Bangalore"),
        // Matrix with optimal result
        /*arrayListOf(
            arrayListOf(BigDecimal(2500),BigDecimal(4000),BigDecimal(3500)),
            arrayListOf(BigDecimal(4000),BigDecimal(6000),BigDecimal(3500)),
            arrayListOf(BigDecimal(2000),BigDecimal(4000),BigDecimal(2500))
        )*/
        // Matrix without optimal result at first try
        arrayListOf(
            arrayListOf(BigDecimal(1500), BigDecimal(4000), BigDecimal(4500)),
            arrayListOf(BigDecimal(2000), BigDecimal(6000), BigDecimal(3500)),
            arrayListOf(BigDecimal(2000), BigDecimal(4000), BigDecimal(2500))
        )
    )
    printMatrix(matrix)
    println()

    val clonedMatrix = matrix.clone() as Matrix
    println("Step 1: Subtract minimum of every row.")
    val step1 = subtractMinForEveryRow(clonedMatrix)
    printMatrix(step1)
    println()

    println("Step 2: Subtract minimum of every column.")
    var step2 = subtractMinForEveryColumn(step1)
    printMatrix(step2)
    println()

    do {
        println("Step 3: Cover all zeroes with minimum number of horizontal and vertical lines")
        val analysis = coverWithLines(step2)
        printMatrix(step2, analysis.lines.rows, analysis.lines.cols)
        println()

        if (analysis.lines.cols.union(analysis.lines.rows).size != matrix.rows.size) {
            println("Step 4: Optimal assignment not found!")
            println("Step 5: Subtract the smallest uncovered entry from all uncovered rows")
            val uncoveredRows = step2.rows.indices - analysis.lines.rows
            val min = uncoveredRows.map { findMinInRow(step2, it, analysis.lines.cols) }
                .reduce { acc, current -> if (current < acc) current else acc }
            println("Min value: $min")
            uncoveredRows.forEach { row -> subtractForRow(step2, row, min) }
            printMatrix(step2)
            println()

            analysis.lines.cols.forEach { col -> subtractForColumn(step2, col, -min) }
            printMatrix(step2)
            println()
        }
    } while (analysis.lines.cols.union(analysis.lines.rows).size < matrix.rows.size)

    println("Found the optimal assignment!")

    val zeros = zeroPositions(step2)
    println("Candidates: $zeros")
    val positions = optimalPositions(zeros, matrix.rows.size)
    println("Optimal positions: $positions")
    val values = positions.map {
        val m = matrix.rows
        m[it.first][it.second]
    }
    println("Values: $values")
    println("Total: ${values.reduce { acc, number -> acc + number }}")
}

fun ArrayList<Pair<Int, Int>>.unique() = filter { pos -> count { it.second == pos.second } == 1 }

fun optimalPositions(zerosPositions: ArrayList<Pair<Int, Int>>, matrixSize: Int): List<Pair<Int, Int>> {
    var nonUniques = zerosPositions
    val result = mutableSetOf<Pair<Int,Int>>()
    do {
        result.addAll(nonUniques.unique())
        println(result)
        nonUniques = (nonUniques - result).filter { it.first !in result.map { it.first } }.toCollection(ArrayList())
        println(nonUniques)
    } while (result.size < matrixSize || nonUniques.isNotEmpty())
    return result.toList()
}

enum class ZeroObservationPlace { ROW, COLUMN }
data class ZeroObservation(
    val place: ZeroObservationPlace, val index: Int = -1, var count: Int = -1,
    var positions: ArrayList<Pair<Int, Int>> = arrayListOf()
) {
    override fun toString() = "ZO($place, i=$index, c=$count)"
}

data class Lines(val rows: Set<Int>, val cols: Set<Int>)
data class ZeroAnalysis(val obs: ArrayList<ZeroObservation>, val lines: Lines)

fun zeroPositions(matrix: Matrix): ArrayList<Pair<Int, Int>> {
    val m = matrix.rows
    val zero = BigDecimal(0)
    val result = arrayListOf<Pair<Int, Int>>()
    for (i in m.indices) {
        for (j in m[i].indices) {
            if (m[i][j] == zero) {
                result.add(Pair(i, j))
            }
        }
    }
    return result
}

fun coverWithLines(matrix: Matrix): ZeroAnalysis {
    val m = matrix.rows
    val obs = arrayListOf<ZeroObservation>()
    val markedRows = mutableSetOf<Int>()
    val markedColumns = mutableSetOf<Int>()

    m.forEachIndexed { i, _ ->
        obs.add(ZeroObservation(ZeroObservationPlace.ROW, i, countZerosInRow(matrix, i)))
        obs.add(ZeroObservation(ZeroObservationPlace.COLUMN, i, countZerosInColumn(matrix, i)))
    }

    val obsMutable = obs.map { it.copy() }.toCollection(ArrayList())

    do {
        obsMutable.sortWith(compareByDescending(ZeroObservation::count).thenBy(ZeroObservation::place))
        // println(obs)
        val first = obsMutable.first()
        when (first.place) {
            ZeroObservationPlace.ROW -> {
                markedRows.add(first.index)
                obsMutable.filter { it.place == ZeroObservationPlace.COLUMN }.forEach { it.count-- }
            }
            ZeroObservationPlace.COLUMN -> {
                markedColumns.add(first.index)
                obsMutable.filter { it.place == ZeroObservationPlace.ROW }.forEach { it.count-- }
            }
        }
        first.count = 0
        /*println(markedRows)
        println(markedColumns)
        println(obs)
        println()*/
    } while (obsMutable.any { it.count > 0 })
    return ZeroAnalysis(obs, Lines(markedRows, markedColumns))
}

fun countZerosInRow(matrix: Matrix, row: Int): Int {
    val m = matrix.rows
    val zero = BigDecimal(0)
    return m[row].count { it == zero }
}

fun countZerosInColumn(matrix: Matrix, col: Int): Int {
    val m = matrix.rows
    val zero = BigDecimal(0)
    var result = 0
    for (i in m.indices) {
        if (m[i][col] == zero) {
            result++
        }
    }
    return result
}

fun isZerosInRow(matrix: Matrix, row: Int): Boolean {
    val m = matrix.rows[row]
    val target = BigDecimal(0)
    for (num in m) {
        if (num == target) {
            return true
        }
    }
    return false
}

fun findMinInRow(matrix: Matrix, row: Int, skipCol: Set<Int> = setOf()): BigDecimal {
    val m = matrix.rows
    var min = m[row][((0 until m.size - 1) - skipCol).first()]
    var current: BigDecimal
    for (j in m.indices) {
        current = m[row][j]
        if (j !in skipCol && current < min) {
            min = current
        }
    }
    return min
}

fun findMinInCol(matrix: Matrix, col: Int): BigDecimal {
    val m = matrix.rows
    var min = m[0][col]
    var current: BigDecimal
    for (i in m.indices) {
        current = m[i][col]
        if (current < min) {
            min = current
        }
    }
    return min
}

fun subtractForRow(matrix: Matrix, row: Int, num: BigDecimal): Matrix {
    val m = matrix.rows
    for (c in 0 until matrix.rows.size) {
        m[row][c] -= num
    }
    return matrix
}

fun subtractMinForEveryRow(matrix: Matrix): Matrix {
    var min: BigDecimal
    for (i in matrix.rows.indices) {
        min = findMinInRow(matrix, i)
        subtractForRow(matrix, i, min)
    }
    return matrix
}

fun subtractForColumn(matrix: Matrix, col: Int, num: BigDecimal): Matrix {
    val m = matrix.rows
    for (i in m.indices) {
        m[i][col] -= num
    }
    return matrix
}

fun subtractMinForEveryColumn(matrix: Matrix): Matrix {
    var min: BigDecimal
    for (c in 0 until matrix.rows.size) {
        min = findMinInCol(matrix, c)
        subtractForColumn(matrix, c, min)
    }
    return matrix
}

fun maxItemLengthIncludingHeaders(matrix: Matrix): Int? = matrix.rowHeaders
    .plus(matrix.columnHeaders)
    .plus(matrix.rows.flatten().map(BigDecimal::toString))
    .map(String::length)
    .maxOrNull()

fun printMatrix(matrix: Matrix, markedRows: Set<Int>? = setOf(), markedColumns: Set<Int>? = setOf()) {
    val biggestLengthTemplate = "%${maxItemLengthIncludingHeaders(matrix)}s"
    if (matrix.rowHeaders.isNotEmpty()) {
        print(biggestLengthTemplate.format(""))
    }
    println(matrix.columnHeaders.joinToString(" ", transform = { biggestLengthTemplate.format(it) }))
    matrix.rows.forEachIndexed { rowIndex, row ->
        val rowHeader = matrix.rowHeaders.getOrNull(rowIndex)
        if (rowHeader != null) {
            print(biggestLengthTemplate.format(rowHeader))
        }
        row.forEachIndexed { columnIndex, number ->
            print(biggestLengthTemplate.format(number))
            markedColumns?.let {
                if (it.contains(columnIndex)) {
                    print("|")
                }
            }
        }
        markedRows?.let {
            if (it.contains(rowIndex)) {
                print("-")
            }
        }
        println()
    }
}
