import Arrow_Type.*
import kotlin.math.*
import kotlinx.browser.document
import org.w3c.dom.*
import org.w3c.dom.events.*

/**
 * classification for drawing an arrow; allows distinction between asymptotes and graph continuation
 */
private enum class Arrow_Type {
    /** for a general curve */
    FULL,
    /** for a curve to the left of an asymptote */
    LEFT_HALF,
    /** for a curve to the right of an asymptote */
    RIGHT_HALF
}

/**
 * generates a path for an arrow, given two points (to indicate direction) and a length (to indicate
 * size) the arrow begins from (x0,y0), and [arrow_direction] computes the points
 */
private fun arrow_points(
        x0: Double,
        y0: Double,
        x1: Double,
        y1: Double,
        length: Double,
        type: Arrow_Type = FULL
): Path2D {
    val theta = atan2(y1 - y0, x1 - x0)
    return arrow_direction(x0, y0, theta, length, type)
}

/**
 * generates a path for an arrow, given one point, an angle (for direction), and a length (for size)
 * the type is used for distinguishing asymptotes
 * @see Arrow_Type
 */
private fun arrow_direction(
        a: Double,
        b: Double,
        theta: Double,
        length: Double,
        type: Arrow_Type = FULL
): Path2D {
    require(length > 0)
    val result = Path2D()
    result.moveTo(a, b)
    when (type) {
        FULL -> {
            result.lineTo(a + length / 2 * sin(theta), b - length / 2 * cos(theta))
            result.lineTo(a + length * cos(theta), b + length * sin(theta))
            result.lineTo(a - length / 2 * sin(theta), b + length / 2 * cos(theta))
        }
        LEFT_HALF -> {
            val c = if (theta < 0.0) -1.0 else 1.0
            result.lineTo(a - length / 2 * sin(theta) * c, b + length / 2 * cos(theta))
            result.lineTo(a + length * cos(theta), b + length * sin(theta))
        }
        RIGHT_HALF -> {
            val c = if (theta < 0.0) 1.0 else -1.0
            result.lineTo(a + length * cos(theta), b + length * sin(theta))
            result.lineTo(a - length / 2 * sin(theta) * c, b + length / 2 * cos(theta))
        }
    }
    result.closePath()
    return result
}

/**
 * A generic object to be graphed. Current descendants are [Plot_Points] and [Text_Object].
 * @property color the color the object should have
 */
abstract class Graph_Object(open val color: String = "#0000ff") {
    /** draw to [Graph_Properties.canvas] */
    abstract fun draw_to(properties: Graph_Properties)

    /**
     * transforms [point] from absolute to view-relative coordinates
     * @param point the point to transform
     * @param x_scale from [Graph_Properties]
     * @param y_scale from [Graph_Properties]
     * @param x_translate from [Graph_Properties]
     * @param y_translate from [Graph_Properties]
     * @param height height of [Graph_Properties.canvas]
     */
    internal fun translate_point(
            point: Pair<Double, Double>,
            x_scale: Double,
            y_scale: Double,
            x_translate: Double,
            y_translate: Double,
            height: Int
    ) = Pair(point.first * x_scale + x_translate, height - (point.second * y_scale) - y_translate)
}

/**
 * contains the properties of a dynamic graph, which can include multiple plots; the actual drawing
 * is unloaded to [Plot_Points.draw_to]
 *
 * "Dynamic graph" means that one can drag or zoom on the graph and the plot(s) will react
 * accordingly.
 *
 * @see Plot_Points
 * @property canvas where to draw the plots
 * @property x_max largest x-value in the view; dynamic plots will use this to redraw
 * @property x_min smallest x-value in the view; dynamic plots will use this to redraw
 * @property y_max largest y-value in the view; dynamic plots will use this to redraw
 * @property y_min smallest y-value in the view; dynamic plots will use this to redraw
 * @property arrow_size how large to draw each plot's arrows, as well as the axes' arrows
 * @property with_gridlines whether to draw grid lines in the background
 * @property grid_dx horizontal distance between grid lines
 * @property grid_dy vertical distance between grid lines
 * @property grid_color the grid lines' color
 */
data class Graph_Properties(
        val canvas: HTMLCanvasElement,
        var x_min: Double = -6.0,
        var x_max: Double = 6.0,
        var y_min: Double = -4.0,
        var y_max: Double = 4.0,
        var arrow_size: Double = canvas.width / 30.0,
        val with_gridlines: Boolean = true,
        val grid_dx: Double = 1.0,
        val grid_dy: Double = 1.0,
        val grid_color: String = "#cccccc"
) {

    /**
     * the following properties convert from absolute x-, y-values to screen positions and are used
     * to help drag the graph
     */
    internal var x_scale = 0.0
    internal var y_scale = 0.0
    internal var x_translate = 0.0
    internal var y_translate = 0.0

    /** whether the graph's view was recently modified by drag or zoom */
    internal var recently_modified = true
    /** records the position where a drag started */
    internal var start_drag = Pair(0.0, 0.0)
    /** records the position where a pinch (second finger) started */
    internal var start_pinch = Pair(0.0, 0.0)

    /** the objects to draw */
    private val objects = ArrayList<Graph_Object>()

    /** determines [x_translate] and [y_translate] for [Δx] and [Δy] for a mouse drag */
    fun translate(Δx: Double = 0.0, Δy: Double = 0.0) {
        x_translate = -(x_min + Δx) * x_scale
        y_translate = -(y_min + Δy) * y_scale
        recently_modified = true
    }

    /**
     * determines [x_scale], [y_scale] for a zoom; also calls [translate] and sets
     * [recently_modified]
     */
    fun rescale() {
        val width = canvas.width
        val height = canvas.height
        x_scale = width / (x_max - x_min)
        y_scale = height / (y_max - y_min)
        translate(0.0, 0.0)
        recently_modified = true
    }

    /** currently this only rescales */
    init {
        rescale()
    }

    /** adds [plot] to the objects being plotted */
    fun add_object(plot: Graph_Object) {
        objects.add(plot)
    }

    /**
     * removes all the objects being plotted
     *
     * Effectively clears [objects]
     */
    fun remove_all_objects() {
        objects.clear()
    }

    /** removes the [i]th object being plotted */
    @Suppress("unused")
    fun remove_object(i: Int) {
        require(i >= 0 && i < objects.size)
        objects.removeAt(i)
    }

    /**
     * number of objects being plotted
     *
     * Effectively, this is the size of [objects]
     */
    @Suppress("unused") fun number_of_objects() = objects.size

    /**
     * draws the background grid
     *
     * @param context a drawing context
     * @param width the canvas' width; effectively, how many pixels wide a horizontal grid line
     * should be
     * @param height the canvas' height; effectively, how many pixels high a vertical grid line
     * should be
     */
    private fun draw_grid(context: CanvasRenderingContext2D, width: Double, height: Double) {

        require((width > 0) and (height > 0))

        context.beginPath()

        // draw horizontal lines first

        context.strokeStyle = grid_color
        var y = grid_dy
        while (y <= y_max + abs(y_translate)) {
            context.moveTo(0.0, height - y * y_scale - y_translate)
            context.lineTo(width, height - y * y_scale - y_translate)
            y += grid_dy
        }
        y = -grid_dy
        while (y >= y_min - abs(y_translate)) {
            context.moveTo(0.0, height - y * y_scale - y_translate)
            context.lineTo(width, height - y * y_scale - y_translate)
            y -= grid_dy
        }

        // now draw vertical grid lines

        var x = grid_dx
        while (x <= x_max + abs(x_translate)) {
            context.moveTo(x * x_scale + x_translate, 0.0)
            context.lineTo(x * x_scale + x_translate, height)
            x += grid_dx
        }
        x = -grid_dx
        while (x >= x_min - abs(x_translate)) {
            context.moveTo(x * x_scale + x_translate, 0.0)
            context.lineTo(x * x_scale + x_translate, height)
            x -= grid_dx
        }

        context.stroke()
    }

    /**
     * draw the axes, including arrows at the end
     *
     * @param context a drawing context
     * @param width the canvas' width; effectively, how many pixels wide a horizontal grid line
     * should be
     * @param height the canvas' height; effectively, how many pixels high a vertical grid line
     * should be
     */
    private fun draw_axes(context: CanvasRenderingContext2D, width: Double, height: Double) {

        require((width > 0) and (height > 0))

        // don't draw the axes if the graph has been translated to a point where they shouldn't be
        // visible

        if (x_translate < width) {
            context.beginPath()
            context.strokeStyle = "#000000"
            context.fillStyle = "#000000"
            context.lineWidth = 1.0
            context.moveTo(x_translate, 0.0)
            context.lineTo(x_translate, height)
            context.stroke()
            context.fill(arrow_direction(x_translate, arrow_size, 3 * PI / 2.0, arrow_size))
            context.fill(arrow_direction(x_translate, height - arrow_size, PI / 2.0, arrow_size))
        }

        if (y_translate < height) {
            context.beginPath()
            context.strokeStyle = "#000000"
            context.lineWidth = 1.0
            context.moveTo(0.0, height - y_translate)
            context.lineTo(width, height - y_translate)
            context.stroke()
            context.fill(arrow_direction(arrow_size, height - y_translate, PI, arrow_size))
            context.fill(arrow_direction(width - arrow_size, height - y_translate, 0.0, arrow_size))
        }
    }

    /** draws all the objects onto the canvas */
    fun draw_all() {

        // clear the background first

        val context = canvas.getContext("2d") as CanvasRenderingContext2D
        context.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())

        val width = canvas.width.toDouble()
        val height = canvas.height.toDouble()

        if (with_gridlines) draw_grid(context, width, height)

        draw_axes(context, width, height)

        for (p in objects) {
            p.draw_to(this)
        }
        // clear this to prevent unnecessary redrawing of static plots
        recently_modified = false
    }
}

