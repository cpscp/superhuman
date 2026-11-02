package com.carlos.liquid.glass

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carlos.liquid.icons.Squircle

/** Ambiente do vidro: wallpaper desfocado + preferências. */
@Immutable
class GlassEnv(
    val source: Bitmap?,          // wallpaper desfocado e reduzido
    val screenW: Int,
    val screenH: Int,
    val dark: Boolean,
    val tinted: Boolean,
    val refraction: Float,
    val dim: Float,
) {
    val shader: BitmapShader? = source?.let { b ->
        BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply { setScale(screenW / b.width.toFloat(), screenH / b.height.toFloat()) })
            if (Build.VERSION.SDK_INT >= 33) filterMode = BitmapShader.FILTER_MODE_LINEAR
        }
    }
}

val LocalGlass = staticCompositionLocalOf { GlassEnv(null, 1080, 2400, false, false, 1f, 0f) }

private const val AGSL = """
uniform shader wall;
uniform float2 size;
uniform float2 origin;
uniform float radius;
uniform float bezel;
uniform float refr;
uniform float4 tint;
uniform float rim;
uniform float squircle;
uniform float dim;

float sdRound(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

float sdSquircle(float2 p, float2 b) {
    float2 n = abs(p) / b;
    float k = pow(pow(n.x, 5.0) + pow(n.y, 5.0), 0.2);
    return (k - 1.0) * min(b.x, b.y);
}

float sdf(float2 p, float2 hs) {
    return squircle > 0.5 ? sdSquircle(p, hs) : sdRound(p, hs, radius);
}

half4 main(float2 fc) {
    float2 hs = size * 0.5;
    float2 p = fc - hs;
    float d = sdf(p, hs);
    if (d > 1.0) { return half4(0.0); }
    float e = 0.8;
    float2 g = float2(sdf(p + float2(e, 0.0), hs) - sdf(p - float2(e, 0.0), hs),
                      sdf(p + float2(0.0, e), hs) - sdf(p - float2(0.0, e), hs));
    float2 n = normalize(g + float2(0.00001, 0.00001));
    float inside = clamp(-d / bezel, 0.0, 1.0);
    float bend = 1.0 - inside;
    bend = bend * bend;
    float2 off = -n * bend * refr * bezel * 0.55;
    float2 base = origin + fc;
    float4 c = float4(0.0);
    c.r = float(wall.eval(base + off * 1.12).r);
    c.g = float(wall.eval(base + off).g);
    c.b = float(wall.eval(base + off * 0.88).b);
    c.a = 1.0;
    float l = dot(c.rgb, float3(0.299, 0.587, 0.114));
    c.rgb = mix(float3(l), c.rgb, 1.25);
    c.rgb *= (1.0 - dim);
    c.rgb = mix(c.rgb, tint.rgb, tint.a);
    float band = smoothstep(2.2, 0.0, -d);
    float ld = dot(n, normalize(float2(-0.55, -0.85)));
    float spec = band * (0.30 + 0.70 * max(ld, 0.0)) + band * 0.45 * max(-ld, 0.0);
    c.rgb += float3(spec * rim);
    c.rgb += float3(bend * 0.07 * rim);
    float a = clamp(0.5 - d, 0.0, 1.0);
    return half4(c * a);
}
"""

/**
 * Liquid Glass: refração nas bordas, aberração cromática ligeira, brilho especular
 * e tom (claro/escuro, transparente/tingido). Em Android < 13 usa vidro fosco simples.
 */
fun Modifier.liquidGlass(
    corner: Dp = 28.dp,
    squircle: Boolean = false,
    tint: Color? = null,
    strength: Float = 1f,
    bezel: Dp = 16.dp,
): Modifier = composed {
    val env = LocalGlass.current
    val origin = remember { mutableStateOf(Offset.Zero) }
    val t = tint ?: defaultTint(env)
    this
        .onGloballyPositioned { origin.value = it.positionInWindow() }
        .drawWithCache {
            val rPx = corner.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
            val bezelPx = bezel.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
            val outline = Path().apply {
                if (squircle) addPath(Squircle.path(size.width, size.height).asComposePath())
                else addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, CornerRadius(rPx)))
            }
            val src = env.shader
            val rs: RuntimeShader? = if (src != null && Build.VERSION.SDK_INT >= 33) runCatching { RuntimeShader(AGSL) }.getOrNull() else null
            if (src != null && rs != null) {
                rs.setInputShader("wall", src)
                rs.setFloatUniform("size", size.width, size.height)
                rs.setFloatUniform("radius", rPx)
                rs.setFloatUniform("bezel", bezelPx)
                rs.setFloatUniform("refr", env.refraction * strength)
                rs.setFloatUniform("tint", t.red, t.green, t.blue, t.alpha)
                rs.setFloatUniform("rim", if (env.dark) 0.22f else 0.38f)
                rs.setFloatUniform("squircle", if (squircle) 1f else 0f)
                rs.setFloatUniform("dim", env.dim)
                val brush = ShaderBrush(rs)
                onDrawBehind {
                    rs.setFloatUniform("origin", origin.value.x, origin.value.y)
                    drawRect(brush)
                }
            } else {
                val m = Matrix()
                onDrawBehind {
                    clipPath(outline) {
                        if (src != null) {
                            val b = env.source!!
                            m.setScale(env.screenW / b.width.toFloat(), env.screenH / b.height.toFloat())
                            m.postTranslate(-origin.value.x, -origin.value.y)
                            src.setLocalMatrix(m)
                            drawRect(ShaderBrush(src))
                            if (env.dim > 0f) drawRect(Color.Black.copy(alpha = env.dim))
                        }
                        drawRect(t)
                    }
                    drawPath(outline, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.2f))), style = Stroke(1.2.dp.toPx()))
                }
            }
        }
}

fun defaultTint(env: GlassEnv): Color = when {
    env.tinted && env.dark -> Color(0xFF1C1C1E).copy(alpha = 0.62f)
    env.tinted -> Color(0xFFF5F5F7).copy(alpha = 0.62f)
    env.dark -> Color(0xFF000000).copy(alpha = 0.22f)
    else -> Color(0xFFFFFFFF).copy(alpha = 0.10f)
}

/** Tom mais opaco para painéis grandes (menus, folhas). */
fun panelTint(env: GlassEnv): Color = when {
    env.dark -> Color(0xFF1C1C1E).copy(alpha = if (env.tinted) 0.82f else 0.55f)
    else -> Color(0xFFF7F7F9).copy(alpha = if (env.tinted) 0.85f else 0.55f)
}

@Composable
fun rememberGlassEnv(source: Bitmap?, w: Int, h: Int, dark: Boolean, tinted: Boolean, refraction: Float, dim: Float): GlassEnv =
    remember(source, w, h, dark, tinted, refraction, dim) { GlassEnv(source, w, h, dark, tinted, refraction, dim) }
