package com.anotherdev.superhealth

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.anotherdev.superhealth.databinding.ActivityMainBinding
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hms.hihealth.ConsentsController
import com.huawei.hms.hihealth.DataController
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.SettingController
import com.huawei.hms.hihealth.data.DataType
import com.huawei.hms.hihealth.data.SamplePoint
import com.huawei.hms.hihealth.data.Scopes
import com.huawei.hms.hihealth.options.ReadOptions
import timber.log.Timber
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val scopes = arrayOf(
        Scopes.HEALTHKIT_STEP_BOTH,
        Scopes.HEALTHKIT_HEARTRATE_BOTH,
        Scopes.HEALTHKIT_HEIGHTWEIGHT_BOTH,
        //Scopes.HEALTHKIT_ACTIVITY_READ,
        //Scopes.HEALTHKIT_ACTIVITY_RECORD_READ,
    )

    private lateinit var binding: ActivityMainBinding
    private lateinit var healthSettingController: SettingController
    private lateinit var consentsController: ConsentsController
    private lateinit var dataController: DataController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarMain)

        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setLogo(R.drawable.logo_super_health)
            setDisplayUseLogoEnabled(true)
            title = getString(R.string.app_label)
        }

        initHuaweiHealth()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_login -> {
                requestAuthHuaweiHealth()
            }
            R.id.menu_connect -> {
                read()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initHuaweiHealth() {
        healthSettingController = HuaweiHiHealth.getSettingController(this)
        consentsController = HuaweiHiHealth.getConsentsController(this)
        dataController = HuaweiHiHealth.getDataController(this)

        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            .apply {
                val hmsAppId = metaData.getString("com.huawei.hms.client.appid")
                Timber.e("packageName: %s hmsAppId: %s", packageName, hmsAppId)

                consentsController.get("en-us", hmsAppId?.removePrefix("appid="))
                    .addOnSuccessListener { scopeItem ->
                        val msgList = mutableListOf(
                            String.format("Granted Scope Query app: %s", scopeItem.appName),
                            String.format("appIconPath: %s", scopeItem.appIconPath),
                            String.format("authTime: %s", scopeItem.authTime)
                        )
                        scopeItem.url2Desc.forEach { entry ->
                            msgList.add(String.format("scope: %s (%s)", entry.key, entry.value))
                        }
                        binding.healthkitLogTextview.text = null
                        msgList.forEach { msg ->
                            Timber.e(msg)
                            binding.healthkitLogTextview.append(msg + "\n")
                        }
                    }
                    .addOnFailureListener { Timber.e(it, "%s: error", packageName) }
            }
    }

    private fun requestAuthHuaweiHealth() {
        val intent = healthSettingController.requestAuthorizationIntent(scopes, true)
        requestAuth.launch(intent)
    }

    private val requestAuth = registerForActivityResult(StartActivityForResult()) { result ->
        val authResult = healthSettingController.parseHealthKitAuthResultFromIntent(result.data)
        if (authResult != null && authResult.isSuccess) {
            Timber.e("Huawei Health Request Auth success. authResult: %s", authResult)
            authResult.authAccount?.authorizedScopes?.forEach { scope ->
                Timber.e("Authorized scope: %s", scope)
            }
        } else {
            Timber.e("Huawei Health Request Auth failed. authResult: %s", authResult)
        }
    }

    private fun read() {
        val onSuccess = OnSuccessListener<Map<DataType,SamplePoint>> { samplePointMap ->
            samplePointMap?.forEach { entry ->
                Timber.e("Type: %s SamplePoint: %s", entry.key, entry.value)
                entry.value.fieldValues.forEach {
                    Timber.e("  Field name: %s value: %s", it.key, it.value)
                }
            }
        }

        val dataTypes = listOf(
            DataType.DT_INSTANTANEOUS_HEART_RATE
        )
        dataController.readLatestData(dataTypes)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener { Timber.e(it, "readLatestData error %s", dataTypes) }

        val yesterday = ZonedDateTime.now().toInstant().toEpochMilli()
        val now = ZonedDateTime.now().toInstant().toEpochMilli()
        val readOptions = ReadOptions.Builder()
            .read(DataType.DT_CONTINUOUS_STEPS_TOTAL)
            .setTimeRange(yesterday, now, TimeUnit.MILLISECONDS)
            .build()
        dataController.read(readOptions)
            .addOnSuccessListener { readReply ->
                Timber.e("OnSuccessListener: %s", readOptions.dataTypes)
                readReply.sampleSets.flatMap { it.samplePoints }
                    .forEach { samplePoint ->
                        samplePoint.toString()
                        Timber.e("SamplePoint: %s", samplePoint)
                        samplePoint.fieldValues.forEach {
                            Timber.e("  Field name: %s value: %s", it.key, it.value)
                        }
                    }
            }
            .addOnFailureListener { Timber.e(it, "read error %s", readOptions) }
    }
}