/**
 * visual properties of a possibly dynamic plot in a graph; see [Function_Properties] for algebraic
 * properties
 *
 * A plot is "dynamic" if it remakes its presentation whenever a graph's view changes by drag or
 * zoom
 *
 * @property f the function that defines this plot, along with some related data
 * @property points the points to plot
 * @property color the curve's color
 * @property thickness how thick to draw the curve
 * @property dashed whether to make the curve dashed
 * @property arrows whether to draw arrows on the left and the right: set corresponding component to
 * true
 * @property connect_the_dots whether to connect the dots
 * @property holes known removable discontinuities (holes) in the plot; we draw holes and the curve
 * skips over them
 * @property asymptotes known asymptotic discontinuities in the plot
 * @property jumps known jump discontinuities in the plot; corresponding [plugs] and [unplugs] would
 * be useful
 * @property plugs dots to draw in the plot (think of plugging the holes of removable
 * discontinuities, but this has other uses)
 * @property unplugs holes to draw in the plot; these are merely drawn and not considered when
 * connecting dots (e.g., jump discontinuity)
 * @property change_with_view whether the plot is dynamic TODO implement [connect_the_dots]
 */
data class Plot_Points(
        val f: Function_Properties,
        var points: Array<Pair<Double, Double>>,
        override val color: String = "#0000ff",
        var fill_color: String = "#0000ff40",
        val thickness: Double = 2.0,
        val dashed: Boolean = false,
        val arrows: Pair<Boolean, Boolean> = Pair(first = true, second = true),
        val connect_the_dots: Boolean = true,
        val holes: Array<Pair<Double, Double>> = emptyArray(),
        val asymptotes: Array<Double> = emptyArray(),
        val jumps: Array<Double> = emptyArray(),
        val plugs: Array<Pair<Double, Double>> = emptyArray(),
        val unplugs: Array<Pair<Double, Double>> = emptyArray(),
        val change_with_view: Boolean = true
) : Graph_Object(color = color) {

    /** used for [discontinuities] */
    private enum class Discontinuity_Type {
        HOLE,
        ASYMPTOTE,
        JUMP
    }

    /** convenience constants */
    private val disc_hole = Discontinuity_Type.HOLE
    private val disc_asymp = Discontinuity_Type.ASYMPTOTE
    private val disc_jump = Discontinuity_Type.JUMP

    /** which points to plot */
    private var plot_points = ArrayList<Pair<Double, Double>>()
    /** used to check if we need to re-compute [plot_points] */
    private var last_translated_to: Graph_Properties? = null

    /** these objects are used to track discontinuities in the graph */
    private var discontinuities =
            Array(holes.size + asymptotes.size + jumps.size) { Pair(disc_hole, 0.0) }
    private var plot_holes = ArrayList<Pair<Double, Double>>()
    private var plot_plugs = ArrayList<Pair<Double, Double>>()
    private var plot_unplugs = ArrayList<Pair<Double, Double>>()

    /**
     * transforms internal data from absolute to view-relative coordinates
     *
     * Properties affected: [last_translated_to], [plot_holes], [plot_plugs], [plot_unplugs],
     * [plot_points], [discontinuities]. The latter is sorted in increasing order.
     *
     * If dynamic, also affects [points].
     */
    private fun rescale_and_translate(properties: Graph_Properties) {

        last_translated_to = properties

        // if dynamic

        if (change_with_view) {
            val delta = (properties.x_min - properties.x_max) * .05
            val start = properties.x_min - delta
            val stop = properties.x_max + delta
            points = plot(start, stop, f.num_points, f.f)
        }

        // use properties to adjust points, holes, plugs, and unplugs
        // text label, if any, is adjusted when drawn

        val height = properties.canvas.height
        val x_scale = properties.x_scale
        val y_scale = properties.y_scale
        val x_translate = properties.x_translate
        val y_translate = properties.y_translate

        plot_holes.clear()
        plot_plugs.clear()
        plot_points.clear()
        plot_unplugs.clear()

        points.mapTo(plot_points) {
            translate_point(it, x_scale, y_scale, x_translate, y_translate, height)
        }
        holes.mapTo(plot_holes) {
            translate_point(it, x_scale, y_scale, x_translate, y_translate, height)
        }
        plugs.mapTo(plot_plugs) {
            translate_point(it, x_scale, y_scale, x_translate, y_translate, height)
        }
        unplugs.mapTo(plot_unplugs) {
            translate_point(it, x_scale, y_scale, x_translate, y_translate, height)
        }

        // update discontinuities

        var i = 0
        for (hole in holes) {
            discontinuities[i] = Pair(disc_hole, hole.first * x_scale + x_translate)
            i += 1
        }
        for (asymp in asymptotes) {
            discontinuities[i] = Pair(disc_asymp, asymp * x_scale + x_translate)
            i += 1
        }
        for (jump in jumps) {
            discontinuities[i] = Pair(disc_jump, jump * x_scale + x_translate)
            i += 1
        }
        discontinuities.sortBy { it.second }
    }

    /** draws the area between the function and the x-axis */
    private fun draw_integral(properties: Graph_Properties) {
        val canvas = properties.canvas
        val context = canvas.getContext("2d") as CanvasRenderingContext2D
        val old_color = context.fillStyle
        context.fillStyle = fill_color
        context.beginPath()
        val path = Path2D()
        val p =
                translate_point(
                        Pair(points[0].first, 0.0),
                        properties.x_scale,
                        properties.y_scale,
                        properties.x_translate,
                        properties.y_translate,
                        canvas.height
                )
        path.moveTo(p.first, p.second)
        for (point in plot_points) path.lineTo(point.first, point.second)
        val q =
                translate_point(
                        Pair(points.last().first, 0.0),
                        properties.x_scale,
                        properties.y_scale,
                        properties.x_translate,
                        properties.y_translate,
                        canvas.height
                )
        path.lineTo(q.first, q.second)
        context.fill(path)
        context.fillStyle = old_color
    }

    /**
     * draws all related data to [properties]
     *
     * TODO: detect when graph moves off screen and add arrows?
     */
    override fun draw_to(properties: Graph_Properties) {

        // if the graph properties have changed, we need to recompute the relative coordinates

        if (last_translated_to != properties || properties.recently_modified) {
            rescale_and_translate(properties)
        }

        // basic setup

        val arrow_size = properties.arrow_size
        val canvas = properties.canvas
        val context = canvas.getContext("2d") as CanvasRenderingContext2D
        val old_thickness = context.lineWidth
        context.strokeStyle = color
        context.fillStyle = color
        context.lineWidth = thickness

        // typically we want the area drawn before the curve
        if (f.show_integral) draw_integral(properties)

        // sometimes we need to dash the curve (e.g., asymptotes) but arrows should not be dashed

        val old_line_dash = context.getLineDash()
        val dash_length = canvas.height / 20.0
        val this_line_dash = if (dashed) arrayOf(dash_length, dash_length) else old_line_dash
        context.setLineDash(this_line_dash)
        context.beginPath()

        // prepare for the loop

        var i = 1 // i indexes points
        var j = 0 // j indexes discontinuities
        val x_delta = 5.0 // used to skip over discontinuities

        // crossed_asymptote is used to prevent the drawing of asymptote arrows
        // when we start drawing after the first asymptote

        var crossed_asymptote = false

        // move to the first point, then loop through the list of points,
        // connecting the dots unless we cross a discontinuity

        context.moveTo(plot_points[0].first, plot_points[0].second)

        while (i + 1 < plot_points.size) {

            var (x, y) = plot_points[i]
            var (x_next, y_next) = plot_points[i + 1]

            // take into account each discontinuity

            if (j < discontinuities.size) {

                // if we cross an asymptote, draw corresponding arrows,
                // and skip over the asymptote

                if (discontinuities[j].first == disc_asymp &&
                                (x_next + x_delta > discontinuities[j].second) &&
                                crossed_asymptote
                ) {

                    context.stroke()
                    context.beginPath()
                    context.setLineDash(old_line_dash)

                    // determine left arrow's direction, size, path

                    var dx = x - plot_points[i - 1].first
                    var dy = y - plot_points[i - 1].second
                    var scale = sqrt(dx * dx + dy * dy) / (canvas.width / 100)
                    dx /= scale
                    dy /= scale
                    var arrow_path =
                            arrow_points(x, y, x + dx * 2.0, y + dy * 2.0, arrow_size, LEFT_HALF)

                    // draw it

                    context.stroke(arrow_path)
                    context.fill(arrow_path)
                    context.beginPath()

                    // skip over points until we pass the asymptote and put some space between us
                    // and the asymptote

                    while (i + 1 < plot_points.size &&
                            plot_points[i].first - x_delta < discontinuities[j].second) i += 1

                    // draw the right arrow only if we haven't moved beyond our point data

                    if (i + 1 < plot_points.size) {

                        // determine right arrow's location, direction, size, path

                        x = plot_points[i].first
                        y = plot_points[i].second
                        x_next = plot_points[i + 1].first
                        y_next = plot_points[i + 1].second
                        context.moveTo(x, y)
                        dx = x_next - x
                        dy = y_next - y
                        scale = sqrt(dx * dx + dy * dy) / (canvas.width / 100)
                        dx /= scale
                        dy /= scale
                        arrow_path =
                                arrow_points(
                                        x,
                                        y,
                                        x - dx * 2.0,
                                        y - dy * 2.0,
                                        arrow_size,
                                        RIGHT_HALF
                                )

                        // draw it

                        context.stroke(arrow_path)
                        context.fill(arrow_path)
                        context.stroke()
                        context.beginPath()
                        context.moveTo(x, y)
                        j += 1
                    }
                }

                // if we cross a hole, draw it and skip over it

                else if (discontinuities[j].first == disc_hole &&
                                x_next + 2 * x_delta > discontinuities[j].second
                ) {

                    // determine the hole's location

                    val x_pos = discontinuities[j].second
                    val y_pos = plot_holes[plot_holes.indexOfFirst { it.first == x_pos }].second

                    // determine the direction to its circle

                    val slope = (y_pos - y) / (x_pos - x)
                    @Suppress("UnnecessaryVariable") // for clarity I want to call it "radius"
                    val radius = x_delta
                    val θ = atan(slope)
                    val xp = x_pos - radius * cos(θ)
                    val yp = y_pos - radius * sin(θ)
                    context.lineTo(xp, yp)
                    context.stroke()

                    // draw the circle

                    context.beginPath()
                    context.setLineDash(old_line_dash)
                    context.arc(x_pos, y_pos, radius, 0.0, 2 * PI)
                    context.stroke()

                    // resume drawing the curve

                    context.beginPath()
                    context.setLineDash(this_line_dash)
                    while (i < plot_points.size && plot_points[i].first < x_pos + radius) i += 1
                    val move_x = x_pos + radius * cos(θ)
                    val move_y = y_pos + radius * sin(θ)
                    context.moveTo(move_x, move_y)
                    j += 1
                }

                // if we cross a jump, skip over it

                else if (discontinuities[j].first == disc_jump &&
                                x_next + x_delta > discontinuities[j].second
                ) {

                    context.lineTo(discontinuities[j].second - x_delta, y)
                    context.stroke()

                    context.beginPath()
                    while (i < plot_points.size &&
                            plot_points[i].first < discontinuities[j].second) i += 1
                    if (i < plot_points.size) {
                        context.moveTo(plot_points[i].first, plot_points[i].second)
                        i += 1
                    }
                    j += 1
                }

                // no discontinuity: so, connect dots or draw a point, unless
                // we're crossing an asymptote -- this might not have been discovered
                // earlier if the curve begins at an asymptote

                else {

                    if (discontinuities[j].first == disc_asymp &&
                                    x <= discontinuities[j].second &&
                                    x_next >= discontinuities[j].second
                    ) {
                        crossed_asymptote = true
                    } else {
                        if (connect_the_dots) context.lineTo(x_next, y_next)
                        else {
                            context.fillStyle = color
                            context.arc(x_next, y_next, x_delta, 0.0, 2 * PI)
                            context.fill()
                            context.stroke()
                            context.beginPath()
                        }
                    }
                }
            }

            // no discontinuity; connect the dots or plot a point and move on

            else {

                if (connect_the_dots) context.lineTo(x_next, y_next)
                else {
                    context.fillStyle = color
                    context.arc(x_next, y_next, x_delta, 0.0, 2 * PI)
                    context.fill()
                    context.stroke()
                    context.beginPath()
                }
            }

            i += 1
        }

        // finish drawing; restore older dash setting

        context.stroke()
        context.setLineDash(old_line_dash)

        // draw arrow on left of graph, if desired

        if (arrows.first) {
            context.beginPath()
            val (x0, y0) = plot_points[0]
            if ((!plot_plugs.any { abs(it.first - x0) < x_delta }) &&
                            (!plot_unplugs.any { abs(it.first - x0) < x_delta })
            ) {
                val dx = x0 - plot_points[1].first
                val dy = y0 - plot_points[1].second
                val arrow_path = arrow_points(x0, y0, x0 + dx * 2.0, y0 + dy * 2.0, arrow_size)
                context.stroke(arrow_path)
                context.fill(arrow_path)
            }
        }

        // draw arrow on right of graph, if desired

        if (arrows.second) {
            context.beginPath()
            val last = plot_points.size - 1
            val (xn, yn) = plot_points[last]
            if ((!plot_plugs.any { abs(it.first - xn) < x_delta }) &&
                            (!plot_unplugs.any { abs(it.first - xn) < x_delta })
            ) {
                val dx = plot_points[last].first - plot_points[last - 1].first
                val dy = plot_points[last].second - plot_points[last - 1].second
                val arrow_path = arrow_points(xn, yn, xn + dx * 2.0, yn + dy * 2.0, arrow_size)
                context.stroke(arrow_path)
                context.fill(arrow_path)
            }
        }

        // draw asymptotes

        context.beginPath()
        context.lineWidth = thickness
        context.setLineDash(arrayOf(dash_length, dash_length))
        for ((type, x) in discontinuities) {
            if (type == disc_asymp) {
                context.moveTo(x, canvas.height.toDouble())
                context.lineTo(x, 0.0)
            }
        }
        context.stroke()
        context.strokeStyle = color
        context.setLineDash(old_line_dash)

        // plot the plugs (points to highlight or that fill in discontinuities)

        for (point in plot_plugs) {
            context.beginPath()
            context.arc(point.first, point.second, x_delta, 0.0, 2 * PI)
            context.fillStyle = color
            context.lineWidth = 2.0
            context.fill()
            context.stroke()
        }

        // plot the unplugs (should be holes related to jump discontinuities)

        for (point in plot_unplugs) {
            context.beginPath()
            context.arc(point.first, point.second, x_delta, 0.0, 2 * PI)
            context.stroke()
        }

        context.lineWidth = old_thickness
    }

    /** tests equality by testing all components */
    override fun equals(other: Any?): Boolean {

        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as Plot_Points

        if (!points.contentEquals(other.points)) return false
        if ((color != other.color) ||
                        (thickness != other.thickness) ||
                        (dashed != other.dashed) ||
                        (connect_the_dots != other.connect_the_dots) ||
                        (disc_hole != other.disc_hole) ||
                        (disc_asymp != other.disc_asymp) ||
                        (disc_jump != other.disc_jump) ||
                        (plot_points != other.plot_points) ||
                        (last_translated_to != other.last_translated_to) ||
                        (plot_holes != other.plot_holes) ||
                        (plot_plugs != other.plot_plugs) ||
                        (plot_unplugs != other.plot_unplugs) ||
                        (!holes.contentEquals(other.holes)) ||
                        (!asymptotes.contentEquals(other.asymptotes)) ||
                        (!jumps.contentEquals(other.jumps)) ||
                        (!plugs.contentEquals(other.plugs)) ||
                        (!unplugs.contentEquals(other.unplugs)) ||
                        (!discontinuities.contentEquals(other.discontinuities))
        )
                return false

        return true
    }

    /** computes a hashcode using all components */
    override fun hashCode(): Int {

        var result = points.contentHashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + thickness.hashCode()
        result = 31 * result + dashed.hashCode()
        result = 31 * result + connect_the_dots.hashCode()
        result = 31 * result + holes.contentHashCode()
        result = 31 * result + asymptotes.contentHashCode()
        result = 31 * result + jumps.contentHashCode()
        result = 31 * result + plugs.contentHashCode()
        result = 31 * result + unplugs.contentHashCode()
        result = 31 * result + disc_hole.hashCode()
        result = 31 * result + disc_asymp.hashCode()
        result = 31 * result + disc_jump.hashCode()
        result = 31 * result + plot_points.hashCode()
        result = 31 * result + (last_translated_to?.hashCode() ?: 0)
        result = 31 * result + discontinuities.contentHashCode()
        result = 31 * result + plot_holes.hashCode()
        result = 31 * result + plot_plugs.hashCode()
        result = 31 * result + plot_unplugs.hashCode()
        return result
    }
}

