package com.takusemba.spotlight

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.animation.ValueAnimator.INFINITE
import android.animation.ValueAnimator.ofFloat
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

/**
 * [SpotlightView] starts/finishes [Spotlight], and starts/finishes a current [Target].
 */
internal class SpotlightView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @ColorRes backgroundColor: Int = R.color.background
) : FrameLayout(context, attrs, defStyleAttr) {

  private val backgroundPaint by lazy {
    Paint().apply { color = ContextCompat.getColor(context, backgroundColor) }
  }

  private val shapePaint by lazy {
    Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
  }

  private val effectPaint by lazy { Paint() }

  private val invalidator = AnimatorUpdateListener { invalidate() }

  private var shapeAnimator: ValueAnimator? = null
  private var effectAnimator: ValueAnimator? = null
  private var target: Target? = null

  init {
    setWillNotDraw(false)
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    isClickable = true
    isFocusable = true
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
    val currentTarget = target
    val currentShapeAnimator = shapeAnimator
    val currentEffectAnimator = effectAnimator
    if (currentTarget != null && currentEffectAnimator != null) {
      currentTarget.effect.draw(
          canvas = canvas,
          point = currentTarget.anchor,
          value = currentEffectAnimator.animatedValue as Float,
          paint = effectPaint
      )
    }
    if (currentTarget != null && currentShapeAnimator != null) {
      currentTarget.shape.draw(
          canvas = canvas,
          point = currentTarget.anchor,
          value = currentShapeAnimator.animatedValue as Float,
          paint = shapePaint
      )
    }
  }

  /**
   * Starts [Spotlight].
   */
  fun startSpotlight(
      duration: Long,
      interpolator: TimeInterpolator,
      listener: Animator.AnimatorListener
  ) {
    val objectAnimator = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
      setDuration(duration)
      setInterpolator(interpolator)
      addListener(listener)
    }
    objectAnimator.start()
  }

  /**
   * Finishes [Spotlight].
   */
  fun finishSpotlight(
      duration: Long,
      interpolator: TimeInterpolator,
      listener: Animator.AnimatorListener
  ) {
    val objectAnimator = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
      setDuration(duration)
      setInterpolator(interpolator)
      addListener(listener)
    }
    objectAnimator.start()
  }

  /**
   * Starts the provided [Target].
   */
  fun startTarget(target: Target, listener: Animator.AnimatorListener) {
    removeAllViews()
    if (target.overlay?.parent != null) (target.overlay.parent as ViewGroup).removeView(
        target.overlay
    )

    val infoDialogParams = LayoutParams(
        MATCH_PARENT,
        WRAP_CONTENT
    )

    if (target.anchor.y < height / 2) {
      infoDialogParams.gravity = Gravity.TOP

      infoDialogParams.setMargins(
          0,
          getRelativeTop(target.currentView!!) + context.resources.getDimension(
              R.dimen.default_margin).toInt() + target.currentView.height,
          0,
          0
      )
    } else {
      infoDialogParams.gravity = Gravity.BOTTOM
      infoDialogParams.setMargins(
          0,
          0,
          0,
          (height - getRelativeTop(target.currentView!!) + context.resources.getDimension(
              R.dimen.default_margin).toInt())
      )
    }

    target.overlay?.layoutParams = infoDialogParams
    target.overlay?.postInvalidate()
    addView(target.overlay)

    this.target = target
    this.shapeAnimator = ofFloat(0f, 1f).apply {
      duration = target.shape.duration
      interpolator = target.shape.interpolator
      addUpdateListener(invalidator)
      addListener(listener)
    }
    this.effectAnimator = ofFloat(0f, 1f).apply {
      duration = target.effect.duration
      interpolator = target.effect.interpolator
      repeatMode = target.effect.repeatMode
      repeatCount = INFINITE
      addUpdateListener(invalidator)
      addListener(listener)
    }
    shapeAnimator?.start()
    effectAnimator?.start()
  }

  /**
   * Finishes the current [Target].
   */
  fun finishTarget(listener: Animator.AnimatorListener) {
    val currentTarget = target ?: return
    val currentShapeAnimator = shapeAnimator ?: return
    shapeAnimator = ofFloat(currentShapeAnimator.animatedValue as Float, 0f).apply {
      duration = currentTarget.shape.duration
      interpolator = currentTarget.shape.interpolator
      addUpdateListener(invalidator)
      addListener(listener)
    }
    effectAnimator?.cancel()
    effectAnimator = null
    shapeAnimator?.start()
  }

  private fun getRelativeTop(myView: View): Int {
    return if (myView.parent === myView.rootView) myView.top else myView.top + getRelativeTop(
        myView.parent as View
    )
  }
}
