package com.example.glancewords

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.example.glancewords.repository.WordsRepository
import com.example.glancewords.widget.WordsWidget
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_LONG).show()
        } else {
            lifecycleScope.launch {
                if(WordsRepository.copyToLocalFile(this@LoadWordsActivity, fileUri)) {
                    updateWidgets()
                    showMessage(R.string.file_saved_successfully)
                } else {
                    showMessage(R.string.failed_to_save_file)
                }
                finish()
            }
        }
    }

    private fun showMessage(@StringRes message: Int) {
        Toast.makeText(this@LoadWordsActivity, message, Toast.LENGTH_LONG).show()
    }

    private fun updateWidgets() = runBlocking { WordsWidget().updateAll(this@LoadWordsActivity) }
}