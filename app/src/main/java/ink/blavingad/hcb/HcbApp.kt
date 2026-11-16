package ink.blavingad.hcb

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import ink.blavingad.hcb.data.Api
import ink.blavingad.hcb.data.Auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class HcbApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: Container
    override fun onCreate() { super.onCreate(); container = Container(this); ink.blavingad.hcb.notify.TxWorker.schedule(this, container.prefs.notifyEvery.value) }
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.http })); add(coil3.svg.SvgDecoder.Factory()) }
        .build()
}

class Container(context: Context) {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true; isLenient = true }
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", "HCBMaterial/${BuildConfig.VERSION_NAME} (Android)").build()) }
        .build()
    val auth = Auth(context, http, json)
    val api = Api(http, auth, json)
    val public = ink.blavingad.hcb.data.PublicApi(http, json)
    /** pending https://hcb.hackclub.com/… or https://hcb.gg/… link to open in-app */
    val deepLink = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val prefs = Prefs(context)
    val fx = ink.blavingad.hcb.data.Fx(context, http, json)
    @Volatile var skipRelock = false
    val unlocked = kotlinx.coroutines.flow.MutableStateFlow(false)
    val receiptShortcut = kotlinx.coroutines.flow.MutableStateFlow<ink.blavingad.hcb.ui.ReceiptShortcut?>(null)
}

val Context.container: Container get() = (applicationContext as HcbApp).container
