package se.gustavkarlsson.chefgpt.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEvent.SwipeEdge

object NavigationTransitions {
    private const val SPRING_STIFFNESS = Spring.StiffnessMediumLow
    private const val PREDICTIVE_SLIDE_IN_OFFSET_RATIO = 0.03 // Ratio of full width/height
    private const val PREDICTIVE_SLIDE_OUT_OFFSET_RATIO = 0.06 // Ratio of full width/height
    private const val SCALE_SMALLER = 0.95f
    private const val SCALE_BIGGER = 1.05f

    private val pushEnterTransition =
        fadeIn(spring(stiffness = SPRING_STIFFNESS)) +
            scaleIn(spring(stiffness = SPRING_STIFFNESS), initialScale = SCALE_SMALLER)

    private val pushExitTransition =
        fadeOut(spring(stiffness = SPRING_STIFFNESS)) +
            scaleOut(spring(stiffness = SPRING_STIFFNESS), targetScale = SCALE_BIGGER)

    private val popEnterTransition =
        fadeIn(spring(stiffness = SPRING_STIFFNESS)) +
            scaleIn(spring(stiffness = SPRING_STIFFNESS), initialScale = SCALE_BIGGER)

    private val popExitTransition =
        fadeOut(spring(stiffness = SPRING_STIFFNESS)) +
            scaleOut(spring(stiffness = SPRING_STIFFNESS), targetScale = SCALE_SMALLER)

    private fun getPredictivePopEnterTransition(swipeEdge: @SwipeEdge Int): EnterTransition {
        val directionMultiplier =
            when (swipeEdge) {
                NavigationEvent.EDGE_RIGHT -> -1
                NavigationEvent.EDGE_LEFT -> 1
                NavigationEvent.EDGE_NONE -> 1
                else -> 1
            }
        return fadeIn(spring(stiffness = SPRING_STIFFNESS)) +
            slideInHorizontally(spring(stiffness = SPRING_STIFFNESS)) {
                (-it * PREDICTIVE_SLIDE_IN_OFFSET_RATIO * directionMultiplier).toInt()
            } + scaleIn(spring(stiffness = SPRING_STIFFNESS), initialScale = SCALE_BIGGER)
    }

    private fun getPredictivePopExitTransition(swipeEdge: @SwipeEdge Int): ExitTransition {
        val directionMultiplier =
            when (swipeEdge) {
                NavigationEvent.EDGE_RIGHT -> -1
                NavigationEvent.EDGE_LEFT -> 1
                NavigationEvent.EDGE_NONE -> 1
                else -> 1
            }
        return fadeOut(spring(stiffness = SPRING_STIFFNESS)) +
            slideOutHorizontally(spring(stiffness = SPRING_STIFFNESS)) {
                (it * PREDICTIVE_SLIDE_OUT_OFFSET_RATIO * directionMultiplier).toInt()
            } + scaleOut(spring(stiffness = SPRING_STIFFNESS), targetScale = SCALE_SMALLER)
    }

    val transitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        ContentTransform(pushEnterTransition, pushExitTransition)
    }
    val popTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        ContentTransform(popEnterTransition, popExitTransition)
    }
    val predictivePopTransitionSpec: AnimatedContentTransitionScope<*>.(swipeEdge: @SwipeEdge Int) -> ContentTransform =
        { swipeEdge ->
            ContentTransform(getPredictivePopEnterTransition(swipeEdge), getPredictivePopExitTransition(swipeEdge))
        }
}
