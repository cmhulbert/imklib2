package net.imglib2.imklib.examples

import bdv.tools.boundingbox.*
import bdv.tools.brightness.BrightnessDialog
import bdv.tools.brightness.ConverterSetup
import bdv.tools.brightness.SetupAssignments
import bdv.tools.brightness.SliderPanel
import bdv.util.*
import bdv.viewer.animate.AbstractTransformAnimator
import net.imglib2.Interval
import net.imglib2.imklib.compareTo
import net.imglib2.imklib.imklib
import net.imglib2.realtransform.AffineTransform3D
import net.imglib2.util.Intervals
import java.awt.*
import java.util.ArrayList
import javax.swing.*

class JuliaSet {
}

fun main() {

    val cr = 0.2;
    val ci = 0.6
    var threshold = 0

    val juliaSet = imklib.function(2, { imklib.types.int }) { x, y ->
        var i = 0
        var v = 0.0
        var c = x.getDoublePosition(0)
        var d = x.getDoublePosition(1)
        while (i < 64 && v < 4096) {
            val e = c * c - d * d
            d = 2 * c * d
            c = e + cr
            d += ci
            v = if (y <= threshold) {
                Math.sqrt(c * c + d * d)
            } else {
                Math.sqrt(c * c * c + d * d)
            }
            ++i
        }
        y.set(i)
    }

    val view = BdvFunctions.show(
        juliaSet,
        Intervals.createMinMax(-1, -1, 1, 1),
        "Julia Set",
        BdvOptions.options().is2D
    )
    view.setDisplayRange(0.0, 64.0)

    val boundedValue = BoundedValue(0, 75, 0)
    val slider = SliderPanel("Threshold", boundedValue, 1)
    view.bdvHandle.cardPanel.addCard("Threshold", "Threshold", slider, true);
    boundedValue.setUpdateListener {
        threshold = boundedValue.currentValue
        view.getBdvHandle().getViewerPanel().requestRepaint()
    }
}
