package ai.genos.core.accessibility

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.Button
import android.text.method.ScrollingMovementMethod

/**
 * Activity for displaying logs
 */
class LogActivity : AppCompatActivity() {
    
    private lateinit var logTextView: TextView
    private lateinit var clearButton: Button
    private lateinit var refreshButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        
        setupToolbar()
        initializeViews()
        setupClickListeners()
        loadLogs()
    }
    
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GENOS Service Logs"
    }
    
    private fun initializeViews() {
        logTextView = findViewById(R.id.log_text_view)
        clearButton = findViewById(R.id.clear_logs_button)
        refreshButton = findViewById(R.id.refresh_logs_button)
        
        // Enable scrolling for log text
        logTextView.movementMethod = ScrollingMovementMethod()
    }
    
    private fun setupClickListeners() {
        clearButton.setOnClickListener {
            Logger.clearLogs()
            loadLogs()
        }
        
        refreshButton.setOnClickListener {
            loadLogs()
        }
    }
    
    private fun loadLogs() {
        val logContents = Logger.getLogContents()
        if (logContents != null) {
            logTextView.text = logContents
            
            // Scroll to bottom
            val scrollAmount = logTextView.layout.getLineTop(logTextView.lineCount) - logTextView.height
            if (scrollAmount > 0) {
                logTextView.scrollTo(0, scrollAmount)
            }
        } else {
            logTextView.text = "No logs available"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}