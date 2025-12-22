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
    
    /**
     * Initialize the activity: set the layout, configure the toolbar, bind views, attach click listeners, and load log contents.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        
        setupToolbar()
        initializeViews()
        setupClickListeners()
        loadLogs()
    }
    
    /**
     * Sets up the activity toolbar as the support action bar, enables the up (back) button, and sets the title to "GENOS Service Logs".
     */
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GENOS Service Logs"
    }
    
    /**
     * Binds the activity's log TextView and control buttons to their view instances and enables scrolling for the log display.
     */
    private fun initializeViews() {
        logTextView = findViewById(R.id.log_text_view)
        clearButton = findViewById(R.id.clear_logs_button)
        refreshButton = findViewById(R.id.refresh_logs_button)
        
        // Enable scrolling for log text
        logTextView.movementMethod = ScrollingMovementMethod()
    }
    
    /**
     * Register click listeners for the Clear and Refresh buttons.
     *
     * The Clear button removes all stored logs and refreshes the displayed log view.
     * The Refresh button reloads the displayed logs.
     */
    private fun setupClickListeners() {
        clearButton.setOnClickListener {
            Logger.clearLogs()
            loadLogs()
        }
        
        refreshButton.setOnClickListener {
            loadLogs()
        }
    }
    
    /**
     * Loads stored log text into the activity's log TextView and scrolls it to the bottom.
     *
     * If log content is available, sets the TextView text to the logs and scrolls to show the latest lines.
     * If no log content is available, sets the TextView text to "No logs available".
     */
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
    
    /**
     * Handle the toolbar "up" button by performing back navigation.
     *
     * @return `true` to indicate the navigation event was handled.
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}