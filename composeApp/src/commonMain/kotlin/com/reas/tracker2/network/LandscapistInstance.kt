package com.reas.tracker2.network

import com.skydoves.landscapist.core.Landscapist
import com.skydoves.landscapist.core.NetworkConfig
import com.skydoves.landscapist.core.network.KtorImageFetcher
import io.ktor.client.*

fun landscapistInstance(ktorClient: HttpClient) =
    Landscapist
        .builder()
        .fetcher(KtorImageFetcher(ktorClient, NetworkConfig()))
        .build()