/**
 * computes the (absolute) points of a function described by [expr], starting at [a], ending at [b],
 * with [n] points
 */
fun plot(a: Double, b: Double, n: Int, expr: Evaluation_Tree_Node): Array<Pair<Double, Double>> {

    require((a < b) and (n > 3))
    val Δx = (b - a) / n
    return Array(((b - a) / Δx).toInt()) { Pair(a + it * Δx, evaluate(expr, a + it * Δx)) }
}

/** relates each canvas to a Graph_Properties the [String] should be an id assigned to the canvas */
var my_properties: MutableMap<String, Graph_Properties> = mutableMapOf()

/** find which graph triggered event [e] */
fun find_events_gp(e: Event): Graph_Properties =
        my_properties[(e.target as HTMLCanvasElement).getAttribute("id")]!!

/** whether the user has clicked the mouse button down on a graph */
var mouse_button_is_down: Boolean = false

/** whether the user is pinching a graph */
var pinching: Boolean = false

/** which fingers are pinching on a graph */
var first_finger_id: Int = 0
var second_finger_id: Int = 0

/**
 * how far the user has dragged a canvas
 * @return the drag's Δx, Δy
 */
fun determine_Δx(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        gp: Graph_Properties
): Pair<Double, Double> {
    var Δx = x2 - x1
    var Δy = y2 - y1
    Δx *= (gp.x_max - gp.x_min) / gp.canvas.width
    Δy *= (gp.y_max - gp.y_min) / gp.canvas.height
    return Pair(Δx, Δy)
}

