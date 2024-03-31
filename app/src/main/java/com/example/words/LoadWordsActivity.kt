package com.example.words

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.example.glancewords.R
import com.example.words.repository.SheetsProvider
import com.example.words.repository.WordsRepository
import com.example.words.widget.WordsGlanceWidget
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LoadWordsActivity : AppCompatActivity() {

    private val wordsRepository = WordsRepository(SheetsProvider.sheets)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fileUri = intent.getWordsSourceFileUri()
        if (fileUri == null) {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_LONG).show()
        } else {
            lifecycleScope.launch {
                if(wordsRepository.copyToLocalFile(this@LoadWordsActivity, fileUri)) {
                    updateWidgets()
                    showMessage(R.string.file_saved_successfully)
                } else {
                    showMessage(R.string.failed_to_save_file)
                }
                finish()
            }
        }
    }

    private fun updateWidgets() = runBlocking { WordsGlanceWidget().updateAll(this@LoadWordsActivity) }

    private fun showMessage(@StringRes message: Int) {
        Toast.makeText(this@LoadWordsActivity, message, Toast.LENGTH_LONG).show()
    }
}

@Suppress("DEPRECATION")
private fun Intent.getWordsSourceFileUri(): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(Intent.EXTRA_STREAM)
    } else {
        getParcelableExtra(Intent.EXTRA_STREAM) as Uri?
    }
}