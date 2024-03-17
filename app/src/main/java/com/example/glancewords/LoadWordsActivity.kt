package com.example.glancewords

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.example.glancewords.repository.WordsRepository
import com.example.glancewords.widget.WordsWidget
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException

class LoadWordsActivity : AppCompatActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM) as Uri?
        }
        if (fileUri == null) {
            Toast.makeText(this, "File not found", Toast.LENGTH_LONG).show()
        } else {
            lifecycleScope.launch {
                if(WordsRepository.copyToLocalFile(this@LoadWordsActivity, fileUri)) {
                    updateWidgets()
                    showMessage("File saved successfully")
                } else {
                    showMessage("Failed to save file")
                }
                finish()
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this@LoadWordsActivity, message, Toast.LENGTH_LONG).show()
    }

    private fun updateWidgets() = runBlocking { WordsWidget().updateAll(this@LoadWordsActivity) }
}