/** a drag has ended; process [e] */
fun end_drag(e: Event) {

    // we should ONLY be here if the mouse button has been released

    mouse_button_is_down = false

    // who did this?

    val props = find_events_gp(e)

    // find how far it was dragged

    val (Δx, Δy) =
            when (e) {
                is MouseEvent ->
                        determine_Δx(
                                e.x,
                                props.start_drag.second,
                                props.start_drag.first,
                                e.y,
                                props
                        )
                is TouchEvent ->
                        determine_Δx(
                                e.changedTouches[0]!!.clientX.toDouble(),
                                props.start_drag.second,
                                props.start_drag.first,
                                e.changedTouches[0]!!.clientY.toDouble(),
                                props
                        )
                else -> Pair(0.0, 0.0)
            }

    // adjust the properties, rescale, redraw

    props.x_min += Δx
    props.x_max += Δx
    props.y_min += Δy
    props.y_max += Δy
    props.rescale()
    props.draw_all()
}

/** a pinch has ended; process [e] */
fun end_pinch(e: Event) {
    e.preventDefault()
    first_finger_id = 0
    second_finger_id = 0
    pinching = false
}

/** the user has clicked on the graph; begin a drag or pinch reported by [e] */
fun record_click(e: Event) {

    // I don't remember why this is here, but I do remember that things don't work right without it
    e.preventDefault()

    // if we record a click when the mouse button is already down,
    // then this "must be" two fingers pressing on the canvas; consider it a pinch.
    // otherwise "must be" a drag

    if (mouse_button_is_down) {

        pinching = true
        mouse_button_is_down = false
        second_finger_id = (e as TouchEvent).changedTouches[0]!!.identifier
        val touch = e.changedTouches[0]!!
        find_events_gp(e).start_pinch = Pair(touch.clientX.toDouble(), touch.clientY.toDouble())
    } else {

        mouse_button_is_down = true
        if (e is MouseEvent) {
            find_events_gp(e).start_drag = Pair(e.x, e.y)
        } else if (e is TouchEvent) {
            if (e.changedTouches[0] != null) {
                val touch = e.changedTouches[0]!!
                find_events_gp(e).start_drag =
                        Pair(touch.clientX.toDouble(), touch.clientY.toDouble())
                first_finger_id = touch.identifier
            }
        }
    }
}

