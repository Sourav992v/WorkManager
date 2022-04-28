package com.sourav.workmangerproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State.*
import androidx.work.WorkManager
import coil.compose.rememberImagePainter
import com.sourav.workmangerproject.WorkerKeys.FILTER_URI
import com.sourav.workmangerproject.WorkerKeys.IMAGE_URI
import com.sourav.workmangerproject.ui.theme.WorkManagerGuideTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        NetworkType.CONNECTED
                    ).build()
            ).build()
        val colorFilterRequest = OneTimeWorkRequestBuilder<ColorFilterWorker>()
            .build()

        val workManager = WorkManager.getInstance(applicationContext)


        setContent {
            WorkManagerGuideTheme {
                val workInfos = workManager.getWorkInfosForUniqueWorkLiveData("download")
                    .observeAsState()
                    .value

                val downloadInfo = remember(key1 = workInfos) {
                    workInfos?.find { it.id == downloadRequest.id }
                }

                val filterInfo = remember(key1 = workInfos) {
                    workInfos?.find { it.id == colorFilterRequest.id }
                }

                val imageUri by derivedStateOf {
                    val downloadUri = downloadInfo?.outputData?.getString(IMAGE_URI)
                        ?.toUri()
                    val filterUri = filterInfo?.outputData?.getString(FILTER_URI)
                        ?.toUri()

                    filterUri ?: downloadUri
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    imageUri?.let { uri ->
                        Image(
                            painter = rememberImagePainter(
                                data = uri,
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Button(onClick = {
                        workManager.beginUniqueWork(
                            "download",
                            ExistingWorkPolicy.KEEP,
                            downloadRequest
                        ).then(colorFilterRequest)
                            .enqueue()
                    },
                        enabled = downloadInfo?.state != RUNNING
                    ){
                        Text(text = "Start download")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    when(downloadInfo?.state){
                        RUNNING -> Text("Downloading...")
                        SUCCEEDED -> Text("Download succeeded...")
                        ENQUEUED -> Text("Download enqueued...")
                        FAILED -> Text("Download failed...")
                        BLOCKED -> Text("Download blocked...")
                        CANCELLED -> Text("Download canceled...")
                        null -> TODO()
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    when(filterInfo?.state){
                        RUNNING -> Text("Applying filter...")
                        SUCCEEDED -> Text("Filter succeeded...")
                        ENQUEUED -> Text("Filter enqueued...")
                        FAILED -> Text("Filter failed...")
                        BLOCKED -> Text("Filter blocked...")
                        CANCELLED -> Text("Filter canceled...")
                        null -> TODO()
                    }
                }
            }
        }
    }
}