package com.randos.resourceprovider.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

data class HomeScreenState(
    val text: String = ""
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel
) {
    val state by viewModel.uiState.observeAsState(initial = HomeScreenState())
    HomeScreen(modifier, state)

    LaunchedEffect(Unit) {
        viewModel.getData()
    }
}


@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeScreenState
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (state.text.isEmpty()) {
            CircularProgressIndicator()
        } else {
            Text(
                text = state.text,
                modifier = modifier
            )
        }
    }

}

@Preview
@Composable
private fun PreviewHomeScreen() {
    HomeScreen(state = HomeScreenState())
}