/** the user has released a click, end a drag or pinch reported by [e] */
fun record_click_release(e: Event) {
    // I don't remember why this is here, but I do remember that things don't work right without it
    e.preventDefault()
    if (mouse_button_is_down) end_drag(e) else if (pinching) end_pinch(e)
}

/** if the user is dragging, process [e]; otherwise, ignore */
fun report_movement(e: Event) {

    // I don't remember why this is here, but I do remember that things don't work right without it
    e.preventDefault()

    if (mouse_button_is_down || pinching) {

        // determine the graph source

        val local_props = find_events_gp(e)

        // if the mouse button is down, we consider this a drag event

        if (mouse_button_is_down) {

            // this is a drag; translate the view

            // where did it drag to?
            val (x, y) =
                    when (e) {
                        is MouseEvent -> Pair(e.x, e.y)
                        is TouchEvent -> {
                            val touch = e.changedTouches[0]!!
                            Pair(touch.clientX.toDouble(), touch.clientY.toDouble())
                        }
                        else -> {
                            Pair(0.0, 0.0)
                        }
                    }
            // where did it drag from?
            val start_drag = local_props.start_drag
            val (Δx, Δy) = determine_Δx(x, start_drag.second, start_drag.first, y, local_props)
            local_props.translate(Δx, Δy)
            local_props.draw_all()
        } else {

            // this is a pinch; rescale the view

            // identify previous finger positions and determine distance between them
            val p1 = local_props.start_drag
            val p2 = local_props.start_pinch
            val distance_original =
                    sqrt(
                            (p1.first - p2.first) * (p1.first - p2.first) +
                                    (p1.second - p2.second) * (p1.second - p2.second)
                    )

            // identify current finger positions and determine distance between them
            val touch1 = (e as TouchEvent).changedTouches[0]!!
            val touch2 = e.changedTouches[1]
            val q1 =
                    when {
                        touch1.identifier == first_finger_id ->
                                Pair(touch1.clientX.toDouble(), touch1.clientY.toDouble())
                        touch2 != null -> Pair(touch2.clientX.toDouble(), touch2.clientY.toDouble())
                        else -> p1
                    }
            val q2 =
                    when {
                        touch1.identifier == second_finger_id ->
                                Pair(touch1.clientX.toDouble(), touch1.clientY.toDouble())
                        touch2 != null -> Pair(touch2.clientX.toDouble(), touch2.clientY.toDouble())
                        else -> p2
                    }
            val distance_new =
                    sqrt(
                            (q1.first - q2.first) * (q1.first - q2.first) +
                                    (q1.second - q2.second) * (q1.second - q2.second)
                    )

            // only rescale if there's a serious difference
            if (abs(distance_new - distance_original) > 1) {
                dilate_view(
                        local_props,
                        (distance_new - distance_original) /
                                (local_props.canvas.width + local_props.canvas.height)
                )
                local_props.start_drag = q1
                local_props.start_pinch = q2
            }
        }
    }
}

/** if the user has released a button, end drag reported by [e] */
fun report_mouse_left(e: Event) {
    if (mouse_button_is_down) {
        mouse_button_is_down = false
        end_drag(e as MouseEvent)
    }
}

/** rescale the view of [graph_props] by [scale] */
fun dilate_view(graph_props: Graph_Properties, scale: Double) {
    val width = graph_props.x_max - graph_props.x_min
    val height = graph_props.y_max - graph_props.y_min
    graph_props.x_max -= scale * width
    graph_props.x_min += scale * width
    graph_props.y_max -= scale * height
    graph_props.y_min += scale * height
    graph_props.rescale()
    graph_props.draw_all()
}

/** process the zoom created by [e] */
fun zoom_in_or_out(e: Event) {
    e.preventDefault()
    val graph_props = find_events_gp(e)
    val we = e as WheelEvent
    val scale = if (we.deltaY > 0) -0.05 else 1.0 / 22.0
    dilate_view(graph_props, scale)
}

/* var control_down = false

fun register_key(e: Event) {
    val ke = e as KeyboardEvent
    if (ke.ctrlKey) control_down = !control_down
    console.log("control down: $control_down")
}

val key_listener = EventListener { e -> register_key(e) } */

