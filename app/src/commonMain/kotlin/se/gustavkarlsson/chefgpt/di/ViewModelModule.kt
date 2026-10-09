package se.gustavkarlsson.chefgpt.di

import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel
import se.gustavkarlsson.chefgpt.screens.chat.ChatViewModel
import se.gustavkarlsson.chefgpt.screens.debug.DebugViewModel
import se.gustavkarlsson.chefgpt.screens.home.HomeViewModel
import se.gustavkarlsson.chefgpt.screens.ingredients.IngredientsViewModel
import se.gustavkarlsson.chefgpt.screens.loading.LoadingViewModel
import se.gustavkarlsson.chefgpt.screens.login.LoginViewModel
import se.gustavkarlsson.chefgpt.screens.onboarding.OnboardingViewModel
import se.gustavkarlsson.chefgpt.screens.recipe.RecipeDetailViewModel
import se.gustavkarlsson.chefgpt.screens.recipescan.RecipeScanSheetViewModel
import se.gustavkarlsson.chefgpt.screens.recipescrape.RecipeScrapeSheetViewModel

val viewModelModule =
    module {
        // ViewModel scoped
        viewModel<LoadingViewModel>()
        viewModel<OnboardingViewModel>()
        viewModel<LoginViewModel>()
        viewModel<HomeViewModel>()
        viewModel<ChatViewModel>()
        viewModel<IngredientsViewModel>()
        viewModel<DebugViewModel>()
        viewModel<RecipeDetailViewModel>()
        viewModel<RecipeScanSheetViewModel>()
        viewModel<RecipeScrapeSheetViewModel>()
    }
