package com.github.hatoyuze.image.edit

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.abs


private val Int.colorRed get() = this shr 16 and 0xFF
private val Int.colorGreen get() = this shr 8 and 0xFF
private val Int.colorBlue get() = this and 0xFF

/**
 * 将不符合 [excludingList] 颜色的像素按照 [fallback] 重新设置像素值
 *
 * 将会返回新的 `BufferedImage` 对象！
 * */
fun filterImageColor(
    image: BufferedImage,
    excludingList: List<Color>,
    enableFuzzy: Boolean = true,
    fallback: (Int) -> Int
): BufferedImage {
    fun Color.matchWithMaxOffset(target: Int): Boolean {
        if (!enableFuzzy) {
            return this.rgb == target
        }
        var remainderOffset = 0x21
        remainderOffset -= abs(target.colorRed - red)
        remainderOffset -= abs(target.colorBlue - blue)
        remainderOffset -= abs(target.colorGreen - green)
        return remainderOffset >= 0
    }

    val result = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val rgb = image.getRGB(x, y)
            if (excludingList.find { it.matchWithMaxOffset(rgb) } != null) {
                result.setRGB(x, y, rgb)
                continue
            }
            val newPixel = fallback.invoke(rgb)
            result.setRGB(x, y, newPixel)
        }
    }
    return result
}


fun Color.toRGBString(): String {
    val r = (this.rgb.colorRed).toString(16).padStart(2, '0')
    val g = (this.rgb.colorGreen).toString(16).padStart(2, '0')
    val b = (this.rgb.colorBlue).toString(16).padStart(2, '0')
    return "#$r$g$b"
}