/**
 * a [Graph_Object] that we use to label a function's graph; this could be used for other things
 * eventually
 * @param text the text to write
 * @param x where to write [text]
 * @param y where to write [text]
 * @param size size of the type
 * @param typeface which typeface ("font") to use
 * @param color the color to use when drawing
 * @param italics whether to italicize the text; the default is true because we ordinarily label
 * with a letter
 * @param bold whether to make the text bold
 */
data class Text_Object(
        val text: String,
        val x: Double,
        val y: Double,
        val size: Int = 18,
        val typeface: String = "Serif",
        override val color: String = "#0000ff",
        val italics: Boolean = true,
        val bold: Boolean = false
) : Graph_Object(color = color) {

    /** draw [text] to [properties] */
    override fun draw_to(properties: Graph_Properties) {

        val context = properties.canvas.getContext("2d") as CanvasRenderingContext2D

        // save current information that we modify

        val old_font = context.font
        val text_alignment = context.textAlign
        val style = context.fillStyle

        context.font =
                (if (italics) "italic " else "") +
                        (if (bold) " bold " else "") +
                        size.toString() +
                        "px " +
                        typeface
        context.textAlign = CanvasTextAlign.CENTER

        // translate the point from absolute location to view-relative

        val point =
                translate_point(
                        Pair(x, y),
                        properties.x_scale,
                        properties.y_scale,
                        properties.x_translate,
                        properties.y_translate,
                        properties.canvas.height
                )

        // mask background behind this somewhat, so as to make the text clearer

        // context.fillStyle = if (document.bgColor == "") "#ffffffb0" else document.bgColor + "b0"
        val metrics = context.measureText(text)
        val y =
                if (point.second -
                                (metrics.actualBoundingBoxAscent +
                                        metrics.actualBoundingBoxDescent * 2) >= 0.0
                )
                        point.second - metrics.actualBoundingBoxDescent * 2
                else
                        point.second +
                                metrics.actualBoundingBoxAscent +
                                metrics.actualBoundingBoxDescent
        /*context.fillRect(
            point.first - metrics.actualBoundingBoxLeft,
            y - metrics.actualBoundingBoxAscent,
            metrics.actualBoundingBoxLeft + metrics.actualBoundingBoxRight,
            size.toDouble()
        )*/

        // finally, write the text

        context.fillStyle = color
        context.fillText(text, point.first, y)

        // restore old settings

        context.fillStyle = style
        context.font = old_font
        context.textAlign = text_alignment
    }
}

/**
 * a [Graph_Object] that we use to label a function's graph in LaTeX format
 *
 * Note that this uses innerHTML to produce the text, rather than a Canvas Context. Hence, this
 * (currently) needs an enclosing object, such as a <div>, placed immediately before the Canvas (it
 * need not enclose the Canvas as well).
 *
 * TODO Consider rolling our own; using MathJax requires a bit of a kludge, and we have to trust the
 * client to remember to include MathJax, set up a <div> so that MathJax can find the text, etc.
 *
 * @param text the text to write; should already be in LaTeX markup
 * @param latex_enclosure an HTMLElement into which we position [text] at ([x], [y]);
 * ```
 *      this should be a <div> that either encloses the Canvas or begins and ends immediately before it
 * @param x
 * ```
 * where to write [text], relative to the Canvas
 * @param y where to write [text], relative to the Canvas
 * @param size size of the type
 * @param typeface which typeface ("font") to use; currently, MathJax 3 uses only its own typeface,
 * ```
 *      so this is unfortunately useless for the time being
 * @param color
 * ```
 * the color to use when drawing
 */
data class LaTeX_Object(
        val text: String,
        val latex_enclosure: String,
        val x: Double,
        val y: Double,
        val size: Int = 18,
        val typeface: String = "Serif",
        override val color: String = "#0000ff",
) : Graph_Object(color = color) {

    /** draw [text] into [latex_enclosure] */
    override fun draw_to(properties: Graph_Properties) {

        // translate the point from absolute location to view-relative

        var point =
                translate_point(
                        Pair(x, y),
                        properties.x_scale,
                        properties.y_scale,
                        properties.x_translate,
                        properties.y_translate,
                        properties.canvas.height
                )

        point =
                Pair(
                        if (point.first + 30 <= properties.canvas.width) point.first + 20
                        else point.first - 10,
                        if (point.second - 20 >= 10) point.second - 20 else point.second + 10
                )

        // write the text

        val target = document.getElementById(latex_enclosure)

        if (target != null) {
            target.innerHTML =
                    "<div><span style=\"position: absolute; left: ${point.first}px; " +
                            "top: ${point.second}px; transform:translateX(-50%); z-index: 5;" +
                            "font-size: $size; font: $typeface; color: $color; background-color: #ffffffaa;\">\\(" +
                            text +
                            "\\)</span></div>"

            js(
                    "function typeset_later() { MathJax.typesetPromise();} setTimeout(typeset_later, 3000);"
            )
        }
    }
}

/**
 * algebraic properties of a function being graphed, along with some permanent visual properties
 *
 * @property ind_var the independent variable; usually "x" but can be "t2" if you like
 * @property f a tree representation of the function
 * @property start x-value to start computing points
 * @property stop x-value to stop computing points
 * @property color which color the function should have
 * @property thickness how thick the plot should be
 * @property dashed whether the plot should be dashed
 * @property change_with_view whether the plot should be dynamic; see [Plot_Points] for an
 * explanation
 * @property num_points number of points to plot in the graph
 * @property holes list of x-, y-values of known removable discontinuities; see [Plot_Points] for
 * more detail
 * @property asymptotes list of x-values known asymptotes
 * @property jumps list of x-values of known jump discontinuities
 * @property plugs points to plot on the curve, such as plugging holes, or also to highlight a point
 * @property unplugs holes to place on the curve for jump discontinuities; see [Plot_Points] for
 * more detail
 * @property modifier name of an input that redefines [f]
 * @property plug_box name of an input that redefines the plugs associated with [f]
 * @property arrows whether to draw arrows on left or right of plot (indicating that the graph
 * continues)
 * @property label an optional text label for the graph, typically placed roughly 3/4 of the way
 * @property latex_enclosure if MathJax version 3 is enabled on the webpage, you can give the ID of
 * an HTMLElement
 * ```
 *      here, into which Agnesi will place a LaTeX'd label, and invoke MathJax.typesetPromise() to typeset it
 * @see Evaluation_Tree_Node
 * ```
 */
