/*
 * Copyright 2019 David Blanc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.speekha.httpmocker.demo.ui

import android.util.Log
import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainPresenter(
    private val view: MainContract.View,
    private val apiService: GithubApiEndpoints,
    private val mocker: MockResponseInterceptor
) : MainContract.Presenter, CoroutineScope by MainScope() {

    override fun callService() {
        launch {
            try {
                val org = "kotlin"
                val repos = loadRepos(org)
                    .map {
                        val contributor = loadTopContributor(org, it.name)?.firstOrNull()
                        it.copy(topContributor = contributor?.run { "$login - $contributions contributions" })
                    }
                view.setResult(repos)
            } catch (e: Throwable) {
                view.setError(e.message)
            }
        }
    }

    private suspend fun loadRepos(org: String) = withContext(Dispatchers.IO) {
        apiService.listRepositoriesForOrganisation(org)
    }

    private suspend fun loadTopContributor(org: String, repo: String) =
        withContext(Dispatchers.IO) {
            try {
                apiService.listContributorsForRepository(org, repo)
            } catch (e: Throwable) {
                Log.e("Presenter", e.message, e)
                null
            }
        }

    override fun setMode(mode: MockResponseInterceptor.Mode) {
        mocker.mode = mode
        if (mocker.mode == MockResponseInterceptor.Mode.RECORD) {
            view.checkPermission()
        }
        view.updateDescriptionLabel(
            when (mocker.mode) {
                MockResponseInterceptor.Mode.DISABLED -> R.string.disabled_description
                MockResponseInterceptor.Mode.ENABLED -> R.string.enabled_description
                MockResponseInterceptor.Mode.MIXED -> R.string.mixed_description
                MockResponseInterceptor.Mode.RECORD -> R.string.record_description
            }
        )
    }

    override fun stop() {
        coroutineContext.cancel()
    }
}