package com.anotherdev.superhealth

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.anotherdev.superhealth.databinding.ActivityMainBinding
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.SettingController
import com.huawei.hms.hihealth.data.Scopes
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var healthSettingController: SettingController

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
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initHuaweiHealth() {
        healthSettingController = HuaweiHiHealth.getSettingController(this)
    }

    private fun requestAuthHuaweiHealth() {
        val scopes = arrayOf(
            Scopes.HEALTHKIT_HEIGHTWEIGHT_READ,
            Scopes.HEALTHKIT_STEP_READ,
            Scopes.HEALTHKIT_HEARTRATE_READ,
            Scopes.HEALTHKIT_ACTIVITY_READ,
            Scopes.HEALTHKIT_ACTIVITY_RECORD_READ,
        )
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
}