data class Function_Properties(
        var ind_var: String,
        var f: Evaluation_Tree_Node,
        var start: Double,
        var stop: Double,
        var color: String = "#0000ff",
        var thickness: Double = 1.0,
        var dashed: Boolean = false,
        val change_with_view: Boolean = false,
        var num_points: Int = 1000,
        var holes: Array<Pair<Double, Double>> = arrayOf(),
        var asymptotes: Array<Double> = arrayOf(),
        var jumps: Array<Double> = arrayOf(),
        var plugs: Array<Pair<Double, Double>> = arrayOf(),
        var unplugs: Array<Pair<Double, Double>> = arrayOf(),
        var show_integral: Boolean = false,
        var fill_color: String = color + "40",
        var modifier: String = "",
        var plug_box: String = "",
        var unplug_box: String = "",
        var arrows: Pair<Boolean, Boolean> = Pair(first = false, second = false),
        var label: String = "",
        var latex_enclosure: String = "",
) {

    fun to_LaTeX(): String = f.toLaTeXString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as Function_Properties

        if (ind_var != other.ind_var) return false
        if (f != other.f) return false
        if (start != other.start) return false
        if (stop != other.stop) return false
        if (color != other.color) return false
        if (thickness != other.thickness) return false
        if (num_points != other.num_points) return false
        if (change_with_view != other.change_with_view) return false
        if (dashed != other.dashed) return false
        if (!holes.contentEquals(other.holes)) return false
        if (!asymptotes.contentEquals(other.asymptotes)) return false
        if (!plugs.contentEquals(other.plugs)) return false
        if (!unplugs.contentEquals(other.plugs)) return false
        if (modifier != other.modifier) return false
        if (arrows != other.arrows) return false
        if (label != other.label) return false

        return true
    }

    @Suppress("DuplicatedCode")
    override fun hashCode(): Int {
        var result = ind_var.hashCode()
        result = 31 * result + f.hashCode()
        result = 31 * result + start.hashCode()
        result = 31 * result + stop.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + thickness.hashCode()
        result = 31 * result + change_with_view.hashCode()
        result = 31 * result + dashed.hashCode()
        result = 31 * result + num_points
        result = 31 * result + holes.contentHashCode()
        result = 31 * result + asymptotes.contentHashCode()
        result = 31 * result + plugs.contentHashCode()
        result = 31 * result + unplugs.contentHashCode()
        result = 31 * result + modifier.hashCode()
        result = 31 * result + arrows.hashCode()
        return result
    }
}

/** all the functions associated with the [HTMLCanvasElement] indicated by the [String] */
var fun_properties: MutableMap<String, ArrayList<Function_Properties>> = mutableMapOf()

/** find the element of [fp] that [e] reports to */
fun find_events_fp(fp: ArrayList<Function_Properties>, e: Event): Int =
        fp.indexOfFirst {
            (e.target as HTMLInputElement).getAttribute("id") in
                    arrayOf(it.modifier, it.plug_box, it.unplug_box)
        }

/**
 * client has redefined a function; identify and notify the function, then re-graph the canvas and
 * function must be made aware (via [which]'s data-input-box tag) that the input box triggering [e]
 * addresses them
 *
 * @param e event triggered by redefining the function in a text input
 * @param which the name of the canvas to which the function is drawn
 */
fun report_function_changed(e: Event, which: String) {

    // find the relevant Function_Properties, Graph_Properties and HTMLInputElement

    val i = find_events_fp(fun_properties[which]!!, e)
    val local_props = fun_properties[which]!![i]
    val source = (e.target as HTMLInputElement).value

    // parse the new input and graph

    local_props.f = parse(local_props.ind_var, source)
    apply(my_properties[which]!!, fun_properties[which]!!)
}

/**
 * client has redefined one or more (un-)plugs; identify and notify the function, then re-graph the
 * canvas and function must be made aware (via [which]'s data-input-box tag) that the input box
 * triggering [e] addresses them
 *
 * @param e event triggered by redefining the function in a text input
 * @param which the name of the canvas to which the function is drawn
 */
fun report_plugs_changed(e: Event, which: String, plugs_not_unplugs: Boolean = true) {

    // find the relevant Function_Properties, Graph_Properties and HTMLInputElement

    val f = fun_properties[which]!!
    val local_props = f[find_events_fp(f, e)]
    val source = (e.target as HTMLInputElement).value

    // parse the new points and graph
    if (plugs_not_unplugs) local_props.plugs = read_xy_values(source)
    else local_props.unplugs = read_xy_values(source)
    apply(my_properties[which]!!, f)
}

/** graph [fun_props] on [plot_canvas]; this removes all of [plot_canvas]'s previous objects */
fun apply(plot_canvas: Graph_Properties, fun_props: ArrayList<Function_Properties>) {

    require(fun_props.all { (it.start < it.stop) and (it.num_points > 2) })

    plot_canvas.remove_all_objects()

    for (f in fun_props) {

        // compute points, then add to plot_canvas
        val points = plot(f.start, f.stop, f.num_points, f.f)
        val plotted_points =
                Plot_Points(
                        f,
                        points,
                        color = f.color,
                        fill_color = f.fill_color,
                        thickness = f.thickness,
                        dashed = f.dashed,
                        holes = f.holes,
                        asymptotes = f.asymptotes,
                        plugs = f.plugs,
                        jumps = f.jumps,
                        unplugs = f.unplugs,
                        arrows = f.arrows,
                        change_with_view = f.change_with_view
                )
        plot_canvas.add_object(plotted_points)

        // if there's a label, add that, too

        if (f.label != "") {

            if (f.latex_enclosure != "") {

                val which_point = points[(points.size * 3) / 4] // points.last()

                plot_canvas.add_object(
                        LaTeX_Object(
                                f.to_LaTeX(),
                                f.latex_enclosure,
                                which_point.first,
                                which_point.second,
                                color = f.color
                        )
                )
            } else {

                val which_point = points[(points.size * 3) / 4]

                plot_canvas.add_object(
                        Text_Object(f.label, which_point.first, which_point.second, color = f.color)
                )
            }
        }
    }

    plot_canvas.draw_all()
}

/** read comma-delimited x-values from [str], where the values satisfy the form of [constant] */
fun read_x_values(str: String): Array<Double> {
    var pos = 0
    var error = false
    val temp = ArrayList<Double>(5)
    while ((!error) && pos < str.length) {
        val (value, j) = constant(str, pos)
        if (j < str.length && str[j] != ',') error = true
        else {
            temp.add(value.value)
            pos = j + 1
        }
    }
    return if (error) emptyArray() else Array(temp.size) { i -> temp[i] }
}

/**
 * read comma-delimited points from [str]; points are in the form (x,y), where x and y satisfy
 * format of [constant]
 */
fun read_xy_values(str: String): Array<Pair<Double, Double>> {
    var pos = 0
    var error = false
    val temp = ArrayList<Pair<Double, Double>>(5)
    while ((!error) && pos < str.length) {
        while (pos < str.length && str[pos] == ' ') pos += 1
        if (str[pos] != '(') error = true else pos += 1
        val (x_value, j) = constant(str, pos)
        if (j < str.length && str[j] != ',') error = true else pos = j + 1
        val (y_value, k) = constant(str, pos)
        if (k < str.length && str[k] != ')') error = true else pos = k + 1
        temp.add(Pair(x_value.value, y_value.value))
        while (pos < str.length && str[pos] == ' ') pos += 1
        if (pos < str.length && str[pos] == ',') pos += 1
        while (pos < str.length && str[pos] == ' ') pos += 1
    }
    return if (error) emptyArray() else Array(temp.size) { i -> temp[i] }
}

