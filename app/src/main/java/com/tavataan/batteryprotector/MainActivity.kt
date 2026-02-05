package com.tavataan.bypass

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var switchBypass: Switch
    private lateinit var textHealth: TextView
    private lateinit var textCurrent: TextView
    private lateinit var textInfo: TextView

    // Variable para controlar el bucle de actualización
    private var monitorJob: Job? = null 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración visual
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_main)

        // Vinculación
        switchBypass = findViewById(R.id.switch_bypass)
        textHealth = findViewById(R.id.tv_battery_health)
        textCurrent = findViewById(R.id.tv_battery_current)
        textInfo = findViewById(R.id.tv_info_details)

        Toast.makeText(this, getString(R.string.tip_toast), Toast.LENGTH_LONG).show()

        loadInfoText()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsSecurity()
        loadSystemData() 
        startCurrentMonitor()
    }

    override fun onPause() {
        super.onPause()
        stopCurrentMonitor()
    }

    // --- FUNCIONES DE SEGURIDAD ---

    private fun checkPermissionsSecurity() {
        lifecycleScope.launch(Dispatchers.IO) {
             if (!RootHandler.isRootGranted()) {
                 withContext(Dispatchers.Main) {
                     redirectToPermissions()
                 }
             }
        }
    }

    private fun redirectToPermissions() {
        val intent = Intent(this, PermissionsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- MONITOREO Y SINCRONIZACIÓN (Aquí está la magia) ---
    
    private fun startCurrentMonitor() {
        stopCurrentMonitor() 

        monitorJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) { 
                // 1. Leemos los datos (Corriente y Estado Real del Bypass)
                val currentStr = RootHandler.getBatteryCurrentNow()
                val isBypassRealmenteActivo = RootHandler.isBypassActive()

                withContext(Dispatchers.Main) {
                    // Actualizamos Texto de Corriente
                    if (currentStr != null) {
                        textCurrent.text = getString(R.string.current_label, currentStr)
                        if (currentStr.contains("-")) {
                            textCurrent.setTextColor(Color.parseColor("#FF8A80")) 
                        } else {
                            textCurrent.setTextColor(Color.parseColor("#69F0AE")) 
                        }
                    } else {
                        textCurrent.text = getString(R.string.current_empty)
                    }

                    // --- AUTOSINCRONIZACIÓN DEL SWITCH ---
                    // Si el switch está diferente a la realidad (porque tocaste el Tile), lo corregimos.
                    if (switchBypass.isChecked != isBypassRealmenteActivo) {
                        // Importante: Quitamos el listener un momento para que al cambiarlo
                        // por código NO se ejecute el comando Root de nuevo (bucle infinito).
                        switchBypass.setOnCheckedChangeListener(null)
                        
                        switchBypass.isChecked = isBypassRealmenteActivo
                        
                        // Volvemos a poner el listener
                        switchBypass.setOnCheckedChangeListener { _, isChecked -> toggleBypass(isChecked) }
                    }
                }
                delay(1000) // Se repite cada 1 segundo
            }
        }
    }

    private fun stopCurrentMonitor() {
        monitorJob?.cancel()
        monitorJob = null
    }

    // --- CONTROL DEL BYPASS ---

    private fun loadSystemData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val isBypassActive = RootHandler.isBypassActive()
            val healthData = RootHandler.getBatteryHealthData()

            withContext(Dispatchers.Main) {
                switchBypass.setOnCheckedChangeListener(null)
                switchBypass.isChecked = isBypassActive
                
                switchBypass.setOnCheckedChangeListener { _, isChecked -> 
                    toggleBypass(isChecked) 
                }

                if (healthData != null) {
                    val (percentage, capacity) = healthData
                    val colorHex = if (percentage > 80) "#69F0AE" else "#FFD54F"
                    
                    textHealth.text = getString(R.string.health_label, percentage, capacity)
                    textHealth.setTextColor(Color.parseColor(colorHex))
                } else {
                    textHealth.text = getString(R.string.error_battery)
                    textHealth.setTextColor(Color.RED)
                }
            }
        }
    }

    private fun toggleBypass(enable: Boolean) {
        // Bloqueamos visualmente para evitar toques múltiples rápidos
        switchBypass.isEnabled = false
        
        lifecycleScope.launch(Dispatchers.IO) {
            val success = RootHandler.setBypass(enable)
            
            withContext(Dispatchers.Main) {
                switchBypass.isEnabled = true
                
                if (!success) {
                    // Si falló, revertimos
                    switchBypass.setOnCheckedChangeListener(null)
                    switchBypass.isChecked = !enable
                    switchBypass.setOnCheckedChangeListener { _, isChecked -> toggleBypass(isChecked) }
                    
                    Toast.makeText(this@MainActivity, getString(R.string.bypass_error_root), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- TEXTOS INFORMATIVOS ---

    private fun loadInfoText() {
        val infoHtml = getString(R.string.info_html)
        textInfo.text = Html.fromHtml(infoHtml, Html.FROM_HTML_MODE_COMPACT)
    }
}