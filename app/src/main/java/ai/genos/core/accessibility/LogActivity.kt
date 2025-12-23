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
     * Initialize the activity UI, bind views, register click listeners, and populate the log display.
     *
     * Sets the activity layout, configures the toolbar, initializes view references, attaches click handlers
     * for clearing and refreshing logs, and loads current log contents into the view.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state, or `null` if none.
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
     * Configures the activity toolbar as the support action bar, enables the Up button, and sets its title.
     */
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GENOS Service Logs"
    }
    
    /**
     * Binds activity view properties and enables scrolling for the log display.
     *
     * Binds `logTextView`, `clearButton`, and `refreshButton` to their corresponding views in the layout
     * and sets a scrolling movement method on `logTextView` to allow scrolling through log content.
     */
    private fun initializeViews() {
        logTextView = findViewById(R.id.log_text_view)
        clearButton = findViewById(R.id.clear_logs_button)
        refreshButton = findViewById(R.id.refresh_logs_button)
        
        // Enable scrolling for log text
        logTextView.movementMethod = ScrollingMovementMethod()
    }
    
    /**
     * Registers click handlers for the activity's buttons.
     *
     * The Clear button clears stored service logs and refreshes the displayed log view.
     * The Refresh button reloads and displays the current logs.
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
     * Loads current service logs into the activity's log text view and scrolls to the bottom if content exists.
     *
     * Retrieves log contents from the Logger and sets them on `logTextView`. If logs are present, attempts to scroll
     * the view to show the most recent entries; if no logs are available, displays "No logs available".
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
     * Navigate back to the previous activity when the app bar's Up button is pressed.
     *
     * @return `true` to indicate the Up navigation was handled.
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}