/** processes the data tags associated with [canvas_id] to produce a new graph */
@Suppress("unused")
@JsName("new_graph")
fun new_graph(canvas_id: String) {

    console.log("new graph for $canvas_id")

    val canvas = document.getElementById(canvas_id) as HTMLCanvasElement

    // options related to the view

    val gp =
            Graph_Properties(
                    canvas = canvas,
                    x_min = canvas.getAttribute("data-xmin")?.toDouble() ?: -4.0,
                    x_max = canvas.getAttribute("data-xmax")?.toDouble() ?: 4.0,
                    y_min = canvas.getAttribute("data-ymin")?.toDouble() ?: -3.0,
                    y_max = canvas.getAttribute("data-ymax")?.toDouble() ?: 3.0,
                    with_gridlines = canvas.getAttribute("data-with-grid")?.toBoolean() ?: true,
                    grid_dx = canvas.getAttribute("data-grid-dx")?.toDouble() ?: 1.0,
                    grid_dy = canvas.getAttribute("data-grid-dy")?.toDouble() ?: 1.0
            )

    // interaction with user

    my_properties[canvas_id] = gp
    gp.canvas.addEventListener("mousemove", EventListener { e -> report_movement(e) }, false)
    gp.canvas.addEventListener("wheel", EventListener { e -> zoom_in_or_out(e) }, false)
    // gp.canvas.addEventListener("keydown", key_listener, false)
    // gp.canvas.addEventListener("keyup", key_listener, false)
    gp.canvas.addEventListener("mousedown", EventListener { e -> record_click(e) }, false)
    gp.canvas.addEventListener("mouseup", EventListener { e -> record_click_release(e) }, false)
    gp.canvas.addEventListener("mouseout", EventListener { e -> report_mouse_left(e) }, false)
    gp.canvas.addEventListener("touchmove", EventListener { e -> report_movement(e) }, false)
    gp.canvas.addEventListener("touchstart", EventListener { e -> record_click(e) }, false)
    gp.canvas.addEventListener("touchend", EventListener { e -> record_click_release(e) }, false)

    // functions associated with this view

    for (i in 1.until(10)) {

        // data-which-function, data-which-function2, data-which-function3, etc.
        val suffix = if (i == 1) "" else i.toString()

        if (canvas.getAttribute("data-which-function$suffix") != null) {

            // function and its variable
            val ind_var = canvas.getAttribute("data-ind-var$suffix") ?: "x"
            val which_function = canvas.getAttribute("data-which-function$suffix") ?: "square"

            // input box, if there is one (overwrites f if so)
            val input_box_name = canvas.getAttribute("data-input-box$suffix") ?: ""
            val f =
                    if (input_box_name == "") parse(ind_var, which_function)
                    else {
                        val input_box = document.getElementById(input_box_name) as HTMLInputElement
                        input_box.addEventListener(
                                "change",
                                { e -> report_function_changed(e, canvas_id) },
                                false
                        )
                        console.log("$canvas_id listening to $input_box_name")
                        parse(ind_var, input_box.value)
                    }

            // if we prefer LaTeX labels on graphs, use a <div> enclosure to position it
            val latex_enclosure = canvas.getAttribute("data-latex-enclosure$suffix") ?: ""

            // x values to start and stop graphing, and number of points to use
            val start = canvas.getAttribute("data-start$suffix")?.toDouble() ?: gp.x_min
            val stop = canvas.getAttribute("data-stop$suffix")?.toDouble() ?: gp.x_max
            val num_points = canvas.getAttribute("data-num-points$suffix")?.toInt() ?: 100

            // visual properties
            val color = canvas.getAttribute("data-curve-color$suffix") ?: "#0000ff"
            val dashed = canvas.getAttribute("data-dashed$suffix")?.toBoolean() ?: false
            val thickness = canvas.getAttribute("data-curve-thickness$suffix")?.toDouble() ?: 2.0

            // arrows
            val arrow_left = canvas.getAttribute("data-arrow-left$suffix")?.toBoolean() ?: true
            val arrow_right = canvas.getAttribute("data-arrow-right$suffix")?.toBoolean() ?: true

            // recompute points plotted when view changes?
            val change_view =
                    canvas.getAttribute("data-recompute-points$suffix")?.toBoolean() ?: false

            // known discontinuities
            val asymptotes = read_x_values(canvas.getAttribute("data-asymptotes$suffix") ?: "")
            val holes = read_xy_values(canvas.getAttribute("data-holes$suffix") ?: "")
            val jumps = read_x_values(canvas.getAttribute("data-jumps$suffix") ?: "")

            // points and holes to highlight
            val plugs = read_xy_values(canvas.getAttribute("data-plugs$suffix") ?: "")
            val plug_box_name = canvas.getAttribute("data-plug-box$suffix") ?: ""
            if (plug_box_name != "") {
                val plug_box = document.getElementById(plug_box_name) as HTMLInputElement
                plug_box.addEventListener(
                        "change",
                        { e -> report_plugs_changed(e, canvas_id) },
                        false
                )
                console.log("$canvas_id listening to plugs at $plug_box_name")
            }
            val unplugs = read_xy_values(canvas.getAttribute("data-unplugs$suffix") ?: "")
            val unplug_box_name = canvas.getAttribute("data-unplug-box$suffix") ?: ""
            if (unplug_box_name != "") {
                val unplug_box = document.getElementById(unplug_box_name) as HTMLInputElement
                unplug_box.addEventListener(
                        "change",
                        { e -> report_plugs_changed(e, canvas_id, false) },
                        false
                )
                console.log("$canvas_id listening to unplugs at $unplug_box_name")
            }

            // integral?
            val show_integral = canvas.getAttribute("data-show-integral$suffix").toBoolean()
            val integral_color = canvas.getAttribute("data-integral-color$suffix") ?: "#0000ff40"
            console.log(show_integral.toString())

            // label?
            val label = canvas.getAttribute("data-label$suffix") ?: ""

            // put it together
            val f_props =
                    Function_Properties(
                            ind_var = ind_var,
                            f = f,
                            start = start,
                            stop = stop,
                            color = color,
                            thickness = thickness,
                            dashed = dashed,
                            change_with_view = change_view,
                            num_points = num_points,
                            holes = holes,
                            asymptotes = asymptotes,
                            jumps = jumps,
                            plugs = plugs,
                            unplugs = unplugs,
                            show_integral = show_integral,
                            fill_color = integral_color,
                            modifier = input_box_name,
                            plug_box = plug_box_name,
                            unplug_box = unplug_box_name,
                            arrows = Pair(arrow_left, arrow_right),
                            label = label,
                            latex_enclosure = latex_enclosure
                    )

            // add to records
            if (!fun_properties.containsKey(canvas_id)) fun_properties[canvas_id] = arrayListOf()
            fun_properties[canvas_id]!!.add(f_props)
        }
    }

    apply(gp, fun_properties[canvas_id]!!)
}

fun main() {
    // TODO eventually this should scan the entire document and process tags automatically
    console.log("read script")
}
