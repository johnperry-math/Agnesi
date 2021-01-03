# Agnesi

## Description

Agnesi is an interactive graphing calculator for use in HTML files.
It is named for [Maria Gaetana Agnesi](http://en.wikipedia.org/wiki/Maria_Gaetana_Agnesi),
who wrote one of the first Calculus textbooks,
was appointed a professor of mathematics at the University of Bologna,
and eventually retired to a monastery.

## Purpose

The library aims to
* be interactive;
* be programmable via JavaScript (or languages that compile to JavaScript);
* automatically put arrows on curves;
* graph known discontinuities;
* provide gridlines;
* look like someone cared at least a little about the output.

The user can drag (by mouse or touch) and zoom (by pinch or scrollwheel),
and the client HTML page can redefine functions and/or highlight points or holes
via input boxes or JavaScript.

This is a long-term project, with more features planned.

## Status

This is a work-in-progress and is provided free-of-charge,
so while it is in current use,
it isn't guaranteed to be suitable for any needs except my own,
and even then I encounter bugs on occasion.

## Usage

* First build it in Idea, obtaining the files
`Agnesi.js`, `Agnesi.js.map` in the `build/distributions` folder.
* Copy these files to the folder where your HTML source code resides.
* In the HTML file, create a `canvas` tag, adding `data-` tags to define the
viewing area, the function(s), and options for graphing.
   * It is essential to give an ID to the Canvas object; for instance,
    `<canvas id="myCanvas" ...>`.
   * A list of `data-` tags Agnesi currently recognizes appear below.
   * An example HTML file is included.
* At the *beginning* of the HTML file, preferably in the header,
add the following line:

   ```html
   <script type="text/javascript" src="path/to/Agnesi.js"></script>
  ```
  
   * `path/to/` is the path to wherever you've placed `Agnesi.js`.
    If `Agnesi.js` is in the same folder as the HTML file,
    you can simply write `src=Agnesi.js` and ignore the path.
   * You can probably place this line anywhere in the file,
     just so long as it appears before the line stated below.
* At the *end* of the HTML file, immediately before the `</body>` tag,
add the following lines:
   ```html
   <script type="text/javascript">
      Agnesi.new_graph("myCanvas")
   </script>
   ```
   where `myCanvas` is the ID you assigned to the canvas above.
   
For an example, see the links below.

## Current `data-` tags

Defaults, if any, shown in parentheses.

* "Per-canvas" tags
  * only one tag per canvas
  * smallest and largest x-values shown: `data-xmin` (-4), `data-xmax` (4)
  * smallest and largest y-values shown: `data-ymin` (-3), `data-ymax` (3)
  * whether to show gridlines: `data-with-gridlines` (true)
  * absolute units the gridlines represent: `data-grid-dx` (1.0), `data-grid-dy` (1.0)
  
* "Per-function" tags
  * up to 10 per canvas
    * for the first function to graph, use the tag "as is"; e.g., `data-which-function=x^2`
    * for the second, third, etc., append a numerical suffix; e.g., `data-whichfunction2=2*x-1`
  * tags that affect the function's definition
    * the function to graph: `data-which-function`
    * the independent variable: `data-ind-var` (x)
    * an input box where the user can redefine the function: `data-input-box`
  * tags that affect the function's domain
    * smallest and largest x-values used to compute y-values:
      `data-start` (`data-xmin`) and `data-stop` (`data-xmax`)
    * how many points to use when graphing: `data-num-points` (100)
    * whether to recompute the points whenever a drag or zoom modifies the graph's view:
      `data-recompute-points` (false)
      * (this effectively ignores `data-start` and `data-stop`!)
  * tags that affect visual properties
    * the color: `data-curve-color` (`#0000ff`)
    * whether the curve is dashed: `data-dashed` (false)
    * the curve's thickness: `data-curve-thickness` (2.0)
  * whether to draw arrow at the left and right endpoints:
    `data-arrow-left` and `data-arrow-right` (true)
  * known discontinuities, graphed correctly
    * asymptotes, given as a comma-separated list of x-values: `data-asymptotes`
    * holes, given as a comma-separated list of ordered pairs: `data-holes`
    * jumps, given as a comma-separated list of x-values: `data-jumps`
  * things to highlight that don't otherwise affect the points drawn, given as a comma-separated list of ordered pairs
    * "plugs", or highlighted points: `data-plugs`
    * "unplugs", or holes
      (but the graph ignores them unless you use `data-holes`, in which case it automatically draws a hole):
      `data-unplugs`
    * an input box where the user can redefine the plugs: `data-plug-box`
    * an input box where the user can redefine the unplugs: `data-unplug-box`
    * the area between the curve and the x-axis: `data-show-integral` (`false`)
    * the color of the area between the curve and the x-axis: `data-integral-color` (`#0000ff40`)
  * a text label, placed slightly above or below the curve, 3/4 of the way across its domain: `data-label`

...more to come!

## Structure

The source code is in Kotlin and compiles to JavaScript.
Once Kotlin compiles to WebAssembly, I intend to bypass JavaScript entirely.

Currently, there are two main files.

* [`Agnesi.kt`](src/main/kotlin/Agnesi.kt) handles the user interface: graphing,
processing of settings, and HTML / JavaScript interaction.
* [`Evaluation.kt`](src/main/kotlin/Evaluation.kt) handles the parsing of functions
and the evaluation of values.

There is also an example HTML page.

* [`index.html`](src/main/resources/index.html) demonstrates a few properties,
including the interactivity.

## Examples of this package in "real-world" use

* [interactive Calculus I notes](https://www.math.usm.edu/perry/CalcI/index.html)
  (most pages are currently using an older version of this package;
   the linked page uses the newer version)

## Gratuitous image of Maria Gaetana Agnesi

![image of Maria Gaetana Agnesi](Maria_Gaetana_Agnesi_transparent.png)

## License

<span xmlns:dct="http://purl.org/dc/terms/" property="dct:title">Agnesi</span> by
<a xmlns:cc="http://creativecommons.org/ns#" href="https://github.com/johnperry-math" property="cc:attributionName" rel="cc:attributionURL">
  John Perry</a>
is licensed under a
<a rel="license" href="http://creativecommons.org/licenses/by-sa/4.0/">
  Creative Commons Attribution-ShareAlike 4.0 International License</a>.

![CC-BY-SA](https://i.creativecommons.org/l/by-sa/4.0/88x31.png)