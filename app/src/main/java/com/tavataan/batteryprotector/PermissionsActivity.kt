package com.tavataan.bypass

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PermissionsActivity : AppCompatActivity() {

    private lateinit var switchRoot: Switch
    private lateinit var layoutTutorial: LinearLayout
    private lateinit var switchOpenPanel: Switch
    private lateinit var btnContinue: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configuración de ventana transparente
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        // --- CORRECCIÓN DEL PARPADEO ---
        // NO llamamos a setContentView todavía. La pantalla está en negro (por el tema).
        
        lifecycleScope.launch(Dispatchers.Main) {
            // 1. Verificación Rápida: ¿El usuario ya terminó el tutorial antes?
            val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
            val isSetupCompleted = prefs.getBoolean("SETUP_COMPLETED", false)

            if (isSetupCompleted) {
                // 2. Si ya terminó, verificamos Root en segundo plano
                val hasRoot = withContext(Dispatchers.IO) {
                    RootHandler.isRootGranted()
                }

                if (hasRoot) {
                    // CASO PERFECTO: Tiene todo listo.
                    // Saltamos directo sin mostrar NADA de esta pantalla.
                    goToMain()
                    return@launch
                }
            }

            // CASO PENDIENTE: O no terminó el tutorial, o perdió el Root.
            // Recién ahora "pintamos" la interfaz del tutorial.
            initializeUI()
        }
    }

    // Movemos toda la carga de interfaz a esta función separada
    private fun initializeUI() {
        setContentView(R.layout.activity_permissions)

        switchRoot = findViewById(R.id.switch_permission_root)
        layoutTutorial = findViewById(R.id.layout_tutorial)
        switchOpenPanel = findViewById(R.id.switch_open_panel)
        btnContinue = findViewById(R.id.btn_continue)

        setupListeners()
        
        // Iniciamos la lógica visual del tutorial
        checkRootAndUpdateUI()
    }

    private fun setupListeners() {
        switchRoot.setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_requesting_root), Toast.LENGTH_LONG).show()
            switchRoot.isEnabled = false 
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    Runtime.getRuntime().exec("su")
                } catch (e: Exception) { e.printStackTrace() }

                delay(1500) 
                withContext(Dispatchers.Main) {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    System.exit(0)
                }
            }
        }

        switchOpenPanel.setOnClickListener {
            forceExpandStatusBarRoot()
            Toast.makeText(this, getString(R.string.toast_edit_panel), Toast.LENGTH_LONG).show()
            
            if (btnContinue.visibility != View.VISIBLE) {
                btnContinue.visibility = View.VISIBLE
                btnContinue.alpha = 0f
                btnContinue.animate().alpha(1f).setDuration(500).start()
            }
        }

        btnContinue.setOnClickListener {
            val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("SETUP_COMPLETED", true).apply()
            goToMain()
        }
    }

    private fun checkRootAndUpdateUI() {
        lifecycleScope.launch(Dispatchers.IO) {
            val hasRoot = RootHandler.isRootGranted()
            
            withContext(Dispatchers.Main) {
                if (hasRoot) {
                    // Tiene Root pero estamos en esta pantalla => Falta completar el tutorial
                    switchRoot.isChecked = true
                    switchRoot.isEnabled = false
                    
                    if (layoutTutorial.visibility != View.VISIBLE) {
                        layoutTutorial.visibility = View.VISIBLE
                        layoutTutorial.alpha = 0f
                        layoutTutorial.animate().alpha(1f).setDuration(500).start()
                    }
                } else {
                    // No tiene Root
                    layoutTutorial.visibility = View.GONE
                    switchRoot.isChecked = false
                    switchRoot.isEnabled = true
                }
            }
        }
    }

    private fun forceExpandStatusBarRoot() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Runtime.getRuntime().exec("su -c cmd statusbar expand-settings")
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    expandStatusBarLegacy()
                }
            }
        }
    }

    private fun expandStatusBarLegacy() {
        try {
            val service = getSystemService("statusbar")
            val statusbarManager = Class.forName("android.app.StatusBarManager")
            val expand = statusbarManager.getMethod("expandSettingsPanel")
            expand.invoke(service)
        } catch (e: Exception) {
             Toast.makeText(this, getString(R.string.toast_manual_panel), Toast.LENGTH_SHORT).show()
        }
    }
    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        
        // --- CORRECCIÓN PROFESIONAL DE TRANSICIÓN ---
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            // Para Android 14 (Upside Down Cake) y superiores
            overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            // Para Android 13 y anteriores
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        
        finish() 
    }
}