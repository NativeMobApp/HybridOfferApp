package com.example.OfferApp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.OfferApp.domain.entities.User
import com.example.OfferApp.view.forgotpassword.ForgotPasswordScreen
import com.example.OfferApp.view.login.LogInScreen
import com.example.OfferApp.view.main.*
import com.example.OfferApp.view.register.RegisterScreen
import com.example.OfferApp.viewmodel.AuthViewModel
import com.example.OfferApp.viewmodel.AuthState
import com.example.OfferApp.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Main : Screen("main")
    object CreatePost : Screen("create_post")
    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    object Map : Screen("map")
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object MyProfile : Screen("my-profile") // For the current user'''s profile
}

class MainViewModelFactory(private val user: User) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(user) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun NavGraph(navController: NavHostController, authViewModel: AuthViewModel) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {

        composable(Screen.Login.route) {
            LogInScreen(authViewModel, navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(authViewModel) { navController.popBackStack() }
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(authViewModel) { navController.popBackStack() }
        }

        composable(Screen.Main.route) { backStackEntry ->
            val authState by authViewModel.state.collectAsState()
            val user = (authState as? AuthState.Success)?.user

            if (user != null && user.uid.isNotBlank()) {
                val mainViewModel: MainViewModel = viewModel(factory = MainViewModelFactory(user))
                MainScreen(
                    mainViewModel = mainViewModel,
                    onNavigateToCreatePost = { navController.navigate(Screen.CreatePost.route) },
                    onNavigateToProfile = { navController.navigate(Screen.MyProfile.route) }, // Navigate to current user'''s profile
                    onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
                    onLogoutClicked = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    },
                    onNavigateToMap = { navController.navigate(Screen.Map.route) }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
        }

        val mainViewModelOwner: @Composable (NavBackStackEntry) -> MainViewModel = { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(Screen.Main.route) }
            viewModel(viewModelStoreOwner = parentEntry)
        }

        composable(Screen.MyProfile.route) {
            val mainViewModel = mainViewModelOwner(it)
            ProfileScreen(
                mainViewModel = mainViewModel,
                userId = mainViewModel.user.uid, // Pass the current user'''s ID
                onBackClicked = { navController.popBackStack() },
                onLogoutClicked = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                },
                onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
                onProfileClick = { userId -> navController.navigate(Screen.Profile.createRoute(userId)) }
            )
        }

        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mainViewModel = mainViewModelOwner(backStackEntry)
            val userId = backStackEntry.arguments?.getString("userId")
            if (userId != null) {
                ProfileScreen(
                    mainViewModel = mainViewModel,
                    userId = userId,
                    onBackClicked = { navController.popBackStack() },
                    onLogoutClicked = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    },
                    onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
                    onProfileClick = { otherUserId -> navController.navigate(Screen.Profile.createRoute(otherUserId)) }
                )
            }
        }

        composable(Screen.CreatePost.route) {
            val mainViewModel = mainViewModelOwner(it)
            CreatePostScreen(
                mainViewModel = mainViewModel,
                onPostCreated = {
                    mainViewModel.refreshPosts()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mainViewModel = mainViewModelOwner(backStackEntry)
            val postId = backStackEntry.arguments?.getString("postId")
            val post = postId?.let { mainViewModel.getPostById(it) }

            if (post != null) {
                PostDetailScreen(
                    mainViewModel = mainViewModel,
                    post = post,
                    onBackClicked = { navController.popBackStack() },
                    onLogoutClicked = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    },
                    onProfileClick = { userId -> navController.navigate(Screen.Profile.createRoute(userId)) }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        composable(Screen.Map.route) {
            MapScreen(
                mainViewModel = mainViewModelOwner(it),
                onBackClicked = { navController.popBackStack() }
            )
        }
    }
}
